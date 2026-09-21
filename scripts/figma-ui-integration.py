from pathlib import Path
import re
import subprocess

ROOT = Path.cwd()

def load(p):
    return (ROOT / p).read_text(encoding='utf-8')

def save(p, text):
    (ROOT / p).write_text(text, encoding='utf-8')

def one(text, before, after):
    count = text.count(before)
    if count != 1:
        raise RuntimeError(f'Expected one source anchor, found {count}: {before[:100]}')
    return text.replace(before, after, 1)

# Every replacement is anchored to the source that was reviewed, never a blind append.
p = 'ios/Vizit/Design/VizitComponents.swift'
t = load(p)
t = one(t, '    @State private var pressed = false', '    @Environment(\\.dynamicTypeSize) private var dynamicTypeSize\n    @State private var pressed = false')
t = one(t, '                    .lineLimit(1)\n                    .minimumScaleFactor(0.72)\n                    .allowsTightening(true)', '                    .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 2)\n                    .fixedSize(horizontal: false, vertical: true)\n                    .multilineTextAlignment(.center)')
t = one(t, '                .focused($focused)', '                .accessibilityLabel(label)\n                .focused($focused)')
t = one(t, '                    .labelsHidden()\n                    .disabled(!isEnabled)', '                    .labelsHidden()\n                    .accessibilityLabel(title)\n                    .accessibilityValue(isOn ? "Be" : "Ki")\n                    .disabled(!isEnabled)')
t = one(t, '            .padding(.vertical, VizitSpace.xs)\n            .background(background)\n            .clipShape(Capsule())', '            .padding(.vertical, VizitSpace.xs)\n            .frame(minHeight: action == nil ? 0 : VizitMetrics.minTouchTarget)\n            .background(background)\n            .clipShape(Capsule())')
t = one(t, '    var destructive = false\n    let onCancel: () -> Void', '    var destructive = false\n    var isLoading = false\n    var isEnabled = true\n    let onCancel: () -> Void')
t = one(t, '                    VizitButton(title: cancelTitle, kind: .secondary, action: onCancel)', '                    VizitButton(title: cancelTitle, kind: .secondary, isEnabled: !isLoading, action: onCancel)')
t = one(t, '                        kind: destructive ? .destructive : .primary,\n                        action: onConfirm', '                        kind: destructive ? .destructive : .primary,\n                        isLoading: isLoading,\n                        isEnabled: isEnabled && !isLoading,\n                        action: onConfirm')
t = one(t, '            } else if !text.isEmpty {', '            }\n            if !text.isEmpty {')
t = one(t, '                .accessibilityLabel("Keresés törlése")', '                .frame(width: VizitMetrics.minTouchTarget, height: VizitMetrics.minTouchTarget)\n                .accessibilityLabel("Keresés törlése")')
t = one(t, '    var resultCount: Int? = nil\n\n    var body: some View {', '    var resultCount: Int? = nil\n    @FocusState private var isFocused: Bool\n\n    var body: some View {')
t = one(t, '            TextField(placeholder, text: $text)\n                .font(VizitFont.body)', '            TextField(placeholder, text: $text)\n                .font(VizitFont.body)\n                .focused($isFocused)\n                .accessibilityLabel(placeholder)')
a = t.index('struct VizitSearchField:'); b = t.index('struct VizitProgressCard:', a)
t = t[:a] + t[a:b].replace('.stroke(VizitColor.border, lineWidth: 1)', '.stroke(isFocused ? VizitColor.borderFocus : VizitColor.border, lineWidth: isFocused ? 2 : 1)') + t[b:]
t = one(t, '                    Text("\\(current) / \\(total)")', '                    Text("\\(min(max(current, 0), max(total, 0))) / \\(max(total, 0))")')
t = one(t, '                ProgressView(value: progress)\n                    .tint(VizitColor.primary)', '                ProgressView(value: progress)\n                    .tint(VizitColor.primary)\n                    .accessibilityLabel(title)\n                    .accessibilityValue("\\(Int(progress * 100)) százalék")')
a = t.index('struct VizitTabHeader:'); b = t.index('struct VizitInlineMessage:', a)
t = t[:a] + t[a:b].replace('.buttonStyle(.plain)', '.buttonStyle(.plain)\n                    .frame(minHeight: VizitMetrics.minTouchTarget)') + t[b:]
t = one(t, '                .font(.system(size: 32, weight: .bold))', '                .font(VizitFont.h1)\n                .fixedSize(horizontal: false, vertical: true)')
a = t.index('struct VizitRow<Trailing:'); b = t.index('extension VizitRow where', a)
u = t[a:b]
u = u.replace('    private var foreground:', '    @Environment(\\.dynamicTypeSize) private var dynamicTypeSize\n\n    private var foreground:', 1)
u = u.replace('            VStack(alignment: .leading, spacing: 1) {', '            VStack(alignment: .leading, spacing: VizitSpace.xxs) {', 1)
u = u.replace('Text(label).font(VizitFont.body).foregroundStyle(foreground)', 'Text(label).font(VizitFont.body).foregroundStyle(foreground).fixedSize(horizontal: false, vertical: true)')
u = u.replace('            }\n            Spacer(minLength: VizitSpace.xs)\n            if let value {\n                Text(value).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)\n            }', '''                if dynamicTypeSize.isAccessibilitySize, let value {
                    Text(value).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .layoutPriority(1)
            Spacer(minLength: VizitSpace.xs)
            if !dynamicTypeSize.isAccessibilitySize, let value {
                Text(value).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)
                    .multilineTextAlignment(.trailing)
                    .lineLimit(2)
                    .frame(maxWidth: 140, alignment: .trailing)
            }''')
