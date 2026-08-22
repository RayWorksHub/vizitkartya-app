package hu.rayworks.vizit.nfc

import hu.rayworks.vizit.data.ContactProfile

object VCardBuilder {
    fun build(profile: ContactProfile): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        appendLine("N:;${escape(profile.fullName)};;;")
        appendLine("FN:${escape(profile.fullName)}")
        appendOptional("ORG", profile.company)
        appendOptional("TITLE", profile.jobTitle)
        if (profile.phone.isNotBlank()) {
            appendLine("TEL;TYPE=CELL:${escape(profile.phone)}")
        }
        if (profile.email.isNotBlank()) {
            appendLine("EMAIL;TYPE=INTERNET:${escape(profile.email)}")
        }
        appendOptional("URL", profile.website)
        if (profile.address.isNotBlank()) {
            appendLine("ADR;TYPE=WORK:;;${escape(profile.address)};;;;")
        }
        if (profile.linkedIn.isNotBlank()) {
            appendLine("X-SOCIALPROFILE;TYPE=linkedin:${escape(profile.linkedIn)}")
        }
        if (profile.photoBase64.isNotBlank()) {
            appendFolded("PHOTO;ENCODING=b;TYPE=JPEG:${profile.photoBase64}")
        }
        append("END:VCARD\r\n")
    }.replace("\n", "\r\n").replace("\r\r\n", "\r\n")

    private fun StringBuilder.appendOptional(property: String, value: String) {
        if (value.isNotBlank()) appendLine("$property:${escape(value)}")
    }

    private fun StringBuilder.appendFolded(line: String) {
        line.chunked(73).forEachIndexed { index, chunk ->
            if (index > 0) append(' ')
            append(chunk)
            appendLine()
        }
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .trim()
}
