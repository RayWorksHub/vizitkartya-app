import SwiftUI
import Contacts
import CoreImage.CIFilterBuiltins
import WebKit

// MARK: - Home

/// Home answers four questions immediately: who is signed in, what their card
/// looks like, how to hand it over, and whether anything needs attention.
/// One primary action, three shortcuts, then status.
struct HomeScreen: View {
    @EnvironmentObject private var store: AppStore
    @Binding var selectedTab: RootTab
    @State private var editing = false
    @State private var showScanner = false
    @State private var showKnowledgeHub = false

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.lg) {
                        VizitBrandHeader(style: .full)
                            .padding(.top, VizitSpace.xs)
                            .padding(.bottom, VizitSpace.xs)

                        VizitUserBadge(
                            profile: store.profile,
                            nameIdentifier: store.hasProfile ? "card.name" : nil
                        ) { selectedTab = .card }

                        VizitDigitalCard(profile: store.profile)
                            .onTapGesture { selectedTab = .card }

                        VizitButton(
                            title: "Névjegy megosztása",
                            systemImage: "square.and.arrow.up",
                            kind: .primary
                        ) { selectedTab = .share }

                        HStack(spacing: VizitSpace.sm) {
                            QuickTile(systemImage: "qrcode", title: "QR-kód", subtitle: "Mutatás") {
                                selectedTab = .share
                            }
                            QuickTile(systemImage: "qrcode.viewfinder", title: "Beolvasás", subtitle: "Új kapcsolat") {
                                showScanner = true
                            }
                            QuickTile(systemImage: "pencil", title: "Szerkesztés", subtitle: "Adataim") {
                                editing = true
                            }
                            .accessibilityIdentifier("card.edit")
                        }

                        if let issue = store.storageError {
                            VizitBanner(text: issue, tone: .error)
                        } else {
                            VizitStatusPill(text: store.syncStatus.label, tone: store.syncStatus.tone)
                        }

                        VizitGroup {
                            VizitRow(
                                label: "Vállalkozói Portál",
                                systemImage: "book.closed",
                                supporting: "VOSZ, edukáció, digitális segítség és eszköztár"
                            ) { showKnowledgeHub = true }
                        }
                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.bottom, VizitSpace.xxl)
                    .frame(maxWidth: 620)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $editing) { ProfileEditor(draft: store.profile) }
            .sheet(isPresented: $showKnowledgeHub) { BusinessHubScreen() }
            .fullScreenCover(isPresented: $showScanner) { ScanFlow() }
        }
    }

}
private struct QuickTile: View {
    let systemImage: String
    let title: String
    let subtitle: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: VizitSpace.xs + 2) {
                VizitIconChip(
                    systemImage: systemImage,
                    tint: VizitColor.primary,
                    background: VizitColor.primarySubtle
                )
                VStack(alignment: .leading, spacing: 1) {
                    Text(title)
                        .font(VizitFont.label)
                        .foregroundStyle(VizitColor.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.85)
                    Text(subtitle)
                        .font(VizitFont.caption)
                        .foregroundStyle(VizitColor.textMuted)
                        .lineLimit(1)
                        .minimumScaleFactor(0.85)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, VizitSpace.sm)
            .padding(.vertical, VizitSpace.md)
            .background(VizitColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous)
                    .stroke(VizitColor.border, lineWidth: 1)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Card

/// The card tab: the card itself, what is on it, and the two things you can do
/// with it. Editing happens in a sheet so this stays a clean preview.
struct CardScreen: View {
    @EnvironmentObject private var store: AppStore
    @Binding var selectedTab: RootTab
    @State private var editing = false

    private var hasDetails: Bool {
        ![store.profile.phone, store.profile.email, store.profile.website, store.profile.address]
            .allSatisfy(\.isEmpty)
    }

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.lg) {
                        VizitBrandHeader(style: .compact)

                        Text("Névjegyem")
                            .font(VizitFont.h1)
                            .foregroundStyle(VizitColor.textPrimary)
                            .padding(.top, VizitSpace.md)

                        VizitDigitalCard(profile: store.profile)

                        VizitButton(
                            title: store.hasProfile ? "Névjegy szerkesztése" : "Névjegy létrehozása",
                            systemImage: store.hasProfile ? "pencil" : "person.badge.plus",
                            kind: .secondary,
                            isEnabled: store.storageError == nil
                        ) { editing = true }

                        VizitButton(title: "Megosztás", systemImage: "square.and.arrow.up") {
                            selectedTab = .share
                        }

                        if !hasDetails {
                            VizitEmptyState(
                                systemImage: "person.text.rectangle",
                                title: "Még üres a névjegyed",
                                message: "Add meg az elérhetőségeidet, hogy legyen mit átadni egy beolvasással.",
                                actionTitle: "Adatok megadása",
                                action: { editing = true }
                            )
                        } else {
                            VizitSectionHeader(title: "Elérhetőségek")
                            VizitGroup {
                                detailRows
                            }

                            if !store.profile.company.isEmpty || !store.profile.jobTitle.isEmpty {
                                VizitSectionHeader(title: "Munkahely")
                                VizitGroup {
                                    VizitRow(
                                        label: store.profile.company.isEmpty
                                            ? store.profile.jobTitle
                                            : store.profile.company,
                                        systemImage: "building.2",
                                        supporting: store.profile.company.isEmpty ? nil : emptyToNil(store.profile.jobTitle),
                                        showsChevron: false
                                    )
                                }
                            }

                            let socials = store.profile.socialProfiles
                            if !socials.isEmpty {
                                VizitSectionHeader(title: "Közösségi profilok")
                                VizitGroup {
                                    ForEach(Array(socials.enumerated()), id: \.offset) { index, item in
                                        if index > 0 { VizitDivider() }
                                        VizitRow(
                                            label: item.platform.label,
                                            systemImage: "link",
                                            supporting: item.url,
                                            showsChevron: false
                                        )
                                    }
                                }
                            }
                        }
                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.bottom, VizitSpace.xxl)
                    .frame(maxWidth: 620)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $editing) { ProfileEditor(draft: store.profile) }
        }
    }

    @ViewBuilder
    private var detailRows: some View {
        let entries: [(String, String, String)] = [
            (store.profile.phone, "phone", "Telefon"),
            (store.profile.email, "envelope", "E-mail"),
            (store.profile.website, "globe", "Weboldal"),
            (store.profile.address, "mappin.and.ellipse", "Cím")
        ].filter { !$0.0.isEmpty }

        ForEach(Array(entries.enumerated()), id: \.offset) { index, entry in
            if index > 0 { VizitDivider() }
            VizitRow(label: entry.0, systemImage: entry.1, supporting: entry.2, showsChevron: false)
        }
    }

    private func emptyToNil(_ value: String) -> String? { value.isEmpty ? nil : value }
}

// MARK: - Share

/// Every hand-off route in one place. The QR surface is an "always-light
/// island": pure white with a quiet zone and fixed dark ink in both themes,
/// because a tinted or low-contrast code is a code that does not scan.
struct ShareScreen: View {
    @EnvironmentObject private var store: AppStore
    @State private var shareFile: ShareFile?
    @State private var temporaryURL: URL?
    @State private var showContact = false
    @State private var showFullScreenQR = false
    @State private var showScanner = false
    @State private var error: String?
    @State private var modeIndex = 0
    @State private var embeddedPhotoQRPayload: String?

