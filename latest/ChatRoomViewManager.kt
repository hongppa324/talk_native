package com.vmerp.works.ui // 패키지 경로

import android.graphics.Color // 안드로이드 색상 타입
import android.view.View // 안드로이드 View 타입
import androidx.compose.material3.MaterialTheme // Material3 테마/폰트
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState // 상태 홀더 타입(제네릭)
import androidx.compose.runtime.mutableStateOf // Compose 상태 생성자 함수 (== useState)
import androidx.compose.ui.platform.ComposeView // Compose를 호스팅하는 플랫폼 View
import androidx.compose.ui.platform.ViewCompositionStrategy // ComposeView의 Composition Lifecycle 전략
import androidx.lifecycle.Lifecycle // andoridx Lifecycle enum/타입
import androidx.lifecycle.LifecycleOwner // LifeCycle Owner 인터페이스
import androidx.lifecycle.LifecycleRegistry // LifeCycle state machine class => 현재 상태가 무엇인지 기록하고 이벤트(ON_CREATE, ON_RESUME 등)를 받아 다음 상태로 transition
import androidx.lifecycle.ViewModelStore // ViewModel 저장소(구성 변경 생존)
import androidx.lifecycle.ViewModelStoreOwner // ViewModel 저장소 Owner
import androidx.lifecycle.findViewTreeLifecycleOwner // ViewTree에서 LifecycleOwner 조회
import androidx.lifecycle.findViewTreeViewModelStoreOwner // ViewTree 에서 ViewModelStoreOwner 조회
import androidx.lifecycle.setViewTreeLifecycleOwner // ViewTree에 LifecycleOwner 설정
import androidx.lifecycle.setViewTreeViewModelStoreOwner // ViewTree에 ViewModelStoreOwner 설정
import androidx.savedstate.SavedStateRegistry // 구성 변경 시 상태 저장/복원 Registry
import androidx.savedstate.SavedStateRegistryController // SavedState 제어기(attach/restore)
import androidx.savedstate.SavedStateRegistryOwner // SavedState Owner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner // ViewTree에서 SavedStateOwner 조회
import androidx.savedstate.setViewTreeSavedStateRegistryOwner // ViewTree에 SavedStateOwner 설정
import com.facebook.react.bridge.Arguments // JS로 보낼 Map/Array 생성 유틸 [RN과 연결]
import com.facebook.react.bridge.LifecycleEventListener // RN host Lifecycle callback listener [RN과 연결]
import com.facebook.react.bridge.ReactApplicationContext // ReactApplication Context [RN과 연결]
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.UiThreadUtil // UI Thread Util (runOnUiThread) [RN과 연결]
import com.facebook.react.bridge.WritableArray // JS 전달 배열 타입
import com.facebook.react.bridge.WritableMap // JS 전달 맵 타입
import com.facebook.react.uimanager.SimpleViewManager // RN Native UIManager 기본 class
import com.facebook.react.uimanager.ThemedReactContext // RN Context(액티비티/테마 참조 포함)
import com.facebook.react.uimanager.annotations.ReactProp // RN → Native prop 전달하는 annotation (@ReactProp)
import com.facebook.react.uimanager.events.RCTEventEmitter // Native -> RN으로 direct event 전달
import com.vmerp.works.model.ChatFile // 대화방 파일 모델 class
import com.vmerp.works.model.ChatMember // 대화방 참여자 모델 class
import com.vmerp.works.model.ChatMessage // 대화방 메시지 모델 class
import com.vmerp.works.model.CommentList // 댓글 목록 모델 class
import com.vmerp.works.model.Emoticon // 이모티콘 모델 class
import com.vmerp.works.model.LikeList // 공감 목록 모델 class
import com.vmerp.works.model.Link // 링크 모델 class
import com.vmerp.works.model.PressTarget // 터치 타겟 구분하는 enum
import com.vmerp.works.model.SelectMode // 이미지 뷰어 버튼 유형 enum
import com.vmerp.works.model.User // 사용자 모델 class
import com.vmerp.works.ui.theme.PretendardTypography // Pretendard 폰트 가져오기
import com.vmerp.works.ui.theme.TalkTheme // 톡 테마 모델 class
import com.vmerp.works.ui.theme.TalkThemeColors // 톡 테마 컬러 모델 class
import com.vmerp.works.ui.theme.toComposeTheme
import com.vmerp.works.util.* // util (JSON 확장/포맷/정렬/문자열 정제 등)
import org.json.JSONArray // JSON 배열 파싱
import org.json.JSONObject // JSON 객체 파싱
import java.time.*

// 고유 태그 키 (시스템 상수 사용 금지)
private const val ROOM_STATE_TAG = 0x0C11A7CB // Compose 상태 보관용
private const val ROOM_CONTENT_SET_TAG = 0x51B0A9E1 // setContent 중복 방지 플래그

