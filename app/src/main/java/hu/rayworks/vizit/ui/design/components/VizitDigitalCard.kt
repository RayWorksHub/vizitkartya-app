package hu.rayworks.vizit.ui.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.ui.design.Vizit

/**
 * The VIZIT digital business card — the product's signature visual.
 *
 * Design intent (Figma: 05 · Card & Profile):
 *  - one object with real presence, not a white rectangle with text on it;
 *  - a navy ink surface with a low-contrast directional gradient that reads as
 *    material, never as decoration;
 *  - a single 4dp cyan edge accent as the brand mark;
 *  - identical in light and dark mode, because the card is the brand.
 */
@Composable
fun VizitDigitalCard(
    fullName: String,
    initials: String,
    jobTitle: String,
    company: String,
    phone: String,
    email: String,
    modifier: Modifier = Modifier,
    photo: androidx.compose.ui.graphics.ImageBitmap? = null,
) {
    val colors = Vizit.colors
    val subtitle = listOf(jobTitle, company).filter { it.isNotBlank() }.joinToString(" · ")
    val surface = remember {
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFF0C2C63),
                0.55f to Color(0xFF071F4C),
                1.0f to Color(0xFF05163A),
            ),
        )
    }

    val describedAs = buildString {
        append(fullName.ifBlank { "Névjegy" })
        if (subtitle.isNotBlank()) append(", ").append(subtitle)
        if (phone.isNotBlank()) append(", telefon ").append(phone)
        if (email.isNotBlank()) append(", e-mail ").append(email)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(343f / 216f)
            .shadow(
                elevation = Vizit.elevation.card,
                shape = RoundedCornerShape(Vizit.radius.xl),
                ambientColor = colors.ink,
                spotColor = colors.ink,
            )
            .clip(RoundedCornerShape(Vizit.radius.xl))
            .background(surface)
            .semantics { contentDescription = describedAs },
    ) {
        // brand edge accent
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(Color(0xFF0FBEE6)),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (photo != null) {
                        Image(
                            bitmap = photo,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)),
                        )
                    } else {
                        androidx.compose.material3.Text(
                            text = initials.ifBlank { "V" },
                            style = Vizit.type.title,
                            color = Color.White.copy(alpha = 0.92f),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    androidx.compose.material3.Text(
                        text = fullName.ifBlank { "Állítsd össze a névjegyed" },
                        style = Vizit.type.h3,
                        color = Color.White,
                        maxLines = 2,
                    )
                    if (subtitle.isNotBlank()) {
                        androidx.compose.material3.Text(
                            text = subtitle,
                            style = Vizit.type.bodySmall,
                            color = Color.White.copy(alpha = 0.68f),
                            maxLines = 1,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (phone.isNotBlank()) {
                        androidx.compose.material3.Text(
                            text = phone,
                            style = Vizit.type.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                    }
                    if (email.isNotBlank()) {
                        androidx.compose.material3.Text(
                            text = email,
                            style = Vizit.type.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 1,
                        )
                    }
                }
                androidx.compose.material3.Text(
                    text = "VIZIT",
                    style = Vizit.type.overline.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0FBEE6),
                )
            }
        }
    }
}