    private var usePublicProfile: Bool { modeIndex == 1 }

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.md) {
                        VStack(alignment: .leading, spacing: VizitSpace.xs) {
                            VizitBrandHeader(style: .compact)

                            Text("Megosztás")
                                .font(VizitFont.h1)
                                .foregroundStyle(VizitColor.textPrimary)
                            Text("Mutasd a QR-kódot, küldd el a linket, vagy oszd meg a névjegyfájlt. A fogadó félnek nem kell VIZIT.")
                                .font(VizitFont.body)
                                .foregroundStyle(VizitColor.textSecondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .padding(.top, VizitSpace.md)

                        VizitSegmentedControl(
                            options: [embeddedPhotoQRPayload != nil ? "Fényképes QR" : "Kontakt QR", "Profil QR"],
                            selection: $modeIndex
                        )

                        if currentQRImage != nil {
                            qrIsland
                            actionGrid
                        } else {
                            VizitEmptyState(
                                systemImage: "qrcode",
                                title: usePublicProfile ? "Nincs még publikus profil" : "A Kontakt QR nem állítható elő",
                                message: usePublicProfile
                                    ? "A Profil QR-hez engedélyezd a publikus profilt, adj meg profilazonosítót, és várd meg a sikeres szinkront."
                                    : "Előbb töltsd ki a névjegyed alapadatait.",
                                actionTitle: usePublicProfile ? "Kontakt QR megnyitása" : nil,
                                action: usePublicProfile ? { modeIndex = 0 } : nil
                            )
                        }

                        VizitSectionHeader(title: "iPhone-on")
                        VizitPanel {
                            VStack(alignment: .leading, spacing: VizitSpace.sm) {
                                VizitStatusPill(text: "NFC-kártyaemuláció nem elérhető", tone: .info)
                                Text("Az iOS nem enged Androidhoz hasonló NFC-kártyaemulációt. iPhone-on a Kontakt QR, az AirDrop és a HTTPS-profil a támogatott átadási módok.")
                                    .font(VizitFont.bodySmall)
                                    .foregroundStyle(VizitColor.textSecondary)
                                    .fixedSize(horizontal: false, vertical: true)
                                VizitButton(
                                    title: "Névjegy beolvasása",
                                    systemImage: "qrcode.viewfinder",
                                    kind: .secondary
                                ) { showScanner = true }
                            }
                        }
                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.bottom, VizitSpace.xxl)
                    .frame(maxWidth: 620)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationBarHidden(true)
            .task(id: store.profile) {
                embeddedPhotoQRPayload = PhotoContactQR.payload(store.profile)
            }
            .sheet(item: $shareFile, onDismiss: cleanupShareFile) { file in
                ActivitySheet(url: file.url)
            }
            .sheet(isPresented: $showContact) {
                ContactEditor(contact: ContactBridge.contact(store.profile)) { _ in showContact = false }
            }
            .fullScreenCover(isPresented: $showFullScreenQR) {
                FullScreenQRView(image: currentQRImage, title: store.profile.displayName) {
                    showFullScreenQR = false
                }
            }
            .fullScreenCover(isPresented: $showScanner) { ScanFlow() }
            .alert("A művelet nem sikerült", isPresented: Binding(
                get: { error != nil }, set: { if !$0 { error = nil } }
            )) {
                Button("Rendben", role: .cancel) { error = nil }
            } message: { Text(error ?? "") }
        }
    }

    /// Always #FFFFFF with a fixed dark caption — never themed.
    private var qrIsland: some View {
        VStack(spacing: VizitSpace.md) {
            if let image = currentQRImage {
                Image(uiImage: image)
                    .interpolation(.none)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: 276)
                    .padding(VizitSpace.md)
                    .accessibilityLabel("A névjegy QR-kódja")
                    .accessibilityIdentifier("share.qr")
            }
            Text(captionText)
                .font(VizitFont.bodySmall)
                .foregroundStyle(Color(uiColor: UIColor(hex: 0x4A5568)))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity)
        .padding(VizitSpace.xl)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous)
                .stroke(VizitColor.border, lineWidth: 1)
        }
    }

    private var captionText: String {
        if usePublicProfile {
            return "A nyilvános névjegyoldalt nyitja meg. A mentéshez nem kell VIZIT alkalmazás."
        }
        if embeddedPhotoQRPayload != nil {
            return "Beolvasás után közvetlenül megnyílik a profilképes névjegy mentése."
        }
        return "vCard kontakt QR – profilkép nélkül, hogy gyorsan beolvasható maradjon."
    }

    private var actionGrid: some View {
        VStack(spacing: VizitSpace.sm) {
            HStack(spacing: VizitSpace.sm) {
                VizitButton(title: "Teljes képernyő", systemImage: "arrow.up.left.and.arrow.down.right", kind: .secondary) {
                    showFullScreenQR = true
                }
                VizitButton(title: "Megosztás", systemImage: "square.and.arrow.up", kind: .secondary) {
                    shareVCard()
                }
            }
            HStack(spacing: VizitSpace.sm) {
                VizitButton(title: "Kontaktokba", systemImage: "person.crop.circle.badge.plus", kind: .secondary) {
                    showContact = true
                }
                if usePublicProfile, let url = publicURL {
                    VizitButton(title: "Link másolása", systemImage: "doc.on.doc", kind: .secondary) {
                        UIPasteboard.general.string = url.absoluteString
                    }
                } else {
                    Color.clear.frame(maxWidth: .infinity, maxHeight: 1)
                }
            }
        }
    }

    private var currentQRImage: UIImage? {
        guard let payload = qrPayload else { return nil }
        return QRImage.make(payload)
    }

    private var qrPayload: String? {
        if usePublicProfile { return publicURL?.absoluteString }
        if let embeddedPhotoQRPayload { return embeddedPhotoQRPayload }
        return try? VCard.qrPayload(store.profile)
    }

    private var publicURL: URL? {
        guard store.profile.isPublic, store.syncStatus == .synced,
              let base = store.configuration?.publicProfileBaseURL else { return nil }
        return PublicProfileLink.preferred(
            baseURL: base,
            slug: store.profile.publicSlug,
            customDomain: store.profile.customDomain,
            customDomainVerified: store.profile.customDomainVerified
        )
    }

    private func shareVCard() {
        do {
            let file = try ContactBridge.shareFile(store.profile)
            temporaryURL = file.url
            shareFile = file
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func cleanupShareFile() {
        if let url = temporaryURL { ContactBridge.removeShareFile(url) }
        temporaryURL = nil
    }
}

private struct FullScreenQRView: View {
    let image: UIImage?
    let title: String
    let dismiss: () -> Void

    var body: some View {
        ZStack {
            Color.white.ignoresSafeArea()
            VStack(spacing: VizitSpace.xl) {
                Spacer()
                if !title.isEmpty {
                    Text(title)
                        .font(VizitFont.h2)
                        .foregroundStyle(VizitColor.ink)
                }
                if let image {
                    Image(uiImage: image)
                        .interpolation(.none)
                        .resizable()
                        .scaledToFit()
                        .padding(VizitSpace.lg)
                }
                Text("Olvasd be a telefon kamerájával")
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(VizitColor.ink.opacity(0.66))
                Spacer()
                VizitButton(
                    title: "Bezárás",
                    containerOverride: Color(uiColor: UIColor(hex: 0x0B5CE8)),
                    contentOverride: .white,
                    action: dismiss
                )
                .padding(.horizontal, VizitSpace.xl)
                .padding(.bottom, VizitSpace.xl)
            }
        }
    }
}

enum QRImage {
    private static let context = CIContext(options: [.useSoftwareRenderer: true])
    private static let logo: UIImage? = {
        guard let url = Bundle.main.url(forResource: "VizitLogoMark", withExtension: "png") else { return nil }
        return UIImage(contentsOfFile: url.path)
    }()

    static func make(_ payload: String) -> UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(payload.utf8)
        filter.correctionLevel = "M"
        guard let code = filter.outputImage else { return nil }
        let scale: CGFloat = 10
        let scaled = code.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        let quietZone = CIImage(color: .white).cropped(to: scaled.extent.insetBy(dx: -4 * scale, dy: -4 * scale))
        let output = scaled.composited(over: quietZone)
        guard let cg = context.createCGImage(output, from: output.extent) else { return nil }
        return addingLogo(to: UIImage(cgImage: cg))
    }

    private static func addingLogo(to qr: UIImage) -> UIImage {
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = true
        return UIGraphicsImageRenderer(size: qr.size, format: format).image { _ in
            qr.draw(in: CGRect(origin: .zero, size: qr.size))

            let plateSize = min(qr.size.width, qr.size.height) * 0.14
            let plate = CGRect(
                x: (qr.size.width - plateSize) / 2,
                y: (qr.size.height - plateSize) / 2,
                width: plateSize,
                height: plateSize
            )
            UIColor.white.setFill()
            UIBezierPath(roundedRect: plate, cornerRadius: plateSize * 0.18).fill()

            if let logo {
                let content = plateSize * 0.78
                let scale = min(content / logo.size.width, content / logo.size.height)
                let logoSize = CGSize(width: logo.size.width * scale, height: logo.size.height * scale)
                logo.draw(in: CGRect(
                    x: (qr.size.width - logoSize.width) / 2,
                    y: (qr.size.height - logoSize.height) / 2,
                    width: logoSize.width,
                    height: logoSize.height
                ))
            } else {
                let text = "V" as NSString
                let font = UIFont.systemFont(ofSize: plateSize * 0.62, weight: .black)
                let attributes: [NSAttributedString.Key: Any] = [
                    .font: font,
                    .foregroundColor: UIColor(red: 0.04, green: 0.34, blue: 0.91, alpha: 1),
                ]
                let size = text.size(withAttributes: attributes)
                text.draw(at: CGPoint(
                    x: (qr.size.width - size.width) / 2,
                    y: (qr.size.height - size.height) / 2
                ), withAttributes: attributes)
            }
        }
    }
}

private enum PhotoContactQR {
    static func payload(_ profile: ContactProfile) -> String? {
        guard !profile.photoBase64.isEmpty,
              let bytes = Data(base64Encoded: profile.photoBase64),
              let source = UIImage(data: bytes) else { return nil }

        for side in [64, 56, 48, 40, 32] {
            let size = CGSize(width: side, height: side)
            let format = UIGraphicsImageRendererFormat.default()
            format.scale = 1
            format.opaque = true
            let image = UIGraphicsImageRenderer(size: size, format: format).image { context in
                UIColor.white.setFill()
                context.cgContext.fill(CGRect(origin: .zero, size: size))
                let scale = max(size.width / source.size.width, size.height / source.size.height)
                let drawSize = CGSize(width: source.size.width * scale, height: source.size.height * scale)
                source.draw(in: CGRect(
                    x: (size.width - drawSize.width) / 2,
                    y: (size.height - drawSize.height) / 2,
                    width: drawSize.width,
                    height: drawSize.height
                ))
            }
            for quality in [0.55, 0.45, 0.35, 0.25, 0.18] {
                guard let jpeg = image.jpegData(compressionQuality: quality) else { continue }
                var candidate = profile
                candidate.photoBase64 = jpeg.base64EncodedString()
                if let value = try? VCard.qrPayload(candidate, includePhoto: true),
                   value.contains("PHOTO;ENCODING=b;TYPE=JPEG:") {
                    return value
                }
            }
        }
        return nil
    }
}

// MARK: - Scan

