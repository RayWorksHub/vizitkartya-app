import SwiftUI

private enum AuthMode: String, CaseIterable, Identifiable {
    case login = "Belépés"
    case register = "Regisztráció"
    var id: String { rawValue }
}

/// Authentication entry point, rebuilt on the VIZIT 2026 design system.
///
/// The old screen was a dark hero gradient with two blurred glow circles and a
/// translucent card — the exact "generic AI SaaS" look the brief rules out. It
/// now follows the product default (light, with a real dark theme) and leads
/// with the brand lockup and the digital card promise instead of decoration.
struct AuthScreen: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var mode: AuthMode = .login
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var legalAccepted = false
    @State private var forgotPassword = false
    @State private var passwordResetSent = false

    private func select(_ next: AuthMode) {
        guard next != mode else { return }
        withAnimation(reduceMotion ? nil : .easeOut(duration: 0.2)) {
            mode = next
            password = ""
        }
    }

    private var modeIndex: Binding<Int> {
        Binding(
            get: { AuthMode.allCases.firstIndex(of: mode) ?? 0 },
            set: { index in
                guard AuthMode.allCases.indices.contains(index) else { return }
                select(AuthMode.allCases[index])
            }
        )
    }

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.lg) {
                        VizitBrandLockup(maxHeight: 96)
                            .padding(.top, VizitSpace.xs)

                        heading

                        modeSelector

                        fields

                        if mode == .register { legalSection }

                        VizitButton(
                            title: mode == .login ? "Belépés" : "Fiók létrehozása",
                            systemImage: mode == .login ? "arrow.right" : "person.badge.plus",
                            isLoading: store.busy,
                            isEnabled: !store.busy,
                            action: submit
                        )
                        .accessibilityIdentifier("auth.submit")

                        if mode == .login { loginExtras }

                        footer
                    }
                    .padding(.horizontal, VizitSpace.lg)
                    .padding(.bottom, VizitSpace.xl)
                    .frame(maxWidth: 540)
                    .frame(maxWidth: .infinity)
                }
                .scrollDismissesKeyboard(.interactively)
            }
            .toolbar(.hidden, for: .navigationBar)
            .sheet(isPresented: $forgotPassword) { passwordResetSheet }
        }
    }

    private var heading: some View {
        VStack(alignment: .leading, spacing: VizitSpace.xs) {
            Text(mode == .login ? "Üdv újra!" : "Csatlakozz a VIZIT-hez")
                .font(VizitFont.h1)
                .foregroundStyle(VizitColor.textPrimary)
                .multilineTextAlignment(.leading)
            Text(mode == .login
                 ? "A digitális névjegyed mindig veled van."
                 : "Hozd létre a fiókod, majd állítsd össze a névjegyed.")
                .font(VizitFont.body)
                .foregroundStyle(VizitColor.textSecondary)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var modeSelector: some View {
        VizitSegmentedControl(
            options: AuthMode.allCases.map(\.rawValue),
            selection: modeIndex,
            accessibilityIdentifier: "auth.mode"
        )
    }

    private var fields: some View {
        VStack(spacing: VizitSpace.sm) {
            if mode == .register {
                VizitTextField(
                    label: "Teljes név",
                    text: $name,
                    placeholder: "Teljes név",
                    contentType: .name,
                    autocapitalization: .words,
                    identifier: "auth.name",
                    submitLabel: .next
                )
            }

            VizitTextField(
                label: "E-mail-cím",
                text: $email,
                placeholder: "nev@pelda.hu",
                keyboard: .emailAddress,
                contentType: .username,
                autocapitalization: .never,
                identifier: "auth.email",
                submitLabel: .next
            )

            VizitTextField(
                label: "Jelszó",
                text: $password,
                placeholder: "••••••••",
                contentType: mode == .login ? .password : .newPassword,
                autocapitalization: .never,
                isSecure: true,
                identifier: "auth.password",
                submitLabel: mode == .login ? .go : .next,
                onSubmit: { if mode == .login { submit() } }
            )

            if mode == .register {
                Text("Legalább 8 karakter, benne kis- és nagybetű, valamint szám.")
                    .font(VizitFont.caption)
                    .foregroundStyle(VizitColor.textMuted)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private var legalSection: some View {
        VStack(alignment: .leading, spacing: VizitSpace.sm) {
            Toggle(isOn: $legalAccepted) {
                Text("Elfogadom az Általános Szerződési Feltételeket és az Adatkezelési tájékoztatót.")
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(VizitColor.textSecondary)
            }
            .tint(VizitColor.primary)

            if let config = store.configuration {
                HStack(spacing: VizitSpace.lg) {
                    Link("Adatkezelés", destination: config.privacyPolicyURL)
                    Link("ÁSZF", destination: config.termsURL)
                }
                .font(VizitFont.label)
                .foregroundStyle(VizitColor.primary)
            }
        }
        .padding(VizitSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(VizitColor.sunken)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))
    }

    private var loginExtras: some View {
        VStack(spacing: VizitSpace.sm) {
            if store.configuration?.googleSignInEnabled == true {
                VizitButton(
                    title: "Folytatás Google-fiókkal",
                    systemImage: "g.circle.fill",
                    kind: .secondary,
                    isEnabled: !store.busy
                ) {
                    Task { await store.googleLogin() }
                }
            }

            Button("Elfelejtettem a jelszavam") {
                passwordResetSent = false
                forgotPassword = true
            }
            .font(VizitFont.label)
            .foregroundStyle(VizitColor.primary)
            .frame(minHeight: VizitMetrics.minTouchTarget)
            .disabled(store.busy)
            .accessibilityIdentifier("auth.forgotPassword")
        }
    }

    private var footer: some View {
        VStack(spacing: VizitSpace.xs) {
            Text(buildVersionLabel)
                .font(VizitFont.caption.monospacedDigit())
                .foregroundStyle(VizitColor.textMuted)
                .accessibilityIdentifier("auth.version")
        }
        .padding(.top, VizitSpace.xs)
        .frame(maxWidth: .infinity)
    }

    private var buildVersionLabel: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "?"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "?"
        return "VIZIT \(version) (\(build))"
    }

    private func submit() {
        Task {
            if mode == .login {
                await store.login(email: email, password: password)
            } else {
                await store.register(name: name, email: email, password: password,
                                     confirmation: password, legalAccepted: legalAccepted)
            }
        }
    }

    private var passwordResetSheet: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(spacing: VizitSpace.lg) {
                        VizitBrandLockup(maxHeight: 72)
                            .padding(.top, VizitSpace.xs)

                        VStack(spacing: VizitSpace.xs) {
                            Text("Új jelszó kérése")
                                .font(VizitFont.h2)
                                .foregroundStyle(VizitColor.textPrimary)
                            Text("Add meg a fiókodhoz tartozó e-mail-címet. Küldünk egy hivatkozást, amivel új jelszót állíthatsz be. A hivatkozás 1 órán át érvényes.")
                                .font(VizitFont.body)
                                .foregroundStyle(VizitColor.textSecondary)
                                .multilineTextAlignment(.center)
                                .fixedSize(horizontal: false, vertical: true)
                        }

                        if passwordResetSent {
                            VStack(spacing: VizitSpace.sm) {
                                VizitStatusPill(text: "A kérés sikeresen elment", tone: .success)
                                Text("Ha a címhez tartozik fiók, hamarosan megérkezik a levél. Mindig csak a legutóbb kért hivatkozást nyisd meg.")
                                    .font(VizitFont.bodySmall)
                                    .foregroundStyle(VizitColor.textSecondary)
                                    .multilineTextAlignment(.center)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                            .accessibilityIdentifier("auth.reset.sent")

                            VizitButton(title: "Kész", kind: .secondary) {
                                forgotPassword = false
                            }
                            .accessibilityIdentifier("auth.reset.done")
                        } else {
                            VizitTextField(
                                label: "E-mail-cím",
                                text: $email,
                                placeholder: "nev@pelda.hu",
                                keyboard: .emailAddress,
                                contentType: .emailAddress,
                                autocapitalization: .never,
                                identifier: "auth.reset.email",
                                submitLabel: .send,
                                onSubmit: requestReset
                            )

                            VizitButton(
                                title: "Hivatkozás küldése",
                                systemImage: "paperplane",
                                isLoading: store.busy,
                                isEnabled: !store.busy,
                                action: requestReset
                            )
                            .accessibilityIdentifier("auth.reset.submit")

                            VizitBanner(
                                text: "Biztonsági okból akkor is ezt a választ adjuk, ha a megadott címhez nem tartozik fiók.",
                                tone: .info
                            )
                        }
                    }
                    .padding(VizitSpace.lg)
                    .frame(maxWidth: 520)
                    .frame(maxWidth: .infinity)
                }
                .scrollDismissesKeyboard(.interactively)
            }
            .navigationTitle("Új jelszó kérése")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Belépés") { forgotPassword = false }
                }
            }
        }
    }

    private func requestReset() {
        Task {
            if await store.requestPasswordReset(email: email) {
                passwordResetSent = true
            }
        }
    }
}

