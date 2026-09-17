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
    public var publicSlug = ""
    public var isPublic = false

    public init() {}

    private enum CodingKeys: String, CodingKey {
        case fullName, firstName, lastName, jobTitle, company, phone, email
        case website, address, linkedIn, photoBase64, publicSlug, isPublic
    }

    /// Explicit decoding keeps profiles created by the earlier local-only iOS
    /// preview readable after the cloud sharing fields were added.
    public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        fullName = try values.decodeIfPresent(String.self, forKey: .fullName) ?? ""
        firstName = try values.decodeIfPresent(String.self, forKey: .firstName) ?? ""
        lastName = try values.decodeIfPresent(String.self, forKey: .lastName) ?? ""
        jobTitle = try values.decodeIfPresent(String.self, forKey: .jobTitle) ?? ""
        company = try values.decodeIfPresent(String.self, forKey: .company) ?? ""
        phone = try values.decodeIfPresent(String.self, forKey: .phone) ?? ""
        email = try values.decodeIfPresent(String.self, forKey: .email) ?? ""
        website = try values.decodeIfPresent(String.self, forKey: .website) ?? ""
        address = try values.decodeIfPresent(String.self, forKey: .address) ?? ""
        linkedIn = try values.decodeIfPresent(String.self, forKey: .linkedIn) ?? ""
        photoBase64 = try values.decodeIfPresent(String.self, forKey: .photoBase64) ?? ""
        publicSlug = try values.decodeIfPresent(String.self, forKey: .publicSlug) ?? ""
        isPublic = try values.decodeIfPresent(Bool.self, forKey: .isPublic) ?? false
    }

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
            \.phone, \.email, \.website, \.address, \.linkedIn, \.publicSlug
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
        guard p.displayName.count <= 80, p.firstName.count <= 100, p.lastName.count <= 100,
              p.jobTitle.count <= 100, p.company.count <= 100, p.phone.count <= 40,
              p.email.count <= 254, p.website.count <= 300, p.address.count <= 180,
              p.linkedIn.count <= 300 else { throw ProfileError.invalidField }
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
        if !p.publicSlug.isEmpty && !PublicProfileLink.isValidSlug(p.publicSlug) {
            throw ProfileError.invalidSlug
        }
    }
}

public enum ProfileError: Error, LocalizedError, Equatable {
    case missingName, missingContact, invalidEmail, invalidPhone, invalidURL
    case invalidField, invalidPhoto, invalidSlug, oversizedQR, unsupportedFile, damagedFile

    public var errorDescription: String? {
        switch self {
        case .missingName: return "Add meg a nevedet."
        case .missingContact: return "Legalább egy telefonszámot vagy e-mail-címet adj meg."
        case .invalidEmail: return "Ellenőrizd az e-mail-cím formátumát."
        case .invalidPhone: return "Ellenőrizd a telefonszámot; számokat, szóközt és + ( ) - . / jeleket használhatsz."
        case .invalidURL: return "A hivatkozások teljes, https:// kezdetű webcímek legyenek."
        case .invalidField: return "Egy mező túl hosszú, sortörést vagy vezérlőkaraktert tartalmaz."
        case .invalidPhoto: return "A profilkép nem olvasható vagy túl nagy. Válassz új képet."
        case .invalidSlug: return "A nyilvános profilazonosító 3–50 kisbetűből, számból és kötőjelből állhat."
        case .oversizedQR: return "Túl sok adat a jól olvasható QR-kódhoz. Rövidítsd a mezőket, vagy használd a névjegyküldést."
        case .unsupportedFile: return "Ezt a mentést újabb alkalmazásverzió készítette. Az adatokat nem írtuk felül."
        case .damagedFile: return "A helyi névjegy nem olvasható. Az eredeti mentést megőriztük; a Beállításokban törölheted."
        }
    }
}

public enum PublicProfileLink {
    public static func isValidSlug(_ slug: String) -> Bool {
        slug.range(of: #"^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$"#, options: .regularExpression) != nil
    }

    public static func make(baseURL: URL, slug: String) -> URL? {
        guard baseURL.scheme?.lowercased() == "https", baseURL.host?.isEmpty == false,
              baseURL.user == nil, baseURL.password == nil,
              isValidSlug(slug) else { return nil }
        return baseURL.appendingPathComponent(slug, isDirectory: false)
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
