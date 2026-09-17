import SwiftUI
import Contacts
import CoreImage.CIFilterBuiltins

struct HomeScreen: View {
    @EnvironmentObject private var store: AppStore
    @State private var editing = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    HStack {
                        Image("VizitLogo").resizable().scaledToFit().frame(width: 145, height: 48)
                            .padding(8).background(.white).clipShape(RoundedRectangle(cornerRadius: 12))
                            .accessibilityLabel("VIZIT")
                        Spacer()
                        Text("iOS DEV").font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                    }
                    PreviewNotice()
                    if let error = store.storageError {
                        Label(error, systemImage: "exclamationmark.triangle")
                            .foregroundStyle(.red)
                        Text("A Beállításokban tudod törölni a hibás helyi mentést. Az alkalmazás nem írja felül automatikusan.")
                            .font(.footnote)
                    }
                    if store.hasProfile {
                        VStack(alignment: .leading, spacing: 16) {
                            ProfileAvatar(profile: store.profile, size: 84)
                            Text(store.profile.displayName).font(.title.bold())
                                .accessibilityIdentifier("card.name")
                            if !store.profile.jobTitle.isEmpty { Text(store.profile.jobTitle).font(.headline) }
                            if !store.profile.company.isEmpty { Text(store.profile.company) }
                            Divider().overlay(.white.opacity(0.25))
                            contactLine("phone", store.profile.phone)
                            contactLine("envelope", store.profile.email)
                            contactLine("globe", store.profile.website)
                            contactLine("mappin", store.profile.address)
                            contactLine("link", store.profile.linkedIn)
                        }
                        .foregroundStyle(.white).padding(28).frame(maxWidth: .infinity, alignment: .leading)
                        .background(LinearGradient(colors: [Brand.navy, Brand.blue], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                    } else {
                        VStack(alignment: .leading, spacing: 14) {
                            Image(systemName: "person.crop.rectangle.badge.plus")
                                .font(.system(size: 42)).foregroundStyle(Brand.blue)
                            Text("A névjegyed, nálad.").font(.title.bold())
                            Text("Add meg az elérhetőségeidet, majd mutasd meg a QR-kódodat. A mentés a bejelentkezett VIZIT-fiókodhoz tartozik.")
                                .foregroundStyle(.secondary)
                        }
                        .padding(24).frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(.secondarySystemGroupedBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                    }
                    Button(store.hasProfile ? "Névjegy szerkesztése" : "Névjegy létrehozása") { editing = true }
                        .buttonStyle(.borderedProminent).controlSize(.large)
                        .frame(maxWidth: .infinity).disabled(store.storageError != nil)
                        .accessibilityIdentifier("card.edit")
                }
                .padding(20).frame(maxWidth: 640)
                .frame(maxWidth: .infinity)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("Saját névjegy").navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $editing) { ProfileEditor(draft: store.profile) }
        }
    }

    @ViewBuilder private func contactLine(_ icon: String, _ value: String) -> some View {
        if !value.isEmpty { Label(value, systemImage: icon).textSelection(.enabled) }
    }
}

