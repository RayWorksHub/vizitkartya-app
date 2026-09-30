package hu.rayworks.vizit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.data.remote.AccountProfile
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitBanner
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitButtonStyle
import hu.rayworks.vizit.ui.design.components.VizitTone

@Composable
fun ProfileSwitcherBar(
    profiles: List<AccountProfile>,
    multiProfileEnabled: Boolean,
    busy: Boolean,
    message: String?,
    onSwitch: (String) -> Unit,
    onCreate: () -> String?,
    onDeleteActive: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!multiProfileEnabled && profiles.size <= 1) return
    val active = profiles.firstOrNull { it.isDefault } ?: profiles.firstOrNull()
    var pickerOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Vizit.space.xs)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !busy, role = Role.Button) { pickerOpen = true },
            shape = RoundedCornerShape(16.dp),
            color = Vizit.colors.surface,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Vizit.space.md, vertical = Vizit.space.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
            ) {
                Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = Vizit.colors.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(active?.displayName ?: "Profil kiválasztása", style = Vizit.type.label, color = Vizit.colors.textPrimary)
                    Text(
                        if (profiles.size > 1) "${profiles.size} profil · váltás" else "Új profil hozzáadása",
                        style = Vizit.type.caption,
                        color = Vizit.colors.textMuted,
                    )
                }
                if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Profilok megnyitása")
            }
        }
        message?.let {
            VizitBanner(text = it, tone = VizitTone.Info, modifier = Modifier.clickable { onDismissMessage() })
        }
    }

    if (pickerOpen) {
        AlertDialog(
            onDismissRequest = { if (!busy) pickerOpen = false },
            title = { Text("Profilok") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Vizit.space.xs),
                ) {
                    profiles.forEach { profile ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !busy && !profile.isDefault, role = Role.Button) {
                                    pickerOpen = false
                                    onSwitch(profile.id)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (profile.isDefault) Vizit.colors.primarySubtle else Vizit.colors.surface,
                        ) {
                            Row(
                                modifier = Modifier.padding(Vizit.space.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                            ) {
                                Icon(
                                    if (profile.isDefault) Icons.Outlined.Check else Icons.Outlined.PersonOutline,
                                    contentDescription = null,
                                    tint = Vizit.colors.primary,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(profile.displayName, style = Vizit.type.label)
                                    Text("/${profile.slug}", style = Vizit.type.caption, color = Vizit.colors.textMuted)
                                }
                                if (profile.isDefault) {
                                    Icon(Icons.Outlined.StarOutline, contentDescription = "Aktív és alapértelmezett")
                                }
                            }
                        }
                    }
                    if (localError != null) VizitBanner(text = localError.orEmpty(), tone = VizitTone.Error)
                    if (multiProfileEnabled) {
                        VizitButton(
                            text = "Új profil",
                            onClick = {
                                val issue = onCreate()
                                if (issue == null) pickerOpen = false else localError = issue
                            },
                            style = VizitButtonStyle.Secondary,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        )
                    }
                    if (active != null) {
                        VizitButton(
                            text = "Aktív profil törlése",
                            onClick = { pickerOpen = false; confirmDelete = true },
                            style = VizitButtonStyle.Tertiary,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickerOpen = false }, enabled = !busy) { Text("Kész") } },
        )
    }

    if (confirmDelete && active != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text("Profil törlése?") },
            text = { Text("A(z) „${active.displayName}” profil végleg törlődik. Ha van másik profil, az automatikusan aktívvá válik.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeleteActive() }) { Text("Törlés") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Mégse") } },
        )
    }
}
