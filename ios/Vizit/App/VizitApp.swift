import SwiftUI
import Supabase

@main
struct VizitApp: App {
    @StateObject private var store = AppStore()
    @StateObject private var presentation = CardPresentationStore()

    var body: some Scene {
        WindowGroup {
            AppGate()
                .environmentObject(store)
                .environmentObject(presentation)
                .tint(VizitColor.primary)
                .onOpenURL { url in Task { await store.handleCallback(url) } }
        }
    }
}

enum AuthStatus: Equatable {
    case launching
    case signedOut
    case verificationSent(String)
    case authenticated
    case offline
    case passwordRecovery
    case configurationError(String)
}

enum SyncStatus: Equatable {
    case localOnly, syncing, synced, pending, conflict, failed

    var label: String {
        switch self {
        case .localOnly: return "Csak ezen a készüléken"
        case .syncing: return "Szinkronizálás…"
        case .synced: return "Szinkronizálva"
        case .pending: return "Helyben mentve – feltöltésre vár"
        case .conflict: return "Szinkronütközés – a helyi példányt megőriztük"
        case .failed: return "Helyben mentve – a szinkron nem sikerült"
        }
    }
}

enum ProfileLoadStatus: Equatable {
    case loading, ready, unavailable
}

@MainActor
final class AppStore: ObservableObject {
    @Published private(set) var profile = ContactProfile()
    @Published private(set) var authStatus: AuthStatus = .launching
    @Published private(set) var syncStatus: SyncStatus = .localOnly
    @Published private(set) var profileLoadStatus: ProfileLoadStatus = .loading
    @Published private(set) var featureFlags = AppFeatureFlags()
    @Published private(set) var storageError: String?
    @Published private(set) var message: String?
    @Published private(set) var busy = false
    @Published private(set) var accountProfiles: [AccountProfile] = []
    @Published private(set) var profileCatalogBusy = false
    @Published private(set) var creatingAdditionalProfile = false

    private(set) var configuration: AppConfiguration?
    private var cloud: CloudService?
    private var fileStore: ProfileFileStore?
    private var syncStore: ProfileSyncStore?
    private var userID: UUID?
    private var userEmail = ""
    private var profileRevision = 0
    private var syncInFlight = false
    private var syncAgain = false
    private var syncFailureMessage: String?
    private let uiTesting: Bool

    init() {
        #if DEBUG
        uiTesting = ProcessInfo.processInfo.arguments.contains("--ui-testing")
        #else
        uiTesting = false
        #endif
        if uiTesting {
            do {
                let directory = try Self.applicationDirectory().appendingPathComponent("UITests", isDirectory: true)
                let storage = ProfileFileStore(directory: directory)
                fileStore = storage
                syncStore = ProfileSyncStore(directory: directory)
                if ProcessInfo.processInfo.arguments.contains("--reset-test-profile") {
                    try storage.reset()
                    try? syncStore?.reset()
                }
                profile = try storage.load()
                authStatus = .authenticated
                syncStatus = .localOnly
                profileLoadStatus = .ready
            } catch {
                storageError = error.localizedDescription
                authStatus = .authenticated
            }
            return
        }

        do {
            let config = try AppConfiguration.load()
            configuration = config
            cloud = CloudService(configuration: config)
            Task { await bootstrap() }
        } catch {
            authStatus = .configurationError(error.localizedDescription)
        }
    }

    var hasProfile: Bool { !profile.displayName.isEmpty }
    var accountEmail: String { userEmail }
    var isOnline: Bool { authStatus == .authenticated }

