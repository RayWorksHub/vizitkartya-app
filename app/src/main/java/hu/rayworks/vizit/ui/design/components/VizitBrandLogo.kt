package hu.rayworks.vizit.ui.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.R
import hu.rayworks.vizit.ui.design.Vizit

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
        Image(
            painter = painterResource(R.drawable.vizit_logo_full),
            contentDescription = "VIZIT – Egy érintés. Egy kapcsolat.",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(Vizit.space.lg)
                .heightIn(min = 104.dp, max = 152.dp),
        )
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
