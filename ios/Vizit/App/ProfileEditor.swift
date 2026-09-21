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
    @EnvironmentObject private var presentation: CardPresentationStore
    @Environment(\.dismiss) private var dismiss
    @State var draft: ContactProfile
    @State private var photo: PhotosPickerItem?
    @State private var error: String?
    @State private var loadingPhoto = false
    @State private var companyLogo: PhotosPickerItem?
    @State private var loadingCompanyLogo = false
    @State private var showSectionOrder = false

    private var defaultProfileAddress: String {
        guard !draft.publicSlug.isEmpty,
              let base = store.configuration?.publicProfileBaseURL,
              let url = PublicProfileLink.make(baseURL: base, slug: draft.publicSlug) else {
            return "Az egyedi azonosítót az első sikeres mentéskor automatikusan létrehozzuk."
        }
        return displayAddress(url.absoluteString)
    }

    private var activeSharingAddress: String {
        if draft.customDomainVerified,
           CustomProfileDomain.isValid(draft.customDomain) {
            return displayAddress(draft.customDomain)
        }
        return defaultProfileAddress
    }

    private var domainValidationError: String? {
        guard !draft.customDomain.isEmpty,
              !CustomProfileDomain.isValid(draft.customDomain) else { return nil }
        return "Csak a hostnevet add meg, útvonal és https:// nélkül. Például: nevjegy.cegem.hu"
    }

    private var domainState: DomainState {
        if draft.customDomain.isEmpty { return .defaultAddress }
        if domainValidationError != nil { return .invalid }
        return draft.customDomainVerified ? .verified : .pending
    }

    private func displayAddress(_ value: String) -> String {
        value
            .replacingOccurrences(of: "https://", with: "")
            .replacingOccurrences(of: "http://", with: "")
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }

    private func socialValidationError(_ platform: SocialPlatform, value: String) -> String? {
        guard !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              !platform.isValidProfileURL(value) else { return nil }
        return "Teljes https://\(platform.label.lowercased()).com profilhivatkozást adj meg."
    }

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.xl) {
                        photoSection
                        personalSection
                        workSection
                        companyLogoSection
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
        }
    }

            .task(id: companyLogo) {
                guard let selected = companyLogo else { loadingCompanyLogo = false; return }
                loadingCompanyLogo = true
                defer { if companyLogo == selected { loadingCompanyLogo = false } }
                do {
                    guard let bytes = try await selected.loadTransferable(type: Data.self),
                          bytes.count <= 15 * 1024 * 1024,
                          let source = CGImageSourceCreateWithData(bytes as CFData, nil),
                          let thumbnail = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                            kCGImageSourceCreateThumbnailFromImageAlways: true,
                            kCGImageSourceCreateThumbnailWithTransform: true,
                            kCGImageSourceThumbnailMaxPixelSize: 384
                          ] as CFDictionary),
                          let png = UIImage(cgImage: thumbnail).pngData(),
                          png.count <= 384 * 1024 else { throw ProfileError.invalidPhoto }
                    try Task.checkCancellation()
                    presentation.value.companyLogoBase64 = png.base64EncodedString()
                } catch is CancellationError {
                } catch {
                    self.error = "A céges logó betöltése nem sikerült. Próbálj másik képet választani."
                }
            }
            .sheet(isPresented: $showSectionOrder) {
                CardSectionOrderScreen(store: presentation)
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

    private var companyLogoSection: some View {
        group("Céges logó") {
            VizitPanel {
                HStack(spacing: VizitSpace.md) {
                    ZStack {
                        RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous)
                            .fill(VizitColor.sunken)
                        if let data = Data(base64Encoded: presentation.value.companyLogoBase64),
                           let image = UIImage(data: data) {
                            Image(uiImage: image)
                                .resizable()
                                .scaledToFit()
                                .padding(VizitSpace.xs)
                        } else {
                            Image(systemName: "building.2")
                                .font(.system(size: 24, weight: .medium))
                                .foregroundStyle(VizitColor.textMuted)
                        }
                    }
                    .frame(width: 72, height: 72)

                    VStack(alignment: .leading, spacing: VizitSpace.xs) {
                        PhotosPicker(selection: $companyLogo, matching: .images) {
                            Label("Logó kiválasztása", systemImage: "photo")
                                .font(VizitFont.label)
                                .foregroundStyle(VizitColor.primary)
                        }
                        .disabled(loadingCompanyLogo)
                        .frame(minHeight: VizitMetrics.minTouchTarget, alignment: .leading)

                        if !presentation.value.companyLogoBase64.isEmpty {
                            Button("Logó eltávolítása") {
                                companyLogo = nil
                                presentation.value.companyLogoBase64 = ""
                            }
                            .font(VizitFont.label)
                            .foregroundStyle(VizitColor.error)
                            .frame(minHeight: VizitMetrics.minTouchTarget, alignment: .leading)
                        }

                        if loadingCompanyLogo {
                            HStack(spacing: VizitSpace.xs) {
                                ProgressView().controlSize(.small)
                                Text("Logó feldolgozása…")
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
                    systemImage: SocialPlatform.linkedin.systemImage,
                    placeholder: "https://…",
                    error: socialValidationError(.linkedin, value: draft.linkedIn),
                    keyboard: .URL,
                    autocapitalization: .never,
                    submitLabel: .next
                )

                VizitTextField(
                    label: "Facebook",
                    text: $draft.facebook,
                    systemImage: SocialPlatform.facebook.systemImage,
                    placeholder: "https://…",
                    error: socialValidationError(.facebook, value: draft.facebook),
                    keyboard: .URL,
                    autocapitalization: .never,
                    submitLabel: .next
                )

                VizitTextField(
                    label: "Instagram",
                    text: $draft.instagram,
                    systemImage: SocialPlatform.instagram.systemImage,
                    placeholder: "https://…",
                    error: socialValidationError(.instagram, value: draft.instagram),
                    keyboard: .URL,
                    autocapitalization: .never,
                    submitLabel: .next
                )

                VizitTextField(
                    label: "TikTok",
                    text: $draft.tiktok,
                    systemImage: SocialPlatform.tiktok.systemImage,
                    placeholder: "https://…",
                    error: socialValidationError(.tiktok, value: draft.tiktok),
                    keyboard: .URL,
                    autocapitalization: .never,
                    submitLabel: .next
                )

                VizitTextField(
                    label: "YouTube",
                    text: $draft.youtube,
                    systemImage: SocialPlatform.youtube.systemImage,
                    placeholder: "https://…",
                    error: socialValidationError(.youtube, value: draft.youtube),
                    keyboard: .URL,
                    autocapitalization: .never,
                    submitLabel: .next
                )

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
                            Text(defaultProfileAddress)
                                .font(VizitFont.bodySmall)
                                .foregroundStyle(VizitColor.textMuted)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }

                    VizitPanel {
                        VStack(alignment: .leading, spacing: VizitSpace.sm) {
                            VizitStatusPill(text: domainState.badge, tone: domainState.tone)
                                .accessibilityIdentifier("profile.domain.status")

                            VizitTextField(
                                label: "Egyedi domain (nem kötelező)",
                                text: Binding(
                                    get: { draft.customDomain },
                                    set: {
                                        draft.customDomain = $0
                                        draft.customDomainVerified = false
                                    }
                                ),
                                placeholder: "pl. nevjegy.cegem.hu",
                                error: domainValidationError,
                                keyboard: .URL,
                                autocapitalization: .never,
                                identifier: "profile.customDomain",
                                submitLabel: .done
                            )

                            if let banner = domainState.banner {
                                VizitBanner(text: banner, tone: domainState.tone)
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: VizitSpace.xs) {
                        Text(domainState.heading)
                            .font(VizitFont.h3)
                            .foregroundStyle(VizitColor.textPrimary)
                        Text(domainState.detail)
                            .font(VizitFont.bodySmall)
                            .foregroundStyle(VizitColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    VizitPanel {
                        VizitRow(
                            label: "Szekciók sorrendje",
                            systemImage: "line.3.horizontal.decrease",
                            supporting: "A névjegy blokkjainak sorrendje"
                        ) { showSectionOrder = true }
                    }

                    VizitBanner(
                        text: "A QR és az NFC ezt adja: \(activeSharingAddress)",
                        tone: .info
                    )
                    .accessibilityIdentifier("profile.domain.activeLink")
                }
            }
        }
    }

}

private struct CardSectionOrderScreen: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var store: CardPresentationStore

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(store.value.sectionOrder) { section in
                        HStack(spacing: VizitSpace.sm) {
                            Image(systemName: "line.3.horizontal")
                                .foregroundStyle(VizitColor.textMuted)
                            Text(section.label)
                                .font(VizitFont.body)
                            Spacer()
                        }
                        .frame(minHeight: VizitMetrics.minTouchTarget)
                    }
                    .onMove { indices, newOffset in
                        store.value.sectionOrder.move(fromOffsets: indices, toOffset: newOffset)
                    }
                } footer: {
                    Text("Húzd a blokkokat a kívánt sorrendbe. A sorrend csak a megjelenést módosítja.")
                }
            }
            .environment(\.editMode, .constant(.active))
            .navigationTitle("Szekciók sorrendje")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Kész") { dismiss() }
                }
            }
        }
    }
}

