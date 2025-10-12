package com.okcanvas.rnnative.model

/**
 * 메시지 UI에서 클릭된 위치(타깃)를 구분하기 위한 enum.
 * Avatar: 프로필 이미지
 * Content: 메시지 본문
 * (필요하면 Name, UnreadBadge, Media 등 확장 가능)
 */
enum class MsgTapTarget {
    Avatar,
    Content
    // Name, UnreadBadge, Media 등 확장 가능
}
