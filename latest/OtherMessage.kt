@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.vmerp.works.ui.chat

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vmerp.works.model.ChatFile
import com.vmerp.works.model.ChatMember
import com.vmerp.works.model.ChatMessage
import com.vmerp.works.model.PressTarget
import com.vmerp.works.model.SelectMode
import com.vmerp.works.model.User
import com.vmerp.works.ui.chat.content.LikeArea
import com.vmerp.works.ui.chat.content.ProfileImg
import com.vmerp.works.ui.chat.content.ShareIcon
import com.vmerp.works.ui.chat.content.TimeBadge
import com.vmerp.works.ui.chat.content.UnreadBadge
import com.vmerp.works.ui.theme.LocalTalkTheme
import com.vmerp.works.util.*

private val ProfileSize = 36.dp
private val ProfileGap = 6.dp
private val LeftInsetForStacked = ProfileSize + ProfileGap

// 상대가 보낸 메시지
@Composable
fun OtherMessage(
    msg: ChatMessage,
    chtMemberList: List<ChatMember>,
    chtUnusedMemberList: List<ChatMember>,
    currentUser: User?,
    deleted: Boolean = false,
    highlight: String? = null,
    videoTalkId: String? = null,
    isDownloading: Boolean = false,
    downloadPercent: Int = 0,
    downloadReceived: Long = 0L,
    downloadTotal: Long = 0L,
    downloadingTalkId: String?,
    downloadingFileNo: String?,
    onPress: (ChatMessage, PressTarget, String?) -> Unit,
    onPressSaveButton: (mode: SelectMode, images: List<ChatFile>, currentIndex: Int?) -> Unit,
    onLongPress: (ChatMessage, PressTarget) -> Unit,
) {
    val i18n = LocalI18n.current
    val talkTheme = LocalTalkTheme.current
    val density = LocalDensity.current

    // 메시지 높이 측정
    var messageHeightPx by remember(msg.talkId) { mutableStateOf<Int?>(null) }
    val messageHeightDp = messageHeightPx?.let { with(density) { it.toDp() } }

    // sendDtm (보낸 시간), lastDtm (읽은 시간 = 마지막으로 입장한 시간) 비교해서 메시지 읽음 여부 구분
    val (readUsers, unreadUsers) =
        remember(chtMemberList, msg.sendDtm) {
            val read = mutableListOf<ChatMember>()
            val unread = mutableListOf<ChatMember>()

            chtMemberList.forEach { m ->
                if (isBeforeOrEqual(msg.sendDtm, m.lastReadDtm)) {
                    read += m
                } else {
                    unread += m
                }
            }
            read to unread
        }

    val unreadCnt = unreadUsers.size

    val unusedIdSet = remember(chtUnusedMemberList) { chtUnusedMemberList.map { it.userId }.toSet() }
    val actionSt = remember(msg) { msg.actionSt ?: "" }
    val editedLabel = i18n.tr("talk.isEdit", "수정됨")
    val isLink = msg.uiType() == UiMsgType.LINK
    val isEmoticon = msg.uiType() == UiMsgType.EMOTICON

    val images = msg.fileList?.filter { it.fileTy?.lowercase()?.startsWith("image/") == true }.orEmpty()
    val isSingleImage = images.isNotEmpty() && images.size == 1
    val showShareIcon = (!msg.fileList.isNullOrEmpty()) || !msg.fileNo.isNullOrBlank() || !msg.fileNm.isNullOrBlank() || !msg.fileTy.isNullOrBlank()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            // 프로필 이미지 영역
            ProfileImg(
                userName = msg.uiUserName(),
                imageUrl = msg.uiProfileUrl(),
                isUnused = unusedIdSet.contains(msg.userId ?: ""),
                actionSt = actionSt,
                sizeDp = ProfileSize.value.toInt(),
                onPress = { onPress?.invoke(msg, PressTarget.Profile, null) },
                onLongPress = { onLongPress?.invoke(msg, PressTarget.Profile) },
            )
            Spacer(Modifier.width(ProfileGap))

            Column(horizontalAlignment = Alignment.Start) {
                // 이름, 직급, 부서 영역
                Row {
                    // 이름
                    Text(
                        text = msg.uiUserName().orEmpty(),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                color = talkTheme.userName.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                    )
                    Spacer(Modifier.width(4.dp))
                    // 직급
                    Text(
                        text = msg.uiJobName().orEmpty(),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                color = talkTheme.userName.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                    )
                    // 부서
                    Text(
                        text = " / " + msg.uiDeptName().orEmpty(),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                color = talkTheme.userName.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                    )
                }

                Spacer(Modifier.height(6.dp))

                // 메시지, 시간 영역
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    // 단일 이미지인 경우 : 저장 버튼
                    if (isSingleImage) {
                        Column(
                            modifier =
                                Modifier.onSizeChanged { size ->
                                    messageHeightPx = size.height
                                },
                        ) {
                            Box(
                                modifier =
                                    Modifier.combinedClickable(
                                        indication = null, // 클릭 시 RippleEffect(회색 음영) 제거
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = { },
                                        onLongClick = { onLongPress?.invoke(msg, PressTarget.Message) },
                                    ),
                            ) {
                                MessageContent(
                                    msg = msg,
                                    isMe = false,
                                    deleted = deleted,
                                    unreadCnt = unreadCnt,
                                    highlight = highlight,
                                    videoTalkId = videoTalkId,
                                    isDownloading = isDownloading,
                                    downloadPercent = downloadPercent,
                                    downloadReceived = downloadReceived,
                                    downloadTotal = downloadTotal,
                                    downloadingTalkId = downloadingTalkId,
                                    downloadingFileNo = downloadingFileNo,
                                    onPress = onPress,
                                    onLongPress = onLongPress,
                                )
                            }
                            Text(
                                text = i18n.tr("common.save", "저장"),
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        color = talkTheme.dateTime,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                modifier =
                                    Modifier
                                        .align(Alignment.Start)
                                        .padding(top = 2.dp)
                                        .combinedClickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = {
                                                onPressSaveButton(SelectMode.SaveOne, images, null)
                                            },
                                            onLongClick = {},
                                        ),
                            )
                        }
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .onSizeChanged { size ->
                                        messageHeightPx = size.height
                                    }
                                    .combinedClickable(
                                        indication = null, // 클릭 시 RippleEffect(회색 음영) 제거
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = { },
                                        onLongClick = { onLongPress?.invoke(msg, PressTarget.Message) },
                                    ),
                        ) {
                            MessageContent(
                                msg = msg,
                                isMe = false,
                                deleted = deleted,
                                unreadCnt = unreadCnt,
                                highlight = highlight,
                                videoTalkId = videoTalkId,
                                isDownloading = isDownloading,
                                downloadPercent = downloadPercent,
                                downloadReceived = downloadReceived,
                                downloadTotal = downloadTotal,
                                downloadingTalkId = downloadingTalkId,
                                downloadingFileNo = downloadingFileNo,
                                onPress = onPress,
                                onLongPress = onLongPress,
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    Box(
                        modifier =
                            Modifier.heightIn(min = messageHeightDp ?: 40.dp).wrapContentWidth(),
                    ) {
                        if (showShareIcon) {
                            ShareIcon(
                                msg = msg,
                                onShare = { onPress(msg, PressTarget.Share, null) },
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }

                        Column(
                            modifier = Modifier.align(Alignment.BottomStart),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            if (unreadUsers.isNotEmpty() && !deleted && !isLink && !isEmoticon) {
                                Box(
                                    modifier =
                                        Modifier
                                            .combinedClickable(
                                                indication = null, // 클릭 시 RippleEffect(회색 음영) 제거
                                                interactionSource = remember { MutableInteractionSource() },
                                                onClick = { onPress?.invoke(msg, PressTarget.UnreadBadge, null) },
                                            )
                                            .align(Alignment.Start),
                                ) {
                                    UnreadBadge(
                                        count = unreadCnt,
                                        contentColor = talkTheme.unreadCount,
                                    )
                                }
                            }

                            if (!deleted && !isLink && !isEmoticon) {
                                TimeBadge(
                                    timeText = if (msg.uiIsEdited()) "($editedLabel) ${msg.uiTime()}" else msg.uiTime(),
                                    contentColor = talkTheme.dateTime, // 테마 반영
                                )
                            }
                        }
                    }
                }
            }
        }
        if (!msg.messageTxt.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            LikeArea(
                msg = msg,
                currentUser = currentUser,
                isMe = false,
                onPress = onPress,
            )
        }
    }
}
