package com.lia.assistant.ui

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.lia.assistant.LiaService
import com.lia.assistant.data.KeyStore
import java.util.Locale

enum class VoiceState { IDLE, LISTENING, THINKING, SPEAKING }

class VoiceViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Application = app
    private val keys = KeyStore(app)
    private val main = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeak: String? = null
    private var lastUtteranceId = ""
    private var errorStreak = 0

    /** LiaApplication এখানে ChatViewModel বসিয়ে দেয় */
    var chat: ChatViewModel? = null

    /** LiaService নোটিফিকেশন আপডেটের জন্য এটা বসায় */
    var onChange: (() -> Unit)? = null

    private var _state by mutableStateOf(VoiceState.IDLE)
    var state: VoiceState
        get() = _state
        private set(v) {
            _state = v
            onChange?.invoke()
        }

    private var _muted by mutableStateOf(false)
    var muted: Boolean
        get() = _muted
        private set(v) {
            _muted = v
            onChange?.invoke()
        }

    var active by mutableStateOf(false)
        private set
    var lang by mutableStateOf(keys.voiceLang)
        private set
    var partial by mutableStateOf("")
    var info by mutableStateOf("")
    var caption by mutableStateOf("")

    private val relisten = Runnable { listen() }

    // ---------- সর্বজনীন কমান্ড ----------

    fun start() {
        if (active) return
        active = true
        muted = false
        info = ""
        caption = ""
        partial = ""
        chat?.newChat()
        try {
            LiaService.start(ctx)
        } catch (e: Exception) {
            info = "Background service চালু করা যায়নি: ${e.message}"
        }
        initTts()
        listen()
    }

    fun stop() {
        active = false
        muted = false
        main.removeCallbacks(relisten)
        lastUtteranceId = ""
        tts?.stop()
        recognizer?.destroy()
        recognizer = null
        state = VoiceState.IDLE
        partial = ""
        info = ""
        LiaService.stop(ctx)
    }

    fun toggleMute() {
        muted = !muted
        if (muted) {
            main.removeCallbacks(relisten)
            recognizer?.cancel()
            if (state == VoiceState.LISTENING) state = VoiceState.IDLE
        } else if (state == VoiceState.IDLE) {
            listen()
        }
    }

    fun toggleLang() {
        lang = if (lang == "bn-BD") "en-US" else "bn-BD"
        keys.voiceLang = lang
        info = ""
        if (ttsReady) applyTtsLang()
        if (active && state == VoiceState.LISTENING) {
            recognizer?.cancel()
            listen()
        }
    }

    /** অরবে ট্যাপ: কথা বললে থামাও ও শোনো, চুপ থাকলে শোনা শুরু করো */
    fun tapOrb() {
        when (state) {
            VoiceState.SPEAKING -> {
                lastUtteranceId = ""
                tts?.stop()
                main.removeCallbacks(relisten)
                state = VoiceState.IDLE
                listen()
            }
            VoiceState.IDLE -> {
                if (muted) muted = false
                listen()
            }
            else -> {}
        }
    }

    // ---------- শোনা ----------

    private fun listen() {
        if (!active) return
        if (muted) {
            state = VoiceState.IDLE
            return
        }
        if (state == VoiceState.THINKING || state == VoiceState.SPEAKING) return
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            info = "Speech recognition নেই। Google app ইনস্টল/চালু আছে কিনা দেখো।"
            state = VoiceState.IDLE
            return
        }
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(ctx).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        partial = ""
        state = VoiceState.LISTENING
        r.startListening(intent)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val p = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!p.isNullOrBlank()) partial = p
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.trim().orEmpty()
            errorStreak = 0
            if (text.isEmpty()) {
                relistenSoon(250)
                return
            }
            partial = text
            handleUtterance(text)
        }

        override fun onError(error: Int) {
            if (!active || muted || state != VoiceState.LISTENING) return
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> relistenSoon(250)

                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    info = "Microphone permission দরকার।"
                    state = VoiceState.IDLE
                }

                else -> {
                    errorStreak++
                    if (errorStreak > 5) {
                        info = "Speech error ($error)। ইন্টারনেট ও Google app দেখো।"
                        state = VoiceState.IDLE
                        return
                    }
                    recognizer?.destroy()
                    recognizer = null
                    relistenSoon(800)
                }
            }
        }
    }

    private fun relistenSoon(delayMs: Long) {
        main.removeCallbacks(relisten)
        main.postDelayed(relisten, delayMs)
    }

    // ---------- AI-কে পাঠানো ----------

    private fun handleUtterance(text: String) {
        state = VoiceState.THINKING
        info = ""
        val c = chat
        if (c == null) {
            state = VoiceState.IDLE
            relistenSoon(300)
            return
        }
        c.send(text, voice = true) { reply ->
            if (!active) return@send
            if (reply == null) {
                info = "উত্তর আনা যায়নি। Connections-এ key ও ইন্টারনেট দেখো।"
                state = VoiceState.IDLE
                relistenSoon(1500)
            } else {
                speak(reply)
            }
        }
    }

    // ---------- বলা (TTS) ----------

    private fun initTts() {
        if (tts != null) return
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                applyTtsLang()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        main.post { if (utteranceId == lastUtteranceId) afterSpeak() }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        main.post { if (utteranceId == lastUtteranceId) afterSpeak() }
                    }
                })
                pendingSpeak?.let { pendingSpeak = null; speak(it) }
            } else {
                info = "Text-to-speech চালু করা যায়নি।"
                if (pendingSpeak != null) {
                    pendingSpeak = null
                    afterSpeak()
                }
            }
        }
    }

    private fun applyTtsLang() {
        val t = tts ?: return
        val loc = if (lang == "en-US") Locale.US else Locale("bn", "BD")
        val r = t.setLanguage(loc)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            info = "এই ভাষার ভয়েস নেই। ফোনের Settings → Text-to-speech-এ Google ভয়েস ইনস্টল করো।"
        }
    }

    private fun speak(raw: String) {
        val t = tts
        if (t == null || !ttsReady) {
            pendingSpeak = raw
            initTts()
            return
        }
        val clean = raw
            .replace(Regex("[*_`#>~]"), "")
            .replace(Regex("(?m)^[ \\t]*[-•] "), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (clean.isEmpty()) {
            afterSpeak()
            return
        }

        val parts = clean.split(Regex("(?<=[.!?।])\\s+")).filter { it.isNotBlank() }
        val chunks = mutableListOf<String>()
        var cur = ""
        for (p in parts) {
            if (cur.isNotEmpty() && cur.length + p.length > 400) {
                chunks.add(cur)
                cur = p
            } else {
                cur = if (cur.isEmpty()) p else "$cur $p"
            }
        }
        if (cur.isNotEmpty()) chunks.add(cur)
        val finalChunks = chunks.flatMap { it.chunked(3000) }

        caption = clean.take(160)
        state = VoiceState.SPEAKING
        val base = "lia_${System.nanoTime()}"
        lastUtteranceId = "$base-${finalChunks.size - 1}"
        finalChunks.forEachIndexed { i, c ->
            t.speak(
                c,
                if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "$base-$i"
            )
        }
    }

    private fun afterSpeak() {
        if (!active) return
        state = VoiceState.IDLE
        relistenSoon(350)
    }

    override fun onCleared() {
        main.removeCallbacks(relisten)
        recognizer?.destroy()
        tts?.shutdown()
        super.onCleared()
    }
}
