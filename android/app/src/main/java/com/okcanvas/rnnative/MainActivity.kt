package com.okcanvas.rnnative

import android.os.Build
import android.os.Bundle
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactInstanceManager.ReactInstanceEventListener
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import expo.modules.ReactActivityDelegateWrapper

class MainActivity : ReactActivity() {

  // 컨텍스트 준비 대기 중인지 플래그
  private var waitingFocusReplay = false
  private var ctxListener: ReactInstanceEventListener? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.AppTheme)
    super.onCreate(null)
  }

  override fun getMainComponentName(): String = "main"

  override fun createReactActivityDelegate(): ReactActivityDelegate {
    return ReactActivityDelegateWrapper(
      this,
      BuildConfig.IS_NEW_ARCHITECTURE_ENABLED,
      object : DefaultReactActivityDelegate(
        this,
        mainComponentName,
        fabricEnabled
      ) {}
    )
  }

  override fun invokeDefaultOnBackPressed() {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
      if (!moveTaskToBack(false)) {
        super.invokeDefaultOnBackPressed()
      }
      return
    }
    super.invokeDefaultOnBackPressed()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    val rim = reactInstanceManagerSafe()
    val contextReady = rim?.currentReactContext != null

    if (hasFocus && !contextReady) {
      // 1) 아직 준비 전이면 리스너로 보증
      if (!waitingFocusReplay && rim != null) {
        waitingFocusReplay = true
        ctxListener = ReactInstanceEventListener { _ ->
          // 컨텍스트가 준비되면 포커스 true를 한 번 재전달
          try {
            super.onWindowFocusChanged(true)
          } finally {
            // 정리
            rim.removeReactInstanceEventListener(ctxListener)
            waitingFocusReplay = false
            ctxListener = null
          }
        }
        rim.addReactInstanceEventListener(ctxListener)
        // 2) 혹시 컨텍스트 생성이 아직 안 시작됐다면 트리거
        if (!rim.hasStartedCreatingInitialContext()) {
          rim.createReactContextInBackground()
        }
      }
      // 준비 전에는 기본 구현 호출을 보류 (SoftException 방지)
      return
    }

    // 컨텍스트가 준비되었거나 포커스 해제 이벤트는 그대로 전달
    super.onWindowFocusChanged(hasFocus)
  }

  override fun onDestroy() {
    // 누수 방지
    reactInstanceManagerSafe()?.removeReactInstanceEventListener(ctxListener)
    ctxListener = null
    waitingFocusReplay = false
    super.onDestroy()
  }

  private fun reactInstanceManagerSafe(): ReactInstanceManager? {
    val app = application
    return if (app is ReactApplication) app.reactNativeHost.reactInstanceManager else null
  }
}
