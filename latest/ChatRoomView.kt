@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class) // Material3 실험 API 사용(Scaffold 등 일부 컴포넌트)

package com.vmerp.works.ui // 패키지 경로

import android.content.Context // 키보드 제어(InputMethodManager) 획득을 위해 사용
import android.view.inputmethod.InputMethodManager // 하드 키보드 숨김 제어
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background // 배경 처리 Modifier 확장
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures // 제스처(탭/롱탭 등) 감지
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.* // 레이아웃 관련 Composable/Modifier (Row/Column/Box/Spacer 등) 및 패딩/사이즈
import androidx.compose.foundation.lazy.LazyColumn // 스크롤 가능한 리스트 Composable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed // LazyColumn의 인덱스 포함한 아이템
import androidx.compose.foundation.lazy.rememberLazyListState // 리스트 스크롤 상태 기억
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon // 아이콘 Composable
import androidx.compose.material3.Scaffold // 상·하단 시스템 영역 포함 레이아웃
import androidx.compose.runtime.* // remember/LaunchedEffect/derivedStateOf 등 상태 & 효과
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow // Compose 상태를 Flow로 관찰
import androidx.compose.ui.Alignment // 정렬 옵션
import androidx.compose.ui.Modifier // Modifier 체인 구축
import androidx.compose.ui.draw.clip // 모서리 자르는 Modifier
import androidx.compose.ui.graphics.Color // 색상 타입
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput // 포인터 입력(제스처) 처리 엔트리 포인트
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager // 포커스 제어(키보드 연동)
import androidx.compose.ui.platform.LocalSoftwareKeyboardController // 소프트 키보드 컨트롤러
import androidx.compose.ui.platform.LocalView // 현재 View 참조(토큰 획득 등)
import androidx.compose.ui.res.painterResource // res/drawable에 있는 xml (<-svg) 그려주는 함수
import androidx.compose.ui.unit.dp // dp 단위 : density-independent pixel => 화면 해상도에 따라 크기가 달라지지 않음
import androidx.compose.ui.window.Dialog // 전체 화면/모달 다이얼로그
import androidx.compose.ui.window.DialogProperties // Dialog 동작 특성
import com.vmerp.works.ChatNativeTokenStore
import com.vmerp.works.R // res/drawable에 접근
import com.vmerp.works.model.ChatFile // 대화방 파일 모델 class
import com.vmerp.works.model.ChatMember // 대화방 참여자 모델 class
import com.vmerp.works.model.ChatMessage // 메시지 모델 class
import com.vmerp.works.model.PressTarget // 터치 타겟 구분하는 enum
import com.vmerp.works.model.SelectMode // 이미지 뷰어 버튼 유형 구분하는 enum
import com.vmerp.works.model.User // 사용자 모델 class
import com.vmerp.works.ui.chat.AdminComment // 시스템 메시지
import com.vmerp.works.ui.chat.DateArea // sendDtm 날짜 표시
import com.vmerp.works.ui.chat.MyMessage // 내가 보낸 메시지
import com.vmerp.works.ui.chat.OtherMessage // 상대가 보낸 메시지
import com.vmerp.works.ui.theme.LocalTalkTheme // 커스텀 톡 테마 CompositionLocal
import com.vmerp.works.ui.theme.TalkThemeColors // 톡 테마 컬러 모델 class
import com.vmerp.works.ui.theme.rememberTalkTheme // 사용자/설정 기반 톡 테마 계산
import com.vmerp.works.ui.viewer.ImageViewer // 전체화면 이미지 뷰어 컴포넌트
import com.vmerp.works.util.* // util (uiType/uiText/uiDate/stableId 등의 확장 함수)
import kotlinx.coroutines.android.awaitFrame // 프레임 양보(레이아웃 완료 후 스크롤)
import kotlinx.coroutines.delay // 비동기 지연
import kotlinx.coroutines.flow.distinctUntilChanged // Flow 중복 억제
import kotlinx.coroutines.launch

// 이미지뷰어 컨트롤러

/**
 * 이미지 뷰어를 외부(자식 Composable)에서 열 수 있도록 전달하는 컨트롤러
 */
@Stable
data class ImageViewerController(
    val openImages: (files: List<ChatFile>, startIndex: Int) -> Unit,
)

// children에서 이미지뷰어를 열 수 있도록 주입하는 CompositionLocal
val LocalImageViewer: ProvidableCompositionLocal<ImageViewerController?> =
    staticCompositionLocalOf { null }

