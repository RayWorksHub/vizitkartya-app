package hu.rayworks.vizit.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.NfcStatus
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.sync.ProfileSyncState
import hu.rayworks.vizit.data.sync.ProfileSyncStatus
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitBrandHeader
import hu.rayworks.vizit.ui.design.components.VizitBrandHeaderStyle
import hu.rayworks.vizit.ui.design.components.VizitUserBadge
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitDigitalCard
import hu.rayworks.vizit.ui.design.components.VizitIconChip
import hu.rayworks.vizit.ui.design.components.VizitRow
import hu.rayworks.vizit.ui.design.components.VizitStatusPill
import hu.rayworks.vizit.ui.design.components.VizitTone
import hu.rayworks.vizit.ui.design.components.VizitGroup
import hu.rayworks.vizit.ui.util.rememberProfilePhoto
import kotlinx.coroutines.launch

/**
 * Home answers four questions immediately: who is signed in, what their card
 * looks like, how to hand it over, and whether the phone is actually ready to
 * do it. One primary action, three shortcuts, then status.
 */
@Composable
fun HomeScreen(
    profile: ContactProfile,
    nfcStatus: NfcStatus,
    syncState: ProfileSyncState,
    onStartNfcShare: () -> String?,
    onOpenCard: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenKnowledgeHub: () -> Unit,
    onShareAsText: (Context) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Vizit.colors
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val photo = rememberProfilePhoto(profile.photoBase64)

    Box(modifier = modifier.fillMaxSize().background(colors.canvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = Vizit.space.md),
            verticalArrangement = Arrangement.spacedBy(Vizit.space.xl),
        ) {
            Spacer(Modifier.height(Vizit.space.xs))

            VizitBrandHeader(style = VizitBrandHeaderStyle.Full)

            VizitUserBadge(
                displayName = profile.resolvedDisplayName,
                initials = profile.initials,
                photoBase64 = profile.photoBase64,
                onClick = onOpenCard,
            )

            Box(modifier = Modifier.clickable(role = Role.Button, onClick = onOpenCard)) {
                VizitDigitalCard(
                    fullName = profile.resolvedDisplayName,
                    initials = profile.initials,
                    jobTitle = profile.jobTitle,
                    company = profile.company,
                    phone = profile.phone,
                    email = profile.email,
                    photo = photo,
                )
            }

            VizitButton(
                text = "Névjegy megosztása",
                onClick = onOpenShare,
                icon = Icons.Outlined.Share,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
            ) {
                QuickTile(
                    icon = Icons.Outlined.Nfc,
                    title = "NFC",
                    subtitle = "Érintéssel",
                    enabled = nfcStatus.isReady,
                    modifier = Modifier.weight(1f),
                ) {
                    onStartNfcShare()?.let { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                }
                QuickTile(
                    icon = Icons.Outlined.QrCode2,
                    title = "QR-kód",
                    subtitle = "Mutatás",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenShare,
                )
                QuickTile(
                    icon = Icons.Outlined.ContentCopy,
                    title = "Egyéb",
                    subtitle = "Megosztás",
                    modifier = Modifier.weight(1f),
                ) { onShareAsText(context) }
            }

            VizitStatusPill(
                text = if (nfcStatus.isReady) "NFC használatra kész" else "NFC beállítás szükséges",
                tone = if (nfcStatus.isReady) VizitTone.Success else VizitTone.Warning,
            )

            if (syncState.status == ProfileSyncStatus.RETRY_SCHEDULED ||
                syncState.status == ProfileSyncStatus.CONFLICT
            ) {
                VizitStatusPill(
                    text = if (syncState.status == ProfileSyncStatus.CONFLICT) {
                        "A szinkron ütközött"
                    } else {
                        "A szinkron újrapróbálásra vár"
                    },
                    tone = if (syncState.status == ProfileSyncStatus.CONFLICT) {
                        VizitTone.Error
                    } else {
                        VizitTone.Warning
                    },
                )
            }

            VizitGroup {
                VizitRow(
                    label = "Vállalkozói Portál",
                    supporting = "VOSZ, edukáció, digitális segítség és eszköztár",
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    onClick = onOpenKnowledgeHub,
                )
            }

            Spacer(Modifier.height(Vizit.space.md))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Vizit.space.md),
        )
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = Vizit.colors
    Column(
        modifier = modifier
            .background(colors.surface, RoundedCornerShape(Vizit.radius.lg))
            .border(1.dp, colors.border, RoundedCornerShape(Vizit.radius.lg))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Vizit.space.sm, vertical = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.xs + 2.dp),
    ) {
        VizitIconChip(
            icon = icon,
            tint = if (enabled) colors.primary else colors.textDisabled,
            background = if (enabled) colors.primarySubtle else colors.controlDisabled,
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = Vizit.type.label,
                color = if (enabled) colors.textPrimary else colors.textDisabled,
            )
            Text(text = subtitle, style = Vizit.type.caption, color = colors.textMuted)
        }
    }
}
