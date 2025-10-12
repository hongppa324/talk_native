@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.okcanvas.rnnative.ui.chat.room.content

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.okcanvas.rnnative.ui.theme.*

/**
 * 오디오 전용 버블
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioBubble(
    title: String,
    duration: String?,
    url: String?,
    isMe: Boolean,
    isPlaying: Boolean,
    progress: Float,
    errorMessage: String? = null,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onLongPress: () -> Unit
) {
    val bg = if (isMe) BrandYellow else BrandSurfaceLight
    val textColor = if (isMe) BrandOnSurfaceLight else BrandOnSurfaceLight
    val subText = if (isMe) Color(0xFF3A3A3A) else Color(0xFF6F6F6F)
    val inactiveTrack = if (isMe) Color(0xFF2B2B2B).copy(alpha = 0.15f) else Color(0xFFE0E0E0)

    val sliderInteractions = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .heightIn(min = 56.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onPlayPause,
                onLongClick = onLongPress
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ▶ 플레이/일시정지 버튼
        Surface(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            color = if (isMe) BrandOnSurfaceLight else Color(0xFFEFEFEF)
        ) {
            IconButton(onClick = onPlayPause, enabled = errorMessage == null) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "일시정지" else "재생",
                    tint = if (isMe) BrandYellow else BrandOnSurfaceLight
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                maxLines = 2,
                minLines = 1,
                overflow = TextOverflow.Ellipsis
            )

//            Slider(
//                value = progress.coerceIn(0f, 1f),
//                onValueChange = onSeek,
//                modifier = Modifier.fillMaxWidth(),
//                enabled = errorMessage == null,
//                interactionSource = sliderInteractions,
//                thumb = {},
//                colors = SliderDefaults.colors(
//                    activeTrackColor = if (isMe) BrandOnSurfaceLight else BrandOnSurfaceLight,
//                    inactiveTrackColor = inactiveTrack,
//                    disabledActiveTrackColor = Color(0xFFBDBDBD),
//                    disabledInactiveTrackColor = Color(0xFFEEEEEE)
//                )
//            )

            Spacer(Modifier.height(2.dp))

            if (errorMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = duration ?: "--:--",
                        style = MaterialTheme.typography.labelSmall,
                        color = subText
                    )
                }
            } else {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = ErrorRed,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* === Preview Section === */

@Preview(name = "AudioBubble (Me) - Light", showBackground = true)
@Preview(name = "AudioBubble (Me) - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun Preview_AudioBubble_Me() {
    MaterialTheme {
        Surface(color = Color(0xFFF5F5F5)) {
            var playing by remember { mutableStateOf(true) }
            var progress by remember { mutableStateOf(0.35f) }

            AudioBubble(
                title = "회의 녹음_요약(12초).m4a",
                duration = "00:12",
                url = "asset:///audio/sample.mp3",
                isMe = true,
                isPlaying = playing,
                progress = progress,
                errorMessage = null,
                onPlayPause = { playing = !playing },
                onSeek = { progress = it },
                onLongPress = {}
            )
        }
    }
}

@Preview(name = "AudioBubble (Other, Error) - Light", showBackground = true)
@Preview(name = "AudioBubble (Other, Error) - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun Preview_AudioBubble_Other_Error() {
    MaterialTheme {
        Surface(color = Color(0xFFF5F5F5)) {
            var playing by remember { mutableStateOf(false) }
            var progress by remember { mutableStateOf(0.7f) }

            AudioBubble(
                title = "상대방의 음성 메시지 — 네트워크 오류로 재생에 실패했습니다. 다시 시도해 주세요.",
                duration = null,
                url = "https://invalid.example.com/audio.mp3",
                isMe = false,
                isPlaying = playing,
                progress = progress,
                errorMessage = "재생할 수 없습니다 (HTTP 404)",
                onPlayPause = { playing = !playing },
                onSeek = { progress = it },
                onLongPress = {}
            )
        }
    }
}
