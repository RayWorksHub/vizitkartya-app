package hu.rayworks.vizit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.NfcStatus
import hu.rayworks.vizit.auth.AuthActionState
import hu.rayworks.vizit.auth.AuthOperation
import hu.rayworks.vizit.auth.AuthValidator
import hu.rayworks.vizit.data.sync.ProfileSyncState
import hu.rayworks.vizit.data.sync.ProfileSyncStatus
import hu.rayworks.vizit.ui.design.ThemeMode
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitBanner
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitButtonStyle
import hu.rayworks.vizit.ui.design.components.VizitDivider
import hu.rayworks.vizit.ui.design.components.VizitGroup
import hu.rayworks.vizit.ui.design.components.VizitRow
import hu.rayworks.vizit.ui.design.components.VizitSectionHeader
import hu.rayworks.vizit.ui.design.components.VizitSegmentedControl
import hu.rayworks.vizit.ui.design.components.VizitTextField
import hu.rayworks.vizit.ui.design.components.VizitTone

/**
 * System-level settings structure: Fiók / Megjelenés / Megosztás / Szinkron /
 * Jogi, with destructive actions isolated in their own outlined group at the
 * very bottom so they can never be hit by accident.
 */
@Composable
fun SettingsScreen(
    nfcStatus: NfcStatus,
    syncState: ProfileSyncState,
    automaticSyncEnabled: Boolean,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAutomaticSyncChanged: (Boolean) -> Unit,
    onRetrySync: () -> Unit,
    onResolveConflict: (Boolean) -> Unit,
    resolutionMessage: String?,
    authActionState: AuthActionState,
    googleSignInEnabled: Boolean,
    cloudAccountAvailable: Boolean,
    privacyPolicyUrl: String,
    termsUrl: String,
    onClearAuthAction: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Vizit.colors
    val context = LocalContext.current
    val authLoading = authActionState is AuthActionState.Loading
    var confirmCloudCopy by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var deleteConfirmation by rememberSaveable { mutableStateOf("") }

    fun openUrl(url: String) {
        if (url.isBlank()) return
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
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
        Text("Beállítások", style = Vizit.type.h1, color = colors.textPrimary)

        // ---------- Appearance
        VizitSectionHeader("Megjelenés")
        VizitGroup {
            VizitRow(
                label = "Téma",
                supporting = "Alapértelmezés: világos",
                icon = Icons.Outlined.DarkMode,
                showChevron = false,
            )
            VizitDivider()
            Column(modifier = Modifier.padding(Vizit.space.md)) {
                VizitSegmentedControl(
                    options = listOf("Világos", "Sötét", "Rendszer"),
                    selectedIndex = when (themeMode) {
                        ThemeMode.LIGHT -> 0
                        ThemeMode.DARK -> 1
                        ThemeMode.SYSTEM -> 2
                    },
                    onSelect = {
                        onThemeModeChange(
                            when (it) {
                                0 -> ThemeMode.LIGHT
                                1 -> ThemeMode.DARK
                                else -> ThemeMode.SYSTEM
                            },
                        )
                    },
                )
            }
        }

        // ---------- Sharing
        VizitSectionHeader("Megosztás")
        VizitGroup {
            VizitRow(
                label = "NFC",
                supporting = nfcStatus.statusText(),
                icon = Icons.Outlined.Nfc,
                showChevron = false,
            )
        }

        // ---------- Sync
        VizitSectionHeader("Szinkronizálás")
        VizitGroup {
            VizitRow(
                label = "Automatikus szinkron",
                supporting = syncState.description(),
                icon = Icons.Outlined.CloudSync,
                checked = automaticSyncEnabled,
                onCheckedChange = onAutomaticSyncChanged,
            )
            if (syncState.status == ProfileSyncStatus.RETRY_SCHEDULED ||
                syncState.status == ProfileSyncStatus.CONFLICT
            ) {
                VizitDivider()
                Column(
                    modifier = Modifier.padding(Vizit.space.md),
                    verticalArrangement = Arrangement.spacedBy(Vizit.space.xs),
                ) {
                    VizitBanner(
                        text = syncState.lastError ?: "A szinkron megszakadt.",
                        tone = if (syncState.status == ProfileSyncStatus.CONFLICT) {
                            VizitTone.Warning
                        } else {
                            VizitTone.Error
                        },
                    )
                    if (syncState.status == ProfileSyncStatus.CONFLICT) {
                        VizitButton(
                            text = "A helyi változat feltöltése",
                            onClick = { onResolveConflict(true) },
                            style = VizitButtonStyle.Secondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        VizitButton(
                            text = "A felhőváltozat használata",
                            onClick = { confirmCloudCopy = true },
                            style = VizitButtonStyle.Tertiary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        VizitButton(
                            text = "Szinkron újrapróbálása",
                            onClick = onRetrySync,
                            style = VizitButtonStyle.Secondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (resolutionMessage != null) {
                        Text(resolutionMessage, style = Vizit.type.bodySmall, color = colors.textSecondary)
                    }
                }
            }
        }

        // ---------- Legal
        VizitSectionHeader("Jogi tudnivalók")
        VizitGroup {
            VizitRow(
                label = "Adatkezelési tájékoztató",
                icon = Icons.Outlined.PrivacyTip,
                onClick = { openUrl(privacyPolicyUrl) },
            )
            VizitDivider()
            VizitRow(
                label = "Felhasználási feltételek",
                icon = Icons.Outlined.Description,
                onClick = { openUrl(termsUrl) },
            )
        }

        // ---------- Account / danger zone
        if (cloudAccountAvailable) {
            VizitSectionHeader("Fiók", tone = colors.error)
            VizitGroup(danger = true) {
                VizitRow(
                    label = "Kijelentkezés",
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    destructive = true,
                    enabled = !authLoading,
                    showChevron = false,
                    onClick = {
                        onClearAuthAction()
                        onLogout()
                    },
                )
                VizitDivider()
                VizitRow(
                    label = "Fiók végleges törlése",
                    supporting = "Az Auth-fiók, a felhőprofil és a helyi adatok is törlődnek.",
                    icon = Icons.Outlined.DeleteOutline,
                    destructive = true,
                    enabled = !authLoading,
                    showChevron = false,
                    onClick = {
                        onClearAuthAction()
                        deleteConfirmation = ""
                        showDeleteConfirmation = true
                    },
                )
            }
        } else {
            VizitBanner(
                text = "A helyi DEV tesztprofilhoz nem tartozik felhőfiók.",
                tone = VizitTone.Info,
            )
        }

        AuthAccountStatus(authActionState)

        Text(
            text = "VIZIT ${BuildConfig.VERSION_NAME}",
            style = Vizit.type.caption,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = Vizit.space.lg),
        )
        Spacer(Modifier.height(Vizit.space.md))
    }

    if (confirmCloudCopy) {
        AlertDialog(
            onDismissRequest = { confirmCloudCopy = false },
            containerColor = colors.elevated,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = { Text("A felhőváltozat használata?", style = Vizit.type.h3) },
            text = {
                Text(
                    "Ezzel lecseréled az ezen a készüléken még fel nem töltött névjegyadatokat és profilképet a felhőben tárolt változatra.",
                    style = Vizit.type.body,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmCloudCopy = false; onResolveConflict(false) }) {
                    Text("Helyi módosítások lecserélése", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCloudCopy = false }) {
                    Text("Mégse", color = colors.textSecondary)
                }
            },
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!authLoading) {
                    showDeleteConfirmation = false
                    deleteConfirmation = ""
                    onClearAuthAction()
                }
            },
            containerColor = colors.elevated,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = { Text("Biztosan törlöd a fiókodat?", style = Vizit.type.h3) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Vizit.space.sm)) {
                    Text(
                        "A művelet végleges. Törlődik az Auth-fiók, a felhőprofil, a média és az ezen az eszközön tárolt profiladat.",
                        style = Vizit.type.body,
                    )
                    Text(
                        "Megerősítésként írd be: ${AuthValidator.ACCOUNT_DELETION_PHRASE}",
                        style = Vizit.type.bodySmall,
                        color = colors.textSecondary,
                    )
                    val deletionError = (authActionState as? AuthActionState.Error)
                        ?.takeIf { it.operation == AuthOperation.DELETE_ACCOUNT }
                    VizitTextField(
                        value = deleteConfirmation,
                        onValueChange = {
                            deleteConfirmation = it
                            if (authActionState is AuthActionState.Error) onClearAuthAction()
                        },
                        label = "Megerősítés",
                        enabled = !authLoading,
                        error = deletionError?.message,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onDeleteAccount(deleteConfirmation) },
                    enabled = !authLoading,
                ) {
                    Text(
                        text = if (authLoading) "Törlés folyamatban…" else "Végleges törlés",
                        color = colors.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        deleteConfirmation = ""
                        onClearAuthAction()
                    },
                    enabled = !authLoading,
                ) {
                    Text("Mégsem", color = colors.textSecondary)
                }
            },
        )
    }
}

