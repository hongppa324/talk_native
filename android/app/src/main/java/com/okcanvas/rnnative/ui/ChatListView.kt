@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.vmerp.works.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vmerp.works.R
import com.vmerp.works.model.ChatRoom
import com.vmerp.works.model.PressTarget
import com.vmerp.works.model.User
import com.vmerp.works.ui.chat.content.RoomProfile
import com.vmerp.works.util.*

// 톡 리스트
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChatListView(
    rooms: List<ChatRoom> = emptyList(),
    currentUser: User?,
    onPress: (ChatRoom, PressTarget) -> Unit = { _, _ -> },
    onLongPress: (ChatRoom) -> Unit = {},
    onPin: (ChatRoom) -> Unit = {},
    onMute: (ChatRoom) -> Unit = {},
    onRefresh: () -> Unit = {},
    rnRefreshing: Boolean = false,
) {
    val pinned = remember(rooms, currentUser?.userId) { rooms.filter { it.isPinned(currentUser?.userId) } }
    val recent = remember(rooms, currentUser?.userId) { rooms.filter { !it.isPinned(currentUser?.userId) } }

    // 진입/데이터 변경 시 항상 맨 위로
    val listState = rememberLazyListState()
    var refreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()

    // == useEffect : 처음 렌더링될 때 실행되고 key로 넣은 rooms 바뀔 때마다 다시 실행됨
    LaunchedEffect(rooms) {
        if (rooms.isNotEmpty()) listState.scrollToItem(0)
        if (refreshing) refreshing = false
    }

    // == RefreshControl : 아래로 스와이프 시 톡리스트 새로고침하는 역할
    PullToRefreshBox(
        isRefreshing = rnRefreshing,
        onRefresh = { onRefresh() },
        state = pullState,
    ) {
        LazyColumn( // == FlatList
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(PlainWhite),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            items(pinned, key = { it.roomId }) { room ->
                SwipeRow(
                    room = room,
                    currentUser = currentUser,
                    onPress = onPress,
                    onLongPress = onLongPress,
                    onPin = onPin,
                    onMute = onMute,
                )
                HorizontalDivider(thickness = 0.5.dp, color = PlaceholderBorder)
            }

            items(recent, key = { it.roomId }) { room ->
                SwipeRow(
                    room = room,
                    currentUser = currentUser,
                    onPress = onPress,
                    onLongPress = onLongPress,
                    onPin = onPin,
                    onMute = onMute,
                )
                HorizontalDivider(thickness = 0.5.dp, color = PlaceholderBorder)
            }
        }
    }
}

@Composable
private fun SwipeRow(
    room: ChatRoom,
    currentUser: User?,
    onPress: (ChatRoom, PressTarget) -> Unit,
    onLongPress: (ChatRoom) -> Unit,
    onPin: (ChatRoom) -> Unit = {},
    onMute: (ChatRoom) -> Unit = {},
) {
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        onPin(room)
                        false
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        onMute(room)
                        false
                    }
                    else -> false
                }
            },
            positionalThreshold = { fullWidth -> fullWidth * 0.25f },
        )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            DismissBackground(
                dir = dismissState.dismissDirection,
                pinned = room.isPinned(currentUser?.userId),
                muted = room.isMuted(),
            )
        },
        content = {
            ChatListItem(
                room = room,
                currentUser = currentUser,
                onPress = onPress,
                onLongPress = onLongPress,
            )
        },
    )
}

// "나" 뱃지
@Composable
private fun MyBadge() {
    Box(
        modifier =
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0F1B2A)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "나",
            color = Color.White,
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle =
                        LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both,
                        ),
                ),
        )
    }
}

// 아이템
@Composable
private fun ChatListItem(
    room: ChatRoom,
    currentUser: User?,
    onPress: (ChatRoom, PressTarget) -> Unit,
    onLongPress: (ChatRoom) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .background(PlainWhite)
                .combinedClickable(
                    onClick = { onPress(room, PressTarget.Room) },
                    onLongClick = { onLongPress(room) },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .combinedClickable(
                        onClick = { onPress(room, PressTarget.RoomProfile) },
                        onLongClick = { onLongPress(room) },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            // RoomProfile 영역
            RoomProfile(
                members = room.chtMemberList,
                size = 48,
                unusedMembers = room.chtUnusedMemberList,
                imageUrl = null,
                menuType = "talkList",
                projectId = room.projectId,
                colorHex = null,
                userId = currentUser?.userId,
            )
        }
        Spacer(Modifier.width(12.dp)) // 가로 gap 12

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 방 제목
                Text(
                    text = displayTitle(room, currentUser),
                    modifier = Modifier.weight(1f, fill = false),
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (room.isMyPrivateRoom()) {
                        MyBadge()
                    }

                    val userCnt = room.userCnt
                    if (userCnt > 2) {
                        Text(
                            text = userCnt.toString(),
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = OnSurfaceSubtle,
                                ),
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                    }

                    if (room.isPinned(currentUser?.userId)) {
                        Icon(
                            painter = painterResource(id = R.drawable.pin_gray),
                            contentDescription = "고정",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Unspecified,
                        )
                    }

                    if (room.isMuted()) {
                        Icon(
                            painter = painterResource(id = R.drawable.bell_off_gray),
                            contentDescription = "음소거",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Unspecified,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // 마지막 보낸 메시지 영역
            Text(
                text = room.lastMsgTxt?.let { stripTag(it) } ?: "",
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceSubtle,
                    ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
        ) {
            // 마지막 보낸 메시지 시간 영역
            Text(
                text = formatTimeOrDate(room.epochTimestamp()),
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = OnSurfaceSubtle,
                    ),
            )

            AnimatedVisibility(
                visible = room.unReadCnt > 0,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(UnreadBadgeBg)
                            .padding(horizontal = 4.dp)
                            .height(15.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (room.isMentioned()) {
                            Text("@", color = UnreadBadgeFg, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(2.dp))
                        }
                        val text = if (room.unReadCnt > 99) "99+" else room.unReadCnt.toString()
                        Text(
                            text,
                            color = UnreadBadgeFg,
                            style =
                                MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = PlainWhite,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DismissBackground(
    dir: SwipeToDismissBoxValue?,
    pinned: Boolean,
    muted: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (dir?.name == "StartToEnd") Arrangement.Start else Arrangement.End,
    ) {
        if (dir?.name == "StartToEnd") {
            if (pinned) {
                Image(
                    painter = painterResource(R.drawable.swipe_pin_off),
                    contentDescription = "pin-off",
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.swipe_pin),
                    contentDescription = "pin-on",
                    modifier = Modifier.size(24.dp),
                )
            }
        } else if (dir?.name == "EndToStart") {
            if (muted) {
                Image(
                    painter = painterResource(R.drawable.swipe_bell),
                    contentDescription = "bell-on",
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.swipe_bell_off),
                    contentDescription = "bell-off",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
