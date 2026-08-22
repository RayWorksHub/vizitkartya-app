package hu.rayworks.vizit.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord

object NdefVCardEncoder {
    private const val VCARD_MIME_TYPE = "text/vcard"

    fun encode(vCard: String, fallbackUrl: String? = null): ByteArray {
        val records = buildList {
            add(
                NdefRecord.createMime(
                    VCARD_MIME_TYPE,
                    vCard.toByteArray(Charsets.UTF_8),
                ),
            )
            fallbackUrl
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?.let { add(NdefRecord.createUri(Uri.parse(it))) }
        }
        return NdefMessage(records.toTypedArray()).toByteArray()
    }
}
