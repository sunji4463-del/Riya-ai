package com.lia.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lia.assistant.ui.theme.LiaColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: ChatViewModel, onOpen: (String) -> Unit) {
    val fmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("History", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = LiaColors.TextMain)
        Text("Your saved chats", fontSize = 13.sp, color = LiaColors.TextSub)
        Spacer(Modifier.height(16.dp))

        if (vm.chats.isEmpty()) {
            Text("No chats yet. Start talking to Lia!", color = LiaColors.TextSub)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(vm.chats, key = { it.id }) { c ->
                    LiaCard(Modifier.fillMaxWidth(), onClick = { onOpen(c.id) }) {
                        Row(
                            Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    c.title, color = LiaColors.TextMain, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium
                                )
                                Text(
                                    fmt.format(Date(c.updatedAt)),
                                    fontSize = 11.sp, color = LiaColors.TextSub
                                )
                            }
                            IconButton(onClick = { vm.deleteChat(c.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, "Delete", tint = LiaColors.TextSub)
                            }
                        }
                    }
                }
            }
        }
    }
}
