package hu.rayworks.vizit.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import hu.rayworks.vizit.NfcStatus
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.qr.QrCodeGenerator
import hu.rayworks.vizit.qr.QrMode
import hu.rayworks.vizit.qr.QrPayloadFactory
import hu.rayworks.vizit.qr.QrShareHelper

@Composable
fun ShareScreen(
    profile: ContactProfile,
    nfcStatus: NfcStatus,
    onStartNfcShare: () -> String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var qrMode by rememberSaveable { mutableStateOf(QrMode.CONTACT) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var showFullScreenQr by rememberSaveable { mutableStateOf(false) }

    val publicProfileUrl = remember(profile) { QrPayloadFactory.profileUrl(profile) }
    val contactPayload = remember(profile, publicProfileUrl) {
        QrPayloadFactory.contact(profile, publicProfileUrl)
    }
    val qrPayload = remember(qrMode, publicProfileUrl, contactPayload) {
        when (qrMode) {
            QrMode.PROFILE -> publicProfileUrl
            QrMode.CONTACT -> contactPayload.getOrNull()
        }
    }
    val qrBitmap = remember(qrPayload) {
        qrPayload?.takeIf(String::isNotBlank)?.let { runCatching { QrCodeGenerator.create(it) }.getOrNull() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Átadás",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Add át a névjegyed NFC-vel Androidra, vagy használj QR-kódot bármely kompatibilis telefonon.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.Nfc, contentDescription = null)
                    Text("NFC", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    if (nfcStatus.isReady) {
                        "Android fogadó telefonon VIZIT telepítése nélkül próbálja megnyitni a kontaktimportot. iPhone-nál a QR/HTTPS fallback az elsődleges."
                    } else {
                        "Az NFC jelenleg nem áll készen ezen a készüléken. A QR-megosztás ettől függetlenül használható."
                    },
                )
                Button(
                    onClick = { message = onStartNfcShare() },
                    enabled = nfcStatus.isReady,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("NFC kontaktátadás")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("QR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = qrMode == QrMode.CONTACT,
                        onClick = { qrMode = QrMode.CONTACT },
                        label = { Text("Kontakt QR") },
                    )
                    FilterChip(
                        selected = qrMode == QrMode.PROFILE,
                        onClick = { qrMode = QrMode.PROFILE },
                        label = { Text("VIZIT profil QR") },
                    )
                }

                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(18.dp))
                            .padding(12.dp),
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "VIZIT QR-kód",
                            modifier = Modifier.size(260.dp),
                        )
                    }
                    Text(
                        if (qrMode == QrMode.PROFILE) {
                            "HTTPS profil QR – Androidon App Link, iPhone-on Safari fallback."
                        } else {
                            "vCard kontakt QR – profilkép nélkül, hogy gyorsan beolvasható maradjon."
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    OutlinedButton(
                        onClick = { showFullScreenQr = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Fullscreen, contentDescription = null)
                        Text("Teljes képernyő", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { QrShareHelper.share(context, qrBitmap) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null)
                            Text("Megosztás", modifier = Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(
                            onClick = {
                                message = if (QrShareHelper.saveToPictures(context, qrBitmap)) {
                                    "A QR-kód mentve a Képek/VIZIT mappába."
                                } else {
                                    "A QR-kód mentése nem sikerült."
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.SaveAlt, contentDescription = null)
                            Text("Mentés", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    if (qrMode == QrMode.PROFILE && publicProfileUrl != null) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("VIZIT profil", publicProfileUrl))
                                message = "A profil-link a vágólapra került."
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Text("Profil-link másolása", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                } else {
                    Text(
                        text = when (qrMode) {
                            QrMode.PROFILE ->
                                "A VIZIT profil QR-hez előbb publikus profilt és érvényes profilazonosítót kell létrehozni. A Kontakt QR addig is használható offline."

                            QrMode.CONTACT -> contactPayload.exceptionOrNull()?.message
                                ?: "A Kontakt QR most nem állítható elő."
                        },
                        textAlign = TextAlign.Center,
                    )
                    if (qrMode == QrMode.PROFILE) {
                        OutlinedButton(onClick = { qrMode = QrMode.CONTACT }) {
                            Text("Kontakt QR megnyitása")
                        }
                    }
                }
            }
        }

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showFullScreenQr && qrBitmap != null) {
        FullScreenQrDialog(
            bitmap = qrBitmap,
            onDismiss = { showFullScreenQr = false },
        )
    }
}

@Composable
private fun FullScreenQrDialog(bitmap: Bitmap, onDismiss: () -> Unit) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        val originalBrightness = activity?.window?.attributes?.screenBrightness
        if (activity != null) {
            val attributes = activity.window.attributes
            attributes.screenBrightness = 1f
            activity.window.attributes = attributes
        }
        onDispose {
            if (activity != null && originalBrightness != null) {
                val attributes = activity.window.attributes
                attributes.screenBrightness = originalBrightness
                activity.window.attributes = attributes
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "VIZIT QR-kód teljes képernyőn",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDismiss) {
                Text("Bezárás")
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
