package com.okcanvas.rnnative.ui.chat.room.content

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.okcanvas.rnnative.ui.theme.BrandYellow
import com.okcanvas.rnnative.ui.theme.BrandSurfaceLight
import com.okcanvas.rnnative.ui.theme.BrandOnSurfaceLight

@Composable
fun FileBubble(
    fileName: String,
    fileSize: String?,
    url: String?,        // 필요 시 다운로드/열기 등에 사용
    isMe: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val backgroundColor = if (isMe) BrandYellow else BrandSurfaceLight
    val textColor = BrandOnSurfaceLight
    val subTextColor = if (isMe) BrandOnSurfaceLight.copy(alpha = 0.7f) else BrandOnSurfaceLight.copy(alpha = 0.6f)
    val iconTint = if (isMe) BrandOnSurfaceLight else BrandOnSurfaceLight

    Row(
        modifier = Modifier
            .widthIn(min = 180.dp, max = 280.dp)
            .background(color = backgroundColor, shape = RoundedCornerShape(10.dp))
            // .clickable { onClick() }
            // .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = Icons.Outlined.FilePresent,
            contentDescription = "파일",
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!fileSize.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = fileSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = subTextColor
                )
            }
        }
    }
}
