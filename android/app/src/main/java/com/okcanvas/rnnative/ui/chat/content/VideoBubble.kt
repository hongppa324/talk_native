package com.okcanvas.rnnative.ui.chat.room.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertPhoto
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.okcanvas.rnnative.ui.theme.BrandYellow
import com.okcanvas.rnnative.ui.theme.BrandSurfaceLight
import com.okcanvas.rnnative.ui.theme.BrandOnSurfaceLight

/**
 * 비디오 메시지 버블 (썸네일 지원)
 */
@Composable
fun VideoBubble(
    caption: String?,
    url: String?,                 // 동영상 원본 URL
    thumbnailUrl: String? = null, // ✅ 썸네일 URL 추가
    durationText: String? = null,
    isMe: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val bubbleShape = RoundedCornerShape(10.dp)
    val bg = if (isMe) BrandYellow else BrandSurfaceLight
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth(0.6f) // 화면 60%만 사용
            .background(bg, bubbleShape)
            .clip(bubbleShape)
            // .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() }) }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f) // 16:9 비율 유지
                .clip(bubbleShape)
        ) {
            if (isPreview) {
                ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = bubbleShape)
            } else {
                // ✅ 썸네일 우선 → 없으면 url → 없으면 최종 폴백
                val displayUrl = thumbnailUrl ?: url ?: "https://picsum.photos/seed/video_fallback/1280/720"
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(displayUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "video message thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = bubbleShape) },
                    error   = { ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = bubbleShape) }
                )
            }

            // 상단 그라데이션 (텍스트 가독성)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                BrandOnSurfaceLight.copy(alpha = 0.4f),
                                BrandOnSurfaceLight.copy(alpha = 0.0f)
                            )
                        )
                    )
            )

            // ▶ 중앙 재생 버튼 오버레이
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = BrandOnSurfaceLight.copy(alpha = 0.75f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "재생",
                    tint = BrandSurfaceLight,
                    modifier = Modifier.padding(6.dp)
                )
            }

            // ⏱️ 우상단 길이 배지
            if (!durationText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandOnSurfaceLight.copy(alpha = 0.75f),
                    contentColor = BrandSurfaceLight,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (!caption.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = BrandOnSurfaceLight,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ImagePlaceholder(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .background(BrandOnSurfaceLight.copy(alpha = 0.06f), shape)
            .border(0.5.dp, BrandOnSurfaceLight.copy(alpha = 0.08f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.InsertPhoto,
            contentDescription = null,
            tint = BrandOnSurfaceLight.copy(alpha = 0.6f),
            modifier = Modifier.size(36.dp)
        )
    }
}
