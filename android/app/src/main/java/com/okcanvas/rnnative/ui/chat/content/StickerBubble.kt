package com.okcanvas.rnnative.ui.chat.room.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertEmoticon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.okcanvas.rnnative.ui.theme.BrandYellow
import com.okcanvas.rnnative.ui.theme.BrandSurfaceLight
import com.okcanvas.rnnative.ui.theme.BrandOnSurfaceLight

/**
 * 스티커/이모티콘 버블
 * - url(원격 이미지)이 있으면 이미지를 로드
 * - 없으면 기본 아이콘 플레이스홀더 표시
 */
@Composable
fun StickerBubble(
    label: String?,
    url: String?,      // 예: "https://..."  (없으면 플레이스홀더)
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(10.dp)
    val contentShape = RoundedCornerShape(12.dp)
    val bg = if (isMe) BrandYellow else BrandSurfaceLight
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .widthIn(max = 160.dp)
            .background(bg, bubbleShape)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(contentShape)
                .background(BrandOnSurfaceLight.copy(alpha = 0.05f), contentShape),
            contentAlignment = Alignment.Center
        ) {
            if (url.isNullOrBlank() || isPreview) {
                StickerPlaceholder()
            } else {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = "sticker",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(contentShape),
                    loading = { StickerPlaceholder() },
                    error   = { StickerPlaceholder() }
                )
            }
        }

        if (!label.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = BrandOnSurfaceLight
            )
        }
    }
}

@Composable
private fun StickerPlaceholder() {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandOnSurfaceLight.copy(alpha = 0.05f), shape)
            .border(0.5.dp, BrandOnSurfaceLight.copy(alpha = 0.08f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.InsertEmoticon,
            contentDescription = null,
            tint = BrandOnSurfaceLight.copy(alpha = 0.6f),
            modifier = Modifier.size(42.dp)
        )
    }
}
