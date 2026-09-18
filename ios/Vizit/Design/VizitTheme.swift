import SwiftUI

/// User-selectable appearance. Light is the product default on both platforms.
enum ThemeMode: String, CaseIterable, Identifiable {
    case light = "LIGHT"
    case dark = "DARK"
    case system = "SYSTEM"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .light: return "Világos"
        case .dark: return "Sötét"
        case .system: return "Rendszer"
        }
    }

    /// nil means "follow the system", which is what SwiftUI expects.
    var colorScheme: ColorScheme? {
        switch self {
        case .light: return .light
        case .dark: return .dark
        case .system: return nil
        }
    }

    static func fromStorage(_ value: String?) -> ThemeMode {
        guard let value, let mode = ThemeMode(rawValue: value) else { return .light }
        return mode
    }
}

/// Stored under the same key name as the Android DataStore entry so the two
/// platforms describe the setting identically.
enum ThemeStorage {
    static let key = "appearance"

    static var current: ThemeMode {
        ThemeMode.fromStorage(UserDefaults.standard.string(forKey: key))
    }

    static func save(_ mode: ThemeMode) {
        UserDefaults.standard.set(mode.rawValue, forKey: key)
    }
}

/// Applies the VIZIT canvas and the user's chosen appearance to a screen.
struct VizitScreen<Content: View>: View {
    private let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        ZStack {
            VizitColor.canvas.ignoresSafeArea()
            content
        }
    }
}