/// Recovery-link destination: the user arrived from the e-mail and must set a
/// new password before the session continues.
struct PasswordChangeScreen: View {
    @EnvironmentObject private var store: AppStore
    @State private var password = ""
    @State private var confirmation = ""

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(spacing: VizitSpace.lg) {
                        VizitIconChip(
                            systemImage: "lock.rotation",
                            tint: VizitColor.primary,
                            background: VizitColor.primarySubtle,
                            size: 64
                        )
                        .padding(.top, VizitSpace.md)

                        VStack(spacing: VizitSpace.xs) {
                            Text("Állíts be új jelszót")
                                .font(VizitFont.h2)
                                .foregroundStyle(VizitColor.textPrimary)
                            Text("A jelszó legalább 8 karakterből, kis- és nagybetűből, valamint számból álljon.")
                                .font(VizitFont.body)
                                .foregroundStyle(VizitColor.textSecondary)
                                .multilineTextAlignment(.center)
                                .fixedSize(horizontal: false, vertical: true)
                        }

                        VStack(spacing: VizitSpace.sm) {
                            VizitTextField(
                                label: "Új jelszó",
                                text: $password,
                                placeholder: "••••••••",
                                contentType: .newPassword,
                                autocapitalization: .never,
                                isSecure: true,
                                submitLabel: .next
                            )
                            VizitTextField(
                                label: "Új jelszó újra",
                                text: $confirmation,
                                placeholder: "••••••••",
                                contentType: .newPassword,
                                autocapitalization: .never,
                                isSecure: true,
                                submitLabel: .go,
                                onSubmit: change
                            )
                        }

                        VizitButton(
                            title: "Jelszó módosítása",
                            systemImage: "checkmark.shield",
                            isLoading: store.busy,
                            isEnabled: !store.busy,
                            action: change
                        )
                    }
                    .padding(VizitSpace.lg)
                    .frame(maxWidth: 540)
                    .frame(maxWidth: .infinity)
                }
                .scrollDismissesKeyboard(.interactively)
            }
            .navigationTitle("Jelszó-visszaállítás")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func change() {
        Task { await store.changePassword(password, confirmation: confirmation) }
    }
}

