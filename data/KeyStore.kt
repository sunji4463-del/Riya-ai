package com.lia.assistant.data

import android.content.Context

class KeyStore(context: Context) {
    private val sp = context.getSharedPreferences("lia_prefs", Context.MODE_PRIVATE)

    var geminiKey: String
        get() = sp.getString("gemini_key", "") ?: ""
        set(v) = sp.edit().putString("gemini_key", v.trim()).apply()

    var groqKeys: String
        get() = sp.getString("groq_keys", "") ?: ""
        set(v) = sp.edit().putString("groq_keys", v.trim()).apply()

    fun groqKeyList(): List<String> =
        groqKeys.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
