from pathlib import Path

ROOT = Path.cwd()
def load(p): return (ROOT / p).read_text(encoding='utf-8')
def save(p, text): (ROOT / p).write_text(text, encoding='utf-8')
def one(text, before, after):
    n = text.count(before)
    if n != 1: raise RuntimeError(f'Expected one anchor; found {n}: {before[:80]}')
    return text.replace(before, after, 1)

p='ios/Vizit/Core/CardPresentation.swift'; t=load(p)
t=one(t,'    /// The number of optional fields currently shared, for the Settings row.',r'''    /// A stable, complete ordering even after loading an older or duplicate list.
    public var orderedSections: [CardSection] {
        var result: [CardSection] = []
        for section in sectionOrder + CardSection.allCases where !result.contains(section) {
            result.append(section)
        }
        return result
    }

    /// The number of optional fields currently shared, for the Settings row.''')
t=one(t,'try values.encode(sectionOrder, forKey: .sectionOrder)','try values.encode(orderedSections, forKey: .sectionOrder)')
save(p,t)

p='ios/Vizit/App/ProfileEditor.swift'; t=load(p)
t=one(t,'    @State private var showSectionOrder = false', '''    @State private var showSectionOrder = false
    @State private var draftPresentation = CardPresentation()
    @State private var presentationLoaded = false''')
t=one(t,'                        sharingSection\n','''                        sharingSection
                        presentationSection
''')
t=one(t,'            .navigationTitle("Névjegy szerkesztése")','''            .onAppear {
                if !presentationLoaded {
                    draftPresentation = presentation.value
                    presentationLoaded = true
                }
            }
            .navigationTitle("Névjegy szerkesztése")''')
t=one(t,'                            let previous = store.profile','''                            let previous = store.profile
                            let previousPresentation = presentation.value
                            let savedPresentation = draftPresentation''')
t=one(t,'                            try store.save(draft)\n                            dismiss()','''                            try store.save(draft)
                            presentation.value = savedPresentation
                            dismiss()''')
t=one(t,'guard store.accountIdentifier == owner, store.profile == savedProfile else {','guard store.accountIdentifier == owner, store.profile == savedProfile,\n                                          presentation.value == savedPresentation else {')
t=one(t,'                                        try store.save(previous)','''                                        try store.save(previous)
                                        presentation.value = previousPresentation''')
t=t.replace('presentation.value.companyLogoBase64','draftPresentation.companyLogoBase64')
t=one(t,'CardSectionOrderScreen(store: presentation)','CardSectionOrderScreen(presentation: $draftPresentation, profile: draft)')
entry='''                    VizitPanel {
                        VizitRow(
                            label: "Szekciók sorrendje",
                            systemImage: "line.3.horizontal.decrease",
                            supporting: "A névjegy blokkjainak sorrendje"
                        ) { showSectionOrder = true }
                    }

'''
t=one(t,entry,'')
t=one(t,'    private var sharingSection: some View {',r'''    private var presentationSection: some View {
        group("Kártya előnézete") {
            VizitDigitalCard(profile: draft, presentation: draftPresentation)
                .accessibilityIdentifier("profile.livePreview")
            VizitPanel(padding: 0) {
                VizitRow(label: "Szekciók sorrendje", systemImage: "line.3.horizontal.decrease",
                         supporting: "A sorrend az előnézeten is azonnal megváltozik.") {
                    showSectionOrder = true
                }
                .accessibilityIdentifier("profile.sectionOrder")
            }
            VizitInlineMessage(text: "A logó és a sorrend a Mentés gombbal véglegesül. A Mégse elveti ezeket a módosításokat.")
        }
    }

    private var sharingSection: some View {''')
a=t.index('private struct CardSectionOrderScreen:'); b=t.index('private enum DomainState',a)
t=t[:a]+r'''struct CardSectionOrderScreen: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var presentation: CardPresentation
    let profile: ContactProfile
    @State private var draft = CardPresentation()
    @State private var loaded = false

    var body: some View {
        NavigationStack {
            List {
                Section("Élő előnézet") {
                    VizitDigitalCard(profile: profile, presentation: draft)
                        .listRowInsets(EdgeInsets(top: 12, leading: 12, bottom: 12, trailing: 12))
                }
                Section {
                    ForEach(draft.orderedSections) { section in
                        Label(section.label, systemImage: "line.3.horizontal")
                            .font(VizitFont.body)
                            .frame(minHeight: VizitMetrics.minTouchTarget)
                            .accessibilityIdentifier("card.section.\(section.rawValue)")
                    }
                    .onMove { indices, offset in
                        var order = draft.orderedSections
                        order.move(fromOffsets: indices, toOffset: offset)
                        draft.sectionOrder = order
                    }
                } header: { Text("Szekciók") }
                footer: { Text("Húzd a szekciókat a kívánt sorrendbe. A kötelező név nem kapcsolható ki.") }
            }
            .environment(\.editMode, .constant(.active))
            .onAppear { if !loaded { draft = presentation; loaded = true } }
            .navigationTitle("Szekciók sorrendje")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Mégse") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Kész") { presentation = draft; dismiss() }
                }
            }
        }
    }
}

'''+t[b:]
save(p,t)

