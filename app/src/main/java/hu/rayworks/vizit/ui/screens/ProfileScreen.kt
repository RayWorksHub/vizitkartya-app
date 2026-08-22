package hu.rayworks.vizit.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.PhotoProcessor
import hu.rayworks.vizit.ui.components.ProfileAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    profile: ContactProfile,
    onSave: suspend (ContactProfile) -> String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var draft by remember(profile) { mutableStateOf(profile) }
    var isPhotoLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isPhotoLoading = true
                runCatching {
                    withContext(Dispatchers.IO) {
                        PhotoProcessor.loadSquareJpegBase64(context, uri)
                    }
                }.onSuccess { photo ->
                    draft = draft.copy(photoBase64 = photo)
                }.onFailure {
                    snackbarHostState.showSnackbar("A kiválasztott kép nem dolgozható fel.")
                }
                isPhotoLoading = false
            }
        }
    }

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Saját névjegy",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Ezek az adatok kerülnek át a másik telefon Kontaktok alkalmazásába.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProfileAvatar(
                        photoBase64 = draft.photoBase64,
                        initials = draft.initials,
                        size = 92.dp,
                    )
                    Column(
                        modifier = Modifier.padding(start = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            enabled = !isPhotoLoading,
                        ) {
                            Icon(Icons.Outlined.AddAPhoto, contentDescription = null)
                            Text(
                                if (isPhotoLoading) "Feldolgozás…" else "Kép kiválasztása",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        if (draft.photoBase64.isNotBlank()) {
                            OutlinedButton(onClick = { draft = draft.copy(photoBase64 = "") }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                                Text("Kép törlése", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }

            item {
                ProfileTextField(
                    value = draft.fullName,
                    onValueChange = { draft = draft.copy(fullName = it) },
                    label = "Teljes név",
                )
            }
            item {
                ProfileTextField(
                    value = draft.jobTitle,
                    onValueChange = { draft = draft.copy(jobTitle = it) },
                    label = "Beosztás / foglalkozás",
                )
            }
            item {
                ProfileTextField(
                    value = draft.company,
                    onValueChange = { draft = draft.copy(company = it) },
                    label = "Cég / szervezet",
                )
            }
            item {
                ProfileTextField(
                    value = draft.phone,
                    onValueChange = { draft = draft.copy(phone = it) },
                    label = "Telefonszám",
                    keyboardType = KeyboardType.Phone,
                )
            }
            item {
                ProfileTextField(
                    value = draft.email,
                    onValueChange = { draft = draft.copy(email = it) },
                    label = "E-mail-cím",
                    keyboardType = KeyboardType.Email,
                )
            }
            item {
                ProfileTextField(
                    value = draft.website,
                    onValueChange = { draft = draft.copy(website = it) },
                    label = "Weboldal",
                    keyboardType = KeyboardType.Uri,
                )
            }
            item {
                ProfileTextField(
                    value = draft.address,
                    onValueChange = { draft = draft.copy(address = it) },
                    label = "Cím",
                )
            }
            item {
                ProfileTextField(
                    value = draft.linkedIn,
                    onValueChange = { draft = draft.copy(linkedIn = it) },
                    label = "LinkedIn-profil",
                    keyboardType = KeyboardType.Uri,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Publikus VIZIT profil", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "A Profil QR csak az itt engedélyezett publikus profilhoz használható.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = draft.isPublic,
                        onCheckedChange = { draft = draft.copy(isPublic = it) },
                    )
                }
            }

            if (draft.isPublic) {
                item {
                    ProfileTextField(
                        value = draft.publicSlug,
                        onValueChange = { draft = draft.copy(publicSlug = it.lowercase()) },
                        label = "Profilazonosító (például: csukardi-rajmund)",
                        keyboardType = KeyboardType.Uri,
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            val message = runCatching { onSave(draft) }
                                .fold(
                                    onSuccess = { it ?: "A névjegy helyben mentve." },
                                    onFailure = { "A helyi mentés nem sikerült. Próbáld újra." },
                                )
                            snackbarHostState.showSnackbar(message)
                            isSaving = false
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        if (isSaving) "Mentés…" else "Névjegy mentése",
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
    )
}
