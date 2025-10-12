package com.okcanvas.rnnative.ui.chat.room.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.okcanvas.rnnative.ui.theme.BrandSurfaceLight
import com.okcanvas.rnnative.ui.theme.BrandOnSurfaceLight

/**
 * 액션 아이콘(버튼) — 예: 메시지 복사, 재생, 삭제 등
 */
@Composable
fun ActionIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: androidx.compose.ui.graphics.Color = BrandSurfaceLight.copy(alpha = 0.95f),
    contentColor: androidx.compose.ui.graphics.Color = BrandOnSurfaceLight,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // content() 내부에서 Icon 등 직접 렌더링 시 contentColor 사용 가능
            content()
        }
    }
}
