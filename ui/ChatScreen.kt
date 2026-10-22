package com.lia.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lia.assistant.data.Msg
import com.lia.assistant.ui.theme.LiaColors

/** হোম স্ক্রিনের ইনপুট থেকে প্রশ্ন চ্যাটে পাঠানোর জন্য */
object PendingQuery { var text: String? = null }

@Composable
fun ChatScreen(vm: ChatViewModel) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val msgs = vm.messages

    LaunchedEffect(Unit) {
        PendingQuery.text?.let { q ->
            PendingQuery.text = null
            vm.newChat()
            vm.send(q)
        }
    }
    LaunchedEffect(msgs.size, vm.loading) {
        val last = msgs.size + (if (vm.loading) 1 else 0) - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

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
            Column(Modifier.weight(1f)) {
                Text("Lia", color = LiaColors.TextMain, fontSize = 17.sp)
                Text("Here to help", color = LiaColors.TextSub, fontSize = 11.sp)
            }
            IconButton(onClick = { vm.newChat() }) {
                Icon(Icons.Rounded.Add, "New chat", tint = LiaColors.PurpleLight)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (msgs.isEmpty() && !vm.loading) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlowOrb(130.dp)
                    Text("Hi! I'm Lia", fontSize = 20.sp, color = LiaColors.TextMain)
                    Text("Ask me anything", fontSize = 13.sp, color = LiaColors.TextSub)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(msgs) { Bubble(it) }
                    if (vm.loading) {
                        item {
                            Text(
                                "Lia is thinking...", fontStyle = FontStyle.Italic,
                                color = LiaColors.TextSub, fontSize = 13.sp
                            )
                        }
                    }
                }
            }
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
                onClick = {
                    if (!vm.loading && input.isNotBlank()) {
                        vm.send(input)
                        input = ""
                    }
                },
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(if (vm.loading) LiaColors.Stroke else LiaColors.Purple)
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
        Box(Modifier.widthIn(max = 310.dp).clip(shape).background(bg).padding(12.dp)) {
            SelectionContainer {
                Text(
                    md(m.text),
                    color = if (m.error) Color(0xFFFF8A8A) else LiaColors.TextMain,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/** ছোট markdown: **bold**, `code`, লিস্ট, হেডার */
private fun md(raw: String): AnnotatedString {
    val text = raw
        .replace("```", "")
        .replace(Regex("(?m)^#{1,6}\\s*"), "")
        .replace(Regex("(?m)^[ \\t]*[*-] "), "• ")
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                if (end > 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end > 0) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x33FFFFFF)
                        )
                    ) { append(text.substring(i + 1, end)) }
                    i = end + 1
                    continue
                }
            }
            append(text[i])
            i++
        }
    }
}
