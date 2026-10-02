package hu.rayworks.vizit.nfc

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation

object NfcRouting {
    private const val NDEF_APPLICATION_AID = "D2760000850101"

    /**
     * Drop any dynamic AID registration left behind by a previous process/session.
     *
     * The manifest contains a harmless proprietary fallback AID. Removing the dynamic
     * group lets Android restore that static route, so VIZIT only emulates an NDEF tag
     * while the user is explicitly on the NFC sharing screen.
     */
    fun reset(context: Context) {
        withCardEmulation(context) { cardEmulation, component ->
            runCatching {
                cardEmulation.removeAidsForService(
                    component,
                    CardEmulation.CATEGORY_OTHER,
                )
            }
        }
    }

    fun activate(activity: Activity): Boolean = withCardEmulation(activity) { cardEmulation, component ->
        val registered = cardEmulation.registerAidsForService(
            component,
            CardEmulation.CATEGORY_OTHER,
            listOf(NDEF_APPLICATION_AID),
        )
        if (!registered) return@withCardEmulation false

        // CATEGORY_OTHER AIDs are routable once registered. Foreground preference is
        // still requested to win an AID conflict, but some OEMs return false here even
        // though the dynamic route is already active. Do not turn a valid route into a
        // false "routing failed" state just because that optional priority call fails.
        runCatching { cardEmulation.setPreferredService(activity, component) }
        true
    } ?: false

    fun deactivate(activity: Activity) {
        withCardEmulation(activity) { cardEmulation, component ->
            runCatching { cardEmulation.unsetPreferredService(activity) }
            runCatching {
                cardEmulation.removeAidsForService(
                    component,
                    CardEmulation.CATEGORY_OTHER,
                )
            }
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
