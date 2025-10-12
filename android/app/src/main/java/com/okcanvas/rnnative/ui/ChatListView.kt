@file:OptIn(
  androidx.compose.foundation.ExperimentalFoundationApi::class,
  androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.okcanvas.rnnative.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.okcanvas.rnnative.model.ChatRoom
import com.okcanvas.rnnative.ui.chat.room.RoomAvatar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/* 색상 최소 정의 */
private val PlainWhite = Color(0xFFFFFFFF)
private val PlaceholderBorder = Color(0xFFE7E7E7)
private val OnSurfaceSubtle = Color(0xFF8E8E8E)
private val BadgeBg = Color(0xFFFFE100)
private val BadgeFg = Color(0xFF3A3A3A)

/* ====== 시간/날짜 포맷 헬퍼 ====== */
private fun formatTimeOrDate(
  ts: Long?,
  zoneId: ZoneId = ZoneId.systemDefault()
): String {
  if (ts == null || ts <= 0L) return ""
  val nowDate = LocalDate.now(zoneId)
  val zdt = Instant.ofEpochMilli(ts).atZone(zoneId)
  val msgDate = zdt.toLocalDate()

  val daysDiff = ChronoUnit.DAYS.between(msgDate, nowDate)
  return when {
    daysDiff == 0L ->
      DateTimeFormatter.ofPattern("a h:mm", Locale.KOREA).format(zdt) // 오늘
    daysDiff == 1L ->
      "어제"
    else ->
      DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.KOREA).format(zdt)
  }
}

/* ====== 리스트(스와이프/롱프레스/헤더 + 진입 시 맨 위) ====== */
@Composable
fun ChatListView(
  rooms: List<ChatRoom> = emptyList(),
  onOpenRoom: (ChatRoom) -> Unit,
  onPin: (String) -> Unit = {},
  onMute: (String) -> Unit = {},
  onLongPress: (ChatRoom) -> Unit = {},
  showHeaders: Boolean = false
) {
  val pinned = remember(rooms) { rooms.filter { it.pinned } }
  val recent = remember(rooms) { rooms.filter { !it.pinned } }

  // ✅ 진입/데이터 변경 시 항상 맨 위로
  val listState = rememberLazyListState()
  LaunchedEffect(rooms) {
    if (rooms.isNotEmpty()) listState.scrollToItem(0)
  }

  LazyColumn(
    state = listState,
    modifier = Modifier
      .fillMaxSize()
      .background(PlainWhite),
    contentPadding = PaddingValues(bottom = 8.dp)
  ) {
    if (showHeaders && pinned.isNotEmpty()) {
      item(key = "__header_pinned") { SectionHeader("고정됨") }
    }

    items(pinned, key = { it.id }) { room ->
      SwipeRow(
        room = room,
        onOpenRoom = onOpenRoom,
        onPin = onPin,
        onMute = onMute,
        onLongPress = onLongPress
      )
      HorizontalDivider(thickness = 0.5.dp, color = PlaceholderBorder)
    }

    if (showHeaders) {
      item(key = "__header_recent") { SectionHeader(if (pinned.isEmpty()) "대화" else "최근") }
    }

    items(recent, key = { it.id }) { room ->
      SwipeRow(
        room = room,
        onOpenRoom = onOpenRoom,
        onPin = onPin,
        onMute = onMute,
        onLongPress = onLongPress
      )
      HorizontalDivider(thickness = 0.5.dp, color = PlaceholderBorder)
    }
  }
}

@Composable
private fun SwipeRow(
  room: ChatRoom,
  onOpenRoom: (ChatRoom) -> Unit,
  onPin: (String) -> Unit,
  onMute: (String) -> Unit,
  onLongPress: (ChatRoom) -> Unit
) {
  val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
      when (value) {
        SwipeToDismissBoxValue.StartToEnd -> { onPin(room.id); false }
        SwipeToDismissBoxValue.EndToStart -> { onMute(room.id); false }
        else -> false
      }
    },
    positionalThreshold = { fullWidth -> fullWidth * 0.25f }
  )

  SwipeToDismissBox(
    state = dismissState,
    enableDismissFromStartToEnd = true,
    enableDismissFromEndToStart = true,
    backgroundContent = {
      DismissBackground(
        dir = dismissState.dismissDirection,
        pinned = room.pinned,
        muted = room.muted
      )
    },
    content = {
      ChatListItem(
        room = room,
        onClick = { onOpenRoom(room) },
        onLongPress = { onLongPress(room) }
      )
    }
  )
}

/* 아이템 */
@Composable
private fun ChatListItem(
  room: ChatRoom,
  onClick: () -> Unit,
  onLongPress: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 76.dp)
      .background(PlainWhite)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongPress
      )
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RoomAvatar(participants = room.participants, size = 52)

    Spacer(Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = room.title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = if (room.unread > 0) FontWeight.Bold else FontWeight.SemiBold
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(6.dp))
        if (room.pinned) {
          Icon(
            imageVector = Icons.Rounded.PushPin,
            contentDescription = "고정",
            modifier = Modifier.size(16.dp),
            tint = OnSurfaceSubtle
          )
        }
        if (room.muted) {
          Spacer(Modifier.width(2.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.VolumeOff,
            contentDescription = "음소거",
            modifier = Modifier.size(16.dp),
            tint = OnSurfaceSubtle
          )
        }
      }

      Spacer(Modifier.height(4.dp))

      Text(
        text = room.lastMessage,
        style = MaterialTheme.typography.bodyMedium.copy(
          color = OnSurfaceSubtle,
          fontWeight = if (room.unread > 0) FontWeight.SemiBold else FontWeight.Normal
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    Spacer(Modifier.width(8.dp))

    Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.wrapContentHeight()
    ) {
      Text(
        text = formatTimeOrDate(room.timestamp),
        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8E8E8E))
      )

      AnimatedVisibility(
        visible = room.unread > 0,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Box(
          modifier = Modifier
            .wrapContentSize()
            .clip(CircleShape)
            .background(BadgeBg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = if (room.unread > 99) "99+" else room.unread.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
              color = BadgeFg,
              fontWeight = FontWeight.SemiBold
            )
          )
        }
      }
    }
  }
}

@Composable
private fun DismissBackground(
  dir: SwipeToDismissBoxValue?,
  pinned: Boolean,
  muted: Boolean
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(76.dp)
      .padding(horizontal = 20.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = if (dir?.name == "StartToEnd") Arrangement.Start else Arrangement.End
  ) {
    if (dir?.name == "StartToEnd") {
      Icon(Icons.Rounded.PushPin, contentDescription = null, tint = OnSurfaceSubtle)
      Spacer(Modifier.width(8.dp))
      Text(if (pinned) "고정 해제" else "고정", color = Color(0xFF3A3A3A))
    } else if (dir?.name == "EndToStart") {
      Text(if (muted) "알림 켜기" else "알림 끄기", color = Color(0xFF3A3A3A))
      Spacer(Modifier.width(8.dp))
      Icon(Icons.AutoMirrored.Rounded.VolumeOff, contentDescription = null, tint = OnSurfaceSubtle)
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  Surface(color = Color(0xFFF6F6F6)) {
    Text(
      text = title,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp),
      style = MaterialTheme.typography.labelSmall.copy(
        color = Color(0xFF3A3A3A).copy(alpha = 0.7f)
      ),
      textAlign = TextAlign.Center
    )
  }
}