    private static func applicationDirectory() throws -> URL {
        try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask,
                                    appropriateFor: nil, create: true)
            .appendingPathComponent("VIZIT", isDirectory: true)
    }

    private func configureStorage(for id: UUID) throws {
        profileLoadStatus = .loading
        let directory = try Self.applicationDirectory()
            .appendingPathComponent("users", isDirectory: true)
            .appendingPathComponent(id.uuidString.lowercased(), isDirectory: true)
        let profileStore = ProfileFileStore(directory: directory)
        let metadataStore = ProfileSyncStore(directory: directory)
        fileStore = profileStore
        syncStore = metadataStore
        userID = id
        let metadata = try metadataStore.load()
        profile = try metadata.pendingProfile ?? profileStore.load()
        try profile.validateIfPresent()
        profileRevision &+= 1
        storageError = nil
    }

    private func bootstrap() async {
        guard let cloud else { return }
        guard let cached = cloud.cachedSession else {
            authStatus = .signedOut
            return
        }
        do {
            try configureStorage(for: cached.user.id)
            userEmail = cached.user.email ?? ""
        } catch {
            storageError = error.localizedDescription
            authStatus = .offline
            return
        }
        do {
            let current = try await cloud.validSession()
            userEmail = current.user.email ?? ""
            authStatus = .authenticated
            await completeInitialProfileLoad()
        } catch {
            authStatus = .offline
            syncStatus = .pending
            profileLoadStatus = hasProfile ? .ready : .unavailable
            message = "Nincs hálózati kapcsolat. A helyi névjegyed olvasható és szerkeszthető; a feltöltés később újrapróbálható."
        }
    }

    func login(email: String, password: String) async {
        if let issue = AuthValidation.login(email: email, password: password) { message = issue; return }
        await performAuth(operation: .login,
                          defaultError: "A bejelentkezés nem sikerült. Ellenőrizd a kapcsolatot, majd próbáld újra.") {
            guard let cloud = self.cloud else { return }
            let session = try await cloud.login(email: email, password: password)
            try self.configureStorage(for: session.user.id)
            self.userEmail = session.user.email ?? ""
            self.authStatus = .authenticated
            await self.completeInitialProfileLoad()
        }
    }

    func register(name: String, email: String, password: String,
                  confirmation: String, legalAccepted: Bool) async {
        if let issue = AuthValidation.registration(name: name, email: email, password: password,
                                                   confirmation: confirmation, legalAccepted: legalAccepted) {
            message = issue
            return
        }
        await performAuth(operation: .registration,
                          defaultError: "A regisztráció nem sikerült. Próbáld újra később.") {
            guard let cloud = self.cloud else { return }
            try await cloud.register(name: name, email: email, password: password)
            self.authStatus = .verificationSent(email.trimmingCharacters(in: .whitespacesAndNewlines))
            self.message = "Ha ez új e-mail-cím, elküldtük a megerősítő levelet. Ha már van fiókod, lépj be vagy kérj új jelszót."
        }
    }

    func googleLogin() async {
        await performAuth(operation: .login,
                          defaultError: "A Google-bejelentkezés megszakadt vagy nem sikerült.") {
            guard let cloud = self.cloud else { return }
            let session = try await cloud.googleLogin()
            try self.configureStorage(for: session.user.id)
            self.userEmail = session.user.email ?? ""
            self.authStatus = .authenticated
            await self.completeInitialProfileLoad()
        }
    }

    @discardableResult
    func requestPasswordReset(email: String) async -> Bool {
        if let issue = AuthValidation.email(email) { message = issue; return false }
        return await performAuth(operation: .passwordResetRequest,
                                 defaultError: "A jelszó-visszaállító e-mail küldése nem sikerült. Ellenőrizd a kapcsolatot.") {
            try await self.cloud?.requestPasswordReset(email: email)
        }
    }

    func handleCallback(_ url: URL) async {
        await performAuth(operation: .callback,
                          defaultError: "A bejelentkezési hivatkozás lejárt vagy érvénytelen. Kérj új hivatkozást.") {
            guard let cloud = self.cloud else { return }
            let flow = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?
                .first(where: { $0.name == "flow" })?.value
            let session = try await cloud.handleCallback(url)
            if flow == "signup" {
                try? await cloud.logout()
                self.clearUser()
                self.authStatus = .signedOut
                self.message = "Az e-mail-címed megerősítve. Most jelentkezz be, és utána létrehozhatod az első profilodat."
                return
            }
            try self.configureStorage(for: session.user.id)
            self.userEmail = session.user.email ?? ""
            self.authStatus = flow == "recovery" ? .passwordRecovery : .authenticated
            if flow != "recovery" { await self.completeInitialProfileLoad() }
        }
    }

    func changePassword(_ password: String, confirmation: String) async {
        if let issue = AuthValidation.passwordChange(password, confirmation: confirmation) { message = issue; return }
        await performAuth(operation: .passwordChange,
                          defaultError: "A jelszó módosítása nem sikerült.") {
            try await self.cloud?.changePassword(password)
            self.authStatus = .authenticated
            self.message = "A jelszavad megváltozott."
            await self.completeInitialProfileLoad()
        }
    }

    func logout() async {
        busy = true
        defer { busy = false }
        do {
            try await cloud?.logout()
        } catch {
            // The SDK removes the local token before its best-effort server call.
        }
        // Keep only unsent edits; signed-out UI has no access to another account's files.
        if (try? syncStore?.load().pendingUpload) == false {
            try? fileStore?.reset()
            try? syncStore?.reset()
        }
        clearUser()
        authStatus = .signedOut
    }

    func deleteAccount(confirmation: String) async {
        if let issue = AuthValidation.deletionPhrase(confirmation) { message = issue; return }
        await performAuth(defaultError: "A fiók törlése nem sikerült; semmilyen helyi adatot nem töröltünk.") {
            guard let cloud = self.cloud else { return }
            try await cloud.deleteAccount()
            try self.fileStore?.reset()
            try self.syncStore?.reset()
            try? await cloud.logout()
            self.clearUser()
            self.authStatus = .signedOut
            self.message = "A fiókot és a hozzá tartozó szerveradatokat töröltük."
        }
    }

    func save(_ draft: ContactProfile) throws {
        guard let storage = fileStore, let metadataStore = syncStore else { throw ProfileError.damagedFile }
        try draft.validate()
        var metadata = try metadataStore.load()
        _ = try storage.load()
        metadata.pendingUpload = !uiTesting
        metadata.pendingProfile = uiTesting ? nil : draft.normalized
        metadata.conflict = false
        try metadataStore.save(metadata)
        try storage.save(draft)
        profile = draft.normalized
        profileRevision &+= 1
        storageError = nil
        syncStatus = uiTesting ? .localOnly : .pending
        if !uiTesting { Task { await synchronize() } }
    }

    func beginAdditionalProfile() {
        guard featureFlags.multiProfile else {
            message = "A többprofilos funkció még nincs bekapcsolva."
            return
        }
        guard authStatus == .authenticated, profileCatalogReady else {
            message = "Előbb várd meg a jelenlegi profil szinkronizálását."
            return
        }
        creatingAdditionalProfile = true
        message = nil
    }

    func cancelAdditionalProfile() {
        creatingAdditionalProfile = false
    }

    func createAdditionalProfile(_ draft: ContactProfile) async throws {
        try draft.validate()
        guard featureFlags.multiProfile, authStatus == .authenticated,
              let cloud, let id = userID, profileCatalogReady else {
            throw CloudError.profileCatalogUnavailable
        }
        profileCatalogBusy = true
        defer { profileCatalogBusy = false }
        let remote = try await cloud.createProfile(ownerID: id, profileID: UUID(), profile: draft.normalized)
        do {
            try await cloud.syncSocialProfiles(profileID: remote.id, value: draft.normalized, expected: ContactProfile())
            try await cloud.makeDefaultProfile(ownerID: id, profileID: remote.id)
        } catch {
            try? await cloud.deleteProfile(ownerID: id, profileID: remote.id)
            throw error
        }
        try await loadCurrentDefaultProfile()
        await refreshProfileCatalog()
    }

    func switchProfile(_ profileID: UUID) async {
        guard !profileCatalogBusy,
              accountProfiles.first(where: { $0.isDefault })?.id != profileID,
              let cloud, let id = userID, profileCatalogReady else {
            if !profileCatalogReady { message = "Előbb várd meg a jelenlegi profil szinkronizálását." }
            return
        }
        profileCatalogBusy = true
        defer { profileCatalogBusy = false }
        do {
            try await cloud.makeDefaultProfile(ownerID: id, profileID: profileID)
            try await loadCurrentDefaultProfile()
            await refreshProfileCatalog()
            message = "Profil átváltva."
        } catch {
            message = error.localizedDescription
        }
    }

    func deleteActiveProfile() async {
        guard !profileCatalogBusy, let active = accountProfiles.first(where: { $0.isDefault }),
              let cloud, let id = userID, profileCatalogReady else {
            if !profileCatalogReady { message = "Előbb várd meg a jelenlegi profil szinkronizálását." }
            return
        }
        profileCatalogBusy = true
        defer { profileCatalogBusy = false }
        do {
            let deletingLast = accountProfiles.count == 1
            try await cloud.deleteProfile(ownerID: id, profileID: active.id)
            if deletingLast {
                try fileStore?.reset()
                try syncStore?.reset()
                profile = ContactProfile()
                profileRevision &+= 1
                syncStatus = .localOnly
                accountProfiles = []
                profileLoadStatus = .ready
            } else {
                try await loadCurrentDefaultProfile()
                await refreshProfileCatalog()
            }
            message = "A profil törölve."
        } catch {
            message = error.localizedDescription
        }
    }

    private var profileCatalogReady: Bool {
        guard !syncInFlight, syncStatus != .conflict, let syncStore,
              let metadata = try? syncStore.load() else { return false }
        return !metadata.pendingUpload && !metadata.conflict
    }

    private func loadCurrentDefaultProfile() async throws {
        guard let storage = fileStore, let metadataStore = syncStore else { throw ProfileError.damagedFile }
        try storage.reset()
        try metadataStore.reset()
        profileRevision &+= 1
        syncStatus = .localOnly
        await synchronize()
        guard syncStatus == .synced else { throw CloudError.profileCatalogUnavailable }
    }

    func retrySync() {
        Task {
            if authStatus == .offline, let cloud {
                do {
                    let session = try await cloud.validSession()
                    userEmail = session.user.email ?? ""
                    authStatus = .authenticated
                } catch {
                    message = "Még nincs hálózati kapcsolat. A helyi névjegy változatlanul megmaradt."
                    return
                }
            }
            await synchronize()
            await refreshFeatureFlags()
            profileLoadStatus = hasProfile || syncStatus != .failed ? .ready : .unavailable
        }
    }

    func reset() throws {
        guard let storage = fileStore else { throw ProfileError.damagedFile }
        if try syncStore?.load().pendingUpload == true {
            message = "Van még fel nem töltött módosítás. Előbb szinkronizálj vagy oldd fel az ütközést."
            return
        }
        try storage.reset()
        try syncStore?.reset()
        profile = ContactProfile()
        profileRevision &+= 1
        storageError = nil
        syncStatus = .localOnly
    }

    func synchronize() async {
        guard !uiTesting, authStatus == .authenticated, let cloud, let id = userID,
              let storage = fileStore, let metadataStore = syncStore, storageError == nil else { return }
        if syncInFlight {
            syncAgain = true
            return
        }
        syncInFlight = true
        defer {
            syncInFlight = false
            if syncAgain {
                syncAgain = false
                Task { await self.synchronize() }
            }
        }
        if message == syncFailureMessage { message = nil }
        syncFailureMessage = nil
        let revision = profileRevision
        let localProfile = profile
        syncStatus = .syncing
        do {
            var metadata = try metadataStore.load()
            let remoteBundle = try await cloud.fetchProfile(ownerID: id, preserving: localProfile,
                                                           loadPhoto: !metadata.pendingUpload)
            guard userID == id else { return }
            guard profileRevision == revision else {
                syncAgain = true
                syncStatus = .pending
                return
            }

            // Upgrade the previous local-only photo only when the exact same
            // cloud revision is still current. Existing conflict protection stays.
            if !metadata.pendingUpload, let (remote, _) = remoteBundle,
               (remote.avatarURL ?? "").isEmpty, !localProfile.photoSyncInitialized,
               !localProfile.photoBase64.isEmpty,
               metadata.profileID == remote.id, metadata.remoteUpdatedAt == remote.updatedAt {
                metadata.pendingUpload = true
                metadata.pendingProfile = localProfile
                try metadataStore.save(metadata)
            }

            if !metadata.pendingUpload, let (remote, _) = remoteBundle,
               !localProfile.photoSyncInitialized, !localProfile.photoBase64.isEmpty,
               (remote.avatarURL ?? "").isEmpty {
                metadata.pendingUpload = true
                metadata.pendingProfile = localProfile
                metadata.conflict = true
                try metadataStore.save(metadata)
                syncStatus = .conflict
                return
            }

            if metadata.pendingUpload {
                if let remoteBundle {
                    guard ProfileRevisionPolicy.mayUpload(
                        localID: metadata.profileID,
                        localRevision: metadata.remoteUpdatedAt,
                        localFingerprint: metadata.remoteFingerprint,
                        remoteID: remoteBundle.0.id,
                        remoteRevision: remoteBundle.0.updatedAt,
                        remoteFingerprint: remoteBundle.0.fingerprint
                    ) else {
                        metadata.conflict = true
                        try metadataStore.save(metadata)
                        syncStatus = .conflict
                        return
                    }
                    guard var remote = try await cloud.updateProfile(ownerID: id, profileID: remoteBundle.0.id,
                                                                     profile: localProfile,
                                                                     expectedUpdatedAt: remoteBundle.0.updatedAt,
                                                                     previousAppearance: remoteBundle.0.appearance,
                                                                     previousLogoBase64: remoteBundle.1.logoBase64,
                                                                     theme: remoteBundle.0.theme) else {
                        metadata.conflict = true
                        try metadataStore.save(metadata)
                        syncStatus = .conflict
                        return
                    }
                    remote.copySocialProfiles(from: remoteBundle.0)
                    metadata = try metadataStore.load()
                    metadata.profileID = remote.id
                    metadata.remoteUpdatedAt = remote.updatedAt
                    metadata.remoteFingerprint = remote.fingerprint
                    try metadataStore.save(metadata)
                    guard userID == id else { return }
                    guard profileRevision == revision else {
                        metadata.pendingUpload = true
                        try metadataStore.save(metadata)
                        syncAgain = true
                        syncStatus = .pending
                        return
                    }
                    try await cloud.syncSocialProfiles(profileID: remote.id, value: localProfile,
                                                       expected: remoteBundle.1)
                } else {
                    let reservedID = metadata.profileID ?? UUID()
                    metadata.profileID = reservedID
                    metadata.remoteUpdatedAt = nil
                    try metadataStore.save(metadata)
                    let remote = try await cloud.createProfile(ownerID: id, profileID: reservedID,
                                                               profile: localProfile)
                    guard remote.id == reservedID else { throw CloudError.emptyResponse }
                    metadata = try metadataStore.load()
                    metadata.profileID = remote.id
                    metadata.remoteUpdatedAt = remote.updatedAt
                    metadata.remoteFingerprint = remote.fingerprint
                    try metadataStore.save(metadata)
                    guard userID == id else { return }
                    guard profileRevision == revision else {
                        var latest = profile
                        if latest.publicSlug == localProfile.publicSlug {
                            latest.publicSlug = remote.slug
                            latest.customDomain = remote.customDomain ?? ""
                            latest.customDomainVerified = remote.customDomainVerified == true
                            try storage.save(latest)
                            profile = latest
                        }
                        metadata.pendingUpload = true
                        try metadataStore.save(metadata)
                        syncAgain = true
                        syncStatus = .pending
                        return
                    }
                    var local = localProfile
                    local.publicSlug = remote.slug
                    local.customDomain = remote.customDomain ?? ""
                    local.customDomainVerified = remote.customDomainVerified == true
                    try storage.save(local)
                    profile = local
                    try await cloud.syncSocialProfiles(profileID: remote.id, value: localProfile,
                                                       expected: ContactProfile())
                }
                guard userID == id else { return }
                metadata = try metadataStore.load()
                guard profileRevision == revision else {
                    metadata.pendingUpload = true
                    try metadataStore.save(metadata)
                    syncAgain = true
                    syncStatus = .pending
                    return
                }
                let verified = try await cloud.fetchProfile(ownerID: id, preserving: profile, loadPhoto: false)
                guard userID == id else { return }
                metadata = try metadataStore.load()
                guard profileRevision == revision else { syncAgain = true; syncStatus = .pending; return }
                guard let verified, verified.0.matches(profile, remoteLogo: verified.1.logoBase64) else { throw CloudError.profileConflict }
                metadata.remoteUpdatedAt = verified.0.updatedAt
                metadata.remoteFingerprint = verified.0.fingerprint
                var photoSynced = profile
                photoSynced.photoSyncInitialized = true
                photoSynced.logoSyncInitialized = true
                photoSynced.publicSlug = verified.1.publicSlug
                photoSynced.customDomain = verified.1.customDomain
                photoSynced.customDomainVerified = verified.1.customDomainVerified
                try storage.save(photoSynced)
                profile = photoSynced
                metadata.pendingUpload = false
                metadata.pendingProfile = nil
                metadata.conflict = false
                try metadataStore.save(metadata)
                syncStatus = .synced
            } else if let (remote, pulled) = remoteBundle {
                try pulled.validate()
                try storage.save(pulled)
                profile = pulled
                metadata.profileID = remote.id
                metadata.remoteUpdatedAt = remote.updatedAt
                metadata.remoteFingerprint = remote.fingerprint
                metadata.pendingProfile = nil
                metadata.conflict = false
                try metadataStore.save(metadata)
                syncStatus = .synced
            } else {
                if hasProfile {
                    metadata.pendingUpload = true
                    metadata.pendingProfile = localProfile
                    metadata.conflict = metadata.profileID != nil
                    try metadataStore.save(metadata)
                    syncStatus = metadata.conflict ? .conflict : .pending
                    if !metadata.conflict { syncAgain = true }
                } else { syncStatus = .localOnly }
            }
        } catch let error as CloudError {
            guard userID == id else { return }
            if case .profileConflict = error {
                if var latest = try? metadataStore.load() {
                    latest.conflict = true
                    try? metadataStore.save(latest)
                }
                syncStatus = .conflict
            } else { syncStatus = .failed }
            let text = error.localizedDescription
            syncFailureMessage = text
            message = text
        } catch {
            guard userID == id else { return }
            syncStatus = .failed
            let text = "A névjegy mentve. A szinkron később folytatódik."
            syncFailureMessage = text
            message = text
        }
    }

    func resolveSyncConflict(keepLocal: Bool) async {
        guard syncStatus == .conflict, !syncInFlight, authStatus == .authenticated,
              let cloud, let id = userID, let storage = fileStore, let metadataStore = syncStore else { return }
        let revision = profileRevision
        let local = profile
        syncInFlight = true
        defer { syncInFlight = false }
        syncStatus = .syncing
        do {
            let remote = try await cloud.fetchProfile(ownerID: id, preserving: ContactProfile(), loadPhoto: !keepLocal)
            guard revision == profileRevision, userID == id else { syncStatus = .pending; return }
            var metadata = try metadataStore.load()
            metadata.profileID = remote?.0.id
            metadata.remoteUpdatedAt = remote?.0.updatedAt
            metadata.remoteFingerprint = remote?.0.fingerprint
            metadata.conflict = false
            if keepLocal {
                metadata.pendingUpload = true
                metadata.pendingProfile = local
                try metadataStore.save(metadata)
                syncStatus = .pending
                syncInFlight = false
                await synchronize()
            } else {
                guard let (server, downloaded) = remote else { throw CloudError.emptyResponse }
                try downloaded.validate()
                // A separate recoverable copy preserves the local conflict version.
                let backup = storage.fileURL.deletingLastPathComponent().appendingPathComponent("conflict-backup.json")
                try JSONEncoder().encode(local).write(to: backup, options: [.atomic, .completeFileProtection])
                try storage.save(downloaded)
                profile = downloaded
                profileRevision &+= 1
                metadata.profileID = server.id
                metadata.pendingUpload = false
                metadata.pendingProfile = nil
                try metadataStore.save(metadata)
                syncStatus = .synced
            }
        } catch {
            syncStatus = .conflict
            message = "Az ütközés feloldása nem sikerült. A helyi változtatásokat megőriztük."
        }
    }

    func dismissMessage() { message = nil }

    private func completeInitialProfileLoad() async {
        await synchronize()
        await refreshFeatureFlags()
        await refreshProfileCatalog()
        profileLoadStatus = hasProfile || syncStatus != .failed ? .ready : .unavailable
    }

    private func refreshFeatureFlags() async {
        guard let cloud, let flags = try? await cloud.fetchFeatureFlags() else { return }
        featureFlags = flags
    }

    private func refreshProfileCatalog() async {
        guard let cloud, let id = userID, authStatus == .authenticated else { return }
        do {
            accountProfiles = try await cloud.fetchProfiles(ownerID: id)
        } catch {
            if accountProfiles.isEmpty { message = "A profillista most nem frissíthető." }
        }
    }

    private func clearUser() {
        profile = ContactProfile()
        profileRevision &+= 1
        fileStore = nil
        syncStore = nil
        userID = nil
        userEmail = ""
        storageError = nil
        syncStatus = .localOnly
        profileLoadStatus = .loading
        featureFlags = AppFeatureFlags()
        accountProfiles = []
        profileCatalogBusy = false
        creatingAdditionalProfile = false
    }

    @discardableResult
    private func performAuth(
        operation authOperation: AuthOperation? = nil,
        defaultError: String,
        work: () async throws -> Void
    ) async -> Bool {
        guard !busy else { return false }
        busy = true
        message = nil
        defer { busy = false }
        do {
            try await work()
            return true
        } catch let error as CloudError {
            message = error.localizedDescription
        } catch let error as ConfigurationError {
            message = error.localizedDescription
        } catch let error as AuthError {
            let status: Int?
            if case .api(_, _, _, let response) = error {
                status = response.statusCode
            } else {
                status = nil
            }
            message = authOperation.flatMap {
                AuthFailureMessage.text(operation: $0,
                                        errorCode: error.errorCode.rawValue,
                                        httpStatus: status,
                                        diagnostic: error.message)
            } ?? defaultError
        } catch {
            message = defaultError
        }
        return false
    }
}

