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
    @State private var error: String?
    @State private var loadingPhoto = false

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.xl) {
                        photoSection
                        personalSection
                        workSection
                        contactSection
                        socialSection
                        sharingSection
                        storageNote
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

                VizitTextField(
                    label: "Profilazonosító",
                    text: $draft.publicSlug,
                    placeholder: "pl. kovacs-anna",
                    helper: draft.publicSlug.isEmpty
                        ? "Az első felhőmentés biztonságos, egyedi azonosítót készít. Ezután itt módosíthatod."
                        : "A nyilvános cím megváltoztatása a korábban megosztott hivatkozásokat érvénytelenné teheti.",
                    autocapitalization: .never,
                    submitLabel: .done
                )
            }
        }
    }

    private var storageNote: some View {
        Text("Először titkosított helyi fájlba mentünk, majd bejelentkezve szinkronizálunk. Mások QR-kódon, megosztott fájlon vagy az általad engedélyezett nyilvános profilon kapják meg az adatokat.")
            .font(VizitFont.bodySmall)
            .foregroundStyle(VizitColor.textMuted)
            .fixedSize(horizontal: false, vertical: true)
            .padding(.horizontal, VizitSpace.xxs)
    }
}
