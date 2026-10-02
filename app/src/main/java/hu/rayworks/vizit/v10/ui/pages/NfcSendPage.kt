package hu.rayworks.vizit.v10.ui.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.NfcSharePhase
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.ui.components.ProfileAvatar
import hu.rayworks.vizit.v10.ui.components.noRippleClickable
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.V
import kotlinx.coroutines.delay

/**
 * Teljes képernyős NFC-küldés (éles NfcShareScreen): sötétkék színátmenet, lüktető ciánkék kör,
 * 60 mp-es visszaszámlálás. Production módban a fázist a valódi HCE-események vezérlik.
 */
@Composable
fun NfcSendPage(app: AppState) {
    val p = app.focus
    val haptic = LocalHapticFeedback.current
    var demoPhase by remember { mutableStateOf(NfcSharePhase.WAITING) }
    val phase = if (app.productionMode) app.nfcSharePhase else demoPhase
    var left by remember { mutableIntStateOf(60) }
    LaunchedEffect(phase) {
        if (phase == NfcSharePhase.PAYLOAD_READ) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (phase == NfcSharePhase.WAITING || phase == NfcSharePhase.IDLE) {
            left = 60
            while (left > 0) {
                delay(1000)
                left--
            }
            if (!app.productionMode) demoPhase = NfcSharePhase.TIMED_OUT
        }
    }
    val (title, desc) = when (phase) {
        NfcSharePhase.IDLE,
        NfcSharePhase.WAITING -> "NFC-küldés aktív" to "Érintsd a másik telefon hátlapjához."
        NfcSharePhase.PAYLOAD_READ -> "NFC adat kiolvasva" to "A fogadó telefon kiolvasta a névjegyet."
        NfcSharePhase.TIMED_OUT -> "Az NFC-megosztás lejárt" to "Indíts új átadást, vagy használd a QR-kódot."
        NfcSharePhase.ROUTING_FAILED -> "Az NFC-küldés nem indítható" to "Használd a Kontakt QR-t."
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(V.h1, V.h3)))
            .noRippleClickable { }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VIZIT", color = V.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.em, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.weight(1f))

            Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                if (phase == NfcSharePhase.WAITING || phase == NfcSharePhase.IDLE) {
                    val t = rememberInfiniteTransition(label = "halo")
                    val s by t.animateFloat(0.86f, 1.18f, infiniteRepeatable(tween(1250), RepeatMode.Reverse), label = "haloScale")
                    Box(
                        Modifier
                            .size(150.dp)
                            .graphicsLayer {
                                scaleX = s
                                scaleY = s
                            }
                            .clip(CircleShape)
                            .background(V.accent.copy(alpha = 0.18f))
                    )
                }
                Box(
                    Modifier
                        .size(126.dp)
                        .clip(CircleShape)
                        .background(V.accent)
                        .pointerInput(phase) {
                            detectTapGestures(
                                onTap = {
                                    if (!app.productionMode && phase == NfcSharePhase.WAITING) {
                                        demoPhase = NfcSharePhase.PAYLOAD_READ
                                    }
                                },
                                onLongPress = {
                                    if (!app.productionMode && phase == NfcSharePhase.WAITING) {
                                        demoPhase = NfcSharePhase.ROUTING_FAILED
                                    }
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(phase, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "nfcIcon") { ph ->
                        Icon(
                            when (ph) {
                                NfcSharePhase.IDLE,
                                NfcSharePhase.WAITING -> VIcons.nfc
                                NfcSharePhase.PAYLOAD_READ -> VIcons.checkCircle
                                else -> VIcons.timer
                            },
                            contentDescription = null,
                            tint = V.h1,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 31.sp)
            Spacer(Modifier.height(8.dp))
            Text(desc, color = Color.White.copy(alpha = 0.78f), fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(V.white10)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileAvatar(p, 56.dp, Color.White.copy(alpha = 0.16f), Color.White)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(p.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = listOf(p.title, p.company).filter { it.isNotBlank() }.joinToString(" · ")
                    if (sub.isNotEmpty()) Text(sub, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (p.photo != null && phase == NfcSharePhase.WAITING) {
                        Text("A profilkép NFC-n nem kerül át.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.5.sp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            if (phase == NfcSharePhase.WAITING || phase == NfcSharePhase.IDLE) {
                Text("Leáll: %d:%02d".format(left / 60, left % 60), color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(V.accent)
                    .noRippleClickable { app.pop() },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(VIcons.close, contentDescription = null, tint = V.h1, modifier = Modifier.size(20.dp))
                    Text(if (phase == NfcSharePhase.WAITING || phase == NfcSharePhase.IDLE) "Küldés leállítása" else "Bezárás", color = V.h1, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
