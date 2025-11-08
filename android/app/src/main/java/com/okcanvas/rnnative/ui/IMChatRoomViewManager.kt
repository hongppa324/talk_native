package com.vmerp.works.ui

import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.vmerp.works.model.ChatFile
import com.vmerp.works.model.ChatMember
import com.vmerp.works.model.ChatMessage
import com.vmerp.works.model.CommentList
import com.vmerp.works.model.Emoticon
import com.vmerp.works.model.LikeList
import com.vmerp.works.model.Link
import com.vmerp.works.model.PressTarget
import com.vmerp.works.model.SelectMode
import com.vmerp.works.model.User
import com.vmerp.works.util.*
import org.json.JSONArray
import org.json.JSONObject

// 고유 태그 키 (시스템 상수 사용 금지)
private const val ROOM_STATE_TAG = 0x0C11A7CB
private const val ROOM_CONTENT_SET_TAG = 0x51B0A9E1

// 파일 전용 상태 타입
private data class ChatRoomState(
    val roomId: androidx.compose.runtime.MutableState<String>,
    val messages: androidx.compose.runtime.MutableState<List<ChatMessage>>,
    val user: androidx.compose.runtime.MutableState<User?>,
    val userList: androidx.compose.runtime.MutableState<List<ChatMember>>,
    val isFetchingNextPage: androidx.compose.runtime.MutableState<Boolean>,
    val scrollToBottom: androidx.compose.runtime.MutableState<Boolean>,
    val scrollToTalkId: androidx.compose.runtime.MutableState<String?>,
)

// * 패키지 내 동일한 경로에 선언하는 class들은 서로 이름이 달라야함 (톡리스트 Lifecycle class 이름과 다르게 설정)
// 톡방 LifecycleOwner 정의: SavedState + ViewModelStore를 갖춘 Owner
private class CahtRooomLifecycleOwner(
    private val context: ThemedReactContext,
) : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner, LifecycleEventListener {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val viewModelStore: ViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    init {
        // (1) SavedState 연결 + 복원 (super.onCreate에 해당)
        savedStateController.performAttach() // attach owner
        savedStateController.performRestore(null) // Bundle 없으면 null

        // (2) 라이프사이클 전이 (onCreate 후 onStart)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        // (3) RN 호스트 생명주기 수신
        context.addLifecycleEventListener(this)
    }

    override fun onHostResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onHostPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onHostDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        context.removeLifecycleEventListener(this)
        // ViewModelStore 정리(선택)
        viewModelStore.clear()
    }
}

