import SwiftUI

private enum AuthMode: String, CaseIterable, Identifiable {
    case login = "Belépés"
    case register = "Regisztráció"
    var id: String { rawValue }
}

struct AuthScreen: View {
    @EnvironmentObject private var store: AppStore
    @State private var mode: AuthMode = .login
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var confirmation = ""
    @State private var legalAccepted = false
    @State private var forgotPassword = false
    @State private var passwordResetSent = false
    @State private var showPassword = false
    @State private var showConfirmation = false

    var body: some View {
        NavigationStack {
            ZStack {
                Brand.heroGradient.ignoresSafeArea()
                Circle()
                    .fill(Brand.cyan.opacity(0.16))
                    .frame(width: 330, height: 330)
                    .blur(radius: 40)
                    .offset(x: 185, y: -360)
                Circle()
                    .fill(Brand.blue.opacity(0.22))
                    .frame(width: 280, height: 280)
                    .blur(radius: 48)
                    .offset(x: -190, y: 390)

                ScrollView {
                    VStack(spacing: 22) {
                        VizitBrandLockup(height: 106)
                            .padding(.top, 10)

                        VStack(spacing: 8) {
                            Text(mode == .login ? "Üdv újra!" : "Csatlakozz a VIZIT-hez")
                                .font(.system(size: 31, weight: .bold, design: .rounded))
                                .foregroundStyle(.white)
                                .multilineTextAlignment(.center)
                            Text(mode == .login
                                 ? "A digitális névjegyed mindig veled van."
                                 : "Hozd létre a fiókod, majd állítsd össze a névjegyed.")
                                .font(.subheadline)
                                .foregroundStyle(.white.opacity(0.72))
                                .multilineTextAlignment(.center)
                        }

                        authCard

                        Label("Titkosított munkamenet az iPhone-kulcstárban",
                              systemImage: "lock.shield.fill")
                            .font(.caption.weight(.medium))
                            .foregroundStyle(.white.opacity(0.66))
                            .multilineTextAlignment(.center)
                        Text(buildVersionLabel)
                            .font(.caption2.monospacedDigit())
                            .foregroundStyle(.white.opacity(0.46))
                            .accessibilityIdentifier("auth.version")
                            .padding(.bottom, 24)
                    }
                    .padding(.horizontal, 20)
                    .frame(maxWidth: 540)
                    .frame(maxWidth: .infinity)
                }
                .scrollDismissesKeyboard(.interactively)
            }
            .toolbar(.hidden, for: .navigationBar)
            .sheet(isPresented: $forgotPassword) { passwordResetSheet }
        }
        .preferredColorScheme(.dark)
    }

