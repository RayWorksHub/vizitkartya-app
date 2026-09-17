import SwiftUI
import Supabase

@main
struct VizitApp: App {
    @StateObject private var store = AppStore()

    var body: some Scene {
        WindowGroup {
            AppGate().environmentObject(store).tint(Brand.blue)
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
        case .syncing: return "Biztonságos szinkron folyamatban…"
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

    private(set) var configuration: AppConfiguration?
    private var cloud: CloudService?
    private var fileStore: ProfileFileStore?
    private var syncStore: ProfileSyncStore?
    private var userID: UUID?
    private var userEmail = ""
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
        let directory = try Self.applicationDirectory()
            .appendingPathComponent("users", isDirectory: true)
            .appendingPathComponent(id.uuidString.lowercased(), isDirectory: true)
        let profileStore = ProfileFileStore(directory: directory)
        let metadataStore = ProfileSyncStore(directory: directory)
        fileStore = profileStore
        syncStore = metadataStore
        userID = id
        profile = try profileStore.load()
        _ = try metadataStore.load()
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
            await synchronize()
        } catch {
            authStatus = .offline
            syncStatus = .pending
            message = "Nincs hálózati kapcsolat. A helyi névjegyed olvasható és szerkeszthető; a feltöltés később újrapróbálható."
        }
    }

