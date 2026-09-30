package hu.rayworks.vizit.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContactPage
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.VizitViewModel
import hu.rayworks.vizit.auth.AuthViewModel
import hu.rayworks.vizit.data.sync.ProfileSyncStatus
import hu.rayworks.vizit.data.remote.NodeBackendApi
import hu.rayworks.vizit.data.remote.SupabaseProvider
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.VizitMinTouchTarget
import hu.rayworks.vizit.ui.design.components.VizitBanner
import hu.rayworks.vizit.ui.design.components.VizitTone
import hu.rayworks.vizit.ui.design.components.vizitReduceMotion
import hu.rayworks.vizit.ui.screens.BusinessHubScreen
import hu.rayworks.vizit.ui.screens.AnalyticsScreen
import hu.rayworks.vizit.ui.screens.CardAppearanceScreen
import hu.rayworks.vizit.ui.screens.CardScreen
import hu.rayworks.vizit.ui.screens.DataVisibilityScreen
import hu.rayworks.vizit.ui.screens.HomeScreen
import hu.rayworks.vizit.ui.screens.NfcShareScreen
import hu.rayworks.vizit.ui.screens.QrScanScreen
import hu.rayworks.vizit.ui.screens.SettingsScreen
import hu.rayworks.vizit.ui.screens.ShareScreen
import hu.rayworks.vizit.ui.screens.ProfileWizardScreen
import hu.rayworks.vizit.ui.screens.ProfileSwitcherBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Four primary destinations. Everything that is not a top-level task — the
 * knowledge hub, profile editing, password change — is reached from inside a
 * destination, so the bar stays learnable at a glance.
 */
enum class AppSection(val label: String, val icon: ImageVector) {
    HOME("Kezdőlap", Icons.Outlined.Home),
    CARD("Névjegy", Icons.Outlined.ContactPage),
    SHARE("Megosztás", Icons.Outlined.Share),
    SETTINGS("Beállítások", Icons.Outlined.Settings),
}

