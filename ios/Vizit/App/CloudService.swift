import AuthenticationServices
import Foundation
import Supabase

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
    let updatedAt: String

    enum CodingKeys: String, CodingKey {
        case id, slug, phone, website, address
        case ownerID = "owner_id"
        case displayName = "display_name"
        case jobTitle = "job_title"
        case company
        case publicEmail = "public_email"
        case isPublic = "is_public"
        case updatedAt = "updated_at"
    }
}

private struct RemoteLink: Decodable {
    let id: UUID
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

    enum CodingKeys: String, CodingKey {
        case id, slug, phone, website, address, company
        case ownerID = "owner_id"
        case displayName = "display_name"
        case jobTitle = "job_title"
        case publicEmail = "public_email"
        case isPublic = "is_public"
    }
}

private struct LinkWrite: Encodable {
    let profileID: UUID
    let platform = "linkedin"
    let label = "LinkedIn"
    let url: String
    let sortOrder = 0
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

    func fetchProfile(ownerID: UUID, preserving local: ContactProfile) async throws -> (RemoteProfile, ContactProfile)? {
        let query = [
            URLQueryItem(name: "owner_id", value: "eq.\(ownerID.uuidString.lowercased())"),
            URLQueryItem(name: "select", value: "id,owner_id,slug,display_name,job_title,company,public_email,phone,website,address,is_public,updated_at"),
            URLQueryItem(name: "limit", value: "1")
        ]
        let rows: [RemoteProfile] = try await request(path: ["rest", "v1", "profiles"], query: query)
        guard let remote = rows.first else { return nil }
        let links: [RemoteLink] = try await request(path: ["rest", "v1", "social_links"], query: [
            URLQueryItem(name: "profile_id", value: "eq.\(remote.id.uuidString.lowercased())"),
            URLQueryItem(name: "platform", value: "eq.linkedin"),
            URLQueryItem(name: "select", value: "id,url"),
            URLQueryItem(name: "limit", value: "1")
        ])
        var profile = local
        profile.fullName = remote.displayName
        profile.jobTitle = remote.jobTitle
        profile.company = remote.company
        profile.email = remote.publicEmail
        profile.phone = remote.phone
        profile.website = remote.website
        profile.address = remote.address
        profile.linkedIn = links.first?.url ?? ""
        profile.publicSlug = remote.slug
        profile.isPublic = remote.isPublic
        return (remote, profile)
    }

    func createProfile(ownerID: UUID, profileID: UUID, profile: ContactProfile) async throws -> RemoteProfile {
        let candidates = ProfileSlug.creationCandidates(requested: profile.publicSlug, ownerID: ownerID)
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
        let payload = write(profile, profileID: profileID, ownerID: ownerID, slug: slug)
        let rows: [RemoteProfile] = try await request(path: ["rest", "v1", "profiles"], method: "POST",
            query: [URLQueryItem(name: "select", value: "id,owner_id,slug,display_name,job_title,company,public_email,phone,website,address,is_public,updated_at")],
            body: payload, prefer: "return=representation")
        guard let remote = rows.first else { throw CloudError.emptyResponse }
        return remote
    }

    func updateProfile(ownerID: UUID, profileID: UUID, profile: ContactProfile,
                       expectedUpdatedAt: String) async throws -> RemoteProfile? {
        let payload = write(profile, profileID: nil, ownerID: nil, slug: profile.publicSlug)
        let rows: [RemoteProfile] = try await request(path: ["rest", "v1", "profiles"], method: "PATCH", query: [
            URLQueryItem(name: "id", value: "eq.\(profileID.uuidString.lowercased())"),
            URLQueryItem(name: "owner_id", value: "eq.\(ownerID.uuidString.lowercased())"),
            URLQueryItem(name: "updated_at", value: "eq.\(expectedUpdatedAt)"),
            URLQueryItem(name: "select", value: "id,owner_id,slug,display_name,job_title,company,public_email,phone,website,address,is_public,updated_at")
        ], body: payload, prefer: "return=representation")
        guard let remote = rows.first else { return nil }
        return remote
    }

    private func write(_ profile: ContactProfile, profileID: UUID?, ownerID: UUID?, slug: String) -> ProfileWrite {
        let p = profile.normalized
        return ProfileWrite(id: profileID, ownerID: ownerID, slug: slug, displayName: p.displayName,
                            jobTitle: p.jobTitle, company: p.company, publicEmail: p.email,
                            phone: p.phone, website: p.website, address: p.address,
                            isPublic: p.isPublic)
    }

    func syncLinkedIn(profileID: UUID, value: String) async throws {
        let baseQuery = [
            URLQueryItem(name: "profile_id", value: "eq.\(profileID.uuidString.lowercased())"),
            URLQueryItem(name: "platform", value: "eq.linkedin")
        ]
        let existing: [RemoteLink] = try await request(path: ["rest", "v1", "social_links"], query:
            baseQuery + [URLQueryItem(name: "select", value: "id,url"), URLQueryItem(name: "limit", value: "1")])
        if value.isEmpty {
            if let id = existing.first?.id {
                let _: EmptyResponse = try await request(path: ["rest", "v1", "social_links"], method: "DELETE",
                    query: [URLQueryItem(name: "id", value: "eq.\(id.uuidString.lowercased())")], prefer: "return=minimal")
            }
        } else if let id = existing.first?.id {
            let _: EmptyResponse = try await request(path: ["rest", "v1", "social_links"], method: "PATCH",
                query: [URLQueryItem(name: "id", value: "eq.\(id.uuidString.lowercased())")],
                body: ["url": value], prefer: "return=minimal")
        } else {
            let _: EmptyResponse = try await request(path: ["rest", "v1", "social_links"], method: "POST",
                body: LinkWrite(profileID: profileID, url: value), prefer: "return=minimal")
        }
    }

    private func request<Response: Decodable>(path: [String], method: String = "GET",
                                              query: [URLQueryItem] = [], body: Encodable? = nil,
                                              prefer: String? = nil) async throws -> Response {
        let session = try await validSession()
        var url = configuration.supabaseURL
        path.forEach { url.appendPathComponent($0) }
        var parts = URLComponents(url: url, resolvingAgainstBaseURL: false)
        parts?.queryItems = query.isEmpty ? nil : query
        guard let finalURL = parts?.url else { throw CloudError.invalidRequest }
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
    case passwordResetCooldown(Int)
    case server(status: Int, code: String?)

    var isUniqueConstraintViolation: Bool {
        guard case .server(let status, let code) = self else { return false }
        return status == 409 && code == "23505"
    }

    var errorDescription: String? {
        switch self {
        case .invalidCallback: return "A bejelentkezési hivatkozás nem a VIZIT biztonságos visszahívási címe."
        case .invalidRequest: return "A kiszolgáló kérése nem állítható össze biztonságosan."
        case .emptyResponse: return "A kiszolgáló nem adott vissza mentett profilt."
        case .emailConfirmationDisabled: return "A kiszolgálón nincs kötelező e-mail-megerősítés. A munkamenetet biztonsági okból megszakítottuk."
        case .providerUnavailable: return "A Google-bejelentkezés ezen a biztonságos builden nincs engedélyezve."
        case .passwordResetCooldown(let seconds):
            return "Már kértél visszaállító levelet. Várj még \(seconds) másodpercet, vagy nyisd meg a legutóbbi levelet."
        case .server(let status, let code):
            if status == 409, code == "23505" {
                return "A választott nyilvános profilazonosító már foglalt. Válassz másikat."
            }
            return "A VIZIT kiszolgáló elutasította a kérést (HTTP \(status))."
        }
    }
}
