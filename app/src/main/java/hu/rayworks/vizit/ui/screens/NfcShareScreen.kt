package hu.rayworks.vizit.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.NfcSharePhase
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.vizitReduceMotion
import hu.rayworks.vizit.ui.util.rememberProfilePhoto

private val NfcInkTop = Color(0xFF0C2C63)
private val NfcInkBottom = Color(0xFF05163A)
private val NfcAccent = Color(0xFF0FBEE6)

/**
 * Full-screen NFC hand-off. It deliberately takes over the whole screen: the
 * user is holding two phones together and needs one unmistakable signal, not a
 * dialog competing with the rest of the UI.
 */
@Composable
fun NfcShareScreen(
    profile: ContactProfile,
    phase: NfcSharePhase,
    photoIncluded: Boolean,
    onRoutingFailed: () -> Unit,
    onStop: () -> Unit,
) {
    BackHandler(onBack = onStop)
    NfcPreferredServiceEffect(
        enabled = phase == NfcSharePhase.WAITING,
        onRoutingFailed = onRoutingFailed,
    )
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(phase) {
        if (phase == NfcSharePhase.PAYLOAD_READ) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val reduceMotion = vizitReduceMotion()
    val pulseScale = if (reduceMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "nfcPulse")
        val animated by transition.animateFloat(
            initialValue = 0.86f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(tween(1_250), RepeatMode.Reverse),
            label = "pulseScale",
        )
        animated
    }

    val photo = rememberProfilePhoto(profile.photoBase64)

    val title = when (phase) {
        NfcSharePhase.WAITING -> "NFC-küldés aktív"
        NfcSharePhase.PAYLOAD_READ -> "NFC adat kiolvasva"
        NfcSharePhase.TIMED_OUT -> "Az NFC-megosztás lejárt"
        NfcSharePhase.ROUTING_FAILED -> "Az NFC-küldés nem indítható"
        NfcSharePhase.IDLE -> "NFC"
    }
    val description = when (phase) {
        NfcSharePhase.WAITING ->
            "Érintsd a másik feloldott Android telefon hátlapját ehhez a készülékhez. iPhone vagy nem kompatibilis eszköz esetén használd a QR/profil-link fallbacket."
        NfcSharePhase.PAYLOAD_READ ->
            "A fogadó készülék kiolvasta az NFC-adatcsomagot. Ez nem jelenti automatikusan azt, hogy a névjegyet már el is mentették."
        NfcSharePhase.TIMED_OUT ->
            "A névjegy már nem olvasható NFC-n. Indíts új átadást, vagy válaszd a QR-megosztást."
        NfcSharePhase.ROUTING_FAILED ->
            "A telefon nem tudta a VIZIT-et előtérbeli NFC-szolgáltatásként aktiválni. Használd a Kontakt QR-t."
        NfcSharePhase.IDLE -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NfcInkTop, NfcInkBottom)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Vizit.space.xl, vertical = Vizit.space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "VIZIT",
            style = Vizit.type.overline,
            color = NfcAccent,
        )
        Spacer(Modifier.weight(0.7f))

        Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
            if (phase == NfcSharePhase.WAITING) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .alpha(0.18f)
                        .background(NfcAccent, CircleShape),
                )
            }
            Box(
                modifier = Modifier.size(126.dp).background(NfcAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (phase) {
                        NfcSharePhase.PAYLOAD_READ -> Icons.Outlined.CheckCircle
                        NfcSharePhase.TIMED_OUT, NfcSharePhase.ROUTING_FAILED -> Icons.Outlined.TimerOff
                        else -> Icons.Outlined.Nfc
                    },
                    contentDescription = null,
                    tint = NfcInkBottom,
                    modifier = Modifier.size(64.dp),
                )
            }
        }

        Spacer(Modifier.height(Vizit.space.xxl))
        Column(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "$title. $description"
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
        ) {
            Text(text = title, style = Vizit.type.h1, color = Color.White, textAlign = TextAlign.Center)
            Text(
                text = description,
                style = Vizit.type.body,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(Vizit.space.xxl))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(Vizit.radius.lg))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(Vizit.radius.lg))
                .padding(Vizit.space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm + 2.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null) {
                    Image(
                        bitmap = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    Text(
                        text = profile.initials.ifBlank { "V" },
                        style = Vizit.type.title,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = profile.resolvedDisplayName,
                    style = Vizit.type.bodyStrong,
                    color = Color.White,
                )
                val subtitle = listOf(profile.jobTitle, profile.company)
                    .filter(String::isNotBlank)
                    .joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(text = subtitle, style = Vizit.type.bodySmall, color = Color.White.copy(alpha = 0.7f))
                }
                if (phase == NfcSharePhase.WAITING && profile.photoBase64.isNotBlank() && !photoIncluded) {
                    Text(
                        text = "A profilkép mérete miatt NFC-n nem kerül át.",
                        style = Vizit.type.caption,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = if (phase == NfcSharePhase.WAITING) {
                "A megosztás 60 másodperc után automatikusan leáll."
            } else {
                "A névjegy NFC-adatcsomagja már nem olvasható."
            },
            style = Vizit.type.bodySmall,
            color = Color.White.copy(alpha = 0.62f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Vizit.space.sm))
        VizitButton(
            text = if (phase == NfcSharePhase.WAITING) "Küldés leállítása" else "Bezárás",
            onClick = onStop,
            icon = Icons.Outlined.Close,
            containerOverride = NfcAccent,
            contentOverride = NfcInkBottom,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
