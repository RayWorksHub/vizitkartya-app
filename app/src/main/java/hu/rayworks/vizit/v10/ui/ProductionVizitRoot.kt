package hu.rayworks.vizit.v10.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
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

/** Az éles állapotkezelők és a ZIP-ből átvett teljes Compose felület közötti híd. */
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

    val runtime = remember(vizitViewModel, authViewModel, context, scope) {
        RuntimeBindings(
            onProfileSelected = vizitViewModel::switchProfile,
            onSaveProfile = save@{ profile ->
                if (app.operationBusy || vizitViewModel.profileCatalogBusy) return@save
                app.operationBusy = true
                val epochAtStart = accountEpoch
                scope.launch {
                    if (accountEpoch != epochAtStart) return@launch
                    try {
                        val issue = runCatching {
                            vizitViewModel.saveProfile(profile.toContactProfile(context), profile.id, profile.toCardPresentation())
                        }.getOrElse { it.localizedMessage ?: "A mentés nem sikerült." }
                        if (accountEpoch != epochAtStart) return@launch
                        if (issue == null) {
                            app.confirmSaved(profile)
                            app.sheet = null
                            if (app.wizardOpen) app.closeWizard(force = true)
                            app.toast("A névjegy mentve.")
                        } else {
                            app.toast(issue)
                        }
                    } finally {
                        if (accountEpoch == epochAtStart) app.operationBusy = false
                    }
                }
            },
            onBeginAdditionalProfile = vizitViewModel::beginAdditionalProfile,
            onCancelAdditionalProfile = vizitViewModel::cancelAdditionalProfile,
            onCreateAdditionalProfile = create@{ profile, isPublic ->
                if (app.operationBusy) return@create
                app.operationBusy = true
                val epochAtStart = accountEpoch
                scope.launch {
                    if (accountEpoch != epochAtStart) return@launch
                    try {
                        val publishable = profile.copy(isPublic = isPublic)
                        val issue = runCatching {
                            vizitViewModel.createAdditionalProfile(publishable.toContactProfile(context), publishable.toCardPresentation())
                        }.getOrElse { it.localizedMessage ?: "Az új profil nem hozható létre." }
                        if (accountEpoch != epochAtStart) return@launch
                        if (issue == null) {
                            app.closeWizard(force = true)
                            app.toast(if (isPublic) "Az új profil elkészült és publikus." else "Az új profil elkészült.")
                        } else {
                            app.toast(issue)
                        }
                    } finally {
                        if (accountEpoch == epochAtStart) app.operationBusy = false
                    }
                }
            },
            onDeleteActiveProfile = vizitViewModel::deleteProfile,
            onPresentationChanged = { vizitViewModel.updateCardPresentation(it.id, it.toCardPresentation()) },
            onAutomaticSyncChanged = vizitViewModel::updateAutomaticSyncEnabled,
            onRetrySync = vizitViewModel::retryProfileSync,
            onResolveConflict = vizitViewModel::resolveProfileConflict,
            onStartNfcShare = vizitViewModel::startNfcShare,
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
            else -> vizitViewModel.unbindProfileOwner()
        }
    }

    if (!authViewModel.debugLocalProfile && vizitViewModel.accountState.ownerId == boundOwnerId) {
        AccountProfileBridge(app, vizitViewModel.accountState)
    } else if (authViewModel.debugLocalProfile) {
        LaunchedEffect(vizitViewModel.profile, vizitViewModel.cardPresentation) {
            app.replaceProfiles(listOf(vizitViewModel.profile.toV10Profile(
                id = "local-profile", label = vizitViewModel.profile.resolvedDisplayName,
                presentation = vizitViewModel.cardPresentation,
            )), "local-profile")
        }
    }

    SideEffect {
        app.accountName = when (val identity = session) {
            is AuthSessionState.Authenticated -> identity.displayName.ifBlank { "Fiókod" }
            is AuthSessionState.RefreshFailed -> identity.displayName.ifBlank { "Fiókod" }
            else -> ""
        }
        app.accountEmail = when (val identity = session) {
            is AuthSessionState.Authenticated -> identity.email
            is AuthSessionState.RefreshFailed -> identity.email
            else -> ""
        }
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
        app.multiProfileEnabled = vizitViewModel.featureFlags.multiProfile
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
        vizitViewModel.profileLoadStatus,
        vizitViewModel.hasOfflineProfileSession,
        vizitViewModel.needsFirstProfile,
    ) {
        app.gate = when {
            authViewModel.registrationConfirmationInProgress -> Gate.Loading
            authViewModel.passwordRecovery && session !is AuthSessionState.SignedOut -> Gate.Auth(AuthMode.NewPassword)
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

        if (
            app.gate == null &&
            vizitViewModel.needsFirstProfile
        ) {
            app.openInitialProfileWizard()
        }
    }

    LaunchedEffect(vizitViewModel.profileCatalogMessage) {
        vizitViewModel.profileCatalogMessage?.let {
            app.toast(it)
            vizitViewModel.clearProfileCatalogMessage()
        }
    }
    LaunchedEffect(vizitViewModel.conflictResolutionMessage) {
        vizitViewModel.conflictResolutionMessage?.let(app::toast)
    }
    LaunchedEffect(vizitViewModel.nfcSharePhase) {
        if (vizitViewModel.nfcSharePhase == NfcSharePhase.IDLE && app.pages.lastOrNull() == Page.NfcSend) {
            app.pop()
        }
    }

    VizitApp(app = app, authViewModel = authViewModel)
}

/** The whole catalog comes from complete, independently stored owner/profile records. */
@Composable
internal fun AccountProfileBridge(app: AppState, state: hu.rayworks.vizit.data.account.AccountProfileState) {
    LaunchedEffect(state.profiles, state.activeId, state.ownerId) {
        val profiles = state.profiles.map { owned ->
            check(owned.ownerId == state.ownerId)
            owned.profile.toV10Profile(id = owned.id, label = owned.profile.resolvedDisplayName,
                presentation = owned.presentation).copy(cloudSynced = owned.status == ProfileSyncStatus.SYNCED)
        }
        app.replaceProfiles(profiles, state.activeId)
    }
}
