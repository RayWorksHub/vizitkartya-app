import SwiftUI
import Contacts
import Photos
import PhotosUI
import CoreImage.CIFilterBuiltins
import WebKit

// MARK: - Home

/// Home answers four questions immediately: who is signed in, what their card
/// looks like, how to hand it over, and whether anything needs attention.
/// One primary action, three shortcuts, then status.
struct HomeScreen: View {
    @EnvironmentObject private var store: AppStore
    @EnvironmentObject private var presentation: CardPresentationStore
    @Binding var selectedTab: RootTab
    @State private var editing = false
    @State private var showScanner = false
    @State private var showKnowledgeHub = false

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.lg) {
                        VizitLargeTitle(title: "VIZIT", tracking: 4) {
                            VizitIdentityChip(profile: store.profile) { selectedTab = .card }
                        }

                        VizitDigitalCard(
                            profile: store.profile,
                            nameIdentifier: store.hasProfile ? "card.name" : nil,
                            presentation: presentation.value
                        )
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
                            .accessibilityIdentifier("home.showQR")
                            QuickTile(systemImage: "qrcode.viewfinder", title: "Beolvasás", subtitle: "Új kapcsolat") {
                                showScanner = true
                            }
                            .accessibilityIdentifier("home.scan")
                            QuickTile(systemImage: "pencil", title: "Szerkesztés", subtitle: "Adataim") {
                                editing = true
                            }
                            .accessibilityIdentifier("card.edit")
                        }

                        if let issue = store.storageError {
                            VizitBanner(text: issue, tone: .error)
                        }

                        VizitGroup {
                            VizitRow(
                                label: "Vállalkozói Portál",
                                systemImage: "book.closed",
                                supporting: "Edukáció, források és digitális segítség"
                            ) { showKnowledgeHub = true }
                            .accessibilityIdentifier("home.businessPortal")
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
    @EnvironmentObject private var presentation: CardPresentationStore
    @Environment(\.openURL) private var openURL
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
                        VizitLargeTitle(title: "Névjegyem") {
                            Button(store.hasProfile ? "Szerkesztés" : "Létrehozás") { editing = true }
                                .font(VizitFont.label)
                                .foregroundStyle(VizitColor.primary)
                                .frame(minHeight: VizitMetrics.minTouchTarget)
                                .disabled(store.storageError != nil)
                        }

                        VizitDigitalCard(profile: store.profile, presentation: presentation.value)

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

                            let socials = store.profile.socialProfiles.filter { !$0.url.isEmpty }
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
            VizitRow(label: entry.0, systemImage: entry.1, supporting: entry.2) {
                guard let url = destination(for: entry.0, kind: entry.2) else { return }
                openURL(url)
            }
        }
    }

    private func destination(for value: String, kind: String) -> URL? {
        switch kind {
        case "Telefon":
            let allowed = value.filter { $0.isNumber || $0 == "+" }
            return URL(string: "tel:\(allowed)")
        case "E-mail":
            return URL(string: "mailto:\(value)")
        case "Weboldal":
            return SafeLink.https(value)
        case "Cím":
            var components = URLComponents(string: "https://maps.apple.com/")
            components?.queryItems = [URLQueryItem(name: "q", value: value)]
            return components?.url
        default:
            return nil
        }
    }

    private func emptyToNil(_ value: String) -> String? { value.isEmpty ? nil : value }
}

// MARK: - Share

/// Every hand-off route in one place. The QR surface is an "always-light
/// island": pure white with a quiet zone and fixed dark ink in both themes,
/// because a tinted or low-contrast code is a code that does not scan.
private enum ShareQRMode: String, CaseIterable, Identifiable {
    case contact, photo, profile

    var id: String { rawValue }
    var label: String {
        switch self {
        case .contact: return "Kontakt"
        case .photo: return "Fényképes"
        case .profile: return "Profil"
        }
    }
}

struct ShareScreen: View {
    @EnvironmentObject private var store: AppStore
    @EnvironmentObject private var presentation: CardPresentationStore
    @State private var shareFile: ShareFile?
    @State private var temporaryURL: URL?
    @State private var showFullScreenQR = false
    @State private var editingProfile = false
    @State private var showAccountDetails = false
    @State private var error: String?
    @State private var copiedProfileLink = false
    @State private var savedQRCode = false
    @State private var showNFCInformation = false
    @State private var selectedMode: ShareQRMode = .contact
    @State private var photoQRPayload: String?

    /// What actually leaves the device: the stored profile minus whatever the
    /// owner switched off in Adatláthatóság.
    private var sharedProfile: ContactProfile { store.profile.visible(through: presentation.value) }

    private var modeIndex: Binding<Int> {
        Binding(
            get: { ShareQRMode.allCases.firstIndex(of: selectedMode) ?? 0 },
            set: { index in
                guard ShareQRMode.allCases.indices.contains(index) else { return }
                copiedProfileLink = false
                savedQRCode = false
                selectedMode = ShareQRMode.allCases[index]
            }
        )
    }

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.md) {
                        VizitLargeTitle("Megosztás")

                        VizitSegmentedControl(
                            options: ShareQRMode.allCases.map(\.label),
                            selection: modeIndex,
                            accessibilityIdentifier: "share.qrMode"
                        )

                        if let image = currentQRImage {
                            qrIsland
                            VizitButton(
                                title: "Teljes képernyőn",
                                systemImage: "arrow.up.left.and.arrow.down.right",
                                kind: .secondary
                            ) { showFullScreenQR = true }
                            .accessibilityIdentifier("share.fullscreen")

                        } else {
                            unavailableMode
                        }

                        VizitSectionHeader(title: "További módok")
                        VizitGroup {
                            VizitRow(
                                label: "NFC érintés",
                                systemImage: "wave.3.right",
                                value: "Készen áll",
                                supporting: "Érintéses átadás és iPhone-kompatibilitás"
                            ) { showNFCInformation = true }
                            VizitDivider()
                            VizitRow(
                                label: "Link másolása",
                                systemImage: "link",
                                supporting: profileLinkSupportingText
                            ) { handleProfileLink() }
                            VizitDivider()
                            VizitRow(
                                label: "Megosztás…",
                                systemImage: "square.and.arrow.up",
                                supporting: "Üzenet, e-mail, AirDrop"
                            ) { shareCurrentSelection() }
                            if let image = currentQRImage {
                                VizitDivider()
                                VizitRow(
                                    label: "QR mentése",
                                    systemImage: "arrow.down.to.line",
                                    supporting: "Mentés a Fotók közé"
                                ) { saveQRCode(image) }
                            }
                        }

                        if copiedProfileLink {
                            VizitBanner(text: "A profil hivatkozását a vágólapra másoltuk.", tone: .success)
                        }
                        if savedQRCode {
                            VizitBanner(text: "A QR-kódot elmentettük a Fotók közé.", tone: .success)
                        }
                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.bottom, VizitSpace.xxl)
                    .frame(maxWidth: 620)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationBarHidden(true)
            .task(id: sharedProfile) {
                photoQRPayload = PhotoContactQR.payload(sharedProfile)
            }
            .sheet(isPresented: $editingProfile) { ProfileEditor(draft: store.profile) }
            .sheet(isPresented: $showAccountDetails) { accountDetailsSheet }
            .sheet(item: $shareFile, onDismiss: cleanupShareFile) { file in
                ActivitySheet(url: file.url)
            }
            .fullScreenCover(isPresented: $showFullScreenQR) {
                FullScreenQRView(
                    image: currentQRImage,
                    title: store.profile.displayName,
                    subtitle: [store.profile.jobTitle, store.profile.company]
                        .filter { !$0.isEmpty }.joined(separator: " · ")
                ) {
                    showFullScreenQR = false
                }
            }
            .alert("A művelet nem sikerült", isPresented: Binding(
                get: { error != nil }, set: { if !$0 { error = nil } }
            )) {
                Button("Rendben", role: .cancel) { error = nil }
            } message: { Text(error ?? "") }
            .alert("NFC iPhone-on", isPresented: $showNFCInformation) {
                Button("Rendben", role: .cancel) {}
            } message: {
                Text("Az iPhone nem enged tetszőleges telefon–telefon NFC-kártyaemulációt. A biztos átadási mód a QR-kód, az AirDrop, a névjegyfájl vagy egy programozott fizikai NFC-kártya.")
            }
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
            VStack(spacing: VizitSpace.xs) {
                Text(qrTitle)
                    .font(VizitFont.title)
                    .foregroundStyle(VizitColor.ink)
                    .multilineTextAlignment(.center)
                Text(captionText)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(Color(uiColor: UIColor(hex: 0x4A5568)))
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                if selectedMode != .profile, let payload = qrPayload {
                    VizitStatusPill(
                        text: "\(payload.utf8.count) / \(VCard.maximumHighCorrectionBytes.formatted()) bájt",
                        tone: .success
                    )
                }
            }
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
        switch selectedMode {
        case .contact:
            return "Ez a kód a teljes névjegyedet tartalmazza, és internet nélkül is bekerül a másik telefon névjegyei közé."
        case .photo:
            return "A teljes névjegyet és a méretre igazított profilképet is tartalmazza. Internet nem szükséges."
        case .profile:
            return "A naprakész nyilvános profilodat nyitja meg. Ehhez internetkapcsolat szükséges."
        }
    }

    private var qrTitle: String {
        selectedMode == .profile ? "Nyisd meg a profilomat" : "Olvastasd be a másik telefonnal"
    }

    private func compactActions(image: UIImage) -> some View {
        HStack(spacing: VizitSpace.sm) {
            VizitButton(title: "NFC", systemImage: "wave.3.right", kind: .secondary) {
                showNFCInformation = true
            }
            VizitButton(title: "Link", systemImage: "link", kind: .secondary) {
                handleProfileLink()
            }
            VizitButton(title: "Mentés", systemImage: "arrow.down.to.line", kind: .secondary) {
                saveQRCode(image)
            }
        }
    }

    @ViewBuilder
    private var unavailableMode: some View {
        switch selectedMode {
        case .photo:
            if sharedProfile.photoBase64.isEmpty {
                VizitEmptyState(
                    systemImage: "person.crop.circle.badge.plus",
                    title: "Nincs még profilképed",
                    message: "A Fényképes QR-hez válassz profilképet. A Kontakt QR kép nélkül is átadja a teljes névjegyet.",
                    actionTitle: "Profilkép hozzáadása",
                    action: { editingProfile = true }
                )
            } else {
                VizitEmptyState(
                    systemImage: "exclamationmark.triangle",
                    title: "A profilkép nem fér bele a kódba",
                    message: "A névjegyed adatai már kitöltik a QR-kapacitást. Választhatsz kisebb profilképet, vagy használhatod a Kontakt módot – abból semmilyen adat nem marad ki, csak a kép.",
                    actionTitle: "Vissza a Kontakt QR-hoz",
                    action: { selectedMode = .contact }
                )
            }
        case .profile:
            VStack(spacing: VizitSpace.md) {
                VizitEmptyState(
                    systemImage: "globe",
                    title: "Nincs még publikus profil",
                    message: "A Profil QR-hez kapcsold be a nyilvános profilt, és várd meg a sikeres szinkront. Addig a Kontakt QR minden adatot átad.",
                    actionTitle: "Kontakt QR megnyitása",
                    action: { selectedMode = .contact }
                )
                VizitBanner(
                    text: "A szinkron újrapróbálásra vár. A kód a sikeres mentés után jelenik meg.",
                    tone: .warning
                )
            }
        case .contact:
            VizitEmptyState(
                systemImage: "person.crop.circle.badge.exclamationmark",
                title: "Állítsd össze a névjegyed",
                message: "A Kontakt QR-hoz név és legalább egy telefonszám vagy e-mail-cím szükséges.",
                actionTitle: "Névjegy szerkesztése",
                action: { editingProfile = true }
            )
        }
    }

    private var currentQRImage: UIImage? {
        guard let payload = qrPayload else { return nil }
        return QRImage.make(payload)
    }

    private var qrPayload: String? {
        switch selectedMode {
        case .contact:
            return try? VCard.qrPayload(sharedProfile, includePhoto: false)
        case .photo:
            return photoQRPayload
        case .profile:
            return publicURL?.absoluteString
        }
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

    private var profileLinkSupportingText: String {
        if let publicURL { return publicURL.absoluteString }
        if !store.profile.isPublic { return "Kapcsold be a nyilvános profilt" }
        if store.syncStatus != .synced { return "Sikeres szinkronizálás után másolható" }
        return "A profilhivatkozás még nem érhető el"
    }

    private func handleProfileLink() {
        guard let publicURL else {
            editingProfile = true
            return
        }
        UIPasteboard.general.string = publicURL.absoluteString
        copiedProfileLink = true
    }

    private func shareCurrentSelection() {
        if selectedMode == .profile {
            guard let publicURL else {
                error = "A publikus profil csak sikeres szinkron után osztható meg."
                return
            }
            temporaryURL = nil
            shareFile = ShareFile(url: publicURL)
            return
        }
        do {
            let file = try ContactBridge.shareFile(
                sharedProfile,
                includePhoto: selectedMode == .photo
            )
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

    private func saveQRCode(_ image: UIImage) {
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else {
                Task { @MainActor in
                    error = "A QR-kód mentéséhez engedélyezd a Fotókhoz való hozzáférést a Beállításokban."
                }
                return
            }
            PHPhotoLibrary.shared().performChanges {
                PHAssetChangeRequest.creationRequestForAsset(from: image)
            } completionHandler: { success, saveError in
                Task { @MainActor in
                    if success {
                        savedQRCode = true
                    } else {
                        error = saveError?.localizedDescription ?? "A QR-kód mentése nem sikerült."
                    }
                }
            }
        }
    }
}

private struct FullScreenQRView: View {
    @Environment(\.scenePhase) private var scenePhase
    let image: UIImage?
    let title: String
    let subtitle: String
    let dismiss: () -> Void
    @State private var originalBrightness: CGFloat?

    var body: some View {
        NavigationStack {
            VizitScreen {
                VStack(spacing: VizitSpace.xl) {
                    Spacer(minLength: VizitSpace.lg)
                    VStack(spacing: VizitSpace.lg) {
                        if let image {
                            Image(uiImage: image)
                                .interpolation(.none)
                                .resizable()
                                .scaledToFit()
                                .frame(maxWidth: 360)
                                .padding(VizitSpace.md)
                        }
                        if !title.isEmpty {
                            VStack(spacing: VizitSpace.xs) {
                                Text(title)
                                    .font(VizitFont.h2)
                                    .foregroundStyle(VizitColor.ink)
                                if !subtitle.isEmpty {
                                    Text(subtitle)
                                        .font(VizitFont.bodySmall)
                                        .foregroundStyle(Color(uiColor: UIColor(hex: 0x6B7688)))
                                }
                            }
                        }
                    }
                    .padding(VizitSpace.xl)
                    .frame(maxWidth: 440)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous))
                    .overlay {
                        RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous)
                            .stroke(VizitColor.border, lineWidth: 1)
                    }

                    Text("A fényerő automatikusan maximumra vált a beolvasás idejére.")
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(VizitColor.textMuted)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, VizitSpace.xl)

                    Spacer(minLength: VizitSpace.lg)
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Bezárás", action: dismiss)
                        .buttonStyle(.bordered)
                        .buttonBorderShape(.capsule)
                }
            }
            .onAppear { maximizeBrightness() }
            .onDisappear { restoreBrightness() }
            .onChange(of: scenePhase) { phase in
                if phase == .active { maximizeBrightness() }
                else { restoreBrightness() }
            }
        }
    }

    private func maximizeBrightness() {
        if originalBrightness == nil { originalBrightness = UIScreen.main.brightness }
        UIScreen.main.brightness = 1
    }

    private func restoreBrightness() {
        guard let originalBrightness else { return }
        UIScreen.main.brightness = originalBrightness
    }
}

