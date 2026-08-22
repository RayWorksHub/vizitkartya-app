package hu.rayworks.vizit.nfc

import hu.rayworks.vizit.data.ContactProfile

object VCardBuilder {
    fun build(
        profile: ContactProfile,
        embeddedPhotoBase64: String = profile.photoBase64,
    ): String = buildString {
        appendVCardLine("BEGIN:VCARD")
        appendVCardLine("VERSION:3.0")
        appendVCardLine(structuredName(profile))
        appendVCardLine("FN:${escape(profile.resolvedDisplayName)}")
        appendOptional("ORG", profile.company)
        appendOptional("TITLE", profile.jobTitle)
        if (profile.phone.isNotBlank()) {
            appendVCardLine("TEL;TYPE=CELL:${escape(profile.phone)}")
        }
        if (profile.email.isNotBlank()) {
            appendVCardLine("EMAIL;TYPE=INTERNET:${escape(profile.email)}")
        }
        if (profile.website.isNotBlank()) {
            appendVCardLine("URL;TYPE=WORK:${escape(profile.website)}")
        }
        if (profile.address.isNotBlank()) {
            appendVCardLine("ADR;TYPE=WORK:;;${escape(profile.address)};;;;")
        }
        if (profile.linkedIn.isNotBlank()) {
            appendVCardLine("X-SOCIALPROFILE;TYPE=linkedin:${escape(profile.linkedIn)}")
        }
        if (profile.publicProfileUrl.isNotBlank()) {
            appendVCardLine("URL;TYPE=VIZIT:${escape(profile.publicProfileUrl)}")
        }
        appendOptional("NOTE", profile.note)
        if (embeddedPhotoBase64.isNotBlank()) {
            appendVCardLine("PHOTO;ENCODING=b;TYPE=JPEG:$embeddedPhotoBase64")
        }
        appendVCardLine("END:VCARD")
    }

    private fun structuredName(profile: ContactProfile): String = when {
        profile.firstName.isNotBlank() || profile.lastName.isNotBlank() ->
            "N:${escape(profile.lastName)};${escape(profile.firstName)};;;"

        else -> "N:;${escape(profile.resolvedDisplayName)};;;"
    }

    private fun StringBuilder.appendOptional(property: String, value: String) {
        if (value.isNotBlank()) appendVCardLine("$property:${escape(value)}")
    }

    private fun StringBuilder.appendVCardLine(line: String) {
        var index = 0
        var bytesOnLine = 0
        var continuation = false

        while (index < line.length) {
            val codePoint = Character.codePointAt(line, index)
            val value = String(Character.toChars(codePoint))
            val valueBytes = value.toByteArray(Charsets.UTF_8).size
            val limit = if (continuation) CONTINUATION_CONTENT_BYTES else FIRST_LINE_BYTES

            if (bytesOnLine > 0 && bytesOnLine + valueBytes > limit) {
                append("\r\n ")
                continuation = true
                bytesOnLine = 0
            }

            append(value)
            bytesOnLine += valueBytes
            index += Character.charCount(codePoint)
        }
        append("\r\n")
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\n", "\\n")
        .trim()

    private const val FIRST_LINE_BYTES = 75
    private const val CONTINUATION_CONTENT_BYTES = 74
}
