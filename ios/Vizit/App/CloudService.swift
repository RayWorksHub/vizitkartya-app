import AuthenticationServices
import Foundation
import Supabase
import UIKit
import ImageIO
import CryptoKit

struct AppFeatureFlags: Equatable, Sendable {
    var businessPortal = true
    var analytics = true
    var crm = true
    var qrScanner = true
    var onlineEditor = true

    init(rows: [RemoteFeatureFlag] = []) {
        let values = Dictionary(uniqueKeysWithValues: rows.map { ($0.key, $0.enabled) })
        businessPortal = values["business_portal"] ?? true
        analytics = values["analytics"] ?? true
        crm = values["crm"] ?? true
        qrScanner = values["qr_scanner"] ?? true
        onlineEditor = values["online_editor"] ?? true
    }
}

struct RemoteFeatureFlag: Decodable, Sendable {
    let key: String
    let enabled: Bool
}

struct ProfileAppearance: Codable, Sendable {
    let version: Int
    let mode: String
    let start: String
    let end: String
    let angle: Int
    let text: String
    var logoURL: String?
    let logoScale: Int
    let logoSurface: String
    let mediaLayout: String

    enum CodingKeys: String, CodingKey {
        case version, mode, start, end, angle, text
        case logoURL = "logo_url"
        case logoScale = "logo_scale"
        case logoSurface = "logo_surface"
        case mediaLayout = "media_layout"
    }

    static func preset(_ theme: String) -> Self {
        let colors: (String, String)
        switch theme {
        case "ivory": colors = ("#1668f0", "#0742a8")
        case "forest": colors = ("#0e6b4a", "#063526")
        case "plum": colors = ("#5b2e9e", "#2c1550")
        default: colors = ("#0c2c63", "#05163a")
        }
        return Self(version: 1, mode: "preset", start: colors.0, end: colors.1,
                    angle: 145, text: "auto", logoURL: nil, logoScale: 88,
                    logoSurface: "white", mediaLayout: "overlay")
    }

    var revision: String {
        [String(version), mode, start, end, String(angle), text, logoURL ?? "",
         String(logoScale), logoSurface, mediaLayout].joined(separator: "|")
    }
}

struct RemoteProfile: Decodable, Sendable {
    let id: UUID
    let ownerID: UUID
    let slug: String
    let displayName: String
    let jobTitle: String
    let company: String
    let bio: String
    let publicEmail: String
    let phone: String
    let website: String
    let address: String
    let isPublic: Bool
    let customDomain: String?
    let customDomainVerified: Bool?
    let updatedAt: String
    let createdAt: String?
    let isPrimary: Bool?
    let avatarURL: String?
    let appearance: ProfileAppearance?
    let theme: String
    let accentColor: String?
    var linkedIn = ""
    var facebook = ""
    var instagram = ""
    var tiktok = ""
    var youtube = ""
    var x = ""
    var github = ""
    var custom = ""

    var fingerprint: String {
        let values = [id.uuidString, ownerID.uuidString, slug, displayName, jobTitle, company, bio,
                      publicEmail, phone, website, address, String(isPublic), customDomain ?? "",
                      String(customDomainVerified == true), avatarURL ?? "", appearance?.revision ?? ""]
            + SocialPlatform.allCases.map { socialURL(for: $0) }
        let bytes = (try? JSONEncoder().encode(values)) ?? Data()
        return SHA256.hash(data: bytes).map { String(format: "%02x", $0) }.joined()
    }

    func matches(_ value: ContactProfile, remoteLogo: String? = nil) -> Bool {
        let p = value.normalized
        return displayName == p.displayName && slug == p.publicSlug && jobTitle == p.jobTitle &&
            company == p.company && bio == p.bio && publicEmail == p.email && phone == p.phone && website == p.website &&
            address == p.address && isPublic == p.isPublic &&
            (customDomain ?? "") == p.customDomain &&
            SocialPlatform.allCases.allSatisfy { socialURL(for: $0) == p.socialURL(for: $0) } &&
            (avatarURL ?? "") == ((try? ProfilePhoto.inlineURL(p.photoBase64)) ?? "invalid-photo") &&
            (remoteLogo == nil || remoteLogo == p.logoBase64)
    }

