package com.lia.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lia.assistant.ui.theme.LiaColors

data class Msg(val text: String, val fromUser: Boolean)

/** Phase 1 এর সাময়িক ব্যবস্থা: হোম থেকে প্রশ্ন চ্যাটে পাঠানোর জন্য */
object PendingQuery { var text: String? = null }

@Composable
fun ChatScreen() {
    val msgs = remember {
        mutableStateListOf(Msg("Hi! I'm Lia. Ask me anything.", false))
    }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    fun send(t: String) {
        if (t.isBlank()) return
        msgs.add(Msg(t.trim(), true))
        msgs.add(Msg("AI replies will be connected in Phase 2 (Groq / Gemini).", false))
    }

    LaunchedEffect(Unit) {
        PendingQuery.text?.let { send(it); PendingQuery.text = null }
    }
    LaunchedEffect(msgs.size) { listState.animateScrollToItem(msgs.lastIndex) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF8B5CF6), Color(0xFF2A1B5E))))
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Lia", color = LiaColors.TextMain, fontSize = 17.sp)
                Text("Here to help", color = LiaColors.TextSub, fontSize = 11.sp)
            }
        }

        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(msgs) { Bubble(it) }
        }

        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = liaFieldColors(),
                placeholder = { Text("Message Lia", color = LiaColors.TextSub) },
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { send(input); input = "" },
                modifier = Modifier.size(48.dp).clip(CircleShape).background(LiaColors.Purple)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, "Send", tint = Color.White)
            }
        }
    }
}

@Composable
private fun Bubble(m: Msg) {
    val shape = RoundedCornerShape(
        topStart = 18.dp, topEnd = 18.dp,
        bottomEnd = if (m.fromUser) 4.dp else 18.dp,
        bottomStart = if (m.fromUser) 18.dp else 4.dp
    )
    val bg: Brush = if (m.fromUser)
        Brush.horizontalGradient(listOf(Color(0xFF9F6BFF), Color(0xFF7C4DFF)))
    else SolidColor(LiaColors.Card)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier.widthIn(max = 300.dp).clip(shape).background(bg).padding(12.dp)
        ) {
            Text(m.text, color = LiaColors.TextMain, fontSize = 15.sp)
        }
    }
}
