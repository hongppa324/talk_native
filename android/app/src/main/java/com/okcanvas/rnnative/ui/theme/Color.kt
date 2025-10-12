package com.okcanvas.rnnative.ui.theme

import androidx.compose.ui.graphics.Color

// Brand tokens
val BrandYellow = Color(0xFFFFE100)
val BrandSurfaceLight = Color(0xFFFFFFFF)
val BrandSurfaceDark  = Color(0xFF121212)
val BrandOnSurfaceLight = Color(0xFF222222)
val BrandOnSurfaceDark  = Color(0xFFEDEDED)

// Plain colors
val PlainWhite = Color(0xFFFFFFFF)   // ✅ 흰색 공용 상수

// Avatar / Accent colors
val AvatarBlue = Color(0xFF9DB8E4)   // 파스텔 블루 (기존 AvatarBubble 기본색)
val AvatarBorder = Color(0x1A000000) // 매우 옅은 테두리용 투명 블랙

// Optional neutrals
val OnSurfaceMuted   = BrandOnSurfaceLight.copy(alpha = 0.7f)
val OnSurfaceSubtle  = BrandOnSurfaceLight.copy(alpha = 0.6f)
val PlaceholderBg    = BrandOnSurfaceLight.copy(alpha = 0.06f)
val PlaceholderBorder= BrandOnSurfaceLight.copy(alpha = 0.08f)

// Semantic tokens (필요시 확장)
val ErrorRed = Color(0xFFD32F2F)
