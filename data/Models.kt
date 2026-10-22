package com.lia.assistant.data

data class Msg(
    val text: String,
    val fromUser: Boolean,
    val error: Boolean = false
)

data class Chat(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<Msg>
)
