package hu.rayworks.vizit.nfc

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation

object NfcRouting {
    private const val NDEF_APPLICATION_AID = "D2760000850101"
    private const val INACTIVE_VIZIT_AID = "F056495A495401"

    fun reset(context: Context) {
        withCardEmulation(context) { cardEmulation, component ->
            cardEmulation.registerAidsForService(
                component,
                CardEmulation.CATEGORY_OTHER,
                listOf(INACTIVE_VIZIT_AID),
            )
        }
    }

    fun activate(activity: Activity): Boolean = withCardEmulation(activity) { cardEmulation, component ->
        val registered = cardEmulation.registerAidsForService(
            component,
            CardEmulation.CATEGORY_OTHER,
            listOf(NDEF_APPLICATION_AID),
        )
        registered && cardEmulation.setPreferredService(activity, component)
    } ?: false

    fun deactivate(activity: Activity) {
        withCardEmulation(activity) { cardEmulation, component ->
            runCatching { cardEmulation.unsetPreferredService(activity) }
            cardEmulation.registerAidsForService(
                component,
                CardEmulation.CATEGORY_OTHER,
                listOf(INACTIVE_VIZIT_AID),
            )
        }
    }

    private inline fun <T> withCardEmulation(
        context: Context,
        block: (CardEmulation, ComponentName) -> T,
    ): T? = runCatching {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return null
        val cardEmulation = CardEmulation.getInstance(adapter)
        val component = ComponentName(context, VizitHostApduService::class.java)
        block(cardEmulation, component)
    }.getOrNull()
}
