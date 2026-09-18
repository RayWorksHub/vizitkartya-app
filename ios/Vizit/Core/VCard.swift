import Foundation

public enum VCard {
    /// Contact QR intentionally contains no photograph: embedding one makes the
    /// QR too dense. The explicit file-sharing route can include the picture.
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
        add("URL;TYPE=WORK", p.linkedIn)
        if !p.address.isEmpty { lines.append("ADR;TYPE=WORK:;;\(escape(p.address));;;;") }
        if includePhoto && !p.photoBase64.isEmpty {
            lines.append("PHOTO;ENCODING=b;TYPE=JPEG:\(p.photoBase64)")
        }
        lines.append("END:VCARD")
        return lines.map(fold).joined(separator: "\r\n") + "\r\n"
    }

    public static func qrPayload(_ profile: ContactProfile) throws -> String {
        let payload = try encode(profile)
        // Keep enough module headroom for reliable camera decoding on a
        // handheld screen; the .vcf share route remains available for denser data.
        guard payload.utf8.count <= 1400 else { throw ProfileError.oversizedQR }
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
