package hu.rayworks.vizit.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord

object NdefVCardEncoder {
    private const val VCARD_MIME_TYPE = "text/vcard"

    fun encodeVCard(vCard: String): ByteArray {
        val record = NdefRecord.createMime(
            VCARD_MIME_TYPE,
            vCard.toByteArray(Charsets.UTF_8),
        )
        return NdefMessage(arrayOf(record)).toByteArray()
    }

    fun encodeProfileUrl(profileUrl: String): ByteArray =
        NdefMessage(arrayOf(NdefRecord.createUri(profileUrl))).toByteArray()
}
