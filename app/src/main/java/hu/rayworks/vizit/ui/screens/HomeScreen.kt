package hu.rayworks.vizit.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.NfcStatus
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.ui.components.ProfileAvatar
import hu.rayworks.vizit.ui.components.VizitBrandMark
import hu.rayworks.vizit.ui.theme.VizitBlue
import hu.rayworks.vizit.ui.theme.VizitNavy
import hu.rayworks.vizit.ui.theme.VizitTeal
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    profile: ContactProfile,
    nfcStatus: NfcStatus,
    onStartNfcShare: () -> String?,
    onEditProfile: () -> Unit,
    onShareAsText: (Context) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(VizitNavy, VizitBlue)),
                        )
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                ) {
                    VizitBrandMark()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Egy érintés.\nEgy új kapcsolat.",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProfileAvatar(
                            photoBase64 = profile.photoBase64,
                            initials = profile.initials,
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(
                                text = profile.resolvedDisplayName.ifBlank { "Állítsd össze a névjegyed" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            val subtitle = listOf(profile.jobTitle, profile.company)
                                .filter(String::isNotBlank)
                                .joinToString(" · ")
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        OutlinedButton(onClick = onEditProfile) {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            onStartNfcShare()?.let { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VizitTeal),
                    ) {
                        Icon(Icons.Outlined.Nfc, contentDescription = null)
                        Text(
                            text = "NFC kontaktátadás",
                            modifier = Modifier.padding(start = 10.dp),
                            color = VizitNavy,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    OutlinedButton(
                        onClick = { onShareAsText(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Text("Megosztás másképp", modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (nfcStatus.isReady) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (nfcStatus.isReady) {
                                Icons.Outlined.CheckCircle
                            } else {
                                Icons.Outlined.ErrorOutline
                            },
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(
                                text = if (nfcStatus.isReady) "NFC használatra kész" else "NFC beállítás szükséges",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = nfcStatus.description(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

private fun NfcStatus.description(): String = when {
    !isAvailable -> "A készülékben nincs elérhető NFC-egység."
    !hasHostCardEmulation -> "A készülék nem támogatja a telefonos kártyaemulációt."
    !isEnabled -> "Kapcsold be az NFC-t a rendszerbeállításokban."
    else -> "A telefon készen áll a közvetlen kontaktátadásra."
}
