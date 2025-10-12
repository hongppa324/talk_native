package com.okcanvas.rnnative.ui

import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.okcanvas.rnnative.model.ChatRoom
import com.okcanvas.rnnative.model.Participant
import org.json.JSONArray
import org.json.JSONObject

// 고유 태그 키 (시스템 상수 사용 금지)
private const val LIST_STATE_TAG = 0x7C11A7CB
private const val LIST_CONTENT_SET_TAG = 0x71B0A9E2

// 이 파일 전용 상태 타입
private data class ChatListState(
  val rooms: androidx.compose.runtime.MutableState<List<ChatRoom>>
)

class IMChatListViewManager(
  private val reactContext: ReactApplicationContext
) : SimpleViewManager<ComposeView>() {

  override fun getName() = "IMChatListView"

  override fun createViewInstance(context: ThemedReactContext): ComposeView {
    val view = ComposeView(context).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    }

    val state = ChatListState(rooms = mutableStateOf(emptyList()))
    view.setTag(LIST_STATE_TAG, state)

    // 최초 1회 setContent
    if (view.getTag(LIST_CONTENT_SET_TAG) != true) {
      view.setTag(LIST_CONTENT_SET_TAG, true)
      view.setContent {
        MaterialTheme {
          ChatListView(
            rooms = state.rooms.value,
            onOpenRoom = { room -> sendOnOpenRoom(view, room.id, room.title) },
            onPin = { id -> sendOnPin(view, id) },
            onMute = { id -> sendOnMute(view, id) },
            onLongPress = { room -> sendOnLongPress(view, room.id) },
            showHeaders = true
          )
        }
      }
    }
    return view
  }

  // ---------- RN -> Native props ----------
  @ReactProp(name = "roomsJson")
  fun setRoomsJson(view: ComposeView, json: String?) {
    Thread {
      val parsed = safeParseRoomsJson(json)
      view.post {
        (view.getTag(LIST_STATE_TAG) as? ChatListState)?.rooms?.value = parsed
      }
    }.start()
  }

  // ---------- Native -> RN events ----------
  private fun themed(view: View) = (view.context as? ThemedReactContext)

  private fun sendOnOpenRoom(view: View, roomId: String, title: String) {
    UiThreadUtil.runOnUiThread {
      val payload = Arguments.createMap().apply {
        putString("roomId", roomId)
        putString("title", title)
      }
      themed(view)?.getJSModule(RCTEventEmitter::class.java)
        ?.receiveEvent(view.id, "onOpenRoom", payload)
    }
  }

  private fun sendOnPin(view: View, roomId: String) {
    UiThreadUtil.runOnUiThread {
      val payload = Arguments.createMap().apply { putString("roomId", roomId) }
      themed(view)?.getJSModule(RCTEventEmitter::class.java)
        ?.receiveEvent(view.id, "onPin", payload)
    }
  }

  private fun sendOnMute(view: View, roomId: String) {
    UiThreadUtil.runOnUiThread {
      val payload = Arguments.createMap().apply { putString("roomId", roomId) }
      themed(view)?.getJSModule(RCTEventEmitter::class.java)
        ?.receiveEvent(view.id, "onMute", payload)
    }
  }

  private fun sendOnLongPress(view: View, roomId: String) {
    UiThreadUtil.runOnUiThread {
      val payload = Arguments.createMap().apply { putString("roomId", roomId) }
      themed(view)?.getJSModule(RCTEventEmitter::class.java)
        ?.receiveEvent(view.id, "onLongPress", payload)
    }
  }

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> =
    hashMapOf(
      "onOpenRoom" to mapOf("registrationName" to "onOpenRoom"),
      "onPin" to mapOf("registrationName" to "onPin"),
      "onMute" to mapOf("registrationName" to "onMute"),
      "onLongPress" to mapOf("registrationName" to "onLongPress"),
    )

  override fun onDropViewInstance(view: ComposeView) {
    super.onDropViewInstance(view)
    view.setTag(LIST_CONTENT_SET_TAG, null)
    view.setTag(LIST_STATE_TAG, null)
    view.disposeComposition()
  }

  // ---------- JSON parsing ----------
  private fun safeParseRoomsJson(json: String?): List<ChatRoom> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching<List<ChatRoom>> {
      val arr = JSONArray(json)
      buildList<ChatRoom> {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          add(o.toChatRoom())
        }
      }.sortedForList() // ✅ pinned → timestamp → unread → id
    }.getOrElse { emptyList() }
  }

  private fun JSONObject.toChatRoom(): ChatRoom {
    val id = optString("id")
    val title = optString("title")
    val lastMessage = optString("lastMessage", "")
    val unread = optInt("unread", 0)
    val pinned = optBoolean("pinned", false)
    val muted = optBoolean("muted", false)

    // timestamp: Long? (없거나 <=0이면 null)
    val timestamp: Long? = if (has("timestamp")) {
      optLong("timestamp", 0L).takeIf { it > 0L }
    } else null

    val participants = optJSONArray("participants")?.let { ja ->
      buildList<Participant> {
        for (i in 0 until ja.length()) {
          val p = ja.optJSONObject(i) ?: continue
          add(
            Participant(
              id = p.optString("id"),
              name = p.optString("name"),
              avatarUrl = p.optString("avatarUrl", p.optString("avatar", null))
            )
          )
        }
      }
    } ?: emptyList()

    return ChatRoom(
      id = id,
      title = title,
      lastMessage = lastMessage,
      unread = unread,
      pinned = pinned,
      muted = muted,
      participants = participants,
      timestamp = timestamp
    )
  }
}

/* =========================
 *  정렬 확장함수 (최신순 상단)
 * =========================
 * - pinned(true) 우선
 * - 같은 그룹 내 timestamp 내림차순(최신 위)
 * - 그 다음 unread, 마지막 id(숫자 우선 비교)
 */
private fun List<ChatRoom>.sortedForList(): List<ChatRoom> =
  this.sortedWith(
    compareByDescending<ChatRoom> { it.pinned }
      .thenByDescending { it.timestamp ?: Long.MIN_VALUE }
      .thenByDescending { it.unread }
      .thenByDescending { it.id.toLongOrNull() ?: Long.MIN_VALUE }
  )
