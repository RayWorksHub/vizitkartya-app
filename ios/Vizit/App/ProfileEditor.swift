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


private struct WizardStepCopy {
    let eyebrow: String?
    let title: String
    let subtitle: String?
}

/// Native implementation of the supplied \`vizit-nevjegyvarazslo.html\` phone UI.
/// The desktop prototype panel, phone bezel and simulated system chrome are
/// intentionally not part of the app.
struct ProfileWizard: View {
    var isAdditional = false
    var onSaving: () -> Void = {}
    var onSaveFailed: () -> Void = {}
    var onFinished: () -> Void = {}
    var onCancel: (() -> Void)? = nil

    @EnvironmentObject private var store: AppStore
    @EnvironmentObject private var presentation: CardPresentationStore

    @State private var draft: ContactProfile = {
        var value = ContactProfile()
        value.isPublic = true
        return value
    }()
    @State private var kind: String? = nil
    @State private var index = 0
    @State private var direction = 1
    @State private var selectedStyle: CardColorway = .ink
    @State private var selectedSocial: Set<SocialPlatform> = []
    @State private var skipped: Set<String> = []
    @State private var returning = false
    @State private var editedSlug = false
    @State private var photo: PhotosPickerItem?
    @State private var logo: PhotosPickerItem?
    @State private var processing = false
    @State private var published = false
    @State private var error: String?

    private var path: [String] {
        kind == "private"
            ? ["type", "identity", "photo", "contact", "social", "look", "done"]
            : ["type", "identity", "photo", "logo", "contact", "social", "bio", "look", "done"]
    }

    private var step: String { path[min(index, path.count - 1)] }
    private var total: Int { path.count - 1 }
    private let optional: Set<String> = ["photo", "logo", "contact", "social", "bio"]
    private let titles = [
        "photo": "Profilkép",
        "logo": "Céges logó",
        "contact": "Elérhetőségek",
        "social": "Közösségi profilok",
        "bio": "Bemutatkozás"
    ]

