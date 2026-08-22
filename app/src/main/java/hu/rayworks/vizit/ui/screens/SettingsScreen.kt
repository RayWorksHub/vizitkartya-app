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
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.NfcStatus
import hu.rayworks.vizit.data.sync.ProfileSyncState
import hu.rayworks.vizit.data.sync.ProfileSyncStatus

@Composable
fun SettingsScreen(
    nfcStatus: NfcStatus,
    syncState: ProfileSyncState,
    automaticSyncEnabled: Boolean,
    onAutomaticSyncChanged: (Boolean) -> Unit,
    onRetrySync: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                description = "Előkészítve – a hitelesítési beállításokra vár.",
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
