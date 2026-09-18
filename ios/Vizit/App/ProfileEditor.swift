import SwiftUI
import PhotosUI
import ImageIO

struct ProfileEditor: View {
    private enum Field: Hashable {
        case fullName, lastName, firstName, phone, email, website
        case company, jobTitle, address, linkedIn, facebook, instagram, tiktok, youtube, publicSlug
    }

    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss
    @State var draft: ContactProfile
    @State private var photo: PhotosPickerItem?
    @State private var error: String?
    @State private var loadingPhoto = false
    @FocusState private var focusedField: Field?

    var body: some View {
        NavigationStack {
            Form {
                Section("Profilkép") {
                    HStack(spacing: 20) {
                        ProfileAvatar(profile: draft)
                        VStack(alignment: .leading, spacing: 10) {
                            PhotosPicker(selection: $photo, matching: .images) {
                                Label("Kép kiválasztása", systemImage: "photo")
                            }
                            .disabled(loadingPhoto)
                            if !draft.photoBase64.isEmpty {
                                Button("Kép eltávolítása", role: .destructive) {
                                    photo = nil
                                    draft.photoBase64 = ""
                                }
                            }
                            if loadingPhoto { ProgressView("Kép feldolgozása…") }
                        }
                    }
                }
                Section("Név") {
                    TextField("Megjelenített név", text: $draft.fullName)
                        .focused($focusedField, equals: .fullName)
                        .textContentType(.name).accessibilityIdentifier("profile.fullName")
                    TextField("Vezetéknév", text: $draft.lastName)
                        .focused($focusedField, equals: .lastName).textContentType(.familyName)
                    TextField("Keresztnév", text: $draft.firstName)
                        .focused($focusedField, equals: .firstName).textContentType(.givenName)
                }
                Section("Elérhetőségek") {
                    TextField("Telefonszám", text: $draft.phone)
                        .focused($focusedField, equals: .phone)
                        .keyboardType(.phonePad).textContentType(.telephoneNumber)
                        .accessibilityIdentifier("profile.phone")
                    TextField("E-mail-cím", text: $draft.email)
                        .focused($focusedField, equals: .email)
                        .keyboardType(.emailAddress).textContentType(.emailAddress)
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                        .accessibilityIdentifier("profile.email")
                    TextField("Weboldal – https://…", text: $draft.website)
                        .focused($focusedField, equals: .website)
                        .keyboardType(.URL).textContentType(.URL)
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                }
                Section("Munkahely") {
                    TextField("Cég", text: $draft.company)
                        .focused($focusedField, equals: .company).textContentType(.organizationName)
                    TextField("Beosztás", text: $draft.jobTitle)
                        .focused($focusedField, equals: .jobTitle).textContentType(.jobTitle)
                    TextField("Cím", text: $draft.address)
                        .focused($focusedField, equals: .address).textContentType(.fullStreetAddress)
                }
                Section("Közösségi média") {
                    TextField("LinkedIn – https://…", text: $draft.linkedIn)
                        .focused($focusedField, equals: .linkedIn)
                        .keyboardType(.URL).textInputAutocapitalization(.never).autocorrectionDisabled()
                    TextField("Facebook – https://…", text: $draft.facebook)
                        .focused($focusedField, equals: .facebook)
                        .keyboardType(.URL).textInputAutocapitalization(.never).autocorrectionDisabled()
                    TextField("Instagram – https://…", text: $draft.instagram)
                        .focused($focusedField, equals: .instagram)
                        .keyboardType(.URL).textInputAutocapitalization(.never).autocorrectionDisabled()
                    TextField("TikTok – https://…", text: $draft.tiktok)
                        .focused($focusedField, equals: .tiktok)
                        .keyboardType(.URL).textInputAutocapitalization(.never).autocorrectionDisabled()
                    TextField("YouTube – https://…", text: $draft.youtube)
                        .focused($focusedField, equals: .youtube)
                        .keyboardType(.URL).textInputAutocapitalization(.never).autocorrectionDisabled()
                    Text("A megadott profilok a VIZIT-névjeggyel együtt szinkronizálódnak és kerülnek átadásra.")
                        .font(.footnote).foregroundStyle(.secondary)
                }
                Section("Nyilvános VIZIT-profil") {
                    Toggle("Nyilvános profil engedélyezése", isOn: $draft.isPublic)
                    TextField("Profilazonosító – pl. kovacs-anna", text: $draft.publicSlug)
                        .focused($focusedField, equals: .publicSlug)
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                    if draft.publicSlug.isEmpty {
                        Text("Az első felhőmentés biztonságos, egyedi azonosítót készít. Ezután itt módosíthatod.")
                            .font(.footnote).foregroundStyle(.secondary)
                    } else {
                        Text("A nyilvános cím megváltoztatása a korábban megosztott hivatkozásokat érvénytelenné teheti.")
                            .font(.footnote).foregroundStyle(.secondary)
                    }
                }
                Section {
                    Text("Először titkosított helyi fájlba mentünk, majd bejelentkezve szinkronizálunk. Mások QR-kódon, megosztott fájlon vagy az általad engedélyezett nyilvános profilon kapják meg az adatokat.")
                        .font(.footnote).foregroundStyle(.secondary)
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle("Névjegy szerkesztése").navigationBarTitleDisplayMode(.inline)
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
                    Button("Kész") { focusedField = nil }
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
}