    private var hasValue: Bool {
        switch step {
        case "photo": return !draft.photoBase64.isEmpty
        case "logo": return !draft.logoBase64.isEmpty
        case "contact": return ![draft.phone, draft.email, draft.website, draft.address].allSatisfy(\.isEmpty)
        case "social": return selectedSocial.contains { !draft.socialURL(for: $0).trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        case "bio": return !draft.bio.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        default: return true
        }
    }

    private var valid: Bool {
        switch step {
        case "type":
            return kind != nil
        case "identity":
            return draft.displayName.trimmingCharacters(in: .whitespacesAndNewlines).count >= 2 &&
                (kind == "private" || !draft.company.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        case "contact":
            return draft.email.isEmpty ||
                draft.email.range(of: #"^[^\s@]+@[^\s@]+\.[^\s@]{2,}$"#, options: .regularExpression) != nil
        case "done":
            return PublicProfileLink.isValidSlug(draft.publicSlug)
        default:
            return true
        }
    }

    private var copy: WizardStepCopy {
        switch step {
        case "type":
            return WizardStepCopy(
                eyebrow: isAdditional ? "Új névjegyed" : "Első névjegyed",
                title: "Milyen névjegyet készítesz?",
                subtitle: "Később bármikor létrehozhatsz egy másikat is."
            )
        case "identity":
            return kind == "private"
                ? WizardStepCopy(eyebrow: nil, title: "Hogy hívnak?", subtitle: "Így jelenik meg a neved a névjegyeden.")
                : WizardStepCopy(eyebrow: nil, title: "Mutatkozz be", subtitle: "Ez kerül a névjegyed tetejére.")
        case "photo":
            return WizardStepCopy(eyebrow: nil, title: "Profilkép", subtitle: "Egy arc többet mond egy névnél. A partnereid könnyebben felismernek.")
        case "logo":
            return WizardStepCopy(eyebrow: nil, title: "Céges logó", subtitle: "A profilkép jobb alsó sarkában jelenik meg, a névjegyen és a nyilvános profilon is.")
        case "contact":
            return WizardStepCopy(eyebrow: nil, title: "Hol érnek el?", subtitle: "Csak azt add meg, amit nyilvánosan is megosztanál.")
        case "social":
            return WizardStepCopy(eyebrow: nil, title: "Közösségi profilok", subtitle: "Koppints arra, amit hozzáadnál. Bármikor bővítheted.")
        case "bio":
            return WizardStepCopy(eyebrow: nil, title: "Pár mondat rólad", subtitle: "Mivel foglalkozol, miben tudsz segíteni? A nyilvános profilodon jelenik meg.")
        case "look":
            return WizardStepCopy(eyebrow: nil, title: "Válassz stílust", subtitle: "Egyedi színeket és színátmenetet később a Megjelenés menüben állíthatsz be.")
        default:
            return WizardStepCopy(eyebrow: "Utolsó lépés", title: "Elkészült a névjegyed", subtitle: "Nézd át, és publikáld. Minden adatot később is módosíthatsz.")
        }
    }

    var body: some View {
        VizitScreen {
            VStack(spacing: 0) {
                wizardTopBar
                if published {
                    publishedContent
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 16) {
                            stepHeading
                            if step != "type" && step != "done" {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Élő előnézet")
                                        .font(VizitFont.caption)
                                        .foregroundStyle(VizitColor.textMuted)
                                    WizardCompactCard(
                                        profile: draft,
                                        colorway: selectedStyle,
                                        business: kind != "private"
                                    )
                                }
                            }

                            stepBody

                            if let error {
                                Text(error)
                                    .font(VizitFont.bodySmall)
                                    .foregroundStyle(VizitColor.error)
                            }
                        }
                        .id(step)
                        .transition(.asymmetric(
                            insertion: .move(edge: direction >= 0 ? .trailing : .leading).combined(with: .opacity),
                            removal: .opacity
                        ))
                        .animation(.timingCurve(0.2, 0.7, 0.2, 1, duration: 0.28), value: step)
                        .frame(maxWidth: 560)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 20)
                        .padding(.top, 4)
                        .padding(.bottom, 24)
                    }
                    .scrollDismissesKeyboard(.interactively)
                    wizardFooter
                }
            }
        }
        .task(id: photo) { await process(photo, isLogo: false) }
        .task(id: logo) { await process(logo, isLogo: true) }
    }

    private var wizardTopBar: some View {
        VStack(spacing: 6) {
            HStack {
                if index > 0 && !published {
                    Button(action: back) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundStyle(VizitColor.textSecondary)
                            .frame(width: 44, height: 44)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Vissza")
                } else if isAdditional, let onCancel, index == 0 {
                    Button(action: onCancel) {
                        Image(systemName: "xmark")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(VizitColor.textSecondary)
                            .frame(width: 44, height: 44)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Bezárás")
                } else {
                    Color.clear.frame(width: 44, height: 44)
                }

                VStack(spacing: 6) {
                    HStack(spacing: 4) {
                        ForEach(0..<max(total, 1), id: \.self) { item in
                            Capsule()
                                .fill(item <= min(index, max(total - 1, 0)) || step == "done"
                                      ? VizitColor.primary : VizitColor.controlTrack)
                                .frame(height: 4)
                        }
                    }
                    Text(step == "done" ? "Kész" : "\(index + 1) / \(total)")
                        .font(VizitFont.caption)
                        .foregroundStyle(VizitColor.textMuted)
                        .monospacedDigit()
                }

                Color.clear.frame(width: 44, height: 44)
            }
            .padding(.horizontal, 12)
            .padding(.top, 4)
            .padding(.bottom, 2)
        }
    }

    private var stepHeading: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let eyebrow = copy.eyebrow {
                Text(eyebrow.uppercased())
                    .font(.system(size: 11, weight: .bold))
                    .tracking(1.2)
                    .foregroundStyle(VizitColor.primary)
            }
            Text(copy.title)
                .font(.system(size: 26, weight: .bold))
                .tracking(-0.4)
                .foregroundStyle(VizitColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            if let subtitle = copy.subtitle {
                Text(subtitle)
                    .font(VizitFont.body)
                    .foregroundStyle(VizitColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    @ViewBuilder private var stepBody: some View {
        switch step {
        case "type":
            VStack(spacing: 12) {
                WizardTypeChoice(
                    title: "Vállalkozói névjegy",
                    detail: "Cégnek vagy egyéni vállalkozásnak: vállalkozás neve, beosztás, céges logó, bemutatkozás.",
                    tags: ["8 lépés", "kb. 2 perc"],
                    systemImage: "briefcase",
                    selected: kind == "business"
                ) { selectKind("business") }
                .accessibilityIdentifier("wizard.business")

                WizardTypeChoice(
                    title: "Magánszemély",
                    detail: "Személyes kapcsolatokhoz. Csak a lényeg, gyorsan kész.",
                    tags: ["6 lépés", "kb. 1 perc"],
                    systemImage: "person",
                    selected: kind == "private"
                ) { selectKind("private") }
                .accessibilityIdentifier("wizard.private")
            }

        case "identity":
            WizardBlock {
                VizitTextField(
                    label: "Teljes név *",
                    text: $draft.fullName,
                    placeholder: "Pl. Kovács Anna",
                    contentType: .name,
                    identifier: "wizard.fullName"
                )
                if kind != "private" {
                    VizitTextField(
                        label: "Vállalkozás / szervezet *",
                        text: $draft.company,
                        placeholder: "Pl. RayWorks Solutions",
                        contentType: .organizationName
                    )
                    VizitTextField(
                        label: "Beosztás · nem kötelező",
                        text: $draft.jobTitle,
                        placeholder: "Pl. ügyvezető",
                        contentType: .jobTitle
                    )
                }
            }

        case "photo":
            WizardBlock {
                VStack(spacing: 16) {
                    WizardPhotoAvatar(profile: draft, showLogo: false)
                    PhotosPicker(selection: $photo, matching: .images) {
                        Text(draft.photoBase64.isEmpty ? "Kép kiválasztása" : "Másik kép")
                            .font(VizitFont.label)
                            .frame(minHeight: 44)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(VizitColor.textPrimary)
                    if !draft.photoBase64.isEmpty {
                        Button("Eltávolítás") { draft.photoBase64 = ""; photo = nil }
                            .font(VizitFont.label)
                            .foregroundStyle(VizitColor.primary)
                    }
                    Text("JPG, PNG vagy WebP, legfeljebb 25 MB. A nagy képet automatikusan kisebbre méretezzük.")
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(VizitColor.textMuted)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
            }

        case "logo":
            WizardBlock {
                VStack(spacing: 16) {
                    WizardPhotoAvatar(profile: draft, showLogo: true)
                    PhotosPicker(selection: $logo, matching: .images) {
                        Text(draft.logoBase64.isEmpty ? "Logó feltöltése" : "Logó cseréje")
                            .font(VizitFont.label)
                            .frame(minHeight: 44)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(VizitColor.textPrimary)
                    if !draft.logoBase64.isEmpty {
                        Button("Eltávolítás") { draft.logoBase64 = ""; logo = nil }
                            .font(VizitFont.label)
                            .foregroundStyle(VizitColor.primary)
                    }
                    Text("Átlátszó hátterű PNG mutat a legjobban.")
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(VizitColor.textMuted)
                }
                .frame(maxWidth: .infinity)
            }

        case "contact":
            WizardBlock {
                VizitTextField(label: "Nyilvános e-mail", text: $draft.email, placeholder: "nev@ceg.hu",
                               keyboard: .emailAddress, autocapitalization: .never)
                VizitTextField(label: "Telefonszám", text: $draft.phone, placeholder: "+36 30 123 4567",
                               keyboard: .phonePad)
                VizitTextField(label: "Weboldal", text: $draft.website, placeholder: "ceg.hu",
                               keyboard: .URL, autocapitalization: .never)
                if kind != "private" {
                    VizitTextField(label: "Hely / cím", text: $draft.address, placeholder: "Budapest")
                }
                if !valid {
                    Text("Ez nem tűnik érvényes e-mail-címnek.")
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(VizitColor.error)
                }
            }

        case "social":
            WizardBlock {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 110), spacing: 8)], spacing: 8) {
                    ForEach(SocialPlatform.allCases, id: \.self) { platform in
                        Button {
                            if selectedSocial.contains(platform) {
                                selectedSocial.remove(platform)
                                draft.setSocialURL("", for: platform)
                            } else {
                                selectedSocial.insert(platform)
                            }
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: selectedSocial.contains(platform) ? "checkmark" : "plus")
                                Text(platform.label)
                            }
                            .font(.system(size: 14, weight: selectedSocial.contains(platform) ? .semibold : .medium))
                            .foregroundStyle(selectedSocial.contains(platform) ? VizitColor.primary : VizitColor.textPrimary)
                            .padding(.horizontal, 12)
                            .frame(minHeight: 36)
                            .background(selectedSocial.contains(platform) ? VizitColor.primarySubtle : VizitColor.surface)
                            .overlay(
                                Capsule().stroke(selectedSocial.contains(platform) ? Color.clear : VizitColor.borderStrong, lineWidth: 1)
                            )
                            .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
                ForEach(SocialPlatform.allCases.filter { selectedSocial.contains($0) }, id: \.self) { platform in
                    VizitTextField(
                        label: platform.label,
                        text: Binding(
                            get: { draft.socialURL(for: platform) },
                            set: { draft.setSocialURL($0, for: platform) }
                        ),
                        placeholder: "https://…",
                        keyboard: .URL,
                        autocapitalization: .never
                    )
                }
            }

        case "bio":
            WizardBlock {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Rövid bemutatkozás · nem kötelező")
                        .font(VizitFont.label)
                        .foregroundStyle(VizitColor.textSecondary)
                    TextEditor(text: $draft.bio)
                        .frame(minHeight: 116)
                        .padding(8)
                        .background(VizitColor.canvas)
                        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous)
                                .stroke(VizitColor.border, lineWidth: 1)
                        )
                        .onChange(of: draft.bio) { newValue in
                            if newValue.count > 420 { draft.bio = String(newValue.prefix(420)) }
                        }
                    HStack {
                        Text("Két-három mondat elég.")
                        Spacer()
                        Text("\(draft.bio.count)/420").monospacedDigit()
                    }
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(VizitColor.textMuted)
                }
            }

        case "look":
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible())], spacing: 10) {
                ForEach([CardColorway.ink, .brand, .emerald, .amethyst], id: \.self) { value in
                    WizardStyleSwatch(colorway: value, selected: selectedStyle == value) {
                        selectedStyle = value
                    }
                }
            }

        default:
            VStack(spacing: 16) {
                VizitDigitalCard(profile: draft, presentation: presentation.value.withColorway(selectedStyle))
                WizardBlock {
                    VizitTextField(
                        label: "A névjegyed címe",
                        text: Binding(
                            get: { draft.publicSlug },
                            set: { editedSlug = true; draft.publicSlug = slug($0) }
                        ),
                        placeholder: "vizitkartyam.hu/…",
                        autocapitalization: .never
                    )
                    Text("A név alapján javasoltuk. Később is módosítható.")
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(VizitColor.textMuted)

                    Toggle(isOn: $draft.isPublic) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Nyilvános profil").font(VizitFont.label)
                            Text(draft.isPublic
                                 ? "A névjegy a hivatkozással bárki számára megnyitható."
                                 : "Csak te látod. QR-rel és NFC-vel ettől még átadhatod.")
                                .font(VizitFont.bodySmall)
                                .foregroundStyle(VizitColor.textSecondary)
                        }
                    }
                    .tint(VizitColor.success)
                }

                let missing = path.filter { skipped.contains($0) }
                if !missing.isEmpty {
                    WizardBlock {
                        VStack(alignment: .leading, spacing: 0) {
                            Text("KÉSŐBB BEÁLLÍTHATOD")
                                .font(.system(size: 11, weight: .bold))
                                .tracking(1.1)
                                .foregroundStyle(VizitColor.textMuted)
                                .padding(.bottom, 4)
                            ForEach(missing, id: \.self) { item in
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(titles[item] ?? item).font(VizitFont.body)
                                        Text(todoHint(item)).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)
                                    }
                                    Spacer()
                                    Button("Beállítás") {
                                        direction = -1
                                        index = path.firstIndex(of: item) ?? index
                                        returning = true
                                    }
                                    .font(VizitFont.label)
                                    .foregroundStyle(VizitColor.textPrimary)
                                    .padding(.horizontal, 12)
                                    .frame(minHeight: 38)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10)
                                            .stroke(VizitColor.borderStrong, lineWidth: 1)
                                    )
                                }
                                .padding(.vertical, 8)
                            }
                        }
                    }
                }
            }
        }
    }

    private var wizardFooter: some View {
        VStack(spacing: 4) {
            VizitButton(
                title: step == "done"
                    ? (draft.isPublic ? "Névjegy publikálása" : "Névjegy mentése")
                    : (returning ? "Mentés" : "Tovább"),
                isLoading: processing,
                isEnabled: valid && (!optional.contains(step) || hasValue)
            ) {
                if step == "done" { save() }
                else { advance(skip: false) }
            }
            .accessibilityIdentifier("wizard.primary")

            if optional.contains(step) {
                VizitButton(title: "Később állítom be", kind: .tertiary) {
                    advance(skip: true)
                }
                .accessibilityIdentifier("wizard.skip")
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
        .padding(.bottom, 16)
        .background(VizitColor.canvas)
        .overlay(alignment: .top) { Rectangle().fill(VizitColor.divider).frame(height: 1) }
    }

    private var publishedContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Image(systemName: "checkmark")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(VizitColor.success)
                    .frame(width: 54, height: 54)
                    .background(VizitColor.successSubtle)
                    .clipShape(Circle())
                Text("Publikálva")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundStyle(VizitColor.textPrimary)
                Text(draft.isPublic
                     ? "A névjegyed él. Oszd meg a címét, vagy mutasd a QR-kódot."
                     : "A névjegyed elkészült. A nyilvános profilt a Beállításokban kapcsolhatod be.")
                    .font(VizitFont.body)
                    .foregroundStyle(VizitColor.textSecondary)
                VizitDigitalCard(profile: draft, presentation: presentation.value.withColorway(selectedStyle))
                VizitButton(title: "Tovább az áttekintéshez") { onFinished() }
                    .accessibilityIdentifier("wizard.finish")
            }
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
            .padding(20)
        }
    }

    private func todoHint(_ id: String) -> String {
        switch id {
        case "photo": return "A névjegy most monogramot mutat."
        case "logo": return "A logó a profilkép sarkában jelenik meg."
        case "contact": return "Még nincs nyilvános elérhetőséged."
        case "social": return "LinkedIn, Instagram és a többi."
        case "bio": return "Pár mondat a nyilvános profilra."
        default: return ""
        }
    }

    private func selectKind(_ value: String) {
        kind = value
        if value == "private" {
            draft.company = ""
            draft.jobTitle = ""
            draft.bio = ""
            draft.logoBase64 = ""
        }
        error = nil
    }

    private func back() {
        direction = -1
        if returning {
            returning = false
            index = path.count - 1
        } else {
            index = max(0, index - 1)
        }
        error = nil
    }

    private func advance(skip: Bool) {
        if skip {
            switch step {
            case "photo": draft.photoBase64 = ""; photo = nil
            case "logo": draft.logoBase64 = ""; logo = nil
            case "contact": draft.email = ""; draft.phone = ""; draft.website = ""; draft.address = ""
            case "social":
                for platform in SocialPlatform.allCases { draft.setSocialURL("", for: platform) }
                selectedSocial.removeAll()
            case "bio": draft.bio = ""
            default: break
            }
        }
        if skip { skipped.insert(step) } else { skipped.remove(step) }
        direction = 1
        index = returning ? path.count - 1 : min(index + 1, path.count - 1)
        returning = false
        error = nil
        if step == "done", !editedSlug {
            draft.publicSlug = slug(kind == "business" ? draft.company : draft.displayName)
        }
    }

    private func slug(_ value: String) -> String {
        let plain = value.applyingTransform(.stripDiacritics, reverse: false)?.lowercased() ?? value.lowercased()
        return String(
            plain.replacingOccurrences(of: "[^a-z0-9]+", with: "-", options: .regularExpression)
                .trimmingCharacters(in: CharacterSet(charactersIn: "-"))
                .prefix(50)
        )
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
        guard let prepared = normalizeURLs() else {
            error = "A webes és közösségi hivatkozások teljes, https:// kezdetű címek legyenek."
            return
        }
        onSaving()
        do {
            var cardPresentation = presentation.value
            cardPresentation.colorway = selectedStyle
            if isAdditional {
                try store.createBusinessCard(prepared, presentation: cardPresentation)
            } else {
                try store.save(prepared)
                store.updateActiveCardPresentation(cardPresentation)
            }
            presentation.value = cardPresentation
            published = true
        } catch {
            onSaveFailed()
            self.error = error.localizedDescription
        }
    }

    private func process(_ selection: PhotosPickerItem?, isLogo: Bool) async {
        guard let selection else { return }
        processing = true
        defer { processing = false }
        do {
            guard
                let bytes = try await selection.loadTransferable(type: Data.self),
                bytes.count <= 25 * 1024 * 1024,
                let source = CGImageSourceCreateWithData(bytes as CFData, nil),
                let thumb = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                    kCGImageSourceCreateThumbnailFromImageAlways: true,
                    kCGImageSourceCreateThumbnailWithTransform: true,
                    kCGImageSourceThumbnailMaxPixelSize: 512
                ] as CFDictionary),
                let jpeg = UIImage(cgImage: thumb).jpegData(compressionQuality: 0.8),
                jpeg.count <= 256 * 1024
            else { throw ProfileError.invalidPhoto }

            try Task.checkCancellation()
            if isLogo { draft.logoBase64 = jpeg.base64EncodedString() }
            else { draft.photoBase64 = jpeg.base64EncodedString() }
        } catch is CancellationError {
            // A newer selection owns the preview.
        } catch {
            self.error = "A kép betöltése nem sikerült. Válassz másik képet."
        }
    }
}

