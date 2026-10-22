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

    /** "bn-BD" (বাংলা) অথবা "en-US" */
    var voiceLang: String
        get() = sp.getString("voice_lang", "bn-BD") ?: "bn-BD"
        set(v) = sp.edit().putString("voice_lang", v).apply()

    /** WhatsApp নম্বরের জন্য দেশের কোড (শুধু সংখ্যা)। ডিফল্ট 880 = বাংলাদেশ */
    var countryCode: String
        get() = sp.getString("country_code", "880") ?: "880"
        set(v) = sp.edit().putString("country_code", v.filter { it.isDigit() }).apply()

    /** কল ও WhatsApp এর আগে "হ্যাঁ/না" জিজ্ঞেস করবে কিনা */
    var confirmActions: Boolean
        get() = sp.getBoolean("confirm_actions", true)
        set(v) = sp.edit().putBoolean("confirm_actions", v).apply()

    fun groqKeyList(): List<String> =
        groqKeys.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
