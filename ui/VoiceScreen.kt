package com.lia.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lia.assistant.ui.theme.LiaColors

@Composable
fun VoiceScreen(voice: VoiceViewModel, onMinimize: () -> Unit, onEnd: () -> Unit) {
    val ctx = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        if (ok) voice.start() else voice.info = "Lia-র সাথে কথা বলতে Microphone permission দরকার।"
    }

    LaunchedEffect(Unit) {
        if (!voice.active) {
            val granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) voice.start() else launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val target = when (voice.state) {
        VoiceState.LISTENING -> Color(0xFF22D3EE)
        VoiceState.THINKING -> Color(0xFFF5B83D)
        VoiceState.SPEAKING -> Color(0xFFEC4899)
        VoiceState.IDLE -> LiaColors.Purple
    }
    val tint by animateColorAsState(target, animationSpec = tween(600), label = "tint")
    val intensity = when (voice.state) {
        VoiceState.SPEAKING -> 1.8f
        VoiceState.LISTENING -> 1.3f
        VoiceState.THINKING -> 0.8f
        VoiceState.IDLE -> 0.6f
    }

    val title = when {
        voice.muted -> "Muted"
        voice.state == VoiceState.LISTENING -> "Lia is listening..."
        voice.state == VoiceState.THINKING -> "Lia is thinking..."
        voice.state == VoiceState.SPEAKING -> "Lia is speaking..."
        else -> "Tap the orb to talk"
    }
    val sub = when {
        voice.info.isNotEmpty() -> voice.info
        voice.state == VoiceState.LISTENING && voice.partial.isNotEmpty() -> voice.partial
        voice.state != VoiceState.LISTENING && voice.caption.isNotEmpty() -> voice.caption
        else -> "Tap the orb to interrupt"
    }

    Box(Modifier.fillMaxSize()) {
        // কিনারার গ্লো
        Box(
            Modifier.fillMaxSize().border(
                2.dp,
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = 0.55f), Color.Transparent, tint.copy(alpha = 0.55f))
                )
            )
        )

        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Lia", fontFamily = FontFamily.Cursive, fontSize = 30.sp,
                    color = LiaColors.PurpleLight, modifier = Modifier.weight(1f)
                )
                Text(
                    if (voice.lang == "bn-BD") "বাংলা" else "EN",
                    color = LiaColors.PurpleLight,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(LiaColors.Card)
                        .border(1.dp, LiaColors.Stroke, RoundedCornerShape(10.dp))
                        .clickable { voice.toggleLang() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.weight(1f))
            GlowOrb(
                size = 290.dp,
                tint = tint,
                showMic = false,
                intensity = intensity,
                onClick = { voice.tapOrb() }
            )
            Spacer(Modifier.height(24.dp))
            Text(
                title, fontSize = 24.sp, fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif, color = LiaColors.TextMain
            )
            Spacer(Modifier.height(8.dp))
            Text(
                sub, fontSize = 13.sp, color = LiaColors.TextSub,
                textAlign = TextAlign.Center, maxLines = 4,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundBtn(
                    if (voice.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    "Mute"
                ) { voice.toggleMute() }
                RoundBtn(Icons.Rounded.CallEnd, "End", 76.dp, Color(0xFFEF4444)) { onEnd() }
                RoundBtn(Icons.Rounded.KeyboardArrowDown, "Minimize") { onMinimize() }
            }
        }
    }
}

@Composable
private fun RoundBtn(
    icon: ImageVector,
    desc: String,
    size: Dp = 58.dp,
    bg: Color = LiaColors.Card,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, LiaColors.Stroke, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = Color.White, modifier = Modifier.size(size * 0.45f))
    }
}
