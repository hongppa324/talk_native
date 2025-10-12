@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.okcanvas.rnnative.ui.chat.room.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    replyPreview: String? = null,
    onCancelReply: (() -> Unit)? = null,
) {
    var showPlus by remember { mutableStateOf(false) }
    var showEmoji by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F7F7))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        AnimatedVisibility(visible = replyPreview != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE9EDF2))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "답장: ${replyPreview.orEmpty()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF50565C),
                    modifier = Modifier.weight(1f)
                )
                if (onCancelReply != null) {
                    TextButton(onClick = onCancelReply, contentPadding = PaddingValues(0.dp)) {
                        Text("취소")
                    }
                }
            }
        }

        if (replyPreview != null) Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {

            RoundIconButton(
                onClick = { showPlus = true },
                modifier = Modifier.size(40.dp)
            ) { Icon(Icons.Outlined.Add, null, tint = Color(0xFF505050)) }

            Spacer(Modifier.width(8.dp))

            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .shadow(1.dp, RoundedCornerShape(22.dp), clip = true),
                placeholder = { Text("메시지 입력", color = Color(0xFFACB4BB)) },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color(0xFF222222),
                    focusedTextColor = Color(0xFF222222),
                    unfocusedTextColor = Color(0xFF222222),
                    focusedPlaceholderColor = Color(0xFFACB4BB),
                    unfocusedPlaceholderColor = Color(0xFFACB4BB)
                )
            )

            Spacer(Modifier.width(8.dp))

            RoundIconButton(
                onClick = { showEmoji = true },
                modifier = Modifier.size(40.dp)
            ) { Icon(Icons.Outlined.EmojiEmotions, null, tint = Color(0xFF505050)) }

            Spacer(Modifier.width(8.dp))

            val canSend = text.isNotBlank()
            if (canSend) {
                Surface(
                    onClick = onSend,
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFE100),
                    shadowElevation = 1.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Send, null, tint = Color(0xFF222222))
                    }
                }
            } else {
                RoundIconButton(
                    onClick = { /* disabled */ },
                    enabled = false,
                    modifier = Modifier.size(40.dp)
                ) { Text("#", color = Color(0xFF222222)) }
            }
        }
    }

    // TODO: showPlus / showEmoji 패널은 기존 구현과 연결
}

@Composable
private fun RoundIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(0.75.dp, Color(0x14000000), CircleShape)
        ) { content() }
    }
}


// 파일 하단에 추가하세요 (import 포함)



@Preview(
    name = "ChatInputBar - 기본 (Light)",
    showBackground = true,
    backgroundColor = 0xFFF7F7F7
)
@Composable
private fun Preview_ChatInputBar_Default_Light() {
    MaterialTheme {
        var text by rememberSaveable { mutableStateOf("") }
        ChatInputBar(
            text = text,
            onTextChange = { text = it },
            onSend = { /* no-op */ },
        )
    }
}

@Preview(
    name = "ChatInputBar - 답장 (Light)",
    showBackground = true,
    backgroundColor = 0xFFF7F7F7
)
@Composable
private fun Preview_ChatInputBar_Reply_Light() {
    MaterialTheme {
        var text by rememberSaveable { mutableStateOf("안녕하세요!") }
        ChatInputBar(
            text = text,
            onTextChange = { text = it },
            onSend = { /* no-op */ },
            replyPreview = "상대 메시지 내용 미리보기",
            onCancelReply = { /* no-op */ }
        )
    }
}

@Preview(
    name = "ChatInputBar - 기본 (Dark)",
    showBackground = true,
    backgroundColor = 0xFF121212,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun Preview_ChatInputBar_Default_Dark() {
    MaterialTheme {
        var text by rememberSaveable { mutableStateOf("보낼 메시지") }
        ChatInputBar(
            text = text,
            onTextChange = { text = it },
            onSend = { /* no-op */ },
        )
    }
}

@Preview(
    name = "ChatInputBar - 답장 (Dark)",
    showBackground = true,
    backgroundColor = 0xFF121212,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun Preview_ChatInputBar_Reply_Dark() {
    MaterialTheme {
        var text by rememberSaveable { mutableStateOf("") }
        ChatInputBar(
            text = text,
            onTextChange = { text = it },
            onSend = { /* no-op */ },
            replyPreview = "이 메시지에 답장합니다",
            onCancelReply = { /* no-op */ }
        )
    }
}

/* 선택: RoundIconButton 단독 미니 프리뷰 */
@Preview(
    name = "RoundIconButton",
    showBackground = true,
    backgroundColor = 0xFFF7F7F7
)
@Composable
private fun Preview_RoundIconButton() {
    MaterialTheme {
        RoundIconButton(onClick = { }) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = Color(0xFF505050))
        }
    }
}
