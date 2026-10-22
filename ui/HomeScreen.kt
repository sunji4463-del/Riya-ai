package com.lia.assistant.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lia.assistant.ui.theme.LiaColors

@Composable
fun HomeScreen(onVoice: () -> Unit, onChat: () -> Unit, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    fun soon() = Toast.makeText(ctx, "Coming soon", Toast.LENGTH_SHORT).show()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { soon() }) {
                Icon(Icons.Rounded.Menu, "Menu", tint = LiaColors.TextMain)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "LIA AI", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    color = LiaColors.TextMain, letterSpacing = 4.sp
                )
                Text("YOUR AI ASSISTANT", fontSize = 9.sp, color = LiaColors.PurpleLight, letterSpacing = 3.sp)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Settings, "Settings", tint = LiaColors.TextMain)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Hi, I'm LIA", fontFamily = FontFamily.Cursive, fontSize = 38.sp, color = LiaColors.PurpleLight)
        Text("Always here for you", fontSize = 13.sp, color = LiaColors.TextSub)
        Spacer(Modifier.height(8.dp))

        GlowOrb(230.dp, onClick = onVoice)
        Text("Tap to Talk", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = LiaColors.TextMain)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = liaFieldColors(),
            placeholder = { Text("Ask me anything...", color = LiaColors.TextSub) },
            trailingIcon = {
                IconButton(onClick = {
                    if (query.isNotBlank()) {
                        PendingQuery.text = query.trim()
                        query = ""
                        onChat()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Rounded.Send, "Send", tint = LiaColors.PurpleLight)
                }
            }
        )

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickCard(Icons.Rounded.ChatBubbleOutline, "Chat", "Get instant answers", Modifier.weight(1f)) { onChat() }
            QuickCard(Icons.Rounded.Image, "Create Image", "Photos into sketches", Modifier.weight(1f)) { soon() }
            QuickCard(Icons.Rounded.Edit, "Write", "Notes, code, content", Modifier.weight(1f)) { soon() }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun QuickCard(icon: ImageVector, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    LiaCard(modifier.height(92.dp), onClick = onClick) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, title, tint = LiaColors.PurpleLight, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LiaColors.TextMain)
            Text(sub, fontSize = 9.sp, color = LiaColors.TextSub, textAlign = TextAlign.Center)
        }
    }
}
