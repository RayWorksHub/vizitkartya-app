package hu.rayworks.vizit.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.card.CardColorway
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.Pic
import hu.rayworks.vizit.v10.data.Profile
import hu.rayworks.vizit.v10.ui.theme.ThemeState
import hu.rayworks.vizit.v10.ui.wizard.ThemedWizardHost as WizardHost
import hu.rayworks.vizit.v10.ui.wizard.WizardState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** The supplied native block pager, attached to the existing owner-isolated save path. */
@Composable
fun ProfileWizardScreen(
    onSave: suspend (ContactProfile) -> String?,
    presentation: CardPresentation,
    onAppearanceChange: (CardPresentation) -> Unit,
    onDone: () -> Unit,
    isAdditional: Boolean = false,
    onCancel: (() -> Unit)? = null,
) {
    val bridge = remember { AppState(WizardState(takenSlugs = { emptyList() })) }
    val scope = rememberCoroutineScope()
    val save by rememberUpdatedState(onSave)
    val done by rememberUpdatedState(onDone)
    val cancel by rememberUpdatedState(onCancel)
    val appearance by rememberUpdatedState(onAppearanceChange)
    val currentPresentation by rememberUpdatedState(presentation)
    val dark = Vizit.colors.canvas.luminance() < 0.5f
    SideEffect {
        ThemeState.dark = dark
        bridge.onExit = {
            if (cancel != null) cancel?.invoke()
            else { bridge.wizard.confirmOpen = false; bridge.wizard.introShown = true }
        }
        bridge.onPublish = { value ->
            if (!bridge.saving) {
                bridge.saving = true
                bridge.error = null
                scope.launch {
                    try {
                        val draft = withContext(Dispatchers.Default) { value.productionProfile() }
                        val issue = save(draft)
                        if (issue == null) {
                            val colorway = when (value.presetId) {
                                "markakek" -> CardColorway.BRAND
                                "smaragd" -> CardColorway.EMERALD
                                "ametiszt" -> CardColorway.AMETHYST
                                else -> CardColorway.INK
                            }
                            appearance(currentPresentation.copy(colorway = colorway))
                            // The repository owns the actual synchronization status.
                            // Never report a simulated or timer-based publication.
                            done()
                        } else bridge.error = issue
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        bridge.error = error.localizedMessage ?: "A névjegy mentése nem sikerült."
                    } finally { bridge.saving = false }
                }
            }
        }
    }
    BackHandler { bridge.closeWizard(false) }
    Box(Modifier.fillMaxSize()) {
        WizardHost(bridge)
        if (bridge.saving) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)).clickable {},
                contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
    bridge.error?.let { message ->
        AlertDialog(
            onDismissRequest = { bridge.error = null },
            title = { Text("A névjegy mentése nem sikerült") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { bridge.error = null }) { Text("Rendben") } },
        )
    }
}

private fun Profile.productionProfile(): ContactProfile = ContactProfile(
    fullName = name, company = company, jobTitle = title, phone = phone, email = email,
    address = address, bio = bio, website = checkedUrl(web), publicSlug = slug, isPublic = isPublic,
    photoBase64 = photo.jpeg(), logoBase64 = logo.jpeg(),
    linkedIn = checkedUrl(socials["linkedin"].orEmpty()),
    facebook = checkedUrl(socials["facebook"].orEmpty()),
    instagram = checkedUrl(socials["instagram"].orEmpty()),
    youtube = checkedUrl(socials["youtube"].orEmpty()),
    tiktok = checkedUrl(socials["tiktok"].orEmpty()),
    x = checkedUrl(socials["x"].orEmpty()),
    github = checkedUrl(socials["github"].orEmpty()),
    customSocial = checkedUrl(socials["other"].orEmpty()),
)

private fun checkedUrl(value: String): String {
    val v = value.trim()
    if (v.isEmpty()) return ""
    val uri = Uri.parse(v)
    require(uri.scheme == "https" && !uri.host.isNullOrBlank() && v.none(Char::isWhitespace)) {
        "Érvényes, https:// kezdetű webcímet adj meg."
    }
    return v
}

private fun Pic?.jpeg(): String {
    if (this == null) return ""
    require(this is Pic.Bmp) { "Válassz képet a készülékedről." }
    val source = bitmap.asAndroidBitmap()
    for (side in listOf(512, 384, 256)) {
        val scale = minOf(1f, side.toFloat() / max(source.width, source.height))
        val resized = Bitmap.createScaledBitmap(source,
            max(1, (source.width * scale).roundToInt()), max(1, (source.height * scale).roundToInt()), true)
        try {
            for (quality in listOf(82, 65, 45)) {
                val output = ByteArrayOutputStream()
                check(resized.compress(Bitmap.CompressFormat.JPEG, quality, output))
                if (output.size() <= 256 * 1024) return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            }
        } finally { if (resized !== source) resized.recycle() }
    }
    error("A kiválasztott kép túl nagy. Válassz másik képet.")
}
