package com.okcanvas.rnnative.ui.chat.room.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertPhoto
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlin.math.ceil
import kotlin.math.max
import com.okcanvas.rnnative.ui.theme.BrandYellow
import com.okcanvas.rnnative.ui.theme.BrandSurfaceLight
import com.okcanvas.rnnative.ui.theme.BrandOnSurfaceLight

/**
 * 여러 장의 이미지를 타일로 묶어 보여주는 버블
 * - 1장: 단일 이미지와 유사하게 크게 (클릭 → index=0)
 * - 2~3장: 2열
 * - 4장 이상: 3열 (최대 9장)
 */
@Composable
fun ImageBubble(
    caption: String?,
    urls: List<String>,
    isMe: Boolean,
    onThumbClick: (Int) -> Unit,   // 인덱스 콜백
    onLongPress: () -> Unit
) {
    val bubbleShape = RoundedCornerShape(10.dp)
    val bg = if (isMe) BrandYellow else BrandSurfaceLight
    val sanitized = urls.filter { it.isNotBlank() }.take(9)
    if (sanitized.isEmpty()) return

    Column(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .background(bg, bubbleShape)
            .padding(6.dp)
    ) {
        val thumbShape = RoundedCornerShape(8.dp)
        val spacing = 4.dp
        val count = sanitized.size
        val columns = when {
            count <= 1 -> 1
            count <= 3 -> 2
            else -> 3
        }

        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 248.dp)
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
        ) {
            val usableWidth = maxWidth
            val cellSize = (usableWidth - spacing * (columns - 1)) / columns
            val rows = ceil(count / columns.toFloat()).toInt()
            val gridHeight = cellSize * rows + spacing * max(0, rows - 1)

            if (columns == 1) {
                // 1장일 때도 클릭 지원(index=0)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThumbClick(0) }
                ) {
                    SingleGalleryImage(
                        url = sanitized.firstOrNull(),
                        shape = thumbShape,
                        minHeight = 140.dp,
                        maxHeight = 240.dp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .height(gridHeight)
                        .width(usableWidth),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    userScrollEnabled = false
                ) {
                    itemsIndexed(sanitized) { index, url ->
                        Box(
                            modifier = Modifier
                                .clip(thumbShape)
                                .clickable { onThumbClick(index) }
                        ) {
                            GalleryThumb(url = url, shape = thumbShape)
                        }
                    }
                }
            }
        }

        if (!caption.isNullOrBlank()) {
            val captionTopSpace = if (count <= 1) 6.dp else 2.dp
            Spacer(Modifier.height(captionTopSpace))
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

/* ---------- 내부 구성 요소 ---------- */

@Composable
private fun SingleGalleryImage(
    url: String?,
    shape: RoundedCornerShape,
    minHeight: androidx.compose.ui.unit.Dp,
    maxHeight: androidx.compose.ui.unit.Dp
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val fallback = "https://picsum.photos/seed/kakaostyle_gallery/900/600"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight, max = maxHeight)
            .clip(shape)
    ) {
        if (isPreview) {
            ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape)
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url ?: fallback)
                    .crossfade(true)
                    .build(),
                contentDescription = "image gallery item",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape) },
                error   = { ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape) }
            )
        }
    }
}

@Composable
private fun GalleryThumb(
    url: String,
    shape: RoundedCornerShape
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
    ) {
        if (isPreview) {
            ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape)
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = "image thumb",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape) },
                error   = { ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape) }
            )
        }
    }
}

@Composable
private fun ImagePlaceholder(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    // 플레이스홀더: 밝은 배경 + 옅은 보더 + 아이콘 톤다운
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