/// Scanning is an iOS-only capability (Android hands over with NFC instead), so
/// it is presented as a task you start, not a permanent tab.
struct ScanFlow: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @State private var scanning = false
    @State private var pendingText: String?
    @State private var incoming: IncomingContact?
    @State private var link: URL?
    @State private var message: String?

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(spacing: VizitSpace.lg) {
                        ZStack {
                            RoundedRectangle(cornerRadius: VizitRadius.xxl, style: .continuous)
                                .fill(
                                    LinearGradient(
                                        colors: [
                                            Color(uiColor: UIColor(hex: 0x0C2C63)),
                                            Color(uiColor: UIColor(hex: 0x05163A))
                                        ],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                                .frame(height: 220)
                            Image(systemName: "qrcode.viewfinder")
                                .font(.system(size: 68, weight: .medium))
                                .foregroundStyle(.white)
                        }

                        VStack(spacing: VizitSpace.xs) {
                            Text("Új kapcsolat, egy beolvasással.")
                                .font(VizitFont.h2)
                                .foregroundStyle(VizitColor.textPrimary)
                                .multilineTextAlignment(.center)
                            Text("Olvass be egy névjegy-QR-kódot. Mentés előtt minden adatot ellenőrizhetsz.")
                                .font(VizitFont.body)
                                .foregroundStyle(VizitColor.textSecondary)
                                .multilineTextAlignment(.center)
                                .fixedSize(horizontal: false, vertical: true)
                        }

                        VizitButton(title: "QR-kód beolvasása", systemImage: "camera.viewfinder") {
                            scanning = true
                        }

                        VizitPanel {
                            HStack(alignment: .top, spacing: VizitSpace.sm) {
                                Image(systemName: "hand.raised.fill")
                                    .foregroundStyle(VizitColor.primary)
                                Text("A kamera csak a beolvasó megnyitásakor aktív. Webcímet az alkalmazás soha nem nyit meg automatikusan.")
                                    .font(VizitFont.bodySmall)
                                    .foregroundStyle(VizitColor.textSecondary)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }
                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.bottom, VizitSpace.xxl)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("Beolvasás")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Kész") { dismiss() }
                }
            }
            .fullScreenCover(isPresented: $scanning, onDismiss: processScan) {
                NavigationStack {
                    QRScanner(onResult: { text in pendingText = text; scanning = false },
                              onError: { text in message = text; scanning = false })
                        .ignoresSafeArea(edges: .bottom)
                        .navigationTitle("Névjegy beolvasása")
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbar {
                            ToolbarItem(placement: .cancellationAction) {
                                Button("Bezárás") { scanning = false }
                            }
                        }
                }
            }
            .sheet(item: $incoming) { item in
                ContactEditor(contact: item.contact) { saved in
                    incoming = nil
                    if saved { message = "A névjegyet elmentetted a Kontaktokba." }
                }
            }
            .alert("Webcím a QR-kódban", isPresented: Binding(
                get: { link != nil }, set: { if !$0 { link = nil } }
            )) {
                Button("Mégse", role: .cancel) { link = nil }
                Button("Megnyitás") {
                    if let url = link { openURL(url) }
                    link = nil
                }
            } message: { Text(link?.absoluteString ?? "") }
            .alert("Beolvasás", isPresented: Binding(
                get: { message != nil && !scanning && incoming == nil },
                set: { if !$0 { message = nil } }
            )) {
                Button("Rendben", role: .cancel) { message = nil }
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    Button("Rendszerbeállítások") { openURL(url); message = nil }
                }
            } message: { Text(message ?? "") }
        }
    }

    private func processScan() {
        guard let text = pendingText else { return }
        pendingText = nil
        let value = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard value.utf8.count <= 16_384 else {
            message = "A QR-kód túl sok adatot tartalmaz."
            return
        }
        if let url = SafeLink.https(value) { link = url; return }
        guard value.uppercased().hasPrefix("BEGIN:VCARD"),
              value.uppercased().hasSuffix("END:VCARD"),
              let contacts = try? CNContactVCardSerialization.contacts(with: Data(value.utf8)),
              contacts.count == 1,
              let clean = contacts[0].mutableCopy() as? CNMutableContact else {
            message = "Ez nem támogatott névjegy-QR vagy HTTPS-webcím. Az alkalmazás nem végzett műveletet."
            return
        }
        clean.urlAddresses = clean.urlAddresses.filter { SafeLink.https($0.value as String) != nil }
        incoming = IncomingContact(contact: clean)
    }
}

// MARK: - Business portal

private enum PortalDestination { case vosz, education, help, toolkit }

private struct PortalFeature: Identifiable {
    let id: String
    let title: String
    let description: String
    let eyebrow: String
    let icon: String
    let destination: PortalDestination
}

private struct BusinessResource: Identifiable {
    let id: String
    let title: String
    let description: String
    let url: String
    let icon: String
}

private struct BusinessCourse: Identifiable {
    let id: String
    let title: String
    let category: String
    let description: String
    let duration: String
    let level: String
    let icon: String
    let videoTitle: String
    let videoSource: String
    let videoURL: String
    let modules: [CourseModule]
}

private struct CourseModule: Identifiable {
    let id: String
    let title: String
    let summary: String
    let resourceTitle: String
    let resourceURL: String
}

private struct GuideLink: Identifiable {
    let id: String
    let title: String
    let description: String
    let url: String
}

private struct ToolkitGuide: Identifiable {
    let id: String
    let title: String
    let description: String
    let result: String
    let icon: String
    let steps: [String]
    let links: [GuideLink]
}

private let portalFeatures = [
    PortalFeature(id: "vosz", title: "VOSZ", description: "Hírek, videók és vállalkozói szolgáltatások egy helyen.", eyebrow: "PARTNERI FORRÁSOK", icon: "briefcase.fill", destination: .vosz),
    PortalFeature(id: "education", title: "Vállalkozói Edukáció", description: "Gyakorlati kurzusok AI-ról, Microsoft 365-ről és cégépítésről.", eyebrow: "6 KURZUS", icon: "graduationcap.fill", destination: .education),
    PortalFeature(id: "help", title: "Digitális segítség", description: "Próbáld ki, hogyan indul majd egy videós szakértői konzultáció.", eyebrow: "BEMUTATÓ MÓD", icon: "video.fill", destination: .help),
    PortalFeature(id: "toolkit", title: "Vállalkozói eszköztár", description: "Gyors digitális állapotfelmérés és személyre szabott következő lépések.", eyebrow: "INTERAKTÍV", icon: "checklist", destination: .toolkit)
]

private let businessResources = [
    BusinessResource(id: "vosz-youtube", title: "VOSZ videók", description: "Vállalkozói hírek, interjúk és gyakorlati videók.", url: "https://youtube.com/@vosz.?si=k2EmMlI8Q5ttlPZC", icon: "play.rectangle.fill"),
    BusinessResource(id: "vosz", title: "VOSZ információk", description: "Érdekképviselet, tanácsadás, programok és aktuális hírek.", url: "https://www.vosz.hu/hu", icon: "briefcase.fill"),
    BusinessResource(id: "voszport", title: "VOSZPort", description: "Digitális ügyintézési és tudásmegosztási felület.", url: "https://voszport.com/", icon: "globe.europe.africa.fill")
]