// 파일 전용 상태 타입
private data class ChatRoomState(
    val roomId: MutableState<String>,
    val user: MutableState<User?>,
    val messages: MutableState<List<ChatMessage>>,
    val userList: MutableState<List<ChatMember>>,
    val unUsedUserList: MutableState<List<ChatMember>>,
    val talkTheme: MutableState<List<TalkThemeColors>>,
    val i18n: MutableState<I18nMap>,
    val isFetchingNextPage: MutableState<Boolean>,
    val scrollToBottom: MutableState<Boolean>,
    val scrollToTalkId: MutableState<String?>,
    val isScrolling: MutableState<Boolean>,
    val scrollSeq: MutableState<Int?>,
    val highlightQuery: MutableState<String?>,
    val videoTalkId: MutableState<String?>,
    val isDownloading: MutableState<Boolean>,
    val downloadPercent: MutableState<Int>,
    val downloadReceived: MutableState<Long>,
    val downloadTotal: MutableState<Long>,
    val downloadingTalkId: MutableState<String?>,
    val downloadingFileNo: MutableState<String?>,
)

// * 패키지 내 동일한 경로에 선언하는 class들은 서로 이름이 달라야함 (톡리스트 Lifecycle class 이름과 다르게 설정)
// 톡방 LifecycleOwner 정의: SavedState + ViewModelStore를 갖춘 Owner
private class ChatRoomLifecycleOwner(
    private val context: ThemedReactContext,
) : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner, LifecycleEventListener {
    private val lifecycleRegistry = LifecycleRegistry(this) // LifeCycle state machine class
    private val savedStateController = SavedStateRegistryController.create(this) // SavedState Controller

    override val viewModelStore: ViewModelStore = ViewModelStore() // Compositoin을 변경하는 동안 ViewModel 생존

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    init {
        // (1) SavedState 연결 + 복원 (super.onCreate에 해당)
        savedStateController.performAttach() // attach owner
        savedStateController.performRestore(null) // Bundle 없으면 null

        // (2) 라이프사이클 transition (전이) : 이전 INITIALIZED -> CREATED -> STARTED (onCreate 후 onStart)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        // (3) RN host lifecycle 수신
        context.addLifecycleEventListener(this)
    }

    override fun onHostResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onHostPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onHostDestroy() {
        // RN host destory 됐을 때 Compose Tree 종료
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        context.removeLifecycleEventListener(this)
        // ViewModelStore 정리 (메모리 누수 방지)
        viewModelStore.clear()
    }
}

/**
 * RN에서 사용할 "ChatRoomView" 네이티브 컴포넌트를 정의하는 ViewManager.
 * - RN props 바인딩: roomId, userJson, messagesJson, userListJson, 페이징/스크롤 제어
 * - Native → RN 이벤트: onPress, onLongPress, onReachTop, onPressViewerButton
 */
