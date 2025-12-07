@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class, // Compose Foundation 실험 API 사용 (combinedClickable 등)
    androidx.compose.material3.ExperimentalMaterial3Api::class, // Material3 실험 API 사용 (PullToRefresh 등)
)

package com.vmerp.works.ui.chat

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp
import com.vmerp.works.model.ChatMessage
import com.vmerp.works.model.PressTarget
import com.vmerp.works.ui.LocalImageViewer
import com.vmerp.works.ui.chat.content.*
import com.vmerp.works.ui.theme.LocalTalkTheme
import com.vmerp.works.util.*
import com.vmerp.works.util.LocalMeasuring
import kotlin.math.min

// 답장 표시하는 UI
@Composable
fun ReplyStack(
    modifier: Modifier = Modifier,
    widthRatio: Float = 0.85f, // 부모 폭의 85%를 최대 폭으로 제한
    isMe: Boolean,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        val safeRatio = widthRatio.coerceIn(0.3f, 0.95f)
        val maxLimit = (constraints.maxWidth * safeRatio).toInt()

        // 1차 측정: "내용 폭" 기준으로 재기 위해 measuring=true 로 하위에 알림
        val loose = constraints.copy(minWidth = 0, maxWidth = maxLimit)

        val topOnce =
            subcompose("topMeasure") {
                CompositionLocalProvider(LocalMeasuring provides true) { top() }
            }.map { it.measure(loose) }
        val bottomOnce =
            subcompose("bottomMeasure") {
                CompositionLocalProvider(LocalMeasuring provides true) { bottom() }
            }.map { it.measure(loose) }

        val topW = topOnce.maxOfOrNull { it.width } ?: 0
        val bottomW = bottomOnce.maxOfOrNull { it.width } ?: 0

        val contentWidth = maxOf(topW, bottomW).coerceAtMost(maxLimit)

        // 2차 배치: 동일 폭으로 고정(measuring=false)
        val fixed = loose.copy(minWidth = contentWidth, maxWidth = contentWidth)

        val topFixed =
            subcompose("topFixed") {
                CompositionLocalProvider(LocalMeasuring provides false) { top() }
            }.map { it.measure(fixed) }

        val bottomFixed =
            subcompose("bottomFixed") {
                CompositionLocalProvider(LocalMeasuring provides false) { bottom() }
            }.map { it.measure(fixed) }

        val height = topFixed.sumOf { it.height } + bottomFixed.sumOf { it.height }

        layout(contentWidth, height) {
            var y = 0
            topFixed.forEach {
                it.place(0, y)
                y += it.height
            }
            bottomFixed.forEach {
                it.place(0, y)
                y += it.height
            }
        }
    }
}

