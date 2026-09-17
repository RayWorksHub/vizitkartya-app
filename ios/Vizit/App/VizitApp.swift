import SwiftUI

@main
struct VizitApp: App {
    @StateObject private var store = AppStore()

    var body: some Scene {
        WindowGroup {
            RootView().environmentObject(store).tint(Brand.blue)
        }
    }
}

@MainActor
final class AppStore: ObservableObject {
    @Published private(set) var profile = ContactProfile()
    @Published private(set) var storageError: String?
    private var fileStore: ProfileFileStore?

    init() {
        do {
            var directory = try FileManager.default.url(for: .applicationSupportDirectory,
                in: .userDomainMask, appropriateFor: nil, create: true).appendingPathComponent("VIZIT", isDirectory: true)
            #if DEBUG
            // UI tests never touch the user's real local profile.
            if ProcessInfo.processInfo.arguments.contains("--ui-testing") {
                directory = directory.appendingPathComponent("UITests", isDirectory: true)
            }
            #endif
            let storage = ProfileFileStore(directory: directory)
            fileStore = storage
            #if DEBUG
            if ProcessInfo.processInfo.arguments.contains("--reset-test-profile") &&
                ProcessInfo.processInfo.arguments.contains("--ui-testing") { try storage.reset() }
            #endif
            profile = try storage.load()
        } catch {
            storageError = error.localizedDescription
        }
    }

    func save(_ draft: ContactProfile) throws {
        guard let storage = fileStore else { throw ProfileError.damagedFile }
        try storage.save(draft)
        profile = draft.normalized
        storageError = nil
    }

    func reset() throws {
        guard let storage = fileStore else { throw ProfileError.damagedFile }
        try storage.reset()
        profile = ContactProfile()
        storageError = nil
    }

    var hasProfile: Bool { !profile.displayName.isEmpty }
}

enum Brand {
    static let navy = Color(red: 6 / 255, green: 27 / 255, blue: 70 / 255)
    static let blue = Color(red: 5 / 255, green: 94 / 255, blue: 236 / 255)
    static let cyan = Color(red: 19 / 255, green: 209 / 255, blue: 252 / 255)
}

struct RootView: View {
    var body: some View {
        TabView {
            HomeScreen().tabItem { Label("Névjegy", systemImage: "person.crop.rectangle") }
            ShareScreen().tabItem { Label("Megosztás", systemImage: "qrcode") }
            ScanScreen().tabItem { Label("Beolvasás", systemImage: "qrcode.viewfinder") }
            SettingsScreen().tabItem { Label("Beállítások", systemImage: "gearshape") }
        }
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
                Text(profile.initials).font(.system(size: size * 0.34, weight: .bold))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Brand.blue.opacity(0.12))
            }
        }
        .frame(width: size, height: size).clipShape(Circle())
        .accessibilityLabel("Profilkép: \(profile.displayName)")
    }
}

struct PreviewNotice: View {
    var body: some View {
        Label("Helyi iOS tesztverzió – felhőszinkron nélkül", systemImage: "iphone")
            .font(.footnote).foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}
