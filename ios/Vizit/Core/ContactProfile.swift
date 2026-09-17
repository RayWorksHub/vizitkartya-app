import Foundation

/// The editable contact fields mirror the Android ContactProfile. This preview
/// deliberately does not publish profiles or modify the existing cloud account.
public struct ContactProfile: Codable, Equatable {
    public var fullName = ""
    public var firstName = ""
    public var lastName = ""
    public var jobTitle = ""
    public var company = ""
    public var phone = ""
    public var email = ""
    public var website = ""
    public var address = ""
    public var linkedIn = ""
    public var photoBase64 = ""

    public init() {}

    public var displayName: String {
        let explicit = fullName.trimmingCharacters(in: .whitespacesAndNewlines)
        if !explicit.isEmpty { return explicit }
        return [lastName, firstName].map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }.joined(separator: " ")
    }

    public var initials: String {
        let letters = displayName.split(whereSeparator: { $0.isWhitespace })
            .prefix(2).compactMap(\.first).map { String($0).uppercased() }.joined()
        return letters.isEmpty ? "V" : letters
    }

    public var normalized: ContactProfile {
        var value = self
        let paths: [WritableKeyPath<ContactProfile, String>] = [
            \.fullName, \.firstName, \.lastName, \.jobTitle, \.company,
            \.phone, \.email, \.website, \.address, \.linkedIn
        ]
        for path in paths {
            value[keyPath: path] = value[keyPath: path].trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return value
    }

    public func validate() throws {
        let p = normalized
        guard !p.displayName.isEmpty else { throw ProfileError.missingName }
        guard !p.phone.isEmpty || !p.email.isEmpty else { throw ProfileError.missingContact }
        let fields = [p.fullName, p.firstName, p.lastName, p.jobTitle, p.company,
                      p.phone, p.email, p.website, p.address, p.linkedIn]
        guard fields.allSatisfy({ $0.utf8.count <= 512 && !$0.unicodeScalars.contains(where: {
            CharacterSet.controlCharacters.contains($0)
        }) }) else { throw ProfileError.invalidField }
        if !p.email.isEmpty {
            guard p.email.range(of: #"^[^\s@]+@[^\s@]+\.[^\s@]+$"#, options: .regularExpression) != nil
            else { throw ProfileError.invalidEmail }
        }
        if !p.phone.isEmpty {
            guard p.phone.filter({ $0.isNumber }).count >= 3,
                  p.phone.unicodeScalars.allSatisfy({ CharacterSet(charactersIn: "+0123456789 ()-./").contains($0) })
            else { throw ProfileError.invalidPhone }
        }
        for link in [p.website, p.linkedIn] where !link.isEmpty {
            guard SafeLink.https(link) != nil else { throw ProfileError.invalidURL }
        }
        if !p.photoBase64.isEmpty {
            guard let data = Data(base64Encoded: p.photoBase64), data.count <= 256 * 1024
            else { throw ProfileError.invalidPhoto }
        }
    }
}

public enum ProfileError: Error, LocalizedError, Equatable {
    case missingName, missingContact, invalidEmail, invalidPhone, invalidURL
    case invalidField, invalidPhoto, oversizedQR, unsupportedFile, damagedFile

    public var errorDescription: String? {
        switch self {
        case .missingName: return "Add meg a nevedet."
        case .missingContact: return "Legalább egy telefonszámot vagy e-mail-címet adj meg."
        case .invalidEmail: return "Ellenőrizd az e-mail-cím formátumát."
        case .invalidPhone: return "Ellenőrizd a telefonszámot; számokat, szóközt és + ( ) - . / jeleket használhatsz."
        case .invalidURL: return "A hivatkozások teljes, https:// kezdetű webcímek legyenek."
        case .invalidField: return "Egy mező túl hosszú, sortörést vagy vezérlőkaraktert tartalmaz."
        case .invalidPhoto: return "A profilkép nem olvasható vagy túl nagy. Válassz új képet."
        case .oversizedQR: return "Túl sok adat a jól olvasható QR-kódhoz. Rövidítsd a mezőket, vagy használd a névjegyküldést."
        case .unsupportedFile: return "Ezt a mentést újabb alkalmazásverzió készítette. Az adatokat nem írtuk felül."
        case .damagedFile: return "A helyi névjegy nem olvasható. Az eredeti mentést megőriztük; a Beállításokban törölheted."
        }
    }
}

public enum SafeLink {
    /// Scanned links are never opened automatically. The UI asks first.
    public static func https(_ text: String) -> URL? {
        guard text.utf8.count <= 2048,
              !text.unicodeScalars.contains(where: { CharacterSet.whitespacesAndNewlines.contains($0) || CharacterSet.controlCharacters.contains($0) }),
              let parts = URLComponents(string: text),
              parts.scheme?.lowercased() == "https", let host = parts.host, !host.isEmpty,
              parts.user == nil, parts.password == nil else { return nil }
        return parts.url
    }
}