// 메시지 내용
@Composable
fun MessageContent(
    msg: ChatMessage,
    isMe: Boolean,
    deleted: Boolean,
    unreadCnt: Int = 0,
    highlight: String?,
    videoTalkId: String?,
    isDownloading: Boolean = false,
    downloadPercent: Int = 0,
    downloadReceived: Long = 0L,
    downloadTotal: Long = 0L,
    downloadingTalkId: String?,
    downloadingFileNo: String?,
    onPress: ((ChatMessage, PressTarget, String?) -> Unit)? = null,
    onLongPress: ((ChatMessage, PressTarget) -> Unit)? = null,
) {
    val i18n = LocalI18n.current
    val viewer = LocalImageViewer.current
    val talkTheme = LocalTalkTheme.current
    val text = if (deleted) i18n.tr("talk.deletedMessage", "삭제된 메시지입니다.") else msg.messageTxt.orEmpty()

    val editedLabel = i18n.tr("talk.isEdit", "수정됨")
    val timeText =
        if (msg.uiIsEdited()) "($editedLabel) ${msg.uiTime()}" else msg.uiTime()

    when (msg.uiType()) {
        // 일반 텍스트 메시지
        UiMsgType.TEXT -> {
            if (msg.reMessage != null && text.isNotBlank()) {
                // 답장으로 텍스트 보냈을 때
                ReplyStack(
                    isMe = isMe,
                    top = {
                        ReplyMessage(
                            target = msg.reMessage!!,
                            message = msg,
                            isMe = isMe,
                            onPress = { onPress?.invoke(msg, PressTarget.ReplyMessage, null) },
                        )
                    },
                    bottom = {
                        TextMessage(
                            text = text,
                            isMe = isMe,
                            deleted = deleted,
                            highlight = highlight,
                            onPress = { onPress?.invoke(msg, PressTarget.ReplyMessage, null) },
                            onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                            removeTopRadius = true,
                            matchParentWidth = true,
                        )
                    },
                )
            } else {
                // 일반 텍스트 메시지
                TextMessage(
                    text = text,
                    isMe = isMe,
                    deleted = deleted,
                    highlight = highlight,
                    onPress = { onPress?.invoke(msg, PressTarget.Message, null) },
                    onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                    removeTopRadius = false,
                    matchParentWidth = false,
                )
            }
        }

        // 이미지 메시지
        UiMsgType.IMAGE -> {
            val files =
                remember(msg.fileList) {
                    (msg.fileList ?: emptyList())
                        .filter { (it.fileTy ?: "").startsWith("image") }
                }

            val urls =
                remember(files) {
                    files.map { it.uiImgUrl() }
                }

            if (urls.isNotEmpty()) {
                ImageMessage(
                    urls = urls,
                    onThumbClick = { index ->
                        val safeIndex = min(index, urls.lastIndex).coerceAtLeast(0)
                        val opened = viewer?.openImages?.invoke(files, safeIndex) != null
                        if (!opened) onPress?.invoke(msg, PressTarget.Message, null) // 뷰어 미제공 시 상위 클릭으로 fallback
                    },
                    onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                )
            } else {
                // 이미지 URL이 전혀 없으면 텍스트로 fallback
                TextMessage(
                    text = text,
                    isMe = isMe,
                    deleted = deleted,
                    highlight = highlight,
                    onPress = { onPress?.invoke(msg, PressTarget.Message, null) },
                    onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                    removeTopRadius = false,
                    matchParentWidth = false,
                )
            }
        }

        // 파일 메시지
        UiMsgType.FILE -> {
            val fileNm = msg.uiFileNm()
            val fileSize = msg.uiFileSize()
            val fileNo = msg.uiFileNo()
            val url = msg.uiFileUrl()

            val isCurrentDownloading =
                isDownloading &&
                    downloadingTalkId != null &&
                    downloadingTalkId == msg.talkId && downloadingFileNo == fileNo

            val progress =
                if (isCurrentDownloading && downloadTotal > 0L) {
                    (downloadReceived.toFloat() / downloadTotal.toFloat())
                        .coerceIn(0f, 1f)
                } else {
                    0f
                }

            FileMessage(
                fileName = fileNm,
                fileSize = fileSize,
                url = url,
                isMe = isMe,
                downloadYn = msg.downloadYn, // RN: 다운로드 여부 뱃지 로직과 동일
                isDownloading = isCurrentDownloading,
                progress = progress,
                received = downloadReceived,
                total = downloadTotal,
                onPress = { onPress?.invoke(msg, PressTarget.File, null) },
                onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                onCancel = { onPress?.invoke(msg, PressTarget.Cancel, null) },
            )
        }

        // 이모티콘 메시지
        UiMsgType.EMOTICON -> {
            val hasEmoticon = msg.emoticonId != null && msg.emoticonFileNo != null
            val isEmoticonOnly = hasEmoticon && msg.emoticonId == msg.messageTxt
            val isEmoticonWithText = hasEmoticon && msg.emoticonId != msg.messageTxt
            val isReply = msg.reMessage != null

            val url = msg.uiEmoticonUrl()

            when {
                isEmoticonOnly && !isReply -> {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (isMe) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (unreadCnt > 0 && !deleted) {
                                    Box(
                                        modifier =
                                            Modifier.combinedClickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() },
                                                onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                            ),
                                    ) {
                                        UnreadBadge(
                                            count = unreadCnt,
                                            contentColor = talkTheme.unreadCount,
                                        )
                                    }
                                }
                                TimeBadge(
                                    timeText = timeText,
                                    contentColor = talkTheme.dateTime,
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.End,
                            ) {
                                EmoticonMessage(
                                    url = url,
                                    onPress = { onPress?.invoke(msg, PressTarget.Message, null) },
                                    onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                EmoticonMessage(
                                    url = url,
                                    onPress = { onPress?.invoke(msg, PressTarget.Message, null) },
                                    onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (unreadCnt > 0 && !deleted) {
                                    Box(
                                        modifier =
                                            Modifier.combinedClickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() },
                                                onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                            ),
                                    ) {
                                        UnreadBadge(
                                            count = unreadCnt,
                                            contentColor = talkTheme.unreadCount,
                                        )
                                    }
                                }
                                TimeBadge(
                                    timeText = timeText,
                                    contentColor = talkTheme.dateTime,
                                )
                            }
                        }
                    }
                }

                isEmoticonWithText && !isReply -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                    ) {
                        EmoticonMessage(
                            url = url,
                            onPress = { onPress?.invoke(msg, PressTarget.Message, null) },
                            onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                        )

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (isMe) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    if (unreadCnt > 0 && !deleted) {
                                        Box(
                                            modifier =
                                                Modifier.combinedClickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                                ),
                                        ) {
                                            UnreadBadge(
                                                count = unreadCnt,
                                                contentColor = talkTheme.unreadCount,
                                            )
                                        }
                                    }

                                    TimeBadge(
                                        timeText = timeText,
                                        contentColor = talkTheme.dateTime,
                                    )
                                }

                                TextMessage(
                                    text = text,
                                    isMe = isMe,
                                    deleted = deleted,
                                    highlight = highlight,
                                    onPress = null,
                                    onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                                    removeTopRadius = false,
                                    matchParentWidth = false,
                                    maxWidthRatio = 0.6f,
                                )
                            } else {
                                TextMessage(
                                    text = text,
                                    isMe = isMe,
                                    deleted = deleted,
                                    highlight = highlight,
                                    onPress = null,
                                    onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                                    removeTopRadius = false,
                                    matchParentWidth = false,
                                    maxWidthRatio = 0.6f,
                                )

                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    if (unreadCnt > 0 && !deleted) {
                                        Box(
                                            modifier =
                                                Modifier.combinedClickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                                ),
                                        ) {
                                            UnreadBadge(
                                                count = unreadCnt,
                                                contentColor = talkTheme.unreadCount,
                                            )
                                        }
                                    }

                                    TimeBadge(
                                        timeText = timeText,
                                        contentColor = talkTheme.dateTime,
                                    )
                                }
                            }
                        }
                    }
                }

                isEmoticonOnly && isReply -> {
                    ReplyStack(
                        isMe = isMe,
                        top = {
                            ReplyMessage(
                                target = msg.reMessage!!,
                                message = msg,
                                isMe = isMe,
                                onPress = { onPress?.invoke(msg, PressTarget.ReplyMessage, null) },
                            )
                        },
                        bottom = {
                            val measuring = LocalMeasuring.current

                            Box(
                                modifier =
                                    if (measuring) {
                                        // 폭 측정 단계: 내용 폭만 재도록
                                        Modifier
                                    } else {
                                        // 실제 배치 단계: ReplyStack이 정한 contentWidth 전체 사용
                                        Modifier.fillMaxWidth()
                                    },
                                contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (isMe) {
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            if (unreadCnt > 0 && !deleted) {
                                                Box(
                                                    modifier =
                                                        Modifier.combinedClickable(
                                                            indication = null,
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                                        ),
                                                ) {
                                                    UnreadBadge(
                                                        count = unreadCnt,
                                                        contentColor = talkTheme.unreadCount,
                                                    )
                                                }
                                            }

                                            TimeBadge(
                                                timeText = timeText,
                                                contentColor = talkTheme.dateTime,
                                            )
                                        }
                                        EmoticonMessage(
                                            url = url,
                                            onPress = { onPress?.invoke(msg, PressTarget.Message, null) },
                                            onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                                        )
                                    } else {
                                        EmoticonMessage(
                                            url = url,
                                            onPress = { onPress?.invoke(msg, PressTarget.Message, null) },
                                            onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.Start,
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            if (unreadCnt > 0 && !deleted) {
                                                Box(
                                                    modifier =
                                                        Modifier.combinedClickable(
                                                            indication = null,
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                                        ),
                                                ) {
                                                    UnreadBadge(
                                                        count = unreadCnt,
                                                        contentColor = talkTheme.unreadCount,
                                                    )
                                                }
                                            }

                                            TimeBadge(
                                                timeText = timeText,
                                                contentColor = talkTheme.dateTime,
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    )
                }

                isEmoticonWithText && isReply -> {
                    ReplyStack(
                        isMe = isMe,
                        top = {
                            ReplyMessage(
                                target = msg.reMessage!!,
                                message = msg,
                                isMe = isMe,
                                onPress = { onPress?.invoke(msg, PressTarget.ReplyMessage, null) },
                            )
                        },
                        bottom = {
                            val measuring = LocalMeasuring.current

                            Column(
                                modifier =
                                    if (measuring) {
                                        Modifier
                                    } else {
                                        Modifier.fillMaxWidth()
                                    },
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (isMe) {
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            if (unreadCnt > 0 && !deleted) {
                                                Box(
                                                    modifier =
                                                        Modifier.combinedClickable(
                                                            indication = null,
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                                        ),
                                                ) {
                                                    UnreadBadge(
                                                        count = unreadCnt,
                                                        contentColor = talkTheme.unreadCount,
                                                    )
                                                }
                                            }

                                            TimeBadge(
                                                timeText = timeText,
                                                contentColor = talkTheme.dateTime,
                                            )
                                        }
                                        EmoticonMessage(
                                            url = url,
                                            onPress = { onPress?.invoke(msg, PressTarget.Message, null) },
                                            onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                                        )
                                    } else {
                                        EmoticonMessage(
                                            url = url,
                                            onPress = { onPress?.invoke(msg, PressTarget.Message, null) },
                                            onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.Start,
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            if (unreadCnt > 0 && !deleted) {
                                                Box(
                                                    modifier =
                                                        Modifier.combinedClickable(
                                                            indication = null,
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                                        ),
                                                ) {
                                                    UnreadBadge(
                                                        count = unreadCnt,
                                                        contentColor = talkTheme.unreadCount,
                                                    )
                                                }
                                            }

                                            TimeBadge(
                                                timeText = timeText,
                                                contentColor = talkTheme.dateTime,
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }

        // 링크 메시지
        UiMsgType.LINK -> {
            val ln = msg.ln
            val hasText = text.isNotBlank()

            if (msg.reMessage != null) {
                // 답장으로 링크 포함된 메시지 보냈을 때
                ReplyStack(
                    isMe = isMe,
                    top = {
                        ReplyMessage(
                            target = msg.reMessage!!,
                            message = msg,
                            isMe = isMe,
                            onPress = { onPress?.invoke(msg, PressTarget.ReplyMessage, null) },
                        )
                    },
                    bottom = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                        ) {
                            if (hasText) {
                                Box(
                                    modifier =
                                        Modifier.combinedClickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = { onPress?.invoke(msg, PressTarget.Link, null) },
                                            onLongClick = { onLongPress?.invoke(msg, PressTarget.Message) },
                                        ),
                                ) {
                                    TextMessage(
                                        text = text,
                                        isMe = isMe,
                                        deleted = deleted,
                                        highlight = highlight,
                                        onPress = null,
                                        onLongPress = null,
                                        removeTopRadius = true,
                                        matchParentWidth = true,
                                        maxWidthRatio = 0.85f,
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (isMe) {
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        if (unreadCnt > 0 && !deleted) {
                                            Box(
                                                modifier =
                                                    Modifier.combinedClickable(
                                                        indication = null,
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                                    ),
                                            ) {
                                                UnreadBadge(
                                                    count = unreadCnt,
                                                    contentColor = talkTheme.unreadCount,
                                                )
                                            }
                                        }
                                        TimeBadge(
                                            timeText = timeText,
                                            contentColor = talkTheme.dateTime,
                                        )
                                    }

                                    LinkMessage(
                                        title = ln?.title,
                                        description = ln?.description,
                                        imageUrl = ln?.image, // ""도 null과 동일 취급
                                        onPress = { onPress?.invoke(msg, PressTarget.Link, null) },
                                        onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                                    )
                                } else {
                                    LinkMessage(
                                        title = ln?.title,
                                        description = ln?.description,
                                        imageUrl = ln?.image, // ""도 null과 동일 취급
                                        onPress = { onPress?.invoke(msg, PressTarget.Link, null) },
                                        onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                                    )

                                    Column(
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        if (unreadCnt > 0 && !deleted) {
                                            Box(
                                                modifier =
                                                    Modifier.combinedClickable(
                                                        indication = null,
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                                    ),
                                            ) {
                                                UnreadBadge(
                                                    count = unreadCnt,
                                                    contentColor = talkTheme.unreadCount,
                                                )
                                            }
                                        }

                                        TimeBadge(
                                            timeText = timeText,
                                            contentColor = talkTheme.dateTime,
                                        )
                                    }
                                }
                            }
                        }
                    },
                )
            } else {
                // 답장 없는 일반 링크 메시지
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                ) {
                    if (hasText) {
                        Box(
                            modifier =
                                Modifier.combinedClickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = { onPress?.invoke(msg, PressTarget.Link, null) },
                                    onLongClick = { onLongPress?.invoke(msg, PressTarget.Message) },
                                ),
                        ) {
                            TextMessage(
                                text = text,
                                isMe = isMe,
                                deleted = deleted,
                                highlight = highlight,
                                onPress = null,
                                onLongPress = null,
                                removeTopRadius = false,
                                matchParentWidth = false,
                                maxWidthRatio = 0.85f,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (isMe) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (unreadCnt > 0 && !deleted) {
                                    Box(
                                        modifier =
                                            Modifier.combinedClickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() },
                                                onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                            ),
                                    ) {
                                        UnreadBadge(
                                            count = unreadCnt,
                                            contentColor = talkTheme.unreadCount,
                                        )
                                    }
                                }
                                TimeBadge(
                                    timeText = timeText,
                                    contentColor = talkTheme.dateTime,
                                )
                            }

                            LinkMessage(
                                title = ln?.title,
                                description = ln?.description,
                                imageUrl = ln?.image, // ""도 null과 동일 취급
                                onPress = { onPress?.invoke(msg, PressTarget.Link, null) },
                                onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                            )
                        } else {
                            LinkMessage(
                                title = ln?.title,
                                description = ln?.description,
                                imageUrl = ln?.image, // ""도 null과 동일 취급
                                onPress = { onPress?.invoke(msg, PressTarget.Link, null) },
                                onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                            )

                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (unreadCnt > 0 && !deleted) {
                                    Box(
                                        modifier =
                                            Modifier.combinedClickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() },
                                                onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                            ),
                                    ) {
                                        UnreadBadge(
                                            count = unreadCnt,
                                            contentColor = talkTheme.unreadCount,
                                        )
                                    }
                                }

                                TimeBadge(
                                    timeText = timeText,
                                    contentColor = talkTheme.dateTime,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 영상 메시지
        UiMsgType.VIDEO -> {
            val fileNo = msg.uiFileNo()
            val url = msg.uiVideoUrl()

            val isCurrentDownloading =
                isDownloading &&
                    downloadingTalkId != null &&
                    downloadingTalkId == msg.talkId && downloadingFileNo == fileNo

            val progress =
                if (isCurrentDownloading && downloadTotal > 0L) {
                    (downloadReceived.toFloat() / downloadTotal.toFloat())
                        .coerceIn(0f, 1f)
                } else {
                    0f
                }
            
            val willPlay = (videoTalkId == msg.talkId && !isDownloading) 

            if (msg.reMessage != null) {
                // 답장으로 영상 보냈을 때
                ReplyStack(
                    isMe = isMe,
                    top = {
                        ReplyMessage(
                            target = msg.reMessage,
                            message = msg,
                            isMe = isMe,
                            onPress = { onPress?.invoke(msg, PressTarget.ReplyMessage, null) },
                        )
                    },
                    bottom = {
                        VideoMessage(
                            url = url,
                            willPlay = willPlay,
                            isDownloading = isCurrentDownloading,
                            onPress = { onPress?.invoke(msg, PressTarget.Video, null) },
                            onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                        )
                    },
                )
            } else {
                // 일반 영상 메시지
                VideoMessage(
                    url = url,
                    willPlay = willPlay,
                    isDownloading = isCurrentDownloading,
                    onPress = { onPress?.invoke(msg, PressTarget.Video, null) },
                    onLongPress = { onLongPress?.invoke(msg, PressTarget.Message) },
                )
            }
        }

        // 시스템 메시지
        UiMsgType.SYSTEM -> {
            AdminComment(
                text = msg.uiText(),
                contentColor = talkTheme.dateTime,
            )
        }

        // 삭제된 메시지
        UiMsgType.DELETED -> {
            TextMessage(
                text = text,
                isMe = isMe,
                deleted = true,
                highlight = null,
                onPress = null,
                onLongPress = null,
                removeTopRadius = false,
                matchParentWidth = false,
            )
        }
    }
}