u = u.replace('        .contentShape(Rectangle())', '        .contentShape(Rectangle())\n        .accessibilityElement(children: .combine)', 1)
t = t[:a] + u + t[b:]
t = t.replace('Button(actionTitle, action: action)\n                    .font(VizitFont.label)', 'Button(actionTitle, action: action)\n                    .frame(minHeight: VizitMetrics.minTouchTarget)\n                    .font(VizitFont.label)')
t += r'''

// MARK: - Feedback attached to real application operations

@MainActor
final class VizitFeedbackCenter: ObservableObject {
    struct Notice: Identifiable {
        let id = UUID()
        let text: String
        let tone: VizitTone
        let actionTitle: String?
        let action: (() -> Void)?
    }
    @Published private(set) var notice: Notice?
    private var dismissal: Task<Void, Never>?

    func show(_ text: String, tone: VizitTone = .success,
              actionTitle: String? = nil, action: (() -> Void)? = nil) {
        dismissal?.cancel()
        let value = Notice(text: text, tone: tone, actionTitle: actionTitle, action: action)
        notice = value
        UIAccessibility.post(notification: .announcement, argument: text)
        // Undo stays available until explicitly dismissed, including for VoiceOver.
        guard action == nil else { return }
        dismissal = Task { [weak self] in
            do { try await Task.sleep(nanoseconds: 5_000_000_000) }
            catch { return }
            guard !Task.isCancelled, self?.notice?.id == value.id else { return }
            self?.notice = nil
        }
    }
    func dismiss() { dismissal?.cancel(); dismissal = nil; notice = nil }
}

struct VizitFeedbackHost: View {
    @EnvironmentObject private var feedback: VizitFeedbackCenter
    var body: some View {
        if let notice = feedback.notice {
            HStack(alignment: .center, spacing: VizitSpace.xxs) {
                if let title = notice.actionTitle, let action = notice.action {
                    VizitSnackbar(text: notice.text, actionTitle: title) {
                        feedback.dismiss()
                        action()
                    }
                } else {
                    VizitToast(text: notice.text, tone: notice.tone)
                }
                VizitIconButton(systemImage: "xmark", accessibilityTitle: "Üzenet bezárása") {
                    feedback.dismiss()
                }
            }
            .padding(.horizontal, VizitSpace.md)
            .padding(.vertical, VizitSpace.xs)
            .background(VizitColor.canvas)
            .accessibilityIdentifier("feedback.notice")
        }
    }
}

/// Appears only while the missing remote profile is actually being fetched.
struct VizitProfileSkeleton: View {
    var body: some View {
        VStack(alignment: .leading, spacing: VizitSpace.md) {
            VizitSkeleton(height: 216, cornerRadius: VizitRadius.xl)
            VizitSkeleton(height: 20)
            VizitSkeleton(height: 16).frame(maxWidth: 220)
            VizitLoadingState(message: "Névjegy betöltése…")
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Névjegy betöltése folyamatban")
    }
}
'''
save(p, t)

