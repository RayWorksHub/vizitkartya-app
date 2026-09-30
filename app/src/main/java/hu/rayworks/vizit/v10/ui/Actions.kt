package hu.rayworks.vizit.v10.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.runtime.staticCompositionLocalOf
import hu.rayworks.vizit.R
import hu.rayworks.vizit.qr.QrCodeGenerator
import hu.rayworks.vizit.qr.QrShareHelper
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.Profile
import hu.rayworks.vizit.v10.data.toContactProfile

/**
 * A valódi Android-műveletek egy helyen: megosztó lap, böngésző, vágólap, névjegy mentése.
 * Az Android rendszerfunkciók és a valódi QR-/vCard-műveletek egy helyen.
 */
class Actions(private val context: Context, private val app: AppState) {

    private fun start(intent: Intent, fallback: String) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            app.toast(fallback)
        }
    }

    /** Kizárólag a külön prototípus-módban használt visszajelzés. */
    fun demo(text: String) = app.toast("Prototípus · $text")

    fun openUrl(url: String) = start(Intent(Intent.ACTION_VIEW, Uri.parse(url)), url)

    private fun linkOf(p: Profile): String? = when {
        app.profileQrAvailable(p) -> app.publicUrl(p)
        else -> null
    }

    /** Link megosztása a rendszer megosztó lapján. */
    fun shareLink(p: Profile) {
        val url = linkOf(p)
        if (url == null) {
            app.toast(if (!p.isPublic) "Nincs még publikus profil" else "Előbb várd meg a szinkront")
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            putExtra(Intent.EXTRA_TITLE, p.name + " · " + p.label)
        }
        start(Intent.createChooser(send, null), url)
    }

    /** Névjegy szövegként (éles „Egyéb” megosztás). */
    fun shareText(p: Profile) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, p.name + " – VIZIT")
            putExtra(Intent.EXTRA_TEXT, app.shareText(p))
        }
        start(Intent.createChooser(send, "Névjegy megosztása"), "Névjegy megosztása")
    }

    fun saveQr(payload: String, withLogo: Boolean) {
        runCatching { createQr(payload, withLogo) }
            .map { QrShareHelper.saveToPictures(context, it) }
            .onSuccess { saved ->
                app.toast(if (saved) "A QR-kód a Képek/VIZIT mappába került." else "A QR-kód nem menthető.")
            }
            .onFailure { app.toast(it.localizedMessage ?: "A QR-kód nem menthető.") }
    }

    fun shareQr(payload: String, withLogo: Boolean) {
        runCatching { QrShareHelper.share(context, createQr(payload, withLogo)) }
            .onFailure { app.toast(it.localizedMessage ?: "A QR-kód nem osztható meg.") }
    }

    fun shareContact(p: Profile) {
        runCatching {
            QrShareHelper.shareContact(
                context = context,
                profile = p.toContactProfile(context),
                profileUrl = linkOf(p),
            )
        }.onFailure { app.toast(it.localizedMessage ?: "A névjegyfájl nem osztható meg.") }
    }

    private fun createQr(payload: String, withLogo: Boolean) = QrCodeGenerator.create(
        payload = payload,
        logo = if (withLogo) BitmapFactory.decodeResource(context.resources, R.drawable.vizit_logo_mark) else null,
    )

    fun copyLink(p: Profile) {
        val url = linkOf(p)
        if (url == null) {
            app.toast("Nincs még publikus profil")
            return
        }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("VIZIT profil", url))
        app.toast("A profil-link a vágólapra került.")
    }

    fun openProfile(p: Profile) {
        when {
            p.real -> openUrl(app.publicUrl(p))
            p.fresh -> app.toast("Prototípus · élesben itt nyílna meg: " + p.short)
            else -> app.toast("Minta profil, még nincs valódi oldala")
        }
    }

    /** Beolvasott névjegy mentése: a rendszer új névjegy szerkesztője nyílik, kitöltve. */
    fun insertContact(name: String, company: String, title: String, phone: String, email: String) {
        val i = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, name)
            putExtra(ContactsContract.Intents.Insert.COMPANY, company)
            putExtra(ContactsContract.Intents.Insert.JOB_TITLE, title)
            putExtra(ContactsContract.Intents.Insert.PHONE, phone)
            putExtra(ContactsContract.Intents.Insert.EMAIL, email)
        }
        start(i, "A névjegy nem menthető")
    }

    /** Az adatexport fájl írása a felhasználó által választott helyre. */
    fun writeExport(uri: Uri) {
        if (app.productionMode) {
            app.exportAccount(uri)
            return
        }
        try {
            context.contentResolver.openOutputStream(uri, "w")?.use { it.write(app.exportJson().toByteArray(Charsets.UTF_8)) }
                ?: error("A kiválasztott fájl nem írható.")
            app.toast("Az adataidat sikeresen exportáltuk.")
        } catch (e: Exception) {
            app.toast(e.localizedMessage ?: "Az exportálás nem sikerült.")
        }
    }
}

val LocalActions = staticCompositionLocalOf<Actions> { error("Actions nincs beállítva") }
