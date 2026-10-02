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
    static let canvas = dynamic(light: 0xF3F5F9, dark: 0x0F1115)
    static let surface = dynamic(light: 0xFFFFFF, dark: 0x1B1E26)
    static let elevated = dynamic(light: 0xFFFFFF, dark: 0x20242C)
    static let sunken = dynamic(light: 0xF3F5F9, dark: 0x111318)
    static let inverse = dynamic(light: 0x0E1733, dark: 0xE4E7EF)
    /// Icon chips sit on white in light mode, per the design brief.
    static let iconSurface = dynamic(light: 0xFFFFFF, dark: 0x20242C)

    // Brand
    static let primary = dynamic(light: 0x2A5BD7, dark: 0x8FAEFF)
    static let primaryFill = dynamic(light: 0x2A5BD7, dark: 0x3F6FE8)
    static let primaryPressed = dynamic(light: 0x204AB6, dark: 0xA9BFFF)
    static let primarySubtle = dynamic(light: 0xE8EEFC, dark: 0x243150)
    static let onPrimary = Color.white
    static let accent = dynamic(light: 0x0FBEE6, dark: 0x38D6FF)
    static let onAccent = dynamic(light: 0xFFFFFF, dark: 0x111318)
    /// Constant in both themes — the card ink is the brand.
    static let ink = Color(uiColor: UIColor(hex: 0x0E1733))

    // Text
    static let textPrimary = dynamic(light: 0x0E1733, dark: 0xE4E7EF)
    static let textSecondary = dynamic(light: 0x677087, dark: 0x9AA2B3)
    static let textMuted = dynamic(light: 0x677087, dark: 0x9AA2B3)
    static let textOnBrand = Color.white
    static let textDisabled = dynamic(light: 0x8A90A0, dark: 0x6E7587)

    // Border
    static let border = dynamic(light: 0xE3E7EF, dark: 0x2C313C)
    static let borderStrong = dynamic(light: 0xC7CCD8, dark: 0x4A5060)
    static let outline = dynamic(light: 0x79808F, dark: 0x8C92A0)
    static let borderFocus = dynamic(light: 0x2A5BD7, dark: 0x8FAEFF)
    static let divider = dynamic(light: 0xE3E7EF, dark: 0x2C313C)

    // State
    static let success = dynamic(light: 0x1F9D57, dark: 0x5CCB8C)
    static let successSubtle = dynamic(light: 0xE4F6EE, dark: 0x1D3A2B)
    static let warning = dynamic(light: 0xB86F0E, dark: 0xF0B35C)
    static let warningSubtle = dynamic(light: 0xFBF0DF, dark: 0x3D2E17)
    static let error = dynamic(light: 0xD13B3B, dark: 0xFF8A80)
    static let errorFill = dynamic(light: 0xD13B3B, dark: 0xC8453F)
    static let errorSubtle = dynamic(light: 0xFDECEC, dark: 0x45201F)
    static let info = dynamic(light: 0x2A5BD7, dark: 0x8FAEFF)
    static let infoSubtle = dynamic(light: 0xE8EEFC, dark: 0x243150)

    // Control
    static let controlTrack = dynamic(light: 0xE3E5EA, dark: 0x2E333D)
    static let controlDisabled = dynamic(light: 0xE3E5EA, dark: 0x2E333D)
    static let skeletonBase = dynamic(light: 0xEEF1F6, dark: 0x262B35)
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
    static let floating = Shadow(color: VizitColor.ink.opacity(0.08), radius: 12, y: 10)
    static let card = Shadow(color: VizitColor.ink.opacity(0.12), radius: 18, y: 12)
}

/// Minimum interactive target — 44pt is the iOS HIG floor.
enum VizitMetrics {
    static let minTouchTarget: CGFloat = 44
    static let controlHeight: CGFloat = 54
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
