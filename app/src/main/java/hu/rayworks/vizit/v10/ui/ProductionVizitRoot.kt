package hu.rayworks.vizit.v10.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import hu.rayworks.vizit.NfcSharePhase
import hu.rayworks.vizit.ProfileLoadStatus
import hu.rayworks.vizit.VizitViewModel
import hu.rayworks.vizit.auth.AuthSessionState
import hu.rayworks.vizit.auth.AuthViewModel
import hu.rayworks.vizit.data.remote.NodeBackendApi
import hu.rayworks.vizit.data.remote.SupabaseProvider
import hu.rayworks.vizit.data.sync.ProfileSyncStatus
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.AuthMode
import hu.rayworks.vizit.v10.data.Gate
import hu.rayworks.vizit.v10.data.NfcDevice
import hu.rayworks.vizit.v10.data.Page
import hu.rayworks.vizit.v10.data.Profile
import hu.rayworks.vizit.v10.data.RuntimeBindings
import hu.rayworks.vizit.v10.data.SyncStatus
import hu.rayworks.vizit.v10.data.toCardPresentation
import hu.rayworks.vizit.v10.data.toContactProfile
import hu.rayworks.vizit.v10.data.toV10Profile
import hu.rayworks.vizit.v10.ui.theme.ThemeMode
import hu.rayworks.vizit.v10.ui.theme.ThemeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOCAL_DEBUG_PROFILE_OWNER_ID = "local-dev-profile"

/**
 * Production bridge for the exact Android UI delivered in VizitTeljes(3).zip.
 *
 * The ZIP owns presentation, navigation and animation. The current production
 * ViewModels remain the single source of truth for account, sync, NFC and cards.
 */
