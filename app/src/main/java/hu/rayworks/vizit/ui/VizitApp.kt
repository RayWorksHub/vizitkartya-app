package hu.rayworks.vizit.ui

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.VizitViewModel
import hu.rayworks.vizit.auth.AuthViewModel
import hu.rayworks.vizit.data.sync.ProfileSyncStatus
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.VizitMinTouchTarget
import hu.rayworks.vizit.ui.design.components.VizitBanner
import hu.rayworks.vizit.ui.design.components.VizitTone
import hu.rayworks.vizit.ui.design.components.vizitReduceMotion
import hu.rayworks.vizit.ui.screens.BusinessHubScreen
import hu.rayworks.vizit.ui.screens.CardAppearanceScreen
import hu.rayworks.vizit.ui.screens.CardScreen
import hu.rayworks.vizit.ui.screens.DataVisibilityScreen
import hu.rayworks.vizit.ui.screens.HomeScreen
import hu.rayworks.vizit.ui.screens.NfcShareScreen
import hu.rayworks.vizit.ui.screens.SettingsScreen
import hu.rayworks.vizit.ui.screens.ShareScreen

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
    var showCardAppearance by rememberSaveable { mutableStateOf(false) }
    var showDataVisibility by rememberSaveable { mutableStateOf(false) }
    val reduceMotion = vizitReduceMotion()

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

    if (showKnowledgeHub) {
        BusinessHubScreen(onBack = { showKnowledgeHub = false })
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

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = selectedSection,
                transitionSpec = {
                    val duration = if (reduceMotion) 0 else 180
                    fadeIn(tween(duration)) togetherWith fadeOut(tween(duration))
                },
                label = "vizit-section",
            ) { section ->
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
                    )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Vizit.space.xs, vertical = Vizit.space.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSection.entries.forEach { section ->
                val isSelected = section == selected
                val interaction = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(section) },
                        )
                        .semantics {
                            this.selected = isSelected
                            contentDescription = section.label
                        }
                        .padding(vertical = Vizit.space.xxs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Vizit.space.xxs),
                ) {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(
                                            colors.primarySubtle,
                                            RoundedCornerShape(Vizit.radius.full),
                                        )
                                        .padding(horizontal = 14.dp)
                                } else {
                                    Modifier.padding(horizontal = 14.dp)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (isSelected) colors.primary else colors.textMuted,
                        )
                    }
                    Text(
                        text = section.label,
                        style = Vizit.type.caption,
                        color = if (isSelected) colors.primary else colors.textMuted,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
        }
    }
}
