import Foundation

public enum VCard {
    /// The caller chooses whether to include a photograph. VIZIT's public
    /// hand-off routes require it; plain encoding remains available for safe
    /// parsing and legacy import tests.
    public static func encode(_ profile: ContactProfile, includePhoto: Bool = false) throws -> String {
        try profile.validate()
        let p = profile.normalized
        var lines = ["BEGIN:VCARD", "VERSION:3.0", "FN:\(escape(p.displayName))",
                     "N:\(escape(p.lastName));\(escape(p.firstName));;;"]
        func add(_ key: String, _ value: String) {
            if !value.isEmpty { lines.append("\(key):\(escape(value))") }
        }
        add("ORG", p.company)
        add("TITLE", p.jobTitle)
        add("TEL;TYPE=CELL", p.phone)
        add("EMAIL;TYPE=INTERNET", p.email)
        add("URL", p.website)
        for profile in p.socialProfiles where !profile.url.isEmpty {
            add("X-SOCIALPROFILE;TYPE=\(profile.platform.rawValue)", profile.url)
        }
        if !p.address.isEmpty { lines.append("ADR;TYPE=WORK:;;\(escape(p.address));;;;") }
        if includePhoto && !p.photoBase64.isEmpty {
            lines.append("PHOTO;ENCODING=b;TYPE=JPEG:\(p.photoBase64)")
        }
        lines.append("END:VCARD")
        return lines.map(fold).joined(separator: "\r\n") + "\r\n"
    }

    public static func qrPayload(_ profile: ContactProfile, includePhoto: Bool = false) throws -> String {
        try profile.validate()
        let p = profile.normalized
        var lines = [
            "BEGIN:VCARD",
            "VERSION:3.0",
            "FN:\(escape(p.displayName))",
            "N:\(escape(p.lastName));\(escape(p.firstName));;;",
        ]
        func add(_ key: String, _ value: String) {
            if !value.isEmpty { lines.append("\(key):\(escape(value))") }
        }

        // QR uses standard, compact property names. The .vcf export keeps the
        // richer TYPE labels, while the QR retains every value and lets the URL
        // itself identify each social service. This saves enough redundant text
        // for a real photo at level-H error correction without dropping data.
        add("ORG", p.company)
        add("TITLE", p.jobTitle)
        add("TEL", p.phone)
        add("EMAIL", p.email)
        add("URL", p.website)
        for social in p.socialProfiles where !social.url.isEmpty {
            add("URL", social.url)
        }
        if !p.address.isEmpty { lines.append("ADR:;;\(escape(p.address));;;;") }
        if includePhoto && !p.photoBase64.isEmpty {
            lines.append("PHOTO;ENCODING=b;TYPE=JPEG:\(p.photoBase64)")
        }
        lines.append("END:VCARD")
        let payload = lines.map(fold).joined(separator: "\r\n") + "\r\n"
        // Keep enough module headroom for reliable camera decoding on a
        // handheld screen; the .vcf share route remains available for denser data.
        guard payload.utf8.count <= 2200 else { throw ProfileError.oversizedQR }
        return payload
    }

    public static func escape(_ text: String) -> String {
        text.replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "\r\n", with: "\n")
            .replacingOccurrences(of: "\r", with: "\n")
            .replacingOccurrences(of: "\n", with: "\\n")
            .replacingOccurrences(of: ";", with: "\\;")
            .replacingOccurrences(of: ",", with: "\\,")
    }

    /// RFC-style 75-octet physical lines; never split a UTF-8 scalar. The leading
    /// continuation space counts towards the next line's 75-octet limit.
    public static func fold(_ line: String) -> String {
        var output = ""
        var bytes = 0
        for scalar in line.unicodeScalars {
            let chunk = String(scalar)
            if bytes + chunk.utf8.count > 75 {
                output += "\r\n "
                bytes = 1
            }
            output += chunk
            bytes += chunk.utf8.count
        }
        return output
    }
}