// 톡방 == TalkContent
@Composable
fun ChatRoomView(
    roomId: String,
    messages: List<ChatMessage> = emptyList(),
    currentUser: User?,
    chtMemberList: List<ChatMember>,
    chtUnusedMemberList: List<ChatMember>,
    isFetchingNextPage: Boolean = false,
    scrollToBottom: Boolean = false,
    scrollToTalkId: String? = null,
    isScrolling: Boolean = false,
    scrollSeq: Int? = 0,
    highlightQuery: String? = null,
    videoTalkId: String? = null,
    isDownloading: Boolean = false,
    downloadPercent: Int = 0,
    downloadReceived: Long = 0L,
    downloadTotal: Long = 0L,
    downloadingTalkId: String?,
    downloadingFileNo: String?,
    talkTheme: List<TalkThemeColors> = emptyList(),
    onPress: (ChatMessage, PressTarget, String?) -> Unit = { _, _, _ -> },
    onLongPress: (ChatMessage, PressTarget) -> Unit = { _, _ -> },
    onReachTop: () -> Unit = {},
    onPressViewerButton: (SelectMode, List<ChatFile>, Int?) -> Unit = { _, _, _ -> },
) {
    // 키보드/포커스/토큰 제어에 필요한 로컬 참조
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val context = LocalContext.current
    val token = remember { ChatNativeTokenStore.getToken(context) }
    val scope = rememberCoroutineScope()
    // println("ChatNativeToken = $token")

    // 다른 영역 클릭 시 키보드 내려가게 하는 함수
    fun hideKeyboardHard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        val inputManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // 현재 포커스 뷰 토큰/루트 토큰 양쪽 시도
        view.rootView?.findFocus()?.windowToken?.let { inputManager.hideSoftInputFromWindow(it, 0) }
        inputManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // 배경 터치 시 키보드 내리기(제스처) + 전체 콘텐츠 컨테이너
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    hideKeyboardHard()
                })
            },
    ) {
        // 사용자/설정에 따른 톡 테마 계산 후 제공
        val talkTheme: TalkThemeColors =
            if (talkTheme.isNotEmpty()) {
                talkTheme.first()
            } else {
                rememberTalkTheme(currentUser)
            }

        CompositionLocalProvider(LocalTalkTheme provides talkTheme) {
            // 상태
            var replyTo by remember { mutableStateOf<ChatMessage?>(null) } // 답장 대상
            val listState = rememberLazyListState() // 스크롤 상태

            // messagesJson 순서를 그대로 사용
            val uiMessages = remember(messages) { messages }

            // Down 버튼 노출 여부
            val showDownButton by remember {
                derivedStateOf {
                    val offset = listState.firstVisibleItemScrollOffset
                    val index = listState.firstVisibleItemIndex

                    index > 0 || (index == 0 && offset > 80)
                }
            }

            // 이미지 뷰어 상태 + 컨트롤러
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

            // 톡방 최초 진입 시 최신 목록으로 스크롤
            var didInitialScroll by remember { mutableStateOf(false) }

            LaunchedEffect(roomId) {
                didInitialScroll = false
            }

            // 톡방 진입 시 최신 메시지(아래쪽)로 한 번만 이동
            LaunchedEffect(messages.lastOrNull()?.talkId, roomId, scrollToTalkId) {
                if (!didInitialScroll && messages.isNotEmpty() && scrollToTalkId == null) {
                    didInitialScroll = true
                    awaitFrame() // Layout이 다 그려지도록 한 프레임 양보 (정확한 위치로 이동할 수 있도록)
                    listState.scrollToItemExact(
                        index = 0, // 최신 메시지가 0번
                        reverse = true, // 순서 뒤집은 경우
                        align = AlignAnchor.Bottom,
                        maxRefine = 5, // retry 횟수
                    )
                }
            }

            // 내가 메시지 보냈을 때는 RN에서 newMessageControl 돌고 scrollToBottom prop으로 전달
            LaunchedEffect(scrollToBottom) {
                if (scrollToBottom && messages.isNotEmpty()) {
                    // 최신 메시지가 0번
                    listState.animateScrollToItem(0)
                }
            }

            // 다른 사람이 보낸 새 메시지이면 내가 아래에 있을 때만 자동 스크롤
            LaunchedEffect(messages.size, isFetchingNextPage) {
                if (isFetchingNextPage || messages.isEmpty()) return@LaunchedEffect

                val lastMsg = messages.firstOrNull() ?: return@LaunchedEffect

                val fromMe = currentUser?.userId != null && lastMsg.userId == currentUser.userId
                if (fromMe) return@LaunchedEffect

                val index = listState.firstVisibleItemIndex
                val offset = listState.firstVisibleItemScrollOffset

                val isNearBottom = (index <= 1 && offset < 200)

                if (isNearBottom) {
                    listState.animateScrollToItem(0)
                }
            }

            val highlightedId = remember { mutableStateOf<String?>(null) }

            // 검색/답장/공지: 정확한 위치로 스크롤(하단 정렬)
            LaunchedEffect(scrollToTalkId, scrollSeq, messages, isScrolling) {
                val target = scrollToTalkId ?: return@LaunchedEffect

                fun matches(
                    m: ChatMessage,
                    key: String,
                ) = (m.talkId == key) || (m.tmpTalkId == key) || (m.stableId() == key)

                // 1) 원본(messages)에서 정확한 인덱스 먼저 찾고
                val idxInMessages = messages.indexOfFirst { matches(it, target) }
                if (idxInMessages >= 0) {
                    // index 그대로 사용
                    val uiIndex = idxInMessages

                    listState.scrollToItemExact(
                        index = uiIndex, // 메시지 index
                        reverse = true, // 순서 뒤집은 경우
                        align = AlignAnchor.Bottom,
                        extraTopPx = 0,
                        extraBottomPx = 0,
                        maxRefine = 2, // retry 횟수
                    )

                    if (isScrolling) {
                        highlightedId.value = target
                        delay(800) // 0.8초 후 하이라이트 해제
                        highlightedId.value = null
                    }
                    return@LaunchedEffect
                }

                // 4) 아직 목록에 없으면 상단 페이징 유도 + 콜백 호출
                if (listState.firstVisibleItemIndex > 0) {
                    val anchor = (listState.firstVisibleItemIndex - 5).coerceAtLeast(0)
                    listState.scrollToItem(anchor)
                }
                onReachTop()
            }

            // 상단 도달 감지
            var lastTopRequestSize by remember { mutableStateOf(0) }

            LaunchedEffect(listState, uiMessages.size, isFetchingNextPage) {
                snapshotFlow {
                    // 현재 화면에 보이는 메시지 중 가장 큰 index (가장 위쪽 있는 아이템)
                    val maxVisibleIndex = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1
                    val lastIndex = uiMessages.lastIndex

                    lastIndex >= 0 && maxVisibleIndex >= lastIndex - 2
                }
                    .distinctUntilChanged()
                    .collect { isNearTop ->
                        if (isNearTop && !isFetchingNextPage && uiMessages.size != lastTopRequestSize) {
                            lastTopRequestSize = uiMessages.size
                            onReachTop()
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

            // 메시지 목록 UI Tree 구성
            CompositionLocalProvider(LocalImageViewer provides imageViewerController) {
                Scaffold(contentWindowInsets = WindowInsets(0.dp)) { innerPadding ->
                    Box(
                        Modifier
                            .padding(innerPadding)
                            .background(talkTheme.backGroundColor)
                            .fillMaxSize(),
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize(),
                        ) {
                            LazyColumn(
                                state = listState,
                                reverseLayout = true, // 순서 뒤집은 경우
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                verticalArrangement =
                                    Arrangement.spacedBy(
                                        space = 0.dp,
                                        alignment = Alignment.Top,
                                    ), // 초기 톡방의 경우 위에서부터 보이도록 추가
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp),
                            ) {
                                // uiMessages 기준으로 UI 그리기
                                itemsIndexed(uiMessages, key = { _, it -> it.stableId() }) { index, msg ->
                                    val prevUi = uiMessages.getOrNull(index - 1)
                                    val nextUi = uiMessages.getOrNull(index + 1)

                                    val currDate = msg.uiDate()
                                    val nextDate = nextUi?.uiDate()

                                    val needHeader = currDate.isNotEmpty() && (nextUi == null || currDate != nextDate)

                                    val isHighlighted = (msg.talkId == highlightedId.value)
                                    val shakeAnim = remember { Animatable(0f) }

                                    LaunchedEffect(isHighlighted) {
                                        if (isHighlighted) {
                                            val seq = listOf(8f, -8f, 4f, -4f, 0f)
                                            for (v in seq) {
                                                shakeAnim.animateTo(v, tween(durationMillis = 60))
                                            }
                                        } else {
                                            shakeAnim.snapTo(0f)
                                        }
                                    }

                                    // 메시지 그룹화
                                    val currKey = msg.uiAuthorKey(currentUser)
                                    val prevKey = prevUi?.uiAuthorKey(currentUser)
                                    val nextKey = nextUi?.uiAuthorKey(currentUser)

                                    val isMe = (msg.uiSender(currentUser) == "me")
                                    val isDeleted = msg.uiIsDeleted()

                                    // 시스템 메시지
                                    if (msg.uiType() == UiMsgType.SYSTEM) {
                                        AdminComment(
                                            text = msg.uiText(),
                                            contentColor = talkTheme.dateTime,
                                        )
                                        Spacer(Modifier.height(10.dp))
                                    } else {
                                        // 일반 메시지
                                        Box(
                                            Modifier
                                                .graphicsLayer { translationX = shakeAnim.value },
                                        ) {
                                            if (isMe) {
                                                MyMessage(
                                                    msg = msg,
                                                    chtMemberList = chtMemberList,
                                                    currentUser = currentUser,
                                                    highlight = highlightQuery?.takeIf { !it.isNullOrBlank() },
                                                    videoTalkId = videoTalkId,
                                                    isDownloading = isDownloading,
                                                    downloadPercent = downloadPercent,
                                                    downloadReceived = downloadReceived,
                                                    downloadTotal = downloadTotal,
                                                    downloadingTalkId = downloadingTalkId,
                                                    downloadingFileNo = downloadingFileNo,
                                                    deleted = isDeleted,
                                                    onPress = onPress,
                                                    onPressSaveButton = onPressViewerButton,
                                                    onLongPress = onLongPress,
                                                )
                                            } else {
                                                OtherMessage(
                                                    msg = msg,
                                                    chtMemberList = chtMemberList,
                                                    chtUnusedMemberList = chtUnusedMemberList,
                                                    currentUser = currentUser,
                                                    highlight = highlightQuery?.takeIf { !it.isNullOrBlank() },
                                                    videoTalkId = videoTalkId,
                                                    isDownloading = isDownloading,
                                                    downloadPercent = downloadPercent,
                                                    downloadReceived = downloadReceived,
                                                    downloadTotal = downloadTotal,
                                                    downloadingTalkId = downloadingTalkId,
                                                    downloadingFileNo = downloadingFileNo,
                                                    deleted = isDeleted,
                                                    onPress = onPress,
                                                    onPressSaveButton = onPressViewerButton,
                                                    onLongPress = onLongPress,
                                                )
                                            }
                                        }

                                        val sameGroupNext = (nextKey == currKey) && (nextUi?.uiDate() == msg.uiDate())
                                        Spacer(Modifier.height(if (sameGroupNext) 6.dp else 10.dp))
                                    }

                                    // 순서를 뒤집었기 때문에 DateArea를 기존과 반대로 배치
                                    if (needHeader) {
                                        DateArea(dateIso = currDate)
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                            }
                        }

                        if (showDownButton) {
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 11.dp, end = 19.dp)
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(hex("#ffffff"))
                                        .border(1.dp, hex("#E4E8EE"), CircleShape)
                                        .clickable(
                                            indication = null, // 클릭 시 RippleEffect(회색 음영) 제거
                                            interactionSource = remember { MutableInteractionSource() },
                                        ) {
                                            // 최신 메시지가 0번
                                            scope.launch { listState.animateScrollToItem(0) }
                                        },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.down_button),
                                    contentDescription = "Down 버튼",
                                    tint = Color.Unspecified,
                                )
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
                                val isMyImage =
                                    remember(viewerFiles, currentUser, messages) {
                                        val groupTalkId = viewerFiles.firstOrNull()?.talkId
                                        if (groupTalkId == null || currentUser?.userId == null) {
                                            false
                                        } else {
                                            val ownerId = messages.firstOrNull { it.talkId == groupTalkId }?.userId
                                            ownerId == currentUser.userId
                                        }
                                    }

                                ImageViewer(
                                    files = viewerFiles,
                                    startIndex = viewerIndex,
                                    isMe = isMyImage,
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
}

enum class AlignAnchor { Top, Center, Bottom }

// 검색, 답장, 공지 스크롤 이동하는 함수
private suspend fun LazyListState.scrollToItemExact(
    index: Int,
    reverse: Boolean = false, // reverseLayout = true인 경우
    align: AlignAnchor = AlignAnchor.Top,
    extraTopPx: Int = 0,
    extraBottomPx: Int = 0,
    maxRefine: Int = 2,
) {
    val anchor =
        if (reverse) {
            // reverseLayout = true일 때 anchor를 반대로 생각해야 함 (Align.Bottom = 사실상 Align.Top)
            when (align) {
                AlignAnchor.Bottom -> AlignAnchor.Top
                AlignAnchor.Top -> AlignAnchor.Bottom
                else -> align
            }
        } else {
            align
        }

    // 1) 1차: 아이템을 일단 화면에 보이게
    animateScrollToItem(index)

    // 2) 레이아웃 확정/미디어 로딩에 따라 최대 maxRefine 횟수 보정
    repeat(maxRefine) {
        awaitFrame()

        val li = layoutInfo
        val item = li.visibleItemsInfo.firstOrNull { it.index == index } ?: return

        val vpTop = li.viewportStartOffset
        val vpBottom = li.viewportEndOffset

        val desiredTop =
            when (anchor) {
                AlignAnchor.Top -> vpTop + extraTopPx
                AlignAnchor.Center -> (vpTop + vpBottom - item.size) / 2
                AlignAnchor.Bottom -> vpBottom - item.size - extraBottomPx
            }

        val offset = item.offset - desiredTop
        if (offset != 0) {
            animateScrollToItem(index, offset)
        }
    }
}