p='ios/Vizit/Design/VizitDigitalCard.swift'; t=load(p)
t=one(t,'    private var visible: ContactProfile', '    @Environment(\\.dynamicTypeSize) private var dynamicTypeSize\n\n    private var visible: ContactProfile')
t=one(t,'presentation.colorway.isLight ? VizitColor.textSecondary : .white.opacity(0.72)', 'presentation.colorway.isLight ? VizitColor.ink.opacity(0.72) : .white.opacity(0.72)')
t=one(t,'''    private var surface: some View {
        ZStack(alignment: .leading) {''','''    @ViewBuilder private var surface: some View {
        if presentation.orderedSections != CardSection.allCases || dynamicTypeSize.isAccessibilitySize {
            orderedContent
                .padding(VizitSpace.lg)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background {
                    LinearGradient(colors: [
                        Color(uiColor: UIColor(hex: presentation.colorway.gradient.start)),
                        Color(uiColor: UIColor(hex: presentation.colorway.gradient.end))
                    ], startPoint: .topLeading, endPoint: .bottomTrailing)
                }
                .overlay(alignment: .leading) { Rectangle().fill(accent).frame(width: 4) }
                .clipShape(RoundedRectangle(cornerRadius: VizitRadius.xl, style: .continuous))
                .vizitShadow(VizitElevation.card)
        } else { standardSurface }
    }

    private var standardSurface: some View {
        ZStack(alignment: .leading) {''')
t=one(t,'    /// Portrait is the signature VIZIT composition',r'''    private var orderedContent: some View {
        let centered = presentation.layout == .portrait
        return VStack(alignment: centered ? .center : .leading, spacing: VizitSpace.md) {
            avatar(size: centered ? 82 : 56)
            ForEach(presentation.orderedSections) { section in
                orderedSection(section, centered: centered)
            }
            Text("VIZIT").vizitOverline().foregroundStyle(accent)
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .frame(maxWidth: .infinity, alignment: centered ? .center : .leading)
        .multilineTextAlignment(centered ? .center : .leading)
    }

    @ViewBuilder private func orderedSection(_ section: CardSection, centered: Bool) -> some View {
        switch section {
        case .identity:
            Text(visible.displayName.isEmpty ? "Állítsd össze a névjegyed" : visible.displayName)
                .font(VizitFont.h3).foregroundStyle(primaryText)
                .fixedSize(horizontal: false, vertical: true)
                .vizitIdentifier(nameIdentifier)
        case .company:
            if presentation.sharesCompany {
                VStack(alignment: centered ? .center : .leading, spacing: VizitSpace.xxs) {
                    if !visible.jobTitle.isEmpty {
                        Text(visible.jobTitle).font(VizitFont.caption).foregroundStyle(accent)
                    }
                    if !visible.company.isEmpty {
                        Text(visible.company).font(VizitFont.bodySmall).foregroundStyle(secondaryText)
                    }
                    companyLogo(size: 24)
                }
            }
        case .contact:
            VStack(alignment: centered ? .center : .leading, spacing: VizitSpace.xxs) {
                ForEach(Array([visible.phone, visible.email, visible.website].filter { !$0.isEmpty }.enumerated()), id: \.offset) { _, text in
                    Text(text).font(VizitFont.bodySmall).foregroundStyle(secondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
        case .social:
            let names = visible.socialProfiles.filter { !$0.url.isEmpty }.map(\.platform.label)
            if !names.isEmpty {
                Text(names.joined(separator: " · ")).font(VizitFont.caption).foregroundStyle(secondaryText)
                    .fixedSize(horizontal: false, vertical: true)
            }
        case .address:
            if !visible.address.isEmpty {
                Text(visible.address).font(VizitFont.caption).foregroundStyle(secondaryText)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    /// Portrait is the signature VIZIT composition''')
t=one(t,'        if let data = Data(base64Encoded: presentation.companyLogoBase64),','        if presentation.sharesCompany, let data = Data(base64Encoded: presentation.companyLogoBase64),')
save(p,t)