class IMChatRoomViewManager(
    private val reactContext: ReactApplicationContext,
) : SimpleViewManager<ComposeView>() {
    override fun getName() = "IMChatRoomView"

    override fun createViewInstance(context: ThemedReactContext): ComposeView {
        val owner = CahtRooomLifecycleOwner(context)

        val view =
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                // 세 가지 Owner 모두 연결
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

        val state =
            ChatRoomState(
                roomId = mutableStateOf("채팅방"),
                messages = mutableStateOf(emptyList()),
                user = mutableStateOf<User?>(null),
                userList = mutableStateOf<List<ChatMember>>(emptyList()),
                isFetchingNextPage = mutableStateOf(false),
                scrollToBottom = mutableStateOf(false),
                scrollToTalkId = mutableStateOf<String?>(null),
            )
        view.setTag(ROOM_STATE_TAG, state)

        // 즉시 1회 setContent
        if (view.getTag(ROOM_CONTENT_SET_TAG) != true) {
            view.setTag(ROOM_CONTENT_SET_TAG, true)
            view.setContent {
                MaterialTheme {
                    ChatRoomView(
                        messages = state.messages.value,
                        currentUser = state.user.value,
                        chtMemberList = state.userList.value,
                        isFetchingNextPage = state.isFetchingNextPage.value,
                        scrollToBottom = state.scrollToBottom.value,
                        scrollToTalkId = state.scrollToTalkId.value,
                        onPress = { msg, target -> sendOnPress(view, msg, target) },
                        onLongPress = { msg, target -> sendOnMessageLongPress(view, msg, target) },
                        onReachTop = { sendOnReachTop(view) },
                        onPressViewerButton = { mode, images, index ->
                            sendOnPressViewerButton(view, mode, images, index)
                        },
                    )
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
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.roomId?.value = value ?: "채팅방"
    }

    // user Json
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

    // messageData Json
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
            fun rank(m: ChatMessage): Int {
                // 낮을수록 우선
                val bad = m.status == "pending" || m.status == "fail"
                return when {
                    !m.talkId.isNullOrBlank() && !bad -> 0 // 서버 확정 최우선
                    !m.talkId.isNullOrBlank() && bad -> 1
                    !m.tmpTalkId.isNullOrBlank() -> 2
                    else -> 3
                }
            }

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

            val sorted =
                dedup.sortedWith(
                    compareBy<ChatMessage> { it.sendDtm.isNullOrBlank() }
                        .thenBy { it.sendDtm ?: "" }
                        .thenBy { it.talkId.orEmpty() }
                        .thenBy { it.tmpTalkId.orEmpty() },
                )

            view.post {
                (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.messages?.value = sorted
                if (errors.isNotEmpty()) sendOnMessagesInvalid(view, errors)
            }
        }.start()
    }

    // userListJson
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

    // isFetchingNextPage
    @ReactProp(name = "isFetchingNextPage")
    fun setIsFetchingNextPage(
        view: ComposeView,
        value: Boolean,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.isFetchingNextPage?.value = value
    }

    // scrollToBottom
    @ReactProp(name = "scrollToBottom")
    fun setScrollToBottom(
        view: ComposeView,
        value: Boolean,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.scrollToBottom?.value = value
    }

    // scrollToTalkId
    @ReactProp(name = "scrollToTalkId")
    fun setScrollToTalkId(
        view: ComposeView,
        value: String?,
    ) {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.scrollToTalkId?.value = value
    }

    // ---------- Native -> RN events ----------
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

    // 클릭 이벤트 전달 함수
    private fun sendOnPress(
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
                ?.receiveEvent(view.id, "onPress", event)
        }
    }

    // 메시지 longPress 이벤트 전달 함수
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

    // 이미지 뷰어 버튼 터치 이벤트 전달 함수
    private fun sendOnPressViewerButton(
        view: View,
        mode: SelectMode,
        images: List<ChatFile>,
        index: Int,
    ) {
        UiThreadUtil.runOnUiThread {
            val event =
                Arguments.createMap().apply {
                    putString(
                        "mode",
                        when (mode) {
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
                    putInt("index", index)
                }
            themed(view)?.getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(view.id, "onPressViewerButton", event)
        }
    }

    // 이벤트 상수 : RCTEventEmitter.receiveEvent()로 직접 이벤트를 전달하는 방식
    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> =
        hashMapOf(
            "onMessagesInvalid" to mapOf("registrationName" to "onMessagesInvalid"),
            "onPress" to mapOf("registrationName" to "onPress"),
            "onMessageLongPress" to mapOf("registrationName" to "onMessageLongPress"),
            "onReachTop" to mapOf("registrationName" to "onReachTop"),
            "onPressViewerButton" to mapOf("registrationName" to "onPressViewerButton"),
        )

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

    // ---------- messageData JSON parsing ----------
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

    // ---------- user JSON parsing ----------
    private fun safeParseUserJson(json: String?): User? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val o = JSONObject(json)
            User(
                userId = o.optString("userId"),
                userNm = o.optNullableString("userNm"),
                corpCd = o.optNullableString("corpCd"),
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

    // ---------- userList JSON parsing ----------
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
                            roomId = p.optNullableString("roomId"),
                            corpCd = p.optNullableString("corpCd"),
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

    /** message 객체 → ChatMessage */
    private fun JSONObject.toChatMessage(): ChatMessage {
        return ChatMessage(
            talkId = optNullableString("talkId"),
            corpCd = optNullableString("corpCd"),
            roomId = optNullableString("roomId"),
            userId = optNullableString("userId"),
            sendDtm = optNullableString("sendDtm"),
            messageTxt = stripTag(optNullableString("messageTxt")),
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

    private fun JSONObject.toLink() =
        Link(
            title = optNullableString("title"),
            image = optNullableString("image"),
            description = optNullableString("description"),
            url = optNullableString("url"),
        )

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

    private fun JSONArray.toChatMemberList(): List<ChatMember> =
        buildList {
            for (i in 0 until length()) {
                val o = optJSONObject(i) ?: continue
                add(
                    ChatMember(
                        userId = o.optString("userId"),
                        userNm = o.optString("userNm"),
                        iconUrl = o.optNullableString("iconUrl"),
                        roomId = o.optNullableString("roomId"),
                        corpCd = o.optNullableString("corpCd"),
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

    // messageTxt에서 html tag 제거
    private fun stripTag(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "") // <br/> 제거
            .replace("\n", " ") // 개행 문자 → 공백
            .replace(Regex("<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(\\/)?>(.*?)", RegexOption.IGNORE_CASE), "") // 모든 HTML 태그 제거
            .replace(Regex("(<([^>]+)>)", RegexOption.IGNORE_CASE), "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }

    // 톡리스트, 톡방에서 터치하는 타겟 직렬화
    private fun PressTarget.toWireString(): String =
        when (this) {
            PressTarget.Profile -> "Profile"
            PressTarget.Message -> "Message"
            PressTarget.UnreadBadge -> "UnreadBadge"
            PressTarget.Link -> "Link"
            PressTarget.File -> "File"
            PressTarget.Room -> "Room"
            PressTarget.RoomProfile -> "RoomProfile"
        }

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
            putString("isEdited", isEdited)

            if (statFg != null) putInt("statFg", statFg!!) else putNull("statFg")
            if (commentMentionCnt != null) putInt("commentMentionCnt", commentMentionCnt!!) else putNull("commentMentionCnt")
            if (likeState != null) putBoolean("likeState", likeState!!) else putNull("likeState")
            if (likeSelect != null) putBoolean("likeSelect", likeSelect!!) else putNull("likeSelect")

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

private fun List<ChatMessage>.sortedForRoom(): List<ChatMessage> =
    this.sortedWith(
        compareBy<ChatMessage> { it.sendDtm.isNullOrBlank() } // null/빈값을 가장 과거로
            .thenBy { it.sendDtm ?: "" } // ISO-8601 문자열 기준 오름차순
            .thenBy { it.talkId?.toLongOrNull() ?: Long.MIN_VALUE } // 동시간대 tie-break
            .thenBy { it.tmpTalkId.orEmpty() },
    )
