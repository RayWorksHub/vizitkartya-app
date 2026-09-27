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
    var serverFingerprint = ""
    var socialLinks: [RemoteLink] = []
    var bio = ""
    var theme = "midnight"
    var accentColor = "#0b5ce8"
    var linkedIn = ""
    var facebook = ""
    var instagram = ""
    var tiktok = ""
    var youtube = ""

    var fingerprint: String { serverFingerprint }

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
        case socialLinks = "social_links"
        case bio, theme
        case accentColor = "accent_color"
    }
}

struct RemoteLink: Decodable, Sendable {
    let id: UUID
    let platform: String
    let label: String
    let url: String
    let enabled: Bool
    let sortOrder: Int

    enum CodingKeys: String, CodingKey {
        case id, platform, label, url, enabled
        case sortOrder = "sort_order"
    }
}

private struct NodeProfileEnvelope: Decodable {
    let profile: RemoteProfile?
    let fingerprint: String?
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
    private static let authStorageKey = "vizit-auth-session-node-v1"
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
        let response = try await publicNodeRequest(path: ["api", "auth", "register"], body: [
            "name": name.trimmingCharacters(in: .whitespacesAndNewlines),
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines),
            "password": password, "consent": true
        ])
        guard let json = try? JSONSerialization.jsonObject(with: response) as? [String: Any],
              json["requires_email_confirmation"] as? Bool == true else {
            throw CloudError.emailConfirmationDisabled
        }
    }

    func resendSignupVerification(email: String) async throws {
        let normalizedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        guard AuthValidation.email(normalizedEmail) == nil else { throw CloudError.invalidRequest }
        let url = configuration.supabaseURL
            .appendingPathComponent("auth", isDirectory: true)
            .appendingPathComponent("v1", isDirectory: true)
            .appendingPathComponent("resend", isDirectory: false)
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(configuration.publishableKey, forHTTPHeaderField: "apikey")
        request.httpBody = try JSONEncoder().encode([
            "type": "signup",
            "email": normalizedEmail,
        ])

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await http.data(for: request)
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            throw CloudError.networkUnavailable
        }
        guard let httpResponse = response as? HTTPURLResponse,
              (200..<300).contains(httpResponse.statusCode) else {
            let payload = try? JSONDecoder().decode(PostgRESTErrorPayload.self, from: data)
            throw CloudError.server(
                status: (response as? HTTPURLResponse)?.statusCode ?? 0,
                code: payload?.code
            )
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
        _ = try await publicNodeRequest(path: ["api", "auth", "reset-password"], body: [
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines)
        ])
        recordPasswordResetRequest()
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
        let _: EmptyResponse = try await nodeRequest(path: ["api", "account"], method: "DELETE")
    }

    func analyticsSummary() async throws -> AnalyticsSummary {
        try await nodeRequest(path: ["api", "analytics", "summary"])
    }

    func accountExport() async throws -> Data {
        let data = try await nodeData(path: ["api", "account"])
        guard (try? JSONSerialization.jsonObject(with: data)) is [String: Any] else {
            throw CloudError.invalidResponse
        }
        return data
    }

    func fetchProfile(ownerID: UUID, preserving local: ContactProfile, loadPhoto: Bool = true) async throws -> (RemoteProfile, ContactProfile)? {
        let envelope: NodeProfileEnvelope = try await nodeRequest(path: ["api", "profile"])
        guard var remote = envelope.profile else { return nil }
        guard remote.ownerID == ownerID, let fingerprint = envelope.fingerprint,
              fingerprint.range(of: "^[a-f0-9]{64}$", options: .regularExpression) != nil
        else { throw CloudError.invalidResponse }
        remote.serverFingerprint = fingerprint
        var profile = local
        profile.fullName = remote.displayName
        profile.jobTitle = remote.jobTitle
        profile.company = remote.company
        profile.email = remote.publicEmail
        profile.phone = remote.phone
        profile.website = remote.website
        profile.address = remote.address
        for platform in SocialPlatform.allCases {
            let url = remote.socialLinks.first(where: { $0.platform == platform.rawValue })?.url ?? ""
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
            profile.photoBase64 = ""
        }
        return (remote, profile)
    }

    func saveProfile(ownerID: UUID, profile: ContactProfile,
                     previous: RemoteProfile?) async throws -> RemoteProfile {
        let candidates = previous.map { [$0.slug] } ?? ProfileSlug.creationCandidates(
            requested: profile.publicSlug, displayName: profile.displayName, ownerID: ownerID
        )
        for (index, slug) in candidates.enumerated() {
            do {
                return try await writeProfile(ownerID: ownerID, profile: profile, previous: previous, slug: slug)
            } catch let error as CloudError {
                guard previous == nil, case .server(let status, _) = error,
                      status == 409, index < candidates.count - 1 else { throw error }
            }
        }
        throw CloudError.emptyResponse
    }

    private func writeProfile(ownerID: UUID, profile: ContactProfile,
                              previous: RemoteProfile?, slug: String) async throws -> RemoteProfile {
        let value = profile.normalized
        let savedLinks = previous?.socialLinks ?? []
        var links = savedLinks.filter { link in
            !SocialPlatform.allCases.contains(where: { $0.rawValue == link.platform })
        }.map { link -> [String: Any] in
            ["id": link.id.uuidString.lowercased(), "platform": link.platform,
             "label": link.label, "url": link.url, "enabled": link.enabled]
        }
        for platform in SocialPlatform.allCases {
            let url = value.socialURL(for: platform)
            guard !url.isEmpty else { continue }
            let old = savedLinks.first(where: { $0.platform == platform.rawValue })
            var item: [String: Any] = ["platform": platform.rawValue,
                                       "label": old?.label ?? platform.label,
                                       "url": url, "enabled": old?.enabled ?? true]
            if let old { item["id"] = old.id.uuidString.lowercased() }
            links.append(item)
        }
        for index in links.indices { links[index]["sort_order"] = index }
        let photo = try ProfilePhoto.inlineURL(value.photoBase64)
        let body: [String: Any] = [
            "slug": slug, "display_name": value.displayName, "job_title": value.jobTitle,
            "company": value.company, "bio": previous?.bio ?? "",
            "public_email": value.email, "phone": value.phone, "website": value.website,
            "address": value.address, "avatar_url": photo,
            "theme": previous?.theme ?? "midnight",
            "accent_color": previous?.accentColor ?? "#0b5ce8",
            "is_public": value.isPublic, "custom_domain": value.customDomain,
            "social_links": links, "base_fingerprint": (previous?.fingerprint as Any?) ?? NSNull(),
            "base_updated_at": (previous?.updatedAt as Any?) ?? NSNull()
        ]
        guard JSONSerialization.isValidJSONObject(body) else { throw CloudError.invalidRequest }
        let data = try JSONSerialization.data(withJSONObject: body)
        let envelope: NodeProfileEnvelope = try await nodeRequest(
            path: ["api", "profile"], method: "PUT", body: data
        )
        guard var remote = envelope.profile, remote.ownerID == ownerID,
              let fingerprint = envelope.fingerprint,
              fingerprint.range(of: "^[a-f0-9]{64}$", options: .regularExpression) != nil
        else { throw CloudError.invalidResponse }
        remote.serverFingerprint = fingerprint
        for platform in SocialPlatform.allCases {
            remote.setSocialURL(remote.socialLinks.first(where: { $0.platform == platform.rawValue })?.url ?? "",
                                for: platform)
        }
        return remote
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

    private func publicNodeRequest(path: [String], body: [String: Any]) async throws -> Data {
        guard path == ["api", "auth", "register"] || path == ["api", "auth", "reset-password"],
              JSONSerialization.isValidJSONObject(body),
              let url = RESTURLBuilder.make(baseURL: configuration.backendURL, path: path, query: [])
        else { throw CloudError.invalidRequest }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        let result: (Data, URLResponse)
        do {
            result = try await http.data(for: request, delegate: PhotoRedirectBlocker())
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            throw CloudError.networkUnavailable
        }
        guard let response = result.1 as? HTTPURLResponse,
              result.0.count <= 2 * 1024 * 1024 else { throw CloudError.invalidResponse }
        guard (200..<300).contains(response.statusCode) else {
            let payload = try? JSONDecoder().decode(PostgRESTErrorPayload.self, from: result.0)
            throw CloudError.server(status: response.statusCode, code: payload?.code)
        }
        return result.0
    }

    private func nodeRequest<Response: Decodable>(
        path: [String], method: String = "GET", body: Data? = nil
    ) async throws -> Response {
        let data = try await nodeData(path: path, method: method, body: body)
        if Response.self == EmptyResponse.self, data.isEmpty {
            return EmptyResponse() as! Response
        }
        do {
            return try JSONDecoder().decode(Response.self, from: data)
        } catch {
            throw CloudError.invalidResponse
        }
    }

    private func nodeData(path: [String], method: String = "GET", body: Data? = nil) async throws -> Data {
        let session: Session
        do {
            session = try await validSession()
        } catch {
            throw CloudError.authenticationRequired
        }
        guard path == ["api", "profile"] || path == ["api", "account"] || path == ["api", "analytics", "summary"],
              let url = RESTURLBuilder.make(baseURL: configuration.backendURL, path: path, query: [])
        else { throw CloudError.invalidRequest }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("Bearer \(session.accessToken)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("no-store", forHTTPHeaderField: "Cache-Control")
        if let body {
            guard body.count <= 768 * 1024 else { throw CloudError.invalidRequest }
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = body
        }
        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await http.data(for: request, delegate: PhotoRedirectBlocker())
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            throw CloudError.networkUnavailable
        }
        guard let httpResponse = response as? HTTPURLResponse else { throw CloudError.invalidResponse }
        guard data.count <= 2 * 1024 * 1024 else { throw CloudError.invalidResponse }
        if httpResponse.statusCode == 409 {
            let conflict = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
            if conflict?["conflict"] as? Bool == true { throw CloudError.profileConflict }
        }
        if httpResponse.statusCode == 401 { throw CloudError.authenticationRequired }
        guard (200..<300).contains(httpResponse.statusCode) else {
            let payload = try? JSONDecoder().decode(PostgRESTErrorPayload.self, from: data)
            throw CloudError.server(status: httpResponse.statusCode, code: payload?.code)
        }
        return data
    }
}

