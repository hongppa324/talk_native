@file:OptIn(
  androidx.compose.foundation.ExperimentalFoundationApi::class,
  androidx.compose.material3.ExperimentalMaterial3Api::class
)


package com.okcanvas.rnnative.ui.chat.room

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.okcanvas.rnnative.model.ChatMessage
import com.okcanvas.rnnative.model.MessageType
import com.okcanvas.rnnative.model.MsgTapTarget
import com.okcanvas.rnnative.ui.chat.room.content.UnreadBadge
import com.okcanvas.rnnative.ui.chat.room.content.TimeBadge
import com.okcanvas.rnnative.ui.chat.room.content.ActionIcon
import com.okcanvas.rnnative.ui.theme.BrandOnSurfaceLight

private val AvatarSize = 40.dp
private val AvatarGap = 8.dp
private val LeftInsetForStacked = AvatarSize + AvatarGap

@Composable
fun WidgetRecvMessage(
    msg: ChatMessage,
    showAvatarAndName: Boolean,
    showTime: Boolean,
    highlight: String? = null,
    onClick: (ChatMessage, MsgTapTarget) -> Unit = { _, _ -> },
    onLongPress: (ChatMessage, MsgTapTarget) -> Unit = { _, _ -> }
) {
    val unread = msg.unreadCount?.takeIf { it > 0 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (showAvatarAndName) {
            AvatarBubble(
                displayName = msg.displayName,
                imageUrl = msg.avatarUrl,
                sizeDp = 40,
                onClick = { onClick(msg, MsgTapTarget.Avatar) },
                onLongPress = { onLongPress(msg, MsgTapTarget.Avatar) }
            )
            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.Start) {
                msg.displayName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BrandOnSurfaceLight.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    // MessageContent(
                    //     msg = msg,
                    //     isMe = false,
                    //     highlight = highlight,
                    //     onClick = { onClick(msg, MsgTapTarget.Content) },
                    //     onLongPress = { onLongPress(msg, MsgTapTarget.Content) }
                    // )
                    Box(
                      modifier = Modifier.combinedClickable(
                        onClick = { onClick(msg, MsgTapTarget.Content) },
                        onLongClick = { onLongPress(msg, MsgTapTarget.Content) }
                      )
                    ) {
                      MessageContent(
                        msg = msg,
                        isMe = false,
                        highlight = highlight,
                        onClick = { },          // 자식 쪽 이벤트 비활성화 (중복 방지)
                        onLongPress = { }
                      )
                    }

                    unread?.let {
                        Spacer(Modifier.width(6.dp))
                        UnreadBadge(it)
                    }

                    // if (msg.type != MessageType.TEXT) {
                    //     Spacer(Modifier.width(6.dp))
                    //     ActionIcon(onClick = { /* TODO: forward */ }) {
                    //         Icon(
                    //             imageVector = Icons.AutoMirrored.Rounded.Forward,
                    //             contentDescription = "전달"
                    //         )
                    //     }
                    // }
                }

                if (showTime || !msg.reactions.isNullOrEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showTime) {
                            TimeBadge(
                                timeText = msg.time,
                                contentColor = BrandOnSurfaceLight.copy(alpha = 0.6f)
                            )
                        }

                        // ✅ 이모지 리액션 표시
                        msg.reactions?.takeIf { it.isNotEmpty() }?.let { list ->
                            Spacer(Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                list.forEach { reaction ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = reaction.emoji,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = reaction.count.toString(),
                                            fontSize = 10.sp,
                                            color = BrandOnSurfaceLight.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return
        } else {
            Spacer(Modifier.width(LeftInsetForStacked))
        }

        Column(horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.Bottom) {
                // MessageContent(
                //     msg = msg,
                //     isMe = false,
                //     highlight = highlight,
                //     onClick = { onClick(msg, MsgTapTarget.Content) },
                //     onLongPress = { onLongPress(msg, MsgTapTarget.Content) }
                // )
                Box(
                  modifier = Modifier.combinedClickable(
                    onClick = { onClick(msg, MsgTapTarget.Content) },
                    onLongClick = { onLongPress(msg, MsgTapTarget.Content) }
                  )
                ) {
                  MessageContent(
                    msg = msg,
                    isMe = false,
                    highlight = highlight,
                    onClick = { },          // 자식 쪽 이벤트 비활성화 (중복 방지)
                    onLongPress = { }
                  )
                }

                unread?.let {
                    Spacer(Modifier.width(6.dp))
                    UnreadBadge(it)
                }
            }

            if (showTime || !msg.reactions.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showTime) {
                        TimeBadge(
                            timeText = msg.time,
                            contentColor = BrandOnSurfaceLight.copy(alpha = 0.6f)
                        )
                    }

                    msg.reactions?.takeIf { it.isNotEmpty() }?.let { list ->
                        Spacer(Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            list.forEach { reaction ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = reaction.emoji,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = reaction.count.toString(),
                                        fontSize = 10.sp,
                                        color = BrandOnSurfaceLight.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
