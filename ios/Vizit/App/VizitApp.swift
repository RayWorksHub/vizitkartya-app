import SwiftUI
import Supabase

@main
struct VizitApp: App {
    @StateObject private var store = AppStore()
    @StateObject private var presentation = CardPresentationStore()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            AppGate()
                .environmentObject(store)
                .environmentObject(presentation)
                .tint(VizitColor.primary)
                .onOpenURL { url in Task { await store.handleCallback(url) } }
                .onChange(of: scenePhase) { phase in
                    if phase == .active { Task { await store.refreshBusinessCards() } }
                }
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
    @Published private(set) var businessCards: [OwnedBusinessCard] = []
    @Published private(set) var activeBusinessCardID: UUID?
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
    private var catalogStore: BusinessCardCatalogStore?
    private var catalog: BusinessCardCatalog?
    private var userID: UUID?
    private var userEmail = ""
    private var profileRevision = 0
    private var syncInFlight = false
    private var syncAgain = false
    private var catalogRefreshInFlight = false
    private var pendingCatalogSyncInFlight = false
    private var syncFailureMessage: String?
    private let uiTesting: Bool
    private let signupLoginKey = "vizit.require-login-after-signup"

    init() {
        #if DEBUG
        uiTesting = ProcessInfo.processInfo.arguments.contains("--ui-testing")
        #else
        uiTesting = false
        #endif
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--ui-test-verification") {
            authStatus = .verificationSent("verification@example.com")
            return
        }
        #endif
        if uiTesting {
            do {
                let ownerID = UUID(uuidString: "00000000-0000-4000-8000-000000000010")!
                let directory = try Self.applicationDirectory()
                    .appendingPathComponent("UITests", isDirectory: true)
                if ProcessInfo.processInfo.arguments.contains("--reset-test-profile") {
                    try? BusinessCardCatalogStore(directory: directory, ownerID: ownerID).reset()
                    try? ProfileFileStore(directory: directory).reset()
                    try? ProfileSyncStore(directory: directory).reset()
                }
                try configureStorage(for: ownerID)
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
    var accountID: UUID? { userID }
    var isOnline: Bool { authStatus == .authenticated }
    var activeCardPresentation: CardPresentation {
        businessCards.first(where: { $0.profileID == activeBusinessCardID })?.presentation
            ?? CardPresentation()
    }

    private static func applicationDirectory() throws -> URL {
        try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask,
                                    appropriateFor: nil, create: true)
            .appendingPathComponent("VIZIT", isDirectory: true)
    }

    private func accountDirectory(for id: UUID) throws -> URL {
        let root = try Self.applicationDirectory()
        if uiTesting { return root.appendingPathComponent("UITests", isDirectory: true) }
        return root
            .appendingPathComponent("users", isDirectory: true)
            .appendingPathComponent(id.uuidString.lowercased(), isDirectory: true)
    }

    private func configureStorage(for id: UUID) throws {
        profileLoadStatus = .loading
        let directory = try accountDirectory(for: id)
        let indexStore = BusinessCardCatalogStore(directory: directory, ownerID: id)
        var index = try indexStore.load()

        // One-time migration from the pre-V10 single-card files. The original
        // files are deliberately left intact until the migrated card has been
        // uploaded and verified, so rollback never destroys an unsent edit.
        if index.cards.isEmpty {
            let legacyProfileStore = ProfileFileStore(directory: directory)
            let legacySyncStore = ProfileSyncStore(directory: directory)
            let legacyMetadata = try legacySyncStore.load()
            let legacyProfile = try legacyMetadata.pendingProfile ?? legacyProfileStore.load()
            if legacyMetadata.profileID != nil || !legacyProfile.displayName.isEmpty {
                let profileID = legacyMetadata.profileID ?? UUID()
                let cardDirectory = indexStore.cardDirectory(profileID)
                let migratedProfileStore = ProfileFileStore(directory: cardDirectory)
                let migratedSyncStore = ProfileSyncStore(directory: cardDirectory)
                if !legacyProfile.displayName.isEmpty { try migratedProfileStore.save(legacyProfile) }
                var metadata = legacyMetadata
                metadata.profileID = profileID
                try migratedSyncStore.save(metadata)
                index.cards = [BusinessCardCatalogEntry(
                    profileID: profileID,
                    ownerID: id,
                    isPrimary: true,
                    createdAt: legacyMetadata.remoteUpdatedAt ?? "",
                    updatedAt: legacyMetadata.remoteUpdatedAt ?? "",
                    presentation: CardPresentationStore().value
                )]
                index.activeProfileID = profileID
                try indexStore.save(index)
            }
        }

        catalogStore = indexStore
        catalog = index
        userID = id
        if let active = index.activeProfileID ?? index.cards.first?.profileID {
            try activateBusinessCard(active, persistSelection: false)
        } else {
            fileStore = nil
            syncStore = nil
            profile = ContactProfile()
            activeBusinessCardID = nil
            syncStatus = .localOnly
            profileRevision &+= 1
        }
        try reloadBusinessCards()
        storageError = nil
    }

    private func activateBusinessCard(_ profileID: UUID, persistSelection: Bool = true) throws {
        guard let ownerID = userID,
              var index = catalog,
              let entry = index.cards.first(where: { $0.profileID == profileID && $0.ownerID == ownerID }),
              let catalogStore else { throw ProfileError.damagedFile }
        let directory = catalogStore.cardDirectory(entry.profileID)
        let nextProfileStore = ProfileFileStore(directory: directory)
        let nextSyncStore = ProfileSyncStore(directory: directory)
        let metadata = try nextSyncStore.load()
        guard metadata.profileID == nil || metadata.profileID == profileID else {
            throw ProfileError.damagedFile
        }
        let loaded = try metadata.pendingProfile ?? nextProfileStore.load()
        try loaded.validateIfPresent()
        fileStore = nextProfileStore
        syncStore = nextSyncStore
        profile = loaded
        activeBusinessCardID = profileID
        syncStatus = metadata.conflict ? .conflict
            : (metadata.pendingUpload ? .pending : (metadata.remoteUpdatedAt == nil ? .localOnly : .synced))
        profileRevision &+= 1
        if persistSelection && index.activeProfileID != profileID {
            index.activeProfileID = profileID
            try catalogStore.save(index)
            catalog = index
        }
    }

    private func reloadBusinessCards() throws {
        guard let ownerID = userID, let index = catalog, let catalogStore else {
            businessCards = []
            return
        }
        businessCards = try index.cards.map { entry in
            guard entry.ownerID == ownerID else { throw ProfileError.damagedFile }
            let directory = catalogStore.cardDirectory(entry.profileID)
            let metadata = try ProfileSyncStore(directory: directory).load()
            guard metadata.profileID == nil || metadata.profileID == entry.profileID else {
                throw ProfileError.damagedFile
            }
            let stored = try ProfileFileStore(directory: directory).load()
            let value = try metadata.pendingProfile ?? stored
            try value.validateIfPresent()
            return OwnedBusinessCard(
                ownerID: ownerID,
                profileID: entry.profileID,
                profile: value,
                fingerprint: metadata.remoteFingerprint,
                isPrimary: entry.isPrimary,
                createdAt: entry.createdAt,
                updatedAt: entry.updatedAt,
                presentation: entry.presentation
            )
        }
    }

    private func updateActiveCardSnapshot() throws {
        guard let ownerID = userID, let profileID = activeBusinessCardID,
              var index = catalog, let catalogStore,
              let position = index.cards.firstIndex(where: {
                  $0.profileID == profileID && $0.ownerID == ownerID
              }) else { return }
        let metadata = try syncStore?.load() ?? ProfileSyncMetadata()
        if let updatedAt = metadata.remoteUpdatedAt {
            index.cards[position].updatedAt = updatedAt
        }
        try catalogStore.save(index)
        catalog = index
        try reloadBusinessCards()
    }

    private func bootstrap() async {
        guard let cloud else { return }
        if UserDefaults.standard.bool(forKey: signupLoginKey) {
            try? await cloud.logout()
            clearUser()
            authStatus = .signedOut
            return
        }
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
            guard !UserDefaults.standard.bool(forKey: signupLoginKey) else {
                clearUser(); authStatus = .signedOut; return
            }
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
            UserDefaults.standard.removeObject(forKey: self.signupLoginKey)
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
            UserDefaults.standard.set(true, forKey: self.signupLoginKey)
            self.clearUser()
            self.message = nil
            self.authStatus = .verificationSent(email.trimmingCharacters(in: .whitespacesAndNewlines))
        }
    }

