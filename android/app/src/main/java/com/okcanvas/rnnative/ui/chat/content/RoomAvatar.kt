// app/src/main/java/com/okcanvas/rnnative/ui/chat/room/RoomAvatar.kt
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.okcanvas.rnnative.ui.chat.room

import android.icu.text.BreakIterator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.okcanvas.rnnative.model.Participant
import com.okcanvas.rnnative.ui.theme.*
import java.util.Locale

// Kakao squircle shape (AvatarBubble과 동일)
private val KakaoAvatarShape = SquircleShape(n = 3.0f)

/**
 * 참여자 수에 따라 룸 아바타를 다르게 렌더링:
 * - 0명  : 이니셜 플레이스홀더
 * - 1명  : 단일 아바타
 * - 2명  : 좌상단/우하단 60~65% 대각 배치
 * - 3명  : 상단 중앙 1개 + 하단 좌/우 2개
 * - 4명  : 2x2 그리드
 * - 5명+ : 2x2 그리드 + 우하단 "+n"
 */
@Composable
fun RoomAvatar(
    participants: List<Participant>,
    size: Int = 52
) {
    val count = participants.size
    when {
        count <= 0     -> SingleInitialSquircle(display = " ", size = size)
        count == 1     -> SingleNetworkSquircle(participants[0], size = size)
        count == 2     -> TwoDiagonalSquircle(participants.take(2), size = size)
        count == 3     -> ThreeStackedSquircle(participants.take(3), size = size)
        count == 4     -> Grid2x2Squircle(participants.take(4), size = size)
        else           -> Grid2x2MoreSquircle(participants.take(4), more = count - 4, size = size)
    }
}

/* -------------------- 2명: 60~65% 대각 -------------------- */

@Composable
private fun TwoDiagonalSquircle(ps: List<Participant>, size: Int) {
    val child = (size * 0.65f).toInt()

    Box(
        modifier = Modifier.size(size.dp)
        // .clip(KakaoAvatarShape) // 필요 시 외곽 스쿼클 적용
    ) {
        // 첫번째: 좌측 상단
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            SingleNetworkSquircle(
                p = ps.getOrNull(0) ?: dummyP(),
                size = child,
                withBorder = false
            )
        }
        // 두번째: 우측 하단
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            SingleNetworkSquircle(
                p = ps.getOrNull(1) ?: dummyP(),
                size = child,
                withBorder = false
            )
        }
    }
}

/* -------------------- 3명: 상단 중앙 + 하단 좌/우 -------------------- */

@Composable
private fun ThreeStackedSquircle(ps: List<Participant>, size: Int) {
    val gap = 2.dp
    val cell = ((size.dp - gap) / 2f) // 하단 좌/우 셀 크기
    val topCell = cell                // 상단도 같은 높이/너비

    Box(
        modifier = Modifier.size(size.dp)
        // .clip(KakaoAvatarShape)
    ) {
        // 상단 중앙 1개
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(topCell)
        ) {
            SingleNetworkSquircle(
                p = ps.getOrNull(0) ?: dummyP(),
                size = topCell.value.toInt(),
                withBorder = false
            )
        }

        // 하단 좌/우 2개
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(size.dp)
                .height(cell),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(Modifier.size(cell)) {
                SingleNetworkSquircle(
                    p = ps.getOrNull(1) ?: dummyP(),
                    size = cell.value.toInt(),
                    withBorder = false
                )
            }
            Spacer(Modifier.width(gap))
            Box(Modifier.size(cell)) {
                SingleNetworkSquircle(
                    p = ps.getOrNull(2) ?: dummyP(),
                    size = cell.value.toInt(),
                    withBorder = false
                )
            }
        }
    }
}

/* -------------------- Building Blocks -------------------- */

