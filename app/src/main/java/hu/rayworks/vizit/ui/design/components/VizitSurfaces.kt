package hu.rayworks.vizit.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import hu.rayworks.vizit.ui.design.Vizit

/**
 * Plain panel. Deliberately restrained: hairline border, no shadow by default.
 * The app is not built out of identical floating cards — elevation is reserved
 * for things that genuinely sit above the page.
 */
@Composable
fun VizitPanel(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(Vizit.space.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Vizit.colors.surface, RoundedCornerShape(Vizit.radius.lg))
            .border(1.dp, Vizit.colors.border, RoundedCornerShape(Vizit.radius.lg))
            .padding(contentPadding),
        content = content,
    )
}

/** Section label above a grouped list — the iOS-style inset group header. */
@Composable
fun VizitSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    tone: Color? = null,
) {
    Text(
        text = text.uppercase(),
        style = Vizit.type.overline,
        color = tone ?: Vizit.colors.textMuted,
        modifier = modifier.padding(horizontal = Vizit.space.xxs),
    )
}

/**
 * Grouped container for settings rows: one rounded box, hairline dividers
 * between rows, no per-row card. `danger = true` outlines the group in the
 * error colour so destructive actions are visually separated.
 */
@Composable
fun VizitGroup(
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Vizit.radius.lg))
            .background(Vizit.colors.surface)
            .border(
                1.dp,
                if (danger) Vizit.colors.error else Vizit.colors.border,
                RoundedCornerShape(Vizit.radius.lg),
            ),
        content = content,
    )
}

@Composable
fun VizitDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Vizit.colors.divider),
    )
}

/** The square icon chip used in rows and tiles — white surface in light mode. */
@Composable
fun VizitIconChip(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    background: Color? = null,
    bordered: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 36.dp,
) {
    val colors = Vizit.colors
    Box(
        modifier = modifier
            .size(size)
            .background(background ?: colors.iconSurface, RoundedCornerShape(Vizit.radius.sm + 2.dp))
            .then(
                if (bordered && background == null) {
                    Modifier.border(1.dp, colors.border, RoundedCornerShape(Vizit.radius.sm + 2.dp))
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(size * 0.55f),
            tint = tint ?: colors.textSecondary,
        )
    }
}

/**
 * A row inside a [VizitGroup]. Exactly one of [onClick] / [checked] should be
 * supplied. The whole row is the touch target and carries the semantics, so a
 * screen reader announces the label and the control together.
 */
@Composable
fun VizitRow(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    value: String? = null,
    supporting: String? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = Vizit.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val clickable = onClick != null && enabled

    val fg = when {
        !enabled -> colors.textDisabled
        destructive -> colors.error
        else -> colors.textPrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (pressed && clickable) colors.sunken else Color.Transparent)
            .then(
                if (clickable) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick!!,
                    )
                } else {
                    Modifier
                },
            )
            .defaultMinSize(minHeight = 60.dp)
            .padding(horizontal = Vizit.space.md, vertical = Vizit.space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm + 2.dp),
    ) {
        if (icon != null) {
            VizitIconChip(
                icon = icon,
                tint = if (destructive) colors.error else colors.textSecondary,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = label, style = Vizit.type.body, color = fg)
            if (supporting != null) {
                Text(text = supporting, style = Vizit.type.bodySmall, color = colors.textMuted)
            }
        }
        if (value != null) {
            Text(text = value, style = Vizit.type.bodySmall, color = colors.textMuted)
        }
        if (checked != null && onCheckedChange != null) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = colors.controlTrack,
                    uncheckedBorderColor = colors.controlTrack,
                ),
            )
        } else if (clickable && showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colors.textMuted,
            )
        }
    }
}