    func returnToLoginAfterRegistration() {
        message = nil
        authStatus = .signedOut
    }

    func googleLogin() async {
        await performAuth(operation: .login,
                          defaultError: "A Google-bejelentkezés megszakadt vagy nem sikerült.") {
            guard let cloud = self.cloud else { return }
            let session = try await cloud.googleLogin()
            UserDefaults.standard.removeObject(forKey: self.signupLoginKey)
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
        let query = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems ?? []
        let fragment = URLComponents(string: "https://callback.invalid/?" + (url.fragment ?? ""))?.queryItems ?? []
        let signup = query.contains { $0.name == "flow" && $0.value == "signup" }
            || (query + fragment).contains { $0.name == "type" && $0.value == "signup" }
        if signup { UserDefaults.standard.set(true, forKey: signupLoginKey) }
        await performAuth(operation: .callback,
                          defaultError: "A bejelentkezési hivatkozás lejárt vagy érvénytelen. Kérj új hivatkozást.") {
            guard let cloud = self.cloud else { return }
            let flow = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?
                .first(where: { $0.name == "flow" })?.value
            let session = try await cloud.handleCallback(url)
            if signup {
                try? await cloud.logout()
                self.clearUser()
                self.authStatus = .signedOut
                self.message = nil
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
            UserDefaults.standard.removeObject(forKey: self.signupLoginKey)
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
        // Keep the account-scoped cache only when at least one card still has
        // an unsent edit. Signed-out and other-account UI never receives a
        // reference to this directory.
        if !hasPendingBusinessCardUploads() {
            try? catalogStore?.reset()
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
            try self.catalogStore?.reset()
            try self.fileStore?.reset()
            try self.syncStore?.reset()
            try? await cloud.logout()
            self.clearUser()
            self.authStatus = .signedOut
            self.message = "A fiókot és a hozzá tartozó szerveradatokat töröltük."
        }
    }

    func save(_ draft: ContactProfile) throws {
        try draft.validate()
        if fileStore == nil || syncStore == nil || activeBusinessCardID == nil {
            try createLocalBusinessCard(presentation: CardPresentationStore().value)
        }
        guard let storage = fileStore, let metadataStore = syncStore else { throw ProfileError.damagedFile }
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
        try updateActiveCardSnapshot()
        if !uiTesting { Task { await synchronizeAllBusinessCards() } }
    }

    func createBusinessCard(_ draft: ContactProfile, presentation: CardPresentation = CardPresentation()) throws {
        try draft.validate()
        guard uiTesting || authStatus == .authenticated else { throw CloudError.invalidRequest }
        if !uiTesting && syncInFlight { throw CloudError.invalidRequest }
        try createLocalBusinessCard(presentation: presentation)
        try save(draft)
    }

    private func createLocalBusinessCard(presentation: CardPresentation) throws {
        let ownerID: UUID
        if let signedInOwner = userID {
            ownerID = signedInOwner
        } else if uiTesting {
            ownerID = UUID(uuidString: "00000000-0000-4000-8000-000000000010")!
            userID = ownerID
        } else {
            throw ProfileError.damagedFile
        }

        let indexStore: BusinessCardCatalogStore
        if let catalogStore {
            indexStore = catalogStore
        } else {
            let directory = try accountDirectory(for: ownerID)
            indexStore = BusinessCardCatalogStore(directory: directory, ownerID: ownerID)
            catalogStore = indexStore
        }
        var index: BusinessCardCatalog
        if let catalog {
            index = catalog
        } else {
            index = try indexStore.load()
        }
        guard index.ownerID == ownerID else { throw ProfileError.damagedFile }
        let profileID = UUID()
        index.cards.append(BusinessCardCatalogEntry(
            profileID: profileID,
            ownerID: ownerID,
            isPrimary: index.cards.isEmpty,
            createdAt: "",
            updatedAt: "",
            presentation: presentation
        ))
        index.activeProfileID = profileID
        index.normalize()
        try indexStore.save(index)
        catalog = index
        let directory = indexStore.cardDirectory(profileID)
        var metadata = ProfileSyncMetadata()
        metadata.profileID = profileID
        try ProfileSyncStore(directory: directory).save(metadata)
        try activateBusinessCard(profileID, persistSelection: false)
        try reloadBusinessCards()
    }

    func selectBusinessCard(_ profileID: UUID) {
        guard profileID != activeBusinessCardID else { return }
        do {
            try activateBusinessCard(profileID)
            try reloadBusinessCards()
            if !uiTesting { Task { await synchronizeAllBusinessCards() } }
        } catch {
            message = error.localizedDescription
        }
    }

    func updateActiveCardPresentation(_ presentation: CardPresentation) {
        guard let ownerID = userID, let profileID = activeBusinessCardID,
              var index = catalog, let catalogStore,
              let position = index.cards.firstIndex(where: {
                  $0.ownerID == ownerID && $0.profileID == profileID
              }) else { return }
        guard index.cards[position].presentation != presentation else { return }
        index.cards[position].presentation = presentation
        do {
            try catalogStore.save(index)
            catalog = index
            try reloadBusinessCards()
        } catch {
            message = error.localizedDescription
        }
    }

    @discardableResult
    func deleteActiveBusinessCard() async -> Bool {
        guard !busy, let ownerID = userID, let profileID = activeBusinessCardID,
              var index = catalog, let catalogStore,
              let entry = index.cards.first(where: {
                  $0.ownerID == ownerID && $0.profileID == profileID
              }) else { return false }
        busy = true
        defer { busy = false }
        do {
            let metadata = try syncStore?.load() ?? ProfileSyncMetadata()
            if !uiTesting {
                guard authStatus == .authenticated, let cloud else { throw CloudError.invalidRequest }
                let deleted = try await cloud.deleteProfile(ownerID: ownerID, profileID: profileID)
                if !deleted && metadata.remoteUpdatedAt != nil { throw CloudError.emptyResponse }
            }
            let directory = catalogStore.cardDirectory(entry.profileID)
            if FileManager.default.fileExists(atPath: directory.path) {
                try FileManager.default.removeItem(at: directory)
            }
            index.cards.removeAll { $0.profileID == profileID }
            index.normalize()
            try catalogStore.save(index)
            catalog = index
            if let next = index.activeProfileID {
                try activateBusinessCard(next, persistSelection: false)
            } else {
                fileStore = nil
                syncStore = nil
                profile = ContactProfile()
                activeBusinessCardID = nil
                syncStatus = .localOnly
                profileRevision &+= 1
            }
            try reloadBusinessCards()
            if !uiTesting { try? await refreshBusinessCardCatalog() }
            message = "A névjegyet töröltük."
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
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
            do {
                try await refreshBusinessCardCatalog()
            } catch {
                message = error.localizedDescription
            }
            await synchronizeAllBusinessCards()
            await refreshFeatureFlags()
            profileLoadStatus = hasProfile || syncStatus != .failed ? .ready : .unavailable
        }
    }

    /// Reconciles the owner-scoped catalog whenever the app becomes active or
    /// the user pulls to refresh. This is what makes a card created on Android,
    /// the web, or another iPhone appear without signing out and back in.
    func refreshBusinessCards() async {
        guard !uiTesting, !busy, authStatus == .authenticated else { return }
        do {
            try await refreshBusinessCardCatalog()
            await synchronizeAllBusinessCards()
            profileLoadStatus = .ready
        } catch {
            if businessCards.isEmpty { profileLoadStatus = .unavailable }
            message = error.localizedDescription
        }
    }

    private func refreshBusinessCardCatalog() async throws {
        guard !uiTesting, authStatus == .authenticated,
              let cloud, let ownerID = userID,
              var index = catalog, let catalogStore else { return }
        guard !catalogRefreshInFlight else { return }
        catalogRefreshInFlight = true
        defer { catalogRefreshInFlight = false }

        var localProfiles: [UUID: ContactProfile] = [:]
        for entry in index.cards where entry.ownerID == ownerID {
            let directory = catalogStore.cardDirectory(entry.profileID)
            let metadata = try ProfileSyncStore(directory: directory).load()
            let stored = try ProfileFileStore(directory: directory).load()
            localProfiles[entry.profileID] = try metadata.pendingProfile ?? stored
        }

        let remoteCards = try await cloud.fetchProfiles(ownerID: ownerID, preserving: localProfiles)
        guard userID == ownerID else { throw CloudError.accountMismatch }
        let remoteIDs = Set(remoteCards.map { $0.0.id })

        for (remote, downloaded) in remoteCards {
            guard remote.ownerID == ownerID else { throw CloudError.accountMismatch }
            let directory = catalogStore.cardDirectory(remote.id)
            let profileStore = ProfileFileStore(directory: directory)
            let metadataStore = ProfileSyncStore(directory: directory)
            var metadata = try metadataStore.load()
            guard metadata.profileID == nil || metadata.profileID == remote.id else {
                throw ProfileError.damagedFile
            }

            if metadata.pendingUpload {
                if !ProfileRevisionPolicy.mayUpload(
                    localID: metadata.profileID,
                    localRevision: metadata.remoteUpdatedAt,
                    localFingerprint: metadata.remoteFingerprint,
                    remoteID: remote.id,
                    remoteRevision: remote.updatedAt,
                    remoteFingerprint: remote.fingerprint
                ) {
                    metadata.conflict = true
                    try metadataStore.save(metadata)
                }
            } else {
                try downloaded.validate()
                try profileStore.save(downloaded)
                metadata.profileID = remote.id
                metadata.remoteUpdatedAt = remote.updatedAt
                metadata.remoteFingerprint = remote.fingerprint
                metadata.pendingProfile = nil
                metadata.pendingUpload = false
                metadata.conflict = false
                try metadataStore.save(metadata)
            }

            if let position = index.cards.firstIndex(where: { $0.profileID == remote.id }) {
                index.cards[position].isPrimary = remote.isPrimary == true
                index.cards[position].createdAt = remote.createdAt ?? index.cards[position].createdAt
                index.cards[position].updatedAt = remote.updatedAt
            } else {
                index.cards.append(BusinessCardCatalogEntry(
                    profileID: remote.id,
                    ownerID: ownerID,
                    isPrimary: remote.isPrimary == true,
                    createdAt: remote.createdAt ?? remote.updatedAt,
                    updatedAt: remote.updatedAt,
                    presentation: CardPresentation()
                ))
            }
        }

        let locallyDeleted = index.cards.filter { !remoteIDs.contains($0.profileID) }
        for entry in locallyDeleted {
            let directory = catalogStore.cardDirectory(entry.profileID)
            let metadata = try ProfileSyncStore(directory: directory).load()
            if !metadata.pendingUpload {
                if FileManager.default.fileExists(atPath: directory.path) {
                    try FileManager.default.removeItem(at: directory)
                }
                index.cards.removeAll { $0.profileID == entry.profileID }
            }
        }

        index.normalize()
        try catalogStore.save(index)
        catalog = index
        if let active = index.activeProfileID {
            try activateBusinessCard(active, persistSelection: false)
        } else {
            fileStore = nil
            syncStore = nil
            profile = ContactProfile()
            activeBusinessCardID = nil
            syncStatus = .localOnly
            profileRevision &+= 1
        }
        try reloadBusinessCards()
    }

    func reset() throws {
        guard let ownerID = userID, let catalogStore, let index = catalog else {
            throw ProfileError.damagedFile
        }
        for entry in index.cards where entry.ownerID == ownerID {
            let metadata = try ProfileSyncStore(directory: catalogStore.cardDirectory(entry.profileID)).load()
            if metadata.pendingUpload {
                message = "Van még fel nem töltött módosítás. Előbb szinkronizálj vagy oldd fel az ütközést."
                return
            }
        }
        try catalogStore.reset()
        catalog = BusinessCardCatalog(ownerID: ownerID)
        businessCards = []
        activeBusinessCardID = nil
        fileStore = nil
        syncStore = nil
        profile = ContactProfile()
        profileRevision &+= 1
        storageError = nil
        syncStatus = .localOnly
        if !uiTesting && authStatus == .authenticated {
            profileLoadStatus = .loading
            Task {
                do {
                    try await refreshBusinessCardCatalog()
                    profileLoadStatus = .ready
                } catch {
                    profileLoadStatus = .unavailable
                    message = error.localizedDescription
                }
            }
        }
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
            let remoteBundle = try await cloud.fetchProfile(ownerID: id, profileID: metadata.profileID,
                                                           preserving: localProfile,
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
                    try await cloud.syncSocialProfiles(ownerID: id, profileID: remote.id, value: localProfile,
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
                    try await cloud.syncSocialProfiles(ownerID: id, profileID: remote.id, value: localProfile,
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
                let verified = try await cloud.fetchProfile(ownerID: id, profileID: metadata.profileID,
                                                            preserving: profile, loadPhoto: false)
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
            try updateActiveCardSnapshot()
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

    /// Uploads the active card first, then drains pending edits belonging to
    /// the same signed-in owner without changing the card visible in the UI.
    /// This closes the offline edge case where a user edits one card, switches
    /// to another, and later expects both changes to appear on Android or web.
    private func synchronizeAllBusinessCards() async {
        await synchronize()
        guard !syncInFlight else { return }
        await synchronizeInactiveBusinessCards()
    }

    private func synchronizeInactiveBusinessCards() async {
        guard !uiTesting, !pendingCatalogSyncInFlight, !syncInFlight,
              authStatus == .authenticated, let cloud, let ownerID = userID,
              let catalogStore, let currentCatalog = catalog else { return }
        pendingCatalogSyncInFlight = true
        defer { pendingCatalogSyncInFlight = false }

        let activeID = activeBusinessCardID
        let candidates = currentCatalog.cards.filter {
            $0.ownerID == ownerID && $0.profileID != activeID
        }
        for entry in candidates {
            guard userID == ownerID, authStatus == .authenticated else { return }
            let directory = catalogStore.cardDirectory(entry.profileID)
            let profileStore = ProfileFileStore(directory: directory)
            let metadataStore = ProfileSyncStore(directory: directory)
            do {
                var metadata = try metadataStore.load()
                guard metadata.pendingUpload else { continue }
                guard metadata.profileID == nil || metadata.profileID == entry.profileID else {
                    throw ProfileError.damagedFile
                }
                var local = try metadata.pendingProfile ?? profileStore.load()
                try local.validate()
                let remoteBundle = try await cloud.fetchProfile(
                    ownerID: ownerID, profileID: entry.profileID,
                    preserving: local, loadPhoto: false
                )
                guard userID == ownerID else { throw CloudError.accountMismatch }

                if let remoteBundle {
                    guard ProfileRevisionPolicy.mayUpload(
                        localID: metadata.profileID,
                        localRevision: metadata.remoteUpdatedAt,
                        localFingerprint: metadata.remoteFingerprint,
                        remoteID: remoteBundle.0.id,
                        remoteRevision: remoteBundle.0.updatedAt,
                        remoteFingerprint: remoteBundle.0.fingerprint
                    ) else { throw CloudError.profileConflict }
                    guard let updated = try await cloud.updateProfile(
                        ownerID: ownerID, profileID: entry.profileID, profile: local,
                        expectedUpdatedAt: remoteBundle.0.updatedAt,
                        previousAppearance: remoteBundle.0.appearance,
                        previousLogoBase64: remoteBundle.1.logoBase64,
                        theme: remoteBundle.0.theme
                    ) else { throw CloudError.profileConflict }
                    guard updated.ownerID == ownerID, updated.id == entry.profileID else {
                        throw CloudError.accountMismatch
                    }
                    try await cloud.syncSocialProfiles(
                        ownerID: ownerID, profileID: entry.profileID,
                        value: local, expected: remoteBundle.1
                    )
                } else {
                    // A known server revision disappearing is a deletion
                    // conflict; only a never-uploaded local card may be created.
                    guard metadata.remoteUpdatedAt == nil else { throw CloudError.profileConflict }
                    let created = try await cloud.createProfile(
                        ownerID: ownerID, profileID: entry.profileID, profile: local
                    )
                    guard created.ownerID == ownerID, created.id == entry.profileID else {
                        throw CloudError.accountMismatch
                    }
                    local.publicSlug = created.slug
                    local.customDomain = created.customDomain ?? ""
                    local.customDomainVerified = created.customDomainVerified == true
                    try await cloud.syncSocialProfiles(
                        ownerID: ownerID, profileID: entry.profileID,
                        value: local, expected: ContactProfile()
                    )
                }

                let verified = try await cloud.fetchProfile(
                    ownerID: ownerID, profileID: entry.profileID,
                    preserving: local, loadPhoto: false
                )
                guard userID == ownerID, let verified,
                      verified.0.ownerID == ownerID,
                      verified.0.id == entry.profileID,
                      verified.0.matches(local, remoteLogo: verified.1.logoBase64) else {
                    throw CloudError.profileConflict
                }
                local.publicSlug = verified.1.publicSlug
                local.customDomain = verified.1.customDomain
                local.customDomainVerified = verified.1.customDomainVerified
                local.photoSyncInitialized = true
                local.logoSyncInitialized = true
                try profileStore.save(local)
                metadata.profileID = entry.profileID
                metadata.remoteUpdatedAt = verified.0.updatedAt
                metadata.remoteFingerprint = verified.0.fingerprint
                metadata.pendingProfile = nil
                metadata.pendingUpload = false
                metadata.conflict = false
                try metadataStore.save(metadata)
            } catch let error as CloudError {
                guard userID == ownerID else { return }
                if case .profileConflict = error, var metadata = try? metadataStore.load() {
                    metadata.conflict = true
                    try? metadataStore.save(metadata)
                }
                // Keep the journal pending. Selecting this card surfaces the
                // exact conflict/failure state and its normal resolution UI.
            } catch {
                guard userID == ownerID else { return }
            }
        }
        if userID == ownerID { try? reloadBusinessCards() }
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
            var metadata = try metadataStore.load()
            let remote = try await cloud.fetchProfile(ownerID: id, profileID: metadata.profileID,
                                                      preserving: ContactProfile(), loadPhoto: !keepLocal)
            guard revision == profileRevision, userID == id else { syncStatus = .pending; return }
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
                try updateActiveCardSnapshot()
            }
        } catch {
            syncStatus = .conflict
            message = "Az ütközés feloldása nem sikerült. A helyi változtatásokat megőriztük."
        }
    }

    func dismissMessage() { message = nil }

    private func completeInitialProfileLoad() async {
        do {
            try await refreshBusinessCardCatalog()
        } catch {
            if businessCards.isEmpty {
                syncStatus = .failed
                message = error.localizedDescription
            }
        }
        await synchronizeAllBusinessCards()
        await refreshFeatureFlags()
        profileLoadStatus = hasProfile || syncStatus != .failed ? .ready : .unavailable
    }

    private func refreshFeatureFlags() async {
        guard let cloud, let flags = try? await cloud.fetchFeatureFlags() else { return }
        featureFlags = flags
    }

    private func clearUser() {
        profile = ContactProfile()
        businessCards = []
        activeBusinessCardID = nil
        profileRevision &+= 1
        fileStore = nil
        syncStore = nil
        catalogStore = nil
        catalog = nil
        userID = nil
        userEmail = ""
        storageError = nil
        syncStatus = .localOnly
        profileLoadStatus = .loading
        featureFlags = AppFeatureFlags()
        syncInFlight = false
        syncAgain = false
        catalogRefreshInFlight = false
        pendingCatalogSyncInFlight = false
        syncFailureMessage = nil
    }

    private func hasPendingBusinessCardUploads() -> Bool {
        guard let ownerID = userID, let catalogStore, let catalog else { return false }
        return catalog.cards.contains { entry in
            guard entry.ownerID == ownerID else { return true }
            return (try? ProfileSyncStore(directory: catalogStore.cardDirectory(entry.profileID))
                .load().pendingUpload) != false
        }
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

            case .signedOut:
                AuthScreen().id("auth-clean-login")

            case .verificationSent(let email):
                RegistrationVerificationScreen(email: email) {
                    store.returnToLoginAfterRegistration()
                }.id("auth-verification")

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

struct RootView: View {
    @EnvironmentObject private var store: AppStore
    @EnvironmentObject private var presentation: CardPresentationStore
    @Binding var themeMode: ThemeMode

    var body: some View {
        HomeScreen(themeMode: $themeMode)
            .tint(VizitColor.primary)
        .onAppear { presentation.value = store.activeCardPresentation }
        .onChange(of: store.activeBusinessCardID) { _ in
            presentation.value = store.activeCardPresentation
        }
        .onChange(of: presentation.value) { value in
            store.updateActiveCardPresentation(value)
        }
    }
}