private struct WizardBlock<Content: View>: View {
    @ViewBuilder let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            content
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(VizitColor.surface)
        .overlay(
            RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous)
                .stroke(VizitColor.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
    }
}

private struct WizardTypeChoice: View {
    let title: String
    let detail: String
    let tags: [String]
    let systemImage: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: systemImage)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(VizitColor.primary)
                    .frame(width: 44, height: 44)
                    .background(VizitColor.primarySubtle)
                    .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md))

                VStack(alignment: .leading, spacing: 4) {
                    Text(title).font(.system(size: 17, weight: .semibold))
                    Text(detail)
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(VizitColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                    HStack(spacing: 6) {
                        ForEach(tags, id: \.self) { tag in
                            Text(tag)
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundStyle(VizitColor.textSecondary)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(VizitColor.sunken)
                                .clipShape(Capsule())
                        }
                    }
                    .padding(.top, 6)
                }

                Spacer(minLength: 4)

                Image(systemName: selected ? "checkmark" : "")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 22, height: 22)
                    .background(selected ? VizitColor.primary : Color.clear)
                    .overlay(Circle().stroke(selected ? VizitColor.primary : VizitColor.borderStrong, lineWidth: 1.5))
                    .clipShape(Circle())
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .foregroundStyle(VizitColor.textPrimary)
            .background(VizitColor.surface)
            .overlay(
                RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous)
                    .stroke(selected ? VizitColor.primary : VizitColor.border, lineWidth: selected ? 2 : 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
            .scaleEffect(selected ? 0.995 : 1)
        }
        .buttonStyle(.plain)
        .animation(.easeOut(duration: 0.15), value: selected)
    }
}

