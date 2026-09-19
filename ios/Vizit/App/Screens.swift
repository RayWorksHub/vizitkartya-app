import SwiftUI
import Contacts
import CoreImage.CIFilterBuiltins

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
                                label: "Tudástár",
                                systemImage: "book.closed",
                                supporting: "Tippek és források a digitális névjegyhez"
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
                            options: [photoContactURL != nil ? "Fényképes QR" : "Kontakt QR", "Profil QR"],
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
        if photoContactURL != nil {
            return "Beolvasás után a profilképpel együtt menthető a névjegy."
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
        if let url = photoContactURL { return url.absoluteString }
        return try? VCard.qrPayload(store.profile)
    }

    private var publicURL: URL? {
        guard store.profile.isPublic, store.syncStatus == .synced,
              let base = store.configuration?.publicProfileBaseURL else { return nil }
        return PublicProfileLink.make(baseURL: base, slug: store.profile.publicSlug)
    }

    private var photoContactURL: URL? {
        guard !store.profile.photoBase64.isEmpty, store.profile.photoSyncInitialized,
              let url = publicURL else { return nil }
        return ContactQRLink.make(publicURL: url)
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

    static func make(_ payload: String) -> UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(payload.utf8)
        filter.correctionLevel = "L"
        guard let code = filter.outputImage else { return nil }
        let scale: CGFloat = 10
        let scaled = code.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        let quietZone = CIImage(color: .white).cropped(to: scaled.extent.insetBy(dx: -4 * scale, dy: -4 * scale))
        let output = scaled.composited(over: quietZone)
        guard let cg = context.createCGImage(output, from: output.extent) else { return nil }
        return UIImage(cgImage: cg)
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

// MARK: - Knowledge hub

private struct BusinessResource: Identifiable {
    let id: String
    let title: String
    let description: String
    let url: String
    let icon: String
}

struct BusinessHubScreen: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    private let resources = [
        BusinessResource(
            id: "vosz-youtube",
            title: "VOSZ videók",
            description: "Vállalkozói hírek, interjúk és gyakorlati videók a VOSZ YouTube-csatornáján.",
            url: "https://youtube.com/@vosz.?si=k2EmMlI8Q5ttlPZC",
            icon: "play.rectangle.fill"
        ),
        BusinessResource(
            id: "vosz",
            title: "VOSZ vállalkozói információk",
            description: "Érdekképviselet, tanácsadás, programok és aktuális vállalkozói hírek.",
            url: "https://www.vosz.hu/hu",
            icon: "briefcase.fill"
        ),
        BusinessResource(
            id: "voszport",
            title: "VOSZPort",
            description: "Digitális ügyintézési és tudásmegosztási felület vállalkozásoknak.",
            url: "https://voszport.com/",
            icon: "globe.europe.africa.fill"
        )
    ]

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.md) {
                        VizitBrandHeader(style: .compact)
                            .padding(.top, VizitSpace.xs)

                        Text("Hasznos külső források hírekhez, fejlődéshez és ügyintézéshez.")
                            .font(VizitFont.body)
                            .foregroundStyle(VizitColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)

                        VizitGroup {
                            ForEach(Array(resources.enumerated()), id: \.element.id) { index, resource in
                                if index > 0 { VizitDivider() }
                                VizitRow(
                                    label: resource.title,
                                    systemImage: resource.icon,
                                    supporting: resource.description
                                ) {
                                    guard let url = SafeLink.https(resource.url) else { return }
                                    openURL(url)
                                }
                            }
                        }

                        Text("A hivatkozások külső oldalakra vezetnek. A VIZIT nem áll kapcsolatban ezek tartalmának üzemeltetésével.")
                            .font(VizitFont.bodySmall)
                            .foregroundStyle(VizitColor.textMuted)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.bottom, VizitSpace.xxl)
                    .frame(maxWidth: 620)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("Tudástár")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Kész") { dismiss() }
                }
            }
        }
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