p='ios/Vizit/App/CardCustomization.swift'; t=load(p)
t=one(t,'    @ObservedObject var store: CardPresentationStore\n    let profile: ContactProfile\n    @State private var draft:', '    @ObservedObject var store: CardPresentationStore\n    @EnvironmentObject private var feedback: VizitFeedbackCenter\n    let profile: ContactProfile\n    @State private var ordering = false\n    @State private var draft:')
t=one(t,'''                            accessibilityIdentifier: "card.layout"
                        )''','''                            accessibilityIdentifier: "card.layout"
                        )
                        VizitPanel(padding: 0) {
                            VizitRow(label: "Szekciók sorrendje", systemImage: "line.3.horizontal.decrease") {
                                ordering = true
                            }
                        }''')
t=one(t,'''                        store.value = draft
                        dismiss()''','''                        store.value = draft
                        dismiss()
                        feedback.show("A kártya megjelenése mentve.")''')
t=one(t,'            .navigationTitle("")','''            .sheet(isPresented: $ordering) {
                CardSectionOrderScreen(presentation: $draft, profile: profile)
            }
            .navigationTitle("")''')
save(p,t)

p='ios/Vizit/App/VizitApp.swift'; t=load(p)
t=one(t,'enum RootTab: Hashable {\n    case home, card, share, settings\n}',r'''enum RootTab: String, CaseIterable, Hashable {
    case home, card, share, settings
    var title: String {
        switch self {
        case .home: return "Kezdőlap"
        case .card: return "Névjegy"
        case .share: return "Megosztás"
        case .settings: return "Beállítások"
        }
    }
    var symbol: String {
        switch self {
        case .home: return "house"
        case .card: return "person.crop.rectangle"
        case .share: return "square.and.arrow.up"
        case .settings: return "gearshape"
        }
    }
}''')
a=t.index('struct RootView: View {')
t=t[:a]+r'''struct RootView: View {
    @EnvironmentObject private var store: AppStore
    @Binding var themeMode: ThemeMode
    @State private var selection: RootTab = .home
    @State private var visited: Set<RootTab> = [.home]

    var body: some View {
        ZStack {
            ForEach(RootTab.allCases, id: \.self) { tab in
                if visited.contains(tab) || selection == tab {
                    destination(tab)
                        .opacity(selection == tab ? 1 : 0)
                        .allowsHitTesting(selection == tab)
                        .accessibilityHidden(selection != tab)
                        .zIndex(selection == tab ? 1 : 0)
                }
            }
        }
        .onChange(of: selection) { visited.insert($0) }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            VStack(spacing: 0) {
                VizitFeedbackHost()
                VizitBottomNavigation(selection: $selection)
            }
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            if store.authStatus == .offline {
                VizitBanner(text: "Offline mód – a helyi névjegyed olvasható és szerkeszthető.", tone: .info)
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.vertical, VizitSpace.xxs)
            }
        }
        .background(VizitColor.canvas)
        .tint(VizitColor.primary)
    }

    @ViewBuilder private func destination(_ tab: RootTab) -> some View {
        switch tab {
        case .home: HomeScreen(selectedTab: $selection)
        case .card: CardScreen(selectedTab: $selection)
        case .share: ShareScreen()
        case .settings: SettingsScreen(themeMode: $themeMode)
        }
    }
}

/// The flat four-destination bar in the approved PDF, independent of OS chrome.
private struct VizitBottomNavigation: View {
    @Binding var selection: RootTab
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        VStack(spacing: 0) {
            VizitDivider()
            HStack(spacing: 0) {
                ForEach(RootTab.allCases, id: \.self) { tab in
                    Button { selection = tab } label: {
                        VStack(spacing: VizitSpace.xxs) {
                            Image(systemName: tab.symbol)
                                .font(.system(size: 21, weight: .regular))
                            Text(tab.title).font(VizitFont.caption)
                                .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 2)
                                .fixedSize(horizontal: false, vertical: true)
                                .multilineTextAlignment(.center)
                        }
                        .foregroundStyle(selection == tab ? VizitColor.primary : VizitColor.textMuted)
                        .frame(maxWidth: .infinity, minHeight: 56)
                        .padding(.vertical, VizitSpace.xxs)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(tab.title)
                    .accessibilityAddTraits(selection == tab ? [.isButton, .isSelected] : .isButton)
                    .accessibilityIdentifier("navigation.\(tab.rawValue)")
                }
            }
        }
        .background(VizitColor.surface.ignoresSafeArea(edges: .bottom))
    }
}
'''
save(p,t)