    func socialURL(for platform: SocialPlatform) -> String {
        switch platform {
        case .linkedin: return linkedIn
        case .facebook: return facebook
        case .instagram: return instagram
        case .tiktok: return tiktok
        case .youtube: return youtube
        case .x: return x
        case .github: return github
        case .custom: return custom
        }
    }

    mutating func setSocialURL(_ value: String, for platform: SocialPlatform) {
        switch platform {
        case .linkedin: linkedIn = value
        case .facebook: facebook = value
        case .instagram: instagram = value
        case .tiktok: tiktok = value
        case .youtube: youtube = value
        case .x: x = value
        case .github: github = value
        case .custom: custom = value
        }
    }

    mutating func copySocialProfiles(from other: RemoteProfile) {
        for platform in SocialPlatform.allCases {
            setSocialURL(other.socialURL(for: platform), for: platform)
        }
    }

    enum CodingKeys: String, CodingKey {
        case id, slug, phone, website, address
        case ownerID = "owner_id"
        case displayName = "display_name"
        case jobTitle = "job_title"
        case company, bio
        case publicEmail = "public_email"
        case isPublic = "is_public"
        case customDomain = "custom_domain"
        case customDomainVerified = "custom_domain_verified"
        case avatarURL = "avatar_url"
        case appearance, theme
        case accentColor = "accent_color"
        case createdAt = "created_at"
        case isPrimary = "is_primary"
        case updatedAt = "updated_at"
    }
}

private struct RemoteLink: Decodable {
    let id: UUID
    let platform: String
    let url: String
}

private struct DeletedProfile: Decodable {
    let id: UUID
}

private struct ProfileWrite: Encodable {
    let id: UUID?
    let ownerID: UUID?
    let slug: String
    let displayName: String
    let jobTitle: String
    let company: String
    let bio: String
    let publicEmail: String
    let phone: String
    let website: String
    let address: String
    let isPublic: Bool
    let customDomain: String?
    let avatarURL: String
    let appearance: ProfileAppearance?

    enum CodingKeys: String, CodingKey {
        case id, slug, phone, website, address, company, bio
        case ownerID = "owner_id"
        case displayName = "display_name"
        case jobTitle = "job_title"
        case publicEmail = "public_email"
        case isPublic = "is_public"
        case customDomain = "custom_domain"
        case avatarURL = "avatar_url"
        case appearance
    }
}

private struct LinkWrite: Encodable {
    let profileID: UUID
    let platform: String
    let label: String
    let url: String
    let sortOrder: Int
    let enabled = true

    enum CodingKeys: String, CodingKey {
        case platform, label, url, enabled
        case profileID = "profile_id"
        case sortOrder = "sort_order"
    }
}

private struct PostgRESTErrorPayload: Decodable {
    let code: String?
}

enum RESTURLBuilder {
    static func make(baseURL: URL, path: [String], query: [URLQueryItem]) -> URL? {
        var url = baseURL
        path.forEach { url.appendPathComponent($0) }
        var parts = URLComponents(url: url, resolvingAgainstBaseURL: false)
        parts?.queryItems = query.isEmpty ? nil : query

        // URLComponents keeps literal "+" characters in query values. Supabase's
        // PostgREST layer decodes them as spaces, which corrupts timestamps such
        // as "2026-09-18T17:12:04+00:00" and returns HTTP 400 / SQLSTATE 22007.
        if let encodedQuery = parts?.percentEncodedQuery {
            parts?.percentEncodedQuery = encodedQuery.replacingOccurrences(of: "+", with: "%2B")
        }
        return parts?.url
    }
}

final class CloudService: @unchecked Sendable {
    private static let authStorageService = "hu.rayworks.vizit.ios.session"
    private static let authStorageKey = "vizit-auth-session"
    private static let passwordResetRequestedAtKey = "vizit-password-reset-requested-at"

    let configuration: AppConfiguration
    let client: SupabaseClient
    private let http: URLSession
    private let authStorage: SecureSessionStorage

