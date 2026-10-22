package com.lia.assistant.agent

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.lia.assistant.LiaAccessibilityService
import com.lia.assistant.PhoneControl
import com.lia.assistant.data.KeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object ActionRunner {
    private const val YT = "com.google.android.youtube"

    /** সব কাজ পরপর চালায়। ব্যর্থ কাজের বার্তা ফেরত দেয় (সব ঠিক থাকলে খালি লিস্ট) */
    suspend fun run(ctx: Context, actions: List<AgentAction>): List<String> {
        val errs = mutableListOf<String>()
        for (a in actions) {
            try {
                runOne(ctx, a)?.let { errs.add(it) }
            } catch (e: Exception) {
                errs.add("${a.type}: ${e.message}")
            }
            delay(500)
        }
        return errs
    }

    private suspend fun runOne(ctx: Context, a: AgentAction): String? {
        return when (a.type) {
            "open_app" -> openApp(ctx, a)
            "call" -> call(ctx, a)
            "whatsapp" -> whatsapp(ctx, a)
            "youtube_play" -> youtube(ctx, a)
            "open_url" -> openUrl(ctx, a)
            "web_search" -> webSearch(ctx, a)
            "lock_screen" -> lockScreen()
            "press" -> press(a)
            "scroll" -> scroll(a)
            "flashlight" -> flashlight(ctx, a)
            "volume" -> volume(ctx, a)
            else -> "এই কাজটা আমি পারি না: ${a.type}"
        }
    }

    // ---------- অ্যাপ, লিংক ----------

    private fun openApp(ctx: Context, a: AgentAction): String? {
        val (ok, msg) = PhoneControl.openAppResult(ctx, a.str("app"))
        return if (ok) null else msg
    }

    private fun openUrl(ctx: Context, a: AgentAction): String? {
        val url = a.str("url")
        if (!(url.startsWith("https://") || url.startsWith("http://"))) {
            return "শুধু http/https লিংক খুলতে পারি।"
        }
        PhoneControl.launch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        return null
    }

    private fun webSearch(ctx: Context, a: AgentAction): String? {
        val q = a.str("query")
        if (q.isEmpty()) return "কী সার্চ করব বুঝিনি।"
        PhoneControl.launch(
            ctx,
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q)))
        )
        return null
    }

    // ---------- কন্টাক্ট ও নম্বর ----------

    private data class Target(val name: String, val number: String)

    private fun ascii(s: String) = buildString {
        s.forEach { c -> append(if (c in '০'..'৯') '0' + (c - '০') else c) }
    }

    private val numberRegex = Regex("^\\+?[\\d\\s\\-()]{5,}$")

    private suspend fun resolve(ctx: Context, a: AgentAction): Target? {
        val to = a.str("to")
        if (to.isEmpty()) return null
        val digits = ascii(to).trim()
        if (numberRegex.matches(digits)) {
            return Target(to, digits.filter { it.isDigit() || it == '+' })
        }
        val hit = withContext(Dispatchers.IO) { Contacts.find(ctx, to, a.str("to_en")) }
            ?: return null
        return Target(hit.name, hit.number)
    }

    private fun notFound(ctx: Context, a: AgentAction): String =
        if (!Contacts.hasPermission(ctx))
            "Contacts permission নেই। Settings → Phone control থেকে দাও।"
        else
            "\"${a.str("to")}\" নামে কোনো কন্টাক্ট পাইনি।"

    // ---------- কল ----------

    private suspend fun call(ctx: Context, a: AgentAction): String? {
        val t = resolve(ctx, a) ?: return notFound(ctx, a)
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        val uri = Uri.parse("tel:" + Uri.encode(t.number))
        PhoneControl.launch(ctx, Intent(if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri))
        return if (granted) null else "Call permission নেই, তাই শুধু ডায়ালার খুলেছি। Phone control থেকে permission দাও।"
    }

    // ---------- WhatsApp ----------

    private fun waNumber(raw: String, cc: String): String {
        val s = raw.filter { it.isDigit() || it == '+' }
        if (s.startsWith("+")) return s.drop(1)
        if (s.startsWith("00")) return s.drop(2)
        if (s.startsWith("0")) return cc + s.drop(1)
        return if (s.startsWith(cc)) s else cc + s
    }

    private fun pickWhatsApp(ctx: Context, business: Boolean): String? {
        val pm = ctx.packageManager
        val order = if (business) listOf("com.whatsapp.w4b", "com.whatsapp")
        else listOf("com.whatsapp", "com.whatsapp.w4b")
        return order.firstOrNull { pm.getLaunchIntentForPackage(it) != null }
    }

    private suspend fun whatsapp(ctx: Context, a: AgentAction): String? {
        val text = a.str("text")
        if (text.isEmpty()) return "মেসেজের লেখা খালি।"
        val t = resolve(ctx, a) ?: return notFound(ctx, a)
        val pkg = pickWhatsApp(ctx, a.args.optBoolean("business", false))
            ?: return "WhatsApp ইনস্টল নেই।"
        val num = waNumber(t.number, KeyStore(ctx).countryCode)

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/$num?text=" + Uri.encode(text))
        ).setPackage(pkg)
        PhoneControl.launch(ctx, intent)

        val svc = LiaAccessibilityService.instance
            ?: return "মেসেজ লিখে রেখেছি। Accessibility বন্ধ, তাই Send নিজে চাপো।"

        repeat(14) {
            delay(700)
            if (LiaAccessibilityService.currentPackage.startsWith("com.whatsapp") &&
                svc.clickWhatsAppSend()
            ) return null
        }
        return "Send বাটন পাইনি। মেসেজ লেখা আছে, নিজে Send চাপো।"
    }

    // ---------- YouTube ----------

    private suspend fun youtube(ctx: Context, a: AgentAction): String? {
        val q = a.str("query")
        if (q.isEmpty()) return "কী চালাব বুঝিনি।"
        val hasApp = ctx.packageManager.getLaunchIntentForPackage(YT) != null
        val i = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(q))
        )
        if (hasApp) i.setPackage(YT)
        PhoneControl.launch(ctx, i)

        val svc = LiaAccessibilityService.instance
            ?: return "YouTube-এ সার্চ খুলেছি। নিজে চালাতে হবে (Accessibility বন্ধ)।"
        if (!hasApp) return "YouTube অ্যাপ নেই, ব্রাউজারে সার্চ খুলেছি।"

        delay(3500)
        repeat(8) {
            if (LiaAccessibilityService.currentPackage == YT && svc.clickFirstVideo(false)) return null
            delay(700)
        }
        if (LiaAccessibilityService.currentPackage == YT && svc.clickFirstVideo(true)) return null
        return "সার্চ খুলেছি, ভিডিওটা নিজে বেছে নাও।"
    }

    // ---------- সিস্টেম ----------

    private suspend fun lockScreen(): String? {
        val s = LiaAccessibilityService.instance ?: return "Accessibility চালু নেই।"
        delay(1200) // যাতে Lia কথাটা বলা শুরু করতে পারে
        return if (s.lockScreen()) null else "স্ক্রিন লক করা যায়নি (Android 9+ লাগে)।"
    }

    private fun press(a: AgentAction): String? {
        val s = LiaAccessibilityService.instance ?: return "Accessibility চালু নেই।"
        val key = a.str("key").lowercase()
        val ok = when (key) {
            "home" -> s.home()
            "back" -> s.back()
            "recents" -> s.recents()
            "notifications" -> s.notifications()
            "quick_settings" -> s.quickSettings()
            "screenshot" -> s.screenshot()
            "power_menu" -> s.powerMenu()
            else -> return "এই বাটন চিনি না: $key"
        }
        return if (ok) null else "কাজটা হয়নি।"
    }

    private fun scroll(a: AgentAction): String? {
        val s = LiaAccessibilityService.instance ?: return "Accessibility চালু নেই।"
        val down = a.str("direction").lowercase() != "up"
        return if (s.scroll(down)) null else "স্ক্রল করার মতো কিছু পাইনি।"
    }

    private fun flashlight(ctx: Context, a: AgentAction): String? {
        val on = a.args.optBoolean("on", true)
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return "এই ফোনে ফ্ল্যাশলাইট পাইনি।"
        cm.setTorchMode(id, on)
        return null
    }

    private fun volume(ctx: Context, a: AgentAction): String? {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val dir = when (a.str("direction").lowercase()) {
            "up" -> AudioManager.ADJUST_RAISE
            "down" -> AudioManager.ADJUST_LOWER
            "mute" -> AudioManager.ADJUST_MUTE
            else -> return "ভলিউম কীভাবে বদলাব বুঝিনি।"
        }
        repeat(if (dir == AudioManager.ADJUST_MUTE) 1 else 3) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
        }
        return null
    }
}
