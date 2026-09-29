package hu.rayworks.vizit.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import hu.rayworks.vizit.NfcStatus
import hu.rayworks.vizit.R
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.card.visibleThrough
import hu.rayworks.vizit.qr.QrCodeGenerator
import hu.rayworks.vizit.qr.QrPayloadFactory
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitButtonStyle
import hu.rayworks.vizit.ui.design.components.VizitDivider
import hu.rayworks.vizit.ui.design.components.VizitGroup
import hu.rayworks.vizit.ui.design.components.VizitPanel
import hu.rayworks.vizit.ui.design.components.VizitRow
import hu.rayworks.vizit.ui.design.components.VizitStatusPill
import hu.rayworks.vizit.ui.design.components.VizitTone

/**
 * VIZIT 9 native sharing surface.
 *
 * This is intentionally not a WebView: the layout is built from Compose
 * primitives and the existing native QR/NFC/profile stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V9ShareHomeScreen(
    profile: ContactProfile,
    presentation: CardPresentation,
    synchronized: Boolean,
    nfcStatus: NfcStatus,
    onStartNfcShare: () -> String?,
    onOpenProfile: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenBusinessHub: () -> Unit,
    onOpenSettings: () -> Unit,
    singleScreen: Boolean,
    analyticsEnabled: Boolean,
    businessPortalEnabled: Boolean,
    scannerEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = Vizit.colors
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    val shared = remember(profile, presentation) { profile.visibleThrough(presentation) }
    val publicProfileUrl = remember(shared, synchronized) {
        QrPayloadFactory.profileUrl(shared, synchronized)
    }
    val logo = remember(context) {
        BitmapFactory.decodeResource(context.resources, R.drawable.vizit_logo_mark)
    }
    val qr = remember(publicProfileUrl, logo) {
        publicProfileUrl?.takeIf(String::isNotBlank)?.let { url ->
            runCatching { QrCodeGenerator.create(url, logo = logo) }.getOrNull()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        Spacer(Modifier.height(Vizit.space.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "VIZIT",
                style = Vizit.type.title,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    if (singleScreen) menuOpen = true
                    else if (scannerEnabled) onOpenScanner()
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.surface, CircleShape)
                    .border(1.dp, colors.border, CircleShape),
            ) {
                Icon(
                    imageVector = if (singleScreen) Icons.Outlined.AccountCircle else Icons.Outlined.QrCodeScanner,
                    contentDescription = if (singleScreen) "Menü" else "Névjegy beolvasása",
                    tint = colors.primary,
                )
            }
        }

        if (nfcStatus.isAvailable) {
            VizitPanel {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (nfcStatus.isReady) colors.primarySubtle else colors.controlTrack,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Nfc,
                            contentDescription = null,
                            tint = if (nfcStatus.isReady) colors.primary else colors.textMuted,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (nfcStatus.isReady) "NFC kész" else "NFC nem érhető el",
                            style = Vizit.type.body,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (nfcStatus.isReady) "A jelenlegi profil átadásra kész." else "Kapcsold be az NFC-t a rendszerbeállításokban.",
                            style = Vizit.type.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    if (nfcStatus.isReady) {
                        VizitButton(
                            text = "Indítás",
                            onClick = { message = onStartNfcShare() },
                            style = VizitButtonStyle.Secondary,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(Vizit.radius.xl))
                .border(1.dp, colors.border, RoundedCornerShape(Vizit.radius.xl))
                .padding(Vizit.space.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
        ) {
            Text(
                text = profile.resolvedDisplayName.ifBlank { "VIZIT profil" },
                style = Vizit.type.h3,
                textAlign = TextAlign.Center,
            )
            if (qr != null) {
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = "VIZIT profil QR-kód",
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .aspectRatio(1f)
                        .background(Color.White, RoundedCornerShape(Vizit.radius.md))
                        .padding(Vizit.space.sm),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .aspectRatio(1f)
                        .background(colors.controlTrack, RoundedCornerShape(Vizit.radius.md)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = colors.textMuted)
                        Spacer(Modifier.height(Vizit.space.xs))
                        Text("A nyilvános profil még nem érhető el", color = colors.textMuted, textAlign = TextAlign.Center)
                    }
                }
            }
            Text(
                text = publicProfileUrl ?: "A profil-link a sikeres szinkron után jelenik meg.",
                style = Vizit.type.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        VizitButton(
            text = "Megosztás",
            onClick = {
                if (publicProfileUrl != null) shareUrl(context, publicProfileUrl)
                else message = "A nyilvános profil-link még nem érhető el."
            },
            icon = Icons.Outlined.Share,
            enabled = publicProfileUrl != null,
            modifier = Modifier.fillMaxWidth(),
        )
        VizitButton(
            text = "Profil megnyitása",
            onClick = {
                if (publicProfileUrl != null) openUrl(context, publicProfileUrl)
                else message = "A nyilvános profil-link még nem érhető el."
            },
            icon = Icons.Outlined.Language,
            style = VizitButtonStyle.Secondary,
            enabled = publicProfileUrl != null,
            modifier = Modifier.fillMaxWidth(),
        )

        message?.let {
            VizitStatusPill(text = it, tone = VizitTone.Info)
        }

        Spacer(Modifier.height(Vizit.space.xl))
    }

    if (menuOpen) {
        ModalBottomSheet(onDismissRequest = { menuOpen = false }) {
            Column(
                modifier = Modifier.padding(horizontal = Vizit.space.md, vertical = Vizit.space.sm),
                verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
            ) {
                Text(profile.resolvedDisplayName.ifBlank { "VIZIT" }, style = Vizit.type.h3)
                VizitGroup {
                    VizitRow(
                        label = "Profil szerkesztése",
                        icon = Icons.Outlined.Edit,
                        onClick = { menuOpen = false; onOpenProfile() },
                    )
                    if (scannerEnabled) {
                        VizitDivider()
                        VizitRow(
                            label = "Névjegy beolvasása",
                            icon = Icons.Outlined.QrCodeScanner,
                            onClick = { menuOpen = false; onOpenScanner() },
                        )
                    }
                    if (analyticsEnabled) {
                        VizitDivider()
                        VizitRow(
                            label = "Statisztikák",
                            icon = Icons.Outlined.BarChart,
                            onClick = { menuOpen = false; onOpenAnalytics() },
                        )
                    }
                    if (businessPortalEnabled) {
                        VizitDivider()
                        VizitRow(
                            label = "Vállalkozói Portál",
                            icon = Icons.Outlined.BusinessCenter,
                            onClick = { menuOpen = false; onOpenBusinessHub() },
                        )
                    }
                }
                VizitGroup {
                    VizitRow(
                        label = "Beállítások",
                        icon = Icons.Outlined.Settings,
                        onClick = { menuOpen = false; onOpenSettings() },
                    )
                }
                Spacer(Modifier.height(Vizit.space.lg))
            }
        }
    }
}

private fun shareUrl(context: Context, url: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "VIZIT profil")
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(send, "VIZIT profil megosztása"))
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
