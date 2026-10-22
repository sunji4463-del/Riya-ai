package com.lia.assistant

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class AppItem(val label: String, val pkg: String)

object PhoneControl {
    /** টেস্ট স্ক্রিনে ফলাফল দেখানোর জন্য */
    var log by mutableStateOf("")

    fun accessibilityEnabled(ctx: Context): Boolean {
        val cn = ComponentName(ctx, LiaAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any {
            it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
        }
    }

    fun openAccessibilitySettings(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openOverlaySettings(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openAppInfo(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---------- অ্যাপ লিস্ট ও খোলা ----------

    fun apps(ctx: Context): List<AppItem> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = ctx.packageManager
        return pm.queryIntentActivities(intent, 0)
            .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    private fun norm(s: String) = s.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")

    /** নাম (যেমন "youtube", "whatsapp") দিয়ে অ্যাপ খোলে। ফলাফলের বার্তা ফেরত দেয় */
    fun openApp(ctx: Context, name: String): String {
        val q = norm(name)
        if (q.isEmpty()) return "অ্যাপের নাম দাও।"
        val all = apps(ctx)
        val hit = all.firstOrNull { norm(it.label) == q }
            ?: all.firstOrNull { norm(it.label).startsWith(q) }
            ?: all.firstOrNull { norm(it.label).contains(q) }
            ?: all.firstOrNull { norm(it.label).length >= 3 && q.contains(norm(it.label)) }
            ?: all.firstOrNull { it.pkg.contains(q, ignoreCase = true) }
            ?: return "\"$name\" নামে কোনো অ্যাপ পাইনি।"

        val intent = ctx.packageManager.getLaunchIntentForPackage(hit.pkg)
            ?: return "${hit.label} খোলা যায়নি।"
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            "${hit.label} খুলেছি।"
        } catch (e: Exception) {
            "${hit.label} খুলতে পারিনি: ${e.message}"
        }
    }
}
