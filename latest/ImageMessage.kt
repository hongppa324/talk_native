package com.vmerp.works.ui.chat.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertPhoto
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.vmerp.works.ChatNativeTokenStore
import com.vmerp.works.ui.theme.BrandOnSurfaceLight
import com.vmerp.works.util.*

private val MaxSingleImageWidth = 240.dp

// 이미지 메시지
@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
) // combinedClickable이 ExperimentalFoundationApi에 속해 있어서 추후 변경될 수 있기 때문에 붙이는 annotation
@Composable
fun ImageMessage(
    urls: List<String>,
    onThumbClick: (Int) -> Unit, // 인덱스 콜백
    onLongPress: (() -> Unit)? = null,
) {
    val sanitized =
        urls
            .mapIndexedNotNull { idx, u -> if (u.isNotBlank()) idx to u else null }
            .take(30)
    if (sanitized.isEmpty()) return

    val count = sanitized.size
    val cell: Dp = if (count == 2 || count == 4) 120.dp else 80.dp
    val spacing = 2.dp

    val maxColumns = if (count == 2 || count == 4) 2 else 3
    val maxRowWidth: Dp = cell * maxColumns + spacing * (maxColumns - 1)

    val rows: List<List<Pair<Int, String>>> =
        remember(sanitized) {
            chunkArray(sanitized, size = 3)
        }

    Column {
        // 이미지 1개일 때
        if (count == 1) {
            val (origIdx, url) = sanitized.first()
            val targetWidth = MaxSingleImageWidth

            Box(
                modifier =
                    Modifier
                        .combinedClickable(
                            indication = null, // 클릭 시 RippleEffect(회색 음영) 제거
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onThumbClick(origIdx) },
                            onLongClick = { onLongPress?.invoke() },
                        ),
            ) {
                SingleGalleryImage(
                    url = url,
                    shape = RoundedCornerShape(4.dp),
                    targetWidth = targetWidth,
                    minHeight = 140.dp,
                    maxHeight = 240.dp,
                )
            }

            return@Column
        }

        // 2개 이상일 때
        rows.forEach { row ->
            Row(
                modifier =
                    Modifier
                        .width(maxRowWidth)
                        .height(cell),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEach { (origIdx, url) ->
                    if (row.size == maxColumns) {
                        Box(
                            modifier =
                                Modifier
                                    .size(cell)
                                    .clip(RoundedCornerShape(4.dp))
                                    .combinedClickable(
                                        indication = null, // 클릭 시 RippleEffect(회색 음영) 제거
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = { onThumbClick(origIdx) },
                                        onLongClick = { onLongPress?.invoke() },
                                    ),
                        ) {
                            GalleryThumbNail(url = url, shape = RoundedCornerShape(4.dp))
                        }
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(cell)
                                    .clip(RoundedCornerShape(4.dp))
                                    .combinedClickable(
                                        indication = null, // 클릭 시 RippleEffect(회색 음영) 제거
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = { onThumbClick(origIdx) },
                                        onLongClick = { onLongPress?.invoke() },
                                    ),
                        ) {
                            GalleryThumbNail(url = url, shape = RoundedCornerShape(4.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun <T> chunkArray(
    items: List<T>,
    size: Int = 3,
): List<List<T>> {
    if (items.isEmpty()) return emptyList()
    val chunks = items.chunked(size).toMutableList()
    // 마지막 배열이 1개면, 앞 줄에서 하나 pop해서 앞으로 붙임
    if (chunks.size > 1 && chunks.last().size == 1) {
        val prev = chunks[chunks.lastIndex - 1].toMutableList()
        val last = chunks.last().toMutableList()
        if (prev.isNotEmpty()) {
            last.add(0, prev.removeAt(prev.lastIndex))
        }
        chunks[chunks.lastIndex - 1] = prev
        chunks[chunks.lastIndex] = last
    }
    return chunks
}

// ---------- 내부 구성 요소 ----------
@Composable
private fun SingleGalleryImage(
    url: String?,
    shape: RoundedCornerShape,
    targetWidth: Dp,
    minHeight: Dp,
    maxHeight: Dp,
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    // 톡 토큰 가져오기
    val token = ChatNativeTokenStore.getToken(context)
    val fallback = "https://picsum.photos/seed/kakaostyle_gallery/900/600"

    // 로딩 후 계산된 targetHeight(dp)
    var targetHeight by remember(url, targetWidth) { mutableStateOf<Dp?>(null) }

    // 기본 높이: RN처럼 캐시가 없을 때 최대 240 안에서 안전한 최소값 사용
    val defaultHeight = minHeight

    Box(
        modifier =
            Modifier
                .width(targetWidth) // 가로 고정
                .height(targetHeight ?: defaultHeight) // 로딩 전엔 기본값, 후엔 계산값
                .clip(shape),
    ) {
        if (isPreview) {
            ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape)
        } else {
            SubcomposeAsyncImage(
                model =
                    ImageRequest.Builder(context)
                        .data(url ?: fallback)
                        .apply {
                            if (!token.isNullOrBlank()) {
                                addHeader("Authorization", "Bearer $token")
                            }
                        }
                        .crossfade(true)
                        .build(),
                contentDescription = "image gallery item",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape) },
                error = { ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape) },
                onSuccess = { success ->
                    val dw = success.result.drawable.intrinsicWidth
                    val dh = success.result.drawable.intrinsicHeight
                    if (dw > 0 && dh > 0) {
                        // height(dp) = targetWidth(dp) * dh/dw
                        val h = targetWidth * (dh.toFloat() / dw.toFloat())
                        // RN 규칙 반영: 최대 240dp 까지만 (이미 targetWidth가 240 이하이므로 높이만 clamp)
                        targetHeight = h.coerceIn(minHeight, maxHeight)
                    }
                },
            )
        }
    }
}

@Composable
private fun GalleryThumbNail(
    url: String,
    shape: RoundedCornerShape,
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    // 톡 토큰 가져오기
    val token = ChatNativeTokenStore.getToken(context)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(shape),
    ) {
        if (isPreview) {
            ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape)
        } else {
            SubcomposeAsyncImage(
                model =
                    ImageRequest.Builder(context)
                        .data(url)
                        .apply {
                            if (!token.isNullOrBlank()) {
                                addHeader("Authorization", "Bearer $token")
                            }
                        }
                        .crossfade(true)
                        .build(),
                contentDescription = "image thumb",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape) },
                error = { ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape) },
            )
        }
    }
}

@Composable
private fun ImagePlaceholder(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
) {
    // 플레이스홀더: 밝은 배경 + 옅은 보더 + 아이콘 톤다운
    Box(
        modifier =
            modifier
                .background(BrandOnSurfaceLight.copy(alpha = 0.06f), shape)
                .border(0.5.dp, BrandOnSurfaceLight.copy(alpha = 0.08f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.InsertPhoto,
            contentDescription = null,
            tint = BrandOnSurfaceLight.copy(alpha = 0.6f),
            modifier = Modifier.size(36.dp),
        )
    }
}