private struct WizardCompactCard: View {
    let profile: ContactProfile
    let colorway: CardColorway
    let business: Bool

    private var subtitle: String {
        business ? [profile.jobTitle, profile.company].filter { !$0.isEmpty }.joined(separator: " · ") : ""
    }

    var body: some View {
        ZStack(alignment: .leading) {
            LinearGradient(
                colors: [
                    Color(uiColor: UIColor(hex: colorway.gradient.start)),
                    Color(uiColor: UIColor(hex: colorway.gradient.end))
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            Rectangle()
                .fill(Color(uiColor: UIColor(hex: colorway.accent)))
                .frame(width: 4)

            HStack(spacing: 14) {
                WizardMiniAvatar(profile: profile)
                VStack(alignment: .leading, spacing: 3) {
                    Text(profile.displayName.isEmpty ? "A neved" : profile.displayName)
                        .font(VizitFont.h3)
                        .foregroundStyle(.white)
                        .lineLimit(1)
                    if !subtitle.isEmpty {
                        Text(subtitle)
                            .font(VizitFont.bodySmall)
                            .foregroundStyle(.white.opacity(0.68))
                            .lineLimit(1)
                    }
                    Spacer(minLength: 4)
                    if !profile.phone.isEmpty {
                        Text(profile.phone).font(VizitFont.caption).foregroundStyle(.white.opacity(0.82))
                    }
                    if !profile.email.isEmpty {
                        Text(profile.email)
                            .font(VizitFont.caption)
                            .foregroundStyle(.white.opacity(0.82))
                            .lineLimit(1)
                    }
                }
                Spacer(minLength: 4)
                VStack {
                    Spacer()
                    Text("VIZIT")
                        .font(.system(size: 11, weight: .bold))
                        .tracking(1.1)
                        .foregroundStyle(Color(uiColor: UIColor(hex: colorway.accent)))
                }
            }
            .padding(.leading, 20)
            .padding(.trailing, 16)
            .padding(.vertical, 14)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 112)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
        .vizitShadow(VizitElevation.card)
    }
}

private struct WizardMiniAvatar: View {
    let profile: ContactProfile

    var body: some View {
        ZStack {
            Circle().fill(Color.white.opacity(0.12))
            Circle().stroke(Color.white.opacity(0.22), lineWidth: 1)
            if let data = Data(base64Encoded: profile.photoBase64), let image = UIImage(data: data) {
                Image(uiImage: image).resizable().scaledToFill().clipShape(Circle())
            } else {
                Text(profile.initials.isEmpty ? "V" : profile.initials)
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(.white.opacity(0.92))
            }
        }
        .frame(width: 44, height: 44)
        .overlay(alignment: .bottomTrailing) {
            if let data = Data(base64Encoded: profile.logoBase64), let image = UIImage(data: data) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .padding(2)
                    .frame(width: 18, height: 18)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 4))
            }
        }
    }
}

