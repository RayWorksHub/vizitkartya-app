import SwiftUI

// MARK: - Kártya megjelenése

/// The card material and layout picker from the approved iOS board.
/// Changes stay as a draft until the explicit Mentés action is used.
struct CardAppearanceScreen: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var store: CardPresentationStore
    let profile: ContactProfile
    @State private var draft: CardPresentation

    init(store: CardPresentationStore, profile: ContactProfile) {
        self.store = store
        self.profile = profile
        _draft = State(initialValue: store.value)
    }

    private var layoutIndex: Binding<Int> {
        Binding(
            get: { CardLayout.allCases.firstIndex(of: draft.layout) ?? 0 },
            set: {
                guard CardLayout.allCases.indices.contains($0) else { return }
                draft.layout = CardLayout.allCases[$0]
            }
        )
    }

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.lg) {
                        VizitBrandLockup(maxHeight: 52)

                        Text("Kártya megjelenése")
                            .font(VizitFont.h1)
                            .foregroundStyle(VizitColor.textPrimary)
                            .accessibilityAddTraits(.isHeader)

                        Text("Az adataid és az elrendezés minden változatban ugyanazok — csak az anyag más.")
                            .font(VizitFont.body)
                            .foregroundStyle(VizitColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)

                        VizitSectionHeader(title: "Anyag")
                        materials

                        VizitSectionHeader(title: "Elrendezés")
                        VizitSegmentedControl(
                            options: CardLayout.allCases.map(\.label),
                            selection: layoutIndex,
                            accessibilityIdentifier: "card.layout"
                        )
                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.bottom, VizitSpace.huge * 2)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                }
                .safeAreaInset(edge: .bottom) {
                    VizitButton(title: "Mentés", systemImage: "checkmark") {
                        store.value = draft
                        dismiss()
                    }
                    .accessibilityIdentifier("card.appearance.save")
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.vertical, VizitSpace.sm)
                    .background(.regularMaterial)
                }
            }
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Mégse") { dismiss() }
                }
            }
        }
    }

    private var materials: some View {
        HStack(spacing: VizitSpace.sm) {
            ForEach(CardColorway.allCases, id: \.self) { colorway in
                Button {
                    draft.colorway = colorway
                } label: {
                    VStack(spacing: VizitSpace.xs) {
                        MaterialPreview(colorway: colorway)
                        Text(colorway.label)
                            .font(VizitFont.caption)
                            .foregroundStyle(VizitColor.textPrimary)
                    }
                    .padding(VizitSpace.xs)
                    .frame(maxWidth: .infinity)
                    .background(VizitColor.surface)
                    .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))
                    .overlay {
                        RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous)
                            .stroke(
                                draft.colorway == colorway ? VizitColor.primary : VizitColor.border,
                                lineWidth: draft.colorway == colorway ? 2 : 1
                            )
                    }
                    .overlay(alignment: .topTrailing) {
                        if draft.colorway == colorway {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(VizitColor.primary)
                                .background(Color(.systemBackground), in: Circle())
                                .padding(6)
                        }
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(colorway.label)
                .accessibilityIdentifier("card.material.\(colorway.rawValue)")
                .accessibilityAddTraits(
                    draft.colorway == colorway ? [.isButton, .isSelected] : .isButton
                )
            }
        }
    }
}

private struct MaterialPreview: View {
    let colorway: CardColorway

    private var foreground: Color {
        colorway.isLight ? VizitColor.ink : .white
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
                .frame(width: 3)
            VStack(alignment: .leading, spacing: 5) {
                Circle()
                    .fill(foreground.opacity(0.18))
                    .frame(width: 22, height: 22)
                RoundedRectangle(cornerRadius: 2)
                    .fill(foreground.opacity(0.92))
                    .frame(width: 42, height: 5)
                RoundedRectangle(cornerRadius: 2)
                    .fill(foreground.opacity(0.48))
                    .frame(width: 30, height: 3)
            }
            .padding(10)
        }
        .aspectRatio(1.36, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.sm, style: .continuous))
        .overlay {
            if colorway.isLight {
                RoundedRectangle(cornerRadius: VizitRadius.sm, style: .continuous)
                    .stroke(VizitColor.border, lineWidth: 1)
            }
        }
        .accessibilityHidden(true)
    }
}

// MARK: - Adatláthatóság

/// Per-field control over what leaves the device. Everything switched off here
/// is stripped from the card, the QR and the shared vCard alike.
struct DataVisibilityScreen: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var store: CardPresentationStore
    let profile: ContactProfile
    let isPublicProfile: Bool

    private struct Field: Identifiable {
        let id: String
        let label: String
        let value: String
        let binding: Binding<Bool>
    }

    private var fields: [Field] {
        [
            Field(id: "company", label: "Cég és beosztás",
                  value: [profile.company, profile.jobTitle].filter { !$0.isEmpty }.joined(separator: " · "),
                  binding: $store.value.sharesCompany),
            Field(id: "email", label: "E-mail-cím", value: profile.email,
                  binding: $store.value.sharesEmail),
            Field(id: "phone", label: "Telefonszám", value: profile.phone,
                  binding: $store.value.sharesPhone),
            Field(id: "website", label: "Weboldal", value: profile.website,
                  binding: $store.value.sharesWebsite),
            Field(id: "social", label: "Közösségi profilok",
                  value: profile.socialProfiles.filter { !$0.url.isEmpty }
                      .map(\.platform.label).joined(separator: ", "),
                  binding: $store.value.sharesSocial),
            Field(id: "address", label: "Lakcím", value: profile.address,
                  binding: $store.value.sharesAddress)
        ]
    }

    /// Who can actually see a field, given both the switch and whether the
    /// public web profile is turned on at all.
    private func audience(_ shared: Bool) -> String {
        guard shared else { return "Senki" }
        return isPublicProfile ? "Mindenki" : "Csak akivel megosztod"
    }

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.lg) {
                        Text("Te döntöd el, mi látszik. Amit itt kikapcsolsz, az sem a megosztott névjegyen, sem a nyilvános weboldaladon nem jelenik meg.")
                            .font(VizitFont.body)
                            .foregroundStyle(VizitColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)

                        VizitSectionHeader(title: "Mindig látható")
                        VizitGroup {
                            VizitRow(
                                label: "Teljes név",
                                supporting: "A névjegy alapja, nem kapcsolható ki.",
                                showsChevron: false
                            ) {
                                VizitStatusPill(text: "kötelező", tone: .info)
                            }
                        }

                        VizitSectionHeader(title: "Mezőnként állítható")
                        VizitGroup {
                            ForEach(Array(fields.enumerated()), id: \.element.id) { index, field in
                                if index > 0 { VizitDivider() }
                                VizitRow(
                                    label: field.label,
                                    supporting: field.value.isEmpty
                                        ? "Még nincs kitöltve"
                                        : audience(field.binding.wrappedValue),
                                    isEnabled: !field.value.isEmpty,
                                    showsChevron: false
                                ) {
                                    Toggle("", isOn: field.binding)
                                        .labelsHidden()
                                        .disabled(field.value.isEmpty)
                                }
                            }
                        }

                        Text("A már megosztott névjegyeken a változás a következő szinkronnál jelenik meg.")
                            .font(VizitFont.caption)
                            .foregroundStyle(VizitColor.textMuted)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .padding(.horizontal, VizitSpace.md)
                    .padding(.bottom, VizitSpace.xxl)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("Adatláthatóság")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Kész") { dismiss() }
                }
            }
        }
    }
}
