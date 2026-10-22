package com.lia.assistant.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lia.assistant.ui.theme.LiaColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float)

/** প্রিমিয়াম ব্যাকগ্রাউন্ড: গ্রেডিয়েন্ট + গ্লো + মিটমিট করা তারা */
@Composable
fun LiaBackground(content: @Composable BoxScope.() -> Unit) {
    val stars = remember {
        List(70) {
            Star(
                Random.nextFloat(),
                Random.nextFloat(),
                Random.nextFloat() * 1.4f + 0.5f,
                Random.nextFloat()
            )
        }
    }
    val t by rememberInfiniteTransition(label = "stars").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "t"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LiaColors.Bg0, LiaColors.Bg1, LiaColors.Bg2)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width * 0.5f, size.height * 0.22f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(LiaColors.Purple.copy(alpha = 0.28f), Color.Transparent),
                    center = c,
                    radius = size.width * 0.9f
                ),
                radius = size.width * 0.9f,
                center = c
            )
            stars.forEach { s ->
                val a = 0.25f + 0.75f * abs(sin((t + s.phase) * 2f * PI.toFloat()))
                drawCircle(
                    color = Color.White.copy(alpha = a * 0.8f),
                    radius = s.r.dp.toPx(),
                    center = Offset(s.x * size.width, s.y * size.height)
                )
            }
        }
        content()
    }
}

/** গ্লো করা অরব (মাইক বাটন) */
@Composable
fun GlowOrb(size: Dp, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val inf = rememberInfiniteTransition(label = "orb")
    val pulse by inf.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val glow by inf.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "glow"
    )
    val clickMod = if (onClick != null)
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ) else Modifier

    Box(modifier.size(size).then(clickMod), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .scale(pulse)
                .background(
                    Brush.radialGradient(listOf(LiaColors.Purple.copy(alpha = glow), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .fillMaxSize(0.72f)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF3B2A7A), Color(0xFF14102B))))
                .border(
                    2.dp,
                    Brush.sweepGradient(listOf(LiaColors.PurpleLight, LiaColors.Pink, LiaColors.PurpleLight)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(size * 0.3f))
        }
    }
}

@Composable
fun LiaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .clip(shape)
            .background(LiaColors.Card.copy(alpha = 0.78f))
            .border(1.dp, LiaColors.Stroke, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        content = content
    )
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF9F6BFF), Color(0xFF7C4DFF))))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun liaFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LiaColors.Purple,
    unfocusedBorderColor = LiaColors.Stroke,
    focusedContainerColor = LiaColors.Bg1,
    unfocusedContainerColor = LiaColors.Bg1,
    cursorColor = LiaColors.PurpleLight,
    focusedTextColor = LiaColors.TextMain,
    unfocusedTextColor = LiaColors.TextMain
)