p = 'ios/Vizit/App/VizitApp.swift'; t = load(p)
t = one(t, '    @StateObject private var presentation = CardPresentationStore()', '    @StateObject private var presentation = CardPresentationStore()\n    @StateObject private var feedback = VizitFeedbackCenter()')
t = one(t, '                .environmentObject(presentation)', '                .environmentObject(presentation)\n                .environmentObject(feedback)')
t = one(t, '    case passwordRecovery\n', '    case passwordRecovery\n    case sessionExpired\n')
t = one(t, '    @Published private(set) var busy = false', '''    @Published private(set) var busy = false
    @Published private(set) var automaticSyncEnabled =
        UserDefaults.standard.object(forKey: "figma.automaticSyncEnabled") as? Bool ?? true
    private var authWatch: Task<Void, Never>?
    private var intentionalSignOut = false''')
t = one(t, '            cloud = CloudService(configuration: config)\n            Task { await bootstrap() }', '''            let service = CloudService(configuration: config)
            cloud = service
            observeSession(service)
            Task { await bootstrap() }''')
t = one(t, '    var accountEmail: String { userEmail }', '    var accountEmail: String { userEmail }\n    var accountIdentifier: UUID? { userID }')
t = one(t, '    var isOnline: Bool { authStatus == .authenticated }', r'''    var isOnline: Bool { authStatus == .authenticated }
    var syncErrorDescription: String? { syncFailureMessage }

    func setAutomaticSync(_ enabled: Bool) {
        automaticSyncEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: "figma.automaticSyncEnabled")
        if enabled { retrySync() }
        else { resetSyncRetry() }
    }
    private func observeSession(_ service: CloudService) {
        authWatch = Task { [weak self] in
            for await (event, session) in await service.client.auth.authStateChanges {
                guard !Task.isCancelled, let self else { return }
                if event == .signedOut, !self.intentionalSignOut, self.userID != nil {
                    switch self.authStatus {
                    case .authenticated, .offline, .passwordRecovery: self.sessionExpired()
                    default: break
                    }
                } else if event == .userUpdated, session?.user.id == self.userID {
                    self.objectWillChange.send()
                    self.userEmail = session?.user.email ?? self.userEmail
                }
            }
        }
    }
    private func isExpiredSession(_ error: Error) -> Bool {
        if let cloudError = error as? CloudError {
            if case .authenticationRequired = cloudError { return true }
            if case .server(let status, _) = cloudError, status == 401 { return true }
        }
        guard let auth = error as? AuthError else { return false }
        if case .sessionMissing = auth { return true }
        return ["session_not_found", "refresh_token_not_found", "refresh_token_already_used", "bad_jwt", "session_expired"]
            .contains(auth.errorCode.rawValue)
    }
    private func sessionExpired() {
        resetSyncRetry()
        syncAgain = false
        message = nil
        authStatus = .sessionExpired
        syncStatus = .pending
        // Retain the account-isolated unsent draft, hidden behind AppGate.
    }''')