private let businessCourses = [
    BusinessCourse(
        id: "ai", title: "AI a mindennapi vállalkozásban", category: "Mesterséges intelligencia",
        description: "Használható promptok, automatizálási ötletek és felelős AI-használat.",
        duration: "Magyar videó", level: "Kezdő", icon: "sparkles",
        videoTitle: "ChatGPT képzés – Promptolási technikák", videoSource: "VOSZ",
        videoURL: "https://www.youtube.com/watch?v=uPYOrXxTxJI",
        modules: [
            CourseModule(id: "ai-value", title: "Hol teremt értéket az AI?", summary: "Gyűjts össze három ismétlődő szöveges feladatot, és válaszd ki azt, amelyiknél a legkisebb a hibakockázat.", resourceTitle: "VOSZ AI-videósorozat", resourceURL: "https://www.youtube.com/results?search_query=VOSZ+ChatGPT+k%C3%A9pz%C3%A9s"),
            CourseModule(id: "ai-prompt", title: "Jó prompt 5 lépésben", summary: "Add meg a szerepet, a célt, a bemenetet, a korlátokat és a kívánt kimeneti formátumot.", resourceTitle: "Promptolási videó megnyitása", resourceURL: "https://www.youtube.com/watch?v=uPYOrXxTxJI"),
            CourseModule(id: "ai-mail", title: "Ajánlat és e-mail gyorsítása", summary: "Készíts ellenőrzött sablont ajánlatra, utánkövetésre és ügyfélválaszra; érzékeny adatot ne másolj be.", resourceTitle: "Microsoft Copilot vállalkozásoknak", resourceURL: "https://www.microsoft.com/hu-hu/microsoft-365/business/copilot-for-microsoft-365"),
            CourseModule(id: "ai-privacy", title: "Ellenőrzés és adatvédelem", summary: "Minden AI-kimenetet ember ellenőrizzen, és legyen belső szabály arra, milyen adat kerülhet a rendszerbe.", resourceTitle: "NAIH tájékoztatók", resourceURL: "https://www.naih.hu/")
        ]
    ),
    BusinessCourse(
        id: "m365", title: "Microsoft 365 kisvállalkozásoknak", category: "Digitális munka",
        description: "Teams, Outlook, OneDrive és SharePoint egyszerű, biztonságos rendszerben.",
        duration: "Magyar videók", level: "Kezdő", icon: "cloud.fill",
        videoTitle: "Microsoft 365 bevezetés és csoportok", videoSource: "Sämling Üzleti Oktatási Központ",
        videoURL: "https://www.youtube.com/watch?v=py9fGXyBZcE",
        modules: [
            CourseModule(id: "m365-accounts", title: "Fiókok és jogosultságok", summary: "Minden munkatársnak külön fiók, szerepkör szerinti hozzáférés és bekapcsolt többtényezős védelem kell.", resourceTitle: "Microsoft 365 Vállalati verzió", resourceURL: "https://www.microsoft.com/hu-hu/microsoft-365/business"),
            CourseModule(id: "m365-files", title: "Közös fájlkezelés OneDrive-val", summary: "Alakíts ki közös mappaszerkezetet, tulajdonost és visszaállítási rendet; ne e-mailben küldözgess fájlmásolatokat.", resourceTitle: "OneDrive magyar súgó", resourceURL: "https://support.microsoft.com/hu-hu/onedrive"),
            CourseModule(id: "m365-teams", title: "Teams-együttműködés", summary: "Hozz létre ügyfél- vagy projektcsatornákat, és rögzítsd, melyik információ hol található.", resourceTitle: "Teams magyar súgó", resourceURL: "https://support.microsoft.com/hu-hu/teams"),
            CourseModule(id: "m365-automation", title: "Naptár és automatizmusok", summary: "Használj közös naptárt, foglalási oldalt és egyetlen jóváhagyott automatizmust egy ismétlődő folyamathoz.", resourceTitle: "Power Automate", resourceURL: "https://www.microsoft.com/hu-hu/power-platform/products/power-automate")
        ]
    ),
    BusinessCourse(
        id: "basics", title: "Vállalkozói alapismeretek", category: "Cégépítés",
        description: "Üzleti modell, célpiac, árazás és az első 90 nap terve.",
        duration: "Magyar videó", level: "Kezdő", icon: "storefront.fill",
        videoTitle: "Az egyéni vállalkozás és indítása", videoSource: "Magyar nyelvű oktatóvideó",
        videoURL: "https://www.youtube.com/watch?v=d2tyNSnyv1Q",
        modules: [
            CourseModule(id: "basics-model", title: "Üzleti modell egy oldalon", summary: "Írd le egy mondatban a vevőt, a problémáját, az ajánlatodat, az értékesítési csatornát és a bevételi módot.", resourceTitle: "VOSZ vállalkozói információk", resourceURL: "https://www.vosz.hu/hu"),
            CourseModule(id: "basics-launch", title: "Indítás és kötelező lépések", summary: "Ellenőrizd a tevékenységet, adózást, képesítési feltételt, kamarai bejelentést és számlázást.", resourceTitle: "NAV: egyéni vállalkozás indítása", resourceURL: "https://nav.gov.hu/Elethelyzetek-adozasa/vallalkozas/Egyeni-vallalkozas-inditasa"),
            CourseModule(id: "basics-pricing", title: "Ideális ügyfél és árazás", summary: "Határozz meg egy konkrét célcsoportot, eredményt, költségszintet és minimális vállalható árat.", resourceTitle: "MKIK Mentorprogram", resourceURL: "https://vallalkozztudatosan.mkik.hu/"),
            CourseModule(id: "basics-plan", title: "90 napos akcióterv", summary: "Bonts három havi célra, heti mérőszámokra és minden héten egy lezárandó ügyfélszerzési feladatra.", resourceTitle: "Vállalkozz digitálisan", resourceURL: "https://vallalkozzdigitalisan.mkik.hu/")
        ]
    ),
    BusinessCourse(
        id: "security", title: "Kiberbiztonság emberi nyelven", category: "Biztonság",
        description: "Fiókvédelem, mentés, adathalászat és egy egyszerű incidens-terv.",
        duration: "Magyar videó", level: "Kezdő", icon: "lock.shield.fill",
        videoTitle: "KiberPajzs – digitális biztonság", videoSource: "Pénziránytű / KiberPajzs",
        videoURL: "https://www.youtube.com/watch?v=2LqpB_03Jt0",
        modules: [
            CourseModule(id: "security-mfa", title: "Többlépcsős belépés", summary: "Kapcsold be először az e-mail-, banki, közösségi és adminisztrációs fiókoknál.", resourceTitle: "KiberPajzs biztonsági tippek", resourceURL: "https://kiberpajzs.hu/hasznos-tippeket-olvasnek"),
            CourseModule(id: "security-backup", title: "Eszközök és mentés", summary: "Automatikus frissítés, képernyőzár és legalább egy külön helyen tárolt, visszaállítással is tesztelt mentés kell.", resourceTitle: "NKI tudásbázis", resourceURL: "https://nki.gov.hu/"),
            CourseModule(id: "security-phishing", title: "Adathalászat felismerése", summary: "Sürgetésnél állj meg, külön csatornán ellenőrizd a feladót, és ne a levélből nyisd meg a belépési oldalt.", resourceTitle: "KiberPajzs videó", resourceURL: "https://www.youtube.com/watch?v=2LqpB_03Jt0"),
            CourseModule(id: "security-incident", title: "Incidens-terv", summary: "Írd le, kit kell hívni, hogyan zárod a fiókokat, hol van a mentés, és hogyan értesíted az érintetteket.", resourceTitle: "NKI incidensbejelentés", resourceURL: "https://nki.gov.hu/intezet/tartalom/incidens-bejelentes/")
        ]
    ),
    BusinessCourse(
        id: "marketing", title: "Online jelenlét és ügyfélszerzés", category: "Marketing",
        description: "Egyszerű pozicionálás, tartalomterv és mérhető kampányalapok.",
        duration: "Magyar videók", level: "Középhaladó", icon: "megaphone.fill",
        videoTitle: "Hogyan kerülhetsz fel a Google Térképre?", videoSource: "Jobbágy András",
        videoURL: "https://www.youtube.com/watch?v=-4qATDuCWgU",
        modules: [
            CourseModule(id: "marketing-position", title: "Pozicionálási mondat", summary: "Ne szolgáltatást sorolj: mondd meg, kinek, milyen eredményt és mitől más módon adsz.", resourceTitle: "VOSZ videók", resourceURL: "https://www.youtube.com/@vosz."),
            CourseModule(id: "marketing-profile", title: "Bizalmat építő Google Cégprofil", summary: "Tölts ki minden adatot, adj képeket, szolgáltatásokat és rendszeresen válaszolj az értékelésekre.", resourceTitle: "Google Cégprofil hozzáadása", resourceURL: "https://support.google.com/business/answer/2911778?hl=hu"),
            CourseModule(id: "marketing-content", title: "4 hetes tartalomterv", summary: "Hetente mutass problémát, megoldást, bizonyítékot és konkrét következő lépést.", resourceTitle: "Meta Business Suite", resourceURL: "https://business.facebook.com/"),
            CourseModule(id: "marketing-measure", title: "Mérés és javítás", summary: "Mérd a forrást, érdeklődést, ajánlatot és vásárlást; a követőszám önmagában nem üzleti eredmény.", resourceTitle: "Google Analytics", resourceURL: "https://analytics.google.com/")
        ]
    ),
    BusinessCourse(
        id: "finance", title: "Pénzügyi tudatosság alapjai", category: "Pénzügy",
        description: "Cash-flow, költségek és a könyvelővel való hatékony együttműködés.",
        duration: "Magyar videó", level: "Kezdő", icon: "building.columns.fill",
        videoTitle: "NAV Online Számlázó Program bemutatása", videoSource: "Nemzeti Adó- és Vámhivatal",
        videoURL: "https://www.youtube.com/watch?v=U5s2bXrgnHc",
        modules: [
            CourseModule(id: "finance-profit", title: "Bevétel, költség és nyereség", summary: "Külön kezeld a beérkezett pénzt, az áfát, a fizetendő adót, a költségeket és a tulajdonosi kivétet.", resourceTitle: "NAV vállalkozói élethelyzetek", resourceURL: "https://nav.gov.hu/Elethelyzetek-adozasa/vallalkozas"),
            CourseModule(id: "finance-cashflow", title: "13 hetes cash-flow", summary: "Hetente vezesd a várható be- és kifizetéseket, és jelöld a bizonytalan tételeket.", resourceTitle: "Pénziránytű", resourceURL: "https://penziranytu.hu/"),
            CourseModule(id: "finance-invoice", title: "Számlázás és adatszolgáltatás", summary: "Ellenőrizd a NAV-kapcsolatot, a számlaadatokat és azt, hogy ki figyeli a hibás adatszolgáltatást.", resourceTitle: "NAV Online Számla", resourceURL: "https://onlineszamla.nav.gov.hu/"),
            CourseModule(id: "finance-close", title: "Havi zárás a könyvelővel", summary: "Legyen fix határidő a bizonylatokra, kintlévőségekre, adókra és a következő három hónap pénzigényére.", resourceTitle: "NAV ONYA", resourceURL: "https://onya.nav.gov.hu/")
        ]
    )
]

