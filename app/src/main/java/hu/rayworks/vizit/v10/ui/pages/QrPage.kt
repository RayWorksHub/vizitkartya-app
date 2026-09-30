package hu.rayworks.vizit.v10.ui.pages

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.Profile
import hu.rayworks.vizit.v10.data.QrMode
import hu.rayworks.vizit.v10.ui.LocalActions
import hu.rayworks.vizit.v10.ui.components.EmptyState
import hu.rayworks.vizit.v10.ui.components.IconCircleButton
import hu.rayworks.vizit.v10.ui.components.PrimaryButton
import hu.rayworks.vizit.v10.ui.components.QrCode
import hu.rayworks.vizit.v10.ui.components.Segmented
import hu.rayworks.vizit.v10.ui.components.Tone
import hu.rayworks.vizit.v10.ui.components.noRippleClickable
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.V

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Amíg a QR látszik, a kijelző teljes fényerőn van (éles: a teljes képernyős QR így működik). */
@Composable
private fun MaxBrightness() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.activity()?.window
        var before = -1f
        if (window != null) {
            val lp = window.attributes
            before = lp.screenBrightness
            lp.screenBrightness = 1f
            window.attributes = lp
        }
        onDispose {
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = before
                window.attributes = lp
            }
        }
    }
}

private data class QrContent(val text: String?, val title: String, val message: String)

private fun contentFor(app: AppState, p: Profile, mode: QrMode): QrContent = when (mode) {
    QrMode.Profile -> QrContent(
        if (app.profileQrAvailable(p)) app.publicUrl(p) else null,
        "Nincs még publikus profil",
        when {
            !p.isPublic -> "Kapcsold be a publikus profilt."
            !app.synced -> "Várd meg a sikeres szinkront."
            else -> "Ehhez a profilhoz még nincs profilcím."
        },
    )
    QrMode.Contact -> QrContent(
        app.contactVCard(p),
        "A Kontakt QR nem állítható elő",
        when {
            p.name.isBlank() -> "Add meg a nevedet."
            (p.hasValue("phone") || p.hasValue("email")) && !p.shows("phone") && !p.shows("email") ->
                "A telefonszám és az e-mail-cím is rejtett az Adatláthatóságban."
            !p.hasValue("phone") && !p.hasValue("email") -> "Adj meg telefonszámot vagy e-mail-címet."
            else -> "A névjegy túl hosszú az offline QR-hoz. Használd a Profil QR-t."
        },
    )
    QrMode.Photo -> QrContent(
        if (p.photo != null && app.profileQrAvailable(p)) app.publicUrl(p) else null,
        "A fényképes profil nem érhető el",
        if (p.photo == null) "Előbb adj meg profilképet." else "Kapcsold be a publikus profilt, és várd meg a szinkront.",
    )
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(V.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = V.onContainer, modifier = Modifier.size(22.dp))
        }
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = V.ink)
    }
}

/**
 * Teljes képernyős QR (a kártya QR-jára koppintva): Profil / Kontakt / Fényképes mód, fehér lapon,
 * maximális fényerővel; alatta Mentés, Megosztás, Link másolása és a fényképes névjegy.
 */
@Composable
fun QrPage(app: AppState, index: Int) {
    val actions = LocalActions.current
    val p = app.profiles.getOrNull(index) ?: app.focus
    MaxBrightness()
    Box(
        Modifier
            .fillMaxSize()
            .background(V.bg)
            .noRippleClickable { }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(p.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(p.short, fontSize = 13.sp, color = V.sub, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = app.qrMode,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "qrMode",
            ) { mode ->
                val c = contentFor(app, p, mode)
                Box(
                    Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (c.text != null) V.qrPlate else V.surface)
                        .padding(if (c.text != null) 22.dp else 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (c.text != null) {
                        QrCode(c.text, Modifier.fillMaxSize(), withLogo = mode != QrMode.Contact)
                    } else {
                        EmptyState(
                            VIcons.qr, c.title, c.message, Tone.Warn,
                            action = if (mode != QrMode.Contact) "Vissza a Kontakt QR-hoz" else null,
                            onAction = { app.qrMode = QrMode.Contact },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Segmented(QrMode.entries.map { it.label }, app.qrMode.ordinal, Modifier.widthIn(max = 360.dp)) { app.qrMode = QrMode.entries[it] }
            Spacer(Modifier.height(6.dp))
            val payload = contentFor(app, p, app.qrMode).text
            val has = payload != null
            val withLogo = app.qrMode != QrMode.Contact
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile(VIcons.download, "Mentés", has) { payload?.let { actions.saveQr(it, withLogo) } }
                ActionTile(VIcons.share, "Megosztás", has) { payload?.let { actions.shareQr(it, withLogo) } }
                if (app.qrMode == QrMode.Profile) ActionTile(VIcons.copy, "Link", has) { actions.copyLink(p) }
            }
            if (app.qrMode == QrMode.Photo && p.photo != null) {
                Spacer(Modifier.height(6.dp))
                PrimaryButton("Fényképes névjegy megosztása", Modifier.widthIn(max = 360.dp), icon = VIcons.contact) {
                    actions.shareContact(p)
                }
            }
        }
        IconCircleButton(
            VIcons.close,
            "Bezárás",
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 16.dp),
            size = 40.dp,
            background = V.chip,
        ) { app.pop() }
    }
}
