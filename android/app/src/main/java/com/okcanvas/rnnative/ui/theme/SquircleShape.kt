package com.okcanvas.rnnative.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * 카카오톡 프로필 같은 "스퀘어클(Squircle)" 모양 Shape
 *
 * @param n     곡선 강도 (값이 작을수록 모서리가 더 둥글어짐, 4.2~4.8 정도가 카톡 느낌)
 * @param steps Path 분해 단위 (클수록 경계가 더 부드러움; 96~128 권장)
 */
class SquircleShape(
    private val n: Float = 4.5f,
    private val steps: Int = 96
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(buildSuperellipsePath(size, n, steps))
    }
}

private fun buildSuperellipsePath(size: Size, n: Float, steps: Int): Path {
    val a = size.width / 2f
    val b = size.height / 2f
    val path = Path()

    fun x(t: Float) = a * sign(cos(t)) * abs(cos(t)).pow(2f / n) + a
    fun y(t: Float) = b * sign(sin(t)) * abs(sin(t)).pow(2f / n) + b

    val t0 = 0f
    path.moveTo(x(t0), y(t0))
    val step = (2f * Math.PI.toFloat()) / steps
    var t = t0 + step
    repeat(steps) {
        path.lineTo(x(t), y(t))
        t += step
    }
    path.close()
    return path
}
