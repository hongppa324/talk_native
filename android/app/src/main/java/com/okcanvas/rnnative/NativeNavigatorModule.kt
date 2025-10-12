// android/app/src/main/java/com/okcanvas/rnnative/NativeNavigatorModule.kt
package com.okcanvas.rnnative

import android.content.Intent
import androidx.activity.ComponentActivity
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class NativeNavigatorModule(private val ctx: ReactApplicationContext)
  : ReactContextBaseJavaModule(ctx) {

  override fun getName(): String = "NativeNavigator"

  /** RN(ReactActivity) 전체 화면 */
//   @ReactMethod
//   fun openChat(roomId: String, messagesJson: String?) {
//     val activity = currentActivity ?: return
//     val safeJson = messagesJson ?: ""
//     activity.runOnUiThread {
//       val intent = Intent(activity, ChatRoomActivity::class.java).apply {
//         putExtra("renderMode", "rn") // 기본 RN 경로(ReactActivity)
//         putExtra("roomId", roomId)
//         putExtra("messagesJson", safeJson)
//       }
//       activity.startActivity(intent)
//     }
//   }

  /** Compose(ComponentActivity) 전체 화면 */
//   @ReactMethod
//   fun openChatCompose(roomId: String, messagesJson: String?) {
//     val activity = currentActivity ?: return
//     val safeJson = messagesJson ?: ""
//     activity.runOnUiThread {
//       val intent = Intent(activity, ChatRoomActivity::class.java).apply {
//         putExtra("renderMode", "compose")
//         putExtra("roomId", roomId)
//         putExtra("messagesJson", safeJson)
//       }
//       activity.startActivity(intent)
//     }
//   }

  /** RN 위에 Compose 오버레이 */
//   @ReactMethod
//   fun openChatOverlay(roomId: String, messagesJson: String?) {
//     val activity = currentActivity ?: return
//     val safeJson = messagesJson ?: ""
//     activity.runOnUiThread {
//       val intent = Intent(activity, ChatRoomActivity::class.java).apply {
//         putExtra("renderMode", "compose_overlay")
//         putExtra("roomId", roomId)
//         putExtra("messagesJson", safeJson)
//       }
//       activity.startActivity(intent)
//     }
//   }

  /** 하이브리드: 상단/하단 RN + 중앙 네이티브(Compose/뷰) */
//   @ReactMethod
//   fun openHybrid(roomId: String, messagesJson: String?) {
//     val activity = currentActivity ?: return
//     val safeJson = messagesJson ?: ""
//     activity.runOnUiThread {
//       val intent = Intent(activity, HybridHostActivity::class.java).apply {
//         putExtra("roomId", roomId)
//         putExtra("messagesJson", safeJson)
//       }
//       activity.startActivity(intent)
//     }
//   }

  /** 뒤로가기(권장: 디스패처 우선) */
//   @ReactMethod
//   fun goBack() {
//     val activity = currentActivity ?: return
//     (activity as? ComponentActivity)
//       ?.onBackPressedDispatcher
//       ?.onBackPressed()
//       ?: run {
//         @Suppress("DEPRECATION")
//         activity.onBackPressed()
//       }
//   }

  /** 화면 종료(즉시 finish) — 필요 시 유지 */
//   @ReactMethod
//   fun back() {
//     currentActivity?.finish()
//   }

  /** 입력 전송 (RN 하단 입력바 → 네이티브 Compose) */
//   @ReactMethod
//   fun onSend(text: String) {
//     val act = currentActivity
//     if (act is HybridHostActivity) {
//       act.deliverFromRN(text) // 중앙 Compose로 전달
//     }
//   }
}
