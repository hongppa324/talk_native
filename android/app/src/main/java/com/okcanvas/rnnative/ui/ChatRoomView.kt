@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.vmerp.works.ui

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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vmerp.works.model.ChatFile
import com.vmerp.works.model.ChatMember
import com.vmerp.works.model.ChatMessage
import com.vmerp.works.model.PressTarget
import com.vmerp.works.model.SelectMode
import com.vmerp.works.model.User
import com.vmerp.works.ui.chat.AdminComment
import com.vmerp.works.ui.chat.DateArea
import com.vmerp.works.ui.chat.MyMessage
import com.vmerp.works.ui.chat.OtherMessage
import com.vmerp.works.ui.theme.LocalTalkTheme
import com.vmerp.works.ui.theme.rememberTalkTheme
import com.vmerp.works.ui.viewer.ImageViewer
import com.vmerp.works.util.*
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

// 이미지뷰어 컨트롤러
@Stable
data class ImageViewerController(
    val openImages: (files: List<ChatFile>, startIndex: Int) -> Unit,
)

/** 위젯들이 이미지뷰어를 열 수 있도록 주입 */
val LocalImageViewer: ProvidableCompositionLocal<ImageViewerController?> =
    staticCompositionLocalOf { null }

// 톡방 == TalkContent
@Composable
fun ChatRoomView(
    messages: List<ChatMessage> = emptyList(),
    currentUser: User?,
    chtMemberList: List<ChatMember>,
    isFetchingNextPage: Boolean = false,
    scrollToBottom: Boolean = false,
    scrollToTalkId: String? = null,
    onPress: (ChatMessage, PressTarget) -> Unit = { _, _ -> },
    onLongPress: (ChatMessage, PressTarget) -> Unit = { _, _ -> },
    onReachTop: () -> Unit = {},
    onPressViewerButton: (SelectMode, List<ChatFile>, Int) -> Unit = { _, _, _ -> },
) {
    val talkTheme = rememberTalkTheme(currentUser)

    CompositionLocalProvider(LocalTalkTheme provides talkTheme) {
        // 상태
        var query by remember { mutableStateOf("") }
        var replyTo by remember { mutableStateOf<ChatMessage?>(null) }
        val listState = rememberLazyListState()

        // 뷰어 상태 + 컨트롤러
        var viewerOpen by remember { mutableStateOf(false) }
        var viewerFiles by remember { mutableStateOf<List<ChatFile>>(emptyList()) }
        var viewerIndex by remember { mutableStateOf(0) }

        val imageViewerController =
            remember {
                ImageViewerController(
                    openImages = { files, index ->
                        if (files.isNotEmpty()) {
                            viewerFiles = files
                            viewerIndex = index.coerceIn(0, files.lastIndex)
                            viewerOpen = true
                        }
                    },
                )
            }

        // 검색 적용 리스트
        val active =
            remember(messages, query) {
                if (query.isBlank()) messages else messages.filter { it.uiText().contains(query, ignoreCase = true) }
            }

        // 최초 진입 시 최신 목록부터
        var didInitialScroll by remember { mutableStateOf(false) }
        LaunchedEffect(active) {
            if (!didInitialScroll && active.isNotEmpty()) {
                listState.scrollToItem(active.lastIndex)
                didInitialScroll = true
            }
        }

        // 스크롤 상단 도달 후 다음 페이지 fetching할 때 고정
        var prevActive by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
        LaunchedEffect(active, isFetchingNextPage) {
            val old = prevActive
            val new = active
            if (isFetchingNextPage && old.isNotEmpty() && new.size >= old.size) {
                val firstVisible = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                if (firstVisible != null) {
                    val oldIndex = firstVisible.index
                    val firstId = old.getOrNull(oldIndex)?.uiId()
                    val newIndex = new.indexOfFirst { it.uiId() == firstId }
                    if (firstId != null && newIndex >= 0) {
                        listState.scrollToItem(newIndex, firstVisible.offset) // 위치 고정
                    }
                }
            }
            prevActive = new
        }

        // 하단 근접 계산
        val atBottom by remember {
            derivedStateOf {
                val last = listState.layoutInfo.totalItemsCount - 1
                if (last < 0) true else listState.firstVisibleItemIndex >= last - 2
            }
        }

        // 새 메시지 받거나 보낸 직후 최하단으로 이동
        //  - 불러오는 중이 아니고(isFetchingNextPage == false)
        //  - 사용자가 하단 근처(atBottom)일 때만 자동 스크롤
        LaunchedEffect(scrollToBottom, active.size, isFetchingNextPage) {
            if (scrollToBottom && !isFetchingNextPage && active.isNotEmpty() && atBottom) {
                listState.animateScrollToItem(active.lastIndex)
            }
        }

        // 검색 결과에 따른 특정 메시지로 스크롤
        // - scrollToTalkId 에 해당하는 uiId()가 현재 목록에 있으면 그 위치로 스크롤
        // - 없으면 상단으로 당겨 onReachTop 트리거를 유도하고 직접 콜백도 호출
        LaunchedEffect(scrollToTalkId, active) {
            val target = scrollToTalkId ?: return@LaunchedEffect

            // 1) 원본(messages)에서 정확한 인덱스 먼저 찾고
            val idxInMessages = messages.indexOfFirst { it.stableId() == target }
            if (idxInMessages >= 0) {
                // 2) 현재 화면에 쓰이는 active 리스트의 인덱스로 역매핑
                val targetId = messages[idxInMessages].stableId()
                val idxInActive = active.indexOfFirst { it.stableId() == targetId }
                if (idxInActive >= 0) {
                    // compose가 리스트 갱신을 마치도록 한 프레임 양보 후 스크롤
                    kotlinx.coroutines.android.awaitFrame()
                    listState.animateScrollToItem(idxInActive)
                    return@LaunchedEffect
                }
            }

            // 3) 아직 목록에 없으면 상단 페이징 유도
            if (listState.firstVisibleItemIndex > 0) {
                val anchor = (listState.firstVisibleItemIndex - 5).coerceAtLeast(0)
                listState.scrollToItem(anchor)
            }
            onReachTop()
        }

        // 상단 도달 감지
        var canFire by remember { mutableStateOf(true) }
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { first: Int ->
                    if (first <= 2 && canFire) {
                        canFire = false
                        onReachTop()
                        delay(500)
                        canFire = true
                    }
                }
        }

        // 이미지 뷰어에서 이미지 삭제 시 변경된 내용 감지
        LaunchedEffect(messages, viewerOpen, viewerFiles) {
            if (!viewerOpen) return@LaunchedEffect
            val currentGroupId = viewerFiles.firstOrNull()?.talkId ?: return@LaunchedEffect

            // 최신 메시지에서 같은 talkId의 파일 목록을 찾음
            val updatedFiles = messages.firstOrNull { it.talkId == currentGroupId }?.fileList.orEmpty()

            // 전부 삭제된 경우 이미지 뷰어 닫기
            if (updatedFiles.isEmpty()) {
                viewerOpen = false
                return@LaunchedEffect
            }

            // fileNo 기준으로 변경 여부 판단
            val oldNos = viewerFiles.mapNotNull { it.fileNo }
            val newNos = updatedFiles.mapNotNull { it.fileNo }
            if (oldNos != newNos) {
                // 현재 보고 있던 파일(fileNo)을 최대한 유지
                val currNo = viewerFiles.getOrNull(viewerIndex)?.fileNo
                viewerFiles = updatedFiles
                viewerIndex =
                    newNos.indexOf(currNo).let { idx ->
                        when {
                            idx >= 0 -> idx
                            updatedFiles.isNotEmpty() -> viewerIndex.coerceIn(0, updatedFiles.lastIndex)
                            else -> 0
                        }
                    }
            }
        }

        CompositionLocalProvider(LocalImageViewer provides imageViewerController) {
            Scaffold(contentWindowInsets = WindowInsets(0.dp)) { innerPadding ->
                Box(
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                ) {
                    Column(
                        Modifier
                            .fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            itemsIndexed(active, key = { _, it -> it.stableId() }) { index, msg ->
                                val prev = active.getOrNull(index - 1)
                                val next = active.getOrNull(index + 1)

                                if (index == 0 || prev?.uiDate() != msg.uiDate()) {
                                    DateArea(dateIso = msg.uiDate())
                                    Spacer(Modifier.height(4.dp))
                                }

                                when (msg.uiType()) {
                                    UiMsgType.SYSTEM -> {
                                        AdminComment(
                                            text = msg.uiText(),
                                            contentColor = talkTheme.dateTime,
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        return@itemsIndexed
                                    }
                                    else -> Unit
                                }

                                val currKey = msg.uiAuthorKey(currentUser)
                                val prevKey = prev?.uiAuthorKey(currentUser)
                                val nextKey = next?.uiAuthorKey(currentUser)

                                val isMe = (msg.uiSender(currentUser) == "me")
                                val isDeleted = msg.uiIsDeleted()

                                if (isMe) {
                                    MyMessage(
                                        msg = msg,
                                        chtMemberList = chtMemberList,
                                        currentUser = currentUser,
                                        highlight = query.takeIf { it.isNotBlank() },
                                        hideTime = isDeleted,
                                        deleted = isDeleted,
                                        onPress = onPress,
                                        onLongPress = onLongPress,
                                    )
                                } else {
                                    OtherMessage(
                                        msg = msg,
                                        chtMemberList = chtMemberList,
                                        currentUser = currentUser,
                                        highlight = query.takeIf { it.isNotBlank() },
                                        hideTime = isDeleted,
                                        deleted = isDeleted,
                                        onPress = onPress,
                                        onLongPress = onLongPress,
                                    )
                                }

                                val sameGroupNext = (nextKey == currKey) && (next?.uiDate() == msg.uiDate())
                                Spacer(Modifier.height(if (sameGroupNext) 6.dp else 10.dp))
                            }
                        }

                        AnimatedVisibility(visible = replyTo != null) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x22000000))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "답장: " + (replyTo?.uiText().orEmpty()),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Spacer(Modifier.weight(1f))
                                androidx.compose.material3.TextButton(onClick = { replyTo = null }) {
                                    androidx.compose.material3.Text("취소")
                                }
                            }
                        }
                    }

                    if (viewerOpen) {
                        // 기존 이미지뷰어를 전체화면으로 띄우기
                        Dialog(
                            onDismissRequest = { viewerOpen = false },
                            properties =
                                DialogProperties(
                                    usePlatformDefaultWidth = false, // 전체폭 사용
                                    decorFitsSystemWindows = false,
                                    dismissOnBackPress = true,
                                    dismissOnClickOutside = true,
                                ),
                        ) {
                            ImageViewer(
                                files = viewerFiles,
                                startIndex = viewerIndex,
                                onDismiss = { viewerOpen = false },
                                onPressButton = onPressViewerButton,
                            )
                        }
                    }
                }
            }
        }
    }
}
