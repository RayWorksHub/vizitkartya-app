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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitDivider
import hu.rayworks.vizit.ui.design.components.VizitGroup
import hu.rayworks.vizit.ui.design.components.VizitIconButton
import hu.rayworks.vizit.ui.design.components.VizitRow

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
fun BusinessHubScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Vizit.colors
    val uriHandler = LocalUriHandler.current
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VizitIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Vissza",
                onClick = onBack,
            )
            Text(
                text = "Tudástár",
                style = Vizit.type.h3,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = Vizit.space.xs),
            )
        }

        Text(
            text = "Hasznos külső források hírekhez, fejlődéshez és ügyintézéshez.",
            style = Vizit.type.body,
            color = colors.textSecondary,
        )

        VizitGroup {
            businessResources.forEachIndexed { index, resource ->
                if (index > 0) VizitDivider()
                VizitRow(
                    label = resource.title,
                    supporting = resource.description,
                    icon = resource.icon,
                    onClick = { runCatching { uriHandler.openUri(resource.url) } },
                )
            }
        }

        Text(
            text = "A hivatkozások külső oldalakra vezetnek. A VIZIT nem áll kapcsolatban ezek tartalmának üzemeltetésével.",
            style = Vizit.type.bodySmall,
            color = colors.textMuted,
            modifier = Modifier.padding(vertical = Vizit.space.xs),
        )
    }
}
