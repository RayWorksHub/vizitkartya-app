import SwiftUI

/// The VIZIT digital business card — the product's signature visual, identical
/// in intent and metrics to the Android composable of the same name.
///
///  - one object with real presence, not a white rectangle with text on it;
///  - navy ink with a low-contrast directional gradient that reads as material;
///  - a single 4pt cyan edge accent as the brand mark;
///  - the same in light and dark, because the card *is* the brand.
struct VizitDigitalCard: View {
    let profile: ContactProfile
    /// Applied to the name itself, so UI tests can read it as a static text.
    var nameIdentifier: String?
    /// Material, layout and which elements the owner chose in Kártya megjelenése.
    var presentation = CardPresentation()

    private var visible: ContactProfile { profile.visible(through: presentation) }

    private var subtitle: String {
        [visible.jobTitle, visible.company].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    private var accent: Color { Color(uiColor: UIColor(hex: presentation.colorway.accent)) }
    private var primaryText: Color { presentation.colorway.isLight ? VizitColor.ink : .white }
    private var secondaryText: Color {
        presentation.colorway.isLight ? VizitColor.textSecondary : .white.opacity(0.72)
    }

    private var cardAspectRatio: CGFloat {
        switch presentation.layout {
        case .portrait: return 343.0 / 365.0
        case .minimal, .classic: return 343.0 / 216.0
        }
    }

    private var accessibilityText: String {
        var parts = [visible.displayName.isEmpty ? "Névjegy" : visible.displayName]
        if !subtitle.isEmpty { parts.append(subtitle) }
        if !visible.phone.isEmpty { parts.append("telefon \(visible.phone)") }
        if !visible.email.isEmpty { parts.append("e-mail \(visible.email)") }
        return parts.joined(separator: ", ")
    }

    var body: some View {
        // With a name identifier the card is the screen's identity surface, so
        // its name has to stay an addressable element. Without one it reads as
        // a single object, which is the better VoiceOver experience.
        if nameIdentifier == nil {
            surface
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(accessibilityText)
        } else {
            surface.accessibilityElement(children: .contain)
        }
    }

    private var surface: some View {
        ZStack(alignment: .leading) {
            LinearGradient(
                stops: [
                    .init(color: Color(uiColor: UIColor(hex: presentation.colorway.gradient.start)), location: 0),
                    .init(color: Color(uiColor: UIColor(hex: presentation.colorway.gradient.mid)), location: 0.55),
                    .init(color: Color(uiColor: UIColor(hex: presentation.colorway.gradient.end)), location: 1)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            Rectangle()
                .fill(accent)
                .frame(width: 4)
                .frame(maxHeight: .infinity)

            if presentation.colorway == .brand {
                Circle()
                    .fill(Color.white.opacity(0.07))
                    .frame(width: 210, height: 210)
                    .offset(x: 225, y: -105)
                    .accessibilityHidden(true)
            }

            Group {
                switch presentation.layout {
                case .portrait:
                    portraitContent
                case .minimal:
                    minimalContent
                case .classic:
                    classicContent
                }
            }
            .padding(.leading, 24)
            .padding(.trailing, 20)
            .padding(.top, 22)
            .padding(.bottom, 20)
        }
        .aspectRatio(cardAspectRatio, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous))
        .overlay {
            if presentation.colorway.isLight {
                RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous)
                    .stroke(VizitColor.border, lineWidth: 1)
            }
        }
        .vizitShadow(VizitElevation.card)
    }

    /// Portrait is the signature VIZIT composition from the approved screens:
    /// centered identity, calm divider, then the shared contact details.
    private var portraitContent: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                avatar(size: 82)
            }
            .frame(maxWidth: .infinity)

            Spacer(minLength: VizitSpace.md)
            portraitIdentity
            Spacer(minLength: VizitSpace.md)

            Rectangle()
                .fill(primaryText.opacity(0.16))
                .frame(height: 1)
                .padding(.horizontal, VizitSpace.md)

            Spacer(minLength: VizitSpace.md)
            portraitDetails
            Spacer(minLength: VizitSpace.md)

