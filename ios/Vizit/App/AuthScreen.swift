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

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 22) {
                    Image("VizitLogo").resizable().scaledToFit().frame(maxWidth: 220, maxHeight: 80)
                        .padding(14).background(.white).clipShape(RoundedRectangle(cornerRadius: 16))
                    Text("A digitális névjegyed iPhone-on").font(.title2.bold()).multilineTextAlignment(.center)
                    Picker("Fiók", selection: $mode) {
                        ForEach(AuthMode.allCases) { Text($0.rawValue).tag($0) }
                    }.pickerStyle(.segmented)

                    if case .verificationSent(let address) = store.authStatus {
                        Label("Megerősítésre vár: \(address)", systemImage: "envelope.badge.shield.half.filled")
                            .padding().frame(maxWidth: .infinity, alignment: .leading)
                            .background(Brand.blue.opacity(0.1)).clipShape(RoundedRectangle(cornerRadius: 12))
                    }

                    VStack(spacing: 14) {
                        if mode == .register {
                            TextField("Név", text: $name).textContentType(.name)
                                .textFieldStyle(.roundedBorder)
                        }
                        TextField("E-mail-cím", text: $email).textContentType(.username)
                            .keyboardType(.emailAddress).textInputAutocapitalization(.never)
                            .autocorrectionDisabled().textFieldStyle(.roundedBorder)
                        SecureField("Jelszó", text: $password).textContentType(mode == .login ? .password : .newPassword)
                            .textFieldStyle(.roundedBorder)
                        if mode == .register {
                            SecureField("Jelszó újra", text: $confirmation).textContentType(.newPassword)
                                .textFieldStyle(.roundedBorder)
                            Toggle(isOn: $legalAccepted) {
                                Text("Elolvastam és elfogadom az adatkezelési tájékoztatót és az ÁSZF-et.")
                                    .font(.footnote)
                            }
                            if let config = store.configuration {
                                HStack {
                                    Link("Adatkezelés", destination: config.privacyPolicyURL)
                                    Spacer()
                                    Link("ÁSZF", destination: config.termsURL)
                                }.font(.footnote)
                            }
                        }
                    }

                    Button(mode == .login ? "Biztonságos belépés" : "Fiók létrehozása") {
                        Task {
                            if mode == .login { await store.login(email: email, password: password) }
                            else { await store.register(name: name, email: email, password: password,
                                                        confirmation: confirmation, legalAccepted: legalAccepted) }
                        }
                    }
                    .buttonStyle(.borderedProminent).controlSize(.large).disabled(store.busy)
                    .accessibilityIdentifier("auth.submit")

                    if mode == .login {
                        if store.configuration?.googleSignInEnabled == true {
                            Button("Folytatás Google-fiókkal") { Task { await store.googleLogin() } }
                                .buttonStyle(.bordered).controlSize(.large).disabled(store.busy)
                        }
                        Button("Elfelejtettem a jelszavam") { forgotPassword = true }
                    }
                    if store.busy { ProgressView() }
                    Text("A munkamenet titkosított iPhone-kulcstárban marad, és nem kerül át másik készülékre mentéssel.")
                        .font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
                }
                .padding(24).frame(maxWidth: 520).frame(maxWidth: .infinity)
            }
            .background(Color(.systemGroupedBackground))
            .sheet(isPresented: $forgotPassword) {
                NavigationStack {
                    Form {
                        Section("Jelszó-visszaállítás") {
                            TextField("E-mail-cím", text: $email).keyboardType(.emailAddress)
                                .textInputAutocapitalization(.never).autocorrectionDisabled()
                            Text("Biztonsági okból ugyanazt az üzenetet mutatjuk akkor is, ha a címhez nincs fiók.")
                                .font(.footnote).foregroundStyle(.secondary)
                        }
                    }
                    .navigationTitle("Új jelszó")
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button("Mégse") { forgotPassword = false } }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("E-mail küldése") {
                                forgotPassword = false
                                Task { await store.requestPasswordReset(email: email) }
                            }
                        }
                    }
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
            Form {
                Section("Új jelszó") {
                    SecureField("Legalább 8 karakter, betű és szám", text: $password).textContentType(.newPassword)
                    SecureField("Új jelszó újra", text: $confirmation).textContentType(.newPassword)
                }
                Section {
                    Button("Jelszó módosítása") {
                        Task { await store.changePassword(password, confirmation: confirmation) }
                    }.disabled(store.busy)
                }
            }
            .navigationTitle("Jelszó-visszaállítás")
        }
    }
}
