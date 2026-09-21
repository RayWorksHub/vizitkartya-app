package hu.rayworks.vizit.ui.design.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.ui.design.Vizit

enum class VizitTone { Success, Warning, Error, Info }

/**
 * Inline status indicator. Always pairs an icon with the text so state is never
 * communicated by colour alone (WCAG 1.4.1).
 */
@Composable
fun VizitStatusPill(
    text: String,
    tone: VizitTone,
    modifier: Modifier = Modifier,
) {
    val colors = Vizit.colors
    val (fg, bg, icon) = when (tone) {
        VizitTone.Success -> Triple(colors.success, colors.successSubtle, Icons.Outlined.CheckCircle)
        VizitTone.Warning -> Triple(colors.warning, colors.warningSubtle, Icons.Outlined.WarningAmber)
        VizitTone.Error -> Triple(colors.error, colors.errorSubtle, Icons.Outlined.ErrorOutline)
        VizitTone.Info -> Triple(colors.info, colors.infoSubtle, Icons.Outlined.Info)
    }
    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(Vizit.radius.full))
            .padding(horizontal = Vizit.space.sm, vertical = Vizit.space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Vizit.space.xs),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = fg)
        Text(text = text, style = Vizit.type.label, color = fg)
    }
}

/** Full-width banner for persistent conditions such as offline mode. */
@Composable
fun VizitBanner(
    text: String,
    tone: VizitTone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = Vizit.colors
    val (fg, bg, icon) = when (tone) {
        VizitTone.Success -> Triple(colors.success, colors.successSubtle, Icons.Outlined.CheckCircle)
        VizitTone.Warning -> Triple(colors.warning, colors.warningSubtle, Icons.Outlined.WarningAmber)
        VizitTone.Error -> Triple(colors.error, colors.errorSubtle, Icons.Outlined.ErrorOutline)
        VizitTone.Info -> Triple(colors.info, colors.infoSubtle, Icons.Outlined.Info)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(Vizit.radius.md))
            .padding(horizontal = Vizit.space.md, vertical = Vizit.space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = fg)
        Text(text = text, style = Vizit.type.bodySmall, color = fg, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = Vizit.type.label,
                color = fg,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = Vizit.space.xs, vertical = Vizit.space.xxs),
            )
        }
    }
}

/** Empty state: icon, what is missing, and the one action that fixes it. */
@Composable
fun VizitEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = Vizit.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Vizit.space.xl, vertical = Vizit.space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(colors.primarySubtle, RoundedCornerShape(Vizit.radius.lg)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = colors.primary)
        }
        Text(text = title, style = Vizit.type.h3, color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(
            text = message,
            style = Vizit.type.body,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            VizitButton(
                text = actionLabel,
                onClick = onAction,
                style = VizitButtonStyle.Secondary,
                modifier = Modifier.padding(top = Vizit.space.xs),
            )
        }
    }
}

/** Error state — same shape as empty, but offers a retry. */
@Composable
fun VizitErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    retryLabel: String = "Újrapróbálás",
    onRetry: (() -> Unit)? = null,
) {
    val colors = Vizit.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Vizit.space.xl, vertical = Vizit.space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(colors.errorSubtle, RoundedCornerShape(Vizit.radius.lg)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = colors.error,
            )
        }
        Text(text = title, style = Vizit.type.h3, color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(text = message, style = Vizit.type.body, color = colors.textSecondary, textAlign = TextAlign.Center)
        if (onRetry != null) {
            VizitButton(
                text = retryLabel,
                onClick = onRetry,
                style = VizitButtonStyle.Secondary,
                modifier = Modifier.padding(top = Vizit.space.xs),
            )
        }
    }
}

/** Centered spinner with an optional caption, for whole-screen waits. */
@Composable
fun VizitLoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Vizit.space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator(color = Vizit.colors.primary, strokeWidth = 3.dp)
        if (message != null) {
            Text(
                text = message,
                style = Vizit.type.bodySmall,
                color = Vizit.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Skeleton placeholder. Honours reduced-motion: when the user has turned
 * animations off the block renders flat instead of pulsing.
 */
@Composable
fun VizitSkeleton(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 16.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp,
) {
    val colors = Vizit.colors
    val reduceMotion = vizitReduceMotion()
    val alpha = if (reduceMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "vizit-skeleton")
        val animated by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "vizit-skeleton-alpha",
        )
        animated
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colors.skeletonBase.copy(alpha = alpha), RoundedCornerShape(cornerRadius)),
    )
}

/** True when the OS asks for reduced motion (Android 13+ exposes this reliably). */
@Composable
fun vizitReduceMotion(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember(context) {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

/** Two-to-three option switch. Selection is conveyed by fill, weight and semantics. */
@Composable
fun VizitSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Vizit.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.controlTrack, RoundedCornerShape(Vizit.radius.md))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(
                        if (selected) colors.surface else Color.Transparent,
                        RoundedCornerShape(Vizit.radius.sm + 1.dp),
                    )
                    .clickable(role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = Vizit.type.label,
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                )
            }
        }
    }
}
