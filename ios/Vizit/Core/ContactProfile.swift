import Foundation

/// The editable contact fields mirror the currently supported Android profile
/// fields and the live Supabase profile schema.
public struct ContactProfile: Codable, Equatable, Sendable {
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
    public var facebook = ""
    public var instagram = ""
    public var tiktok = ""
    public var youtube = ""
    public var photoBase64 = ""
    public var photoSyncInitialized = false
    public var publicSlug = ""
    public var isPublic = false

    public init() {}

    private enum CodingKeys: String, CodingKey {
        case fullName, firstName, lastName, jobTitle, company, phone, email
        case website, address, linkedIn, facebook, instagram, tiktok, youtube
        case photoBase64, photoSyncInitialized, publicSlug, isPublic
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
        facebook = try values.decodeIfPresent(String.self, forKey: .facebook) ?? ""
        instagram = try values.decodeIfPresent(String.self, forKey: .instagram) ?? ""
        tiktok = try values.decodeIfPresent(String.self, forKey: .tiktok) ?? ""
        youtube = try values.decodeIfPresent(String.self, forKey: .youtube) ?? ""
        photoBase64 = try values.decodeIfPresent(String.self, forKey: .photoBase64) ?? ""
        photoSyncInitialized = try values.decodeIfPresent(Bool.self, forKey: .photoSyncInitialized) ?? false
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
            \.phone, \.email, \.website, \.address, \.linkedIn, \.facebook,
            \.instagram, \.tiktok, \.youtube, \.publicSlug
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
                      p.phone, p.email, p.website, p.address, p.linkedIn,
                      p.facebook, p.instagram, p.tiktok, p.youtube]
        guard fields.allSatisfy({ $0.utf8.count <= 512 && !$0.unicodeScalars.contains(where: {
            CharacterSet.controlCharacters.contains($0)
        }) }) else { throw ProfileError.invalidField }
        guard p.displayName.count <= 80, p.firstName.count <= 100, p.lastName.count <= 100,
              p.jobTitle.count <= 100, p.company.count <= 100, p.phone.count <= 40,
              p.email.count <= 254, p.website.count <= 300, p.address.count <= 180,
              p.linkedIn.count <= 300, p.facebook.count <= 300, p.instagram.count <= 300,
              p.tiktok.count <= 300, p.youtube.count <= 300 else { throw ProfileError.invalidField }
        if !p.email.isEmpty {
            guard p.email.range(of: #"^[^\s@]+@[^\s@]+\.[^\s@]+$"#, options: .regularExpression) != nil
            else { throw ProfileError.invalidEmail }
        }
        if !p.phone.isEmpty {
            guard p.phone.filter({ $0.isNumber }).count >= 3,
                  p.phone.unicodeScalars.allSatisfy({ CharacterSet(charactersIn: "+0123456789 ()-./").contains($0) })
            else { throw ProfileError.invalidPhone }
        }
        for link in [p.website] + p.socialProfiles.map({ $0.url }) where !link.isEmpty {
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

public enum SocialPlatform: String, CaseIterable, Sendable {
    case linkedin, facebook, instagram, tiktok, youtube

    public var label: String {
        switch self {
        case .linkedin: return "LinkedIn"
        case .facebook: return "Facebook"
        case .instagram: return "Instagram"
        case .tiktok: return "TikTok"
        case .youtube: return "YouTube"
        }
    }

    public var sortOrder: Int {
        switch self {
        case .linkedin: return 1
        case .facebook: return 2
        case .instagram: return 3
        case .tiktok: return 4
        case .youtube: return 5
        }
    }
}

public extension ContactProfile {
    func socialURL(for platform: SocialPlatform) -> String {
        switch platform {
        case .linkedin: return linkedIn
        case .facebook: return facebook
        case .instagram: return instagram
        case .tiktok: return tiktok
        case .youtube: return youtube
        }
    }

    mutating func setSocialURL(_ value: String, for platform: SocialPlatform) {
        switch platform {
        case .linkedin: linkedIn = value
        case .facebook: facebook = value
        case .instagram: instagram = value
        case .tiktok: tiktok = value
        case .youtube: youtube = value
        }
    }

    var socialProfiles: [(platform: SocialPlatform, url: String)] {
        SocialPlatform.allCases.map { ($0, socialURL(for: $0)) }
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
              baseURL.query == nil, baseURL.fragment == nil,
              isValidSlug(slug) else { return nil }
        return baseURL.appendingPathComponent(slug, isDirectory: false)
    }
}

public enum ProfileSlug {
    /// Ordered candidates for first profile creation. The first value preserves
    /// the requested public URL. Later values are stable per account and stay
    /// inside the 50-character public-slug contract.
    public static func creationCandidates(requested: String, ownerID: UUID) -> [String] {
        let ownerToken = ownerID.uuidString.lowercased().replacingOccurrences(of: "-", with: "")
        let requested = requested.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let primary = requested.isEmpty ? "vizit-\(ownerToken.prefix(12))" : requested
        let readableFallback = requested.isEmpty
            ? "vizit-\(ownerToken)"
            : "\(requested.prefix(40))-\(ownerToken.prefix(8))"
        let ownerFallback = "vizit-\(ownerToken)"

        var seen = Set<String>()
        return [primary, readableFallback, ownerFallback].filter { seen.insert($0).inserted }
    }
}

public enum ProfileSyncPolicy {
    /// A locally reserved profile ID with no timestamp is a recoverable first
    /// upload (the insert may have succeeded before the connection dropped).
    /// A new device with no matching ID cannot overwrite an existing profile.
    public static func mayUploadPending(localProfileID: UUID?, localUpdatedAt: String?,
                                        remoteProfileID: UUID, remoteUpdatedAt: String) -> Bool {
        guard localProfileID == remoteProfileID else { return false }
        return localUpdatedAt != nil && localUpdatedAt == remoteUpdatedAt
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


/// A bounded JPEG is stored atomically with the owner's RLS-protected profile.
/// Private photos are not uploaded to the globally public avatar bucket.
public enum ProfilePhoto {
    public static func inlineURL(_ base64: String) throws -> String {
        guard !base64.isEmpty else { return "" }
        guard let bytes = Data(base64Encoded: base64), bytes.count <= 256 * 1024,
              bytes.count >= 3, Array(bytes.prefix(3)) == [0xff, 0xd8, 0xff],
              bytes.base64EncodedString() == base64 else { throw ProfileError.invalidPhoto }
        return "data:image/jpeg;base64," + base64
    }

    public static func trustedStorageURL(_ text: String, origin: URL) -> URL? {
        guard let url = SafeLink.https(text), url.host == origin.host, url.port == origin.port,
              url.query == nil, url.fragment == nil,
              !url.path.contains(".."),
              url.path.range(of: #"^/storage/v1/object/public/avatars/[a-fA-F0-9-]{36}/[a-zA-Z0-9_.-]+$"#,
                             options: .regularExpression) != nil else { return nil }
        return url
    }
}

public enum ContactQRLink {
    public static func make(publicURL: URL) -> URL? {
        guard SafeLink.https(publicURL.absoluteString) != nil else { return nil }
        var parts = URLComponents(url: publicURL, resolvingAgainstBaseURL: false)
        parts?.queryItems = [URLQueryItem(name: "contact", value: "1")]
        parts?.fragment = nil
        return parts?.url
    }
}


public enum ProfileRevisionPolicy {
    public static func mayUpload(localID: UUID?, localRevision: String?, localFingerprint: String?,
                                 remoteID: UUID, remoteRevision: String, remoteFingerprint: String) -> Bool {
        guard localID == remoteID else { return false }
        if let localFingerprint { return localFingerprint == remoteFingerprint }
        return localRevision != nil && localRevision == remoteRevision
    }

    public static func migrateLocalPhoto(initialized: Bool, hasLocalPhoto: Bool,
                                         remoteHasPhoto: Bool, sameRevision: Bool) -> Bool {
        return !initialized && hasLocalPhoto && !remoteHasPhoto && sameRevision
    }
}


public extension ContactProfile {
    func validateIfPresent() throws {
        if !displayName.isEmpty || !phone.isEmpty || !email.isEmpty { try validate() }
    }
}
