package com.okcanvas.rnnative.ui

import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.okcanvas.rnnative.model.ChatMessage
import com.okcanvas.rnnative.model.MessageType
import com.okcanvas.rnnative.model.MsgTapTarget
import com.okcanvas.rnnative.model.Reaction
import org.json.JSONArray
import org.json.JSONObject

// 고유 태그 키 (시스템 상수 사용 금지)
private const val ROOM_STATE_TAG = 0x0C11A7CB
private const val ROOM_CONTENT_SET_TAG = 0x51B0A9E1

// 파일 전용 상태 타입
private data class ChatRoomState(
  val roomId: androidx.compose.runtime.MutableState<String>,
  val messages: androidx.compose.runtime.MutableState<List<ChatMessage>>
)

class IMChatRoomViewManager(
  private val reactContext: ReactApplicationContext
) : SimpleViewManager<ComposeView>() {

  override fun getName() = "IMChatRoomView"

  override fun createViewInstance(reactContext: ThemedReactContext): ComposeView {
    val view = ComposeView(reactContext).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    }

    val state = ChatRoomState(
      roomId = mutableStateOf("채팅방"),
      messages = mutableStateOf(emptyList())
    )
    view.setTag(ROOM_STATE_TAG, state)

    // 즉시 1회 setContent
    if (view.getTag(ROOM_CONTENT_SET_TAG) != true) {
      view.setTag(ROOM_CONTENT_SET_TAG, true)
      view.setContent {
        MaterialTheme {
          ChatRoomView(
            roomId = state.roomId.value,
            initialMessages = state.messages.value,
            onBack = { sendOnBack(view) },
            onClick = { msg, target -> sendOnMessageClick(view, msg, target) },
            onLongPress = { msg, target -> sendOnMessageLongPress(view, msg, target) }
          )
        }
      }
    }
    return view
  }

  /* ---------- RN -> Native props ---------- */

  @ReactProp(name = "roomId")
  fun setRoomId(view: ComposeView, value: String?) {
    (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.roomId?.value = value ?: "채팅방"
  }

  @ReactProp(name = "messagesJson")
  fun setMessagesJson(view: ComposeView, json: String?) {
    Thread {
      val (parsed, errors) = safeParseMessagesJson(json)
      view.post {
        (view.getTag(ROOM_STATE_TAG) as? ChatRoomState)?.messages?.value = parsed
        if (errors.isNotEmpty()) sendOnMessagesInvalid(view, errors)
      }
    }.start()
  }

  /* ---------- Native -> RN events ---------- */

  private fun themed(view: View): ThemedReactContext? = (view.context as? ThemedReactContext)

  private fun sendOnBack(view: View) {
    UiThreadUtil.runOnUiThread {
      themed(view)?.getJSModule(RCTEventEmitter::class.java)
        ?.receiveEvent(view.id, "onBack", Arguments.createMap())
    }
  }

  private fun sendOnMessagesInvalid(view: View, errors: List<String>) {
    UiThreadUtil.runOnUiThread {
      val event = Arguments.createMap().apply {
        val arr = Arguments.createArray()
        errors.forEach { arr.pushString(it) }
        putArray("errors", arr)
      }
      themed(view)?.getJSModule(RCTEventEmitter::class.java)
        ?.receiveEvent(view.id, "onMessagesInvalid", event)
    }
  }

  private fun sendOnMessageClick(view: View, msg: ChatMessage, target: MsgTapTarget) {
    UiThreadUtil.runOnUiThread {
      val event = Arguments.createMap().apply {
        putString("element", target.toWireString())
        putMap("message", msg.toWritableMap())
      }
      themed(view)?.getJSModule(RCTEventEmitter::class.java)
        ?.receiveEvent(view.id, "onMessageClick", event)
    }
  }

  private fun sendOnMessageLongPress(view: View, msg: ChatMessage, target: MsgTapTarget) {
    UiThreadUtil.runOnUiThread {
      val event = Arguments.createMap().apply {
        putString("element", target.toWireString())
        putMap("message", msg.toWritableMap())
      }
      themed(view)?.getJSModule(RCTEventEmitter::class.java)
        ?.receiveEvent(view.id, "onMessageLongPress", event)
    }
  }

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> =
    hashMapOf(
      "onBack" to mapOf("registrationName" to "onBack"),
      "onMessagesInvalid" to mapOf("registrationName" to "onMessagesInvalid"),
      "onMessageClick" to mapOf("registrationName" to "onMessageClick"),
      "onMessageLongPress" to mapOf("registrationName" to "onMessageLongPress")
    )

  override fun onDropViewInstance(view: ComposeView) {
    super.onDropViewInstance(view)
    view.setTag(ROOM_CONTENT_SET_TAG, null)
    view.setTag(ROOM_STATE_TAG, null)
    view.disposeComposition()
  }

  /* ---------- JSON parsing ---------- */

  private fun safeParseMessagesJson(json: String?): Pair<List<ChatMessage>, List<String>> {
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

  private fun JSONObject.toChatMessage(): ChatMessage {
    val id = optString("id", System.nanoTime().toString())
    val sender = optString("sender", "other")
    val text = optString("text", "")
    val time = optString("time", "")
    val date = optString("date", "")
    val displayName = optString("displayName", null)
    val avatarUrl = optString("avatarUrl", null)
    val typeStr = optString("type", "TEXT")
    val type = runCatching { MessageType.valueOf(typeStr) }.getOrElse { MessageType.TEXT }

    val mediaUrl = optString("mediaUrl", null)
    val mediaDuration = optString("mediaDuration", null)
    val fileSize = optString("fileSize", null)
    val thumbnailUrl = optString("thumbnailUrl", null)

    val mediaUrls = optJSONArray("mediaUrls")?.let { ja ->
      buildList {
        for (i in 0 until ja.length()) {
          val u = ja.optString(i)
          if (!u.isNullOrBlank()) add(u)
        }
      }.ifEmpty { null }
    }

    val totalRecipients = if (has("totalRecipients")) optInt("totalRecipients") else null
    val readCount = if (has("readCount")) optInt("readCount") else null

    val reactions = optJSONArray("reactions")?.let { ja ->
      buildList {
        for (i in 0 until ja.length()) {
          val r = ja.optJSONObject(i) ?: continue
          add(Reaction(emoji = r.optString("emoji"), count = r.optInt("count", 1)))
        }
      }
    } ?: emptyList()

    return ChatMessage(
      id = id,
      sender = sender,
      text = text,
      time = time,
      date = date,
      displayName = displayName,
      avatarUrl = avatarUrl,
      type = type,
      mediaUrl = mediaUrl,
      mediaUrls = mediaUrls,
      fileSize = fileSize,
      mediaDuration = mediaDuration,
      thumbnailUrl = thumbnailUrl,
      totalRecipients = totalRecipients ?: 2,
      readCount = readCount ?: 1,
      reactions = reactions
    )
  }

  /* ---------- Serialization to JS ---------- */

  private fun MsgTapTarget.toWireString(): String = when (this) {
    MsgTapTarget.Avatar  -> "Avatar"
    MsgTapTarget.Content -> "Content"
  }

  private fun ChatMessage.toWritableMap(): WritableMap {
    val map = Arguments.createMap()
    map.putString("id", id)
    map.putString("sender", sender)
    map.putString("text", text)
    map.putString("time", time)
    map.putString("date", date)
    if (displayName != null) map.putString("displayName", displayName) else map.putNull("displayName")
    map.putString("type", type.name)
    if (avatarUrl != null) map.putString("avatarUrl", avatarUrl) else map.putNull("avatarUrl")

    if (mediaUrl != null) map.putString("mediaUrl", mediaUrl) else map.putNull("mediaUrl")
    if (thumbnailUrl != null) map.putString("thumbnailUrl", thumbnailUrl) else map.putNull("thumbnailUrl")
    if (fileSize != null) map.putString("fileSize", fileSize) else map.putNull("fileSize")
    if (mediaDuration != null) map.putString("mediaDuration", mediaDuration) else map.putNull("mediaDuration")

    if (mediaUrls != null) {
      val arr: WritableArray = Arguments.createArray()
      mediaUrls.forEach { arr.pushString(it) }
      map.putArray("mediaUrls", arr)
    } else {
      map.putNull("mediaUrls")
    }

    if (totalRecipients != null) map.putInt("totalRecipients", totalRecipients) else map.putNull("totalRecipients")
    if (readCount != null) map.putInt("readCount", readCount) else map.putNull("readCount")

    val reactionsArr: WritableArray = Arguments.createArray()
    reactions.forEach { r ->
      val rm = Arguments.createMap()
      rm.putString("emoji", r.emoji)
      rm.putInt("count", r.count)
      reactionsArr.pushMap(rm)
    }
    map.putArray("reactions", reactionsArr)

    return map
  }
}