@Composable
fun ProductionVizitRoot(
    vizitViewModel: VizitViewModel,
    authViewModel: AuthViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session by authViewModel.sessionState.collectAsState()
    val app = remember {
        AppState(
            initialProfiles = listOf(Profile(id = "loading", label = "VIZIT", real = true, name = "")),
            productionMode = true,
        ).apply { gate = Gate.Loading }
    }
    var boundOwnerId by remember { mutableStateOf<String?>(null) }
    var accountEpoch by remember { mutableStateOf(0L) }

    fun selectIfNeeded(profileId: String) {
        if (vizitViewModel.activeBusinessCardId != profileId) {
            vizitViewModel.selectBusinessCard(profileId)
        }
    }

    val runtime = remember(vizitViewModel, authViewModel, context, scope) {
        RuntimeBindings(
            onProfileSelected = vizitViewModel::selectBusinessCard,
            onSaveProfile = save@{ profile ->
                if (app.operationBusy) return@save
                app.operationBusy = true
                val epochAtStart = accountEpoch
                scope.launch {
                    try {
                        if (accountEpoch != epochAtStart) return@launch
                        selectIfNeeded(profile.id)
                        val issue = runCatching {
                            vizitViewModel.saveProfile(profile.toContactProfile(context))
                        }.getOrElse { it.localizedMessage ?: "A mentés nem sikerült." }
                        if (accountEpoch != epochAtStart) return@launch
                        if (issue == null) {
                            vizitViewModel.updateCardPresentation(profile.toCardPresentation())
                            app.confirmSaved(profile)
                            app.sheet = null
                            app.toast("A névjegy mentve.")
                        } else {
                            app.toast(issue)
                        }
                    } finally {
                        if (accountEpoch == epochAtStart) app.operationBusy = false
                    }
                }
            },
            // The current repository reserves no remote row before the wizard is
            // completed, so opening/cancelling an additional card is UI-only.
            onBeginAdditionalProfile = { null },
            onCancelAdditionalProfile = {},
            onCreateAdditionalProfile = create@{ profile, isPublic ->
                if (app.operationBusy) return@create
                app.operationBusy = true
                val epochAtStart = accountEpoch
                scope.launch {
                    try {
                        if (accountEpoch != epochAtStart) return@launch
                        val publishable = profile.copy(isPublic = isPublic)
                        val issue = runCatching {
                            vizitViewModel.createBusinessCard(publishable.toContactProfile(context))
                        }.getOrElse { it.localizedMessage ?: "Az új névjegy nem hozható létre." }
                        if (accountEpoch != epochAtStart) return@launch
                        if (issue == null) {
                            // The repository observation will replace the catalog
                            // with the server-owned card immediately afterwards.
                            vizitViewModel.updateCardPresentation(publishable.toCardPresentation())
                            app.closeWizard(force = true)
                            app.toast(if (isPublic) "Az új névjegy elkészült és publikus." else "Az új névjegy elkészült.")
                        } else {
                            app.toast(issue)
                        }
                    } finally {
                        if (accountEpoch == epochAtStart) app.operationBusy = false
                    }
                }
            },
            onDeleteActiveProfile = delete@{ profileId ->
                if (app.operationBusy) return@delete
                app.operationBusy = true
                val epochAtStart = accountEpoch
                scope.launch {
                    try {
                        if (accountEpoch != epochAtStart) return@launch
                        selectIfNeeded(profileId)
                        val issue = vizitViewModel.deleteActiveBusinessCard()
                        if (accountEpoch != epochAtStart) return@launch
                        if (issue == null) {
                            app.sheet = null
                            app.toast("A névjegy törölve.")
                        } else {
                            app.toast(issue)
                        }
                    } finally {
                        if (accountEpoch == epochAtStart) app.operationBusy = false
                    }
                }
            },
            onPresentationChanged = { profile ->
                selectIfNeeded(profile.id)
                vizitViewModel.updateCardPresentation(profile.toCardPresentation())
            },
            onAutomaticSyncChanged = vizitViewModel::updateAutomaticSyncEnabled,
            onRetrySync = vizitViewModel::retryProfileSync,
            onResolveConflict = vizitViewModel::resolveProfileConflict,
            onStartNfcShare = { profileId ->
                selectIfNeeded(profileId)
                vizitViewModel.startNfcShare()
            },
            onStopNfcShare = vizitViewModel::stopNfcShare,
            onOpenNfcSettings = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure { app.toast("Az NFC-beállítások nem nyithatók meg.") }
            },
            onThemeChanged = { mode ->
                vizitViewModel.updateThemeMode(
                    when (mode) {
                        ThemeMode.Light -> hu.rayworks.vizit.ui.design.ThemeMode.LIGHT
                        ThemeMode.Dark -> hu.rayworks.vizit.ui.design.ThemeMode.DARK
                        ThemeMode.System -> hu.rayworks.vizit.ui.design.ThemeMode.SYSTEM
                    },
                )
            },
            onExportAccount = { destination ->
                scope.launch {
                    try {
                        val payload = NodeBackendApi(SupabaseProvider.getOrNull())
                            .request("GET", "/api/account")
                            .toString()
                            .toByteArray(Charsets.UTF_8)
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destination, "w")?.use { it.write(payload) }
                                ?: error("A kiválasztott fájl nem írható.")
                        }
                        app.toast("Az adataidat sikeresen exportáltuk.")
                    } catch (failure: Exception) {
                        app.toast(failure.localizedMessage ?: "Az exportálás nem sikerült.")
                    }
                }
            },
            onRetryProfileLoad = vizitViewModel::retryProfileLoad,
            onRetryLegalAcceptance = authViewModel::retryLegalAcceptanceCheck,
            onUseDebugLocalProfile = authViewModel::useDebugLocalProfile,
            onLogout = authViewModel::logout,
            onDeleteAccount = { authViewModel.deleteAccount("TÖRLÉS") },
        )
    }
    SideEffect { app.runtime = runtime }

    LaunchedEffect(session, authViewModel.debugLocalProfile) {
        val nextOwnerId = when {
            authViewModel.debugLocalProfile -> LOCAL_DEBUG_PROFILE_OWNER_ID
            session is AuthSessionState.Authenticated -> (session as AuthSessionState.Authenticated).userId
            session is AuthSessionState.RefreshFailed -> (session as AuthSessionState.RefreshFailed).cachedUserId
            else -> null
        }
        if (nextOwnerId != boundOwnerId) {
            accountEpoch++
            app.resetForAccountChange()
            boundOwnerId = nextOwnerId
        }
        when {
            authViewModel.debugLocalProfile -> vizitViewModel.bindProfileOwner(
                userId = LOCAL_DEBUG_PROFILE_OWNER_ID,
                enableCloudSync = false,
            )
            session is AuthSessionState.Authenticated -> vizitViewModel.bindProfileOwner(
                userId = (session as AuthSessionState.Authenticated).userId,
                enableCloudSync = true,
            )
            session is AuthSessionState.RefreshFailed ->
                (session as AuthSessionState.RefreshFailed).cachedUserId?.let { userId ->
                    vizitViewModel.bindProfileOwner(userId = userId, enableCloudSync = true)
                }
            else -> vizitViewModel.clearProfileOwnerBinding()
        }
    }

    // Current multi-card repository -> exact ZIP card model.
    LaunchedEffect(
        vizitViewModel.businessCards,
        vizitViewModel.activeBusinessCardId,
        authViewModel.debugLocalProfile,
        vizitViewModel.profile,
        vizitViewModel.cardPresentation,
    ) {
        if (authViewModel.debugLocalProfile) {
            val local = vizitViewModel.profile
            if (local.resolvedDisplayName.isNotBlank()) {
                app.replaceProfiles(
                    listOf(
                        local.toV10Profile(
                            id = "local-profile",
                            label = local.resolvedDisplayName,
                            presentation = vizitViewModel.cardPresentation,
                        ),
                    ),
                    "local-profile",
                )
            }
        } else {
            val ownerId = boundOwnerId
            if (ownerId != null) {
                val cards = vizitViewModel.businessCards
                    .filter { it.ownerId == ownerId }
                    .map { card ->
                        card.profile.toV10Profile(
                            id = card.profileId,
                            label = card.profile.resolvedDisplayName,
                            presentation = card.presentation,
                        ).copy(cloudSynced = vizitViewModel.profileSyncState.status == ProfileSyncStatus.SYNCED)
                    }
                app.replaceProfiles(cards, vizitViewModel.activeBusinessCardId)
            }
        }
    }

    SideEffect {
        app.accountName = vizitViewModel.profile.resolvedDisplayName
        app.accountEmail = vizitViewModel.profile.email
        app.offline = session is AuthSessionState.RefreshFailed
        app.sync = when (vizitViewModel.profileSyncState.status) {
            ProfileSyncStatus.LOCAL_ONLY -> SyncStatus.LocalOnly
            ProfileSyncStatus.SYNCED -> SyncStatus.Synced
            ProfileSyncStatus.PENDING -> SyncStatus.Pending
            ProfileSyncStatus.SYNCING -> SyncStatus.Syncing
            ProfileSyncStatus.RETRY_SCHEDULED -> SyncStatus.Retry
            ProfileSyncStatus.CONFLICT -> SyncStatus.Conflict
        }
        app.autoSync = vizitViewModel.automaticSyncEnabled
        app.nfcDevice = when {
            !vizitViewModel.nfcStatus.isAvailable || !vizitViewModel.nfcStatus.hasHostCardEmulation -> NfcDevice.Unsupported
            !vizitViewModel.nfcStatus.isEnabled -> NfcDevice.SystemOff
            else -> NfcDevice.Ready
        }
        app.nfcOn = vizitViewModel.nfcStatus.isEnabled
        app.nfcSharePhase = vizitViewModel.nfcSharePhase
        app.multiProfileEnabled = true
        app.businessPortalEnabled = vizitViewModel.featureFlags.businessPortal
        app.analyticsEnabled = vizitViewModel.featureFlags.analytics
        app.qrScannerEnabled = vizitViewModel.featureFlags.qrScanner
        ThemeState.mode = when (vizitViewModel.themeMode) {
            hu.rayworks.vizit.ui.design.ThemeMode.LIGHT -> ThemeMode.Light
            hu.rayworks.vizit.ui.design.ThemeMode.DARK -> ThemeMode.Dark
            hu.rayworks.vizit.ui.design.ThemeMode.SYSTEM -> ThemeMode.System
        }
    }

    LaunchedEffect(
        session,
        authViewModel.debugLocalProfile,
        authViewModel.passwordRecovery,
        authViewModel.registrationConfirmationInProgress,
        authViewModel.actionState,
        vizitViewModel.profileLoadStatus,
        vizitViewModel.businessCards,
        vizitViewModel.profile,
    ) {
        app.gate = when {
            authViewModel.registrationConfirmationInProgress -> Gate.Loading
            authViewModel.passwordRecovery &&
                (session is AuthSessionState.Authenticated ||
                    session is AuthSessionState.LegalAcceptanceRequired ||
                    session is AuthSessionState.LegalAcceptanceCheckFailed) ->
                Gate.Auth(AuthMode.NewPassword)

            authViewModel.debugLocalProfile -> when (vizitViewModel.profileLoadStatus) {
                ProfileLoadStatus.READY -> null
                ProfileLoadStatus.UNAVAILABLE -> Gate.ProfileError
                else -> Gate.Loading
            }

            session is AuthSessionState.Authenticated -> when (vizitViewModel.profileLoadStatus) {
                ProfileLoadStatus.READY -> null
                ProfileLoadStatus.UNAVAILABLE -> Gate.ProfileError
                else -> Gate.Loading
            }

            session is AuthSessionState.LegalAcceptanceRequired -> Gate.Auth(AuthMode.Legal)
            session is AuthSessionState.LegalAcceptanceCheckFailed -> Gate.LegalFailed

            session is AuthSessionState.RefreshFailed ->
                if (vizitViewModel.hasOfflineProfileSession) null else Gate.SessionExpired

            session is AuthSessionState.SignedOut -> {
                val current = app.gate as? Gate.Auth
                if (current?.mode in listOf(AuthMode.Register, AuthMode.EmailSent, AuthMode.Forgot, AuthMode.ResetSent)) {
                    current
                } else {
                    Gate.Auth(AuthMode.Login)
                }
            }

            session is AuthSessionState.BackendUnavailable -> {
                app.authBanner = "A VIZIT backend ebben a buildben nincs konfigurálva." to false
                Gate.Auth(AuthMode.Login)
            }

            else -> Gate.Loading
        }

        val authenticatedReady =
            (session is AuthSessionState.Authenticated || authViewModel.debugLocalProfile) &&
                vizitViewModel.profileLoadStatus == ProfileLoadStatus.READY
        val hasAnyCard = if (authViewModel.debugLocalProfile) {
            vizitViewModel.profile.resolvedDisplayName.isNotBlank()
        } else {
            vizitViewModel.businessCards.isNotEmpty()
        }
        if (app.gate == null && authenticatedReady && !hasAnyCard) {
            app.openInitialProfileWizard()
        }
    }

    LaunchedEffect(vizitViewModel.nfcSharePhase) {
        if (vizitViewModel.nfcSharePhase == NfcSharePhase.IDLE && app.pages.lastOrNull() == Page.NfcSend) {
            app.pop()
        }
    }

    VizitApp(app = app, authViewModel = authViewModel)
}
