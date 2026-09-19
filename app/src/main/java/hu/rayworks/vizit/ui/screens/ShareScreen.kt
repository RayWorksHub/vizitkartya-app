package hu.rayworks.vizit.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import hu.rayworks.vizit.NfcStatus
import hu.rayworks.vizit.R
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.qr.QrCodeGenerator
import hu.rayworks.vizit.qr.QrMode
import hu.rayworks.vizit.qr.QrPayloadFactory
import hu.rayworks.vizit.qr.QrShareHelper
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitBrandHeader
import hu.rayworks.vizit.ui.design.components.VizitBrandHeaderStyle
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitButtonStyle
import hu.rayworks.vizit.ui.design.components.VizitEmptyState
import hu.rayworks.vizit.ui.design.components.VizitSectionHeader
import hu.rayworks.vizit.ui.design.components.VizitSegmentedControl
import hu.rayworks.vizit.ui.design.components.VizitStatusPill
import hu.rayworks.vizit.ui.design.components.VizitTone
import kotlinx.coroutines.launch

/**
 * Every hand-off route in one place, with the two that matter — NFC and QR —
 * front and centre rather than buried.
 *
 * The QR surface is an "always-light island": it stays pure #FFFFFF with a
 * generous quiet zone in both themes, and the caption inside it uses fixed dark
 * ink, because a tinted or low-contrast code is a code that does not scan.
 */
