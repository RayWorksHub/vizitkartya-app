import AuthenticationServices
import Foundation
import Supabase
import UIKit
import ImageIO
import CryptoKit

struct RemoteProfile: Decodable, Sendable {
    let id: UUID
    let ownerID: UUID
    let slug: String
    let displayName: String
    let jobTitle: String
    let company: String
    let publicEmail: String
    let phone: String
    let website: String
    let address: String
    let isPublic: Bool
    let customDomain: String?
    let customDomainVerified: Bool?
    let updatedAt: String
    let avatarURL: String?
    var linkedIn = ""
    var facebook = ""
    var instagram = ""
    var tiktok = ""
    var youtube = ""

    var fingerprint: String {
        let values = [id.uuidString, ownerID.uuidString, slug, displayName, jobTitle, company,
                      publicEmail, phone, website, address, String(isPublic), customDomain ?? "",
                      String(customDomainVerified == true), avatarURL ?? ""]
            + SocialPlatform.allCases.map { socialURL(for: $0) }
        let bytes = (try? JSONEncoder().encode(values)) ?? Data()
        return SHA256.hash(data: bytes).map { String(format: "%02x", $0) }.joined()
    }

    func matches(_ value: ContactProfile) -> Bool {
        let p = value.normalized
        return displayName == p.displayName && slug == p.publicSlug && jobTitle == p.jobTitle &&
            company == p.company && publicEmail == p.email && phone == p.phone && website == p.website &&
            address == p.address && isPublic == p.isPublic &&
            (customDomain ?? "") == p.customDomain &&
            SocialPlatform.allCases.allSatisfy { socialURL(for: $0) == p.socialURL(for: $0) } &&
            (avatarURL ?? "") == ((try? ProfilePhoto.inlineURL(p.photoBase64)) ?? "invalid-photo")
    }

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
        case company
        case publicEmail = "public_email"
        case isPublic = "is_public"
        case customDomain = "custom_domain"
        case customDomainVerified = "custom_domain_verified"
        case avatarURL = "avatar_url"
        case updatedAt = "updated_at"
    }
}

private struct RemoteLink: Decodable {
    let id: UUID
    let platform: String
    let url: String
}

private struct ProfileWrite: Encodable {
    let id: UUID?
    let ownerID: UUID?
    let slug: String
    let displayName: String
    let jobTitle: String
    let company: String
    let publicEmail: String
    let phone: String
    let website: String
    let address: String
    let isPublic: Bool
    let customDomain: String?
    let avatarURL: String

    enum CodingKeys: String, CodingKey {
        case id, slug, phone, website, address, company
        case ownerID = "owner_id"
        case displayName = "display_name"
        case jobTitle = "job_title"
        case publicEmail = "public_email"
        case isPublic = "is_public"
        case customDomain = "custom_domain"
        case avatarURL = "avatar_url"
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

    func login(email: String, password: String) async throws -> Session {
        try await client.auth.signIn(email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password)
    }

    func register(name: String, email: String, password: String) async throws {
        let response = try await client.auth.signUp(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines),
            password: password,
            data: [
                "display_name": .string(name.trimmingCharacters(in: .whitespacesAndNewlines)),
                "privacy_version": .string(configuration.privacyPolicyVersion),
                "terms_version": .string(configuration.termsVersion)
            ],
            redirectTo: configuration.callbackURL
        )
        // DEV is required to enforce email confirmation. A session here means
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

    func changeEmail(_ email: String) async throws {
        _ = try await client.auth.update(user: UserAttributes(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines)
        ))
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

    func deleteAccount() async throws {
        try await client.functions.invoke("delete-account")
    }

    func fetchProfile(ownerID: UUID, preserving local: ContactProfile, loadPhoto: Bool = true) async throws -> (RemoteProfile, ContactProfile)? {
        let query = [
            URLQueryItem(name: "owner_id", value: "eq.\(ownerID.uuidString.lowercased())"),
            URLQueryItem(name: "select", value: "id,owner_id,slug,display_name,job_title,company,public_email,phone,website,address,is_public,custom_domain,custom_domain_verified,updated_at,avatar_url"),
            URLQueryItem(name: "limit", value: "1")
        ]
        let rows: [RemoteProfile] = try await request(path: ["rest", "v1", "profiles"], query: query)
        guard var remote = rows.first else { return nil }
        let links: [RemoteLink] = try await request(path: ["rest", "v1", "social_links"], query: [
            URLQueryItem(name: "profile_id", value: "eq.\(remote.id.uuidString.lowercased())"),
            URLQueryItem(name: "select", value: "id,platform,url")
        ])
        var profile = local
        profile.fullName = remote.displayName
        profile.jobTitle = remote.jobTitle
        profile.company = remote.company
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
        return (remote, profile)
    }

    func createProfile(ownerID: UUID, profileID: UUID, profile: ContactProfile) async throws -> RemoteProfile {
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
        let payload = try write(profile, profileID: profileID, ownerID: ownerID, slug: slug)
        let rows: [RemoteProfile] = try await request(path: ["rest", "v1", "profiles"], method: "POST",
            query: [URLQueryItem(name: "select", value: "id,owner_id,slug,display_name,job_title,company,public_email,phone,website,address,is_public,custom_domain,custom_domain_verified,updated_at,avatar_url")],
            body: payload, prefer: "return=representation")
        guard let remote = rows.first else { throw CloudError.emptyResponse }
        return remote
    }

    func updateProfile(ownerID: UUID, profileID: UUID, profile: ContactProfile,
                       expectedUpdatedAt: String) async throws -> RemoteProfile? {
        let payload = try write(profile, profileID: nil, ownerID: nil, slug: profile.publicSlug)
        let rows: [RemoteProfile] = try await request(path: ["rest", "v1", "profiles"], method: "PATCH", query: [
            URLQueryItem(name: "id", value: "eq.\(profileID.uuidString.lowercased())"),
            URLQueryItem(name: "owner_id", value: "eq.\(ownerID.uuidString.lowercased())"),
            URLQueryItem(name: "updated_at", value: "eq.\(expectedUpdatedAt)"),
            URLQueryItem(name: "select", value: "id,owner_id,slug,display_name,job_title,company,public_email,phone,website,address,is_public,custom_domain,custom_domain_verified,updated_at,avatar_url")
        ], body: payload, prefer: "return=representation")
        guard let remote = rows.first else { return nil }
        return remote
    }

    private func write(_ profile: ContactProfile, profileID: UUID?, ownerID: UUID?, slug: String) throws -> ProfileWrite {
        let p = profile.normalized
        return ProfileWrite(id: profileID, ownerID: ownerID, slug: slug, displayName: p.displayName,
                            jobTitle: p.jobTitle, company: p.company, publicEmail: p.email,
                            phone: p.phone, website: p.website, address: p.address,
                            isPublic: p.isPublic,
                            customDomain: p.customDomain.isEmpty ? nil : p.customDomain,
                            avatarURL: try ProfilePhoto.inlineURL(p.photoBase64))
    }

    func syncSocialProfiles(profileID: UUID, value: ContactProfile, expected: ContactProfile) async throws {
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
              width.doubleValue * height.doubleValue <= 16_000_000,
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
    case profileConflict
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
