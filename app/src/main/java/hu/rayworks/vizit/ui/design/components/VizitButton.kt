package hu.rayworks.vizit.ui.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.VizitMinTouchTarget

enum class VizitButtonStyle { Primary, Secondary, Tertiary, Destructive }

/**
 * The single button primitive for the app.
 *
 * Covers the default / pressed / disabled / loading states from the Figma
 * Button component set. Pressed feedback is a restrained 0.97 scale plus a
 * colour shift — it exists to confirm the tap, not to perform.
 */
@Composable
fun VizitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: VizitButtonStyle = VizitButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentDescription: String? = null,
    /**
     * Fixed colours for "island" surfaces that do not follow the theme — the
     * always-dark NFC hand-off screen, the always-light QR panel. Leave null
     * everywhere else so the button stays themed.
     */
    containerOverride: Color? = null,
    contentOverride: Color? = null,
) {
    val colors = Vizit.colors
    val active = enabled && !loading
    val shape = RoundedCornerShape(26.dp)

    val container: Color = when {
        containerOverride != null -> containerOverride
        !active -> colors.controlDisabled
        style == VizitButtonStyle.Primary -> colors.primaryFill
        style == VizitButtonStyle.Secondary -> Color.Transparent
        style == VizitButtonStyle.Tertiary -> Color.Transparent
        else -> colors.errorFill
    }
    val content: Color = when {
        contentOverride != null -> contentOverride
        !active -> colors.textDisabled
        style == VizitButtonStyle.Primary -> colors.onPrimary
        style == VizitButtonStyle.Secondary -> colors.primary
        style == VizitButtonStyle.Tertiary -> colors.primary
        else -> colors.onPrimary
    }
    val border: BorderStroke? = when {
        containerOverride != null -> null
        style == VizitButtonStyle.Secondary && active -> BorderStroke(1.dp, colors.outline)
        else -> null
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(container, shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(
                enabled = active,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Vizit.space.lg, vertical = Vizit.space.sm),
        horizontalArrangement = Arrangement.spacedBy(Vizit.space.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = content,
                    strokeWidth = 2.dp,
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(20.dp),
                    tint = content,
                )
            }
            Text(text = text, style = Vizit.type.button, color = content)
        }
    }
}

/**
 * Icon-only action. Always 48dp of touch area even though the visual chip is
 * smaller, and always requires a contentDescription for screen readers.
 */
@Composable
fun VizitIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color? = null,
) {
    val colors = Vizit.colors
    Box(
        modifier = modifier
            .size(VizitMinTouchTarget)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = when {
                !enabled -> colors.textDisabled
                tint != null -> tint
                else -> colors.textSecondary
            },
        )
    }
}
