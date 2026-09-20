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
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.ImageBitmap
import hu.rayworks.vizit.data.card.CardColorway
import hu.rayworks.vizit.data.card.CardLayout
import hu.rayworks.vizit.data.card.CardPresentation
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
    photo: ImageBitmap? = null,
    presentation: CardPresentation = CardPresentation(),
    socialLabels: List<String> = emptyList(),
    qrCode: ImageBitmap? = null,
) {
    val colors = Vizit.colors
    val subtitle = listOf(jobTitle, company).filter { it.isNotBlank() }.joinToString(" · ")
    val accent = presentation.colorway.accentColor
    val surface = remember(presentation.colorway) {
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to presentation.colorway.startColor,
                0.55f to presentation.colorway.midColor,
                1.0f to presentation.colorway.endColor,
            ),
        )
    }
    val socials = if (presentation.showsSocial) socialLabels else emptyList()

    val describedAs = buildString {
        append(fullName.ifBlank { "Névjegy" })
        if (subtitle.isNotBlank()) append(", ").append(subtitle)
        if (phone.isNotBlank()) append(", telefon ").append(phone)
        if (email.isNotBlank()) append(", e-mail ").append(email)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (presentation.layout == CardLayout.PORTRAIT) 343f / 216f else 343f / 180f)
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
                .background(accent),
        )

        val contentModifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(start = 24.dp, end = 20.dp, top = 22.dp, bottom = 20.dp)

        if (presentation.layout == CardLayout.PORTRAIT) {
            Column(
                modifier = contentModifier,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (presentation.showsPhoto) {
                        CardAvatar(initials = initials, photo = photo)
                        Spacer(Modifier.width(14.dp))
                    }
                    CardIdentity(fullName = fullName, subtitle = subtitle, modifier = Modifier.weight(1f))
                    if (qrCode != null) {
                        Spacer(Modifier.width(12.dp))
                        CardQr(qrCode)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    CardDetails(phone = phone, email = email, socials = socials)
                    Text(
                        text = "VIZIT",
                        style = Vizit.type.overline.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                }
            }
        } else {
            Row(modifier = contentModifier) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (presentation.showsPhoto) {
                            CardAvatar(initials = initials, photo = photo)
                            Spacer(Modifier.width(12.dp))
                        }
                        CardIdentity(fullName = fullName, subtitle = subtitle)
                    }
                    CardDetails(phone = phone, email = email, socials = socials)
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (qrCode != null) CardQr(qrCode) else Spacer(Modifier.height(1.dp))
                    Text(
                        text = "VIZIT",
                        style = Vizit.type.overline.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardAvatar(initials: String, photo: ImageBitmap?) {
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
            Text(
                text = initials.ifBlank { "V" },
                style = Vizit.type.title,
                color = Color.White.copy(alpha = 0.92f),
            )
        }
    }
}

@Composable
private fun CardIdentity(fullName: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = fullName.ifBlank { "Állítsd össze a névjegyed" },
            style = Vizit.type.h3,
            color = Color.White,
            maxLines = 2,
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = Vizit.type.bodySmall,
                color = Color.White.copy(alpha = 0.68f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CardDetails(phone: String, email: String, socials: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (phone.isNotBlank()) {
            Text(
                text = phone,
                style = Vizit.type.bodySmall,
                color = Color.White.copy(alpha = 0.82f),
            )
        }
        if (email.isNotBlank()) {
            Text(
                text = email,
                style = Vizit.type.bodySmall,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
            )
        }
        if (socials.isNotEmpty()) {
            Text(
                text = socials.joinToString(" · "),
                style = Vizit.type.caption,
                color = Color.White.copy(alpha = 0.62f),
                maxLines = 1,
            )
        }
    }
}

/** A miniature of the contact QR, on the white plate a scanner needs. */
@Composable
private fun CardQr(code: ImageBitmap) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(Vizit.radius.xs))
            .background(Color.White)
            .padding(4.dp),
    ) {
        Image(
            bitmap = code,
            contentDescription = null,
            filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The colourway's ARGB values as Compose colours. The presentation model itself
 * stays free of Compose so it can be unit-tested on a plain JVM.
 */
val CardColorway.startColor: Color get() = Color(gradientStart)
val CardColorway.midColor: Color get() = Color(gradientMid)
val CardColorway.endColor: Color get() = Color(gradientEnd)
val CardColorway.accentColor: Color get() = Color(accent)
