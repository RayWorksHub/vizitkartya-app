package hu.rayworks.vizit.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.VizitViewModel
import hu.rayworks.vizit.ui.screens.HomeScreen
import hu.rayworks.vizit.ui.screens.NfcShareScreen
import hu.rayworks.vizit.ui.screens.ProfileScreen
import hu.rayworks.vizit.ui.screens.SettingsScreen
import hu.rayworks.vizit.ui.screens.ShareScreen

private enum class AppSection(val label: String) {
    HOME("Kezdőlap"),
    PROFILE("Névjegyem"),
    SHARE("Átadás"),
    SETTINGS("Beállítások"),
}

@Composable
fun VizitApp(viewModel: VizitViewModel, offlineMode: Boolean = false) {
    var selectedSection by rememberSaveable { mutableStateOf(AppSection.HOME) }

    if (viewModel.isNfcShareActive) {
        NfcShareScreen(
            profile = viewModel.profile,
            phase = viewModel.nfcSharePhase,
            photoIncluded = viewModel.nfcPhotoIncluded,
            onStop = viewModel::stopNfcShare,
        )
        return
    }

    Scaffold(
        topBar = {
            if (offlineMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        text = "Offline mód – a helyi profil használható, a szinkron később folytatódik.",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                AppSection.entries.forEach { section ->
                    NavigationBarItem(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        icon = {
                            Icon(
                                imageVector = when (section) {
                                    AppSection.HOME -> Icons.Outlined.Home
                                    AppSection.PROFILE -> Icons.Outlined.Person
                                    AppSection.SHARE -> Icons.Outlined.Share
                                    AppSection.SETTINGS -> Icons.Outlined.Settings
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text(section.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedSection) {
            AppSection.HOME -> HomeScreen(
                profile = viewModel.profile,
                nfcStatus = viewModel.nfcStatus,
                onStartNfcShare = viewModel::startNfcShare,
                onEditProfile = { selectedSection = AppSection.PROFILE },
                onShareAsText = viewModel::shareAsText,
                modifier = Modifier.padding(innerPadding),
            )

            AppSection.PROFILE -> ProfileScreen(
                profile = viewModel.profile,
                onSave = viewModel::saveProfile,
                modifier = Modifier.padding(innerPadding),
            )

            AppSection.SHARE -> ShareScreen(
                profile = viewModel.profile,
                nfcStatus = viewModel.nfcStatus,
                onStartNfcShare = viewModel::startNfcShare,
                modifier = Modifier.padding(innerPadding),
            )

            AppSection.SETTINGS -> SettingsScreen(
                nfcStatus = viewModel.nfcStatus,
                syncState = viewModel.profileSyncState,
                automaticSyncEnabled = viewModel.automaticSyncEnabled,
                onAutomaticSyncChanged = viewModel::setAutomaticSyncEnabled,
                onRetrySync = viewModel::retryProfileSync,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