private struct WizardPhotoAvatar: View {
    let profile: ContactProfile
    let showLogo: Bool

    var body: some View {
        ZStack {
            Circle()
                .fill(VizitColor.primarySubtle)
                .frame(width: 132, height: 132)
            if let data = Data(base64Encoded: profile.photoBase64), let image = UIImage(data: data) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 132, height: 132)
                    .clipShape(Circle())
            } else {
                Text(profile.initials.isEmpty ? "V" : profile.initials)
                    .font(.system(size: 40, weight: .bold))
                    .foregroundStyle(VizitColor.primary)
            }

            if showLogo {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(VizitColor.surface)
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(VizitColor.borderStrong, style: StrokeStyle(lineWidth: 1.5, dash: [5]))
                    if let data = Data(base64Encoded: profile.logoBase64), let image = UIImage(data: data) {
                        Image(uiImage: image).resizable().scaledToFit().padding(5)
                    } else {
                        Image(systemName: "plus").foregroundStyle(VizitColor.textMuted)
                    }
                }
                .frame(width: 50, height: 50)
                .offset(x: 48, y: 48)
            }
        }
        .frame(width: 148, height: 148)
    }
}

private struct WizardStyleSwatch: View {
    let colorway: CardColorway
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 10) {
                LinearGradient(
                    colors: [
                        Color(uiColor: UIColor(hex: colorway.gradient.start)),
                        Color(uiColor: UIColor(hex: colorway.gradient.end))
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .frame(height: 56)
                .overlay(alignment: .leading) {
                    Rectangle().fill(Color(uiColor: UIColor(hex: colorway.accent))).frame(width: 4)
                }
                .clipShape(RoundedRectangle(cornerRadius: 10))

                HStack {
                    Text(colorway.label).font(VizitFont.label)
                    Spacer()
                    if colorway == .ink {
                        Text("Alap").font(.system(size: 11, weight: .semibold)).foregroundStyle(VizitColor.textMuted)
                    }
                }
            }
            .padding(10)
            .foregroundStyle(VizitColor.textPrimary)
            .background(VizitColor.surface)
            .overlay(
                RoundedRectangle(cornerRadius: VizitRadius.lg)
                    .stroke(selected ? VizitColor.primary : VizitColor.border, lineWidth: selected ? 2 : 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg))
        }
        .buttonStyle(.plain)
    }
}

private extension CardPresentation {
    func withColorway(_ colorway: CardColorway) -> CardPresentation {
        var value = self
        value.colorway = colorway
        return value
    }
}
