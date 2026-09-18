import SwiftUI
import Contacts
import CoreImage.CIFilterBuiltins

struct HomeScreen: View {
    @EnvironmentObject private var store: AppStore
    @State private var editing = false

    var body: some View {
        NavigationStack {
            ZStack {
                VizitScreenBackground()
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        hero
                        PreviewNotice()

                        if let error = store.storageError {
                            VizitCard {
                                Label(error, systemImage: "exclamationmark.triangle.fill")
                                    .font(.headline)
                                    .foregroundStyle(.red)
                                Text("A hibás mentést nem írjuk felül. A Beállításokban biztonságosan törölheted a helyi gyorsítótárat.")
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                                    .padding(.top, 5)
                            }
                        }

                        profileCard

                        Button { editing = true } label: {
                            VizitPrimaryButtonLabel(
                                title: store.hasProfile ? "Névjegy szerkesztése" : "Névjegy létrehozása",
                                systemImage: store.hasProfile ? "pencil" : "person.badge.plus"
                            )
                        }
                        .buttonStyle(.plain)
                        .disabled(store.storageError != nil)
                        .opacity(store.storageError == nil ? 1 : 0.55)
                        .accessibilityIdentifier("card.edit")

                        capabilityCard
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .frame(maxWidth: 640)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("Saját névjegy")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $editing) { ProfileEditor(draft: store.profile) }
        }
    }

