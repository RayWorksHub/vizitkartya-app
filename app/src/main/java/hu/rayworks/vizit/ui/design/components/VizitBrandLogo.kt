package hu.rayworks.vizit.ui.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.R
import hu.rayworks.vizit.ui.design.Vizit

private const val VIZIT_SLOGAN = "Egy érintés. Egy kapcsolat."

/**
 * The official VIZIT artwork always sits on a white plate, in both themes —
 * the logo is a fixed brand asset and must not be tinted or inverted. In dark
 * mode the plate gets a hairline so it does not float on the canvas.
 */
@Composable
fun VizitBrandMark(modifier: Modifier = Modifier) {
    BrandPlate(modifier = modifier, radius = Vizit.radius.md) {
        Image(
            painter = painterResource(R.drawable.vizit_logo_mark),
            contentDescription = "VIZIT",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .sizeIn(minWidth = 58.dp, minHeight = 38.dp),
        )
    }
}

@Composable
fun VizitBrandLockup(modifier: Modifier = Modifier) {
    BrandPlate(modifier = modifier, radius = Vizit.radius.xl) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Vizit.space.lg, vertical = Vizit.space.md)
                .clearAndSetSemantics {
                    contentDescription = "VIZIT – $VIZIT_SLOGAN"
                    heading()
                },
            horizontalArrangement = Arrangement.spacedBy(Vizit.space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.vizit_logo_mark),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(64.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Vizit.space.xxs),
            ) {
                Text(
                    text = "VIZIT",
                    style = Vizit.type.h1.copy(
                        fontSize = 32.sp,
                        lineHeight = 36.sp,
                        letterSpacing = 5.sp,
                    ),
                    color = Vizit.colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    text = VIZIT_SLOGAN,
                    style = Vizit.type.label.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 0.5.sp,
                    ),
                    color = Vizit.colors.ink.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BrandPlate(
    modifier: Modifier,
    radius: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .background(Color.White, shape)
            .then(
                if (Vizit.colors.isDark) {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.16f), shape)
                } else {
                    Modifier.border(1.dp, Vizit.colors.border, shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