    init(configuration: AppConfiguration) {
        self.configuration = configuration
        let sessionConfiguration = URLSessionConfiguration.ephemeral
        sessionConfiguration.waitsForConnectivity = true
        sessionConfiguration.timeoutIntervalForRequest = 30
        sessionConfiguration.requestCachePolicy = .reloadIgnoringLocalCacheData
        http = URLSession(configuration: sessionConfiguration)
        let storage = SecureSessionStorage(service: Self.authStorageService)
        authStorage = storage
        let auth = SupabaseClientOptions.AuthOptions(
            storage: storage,
            redirectToURL: configuration.callbackURL,
            storageKey: Self.authStorageKey,
            flowType: .pkce,
            autoRefreshToken: true,
            emitLocalSessionAsInitialSession: true
        )
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "unknown"
        let options = SupabaseClientOptions(
            auth: auth,
            global: .init(headers: ["X-Client-Info": "vizit-ios/\(version)"], session: http)
        )
        client = SupabaseClient(supabaseURL: configuration.supabaseURL,
                                supabaseKey: configuration.publishableKey,
                                options: options)
    }

    var cachedSession: Session? { client.auth.currentSession }

    func validSession() async throws -> Session { try await client.auth.session }

    @discardableResult
    private func requireOwnerSession(_ ownerID: UUID) async throws -> Session {
        let session = try await validSession()
        guard session.user.id == ownerID else { throw CloudError.accountMismatch }
        return session
    }

    func login(email: String, password: String) async throws -> Session {
        try await client.auth.signIn(email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password)
    }

