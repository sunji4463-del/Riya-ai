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

    val chats = mutableStateListOf<Chat>()      // সেভ করা সব চ্যাট (নতুন আগে)
    val messages = mutableStateListOf<Msg>()    // এখন খোলা চ্যাট
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

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || loading) return
        messages.add(Msg(t, true))
        persist()
        loading = true
        job = viewModelScope.launch {
            try {
                val history = messages.filter { !it.error }.takeLast(20)
                val reply = AiClient.reply(history, keys.groqKeyList(), keys.geminiKey)
                messages.add(Msg(reply, false))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                messages.add(Msg("⚠️ ${e.message}", false, error = true))
            }
            loading = false
            persist()
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
