package com.lia.assistant

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.lia.assistant.ui.ChatViewModel
import com.lia.assistant.ui.VoiceViewModel

class LiaApplication : Application(), ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val factory by lazy { ViewModelProvider.AndroidViewModelFactory.getInstance(this) }

    val chatVm: ChatViewModel by lazy {
        ViewModelProvider(this, factory)[ChatViewModel::class.java]
    }

    val voiceVm: VoiceViewModel by lazy {
        ViewModelProvider(this, factory)[VoiceViewModel::class.java].also { v ->
            v.chat = chatVm
            chatVm.onActionsExecuted = { v.enterWakeMode() }
        }
    }
}
