package hu.rayworks.vizit.v10.ui.wizard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.v10.ui.components.IconCircleButton
import hu.rayworks.vizit.v10.ui.components.cssLinearGradient
import hu.rayworks.vizit.v10.ui.components.drawEllipticalGradient
import hu.rayworks.vizit.v10.ui.components.noRippleClickable
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.V

/**
 * „Kezdjük meg a profilod létrehozását!” – teljes képernyős sötétkék kezdőlap NFC-hullámokkal.
 * A két típusgomb (Vállalkozói / Magánszemély) alatt színes csíkok mutatják, hány blokk jön.
 */
@Composable
fun WizardIntro(c: WizardController) {
    val wiz = c.wiz
    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .drawBehind {
                val w = size.width
                val h = size.height
                // linear-gradient(165deg, h1 0%, h1 45%, h2 130%) – a 100%-os pont színe kiszámolva
                drawRect(cssLinearGradient(165f, size, 0f to V.h1, 0.45f to V.h1, 1f to lerp(V.h1, V.h2, 0.55f / 0.85f)))
                // radial-gradient(90% 60% at -10% 105%, glow2, transparent 65%)
                drawEllipticalGradient(-0.1f * w, 1.05f * h, 0.9f * w, 0.6f * h, 0f to V.glow2, 0.65f to V.glow2.copy(alpha = 0f))
                // radial-gradient(120% 70% at 105% -5%, glow1, transparent 60%)
                drawEllipticalGradient(1.05f * w, -0.05f * h, 1.2f * w, 0.7f * h, 0f to V.glow1, 0.6f to V.glow1.copy(alpha = 0f))
            }
            .noRippleClickable { }
    ) {
        Image(
            VIcons.introWaves,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 70.dp)
                .size(260.dp, 283.6.dp),
        )
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircleButton(VIcons.close, "Bezárás", background = V.white10, tint = Color.White) { c.close(false) }
                Text(
                    "VIZIT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.em,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(44.dp))
            }
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
                ) {
                    Text(
                        "Kezdjük meg a profilod létrehozását!",
                        fontSize = 34.sp,
                        lineHeight = 37.4.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.01).em,
                        color = Color.White,
                    )
                    Text(
                        "Milyen profil lesz?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TypePick(wiz, ProfileType.Business, VIcons.briefcase, "Vállalkozói") { pick(c, ProfileType.Business) }
                        TypePick(wiz, ProfileType.Private, VIcons.person, "Magánszemély") { pick(c, ProfileType.Private) }
                    }
                }
            }
        }
    }
}

private fun pick(c: WizardController, t: ProfileType) {
    c.wiz.chooseType(t)
    c.wiz.begin()
}

/** .w-pick: áttetsző kártya; kiválasztva fehér, kék ikonnal. Nyomásra 98,5%-ra húzódik össze. */
@Composable
private fun TypePick(wiz: WizardState, t: ProfileType, icon: ImageVector, label: String, onClick: () -> Unit) {
    val checked = wiz.type == t
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(120), label = "pickScale")
    val bg by animateColorAsState(if (checked) V.surface else V.white10, tween(150), label = "pickBg")
    val shape = RoundedCornerShape(20.dp)
    val bars = BLOCKS.filter { t != ProfileType.Private || it.id != "company" }.map { it.color.c }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bg)
            .border(1.dp, if (checked) V.surface else V.white22, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (checked) V.h2 else V.white15),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = if (checked) V.ink else Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                bars.forEach { col ->
                    Box(
                        Modifier
                            .size(22.dp, 5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(col)
                    )
                }
            }
        }
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (checked) V.h2 else V.white15),
            contentAlignment = Alignment.Center,
        ) {
            Icon(VIcons.chev, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
