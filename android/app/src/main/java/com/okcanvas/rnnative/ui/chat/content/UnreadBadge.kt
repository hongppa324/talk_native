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
 * 읽지 않은 메시지 개수를 표시하는 작은 배지.
 *
 * ex) UnreadBadge(count = 3)
 */
@Composable
fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier,
    contentColor: androidx.compose.ui.graphics.Color = BrandOnSurfaceLight.copy(alpha = 0.7f)
) {
    if (count <= 0) return
    Text(
        text = count.toString(),
        color = contentColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.padding(vertical = 2.dp)
    )
}