            HStack {
                Spacer(minLength: 0)
                Text("VIZIT").vizitOverline().foregroundStyle(accent)
            }
        }
    }

    /// Reduced card face for a calm, contemporary hand-off.
    private var minimalContent: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 12) {
                avatar()
                identity
                Spacer(minLength: 0)
            }
            Spacer(minLength: VizitSpace.md)
            HStack(alignment: .bottom) {
                compactDetails
                Spacer(minLength: VizitSpace.sm)
                Text("VIZIT").vizitOverline().foregroundStyle(accent)
            }
        }
    }

    /// Printed-card composition: identity on the leading side and contact
    /// details in a distinct trailing column.
    private var classicContent: some View {
        HStack(alignment: .top, spacing: VizitSpace.md) {
            VStack(alignment: .leading, spacing: VizitSpace.sm) {
                HStack(spacing: 12) {
                    avatar()
                    identity
                }
                Spacer(minLength: 0)
                details
            }
            Spacer(minLength: 0)
            VStack(alignment: .trailing, spacing: VizitSpace.xs) {
                Spacer(minLength: 0)
                Text("VIZIT").vizitOverline().foregroundStyle(accent)
            }
        }
    }

    private var identity: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(visible.displayName.isEmpty ? "Állítsd össze a névjegyed" : visible.displayName)
                .font(VizitFont.h3)
                .foregroundStyle(primaryText)
                .lineLimit(2)
                .minimumScaleFactor(0.8)
                .vizitIdentifier(nameIdentifier)
            if !subtitle.isEmpty {
                Text(subtitle)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(secondaryText)
                    .lineLimit(1)
            }
        }
    }

    private var portraitIdentity: some View {
        VStack(spacing: 5) {
            Text(visible.displayName.isEmpty ? "Állítsd össze a névjegyed" : visible.displayName)
                .font(VizitFont.h3)
                .foregroundStyle(primaryText)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .minimumScaleFactor(0.8)
                .vizitIdentifier(nameIdentifier)
            if !visible.jobTitle.isEmpty {
                Text(visible.jobTitle)
                    .font(VizitFont.caption)
                    .foregroundStyle(accent)
                    .lineLimit(1)
            }
            if !visible.company.isEmpty {
                Text(visible.company)
                    .font(VizitFont.caption)
                    .foregroundStyle(secondaryText)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var details: some View {
        VStack(alignment: .leading, spacing: 5) {
            if !visible.phone.isEmpty {
                Text(visible.phone)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(primaryText.opacity(0.84))
            }
            if !visible.email.isEmpty {
                Text(visible.email)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(primaryText.opacity(0.84))
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
            if !visible.website.isEmpty {
                Text(visible.website)
                    .font(VizitFont.caption)
                    .foregroundStyle(secondaryText)
                    .lineLimit(1)
            }
            if !visible.address.isEmpty {
                Text(visible.address)
                    .font(VizitFont.caption)
                    .foregroundStyle(secondaryText)
                    .lineLimit(1)
            }
        }
    }

    private var compactDetails: some View {
        VStack(alignment: .leading, spacing: 4) {
            if !visible.phone.isEmpty {
                Text(visible.phone)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(primaryText.opacity(0.84))
            }
            if !visible.email.isEmpty {
                Text(visible.email)
                    .font(VizitFont.caption)
                    .foregroundStyle(secondaryText)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
        }
    }

    private var portraitDetails: some View {
        VStack(spacing: 5) {
            if !visible.phone.isEmpty {
                Text(visible.phone)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(primaryText.opacity(0.84))
            }
            if !visible.email.isEmpty {
                Text(visible.email)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(primaryText.opacity(0.84))
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
            if !visible.website.isEmpty {
                Text(visible.website)
                    .font(VizitFont.caption)
                    .foregroundStyle(secondaryText)
                    .lineLimit(1)
            }
            if !visible.address.isEmpty {
                Text(visible.address)
                    .font(VizitFont.caption)
                    .foregroundStyle(secondaryText)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func avatar(size: CGFloat = 56) -> some View {
        ZStack {
            Circle().fill(primaryText.opacity(presentation.colorway.isLight ? 0.06 : 0.12))
            Circle().stroke(primaryText.opacity(presentation.colorway.isLight ? 0.18 : 0.22), lineWidth: 1)
            if let data = Data(base64Encoded: visible.photoBase64), let image = UIImage(data: data) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .clipShape(Circle())
            } else {
                Text(visible.initials.isEmpty ? "V" : visible.initials)
                    .font(VizitFont.title)
                    .foregroundStyle(primaryText.opacity(0.92))
            }
        }
        .frame(width: size, height: size)
    }
}

/// Profile photo outside the card, on a themed surface rather than the card's
/// white-on-ink treatment. Falls back to the initials monogram.
struct VizitAvatar: View {
    let profile: ContactProfile
    var size: CGFloat = 72

    var body: some View {
        ZStack {
            Circle().fill(VizitColor.primarySubtle)
            Circle().stroke(VizitColor.border, lineWidth: 1)
            if let data = Data(base64Encoded: profile.photoBase64), let image = UIImage(data: data) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .clipShape(Circle())
            } else {
                Text(profile.initials.isEmpty ? "V" : profile.initials)
                    .font(.system(size: size * 0.34, weight: .semibold))
                    .foregroundStyle(VizitColor.primary)
            }
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

/// The official VIZIT artwork always sits on a white plate, in both themes —
/// the logo is a fixed brand asset and must not be tinted or inverted.
struct VizitBrandLockup: View {
    var maxHeight: CGFloat = 132

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
                    .frame(maxHeight: maxHeight)
                    .padding(VizitSpace.lg)
            } else {
                VStack(spacing: 4) {
                    Text("VIZIT")
                        .font(.system(size: 32, weight: .black))
                        .tracking(7)
                        .foregroundStyle(VizitColor.ink)
                    Text("EGY ÉRINTÉS. EGY KAPCSOLAT.")
                        .vizitOverline()
                        .foregroundStyle(Color(uiColor: UIColor(hex: 0x0B5CE8)))
                }
                .padding(VizitSpace.lg)
            }
        }
        .frame(maxWidth: .infinity)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous)
                .stroke(VizitColor.border, lineWidth: 1)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("VIZIT – Egy érintés. Egy kapcsolat.")
    }
}
