package com.okcanvas.rnnative.ui.chat.room.content

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.okcanvas.rnnative.ui.theme.BrandOnSurfaceLight
import com.okcanvas.rnnative.ui.theme.BrandSurfaceLight
import com.okcanvas.rnnative.ui.theme.BrandYellow

@Composable
fun TextBubble(
    text: String,
    isMe: Boolean,
    highlight: String?,
    onLongPress: () -> Unit,
    maxWidthRatio: Float = 0.65f, // 부모 폭의 65%를 최대 폭으로 제한
    highlightColor: androidx.compose.ui.graphics.Color = BrandYellow.copy(alpha = 0.45f)
) {
    // ✅ 하이라이트 처리 (locale-safe, substring 기반)
    val annotated = remember(text, highlight, highlightColor) {
        if (!highlight.isNullOrBlank()) {
            val key = highlight
            buildAnnotatedString {
                var i = 0
                while (i < text.length) {
                    val hit = text.indexOf(key, startIndex = i, ignoreCase = true)
                    if (hit == -1) {
                        append(text.substring(i, text.length))
                        break
                    } else {
                        if (hit > i) append(text.substring(i, hit))
                        withStyle(SpanStyle(background = highlightColor)) {
                            append(text.substring(hit, hit + key.length))
                        }
                        i = hit + key.length
                    }
                }
            }
        } else {
            buildAnnotatedString { append(text) }
        }
    }

    BoxWithConstraints {
        val safeRatio = maxWidthRatio.coerceIn(0.3f, 0.9f)
        val maxBubbleWidth = this.maxWidth * safeRatio

        val bubbleColor = if (isMe) BrandYellow else BrandSurfaceLight
        val textColor = BrandOnSurfaceLight

        val shape = if (isMe) {
            RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 2.dp)
        } else {
            RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 2.dp, bottomEnd = 10.dp)
        }

        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp)
                .widthIn(min = 40.dp, max = maxBubbleWidth)
                .background(color = bubbleColor, shape = shape)
//                .combinedClickable(
//                    onClick = { /* no-op (필요 시 추가) */ },
//                    onLongClick = onLongPress
//                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = annotated,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineBreak = LineBreak.Paragraph,
                    hyphens = Hyphens.Auto
                ),
                softWrap = true,
                overflow = TextOverflow.Clip
            )
        }
    }
}