t = one(t, '            authStatus = .authenticated\n            await synchronize()\n        } catch {\n            authStatus = .offline', '            authStatus = .authenticated\n            if automaticSyncEnabled { await synchronize(automatically: true) }\n            else { syncStatus = (try? syncStore?.load().pendingUpload) == true ? .pending : .localOnly }\n        } catch {\n            if isExpiredSession(error) { sessionExpired(); return }\n            authStatus = .offline')
t = one(t, '    func logout() async {\n        busy = true\n        defer { busy = false }', '    func logout() async {\n        busy = true\n        intentionalSignOut = true\n        defer { busy = false; intentionalSignOut = false }')
t = one(t, '            try? await cloud.logout()\n            self.clearUser()', '            self.intentionalSignOut = true\n            defer { self.intentionalSignOut = false }\n            try? await cloud.logout()\n            self.clearUser()')
t = one(t, '        if !uiTesting { Task { await synchronize() } }', '        if !uiTesting && automaticSyncEnabled { Task { await synchronize(automatically: true) } }')
t = one(t, '                } catch {\n                    message = "Még nincs hálózati kapcsolat.', '                } catch {\n                    if isExpiredSession(error) { sessionExpired(); return }\n                    message = "Még nincs hálózati kapcsolat.')
t = one(t, '    func synchronize() async {\n        guard', '    func synchronize(automatically: Bool = false) async {\n        guard !automatically || automaticSyncEnabled else { return }\n        guard')
t = one(t, '                Task { await self.synchronize() }', '                Task { await self.synchronize(automatically: automatically) }')
t = one(t, '        guard syncRetryTask == nil, authStatus == .authenticated else { return }', '        guard automaticSyncEnabled, syncRetryTask == nil, authStatus == .authenticated else { return }')
t = one(t, '            self.syncRetryTask = nil\n            await self.synchronize()', '            self.syncRetryTask = nil\n            await self.synchronize(automatically: true)')
t = one(t, '        } catch let error as CloudError {\n            guard userID == id else { return }', '        } catch let error as CloudError {\n            guard userID == id else { return }\n            if isExpiredSession(error) { sessionExpired(); return }')
t = one(t, '        } catch {\n            guard userID == id else { return }\n            syncStatus = .failed', '        } catch {\n            guard userID == id else { return }\n            if isExpiredSession(error) { sessionExpired(); return }\n            syncStatus = .failed')
t = one(t, '            case .signedOut:\n                AuthScreen()', '            case .signedOut:\n                AuthScreen()\n\n            case .sessionExpired:\n                VizitScreen { VizitSessionExpiredState(login: store.showSignIn) }')
t = one(t, '                ProgressView()\n                    .controlSize(.large)\n                    .tint(Color(uiColor: UIColor(hex: 0x0FBEE6)))', '                VizitLoadingState(message: "A VIZIT indítása…")\n                    .environment(\\.colorScheme, .dark)\n                    .frame(maxHeight: 150)')
t = one(t, '        .tint(VizitColor.primary)\n        .toolbarBackground', '        .safeAreaInset(edge: .bottom, spacing: 0) { VizitFeedbackHost() }\n        .tint(VizitColor.primary)\n        .toolbarBackground')
t = one(t, '                .onOpenURL { url in Task { await store.handleCallback(url) } }', '''                .onOpenURL { url in Task { await store.handleCallback(url) } }
                .onChange(of: store.authStatus) { status in
                    switch status {
                    case .authenticated, .offline: break
                    default: feedback.dismiss()
                    }
                }''')
save(p, t)

p = 'ios/Vizit/App/ProfileEditor.swift'; t = load(p)
t = one(t, '    @EnvironmentObject private var presentation: CardPresentationStore', '    @EnvironmentObject private var presentation: CardPresentationStore\n    @EnvironmentObject private var feedback: VizitFeedbackCenter')
t = one(t, '                        do { try store.save(draft); dismiss() }', '''                        do {
                            let previous = store.profile
                            let owner = store.accountIdentifier
                            let savedProfile = draft.normalized
                            try store.save(draft)
                            dismiss()
                            if (try? previous.validate()) != nil {
                                feedback.show("A névjegy mentve.", actionTitle: "Visszavonás") {
                                    guard store.accountIdentifier == owner, store.profile == savedProfile else {
                                        feedback.show("A névjegy azóta megváltozott. Nyisd meg a szerkesztőt a módosításhoz.", tone: .info)
                                        return
                                    }
                                    do {
                                        try store.save(previous)
                                        feedback.show("Az előző névjegyadatokat visszaállítottuk.")
                                    } catch { feedback.show(error.localizedDescription, tone: .error) }
                                }
                            } else { feedback.show("A névjegyed elkészült.") }
                        }''')
t = one(t, '                    .disabled(loadingPhoto || store.storageError != nil)', '                    .disabled(loadingPhoto || loadingCompanyLogo || store.storageError != nil)')
t = one(t, '                        sharingSection\n', '''                        sharingSection
                        if let error {
                            VizitInlineMessage(text: error, tone: .error)
                                .accessibilityIdentifier("profile.inlineError")
                        }
''')
save(p, t)

p = 'ios/Vizit/App/Screens.swift'; t = load(p)
t = one(t, 'import WebKit\n', 'import WebKit\nimport AVFoundation\n')
t = one(t, '''                        VizitDigitalCard(
                            profile: store.profile,
                            nameIdentifier: store.hasProfile ? "card.name" : nil,
                            presentation: presentation.value
                        )
                            .onTapGesture { selectedTab = .card }''', '''                        if store.syncStatus == .syncing && !store.hasProfile {
                            VizitProfileSkeleton()
                        } else {
                            VizitDigitalCard(
                                profile: store.profile,
                                nameIdentifier: store.hasProfile ? "card.name" : nil,
                                presentation: presentation.value
                            ).onTapGesture { selectedTab = .card }
                        }''')
