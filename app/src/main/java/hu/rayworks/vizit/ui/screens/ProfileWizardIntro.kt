package hu.rayworks.vizit.ui.screens

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import hu.rayworks.vizit.ui.design.Vizit
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val WizardNavy = Color(0xFF0C2C63)
private val WizardBlue = Color(0xFF2A5BD7)
private val WizardCyanGlow = Color(0x614FB3D9)
private val WizardBlueGlow = Color(0x992A5BD7)
private val WizardAmber = Color(0xFFB86F0E)
private val WizardTeal = Color(0xFF0E8494)

internal data class V10WizardVisualGroup(
    val id: String,
    val title: String,
    val color: Color,
    val steps: Set<String>,
)

@Composable
internal fun v10WizardVisualGroups(type: String): List<V10WizardVisualGroup> {
    val dark = Vizit.colors.isDark
    val groups = mutableListOf(
        V10WizardVisualGroup(
            id = "personal",
            title = "Személyes adatok",
            color = if (dark) Color(0xFF7FA2FF) else Vizit.colors.primary,
            steps = setOf("identity", "photo"),
        ),
    )
    if (type != "private") {
        groups += V10WizardVisualGroup(
            id = "company",
            title = "Céges adatok",
            color = if (dark) Color(0xFFE3A04A) else WizardAmber,
            steps = setOf("logo", "bio"),
        )
    }
    groups += V10WizardVisualGroup(
        id = "online",
        title = "Online elérés",
        color = if (dark) Color(0xFF3DC1D1) else WizardTeal,
        steps = setOf("contact", "social"),
    )
    groups += V10WizardVisualGroup(
        id = "done",
        title = "Befejezés",
        color = Vizit.colors.success,
        steps = setOf("look", "done"),
    )
    return groups
}

@Composable
internal fun V10WizardTopBar(
    profileType: String,
    group: V10WizardVisualGroup,
    groupIndex: Int,
    groupCount: Int,
    canClose: Boolean,
    onClose: () -> Unit,
    onChangeType: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (canClose) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, "Bezárás", tint = Vizit.colors.textMuted, modifier = Modifier.size(21.dp))
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onChangeType)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    "$profileType · ${groupIndex + 1}/$groupCount",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Vizit.colors.textMuted,
                    maxLines = 1,
                )
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Vizit.colors.textMuted,
                    modifier = Modifier.size(15.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(group.color))
                Text(
                    group.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Vizit.colors.textPrimary,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun V10WizardProgress(groups: List<V10WizardVisualGroup>, currentIndex: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        groups.forEachIndexed { index, group ->
            val current = index == currentIndex
            Box(
                Modifier
                    .weight(1f)
                    .height(14.dp)
                    .padding(vertical = if (current) 4.dp else 6.dp)
                    .clip(RoundedCornerShape(if (current) 3.dp else 1.dp))
                    .background(if (index < currentIndex) group.color else Vizit.colors.controlTrack),
            ) {
                if (current) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.45f)
                            .fillMaxHeight()
                            .background(group.color),
                    )
                }
            }
        }
    }
}

