import SwiftUI
import Supabase
import UIKit

@main
struct VizitApp: App {
    @StateObject private var store = AppStore()
    @StateObject private var presentation = CardPresentationStore()
    @StateObject private var feedback = VizitFeedbackCenter()

    var body: some Scene {
        WindowGroup {
            AppGate()
                .environmentObject(store)
                .environmentObject(presentation)
                .environmentObject(feedback)
                .tint(VizitColor.primary)
                .onOpenURL { url in Task { await store.handleCallback(url) } }
                .onChange(of: store.authStatus) { status in
                    switch status {
                    case .authenticated, .offline: break
                    default: feedback.dismiss()
                    }
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
    case sessionExpired
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

@MainActor
final class AppStore: ObservableObject {
    @Published private(set) var profile = ContactProfile()
    @Published private(set) var authStatus: AuthStatus = .launching
    @Published private(set) var syncStatus: SyncStatus = .localOnly
    @Published private(set) var storageError: String?
    @Published private(set) var message: String?
    @Published private(set) var busy = false
    @Published private(set) var automaticSyncEnabled =
        UserDefaults.standard.object(forKey: "figma.automaticSyncEnabled") as? Bool ?? true
    private var authWatch: Task<Void, Never>?
    private var intentionalSignOut = false

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
    private var syncRetryTask: Task<Void, Never>?
    private var syncRetryAttempt = 0
    private let uiTesting: Bool

    init() {
        #if DEBUG
        let launchArguments = ProcessInfo.processInfo.arguments
        uiTesting = launchArguments.contains("--ui-testing")
        #else
        uiTesting = false
        #endif
        if uiTesting {
            #if DEBUG
            if launchArguments.contains("--ui-testing-signed-out") {
                configuration = try? AppConfiguration.load()
                authStatus = .signedOut
                syncStatus = .localOnly
                return
            }
            if launchArguments.contains("--ui-testing-verification") {
                authStatus = .verificationSent("teszt@vizit.hu")
                syncStatus = .localOnly
                return
            }
            do {
                let directory = try Self.applicationDirectory().appendingPathComponent("UITests", isDirectory: true)
                let storage = ProfileFileStore(directory: directory)
                fileStore = storage
                syncStore = ProfileSyncStore(directory: directory)
                if launchArguments.contains("--reset-test-profile") {
                    try storage.reset()
                    try? syncStore?.reset()
                }
                if launchArguments.contains("--ui-testing-release-profile") {
                    profile = Self.releaseAuditProfile(arguments: launchArguments)
                    configuration = try? AppConfiguration.load()
                    userEmail = "csukardi.rajmund@gmail.com"
                    syncStatus = launchArguments.contains("--ui-testing-profile-qr-unavailable")
                        ? .pending
                        : .synced
                } else {
                    profile = try storage.load()
                    syncStatus = .localOnly
                }
                authStatus = .authenticated
            } catch {
                storageError = error.localizedDescription
                authStatus = .authenticated
            }
            #endif
            return
        }

        do {
            let config = try AppConfiguration.load()
            configuration = config
            let service = CloudService(configuration: config)
            cloud = service
            observeSession(service)
            Task { await bootstrap() }
        } catch {
            authStatus = .configurationError(error.localizedDescription)
        }
    }

    var hasProfile: Bool { !profile.displayName.isEmpty }
    var accountEmail: String { userEmail }
    var accountIdentifier: UUID? { userID }
    var isOnline: Bool { authStatus == .authenticated }
    var syncErrorDescription: String? { syncFailureMessage }

    func setAutomaticSync(_ enabled: Bool) {
        automaticSyncEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: "figma.automaticSyncEnabled")
        if enabled { retrySync() }
        else { resetSyncRetry() }
    }
    private func observeSession(_ service: CloudService) {
        authWatch = Task { [weak self] in
            for await (event, session) in await service.client.auth.authStateChanges {
                guard !Task.isCancelled, let self else { return }
                if event == .signedOut, !self.intentionalSignOut, self.userID != nil {
                    switch self.authStatus {
                    case .authenticated, .offline, .passwordRecovery: self.sessionExpired()
                    default: break
                    }
                } else if event == .userUpdated, session?.user.id == self.userID {
                    self.objectWillChange.send()
                    self.userEmail = session?.user.email ?? self.userEmail
                }
            }
        }
    }
    private func isExpiredSession(_ error: Error) -> Bool {
        if let cloudError = error as? CloudError {
            if case .authenticationRequired = cloudError { return true }
            if case .server(let status, _) = cloudError, status == 401 { return true }
        }
        guard let auth = error as? AuthError else { return false }
        if case .sessionMissing = auth { return true }
        return ["session_not_found", "refresh_token_not_found", "refresh_token_already_used", "bad_jwt", "session_expired"]
            .contains(auth.errorCode.rawValue)
    }
    private func sessionExpired() {
        resetSyncRetry()
        syncAgain = false
        message = nil
        authStatus = .sessionExpired
        syncStatus = .pending
        // Retain the account-isolated unsent draft, hidden behind AppGate.
    }

    #if DEBUG
    /// A complete, deterministic profile used only by UI release-audit tests.
    /// It never enters a signed build and never touches Supabase.
    private static func releaseAuditProfile(arguments: [String]) -> ContactProfile {
        var value = ContactProfile()
        value.fullName = "Csukárdi Rajmund"
        value.firstName = "Rajmund"
        value.lastName = "Csukárdi"
        value.jobTitle = "CEO"
        value.company = "RayWorks | Solutions Kft."
        value.phone = "+36 70 298 0003"
        value.email = "csukardi.rajmund@gmail.com"
        value.website = "https://rayworks.hu"
        value.address = "Budapest"
        value.linkedIn = "https://linkedin.com/in/csukardi-rajmund"
        value.facebook = "https://facebook.com/csukardi.rajmund"
        value.instagram = "https://instagram.com/csukardi.rajmund"
        value.tiktok = "https://tiktok.com/@csukardi.rajmund"
        value.youtube = "https://youtube.com/@rayworks"
        value.photoSyncInitialized = true
        value.publicSlug = "csukardi-rajmund"
        value.isPublic = !arguments.contains("--ui-testing-profile-qr-unavailable")
        value.customDomain = arguments.contains("--ui-testing-domain-empty")
            ? ""
            : (arguments.contains("--ui-testing-domain-invalid")
                ? "https://nevjegy.cegem.hu/profil"
                : "nevjegy.cegem.hu")
        value.customDomainVerified = arguments.contains("--ui-testing-domain-verified")

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = true
        let image = UIGraphicsImageRenderer(size: CGSize(width: 96, height: 96), format: format).image { context in
            UIColor(red: 0.07, green: 0.22, blue: 0.43, alpha: 1).setFill()
            context.fill(CGRect(x: 0, y: 0, width: 96, height: 96))
            let initials = "CR" as NSString
            initials.draw(
                at: CGPoint(x: 20, y: 31),
                withAttributes: [
                    .font: UIFont.systemFont(ofSize: 28, weight: .bold),
                    .foregroundColor: UIColor.white,
                ]
            )
        }
        value.photoBase64 = arguments.contains("--ui-testing-photo-empty")
            ? ""
            : (image.jpegData(compressionQuality: 0.72)?.base64EncodedString() ?? "")
        return value
    }
    #endif

    func showSignIn() {
        message = nil
        authStatus = .signedOut
    }

    private static func applicationDirectory() throws -> URL {
        try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask,
                                    appropriateFor: nil, create: true)
            .appendingPathComponent("VIZIT", isDirectory: true)
    }

    private func configureStorage(for id: UUID) throws {
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
            if automaticSyncEnabled { await synchronize(automatically: true) }
            else { syncStatus = (try? syncStore?.load().pendingUpload) == true ? .pending : .localOnly }
        } catch {
            if isExpiredSession(error) { sessionExpired(); return }
            authStatus = .offline
            syncStatus = .pending
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
            await self.synchronize()
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
        }
    }

    @discardableResult
    func resendVerification(email: String) async -> Bool {
        if let issue = AuthValidation.email(email) { message = issue; return false }
        return await performAuth(
            operation: .registration,
            defaultError: "A megerősítő levél újraküldése nem sikerült. Próbáld újra később."
        ) {
            guard let cloud = self.cloud else { return }
            try await cloud.resendSignupVerification(email: email)
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
            await self.synchronize()
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
            let recovery = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?
                .contains(where: { $0.name == "flow" && $0.value == "recovery" }) == true
            let session = try await cloud.handleCallback(url)
            try self.configureStorage(for: session.user.id)
            self.userEmail = session.user.email ?? ""
            self.authStatus = recovery ? .passwordRecovery : .authenticated
            if !recovery { await self.synchronize() }
        }
    }

    func changePassword(_ password: String, confirmation: String) async {
        if let issue = AuthValidation.passwordChange(password, confirmation: confirmation) { message = issue; return }
        await performAuth(operation: .passwordChange,
                          defaultError: "A jelszó módosítása nem sikerült.") {
            try await self.cloud?.changePassword(password)
            self.authStatus = .authenticated
            self.message = "A jelszavad megváltozott."
            await self.synchronize()
        }
    }

    @discardableResult
    func changeEmail(_ email: String) async -> Bool {
        if let issue = AuthValidation.email(email) { message = issue; return false }
        return await performAuth(
            defaultError: "Az e-mail-cím módosítása nem sikerült. Ellenőrizd a kapcsolatot."
        ) {
            try await self.cloud?.changeEmail(email)
            self.message = "Megerősítő levelet küldtünk az új e-mail-címre. A cím csak a jóváhagyás után változik meg."
        }
    }

    func logout() async {
        busy = true
        intentionalSignOut = true
        defer { busy = false; intentionalSignOut = false }
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
            self.intentionalSignOut = true
            defer { self.intentionalSignOut = false }
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
        if !uiTesting && automaticSyncEnabled { Task { await synchronize(automatically: true) } }
    }

    func retrySync() {
        Task {
            if authStatus == .offline, let cloud {
                do {
                    let session = try await cloud.validSession()
                    userEmail = session.user.email ?? ""
                    authStatus = .authenticated
                } catch {
                    if isExpiredSession(error) { sessionExpired(); return }
                    message = "Még nincs hálózati kapcsolat. A helyi névjegy változatlanul megmaradt."
                    return
                }
            }
            await synchronize()
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

    func synchronize(automatically: Bool = false) async {
        guard !automatically || automaticSyncEnabled else { return }
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
                Task { await self.synchronize(automatically: automatically) }
            }
        }
        if message == syncFailureMessage { message = nil }
        syncFailureMessage = nil
        let revision = profileRevision
        let localProfile = profile
        syncStatus = .syncing
        do {
            var metadata = try metadataStore.load()
            let restoresLegacyDraft = ProfileBootstrapPolicy.shouldRestoreRemote(
                pendingUpload: metadata.pendingUpload,
                local: localProfile
            )
            let remoteBundle = try await cloud.fetchProfile(ownerID: id, preserving: localProfile,
                loadPhoto: ProfileBootstrapPolicy.shouldDownloadPhoto(
                    pendingUpload: metadata.pendingUpload,
                    local: localProfile
                )
            )
            guard userID == id else { return }
            guard profileRevision == revision else {
                syncAgain = true
                syncStatus = .pending
                return
            }

            // Builds before cloud-first bootstrap could mark an empty local
            // placeholder as pending. Uploading it would attempt to replace a
            // valid cloud profile with an invalid blank record and leave the
            // app permanently in the failed state. Recover the authoritative
            // remote profile before any write is attempted.
            if restoresLegacyDraft, let (remote, pulled) = remoteBundle {
                try pulled.validate()
                try storage.save(pulled)
                profile = pulled
                profileRevision &+= 1
                metadata.profileID = remote.id
                metadata.remoteUpdatedAt = remote.updatedAt
                metadata.remoteFingerprint = remote.fingerprint
                metadata.pendingUpload = false
                metadata.pendingProfile = nil
                metadata.conflict = false
                try metadataStore.save(metadata)
                syncStatus = .synced
                resetSyncRetry()
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
                    let uploadProfile = localProfile.preparedForUpload(
                        existingRemoteSlug: remoteBundle.0.slug
                    )
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
                                                                     profile: uploadProfile,
                                                                     expectedUpdatedAt: remoteBundle.0.updatedAt) else {
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
                    if profileRevision == revision {
                        metadata.pendingProfile = uploadProfile
                    }
                    try metadataStore.save(metadata)
                    guard userID == id else { return }
                    guard profileRevision == revision else {
                        metadata.pendingUpload = true
                        try metadataStore.save(metadata)
                        syncAgain = true
                        syncStatus = .pending
                        return
                    }
                    try await cloud.syncSocialProfiles(profileID: remote.id, value: uploadProfile,
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
                    metadata = try metadataStore.load()
                    metadata.pendingProfile = local
                    try metadataStore.save(metadata)
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
                let expectedProfile = profile.preparedForUpload(
                    existingRemoteSlug: verified?.0.slug ?? ""
                )
                guard let verified, verified.0.matches(expectedProfile) else { throw CloudError.profileConflict }
                metadata.remoteUpdatedAt = verified.0.updatedAt
                metadata.remoteFingerprint = verified.0.fingerprint
                var photoSynced = expectedProfile
                photoSynced.photoSyncInitialized = true
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
                resetSyncRetry()
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
                resetSyncRetry()
            } else {
                if hasProfile {
                    metadata.pendingUpload = true
                    metadata.pendingProfile = localProfile
                    metadata.conflict = metadata.profileID != nil
                    try metadataStore.save(metadata)
                    syncStatus = metadata.conflict ? .conflict : .pending
                    if !metadata.conflict { syncAgain = true }
                } else {
                    syncStatus = .localOnly
                    resetSyncRetry()
                }
            }
        } catch let error as CloudError {
            guard userID == id else { return }
            if isExpiredSession(error) { sessionExpired(); return }
            if case .profileConflict = error {
                if var latest = try? metadataStore.load() {
                    latest.conflict = true
                    try? metadataStore.save(latest)
                }
                syncStatus = .conflict
            } else if error.isRetryable {
                syncStatus = .pending
                scheduleSyncRetry()
            } else {
                syncStatus = .failed
            }
            let text = error.localizedDescription
            syncFailureMessage = text
            message = text
        } catch {
            guard userID == id else { return }
            if isExpiredSession(error) { sessionExpired(); return }
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

    private func clearUser() {
        syncRetryTask?.cancel()
        syncRetryTask = nil
        syncRetryAttempt = 0
        profile = ContactProfile()
        profileRevision &+= 1
        fileStore = nil
        syncStore = nil
        userID = nil
        userEmail = ""
        storageError = nil
        syncStatus = .localOnly
    }

    private func scheduleSyncRetry() {
        guard automaticSyncEnabled, syncRetryTask == nil, authStatus == .authenticated else { return }
        let delays: [UInt64] = [3, 10, 30, 120]
        let delay = delays[min(syncRetryAttempt, delays.count - 1)]
        syncRetryAttempt = min(syncRetryAttempt + 1, delays.count - 1)
        syncRetryTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: delay * 1_000_000_000)
            guard !Task.isCancelled, let self else { return }
            self.syncRetryTask = nil
            await self.synchronize(automatically: true)
        }
    }

    private func resetSyncRetry() {
        syncRetryTask?.cancel()
        syncRetryTask = nil
        syncRetryAttempt = 0
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
    @State private var themeMode: ThemeMode

    init() {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if arguments.contains("--ui-testing-theme-dark") {
            _themeMode = State(initialValue: .dark)
        } else if arguments.contains("--ui-testing") {
            // UI evidence must not inherit a theme written by another test.
            _themeMode = State(initialValue: .light)
        } else {
            _themeMode = State(initialValue: ThemeStorage.current)
        }
        #else
        _themeMode = State(initialValue: ThemeStorage.current)
        #endif
    }

    var body: some View {
        Group {
            switch store.authStatus {
            case .launching:
                LaunchScreen()

            case .signedOut:
                AuthScreen()

            case .sessionExpired:
                VizitScreen { VizitSessionExpiredState(login: store.showSignIn) }

            case .verificationSent(let email):
                EmailVerificationScreen(email: email)

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
                RootView(themeMode: $themeMode)
            }
        }
        .preferredColorScheme(themeMode.colorScheme)
        .overlay(alignment: .topLeading) {
            ColorSchemeProbe()
        }
        .alert("VIZIT", isPresented: Binding(
            get: { store.message != nil },
            set: { if !$0 { store.dismissMessage() } }
        )) {
            Button("Rendben", role: .cancel) { store.dismissMessage() }
        } message: { Text(store.message ?? "") }
    }
}

private struct ColorSchemeProbe: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Text(colorScheme == .dark ? "DARK" : "LIGHT")
            .font(.system(size: 1))
            .foregroundStyle(Color.clear)
            .frame(width: 1, height: 1)
            .accessibilityIdentifier("app.colorScheme")
            .accessibilityHidden(!ProcessInfo.processInfo.arguments.contains("--ui-testing"))
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
                VizitLoadingState(message: "A VIZIT indítása…")
                    .environment(\.colorScheme, .dark)
                    .frame(maxHeight: 150)
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
        .safeAreaInset(edge: .bottom, spacing: 0) { VizitFeedbackHost() }
        .tint(VizitColor.primary)
        .toolbarBackground(VizitColor.surface, for: .tabBar)
        .toolbarBackground(.visible, for: .tabBar)
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
