import SwiftUI
import CoreImage.CIFilterBuiltins

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

    private var socialLabels: [String] {
        guard presentation.showsSocial else { return [] }
        return visible.socialProfiles.filter { !$0.url.isEmpty }.map { $0.platform.label }
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

            Group {
                if presentation.layout == .portrait {
                    portraitContent
                } else {
                    landscapeContent
                }
            }
            .padding(.leading, 24)
            .padding(.trailing, 20)
            .padding(.top, 22)
            .padding(.bottom, 20)
        }
        .aspectRatio(presentation.layout == .portrait ? 343.0 / 216.0 : 343.0 / 180.0, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous))
        .vizitShadow(VizitElevation.card)
    }

    /// Identity on top, reachable details at the bottom: the layout the card
    /// has always used, now one of two the owner can pick.
    private var portraitContent: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 14) {
                if presentation.showsPhoto { avatar }
                identity
                Spacer(minLength: 0)
                if presentation.showsQR { cardQR }
            }

            Spacer(minLength: VizitSpace.md)

            HStack(alignment: .bottom) {
                details
                Spacer(minLength: VizitSpace.sm)
                Text("VIZIT").vizitOverline().foregroundStyle(accent)
            }
        }
    }

    /// Identity on the leading edge, details beside it: reads like a printed
    /// card held landscape, and keeps the QR square in the corner.
    private var landscapeContent: some View {
        HStack(alignment: .top, spacing: VizitSpace.md) {
            VStack(alignment: .leading, spacing: VizitSpace.sm) {
                HStack(spacing: 12) {
                    if presentation.showsPhoto { avatar }
                    identity
                }
                Spacer(minLength: 0)
                details
            }
            Spacer(minLength: 0)
            VStack(alignment: .trailing, spacing: VizitSpace.xs) {
                if presentation.showsQR { cardQR }
                Spacer(minLength: 0)
                Text("VIZIT").vizitOverline().foregroundStyle(accent)
            }
        }
    }

    private var identity: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(visible.displayName.isEmpty ? "Állítsd össze a névjegyed" : visible.displayName)
                .font(VizitFont.h3)
                .foregroundStyle(.white)
                .lineLimit(2)
                .minimumScaleFactor(0.8)
                .vizitIdentifier(nameIdentifier)
            if !subtitle.isEmpty {
                Text(subtitle)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(.white.opacity(0.68))
                    .lineLimit(1)
            }
        }
    }

    private var details: some View {
        VStack(alignment: .leading, spacing: 5) {
            if !visible.phone.isEmpty {
                Text(visible.phone)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(.white.opacity(0.82))
            }
            if !visible.email.isEmpty {
                Text(visible.email)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(.white.opacity(0.82))
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
            if !socialLabels.isEmpty {
                Text(socialLabels.joined(separator: " · "))
                    .font(VizitFont.caption)
                    .foregroundStyle(.white.opacity(0.62))
                    .lineLimit(1)
            }
        }
    }

    /// A miniature of the contact QR, drawn only when the payload is valid —
    /// a placeholder square would promise a scan that cannot happen.
    @ViewBuilder private var cardQR: some View {
        if let payload = try? VCard.qrPayload(visible),
           let image = CardQRThumbnail.make(payload) {
            Image(uiImage: image)
                .interpolation(.none)
                .resizable()
                .frame(width: 52, height: 52)
                .padding(4)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: VizitRadius.xs, style: .continuous))
                .accessibilityHidden(true)
        }
    }

    private var avatar: some View {
        ZStack {
            Circle().fill(Color.white.opacity(0.12))
            Circle().stroke(Color.white.opacity(0.22), lineWidth: 1)
            if let data = Data(base64Encoded: visible.photoBase64), let image = UIImage(data: data) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .clipShape(Circle())
            } else {
                Text(visible.initials.isEmpty ? "V" : visible.initials)
                    .font(VizitFont.title)
                    .foregroundStyle(.white.opacity(0.92))
            }
        }
        .frame(width: 56, height: 56)
    }
}

/// A small, cached, logo-free QR for the card face. The full-size branded code
/// with its quiet zone lives on the Megosztás screen; this one only has to
/// survive a scan from a card held in the hand.
private enum CardQRThumbnail {
    private static let context = CIContext(options: [.useSoftwareRenderer: true])
    private static let cache = NSCache<NSString, UIImage>()

    static func make(_ payload: String) -> UIImage? {
        let key = payload as NSString
        if let cached = cache.object(forKey: key) { return cached }
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(payload.utf8)
        filter.correctionLevel = "M"
        guard let code = filter.outputImage else { return nil }
        let scaled = code.transformed(by: CGAffineTransform(scaleX: 6, y: 6))
        guard let cg = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        let image = UIImage(cgImage: cg)
        cache.setObject(image, forKey: key)
        return image
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
