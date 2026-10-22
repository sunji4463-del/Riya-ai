package com.lia.assistant.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiClient {
    // মডেলের নাম ভবিষ্যতে বদলাতে চাইলে শুধু এখানে বদলাও
    const val GROQ_MODEL = "llama-3.3-70b-versatile"
    const val GEMINI_MODEL = "gemini-2.5-flash"

    private const val SYSTEM =
        "You are Lia, a friendly, smart personal AI assistant living on the user's phone. " +
        "Always reply in the same language the user writes. If the user writes Bengali " +
        "(Bengali script or romanized Banglish), reply in Bengali script. " +
        "Keep replies short and clear unless the user asks for detail."

    private var rr = 0 // Groq key গুলো পালা করে ব্যবহারের জন্য

    suspend fun reply(
        history: List<Msg>,
        groqKeys: List<String>,
        geminiKey: String
    ): String = withContext(Dispatchers.IO) {
        var lastErr = "কোনো API key নেই। Settings → Connections-এ key দাও।"

        for (i in groqKeys.indices) {
            val key = groqKeys[(rr + i) % groqKeys.size]
            try {
                val out = groq(history, key)
                rr = (rr + i + 1) % groqKeys.size
                return@withContext out
            } catch (e: Exception) {
                lastErr = "Groq: ${e.message}"
            }
        }
        if (geminiKey.isNotEmpty()) {
            try {
                return@withContext gemini(history, geminiKey)
            } catch (e: Exception) {
                lastErr = "Gemini: ${e.message}"
            }
        }
        throw Exception(lastErr)
    }

    private fun groq(history: List<Msg>, key: String): String {
        val msgs = JSONArray().put(JSONObject().put("role", "system").put("content", SYSTEM))
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
            .put("temperature", 0.7)
            .toString()
        val res = post(
            "https://api.groq.com/openai/v1/chat/completions",
            body,
            mapOf("Authorization" to "Bearer $key")
        )
        return JSONObject(res).getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content").trim()
    }

    private fun gemini(history: List<Msg>, key: String): String {
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
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM)))
            )
            .put("contents", contents)
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
