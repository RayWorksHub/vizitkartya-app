package hu.rayworks.vizit.ui.screens

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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.ui.components.ProfileAvatar
import hu.rayworks.vizit.ui.components.VizitBrandMark
import hu.rayworks.vizit.ui.theme.VizitBlue
import hu.rayworks.vizit.ui.theme.VizitNavy
import hu.rayworks.vizit.ui.theme.VizitTeal

@Composable
fun NfcShareScreen(
    profile: ContactProfile,
    onStop: () -> Unit,
) {
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
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(pulseScale)
                    .alpha(0.18f)
                    .background(VizitTeal, CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .background(VizitTeal, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Nfc,
                    contentDescription = null,
                    tint = VizitNavy,
                    modifier = Modifier.size(66.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = "NFC-küldés aktív",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Érintsd a másik feloldott Android telefon hátlapját ehhez a készülékhez.",
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
            ) {
                ProfileAvatar(
                    photoBase64 = profile.photoBase64,
                    initials = profile.initials,
                    size = 56.dp,
                )
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(profile.fullName, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOf(profile.jobTitle, profile.company)
                            .filter(String::isNotBlank)
                            .joinToString(" · "),
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = "A küldés leállításakor az NFC-adat azonnal törlődik.",
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
            Text("Küldés leállítása", modifier = Modifier.padding(start = 8.dp))
        }
    }
}
