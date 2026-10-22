package com.lia.assistant.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lia.assistant.agent.ActionRunner
import com.lia.assistant.agent.Agent
import com.lia.assistant.agent.AgentAction
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

    /** "হ্যাঁ/না" এর অপেক্ষায় থাকা কাজ */
    private var pending: List<AgentAction>? = null

    /** অ্যাপ ছেড়ে যাওয়ার মতো কাজ শুরু হলে ভয়েসকে জানানো হয় */
    var onActionsExecuted: (() -> Unit)? = null

    val chats = mutableStateListOf<Chat>()
    val messages = mutableStateListOf<Msg>()
    var loading by mutableStateOf(false)
        private set

    init {
        chats.addAll(store.loadAll())
    }

    private fun tr(bn: String, en: String) = if (keys.voiceLang == "en-US") en else bn

    fun newChat() {
        job?.cancel()
        loading = false
        currentId = null
        pending = null
        messages.clear()
    }

    fun openChat(id: String) {
        val c = chats.firstOrNull { it.id == id } ?: return
        job?.cancel()
        loading = false
        currentId = id
        pending = null
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
     * onResult: সফল হলে Lia যা বলবে তার লেখা, ব্যর্থ হলে null।
     */
    fun send(text: String, voice: Boolean = false, onResult: ((String?) -> Unit)? = null) {
        val t = text.trim()
        if (t.isEmpty() || loading) {
            onResult?.invoke(null)
            return
        }
        messages.add(Msg(t, true))
        persist()

        // আগের কল/মেসেজের "হ্যাঁ বা না" উত্তর
        val p = pending
        if (p != null) {
            pending = null
            when (Agent.yesNo(t)) {
                true -> {
                    val say = tr("ঠিক আছে, করছি।", "Okay, doing it.")
                    messages.add(Msg(say, false))
                    persist()
                    execute(p)
                    onResult?.invoke(say)
                    return
                }
                false -> {
                    val say = tr("ঠিক আছে, বাদ দিলাম।", "Okay, cancelled.")
                    messages.add(Msg(say, false))
                    persist()
                    onResult?.invoke(say)
                    return
                }
                null -> {}
            }
        }

        loading = true
        job = viewModelScope.launch {
            var result: String? = null
            try {
                val history = messages.filter { !it.error }.takeLast(20)
                val raw = AiClient.reply(history, keys.groqKeyList(), keys.geminiKey, voice)
                val r = Agent.parse(raw)

                val needConfirm = keys.confirmActions && r.actions.any { it.sensitive }
                val say = when {
                    needConfirm -> r.confirm.ifEmpty {
                        r.say + " " + tr("হ্যাঁ বা না বলো।", "Say yes or no.")
                    }
                    else -> r.say
                }.ifEmpty { tr("ঠিক আছে।", "Okay.") }

                messages.add(Msg(say, false))
                result = say

                if (r.actions.isNotEmpty()) {
                    if (needConfirm) pending = r.actions else execute(r.actions)
                }
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

    private fun execute(actions: List<AgentAction>) {
        if (Agent.leavesApp(actions)) onActionsExecuted?.invoke()
        viewModelScope.launch {
            val errs = ActionRunner.run(getApplication<Application>(), actions)
            if (errs.isNotEmpty()) {
                messages.add(Msg("⚠️ " + errs.joinToString("\n"), false, error = true))
            }
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