@Composable
private fun AuthAccountStatus(state: AuthActionState) {
    when (state) {
        is AuthActionState.Error ->
            if (state.operation == AuthOperation.LOGOUT || state.operation == AuthOperation.DELETE_ACCOUNT) {
                VizitBanner(text = state.message, tone = VizitTone.Error)
            }

        is AuthActionState.Success ->
            if (state.operation == AuthOperation.LOGOUT || state.operation == AuthOperation.DELETE_ACCOUNT) {
                VizitBanner(text = state.message, tone = VizitTone.Success)
            }

        else -> Unit
    }
}

private fun NfcStatus.statusText(): String = when {
    !isAvailable -> "A készülékben nincs elérhető NFC-egység."
    !hasHostCardEmulation -> "A készülék nem támogatja a kártyaemulációt."
    !isEnabled -> "Kapcsold be az NFC-t a rendszerbeállításokban."
    else -> "Készen áll a közvetlen kontaktátadásra."
}

private fun ProfileSyncState.description(): String = when (status) {
    ProfileSyncStatus.LOCAL_ONLY -> "Csak ezen az eszközön tárolva."
    ProfileSyncStatus.SYNCED -> "A helyi és a felhőprofil szinkronban van."
    ProfileSyncStatus.PENDING -> "A módosítás helyben mentve, felhőszinkronra vár."
    ProfileSyncStatus.SYNCING -> "Felhőszinkron folyamatban."
    ProfileSyncStatus.RETRY_SCHEDULED -> lastError ?: "A szinkron újrapróbálásra vár."
    ProfileSyncStatus.CONFLICT -> "A helyi adat megmaradt; az automatikus felülírás leállt."
}
