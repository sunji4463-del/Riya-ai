package com.lia.assistant.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lia.assistant.data.KeyStore
import com.lia.assistant.ui.theme.LiaColors

@Composable
fun ConnectionsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { KeyStore(ctx) }
    var gemini by remember { mutableStateOf(store.geminiKey) }
    var groq by remember { mutableStateOf(store.groqKeys) }
    var geminiReady by remember { mutableStateOf(store.geminiKey.isNotEmpty()) }
    var groqCount by remember { mutableStateOf(store.groqKeyList().size) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = LiaColors.TextMain)
            }
            Text("Connections", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = LiaColors.TextMain)
        }
        Text(
            "LIA talks and chats through your own keys. They are stored only on this phone.",
            color = LiaColors.TextSub, fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        KeyCard(
            title = "Voice - Gemini key",
            subtitle = "Powers live voice conversations.",
            status = if (geminiReady) "Ready" else null,
            value = gemini,
            onChange = { gemini = it },
            buttonText = "Save Gemini key",
            linkText = "Get a free key at Google AI Studio",
            url = "https://aistudio.google.com/app/apikey",
            onSave = {
                store.geminiKey = gemini
                geminiReady = store.geminiKey.isNotEmpty()
                Toast.makeText(ctx, "Gemini key saved", Toast.LENGTH_SHORT).show()
            }
        )
        Spacer(Modifier.height(14.dp))
        KeyCard(
            title = "Chat - Groq keys",
            subtitle = "Fast typed replies. Add several keys, separated by commas - they take turns if one is rate limited.",
            status = if (groqCount > 0) "$groqCount key ready" else null,
            value = groq,
            onChange = { groq = it },
            buttonText = "Save Groq keys",
            linkText = "Get a free key at Groq",
            url = "https://console.groq.com/keys",
            onSave = {
                store.groqKeys = groq
                groqCount = store.groqKeyList().size
                Toast.makeText(ctx, "Groq keys saved", Toast.LENGTH_SHORT).show()
            }
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun KeyCard(
    title: String, subtitle: String, status: String?,
    value: String, onChange: (String) -> Unit,
    buttonText: String, linkText: String, url: String, onSave: () -> Unit
) {
    val ctx = LocalContext.current
    LiaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, color = LiaColors.TextMain, modifier = Modifier.weight(1f))
                if (status != null) Text("✓ $status", color = LiaColors.Green, fontSize = 12.sp)
            }
            Text(subtitle, color = LiaColors.TextSub, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = liaFieldColors(),
                placeholder = { Text("Paste key here", color = LiaColors.TextSub) }
            )
            Spacer(Modifier.height(12.dp))
            GradientButton(buttonText) { onSave() }
            Spacer(Modifier.height(10.dp))
            Text(
                "$linkText ↗", color = LiaColors.PurpleLight, fontSize = 13.sp,
                modifier = Modifier.clickable {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            )
        }
    }
}
