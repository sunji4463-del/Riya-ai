package com.lia.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object LiaColors {
    val Bg0 = Color(0xFF05030F)
    val Bg1 = Color(0xFF0A0620)
    val Bg2 = Color(0xFF130B33)
    val Purple = Color(0xFF8B5CF6)
    val PurpleLight = Color(0xFFB69CFF)
    val Pink = Color(0xFFEC6FD1)
    val Gold = Color(0xFFFFB84D)
    val Card = Color(0xFF16102E)
    val Stroke = Color(0xFF2E2559)
    val TextMain = Color(0xFFF1ECFF)
    val TextSub = Color(0xFF9D95C9)
    val Green = Color(0xFF34D399)
}

@Composable
fun LiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LiaColors.Purple,
            onPrimary = Color.White,
            background = LiaColors.Bg0,
            surface = LiaColors.Card,
            onBackground = LiaColors.TextMain,
            onSurface = LiaColors.TextMain
        ),
        content = content
    )
}
