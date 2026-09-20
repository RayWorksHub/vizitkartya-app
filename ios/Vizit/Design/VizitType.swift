import SwiftUI

/// VIZIT type ramp, mirroring the VIZIT/* text styles in Figma.
///
/// Every style is built on a relative text style so Dynamic Type keeps working:
/// the numbers below are the default sizes, and the system scales them with the
/// user's preferred content size. San Francisco is used deliberately — the ramp
/// is shared with Android, the typeface is native to each platform.
enum VizitFont {
    static let display = Font.system(size: 34, weight: .bold).leading(.tight)
    static let h1 = Font.system(size: 28, weight: .bold).leading(.tight)
    static let h2 = Font.system(size: 22, weight: .semibold)
    static let h3 = Font.system(size: 18, weight: .semibold)
    static let title = Font.system(size: 17, weight: .semibold)
    static let body = Font.system(size: 15, weight: .regular)
    static let bodyStrong = Font.system(size: 15, weight: .semibold)
    static let bodySmall = Font.system(size: 13, weight: .regular)
    static let label = Font.system(size: 13, weight: .semibold)
    static let button = Font.system(size: 15, weight: .semibold)
    static let caption = Font.system(size: 11, weight: .medium)
    static let overline = Font.system(size: 11, weight: .bold)
}

extension View {
    /// Overline is the only style that carries tracking, used for section
    /// headers and the wordmark.
    func vizitOverline() -> some View {
        self.font(VizitFont.overline)
            .tracking(1.2)
            .textCase(.uppercase)
    }
}
