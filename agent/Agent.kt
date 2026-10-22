package com.lia.assistant.agent

import org.json.JSONObject

data class AgentAction(val type: String, val args: JSONObject) {
    fun str(key: String): String =
        if (args.isNull(key)) "" else args.optString(key, "").trim()

    /** কল ও মেসেজের আগে অনুমতি চাওয়া হবে */
    val sensitive: Boolean get() = type == "call" || type == "whatsapp"
}

data class AgentReply(val say: String, val actions: List<AgentAction>, val confirm: String)

object Agent {
    private val LEAVES_APP = setOf(
        "open_app", "youtube_play", "open_url", "web_search", "call", "whatsapp", "lock_screen"
    )

    /** এই কাজগুলোর পর ভয়েসে "Lia" বলে ডাকতে হবে (গানের শব্দে যেন ভুল কমান্ড না যায়) */
    fun leavesApp(actions: List<AgentAction>) = actions.any { it.type in LEAVES_APP }

    fun parse(raw: String): AgentReply {
        var s = raw.trim()
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start < 0 || end <= start) return AgentReply(raw.trim(), emptyList(), "")
        return try {
            val o = JSONObject(s.substring(start, end + 1))
            val say = o.optString("say", "").trim()
            val arr = o.optJSONArray("actions")
            val list = mutableListOf<AgentAction>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val a = arr.optJSONObject(i) ?: continue
                    val t = a.optString("type", "").trim()
                    if (t.isNotEmpty()) list.add(AgentAction(t, a))
                }
            }
            val confirm = if (o.isNull("confirm")) "" else o.optString("confirm", "").trim()
            AgentReply(
                say.ifEmpty { if (list.isEmpty()) raw.trim() else "" },
                list.take(5),
                confirm
            )
        } catch (e: Exception) {
            AgentReply(raw.trim(), emptyList(), "")
        }
    }

    private val YES = setOf(
        "yes", "yeah", "yep", "yup", "ok", "okay", "sure", "confirm", "haan", "han", "ha",
        "হ্যাঁ", "হ্যা", "হাঁ", "হা", "জি", "ঠিক", "করো", "কর", "পাঠাও", "দাও"
    )
    private val NO = setOf(
        "no", "nope", "nah", "cancel", "stop", "dont",
        "না", "নাহ", "থাক", "থাকুক", "বাদ", "বাতিল", "নয়", "নেই"
    )

    /** true = হ্যাঁ, false = না, null = বোঝা যায়নি (নতুন কমান্ড) */
    fun yesNo(text: String): Boolean? {
        val t = text.lowercase().replace(Regex("[^\\p{L}\\p{M}\\p{N}\\s]"), " ").trim()
        if (t.isEmpty()) return null
        val words = t.split(Regex("\\s+"))
        if (words.size > 6) return null
        if (words.any { it in NO } || t.contains("করো না") || t.contains("করবে না")) return false
        if (words.any { it in YES }) return true
        return null
    }
}