    private var hero: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top) {
                VizitBrandLockup(height: 54, padded: false)
                    .frame(width: 170)
                Spacer()
                Text("BÉTA")
                    .font(.caption2.weight(.black))
                    .tracking(1.5)
                    .foregroundStyle(Brand.navy)
                    .padding(.horizontal, 11)
                    .padding(.vertical, 7)
                    .background(Brand.cyan)
                    .clipShape(Capsule())
            }
            Text("Egy érintés.\nEgy kapcsolat.")
                .font(.system(size: 30, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
            Text("A névjegyedet te kezeled, és akkor osztod meg, amikor szeretnéd.")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.74))
        }
        .padding(22)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Brand.heroGradient)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(alignment: .bottomTrailing) {
            Image(systemName: "wave.3.right")
                .font(.system(size: 74, weight: .thin))
                .foregroundStyle(.white.opacity(0.08))
                .padding(18)
                .accessibilityHidden(true)
        }
        .shadow(color: Brand.blue.opacity(0.2), radius: 18, y: 9)
    }

    private var profileCard: some View {
        VizitCard {
            if store.hasProfile {
                VStack(alignment: .leading, spacing: 18) {
                    HStack(alignment: .center, spacing: 16) {
                        ProfileAvatar(profile: store.profile, size: 86)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(store.profile.displayName)
                                .font(.title2.weight(.bold))
                                .accessibilityIdentifier("card.name")
                            let subtitle = [store.profile.jobTitle, store.profile.company]
                                .filter { !$0.isEmpty }.joined(separator: " · ")
                            if !subtitle.isEmpty {
                                Text(subtitle)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        Spacer(minLength: 0)
                        Button { editing = true } label: {
                            Image(systemName: "pencil")
                                .font(.headline)
                                .foregroundStyle(Brand.blue)
                                .frame(width: 42, height: 42)
                                .background(Brand.blue.opacity(0.1))
                                .clipShape(Circle())
                        }
                        .accessibilityLabel("Névjegy szerkesztése")
                    }

                    if hasContactDetails {
                        Divider()
                        VStack(alignment: .leading, spacing: 12) {
                            contactLine("phone.fill", store.profile.phone)
                            contactLine("envelope.fill", store.profile.email)
                            contactLine("globe", store.profile.website)
                            contactLine("mappin.and.ellipse", store.profile.address)
                            contactLine("link", store.profile.linkedIn)
                        }
                    }
                }
            } else {
                HStack(alignment: .top, spacing: 16) {
                    Image(systemName: "person.crop.rectangle.badge.plus")
                        .font(.system(size: 34, weight: .medium))
                        .foregroundStyle(Brand.blue)
                        .frame(width: 64, height: 64)
                        .background(Brand.blue.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    VStack(alignment: .leading, spacing: 7) {
                        Text("Állítsd össze a névjegyed")
                            .font(.title3.weight(.bold))
                        Text("Add meg az elérhetőségeidet és a profilképedet. A mentés a saját VIZIT-fiókodhoz tartozik.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
    }

    private var capabilityCard: some View {
        VizitCard {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: "qrcode")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(Brand.navy)
                    .frame(width: 48, height: 48)
                    .background(Brand.cyan)
                    .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
                VStack(alignment: .leading, spacing: 5) {
                    Text("Megosztás iPhone-on")
                        .font(.headline)
                    Text("Kontakt QR, AirDrop és nyilvános profil – a fogadó félnek nem kell telepítenie a VIZIT-et.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private var hasContactDetails: Bool {
        ![store.profile.phone, store.profile.email, store.profile.website,
          store.profile.address, store.profile.linkedIn].allSatisfy(\.isEmpty)
    }

    @ViewBuilder private func contactLine(_ icon: String, _ value: String) -> some View {
        if !value.isEmpty {
            Label {
                Text(value).textSelection(.enabled)
            } icon: {
                Image(systemName: icon).foregroundStyle(Brand.blue)
            }
            .font(.subheadline)
        }
    }
}

struct ShareScreen: View {
    @EnvironmentObject private var store: AppStore
    @State private var shareFile: ShareFile?
    @State private var temporaryURL: URL?
    @State private var showContact = false
    @State private var showFullScreenQR = false
    @State private var error: String?
    @State private var usePublicProfile = false

    var body: some View {
        NavigationStack {
            ZStack {
                VizitScreenBackground()
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        VStack(alignment: .leading, spacing: 5) {
                            Text("Átadás")
                                .font(.largeTitle.bold())
                            Text("Oszd meg a névjegyedet bármely kompatibilis telefonnal.")
                                .foregroundStyle(.secondary)
                        }

                        if store.hasProfile {
                            VizitCard {
                                VStack(spacing: 18) {
                                    HStack(spacing: 12) {
                                        ProfileAvatar(profile: store.profile, size: 54)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(store.profile.displayName).font(.headline)
                                            Text("Megosztásra kész")
                                                .font(.caption)
                                                .foregroundStyle(.secondary)
                                        }
                                        Spacer()
                                        Image(systemName: "checkmark.circle.fill")
                                            .foregroundStyle(Brand.cyan)
                                    }

                                    if store.profile.isPublic, publicURL != nil {
                                        Picker("QR típusa", selection: $usePublicProfile) {
                                            Text(photoContactURL != nil ? "Fényképes QR" : "Kontakt QR").tag(false)
                                            Text("Profil QR").tag(true)
                                        }
                                        .pickerStyle(.segmented)
                                    }

                                    qrContent

                                    VStack(spacing: 5) {
                                        Text(usePublicProfile ? "Nyilvános VIZIT-profil" : (photoContactURL != nil ? "Névjegy profilképpel" : "Offline Kontakt QR"))
                                            .font(.headline)
                                        Text(usePublicProfile
                                             ? "A QR-kód a nyilvános profil biztonságos webcímét adja át."
                                             : (photoContactURL != nil
                                                ? "Beolvasás után a névjegyoldalon a képpel együtt menthető a kontakt."
                                                : "Közvetlen, internet nélküli névjegyátadás – profilkép nélkül."))
                                            .font(.subheadline)
                                            .foregroundStyle(.secondary)
                                            .multilineTextAlignment(.center)
                                    }

                                    Button { showFullScreenQR = true } label: {
                                        Label("QR teljes képernyőn", systemImage: "arrow.up.left.and.arrow.down.right")
                                            .font(.subheadline.weight(.semibold))
                                            .frame(maxWidth: .infinity)
                                            .frame(height: 48)
                                            .background(Brand.blue.opacity(0.1))
                                            .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
                                    }
                                    .buttonStyle(.plain)
                                    .foregroundStyle(Brand.blue)
                                }
                            }

                            HStack(spacing: 12) {
                                secondaryAction(title: "Kontaktokba", icon: "person.crop.circle.badge.plus") {
                                    showContact = true
                                }
                                secondaryAction(title: "AirDrop / küldés", icon: "square.and.arrow.up") {
                                    shareVCard()
                                }
                            }

                            VizitCard {
                                DisclosureGroup("Milyen adatokat ad át?") {
                                    Text("A szinkronizált, nyilvános profil fényképes QR-ja megnyitja a névjegyoldalt, ahonnan a kép is elmenthető. Ehhez a fogadó telefonnak internet kell. A nem nyilvános vagy még nem szinkronizált profil Offline Kontakt QR-ja csak szöveget ad át. AirDroppal a kép közvetlenül a névjegyfájlban érkezik.")
                                        .font(.footnote)
                                        .foregroundStyle(.secondary)
                                        .padding(.top, 10)
                                }
                                .font(.subheadline.weight(.semibold))
                            }
                        } else {
                            VizitCard {
                                VStack(spacing: 16) {
                                    Image(systemName: "qrcode")
                                        .font(.system(size: 52, weight: .medium))
                                        .foregroundStyle(Brand.blue)
                                    Text("Még nincs megosztható névjegyed")
                                        .font(.title3.bold())
                                    Text("Először készítsd el a névjegyedet a Névjegy fülön.")
                                        .foregroundStyle(.secondary)
                                        .multilineTextAlignment(.center)
                                }
                                .frame(maxWidth: .infinity)
                            }
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: 600)
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
            .alert("A megosztás nem sikerült", isPresented: Binding(
                get: { error != nil }, set: { if !$0 { error = nil } }
            )) {
                Button("Rendben", role: .cancel) { error = nil }
            } message: { Text(error ?? "") }
        }
    }

    @ViewBuilder private var qrContent: some View {
        if let image = currentQRImage {
            Image(uiImage: image)
                .interpolation(.none)
                .resizable()
                .scaledToFit()
                .frame(maxWidth: 276)
                .padding(16)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(Brand.navy.opacity(0.08), lineWidth: 1)
                }
                .accessibilityLabel("A névjegy Kontakt QR-kódja")
                .accessibilityIdentifier("share.qr")
        } else {
            Label("A QR-kód nem készíthető el.", systemImage: "exclamationmark.triangle")
                .foregroundStyle(.red)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 30)
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

    private func secondaryAction(title: String, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon).font(.title3.weight(.semibold))
                Text(title)
                    .font(.caption.weight(.semibold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .foregroundStyle(Brand.blue)
            .frame(maxWidth: .infinity)
            .frame(height: 76)
            .background(Brand.surface)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Brand.border, lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
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
            VStack(spacing: 24) {
                Spacer()
                Text(title)
                    .font(.title2.bold())
                    .foregroundStyle(Brand.navy)
                if let image {
                    Image(uiImage: image)
                        .interpolation(.none)
                        .resizable()
                        .scaledToFit()
                        .padding(18)
                }
                Text("Olvasd be a telefon kamerájával")
                    .foregroundStyle(Brand.navy.opacity(0.66))
                Button("Bezárás", action: dismiss)
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 34)
                    .frame(height: 50)
                    .background(Brand.blue)
                    .clipShape(Capsule())
                    .padding(.bottom, 24)
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

struct ScanScreen: View {
    @Environment(\.openURL) private var openURL
    @State private var scanning = false
    @State private var pendingText: String?
    @State private var incoming: IncomingContact?
    @State private var link: URL?
    @State private var message: String?

    var body: some View {
        NavigationStack {
            ZStack {
                VizitScreenBackground()
                ScrollView {
                    VStack(spacing: 22) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 30, style: .continuous)
                                .fill(Brand.heroGradient)
                                .frame(height: 230)
                            Circle()
                                .stroke(Brand.cyan.opacity(0.45), lineWidth: 2)
                                .frame(width: 132, height: 132)
                            Image(systemName: "qrcode.viewfinder")
                                .font(.system(size: 72, weight: .medium))
                                .foregroundStyle(.white)
                        }
                        Text("Új kapcsolat, egy beolvasással.")
                            .font(.title2.bold())
                            .multilineTextAlignment(.center)
                        Text("Olvass be egy névjegy-QR-kódot. Mentés előtt minden adatot ellenőrizhetsz.")
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                        Button { scanning = true } label: {
                            VizitPrimaryButtonLabel(title: "QR-kód beolvasása", systemImage: "camera.viewfinder")
                        }
                        .buttonStyle(.plain)
                        VizitCard {
                            Label {
                                Text("A kamera csak a beolvasó megnyitásakor aktív. Webcímet az alkalmazás soha nem nyit meg automatikusan.")
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            } icon: {
                                Image(systemName: "hand.raised.fill").foregroundStyle(Brand.blue)
                            }
                        }
                    }
                    .padding(20)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("Beolvasás")
            .navigationBarTitleDisplayMode(.inline)
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

struct SettingsScreen: View {
    @EnvironmentObject private var store: AppStore
    @State private var confirmReset = false
    @State private var error: String?
    @State private var confirmLogout = false
    @State private var confirmDelete = false
    @State private var deletionPhrase = ""

    var body: some View {
        NavigationStack {
            ZStack {
                VizitScreenBackground()
                ScrollView {
                    VStack(alignment: .leading, spacing: 14) {
                        VStack(alignment: .leading, spacing: 5) {
                            Text("Beállítások").font(.largeTitle.bold())
                            Text("Fiók, adatvédelem és alkalmazásállapot.")
                                .foregroundStyle(.secondary)
                        }

                        settingCard(icon: "person.crop.circle.fill", title: "Fiók", color: Brand.blue) {
                            if !store.accountEmail.isEmpty {
                                LabeledContent("Belépve", value: store.accountEmail)
                                    .font(.subheadline)
                            }
                            Button("Kijelentkezés") { confirmLogout = true }
                                .font(.subheadline.weight(.semibold))
                            Button("Fiók végleges törlése", role: .destructive) { confirmDelete = true }
                                .font(.subheadline.weight(.semibold))
                        }

                        settingCard(icon: "arrow.triangle.2.circlepath", title: "Profil szinkron", color: Brand.cyan) {
                            Text(store.syncStatus.label)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                            if [.pending, .failed].contains(store.syncStatus), store.isOnline {
                                Button("Szinkron újrapróbálása") { store.retrySync() }
                                    .font(.subheadline.weight(.semibold))
                            }
                            if store.syncStatus == .conflict, store.isOnline {
                                Text("Válassz példányt. A felhőből letöltés előtt a helyi változatról biztonsági másolat készül.")
                                    .font(.footnote).foregroundStyle(.secondary)
                                Button("Helyi változat feltöltése") {
                                    Task { await store.resolveSyncConflict(keepLocal: true) }
                                }
                                Button("Felhőben lévő változat használata") {
                                    Task { await store.resolveSyncConflict(keepLocal: false) }
                                }
                            }
                        }

                        settingCard(icon: "lock.shield.fill", title: "Adatvédelem", color: .green) {
                            Text("A helyi névjegy teljes fájlvédelemmel, a munkamenet pedig az iPhone kulcstárában tárolódik.")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                            Text("A megosztott vagy Kontaktokba mentett példányokat a helyi törlés nem vonja vissza.")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                            if let issue = store.storageError {
                                Text(issue).font(.footnote).foregroundStyle(.red)
                            }
                            Button("Helyi gyorsítótár törlése", role: .destructive) { confirmReset = true }
                                .font(.subheadline.weight(.semibold))
                                .accessibilityIdentifier("settings.reset")
                        }

                        settingCard(icon: "iphone", title: "iPhone-megosztás", color: .orange) {
                            Text("Az iOS nem enged Android HCE-szerű NFC-kártyaemulációt. iPhone-on Kontakt QR, AirDrop és HTTPS-profil használható.")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }

                        Text("VIZIT \(versionText) · Biztonságos fejlesztői béta")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 18)
                    }
                    .padding(16)
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

    private func settingCard<Content: View>(
        icon: String,
        title: String,
        color: Color,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VizitCard {
            VStack(alignment: .leading, spacing: 13) {
                HStack(spacing: 12) {
                    Image(systemName: icon)
                        .font(.headline)
                        .foregroundStyle(color)
                        .frame(width: 40, height: 40)
                        .background(color.opacity(0.12))
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    Text(title).font(.headline)
                }
                content()
            }
        }
    }

    private var deleteAccountSheet: some View {
        NavigationStack {
            Form {
                Section("Végleges fióktörlés") {
                    Text("Ez törli a VIZIT-fiókot és a hozzá tartozó szerveradatokat. A művelet nem vonható vissza.")
                    TextField("Írd be: TÖRLÉS", text: $deletionPhrase)
                        .textInputAutocapitalization(.characters)
                }
                Section {
                    Button("Fiók végleges törlése", role: .destructive) {
                        confirmDelete = false
                        Task { await store.deleteAccount(confirmation: deletionPhrase) }
                    }
                }
            }
            .navigationTitle("Fiók törlése")
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
