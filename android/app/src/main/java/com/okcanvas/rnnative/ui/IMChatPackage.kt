package com.okcanvas.rnnative.ui

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.NativeModule
import com.facebook.react.uimanager.ViewManager

class IMChatPackage : ReactPackage {
  override fun createNativeModules(rc: ReactApplicationContext): List<NativeModule> =
    emptyList()

  override fun createViewManagers(rc: ReactApplicationContext): List<ViewManager<*, *>> =
    listOf(
      IMChatRoomViewManager(rc),   // 채팅방 (Compose)
      IMChatListViewManager(rc)    // 채팅 리스트 (Compose)  ← 필요하면 함께 등록
    )
}
