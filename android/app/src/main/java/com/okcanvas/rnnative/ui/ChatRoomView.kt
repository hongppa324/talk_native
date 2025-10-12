@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.okcanvas.rnnative.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.okcanvas.rnnative.model.ChatMessage
import com.okcanvas.rnnative.model.MessageType
import com.okcanvas.rnnative.model.MsgTapTarget
import com.okcanvas.rnnative.ui.chat.room.WidgetDateDivider
import com.okcanvas.rnnative.ui.chat.room.WidgetRecvMessage
import com.okcanvas.rnnative.ui.chat.room.WidgetSendMessage
import com.okcanvas.rnnative.ui.chat.room.WidgetSystemMessage
import com.okcanvas.rnnative.ui.viewer.ImageGalleryViewer

/* ---------- ImageViewer Local ---------- */

@Stable
data class ImageViewerController(
  val openSingle: (String) -> Unit,
  val openGallery: (List<String>, Int) -> Unit
)

/** 위젯들이 이미지뷰어를 열 수 있도록 주입 */
val LocalImageViewer: ProvidableCompositionLocal<ImageViewerController?> =
  staticCompositionLocalOf { null }

/* ---------- Screen ---------- */

/**
 * @param onClick (msg, target) 버블/이미지/링크 등 클릭 타깃 구분
 * @param onLongPress (msg, target) 롱프레스 타깃 구분
 */
@Composable
fun ChatRoomView(
  roomId: String,
  initialMessages: List<ChatMessage> = emptyList(),
  onBack: () -> Unit,
  onClick: (ChatMessage, MsgTapTarget) -> Unit = { _, _ -> },
  onLongPress: (ChatMessage, MsgTapTarget) -> Unit = { _, _ -> }
) {
  // 상태
  var bgColor by remember { mutableStateOf(Color(0xFFD5E6F5)) }
  var query by remember { mutableStateOf("") }
  var replyTo by remember { mutableStateOf<ChatMessage?>(null) }

  val messages = remember(initialMessages) { initialMessages.toMutableStateList() }
  val listState = rememberLazyListState()

  // 뷰어 상태 + 컨트롤러
  var viewerOpen by remember { mutableStateOf(false) }
  var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }
  var viewerIndex by remember { mutableStateOf(0) }
  val imageViewerController = remember {
    ImageViewerController(
      openSingle = { url ->
        viewerImages = listOf(url); viewerIndex = 0; viewerOpen = true
      },
      openGallery = { urls, index ->
        if (urls.isNotEmpty()) {
          viewerImages = urls
          viewerIndex = index.coerceIn(0, urls.lastIndex)
          viewerOpen = true
        }
      }
    )
  }

  // 검색 적용 리스트
  val active = remember(messages, query) {
    if (query.isBlank()) messages else messages.filter { it.text.contains(query, ignoreCase = true) }
  }

  // 처음/목록 갱신 시 맨 아래
  LaunchedEffect(active) {
    val last = (active.size - 1).coerceAtLeast(0)
    if (last >= 0) listState.scrollToItem(last)
  }

  CompositionLocalProvider(LocalImageViewer provides imageViewerController) {
    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { innerPadding ->
      Box(
        Modifier
          .padding(innerPadding)
          .fillMaxSize()
      ) {
        Column(
          Modifier
            .fillMaxSize()
            .background(bgColor)
        ) {
          LazyColumn(
            state = listState,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
          ) {
            itemsIndexed(active, key = { _, it -> it.id }) { index, msg ->
              val prev = active.getOrNull(index - 1)
              val next = active.getOrNull(index + 1)

              if (index == 0 || prev?.date != msg.date) {
                WidgetDateDivider(dateIso = msg.date)
                Spacer(Modifier.height(4.dp))
              }

              if (msg.type == MessageType.SYSTEM) {
                WidgetSystemMessage(text = msg.text)
                Spacer(Modifier.height(10.dp))
                return@itemsIndexed
              }

              val isFirstInGroup = prev?.sender != msg.sender || prev?.date != msg.date
              val isLastInGroup  = next?.sender != msg.sender || next?.date != msg.date
              if (isFirstInGroup && index != 0) Spacer(Modifier.height(6.dp))

              if (msg.sender == "me") {
                WidgetSendMessage(
                  msg = msg,
                  showTime = isLastInGroup,
                  highlight = query.takeIf { it.isNotBlank() },
                  onClick = onClick,
                  onLongPress = onLongPress
                )
              } else {
                WidgetRecvMessage(
                  msg = msg,
                  showAvatarAndName = (msg.sender != "me") && isFirstInGroup,
                  showTime = isLastInGroup,
                  highlight = query.takeIf { it.isNotBlank() },
                  onClick = onClick,
                  onLongPress = onLongPress
                )
              }

              val sameGroupNext = next?.sender == msg.sender && next?.date == msg.date
              Spacer(Modifier.height(if (sameGroupNext) 6.dp else 10.dp))
            }
          }

          AnimatedVisibility(visible = replyTo != null) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x22000000))
                .padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "답장: " + (replyTo?.text.orEmpty()),
                style = MaterialTheme.typography.labelMedium
              )
              Spacer(Modifier.weight(1f))
              androidx.compose.material3.TextButton(onClick = { replyTo = null }) {
                androidx.compose.material3.Text("취소")
              }
            }
          }
        }

        // 하단 근접 시 새 메시지 자동 스크롤
        val atBottom by remember {
          derivedStateOf {
            val last = listState.layoutInfo.totalItemsCount - 1
            if (last < 0) true else listState.firstVisibleItemIndex >= last - 2
          }
        }
        LaunchedEffect(active.size, atBottom) {
          if (atBottom) {
            val last = (active.size - 1).coerceAtLeast(0)
            if (last >= 0) listState.animateScrollToItem(last)
          }
        }

        if (viewerOpen) {
          ImageGalleryViewer(
            images = viewerImages,
            startIndex = viewerIndex,
            onDismiss = { viewerOpen = false }
          )
        }
      }
    }
  }
}