@Composable
private fun SingleNetworkSquircle(
    p: Participant,
    size: Int,
    withBorder: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { size.dp.roundToPx() }
    val isPreview = LocalInspectionMode.current
    val label = avatarLabel(p.name)

    val url = p.avatarUrl?.trim().orEmpty()
    val hasUrl = url.isNotEmpty()

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(KakaoAvatarShape)
            .background(AvatarBlue)
            .then(if (withBorder) Modifier.border(0.75.dp, AvatarBorder, KakaoAvatarShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // 1) 항상 이니셜을 바닥에 렌더링 (이미지 실패 시 그대로 보임)
        androidx.compose.material3.Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = BrandSurfaceLight
        )

        // 2) URL이 있을 때만 이미지 시도 (성공하면 위에서 덮음, 실패하면 아무것도 안 그려짐)
        if (!isPreview && hasUrl) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .size(sizePx, sizePx)
                    .precision(Precision.EXACT)
                    .scale(Scale.FILL)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "room avatar",
                modifier = Modifier
                    .matchParentSize()
                    .clip(KakaoAvatarShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}


@Composable
private fun SingleInitialSquircle(display: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(KakaoAvatarShape)
            .background(AvatarBlue)
            .border(0.75.dp, AvatarBorder, KakaoAvatarShape),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = display.ifBlank { "E" },
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = BrandSurfaceLight
        )
    }
}

@Composable
private fun Grid2x2Squircle(ps: List<Participant>, size: Int) {
    val gap = 2.dp
    val cell = ((size.dp - gap) / 2f)

    Column(modifier = Modifier.size(size.dp)) {
        Row(Modifier.weight(1f)) {
            GridCell(ps.getOrNull(0), cell)
            Spacer(Modifier.width(gap))
            GridCell(ps.getOrNull(1), cell)
        }
        Spacer(Modifier.height(gap))
        Row(Modifier.weight(1f)) {
            GridCell(ps.getOrNull(2), cell)
            Spacer(Modifier.width(gap))
            GridCell(ps.getOrNull(3), cell)
        }
    }
}

@Composable
private fun Grid2x2MoreSquircle(ps: List<Participant>, more: Int, size: Int) {
    val gap = 2.dp
    val cell = ((size.dp - gap) / 2f)

    Column(modifier = Modifier.size(size.dp)) {
        Row(Modifier.weight(1f)) {
            GridCell(ps.getOrNull(0), cell)
            Spacer(Modifier.width(gap))
            GridCell(ps.getOrNull(1), cell)
        }
        Spacer(Modifier.height(gap))
        Row(Modifier.weight(1f)) {
            GridCell(ps.getOrNull(2), cell)
            Spacer(Modifier.width(gap))
            Box(
                modifier = Modifier
                    .size(cell)
                    .clip(KakaoAvatarShape)
                    .background(AvatarBlue)
                    .border(0.75.dp, AvatarBorder, KakaoAvatarShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = "+$more",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = BrandSurfaceLight
                )
            }
        }
    }
}

@Composable
private fun GridCell(p: Participant?, size: androidx.compose.ui.unit.Dp) {
    if (p == null) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(KakaoAvatarShape)
                .background(AvatarBlue)
                .border(0.75.dp, AvatarBorder, KakaoAvatarShape)
        )
        return
    }
    SingleNetworkSquircle(p, size = size.value.toInt())
}

/* -------------------- Helpers -------------------- */

// grapheme 단위로 앞에서 n개 잘라 반환 (Locale 명시 + 폴백)
private fun firstGraphemes(s: String, count: Int): String {
    if (s.isEmpty() || count <= 0) return ""
    val it = BreakIterator.getCharacterInstance(Locale.getDefault())
    it.setText(s)
    var end = 0
    var taken = 0
    while (taken < count) {
        val next = it.next()
        if (next == BreakIterator.DONE) break
        end = next
        taken++
    }
    // 폴백: BreakIterator가 실패하면 length 기반 최소 보장
    return when {
        end > 0 -> s.substring(0, end)
        else -> s.substring(0, minOf(count, s.length))
    }
}

// ASCII 영문/숫자 여부를 "코드포인트"로 판정 (이모지/서러게이트 대비)
private fun isAsciiLetterOrDigit(cp: Int): Boolean {
    return (cp in 0x30..0x39) || (cp in 0x41..0x5A) || (cp in 0x61..0x7A)
}

/** 아바타 이니셜 규칙
 * - 영문/숫자 시작: 1 grapheme
 * - 그 외(한글/이모지/기타): 2 graphemes
 * - 비어있으면 " "
 */
private fun avatarLabel(name: String?): String {
    val n = name?.trim().orEmpty()
    if (n.isEmpty()) return "E"
    val cp = Character.codePointAt(n, 0)
    val takeCount = if (isAsciiLetterOrDigit(cp)) 1 else 1
    return firstGraphemes(n, takeCount).ifBlank { "E" }
}

private fun dummyP() = Participant(id = "dummy", name = "D")
