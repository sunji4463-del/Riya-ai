package com.lia.assistant.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiClient {
    const val GROQ_MODEL = "llama-3.3-70b-versatile"
    const val GEMINI_MODEL = "gemini-2.5-flash"

    private val AGENT_SYSTEM = """
You are Lia, a friendly personal AI assistant inside an Android phone app. You can chat AND control the phone.

Always reply with exactly ONE JSON object and nothing else:
{"say": "...", "actions": [], "confirm": ""}

- "say": what Lia tells the user. Use the same language as the user (if the user writes Bengali or romanized Banglish, answer in Bengali script). Keep it short and natural unless the user asks for detail.
- "actions": phone actions to perform. Use [] for normal chat and questions.
- "confirm": only when actions contain a call or whatsapp action: one short yes/no question in the user's language that names the person (and the message for whatsapp). Otherwise "".

Available actions (use these exact types):
{"type":"open_app","app":"<app name in English, as shown on the phone>"}
{"type":"call","to":"<contact name or phone number>","to_en":"<same person name in English letters>"}
{"type":"whatsapp","to":"<contact name or phone number>","to_en":"<same name in English letters>","text":"<message to send>","business":false}
{"type":"youtube_play","query":"<what to search and play on YouTube>"}
{"type":"open_url","url":"<full https link>"}
{"type":"web_search","query":"<search text>"}
{"type":"lock_screen"}
{"type":"press","key":"home|back|recents|notifications|quick_settings|screenshot|power_menu"}
{"type":"scroll","direction":"down|up"}
{"type":"flashlight","on":true}
{"type":"volume","direction":"up|down|mute"}

Rules:
- Only add actions the user clearly asked for. Never invent other action types or contact details.
- If the user says to turn off or switch off the phone or screen, use lock_screen (an app cannot power the phone off). Say this politely in "say".
- Convert app names written in Bengali or Banglish into the real app name in English (for example ফেসবুক becomes Facebook).
- Keep contact names exactly as the user said them. Omit to_en when "to" is a phone number.
- The whatsapp text must be exactly what the user wants to send, in the language they dictated. Do not add anything.
- When you add actions, "say" is a short line about what you are doing.
- You cannot read the screen, send SMS, install apps or change phone settings yet. Say so politely if asked.
""".trimIndent()

    private const val VOICE_HINT =
        "\nThe user is talking to you by voice, so keep \"say\" to 1 to 3 short natural spoken " +
        "sentences with no markdown, lists, emojis or symbols."

    private var rr = 0

    suspend fun reply(
        history: List<Msg>,
        groqKeys: List<String>,
        geminiKey: String,
        voice: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val system = if (voice) AGENT_SYSTEM + VOICE_HINT else AGENT_SYSTEM
        var lastErr = "কোনো API key নেই। Settings → Connections-এ key দাও।"

        for (i in groqKeys.indices) {
            val key = groqKeys[(rr + i) % groqKeys.size]
            try {
                val out = groq(history, key, system)
                rr = (rr + i + 1) % groqKeys.size
                return@withContext out
            } catch (e: Exception) {
                lastErr = "Groq: ${e.message}"
            }
        }
        if (geminiKey.isNotEmpty()) {
            try {
                return@withContext gemini(history, geminiKey, system)
            } catch (e: Exception) {
                lastErr = "Gemini: ${e.message}"
            }
        }
        throw Exception(lastErr)
    }

    private fun groq(history: List<Msg>, key: String, system: String): String {
        val msgs = JSONArray().put(JSONObject().put("role", "system").put("content", system))
        history.forEach {
            msgs.put(
                JSONObject()
                    .put("role", if (it.fromUser) "user" else "assistant")
                    .put("content", it.text)
            )
        }
        val body = JSONObject()
            .put("model", GROQ_MODEL)
            .put("messages", msgs)
            .put("temperature", 0.4)
            .put("response_format", JSONObject().put("type", "json_object"))
            .toString()
        val res = post(
            "https://api.groq.com/openai/v1/chat/completions",
            body,
            mapOf("Authorization" to "Bearer $key")
        )
        return JSONObject(res).getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content").trim()
    }

    private fun gemini(history: List<Msg>, key: String, system: String): String {
        val contents = JSONArray()
        history.forEach {
            contents.put(
                JSONObject()
                    .put("role", if (it.fromUser) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", it.text)))
            )
        }
        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system)))
            )
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            .toString()
        val res = post(
            "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent",
            body,
            mapOf("x-goog-api-key" to key)
        )
        return JSONObject(res).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
            .getString("text").trim()
    }

    private fun post(url: String, body: String, headers: Map<String, String>): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 15000
        c.readTimeout = 60000
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        c.outputStream.use { it.write(body.toByteArray()) }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw Exception("HTTP $code ${text.take(160)}")
        return text
    }
}