@Composable
fun VizitApp(
    viewModel: VizitViewModel,
    authViewModel: AuthViewModel,
    offlineMode: Boolean = false,
) {
    var selectedSection by rememberSaveable { mutableStateOf(AppSection.HOME) }
    var showKnowledgeHub by rememberSaveable { mutableStateOf(false) }
    var showAnalytics by rememberSaveable { mutableStateOf(false) }
    var showCardAppearance by rememberSaveable { mutableStateOf(false) }
    var showDataVisibility by rememberSaveable { mutableStateOf(false) }
    var showScanner by rememberSaveable { mutableStateOf(false) }
    var wizardCompleted by rememberSaveable { mutableStateOf(false) }
    var wizardSaving by rememberSaveable { mutableStateOf(false) }
    val reduceMotion = vizitReduceMotion()
    val sectionStateHolder = rememberSaveableStateHolder()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var exportError by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<ByteArray?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { destination ->
        val data = pendingExport
        pendingExport = null
        if (destination != null && data != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(destination, "w")?.use { it.write(data) }
                        ?: error("A kiválasztott fájl nem írható.")
                }
                exportError = false
                exportMessage = "Az adataidat sikeresen exportáltuk."
            } catch (failure: Exception) {
                exportError = true
                exportMessage = failure.localizedMessage ?: "Az exportálás nem sikerült."
            }
        }
    }
    fun exportAccount() {
        scope.launch {
            exportMessage = null
            try {
                val payload = NodeBackendApi(SupabaseProvider.getOrNull()).request("GET", "/api/account")
                pendingExport = payload.toString().toByteArray(Charsets.UTF_8)
                exportLauncher.launch("vizit-adataim.json")
            } catch (failure: Exception) {
                exportError = true
                exportMessage = failure.localizedMessage ?: "Az exportálás nem sikerült."
            }
        }
    }

    BackHandler(
        enabled = viewModel.isNfcShareActive ||
            showKnowledgeHub ||
            showAnalytics ||
            showCardAppearance ||
            showDataVisibility ||
            showScanner ||
            selectedSection != AppSection.HOME,
    ) {
        when {
            viewModel.isNfcShareActive -> viewModel.stopNfcShare()
            showScanner -> showScanner = false
            showDataVisibility -> showDataVisibility = false
            showCardAppearance -> showCardAppearance = false
            showAnalytics -> showAnalytics = false
            showKnowledgeHub -> showKnowledgeHub = false
            selectedSection != AppSection.HOME -> selectedSection = AppSection.HOME
        }
    }

    val creatingAdditionalProfile = viewModel.creatingAdditionalProfile
    if (creatingAdditionalProfile ||
        ((viewModel.profile.resolvedDisplayName.isBlank() || wizardSaving) && !wizardCompleted)
    ) {
        ProfileWizardScreen(onSave = { draft ->
            wizardSaving = true
            val issue = try {
                if (creatingAdditionalProfile) viewModel.createAdditionalProfile(draft)
                else viewModel.saveProfile(draft)
            } catch (failure: Exception) {
                failure.localizedMessage ?: "A mentés nem sikerült."
            }
            if (issue != null) wizardSaving = false
            issue
        }, presentation = viewModel.cardPresentation,
            onAppearanceChange = viewModel::updateCardPresentation,
            additionalProfile = creatingAdditionalProfile,
            onCancel = if (creatingAdditionalProfile) viewModel::cancelAdditionalProfile else null,
            onDone = {
                viewModel.cancelAdditionalProfile()
                if (!creatingAdditionalProfile) wizardCompleted = true
                wizardSaving = false
            })
        return
    }

    // Full-screen NFC hand-off takes over the whole app while it is running.
    if (viewModel.isNfcShareActive) {
        NfcShareScreen(
            profile = viewModel.profile,
            phase = viewModel.nfcSharePhase,
            photoIncluded = viewModel.nfcPhotoIncluded,
            onRoutingFailed = viewModel::reportNfcRoutingFailure,
            onStop = viewModel::stopNfcShare,
        )
        return
    }

    if (showKnowledgeHub && viewModel.featureFlags.businessPortal) {
        BusinessHubScreen(onBack = { showKnowledgeHub = false })
        return
    }

    if (showAnalytics && viewModel.featureFlags.analytics) {
        AnalyticsScreen(onBack = { showAnalytics = false })
        return
    }

    if (showScanner && viewModel.featureFlags.qrScanner) {
        QrScanScreen(onClose = { showScanner = false })
        return
    }

    if (showCardAppearance) {
        CardAppearanceScreen(
            profile = viewModel.profile,
            presentation = viewModel.cardPresentation,
            onPresentationChange = viewModel::updateCardPresentation,
            onClose = { showCardAppearance = false },
        )
        return
    }

    if (showDataVisibility) {
        DataVisibilityScreen(
            profile = viewModel.profile,
            presentation = viewModel.cardPresentation,
            onPresentationChange = viewModel::updateCardPresentation,
            onClose = { showDataVisibility = false },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Vizit.colors.canvas),
    ) {
        if (offlineMode) {
            VizitBanner(
                text = "Offline mód – a helyi profil használható, a szinkron később folytatódik.",
                tone = VizitTone.Info,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = Vizit.space.md, vertical = Vizit.space.xs),
            )
        }

        ProfileSwitcherBar(
            profiles = viewModel.accountProfiles,
            multiProfileEnabled = viewModel.featureFlags.multiProfile,
            busy = viewModel.profileCatalogBusy,
            message = viewModel.profileCatalogMessage,
            onSwitch = viewModel::switchProfile,
            onCreate = viewModel::beginAdditionalProfile,
            onDeleteActive = viewModel::deleteActiveProfile,
            onDismissMessage = viewModel::clearProfileCatalogMessage,
            modifier = Modifier.padding(horizontal = Vizit.space.md, vertical = Vizit.space.xs),
        )

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = selectedSection,
                transitionSpec = {
                    val duration = if (reduceMotion) 0 else 180
                    fadeIn(tween(duration)) togetherWith fadeOut(tween(duration))
                },
                label = "vizit-section",
            ) { section ->
                sectionStateHolder.SaveableStateProvider(section.name) {
                    when (section) {
                    AppSection.HOME -> HomeScreen(
                        profile = viewModel.profile,
                        presentation = viewModel.cardPresentation,
                        nfcStatus = viewModel.nfcStatus,
                        syncState = viewModel.profileSyncState,
                        onStartNfcShare = viewModel::startNfcShare,
                        onOpenCard = { selectedSection = AppSection.CARD },
                        onOpenShare = { selectedSection = AppSection.SHARE },
                        onOpenKnowledgeHub = { showKnowledgeHub = true },
                        onOpenAnalytics = { showAnalytics = true },
                        onOpenCRM = { uriHandler.openUri("https://www.vizitkartyam.hu/auth/sign-in?next=%2Fdashboard%2Fcrm") },
                        onOpenOnlineEditor = { uriHandler.openUri("https://www.vizitkartyam.hu/auth/sign-in?next=%2Fdashboard%2Fprofile") },
                        onOpenScanner = { showScanner = true },
                        featureFlags = viewModel.featureFlags,
                        onShareAsText = viewModel::shareAsText,
                    )

                    AppSection.CARD -> CardScreen(
                        profile = viewModel.profile,
                        presentation = viewModel.cardPresentation,
                        onSave = viewModel::saveProfile,
                        onShare = { selectedSection = AppSection.SHARE },
                        onOpenCardAppearance = { showCardAppearance = true },
                        onOpenDataVisibility = { showDataVisibility = true },
                    )

                    AppSection.SHARE -> ShareScreen(
                        profile = viewModel.profile,
                        presentation = viewModel.cardPresentation,
                        synchronized = viewModel.profileSyncState.status == ProfileSyncStatus.SYNCED &&
                            !viewModel.profileSyncState.pendingChanges,
                        nfcStatus = viewModel.nfcStatus,
                        onStartNfcShare = viewModel::startNfcShare,
                    )

                    AppSection.SETTINGS -> SettingsScreen(
                        nfcStatus = viewModel.nfcStatus,
                        cardPresentation = viewModel.cardPresentation,
                        onOpenCardAppearance = { showCardAppearance = true },
                        onOpenDataVisibility = { showDataVisibility = true },
                        syncState = viewModel.profileSyncState,
                        automaticSyncEnabled = viewModel.automaticSyncEnabled,
                        themeMode = viewModel.themeMode,
                        onThemeModeChange = viewModel::updateThemeMode,
                        onAutomaticSyncChanged = viewModel::updateAutomaticSyncEnabled,
                        onRetrySync = viewModel::retryProfileSync,
                        onResolveConflict = viewModel::resolveProfileConflict,
                        resolutionMessage = viewModel.conflictResolutionMessage,
                        authActionState = authViewModel.actionState,
                        googleSignInEnabled = authViewModel.googleSignInEnabled,
                        cloudAccountAvailable = authViewModel.cloudAccountAvailable,
                        privacyPolicyUrl = authViewModel.privacyPolicyUrl,
                        termsUrl = authViewModel.termsUrl,
                        onClearAuthAction = authViewModel::clearActionState,
                        onLogout = authViewModel::logout,
                        onDeleteAccount = authViewModel::deleteAccount,
                        onExportAccount = ::exportAccount,
                        exportMessage = exportMessage,
                        exportError = exportError,
                    )
                    }
                }
            }
        }

        VizitBottomBar(
            selected = selectedSection,
            onSelect = { selectedSection = it },
        )
    }
}

@Composable
private fun VizitBottomBar(
    selected: AppSection,
    onSelect: (AppSection) -> Unit,
) {
    val colors = Vizit.colors
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = colors.surface,
        contentColor = colors.textSecondary,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets.navigationBars,
    ) {
        AppSection.entries.forEach { section ->
            val isSelected = section == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(section) },
                icon = {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(
                        text = section.label,
                        style = Vizit.type.caption,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.primarySubtle,
                    unselectedIconColor = colors.textMuted,
                    unselectedTextColor = colors.textMuted,
                ),
            )
        }
    }
}
