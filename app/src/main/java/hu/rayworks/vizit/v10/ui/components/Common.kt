package hu.rayworks.vizit.v10.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.v10.ui.theme.SheetEasing
import hu.rayworks.vizit.v10.ui.theme.V
import kotlinx.coroutines.delay

/** Kattintás hullámeffekt nélkül (háttérre, átfedésekre). */
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

/** Szaggatott keret (pl. „Új profil” kártya, még nem kitöltött blokk). A tartalom fölé rajzol, mint a CSS border. */
fun Modifier.dashedBorder(width: Dp, color: Color, radius: Dp, dash: Dp = 6.dp, gap: Dp = 5.dp): Modifier =
    drawWithContent {
        drawContent()
        val w = width.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(w / 2, w / 2),
            size = Size(size.width - w, size.height - w),
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))),
        )
    }

/**
 * Kitöltött gomb (.primary). soft = „Kihagyom” stílus, danger = piros,
 * enabled = false → szürke (varázsló), dimmed = true → 40%-os átlátszóság (.is-disabled a főképernyőn).
 */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    secondaryText: String? = null,
    enabled: Boolean = true,
    dimmed: Boolean = false,
    soft: Boolean = false,
    danger: Boolean = false,
    height: Dp = 52.dp,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        when {
            !enabled -> V.switchOff
            danger -> V.redFill
            soft -> V.blueSoft
            else -> V.blueFill
        }, tween(200), label = "btnBg"
    )
    val fg by animateColorAsState(
        when {
            !enabled -> V.sub
            soft -> V.blue
            else -> V.onBlueFill
        }, tween(200), label = "btnFg"
    )
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .alpha(if (dimmed) 0.4f else 1f)
            .clip(RoundedCornerShape(height / 2))
            .background(bg)
            .clickable(enabled = enabled && !dimmed, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        if (secondaryText != null) {
            Spacer(Modifier.width(6.dp))
            Text("· $secondaryText", color = fg.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
        if (trailingIcon != null) {
            Spacer(Modifier.width(6.dp))
            Icon(trailingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        }
    }
}

/** Körvonalas gomb (.secondary, androidos változat). */
@Composable
fun OutlineButton(text: String, icon: ImageVector?, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, V.outline, RoundedCornerShape(26.dp))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = V.blue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = V.blue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/** Tónusos kis gomb (.tonal-sm). */
@Composable
fun TonalSmallButton(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = V.onContainer,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(V.container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/** Körvonalas kis gomb (.w-sbtn). */
@Composable
fun SmallOutlineButton(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = V.ink,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, V.outline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

/** Kerek ikongomb. */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    background: Color = Color.Transparent,
    tint: Color = V.ink,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Profil címke színes pöttyel (.plabel). */
@Composable
fun ProfileLabel(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(V.chip)
            .padding(start = 9.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(7.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = V.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** „MINTA” / „ÚJ” jelölés (.mintag). */
@Composable
fun ProfileTag(text: String, fresh: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        text.uppercase(),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.06.em,
        color = if (fresh) V.blue else V.sub,
        modifier = Modifier
            .clip(shape)
            .then(if (fresh) Modifier.background(V.blueSoft) else Modifier.border(1.dp, V.line, shape))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/** Kis kapszula (állapotjelzők, „Szabad”, „Most”, „Kész”). */
@Composable
fun Pill(text: String, background: Color, color: Color, icon: ImageVector? = null, fontSize: Float = 12f) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = color, fontSize = fontSize.sp, fontWeight = FontWeight.Medium)
    }
}

/** Fehér, lekerekített lista-csoport (.group). */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(V.surface),
        content = content,
    )
}

/** Csoportcím (.group-label, androidos: kék, 14sp). */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 6.dp)) {
        Text(text, color = V.blue, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = V.blue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** Lista sor (.row). A prototípus androidos nézetében nincs jobbra nyíl. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    sub: String? = null,
    value: String? = null,
    valueColor: Color = V.sub,
    valueBold: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    divider: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.4f)) {
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(V.line))
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = 56.dp)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            } else if (icon != null) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (danger) V.redSoft else V.blueSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = if (danger) V.red else V.blue, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (danger) V.red else V.ink)
                if (sub != null) Text(sub, fontSize = 13.sp, color = V.sub, lineHeight = 17.sp)
            }
            if (value != null) {
                Spacer(Modifier.width(8.dp))
                Text(value, fontSize = 14.sp, color = valueColor, fontWeight = if (valueBold) FontWeight.SemiBold else FontWeight.Normal)
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
    }
}

/** Material 3 kapcsoló a prototípus színeivel. */
@Composable
fun VSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = V.onBlueFill,
            checkedTrackColor = V.blueFill,
            checkedBorderColor = V.blueFill,
            uncheckedThumbColor = V.outline,
            uncheckedTrackColor = V.nav,
            uncheckedBorderColor = V.outline,
        ),
    )
}
