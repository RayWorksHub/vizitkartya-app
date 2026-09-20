import SwiftUI

/// VIZIT type ramp, mirroring the VIZIT/* text styles in the supplied design.
/// Inter is bundled with the app, while `relativeTo` keeps every size tied to
/// Dynamic Type instead of freezing the design at one accessibility setting.
enum VizitFont {
    private static let family = "Inter-Regular"

    static let display = Font.custom(family, size: 34, relativeTo: .largeTitle).weight(.bold).leading(.tight)
    static let h1 = Font.custom(family, size: 28, relativeTo: .title).weight(.bold).leading(.tight)
    static let h2 = Font.custom(family, size: 22, relativeTo: .title2).weight(.semibold)
    static let h3 = Font.custom(family, size: 18, relativeTo: .title3).weight(.semibold)
    static let title = Font.custom(family, size: 17, relativeTo: .headline).weight(.semibold)
    static let body = Font.custom(family, size: 15, relativeTo: .body)
    static let bodyStrong = Font.custom(family, size: 15, relativeTo: .body).weight(.semibold)
    static let bodySmall = Font.custom(family, size: 13, relativeTo: .subheadline)
    static let label = Font.custom(family, size: 13, relativeTo: .subheadline).weight(.semibold)
    static let button = Font.custom(family, size: 15, relativeTo: .headline).weight(.semibold)
    static let caption = Font.custom(family, size: 11, relativeTo: .caption).weight(.medium)
    static let overline = Font.custom(family, size: 11, relativeTo: .caption).weight(.bold)
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
