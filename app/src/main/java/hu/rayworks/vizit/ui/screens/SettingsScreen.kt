package hu.rayworks.vizit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.NfcStatus
import hu.rayworks.vizit.auth.AuthActionState
import hu.rayworks.vizit.auth.AuthOperation
import hu.rayworks.vizit.auth.AuthValidator
import hu.rayworks.vizit.data.sync.ProfileSyncState
import hu.rayworks.vizit.data.sync.ProfileSyncStatus

@Composable
fun SettingsScreen(
    nfcStatus: NfcStatus,
    syncState: ProfileSyncState,
    automaticSyncEnabled: Boolean,
    onAutomaticSyncChanged: (Boolean) -> Unit,
    onRetrySync: () -> Unit,
    authActionState: AuthActionState,
    googleSignInEnabled: Boolean,
    cloudAccountAvailable: Boolean,
    onClearAuthAction: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var deleteConfirmation by rememberSaveable { mutableStateOf("") }
    val authLoading = authActionState is AuthActionState.Loading

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Beállítások",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "A VIZIT alkalmazás állapota és biztonsági információi.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            SettingsCard(
                icon = Icons.Outlined.Nfc,
                title = "NFC",
                description = if (nfcStatus.isReady) {
                    "A közvetlen kontaktátadás használatra kész."
                } else {
                    "Az NFC vagy a kártyaemuláció jelenleg nem érhető el."
                },
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Sync, contentDescription = null)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp),
                        ) {
                            Text("Profil szinkron", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = syncState.description(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Switch(
                            checked = automaticSyncEnabled,
                            onCheckedChange = onAutomaticSyncChanged,
                        )
                    }
                    if (syncState.status == ProfileSyncStatus.RETRY_SCHEDULED) {
                        Button(onClick = onRetrySync, modifier = Modifier.fillMaxWidth()) {
                            Text("Szinkron újrapróbálása")
                        }
                    }
                }
            }
        }
        item {
            SettingsCard(
                icon = Icons.Outlined.Person,
                title = "Google-belépés",
                description = if (googleSignInEnabled) {
                    "A Google-belépés ebben a DEV buildben elérhető."
                } else {
                    "Előkészítve – a hitelesítési vagy jogi beállításokra vár."
                },
            )
        }
        item {
            SettingsCard(
                icon = Icons.Outlined.Lock,
                title = "Adatvédelem",
                description = "A névjegy NFC-példánya csak az aktív küldés idején létezik.",
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Logout, contentDescription = null)
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text("Fiók", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Kijelentkezés vagy a VIZIT-fiók végleges törlése.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (cloudAccountAvailable) {
                        OutlinedButton(
                            onClick = {
                                onClearAuthAction()
                                onLogout()
                            },
                            enabled = !authLoading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Kijelentkezés")
                        }
                        Button(
                            onClick = {
                                onClearAuthAction()
                                deleteConfirmation = ""
                                showDeleteConfirmation = true
                            },
                            enabled = !authLoading,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text("Fiók végleges törlése")
                        }
                    } else {
                        Text(
                            text = "A helyi DEV tesztprofilhoz nem tartozik felhőfiók.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AuthAccountStatus(authActionState)
                }
            }
        }

        item {
            Text(
                text = "VIZIT ${BuildConfig.VERSION_NAME}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
            title = { Text("Biztosan törlöd a fiókodat?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "A művelet végleges. Törlődik az Auth-fiók, a felhőprofil, a média és az ezen az eszközön tárolt profiladat.",
                    )
                    Text("Megerősítésként írd be: ${AuthValidator.ACCOUNT_DELETION_PHRASE}")
                    OutlinedTextField(
                        value = deleteConfirmation,
                        onValueChange = {
                            deleteConfirmation = it
                            if (authActionState is AuthActionState.Error) onClearAuthAction()
                        },
                        label = { Text("Megerősítés") },
                        singleLine = true,
                        enabled = !authLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val deletionError = authActionState as? AuthActionState.Error
                    if (deletionError?.operation == AuthOperation.DELETE_ACCOUNT) {
                        Text(deletionError.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onDeleteAccount(deleteConfirmation) },
                    enabled = !authLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(if (authLoading) "Törlés folyamatban…" else "Végleges törlés")
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
                    Text("Mégsem")
                }
            },
        )
    }
}

@Composable
private fun AuthAccountStatus(state: AuthActionState) {
    when (state) {
        is AuthActionState.Error -> if (
            state.operation == AuthOperation.LOGOUT || state.operation == AuthOperation.DELETE_ACCOUNT
        ) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }

        is AuthActionState.Success -> if (
            state.operation == AuthOperation.LOGOUT || state.operation == AuthOperation.DELETE_ACCOUNT
        ) {
            Text(state.message, color = MaterialTheme.colorScheme.primary)
        }

        else -> Unit
    }
}

private fun ProfileSyncState.description(): String = when (status) {
    ProfileSyncStatus.LOCAL_ONLY -> "Csak ezen az eszközön tárolva."
    ProfileSyncStatus.SYNCED -> "A helyi és a felhőprofil szinkronban van."
    ProfileSyncStatus.PENDING -> "A módosítás helyben mentve, felhőszinkronra vár."
    ProfileSyncStatus.SYNCING -> "Felhőszinkron folyamatban."
    ProfileSyncStatus.RETRY_SCHEDULED -> lastError
        ?: "A helyi adat biztonságban van; a szinkron automatikusan újrapróbálkozik."
    ProfileSyncStatus.CONFLICT -> "A helyi adat megmaradt; az automatikus felülírás konfliktus miatt leállt."
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null)
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
