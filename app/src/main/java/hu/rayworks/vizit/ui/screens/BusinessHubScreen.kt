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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class BusinessResource(
    val title: String,
    val description: String,
    val url: String,
    val icon: ImageVector,
)

private val businessResources = listOf(
    BusinessResource(
        title = "VOSZ videók",
        description = "Vállalkozói hírek, interjúk és gyakorlati videók a VOSZ YouTube-csatornáján.",
        url = "https://youtube.com/@vosz.?si=k2EmMlI8Q5ttlPZC",
        icon = Icons.Outlined.PlayCircleOutline,
    ),
    BusinessResource(
        title = "VOSZ vállalkozói információk",
        description = "Érdekképviselet, tanácsadás, programok és aktuális vállalkozói hírek.",
        url = "https://www.vosz.hu/hu",
        icon = Icons.Outlined.BusinessCenter,
    ),
    BusinessResource(
        title = "VOSZPort",
        description = "Digitális ügyintézési és tudásmegosztási felület vállalkozásoknak.",
        url = "https://voszport.com/",
        icon = Icons.Outlined.Language,
    ),
)

@Composable
fun BusinessHubScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Vállalkozói tudástár",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Hasznos külső források hírekhez, fejlődéshez és ügyintézéshez.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(businessResources) { resource ->
            Card(
                onClick = { runCatching { uriHandler.openUri(resource.url) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = resource.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(resource.title, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = resource.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = "Megnyitás böngészőben",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Text(
                text = "A hivatkozások külső oldalakra vezetnek. A VIZIT nem áll kapcsolatban ezek tartalmának üzemeltetésével.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}
