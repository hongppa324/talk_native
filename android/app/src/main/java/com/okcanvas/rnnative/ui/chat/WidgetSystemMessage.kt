// app/src/main/java/com/example/kakaostyle/ui/chat/room/WidgetSystemMessage.kt
package com.okcanvas.rnnative.ui.chat.room

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.okcanvas.rnnative.ui.theme.BrandOnSurfaceLight

@Composable
fun WidgetSystemMessage(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = BrandOnSurfaceLight.copy(alpha = 0.10f), // 살짝 투명
    contentColor: androidx.compose.ui.graphics.Color = BrandOnSurfaceLight.copy(alpha = 0.80f),  // 읽기 좋은 톤
    shape: RoundedCornerShape = RoundedCornerShape(6.dp)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(containerColor, shape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* -------------------- Previews -------------------- */

@Preview(
    name = "SystemMessage - Light",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5
)
@Composable
private fun Preview_WidgetSystemMessage_Light() {
    MaterialTheme {
        WidgetSystemMessage(
            text = "상대방이 채팅방에 초대되었습니다."
        )
    }
}

@Preview(
    name = "SystemMessage - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun Preview_WidgetSystemMessage_Dark() {
    MaterialTheme {
        WidgetSystemMessage(
            text = "대화방 이름이 변경되었습니다.",
            containerColor = BrandOnSurfaceLight.copy(alpha = 0.18f),
            contentColor = BrandOnSurfaceLight.copy(alpha = 0.90f)
        )
    }
}

/* 옵션: 살짝 더 둥글고 강조된 스타일 */
@Preview(
    name = "SystemMessage - Rounded & Emphasized",
    showBackground = true,
    backgroundColor = 0xFFF0F0F0
)
@Composable
private fun Preview_WidgetSystemMessage_Rounded() {
    MaterialTheme {
        WidgetSystemMessage(
            text = "관리자가 공지를 등록했습니다.",
            containerColor = BrandOnSurfaceLight.copy(alpha = 0.12f),
            contentColor = BrandOnSurfaceLight.copy(alpha = 0.85f),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