/// Registration completion is intentionally its own state instead of a banner
/// inside the sign-in form. This keeps the next action unambiguous and mirrors
/// the confirmation destination used by the web app.
struct EmailVerificationScreen: View {
    @EnvironmentObject private var store: AppStore
    let email: String
    @State private var resent = false

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(spacing: VizitSpace.lg) {
                        VizitBrandLockup(maxHeight: 96)
                            .padding(.top, VizitSpace.xs)

                        VizitIconChip(
                            systemImage: "checkmark",
                            tint: VizitColor.success,
                            background: VizitColor.successSubtle,
                            size: 72
                        )
                        .accessibilityHidden(true)

                        VStack(spacing: VizitSpace.sm) {
                            Text("Ellenőrizd a postaládádat")
                                .font(VizitFont.h1)
                                .foregroundStyle(VizitColor.textPrimary)
                                .multilineTextAlignment(.center)

                            Text("Megerősítő hivatkozást küldtünk ide:")
                                .font(VizitFont.body)
                                .foregroundStyle(VizitColor.textSecondary)

                            Text(email)
                                .font(VizitFont.label)
                                .foregroundStyle(VizitColor.textPrimary)
                                .multilineTextAlignment(.center)
                                .textSelection(.enabled)
                                .accessibilityIdentifier("auth.verification.email")

                            Text("Nyisd meg a levélben kapott hivatkozást ezen a készüléken. Ha nem találod, ellenőrizd a levélszemét mappát is.")
                                .font(VizitFont.bodySmall)
                                .foregroundStyle(VizitColor.textSecondary)
                                .multilineTextAlignment(.center)
                                .fixedSize(horizontal: false, vertical: true)
                        }

                        VizitBanner(
                            text: "A profilod csak a cím megerősítése után jön létre. A hivatkozás egyszer használható.",
                            tone: .info
                        )

                        VizitButton(
                            title: "Vissza a belépéshez",
                            systemImage: "arrow.left",
                            kind: .secondary
                        ) {
                            store.showSignIn()
                        }
                        .accessibilityIdentifier("auth.verification.back")

                        Button(resent ? "A levelet újraküldtük" : "Nem érkezett meg? Levél újraküldése") {
                            Task { resent = await store.resendVerification(email: email) }
                        }
                        .font(VizitFont.label)
                        .foregroundStyle(VizitColor.primary)
                        .frame(minHeight: VizitMetrics.minTouchTarget)
                        .disabled(store.busy)
                        .accessibilityIdentifier("auth.verification.resend")
                    }
                    .padding(.horizontal, VizitSpace.lg)
                    .padding(.bottom, VizitSpace.xl)
                    .frame(maxWidth: 540)
                    .frame(maxWidth: .infinity)
                }
            }
            .toolbar(.hidden, for: .navigationBar)
        }
    }
}
