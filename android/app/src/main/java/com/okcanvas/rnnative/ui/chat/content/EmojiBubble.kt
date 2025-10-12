package com.okcanvas.rnnative.ui.chat.room.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.okcanvas.rnnative.ui.theme.BrandOnSurfaceLight
import com.okcanvas.rnnative.ui.theme.BrandSurfaceLight
import com.okcanvas.rnnative.ui.theme.PlaceholderBorder
import com.okcanvas.rnnative.ui.theme.PlaceholderBg

/**
 * 이모지 버블 — 예: 😀 12
 *
 * - 이모지를 동그란 칩에 담고, 오른쪽에 숫자 배지 표시
 * - 숫자가 0 이하이거나 null이면 배지는 숨김
 */
@Composable
fun EmojiBubble(
    emoji: String,
    count: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    // 스타일 파라미터
    bubbleSize: Int = 28, // 이모지 원형 버블 지름(dp)
    horizontalPadding: Int = 6,
    backgroundColor: androidx.compose.ui.graphics.Color = BrandSurfaceLight.copy(alpha = 0.95f),
    emojiColor: androidx.compose.ui.graphics.Color = BrandOnSurfaceLight,
    badgeBgColor: androidx.compose.ui.graphics.Color = PlaceholderBg,
    badgeBorderColor: androidx.compose.ui.graphics.Color = PlaceholderBorder,
    badgeTextColor: androidx.compose.ui.graphics.Color = BrandOnSurfaceLight
) {
    val showCount = (count ?: 0) > 0
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = horizontalPadding.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 이모지 원형 버블
        Box(
            modifier = Modifier
                .size(bubbleSize.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .padding(0.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                color = emojiColor,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 16.sp, // 이모지 가독성 위해 고정
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }

        if (showCount) {
            Spacer(Modifier.width(6.dp))
            // 숫자 배지 (작은 pill)
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeBgColor)
//                    .border(width = 0.5.dp, color = badgeBorderColor, shape = RoundedCornerShape(999.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatCount(count!!),
                    color = badgeTextColor,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

/** 1.2K, 3.4M 같은 축약 표기 */
private fun formatCount(n: Int): String {
    if (n < 1000) return n.toString()
    val units = arrayOf("", "K", "M", "B")
    var value = n.toDouble()
    var unit = 0
    while (value >= 1000 && unit < units.lastIndex) {
        value /= 1000.0
        unit++
    }
    // 소수 첫째 자리까지, 정수면 .0 제거
    val s = String.format("%.1f", value).trimEnd('0').trimEnd('.')
    return "$s${units[unit]}"
}
