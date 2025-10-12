package com.okcanvas.rnnative.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandYellow,
    onPrimary = Color(0xFF1A1A1A),
    surface = BrandSurfaceLight,
    onSurface = BrandOnSurfaceLight,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = BrandYellow,
    onPrimary = Color(0xFF1A1A1A),
    surface = BrandSurfaceDark,
    onSurface = BrandOnSurfaceDark,
    error = ErrorRed
)

@Composable
fun KakaoStyleTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = Typography,
        shapes      = Shapes,
        content     = content
    )
}
