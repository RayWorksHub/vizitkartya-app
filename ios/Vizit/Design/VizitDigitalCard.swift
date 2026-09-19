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

    private var subtitle: String {
        [profile.jobTitle, profile.company].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    private var accessibilityText: String {
        var parts = [profile.displayName.isEmpty ? "Névjegy" : profile.displayName]
        if !subtitle.isEmpty { parts.append(subtitle) }
        if !profile.phone.isEmpty { parts.append("telefon \(profile.phone)") }
        if !profile.email.isEmpty { parts.append("e-mail \(profile.email)") }
        return parts.joined(separator: ", ")
    }

    var body: some View {
        ZStack(alignment: .leading) {
            LinearGradient(
                stops: [
                    .init(color: Color(uiColor: UIColor(hex: 0x0C2C63)), location: 0),
                    .init(color: Color(uiColor: UIColor(hex: 0x071F4C)), location: 0.55),
                    .init(color: Color(uiColor: UIColor(hex: 0x05163A)), location: 1)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            Rectangle()
                .fill(Color(uiColor: UIColor(hex: 0x0FBEE6)))
                .frame(width: 4)
                .frame(maxHeight: .infinity)

            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 14) {
                    avatar
                    VStack(alignment: .leading, spacing: 3) {
                        Text(profile.displayName.isEmpty ? "Állítsd össze a névjegyed" : profile.displayName)
                            .font(VizitFont.h3)
                            .foregroundStyle(.white)
                            .lineLimit(2)
                            .minimumScaleFactor(0.8)
                        if !subtitle.isEmpty {
                            Text(subtitle)
                                .font(VizitFont.bodySmall)
                                .foregroundStyle(.white.opacity(0.68))
                                .lineLimit(1)
                        }
                    }
                    Spacer(minLength: 0)
                }

                Spacer(minLength: VizitSpace.md)

                HStack(alignment: .bottom) {
                    VStack(alignment: .leading, spacing: 5) {
                        if !profile.phone.isEmpty {
                            Text(profile.phone)
                                .font(VizitFont.bodySmall)
                                .foregroundStyle(.white.opacity(0.82))
                        }
                        if !profile.email.isEmpty {
                            Text(profile.email)
                                .font(VizitFont.bodySmall)
                                .foregroundStyle(.white.opacity(0.82))
                                .lineLimit(1)
                                .truncationMode(.middle)
                        }
                    }
                    Spacer(minLength: VizitSpace.sm)
                    Text("VIZIT")
                        .vizitOverline()
                        .foregroundStyle(Color(uiColor: UIColor(hex: 0x0FBEE6)))
                }
            }
            .padding(.leading, 24)
            .padding(.trailing, 20)
            .padding(.top, 22)
            .padding(.bottom, 20)
        }
        .aspectRatio(343.0 / 216.0, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous))
        .vizitShadow(VizitElevation.card)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityText)
    }

    private var avatar: some View {
        ZStack {
            Circle().fill(Color.white.opacity(0.12))
            Circle().stroke(Color.white.opacity(0.22), lineWidth: 1)
            if let data = Data(base64Encoded: profile.photoBase64), let image = UIImage(data: data) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .clipShape(Circle())
            } else {
                Text(profile.initials.isEmpty ? "V" : profile.initials)
                    .font(VizitFont.title)
                    .foregroundStyle(.white.opacity(0.92))
            }
        }
        .frame(width: 56, height: 56)
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
