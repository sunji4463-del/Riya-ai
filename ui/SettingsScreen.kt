package com.lia.assistant.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lia.assistant.data.KeyStore
import com.lia.assistant.ui.theme.LiaColors

@Composable
fun SettingsScreen(onConnections: () -> Unit, onPhone: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { KeyStore(ctx) }
    val ready = store.geminiKey.isNotEmpty() && store.groqKeyList().isNotEmpty()
    fun soon() = Toast.makeText(ctx, "Coming soon", Toast.LENGTH_SHORT).show()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = LiaColors.TextMain)
        Text("Customize your AI experience", fontSize = 13.sp, color = LiaColors.TextSub)
        Spacer(Modifier.height(16.dp))

        LiaCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = LiaColors.PurpleLight)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("LIA AI", fontWeight = FontWeight.Bold, color = LiaColors.TextMain)
                    Text("Your Personal AI Assistant  v1.0", fontSize = 12.sp, color = LiaColors.TextSub)
                }
            }
        }

        SectionTitle("AI SETTINGS")
        SettingRow(
            Icons.Rounded.Key, "Connections", "Gemini and Groq keys",
            if (ready) "Ready" else "Setup", onConnections
        )
        SettingRow(Icons.Rounded.RecordVoiceOver, "Voice & language", "Voice, speed, language") { soon() }
        SettingRow(Icons.Rounded.Forum, "Chats & memory", "Saved chats") { soon() }

        SectionTitle("APP SETTINGS")
        SettingRow(Icons.Rounded.Palette, "Look & feel", "Dark - Classic orb") { soon() }
        SettingRow(
            Icons.Rounded.PhoneAndroid, "Phone control", "Accessibility, permissions, test actions",
            onClick = onPhone
        )
        SettingRow(
            Icons.Rounded.BatteryChargingFull,
            "Background voice",
            "Set LIA to unrestricted battery"
        ) {
            ctx.startActivity(
                Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            )
        }

        SectionTitle("ABOUT")
        SettingRow(Icons.Rounded.Shield, "Privacy & about", "Your data, version, support") { soon() }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(
        t, fontSize = 12.sp, color = LiaColors.PurpleLight, letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector, title: String, sub: String,
    badge: String? = null, onClick: () -> Unit
) {
    LiaCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(LiaColors.Purple.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = LiaColors.PurpleLight) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = LiaColors.TextMain)
                Text(sub, fontSize = 12.sp, color = LiaColors.TextSub)
            }
            if (badge != null) {
                val c = if (badge == "Ready") LiaColors.Green else LiaColors.Gold
                Text(
                    badge, color = c, fontSize = 11.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(c.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