    func register(name: String, email: String, password: String) async throws {
        var callback = URLComponents(url: configuration.callbackURL, resolvingAgainstBaseURL: false)
        callback?.queryItems = [URLQueryItem(name: "flow", value: "signup")]
        guard let signupCallback = callback?.url else { throw CloudError.invalidCallback }
        let response = try await client.auth.signUp(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines),
            password: password,
            data: [
                "display_name": .string(name.trimmingCharacters(in: .whitespacesAndNewlines)),
                "privacy_version": .string(configuration.privacyPolicyVersion),
                "terms_version": .string(configuration.termsVersion)
            ],
            redirectTo: signupCallback
        )
        // Email confirmation is required by the active Auth policy. A session here means
        // the backend was weakened, so discard it instead of silently signing in.
        if response.session != nil {
            try? await client.auth.signOut()
            throw CloudError.emailConfirmationDisabled
        }
    }

    @MainActor
    func googleLogin() async throws -> Session {
        guard configuration.googleSignInEnabled else { throw CloudError.providerUnavailable }
        return try await client.auth.signInWithOAuth(provider: .google, redirectTo: configuration.callbackURL) {
            $0.prefersEphemeralWebBrowserSession = true
        }
    }

    func handleCallback(_ url: URL) async throws -> Session {
        guard AuthCallback.accepts(url, expected: configuration.callbackURL), url.absoluteString.utf8.count <= 8_192
        else { throw CloudError.invalidCallback }
        let session = try await client.auth.session(from: url)
        if isPasswordRecovery(url) {
            try? authStorage.remove(key: Self.passwordResetRequestedAtKey)
        }
        return session
    }

    func requestPasswordReset(email: String) async throws {
        let remaining = PasswordResetPolicy.remainingSeconds(since: lastPasswordResetRequest())
        guard remaining == 0 else { throw CloudError.passwordResetCooldown(remaining) }

        var parts = URLComponents(url: configuration.callbackURL, resolvingAgainstBaseURL: false)
        parts?.queryItems = [URLQueryItem(name: "flow", value: "recovery")]
        guard let redirect = parts?.url else { throw CloudError.invalidCallback }
        let verifierKey = "\(Self.authStorageKey)-code-verifier"
        let previousVerifier = try? authStorage.retrieve(key: verifierKey)
        do {
            try await client.auth.resetPasswordForEmail(email.trimmingCharacters(in: .whitespacesAndNewlines),
                                                        redirectTo: redirect)
            recordPasswordResetRequest()
        } catch {
            // Supabase prepares a new PKCE verifier before the network request.
            // Restore the previous verifier if the request itself was rejected,
            // otherwise an already-delivered recovery link would be invalidated.
            if let previousVerifier {
                try? authStorage.store(key: verifierKey, value: previousVerifier)
            } else {
                try? authStorage.remove(key: verifierKey)
            }
            throw error
        }
    }

    func changePassword(_ password: String) async throws {
        _ = try await client.auth.update(user: UserAttributes(password: password))
        try? authStorage.remove(key: Self.passwordResetRequestedAtKey)
    }

    private func isPasswordRecovery(_ url: URL) -> Bool {
        URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?
            .contains(where: { $0.name == "flow" && $0.value == "recovery" }) == true
    }

    private func lastPasswordResetRequest() -> Date? {
        guard let data = try? authStorage.retrieve(key: Self.passwordResetRequestedAtKey),
              let value = String(data: data, encoding: .utf8),
              let timestamp = TimeInterval(value) else { return nil }
        return Date(timeIntervalSince1970: timestamp)
    }

    private func recordPasswordResetRequest() {
        let value = Data(String(Date().timeIntervalSince1970).utf8)
        try? authStorage.store(key: Self.passwordResetRequestedAtKey, value: value)
    }

    func logout() async throws { try await client.auth.signOut() }

    func fetchFeatureFlags() async throws -> AppFeatureFlags {
        let rows: [RemoteFeatureFlag] = try await request(
            path: ["rest", "v1", "app_feature_flags"],
            query: [
                URLQueryItem(name: "select", value: "key,enabled"),
                URLQueryItem(name: "order", value: "key.asc")
            ]
        )
        return AppFeatureFlags(rows: rows)
    }

    func deleteAccount() async throws {
        try await client.functions.invoke("delete-account")
    }

    func fetchProfile(ownerID: UUID, profileID: UUID? = nil,
                      preserving local: ContactProfile, loadPhoto: Bool = true) async throws -> (RemoteProfile, ContactProfile)? {
        try await requireOwnerSession(ownerID)
        var query = [
            URLQueryItem(name: "owner_id", value: "eq.\(ownerID.uuidString.lowercased())"),
            URLQueryItem(name: "select", value: "id,owner_id,slug,display_name,job_title,company,bio,public_email,phone,website,address,is_public,custom_domain,custom_domain_verified,created_at,updated_at,is_primary,avatar_url,appearance,theme,accent_color"),
            URLQueryItem(name: "limit", value: "1")
        ]
        if let profileID {
            query.append(URLQueryItem(name: "id", value: "eq.\(profileID.uuidString.lowercased())"))
        } else {
            query.append(URLQueryItem(name: "is_primary", value: "eq.true"))
        }
        let rows: [RemoteProfile] = try await request(path: ["rest", "v1", "profiles"], query: query)
        guard var remote = rows.first else { return nil }
        guard remote.ownerID == ownerID, profileID == nil || remote.id == profileID else {
            throw CloudError.accountMismatch
        }
        let profile = try await hydrate(remote: &remote, preserving: local, loadPhoto: loadPhoto)
        try await requireOwnerSession(ownerID)
        return (remote, profile)
    }

    /// Loads every card owned by the active account. The explicit owner filter,
    /// RLS, and the response-side owner check are all intentional: no public or
    /// previously cached card from another account may enter the local catalog.
    func fetchProfiles(ownerID: UUID,
                       preserving local: [UUID: ContactProfile] = [:]) async throws -> [(RemoteProfile, ContactProfile)] {
        try await requireOwnerSession(ownerID)
        let query = [
            URLQueryItem(name: "owner_id", value: "eq.\(ownerID.uuidString.lowercased())"),
            URLQueryItem(name: "select", value: "id,owner_id,slug,display_name,job_title,company,bio,public_email,phone,website,address,is_public,custom_domain,custom_domain_verified,created_at,updated_at,is_primary,avatar_url,appearance,theme,accent_color"),
            URLQueryItem(name: "order", value: "is_primary.desc,created_at.asc,id.asc")
        ]
        let rows: [RemoteProfile] = try await request(path: ["rest", "v1", "profiles"], query: query)
        guard rows.allSatisfy({ $0.ownerID == ownerID }) else { throw CloudError.accountMismatch }

        var values: [(RemoteProfile, ContactProfile)] = []
        values.reserveCapacity(rows.count)
        for row in rows {
            var remote = row
            let profile = try await hydrate(
                remote: &remote,
                preserving: local[row.id] ?? ContactProfile(),
                loadPhoto: true
            )
            values.append((remote, profile))
        }
        try await requireOwnerSession(ownerID)
        return values
    }

    private func hydrate(remote: inout RemoteProfile, preserving local: ContactProfile,
                         loadPhoto: Bool) async throws -> ContactProfile {
        let links: [RemoteLink] = try await request(path: ["rest", "v1", "social_links"], query: [
            URLQueryItem(name: "profile_id", value: "eq.\(remote.id.uuidString.lowercased())"),
            URLQueryItem(name: "select", value: "id,platform,url")
        ])
        var profile = local
        profile.fullName = remote.displayName
        profile.jobTitle = remote.jobTitle
        profile.company = remote.company
        profile.bio = remote.bio
        profile.email = remote.publicEmail
        profile.phone = remote.phone
        profile.website = remote.website
        profile.address = remote.address
        for platform in SocialPlatform.allCases {
            let url = links.first(where: { $0.platform == platform.rawValue })?.url ?? ""
            remote.setSocialURL(url, for: platform)
            profile.setSocialURL(url, for: platform)
        }
        profile.publicSlug = remote.slug
        profile.isPublic = remote.isPublic
        profile.customDomain = remote.customDomain ?? ""
        profile.customDomainVerified = remote.customDomainVerified == true
        if loadPhoto, let avatar = remote.avatarURL {
            profile.photoBase64 = try await readProfilePhoto(avatar)
            profile.photoSyncInitialized = true
        } else if loadPhoto && local.photoSyncInitialized {
            // A later deletion from the web must not resurrect an old local image.
            profile.photoBase64 = ""
        }
        if let logo = remote.appearance?.logoURL {
            profile.logoBase64 = try await readProfilePhoto(logo)
            profile.logoSyncInitialized = true
        } else if local.logoSyncInitialized {
            profile.logoBase64 = ""
            profile.logoSyncInitialized = true
        }
        return profile
    }

    func createProfile(ownerID: UUID, profileID: UUID, profile: ContactProfile) async throws -> RemoteProfile {
        try await requireOwnerSession(ownerID)
        let candidates = ProfileSlug.creationCandidates(
            requested: profile.publicSlug,
            displayName: profile.displayName,
            ownerID: ownerID
        )
        for (index, slug) in candidates.enumerated() {
            do {
                return try await insertProfile(ownerID: ownerID, profileID: profileID,
                                               profile: profile, slug: slug)
            } catch let error as CloudError {
                guard error.isUniqueConstraintViolation, index < candidates.count - 1 else { throw error }
            }
        }
        throw CloudError.emptyResponse
    }

    private func insertProfile(ownerID: UUID, profileID: UUID,
                               profile: ContactProfile, slug: String) async throws -> RemoteProfile {
        let payload = try await write(
            profile, profileID: profileID, ownerID: ownerID, storageOwnerID: ownerID, slug: slug
        )
        let rows: [RemoteProfile] = try await request(path: ["rest", "v1", "profiles"], method: "POST",
            query: [URLQueryItem(name: "select", value: "id,owner_id,slug,display_name,job_title,company,bio,public_email,phone,website,address,is_public,custom_domain,custom_domain_verified,created_at,updated_at,is_primary,avatar_url,appearance,theme,accent_color")],
            body: payload, prefer: "return=representation")
        guard let remote = rows.first else { throw CloudError.emptyResponse }
        guard remote.ownerID == ownerID, remote.id == profileID else { throw CloudError.accountMismatch }
        try await requireOwnerSession(ownerID)
        return remote
    }

    func updateProfile(ownerID: UUID, profileID: UUID, profile: ContactProfile,
                       expectedUpdatedAt: String, previousAppearance: ProfileAppearance? = nil,
                       previousLogoBase64: String = "", theme: String = "midnight") async throws -> RemoteProfile? {
        try await requireOwnerSession(ownerID)
        let payload = try await write(profile, profileID: nil, ownerID: nil, storageOwnerID: ownerID,
                                      slug: profile.publicSlug,
                                      previousAppearance: previousAppearance,
                                      previousLogoBase64: previousLogoBase64, theme: theme)
        let rows: [RemoteProfile] = try await request(path: ["rest", "v1", "profiles"], method: "PATCH", query: [
            URLQueryItem(name: "id", value: "eq.\(profileID.uuidString.lowercased())"),
            URLQueryItem(name: "owner_id", value: "eq.\(ownerID.uuidString.lowercased())"),
            URLQueryItem(name: "updated_at", value: "eq.\(expectedUpdatedAt)"),
            URLQueryItem(name: "select", value: "id,owner_id,slug,display_name,job_title,company,bio,public_email,phone,website,address,is_public,custom_domain,custom_domain_verified,created_at,updated_at,is_primary,avatar_url,appearance,theme,accent_color")
        ], body: payload, prefer: "return=representation")
        guard let remote = rows.first else { return nil }
        guard remote.ownerID == ownerID, remote.id == profileID else { throw CloudError.accountMismatch }
        try await requireOwnerSession(ownerID)
        return remote
    }

    private func write(_ profile: ContactProfile, profileID: UUID?, ownerID: UUID?,
                       storageOwnerID: UUID, slug: String,
                       previousAppearance: ProfileAppearance? = nil, previousLogoBase64: String = "",
                       theme: String = "midnight") async throws -> ProfileWrite {
        let p = profile.normalized
        var appearance = previousAppearance
        if p.logoBase64 != previousLogoBase64 {
            if p.logoBase64.isEmpty {
                if appearance != nil { appearance?.logoURL = nil }
            } else {
                guard let data = Data(base64Encoded: p.logoBase64), data.count <= 256 * 1024,
                      data.starts(with: [0xff, 0xd8, 0xff])
                else { throw ProfileError.invalidPhoto }
                let path = "\(storageOwnerID.uuidString.lowercased())/logo-\(UUID().uuidString.lowercased()).jpg"
                try await client.storage.from("avatars").upload(
                    path: path, file: data,
                    options: FileOptions(cacheControl: "3600", contentType: "image/jpeg", upsert: false)
                )
                let url = try client.storage.from("avatars").getPublicURL(path: path)
                appearance = appearance ?? ProfileAppearance.preset(theme)
                appearance?.logoURL = url.absoluteString
            }
        }
        return ProfileWrite(id: profileID, ownerID: ownerID, slug: slug, displayName: p.displayName,
                            jobTitle: p.jobTitle, company: p.company, bio: p.bio, publicEmail: p.email,
                            phone: p.phone, website: p.website, address: p.address,
                            isPublic: p.isPublic,
                            customDomain: p.customDomain.isEmpty ? nil : p.customDomain,
                            avatarURL: try ProfilePhoto.inlineURL(p.photoBase64), appearance: appearance)
    }

    func syncSocialProfiles(ownerID: UUID, profileID: UUID,
                            value: ContactProfile, expected: ContactProfile) async throws {
        try await requireOwnerSession(ownerID)
        let existing: [RemoteLink] = try await request(path: ["rest", "v1", "social_links"], query: [
            URLQueryItem(name: "profile_id", value: "eq.\(profileID.uuidString.lowercased())"),
            URLQueryItem(name: "select", value: "id,platform,url")
        ])
        for platform in SocialPlatform.allCases {
            let current = existing.first(where: { $0.platform == platform.rawValue })?.url ?? ""
            guard current == expected.socialURL(for: platform) else { throw CloudError.profileConflict }
        }

        for platform in SocialPlatform.allCases {
            let current = existing.first(where: { $0.platform == platform.rawValue })
            let desiredURL = value.socialURL(for: platform)
            let expectedURL = expected.socialURL(for: platform)
            if current?.url == desiredURL { continue }
            if desiredURL.isEmpty {
                if let id = current?.id {
                    let _: EmptyResponse = try await request(path: ["rest", "v1", "social_links"], method: "DELETE",
                        query: [URLQueryItem(name: "id", value: "eq.\(id.uuidString.lowercased())"),
                                URLQueryItem(name: "url", value: "eq.\(expectedURL)")], prefer: "return=minimal")
                }
            } else if let id = current?.id {
                let _: EmptyResponse = try await request(path: ["rest", "v1", "social_links"], method: "PATCH",
                    query: [URLQueryItem(name: "id", value: "eq.\(id.uuidString.lowercased())"),
                            URLQueryItem(name: "url", value: "eq.\(expectedURL)")],
                    body: ["url": desiredURL], prefer: "return=minimal")
            } else {
                let payload = LinkWrite(profileID: profileID, platform: platform.rawValue,
                                        label: platform.label, url: desiredURL,
                                        sortOrder: platform.sortOrder)
                let _: EmptyResponse = try await request(path: ["rest", "v1", "social_links"], method: "POST",
                    body: payload, prefer: "return=minimal")
            }
        }
        try await requireOwnerSession(ownerID)
    }

    func deleteProfile(ownerID: UUID, profileID: UUID) async throws -> Bool {
        try await requireOwnerSession(ownerID)
        let rows: [DeletedProfile] = try await request(
            path: ["rest", "v1", "profiles"],
            method: "DELETE",
            query: [
                URLQueryItem(name: "id", value: "eq.\(profileID.uuidString.lowercased())"),
                URLQueryItem(name: "owner_id", value: "eq.\(ownerID.uuidString.lowercased())"),
                URLQueryItem(name: "select", value: "id")
            ],
            prefer: "return=representation"
        )
        guard rows.count <= 1, rows.first?.id == profileID || rows.isEmpty else {
            throw CloudError.accountMismatch
        }
        try await requireOwnerSession(ownerID)
        return rows.first?.id == profileID
    }

    private func readProfilePhoto(_ avatar: String) async throws -> String {
        if avatar.isEmpty { return "" }
        if avatar.hasPrefix("data:image/jpeg;base64,") {
            let base64 = String(avatar.dropFirst("data:image/jpeg;base64,".count))
            _ = try ProfilePhoto.inlineURL(base64)
            guard let bytes = Data(base64Encoded: base64),
                  let source = CGImageSourceCreateWithData(bytes as CFData, nil),
                  CGImageSourceGetCount(source) == 1,
                  let info = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
                  let width = info[kCGImagePropertyPixelWidth] as? NSNumber,
                  let height = info[kCGImagePropertyPixelHeight] as? NSNumber,
                  width.doubleValue * height.doubleValue <= 16_000_000,
                  CGImageSourceCreateThumbnailAtIndex(source, 0, [
                    kCGImageSourceCreateThumbnailFromImageAlways: true,
                    kCGImageSourceThumbnailMaxPixelSize: 512
                  ] as CFDictionary) != nil else { throw ProfileError.invalidPhoto }
            return base64
        }
        guard let url = ProfilePhoto.trustedStorageURL(avatar, origin: configuration.supabaseURL)
        else { throw ProfileError.invalidPhoto }
        var request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 8)
        request.setValue("image/jpeg,image/png,image/webp", forHTTPHeaderField: "Accept")
        let (stream, response) = try await http.bytes(for: request, delegate: PhotoRedirectBlocker())
        guard let response = response as? HTTPURLResponse, response.statusCode == 200,
              response.expectedContentLength <= 3 * 1024 * 1024 else { throw ProfileError.invalidPhoto }
        var bytes = Data()
        for try await byte in stream {
            if bytes.count >= 3 * 1024 * 1024 { throw ProfileError.invalidPhoto }
            bytes.append(byte)
        }
        try Task.checkCancellation()
        guard let source = CGImageSourceCreateWithData(bytes as CFData, nil),
              CGImageSourceGetCount(source) == 1,
              let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              let width = properties[kCGImagePropertyPixelWidth] as? NSNumber,
              let height = properties[kCGImagePropertyPixelHeight] as? NSNumber,
              width.doubleValue * height.doubleValue <= 16_000_000 else { throw ProfileError.invalidPhoto }
        // Keep our own small JPEG unchanged so a later save does not upload a
        // second, visually identical logo and conflict with the web revision.
        if bytes.count <= 256 * 1024 && bytes.starts(with: [0xff, 0xd8, 0xff]) {
            return bytes.base64EncodedString()
        }
        guard
              let thumbnail = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: 512
              ] as CFDictionary),
              let jpeg = UIImage(cgImage: thumbnail).jpegData(compressionQuality: 0.8),
              jpeg.count <= 256 * 1024 else { throw ProfileError.invalidPhoto }
        return jpeg.base64EncodedString()
    }

    private func request<Response: Decodable>(path: [String], method: String = "GET",
                                              query: [URLQueryItem] = [], body: Encodable? = nil,
                                              prefer: String? = nil) async throws -> Response {
        let session = try await validSession()
        guard let finalURL = RESTURLBuilder.make(baseURL: configuration.supabaseURL,
                                                 path: path, query: query) else {
            throw CloudError.invalidRequest
        }
        var request = URLRequest(url: finalURL)
        request.httpMethod = method
        request.setValue(configuration.publishableKey, forHTTPHeaderField: "apikey")
        request.setValue("Bearer \(session.accessToken)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let prefer { request.setValue(prefer, forHTTPHeaderField: "Prefer") }
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONEncoder().encode(AnyEncodable(body))
        }
        let (data, response) = try await http.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse,
              (200..<300).contains(httpResponse.statusCode) else {
            let payload = try? JSONDecoder().decode(PostgRESTErrorPayload.self, from: data)
            throw CloudError.server(status: (response as? HTTPURLResponse)?.statusCode ?? 0,
                                    code: payload?.code)
        }
        if Response.self == EmptyResponse.self, data.isEmpty {
            return EmptyResponse() as! Response
        }
        return try JSONDecoder().decode(Response.self, from: data)
    }
}

