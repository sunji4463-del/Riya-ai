package com.lia.assistant.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lia.assistant.ui.theme.LiaColors

private fun NavController.go(route: String) = navigate(route) {
    popUpTo("home") { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiaApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val showBar = route in listOf("home", "chat", "history", "settings")
    val imeVisible = WindowInsets.isImeVisible

    LiaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBar && !imeVisible) {
                    BottomBar(route, onGo = { nav.go(it) }, onVoice = { nav.navigate("voice") })
                }
            }
        ) { pad ->
            NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
                composable("home") {
                    HomeScreen(
                        onVoice = { nav.navigate("voice") },
                        onChat = { nav.go("chat") },
                        onSettings = { nav.go("settings") }
                    )
                }
                composable("chat") { ChatScreen() }
                composable("history") { HistoryScreen() }
                composable("settings") { SettingsScreen(onConnections = { nav.navigate("connections") }) }
                composable("connections") { ConnectionsScreen(onBack = { nav.popBackStack() }) }
                composable("voice") { VoiceScreen(onEnd = { nav.popBackStack() }) }
            }
        }
    }
}

@Composable
private fun BottomBar(route: String, onGo: (String) -> Unit, onVoice: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(LiaColors.Card.copy(alpha = 0.92f), shape)
                .border(1.dp, LiaColors.Stroke, shape),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(Icons.Rounded.Home, "Home", route == "home") { onGo("home") }
            NavItem(Icons.Rounded.ChatBubbleOutline, "Chat", route == "chat") { onGo("chat") }
            Box(Modifier.requiredSize(72.dp).offset(y = (-8).dp)) {
                GlowOrb(72.dp, onClick = onVoice)
            }
            NavItem(Icons.Rounded.History, "History", route == "history") { onGo("history") }
            NavItem(Icons.Rounded.Settings, "Settings", route == "settings") { onGo("settings") }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = if (selected) LiaColors.Gold else LiaColors.TextSub
    Column(
        Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = c, modifier = Modifier.size(22.dp))
        Text(label, color = c, fontSize = 10.sp)
    }
}
