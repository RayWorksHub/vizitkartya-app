package hu.rayworks.vizit.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.PhotoProcessor
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitBrandHeader
import hu.rayworks.vizit.ui.design.components.VizitBrandHeaderStyle
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitButtonStyle
import hu.rayworks.vizit.ui.design.components.VizitDivider
import hu.rayworks.vizit.ui.design.components.VizitGroup
import hu.rayworks.vizit.ui.design.components.VizitIconButton
import hu.rayworks.vizit.ui.design.components.VizitRow
import hu.rayworks.vizit.ui.design.components.VizitSectionHeader
import hu.rayworks.vizit.ui.design.components.VizitTextField
import hu.rayworks.vizit.ui.util.rememberProfilePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Profile editing, grouped into the six things a person actually thinks about
 * instead of one unbroken column of inputs.
 */
@Composable
fun ProfileEditScreen(
    profile: ContactProfile,
    onSave: suspend (ContactProfile) -> String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Vizit.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var draft by remember(profile) { mutableStateOf(profile) }
    var photoLoading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val photo = rememberProfilePhoto(draft.photoBase64)

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                photoLoading = true
                runCatching {
                    withContext(Dispatchers.IO) { PhotoProcessor.loadSquareJpegBase64(context, uri) }
                }.onSuccess { draft = draft.copy(photoBase64 = it) }
                    .onFailure { snackbarHostState.showSnackbar("A kiválasztott kép nem dolgozható fel.") }
                photoLoading = false
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.canvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
                .padding(horizontal = Vizit.space.md),
            verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
        ) {
            VizitBrandHeader(
                style = VizitBrandHeaderStyle.Compact,
                onBack = onClose,
            )

            Text(
                text = "Névjegy szerkesztése",
                style = Vizit.type.h2,
                color = colors.textPrimary,
            )

            // --- Photo
            VizitSectionHeader("Profilkép")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Vizit.space.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(colors.primarySubtle),
                    contentAlignment = Alignment.Center,
                ) {
                    if (photo != null) {
                        Image(
                            bitmap = photo,
                            contentDescription = "Profilkép",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    } else {
                        Text(
                            text = draft.initials.ifBlank { "V" },
                            style = Vizit.type.h2,
                            color = colors.primary,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(Vizit.space.xs)) {
                    VizitButton(
                        text = if (photoLoading) "Feldolgozás…" else "Kép kiválasztása",
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        icon = Icons.Outlined.AddAPhoto,
                        style = VizitButtonStyle.Secondary,
                        enabled = !photoLoading,
                        loading = photoLoading,
                    )
                    if (draft.photoBase64.isNotBlank()) {
                        VizitButton(
                            text = "Kép törlése",
                            onClick = { draft = draft.copy(photoBase64 = "") },
                            icon = Icons.Outlined.DeleteOutline,
                            style = VizitButtonStyle.Tertiary,
                        )
                    }
                }
            }

            // --- Personal
            VizitSectionHeader("Személyes adatok")
            VizitTextField(
                value = draft.fullName,
                onValueChange = { draft = draft.copy(fullName = it) },
                label = "Teljes név",
                placeholder = "Add meg a neved",
            )
            VizitTextField(
                value = draft.jobTitle,
                onValueChange = { draft = draft.copy(jobTitle = it) },
                label = "Beosztás / foglalkozás",
            )

            // --- Work
            VizitSectionHeader("Munkahely")
            VizitTextField(
                value = draft.company,
                onValueChange = { draft = draft.copy(company = it) },
                label = "Cég / szervezet",
            )
            VizitTextField(
                value = draft.address,
                onValueChange = { draft = draft.copy(address = it) },
                label = "Cím",
            )

            // --- Contact
            VizitSectionHeader("Elérhetőségek")
            VizitTextField(
                value = draft.phone,
                onValueChange = { draft = draft.copy(phone = it) },
                label = "Telefonszám",
                keyboardType = KeyboardType.Phone,
            )
            VizitTextField(
                value = draft.email,
                onValueChange = { draft = draft.copy(email = it) },
                label = "E-mail-cím",
                keyboardType = KeyboardType.Email,
            )
            VizitTextField(
                value = draft.website,
                onValueChange = { draft = draft.copy(website = it) },
                label = "Weboldal",
                keyboardType = KeyboardType.Uri,
            )

            // --- Social
            VizitSectionHeader("Közösségi profilok")
            Text(
                text = "Teljes, https:// kezdetű hivatkozásokat adj meg.",
                style = Vizit.type.bodySmall,
                color = colors.textMuted,
            )
            VizitTextField(
                value = draft.linkedIn,
                onValueChange = { draft = draft.copy(linkedIn = it) },
                label = "LinkedIn",
                keyboardType = KeyboardType.Uri,
            )
            VizitTextField(
                value = draft.facebook,
                onValueChange = { draft = draft.copy(facebook = it) },
                label = "Facebook",
                keyboardType = KeyboardType.Uri,
            )
            VizitTextField(
                value = draft.instagram,
                onValueChange = { draft = draft.copy(instagram = it) },
                label = "Instagram",
                keyboardType = KeyboardType.Uri,
            )
            VizitTextField(
                value = draft.tiktok,
                onValueChange = { draft = draft.copy(tiktok = it) },
                label = "TikTok",
                keyboardType = KeyboardType.Uri,
            )
            VizitTextField(
                value = draft.youtube,
                onValueChange = { draft = draft.copy(youtube = it) },
                label = "YouTube",
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            )

            // --- Sharing
            VizitSectionHeader("Megosztási adatok")
            VizitGroup {
                VizitRow(
                    label = "Publikus VIZIT profil",
                    supporting = "A Profil QR csak engedélyezett publikus profillal működik.",
                    checked = draft.isPublic,
                    onCheckedChange = { draft = draft.copy(isPublic = it) },
                )
                if (draft.isPublic) {
                    VizitDivider()
                    Column(
                        modifier = Modifier.padding(Vizit.space.md),
                        verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                    ) {
                        Text(
                            text = "Automatikus profilcím",
                            style = Vizit.type.label,
                            color = colors.textSecondary,
                        )
                        Text(
                            text = draft.publicSlug.takeIf(String::isNotBlank)
                                ?.let { "e-nevjegy.vercel.app/p/$it" }
                                ?: "Az egyedi azonosítót az első mentéskor a nevedből hozzuk létre.",
                            style = Vizit.type.bodySmall,
                            color = colors.textMuted,
                        )
                        VizitTextField(
                            value = draft.customDomain,
                            onValueChange = {
                                draft = draft.copy(customDomain = it, customDomainVerified = false)
                            },
                            label = "Egyedi domain (opcionális)",
                            placeholder = "nevjegy.cegem.hu",
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        )
                        if (draft.customDomain.isNotBlank()) {
                            Text(
                                text = if (draft.customDomainVerified) {
                                    "Ellenőrzött domain · ezt használja a QR és az NFC."
                                } else {
                                    "Beállításra vár · addig a biztos VIZIT-cím marad aktív."
                                },
                                style = Vizit.type.bodySmall,
                                color = if (draft.customDomainVerified) colors.success else colors.warning,
                            )
                        }
                    }
                }
            }

            VizitButton(
                text = if (saving) "Mentés…" else "Névjegy mentése",
                onClick = {
                    scope.launch {
                        saving = true
                        val message = runCatching { onSave(draft) }.fold(
                            onSuccess = { it ?: "A névjegy mentve." },
                            onFailure = { "A mentés nem sikerült. Próbáld újra." },
                        )
                        snackbarHostState.showSnackbar(message)
                        saving = false
                    }
                },
                enabled = !saving,
                loading = saving,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Vizit.space.xxl))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(Vizit.space.md),
        )
    }
}