    func login(email: String, password: String) async {
        if let issue = AuthValidation.login(email: email, password: password) { message = issue; return }
        await performAuth(defaultError: "A bejelentkezés nem sikerült. Ellenőrizd az adatokat és a kapcsolatot.") {
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
        await performAuth(defaultError: "A regisztráció nem sikerült. Próbáld újra később.") {
            guard let cloud = self.cloud else { return }
            try await cloud.register(name: name, email: email, password: password)
            self.authStatus = .verificationSent(email.trimmingCharacters(in: .whitespacesAndNewlines))
            self.message = "Megerősítő e-mailt küldtünk. Bejelentkezés előtt nyisd meg a benne lévő hivatkozást."
        }
    }

    func googleLogin() async {
        await performAuth(defaultError: "A Google-bejelentkezés megszakadt vagy nem sikerült.") {
            guard let cloud = self.cloud else { return }
            let session = try await cloud.googleLogin()
            try self.configureStorage(for: session.user.id)
            self.userEmail = session.user.email ?? ""
            self.authStatus = .authenticated
            await self.synchronize()
        }
    }

    func requestPasswordReset(email: String) async {
        if let issue = AuthValidation.email(email) { message = issue; return }
        await performAuth(defaultError: "A jelszó-visszaállító e-mail küldése nem sikerült.") {
            try await self.cloud?.requestPasswordReset(email: email)
            self.message = "Ha a címhez tartozik fiók, elküldtük a jelszó-visszaállító e-mailt."
        }
    }

    func handleCallback(_ url: URL) async {
        await performAuth(defaultError: "A bejelentkezési hivatkozás lejárt vagy érvénytelen.") {
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
        await performAuth(defaultError: "A jelszó módosítása nem sikerült.") {
            try await self.cloud?.changePassword(password)
            self.authStatus = .authenticated
            self.message = "A jelszavadat biztonságosan módosítottuk."
            await self.synchronize()
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
        try? fileStore?.reset()
        try? syncStore?.reset()
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
        metadata.pendingUpload = !uiTesting
        metadata.conflict = false
        try metadataStore.save(metadata)
        try storage.save(draft)
        profile = draft.normalized
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
        }
    }

    func reset() throws {
        guard let storage = fileStore else { throw ProfileError.damagedFile }
        try storage.reset()
        try syncStore?.reset()
        profile = ContactProfile()
        storageError = nil
        syncStatus = .localOnly
    }

    func synchronize() async {
        guard !uiTesting, authStatus == .authenticated, let cloud, let id = userID,
              let storage = fileStore, let metadataStore = syncStore, storageError == nil else { return }
        syncStatus = .syncing
        do {
            var metadata = try metadataStore.load()
            let remoteBundle = try await cloud.fetchProfile(ownerID: id, preserving: profile)

            if metadata.pendingUpload {
                if let remoteBundle {
                    guard metadata.profileID == remoteBundle.0.id,
                          metadata.remoteUpdatedAt == remoteBundle.0.updatedAt else {
                        metadata.conflict = true
                        try metadataStore.save(metadata)
                        syncStatus = .conflict
                        return
                    }
                    guard let remote = try await cloud.updateProfile(ownerID: id, profileID: remoteBundle.0.id,
                                                                     profile: profile,
                                                                     expectedUpdatedAt: remoteBundle.0.updatedAt) else {
                        metadata.conflict = true
                        try metadataStore.save(metadata)
                        syncStatus = .conflict
                        return
                    }
                    metadata.profileID = remote.id
                    metadata.remoteUpdatedAt = remote.updatedAt
                } else {
                    let remote = try await cloud.createProfile(ownerID: id, profile: profile)
                    metadata.profileID = remote.id
                    metadata.remoteUpdatedAt = remote.updatedAt
                    var local = profile
                    local.publicSlug = remote.slug
                    try storage.save(local)
                    profile = local
                }
                metadata.pendingUpload = false
                metadata.conflict = false
                try metadataStore.save(metadata)
                syncStatus = .synced
            } else if let (remote, pulled) = remoteBundle {
                try pulled.validate()
                try storage.save(pulled)
                profile = pulled
                metadata.profileID = remote.id
                metadata.remoteUpdatedAt = remote.updatedAt
                metadata.conflict = false
                try metadataStore.save(metadata)
                syncStatus = .synced
            } else {
                syncStatus = hasProfile ? .pending : .synced
            }
        } catch {
            syncStatus = .failed
            message = "A névjegyet helyben megőriztük, de a felhőszinkron most nem sikerült."
        }
    }

    func dismissMessage() { message = nil }

    private func clearUser() {
        profile = ContactProfile()
        fileStore = nil
        syncStore = nil
        userID = nil
        userEmail = ""
        storageError = nil
        syncStatus = .localOnly
    }

    private func performAuth(defaultError: String, operation: () async throws -> Void) async {
        guard !busy else { return }
        busy = true
        message = nil
        defer { busy = false }
        do { try await operation() }
        catch let error as CloudError { message = error.localizedDescription }
        catch let error as ConfigurationError { message = error.localizedDescription }
        catch { message = defaultError }
    }
}

struct AppGate: View {
    @EnvironmentObject private var store: AppStore

    var body: some View {
        Group {
            switch store.authStatus {
            case .launching:
                ZStack {
                    Brand.heroGradient.ignoresSafeArea()
                    VStack(spacing: 20) {
                        VizitBrandLockup(height: 92)
                            .frame(maxWidth: 300)
                        ProgressView()
                            .controlSize(.large)
                            .tint(Brand.cyan)
                        Text("Biztonságos munkamenet ellenőrzése…")
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(.white.opacity(0.72))
                    }
                    .padding(28)
                }
                .preferredColorScheme(.dark)
            case .signedOut, .verificationSent:
                AuthScreen()
            case .passwordRecovery:
                PasswordChangeScreen()
            case .configurationError(let detail):
                ZStack {
                    VizitScreenBackground()
                    VizitCard {
                        VStack(spacing: 18) {
                            Image(systemName: "exclamationmark.shield.fill")
                                .font(.system(size: 52))
                                .foregroundStyle(.red)
                            Text("Ez a build nem használható").font(.title2.bold())
                            Text(detail).foregroundStyle(.secondary).multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .padding(24)
                    .frame(maxWidth: 520)
                }
            case .authenticated, .offline:
                RootView()
            }
        }
        .alert("VIZIT", isPresented: Binding(get: { store.message != nil },
                                              set: { if !$0 { store.dismissMessage() } })) {
            Button("Rendben", role: .cancel) { store.dismissMessage() }
        } message: { Text(store.message ?? "") }
    }
}

enum Brand {
    static let navy = Color(red: 6 / 255, green: 27 / 255, blue: 70 / 255)
    static let blue = Color(red: 5 / 255, green: 94 / 255, blue: 236 / 255)
    static let cyan = Color(red: 19 / 255, green: 209 / 255, blue: 252 / 255)
    static let cyanLight = Color(red: 139 / 255, green: 233 / 255, blue: 255 / 255)
    static let ice = Color(red: 234 / 255, green: 248 / 255, blue: 255 / 255)
    static let cloud = Color(red: 245 / 255, green: 247 / 255, blue: 249 / 255)
    static let darkSurface = Color(red: 8 / 255, green: 35 / 255, blue: 92 / 255)

    static let heroGradient = LinearGradient(
        colors: [navy, Color(red: 4 / 255, green: 52 / 255, blue: 132 / 255), blue],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    static let actionGradient = LinearGradient(
        colors: [blue, Color(red: 20 / 255, green: 116 / 255, blue: 255 / 255)],
        startPoint: .leading,
        endPoint: .trailing
    )

    static let canvas = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 3 / 255, green: 13 / 255, blue: 34 / 255, alpha: 1)
            : UIColor(red: 245 / 255, green: 247 / 255, blue: 249 / 255, alpha: 1)
    })

    static let surface = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 8 / 255, green: 28 / 255, blue: 66 / 255, alpha: 1)
            : .white
    })

    static let elevatedSurface = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 12 / 255, green: 39 / 255, blue: 88 / 255, alpha: 1)
            : UIColor(red: 250 / 255, green: 252 / 255, blue: 255 / 255, alpha: 1)
    })

    static let border = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor.white.withAlphaComponent(0.14)
            : UIColor(red: 6 / 255, green: 27 / 255, blue: 70 / 255, alpha: 0.10)
    })
}