// MARK: - Root navigation

/// Four primary destinations, matching Android. Scanning and the knowledge hub
/// are tasks you start from a destination, not tabs of their own.
enum RootTab: Hashable {
    case home, card, share, settings
}

struct AppGate: View {
    @EnvironmentObject private var store: AppStore
    @State private var themeMode: ThemeMode = ThemeStorage.current
    @State private var finishingWizard = false

    var body: some View {
        gateContent
        .preferredColorScheme(themeMode.colorScheme)
        .onChange(of: store.authStatus) { status in
            if status == .signedOut { finishingWizard = false }
        }
        .alert("VIZIT", isPresented: Binding(
            get: { store.message != nil },
            set: { if !$0 { store.dismissMessage() } }
        )) {
            Button("Rendben", role: .cancel) { store.dismissMessage() }
        } message: { Text(store.message ?? "") }
    }

    @ViewBuilder
    private var gateContent: some View {
        switch store.authStatus {
        case .launching:
            LaunchScreen()
        case .signedOut, .verificationSent:
            AuthScreen()
        case .passwordRecovery:
            PasswordChangeScreen()
        case .configurationError(let detail):
            configurationError(detail)
        case .authenticated, .offline:
            profileContent
        }
    }

    @ViewBuilder
    private var profileContent: some View {
        switch store.profileLoadStatus {
        case .loading:
            LaunchScreen()
        case .unavailable:
            unavailableProfile
        case .ready:
            if store.hasProfile && !finishingWizard && !store.creatingAdditionalProfile {
                RootView(themeMode: $themeMode)
            } else {
                profileWizard
            }
        }
    }