enum QRImage {
    private static let context = CIContext(options: [.useSoftwareRenderer: true])
    private static let logo: UIImage? = {
        guard let url = Bundle.main.url(forResource: "VizitLogoMark", withExtension: "png") else { return nil }
        return UIImage(contentsOfFile: url.path)
    }()

    static func make(_ payload: String) -> UIImage? {
        guard let code = code(for: payload) else { return nil }
        let scale: CGFloat = 10
        let scaled = code.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        let quietZone = CIImage(color: .white).cropped(to: scaled.extent.insetBy(dx: -4 * scale, dy: -4 * scale))
        let output = scaled.composited(over: quietZone)
        guard let cg = context.createCGImage(output, from: output.extent) else { return nil }
        return addingLogo(to: UIImage(cgImage: cg))
    }

    /// The approved board requires highest error correction, a four-module
    /// quiet zone and a fixed 14%-wide brand plate. Payloads that do not fit at
    /// level H fail closed instead of silently producing a weaker code.
    static func canEncode(_ payload: String) -> Bool {
        code(for: payload) != nil
    }

    private static func code(for payload: String) -> CIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(payload.utf8)
        filter.correctionLevel = "H"
        return filter.outputImage
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

enum PhotoContactQR {
    static func payload(_ profile: ContactProfile) -> String? {
        guard !profile.photoBase64.isEmpty,
              let bytes = Data(base64Encoded: profile.photoBase64),
              let source = UIImage(data: bytes) else { return nil }

        for side in [72, 64, 56, 48, 40, 32, 28, 24, 20, 16, 12, 10, 8, 6, 4] {
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
            for quality in [0.65, 0.55, 0.45, 0.35, 0.25, 0.18, 0.12, 0.08] {
                guard let encoded = image.jpegData(compressionQuality: quality),
                      let jpeg = jpegWithoutMetadata(encoded) else { continue }
                var candidate = profile
                candidate.photoBase64 = jpeg.base64EncodedString()
                if let value = try? VCard.qrPayload(candidate, includePhoto: true),
                   value.contains("PHOTO;ENCODING=b;TYPE=JPEG:"),
                   QRImage.canEncode(value) {
                    return value
                }
            }
        }

        // A baseline colour JPEG carries two quantisation tables and four
        // Huffman tables even at 4x4 pixels. For a contact with every optional
        // field populated that fixed overhead can be the last few hundred
        // bytes above QR version 40-H. A grayscale JPEG remains a real embedded
        // contact photo while using one component and roughly half the tables.
        for side in [16, 12, 10, 8, 6, 4] {
            guard let image = grayscaleThumbnail(source, side: side) else { continue }
            for quality in [0.55, 0.35, 0.18, 0.08] {
                guard let encoded = image.jpegData(compressionQuality: quality),
                      let jpeg = jpegWithoutMetadata(encoded) else { continue }
                var candidate = profile
                candidate.photoBase64 = jpeg.base64EncodedString()
                if let value = try? VCard.qrPayload(candidate, includePhoto: true),
                   value.contains("PHOTO;ENCODING=b;TYPE=JPEG:"),
                   QRImage.canEncode(value) {
                    return value
                }
            }
        }
        return nil
    }

    private static func grayscaleThumbnail(_ source: UIImage, side: Int) -> UIImage? {
        guard let sourceImage = source.cgImage else { return nil }
        let bytesPerRow = ((side + 15) / 16) * 16
        guard let context = CGContext(
            data: nil,
            width: side,
            height: side,
            bitsPerComponent: 8,
            bytesPerRow: bytesPerRow,
            space: CGColorSpaceCreateDeviceGray(),
            bitmapInfo: CGImageAlphaInfo.none.rawValue
        ) else { return nil }

        context.interpolationQuality = .medium
        context.translateBy(x: 0, y: CGFloat(side))
        context.scaleBy(x: 1, y: -1)
        let width = CGFloat(sourceImage.width)
        let height = CGFloat(sourceImage.height)
        let scale = max(CGFloat(side) / width, CGFloat(side) / height)
        let drawSize = CGSize(width: width * scale, height: height * scale)
        context.draw(sourceImage, in: CGRect(
            x: (CGFloat(side) - drawSize.width) / 2,
            y: (CGFloat(side) - drawSize.height) / 2,
            width: drawSize.width,
            height: drawSize.height
        ))
        guard let image = context.makeImage() else { return nil }
        return UIImage(cgImage: image)
    }