p='ios/Vizit/App/Screens.swift'; t=load(p)
a=t.index('struct CardScreen: View {'); b=t.index('// MARK: - Share',a)
u=t[a:b]
u=one(u,'    @State private var editing = false','''    @State private var editing = false
    @State private var customizing = false
    @State private var adjustingVisibility = false''')
start=u.index('                            VizitSectionHeader(title: "Elérhetőségek")')
end=u.index('                        }\n                    }',start)
u=u[:start]+'''                            ForEach(presentation.value.orderedSections) { section in
                                detailSection(section)
                            }
'''+u[end:]
u=one(u,'''                        }
                    }
                    .padding(.horizontal, VizitSpace.md)''','''                        }
                        VizitGroup {
                            VizitRow(label: "Kártya megjelenése", systemImage: "rectangle.on.rectangle") {
                                customizing = true
                            }
                            VizitDivider()
                            VizitRow(label: "Adatok láthatósága", systemImage: "eye",
                                value: "\\(presentation.value.sharedFieldCount) látható") {
                                adjustingVisibility = true
                            }
                        }
                    }
                    .padding(.horizontal, VizitSpace.md)''')
u=one(u,'''            .sheet(isPresented: $editing) { ProfileEditor(draft: store.profile) }''','''            .sheet(isPresented: $editing) { ProfileEditor(draft: store.profile) }
            .sheet(isPresented: $customizing) {
                CardAppearanceScreen(store: presentation, profile: store.profile)
            }
            .sheet(isPresented: $adjustingVisibility) {
                DataVisibilityScreen(store: presentation, profile: store.profile, isPublicProfile: store.profile.isPublic)
            }''')
u=one(u,'''    @ViewBuilder
    private var detailRows: some View {''',r'''    @ViewBuilder private func detailSection(_ section: CardSection) -> some View {
        switch section {
        case .identity: EmptyView()
        case .company:
            if !store.profile.company.isEmpty || !store.profile.jobTitle.isEmpty {
                VizitSectionHeader(title: "Munkahely")
                VizitGroup {
                    VizitRow(label: store.profile.company.isEmpty ? store.profile.jobTitle : store.profile.company,
                        systemImage: "building.2", supporting: emptyToNil(store.profile.jobTitle), showsChevron: false)
                }
            }
        case .contact:
            if ![store.profile.phone, store.profile.email, store.profile.website].allSatisfy(\.isEmpty) {
                VizitSectionHeader(title: "Elérhetőségek")
                VizitGroup { detailRows }
            }
        case .social:
            let profiles = store.profile.socialProfiles.filter { !$0.url.isEmpty }
            if !profiles.isEmpty {
                VizitSectionHeader(title: "Közösségi profilok")
                VizitGroup {
                    ForEach(Array(profiles.enumerated()), id: \.offset) { index, item in
                        if index > 0 { VizitDivider() }
                        VizitContactRow(title: item.platform.label, subtitle: item.url, systemImage: "link") {
                            if let url = SafeLink.https(item.url) { openURL(url) }
                        }
                    }
                }
            }
        case .address:
            if !store.profile.address.isEmpty {
                VizitSectionHeader(title: "Cím")
                VizitGroup {
                    VizitContactRow(title: store.profile.address, subtitle: "Megnyitás a térképen", systemImage: "mappin.and.ellipse") {
                        if let url = destination(for: store.profile.address, kind: "Cím") { openURL(url) }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var detailRows: some View {''')
u=one(u,'''            (store.profile.website, "globe", "Weboldal"),
            (store.profile.address, "mappin.and.ellipse", "Cím")''','''            (store.profile.website, "globe", "Weboldal")''')
t=t[:a]+u+t[b:]
t=one(t,'    @State private var videoUnavailable = false', '    @State private var videoUnavailable = false\n    @State private var playbackAttempt = 0')
t=one(t,'            .id(lesson.id)','            .id("\\(lesson.id)-\\(playbackAttempt)")')
t=one(t,'''                    Button("Megnyitás böngészőben") {''','''                    Button("Újrapróbálás") {
                        videoReady = false
                        videoUnavailable = false
                        playbackAttempt += 1
                    }
                    .font(VizitFont.label)
                    .foregroundStyle(CoursePlayer.accent)
                    .frame(minHeight: VizitMetrics.minTouchTarget)
                    Button("Megnyitás böngészőben") {''')
save(p,t)
print('Integrated flat navigation, presentation drafts, real card ordering and playback retry.')
