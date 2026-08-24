package hu.rayworks.vizit.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.NfcSharePhase
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.ui.components.ProfileAvatar
import hu.rayworks.vizit.ui.components.VizitBrandMark
import hu.rayworks.vizit.ui.theme.VizitBlue
import hu.rayworks.vizit.ui.theme.VizitNavy
import hu.rayworks.vizit.ui.theme.VizitTeal

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

    val transition = rememberInfiniteTransition(label = "nfcPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_250),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

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
            "A telefon nem tudta a VIZIT-et előtérbeli NFC-szolgáltatásként aktiválni. Használd a Contact QR-t."
        NfcSharePhase.IDLE -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VizitNavy, VizitBlue)))
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VizitBrandMark()
        Spacer(Modifier.weight(0.7f))

        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (phase == NfcSharePhase.WAITING) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .alpha(0.18f)
                        .background(VizitTeal, CircleShape),
                )
            }
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .background(VizitTeal, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (phase) {
                        NfcSharePhase.PAYLOAD_READ -> Icons.Outlined.CheckCircle
                        NfcSharePhase.TIMED_OUT -> Icons.Outlined.TimerOff
                        NfcSharePhase.ROUTING_FAILED -> Icons.Outlined.TimerOff
                        else -> Icons.Outlined.Nfc
                    },
                    contentDescription = null,
                    tint = VizitNavy,
                    modifier = Modifier.size(66.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProfileAvatar(
                    photoBase64 = profile.photoBase64,
                    initials = profile.initials,
                    size = 56.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.resolvedDisplayName, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOf(profile.jobTitle, profile.company)
                            .filter(String::isNotBlank)
                            .joinToString(" · "),
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    if (phase == NfcSharePhase.WAITING && profile.photoBase64.isNotBlank() && !photoIncluded) {
                        Text(
                            "A kontaktfotó a biztonságos NFC-méret miatt most nem kerül át.",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
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
            color = Color.White.copy(alpha = 0.62f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = null)
            Text(
                if (phase == NfcSharePhase.WAITING) "Küldés leállítása" else "Bezárás",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