    private func configurationError(_ detail: String) -> some View {
        VizitScreen {
            VizitErrorState(title: "Ez a build nem használható", message: detail)
                .padding(.horizontal, VizitSpace.md)
        }
    }

    private var unavailableProfile: some View {
        VizitScreen {
            VizitErrorState(
                title: "A névjegy most nem tölthető be",
                message: "Nem nyitjuk meg az újprofil-varázslót, amíg nem derül ki biztosan, hogy ehhez a fiókhoz még nincs névjegy.",
                retryTitle: "Újrapróbálás",
                onRetry: store.retrySync
            )
            .padding(.horizontal, VizitSpace.md)
        }
    }

    private var profileWizard: some View {
        ProfileWizard(
            additionalProfile: store.creatingAdditionalProfile,
            onCancel: wizardCancel,
            onSaving: { finishingWizard = true },
            onSaveFailed: { finishingWizard = false },
            onFinished: {
                store.cancelAdditionalProfile()
                finishingWizard = false
            }
        )
    }

    private var wizardCancel: (() -> Void)? {
        guard store.creatingAdditionalProfile else { return nil }
        return { store.cancelAdditionalProfile() }
    }
}

private struct LaunchScreen: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(uiColor: UIColor(hex: 0x0C2C63)),
                    Color(uiColor: UIColor(hex: 0x05163A))
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: VizitSpace.lg) {
                VizitBrandLockup(maxHeight: 96)
                    .frame(maxWidth: 300)
                ProgressView()
                    .controlSize(.large)
                    .tint(Color(uiColor: UIColor(hex: 0x0FBEE6)))
            }
            .padding(VizitSpace.xl)
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var store: AppStore
    @Binding var themeMode: ThemeMode
    @State private var selection: RootTab = .home

    var body: some View {
        TabView(selection: $selection) {
            HomeScreen(selectedTab: $selection)
                .tabItem { Label("Kezdőlap", systemImage: "house") }
                .tag(RootTab.home)

            CardScreen(selectedTab: $selection)
                .tabItem { Label("Névjegy", systemImage: "person.crop.rectangle") }
                .tag(RootTab.card)

            ShareScreen()
                .tabItem { Label("Megosztás", systemImage: "square.and.arrow.up") }
                .tag(RootTab.share)

            SettingsScreen(themeMode: $themeMode)
                .tabItem { Label("Beállítások", systemImage: "gearshape") }
                .tag(RootTab.settings)
        }
        .tint(VizitColor.primary)
        .toolbarBackground(VizitColor.surface, for: .tabBar)
        .toolbarBackground(.visible, for: .tabBar)
        .safeAreaInset(edge: .top, spacing: 0) {
            AccountProfileSwitcher()
        }
        .overlay(alignment: .top) {
            if store.authStatus == .offline {
                VizitBanner(
                    text: "Offline mód – a helyi névjegyed olvasható és szerkeszthető.",
                    tone: .info
                )
                .padding(.horizontal, VizitSpace.md)
                .padding(.top, VizitSpace.xxs)
            }
        }
    }
}

