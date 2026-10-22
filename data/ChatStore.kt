package com.lia.assistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChatStore(context: Context) {
    private val file = File(context.filesDir, "chats.json")

    fun loadAll(): List<Chat> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val m = o.getJSONArray("messages")
                Chat(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    updatedAt = o.getLong("updatedAt"),
                    messages = (0 until m.length()).map { j ->
                        val x = m.getJSONObject(j)
                        Msg(x.getString("text"), x.getBoolean("user"))
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAll(chats: List<Chat>) {
        val arr = JSONArray()
        chats.forEach { c ->
            val ms = JSONArray()
            c.messages.forEach {
                ms.put(JSONObject().put("text", it.text).put("user", it.fromUser))
            }
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("title", c.title)
                    .put("updatedAt", c.updatedAt)
                    .put("messages", ms)
            )
        }
        file.writeText(arr.toString())
    }
}