/** Exact V10 cover supplied in the prototype, including the NFC-wave artwork. */
@Composable
internal fun V10ProfileWizardIntro(
    selectedType: String,
    canClose: Boolean,
    onClose: () -> Unit,
    onPick: (String) -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (previousStatus != null) controller?.isAppearanceLightStatusBars = previousStatus
            if (previousNavigation != null) controller?.isAppearanceLightNavigationBars = previousNavigation
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .drawBehind {
                val width = size.width
                val height = size.height
                drawRect(
                    wizardCssLinearGradient(
                        angleDegrees = 165f,
                        size = size,
                        0f to WizardNavy,
                        0.45f to WizardNavy,
                        1f to lerp(WizardNavy, WizardBlue, 0.55f / 0.85f),
                    )
                )
                drawWizardEllipticalGradient(
                    cx = -0.10f * width,
                    cy = 1.05f * height,
                    rx = 0.90f * width,
                    ry = 0.60f * height,
                    0f to WizardBlueGlow,
                    0.65f to WizardBlueGlow.copy(alpha = 0f),
                )
                drawWizardEllipticalGradient(
                    cx = 1.05f * width,
                    cy = -0.05f * height,
                    rx = 1.20f * width,
                    ry = 0.70f * height,
                    0f to WizardCyanGlow,
                    0.60f to WizardCyanGlow.copy(alpha = 0f),
                )
            },
    ) {
        WizardIntroWaves(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 70.dp)
                .size(width = 260.dp, height = 284.dp),
        )
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canClose) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Close, "Bezárás", tint = Color.White, modifier = Modifier.size(21.dp))
                    }
                } else {
                    Spacer(Modifier.size(44.dp))
                }
                Text(
                    "VIZIT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.9.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(44.dp))
            }
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
                ) {
                    Text(
                        "Kezdjük meg a profilod létrehozását!",
                        fontSize = 34.sp,
                        lineHeight = 37.4.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.34).sp,
                        color = Color.White,
                    )
                    Text(
                        "Milyen profil lesz?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        WizardTypePick(
                            checked = selectedType == "business",
                            type = "business",
                            icon = Icons.Outlined.BusinessCenter,
                            label = "Vállalkozói",
                            bars = listOf(Vizit.colors.primary, WizardAmber, WizardTeal, Vizit.colors.success),
                            onPick = onPick,
                        )
                        WizardTypePick(
                            checked = selectedType == "private",
                            type = "private",
                            icon = Icons.Outlined.Person,
                            label = "Magánszemély",
                            bars = listOf(Vizit.colors.primary, WizardTeal, Vizit.colors.success),
                            onPick = onPick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardTypePick(
    checked: Boolean,
    type: String,
    icon: ImageVector,
    label: String,
    bars: List<Color>,
    onPick: (String) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(120), label = "wizardPickScale")
    val background by animateColorAsState(
        if (checked) Vizit.colors.surface else Color.White.copy(alpha = 0.10f),
        tween(150),
        label = "wizardPickBackground",
    )
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(background)
            .border(1.dp, if (checked) Vizit.colors.surface else Color.White.copy(alpha = 0.22f), shape)
            .clickable(interactionSource = interaction, indication = null) { onPick(type) }
            .padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (checked) WizardBlue else Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(
                label,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = if (checked) Vizit.colors.textPrimary else Color.White,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                bars.forEach { color ->
                    Box(
                        Modifier
                            .size(width = 22.dp, height = 5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color),
                    )
                }
            }
        }
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (checked) WizardBlue else Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun WizardIntroWaves(modifier: Modifier) {
    Canvas(modifier) {
        val scale = size.width / 220f
        val color = Color.White.copy(alpha = 0.15f)
        drawCircle(color = color, radius = 13f * scale, center = Offset(40f * scale, 120f * scale))
        val arcs = listOf(
            floatArrayOf(78f, 62f, 178f, 82f),
            floatArrayOf(112f, 30f, 210f, 124f),
            floatArrayOf(146f, -2f, 242f, 166f),
        )
        for (arc in arcs) {
            val half = (arc[2] - arc[1]) / 2f
            val distance = sqrt(arc[3] * arc[3] - half * half)
            val centerX = arc[0] - distance
            val centerY = (arc[1] + arc[2]) / 2f
            val sweep = atan2(half, distance)
            val path = Path()
            for (index in 0..40) {
                val angle = -sweep + 2f * sweep * index / 40f
                val point = Offset(
                    (centerX + arc[3] * cos(angle)) * scale,
                    (centerY + arc[3] * sin(angle)) * scale,
                )
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(
                path = path,
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 12f * scale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
        }
    }
}

/** CSS linear-gradient geometry used by the supplied V10 prototype. */
private fun wizardCssLinearGradient(
    angleDegrees: Float,
    size: Size,
    vararg stops: Pair<Float, Color>,
): Brush {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val dx = sin(radians).toFloat()
    val dy = -cos(radians).toFloat()
    val length = abs(size.width * dx) + abs(size.height * dy)
    val center = Offset(size.width / 2f, size.height / 2f)
    val half = Offset(dx * length / 2f, dy * length / 2f)
    return Brush.linearGradient(*stops, start = center - half, end = center + half)
}

/** CSS elliptical radial-gradient geometry used by the supplied V10 prototype. */
private fun DrawScope.drawWizardEllipticalGradient(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    vararg stops: Pair<Float, Color>,
) {
    if (rx <= 0f || ry <= 0f) return
    val center = Offset(cx, cy)
    scale(scaleX = rx / ry, scaleY = 1f, pivot = center) {
        drawRect(
            brush = Brush.radialGradient(*stops, center = center, radius = ry),
            topLeft = Offset(-size.width * 4f, -size.height * 4f),
            size = Size(size.width * 9f, size.height * 9f),
        )
    }
}