    private var authCard: some View {
        VStack(spacing: 18) {
            modeSelector

            if let address = verificationAddress {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "envelope.badge.shield.half.filled")
                        .font(.title3)
                        .foregroundStyle(Brand.cyan)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Ellenőrizd az e-mailjeidet")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.white)
                        Text("Ha ez új cím, megerősítő linket küldtünk ide: \(address). Ha már van fiókod, lépj be vagy kérj új jelszót.")
                            .font(.caption)
                            .foregroundStyle(.white.opacity(0.72))
                    }
                    Spacer(minLength: 0)
                }
                .padding(14)
                .background(Brand.cyan.opacity(0.10))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Brand.cyan.opacity(0.3), lineWidth: 1)
                }
            }

            VStack(spacing: 14) {
                if mode == .register {
                    authField(title: "Név", icon: "person.fill") {
                        TextField("Teljes név", text: $name,
                                  prompt: Text("Teljes név").foregroundColor(.white.opacity(0.42)))
                            .textContentType(.name)
                            .submitLabel(.next)
                            .accessibilityIdentifier("auth.name")
                    }
                }

                authField(title: "E-mail-cím", icon: "envelope.fill") {
                    TextField("E-mail-cím", text: $email,
                              prompt: Text("nev@pelda.hu").foregroundColor(.white.opacity(0.42)))
                        .textContentType(.username)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .submitLabel(.next)
                        .accessibilityIdentifier("auth.email")
                }

                passwordField(
                    title: "Jelszó",
                    text: $password,
                    revealed: $showPassword,
                    contentType: mode == .login ? .password : .newPassword,
                    submitLabel: mode == .login ? .go : .next,
                    identifier: "auth.password"
                )

                if mode == .register {
                    passwordField(
                        title: "Jelszó újra",
                        text: $confirmation,
                        revealed: $showConfirmation,
                        contentType: .newPassword,
                        submitLabel: .go,
                        identifier: "auth.confirmation"
                    )
                    Text("Legalább 8 karakter, kis- és nagybetű, valamint szám szükséges.")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.58))
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }

            if mode == .register { legalSection }

            Button(action: submit) {
                VizitPrimaryButtonLabel(
                    title: mode == .login ? "Belépés" : "Fiók létrehozása",
                    systemImage: mode == .login ? "arrow.right" : "person.badge.plus",
                    busy: store.busy
                )
            }
            .buttonStyle(.plain)
            .disabled(store.busy)
            .opacity(store.busy ? 0.72 : 1)
            .accessibilityIdentifier("auth.submit")

            if mode == .login {
                if store.configuration?.googleSignInEnabled == true {
                    Button {
                        Task { await store.googleLogin() }
                    } label: {
                        Label("Folytatás Google-fiókkal", systemImage: "g.circle.fill")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(Color.white.opacity(0.08))
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .overlay {
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .stroke(Color.white.opacity(0.16), lineWidth: 1)
                            }
                    }
                    .buttonStyle(.plain)
                    .disabled(store.busy)
                }

                Button("Elfelejtettem a jelszavam") {
                    passwordResetSent = false
                    forgotPassword = true
                }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Brand.cyanLight)
                    .disabled(store.busy)
                    .accessibilityIdentifier("auth.forgotPassword")
            }
        }
        .padding(20)
        .background(Color(red: 4 / 255, green: 19 / 255, blue: 49 / 255).opacity(0.92))
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .stroke(Color.white.opacity(0.14), lineWidth: 1)
        }
        .shadow(color: Color.black.opacity(0.24), radius: 28, y: 16)
    }

    private var modeSelector: some View {
        HStack(spacing: 5) {
            ForEach(AuthMode.allCases) { item in
                Button {
                    withAnimation(.easeOut(duration: 0.2)) {
                        mode = item
                        password = ""
                        confirmation = ""
                    }
                } label: {
                    Text(item.rawValue)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(mode == item ? Color.white : Color.white.opacity(0.58))
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .background(mode == item ? Brand.blue : Color.clear)
                        .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(Color.white.opacity(0.07))
        .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
    }

    private func authField<Content: View>(
        title: String,
        icon: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.white.opacity(0.76))
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .foregroundStyle(Brand.cyanLight)
                    .frame(width: 20)
                content()
                    .foregroundStyle(.white)
                    .tint(Brand.cyan)
            }
            .padding(.horizontal, 15)
            .frame(minHeight: 54)
            .background(Color.white.opacity(0.075))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color.white.opacity(0.16), lineWidth: 1)
            }
        }
    }

    private func passwordField(
        title: String,
        text: Binding<String>,
        revealed: Binding<Bool>,
        contentType: UITextContentType,
        submitLabel: SubmitLabel,
        identifier: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.white.opacity(0.76))
            HStack(spacing: 12) {
                Image(systemName: "lock.fill")
                    .foregroundStyle(Brand.cyanLight)
                    .frame(width: 20)
                Group {
                    if revealed.wrappedValue {
                        TextField(title, text: text,
                                  prompt: Text("••••••••").foregroundColor(.white.opacity(0.42)))
                            .accessibilityIdentifier(identifier)
                    } else {
                        SecureField(title, text: text,
                                    prompt: Text("••••••••").foregroundColor(.white.opacity(0.42)))
                            .accessibilityIdentifier(identifier)
                    }
                }
                .textContentType(contentType)
                .submitLabel(submitLabel)
                .foregroundStyle(.white)
                .tint(Brand.cyan)
                Button { revealed.wrappedValue.toggle() } label: {
                    Image(systemName: revealed.wrappedValue ? "eye.slash.fill" : "eye.fill")
                        .foregroundStyle(.white.opacity(0.58))
                        .frame(width: 30, height: 38)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(revealed.wrappedValue ? "Jelszó elrejtése" : "Jelszó megjelenítése")
            }
            .padding(.horizontal, 15)
            .frame(minHeight: 54)
            .background(Color.white.opacity(0.075))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color.white.opacity(0.16), lineWidth: 1)
            }
        }
    }

    private var legalSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Toggle(isOn: $legalAccepted) {
                Text("Elfogadom az adatkezelési tájékoztatót és az ÁSZF-et.")
                    .font(.footnote)
                    .foregroundStyle(.white.opacity(0.82))
            }
            .tint(Brand.cyan)

            if let config = store.configuration {
                HStack(spacing: 20) {
                    Link("Adatkezelés", destination: config.privacyPolicyURL)
                    Link("ÁSZF", destination: config.termsURL)
                }
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Brand.cyanLight)
            }
        }
    }

    private var verificationAddress: String? {
        if case .verificationSent(let address) = store.authStatus { return address }
        return nil
    }

    private var buildVersionLabel: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "?"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "?"
        return "DEV \(version) (\(build))"
    }

    private func submit() {
        Task {
            if mode == .login {
                await store.login(email: email, password: password)
            } else {
                await store.register(name: name, email: email, password: password,
                                     confirmation: confirmation, legalAccepted: legalAccepted)
            }
        }
    }

    private var passwordResetSheet: some View {
        NavigationStack {
            ZStack {
                Brand.canvas.ignoresSafeArea()
                VStack(spacing: 20) {
                    Image(systemName: "key.horizontal.fill")
                        .font(.system(size: 42, weight: .semibold))
                        .foregroundStyle(Brand.cyan)
                        .frame(width: 82, height: 82)
                        .background(Brand.blue.opacity(0.18))
                        .clipShape(Circle())
                    Text("Új jelszó kérése")
                        .font(.title2.bold())
                    Text("Add meg az e-mail-címed, és elküldjük a biztonságos jelszó-visszaállító hivatkozást.")
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                    if passwordResetSent {
                        VStack(spacing: 10) {
                            Label("A kérés sikeresen elment", systemImage: "checkmark.circle.fill")
                                .font(.headline)
                                .foregroundStyle(.green)
                            Text("Ha a címhez tartozik fiók, hamarosan megérkezik a levél. Mindig csak a legutóbb kért hivatkozást nyisd meg.")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .accessibilityIdentifier("auth.reset.sent")
                        Button("Kész") { forgotPassword = false }
                            .buttonStyle(.borderedProminent)
                            .accessibilityIdentifier("auth.reset.done")
                    } else {
                        TextField("E-mail-cím", text: $email)
                            .textContentType(.emailAddress)
                            .keyboardType(.emailAddress)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .submitLabel(.send)
                            .textFieldStyle(.roundedBorder)
                            .frame(minHeight: 52)
                            .accessibilityIdentifier("auth.reset.email")
                        Button {
                            Task {
                                if await store.requestPasswordReset(email: email) {
                                    passwordResetSent = true
                                }
                            }
                        } label: {
                            VizitPrimaryButtonLabel(title: "E-mail küldése",
                                                    systemImage: "paperplane.fill",
                                                    busy: store.busy)
                        }
                        .buttonStyle(.plain)
                        .disabled(store.busy)
                        .opacity(store.busy ? 0.72 : 1)
                        .accessibilityIdentifier("auth.reset.submit")
                    }
                    Spacer()
                }
                .padding(24)
                .frame(maxWidth: 520)
            }
            .navigationTitle("Jelszó-visszaállítás")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Mégse") { forgotPassword = false }
                }
            }
        }
    }
}