private struct EmptyResponse: Codable { init() {} }

private struct AnyEncodable: Encodable {
    let encodeValue: (Encoder) throws -> Void
    init(_ value: Encodable) { encodeValue = value.encode }
    func encode(to encoder: Encoder) throws { try encodeValue(encoder) }
}

enum CloudError: LocalizedError {
    case invalidCallback, invalidRequest, emptyResponse, emailConfirmationDisabled, providerUnavailable
    case profileConflict, accountMismatch
    case passwordResetCooldown(Int)
    case server(status: Int, code: String?)

    var isUniqueConstraintViolation: Bool {
        guard case .server(let status, let code) = self else { return false }
        return status == 409 && code == "23505"
    }

    var errorDescription: String? {
        switch self {
        case .invalidCallback: return "A bejelentkezési hivatkozás érvénytelen."
        case .profileConflict: return "A profil közben másik eszközön megváltozott. Válaszd ki a megtartandó változatot."
        case .accountMismatch: return "A munkamenet megváltozott. A névjegyadatokat biztonsági okból nem töltöttük be."
        case .invalidRequest: return "A kérés most nem küldhető el. Próbáld újra."
        case .emptyResponse: return "A kiszolgáló nem adott vissza mentett profilt."
        case .emailConfirmationDisabled: return "A kiszolgálón nincs kötelező e-mail-megerősítés. A munkamenetet biztonsági okból megszakítottuk."
        case .providerUnavailable: return "A Google-bejelentkezés ebben a verzióban nem érhető el."
        case .passwordResetCooldown(let seconds):
            return "Már kértél visszaállító levelet. Várj még \(seconds) másodpercet, vagy nyisd meg a legutóbbi levelet."
        case .server(let status, let code):
            if status == 409, code == "23505" {
                return "A választott nyilvános profilazonosító már foglalt. Válassz másikat."
            }
            let diagnostic = code.map { ", kód: \($0)" } ?? ""
            return "A VIZIT kiszolgáló elutasította a kérést (HTTP \(status)\(diagnostic))."
        }
    }
}


private final class PhotoRedirectBlocker: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    func urlSession(_ session: URLSession, task: URLSessionTask,
                    willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest,
                    completionHandler: @escaping (URLRequest?) -> Void) {
        completionHandler(nil)
    }
}