struct ShareScreen: View {
    @EnvironmentObject private var store: AppStore
    @State private var shareFile: ShareFile?
    @State private var temporaryURL: URL?
    @State private var showContact = false
    @State private var error: String?
    @State private var usePublicProfile = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 22) {
                    if store.hasProfile {
                        Text(store.profile.displayName).font(.title2.bold())
                        if store.profile.isPublic, publicURL != nil {
                            Picker("QR típusa", selection: $usePublicProfile) {
                                Text("Kontakt").tag(false)
                                Text("Nyilvános profil").tag(true)
                            }.pickerStyle(.segmented)
                        }
                        qrContent
                        Text(usePublicProfile ? "Nyilvános VIZIT-profil" : "Kontakt QR – fénykép nélkül").font(.headline)
                        Text(usePublicProfile
                             ? "A kód a nyilvános profil HTTPS-címét adja át. Csak akkor használd, ha valóban nyilvánossá tetted a profilt."
                             : "A fogadó telefon kamerájával olvasd be. A felkínált névjegyet a fogadó hagyja jóvá; a pontos működés a kameraalkalmazástól függ.")
                            .multilineTextAlignment(.center).foregroundStyle(.secondary)
                        DisclosureGroup("Milyen adatokat ad át a QR-kód?") {
                            Text("A mentett nevet, telefonszámot, e-mail-címet, céget, beosztást, címet és hivatkozásokat. A profilkép nincs a kódban; ez segíti a beolvashatóságot.")
                                .font(.footnote).padding(.top, 8)
                        }
                        Divider()
                        Button { showContact = true } label: {
                            Label("Mentés a Kontaktokba", systemImage: "person.crop.circle.badge.plus")
                        }
                        .buttonStyle(.bordered).controlSize(.large)
                        Button {
                            do {
                                let file = try ContactBridge.shareFile(store.profile)
                                temporaryURL = file.url
                                shareFile = file
                            } catch { self.error = error.localizedDescription }
                        } label: {
                            Label("Névjegy küldése képpel", systemImage: "square.and.arrow.up")
                        }
                        .buttonStyle(.borderedProminent).controlSize(.large)
                        Text("A képes küldés külön .vcf fájlt oszt meg, például AirDroppal. A Kontakt QR nem használ fájlletöltést.")
                            .font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
                        Label("Az iPhone nem képes Android HCE-névjegyként NFC-jelet sugározni. iPhone-on QR-t, AirDropot vagy HTTPS-profilt használj.",
                              systemImage: "wave.3.right.circle")
                            .font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
                    } else {
                        Image(systemName: "qrcode").font(.system(size: 54)).foregroundStyle(Brand.blue)
                        Text("Először készítsd el a névjegyedet a Névjegy fülön.")
                            .font(.headline).multilineTextAlignment(.center)
                    }
                }
                .padding(24).frame(maxWidth: 560).frame(maxWidth: .infinity)
            }
            .navigationTitle("Megosztás")
            .sheet(item: $shareFile, onDismiss: {
                if let url = temporaryURL { ContactBridge.removeShareFile(url) }
                temporaryURL = nil
            }) { file in ActivitySheet(url: file.url) }
            .sheet(isPresented: $showContact) {
                ContactEditor(contact: ContactBridge.contact(store.profile)) { _ in showContact = false }
            }
            .alert("A megosztás nem sikerült", isPresented: Binding(get: { error != nil }, set: { if !$0 { error = nil } })) {
                Button("Rendben", role: .cancel) { error = nil }
            } message: { Text(error ?? "") }
        }
    }

    @ViewBuilder private var qrContent: some View {
        switch Result(catching: {
            if usePublicProfile, let url = publicURL { return url.absoluteString }
            return try VCard.qrPayload(store.profile)
        }) {
        case .success(let payload):
            if let image = QRImage.make(payload) {
                Image(uiImage: image).interpolation(.none).resizable().scaledToFit()
                    .frame(maxWidth: 320).padding(24).background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                    .accessibilityLabel("A névjegy Kontakt QR-kódja")
                    .accessibilityIdentifier("share.qr")
            } else {
                Label("A QR-kód nem készíthető el.", systemImage: "exclamationmark.triangle")
            }
        case .failure(let error):
            Label(error.localizedDescription, systemImage: "exclamationmark.triangle")
                .foregroundStyle(.red)
        }
    }

    private var publicURL: URL? {
        guard let base = store.configuration?.publicProfileBaseURL else { return nil }
        return PublicProfileLink.make(baseURL: base, slug: store.profile.publicSlug)
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
            VStack(spacing: 22) {
                Image(systemName: "qrcode.viewfinder").font(.system(size: 72)).foregroundStyle(Brand.blue)
                Text("Új kapcsolat, egy beolvasással.").font(.title2.bold()).multilineTextAlignment(.center)
                Text("Olvass be egy névjegy-QR-kódot. A Kontaktokba mentés előtt ellenőrizheted és módosíthatod az adatokat.")
                    .foregroundStyle(.secondary).multilineTextAlignment(.center)
                Button("QR-kód beolvasása") { scanning = true }
                    .buttonStyle(.borderedProminent).controlSize(.large)
                Text("A kamera csak a beolvasó megnyitásakor kér engedélyt. Webcímet az alkalmazás nem nyit meg automatikusan.")
                    .font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
            }
            .padding(24).frame(maxWidth: 560).frame(maxWidth: .infinity, maxHeight: .infinity)
            .navigationTitle("Beolvasás")
            .fullScreenCover(isPresented: $scanning, onDismiss: processScan) {
                NavigationStack {
                    QRScanner(onResult: { text in pendingText = text; scanning = false },
                              onError: { text in message = text; scanning = false })
                        .ignoresSafeArea(edges: .bottom)
                        .navigationTitle("Névjegy beolvasása").navigationBarTitleDisplayMode(.inline)
                        .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Bezárás") { scanning = false } } }
                }
            }
            .sheet(item: $incoming) { item in
                ContactEditor(contact: item.contact) { saved in
                    incoming = nil
                    if saved { message = "A névjegyet elmentetted a Kontaktokba." }
                }
            }
            .alert("Webcím a QR-kódban", isPresented: Binding(get: { link != nil }, set: { if !$0 { link = nil } })) {
                Button("Mégse", role: .cancel) { link = nil }
                Button("Megnyitás") { if let url = link { openURL(url) }; link = nil }
            } message: { Text(link?.absoluteString ?? "") }
            .alert("Beolvasás", isPresented: Binding(get: { message != nil && !scanning && incoming == nil }, set: { if !$0 { message = nil } })) {
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
        guard value.utf8.count <= 16_384 else { message = "A QR-kód túl sok adatot tartalmaz."; return }
        if let url = SafeLink.https(value) { link = url; return }
        guard value.uppercased().hasPrefix("BEGIN:VCARD"), value.uppercased().hasSuffix("END:VCARD"),
              let contacts = try? CNContactVCardSerialization.contacts(with: Data(value.utf8)), contacts.count == 1,
              let clean = contacts[0].mutableCopy() as? CNMutableContact else {
            message = "Ez nem támogatott névjegy-QR vagy HTTPS-webcím. Az alkalmazás nem hajtott végre semmilyen műveletet."
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
            List {
                Section("Alkalmazás") {
                    Label("VIZIT iOS DEV 0.2", systemImage: "iphone")
                    Text("Natív iPhone- és iPad-felület, iOS/iPadOS 16-tól.")
                    Text("E-mailes belépés, helyi-first profil, felhőszinkron, profilkép, Kontakt- és profil-QR, kamerás beolvasás és rendszermegosztás. A Google-belépés csak külön szolgáltatói engedélyezés után jelenik meg.")
                }
                Section("iPhone-korlát") {
                    Text("Az iOS nem enged Android HCE-szerű NFC-névjegyküldést. Ez nem emuláció: iPhone-on QR, AirDrop és a nyilvános HTTPS-profil használható.")
                }
                Section("Adatok") {
                    Text("A helyi névjegy teljes fájlvédelemmel, mentésből kizárva tárolódik. A munkamenet csak feloldott készüléken elérhető kulcstár-elemként marad meg.")
                    Text("A megosztott másolatokat és a Kontaktokba mentett névjegyeket a helyi törlés nem vonja vissza.")
                    if let issue = store.storageError { Text(issue).foregroundStyle(.red) }
                    Button("Helyi gyorsítótár törlése", role: .destructive) { confirmReset = true }
                        .accessibilityIdentifier("settings.reset")
                }
                Section("Fiók") {
                    if !store.accountEmail.isEmpty { LabeledContent("Belépve", value: store.accountEmail) }
                    Button("Kijelentkezés") { confirmLogout = true }
                    Button("Fiók és szerveradatok végleges törlése", role: .destructive) { confirmDelete = true }
                }
            }
            .navigationTitle("Beállítások")
            .confirmationDialog("Törlöd a helyi gyorsítótárat és a profilképet erről az iPhone-ról?", isPresented: $confirmReset, titleVisibility: .visible) {
                Button("Helyi adatok törlése", role: .destructive) {
                    do { try store.reset() } catch { self.error = error.localizedDescription }
                }
                Button("Mégse", role: .cancel) {}
            }
            .confirmationDialog("Biztosan kijelentkezel? A helyi gyorsítótár is törlődik.", isPresented: $confirmLogout, titleVisibility: .visible) {
                Button("Kijelentkezés", role: .destructive) { Task { await store.logout() } }
                Button("Mégse", role: .cancel) {}
            }
            .sheet(isPresented: $confirmDelete) {
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
                    .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Mégse") { confirmDelete = false } } }
                }
            }
            .alert("A törlés nem sikerült", isPresented: Binding(get: { error != nil }, set: { if !$0 { error = nil } })) {
                Button("Rendben", role: .cancel) { error = nil }
            } message: { Text(error ?? "") }
        }
    }
}
