import SwiftUI

/// VIZIT design tokens, generated from the VIZIT Design System 2026 Figma
/// library (https://www.figma.com/design/PWzvA7sVoYIBqgfw6MCpRf).
///
/// The same 34 semantic colours, type ramp, spacing scale, radius ladder and
/// elevation levels exist on Android; the two platforms must stay numerically
/// identical here so the apps look like one product.
///
/// Colours resolve per trait collection, so a single token is correct in both
/// light and dark appearance without any branching at the call site.
enum VizitColor {
    private static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light)
        })
    }

    // Background & surface
    static let canvas = dynamic(light: 0xF6F7F9, dark: 0x0A0F1C)
    static let surface = dynamic(light: 0xFFFFFF, dark: 0x131B2C)
    static let elevated = dynamic(light: 0xFFFFFF, dark: 0x1B2437)
    static let sunken = dynamic(light: 0xEDEFF3, dark: 0x060A14)
    static let inverse = dynamic(light: 0x061B46, dark: 0xE8ECF2)
    /// Icon chips sit on white in light mode, per the design brief.
    static let iconSurface = dynamic(light: 0xFFFFFF, dark: 0x1B2437)

    // Brand
    static let primary = dynamic(light: 0x0B5CE8, dark: 0x4A90FF)
    static let primaryPressed = dynamic(light: 0x0848BC, dark: 0x6BA5FF)
    static let primarySubtle = dynamic(light: 0xE8F0FE, dark: 0x14264A)
    static let onPrimary = dynamic(light: 0xFFFFFF, dark: 0x04122E)
    static let accent = dynamic(light: 0x0FBEE6, dark: 0x38D6FF)
    static let onAccent = dynamic(light: 0x04122E, dark: 0x04122E)
    /// Constant in both themes — the card ink is the brand.
    static let ink = Color(uiColor: UIColor(hex: 0x061B46))

    // Text
    static let textPrimary = dynamic(light: 0x0C1729, dark: 0xF2F5FA)
    static let textSecondary = dynamic(light: 0x4A5568, dark: 0xA3AFC2)
    static let textMuted = dynamic(light: 0x6B7688, dark: 0x8592A6)
    static let textOnBrand = Color.white
    static let textDisabled = dynamic(light: 0xA6AEBB, dark: 0x5A6577)

    // Border
    static let border = dynamic(light: 0xE2E6EC, dark: 0x263149)
    static let borderStrong = dynamic(light: 0xC9D0DA, dark: 0x35415C)
    static let borderFocus = dynamic(light: 0x0B5CE8, dark: 0x4A90FF)
    static let divider = dynamic(light: 0xEDEFF3, dark: 0x1E2739)

    // State
    static let success = dynamic(light: 0x12855A, dark: 0x34D399)
    static let successSubtle = dynamic(light: 0xE4F6EE, dark: 0x0E2C22)
    static let warning = dynamic(light: 0xA8690A, dark: 0xFBBF24)
    static let warningSubtle = dynamic(light: 0xFDF3E0, dark: 0x33260A)
    static let error = dynamic(light: 0xC62828, dark: 0xFF6B6B)
    static let errorSubtle = dynamic(light: 0xFDEAEA, dark: 0x3A1516)
    static let info = dynamic(light: 0x0B5CE8, dark: 0x4A90FF)
    static let infoSubtle = dynamic(light: 0xE8F0FE, dark: 0x14264A)

    // Control
    static let controlTrack = dynamic(light: 0xDDE2E9, dark: 0x2A3548)
    static let controlDisabled = dynamic(light: 0xEDEFF3, dark: 0x1A2334)
    static let skeletonBase = dynamic(light: 0xE8EBF0, dark: 0x1A2334)
}

/// 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 — no other values are permitted.
enum VizitSpace {
    static let xxs: CGFloat = 4
    static let xs: CGFloat = 8
    static let sm: CGFloat = 12
    static let md: CGFloat = 16
    static let lg: CGFloat = 20
    static let xl: CGFloat = 24
    static let xxl: CGFloat = 32
    static let xxxl: CGFloat = 40
    static let huge: CGFloat = 48
}

/// Deliberate radius ladder — not "everything is 24pt".
enum VizitRadius {
    static let xs: CGFloat = 6
    static let sm: CGFloat = 8
    static let md: CGFloat = 12
    static let lg: CGFloat = 16
    static let xl: CGFloat = 20
    static let xxl: CGFloat = 28
    static let full: CGFloat = 999
}

/// Three real levels plus the card, matching the Figma effect styles.
enum VizitElevation {
    struct Shadow {
        let color: Color
        let radius: CGFloat
        let y: CGFloat
    }

    static let raised = Shadow(color: VizitColor.ink.opacity(0.06), radius: 3, y: 1)
    static let floating = Shadow(color: VizitColor.ink.opacity(0.10), radius: 20, y: 8)
    static let card = Shadow(color: VizitColor.ink.opacity(0.18), radius: 28, y: 16)
}

/// Minimum interactive target — 44pt is the iOS HIG floor.
enum VizitMetrics {
    static let minTouchTarget: CGFloat = 44
    static let controlHeight: CGFloat = 52
    static let fieldHeight: CGFloat = 52
}

extension View {
    func vizitShadow(_ shadow: VizitElevation.Shadow) -> some View {
        self.shadow(color: shadow.color, radius: shadow.radius, y: shadow.y)
    }
}

extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}
