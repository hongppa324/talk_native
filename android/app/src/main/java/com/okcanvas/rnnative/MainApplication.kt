package com.okcanvas.rnnative

import com.okcanvas.rnnative.NativeNavigatorPackage
import android.app.Application
import android.content.res.Configuration
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.ReactHost
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader
import expo.modules.ApplicationLifecycleDispatcher
import expo.modules.ReactNativeHostWrapper

class MainApplication : Application(), ReactApplication {

  override val reactNativeHost: ReactNativeHost = ReactNativeHostWrapper(
    this,
    object : DefaultReactNativeHost(this) {
      override fun getPackages(): List<ReactPackage> {
        val packages = PackageList(this).packages
        // 수동 등록 패키지
        // packages.add(NativeNavigatorPackage())
        packages.add(com.okcanvas.rnnative.ui.IMChatPackage())
        return packages
      }

      // Expo 프리빌드가 아니라면 보통 "index"
      override fun getJSMainModuleName(): String = "index"

      override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG
      override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
      override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
    }
  )

  override val reactHost: ReactHost
    get() = ReactNativeHostWrapper.createReactHost(applicationContext, reactNativeHost)

  override fun onCreate() {
    super.onCreate()

    // SoLoader/Hermes 초기화
    SoLoader.init(this, OpenSourceMergedSoMapping)

    if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
      // New Architecture(ReactHost) 엔트리 로드
      load()
    }

    // Expo 생명주기 브리지
    ApplicationLifecycleDispatcher.onApplicationCreate(this)

    // 🔵 핵심: RN 컨텍스트/호스트 사전 초기화
    try {
      if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
        // ReactHost 사용: 앱 시작 시 곧바로 호스트 시작 → 컨텍스트 준비
        reactHost.start()
      } else {
        // Old Architecture: 브릿지 컨텍스트 백그라운드 생성
        reactNativeHost.reactInstanceManager.createReactContextInBackground()
      }
    } catch (t: Throwable) {
      // 초기화 실패해도 앱이 죽지 않도록 방어
      t.printStackTrace()
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    ApplicationLifecycleDispatcher.onConfigurationChanged(this, newConfig)
  }
}
