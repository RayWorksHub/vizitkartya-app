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
        profileLoadStatus = hasProfile || syncStatus != .failed ? .ready : .unavailable
    }

    private func refreshFeatureFlags() async {
        guard let cloud, let flags = try? await cloud.fetchFeatureFlags() else { return }
        featureFlags = flags
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
        Group {
            switch store.authStatus {
            case .launching:
                LaunchScreen()

            case .signedOut, .verificationSent:
                AuthScreen()

            case .passwordRecovery:
                PasswordChangeScreen()

            case .configurationError(let detail):
                VizitScreen {
                    VizitErrorState(
                        title: "Ez a build nem használható",
                        message: detail
                    )
                    .padding(.horizontal, VizitSpace.md)
                }

            case .authenticated, .offline:
                switch store.profileLoadStatus {
                case .loading:
                    LaunchScreen()
                case .unavailable:
                    VizitScreen {
                        VizitErrorState(
                            title: "A névjegy most nem tölthető be",
                            message: "Nem nyitjuk meg az újprofil-varázslót, amíg nem derül ki biztosan, hogy ehhez a fiókhoz még nincs névjegy.",
                            retryTitle: "Újrapróbálás",
                            onRetry: store.retrySync
                        )
                        .padding(.horizontal, VizitSpace.md)
                    }
                case .ready:
                    if store.hasProfile && !finishingWizard {
                        RootView(themeMode: $themeMode)
                    } else {
                        ProfileWizard(onSaving: { finishingWizard = true },
                                      onSaveFailed: { finishingWizard = false },
                                      onFinished: { finishingWizard = false })
                    }
                }
            }
        }
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

enum NavigationLayoutMode: String, CaseIterable {
    case threeTabs = "three_tabs"
    case oneScreen = "one_screen"

    var label: String {
        switch self {
        case .threeTabs: return "Három fül"
        case .oneScreen: return "Egy képernyő"
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var store: AppStore
    @Binding var themeMode: ThemeMode
    @AppStorage("navigationLayoutMode") private var navigationLayoutRaw = NavigationLayoutMode.oneScreen.rawValue
    @State private var selection: RootTab = .share

    private var navigationLayout: NavigationLayoutMode {
        NavigationLayoutMode(rawValue: navigationLayoutRaw) ?? .oneScreen
    }

    var body: some View {
        Group {
            if navigationLayout == .oneScreen {
                V9ShareHome(themeMode: $themeMode, singleScreen: true)
            } else {
                TabView(selection: $selection) {
                    V9ShareHome(themeMode: $themeMode, singleScreen: false)
                        .tabItem { Label("Megosztás", systemImage: "qrcode") }
                        .tag(RootTab.share)

                    CardScreen(selectedTab: $selection)
                        .tabItem { Label("Profil", systemImage: "person.crop.rectangle") }
                        .tag(RootTab.card)

                    SettingsScreen(themeMode: $themeMode)
                        .tabItem { Label("Továbbiak", systemImage: "ellipsis.circle") }
                        .tag(RootTab.settings)
                }
                .tint(VizitColor.primary)
                .toolbarBackground(VizitColor.surface, for: .tabBar)
                .toolbarBackground(.visible, for: .tabBar)
            }
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

/// VIZIT 9 native home/share surface. No HTML or embedded browser UI is used.
private struct V9ShareHome: View {
    @EnvironmentObject private var store: AppStore
    @EnvironmentObject private var presentation: CardPresentationStore
    @Environment(\.openURL) private var openURL
    @Binding var themeMode: ThemeMode
    let singleScreen: Bool

    @State private var menuOpen = false
    @State private var editing = false
    @State private var settingsOpen = false
    @State private var scannerOpen = false
    @State private var businessOpen = false

    private var publicURL: URL? {
        guard store.profile.isPublic, store.syncStatus == .synced,
              let base = store.configuration?.publicProfileBaseURL else { return nil }
        return PublicProfileLink.preferred(
            baseURL: base,
            slug: store.profile.publicSlug,
            customDomain: store.profile.customDomain,
            customDomainVerified: store.profile.customDomainVerified
        )
    }

    private var qrImage: UIImage? {
        publicURL.flatMap { QRImage.make($0.absoluteString) }
    }

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(spacing: VizitSpace.md) {
                        HStack(spacing: VizitSpace.sm) {
                            Text("VIZIT")
                                .font(.system(size: 28, weight: .bold))
                                .tracking(5)
                                .foregroundStyle(VizitColor.textPrimary)
                            Spacer()
                            Button {
                                if singleScreen {
                                    menuOpen = true
                                } else if store.featureFlags.qrScanner {
                                    scannerOpen = true
                                }
                            } label: {
                                if singleScreen {
                                    VizitAvatar(profile: store.profile, size: 42)
                                } else {
                                    Image(systemName: "qrcode.viewfinder")
                                        .font(.system(size: 20, weight: .semibold))
                                        .foregroundStyle(VizitColor.primary)
                                        .frame(width: 44, height: 44)
                                        .background(VizitColor.surface)
                                        .clipShape(Circle())
                                        .overlay(Circle().stroke(VizitColor.border, lineWidth: 1))
                                }
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(singleScreen ? "Menü" : "Névjegy beolvasása")
                        }

                        VStack(spacing: VizitSpace.md) {
                            Text(store.profile.displayName.isEmpty ? "VIZIT profil" : store.profile.displayName)
                                .font(VizitFont.h3)
                                .foregroundStyle(VizitColor.textPrimary)
                                .multilineTextAlignment(.center)

                            if let qrImage {
                                Image(uiImage: qrImage)
                                    .interpolation(.none)
                                    .resizable()
                                    .scaledToFit()
                                    .padding(VizitSpace.sm)
                                    .background(Color.white)
                                    .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
                                    .accessibilityLabel("VIZIT profil QR-kód")
                            } else {
                                VStack(spacing: VizitSpace.sm) {
                                    Image(systemName: "qrcode")
                                        .font(.system(size: 44))
                                    Text("A nyilvános profil még nem érhető el")
                                        .font(VizitFont.bodySmall)
                                        .multilineTextAlignment(.center)
                                }
                                .foregroundStyle(VizitColor.textMuted)
                                .frame(maxWidth: .infinity, minHeight: 220)
                                .background(VizitColor.controlTrack)
                                .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
                            }

                            Text(publicURL?.absoluteString ?? "A profil-link a sikeres szinkron után jelenik meg.")
                                .font(VizitFont.bodySmall)
                                .foregroundStyle(VizitColor.textSecondary)
                                .multilineTextAlignment(.center)
                                .textSelection(.enabled)
                        }
                        .padding(VizitSpace.lg)
                        .background(VizitColor.surface)
                        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous)
                                .stroke(VizitColor.border, lineWidth: 1)
                        }

                        if let url = publicURL {
                            ShareLink(item: url) {
                                Label("Megosztás", systemImage: "square.and.arrow.up")
                                    .font(VizitFont.label)
                                    .foregroundStyle(Color.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 54)
                                    .background(VizitColor.primary)
                                    .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
                            }
                            .buttonStyle(.plain)

                            VizitButton(
                                title: "Profil megnyitása",
                                systemImage: "safari",
                                kind: .secondary
                            ) { openURL(url) }
                        } else {
                            VizitButton(
                                title: "Megosztás",
                                systemImage: "square.and.arrow.up",
                                isEnabled: false
                            ) {}
                            VizitButton(
                                title: "Profil megnyitása",
                                systemImage: "safari",
                                kind: .secondary,
                                isEnabled: false
                            ) {}
                        }
                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.bottom, VizitSpace.xxl)
                    .frame(maxWidth: 620)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationBarHidden(true)
        }
        .sheet(isPresented: $menuOpen) {
            V9Menu(
                themeMode: $themeMode,
                editing: $editing,
                settingsOpen: $settingsOpen,
                scannerOpen: $scannerOpen,
                businessOpen: $businessOpen,
                openDashboard: openDashboard
            )
            .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $editing) { ProfileEditor(draft: store.profile) }
        .sheet(isPresented: $settingsOpen) { SettingsScreen(themeMode: $themeMode) }
        .sheet(isPresented: $businessOpen) { BusinessHubScreen() }
        .fullScreenCover(isPresented: $scannerOpen) { ScanFlow() }
    }

    private func openDashboard(_ destination: String) {
        guard let url = URL(string: "https://www.vizitkartyam.hu/auth/sign-in?next=%2Fdashboard%2F\(destination)") else { return }
        openURL(url)
    }
}

private struct V9Menu: View {
    @EnvironmentObject private var store: AppStore
    @Binding var themeMode: ThemeMode
    @Binding var editing: Bool
    @Binding var settingsOpen: Bool
    @Binding var scannerOpen: Bool
    @Binding var businessOpen: Bool
    let openDashboard: (String) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.md) {
                        HStack(spacing: VizitSpace.sm) {
                            VizitAvatar(profile: store.profile, size: 48)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(store.profile.displayName).font(VizitFont.h3)
                                if !store.accountEmail.isEmpty {
                                    Text(store.accountEmail).font(VizitFont.caption).foregroundStyle(VizitColor.textMuted)
                                }
                            }
                        }

                        VizitGroup {
                            VizitRow(label: "Profil szerkesztése", systemImage: "pencil") {
                                dismiss(); editing = true
                            }
                            if store.featureFlags.qrScanner {
                                VizitDivider()
                                VizitRow(label: "Névjegy beolvasása", systemImage: "qrcode.viewfinder") {
                                    dismiss(); scannerOpen = true
                                }
                            }
                            if store.featureFlags.analytics {
                                VizitDivider()
                                VizitRow(label: "Statisztikák", systemImage: "chart.bar") {
                                    dismiss(); openDashboard("analytics")
                                }
                            }
                            if store.featureFlags.businessPortal {
                                VizitDivider()
                                VizitRow(label: "Vállalkozói Portál", systemImage: "book.closed") {
                                    dismiss(); businessOpen = true
                                }
                            }
                        }

                        VizitGroup {
                            VizitRow(label: "Beállítások", systemImage: "gearshape") {
                                dismiss(); settingsOpen = true
                            }
                        }
                    }
                    .padding(VizitSpace.md)
                }
            }
            .navigationTitle("Menü")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