t = one(t, '            VizitRow(label: entry.0, systemImage: entry.1, supporting: entry.2) {', '            VizitContactRow(title: entry.0, subtitle: entry.2, systemImage: entry.1) {')
a = t.index('private struct EducationCatalogScreen:'); b = t.index('private struct CourseCard:', a)
t = t[:a] + r'''private struct EducationCatalogScreen: View {
    @State private var selectedCategory = "Mind"
    @State private var searchText = ""
    @AppStorage("education.completedLessonIDs") private var completedLessonIDs = ""
    private let categories = ["Mind", "AI", "Digitális munka", "Cégépítés", "Biztonság", "Marketing", "Pénzügy"]
    private var completed: Set<String> { Set(completedLessonIDs.split(separator: "|").map(String.init)) }
    private var totalLessonCount: Int { businessTopics.reduce(0) { $0 + $1.lessons.count } }
    private var completedLessonCount: Int {
        completed.intersection(Set(businessTopics.flatMap { $0.lessons.map(\.id) })).count
    }
    private var filtered: [BusinessTopic] {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        return businessTopics.filter { topic in
            let categoryMatches = selectedCategory == "Mind" || (selectedCategory == "AI" && topic.id == "ai") || topic.category == selectedCategory
            let searchable = ([topic.title, topic.description, topic.category] + topic.lessons.map(\.title)).joined(separator: " ")
            return categoryMatches && (query.isEmpty || searchable.localizedStandardContains(query))
        }
    }
    var body: some View {
        PortalScroll(title: "Vállalkozói Edukáció", subtitle: "Rövid, gyakorlatias tananyagok, saját tempóban.") {
            VizitProgressCard(title: "A haladásod", current: completedLessonCount, total: totalLessonCount,
                supporting: "A haladás a készüléken tárolódik, a ténylegesen teljesített videóleckék alapján.")
            VizitSearchField(text: $searchText, placeholder: "Kurzusok és leckék keresése", resultCount: filtered.count)
                .accessibilityIdentifier("portal.courseSearch")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: VizitSpace.xs) {
                    ForEach(categories, id: \.self) { category in
                        VizitChip(text: category, selected: category == selectedCategory) { selectedCategory = category }
                            .accessibilityIdentifier("portal.category.\(category)")
                    }
                }
            }
            if filtered.isEmpty {
                VizitSearchEmptyState(query: searchText.isEmpty ? selectedCategory : searchText) {
                    searchText = ""; selectedCategory = "Mind"
                }.accessibilityIdentifier("portal.searchEmpty")
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: VizitSpace.sm) {
                        ForEach(filtered) { course in
                            NavigationLink { CourseDetailScreen(course: course) } label: {
                                CourseCard(course: course, completedCount: completed.intersection(Set(course.lessons.map(\.id))).count)
                            }
                            .buttonStyle(.plain)
                            .accessibilityIdentifier("portal.course.\(course.id)")
                        }
                    }.padding(.horizontal, VizitSpace.md)
                }.padding(.horizontal, -VizitSpace.md)
            }
        }
    }
}

''' + t[b:]
a = t.index('private struct PortalScroll<'); b = t.index('// MARK:', a)
u = t[a:b]
u = one(u, '                    Text(subtitle)', '                    VizitTabHeader(title: title)\n                    Text(subtitle)')
u = one(u, '.navigationTitle(title)', '.navigationTitle("")')
t = t[:a] + u + t[b:]
t = one(t, '    @AppStorage("figma.automaticSyncEnabled") private var automaticSyncEnabled = true', '''    private var automaticSync: Binding<Bool> {
        Binding(get: { store.automaticSyncEnabled }, set: { store.setAutomaticSync($0) })
    }''')
t = one(t, '                                isOn: $automaticSyncEnabled', '''                                isOn: automaticSync,
                                isEnabled: store.authStatus != .sessionExpired,
                                isError: store.syncStatus == .failed,
                                isLoading: store.syncStatus == .syncing''')