private let toolkitGuides = [
    ToolkitGuide(
        id: "launch", title: "Vállalkozás indítása",
        description: "Az ötlettől a jogszerű indulásig, kihagyott kötelező lépések nélkül.",
        result: "Működő vállalkozói státusz és rendezett alapadatok.", icon: "storefront.fill",
        steps: [
            "Írd le a tevékenységet és ellenőrizd, kell-e képesítés vagy engedély.",
            "Könyvelővel válassz vállalkozási formát és adózást még a bejelentés előtt.",
            "Indítsd el az egyéni vállalkozást a NAV Vállalkozói Ügysegédjén vagy intézd a cégalapítást szakértővel.",
            "Jelentkezz be az illetékes gazdasági kamarához a NAV által jelzett határidőn belül.",
            "Állíts be számlázást, vállalkozói bankszámlát és iratmegőrzési rendet."
        ],
        links: [
            GuideLink(id: "launch-nav", title: "NAV indítási útmutató", description: "Hivatalos, naprakész lépések és feltételek.", url: "https://nav.gov.hu/Elethelyzetek-adozasa/vallalkozas/Egyeni-vallalkozas-inditasa"),
            GuideLink(id: "launch-upo", title: "NAV Vállalkozói Ügysegéd", description: "Az online bejelentés belépési pontja.", url: "https://ugyfelportal.nav.gov.hu/"),
            GuideLink(id: "launch-mkik", title: "MKIK", description: "Kamarai információk és területi kamarák.", url: "https://mkik.hu/")
        ]
    ),
    ToolkitGuide(
        id: "billing", title: "Számlázás és NAV-ügyintézés",
        description: "A számla kiállításától a bevallási feladatok követéséig.",
        result: "Ellenőrizhető számlázási folyamat és kevesebb adminisztrációs hiba.", icon: "building.columns.fill",
        steps: [
            "Regisztrálj a NAV Online Számla rendszerébe és rögzíts technikai felhasználót, ha a program kéri.",
            "Válassz NAV-kapcsolatos számlázót, majd állíts ki és ellenőrizz egy tesztszámlát.",
            "Rögzíts heti rutint a hibás adatszolgáltatások és a kintlévőségek ellenőrzésére.",
            "Egyeztess a könyvelővel dokumentumleadási határidőt és felelőst."
        ],
        links: [
            GuideLink(id: "billing-online", title: "NAV Online Számla", description: "Regisztráció, számlák és adatszolgáltatási hibák.", url: "https://onlineszamla.nav.gov.hu/"),
            GuideLink(id: "billing-onya", title: "NAV ONYA", description: "Online nyomtatványok és bejelentések.", url: "https://onya.nav.gov.hu/"),
            GuideLink(id: "billing-report", title: "e-Beszámoló", description: "Közzétett éves beszámolók hivatalos keresője.", url: "https://e-beszamolo.im.gov.hu/")
        ]
    ),
    ToolkitGuide(
        id: "office", title: "Digitális iroda kialakítása",
        description: "Céges e-mail, közös dokumentumok és világos hozzáférések.",
        result: "Egy helyen megtalálható fájlok és átadható működés.", icon: "cloud.fill",
        steps: [
            "Használj saját domaines céges e-mail-címet, ne közös jelszóval használt postafiókot.",
            "Hozz létre egységes mappaszerkezetet ügyfelekre, pénzügyre és belső működésre.",
            "Adj személyenként jogosultságot, és távozáskor azonnal vond vissza.",
            "Kapcsold be a verziókövetést, mentést és teszteld egy fájl visszaállítását."
        ],
        links: [
            GuideLink(id: "office-m365", title: "Microsoft 365 vállalkozásoknak", description: "Outlook, Teams, OneDrive és irodai alkalmazások.", url: "https://www.microsoft.com/hu-hu/microsoft-365/business"),
            GuideLink(id: "office-google", title: "Google Workspace", description: "Céges Gmail, Drive, Meet és közös munka.", url: "https://workspace.google.com/intl/hu/"),
            GuideLink(id: "office-mkik", title: "Vállalkozz digitálisan", description: "MKIK digitális megoldások és segítség.", url: "https://vallalkozzdigitalisan.mkik.hu/")
        ]
    ),
    ToolkitGuide(
        id: "security", title: "Biztonság és adatvédelem",
        description: "A leggyakoribb fiók-, adat- és csalási kockázatok kezelése.",
        result: "Védett fiókok, működő mentés és leírt incidensfolyamat.", icon: "lock.shield.fill",
        steps: [
            "Kapcsold be a többtényezős azonosítást az e-mailen, bankon, közösségi és adminfiókokon.",
            "Használj jelszókezelőt, és szüntesd meg a közös vagy újrahasznált jelszavakat.",
            "Készíts automatikus mentést külön helyre, majd próbáld visszaállítani.",
            "Írd le, ki mit tesz csalás, elveszett eszköz vagy adatszivárgás esetén.",
            "Tarts naprakész adatkezelési tájékoztatót, és csak szükséges ügyféladatot gyűjts."
        ],
        links: [
            GuideLink(id: "security-kiberpajzs", title: "KiberPajzs", description: "Magyar csalásmegelőzési és digitális biztonsági tippek.", url: "https://kiberpajzs.hu/hasznos-tippeket-olvasnek"),
            GuideLink(id: "security-nki", title: "Nemzeti Kibervédelmi Intézet", description: "Riasztások, tudásanyag és incidensbejelentés.", url: "https://nki.gov.hu/"),
            GuideLink(id: "security-naih", title: "NAIH", description: "Hivatalos adatvédelmi tájékoztatók és ügyintézés.", url: "https://www.naih.hu/")
        ]
    ),
    ToolkitGuide(
        id: "sales", title: "Online jelenlét és ügyfélszerzés",
        description: "Megtalálható profilok, egyértelmű ajánlat és mérhető érdeklődők.",
        result: "Friss online jelenlét és követhető ügyfélszerzési tölcsér.", icon: "megaphone.fill",
        steps: [
            "Fogalmazd meg egy mondatban, kinek milyen eredményt adsz.",
            "Igényeld és töltsd ki a Google Cégprofilt, ha helyi vagy személyes szolgáltatást végzel.",
            "Válassz legfeljebb két aktív közösségi csatornát, és legyen minden felületen egyértelmű kapcsolatfelvétel.",
            "Rögzítsd minden érdeklődő forrását és a következő lépését.",
            "Havonta értékeld az érdeklődő, ajánlat és vásárlás számát."
        ],
        links: [
            GuideLink(id: "sales-google", title: "Google Cégprofil létrehozása", description: "Ingyenes megjelenés a Google Keresőben és Térképen.", url: "https://support.google.com/business/answer/2911778?hl=hu"),
            GuideLink(id: "sales-meta", title: "Meta Business Suite", description: "Facebook- és Instagram-oldalak kezelése.", url: "https://business.facebook.com/"),
            GuideLink(id: "sales-analytics", title: "Google Analytics", description: "Webes forgalom és konverziók mérése.", url: "https://analytics.google.com/")
        ]
    ),
    ToolkitGuide(
        id: "growth", title: "Mentor, digitalizáció és fejlődés",
        description: "Hiteles segítség, partnerkapcsolatok és fejlesztési programok keresése.",
        result: "Kiválasztott fejlesztési cél és konkrét jelentkezési következő lépés.", icon: "rocket.fill",
        steps: [
            "Válassz egy 90 napos fejlesztési célt: értékesítés, digitalizáció, export vagy működés.",
            "Készíts egyoldalas helyzetképet a számokról, problémáról és elvárt eredményről.",
            "Keress hozzá szakmai szervezetet, mentort vagy minősített digitális szolgáltatót.",
            "Jelölj ki felelőst, határidőt és egy mérőszámot."
        ],
        links: [
            GuideLink(id: "growth-vosz", title: "VOSZ", description: "Érdekképviselet, tanácsadás, programok és vállalkozói hírek.", url: "https://www.vosz.hu/hu"),
            GuideLink(id: "growth-mentor", title: "MKIK Mentorprogram", description: "Gyakorlati mentorálás növekedéshez és külpiachoz.", url: "https://vallalkozztudatosan.mkik.hu/"),
            GuideLink(id: "growth-digital", title: "Vállalkozz digitálisan", description: "Digitális fejlesztési tudás és megoldások.", url: "https://vallalkozzdigitalisan.mkik.hu/")
        ]
    )
]

struct BusinessHubScreen: View {
    @Environment(\.dismiss) private var dismiss
    private let columns = [GridItem(.flexible(), spacing: VizitSpace.sm), GridItem(.flexible(), spacing: VizitSpace.sm)]

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.lg) {
                        VizitBrandHeader(style: .compact).padding(.top, VizitSpace.xs)
                        VStack(alignment: .leading, spacing: VizitSpace.xs) {
                            Text("Vállalkozói Portál").font(VizitFont.display).foregroundStyle(VizitColor.textPrimary)
                            Text("Tanulás, hiteles források és digitális segítség a vállalkozásod következő lépéséhez.")
                                .font(VizitFont.body).foregroundStyle(VizitColor.textSecondary)
                        }
                        LazyVGrid(columns: columns, spacing: VizitSpace.sm) {
                            ForEach(portalFeatures) { feature in
                                NavigationLink { destination(for: feature.destination) } label: { PortalFeatureCard(feature: feature) }
                                    .buttonStyle(.plain)
                            }
                        }
                        VizitPanel {
                            HStack(alignment: .top, spacing: VizitSpace.sm) {
                                VizitIconChip(systemImage: "lightbulb.fill", tint: VizitColor.primary, background: VizitColor.primarySubtle)
                                VStack(alignment: .leading, spacing: VizitSpace.xxs) {
                                    Text("Heti fejlődési tipp").font(VizitFont.label).foregroundStyle(VizitColor.textPrimary)
                                    Text("Válassz ki egyetlen ismétlődő feladatot, és dokumentáld, mielőtt automatizálod.")
                                        .font(VizitFont.bodySmall).foregroundStyle(VizitColor.textSecondary)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, VizitSpace.md).padding(.bottom, VizitSpace.xxl)
                    .frame(maxWidth: 620).frame(maxWidth: .infinity)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Kész") { dismiss() } } }
        }
    }

    @ViewBuilder private func destination(for destination: PortalDestination) -> some View {
        switch destination {
        case .vosz: VoszCenterScreen()
        case .education: EducationCatalogScreen()
        case .help: DigitalHelpScreen()
        case .toolkit: BusinessToolkitScreen()
        }
    }
}

