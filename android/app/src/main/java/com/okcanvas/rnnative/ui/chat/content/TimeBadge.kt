package com.okcanvas.rnnative.ui.chat.room.content

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.okcanvas.rnnative.ui.theme.BrandOnSurfaceLight

/**
 * 말풍선 근처에 붙는 시간 표시용 텍스트 배지.
 *
 * ex) TimeBadge(timeText = "오후 6:12")
 */
@Composable
fun TimeBadge(
    timeText: String,
    modifier: Modifier = Modifier,
    contentColor: androidx.compose.ui.graphics.Color = BrandOnSurfaceLight.copy(alpha = 0.6f)
) {
    if (timeText.isBlank()) return
    Text(
        text = timeText,
        color = contentColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 12.sp,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.padding(vertical = 2.dp)
    )
}
