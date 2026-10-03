package hu.rayworks.vizit.v10.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * CSS `linear-gradient(<angle>deg, ...)` pontos megfelelője: 0° = felfelé, 90° = jobbra,
 * a gradiens vonal hossza |w·sin a| + |h·cos a|, ahogy a böngésző számolja.
 */
fun cssLinearGradient(angleDeg: Float, size: Size, vararg stops: Pair<Float, Color>): Brush {
    val rad = Math.toRadians(angleDeg.toDouble())
    val dx = sin(rad).toFloat()
    val dy = -cos(rad).toFloat()
    val len = abs(size.width * dx) + abs(size.height * dy)
    val c = Offset(size.width / 2f, size.height / 2f)
    val half = Offset(dx * len / 2f, dy * len / 2f)
    return Brush.linearGradient(*stops, start = c - half, end = c + half)
}

/**
 * CSS `radial-gradient(<rx> <ry> at <cx> <cy>, ...)` ellipszis alakú sugaras színátmenet.
 * A hívó elem legyen clipToBounds(), mert a téglalap a képernyőn túlra is rajzol.
 */
fun DrawScope.drawEllipticalGradient(cx: Float, cy: Float, rx: Float, ry: Float, vararg stops: Pair<Float, Color>) {
    if (rx <= 0f || ry <= 0f) return
    val c = Offset(cx, cy)
    scale(scaleX = rx / ry, scaleY = 1f, pivot = c) {
        drawRect(
            brush = Brush.radialGradient(*stops, center = c, radius = ry),
            topLeft = Offset(-size.width * 4f, -size.height * 4f),
            size = Size(size.width * 9f, size.height * 9f),
        )
    }
}
