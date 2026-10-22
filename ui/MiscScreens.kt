package com.lia.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lia.assistant.ui.theme.LiaColors

@Composable
fun HistoryScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("History", fontSize = 28.sp, color = LiaColors.TextMain)
        Spacer(Modifier.height(24.dp))
        Text("No chats yet. They will appear here from Phase 2.", color = LiaColors.TextSub)
    }
}

@Composable
fun VoiceScreen(onEnd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Lia", fontFamily = FontFamily.Cursive, fontSize = 30.sp, color = LiaColors.PurpleLight)
        Spacer(Modifier.weight(1f))
        GlowOrb(280.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            "Lia is listening...", fontSize = 24.sp, fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Serif, color = LiaColors.TextMain
        )
        Text("Voice will be enabled in Phase 3", fontSize = 12.sp, color = LiaColors.TextSub)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFEF4444)).clickable { onEnd() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CallEnd, "End", tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}