    /// `UIImage.jpegData` may attach a multi-kilobyte ICC/EXIF payload even to
    /// a tiny thumbnail. Those application segments describe the source image,
    /// not the pixels Contacts needs, and can by themselves overflow a level-H
    /// QR code. Preserve the actual JPEG image/colour-transform segments while
    /// removing only metadata segments before embedding the photo in the vCard.
    private static func jpegWithoutMetadata(_ data: Data) -> Data? {
        let bytes = [UInt8](data)
        guard bytes.count >= 4, bytes[0] == 0xff, bytes[1] == 0xd8 else { return nil }

        var output = Data([0xff, 0xd8])
        var index = 2
        while index < bytes.count {
            let markerStart = index
            guard bytes[index] == 0xff else { return nil }
            while index < bytes.count, bytes[index] == 0xff { index += 1 }
            guard index < bytes.count else { return nil }

            let marker = bytes[index]
            index += 1

            // Start-of-scan owns the entropy-coded remainder, where 0xff bytes
            // no longer follow the regular segment-length grammar.
            if marker == 0xda {
                output.append(contentsOf: bytes[markerStart...])
                return output
            }
            if marker == 0xd9 {
                output.append(contentsOf: bytes[markerStart..<index])
                return output
            }
            if marker == 0x01 || (0xd0...0xd7).contains(marker) {
                output.append(contentsOf: bytes[markerStart..<index])
                continue
            }

            guard index + 1 < bytes.count else { return nil }
            let length = Int(bytes[index]) << 8 | Int(bytes[index + 1])
            guard length >= 2, index + length <= bytes.count else { return nil }
            let segmentEnd = index + length

            // APP1...APP13, APP15 and COM are EXIF/XMP/ICC/vendor metadata.
            // APP0 (JFIF) and APP14 (Adobe colour transform) remain intact.
            let metadata = (0xe1...0xed).contains(marker) || marker == 0xef || marker == 0xfe
            if !metadata {
                output.append(contentsOf: bytes[markerStart..<segmentEnd])
            }
            index = segmentEnd
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
    @State private var scanning = true
    @State private var torchOn = false
    @State private var torchAvailable = false
    @State private var pickedCode: PhotosPickerItem?
    @State private var pendingText: String?
    @State private var incoming: IncomingContact?
    @State private var link: URL?
    @State private var message: String?

    private var isUITesting: Bool {
        ProcessInfo.processInfo.arguments.contains("--ui-testing")
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()
                if isUITesting {
                    Color(uiColor: UIColor(hex: 0x1B2437)).ignoresSafeArea()
                } else if scanning {
                    QRScanner(
                        onResult: finishScan,
                        onError: { text in
                            message = text
                            torchOn = false
                            scanning = false
                        },
                        torchOn: torchOn,
                        onTorchAvailability: { torchAvailable = $0 }
                    )
                    .ignoresSafeArea()
                }

                ScannerOverlay {
                    PhotosPicker(selection: $pickedCode, matching: .images, photoLibrary: .shared()) {
                        HStack(spacing: VizitSpace.xs) {
                            Image(systemName: "photo").font(.system(size: 15, weight: .semibold))
                            Text("Kód kiválasztása a képtárból").font(VizitFont.label)
                        }
                        .foregroundStyle(.white)
                        .padding(.horizontal, VizitSpace.md)
                        .frame(minHeight: VizitMetrics.minTouchTarget)
                        .background(Color.white.opacity(0.14))
                        .clipShape(Capsule())
                        .overlay { Capsule().stroke(Color.white.opacity(0.22), lineWidth: 1) }
                    }
                    .accessibilityIdentifier("scanner.photo")
                }
            }
            .navigationTitle("Beolvasás")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(Color.black, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Bezárás") {
                        torchOn = false
                        dismiss()
                    }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button(torchOn ? "Vaku ki" : "Vaku") { torchOn.toggle() }
                        .disabled(!torchAvailable && !isUITesting)
                        .accessibilityLabel(torchOn ? "Vaku kikapcsolása" : "Vaku bekapcsolása")
                }
            }
            .onChange(of: pickedCode) { _ in decodePickedCode() }
            .sheet(item: $incoming) { item in
                ContactEditor(contact: item.contact) { saved in
                    incoming = nil
                    if saved {
                        message = "A névjegyet elmentetted a Kontaktokba."
                    } else {
                        scanning = true
                    }
                }
            }
            .alert("Webcím a QR-kódban", isPresented: Binding(
                get: { link != nil },
                set: {
                    if !$0 {
                        link = nil
                        scanning = true
                    }
                }
            )) {
                Button("Mégse", role: .cancel) {
                    link = nil
                    scanning = true
                }
                Button("Megnyitás") {
                    if let url = link { openURL(url) }
                    link = nil
                    scanning = true
                }
            } message: { Text(link?.absoluteString ?? "") }
            .alert("Beolvasás", isPresented: Binding(
                get: { message != nil && incoming == nil },
                set: {
                    if !$0 {
                        message = nil
                        scanning = true
                    }
                }
            )) {
                Button("Rendben", role: .cancel) {
                    message = nil
                    scanning = true
                }
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    Button("Rendszerbeállítások") {
                        openURL(url)
                        message = nil
                        scanning = true
                    }
                }
            } message: { Text(message ?? "") }
        }
    }

    private func finishScan(_ text: String) {
        pendingText = text
        torchOn = false
        scanning = false
        processScan()
    }

    /// A picked still is decoded in the background, then handed to the same
    /// path a live scan takes — so one set of safety checks covers both.
    private func decodePickedCode() {
        guard let item = pickedCode else { return }
        pickedCode = nil
        Task { @MainActor in
            guard let data = try? await item.loadTransferable(type: Data.self),
                  let value = QRImageDecoder.decode(data) else {
                message = "Ezen a képen nem találtunk egyetlen egyértelmű QR-kódot sem."
                scanning = false
                return
            }
            finishScan(value)
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

/// Carousel card metrics: wide enough to read, narrow enough that the next card
/// peeks in and signals the row scrolls.
private enum PortalMetrics {
    static let cardWidth: CGFloat = 300
    static let cardHeight: CGFloat = 300
    static let courseWidth: CGFloat = 296
    static let courseHeight: CGFloat = 330
}

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

private struct BusinessTopic: Identifiable {
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
    let modules: [LearningModule]

    var lessons: [CourseLesson] { modules.flatMap(\.lessons) }
}

private struct LearningModule: Identifiable {
    let id: String
    let title: String
    let lessons: [CourseLesson]
}

private struct CourseLesson: Identifiable {
    let id: String
    let title: String
    let summary: String
    /// When nil, the topic video is divided into stable lesson chapters.
    let videoURL: String?
    let resourceTitle: String
    let resourceURL: String

    init(
        id: String,
        title: String,
        summary: String,
        videoURL: String? = nil,
        resourceTitle: String,
        resourceURL: String
    ) {
        self.id = id
        self.title = title
        self.summary = summary
        self.videoURL = videoURL
        self.resourceTitle = resourceTitle
        self.resourceURL = resourceURL
    }
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

private struct ToolkitQuestion: Identifiable {
    let id: String
    let title: String
    let recommendationGuideID: String
}

private let portalFeatures = [
    PortalFeature(id: "vosz", title: "VOSZ", description: "A Vállalkozók és Munkáltatók Országos Szövetségének hírei, videói és tanácsadói szolgáltatásai — egy helyen, magyarul.", eyebrow: "PARTNERI FORRÁSOK", icon: "briefcase.fill", destination: .vosz),
    PortalFeature(id: "education", title: "Vállalkozói Edukáció", description: "Hat témakör, témánként két modul és négy magyar videólecke. A haladás csak tényleges megtekintés után frissül.", eyebrow: "6 TÉMA · 12 MODUL · 24 LECKE", icon: "graduationcap.fill", destination: .education),
    PortalFeature(id: "help", title: "Digitális segítség", description: "Nézd meg, hogyan indul majd egy videós konzultáció digitalizációs szakértővel, mielőtt időpontot foglalnál.", eyebrow: "BEMUTATÓ MÓD", icon: "video.fill", destination: .help),
    PortalFeature(id: "toolkit", title: "Vállalkozói eszköztár", description: "Négy kérdés a digitális felkészültségedről, és egy konkrét következő lépés, amit még ma elkezdhetsz.", eyebrow: "INTERAKTÍV", icon: "checklist", destination: .toolkit)
]

private let businessResources = [
    BusinessResource(id: "vosz-youtube", title: "VOSZ videók", description: "Vállalkozói hírek, interjúk és gyakorlati videók.", url: "https://youtube.com/@vosz.?si=k2EmMlI8Q5ttlPZC", icon: "play.rectangle.fill"),
    BusinessResource(id: "vosz", title: "VOSZ információk", description: "Érdekképviselet, tanácsadás, programok és aktuális hírek.", url: "https://www.vosz.hu/hu", icon: "briefcase.fill"),
    BusinessResource(id: "voszport", title: "VOSZPort", description: "Digitális ügyintézési és tudásmegosztási felület.", url: "https://voszport.com/", icon: "globe.europe.africa.fill")
]

private let toolkitQuestions = [
    ToolkitQuestion(id: "mail", title: "Van külön céges e-mail-címed?", recommendationGuideID: "office"),
    ToolkitQuestion(id: "mfa", title: "Bekapcsoltad a kétlépcsős belépést?", recommendationGuideID: "security"),
    ToolkitQuestion(id: "backup", title: "Van rendszeres, visszaállítással tesztelt mentésed?", recommendationGuideID: "security"),
    ToolkitQuestion(id: "presence", title: "Naprakész a Google Cégprofilod és az online elérhetőséged?", recommendationGuideID: "sales")
]

private let businessTopics = [
    BusinessTopic(
        id: "ai", title: "AI a mindennapi vállalkozásban", category: "Mesterséges intelligencia",
        description: "Használható promptok, automatizálási ötletek és felelős AI-használat. Megtanulod, mely feladatokat érdemes AI-ra bízni, és melyeket soha.",
        duration: "Magyar videó", level: "Kezdő", icon: "sparkles",
        videoTitle: "ChatGPT képzés – Promptolási technikák", videoSource: "VOSZ",
        videoURL: "https://www.youtube.com/watch?v=uPYOrXxTxJI",
        modules: [
            LearningModule(id: "ai-foundations", title: "Lehetőségek és jó promptok", lessons: [
                CourseLesson(id: "ai-value", title: "Hol teremt értéket az AI?", summary: "Gyűjts össze három ismétlődő szöveges feladatot, és válaszd ki azt, amelyiknél a legkisebb a hibakockázat.", resourceTitle: "VOSZ AI-videósorozat", resourceURL: "https://www.youtube.com/results?search_query=VOSZ+ChatGPT+k%C3%A9pz%C3%A9s"),
                CourseLesson(id: "ai-prompt", title: "Jó prompt 5 lépésben", summary: "Add meg a szerepet, a célt, a bemenetet, a korlátokat és a kívánt kimeneti formátumot.", videoURL: "https://www.youtube.com/watch?v=uPYOrXxTxJI", resourceTitle: "Promptolási videó megnyitása", resourceURL: "https://www.youtube.com/watch?v=uPYOrXxTxJI")
            ]),
            LearningModule(id: "ai-practice", title: "Napi munka és felelős használat", lessons: [
                CourseLesson(id: "ai-mail", title: "Ajánlat és e-mail gyorsítása", summary: "Készíts ellenőrzött sablont ajánlatra, utánkövetésre és ügyfélválaszra; érzékeny adatot ne másolj be.", resourceTitle: "Microsoft Copilot vállalkozásoknak", resourceURL: "https://www.microsoft.com/hu-hu/microsoft-365/business/copilot-for-microsoft-365"),
                CourseLesson(id: "ai-privacy", title: "Ellenőrzés és adatvédelem", summary: "Minden AI-kimenetet ember ellenőrizzen, és legyen belső szabály arra, milyen adat kerülhet a rendszerbe.", resourceTitle: "NAIH tájékoztatók", resourceURL: "https://www.naih.hu/")
            ])
        ]
    ),
    BusinessTopic(
        id: "m365", title: "Microsoft 365 kisvállalkozásoknak", category: "Digitális munka",
        description: "Teams, Outlook, OneDrive és SharePoint egyetlen, biztonságos rendszerben. Fiókok, jogosultságok, közös fájlkezelés és automatizmusok.",
        duration: "Magyar videók", level: "Kezdő", icon: "cloud.fill",
        videoTitle: "Microsoft 365 bevezetés és csoportok", videoSource: "Sämling Üzleti Oktatási Központ",
        videoURL: "https://www.youtube.com/watch?v=py9fGXyBZcE",
        modules: [
            LearningModule(id: "m365-access", title: "Fiókok és fájlok", lessons: [
                CourseLesson(id: "m365-accounts", title: "Fiókok és jogosultságok", summary: "Minden munkatársnak külön fiók, szerepkör szerinti hozzáférés és bekapcsolt többtényezős védelem kell.", resourceTitle: "Microsoft 365 Vállalati verzió", resourceURL: "https://www.microsoft.com/hu-hu/microsoft-365/business"),
                CourseLesson(id: "m365-files", title: "Közös fájlkezelés OneDrive-val", summary: "Alakíts ki közös mappaszerkezetet, tulajdonost és visszaállítási rendet; ne e-mailben küldözgess fájlmásolatokat.", resourceTitle: "OneDrive magyar súgó", resourceURL: "https://support.microsoft.com/hu-hu/onedrive")
            ]),
            LearningModule(id: "m365-collaboration", title: "Együttműködés és automatizmusok", lessons: [
                CourseLesson(id: "m365-teams", title: "Teams-együttműködés", summary: "Hozz létre ügyfél- vagy projektcsatornákat, és rögzítsd, melyik információ hol található.", resourceTitle: "Teams magyar súgó", resourceURL: "https://support.microsoft.com/hu-hu/teams"),
                CourseLesson(id: "m365-automation", title: "Naptár és automatizmusok", summary: "Használj közös naptárt, foglalási oldalt és egyetlen jóváhagyott automatizmust egy ismétlődő folyamathoz.", resourceTitle: "Power Automate", resourceURL: "https://www.microsoft.com/hu-hu/power-platform/products/power-automate")
            ])
        ]
    ),
    BusinessTopic(
        id: "basics", title: "Vállalkozói alapismeretek", category: "Cégépítés",
        description: "Üzleti modell, célpiac, árazás és az első 90 nap terve. Az indítás kötelező lépéseitől a heti mérőszámokig.",
        duration: "Magyar videó", level: "Kezdő", icon: "storefront.fill",
        videoTitle: "Az egyéni vállalkozás és indítása", videoSource: "Magyar nyelvű oktatóvideó",
        videoURL: "https://www.youtube.com/watch?v=d2tyNSnyv1Q",
        modules: [
            LearningModule(id: "basics-start", title: "Üzleti alapok és indulás", lessons: [
                CourseLesson(id: "basics-model", title: "Üzleti modell egy oldalon", summary: "Írd le egy mondatban a vevőt, a problémáját, az ajánlatodat, az értékesítési csatornát és a bevételi módot.", resourceTitle: "VOSZ vállalkozói információk", resourceURL: "https://www.vosz.hu/hu"),
                CourseLesson(id: "basics-launch", title: "Indítás és kötelező lépések", summary: "Ellenőrizd a tevékenységet, adózást, képesítési feltételt, kamarai bejelentést és számlázást.", resourceTitle: "NAV: egyéni vállalkozás indítása", resourceURL: "https://nav.gov.hu/Elethelyzetek-adozasa/vallalkozas/Egyeni-vallalkozas-inditasa")
            ]),
            LearningModule(id: "basics-plan-module", title: "Árazás és 90 napos terv", lessons: [
                CourseLesson(id: "basics-pricing", title: "Ideális ügyfél és árazás", summary: "Határozz meg egy konkrét célcsoportot, eredményt, költségszintet és minimális vállalható árat.", resourceTitle: "MKIK Mentorprogram", resourceURL: "https://vallalkozztudatosan.mkik.hu/"),
                CourseLesson(id: "basics-plan", title: "90 napos akcióterv", summary: "Bonts három havi célra, heti mérőszámokra és minden héten egy lezárandó ügyfélszerzési feladatra.", resourceTitle: "Vállalkozz digitálisan", resourceURL: "https://vallalkozzdigitalisan.mkik.hu/")
            ])
        ]
    ),
    BusinessTopic(
        id: "security", title: "Kiberbiztonság emberi nyelven", category: "Biztonság",
        description: "Fiókvédelem, mentés, adathalászat és egy egyszerű incidens-terv. A legnagyobb védelem a legkisebb munkáért.",
        duration: "Magyar videó", level: "Kezdő", icon: "lock.shield.fill",
        videoTitle: "KiberPajzs – digitális biztonság", videoSource: "Pénziránytű / KiberPajzs",
        videoURL: "https://www.youtube.com/watch?v=2LqpB_03Jt0",
        modules: [
            LearningModule(id: "security-protection", title: "Fiókvédelem és mentés", lessons: [
                CourseLesson(id: "security-mfa", title: "Többlépcsős belépés", summary: "Kapcsold be először az e-mail-, banki, közösségi és adminisztrációs fiókoknál.", resourceTitle: "KiberPajzs biztonsági tippek", resourceURL: "https://kiberpajzs.hu/hasznos-tippeket-olvasnek"),
                CourseLesson(id: "security-backup", title: "Eszközök és mentés", summary: "Automatikus frissítés, képernyőzár és legalább egy külön helyen tárolt, visszaállítással is tesztelt mentés kell.", resourceTitle: "NKI tudásbázis", resourceURL: "https://nki.gov.hu/")
            ]),
            LearningModule(id: "security-response", title: "Csalások és incidenskezelés", lessons: [
                CourseLesson(id: "security-phishing", title: "Adathalászat felismerése", summary: "Sürgetésnél állj meg, külön csatornán ellenőrizd a feladót, és ne a levélből nyisd meg a belépési oldalt.", videoURL: "https://www.youtube.com/watch?v=2LqpB_03Jt0", resourceTitle: "KiberPajzs videó", resourceURL: "https://www.youtube.com/watch?v=2LqpB_03Jt0"),
                CourseLesson(id: "security-incident", title: "Incidens-terv", summary: "Írd le, kit kell hívni, hogyan zárod a fiókokat, hol van a mentés, és hogyan értesíted az érintetteket.", resourceTitle: "NKI incidensbejelentés", resourceURL: "https://nki.gov.hu/intezet/tartalom/incidens-bejelentes/")
            ])
        ]
    ),
    BusinessTopic(
        id: "marketing", title: "Online jelenlét és ügyfélszerzés", category: "Marketing",
        description: "Pozicionálás, Google Cégprofil, négyhetes tartalomterv és mérés. Ügyfélszerzés követhető lépésekben, nem követőszámban.",
        duration: "Magyar videók", level: "Középhaladó", icon: "megaphone.fill",
        videoTitle: "Hogyan kerülhetsz fel a Google Térképre?", videoSource: "Jobbágy András",
        videoURL: "https://www.youtube.com/watch?v=-4qATDuCWgU",
        modules: [
            LearningModule(id: "marketing-presence", title: "Pozicionálás és megtalálhatóság", lessons: [
                CourseLesson(id: "marketing-position", title: "Pozicionálási mondat", summary: "Ne szolgáltatást sorolj: mondd meg, kinek, milyen eredményt és mitől más módon adsz.", resourceTitle: "VOSZ videók", resourceURL: "https://www.youtube.com/@vosz."),
                CourseLesson(id: "marketing-profile", title: "Bizalmat építő Google Cégprofil", summary: "Tölts ki minden adatot, adj képeket, szolgáltatásokat és rendszeresen válaszolj az értékelésekre.", resourceTitle: "Google Cégprofil hozzáadása", resourceURL: "https://support.google.com/business/answer/2911778?hl=hu")
            ]),
            LearningModule(id: "marketing-growth", title: "Tartalom és mérés", lessons: [
                CourseLesson(id: "marketing-content", title: "4 hetes tartalomterv", summary: "Hetente mutass problémát, megoldást, bizonyítékot és konkrét következő lépést.", resourceTitle: "Meta Business Suite", resourceURL: "https://business.facebook.com/"),
                CourseLesson(id: "marketing-measure", title: "Mérés és javítás", summary: "Mérd a forrást, érdeklődést, ajánlatot és vásárlást; a követőszám önmagában nem üzleti eredmény.", resourceTitle: "Google Analytics", resourceURL: "https://analytics.google.com/")
            ])
        ]
    ),
    BusinessTopic(
        id: "finance", title: "Pénzügyi tudatosság alapjai", category: "Pénzügy",
        description: "Cash-flow, költségek, számlázás és együttműködés a könyvelővel. Hogy a bevétel és a nyereség ne csússzon össze.",
        duration: "Magyar videó", level: "Kezdő", icon: "building.columns.fill",
        videoTitle: "NAV Online Számlázó Program bemutatása", videoSource: "Nemzeti Adó- és Vámhivatal",
        videoURL: "https://www.youtube.com/watch?v=U5s2bXrgnHc",
        modules: [
            LearningModule(id: "finance-basics", title: "Pénzügyi alapok és cash-flow", lessons: [
                CourseLesson(id: "finance-profit", title: "Bevétel, költség és nyereség", summary: "Külön kezeld a beérkezett pénzt, az áfát, a fizetendő adót, a költségeket és a tulajdonosi kivétet.", resourceTitle: "NAV vállalkozói élethelyzetek", resourceURL: "https://nav.gov.hu/Elethelyzetek-adozasa/vallalkozas"),
                CourseLesson(id: "finance-cashflow", title: "13 hetes cash-flow", summary: "Hetente vezesd a várható be- és kifizetéseket, és jelöld a bizonytalan tételeket.", resourceTitle: "Pénziránytű", resourceURL: "https://penziranytu.hu/")
            ]),
            LearningModule(id: "finance-operations", title: "Számlázás és havi zárás", lessons: [
                CourseLesson(id: "finance-invoice", title: "Számlázás és adatszolgáltatás", summary: "Ellenőrizd a NAV-kapcsolatot, a számlaadatokat és azt, hogy ki figyeli a hibás adatszolgáltatást.", resourceTitle: "NAV Online Számla", resourceURL: "https://onlineszamla.nav.gov.hu/"),
                CourseLesson(id: "finance-close", title: "Havi zárás a könyvelővel", summary: "Legyen fix határidő a bizonylatokra, kintlévőségekre, adókra és a következő három hónap pénzigényére.", resourceTitle: "NAV ONYA", resourceURL: "https://onya.nav.gov.hu/")
            ])
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

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.lg) {
                        VStack(alignment: .leading, spacing: VizitSpace.xs) {
                            VizitLargeTitle("Vállalkozói Portál")
                            Text("Tanulás, hiteles források és digitális segítség a vállalkozásod következő lépéséhez.")
                                .font(VizitFont.body).foregroundStyle(VizitColor.textSecondary)
                        }
                        // The four areas sit side by side so each one can be large
                        // enough to read at a glance. The row bleeds to the screen
                        // edges so a partially visible card hints that it scrolls.
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: VizitSpace.sm) {
                                ForEach(portalFeatures) { feature in
                                    NavigationLink { destination(for: feature.destination) } label: { PortalFeatureCard(feature: feature) }
                                        .buttonStyle(.plain)
                                        .accessibilityLabel(feature.title)
                                        .accessibilityIdentifier("portal.\(feature.id)")
                                }
                            }
                            .padding(.horizontal, VizitSpace.md)
                        }
                        .padding(.horizontal, -VizitSpace.md)
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
            Text(feature.description).font(VizitFont.body).foregroundStyle(VizitColor.textSecondary).lineLimit(6)
            Spacer(minLength: 0)
            Image(systemName: "arrow.right").font(.system(size: 15, weight: .semibold)).foregroundStyle(VizitColor.primary).frame(maxWidth: .infinity, alignment: .trailing)
        }
        .padding(VizitSpace.md)
        .frame(width: PortalMetrics.cardWidth, height: PortalMetrics.cardHeight, alignment: .topLeading)
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
    @AppStorage("education.completedLessonIDs") private var completedLessonIDs = ""
    private let categories = ["Mind", "AI", "Digitális munka", "Cégépítés", "Biztonság", "Marketing", "Pénzügy"]
    private var completed: Set<String> { Set(completedLessonIDs.split(separator: "|").map(String.init)) }
    private var totalLessonCount: Int { businessTopics.reduce(0) { $0 + $1.lessons.count } }
    private var completedLessonCount: Int {
        completed.intersection(Set(businessTopics.flatMap { $0.lessons.map(\.id) })).count
    }
    private var filtered: [BusinessTopic] {
        businessTopics.filter { selectedCategory == "Mind" || (selectedCategory == "AI" && $0.id == "ai") || $0.category == selectedCategory }
    }
    var body: some View {
        PortalScroll(title: "Vállalkozói Edukáció", subtitle: "Rövid, gyakorlatias tananyagok, saját tempóban.") {
            VizitPanel {
                VStack(alignment: .leading, spacing: VizitSpace.xs) {
                    HStack {
                        Text("A haladásod").font(VizitFont.label).foregroundStyle(VizitColor.textPrimary)
                        Spacer()
                        Text("\(completedLessonCount) / \(totalLessonCount) lecke")
                            .font(VizitFont.caption.monospacedDigit())
                            .foregroundStyle(VizitColor.primary)
                    }
                    ProgressView(value: Double(completedLessonCount), total: Double(max(totalLessonCount, 1)))
                        .tint(VizitColor.primary)
                        .accessibilityLabel("Kurzusteljesítés")
                        .accessibilityValue("\(completedLessonCount) a \(totalLessonCount) leckéből")
                }
            }
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
            // Topics run side by side, so the whole catalogue is reachable by
            // swiping instead of scrolling past six full-width rows.
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: VizitSpace.sm) {
                    ForEach(filtered) { course in
                        NavigationLink { CourseDetailScreen(course: course) } label: {
                            CourseCard(
                                course: course,
                                completedCount: completed.intersection(Set(course.lessons.map(\.id))).count
                            )
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("portal.course.\(course.id)")
                    }
                }
                .padding(.horizontal, VizitSpace.md)
            }
            .padding(.horizontal, -VizitSpace.md)
        }
    }
}

private struct CourseCard: View {
    let course: BusinessTopic
    let completedCount: Int
    var body: some View {
        VStack(alignment: .leading, spacing: VizitSpace.xs) {
            VizitIconChip(systemImage: course.icon, tint: VizitColor.primary, background: VizitColor.primarySubtle, size: 56)
            Text(course.category.uppercased()).vizitOverline().foregroundStyle(VizitColor.primary)
            Text(course.title).font(VizitFont.h3).foregroundStyle(VizitColor.textPrimary).lineLimit(2)
            Text(course.description).font(VizitFont.body).foregroundStyle(VizitColor.textSecondary).lineLimit(5)
            Spacer(minLength: 0)
            ProgressView(value: Double(completedCount), total: Double(max(course.lessons.count, 1)))
                .tint(VizitColor.primary)
            Text("\(course.level) · \(course.modules.count) modul · \(completedCount)/\(course.lessons.count) lecke")
                .font(VizitFont.caption)
                .foregroundStyle(VizitColor.textMuted)
        }
        .padding(VizitSpace.md)
        .frame(width: PortalMetrics.courseWidth, height: PortalMetrics.courseHeight, alignment: .topLeading)
        .background(VizitColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
        .overlay { RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous).stroke(VizitColor.border, lineWidth: 1) }
    }
}

/// The course player, laid out the way a video-course app is: the video owns the
/// top of the screen, and everything below it is a tabbed index of the course.
/// The screen keeps its own dark chrome regardless of the app theme, because the
/// video is the surface everything else has to sit against.
private enum CoursePlayer {
    static let canvas = Color(red: 0.043, green: 0.043, blue: 0.059)
    static let surface = Color(red: 0.086, green: 0.086, blue: 0.110)
    static let divider = Color(red: 0.149, green: 0.149, blue: 0.180)
    static let text = Color.white
    static let secondary = Color(red: 0.655, green: 0.655, blue: 0.698)
    static let muted = Color(red: 0.431, green: 0.431, blue: 0.471)
    static let accent = Color(red: 0.306, green: 0.608, blue: 1.0)
}

private enum CourseTab: String, CaseIterable {
    case lessons, resources, about
    var title: String {
        switch self {
        case .lessons: return "Leckék"
        case .resources: return "Anyagok"
        case .about: return "A kurzusról"
        }
    }
}

private struct CourseDetailScreen: View {
    let course: BusinessTopic
    @Environment(\.openURL) private var openURL
    @Environment(\.dismiss) private var dismiss
    @AppStorage("education.completedLessonIDs") private var completedLessonIDs = ""
    @State private var selectedLessonID: String?
    @State private var tab: CourseTab = .lessons
    @State private var videoReady = false
    @State private var videoUnavailable = false

    private var lessons: [CourseLesson] { course.lessons }
    private var selectedLesson: CourseLesson { lessons.first { $0.id == selectedLessonID } ?? lessons[0] }
    private var completed: Set<String> { Set(completedLessonIDs.split(separator: "|").map(String.init)) }
    private var completedLessonCount: Int { completed.intersection(Set(lessons.map(\.id))).count }
    private func markLessonComplete(_ lessonID: String) {
        var next = completed
        next.insert(lessonID)
        completedLessonIDs = next.sorted().joined(separator: "|")
    }
    private func videoURL(for lesson: CourseLesson) -> String {
        lesson.videoURL ?? course.videoURL
    }
    private func videoChapter(for lesson: CourseLesson) -> (index: Int, count: Int) {
        if lesson.videoURL != nil { return (0, 1) }
        return (lessons.firstIndex(where: { $0.id == lesson.id }) ?? 0, max(lessons.count, 1))
    }

    var body: some View {
        VStack(spacing: 0) {
            stage
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0, pinnedViews: [.sectionHeaders]) {
                    Section {
                        tabContent.padding(.bottom, VizitSpace.xxl)
                    } header: {
                        VStack(alignment: .leading, spacing: 0) {
                            courseHeader
                            tabBar
                        }
                        .background(CoursePlayer.canvas)
                    }
                }
            }
        }
        .background(CoursePlayer.canvas.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear { if selectedLessonID == nil { selectedLessonID = lessons.first?.id } }
        .onChange(of: selectedLessonID) { _ in
            videoReady = false
            videoUnavailable = false
        }
    }

    /// The video, full width and edge to edge, with the only way back overlaid on it.
    private var stage: some View {
        let lesson = selectedLesson
        let chapter = videoChapter(for: lesson)
        return ZStack {
            YouTubeLessonPlayer(
                url: videoURL(for: lesson),
                chapterIndex: chapter.index,
                chapterCount: chapter.count,
                onReady: { videoReady = true },
                onCompleted: { markLessonComplete(lesson.id) },
                onError: { videoUnavailable = true }
            )
            .id(lesson.id)
            .opacity(videoUnavailable ? 0 : 1)

            if !videoReady && !videoUnavailable {
                VStack(spacing: VizitSpace.sm) {
                    ProgressView().tint(.white)
                    Text("Magyar videólecke betöltése…")
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(Color.white.opacity(0.74))
                }
                .allowsHitTesting(false)
            }

            if videoUnavailable {
                VStack(spacing: VizitSpace.sm) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundStyle(CoursePlayer.accent)
                    Text("A videó most nem érhető el")
                        .font(VizitFont.h3)
                        .foregroundStyle(CoursePlayer.text)
                    Text("A lecke és a haladás megmarad. Próbáld újra, vagy nyisd meg a magyar videót a böngészőben.")
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(CoursePlayer.secondary)
                        .multilineTextAlignment(.center)
                    Button("Megnyitás böngészőben") {
                        guard let url = SafeLink.https(videoURL(for: lesson)) else { return }
                        openURL(url)
                    }
                    .font(VizitFont.label)
                    .foregroundStyle(CoursePlayer.accent)
                    .frame(minHeight: VizitMetrics.minTouchTarget)
                }
                .padding(VizitSpace.xl)
            }
        }
            .frame(maxWidth: .infinity)
            .aspectRatio(16 / 9, contentMode: .fit)
            .background(Color.black)
            .overlay(alignment: .topLeading) {
                Button { dismiss() } label: {
                    Image(systemName: "chevron.down")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 44, height: 44)
                        .background(Color.black.opacity(0.45), in: Circle())
                }
                .buttonStyle(.plain)
                .padding(VizitSpace.xs)
                .accessibilityLabel("Vissza a kurzusokhoz")
            }
    }

    private var courseHeader: some View {
        VStack(alignment: .leading, spacing: VizitSpace.xxs) {
            Text(course.title)
                .font(VizitFont.h2)
                .foregroundStyle(CoursePlayer.text)
                .fixedSize(horizontal: false, vertical: true)
            Text(course.videoSource)
                .font(VizitFont.bodySmall)
                .foregroundStyle(CoursePlayer.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, VizitSpace.md)
        .padding(.top, VizitSpace.md)
        .padding(.bottom, VizitSpace.sm)
    }

    private var tabBar: some View {
        HStack(spacing: 0) {
            ForEach(CourseTab.allCases, id: \.self) { item in
                Button { tab = item } label: {
                    VStack(spacing: VizitSpace.xs) {
                        Text(item.title)
                            .font(item == tab ? VizitFont.bodyStrong : VizitFont.body)
                            .foregroundStyle(item == tab ? CoursePlayer.text : CoursePlayer.secondary)
                        Rectangle()
                            .fill(item == tab ? CoursePlayer.accent : Color.clear)
                            .frame(height: 3)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(minHeight: VizitMetrics.minTouchTarget, alignment: .bottom)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(item == tab ? [.isSelected] : [])
            }
        }
        .padding(.horizontal, VizitSpace.md)
        .overlay(alignment: .bottom) { Rectangle().fill(CoursePlayer.divider).frame(height: 1) }
    }

    @ViewBuilder
    private var tabContent: some View {
        switch tab {
        case .lessons: lessonList
        case .resources: resourceList
        case .about: aboutPane
        }
    }

    private var lessonList: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(course.modules.enumerated()), id: \.element.id) { moduleIndex, module in
                sectionHeader(moduleIndex: moduleIndex, module: module)
                ForEach(Array(module.lessons.enumerated()), id: \.element.id) { lessonIndex, lesson in
                    lessonRow(lesson: lesson, number: "\(moduleIndex + 1).\(lessonIndex + 1)")
                }
            }
        }
    }

    private func sectionHeader(moduleIndex: Int, module: LearningModule) -> some View {
        let done = module.lessons.filter { completed.contains($0.id) }.count
        return HStack(spacing: VizitSpace.sm) {
            Text("\(moduleIndex + 1). modul · \(module.title)")
                .font(VizitFont.label)
                .foregroundStyle(CoursePlayer.secondary)
            Spacer(minLength: VizitSpace.xs)
            Text("\(done)/\(module.lessons.count)")
                .font(VizitFont.caption)
                .foregroundStyle(done == module.lessons.count ? CoursePlayer.accent : CoursePlayer.muted)
        }
        .padding(.horizontal, VizitSpace.md)
        .padding(.top, VizitSpace.lg)
        .padding(.bottom, VizitSpace.xs)
    }

    private func lessonRow(lesson: CourseLesson, number: String) -> some View {
        let isDone = completed.contains(lesson.id)
        let isCurrent = lesson.id == selectedLesson.id
        return Button { selectedLessonID = lesson.id } label: {
            HStack(alignment: .top, spacing: VizitSpace.sm) {
                Text(number)
                    .font(VizitFont.bodySmall.monospacedDigit())
                    .foregroundStyle(CoursePlayer.muted)
                    .frame(width: 30, alignment: .leading)
                VStack(alignment: .leading, spacing: 4) {
                    Text(lesson.title)
                        .font(isCurrent ? VizitFont.bodyStrong : VizitFont.body)
                        .foregroundStyle(CoursePlayer.text)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(isCurrent ? "Videólecke · most játszik" : isDone ? "Videólecke · elvégezve" : "Videólecke")
                        .font(VizitFont.caption)
                        .foregroundStyle(isCurrent ? CoursePlayer.accent : CoursePlayer.muted)
                }
                Spacer(minLength: VizitSpace.xs)
                Image(systemName: isDone ? "checkmark.circle.fill" : isCurrent ? "speaker.wave.2.fill" : "play.circle")
                    .font(.system(size: 20))
                    .foregroundStyle(isDone ? CoursePlayer.accent : isCurrent ? CoursePlayer.accent : CoursePlayer.muted)
            }
            .padding(.horizontal, VizitSpace.md)
            .padding(.vertical, VizitSpace.sm)
            .frame(maxWidth: .infinity, minHeight: VizitMetrics.minTouchTarget, alignment: .leading)
            .background(isCurrent ? CoursePlayer.surface : Color.clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(number). \(lesson.title)")
    }

    private var resourceList: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("A leckékhez tartozó hiteles külső anyagok. A telefon böngészőjében nyílnak meg.")
                .font(VizitFont.bodySmall)
                .foregroundStyle(CoursePlayer.secondary)
                .padding(.horizontal, VizitSpace.md)
                .padding(.vertical, VizitSpace.md)
            ForEach(lessons) { lesson in
                Button {
                    guard let url = SafeLink.https(lesson.resourceURL) else { return }
                    openURL(url)
                } label: {
                    HStack(spacing: VizitSpace.sm) {
                        Image(systemName: "doc.text")
                            .font(.system(size: 18))
                            .foregroundStyle(CoursePlayer.accent)
                            .frame(width: 30)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(lesson.resourceTitle)
                                .font(VizitFont.body)
                                .foregroundStyle(CoursePlayer.text)
                                .multilineTextAlignment(.leading)
                            Text(lesson.title)
                                .font(VizitFont.caption)
                                .foregroundStyle(CoursePlayer.muted)
                        }
                        Spacer(minLength: VizitSpace.xs)
                        Image(systemName: "arrow.up.forward")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(CoursePlayer.muted)
                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.vertical, VizitSpace.sm)
                    .frame(maxWidth: .infinity, minHeight: VizitMetrics.minTouchTarget, alignment: .leading)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var aboutPane: some View {
        VStack(alignment: .leading, spacing: VizitSpace.md) {
            Text(course.description)
                .font(VizitFont.body)
                .foregroundStyle(CoursePlayer.secondary)
                .fixedSize(horizontal: false, vertical: true)

            VStack(alignment: .leading, spacing: VizitSpace.sm) {
                factRow("Szint", course.level)
                factRow("Terjedelem", "\(course.modules.count) modul · \(lessons.count) lecke")
                factRow("Nyelv", course.duration)
                factRow("Forrás", course.videoSource)
                factRow("Haladásod", "\(completedLessonCount) / \(lessons.count) lecke")
            }

            Text("A kész állapotot az alkalmazás akkor rögzíti, ha a videó legalább 90%-a ténylegesen lejátszódott. Kézzel nem módosítható, és ezen a készüléken tárolódik.")
                .font(VizitFont.caption)
                .foregroundStyle(CoursePlayer.muted)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, VizitSpace.md)
        .padding(.top, VizitSpace.md)
    }

    private func factRow(_ label: String, _ value: String) -> some View {
        HStack(alignment: .top, spacing: VizitSpace.sm) {
            Text(label)
                .font(VizitFont.bodySmall)
                .foregroundStyle(CoursePlayer.muted)
                .frame(width: 96, alignment: .leading)
            Text(value)
                .font(VizitFont.bodySmall)
                .foregroundStyle(CoursePlayer.text)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
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
    let chapterIndex: Int
    let chapterCount: Int
    let onReady: () -> Void
    let onCompleted: () -> Void
    let onError: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onReady: onReady, onCompleted: onCompleted, onError: onError)
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = [.video, .audio]
        configuration.websiteDataStore = .nonPersistent()
        configuration.userContentController.add(context.coordinator, name: "lessonCompleted")
        configuration.userContentController.add(context.coordinator, name: "lessonReady")
        configuration.userContentController.add(context.coordinator, name: "lessonError")
        let view = WKWebView(frame: .zero, configuration: configuration)
        view.isOpaque = false
        view.backgroundColor = .black
        view.scrollView.isScrollEnabled = false
        return view
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.onReady = onReady
        context.coordinator.onCompleted = onCompleted
        context.coordinator.onError = onError
        guard let videoID = YouTubeVideoID.from(url) else {
            DispatchQueue.main.async { onError() }
            return
        }
        let signature = "\(videoID):\(chapterIndex):\(chapterCount)"
        guard context.coordinator.signature != signature else { return }
        context.coordinator.signature = signature
        webView.loadHTMLString(
            Self.html(videoID: videoID, chapterIndex: chapterIndex, chapterCount: chapterCount),
            // A real HTTPS document origin is required by the YouTube iframe
            // API. Without it the player may reject WKWebView as an unidentified
            // client with error 153 even though the video itself is available.
            baseURL: URL(string: "https://e-nevjegy.vercel.app")
        )
    }

    static func dismantleUIView(_ webView: WKWebView, coordinator: Coordinator) {
        webView.configuration.userContentController.removeScriptMessageHandler(forName: "lessonCompleted")
        webView.configuration.userContentController.removeScriptMessageHandler(forName: "lessonReady")
        webView.configuration.userContentController.removeScriptMessageHandler(forName: "lessonError")
        webView.stopLoading()
    }

    private static func html(videoID: String, chapterIndex: Int, chapterCount: Int) -> String {
        let safeID = videoID.replacingOccurrences(of: "'", with: "")
        let safeCount = max(chapterCount, 1)
        let safeIndex = min(max(chapterIndex, 0), safeCount - 1)
        return """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
        <style>html,body,#player{margin:0;width:100%;height:100%;background:#000;overflow:hidden}</style></head>
        <body><div id="player"></div><script src="https://www.youtube.com/iframe_api"></script><script>
        var player, tick=0, lastTime=0, segmentStart=0, segmentEnd=0, sent=false;
        const watchedBuckets=new Set(), bucketCount=100;
        const chapterIndex=\(safeIndex), chapterCount=\(safeCount);
        function onYouTubeIframeAPIReady(){
          player=new YT.Player('player',{videoId:'\(safeID)',host:'https://www.youtube-nocookie.com',playerVars:{playsinline:1,rel:0,modestbranding:1,origin:'https://e-nevjegy.vercel.app'},events:{onReady:onReady,onStateChange:onState,onError:onError}});
        }
        function onReady(){
          const duration=player.getDuration();
          segmentStart=duration*(chapterIndex/chapterCount);
          segmentEnd=duration*((chapterIndex+1)/chapterCount);
          if(segmentStart>0.5){player.seekTo(segmentStart,true);}
          window.webkit.messageHandlers.lessonReady.postMessage('ready');
        }
        function stopTick(){if(tick){clearInterval(tick);tick=0;}}
        function finishIfWatched(){
          if(!sent && watchedBuckets.size/bucketCount>=0.9){sent=true;window.webkit.messageHandlers.lessonCompleted.postMessage('done');}
        }
        function recordRange(from,to){
          const length=Math.max(1,segmentEnd-segmentStart);
          const first=Math.max(0,Math.floor(((from-segmentStart)/length)*bucketCount));
          const last=Math.min(bucketCount-1,Math.floor(((to-segmentStart)/length)*bucketCount));
          for(let index=first;index<=last;index++){watchedBuckets.add(index);}
        }
        function track(){
          const current=player.getCurrentTime();
          const delta=current-lastTime;
          if(delta>0 && delta<1.5){recordRange(lastTime,current);}
          lastTime=current;
          if(segmentEnd>0 && current>=segmentEnd-0.25){
            stopTick(); player.pauseVideo(); finishIfWatched();
            if(!sent){player.seekTo(segmentStart,true);}
          }
        }
        function onState(e){
          if(e.data===YT.PlayerState.PLAYING && !tick){lastTime=player.getCurrentTime();tick=setInterval(track,500);}
          if(e.data!==YT.PlayerState.PLAYING){stopTick();}
          if(e.data===YT.PlayerState.ENDED){finishIfWatched();}
        }
        function onError(){stopTick();window.webkit.messageHandlers.lessonError.postMessage('error');}
        </script></body></html>
        """
    }

    final class Coordinator: NSObject, WKScriptMessageHandler {
        var signature: String?
        var onReady: () -> Void
        var onCompleted: () -> Void
        var onError: () -> Void
        init(onReady: @escaping () -> Void, onCompleted: @escaping () -> Void, onError: @escaping () -> Void) {
            self.onReady = onReady
            self.onCompleted = onCompleted
            self.onError = onError
        }
        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            DispatchQueue.main.async {
                switch message.name {
                case "lessonReady": self.onReady()
                case "lessonCompleted": self.onCompleted()
                case "lessonError": self.onError()
                default: break
                }
            }
        }
    }
}

private struct DigitalHelpScreen: View {
    @State private var inCall = false
    @State private var microphone = true
    @State private var camera = true
    var body: some View {
        PortalScroll(title: "Digitális segítség", subtitle: "Szakértői videókonzultáció élményének interaktív előnézete.") {
            VizitBanner(
                text: "Bemutató mód. Itt ellenőrizheted a hívás felületét; valódi szakértőhöz még nem kapcsolunk, és engedélyt sem kérünk.",
                tone: .warning
            )
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
            Text("A VIZIT interaktív próbája nem továbbít hangot vagy videót. Éles foglaláskor külön, egyértelmű hozzájárulás előzi meg a kamera- és mikrofonengedélyt.")
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
    @AppStorage("toolkit.answer.mail") private var mailAnswer = -1
    @AppStorage("toolkit.answer.mfa") private var mfaAnswer = -1
    @AppStorage("toolkit.answer.backup") private var backupAnswer = -1
    @AppStorage("toolkit.answer.presence") private var presenceAnswer = -1
    @AppStorage("toolkit.openedGuideIDs") private var openedGuideIDs = ""
    @State private var expandedGuideIDs = Set<String>()

    private var answers: [String: Int] {
        ["mail": mailAnswer, "mfa": mfaAnswer, "backup": backupAnswer, "presence": presenceAnswer]
    }

    private var answeredCount: Int { answers.values.filter { $0 >= 0 }.count }
    private var yesCount: Int { answers.values.filter { $0 == 1 }.count }
    private var openedGuides: Set<String> { Set(openedGuideIDs.split(separator: "|").map(String.init)) }
    private var recommendedGuide: ToolkitGuide? {
        let guideID = toolkitQuestions.first(where: { answers[$0.id] == 0 })?.recommendationGuideID
            ?? toolkitQuestions.first(where: { answers[$0.id] == -1 })?.recommendationGuideID
            ?? "sales"
        return toolkitGuides.first(where: { $0.id == guideID })
    }

    var body: some View {
        PortalScroll(
            title: "Vállalkozói eszköztár",
            subtitle: "Négy kérdés a digitális felkészültségedről. A válaszaid alapján konkrét következő lépést és hivatalos szolgáltatásokat kapsz."
        ) {
            HStack(spacing: VizitSpace.md) {
                VStack(alignment: .leading, spacing: VizitSpace.xxs) {
                    Text("Digitális állapotod")
                        .font(VizitFont.h3)
                        .foregroundStyle(.white)
                    Text(answeredCount < toolkitQuestions.count
                         ? "Válaszolj még \(toolkitQuestions.count - answeredCount) kérdésre."
                         : yesCount == toolkitQuestions.count
                            ? "Erős alapokkal dolgozol."
                            : "\(toolkitQuestions.count - yesCount) terület fejleszthető.")
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(Color.white.opacity(0.76))
                }
                Spacer(minLength: 0)
                Text("\(yesCount)/\(toolkitQuestions.count)")
                    .font(VizitFont.h3.monospacedDigit())
                    .foregroundStyle(VizitColor.accent)
                    .frame(width: 68, height: 68)
                    .background(Color.white.opacity(0.10))
                    .clipShape(Circle())
                    .accessibilityLabel("\(yesCount) igen válasz \(toolkitQuestions.count) kérdésből")
            }
            .padding(VizitSpace.md)
            .background(VizitColor.ink)
            .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))

            VizitGroup {
                ForEach(Array(toolkitQuestions.enumerated()), id: \.element.id) { index, question in
                    if index > 0 { VizitDivider() }
                    HStack(alignment: .center, spacing: VizitSpace.sm) {
                        Text(question.title)
                            .font(VizitFont.body)
                            .foregroundStyle(VizitColor.textPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                        Spacer(minLength: VizitSpace.xs)
                        answerControl(for: question)
                    }
                    .padding(VizitSpace.md)
                }
            }

            if let guide = recommendedGuide {
                VizitSectionHeader(title: "A következő lépésed")
                Button {
                    expandedGuideIDs.insert(guide.id)
                } label: {
                    HStack(alignment: .top, spacing: VizitSpace.sm) {
                        VizitIconChip(systemImage: guide.icon, tint: VizitColor.primary, background: VizitColor.primarySubtle)
                        VStack(alignment: .leading, spacing: VizitSpace.xxs) {
                            Text(guide.title)
                                .font(VizitFont.label)
                                .foregroundStyle(VizitColor.textPrimary)
                            Text(guide.result)
                                .font(VizitFont.bodySmall)
                                .foregroundStyle(VizitColor.textSecondary)
                                .fixedSize(horizontal: false, vertical: true)
                            Text("Konkrét útmutató megnyitása")
                                .font(VizitFont.label)
                                .foregroundStyle(VizitColor.primary)
                        }
                        Spacer(minLength: 0)
                        Image(systemName: "arrow.right")
                            .foregroundStyle(VizitColor.primary)
                    }
                    .padding(VizitSpace.md)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(VizitColor.primarySubtle)
                    .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
                }
                .buttonStyle(.plain)
            }

            VizitSectionHeader(title: "Gyakorlati útmutatók")
            ForEach(toolkitGuides) { guide in
                DisclosureGroup(
                    isExpanded: Binding(
                        get: { expandedGuideIDs.contains(guide.id) },
                        set: { expanded in
                            if expanded { expandedGuideIDs.insert(guide.id) }
                            else { expandedGuideIDs.remove(guide.id) }
                        }
                    )
                ) {
                    VStack(alignment: .leading, spacing: VizitSpace.md) {
                        VStack(alignment: .leading, spacing: VizitSpace.xxs) {
                            Text("ELÉRENDŐ EREDMÉNY").vizitOverline().foregroundStyle(VizitColor.primary)
                            Text(guide.result).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textPrimary)
                        }
                        .padding(VizitSpace.sm)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(VizitColor.primarySubtle)
                        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))

                        Text("Lépésről lépésre").font(VizitFont.label).foregroundStyle(VizitColor.textPrimary)
                        ForEach(Array(guide.steps.enumerated()), id: \.offset) { index, step in
                            HStack(alignment: .top, spacing: VizitSpace.sm) {
                                Text("\(index + 1)")
                                    .font(VizitFont.caption.monospacedDigit())
                                    .foregroundStyle(VizitColor.textSecondary)
                                    .frame(width: 26, height: 26)
                                    .background(VizitColor.controlTrack)
                                    .clipShape(Circle())
                                Text(step)
                                    .font(VizitFont.bodySmall)
                                    .foregroundStyle(VizitColor.textSecondary)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }

                        Text("Hivatalos szolgáltatások").font(VizitFont.label).foregroundStyle(VizitColor.textPrimary)
                        VizitGroup {
                            ForEach(Array(guide.links.enumerated()), id: \.element.id) { index, link in
                                if index > 0 { VizitDivider() }
                                VizitRow(label: link.title, systemImage: "arrow.up.forward.app", supporting: link.description) {
                                    open(link, for: guide.id)
                                }
                            }
                        }
                    }
                    .padding(.top, VizitSpace.md)
                } label: {
                    HStack(alignment: .top, spacing: VizitSpace.sm) {
                        VizitIconChip(systemImage: guide.icon, tint: VizitColor.primary, background: VizitColor.primarySubtle)
                        VStack(alignment: .leading, spacing: VizitSpace.xxs) {
                            Text(guide.title).font(VizitFont.h3).foregroundStyle(VizitColor.textPrimary)
                            Text(guide.description).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textSecondary)
                            if openedGuides.contains(guide.id) {
                                VizitStatusPill(text: "Elkezdve", tone: .success)
                            }
                        }
                    }
                }
                .tint(VizitColor.primary)
                .padding(VizitSpace.md)
                .background(VizitColor.surface)
                .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous)
                        .stroke(VizitColor.border, lineWidth: 1)
                }
            }

            VizitPanel {
                HStack(spacing: VizitSpace.md) {
                    VizitIconChip(systemImage: "checkmark.shield", tint: VizitColor.primary, background: VizitColor.primarySubtle)
                    Text("Az eszköztár nem jelöl önkényesen készre semmit. A válaszaid megmaradnak, a külső szolgáltatás megnyitása pedig csak „Elkezdve” állapotot jelent.")
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(VizitColor.textSecondary)
                }
            }
            Text("A linkek hivatalos vagy széles körben használt külső szolgáltatásokhoz vezetnek. Az adózási és jogi döntést egyeztesd könyvelővel vagy jogi szakértővel.")
                .font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)
        }
    }

    private func answerControl(for question: ToolkitQuestion) -> some View {
        HStack(spacing: 2) {
            answerButton("Igen", value: 1, question: question)
            answerButton("Nem", value: 0, question: question)
        }
        .padding(3)
        .background(VizitColor.controlTrack)
        .clipShape(Capsule())
        .accessibilityElement(children: .contain)
    }

    private func answerButton(_ title: String, value: Int, question: ToolkitQuestion) -> some View {
        let selected = answers[question.id] == value
        return Button { setAnswer(value, for: question.id) } label: {
            Text(title)
                .font(VizitFont.caption)
                .foregroundStyle(selected ? VizitColor.textPrimary : VizitColor.textSecondary)
                .padding(.horizontal, VizitSpace.xs)
                .frame(minHeight: VizitMetrics.minTouchTarget)
                .background(selected ? VizitColor.surface : Color.clear)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
    }

    private func setAnswer(_ value: Int, for id: String) {
        switch id {
        case "mail": mailAnswer = value
        case "mfa": mfaAnswer = value
        case "backup": backupAnswer = value
        case "presence": presenceAnswer = value
        default: break
        }
    }

    private func open(_ link: GuideLink, for guideID: String) {
        guard let url = SafeLink.https(link.url) else { return }
        var opened = openedGuides
        opened.insert(guideID)
        openedGuideIDs = opened.sorted().joined(separator: "|")
        openURL(url)
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
    @EnvironmentObject private var presentation: CardPresentationStore
    @Binding var themeMode: ThemeMode
    @State private var editingProfile = false
    @State private var changingEmail = false
    @State private var emailDraft = ""
    @State private var confirmPasswordReset = false
    @State private var changingTheme = false
    @State private var customizing = false
    @State private var adjustingVisibility = false
    @State private var showNFCInformation = false
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
                        VizitLargeTitle("Beállítások")

                        VizitSectionHeader(title: "Megjelenés")
                        VizitGroup {
                            VizitRow(
                                label: "Téma",
                                systemImage: "paintpalette",
                                value: themeMode.label
                            ) { changingTheme = true }
                            .accessibilityIdentifier("settings.theme.open")
                            VizitDivider()
                            VizitRow(
                                label: "Kártya megjelenése",
                                systemImage: "rectangle.on.rectangle",
                                value: presentation.value.colorway.label
                            ) { customizing = true }
                            .accessibilityIdentifier("card.appearance")
                        }

                        VizitSectionHeader(title: "Megosztás")
                        VizitGroup {
                            VizitRow(
                                label: "NFC",
                                systemImage: "wave.3.right",
                                value: "Be",
                                supporting: "Érintéses átadás és fizikai NFC-kártya"
                            ) { showNFCInformation = true }
                            VizitDivider()
                            VizitRow(
                                label: "Adatok láthatósága",
                                systemImage: "eye",
                                value: "(presentation.value.sharedFieldCount) látható",
                                supporting: "Mezőnként szabályozható"
                            ) { adjustingVisibility = true }
                            .accessibilityIdentifier("card.visibility")
                            VizitDivider()
                            VizitRow(
                                label: "Nyilvános profil",
                                systemImage: "globe",
                                value: store.profile.isPublic ? "Be" : "Ki",
                                supporting: store.profile.customDomainVerified
                                    ? store.profile.customDomain
                                    : "Automatikus VIZIT-cím"
                            ) { editingProfile = true }
                            .accessibilityIdentifier("settings.publicProfile")
                        }

                        VizitSectionHeader(title: "Fiók")
                        VizitGroup {
                            VizitRow(
                                label: "Belépve",
                                systemImage: "person.crop.circle",
                                value: store.accountEmail,
                                supporting: store.profile.displayName.isEmpty ? nil : store.profile.displayName
                            ) { showAccountDetails = true }
                            VizitDivider()
                            VizitRow(
                                label: "Kijelentkezés",
                                systemImage: "rectangle.portrait.and.arrow.right"
                            ) { confirmLogout = true }
                        }

                        VizitSectionHeader(title: "Veszélyes műveletek", tone: VizitColor.error)
                        VizitGroup(danger: true) {
                            VizitRow(
                                label: "Fiók végleges törlése",
                                systemImage: "trash",
                                supporting: "A fiók és a szerveradatok is törlődnek.",
                                destructive: true
                            ) { confirmDelete = true }
                        }

                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.bottom, VizitSpace.xxl)
                    .frame(maxWidth: 620)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $editingProfile) { ProfileEditor(draft: store.profile) }
            .sheet(isPresented: $changingEmail) { emailChangeSheet }
            .sheet(isPresented: $changingTheme) { themeSheet }
            .sheet(isPresented: $customizing) {
                CardAppearanceScreen(store: presentation, profile: store.profile)
            }
            .sheet(isPresented: $adjustingVisibility) {
                DataVisibilityScreen(
                    store: presentation,
                    profile: store.profile,
                    isPublicProfile: store.profile.isPublic
                )
            }
            .confirmationDialog(
                "Jelszómódosító e-mailt küldünk a bejelentkezési címedre.",
                isPresented: $confirmPasswordReset,
                titleVisibility: .visible
            ) {
                Button("E-mail küldése") {
                    Task { _ = await store.requestPasswordReset(email: store.accountEmail) }
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
            .alert("NFC iPhone-on", isPresented: $showNFCInformation) {
                Button("Rendben", role: .cancel) {}
            } message: {
                Text("Az iPhone nem támogat tetszőleges telefon–telefon NFC-kártyaemulációt. QR-kód, AirDrop, névjegyfájl és programozott fizikai NFC-kártya használható.")
            }
            .alert("A művelet nem sikerült", isPresented: Binding(
                get: { error != nil }, set: { if !$0 { error = nil } }
            )) {
                Button("Rendben", role: .cancel) { error = nil }
            } message: { Text(error ?? "") }
        }
    }

    private var accountDetailsSheet: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.md) {
                        VizitSectionHeader(title: "Fiók")
                        VizitGroup {
                            VizitRow(
                                label: "Profil",
                                systemImage: "person.crop.circle",
                                supporting: store.profile.displayName.isEmpty
                                    ? "Névjegyadatok beállítása"
                                    : store.profile.displayName
                            ) {
                                showAccountDetails = false
                                editingProfile = true
                            }
                            VizitDivider()
                            VizitRow(
                                label: "E-mail",
                                systemImage: "envelope",
                                value: store.accountEmail,
                                supporting: "Bejelentkezési cím módosítása"
                            ) {
                                emailDraft = store.accountEmail
                                showAccountDetails = false
                                changingEmail = true
                            }
                            VizitDivider()
                            VizitRow(
                                label: "Jelszó",
                                systemImage: "lock",
                                supporting: "Biztonságos módosító link kérése"
                            ) {
                                showAccountDetails = false
                                confirmPasswordReset = true
                            }
                        }
                    }
                    .padding(VizitSpace.md)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("Fiók")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Kész") { showAccountDetails = false }
                }
            }
        }
    }

    private var themeSheet: some View {
        NavigationStack {
            VizitScreen {
                VStack(alignment: .leading, spacing: VizitSpace.lg) {
                    Text("Válaszd ki, hogy a VIZIT világos, sötét vagy a rendszer beállítását követő megjelenést használjon.")
                        .font(VizitFont.body)
                        .foregroundStyle(VizitColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                    VizitSegmentedControl(
                        options: ThemeMode.allCases.map(\.label),
                        selection: themeIndex,
                        accessibilityIdentifier: "settings.theme"
                    )
                    Spacer()
                }
                .padding(VizitSpace.md)
            }
            .navigationTitle("Téma")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Kész") { changingTheme = false }
                }
            }
        }
    }

    private var emailChangeSheet: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.md) {
                        VizitBanner(
                            text: "Az új cím csak a megerősítő levél jóváhagyása után lép életbe. Addig a jelenlegi címmel tudsz belépni.",
                            tone: .info
                        )
                        VizitTextField(
                            label: "Új e-mail-cím",
                            text: $emailDraft,
                            placeholder: "nev@pelda.hu",
                            keyboard: .emailAddress,
                            contentType: .emailAddress,
                            autocapitalization: .never,
                            submitLabel: .send
                        ) {
                            submitEmailChange()
                        }
                        VizitButton(
                            title: "Megerősítő e-mail küldése",
                            systemImage: "paperplane",
                            isLoading: store.busy,
                            isEnabled: !store.busy,
                            action: submitEmailChange
                        )
                    }
                    .padding(VizitSpace.md)
                    .frame(maxWidth: 540)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("E-mail módosítása")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Mégse") { changingEmail = false }
                }
            }
        }
    }

    private func submitEmailChange() {
        Task {
            if await store.changeEmail(emailDraft) { changingEmail = false }
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
