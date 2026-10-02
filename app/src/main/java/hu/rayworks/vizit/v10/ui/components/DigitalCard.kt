package hu.rayworks.vizit.v10.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.v10.data.CardLayout
import hu.rayworks.vizit.v10.data.Pic
import hu.rayworks.vizit.v10.data.Profile
import hu.rayworks.vizit.v10.data.SOCIALS

/** Beépített vagy választott kép kitöltve. */
@Composable
fun PicImage(pic: Pic, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop) {
    when (pic) {
        is Pic.Res -> Image(painterResource(pic.id), contentDescription = null, contentScale = scale, modifier = modifier)
        is Pic.Bmp -> Image(pic.bitmap, contentDescription = null, contentScale = scale, modifier = modifier)
    }
}

/** Kerek profilkép vagy monogram (a sorokban, lapokon). */
@Composable
fun ProfileAvatar(p: Profile, size: androidx.compose.ui.unit.Dp, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        val photo = p.photo
        if (photo != null) PicImage(photo, Modifier.fillMaxSize())
        else Text(p.initials, color = fg, fontSize = (size.value * 0.34f).sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * A digitális névjegykártya (éles VizitDigitalCard): háromszínű átlós színátmenet a választott
 * színvilágból, bal oldalon 4 dp-es kiemelő csík, profilkép logóval, név, beosztás · cég,
 * elérhetőségek, közösségi profilok, sarokban Kontakt QR, „VIZIT” jelzés.
 * Álló: 343:216, Fekvő: 343:180. Csak a látható (Adatláthatóság) mezőket mutatja.
 */
@Composable
fun DigitalCard(p: Profile, qr: String?, modifier: Modifier = Modifier) {
    val pr = p.preset
    val shape = RoundedCornerShape(20.dp)
    val ratio = if (p.layout == CardLayout.Portrait) 343f / 216f else 343f / 180f
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .shadow(10.dp, shape)
            .clip(shape)
            .drawBehind {
                drawRect(cssLinearGradient(135f, size, 0f to pr.c1, 0.5f to pr.c2, 1f to pr.c3))
                drawRect(pr.acc, size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height))
            }
            .padding(start = 22.dp, end = 18.dp, top = 18.dp, bottom = 16.dp)
    ) {
        val name = p.name.ifBlank { "Állítsd össze a névjegyed" }
        val subtitle = if (p.shows("company")) listOf(p.title, p.company).filter { it.isNotBlank() }.joinToString(" · ") else ""
        val details = listOfNotNull(p.phone.takeIf { p.shows("phone") }, p.email.takeIf { p.shows("email") })
        val socialLine = if (p.showSocial && p.shows("social")) {
            SOCIALS.filter { !p.socials[it.id].isNullOrBlank() }.joinToString(" · ") { it.label }
        } else ""
        val qrText = qr?.takeIf { p.showQr }

        val avatar: @Composable () -> Unit = {
            if (p.showPhoto) {
                Box {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        val photo = p.photo
                        if (photo != null) PicImage(photo, Modifier.fillMaxSize())
                        else Text(p.initials, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                    val logo = p.logo
                    if (logo != null) {
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 3.dp, y = 3.dp)
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.White)
                                .border(1.5.dp, Color.White, RoundedCornerShape(5.dp))
                        ) { PicImage(logo, Modifier.fillMaxSize(), ContentScale.Fit) }
                    }
                }
            }
        }
        val identity: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, color = Color.White.copy(alpha = 0.68f), fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        val detailBlock: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                details.forEach { Text(it, color = Color.White.copy(alpha = 0.82f), fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                if (socialLine.isNotEmpty()) {
                    Text(socialLine, color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        val qrPlate: @Composable () -> Unit = {
            if (qrText != null) {
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(4.dp)
                ) { QrCode(qrText, Modifier.fillMaxSize(), withLogo = false) }
            }
        }
        val mark: @Composable () -> Unit = {
            Text("VIZIT", color = pr.acc, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.16.em)
        }

        if (p.layout == CardLayout.Portrait) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    avatar()
                    Box(Modifier.weight(1f).padding(top = if (p.showPhoto) 6.dp else 0.dp)) { identity() }
                    qrPlate()
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.weight(1f)) { detailBlock() }
                    mark()
                }
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        avatar()
                        Box(Modifier.weight(1f)) { identity() }
                    }
                    detailBlock()
                }
                Column(Modifier.fillMaxHeight().width(60.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                    Box(Modifier.size(60.dp)) { qrPlate() }
                    mark()
                }
            }
        }
    }
}