private struct PortalFeatureCard: View {
    let feature: PortalFeature
    var body: some View {
        VStack(alignment: .leading, spacing: VizitSpace.sm) {
            VizitIconChip(systemImage: feature.icon, tint: VizitColor.primary, background: VizitColor.primarySubtle, size: 44)
            Text(feature.eyebrow).vizitOverline().foregroundStyle(VizitColor.primary)
            Text(feature.title).font(VizitFont.h3).foregroundStyle(VizitColor.textPrimary).lineLimit(2)
            Text(feature.description).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textSecondary).lineLimit(4)
            Spacer(minLength: 0)
            Image(systemName: "arrow.right").font(.system(size: 15, weight: .semibold)).foregroundStyle(VizitColor.primary).frame(maxWidth: .infinity, alignment: .trailing)
        }
        .padding(VizitSpace.md).frame(maxWidth: .infinity, minHeight: 222, alignment: .topLeading)
        .background(VizitColor.surface).clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
        .overlay { RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous).stroke(VizitColor.border, lineWidth: 1) }
    }
}

private struct VoszCenterScreen: View {
    @Environment(\.openURL) private var openURL
    var body: some View {
        PortalScroll(title: "VOSZ forrásközpont", subtitle: "Hivatalos vállalkozói tartalmak és szolgáltatások.") {
            VizitGroup {
                ForEach(Array(businessResources.enumerated()), id: \.element.id) { index, resource in
                    if index > 0 { VizitDivider() }
                    VizitRow(label: resource.title, systemImage: resource.icon, supporting: resource.description) {
                        guard let url = SafeLink.https(resource.url) else { return }
                        openURL(url)
                    }
                }
            }
            Text("A hivatkozások külső oldalakra vezetnek; azok tartalmáért az adott szolgáltató felel.")
                .font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)
        }
    }
}

private struct EducationCatalogScreen: View {
    @State private var selectedCategory = "Mind"
    private let categories = ["Mind", "AI", "Digitális munka", "Cégépítés", "Biztonság"]
    private var filtered: [BusinessCourse] {
        businessCourses.filter { selectedCategory == "Mind" || (selectedCategory == "AI" && $0.id == "ai") || $0.category == selectedCategory }
    }
    var body: some View {
        PortalScroll(title: "Vállalkozói Edukáció", subtitle: "Rövid, gyakorlatias tananyagok, saját tempóban.") {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: VizitSpace.xs) {
                    ForEach(categories, id: \.self) { category in
                        Button { selectedCategory = category } label: {
                            HStack(spacing: VizitSpace.xxs) {
                                if category == selectedCategory { Image(systemName: "checkmark.circle.fill") }
                                Text(category).font(VizitFont.label)
                            }
                            .foregroundStyle(category == selectedCategory ? VizitColor.primary : VizitColor.textSecondary)
                            .padding(.horizontal, VizitSpace.sm).frame(minHeight: 40)
                            .background(category == selectedCategory ? VizitColor.primarySubtle : VizitColor.surface)
                            .clipShape(Capsule()).overlay { Capsule().stroke(VizitColor.border, lineWidth: 1) }
                        }.buttonStyle(.plain)
                    }
                }
            }
            ForEach(filtered) { course in
                NavigationLink { CourseDetailScreen(course: course) } label: { CourseCard(course: course) }.buttonStyle(.plain)
            }
        }
    }
}

private struct CourseCard: View {
    let course: BusinessCourse
    var body: some View {
        HStack(alignment: .top, spacing: VizitSpace.md) {
            VizitIconChip(systemImage: course.icon, tint: VizitColor.primary, background: VizitColor.primarySubtle, size: 64)
            VStack(alignment: .leading, spacing: VizitSpace.xxs) {
                Text(course.category.uppercased()).vizitOverline().foregroundStyle(VizitColor.primary)
                Text(course.title).font(VizitFont.h3).foregroundStyle(VizitColor.textPrimary)
                Text(course.description).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textSecondary).lineLimit(2)
                Text("\(course.duration)  ·  \(course.level)  ·  \((course.modules.count + 1) / 2) modul  ·  \(course.modules.count) lecke").font(VizitFont.caption).foregroundStyle(VizitColor.textMuted)
            }
            Spacer(minLength: 0)
            Image(systemName: "chevron.right").font(.system(size: 13, weight: .semibold)).foregroundStyle(VizitColor.textMuted)
        }
        .padding(VizitSpace.md).background(VizitColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
        .overlay { RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous).stroke(VizitColor.border, lineWidth: 1) }
    }
}

private struct CourseDetailScreen: View {
    let course: BusinessCourse
    @Environment(\.openURL) private var openURL
    @AppStorage("education.completedLessonIDs") private var completedLessonIDs = ""
    @State private var selectedLessonID: String?

    private var lessons: [CourseModule] { course.modules }
    private var selectedLesson: CourseModule { lessons.first { $0.id == selectedLessonID } ?? lessons[0] }
    private var completed: Set<String> { Set(completedLessonIDs.split(separator: "|").map(String.init)) }
    private var learningModules: [[CourseModule]] {
        stride(from: 0, to: lessons.count, by: 2).map { Array(lessons[$0..<min($0 + 2, lessons.count)]) }
    }

    private func markSelectedLessonComplete() {
        var next = completed
        next.insert(selectedLesson.id)
        completedLessonIDs = next.sorted().joined(separator: "|")
    }

    private func videoURL(for lesson: CourseModule) -> String {
        YouTubeVideoID.from(lesson.resourceURL) == nil ? course.videoURL : lesson.resourceURL
    }

    var body: some View {
        PortalScroll(title: course.title, subtitle: course.description) {
            VizitPanel {
                HStack(spacing: VizitSpace.sm) {
                    VizitIconChip(systemImage: course.icon, tint: VizitColor.primary, background: VizitColor.primarySubtle)
                    VStack(alignment: .leading) {
                        Text("\(course.duration) · \(course.level)").font(VizitFont.label).foregroundStyle(VizitColor.textPrimary)
                        Text("\(learningModules.count) modul · \(lessons.count) videólecke").font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)
                    }
                }
            }
            VStack(alignment: .leading, spacing: VizitSpace.xs) {
                HStack {
                    Text("Kurzus haladása").font(VizitFont.label).foregroundStyle(VizitColor.textPrimary)
                    Spacer()
                    Text("\(completed.intersection(Set(lessons.map(\.id))).count)/\(lessons.count) lecke")
                        .font(VizitFont.caption).foregroundStyle(VizitColor.textMuted)
                }
                ProgressView(value: Double(completed.intersection(Set(lessons.map(\.id))).count), total: Double(lessons.count))
                    .tint(VizitColor.primary)
            }

            VStack(alignment: .leading, spacing: VizitSpace.sm) {
                YouTubeLessonPlayer(url: videoURL(for: selectedLesson), onCompleted: markSelectedLessonComplete)
                    .id(selectedLesson.id)
                    .frame(maxWidth: .infinity).aspectRatio(16 / 9, contentMode: .fit)
                    .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
                Text("AKTUÁLIS LECKE").vizitOverline().foregroundStyle(VizitColor.primary)
                Text(selectedLesson.title).font(VizitFont.h3).foregroundStyle(VizitColor.textPrimary)
                Text(selectedLesson.summary).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textSecondary)
                HStack(spacing: VizitSpace.xs) {
                    Image(systemName: completed.contains(selectedLesson.id) ? "checkmark.circle.fill" : "play.circle")
                    Text(completed.contains(selectedLesson.id) ? "Lecke teljesítve" : "A lecke a videó legalább 90%-ának lejátszása után lesz kész")
                        .font(VizitFont.caption)
                }
                .foregroundStyle(completed.contains(selectedLesson.id) ? VizitColor.success : VizitColor.textMuted)
                if selectedLesson.resourceURL != videoURL(for: selectedLesson) {
                    VizitButton(title: selectedLesson.resourceTitle, systemImage: "doc.text", kind: .secondary) {
                        guard let url = SafeLink.https(selectedLesson.resourceURL) else { return }
                        openURL(url)
                    }
                }
            }
            .padding(VizitSpace.md).background(VizitColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
            .overlay { RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous).stroke(VizitColor.border, lineWidth: 1) }

            VizitSectionHeader(title: "Tananyag")
            ForEach(Array(learningModules.enumerated()), id: \.offset) { moduleIndex, moduleLessons in
                let moduleComplete = moduleLessons.allSatisfy { completed.contains($0.id) }
                VStack(alignment: .leading, spacing: VizitSpace.sm) {
                    HStack(spacing: VizitSpace.sm) {
                        Image(systemName: moduleComplete ? "checkmark.circle.fill" : "rectangle.stack.fill")
                            .foregroundStyle(moduleComplete ? VizitColor.success : VizitColor.primary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(moduleIndex + 1). MODUL").vizitOverline().foregroundStyle(VizitColor.textMuted)
                            Text(moduleIndex == 0 ? "Alapok és felkészülés" : "Gyakorlati alkalmazás")
                                .font(VizitFont.h3).foregroundStyle(VizitColor.textPrimary)
                        }
                        Spacer()
                        Text("\(moduleLessons.filter { completed.contains($0.id) }.count)/\(moduleLessons.count)")
                            .font(VizitFont.caption).foregroundStyle(VizitColor.textMuted)
                    }
                    ForEach(Array(moduleLessons.enumerated()), id: \.element.id) { lessonIndex, lesson in
                        if lessonIndex > 0 { VizitDivider() }
                        Button { selectedLessonID = lesson.id } label: {
                            HStack(spacing: VizitSpace.sm) {
                                Image(systemName: completed.contains(lesson.id) ? "checkmark.circle.fill" : "play.circle")
                                    .font(.system(size: 21)).foregroundStyle(completed.contains(lesson.id) ? VizitColor.success : VizitColor.primary)
                                VStack(alignment: .leading, spacing: 3) {
                                    Text("\(moduleIndex + 1).\(lessonIndex + 1) · VIDEÓLECKE")
                                        .font(VizitFont.caption).foregroundStyle(VizitColor.textMuted)
                                    Text(lesson.title).font(VizitFont.label).foregroundStyle(VizitColor.textPrimary)
                                }
                                Spacer()
                                Image(systemName: selectedLesson.id == lesson.id ? "speaker.wave.2.fill" : "chevron.right")
                                    .font(.system(size: 13, weight: .semibold)).foregroundStyle(VizitColor.textMuted)
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(VizitSpace.md).background(VizitColor.surface)
                .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))
                .overlay { RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous).stroke(VizitColor.border, lineWidth: 1) }
            }
            Text("A kész állapotot az alkalmazás automatikusan rögzíti a videó legalább 90%-ának tényleges lejátszása után. Kézzel nem módosítható.")
                .font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)
        }
        .onAppear { if selectedLessonID == nil { selectedLessonID = lessons.first?.id } }
    }
}