private struct AccountProfileSwitcher: View {
    @EnvironmentObject private var store: AppStore
    @State private var confirmDelete = false

    private var visible: Bool {
        store.featureFlags.multiProfile || store.accountProfiles.count > 1
    }

    private var active: AccountProfile? {
        store.accountProfiles.first(where: \.isDefault) ?? store.accountProfiles.first
    }

    var body: some View {
        if visible {
            Menu {
                ForEach(store.accountProfiles) { profile in
                    Button {
                        Task { await store.switchProfile(profile.id) }
                    } label: {
                        Label(
                            profile.displayName,
                            systemImage: profile.isDefault ? "checkmark.circle.fill" : "person.crop.circle"
                        )
                    }
                    .disabled(profile.isDefault || store.profileCatalogBusy)
                }
                if store.featureFlags.multiProfile {
                    Divider()
                    Button(action: store.beginAdditionalProfile) {
                        Label("Új profil", systemImage: "plus.circle")
                    }
                }
                if active != nil {
                    Divider()
                    Button(role: .destructive) { confirmDelete = true } label: {
                        Label("Aktív profil törlése", systemImage: "trash")
                    }
                }
            } label: {
                HStack(spacing: VizitSpace.sm) {
                    Image(systemName: "person.crop.circle")
                        .foregroundStyle(VizitColor.primary)
                    VStack(alignment: .leading, spacing: 1) {
                        Text(active?.displayName ?? "Profil kiválasztása")
                            .font(VizitFont.label)
                            .foregroundStyle(VizitColor.textPrimary)
                            .lineLimit(1)
                        Text(store.accountProfiles.count > 1
                             ? "\(store.accountProfiles.count) profil · váltás"
                             : "Új profil hozzáadása")
                            .font(VizitFont.caption)
                            .foregroundStyle(VizitColor.textMuted)
                    }
                    Spacer()
                    if store.profileCatalogBusy { ProgressView().controlSize(.small) }
                    else { Image(systemName: "chevron.down").foregroundStyle(VizitColor.textMuted) }
                }
                .padding(.horizontal, VizitSpace.md)
                .padding(.vertical, VizitSpace.sm)
                .background(VizitColor.surface)
                .overlay(alignment: .bottom) { Divider() }
            }
            .disabled(store.profileCatalogBusy)
            .alert("Profil törlése?", isPresented: $confirmDelete) {
                Button("Mégse", role: .cancel) {}
                Button("Törlés", role: .destructive) { Task { await store.deleteActiveProfile() } }
            } message: {
                Text("A(z) „\(active?.displayName ?? "")” profil végleg törlődik. Ha van másik profil, az automatikusan aktívvá válik.")
            }
        }
    }
}
