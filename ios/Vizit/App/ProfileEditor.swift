import SwiftUI
import PhotosUI
import ImageIO

/// Profile editing, restructured from one unbroken list of fields into the same
/// six labelled groups the Android `ProfileEditScreen` uses: Profilkép,
/// Személyes adatok, Munkahely, Elérhetőségek, Közösségi profilok, Megosztási
/// adatok. The photo pipeline, validation, save path and every accessibility
/// identifier are carried over unchanged.
struct ProfileEditor: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss
    @State var draft: ContactProfile
    @State private var photo: PhotosPickerItem?
    @State private var logoSelection: PhotosPickerItem?
    @State private var error: String?
    @State private var loadingPhoto = false

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.xl) {
                        photoSection
                        group("Céges logó") {
                            if let data = Data(base64Encoded: draft.logoBase64), let image = UIImage(data: data) {
                                Image(uiImage: image).resizable().scaledToFit().frame(height: 96)
                                    .accessibilityLabel("Céges logó")
                            }
                            PhotosPicker(selection: $logoSelection, matching: .images) {
                                Label("Logó kiválasztása", systemImage: "photo")
                            }
                            if !draft.logoBase64.isEmpty {
                                Button("Logó eltávolítása") { draft.logoBase64 = ""; logoSelection = nil }
                            }
                        }
                        personalSection
                        workSection
                        contactSection
                        socialSection
                        sharingSection
                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.vertical, VizitSpace.lg)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                }
                .scrollDismissesKeyboard(.interactively)
            }
            .navigationTitle("Névjegy szerkesztése")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Mégse") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Mentés") {
                        do { try store.save(draft); dismiss() }
                        catch { self.error = error.localizedDescription }
                    }
                    .disabled(loadingPhoto || store.storageError != nil)
                    .accessibilityIdentifier("profile.save")
                }
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Kész") { dismissKeyboard() }
                        .accessibilityIdentifier("profile.keyboardDone")
                }
            }
            .alert("A névjegy nem menthető", isPresented: Binding(get: { error != nil }, set: { if !$0 { error = nil } })) {
                Button("Rendben", role: .cancel) { error = nil }
            } message: { Text(error ?? "") }
            .task(id: photo) {
                guard let selected = photo else { loadingPhoto = false; return }
                loadingPhoto = true
                defer { if photo == selected { loadingPhoto = false } }
                do {
                    guard let bytes = try await selected.loadTransferable(type: Data.self),
                          bytes.count <= 30 * 1024 * 1024,
                          let source = CGImageSourceCreateWithData(bytes as CFData, nil),
                          let thumbnail = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                            kCGImageSourceCreateThumbnailFromImageAlways: true,
                            kCGImageSourceCreateThumbnailWithTransform: true,
                            kCGImageSourceThumbnailMaxPixelSize: 512
                          ] as CFDictionary),
                          let jpeg = UIImage(cgImage: thumbnail).jpegData(compressionQuality: 0.8),
                          jpeg.count <= 256 * 1024 else { throw ProfileError.invalidPhoto }
                    try Task.checkCancellation()
                    draft.photoBase64 = jpeg.base64EncodedString()
                } catch is CancellationError {
                    // A newer selection owns the editor; do not replace it.
                } catch {
                    self.error = "A kép betöltése nem sikerült. Próbálj másik képet választani."
                }
            }
            .task(id: logoSelection) {
                guard let selected = logoSelection else { return }
                loadingPhoto = true
                defer { if logoSelection == selected { loadingPhoto = false } }
                do {
                    guard let bytes = try await selected.loadTransferable(type: Data.self), bytes.count <= 25 * 1024 * 1024,
                          let source = CGImageSourceCreateWithData(bytes as CFData, nil),
                          let thumbnail = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                            kCGImageSourceCreateThumbnailFromImageAlways: true,
                            kCGImageSourceCreateThumbnailWithTransform: true,
                            kCGImageSourceThumbnailMaxPixelSize: 512
                          ] as CFDictionary),
                          let jpeg = UIImage(cgImage: thumbnail).jpegData(compressionQuality: 0.8),
                          jpeg.count <= 256 * 1024 else { throw ProfileError.invalidPhoto }
                    try Task.checkCancellation()
                    draft.logoBase64 = jpeg.base64EncodedString()
                } catch is CancellationError { /* A newer selection owns the editor. */ }
                catch { self.error = "A logó betöltése nem sikerült. Válassz másik képet." }
            }
        }
    }

    /// Resigns first responder globally. The fields are composite views, so a
    /// FocusState binding on them would not reach the inner UITextField.
    private func dismissKeyboard() {
        UIApplication.shared.sendAction(
            #selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil
        )
    }

    // MARK: - Groups

    private func group<Content: View>(
        _ title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: VizitSpace.sm) {
            VizitSectionHeader(title: title)
            content()
        }
    }

    private var photoSection: some View {
        group("Profilkép") {
            VizitPanel {
                HStack(spacing: VizitSpace.md) {
                    VizitAvatar(profile: draft, size: 72)
                    VStack(alignment: .leading, spacing: VizitSpace.xs) {
                        PhotosPicker(selection: $photo, matching: .images) {
                            Label("Kép kiválasztása", systemImage: "photo")
                                .font(VizitFont.label)
                                .foregroundStyle(VizitColor.primary)
                        }
                        .disabled(loadingPhoto)
                        .frame(minHeight: VizitMetrics.minTouchTarget, alignment: .leading)

                        if !draft.photoBase64.isEmpty {
                            Button("Kép eltávolítása") {
                                photo = nil
                                draft.photoBase64 = ""
                            }
                            .font(VizitFont.label)
                            .foregroundStyle(VizitColor.error)
                            .frame(minHeight: VizitMetrics.minTouchTarget, alignment: .leading)
                        }

                        if loadingPhoto {
                            HStack(spacing: VizitSpace.xs) {
                                ProgressView().controlSize(.small)
                                Text("Kép feldolgozása…")
                                    .font(VizitFont.bodySmall)
                                    .foregroundStyle(VizitColor.textSecondary)
                            }
                        }
                    }
                    Spacer(minLength: 0)
                }
            }
        }
    }

    private var personalSection: some View {
        group("Személyes adatok") {
            VStack(spacing: VizitSpace.sm) {
                VizitTextField(
                    label: "Megjelenített név",
                    text: $draft.fullName,
                    placeholder: "pl. Kovács Anna",
                    contentType: .name,
                    autocapitalization: .words,
                    identifier: "profile.fullName",
                    submitLabel: .next
                )

                VizitTextField(
                    label: "Vezetéknév",
                    text: $draft.lastName,
                    contentType: .familyName,
                    autocapitalization: .words,
                    submitLabel: .next
                )

                VizitTextField(
                    label: "Keresztnév",
                    text: $draft.firstName,
                    contentType: .givenName,
                    autocapitalization: .words,
                    submitLabel: .next
                )
            }
        }
    }

    private var workSection: some View {
        group("Munkahely") {
            VStack(spacing: VizitSpace.sm) {
                VizitTextField(
                    label: "Cég",
                    text: $draft.company,
                    contentType: .organizationName,
                    autocapitalization: .words,
                    submitLabel: .next
                )

                VizitTextField(
                    label: "Beosztás",
                    text: $draft.jobTitle,
                    contentType: .jobTitle,
                    autocapitalization: .sentences,
                    submitLabel: .next
                )
                VizitTextField(label: "Rövid bemutatkozás", text: $draft.bio, submitLabel: .next)

                VizitTextField(
                    label: "Cím",
                    text: $draft.address,
                    contentType: .fullStreetAddress,
                    autocapitalization: .sentences,
                    submitLabel: .next
                )
            }
        }
    }

    private var contactSection: some View {
        group("Elérhetőségek") {
            VStack(spacing: VizitSpace.sm) {
                VizitTextField(
                    label: "Telefonszám",
                    text: $draft.phone,
                    placeholder: "+36 …",
                    keyboard: .phonePad,
                    contentType: .telephoneNumber,
                    identifier: "profile.phone",
                    submitLabel: .next
                )

                VizitTextField(
                    label: "E-mail-cím",
                    text: $draft.email,
                    placeholder: "nev@pelda.hu",
                    keyboard: .emailAddress,
                    contentType: .emailAddress,
                    autocapitalization: .never,
                    identifier: "profile.email",
                    submitLabel: .next
                )

                VizitTextField(
                    label: "Weboldal",
                    text: $draft.website,
                    placeholder: "https://…",
                    keyboard: .URL,
                    contentType: .URL,
                    autocapitalization: .never,
                    submitLabel: .next
                )
            }
        }
    }

    private var socialSection: some View {
        group("Közösségi profilok") {
            VStack(spacing: VizitSpace.sm) {
                VizitTextField(
                    label: "LinkedIn",
                    text: $draft.linkedIn,
                    placeholder: "https://…",
                    keyboard: .URL,
                    autocapitalization: .never,
                    submitLabel: .next
                )

                VizitTextField(
                    label: "Facebook",
                    text: $draft.facebook,
                    placeholder: "https://…",
                    keyboard: .URL,
                    autocapitalization: .never,
                    submitLabel: .next
                )

                VizitTextField(
                    label: "Instagram",
                    text: $draft.instagram,
                    placeholder: "https://…",
                    keyboard: .URL,
                    autocapitalization: .never,
                    submitLabel: .next
                )

                VizitTextField(
                    label: "TikTok",
                    text: $draft.tiktok,
                    placeholder: "https://…",
                    keyboard: .URL,
                    autocapitalization: .never,
                    submitLabel: .next
                )

                VizitTextField(
                    label: "YouTube",
                    text: $draft.youtube,
                    placeholder: "https://…",
                    keyboard: .URL,
                    autocapitalization: .never,
                    submitLabel: .next
                )
                ForEach([SocialPlatform.x, .github, .custom], id: \.self) { platform in
                    VizitTextField(label: platform.label, text: Binding(
                        get: { draft.socialURL(for: platform) },
                        set: { draft.setSocialURL($0, for: platform) }
                    ), placeholder: "https://…", keyboard: .URL, autocapitalization: .never)
                }

                Text("A megadott profilok a VIZIT-névjeggyel együtt szinkronizálódnak és kerülnek átadásra.")
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(VizitColor.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var sharingSection: some View {
        group("Megosztási adatok") {
            VStack(spacing: VizitSpace.sm) {
                VizitPanel {
                    Toggle(isOn: $draft.isPublic) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Nyilvános profil engedélyezése")
                                .font(VizitFont.body)
                                .foregroundStyle(VizitColor.textPrimary)
                            Text("Bárki megnyithatja a megosztott hivatkozást.")
                                .font(VizitFont.bodySmall)
                                .foregroundStyle(VizitColor.textSecondary)
                        }
                    }
                    .tint(VizitColor.primary)
                }

                if draft.isPublic {
                    VizitPanel {
                        VStack(alignment: .leading, spacing: VizitSpace.xs) {
                            Text("Automatikus profilcím")
                                .font(VizitFont.label)
                                .foregroundStyle(VizitColor.textSecondary)
                            Text(draft.publicSlug.isEmpty
                                 ? "Az egyedi azonosítót az első mentéskor a nevedből hozzuk létre."
                                 : "www.vizitkartyam.hu/p/\(draft.publicSlug)")
                                .font(VizitFont.bodySmall)
                                .foregroundStyle(VizitColor.textMuted)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }

                    VizitTextField(
                        label: "Egyedi domain (opcionális)",
                        text: Binding(
                            get: { draft.customDomain },
                            set: {
                                draft.customDomain = $0
                                draft.customDomainVerified = false
                            }
                        ),
                        placeholder: "nevjegy.cegem.hu",
                        helper: draft.customDomain.isEmpty ? nil : (draft.customDomainVerified
                            ? "Ellenőrzött domain · ezt használja a QR és az NFC."
                            : "Beállításra vár · addig a biztos VIZIT-cím marad aktív."),
                        keyboard: .URL,
                        autocapitalization: .never,
                        submitLabel: .done
                    )
                }
            }
        }
    }

}

/// First card flow. Uses the existing AppStore save path and the device's own screen chrome.
struct ProfileWizard: View {
    var onSaving: () -> Void = {}
    var onSaveFailed: () -> Void = {}
    var onFinished: () -> Void = {}
    @EnvironmentObject private var store: AppStore
    @EnvironmentObject private var presentation: CardPresentationStore
    @State private var draft: ContactProfile = {
        var value = ContactProfile()
        value.isPublic = true
        return value
    }()
    @State private var kind: String? = nil
    @State private var index = 0
    @State private var selectedStyle: CardColorway = .ink
    @State private var skipped: Set<String> = []
    @State private var returning = false
    @State private var editedSlug = false
    @State private var photo: PhotosPickerItem?
    @State private var logo: PhotosPickerItem?
    @State private var processing = false
    @State private var published = false
    @State private var error: String?

    private var path: [String] {
        kind == "private" ? ["type", "identity", "photo", "contact", "social", "look", "done"]
            : ["type", "identity", "photo", "logo", "contact", "social", "bio", "look", "done"]
    }
    private var step: String { path[min(index, path.count - 1)] }
    private var previewPresentation: CardPresentation {
        var value = presentation.value
        value.colorway = selectedStyle
        return value
    }
    private let titles = ["type": "Névjegy típusa", "identity": "Alapadatok", "photo": "Profilkép",
                          "logo": "Céges logó", "contact": "Elérhetőségek", "social": "Közösségi profilok",
                          "bio": "Bemutatkozás", "look": "Stílus", "done": "Befejezés"]
    private let optional: Set<String> = ["photo", "logo", "contact", "social", "bio"]

    private var hasValue: Bool {
        switch step {
        case "photo": return !draft.photoBase64.isEmpty
        case "logo": return !draft.logoBase64.isEmpty
        case "contact": return ![draft.phone,draft.email,draft.website,draft.address].allSatisfy(\.isEmpty)
        case "social": return draft.socialProfiles.contains { !$0.url.isEmpty }
        case "bio": return !draft.bio.isEmpty
        default: return true
        }
    }
    private var valid: Bool {
        switch step {
        case "type": return kind != nil
        case "identity": return draft.displayName.count >= 2 && (kind == "private" || !draft.company.trimmingCharacters(in: .whitespaces).isEmpty)
        case "contact": return draft.email.isEmpty || draft.email.range(of: #"^[^\s@]+@[^\s@]+\.[^\s@]{2,}$"#, options: .regularExpression) != nil
        case "done": return PublicProfileLink.isValidSlug(draft.publicSlug)
        default: return true
        }
    }

    var body: some View {
        VizitScreen {
            if published {
                VStack(spacing: VizitSpace.md) {
                    Text("Elkészült a névjegyed").font(VizitFont.title).foregroundStyle(VizitColor.textPrimary)
                    Text(draft.isPublic ? "A névjegyed publikálva. Most már megoszthatod a profilcímedet." :
                            "A névjegyed elmentve. A nyilvános profilt később is bekapcsolhatod.")
                        .foregroundStyle(VizitColor.textSecondary)
                    VizitButton(title: "Tovább az áttekintéshez") { onFinished() }
                }
                .frame(maxWidth: 560).frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(VizitSpace.md)
            } else {
            VStack(spacing: 0) {
                HStack {
                    Text(titles[step] ?? "Névjegy").font(VizitFont.title).foregroundStyle(VizitColor.textPrimary)
                    Spacer()
                    Text(step == "done" ? "Kész" : "\(index + 1) / \(path.count - 1)")
                        .font(VizitFont.caption).foregroundStyle(VizitColor.textMuted)
                }
                .padding(.horizontal, VizitSpace.md).padding(.top, VizitSpace.md)
                ProgressView(value: Double(index + 1), total: Double(path.count - 1))
                    .tint(VizitColor.primary).padding(.horizontal, VizitSpace.md).padding(.vertical, VizitSpace.sm)
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.md) {
                        if step != "type" {
                            VizitDigitalCard(profile: draft, presentation: previewPresentation)
                        }
                        content
                        if let error { Text(error).foregroundStyle(VizitColor.error).font(VizitFont.bodySmall) }
                    }
                    .frame(maxWidth: 560).frame(maxWidth: .infinity)
                    .padding(VizitSpace.md)
                }
                .scrollDismissesKeyboard(.interactively)
                VStack(spacing: VizitSpace.xs) {
                    if index > 0 {
                        VizitButton(title: "Vissza", kind: .tertiary) {
                            index = returning ? path.count - 1 : max(0, index - 1)
                            returning = false; error = nil
                        }
                    }
                    VizitButton(title: step == "done" ? (draft.isPublic ? "Névjegy publikálása" : "Névjegy mentése") : (returning ? "Mentés" : "Tovább"),
                                isLoading: processing, isEnabled: valid && (!optional.contains(step) || hasValue)) {
                        if step == "done" { save() } else { advance(skip: false) }
                    }
                    if optional.contains(step) {
                        VizitButton(title: "Később állítom be", kind: .tertiary) { advance(skip: true) }
                    }
                }
                .padding(VizitSpace.md)
                .background(VizitColor.surface)
            }
            }
        }
        .task(id: photo) { await process(photo, isLogo: false) }
        .task(id: logo) { await process(logo, isLogo: true) }
    }

    @ViewBuilder private var content: some View {
        switch step {
        case "type":
            Text("Milyen névjegyet készítesz?").font(VizitFont.title)
            choice("Vállalkozói névjegy", detail: "Vállalkozás, beosztás, logó és bemutatkozás", selected: kind == "business") {
                kind = "business"
            }
            choice("Magánszemély", detail: "Személyes kapcsolatokhoz, csak a lényeg", selected: kind == "private") {
                kind = "private"; draft.company = ""; draft.jobTitle = ""; draft.bio = ""; draft.logoBase64 = ""
            }
        case "identity":
            Text(kind == "business" ? "Mutatkozz be" : "Hogy hívnak?").font(VizitFont.title)
            VizitTextField(label: "Teljes név *", text: $draft.fullName, contentType: .name)
            if kind == "business" {
                VizitTextField(label: "Vállalkozás / szervezet *", text: $draft.company, contentType: .organizationName)
                VizitTextField(label: "Beosztás · nem kötelező", text: $draft.jobTitle, contentType: .jobTitle)
            }
        case "photo", "logo":
            Text(step == "photo" ? "Profilkép" : "Céges logó").font(VizitFont.title)
            Text(step == "photo" ? "A partnereid könnyebben felismernek." : "A logó a profilkép sarkán jelenik meg.")
            if let data = Data(base64Encoded: step == "photo" ? draft.photoBase64 : draft.logoBase64), let image = UIImage(data: data) {
                Image(uiImage: image).resizable().scaledToFit().frame(height: 130)
                    .accessibilityLabel(step == "photo" ? "Profilkép" : "Céges logó")
            }
            if step == "photo" {
                PhotosPicker(selection: $photo, matching: .images) { Label("Kép kiválasztása", systemImage: "photo").frame(minHeight: 48) }
                if !draft.photoBase64.isEmpty { Button("Eltávolítás") { draft.photoBase64 = ""; photo = nil } }
            } else {
                PhotosPicker(selection: $logo, matching: .images) { Label("Logó kiválasztása", systemImage: "photo").frame(minHeight: 48) }
                if !draft.logoBase64.isEmpty { Button("Eltávolítás") { draft.logoBase64 = ""; logo = nil } }
            }
        case "contact":
            Text("Hol érnek el?").font(VizitFont.title)
            Text("Csak azt add meg, amit nyilvánosan is megosztanál.").foregroundStyle(VizitColor.textSecondary)
            VizitTextField(label: "Nyilvános e-mail", text: $draft.email, keyboard: .emailAddress, autocapitalization: .never)
            VizitTextField(label: "Telefonszám", text: $draft.phone, keyboard: .phonePad)
            VizitTextField(label: "Weboldal", text: $draft.website, keyboard: .URL, autocapitalization: .never)
            if kind == "business" { VizitTextField(label: "Hely / cím", text: $draft.address) }
        case "social":
            Text("Közösségi profilok").font(VizitFont.title)
            Text("Add meg, amelyiket használod. A többit később is hozzáadhatod.")
            ForEach(SocialPlatform.allCases, id: \.self) { platform in
                VizitTextField(label: platform.label, text: Binding(
                    get: { draft.socialURL(for: platform) },
                    set: { draft.setSocialURL($0, for: platform) }
                ), keyboard: .URL, autocapitalization: .never)
            }
        case "bio":
            Text("Pár mondat rólad").font(VizitFont.title)
            TextEditor(text: $draft.bio).frame(minHeight: 110)
                .onChange(of: draft.bio) { newValue in if newValue.count > 420 { draft.bio = String(newValue.prefix(420)) } }
                .accessibilityLabel("Rövid bemutatkozás")
            Text("\(draft.bio.count)/420").foregroundStyle(VizitColor.textMuted)
        case "look":
            Text("Válassz stílust").font(VizitFont.title)
            ForEach([CardColorway.ink, .brand, .emerald, .amethyst], id: \.self) { value in
                choice(value.label, detail: "Kártya színvilága", selected: selectedStyle == value) { selectedStyle = value }
            }
        default:
            Text("Elkészült a névjegyed").font(VizitFont.title)
            Text("Nézd át, és mentsd el. Később minden adatot módosíthatsz.")
            VizitTextField(label: "Profilcím: vizitkartyam.hu/…", text: Binding(
                get: { draft.publicSlug }, set: { editedSlug = true; draft.publicSlug = slug($0) }
            ), autocapitalization: .never)
            Text("A cím foglaltságát szinkronizáláskor ellenőrizzük.").font(VizitFont.caption).foregroundStyle(VizitColor.textMuted)
            Toggle("Nyilvános profil", isOn: $draft.isPublic).tint(VizitColor.primary)
            if !skipped.isEmpty {
                Text("Később beállíthatod").font(VizitFont.label)
                ForEach(path.filter { skipped.contains($0) }, id: \.self) { missing in
                    VizitButton(title: (titles[missing] ?? missing) + " · Beállítás", kind: .secondary) {
                        index = path.firstIndex(of: missing) ?? index; returning = true
                    }
                }
            }
        }
    }

    private func choice(_ title: String, detail: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack { VStack(alignment: .leading, spacing: 4) {
                Text(title).font(VizitFont.label); Text(detail).font(VizitFont.caption)
            }; Spacer(); if selected { Image(systemName: "checkmark.circle.fill") } }
            .padding(VizitSpace.md).frame(maxWidth: .infinity)
            .foregroundStyle(VizitColor.textPrimary)
            .background(selected ? VizitColor.primarySubtle : VizitColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg))
        }.buttonStyle(.plain)
    }
    private func slug(_ value: String) -> String {
        let plain = value.applyingTransform(.stripDiacritics, reverse: false)?.lowercased() ?? value.lowercased()
        return String(plain.replacingOccurrences(of: "[^a-z0-9]+", with: "-", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "-")).prefix(50))
    }
    private func advance(skip: Bool) {
        if skip {
            switch step {
            case "photo": draft.photoBase64 = ""; photo = nil
            case "logo": draft.logoBase64 = ""; logo = nil
            case "contact": draft.email = ""; draft.phone = ""; draft.website = ""; draft.address = ""
            case "social": for platform in SocialPlatform.allCases { draft.setSocialURL("", for: platform) }
            case "bio": draft.bio = ""
            default: break
            }
        }
        if skip { skipped.insert(step) } else { skipped.remove(step) }
        index = returning ? path.count - 1 : min(index + 1, path.count - 1)
        returning = false; error = nil
        if step == "done", !editedSlug { draft.publicSlug = slug(kind == "business" ? draft.company : draft.displayName) }
    }
    private func normalizeURLs() -> ContactProfile? {
        var value = draft
        func normalize(_ url: String) -> String? {
            let text = url.trimmingCharacters(in: .whitespacesAndNewlines)
            if text.isEmpty { return "" }
            let full = text.contains("://") ? text : "https://\(text)"
            return SafeLink.https(full) == nil ? nil : full
        }
        guard let website = normalize(value.website) else { return nil }
        value.website = website
        for platform in SocialPlatform.allCases {
            guard let url = normalize(value.socialURL(for: platform)) else { return nil }
            value.setSocialURL(url, for: platform)
        }
        return value
    }
    private func save() {
        guard let prepared = normalizeURLs() else { error = "A hivatkozások teljes, https:// kezdetű címek legyenek."; return }
        onSaving()
        do { try store.save(prepared); presentation.value.colorway = selectedStyle; published = true }
        catch { onSaveFailed(); self.error = error.localizedDescription }
    }
    private func process(_ selection: PhotosPickerItem?, isLogo: Bool) async {
        guard let selection else { return }
        processing = true
        defer { processing = false }
        do {
            guard let bytes = try await selection.loadTransferable(type: Data.self), bytes.count <= 25 * 1024 * 1024,
                  let source = CGImageSourceCreateWithData(bytes as CFData, nil),
                  let thumb = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                    kCGImageSourceCreateThumbnailFromImageAlways: true,
                    kCGImageSourceCreateThumbnailWithTransform: true,
                    kCGImageSourceThumbnailMaxPixelSize: 512
                  ] as CFDictionary),
                  let jpeg = UIImage(cgImage: thumb).jpegData(compressionQuality: 0.8),
                  jpeg.count <= 256 * 1024 else { throw ProfileError.invalidPhoto }
            try Task.checkCancellation()
            if isLogo { draft.logoBase64 = jpeg.base64EncodedString() }
            else { draft.photoBase64 = jpeg.base64EncodedString() }
        } catch is CancellationError { /* A newer selection owns the preview. */ }
        catch { self.error = "A kép betöltése nem sikerült. Válassz másik képet." }
    }
}
