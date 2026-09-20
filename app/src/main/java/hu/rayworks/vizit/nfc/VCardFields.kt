package hu.rayworks.vizit.nfc

/**
 * Just enough vCard reading to pre-fill the system contact editor after a scan.
 *
 * This is deliberately not a general parser: it takes the properties the editor
 * can accept, unescapes them, and ignores everything else. A scanned card is
 * untrusted input, so sizes are bounded and only the first value of each
 * property is kept.
 */
object VCardFields {
    val SUPPORTED = setOf("FN", "ORG", "TITLE", "TEL", "EMAIL", "ADR")

    private const val MAX_INPUT_CHARS = 64_000
    private const val MAX_VALUE_CHARS = 512

    fun parse(vcard: String): Map<String, String> {
        if (vcard.length > MAX_INPUT_CHARS) return emptyMap()
        // Unfold the RFC continuation lines before anything is split on ':'.
        val unfolded = vcard
            .replace("\r\n ", "")
            .replace("\n ", "")
            .replace("\r ", "")

        val fields = LinkedHashMap<String, String>()
        unfolded.split("\r\n", "\n", "\r").forEach { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) return@forEach
            val key = line.substring(0, separator).substringBefore(';').uppercase()
            if (key !in SUPPORTED || fields.containsKey(key)) return@forEach

            val value = unescape(line.substring(separator + 1))
                .split('\u0000')
                .filter(String::isNotBlank)
                .joinToString(", ")
                .trim()
            if (value.isNotBlank() && value.length <= MAX_VALUE_CHARS) fields[key] = value
        }
        return fields
    }

    /**
     * vCard escaping, in one pass so an escaped backslash can never turn the
     * character after it into a separator. Unescaped semicolons are structural,
     * so they become a placeholder the caller joins on.
     */
    private fun unescape(raw: String): String = buildString(raw.length) {
        var index = 0
        while (index < raw.length) {
            val char = raw[index]
            if (char == '\\' && index + 1 < raw.length) {
                when (val next = raw[index + 1]) {
                    'n', 'N' -> append(' ')
                    else -> append(next)
                }
                index += 2
            } else {
                append(if (char == ';') '\u0000' else char)
                index++
            }
        }
    }
}
