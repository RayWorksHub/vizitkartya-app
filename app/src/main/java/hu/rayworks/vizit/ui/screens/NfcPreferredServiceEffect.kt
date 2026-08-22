package hu.rayworks.vizit.ui.screens

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import hu.rayworks.vizit.nfc.VizitHostApduService

@Composable
internal fun NfcPreferredServiceEffect(enabled: Boolean) {
    val context = LocalContext.current
    val activity = context.findNfcActivity()

    DisposableEffect(enabled, activity) {
        if (!enabled || activity == null) return@DisposableEffect onDispose {}

        val adapter = NfcAdapter.getDefaultAdapter(context)
        val cardEmulation = adapter?.let(CardEmulation::getInstance)
        val component = ComponentName(context, VizitHostApduService::class.java)
        val preferred = runCatching {
            cardEmulation?.setPreferredService(activity, component) == true
        }.getOrDefault(false)

        onDispose {
            if (preferred) {
                runCatching { cardEmulation?.unsetPreferredService(activity) }
            }
        }
    }
}

private tailrec fun Context.findNfcActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findNfcActivity()
    else -> null
}
