package hu.rayworks.vizit.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.ui.design.Vizit

/**
 * VIZIT text field. Label above the box (never a floating label that collides
 * with the value), 52dp box, hairline border that thickens to 2dp on focus and
 * turns state/error with a helper message when invalid.
 */
@Composable
fun VizitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = Vizit.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hasError = error != null

    val borderColor = when {
        !enabled -> colors.border
        hasError -> colors.error
        focused -> colors.borderFocus
        else -> colors.border
    }
    val borderWidth = if ((focused || hasError) && enabled) 2.dp else 1.dp

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Vizit.space.xxs + 2.dp)) {
        Text(
            text = label,
            style = Vizit.type.label,
            color = if (enabled) colors.textSecondary else colors.textDisabled,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .background(
                    if (enabled) colors.surface else colors.controlDisabled,
                    RoundedCornerShape(Vizit.radius.md),
                )
                .border(borderWidth, borderColor, RoundedCornerShape(Vizit.radius.md))
                .padding(horizontal = Vizit.space.md, vertical = Vizit.space.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.textMuted,
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = Vizit.type.body,
                        color = colors.textMuted,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = Vizit.type.body.copy(
                        color = if (enabled) colors.textPrimary else colors.textDisabled,
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    interactionSource = interaction,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            trailing?.invoke()
        }
        val message = error ?: helper
        if (message != null) {
            Text(
                text = message,
                style = Vizit.type.bodySmall,
                color = if (hasError) colors.error else colors.textMuted,
            )
        }
    }
}

/** Password field with a reveal toggle that is itself an accessible control. */
@Composable
fun VizitPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var revealed by remember { mutableStateOf(false) }
    VizitTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        error = error,
        helper = helper,
        enabled = enabled,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            VizitIconButton(
                icon = if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (revealed) "Jelszó elrejtése" else "Jelszó megjelenítése",
                onClick = { revealed = !revealed },
                enabled = enabled,
                modifier = Modifier.size(40.dp),
            )
        },
    )
}
