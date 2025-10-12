@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.okcanvas.rnnative.ui.chat.room

import android.content.res.Configuration
import android.icu.text.BreakIterator
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.okcanvas.rnnative.ui.theme.SquircleShape
import com.okcanvas.rnnative.ui.theme.AvatarBorder
import com.okcanvas.rnnative.ui.theme.AvatarBlue
import com.okcanvas.rnnative.ui.theme.BrandSurfaceLight

private val KakaoAvatarShape = SquircleShape(n = 3.0f)

@Composable
fun AvatarBubble(
    displayName: String? = null,
    sizeDp: Int = 40,
    useIcon: Boolean = false,
    imageRes: Int? = null,
    imageUrl: String? = null,
    avatarVersion: Long? = null, // 캐시 무효화용
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentDescription: String? = "프로필 이미지",
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { sizeDp.dp.roundToPx() } // dp → px

    // 캐시 버스터(?v=...) 적용
    val finalUrl = imageUrl?.let { url -> avatarVersion?.let { "$url?v=$it" } ?: url }

    // 안전한 라벨 계산(영문/숫자=1 grapheme, 그 외=2 graphemes, 비어있으면 공백)
    val label = remember(displayName) { avatarLabel(displayName) }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(KakaoAvatarShape)
            .background(AvatarBlue)
            .border(0.75.dp, AvatarBorder, KakaoAvatarShape) // ✅ 알파 중복 제거
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        contentAlignment = Alignment.Center
    ) {
        when {
            finalUrl != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(finalUrl)
                        .size(sizePx, sizePx)       // 요청부터 정확 크기
                        .precision(Precision.EXACT) // 내부 스케일 최소화
                        .scale(Scale.FILL)          // 꽉 채우기(잘림 허용)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(KakaoAvatarShape),
                    contentScale = ContentScale.Crop
                )
            }
            imageRes != null -> {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(KakaoAvatarShape),
                    contentScale = ContentScale.Crop
                )
            }
            useIcon -> {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = contentDescription,
                    tint = BrandSurfaceLight
                )
            }
            else -> {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandSurfaceLight
                )
            }
        }
    }
}

/* ---------- helpers ---------- */

// grapheme 단위로 앞에서 n개 잘라 반환
private fun firstGraphemes(s: String, count: Int): String {
    if (s.isEmpty() || count <= 0) return ""
    val it = BreakIterator.getCharacterInstance()
    it.setText(s)
    var end = 0
    var taken = 0
    while (taken < count) {
        val next = it.next()
        if (next == BreakIterator.DONE) break
        end = next
        taken++
    }
    return if (end > 0) s.substring(0, end) else s
}

// 요구사항:
// - 영문/숫자 시작: 1 grapheme
// - 그 외(한글/이모지/기타 유니코드): 2 graphemes
// - 비어있으면 " "
private fun avatarLabel(name: String?): String {
    val n = name?.trim().orEmpty()
    if (n.isEmpty()) return " "
    val first = n.first()
    val isAsciiLetterOrDigit = (first in 'A'..'Z') || (first in 'a'..'z') || (first in '0'..'9')
    val takeCount = if (isAsciiLetterOrDigit) 1 else 1
    return firstGraphemes(n, takeCount).ifBlank { " " }
}