private enum YouTubeVideoID {
    static func from(_ value: String) -> String? {
        guard let components = URLComponents(string: value),
              let host = components.host?.lowercased() else { return nil }
        if host == "youtu.be" { return components.path.split(separator: "/").first.map(String.init) }
        if host.hasSuffix("youtube.com") {
            if components.path == "/watch" { return components.queryItems?.first(where: { $0.name == "v" })?.value }
            let parts = components.path.split(separator: "/")
            if parts.first == "embed", parts.count > 1 { return String(parts[1]) }
        }
        return nil
    }
}

private struct YouTubeLessonPlayer: UIViewRepresentable {
    let url: String
    let onCompleted: () -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onCompleted: onCompleted) }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = [.video, .audio]
        configuration.userContentController.add(context.coordinator, name: "lessonCompleted")
        let view = WKWebView(frame: .zero, configuration: configuration)
        view.isOpaque = false
        view.backgroundColor = .black
        view.scrollView.isScrollEnabled = false
        return view
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.onCompleted = onCompleted
        guard let videoID = YouTubeVideoID.from(url), context.coordinator.videoID != videoID else { return }
        context.coordinator.videoID = videoID
        webView.loadHTMLString(Self.html(videoID: videoID), baseURL: URL(string: "https://www.youtube-nocookie.com"))
    }

    static func dismantleUIView(_ webView: WKWebView, coordinator: Coordinator) {
        webView.configuration.userContentController.removeScriptMessageHandler(forName: "lessonCompleted")
        webView.stopLoading()
    }

    private static func html(videoID: String) -> String {
        let safeID = videoID.replacingOccurrences(of: "'", with: "")
        return """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
        <style>html,body,#player{margin:0;width:100%;height:100%;background:#000;overflow:hidden}</style></head>
        <body><div id="player"></div><script src="https://www.youtube.com/iframe_api"></script><script>
        var player, watched=0, tick=0;
        function onYouTubeIframeAPIReady(){ player=new YT.Player('player',{videoId:'\(safeID)',playerVars:{playsinline:1,rel:0},events:{onStateChange:onState}}); }
        function onState(e){
          if(e.data===YT.PlayerState.PLAYING && !tick){ tick=setInterval(()=>{watched+=1;},1000); }
          if(e.data!==YT.PlayerState.PLAYING && tick){clearInterval(tick);tick=0;}
          if(e.data===YT.PlayerState.ENDED){ const d=player.getDuration(); if(d>0 && watched/d>=0.9){window.webkit.messageHandlers.lessonCompleted.postMessage('done');} }
        }
        </script></body></html>
        """
    }

    final class Coordinator: NSObject, WKScriptMessageHandler {
        var videoID: String?
        var onCompleted: () -> Void
        init(onCompleted: @escaping () -> Void) { self.onCompleted = onCompleted }
        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            guard message.name == "lessonCompleted" else { return }
            DispatchQueue.main.async { self.onCompleted() }
        }
    }
}

private struct DigitalHelpScreen: View {
    @State private var inCall = false
    @State private var microphone = true
    @State private var camera = true
    var body: some View {
        PortalScroll(title: "Digitális segítség", subtitle: "Szakértői videókonzultáció élményének interaktív előnézete.") {
            VStack(spacing: VizitSpace.lg) {
                HStack { Text(inCall ? "KAPCSOLÓDVA · DEMÓ" : "BEMUTATÓ MÓD").vizitOverline(); Spacer(); Text(inCall ? "00:24" : "ELŐNÉZET").font(VizitFont.caption) }
                    .foregroundStyle(Color.white.opacity(0.82))
                Spacer()
                ZStack { Circle().fill(VizitColor.primary).frame(width: 116, height: 116); Image(systemName: "person.fill").font(.system(size: 54)).foregroundStyle(.white) }
                VStack(spacing: VizitSpace.xxs) {
                    Text(inCall ? "VIZIT digitális tanácsadó" : "Próbahívás").font(VizitFont.h3).foregroundStyle(.white)
                    Text(inCall ? "A kapcsolat bemutató módban fut" : "Ellenőrizd a kamerát és a mikrofont").font(VizitFont.bodySmall).foregroundStyle(Color.white.opacity(0.72))
                }
                Spacer()
                HStack(spacing: VizitSpace.md) {
                    CallControl(systemImage: microphone ? "mic.fill" : "mic.slash.fill", label: "Mikrofon") { microphone.toggle() }
                    CallControl(systemImage: camera ? "video.fill" : "video.slash.fill", label: "Kamera") { camera.toggle() }
                    if inCall { CallControl(systemImage: "phone.down.fill", label: "Befejezés", tint: VizitColor.error) { inCall = false } }
                }
            }
            .padding(VizitSpace.lg).frame(maxWidth: .infinity, minHeight: 500)
            .background(VizitColor.ink).clipShape(RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous))
            if !inCall { VizitButton(title: "Próbahívás indítása", systemImage: "video.fill") { inCall = true } }
            Text("Ez a 6.1 verzió interaktív bemutatója: nem kapcsol valódi tanácsadóhoz és nem továbbít hangot vagy videót.")
                .font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)
        }
    }
}

private struct CallControl: View {
    let systemImage: String
    let label: String
    var tint: Color = .white
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(spacing: VizitSpace.xxs) {
                Image(systemName: systemImage).font(.system(size: 20, weight: .semibold)).foregroundStyle(tint)
                    .frame(width: 52, height: 52).background(Color.white.opacity(0.14)).clipShape(Circle())
                Text(label).font(VizitFont.caption).foregroundStyle(Color.white.opacity(0.8))
            }
        }.buttonStyle(.plain)
    }
}

private struct BusinessToolkitScreen: View {
    @Environment(\.openURL) private var openURL
    @State private var completedGuideIDs = Set<String>()
    private var score: Int { completedGuideIDs.count * 100 / toolkitGuides.count }
    var body: some View {
        PortalScroll(
            title: "Vállalkozói eszköztár",
            subtitle: "Válassz célt, hajtsd végre a lépéseket, majd nyisd meg közvetlenül a szükséges hivatalos szolgáltatást."
        ) {
            VizitPanel {
                HStack(spacing: VizitSpace.md) {
                    Text("\(score)%").font(VizitFont.h3).foregroundStyle(VizitColor.onPrimary)
                        .frame(width: 68, height: 68).background(VizitColor.primary).clipShape(Circle())
                    VStack(alignment: .leading) {
                        Text("Megvalósítási állapot").font(VizitFont.h3).foregroundStyle(VizitColor.textPrimary)
                        Text("\(completedGuideIDs.count)/\(toolkitGuides.count) útmutató teljesítve")
                            .font(VizitFont.bodySmall).foregroundStyle(VizitColor.textSecondary)
                    }
                }
            }
            ForEach(toolkitGuides) { guide in
                let completed = completedGuideIDs.contains(guide.id)
                VStack(alignment: .leading, spacing: VizitSpace.md) {
                    HStack(alignment: .top, spacing: VizitSpace.sm) {
                        VizitIconChip(systemImage: guide.icon, tint: VizitColor.primary, background: VizitColor.primarySubtle)
                        VStack(alignment: .leading, spacing: VizitSpace.xxs) {
                            Text(guide.title).font(VizitFont.h3).foregroundStyle(VizitColor.textPrimary)
                            Text(guide.description).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textSecondary)
                        }
                        Spacer(minLength: 0)
                        if completed { Image(systemName: "checkmark.circle.fill").foregroundStyle(VizitColor.success) }
                    }
                    VStack(alignment: .leading, spacing: VizitSpace.xxs) {
                        Text("ELÉRENDŐ EREDMÉNY").vizitOverline().foregroundStyle(VizitColor.primary)
                        Text(guide.result).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textPrimary)
                    }
                    .padding(VizitSpace.sm).frame(maxWidth: .infinity, alignment: .leading)
                    .background(VizitColor.primarySubtle)
                    .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))

