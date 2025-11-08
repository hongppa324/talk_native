package com.vmerp.works.ui

import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.MutableState
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
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.vmerp.works.model.ChatMember
import com.vmerp.works.model.ChatRoom
import com.vmerp.works.model.PressTarget
import com.vmerp.works.model.User
import com.vmerp.works.util.*
import org.json.JSONArray
import org.json.JSONObject

// 고유 태그 키 (시스템 상수 사용 금지)
private const val LIST_STATE_TAG = 0x7C11A7CB
private const val LIST_CONTENT_SET_TAG = 0x71B0A9E2

// 이 파일 전용 상태 타입
private data class ChatListState(
    val rooms: MutableState<List<ChatRoom>>,
    val user: MutableState<User?>,
    val talkListRaw: MutableState<String?>,
    val refreshing: MutableState<Boolean>,
)

// 톡 리스트 LifecycleOwner 정의: SavedState + ViewModelStore를 갖춘 Owner
private class ChatListLifecycleOwner(
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

class IMChatListViewManager(
    private val reactContext: ReactApplicationContext,
) : SimpleViewManager<ComposeView>() {
    override fun getName() = "IMChatListView"

    override fun createViewInstance(context: ThemedReactContext): ComposeView {
        val owner = ChatListLifecycleOwner(context)

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
            ChatListState(
                rooms = mutableStateOf<List<ChatRoom>>(emptyList()),
                user = mutableStateOf<User?>(null),
                talkListRaw = mutableStateOf<String?>(null),
                refreshing = mutableStateOf(false),
            )
        view.setTag(LIST_STATE_TAG, state)

        // 최초 1회 setContent
        if (view.getTag(LIST_CONTENT_SET_TAG) != true) {
            view.setTag(LIST_CONTENT_SET_TAG, true)
            view.setContent {
                MaterialTheme {
                    ChatListView(
                        rooms = state.rooms.value,
                        currentUser = state.user.value,
                        onPress = { room, target -> sendOnPress(view, room, target) },
                        onLongPress = { room -> sendOnLongPress(view, room) },
                        onPin = { room -> sendOnPin(view, room) },
                        onMute = { room -> sendOnMute(view, room) },
                        onRefresh = { sendOnRefresh(view) },
                        rnRefreshing = state.refreshing.value,
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

    // user Json
    @ReactProp(name = "userJson")
    fun setUser(
        view: ComposeView,
        json: String?,
    ) {
        Thread {
            val parsed = safeParseUserJson(json)
            view.post {
                val s = (view.getTag(LIST_STATE_TAG) as? ChatListState) ?: return@post
                s.user.value = parsed
                val raw = s.talkListRaw.value
                if (!raw.isNullOrBlank()) {
                    val list = safeParseTalkListJson(raw, parsed)
                    s.rooms.value = list
                }
            }
        }.start()
    }

    // talkListData Json
    @ReactProp(name = "talkListJson")
    fun setTalkListJson(
        view: ComposeView,
        json: String?,
    ) {
        Thread {
            val s = (view.getTag(LIST_STATE_TAG) as? ChatListState)
            val currentUser = s?.user?.value
            // 유저가 아직 없으면 저장만 하고 return
            if (currentUser == null) {
                view.post {
                    s?.talkListRaw?.value = json
                }
                return@Thread
            }
            val parsed = safeParseTalkListJson(json, currentUser)
            view.post {
                s!!.talkListRaw.value = json
                s.rooms.value = parsed
            }
        }.start()
    }

    @ReactProp(name = "refreshing")
    fun setRefreshing(
        view: ComposeView,
        refreshing: Boolean,
    ) {
        view.post {
            val s = view.getTag(LIST_STATE_TAG) as? ChatListState ?: return@post
            s.refreshing.value = refreshing
        }
    }

    // ---------- Native -> RN events ----------
    private fun themed(view: View) = (view.context as? ThemedReactContext)

    private fun sendOnPress(
        view: View,
        room: ChatRoom,
        target: PressTarget,
    ) {
        UiThreadUtil.runOnUiThread {
            val payload =
                Arguments.createMap().apply {
                    putString("element", target.toWireString()) // "Room" | "RoomProfile"
                    putMap("room", room.toWritableMap())
                }
            themed(view)?.getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(view.id, "onPress", payload)
        }
    }

    private fun sendOnLongPress(
        view: View,
        room: ChatRoom,
    ) {
        UiThreadUtil.runOnUiThread {
            val payload =
                Arguments.createMap().apply {
                    putMap("room", room.toWritableMap())
                }
            themed(view)?.getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(view.id, "onLongPress", payload)
        }
    }

    private fun sendOnPin(
        view: View,
        room: ChatRoom,
    ) {
        UiThreadUtil.runOnUiThread {
            val payload =
                Arguments.createMap().apply {
                    putMap("room", room.toWritableMap())
                }
            themed(view)?.getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(view.id, "onPin", payload)
        }
    }

    private fun sendOnMute(
        view: View,
        room: ChatRoom,
    ) {
        UiThreadUtil.runOnUiThread {
            val payload =
                Arguments.createMap().apply {
                    putMap("room", room.toWritableMap())
                }
            themed(view)?.getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(view.id, "onMute", payload)
        }
    }

    private fun sendOnRefresh(view: View) {
        UiThreadUtil.runOnUiThread {
            themed(view)?.getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(view.id, "onRefresh", null)
        }
    }

    // 이벤트 상수 : RCTEventEmitter.receiveEvent()로 직접 이벤트를 전달하는 방식
    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> =
        hashMapOf(
            "onPress" to mapOf("registrationName" to "onPress"),
            "onLongPress" to mapOf("registrationName" to "onLongPress"),
            "onPin" to mapOf("registrationName" to "onPin"),
            "onMute" to mapOf("registrationName" to "onMute"),
            "onRefresh" to mapOf("registrationName" to "onRefresh"),
        )

    override fun onDropViewInstance(view: ComposeView) {
        super.onDropViewInstance(view)
        view.setTag(LIST_CONTENT_SET_TAG, null)
        view.setTag(LIST_STATE_TAG, null)
        view.disposeComposition()
    }

  /* ============================
   *  JSON parsing
   * =============================
   */

    // ---------- talkListData JSON parsing ----------
    private fun safeParseTalkListJson(
        json: String?,
        currentUser: User?,
    ): List<ChatRoom> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching<List<ChatRoom>> {
            val arr = JSONArray(json)
            val list =
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        add(o.toChatRoom())
                    }
                }

            list.sortedForList(currentUser?.userId)
        }.getOrElse { emptyList() }
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

    /** talkListData 객체 → ChatRoom */
    private fun JSONObject.toChatRoom(): ChatRoom {
        fun JSONArray.toMemberList(): List<ChatMember> =
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
                        ),
                    )
                }
            }

        val members = (optJSONArray("chtMemberList") ?: JSONArray()).toMemberList()
        val unused = (optJSONArray("chtUnusedMemberList") ?: JSONArray()).toMemberList()

        val noticeArray = optJSONArray("noticeList") ?: JSONArray()
        val notices =
            buildList {
                for (i in 0 until noticeArray.length()) add(noticeArray.opt(i))
            }

        return ChatRoom(
            chatNoticeYn = optAnyToString("chatNoticeYn"),
            chtMemberList = members,
            chtUnusedMemberList = unused,
            corpCd = optNullableString("corpCd"),
            emoticonFileNo = optNullableString("emoticonFileNo"),
            emoticonId = optNullableString("emoticonId"),
            fileNm = optNullableString("fileNm"),
            iconUrl = optNullableString("iconUrl"),
            lastMsgTxt = optNullableString("lastMsgTxt"),
            lastMsgTxtDeleteTime = optNullableString("lastMsgTxtDeleteTime"),
            lastSendDtm = optNullableString("lastSendDtm"),
            mentionYn = optAnyToInt("mentionYn"),
            noticeList = notices,
            pin = opt("pin"),
            privateRoomId = optNullableString("privateRoomId"),
            projectId = optNullableString("projectId"),
            roomId = optString("roomId"),
            titleTxt = optNullableString("titleTxt"),
            titleTxtMe = optNullableString("titleTxtMe"),
            unReadCnt = optInt("unReadCnt", 0),
            userCnt = optInt("userCnt", 0),
        )
    }
}
