@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.vmerp.works.ui.chat.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vmerp.works.R // res/drawable에 접근
import com.vmerp.works.util.*

// 파일 메시지
@Composable
fun FileMessage(
    fileName: String?,
    fileSize: String?,
    url: String?, // 서버 다운로드/뷰어 URL
    isMe: Boolean,
    downloadYn: String?, // "Y" | "N" | null
    isDownloading: Boolean = false,
    progress: Float = 0f,
    received: Long = 0L,
    total: Long = 0L,
    onPress: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    maxWidthRatio: Float = 0.85f,
    reserveRightDp: Dp = 56.dp,
    minBubbleWidth: Dp = 180.dp,
) {
    val i18n = LocalI18n.current
    var showDialog by remember { mutableStateOf(false) }

    val bgColor = Color(0xFFFFFFFF)
    val beforeDownload = !isMe && downloadYn != "Y"

    val fileLabel = i18n.tr("common.file.file", "파일")
    val sizeLabel = i18n.tr("common.file.size", "용량 :")

    BoxWithConstraints {
        val safeRatio = maxWidthRatio.coerceIn(0.3f, 0.95f)
        val ratioMax = this.maxWidth * safeRatio
        val max = (ratioMax - reserveRightDp).coerceAtLeast(minBubbleWidth)
        val min = minBubbleWidth.coerceAtMost(max)

        Box(
            modifier =
                Modifier
                    .widthIn(min = min, max = max)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .combinedClickable(
                        indication = null, // 클릭 시 RippleEffect(회색 음영) 제거
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onPress?.invoke() },
                        onLongClick = { onLongPress?.invoke() },
                    )
                    .padding(10.dp)
                    .widthIn(min = 180.dp, max = 320.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (beforeDownload) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(hex("#EEF1F6")),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.talk_download),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else if (isDownloading) {
                    ProgressCircle(
                        progress = progress,
                        size = 40.dp,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .combinedClickable(
                                        onClick = {
                                            if (isDownloading) {
                                                onCancel?.invoke()
                                            }
                                        },
                                    )
                                    .background(hex("#EEF1F6")),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.talk_file_on_download),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                } else {
                    // 변환해 두신 벡터(XML) 아이콘을 확장자로 선택해서 표시
                    FileIcon(fileNm = fileName.orEmpty(), sizeDp = 40)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName?.ifBlank { fileLabel } ?: fileLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!fileSize.isNullOrBlank()) {
                        Text(
                            text = "$sizeLabel $fileSize",
                            style =
                                MaterialTheme.typography.bodySmall.copy(
                                    color = hex("#8D99A8"), // CoolGray400
                                ),
                        )
                    }
                }
            }
        }
    }
}