t = one(t, '''                        VizitButton(title: "Fiók végleges törlése", kind: .destructive) {
                            confirmDelete = false
                            Task { await store.deleteAccount(confirmation: deletionPhrase) }
                        }''', '''                        VizitConfirmationCard(
                            title: "Fiók végleges törlése",
                            message: "A művelet nem vonható vissza. A másokhoz már átadott névjegymásolatokat nem törli.",
                            confirmTitle: "Végleges törlés", destructive: true,
                            isLoading: store.busy,
                            isEnabled: AuthValidation.deletionPhrase(deletionPhrase) == nil,
                            onCancel: { confirmDelete = false },
                            onConfirm: { Task { await store.deleteAccount(confirmation: deletionPhrase) } }
                        )''')
t = one(t, '                            VizitBanner(text: "A profil hivatkozását a vágólapra másoltuk.", tone: .success)', '                            VizitToast(text: "A profil hivatkozását a vágólapra másoltuk.")')
t = one(t, '                            VizitBanner(text: "A QR-kódot elmentettük a Fotók közé.", tone: .success)', '                            VizitToast(text: "A QR-kódot elmentettük a Fotók közé.")')
a = t.index('struct ScanFlow: View'); b = t.index('// MARK: - Business portal', a)
original = t[a:b]
parser = original[original.index('    private func processScan()'):]
t = t[:a] + r'''struct ScanFlow: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @Environment(\.scenePhase) private var scenePhase
    @State private var scanning = true
    @State private var torchOn = false
    @State private var torchAvailable = false
    @State private var permission = AVCaptureDevice.authorizationStatus(for: .video)
    @State private var requestingPermission = false
    @State private var pickedCode: PhotosPickerItem?
    @State private var decodeTask: Task<Void, Never>?
    @State private var decodingImage = false
    @State private var pendingText: String?
    @State private var incoming: IncomingContact?
    @State private var link: URL?
    @State private var message: String?

    private var isUITesting: Bool {
        #if DEBUG
        return ProcessInfo.processInfo.arguments.contains("--ui-testing")
        #else
        return false
        #endif
    }
    private var canUseCamera: Bool { permission == .authorized || isUITesting }
    var body: some View {
        NavigationStack {
            Group {
                if canUseCamera { cameraSurface }
                else { permissionSurface }
            }
            .navigationTitle("Beolvasás")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(canUseCamera ? Color.black : VizitColor.canvas, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(canUseCamera ? .dark : nil, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Bezárás") { torchOn = false; dismiss() }.frame(minHeight: VizitMetrics.minTouchTarget)
                }
                ToolbarItem(placement: .primaryAction) {
                    if canUseCamera {
                        Button(torchOn ? "Vaku ki" : "Vaku") { torchOn.toggle() }
                            .frame(minHeight: VizitMetrics.minTouchTarget)
                            .disabled((!torchAvailable && !isUITesting) || decodingImage)
                            .accessibilityLabel(torchOn ? "Vaku kikapcsolása" : "Vaku bekapcsolása")
                    }
                }
            }
            .onAppear { refreshPermission() }
            .onChange(of: scenePhase) { phase in
                if phase == .active { refreshPermission() }
                else { torchOn = false }
            }
            .onChange(of: pickedCode) { _ in decodePickedCode() }
            .onDisappear { torchOn = false; decodeTask?.cancel() }
            .sheet(item: $incoming) { item in
                ContactEditor(contact: item.contact) { saved in
                    incoming = nil
                    if saved { message = "A névjegyet elmentetted a Kontaktokba." }
                    else { scanning = true }
                }
            }
            .alert("Webcím a QR-kódban", isPresented: Binding(
                get: { link != nil }, set: { if !$0 { link = nil; scanning = true } }
            )) {
                Button("Mégse", role: .cancel) { link = nil; scanning = true }
                Button("Megnyitás") {
                    if let url = link { openURL(url) }
                    link = nil; scanning = true
                }
            } message: { Text(link?.absoluteString ?? "") }
            .alert("Beolvasás", isPresented: Binding(
                get: { message != nil && incoming == nil }, set: { if !$0 { message = nil; scanning = true } }
            )) {
                Button("Rendben", role: .cancel) { message = nil; scanning = true }
            } message: { Text(message ?? "") }
        }
    }
    private var cameraSurface: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if isUITesting {
                VizitColor.elevated.environment(\.colorScheme, .dark).ignoresSafeArea()
            } else if scanning && !decodingImage {
                QRScanner(onResult: finishScan, onError: { text in
                    refreshPermission()
                    if permission == .authorized { message = text }
                    torchOn = false; scanning = false
                }, torchOn: torchOn, onTorchAvailability: { torchAvailable = $0 }).ignoresSafeArea()
            }
            ScannerOverlay(isSweeping: scanning && !decodingImage) { galleryPicker }
            if decodingImage {
                VizitLoadingState(message: "QR-kód olvasása a képből…")
                    .environment(\.colorScheme, .dark).background(Color.black.opacity(0.72))
            }
        }
    }
    private var permissionSurface: some View {
        VizitScreen {
            ScrollView {
                VStack(spacing: VizitSpace.lg) {
                    switch permission {
                    case .notDetermined:
                        VizitPermissionCard(systemImage: "camera", title: "Kameraengedély szükséges",
                            message: "A kamera a QR-kód beolvasásához kell. Felvételt nem készítünk, és a kameraképet nem tároljuk.",
                            primaryTitle: "Kamera engedélyezése", secondaryTitle: "Most nem",
                            primaryAction: requestPermission, secondaryAction: { dismiss() })
                            .disabled(requestingPermission)
                    case .denied, .restricted:
                        VizitPermissionDeniedState {
                            guard let settings = URL(string: UIApplication.openSettingsURLString) else { return }
                            openURL(settings)
                        }.accessibilityIdentifier("scanner.permissionDenied")
                    default:
                        VizitErrorState(title: "A kamera nem érhető el",
                            message: "Válassz QR-kódot tartalmazó képet, vagy ellenőrizd a kamera hozzáférését.",
                            onRetry: refreshPermission)
                    }
                    if requestingPermission || decodingImage {
                        VizitLoadingState(message: requestingPermission ? "Várakozás az engedélyre…" : "QR-kód olvasása…")
                    }
                    VizitInlineMessage(text: "Kameraengedély nélkül is beolvashatsz egy saját képernyőképet.")
                    galleryPicker
                }.padding(VizitSpace.md).frame(maxWidth: 560).frame(maxWidth: .infinity)
            }
        }
    }
    private var galleryPicker: some View {
        PhotosPicker(selection: $pickedCode, matching: .images, photoLibrary: .shared()) {
            Label("Kód kiválasztása a képtárból", systemImage: "photo")
                .font(VizitFont.label)
                .foregroundStyle(canUseCamera ? Color.white : VizitColor.primary)
                .padding(.horizontal, VizitSpace.md)
                .frame(minHeight: VizitMetrics.minTouchTarget)
                .background(canUseCamera ? Color.white.opacity(0.14) : VizitColor.primarySubtle)
                .clipShape(Capsule())
        }.disabled(decodingImage).accessibilityIdentifier("scanner.photo")
    }
    private func refreshPermission() {
        permission = AVCaptureDevice.authorizationStatus(for: .video)
        if canUseCamera && incoming == nil && link == nil && message == nil && !decodingImage { scanning = true }
    }
    private func requestPermission() {
        guard !requestingPermission, permission == .notDetermined else { return }
        requestingPermission = true
        Task { @MainActor in
            _ = await AVCaptureDevice.requestAccess(for: .video)
            requestingPermission = false
            refreshPermission()
        }
    }
    private func finishScan(_ text: String) {
        pendingText = text; torchOn = false; scanning = false
        processScan()
    }
    private func decodePickedCode() {
        guard let selected = pickedCode else { return }
        decodeTask?.cancel()
        decodingImage = true; torchOn = false; scanning = false
        decodeTask = Task { @MainActor in
            defer { if !Task.isCancelled { decodingImage = false } }
            do {
                guard let data = try await selected.loadTransferable(type: Data.self) else { throw ProfileError.invalidPhoto }
                try Task.checkCancellation()
                let result = await Task.detached(priority: .userInitiated) { QRImageDecoder.decode(data) }.value
                try Task.checkCancellation()
                pickedCode = nil
                guard let value = result else {
                    message = "Ezen a képen nem találtunk egyetlen egyértelmű QR-kódot sem."
                    return
                }
                finishScan(value)
            } catch is CancellationError {
            } catch { message = "A kép nem olvasható. Válassz másik képet." }
        }
    }

''' + parser + t[b:]
save(p, t)
print('Integrated feedback, permission, session, search and automatic-sync controls.')
