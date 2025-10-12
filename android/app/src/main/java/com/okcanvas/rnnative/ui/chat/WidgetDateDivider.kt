package com.okcanvas.rnnative.ui.chat.room

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.okcanvas.rnnative.ui.theme.BrandOnSurfaceLight
import com.okcanvas.rnnative.ui.theme.BrandSurfaceLight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 날짜 구분선 위젯 (ex. "2025년 10월 05일 일요일")
 */
@Composable
fun WidgetDateDivider(dateIso: String) {
    val label = try {
        val d = LocalDate.parse(dateIso)
        val fmt = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 EEEE", Locale.KOREAN)
        d.format(fmt)
    } catch (_: Throwable) {
        dateIso
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = BrandOnSurfaceLight.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = BrandOnSurfaceLight.copy(alpha = 0.8f)
            )
        }
    }
}

@Preview(
    name = "DateDivider Light",
    showBackground = true,
    backgroundColor = 0xFFF7F7F7
)
@Composable
private fun Preview_DateDivider_Light() {
    MaterialTheme {
        WidgetDateDivider(dateIso = "2025-10-05")
    }
}

@Preview(
    name = "DateDivider Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun Preview_DateDivider_Dark() {
    MaterialTheme {
        WidgetDateDivider(dateIso = "2025-10-05")
    }
}
