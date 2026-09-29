package hu.rayworks.vizit.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.qr.QrPayloadFactory
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitButtonStyle
import hu.rayworks.vizit.ui.design.components.VizitDivider
import hu.rayworks.vizit.ui.design.components.VizitGroup
import hu.rayworks.vizit.ui.design.components.VizitLargeTitle
import hu.rayworks.vizit.ui.design.components.VizitPanel
import hu.rayworks.vizit.ui.design.components.VizitRow
import hu.rayworks.vizit.ui.design.components.VizitSectionHeader
import hu.rayworks.vizit.ui.design.components.VizitStatusPill
import hu.rayworks.vizit.ui.design.components.VizitTone

/**
 * VIZIT 9 Profile destination.
 *
 * Deliberately contains no owner-card preview: the v9 concept makes the public
 * profile link and the three editing entry points the focus of this tab.
 */
@Composable
fun V9ProfileScreen(
    profile: ContactProfile,
    presentation: CardPresentation,
    synchronized: Boolean,
    onEdit: () -> Unit,
    onOpenCardAppearance: () -> Unit,
    onOpenDataVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = Vizit.colors
    val publicProfileUrl = QrPayloadFactory.profileUrl(profile, synchronized)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        Spacer(Modifier.height(Vizit.space.xs))
        VizitLargeTitle(title = "Profil")

        VizitPanel {
            Text("WEB PROFILOD", style = Vizit.type.overline, color = colors.textMuted)
            Text(
                publicProfileUrl ?: "A profil-link a sikeres szinkron után jelenik meg.",
                style = Vizit.type.body,
                color = if (publicProfileUrl != null) colors.textPrimary else colors.textMuted,
            )
            if (publicProfileUrl != null) {
                VizitButton(
                    text = "Profil megnyitása",
                    onClick = { openProfile(context, publicProfileUrl) },
                    icon = Icons.Outlined.Language,
                    style = VizitButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                VizitStatusPill(
                    text = if (profile.isPublic) "Szinkronizálásra vár" else "Nyilvános profil kikapcsolva",
                    tone = VizitTone.Info,
                )
            }
        }

        VizitGroup {
            VizitRow(
                label = "Profiljaid",
                supporting = "1 profil · a jelenlegi adatmodell egy aktív profilt kezel",
                icon = Icons.Outlined.Person,
                showChevron = false,
            )
        }

        VizitSectionHeader("Tartalom")
        VizitGroup {
            VizitRow(
                label = "Adatok",
                supporting = "Név, elérhetőségek, cég és közösségi profilok",
                icon = Icons.Outlined.Edit,
                onClick = onEdit,
            )
            VizitDivider()
            VizitRow(
                label = "Mi látszik a profilon",
                supporting = presentation.sharedFieldCount.toString() + "/" +
                    CardPresentation.OPTIONAL_FIELD_COUNT + " megosztható mező",
                icon = Icons.Outlined.Visibility,
                onClick = onOpenDataVisibility,
            )
            VizitDivider()
            VizitRow(
                label = "Megjelenés",
                supporting = presentation.colorway.label,
                icon = Icons.Outlined.Palette,
                onClick = onOpenCardAppearance,
            )
        }

        Spacer(Modifier.height(Vizit.space.xxl))
    }
}

private fun openProfile(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
