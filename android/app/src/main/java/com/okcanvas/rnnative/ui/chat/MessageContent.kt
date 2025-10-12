// app/src/main/java/com/example/kakaostyle/ui/chat/room/MessageContent.kt
package com.okcanvas.rnnative.ui.chat.room

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.okcanvas.rnnative.model.ChatMessage
import com.okcanvas.rnnative.model.MessageType
import com.okcanvas.rnnative.ui.LocalImageViewer
import com.okcanvas.rnnative.ui.chat.room.content.*

@Composable
fun MessageContent(
    msg: ChatMessage,
    isMe: Boolean,
    highlight: String?,
    onClick: (ChatMessage) -> Unit = {},
    onLongPress: (ChatMessage) -> Unit = {}
) {
    val viewer = LocalImageViewer.current
    val context = LocalContext.current // (현재 파일에선 직접 사용 X, 필요시 확장)

    when (msg.type) {
        MessageType.TEXT -> {
            TextBubble(
                text = msg.text,
                isMe = isMe,
                highlight = highlight,
                onLongPress = { onLongPress(msg) }
            )
        }

        MessageType.IMAGE -> {
            val urls = remember(msg.mediaUrl, msg.mediaUrls) {
                when {
                    !msg.mediaUrls.isNullOrEmpty() -> msg.mediaUrls.filter { it.isNotBlank() }
                    !msg.mediaUrl.isNullOrBlank() -> listOf(msg.mediaUrl)
                    else -> emptyList()
                }
            }

            if (urls.isNotEmpty()) {
                ImageBubble(
                    caption = msg.text.takeIf { it.isNotBlank() },
                    urls = urls,
                    isMe = isMe,
                    onThumbClick = { index ->
                        val opened = viewer?.openGallery?.invoke(urls, index) != null
                        if (!opened) onClick(msg) // 뷰어 미제공 시 상위 클릭으로 폴백
                    },
                    onLongPress = { onLongPress(msg) }
                )
            }
        }

        MessageType.FILE -> {
            FileBubble(
                fileName = msg.text,
                fileSize = msg.fileSize,
                url = msg.mediaUrl,
                isMe = isMe,
                onClick = { onClick(msg) },
                onLongPress = { onLongPress(msg) }
            )
        }

        MessageType.STICKER -> {
            StickerBubble(
                label = msg.text.takeIf { it.isNotBlank() },
                url = msg.mediaUrl,
                isMe = isMe
            )
            // 필요시 StickerBubble에 onLongPress 추가 가능
        }

        MessageType.SYSTEM -> {
            WidgetSystemMessage(text = msg.text)
        }

        MessageType.AUDIO -> {
            AudioBubble(
                title = msg.text.ifBlank { "음성 메시지" },
                duration = msg.mediaDuration,
                url = msg.mediaUrl,
                isMe = isMe,
                isPlaying = false,   // 리스트에선 기본 재생 X
                progress = 0f,
                errorMessage = null,
                onPlayPause = { onClick(msg) },   // 재생 요청을 상위로 위임
                onSeek = { /* 오버레이/플레이어에서 처리 */ },
                onLongPress = { onLongPress(msg) }
            )
        }

        MessageType.VIDEO -> {
            VideoBubble(
                caption = msg.text.takeIf { it.isNotBlank() },
                url = msg.mediaUrl,
                thumbnailUrl = msg.thumbnailUrl,   // ✅ 썸네일 전달 추가
                durationText = msg.mediaDuration,
                isMe = isMe,
                onClick = { onClick(msg) },        // 플레이/오버레이 열기 위임
                onLongPress = { onLongPress(msg) }
            )
        }
    }
}