struct VizitBrandLockup: View {
    var height: CGFloat = 94
    var padded = true

    private static let image: UIImage? = {
        guard let url = Bundle.main.url(forResource: "VizitLogo", withExtension: "png") else { return nil }
        return UIImage(contentsOfFile: url.path)
    }()

    var body: some View {
        Group {
            if let image = Self.image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
            } else {
                VStack(spacing: 3) {
                    Text("VIZIT")
                        .font(.system(size: 30, weight: .black, design: .rounded))
                        .tracking(7)
                        .foregroundStyle(Brand.navy)
                    Text("EGY ÉRINTÉS. EGY KAPCSOLAT.")
                        .font(.system(size: 8, weight: .bold))
                        .tracking(2)
                        .foregroundStyle(Brand.blue)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: height)
        .padding(padded ? 14 : 0)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .stroke(Color.white.opacity(0.2), lineWidth: 1)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("VIZIT – Egy érintés. Egy kapcsolat.")
    }
}

struct VizitMark: View {
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(Color.white)
            Image(systemName: "link")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(Brand.actionGradient)
        }
        .frame(width: 54, height: 46)
        .accessibilityLabel("VIZIT")
    }
}

struct VizitScreenBackground: View {
    var body: some View {
        ZStack {
            Brand.canvas
            Circle()
                .fill(Brand.blue.opacity(0.08))
                .frame(width: 280, height: 280)
                .blur(radius: 2)
                .offset(x: 170, y: -330)
        }
        .ignoresSafeArea()
    }
}

struct VizitCard<Content: View>: View {
    private let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Brand.surface)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(Brand.border, lineWidth: 1)
            }
            .shadow(color: Brand.navy.opacity(0.08), radius: 18, y: 8)
    }
}

struct VizitPrimaryButtonLabel: View {
    let title: String
    var systemImage: String?
    var busy = false

    var body: some View {
        HStack(spacing: 10) {
            if busy {
                ProgressView().tint(.white)
            } else if let systemImage {
                Image(systemName: systemImage).font(.headline)
            }
            Text(title).font(.headline.weight(.bold))
        }
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity)
        .frame(height: 56)
        .background(Brand.actionGradient)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .shadow(color: Brand.blue.opacity(0.25), radius: 14, y: 7)
    }
}

struct RootView: View {
    var body: some View {
        TabView {
            HomeScreen().tabItem { Label("Névjegy", systemImage: "person.crop.rectangle") }
            ShareScreen().tabItem { Label("Megosztás", systemImage: "qrcode") }
            ScanScreen().tabItem { Label("Beolvasás", systemImage: "qrcode.viewfinder") }
            SettingsScreen().tabItem { Label("Beállítások", systemImage: "gearshape") }
        }
        .tint(Brand.blue)
        .toolbarBackground(Brand.surface, for: .tabBar)
        .toolbarBackground(.visible, for: .tabBar)
    }
}

struct ProfileAvatar: View {
    let profile: ContactProfile
    var size: CGFloat = 72
    var body: some View {
        Group {
            if let bytes = Data(base64Encoded: profile.photoBase64), let image = UIImage(data: bytes) {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                Text(profile.initials.isEmpty ? "V" : profile.initials)
                    .font(.system(size: size * 0.34, weight: .bold, design: .rounded))
                    .foregroundStyle(Brand.navy)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(
                        LinearGradient(colors: [Brand.cyanLight, Brand.ice],
                                       startPoint: .topLeading, endPoint: .bottomTrailing)
                    )
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay { Circle().stroke(Color.white.opacity(0.75), lineWidth: 3) }
        .shadow(color: Brand.navy.opacity(0.16), radius: 10, y: 5)
        .accessibilityLabel("Profilkép: \(profile.displayName)")
    }
}

struct PreviewNotice: View {
    @EnvironmentObject private var store: AppStore
    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Label(store.syncStatus.label, systemImage: statusIcon)
            Spacer()
            if [.pending, .conflict, .failed].contains(store.syncStatus), store.isOnline {
                Button("Újra") { store.retrySync() }
                    .buttonStyle(.borderless)
                    .fontWeight(.semibold)
            }
        }
        .font(.footnote)
        .foregroundStyle(store.syncStatus == .conflict ? .orange : .secondary)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 11)
        .background(Brand.surface)
        .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 15, style: .continuous)
                .stroke(Brand.border, lineWidth: 1)
        }
    }

    private var statusIcon: String {
        if store.authStatus == .offline { return "wifi.slash" }
        switch store.syncStatus {
        case .syncing: return "arrow.triangle.2.circlepath"
        case .conflict, .failed: return "exclamationmark.triangle.fill"
        case .pending: return "clock.fill"
        case .localOnly: return "iphone"
        case .synced: return "checkmark.shield.fill"
        }
    }
}