private struct EmptyResponse: Codable { init() {} }

private struct AnyEncodable: Encodable {
    let encodeValue: (Encoder) throws -> Void
    init(_ value: Encodable) { encodeValue = value.encode }
    func encode(to encoder: Encoder) throws { try encodeValue(encoder) }
}

enum CloudError: LocalizedError {
    case invalidCallback, invalidRequest, invalidResponse, emptyResponse
    case authenticationRequired, networkUnavailable
    case emailConfirmationDisabled, providerUnavailable
    case profileConflict
    case passwordResetCooldown(Int)
    case server(status: Int, code: String?)

    var isUniqueConstraintViolation: Bool {
        guard case .server(let status, let code) = self else { return false }
        return status == 409 && code == "23505"
    }

    var isRetryable: Bool {
        switch self {
        case .networkUnavailable:
            return true
        case .server(let status, _):
            return status == 408 || status == 425 || status == 429 || (500...599).contains(status)
        default:
            return false
        }
    }

    var errorDescription: String? {
        switch self {
        case .invalidCallback: return "A bejelentkezési hivatkozás érvénytelen."
        case .profileConflict: return "A profil közben másik eszközön megváltozott. Válaszd ki a megtartandó változatot."
        case .invalidRequest: return "A kérés most nem küldhető el. Próbáld újra."
        case .invalidResponse: return "A kiszolgáló válasza nem olvasható. A helyi névjegyedet megőriztük."
        case .emptyResponse: return "A kiszolgáló nem adott vissza mentett profilt."
        case .authenticationRequired: return "A felhőmunkamenet lejárt. Jelentkezz be újra; a helyi névjegyedet megőriztük."
        case .networkUnavailable: return "Nincs elérhető hálózati kapcsolat. A módosítás helyben megmaradt, és automatikusan újrapróbáljuk."
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
