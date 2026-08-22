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
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.NfcStatus
import hu.rayworks.vizit.data.remote.BackendConfiguration
import hu.rayworks.vizit.data.remote.BackendConfigurationStatus
import hu.rayworks.vizit.ui.components.VizitBrandLockup

@Composable
fun SettingsScreen(
    nfcStatus: NfcStatus,
    modifier: Modifier = Modifier,
) {
    val backendConfiguration = BackendConfiguration.fromBuildConfig()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            VizitBrandLockup(modifier = Modifier.fillMaxWidth())
        }

        item {
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
            val backendReady = backendConfiguration.status == BackendConfigurationStatus.READY
            SettingsCard(
                icon = if (backendReady) Icons.Outlined.CloudQueue else Icons.Outlined.CloudOff,
                title = "Felhőszinkron",
                description = when (backendConfiguration.status) {
                    BackendConfigurationStatus.READY -> "A ${BuildConfig.ENVIRONMENT} Supabase-környezet be van állítva."
                    BackendConfigurationStatus.MISSING ->
                        "Az integráció előkészítve – a környezeti konfiguráció aktiválására vár."

                    BackendConfigurationStatus.INVALID ->
                        "A környezeti konfiguráció hibás; a szinkron biztonsági okból le van tiltva."
                },
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
            SettingsCard(
                icon = Icons.Outlined.Person,
                title = "Google-belépés",
                description = if (backendConfiguration.googleAuthEnabled) {
                    "A Google-belépés engedélyezve van ebben a buildben."
                } else {
                    "Előkészítve – a hitelesítési beállításokra vár."
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
            Text(
                text = "VIZIT ${BuildConfig.VERSION_NAME} · ${BuildConfig.ENVIRONMENT}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
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
