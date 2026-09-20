package hu.rayworks.vizit.ui.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import hu.rayworks.vizit.ui.util.rememberProfilePhoto
import hu.rayworks.vizit.R
import hu.rayworks.vizit.ui.design.Vizit

private const val SLOGAN = "Egy érintés. Egy kapcsolat."

/**
 * How much brand presence a screen gets.
 *
 * [Full] is the brand moment on primary destinations: the mark, the VIZIT name
 * and the slogan, each at a size that is actually readable. [Compact] keeps all
 * three on secondary and inner screens — smaller, but never reduced to a
 * generic app bar where the brand disappears.
 */
enum class VizitBrandHeaderStyle(
    val markHeight: Dp,
    val nameSize: Int,
    val nameTracking: Double,
) {
    Full(markHeight = 34.dp, nameSize = 26, nameTracking = 5.0),
    Compact(markHeight = 22.dp, nameSize = 16, nameTracking = 3.0),
}

/**
 * The VIZIT mark on its mandated white plate.
 *
 * The artwork is a fixed brand asset: white background, blue "V". It is never
 * tinted or inverted, so the plate is part of the mark rather than decoration
 * around it. In dark mode a hairline keeps it from floating on the canvas.
 */
@Composable
fun VizitBrandMarkPlate(
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Vizit.radius.md)
    Box(
        modifier = modifier
            .background(Color.White, shape)
            .border(
                1.dp,
                if (Vizit.colors.isDark) Color.White.copy(alpha = 0.16f) else Vizit.colors.border,
                shape,
            )
            .padding(horizontal = height * 0.34f, vertical = height * 0.26f),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.vizit_logo_mark),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(height),
        )
    }
}

/**
 * The one brand header every screen uses. Screens never hand-roll their own.
 *
 * The caller supplies the window-inset padding, so this composable stays usable
 * inside scrolling content as well as directly under the status bar.
 */
@Composable
fun VizitBrandHeader(
    modifier: Modifier = Modifier,
    style: VizitBrandHeaderStyle = VizitBrandHeaderStyle.Full,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    when (style) {
        VizitBrandHeaderStyle.Full -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
            ) {
                VizitBrandMarkPlate(height = style.markHeight)
                BrandWordmark(style)
            }
            trailing?.invoke()
        }

        VizitBrandHeaderStyle.Compact -> Row(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                        .clickable(onClick = onBack)
                        .semantics { contentDescription = "Vissza" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = Vizit.colors.textSecondary,
                    )
                }
            }
            VizitBrandMarkPlate(height = style.markHeight)
            Box(modifier = Modifier.weight(1f)) { BrandWordmark(style) }
            trailing?.invoke()
        }
    }
}

@Composable
private fun BrandWordmark(style: VizitBrandHeaderStyle) {
    Column(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "VIZIT – $SLOGAN"
            heading()
        },
    ) {
        Text(
            text = "VIZIT",
            style = Vizit.type.h1.copy(
                fontSize = style.nameSize.sp,
                letterSpacing = style.nameTracking.sp,
            ),
            color = Vizit.colors.textPrimary,
        )
        Text(
            text = SLOGAN,
            style = if (style == VizitBrandHeaderStyle.Full) Vizit.type.body else Vizit.type.caption,
            color = Vizit.colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The signed-in person, shown as its own row under the brand rather than
 * competing with it. The real photo is used whenever there is one.
 */
@Composable
fun VizitUserBadge(
    displayName: String,
    initials: String,
    photoBase64: String,
    modifier: Modifier = Modifier,
    greeting: String = "Üdv újra,",
    onClick: (() -> Unit)? = null,
) {
    val colors = Vizit.colors
    val name = displayName.ifBlank { "Állítsd be a névjegyed" }
    val photo = rememberProfilePhoto(photoBase64)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(colors.primarySubtle, CircleShape)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                )
            } else {
                Text(
                    text = initials.ifBlank { "V" },
                    style = Vizit.type.label,
                    color = colors.primary,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(greeting, style = Vizit.type.bodySmall, color = colors.textMuted)
            Text(
                text = name,
                style = Vizit.type.h3,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (onClick != null) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textMuted,
            )
        }
    }
}

/**
 * The screen's own title, rendered the way the platform renders one: large,
 * left-aligned, with an optional action or identity chip on the trailing edge.
 *
 * Primary screens carry the brand here instead of a separate header block, so
 * the brand costs nothing but the title line the screen already needed.
 */
@Composable
fun VizitLargeTitle(
    title: String,
    modifier: Modifier = Modifier,
    letterSpacing: Double = 0.0,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
    ) {
        Text(
            text = title,
            style = Vizit.type.display.copy(letterSpacing = letterSpacing.sp),
            color = Vizit.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        trailing?.invoke()
    }
}

/**
 * The signed-in person as a tappable chip beside the title. It replaces the
 * full greeting row on the home screen: same destination, a fraction of the
 * height.
 */
@Composable
fun VizitIdentityChip(
    initials: String,
    photoBase64: String,
    displayName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Vizit.colors
    val photo = rememberProfilePhoto(photoBase64)
    val label = if (displayName.isBlank()) {
        "Névjegy beállítása"
    } else {
        "Megnyitás: $displayName névjegye"
    }
    Box(
        modifier = modifier
            .size(44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(colors.primarySubtle, CircleShape)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(34.dp).clip(CircleShape),
                )
            } else {
                Text(
                    text = initials.ifBlank { "V" },
                    style = Vizit.type.caption,
                    color = colors.primary,
                )
            }
        }
    }
}