private enum DomainState {
    case defaultAddress, pending, verified, invalid

    var badge: String {
        switch self {
        case .defaultAddress: return "ALAPÉRTELMEZETT"
        case .pending: return "FÜGGŐBEN"
        case .verified: return "ÉLES"
        case .invalid: return "HIBA"
        }
    }

    var tone: VizitTone {
        switch self {
        case .defaultAddress: return .info
        case .pending: return .warning
        case .verified: return .success
        case .invalid: return .error
        }
    }

    var banner: String? {
        switch self {
        case .defaultAddress:
            return nil
        case .pending:
            return "Ellenőrzésre vár. Add hozzá a domaint a szolgáltatónál, majd a DNS-ellenőrzés után kapcsoljuk élesbe."
        case .verified:
            return "Ellenőrzött domain · ezt használja a QR és az NFC."
        case .invalid:
            return "Csak a hostnevet add meg, útvonal és https:// nélkül. Például: nevjegy.cegem.hu"
        }
    }

    var heading: String {
        switch self {
        case .defaultAddress: return "Nincs megadva"
        case .pending: return "Megadva, ellenőrzésre vár"
        case .verified: return "Ellenőrzött"
        case .invalid: return "Formailag hibás"
        }
    }

    var detail: String {
        switch self {
        case .defaultAddress:
            return "A profil a VIZIT saját címén él. A mező üres, a felirat megmondja, hogy nem kötelező kitölteni."
        case .pending:
            return "A mentés megtörtént, de a tulajdonjog még nincs igazolva. A QR ilyenkor nem vált át — így nem keletkezik halott hivatkozás."
        case .verified:
            return "A következő sikeres szinkron után a mobil QR és NFC is a saját címet adja át. A VIZIT.hu-cím továbbra is működik."
        case .invalid:
            return "A hibaüzenet megmutatja a helyes formátumot is, nem csak azt, hogy érvénytelen."
        }
    }
}