@Composable
fun ShareScreen(
    profile: ContactProfile,
    synchronized: Boolean,
    nfcStatus: NfcStatus,
    onStartNfcShare: () -> String?,
    modifier: Modifier = Modifier,
) {
    val colors = Vizit.colors
    val context = LocalContext.current
    val qrLogo = remember(context) {
        BitmapFactory.decodeResource(context.resources, R.drawable.vizit_logo_mark)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var qrMode by rememberSaveable { mutableStateOf(QrMode.CONTACT) }
    var fullScreen by rememberSaveable { mutableStateOf(false) }

    val publicProfileUrl = remember(profile, synchronized) {
        QrPayloadFactory.profileUrl(profile, synchronized)
    }
    val contactPayload = remember(profile, publicProfileUrl) {
        QrPayloadFactory.contact(profile, publicProfileUrl)
    }
    val photoContactPayload = remember(profile, publicProfileUrl) {
        profile.photoBase64.takeIf(String::isNotBlank)
            ?.let { QrPayloadFactory.photoContact(profile, publicProfileUrl).getOrNull() }
    }
    val qrPayload = remember(qrMode, publicProfileUrl, contactPayload, photoContactPayload) {
        when (qrMode) {
            QrMode.PROFILE -> publicProfileUrl
            QrMode.CONTACT -> photoContactPayload ?: contactPayload.getOrNull()
        }
    }
    val qrBitmap = remember(qrPayload, qrLogo) {
        qrPayload?.takeIf(String::isNotBlank)?.let {
            runCatching { QrCodeGenerator.create(it, logo = qrLogo) }.getOrNull()
        }
    }

    fun toast(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.canvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = Vizit.space.md),
            verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
        ) {
            Spacer(Modifier.height(Vizit.space.xs))
            VizitBrandHeader(style = VizitBrandHeaderStyle.Compact)

            Text("Megosztás", style = Vizit.type.h1, color = colors.textPrimary)
            Text(
                text = "Érintsd össze a telefonokat, vagy mutasd a QR-kódot. A fogadó félnek nem kell VIZIT.",
                style = Vizit.type.body,
                color = colors.textSecondary,
            )

            VizitSegmentedControl(
                options = listOf(
                    if (photoContactPayload != null) "Fényképes QR" else "Kontakt QR",
                    "Profil QR",
                ),
                selectedIndex = if (qrMode == QrMode.CONTACT) 0 else 1,
                onSelect = { qrMode = if (it == 0) QrMode.CONTACT else QrMode.PROFILE },
            )

            if (qrBitmap != null) {
                QrIsland(
                    bitmap = qrBitmap,
                    caption = when {
                        qrMode == QrMode.PROFILE ->
                            "A nyilvános névjegyoldalt nyitja meg. A mentéshez nem kell VIZIT alkalmazás."
                        photoContactPayload != null ->
                            "Beolvasás után közvetlenül megnyílik a profilképes névjegy mentése."
                        else ->
                            "vCard kontakt QR – profilkép nélkül, hogy gyorsan beolvasható maradjon."
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                ) {
                    VizitButton(
                        text = "Teljes képernyő",
                        onClick = { fullScreen = true },
                        icon = Icons.Outlined.Fullscreen,
                        style = VizitButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    VizitButton(
                        text = "Megosztás",
                        onClick = { QrShareHelper.share(context, qrBitmap) },
                        icon = Icons.Outlined.Share,
                        style = VizitButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                ) {
                    VizitButton(
                        text = "Mentés",
                        onClick = {
                            toast(
                                if (QrShareHelper.saveToPictures(context, qrBitmap)) {
                                    "A QR-kód mentve a Képek/VIZIT mappába."
                                } else {
                                    "A QR-kód mentése nem sikerült."
                                },
                            )
                        },
                        icon = Icons.Outlined.SaveAlt,
                        style = VizitButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    if (qrMode == QrMode.PROFILE && publicProfileUrl != null) {
                        VizitButton(
                            text = "Link másolása",
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("VIZIT profil", publicProfileUrl),
                                )
                                toast("A profil-link a vágólapra került.")
                            },
                            icon = Icons.Outlined.ContentCopy,
                            style = VizitButtonStyle.Secondary,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                VizitEmptyState(
                    icon = Icons.Outlined.QrCode2,
                    title = if (qrMode == QrMode.PROFILE) "Nincs még publikus profil" else "A Kontakt QR nem állítható elő",
                    message = when (qrMode) {
                        QrMode.PROFILE ->
                            "A Profil QR-hez engedélyezd a publikus profilt, adj meg profilazonosítót, és várd meg a sikeres szinkront."
                        QrMode.CONTACT ->
                            contactPayload.exceptionOrNull()?.message
                                ?: "Előbb töltsd ki a névjegyed alapadatait."
                    },
                    actionLabel = if (qrMode == QrMode.PROFILE) "Kontakt QR megnyitása" else null,
                    onAction = if (qrMode == QrMode.PROFILE) {
                        { qrMode = QrMode.CONTACT }
                    } else {
                        null
                    },
                )
            }

            VizitSectionHeader("Közvetlen átadás")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, RoundedCornerShape(Vizit.radius.lg))
                    .border(1.dp, colors.border, RoundedCornerShape(Vizit.radius.lg))
                    .padding(Vizit.space.md),
                verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
            ) {
                VizitStatusPill(
                    text = if (nfcStatus.isReady) "NFC használatra kész" else "NFC nem érhető el",
                    tone = if (nfcStatus.isReady) VizitTone.Success else VizitTone.Warning,
                )
                Text(
                    text = if (nfcStatus.isReady) {
                        "Android fogadó telefonon a kontaktimport VIZIT telepítése nélkül indul. iPhone-nál a QR a biztos út."
                    } else {
                        "Kapcsold be az NFC-t a rendszerbeállításokban. A QR-megosztás ettől függetlenül működik."
                    },
                    style = Vizit.type.bodySmall,
                    color = colors.textSecondary,
                )
                VizitButton(
                    text = "NFC kontaktátadás",
                    onClick = { onStartNfcShare()?.let(::toast) },
                    icon = Icons.Outlined.Nfc,
                    enabled = nfcStatus.isReady,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(Vizit.space.xl))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(Vizit.space.md),
        )
    }

    if (fullScreen && qrBitmap != null) {
        FullScreenQrDialog(bitmap = qrBitmap, onDismiss = { fullScreen = false })
    }
}

/** Fixed ink used inside the always-white QR island, in both themes. */
private val QrCaptionInk = Color(0xFF4A5568)

@Composable
private fun QrIsland(bitmap: Bitmap, caption: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(Vizit.radius.xl))
            .border(1.dp, Vizit.colors.border, RoundedCornerShape(Vizit.radius.xl))
            .padding(Vizit.space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "VIZIT QR-kód",
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(1f)
                .background(Color.White)
                .padding(Vizit.space.md),
        )
        Text(
            text = caption,
            style = Vizit.type.bodySmall,
            color = QrCaptionInk,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FullScreenQrDialog(bitmap: Bitmap, onDismiss: () -> Unit) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        val original = activity?.window?.attributes?.screenBrightness
        if (activity != null) {
            val attributes = activity.window.attributes
            attributes.screenBrightness = 1f
            activity.window.attributes = attributes
        }
        onDispose {
            if (activity != null && original != null) {
                val attributes = activity.window.attributes
                attributes.screenBrightness = original
                activity.window.attributes = attributes
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(Vizit.space.xl)
                .semantics { contentDescription = "VIZIT QR-kód teljes képernyőn" },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(Vizit.space.lg),
            )
            Spacer(Modifier.height(Vizit.space.xl))
            VizitButton(text = "Bezárás", onClick = onDismiss, style = VizitButtonStyle.Secondary)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