                    Text("Lépések").font(VizitFont.label).foregroundStyle(VizitColor.textPrimary)
                    ForEach(Array(guide.steps.enumerated()), id: \.offset) { index, step in
                        HStack(alignment: .top, spacing: VizitSpace.sm) {
                            Text("\(index + 1)").font(VizitFont.caption).foregroundStyle(VizitColor.textSecondary)
                                .frame(width: 24, height: 24).background(VizitColor.controlTrack).clipShape(Circle())
                            Text(step).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textSecondary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }

                    Text("Közvetlen linkek").font(VizitFont.label).foregroundStyle(VizitColor.textPrimary)
                    VizitGroup {
                        ForEach(Array(guide.links.enumerated()), id: \.element.id) { index, link in
                            if index > 0 { VizitDivider() }
                            VizitRow(label: link.title, systemImage: "link", supporting: link.description) {
                                guard let url = SafeLink.https(link.url) else { return }
                                openURL(url)
                            }
                        }
                    }
                    VizitButton(
                        title: completed ? "Teljesítve" : "Útmutató teljesítve",
                        systemImage: "checkmark.circle",
                        kind: completed ? .tertiary : .secondary
                    ) {
                        if completed { completedGuideIDs.remove(guide.id) } else { completedGuideIDs.insert(guide.id) }
                    }
                }
                .padding(VizitSpace.md).background(VizitColor.surface)
                .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
                .overlay { RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous).stroke(VizitColor.border, lineWidth: 1) }
            }
            VizitPanel {
                HStack(alignment: .top, spacing: VizitSpace.sm) {
                    VizitIconChip(systemImage: "rocket.fill", tint: VizitColor.primary, background: VizitColor.primarySubtle)
                    VStack(alignment: .leading, spacing: VizitSpace.xxs) {
                        Text(score < 50 ? "Következő lépés: válassz egy útmutatót" : "Következő lépés: mérd az eredményt").font(VizitFont.label)
                        Text(score < 50 ? "Ne mindent egyszerre: kezdd azzal, amelyik most a legtöbb hibát vagy elveszett időt okozza." : "Harminc nap múlva ellenőrizd, csökkent-e a hiba, az átfutási idő vagy a költség.")
                            .font(VizitFont.bodySmall).foregroundStyle(VizitColor.textSecondary)
                    }
                }
            }
            Text("A linkek hivatalos vagy széles körben használt külső szolgáltatásokhoz vezetnek. Az adózási és jogi döntést egyeztesd könyvelővel vagy jogi szakértővel.")
                .font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)
        }
    }
}

private struct PortalScroll<Content: View>: View {
    let title: String
    let subtitle: String
    let content: Content

    init(title: String, subtitle: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.subtitle = subtitle
        self.content = content()
    }

    var body: some View {
        VizitScreen {
            ScrollView {
                VStack(alignment: .leading, spacing: VizitSpace.md) {
                    Text(subtitle).font(VizitFont.body).foregroundStyle(VizitColor.textSecondary)
                    content
                }
                .padding(.horizontal, VizitSpace.md).padding(.bottom, VizitSpace.xxl)
                .frame(maxWidth: 620).frame(maxWidth: .infinity)
            }
        }
        .navigationTitle(title).navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Settings

/// System-level settings structure, with destructive actions isolated in their
/// own outlined group at the very bottom.
struct SettingsScreen: View {
    @EnvironmentObject private var store: AppStore
    @Binding var themeMode: ThemeMode
    @State private var confirmReset = false
    @State private var error: String?
    @State private var confirmLogout = false
    @State private var confirmDelete = false
    @State private var deletionPhrase = ""

    private var themeIndex: Binding<Int> {
        Binding(
            get: { ThemeMode.allCases.firstIndex(of: themeMode) ?? 0 },
            set: { newValue in
                let mode = ThemeMode.allCases[newValue]
                themeMode = mode
                ThemeStorage.save(mode)
            }
        )
    }

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.md) {
                        VizitBrandHeader(style: .compact)

                        Text("Beállítások")
                            .font(VizitFont.h1)
                            .foregroundStyle(VizitColor.textPrimary)
                            .padding(.top, VizitSpace.md)

                        VizitSectionHeader(title: "Megjelenés")
                        VizitGroup {
                            VizitRow(
                                label: "Téma",
                                systemImage: "circle.lefthalf.filled",
                                supporting: "Alapértelmezés: világos",
                                showsChevron: false
                            )
                            VizitDivider()
                            VizitSegmentedControl(
                                options: ThemeMode.allCases.map(\.label),
                                selection: themeIndex
                            )
                            .padding(VizitSpace.md)
                        }

                        VizitSectionHeader(title: "Szinkronizálás")
                        VizitGroup {
                            VizitRow(
                                label: "Profil szinkron",
                                systemImage: "arrow.triangle.2.circlepath",
                                supporting: store.syncStatus.label,
                                showsChevron: false
                            )
                            if [.pending, .failed].contains(store.syncStatus), store.isOnline {
                                VizitDivider()
                                VizitRow(label: "Szinkron újrapróbálása", systemImage: "arrow.clockwise") {
                                    store.retrySync()
                                }
                            }
                            if store.syncStatus == .conflict, store.isOnline {
                                VizitDivider()
                                VStack(alignment: .leading, spacing: VizitSpace.xs) {
                                    VizitBanner(
                                        text: "Válassz példányt. A felhőből letöltés előtt a helyi változatról biztonsági másolat készül.",
                                        tone: .warning
                                    )
                                    VizitButton(title: "Helyi változat feltöltése", kind: .secondary) {
                                        Task { await store.resolveSyncConflict(keepLocal: true) }
                                    }
                                    VizitButton(title: "Felhőben lévő változat használata", kind: .tertiary) {
                                        Task { await store.resolveSyncConflict(keepLocal: false) }
                                    }
                                }
                                .padding(VizitSpace.md)
                            }
                        }

                        VizitSectionHeader(title: "Adatvédelem")
                        VizitPanel {
                            VStack(alignment: .leading, spacing: VizitSpace.xs) {
                                Text("A megosztott vagy Kontaktokba mentett példányokat a helyi törlés nem vonja vissza.")
                                    .font(VizitFont.bodySmall)
                                    .foregroundStyle(VizitColor.textSecondary)
                                    .fixedSize(horizontal: false, vertical: true)
                                if let issue = store.storageError {
                                    VizitBanner(text: issue, tone: .error)
                                }
                            }
                        }

                        VizitSectionHeader(title: "Fiók", tone: VizitColor.error)
                        VizitGroup(danger: true) {
                            if !store.accountEmail.isEmpty {
                                VizitRow(
                                    label: "Belépve",
                                    systemImage: "person.crop.circle",
                                    value: store.accountEmail,
                                    showsChevron: false
                                )
                                VizitDivider()
                            }
                            VizitRow(
                                label: "Helyi gyorsítótár törlése",
                                systemImage: "trash",
                                destructive: true,
                                showsChevron: false
                            ) { confirmReset = true }
                            VizitDivider()
                            VizitRow(
                                label: "Kijelentkezés",
                                systemImage: "rectangle.portrait.and.arrow.right",
                                destructive: true,
                                showsChevron: false
                            ) { confirmLogout = true }
                            VizitDivider()
                            VizitRow(
                                label: "Fiók végleges törlése",
                                systemImage: "person.crop.circle.badge.xmark",
                                supporting: "A fiók és a szerveradatok is törlődnek.",
                                destructive: true,
                                showsChevron: false
                            ) { confirmDelete = true }
                        }

                        Text("VIZIT \(versionText)")
                            .font(VizitFont.caption)
                            .foregroundStyle(VizitColor.textMuted)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, VizitSpace.lg)
                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.bottom, VizitSpace.xxl)
                    .frame(maxWidth: 620)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationBarHidden(true)
            .confirmationDialog(
                "Törlöd a helyi gyorsítótárat és a profilképet erről az iPhone-ról?",
                isPresented: $confirmReset,
                titleVisibility: .visible
            ) {
                Button("Helyi adatok törlése", role: .destructive) {
                    do { try store.reset() } catch { self.error = error.localizedDescription }
                }
                Button("Mégse", role: .cancel) {}
            }
            .confirmationDialog(
                "Biztosan kijelentkezel? A helyi gyorsítótár is törlődik.",
                isPresented: $confirmLogout,
                titleVisibility: .visible
            ) {
                Button("Kijelentkezés", role: .destructive) { Task { await store.logout() } }
                Button("Mégse", role: .cancel) {}
            }
            .sheet(isPresented: $confirmDelete) { deleteAccountSheet }
            .alert("A művelet nem sikerült", isPresented: Binding(
                get: { error != nil }, set: { if !$0 { error = nil } }
            )) {
                Button("Rendben", role: .cancel) { error = nil }
            } message: { Text(error ?? "") }
        }
    }

    private var deleteAccountSheet: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.md) {
                        VizitBanner(
                            text: "Ez törli a VIZIT-fiókot és a hozzá tartozó szerveradatokat. A művelet nem vonható vissza.",
                            tone: .error
                        )
                        VizitTextField(
                            label: "Megerősítés",
                            text: $deletionPhrase,
                            placeholder: "TÖRLÉS",
                            helper: "Írd be nagybetűkkel: TÖRLÉS",
                            autocapitalization: .characters
                        )
                        VizitButton(title: "Fiók végleges törlése", kind: .destructive) {
                            confirmDelete = false
                            Task { await store.deleteAccount(confirmation: deletionPhrase) }
                        }
                    }
                    .padding(VizitSpace.md)
                }
            }
            .navigationTitle("Fiók törlése")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Mégse") { confirmDelete = false }
                }
            }
        }
    }

    private var versionText: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0.3"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "1"
        return "\(version) (\(build))"
    }
}

extension SyncStatus {
    var tone: VizitTone {
        switch self {
        case .synced: return .success
        case .syncing, .localOnly, .pending: return .info
        case .conflict: return .warning
        case .failed: return .error
        }
    }
}
