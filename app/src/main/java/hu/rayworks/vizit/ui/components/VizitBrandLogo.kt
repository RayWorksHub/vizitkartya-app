package hu.rayworks.vizit.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.R

@Composable
fun VizitBrandMark(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
    ) {
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
fun VizitBrandLockup(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.vizit_logo_full),
            contentDescription = "VIZIT – Egy érintés. Egy kapcsolat.",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(18.dp)
                .heightIn(min = 120.dp, max = 180.dp),
        )
    }
}
