package hu.rayworks.vizit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitLargeTitle
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitButtonStyle
import hu.rayworks.vizit.ui.design.components.VizitDigitalCard
import hu.rayworks.vizit.ui.design.components.VizitDivider
import hu.rayworks.vizit.ui.design.components.VizitEmptyState
import hu.rayworks.vizit.ui.design.components.VizitGroup
import hu.rayworks.vizit.ui.design.components.VizitRow
import hu.rayworks.vizit.ui.design.components.VizitSectionHeader
import hu.rayworks.vizit.ui.util.rememberProfilePhoto

/**
 * The card tab: the card itself, what is on it, and the two things you can do
 * with it. Editing happens on a dedicated screen so this stays a clean preview.
 */
@Composable
fun CardScreen(
    profile: ContactProfile,
    onSave: suspend (ContactProfile) -> String?,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val photo = rememberProfilePhoto(profile.photoBase64)

    if (editing) {
        ProfileEditScreen(
            profile = profile,
            onSave = onSave,
            onClose = { editing = false },
            modifier = modifier,
        )
        return
    }

    val hasDetails = listOf(profile.phone, profile.email, profile.website, profile.address)
        .any { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Vizit.colors.canvas)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.lg),
    ) {
        Spacer(Modifier.height(Vizit.space.xs))
        VizitLargeTitle(title = "Névjegyem")

        Text("Névjegyem", style = Vizit.type.h1, color = Vizit.colors.textPrimary)

        VizitDigitalCard(
            fullName = profile.resolvedDisplayName,
            initials = profile.initials,
            jobTitle = profile.jobTitle,
            company = profile.company,
            phone = profile.phone,
            email = profile.email,
            photo = photo,
        )

        VizitButton(
            text = "Névjegy szerkesztése",
            onClick = { editing = true },
            icon = Icons.Outlined.Edit,
            style = VizitButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        VizitButton(
            text = "Megosztás",
            onClick = onShare,
            icon = Icons.Outlined.Share,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!hasDetails) {
            VizitEmptyState(
                icon = Icons.Outlined.Edit,
                title = "Még üres a névjegyed",
                message = "Add meg az elérhetőségeidet, hogy legyen mit átadni egy érintéssel.",
                actionLabel = "Adatok megadása",
                onAction = { editing = true },
            )
        } else {
            VizitSectionHeader("Elérhetőségek")
            VizitGroup {
                var first = true
                if (profile.phone.isNotBlank()) {
                    VizitRow(label = profile.phone, supporting = "Telefon", icon = Icons.Outlined.Phone, showChevron = false)
                    first = false
                }
                if (profile.email.isNotBlank()) {
                    if (!first) VizitDivider()
                    VizitRow(label = profile.email, supporting = "E-mail", icon = Icons.Outlined.Mail, showChevron = false)
                    first = false
                }
                if (profile.website.isNotBlank()) {
                    if (!first) VizitDivider()
                    VizitRow(label = profile.website, supporting = "Weboldal", icon = Icons.Outlined.Language, showChevron = false)
                    first = false
                }
                if (profile.address.isNotBlank()) {
                    if (!first) VizitDivider()
                    VizitRow(label = profile.address, supporting = "Cím", icon = Icons.Outlined.LocationOn, showChevron = false)
                }
            }

            if (profile.company.isNotBlank() || profile.jobTitle.isNotBlank()) {
                VizitSectionHeader("Munkahely")
                VizitGroup {
                    VizitRow(
                        label = profile.company.ifBlank { profile.jobTitle },
                        supporting = if (profile.company.isNotBlank()) profile.jobTitle.ifBlank { null } else null,
                        icon = Icons.Outlined.Business,
                        showChevron = false,
                    )
                }
            }
        }

        Spacer(Modifier.height(Vizit.space.xl))
    }
}
