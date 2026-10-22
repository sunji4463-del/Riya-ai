package com.lia.assistant.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lia.assistant.data.AiClient
import com.lia.assistant.data.Chat
import com.lia.assistant.data.ChatStore
import com.lia.assistant.data.KeyStore
import com.lia.assistant.data.Msg
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val keys = KeyStore(app)
    private val store = ChatStore(app)
    private var job: Job? = null
    private var currentId: String? = null

    val chats = mutableStateListOf<Chat>()
    val messages = mutableStateListOf<Msg>()
    var loading by mutableStateOf(false)
        private set

    init {
        chats.addAll(store.loadAll())
    }

    fun newChat() {
        job?.cancel()
        loading = false
        currentId = null
        messages.clear()
    }

    fun openChat(id: String) {
        val c = chats.firstOrNull { it.id == id } ?: return
        job?.cancel()
        loading = false
        currentId = id
        messages.clear()
        messages.addAll(c.messages)
    }

    fun deleteChat(id: String) {
        chats.removeAll { it.id == id }
        store.saveAll(chats.toList())
        if (currentId == id) newChat()
    }

    /**
     * voice = true হলে উত্তর ছোট ও মুখে বলার মতো হয়।
     * onResult: সফল হলে উত্তরের লেখা, ব্যর্থ হলে null।
     */
    fun send(text: String, voice: Boolean = false, onResult: ((String?) -> Unit)? = null) {
        val t = text.trim()
        if (t.isEmpty() || loading) {
            onResult?.invoke(null)
            return
        }
        messages.add(Msg(t, true))
        persist()
        loading = true
        job = viewModelScope.launch {
            var result: String? = null
            try {
                val history = messages.filter { !it.error }.takeLast(20)
                val reply = AiClient.reply(history, keys.groqKeyList(), keys.geminiKey, voice)
                messages.add(Msg(reply, false))
                result = reply
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                messages.add(Msg("⚠️ ${e.message}", false, error = true))
            }
            loading = false
            persist()
            onResult?.invoke(result)
        }
    }

    private fun persist() {
        val saved = messages.filter { !it.error }
        if (saved.isEmpty()) return
        val id = currentId ?: UUID.randomUUID().toString().also { currentId = it }
        val title = saved.first().text.replace("\n", " ").take(40)
        chats.removeAll { it.id == id }
        chats.add(0, Chat(id, title, System.currentTimeMillis(), saved))
        store.saveAll(chats.take(100))
    }
}
