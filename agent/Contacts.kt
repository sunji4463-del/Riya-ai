package com.lia.assistant.agent

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object Contacts {
    data class Hit(val name: String, val number: String)

    fun hasPermission(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    private fun norm(s: String) = s.lowercase().replace(Regex("[^\\p{L}\\p{M}\\p{N}]"), "")

    /** একাধিক নামের (বাংলা, ইংরেজি বানান) যেকোনো একটা মিললে সেই কন্টাক্ট */
    fun find(ctx: Context, vararg names: String): Hit? {
        val wanted = names.map { norm(it) }.filter { it.isNotEmpty() }
        if (wanted.isEmpty() || !hasPermission(ctx)) return null

        val rows = mutableListOf<Hit>()
        ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                val n = c.getString(0) ?: continue
                val p = c.getString(1) ?: continue
                rows.add(Hit(n, p))
            }
        }

        for (w in wanted) rows.firstOrNull { norm(it.name) == w }?.let { return it }
        for (w in wanted) rows.firstOrNull { norm(it.name).startsWith(w) }?.let { return it }
        for (w in wanted) rows.firstOrNull { norm(it.name).contains(w) }?.let { return it }
        return null
    }
}