class ChatRoomViewManager(
    private val reactContext: ReactApplicationContext,
) : SimpleViewManager<ComposeView>() {
    override fun getName() = "ChatRoomView" // JavaScript에서 호출할 때 사용할 이름

    /*
     * RN이 View Instance를 생성할 때 호출
     * ComposeView를 생성하고, Lifecycle/SavedState/ViewModelStore Owner를 트리에 연결
     * 최초 1회 setContent로 ChatRoomView UI 그리기
     */
    override fun createViewInstance(context: ThemedReactContext): ComposeView {
        // 1) LifecycleOwner 생성 및 보관
        val owner = ChatRoomLifecycleOwner(context)

        // 2) View,Owner, 저장소 연결
        val view =
            ComposeView(context).apply {
                // 톡방 이동 시 잔상 남는 듯한 현상 수정 => 여전히 그렇긴 함.
                setBackgroundColor(Color.WHITE)

                // Strategy : ViewTree의 Lifecycle destroy 시 Composition 정리
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                // Tree 상에 Owner 세팅(없을 때만) : 세 가지 Owner 모두 연결
                if (findViewTreeLifecycleOwner() == null) {
                    setViewTreeLifecycleOwner(owner)
                }
                if (findViewTreeSavedStateRegistryOwner() == null) {
                    setViewTreeSavedStateRegistryOwner(owner)
                }
                if (findViewTreeViewModelStoreOwner() == null) {
                    setViewTreeViewModelStoreOwner(owner)
                }
            }

        // 3) Compose 상태 컨테이너 구성 & 태그로 보관 (RN -> Native)
        val state =
            ChatRoomState(
                roomId = mutableStateOf<String>(""),
                messages = mutableStateOf<List<ChatMessage>>(emptyList()),
                user = mutableStateOf<User?>(null),
                userList = mutableStateOf<List<ChatMember>>(emptyList()),
                unUsedUserList = mutableStateOf<List<ChatMember>>(emptyList()),
                talkTheme = mutableStateOf<List<TalkThemeColors>>(emptyList()),
                i18n = mutableStateOf(emptyMap()),
                isFetchingNextPage = mutableStateOf(false),
                scrollToBottom = mutableStateOf(false),
                scrollToTalkId = mutableStateOf<String?>(null),
                isScrolling = mutableStateOf(false),
                scrollSeq = mutableStateOf<Int?>(0),
                highlightQuery = mutableStateOf<String?>(null),
                videoTalkId = mutableStateOf<String?>(null),
                isDownloading = mutableStateOf(false),
                downloadPercent = mutableStateOf(0),
                downloadReceived = mutableStateOf(0L),
                downloadTotal = mutableStateOf(0L),
                downloadingTalkId = mutableStateOf<String?>(null),
                downloadingFileNo = mutableStateOf<String?>(null),
            )
        view.setTag(ROOM_STATE_TAG, state)

        // 4) 중복 setContent 방지용 플래그와 함께 최초 1회 Content 설정
        if (view.getTag(ROOM_CONTENT_SET_TAG) != true) {
            view.setTag(ROOM_CONTENT_SET_TAG, true)
            view.setContent {
                MaterialTheme(
                    typography = PretendardTypography(), // 하위 요소에 폰트 적용
                ) {
                    CompositionLocalProvider(LocalI18n provides state.i18n.value) {
                        ChatRoomView(
                            roomId = state.roomId.value,
                            messages = state.messages.value,
                            currentUser = state.user.value,
                            chtMemberList = state.userList.value,
                            chtUnusedMemberList = state.unUsedUserList.value,
                            talkTheme = state.talkTheme.value,
                            isFetchingNextPage = state.isFetchingNextPage.value,
                            scrollToBottom = state.scrollToBottom.value,
                            scrollToTalkId = state.scrollToTalkId.value,
                            isScrolling = state.isScrolling.value,
                            scrollSeq = state.scrollSeq.value,
                            highlightQuery = state.highlightQuery.value,
                            videoTalkId = state.videoTalkId.value,
                            isDownloading = state.isDownloading.value,
                            downloadPercent = state.downloadPercent.value,
                            downloadReceived = state.downloadReceived.value,
                            downloadTotal = state.downloadTotal.value,
                            downloadingTalkId = state.downloadingTalkId.value,
                            downloadingFileNo = state.downloadingFileNo.value,
                            onPress = { msg, target, type -> sendOnPress(view, msg, target, type) },
                            onLongPress = { msg, target -> sendOnMessageLongPress(view, msg, target) },
                            onReachTop = { sendOnReachTop(view) },
                            onPressViewerButton = { mode, images, index ->
                                sendOnPressViewerButton(view, mode, images, index)
                            },
                        )
                    }
                }
            }
        }
        return view
    }

  /* ============================
   *  RN에서 Native에 전달하는 props
   * =============================
   */
    // roomId
    @ReactProp(name = "roomId")
    fun setRoomId(
        view: ComposeView,
        value: String?,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.roomId?.value = value ?: ""
    }

    // user Json : 사용자 정보
    @ReactProp(name = "userJson")
    fun setUser(
        view: ComposeView,
        json: String?,
    ) {
        Thread {
            val parsed = safeParseUserJson(json)
            view.post {
                (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.user?.value = parsed
            }
        }.start()
    }

    // messageData Json : 톡방 메시지 목록
    // 중복키 제거 후 시간/키 기준 정렬
    @ReactProp(name = "messagesJson")
    fun setMessagesJson(
        view: ComposeView,
        json: String?,
    ) {
        Thread {
            val s = (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)
            val currentUser = s?.user?.value
            val (parsed, errors) = safeParseMessagesJson(json, currentUser)

            // key 중복 현상 제거
            val dedup =
                parsed
                    .groupBy { it.talkId ?: it.tmpTalkId ?: "${it.userId}-${it.sendDtm}" }
                    .map { (_, list) ->
                        list.reduce { a, b ->
                            val ra = rank(a)
                            val rb = rank(b)
                            when {
                                ra < rb -> a
                                ra > rb -> b
                                // 동점이면 더 나중(sendDtm 큰 쪽) 또는 talkId 사전순으로
                                else -> if ((a.sendDtm ?: "") >= (b.sendDtm ?: "")) a else b
                            }
                        }
                    }

            view.post {
                (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.messages?.value = dedup
                if (errors.isNotEmpty()) sendOnMessagesInvalid(view, errors)
            }
        }.start()
    }

    // userListJson : 대화방 참여자 목록
    @ReactProp(name = "userListJson")
    fun setUserListJson(
        view: ComposeView,
        json: String?,
    ) {
        Thread {
            val parsed = safeParseUserListJson(json)
            view.post {
                (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.userList?.value = parsed
            }
        }.start()
    }

    // unUsedUserListJson : 대화방 미사용자 목록 = chtUnusedMemberList
    @ReactProp(name = "unUsedUserListJson")
    fun setUnUsedUserListJson(
        view: ComposeView,
        json: String?,
    ) {
        Thread {
            val parsed = safeParseUserListJson(json)
            view.post {
                (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.unUsedUserList?.value = parsed
            }
        }.start()
    }

    // talkThemeJson : 톡 테마 컬러
    @ReactProp(name = "talkThemeJson")
    fun setTalkTheme(
        view: ComposeView,
        json: String?,
    ) {
        Thread {
            val colors: List<TalkThemeColors> =
                runCatching {
                    if (json.isNullOrBlank()) {
                        emptyList()
                    } else {
                        val arr = org.json.JSONArray(json)
                        val dto =
                            buildList {
                                for (i in 0 until arr.length()) {
                                    val o = arr.optJSONObject(i) ?: continue
                                    add(
                                        TalkTheme(
                                            backGroundColor = o.optString("backGroundColor"),
                                            otherBox = o.optString("otherBox"),
                                            otherText = o.optString("otherText"),
                                            myBox = o.optString("myBox"),
                                            myText = o.optString("myText"),
                                            unreadCount = o.optString("unreadCount"),
                                            dateTime = o.optString("dateTime"),
                                            userName = o.optString("userName"),
                                            headerCount = o.optString("headerCount"),
                                        ),
                                    )
                                }
                            }
                        // 🔑 DTO(String) -> Color 팔레트
                        dto.map { it.toComposeTheme() }
                    }
                }.getOrElse { emptyList() }

            view.post {
                (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)
                    ?.talkTheme
                    ?.value = colors
            }
        }.start()
    }

    // i18n : 톡방에서 사용하는 번역
    @ReactProp(name = "i18n")
    fun setI18n(
        view: ComposeView,
        map: ReadableMap?,
    ) {
        val parsed: I18nMap =
            map?.let { readable ->
                val iterator = readable.keySetIterator()
                val out = mutableMapOf<String, String>()
                while (iterator.hasNextKey()) {
                    val key = iterator.nextKey()
                    val value = readable.getString(key) ?: ""
                    out[key] = value
                }
                out
            } ?: emptyMap()

        val state = view.getTag(ROOM_STATE_TAG) as? ChatRoomState ?: return
        state.i18n.value = parsed
    }

    // isFetchingNextPage : React Query에서 다음 페이지 fetching 중인지 여부
    @ReactProp(name = "isFetchingNextPage")
    fun setIsFetchingNextPage(
        view: ComposeView,
        value: Boolean,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.isFetchingNextPage?.value = value
    }

    // scrollToBottom : 새 메시지/전송 직후 하단 자동 스크롤 여부
    @ReactProp(name = "scrollToBottom")
    fun setScrollToBottom(
        view: ComposeView,
        value: Boolean,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.scrollToBottom?.value = value
    }

    // scrollToTalkId : 검색, 답장, 공지로 스크롤하는 데에 사용하는 talkId
    @ReactProp(name = "scrollToTalkId")
    fun setScrollToTalkId(
        view: ComposeView,
        value: String?,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.scrollToTalkId?.value = value
    }

    // isScrolling : 검색, 답장, 공지로 스크롤 이동 중인지 여부
    @ReactProp(name = "isScrolling")
    fun setIsScrolling(
        view: ComposeView,
        value: Boolean,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.isScrolling?.value = value
    }

    // 검색 시 결과 스택
    @ReactProp(name = "scrollSeq")
    fun setScrollSeq(
        view: ComposeView,
        value: Int,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.scrollSeq?.value = value
    }

    // highlightQuery : 검색어
    @ReactProp(name = "highlightQuery")
    fun setHighlightQuery(
        view: ComposeView,
        value: String?,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.highlightQuery?.value = value
    }

    // videoTalkId : 영상 미리보기 버튼 클릭 시 Native 비디오 플레이어 보여주기 위한 talkId
    @ReactProp(name = "videoTalkId")
    fun setVideoTalkId(
        view: ComposeView,
        value: String?,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.videoTalkId?.value = value
    }

    // isDownloading : 다운로드 진행 여부
    @ReactProp(name = "isDownloading")
    fun setIsDownloading(
        view: ComposeView,
        value: Boolean,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.isDownloading?.value = value
    }

    // downloadPercent : 다운로드 진행률
    @ReactProp(name = "downloadPercent")
    fun setDownloadPercent(
        view: ComposeView,
        value: Int,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.downloadPercent?.value = value
    }

    // downloadReceived : 다운로드된 파일 크기
    @ReactProp(name = "downloadReceived")
    fun setDownloadReceived(
        view: ComposeView,
        value: Double,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.downloadReceived?.value = value.toLong()
    }

    // downloadTotal : 다운로드 전체 파일 크기
    @ReactProp(name = "downloadTotal")
    fun setDownloadTotal(
        view: ComposeView,
        value: Double,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.downloadTotal?.value = value.toLong()
    }

    // =================================
    //  Native → RN 이벤트 전달 (디스패치)
    // =================================
    private fun themed(view: View): ThemedReactContext? = (view.context as? ThemedReactContext)

    // messageJson parsing 오류 이벤트 전달 함수
    private fun sendOnMessagesInvalid(
        view: View,
        errors: List<String>,
    ) {
        UiThreadUtil.runOnUiThread {
            val event =
                Arguments.createMap().apply {
                    val arr = Arguments.createArray()
                    errors.forEach { arr.pushString(it) }
                    putArray("errors", arr)
                }
            themed(view)?.getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(view.id, "onMessagesInvalid", event)
        }
    }

    // onPress 이벤트 : "Profile", "Message", "ReplyMessage", "UnreadBadge", "Link", "File", "Emoticon", "Share", "Reaction", "Comment", "Mention", "Like", "Cancel"
    private fun sendOnPress(
        view: View,
        msg: ChatMessage,
        target: PressTarget,
        reactionTp: String?,
    ) {
        if (target == PressTarget.File) {
            val state = view.getTag(ROOM_STATE_TAG) as? ChatRoomState
            state?.downloadingTalkId?.value = msg.talkId

            val fileNo = msg.fileNo ?: msg.fileList?.firstOrNull()?.fileNo
            state?.downloadingFileNo?.value = fileNo
        }

        UiThreadUtil.runOnUiThread {
            val event =
                Arguments.createMap().apply {
                    putString("element", target.toWireString())
                    putMap("message", msg.toWritableMap())
                    if (target == PressTarget.Reaction && !reactionTp.isNullOrBlank()) {
                        putString("reactionTp", reactionTp)
                    }
                }
            themed(view)?.getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(view.id, "onPress", event)
        }
    }

    // 메시지 onLongPress 이벤트 전달 함수 : RoomContextMenu 호출
    private fun sendOnMessageLongPress(
        view: View,
        msg: ChatMessage,
        target: PressTarget,
    ) {
        UiThreadUtil.runOnUiThread {
            val event =
                Arguments.createMap().apply {
                    putString("element", target.toWireString())
                    putMap("message", msg.toWritableMap())
                }
            themed(view)?.getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(view.id, "onMessageLongPress", event)
        }
    }

    // 스크롤 상단 도달 이벤트 전달 함수
    private fun sendOnReachTop(view: View) {
        UiThreadUtil.runOnUiThread {
            themed(view)?.getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(view.id, "onReachTop", Arguments.createMap())
        }
    }

    // 이미지 뷰어 버튼 터치 이벤트 전달 함수 : 이미지 선택/전체 저장, 공유, 삭제
    private fun sendOnPressViewerButton(
        view: View,
        mode: SelectMode,
        images: List<ChatFile>,
        index: Int?,
    ) {
        UiThreadUtil.runOnUiThread {
            val event =
                Arguments.createMap().apply {
                    putString(
                        "mode",
                        when (mode) {
                            SelectMode.SaveOne -> "SaveOne"
                            SelectMode.SaveSelected -> "SaveSelected"
                            SelectMode.SaveThis -> "SaveThis"
                            SelectMode.SaveAll -> "SaveAll"
                            SelectMode.Save -> "Save"
                            SelectMode.Share -> "Share"
                            SelectMode.Delete -> "Delete"
                        },
                    )
                    val arr = Arguments.createArray()
                    images.forEach { f ->
                        val m = Arguments.createMap()
                        m.putString("fileNo", f.fileNo)
                        m.putString("corpCd", f.corpCd)
                        m.putString("fileNm", f.fileNm)
                        m.putString("fileTy", f.fileTy)
                        m.putString("fileSize", f.fileSize)
                        m.putString("downloadYn", f.downloadYn)
                        m.putString("talkId", f.talkId)
                        arr.pushMap(m)
                    }
                    putArray("images", arr)
                    if (index != null) {
                        putInt("index", index)
                    } else {
                        putNull("index")
                    }
                }
            themed(view)?.getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(view.id, "onPressViewerButton", event)
        }
    }

    // 이벤트 상수 : RCTEventEmitter.receiveEvent()로 직접 이벤트를 전달하는 방식

    /** RN이 addEventListener 없이 직접 등록하는 방식 */
    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> =
        hashMapOf(
            "onMessagesInvalid" to mapOf("registrationName" to "onMessagesInvalid"),
            "onPress" to mapOf("registrationName" to "onPress"),
            "onMessageLongPress" to mapOf("registrationName" to "onMessageLongPress"),
            "onReachTop" to mapOf("registrationName" to "onReachTop"),
            "onPressViewerButton" to mapOf("registrationName" to "onPressViewerButton"),
        )

    /*
     * View가 destory될 때 Composition/Listener/Coroutine 정리
     * - 누수 방지를 위해 owner 해제, scope.cancel() 수행
     */
    override fun onDropViewInstance(view: ComposeView) {
        super.onDropViewInstance(view)
        view.setTag(ROOM_CONTENT_SET_TAG, null)
        view.setTag(ROOM_STATE_TAG, null)
        view.disposeComposition()
    }

    /* ============================
     *  JSON parsing
     * =============================
     */

    // messageData JSON parsing
    private fun safeParseMessagesJson(
        json: String?,
        currentUser: User?,
    ): Pair<List<ChatMessage>, List<String>> {
        if (json.isNullOrBlank()) return emptyList<ChatMessage>() to emptyList()
        val errors = mutableListOf<String>()
        val out = mutableListOf<ChatMessage>()
        runCatching {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i)
                if (o == null) {
                    errors += "[$i] not an object"
                    continue
                }
                runCatching { out += o.toChatMessage() }
                    .onFailure { e -> errors += "[$i] ${e.message ?: "parse error"}" }
            }
        }.onFailure { e -> errors += e.message ?: "invalid json" }
        return out to errors
    }

    // user JSON을 parsing해서 User 객체로 mapping
    private fun safeParseUserJson(json: String?): User? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val o = JSONObject(json)
            User(
                userId = o.optString("userId"),
                userNm = o.optNullableString("userNm"),
                corpCd = o.optString("corpCd"),
                deptNm = o.optNullableString("deptNm"),
                jobNm = o.optNullableString("jobNm"),
                iconUrl = o.optNullableString("iconUrl"),
                themeTp = o.optNullableString("themeTp"),
                chatFontLevel = o.optIntOrNull("chatFontLevel"),
                localeTxt = o.optNullableString("localeTxt"),
                prohbYn = o.optNullableString("prohbYn"),
                prohbFrDtm = o.optNullableString("prohbFrDtm"),
                prohbToDtm = o.optNullableString("prohbToDtm"),
            )
        }.getOrElse { null }
    }

    // userList JSON parsing하여 ChatMember에 mapping
    private fun safeParseUserListJson(json: String?): List<ChatMember> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val p = arr.optJSONObject(i) ?: continue
                    add(
                        ChatMember(
                            // 키가 다를 수 있어 유연 매핑
                            userId = p.optString("userId", p.optString("id", "")),
                            userNm = p.optString("userNm", p.optString("name", "")),
                            iconUrl = p.optNullableString("iconUrl") ?: p.optNullableString("profileImgUrl"),
                            roomId = p.optString("roomId"),
                            corpCd = p.optString("corpCd"),
                            // 서버가 deptCd만 줄 수도 있어 deptNm로 폴백 저장
                            deptNm = p.optNullableString("deptNm") ?: p.optNullableString("deptCd"),
                            jobNm = p.optNullableString("jobNm"),
                            statFg = p.optInt("statFg", 0),
                            actionSt = p.optNullableString("actionSt"),
                            lastReadDtm = p.optNullableString("lastReadDtm") ?: p.optNullableString("lastReadTime"),
                            ifUseYn = p.optInt("ifUseYn", 1),
                            vmCertCd = p.optNullableString("vmCertCd"),
                            vmCertNm = p.optNullableString("vmCertNm"),
                            vmMarket = p.optNullableString("vmMarket"),
                            vmMarketNm = p.optNullableString("vmMarketNm"),
                            vmViewYn = p.optNullableString("vmViewYn"),
                            vmWriteCd = p.optNullableString("vmWriteCd"),
                            vmWriteNm = p.optNullableString("vmWriteNm"),
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    // Json 객체 -> ChatMessage로 변환
    private fun JSONObject.toChatMessage(): ChatMessage {
        return ChatMessage(
            talkId = optString("talkId"),
            corpCd = optString("corpCd"),
            roomId = optString("roomId"),
            userId = optString("userId"),
            sendDtm = optString("sendDtm"),
            messageTxt = optString("messageTxt", ""),
            userNm = optNullableString("userNm"),
            fileNo = optNullableString("fileNo"),
            fileNm = optNullableString("fileNm"),
            fileTy = optNullableString("fileTy"),
            ln = optJSONObject("ln")?.let { it.toLink() },
            emoticon = optJSONObject("emoticon")?.let { it.toEmoticon() },
            replyId = optNullableString("replyId"),
            reMessage = optJSONObject("reMessage")?.let { it.toChatMessage() },
            iconUrl = optNullableString("iconUrl"),
            fileSize = optNullableString("fileSize"),
            mentionList = optNullableString("mentionList"),
            likeState = if (has("likeState") && !isNull("likeState")) optBoolean("likeState") else null,
            likeSelect = if (has("likeSelect") && !isNull("likeSelect")) optBoolean("likeSelect") else null,
            likeList = optJSONArray("likeList")?.toLikeList(),
            commentList = optJSONArray("commentList")?.toCommentList(),
            statFg = optIntOrNull("statFg"),
            actionSt = optNullableString("actionSt"),
            commentMentionCnt = optIntOrNull("commentMentionCnt"),
            downloadYn = optNullableString("downloadYn"),
            jobNm = optNullableString("jobNm"),
            parentCommentId = optNullableString("parentCommentId"),
            viewYn = optNullableString("viewYn"),
            emoticonId = optNullableString("emoticonId"),
            emoticonFileNo = optNullableString("emoticonFileNo"),
            fileList = optJSONArray("fileList")?.toFileList(),
            deptNm = optNullableString("deptNm"),
            vmMarket = optNullableString("vmMarket"),
            vmMarketNm = optNullableString("vmMarketNm"),
            vmWriteCd = optNullableString("vmWriteCd"),
            vmWriteNm = optNullableString("vmWriteNm"),
            vmViewYn = optNullableString("vmViewYn"),
            vmCertCd = optNullableString("vmCertCd"),
            vmCertNm = optNullableString("vmCertNm"),
            tmpTalkId = optNullableString("tmpTalkId"),
            status = optNullableString("status"),
            isEdited = if (has("isEdited") && !isNull("isEdited")) optBoolean("isEdited") else null,
        )
    }

    // Json 객체 -> Link로 변환
    private fun JSONObject.toLink() =
        Link(
            title = optNullableString("title"),
            image = optNullableString("image"),
            description = optNullableString("description"),
            url = optNullableString("url"),
        )

    // Json 객체 -> Emoticon으로 변환
    private fun JSONObject.toEmoticon() =
        Emoticon(
            emoticonId = optString("emoticonId"),
            emoticonFileNo = optNullableString("emoticonFileNo"),
            emoticonTp = optString("emoticonTp"),
            emoticonNm = optString("emoticonNm"),
            filePath = optString("filePath"),
            fileTy = optString("fileTy"),
            fileNm = optString("fileNm"),
            fileNo = optNullableString("fileNo"),
            empty = if (has("empty") && !isNull("empty")) optBoolean("empty") else null,
        )

    // Json 배열 -> ChatMember로 변환
    private fun JSONArray.toChatMemberList(): List<ChatMember> =
        buildList {
            for (i in 0 until length()) {
                val o = optJSONObject(i) ?: continue
                add(
                    ChatMember(
                        userId = o.optString("userId"),
                        userNm = o.optString("userNm"),
                        iconUrl = o.optNullableString("iconUrl"),
                        roomId = o.optString("roomId"),
                        corpCd = o.optString("corpCd"),
                        deptNm = o.optNullableString("deptNm"),
                        jobNm = o.optNullableString("jobNm"),
                        statFg = o.optInt("statFg", 0),
                        actionSt = o.optNullableString("actionSt"),
                        ifUseYn = o.optInt("ifUseYn", 1),
                        vmCertCd = o.optNullableString("vmCertCd"),
                        vmCertNm = o.optNullableString("vmCertNm"),
                        vmMarket = o.optNullableString("vmMarket"),
                        vmMarketNm = o.optNullableString("vmMarketNm"),
                        vmViewYn = o.optNullableString("vmViewYn"),
                        vmWriteCd = o.optNullableString("vmWriteCd"),
                        vmWriteNm = o.optNullableString("vmWriteNm"),
                    ),
                )
            }
        }

    // Json 배열 -> LikeList로 변환
    private fun JSONArray.toLikeList(): List<LikeList> =
        buildList {
            for (i in 0 until length()) {
                val o = optJSONObject(i) ?: continue
                add(
                    LikeList(
                        userId = o.optString("userId"),
                        type = o.optString("type"),
                        name = o.optString("name"),
                        jobNm = o.optString("jobNm"),
                        deptNm = o.optNullableString("deptNm"),
                        iconUrl = o.optNullableString("iconUrl"),
                        statFg = o.optNullableString("statFg"),
                        insertDateTime = o.optNullableString("insertDateTime"),
                    ),
                )
            }
        }

    // Json 배열 -> CommentList로 변환
    private fun JSONArray.toCommentList(): List<CommentList> =
        buildList {
            for (i in 0 until length()) {
                val o = optJSONObject(i) ?: continue
                add(
                    CommentList(
                        commentId = o.optString("commentId"),
                        insertUser = o.optString("insertUser"),
                        commentTxt = o.optString("commentTxt"),
                        userNm = o.optString("userNm"),
                        jobNm = o.optString("jobNm"),
                        iconUrl = o.optNullableString("iconUrl"),
                        insertDatetime = o.optString("insertDatetime"),
                        statFg = o.optInt("statFg"),
                        mentionList = o.optNullableString("mentionList"),
                        parentCommentId = o.optNullableString("parentCommentId"),
                        child = o.optJSONArray("child")?.toCommentList(),
                    ),
                )
            }
        }

    // Json 배열 -> ChatFile로 변환
    private fun JSONArray.toFileList(): List<ChatFile> =
        buildList {
            for (i in 0 until length()) {
                val o = optJSONObject(i) ?: continue
                add(
                    ChatFile(
                        fileNo = o.optNullableString("fileNo"),
                        fileNm = o.optNullableString("fileNm"),
                        fileTy = o.optNullableString("fileTy"),
                        fileSize = o.optNullableString("fileSize"),
                        corpCd = o.optNullableString("corpCd"),
                        downloadYn = o.optNullableString("downloadYn"),
                        talkId = o.optNullableString("talkId"),
                    ),
                )
            }
        }

    // ============================
    //  직렬화/매핑 유틸
    // ============================

    // 톡리스트, 톡방에서 터치하는 타겟 직렬화
    private fun PressTarget.toWireString(): String =
        when (this) {
            PressTarget.Profile -> "Profile"
            PressTarget.Message -> "Message"
            PressTarget.ReplyMessage -> "ReplyMessage"
            PressTarget.UnreadBadge -> "UnreadBadge"
            PressTarget.Link -> "Link"
            PressTarget.File -> "File"
            PressTarget.Emoticon -> "Emoticon"
            PressTarget.Video -> "Video"
            PressTarget.Share -> "Share"
            PressTarget.Reaction -> "Reaction"
            PressTarget.Comment -> "Comment"
            PressTarget.Mention -> "Mention"
            PressTarget.Like -> "Like"
            PressTarget.Cancel -> "Cancel"
            PressTarget.Room -> "Room"
            PressTarget.RoomProfile -> "RoomProfile"
        }

    // ChatMessage → RN Map 직렬화(중첩 구조 포함)
    private fun ChatMessage.toWritableMap(): WritableMap =
        Arguments.createMap().apply {
            putString("talkId", talkId)
            putString("corpCd", corpCd)
            putString("roomId", roomId)
            putString("userId", userId)
            putString("sendDtm", sendDtm)
            putString("messageTxt", messageTxt)
            putString("userNm", userNm)
            putString("fileNo", fileNo)
            putString("fileNm", fileNm)
            putString("fileTy", fileTy)
            putString("iconUrl", iconUrl)
            putString("fileSize", fileSize)
            putString("mentionList", mentionList)
            putString("replyId", replyId)
            putString("actionSt", actionSt)
            putString("downloadYn", downloadYn)
            putString("jobNm", jobNm)
            putString("parentCommentId", parentCommentId)
            putString("viewYn", viewYn)
            putString("emoticonId", emoticonId)
            putString("emoticonFileNo", emoticonFileNo)
            putString("deptNm", deptNm)
            putString("vmMarket", vmMarket)
            putString("vmMarketNm", vmMarketNm)
            putString("vmWriteCd", vmWriteCd)
            putString("vmWriteNm", vmWriteNm)
            putString("vmViewYn", vmViewYn)
            putString("vmCertCd", vmCertCd)
            putString("vmCertNm", vmCertNm)
            putString("tmpTalkId", tmpTalkId)
            putString("status", status)

            if (statFg != null) putInt("statFg", statFg!!) else putNull("statFg")
            if (commentMentionCnt != null) putInt("commentMentionCnt", commentMentionCnt!!) else putNull("commentMentionCnt")
            if (likeState != null) putBoolean("likeState", likeState!!) else putNull("likeState")
            if (likeSelect != null) putBoolean("likeSelect", likeSelect!!) else putNull("likeSelect")
            if (isEdited != null) putBoolean("isEdited", isEdited!!) else putNull("isEdited")

            // ln
            if (ln != null) {
                val m = Arguments.createMap()
                m.putString("title", ln.title)
                m.putString("image", ln.image)
                m.putString("description", ln.description)
                m.putString("url", ln.url)
                putMap("ln", m)
            } else {
                putNull("ln")
            }

            // emoticon
            if (emoticon != null) {
                val m = Arguments.createMap()
                m.putString("emoticonId", emoticon.emoticonId)
                m.putString("emoticonFileNo", emoticon.emoticonFileNo)
                m.putString("emoticonTp", emoticon.emoticonTp)
                m.putString("emoticonNm", emoticon.emoticonNm)
                m.putString("filePath", emoticon.filePath)
                m.putString("fileTy", emoticon.fileTy)
                m.putString("fileNm", emoticon.fileNm)
                m.putString("fileNo", emoticon.fileNo)
                if (emoticon.empty != null) m.putBoolean("empty", emoticon.empty!!) else m.putNull("empty")
                putMap("emoticon", m)
            } else {
                putNull("emoticon")
            }

            // likeList
            if (!likeList.isNullOrEmpty()) {
                val arr = Arguments.createArray()
                likeList!!.forEach { lk ->
                    val m = Arguments.createMap()
                    m.putString("userId", lk.userId)
                    m.putString("type", lk.type)
                    m.putString("name", lk.name)
                    m.putString("jobNm", lk.jobNm)
                    m.putString("deptNm", lk.deptNm)
                    m.putString("iconUrl", lk.iconUrl)
                    m.putString("statFg", lk.statFg)
                    m.putString("insertDateTime", lk.insertDateTime)
                    arr.pushMap(m)
                }
                putArray("likeList", arr)
            } else {
                putNull("likeList")
            }

            // commentList (재귀)
            if (!commentList.isNullOrEmpty()) {
                putArray("commentList", commentList!!.toWritableArrayComment())
            } else {
                putNull("commentList")
            }

            // fileList
            if (!fileList.isNullOrEmpty()) {
                val arr = Arguments.createArray()
                fileList!!.forEach { f ->
                    val m = Arguments.createMap()
                    m.putString("fileNo", f.fileNo)
                    m.putString("fileNm", f.fileNm)
                    m.putString("fileTy", f.fileTy)
                    m.putString("fileSize", f.fileSize)
                    m.putString("corpCd", f.corpCd)
                    m.putString("downloadYn", f.downloadYn)
                    m.putString("talkId", f.talkId)
                    arr.pushMap(m)
                }
                putArray("fileList", arr)
            } else {
                putNull("fileList")
            }

            // reMessage
            if (reMessage != null) {
                putMap("reMessage", reMessage!!.toWritableMap())
            } else {
                putNull("reMessage")
            }
        }

    private fun List<CommentList>.toWritableArrayComment(): WritableArray {
        val arr = Arguments.createArray()
        this.forEach { c ->
            val m = Arguments.createMap()
            m.putString("commentId", c.commentId)
            m.putString("insertUser", c.insertUser)
            m.putString("commentTxt", c.commentTxt)
            m.putString("userNm", c.userNm)
            m.putString("jobNm", c.jobNm)
            m.putString("iconUrl", c.iconUrl)
            m.putString("insertDatetime", c.insertDatetime)
            m.putInt("statFg", c.statFg)
            m.putString("mentionList", c.mentionList)
            m.putString("parentCommentId", c.parentCommentId)
            if (!c.child.isNullOrEmpty()) m.putArray("child", c.child!!.toWritableArrayComment()) else m.putNull("child")
            arr.pushMap(m)
        }
        return arr
    }
}