struct PasswordChangeScreen: View {
    @EnvironmentObject private var store: AppStore
    @State private var password = ""
    @State private var confirmation = ""

    var body: some View {
        NavigationStack {
            ZStack {
                VizitScreenBackground()
                ScrollView {
                    VStack(spacing: 20) {
                        Image(systemName: "lock.rotation")
                            .font(.system(size: 48, weight: .semibold))
                            .foregroundStyle(Brand.blue)
                        Text("Állíts be új jelszót")
                            .font(.title.bold())
                        Text("A jelszó legalább 8 karakterből, kis- és nagybetűből, valamint számból álljon.")
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                        VizitCard {
                            VStack(spacing: 16) {
                                SecureField("Új jelszó", text: $password)
                                    .textContentType(.newPassword)
                                    .textFieldStyle(.roundedBorder)
                                SecureField("Új jelszó újra", text: $confirmation)
                                    .textContentType(.newPassword)
                                    .textFieldStyle(.roundedBorder)
                            }
                        }
                        Button {
                            Task { await store.changePassword(password, confirmation: confirmation) }
                        } label: {
                            VizitPrimaryButtonLabel(title: "Jelszó módosítása", systemImage: "checkmark.shield.fill",
                                                    busy: store.busy)
                        }
                        .buttonStyle(.plain)
                        .disabled(store.busy)
                    }
                    .padding(24)
                    .frame(maxWidth: 540)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("Jelszó-visszaállítás")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
