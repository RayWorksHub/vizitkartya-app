import SwiftUI

/// How much brand presence a screen gets.
///
/// `full` is the brand moment on primary destinations: the mark, the VIZIT
/// name and the slogan, each at a size that is actually readable. `compact`
/// keeps all three on secondary and inner screens — smaller, but never reduced
/// to a generic app bar where the brand disappears.
enum VizitBrandHeaderStyle {
    case full
    case compact

    var markHeight: CGFloat { self == .full ? 34 : 22 }
    var nameSize: CGFloat { self == .full ? 26 : 16 }
    var nameTracking: CGFloat { self == .full ? 5 : 3 }
    var sloganFont: Font { self == .full ? VizitFont.body : VizitFont.caption }
}

/// The VIZIT mark on its mandated white plate.
///
/// The artwork is a fixed brand asset: white background, blue "V". It is never
/// tinted, inverted or placed straight onto a themed surface, so the plate is
/// part of the mark rather than a decoration around it. In dark mode a hairline
/// keeps the plate from floating on the canvas.
struct VizitBrandMark: View {
    var height: CGFloat = 34

    private static let image: UIImage? = {
        guard let url = Bundle.main.url(forResource: "VizitLogoMark", withExtension: "png") else { return nil }
        return UIImage(contentsOfFile: url.path)
    }()

    var body: some View {
        Group {
            if let image = Self.image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .frame(height: height)
            } else {
                // The mark is the brand; if the asset is missing, the wordmark
                // still has to read as VIZIT rather than as an empty box.
                Text("V")
                    .font(.system(size: height * 0.9, weight: .black))
                    .foregroundStyle(VizitColor.primary)
                    .frame(height: height)
            }
        }
        .padding(.horizontal, height * 0.34)
        .padding(.vertical, height * 0.26)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous)
                .stroke(VizitColor.border, lineWidth: 1)
        }
        .accessibilityHidden(true)
    }
}

/// The one brand header every screen uses. Screens never hand-roll their own.
struct VizitBrandHeader<Trailing: View>: View {
    var style: VizitBrandHeaderStyle = .full
    var onBack: (() -> Void)?
    @ViewBuilder var trailing: () -> Trailing

    private let slogan = "Egy érintés. Egy kapcsolat."

    var body: some View {
        switch style {
        case .full: fullHeader
        case .compact: compactHeader
        }
    }

    private var wordmark: some View {
        Text("VIZIT")
            .font(.system(size: style.nameSize, weight: .bold))
            .tracking(style.nameTracking)
            .foregroundStyle(VizitColor.textPrimary)
    }

    private var sloganText: some View {
        Text(slogan)
            .font(style.sloganFont)
            .foregroundStyle(VizitColor.textSecondary)
            .lineLimit(2)
            .minimumScaleFactor(0.85)
            .fixedSize(horizontal: false, vertical: true)
    }

    private var fullHeader: some View {
        HStack(alignment: .top, spacing: VizitSpace.md) {
            VStack(alignment: .leading, spacing: VizitSpace.sm) {
                VizitBrandMark(height: style.markHeight)
                VStack(alignment: .leading, spacing: 2) {
                    wordmark
                    sloganText
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("VIZIT – \(slogan)")
            .accessibilityAddTraits(.isHeader)

            Spacer(minLength: VizitSpace.sm)

            trailing()
        }
    }

    private var compactHeader: some View {
        HStack(spacing: VizitSpace.sm) {
            if let onBack {
                VizitIconButton(systemImage: "chevron.left", accessibilityTitle: "Vissza", action: onBack)
                    .padding(.leading, -VizitSpace.sm)
            }

            VizitBrandMark(height: style.markHeight)

            VStack(alignment: .leading, spacing: 0) {
                wordmark
                sloganText
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("VIZIT – \(slogan)")
            .accessibilityAddTraits(.isHeader)

            Spacer(minLength: VizitSpace.xs)

            trailing()
        }
        .frame(minHeight: VizitMetrics.minTouchTarget)
    }
}

extension VizitBrandHeader where Trailing == EmptyView {
    init(style: VizitBrandHeaderStyle = .full, onBack: (() -> Void)? = nil) {
        self.init(style: style, onBack: onBack) { EmptyView() }
    }
}

/// The signed-in person, shown as its own row under the brand rather than
/// competing with it. The real photo is used whenever there is one.
struct VizitUserBadge: View {
    let profile: ContactProfile
    var greeting = "Üdv újra,"
    /// Applied to the name itself, so UI tests can read it as a static text.
    /// Children are deliberately left un-combined for that reason.
    var nameIdentifier: String?
    var action: (() -> Void)?

    private var name: String {
        profile.displayName.isEmpty ? "Állítsd be a névjegyed" : profile.displayName
    }

    var body: some View {
        Button {
            action?()
        } label: {
            HStack(spacing: VizitSpace.sm) {
                VizitAvatar(profile: profile, size: 48)

                VStack(alignment: .leading, spacing: 1) {
                    Text(greeting)
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(VizitColor.textMuted)
                    Text(name)
                        .font(VizitFont.h3)
                        .foregroundStyle(VizitColor.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                        .vizitIdentifier(nameIdentifier)
                }

                Spacer(minLength: 0)

                if action != nil {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(VizitColor.textMuted)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(action == nil)
        .accessibilityLabel("\(greeting) \(name)")
    }
}
