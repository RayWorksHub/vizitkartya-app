import SwiftUI

// MARK: - Kártya megjelenése

/// The card's material, layout and visible elements, decided against a live
/// preview of the owner's own card rather than abstract swatches.
struct CardAppearanceScreen: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var store: CardPresentationStore
    let profile: ContactProfile

    private var layoutIndex: Binding<Int> {
        Binding(
            get: { store.value.layout == .portrait ? 0 : 1 },
            set: { store.value.layout = $0 == 0 ? .portrait : .landscape }
        )
    }

    var body: some View {
        NavigationStack {
            VizitScreen {
                ScrollView {
                    VStack(alignment: .leading, spacing: VizitSpace.lg) {
                        VStack(alignment: .leading, spacing: VizitSpace.xs) {
                            VizitDigitalCard(profile: profile, presentation: store.value)
                            Text("Élő előnézet — pontosan ezt látja, akivel megosztod.")
                                .font(VizitFont.caption)
                                .foregroundStyle(VizitColor.textMuted)
                                .frame(maxWidth: .infinity, alignment: .center)
                        }
                        .animation(.easeInOut(duration: 0.2), value: store.value)

                        VizitSectionHeader(title: "Színvilág")
                        colorways

                        VizitSectionHeader(title: "Elrendezés")
                        VizitSegmentedControl(
                            options: CardLayout.allCases.map(\.label),
                            selection: layoutIndex
                        )

                        VizitSectionHeader(title: "Megjelenő elemek")
                        VizitGroup {
                            toggleRow(
                                "Profilkép",
                                supporting: profile.photoBase64.isEmpty
                                    ? "Még nincs feltöltött profilképed."
                                    : nil,
                                isOn: $store.value.showsPhoto
                            )
                            VizitDivider()
                            toggleRow(
                                "QR-kód a kártyán",
                                supporting: "A kártya sarkába kerül, így egy fotóról is beolvasható.",
                                isOn: $store.value.showsQR
                            )
                            VizitDivider()
                            toggleRow(
                                "Közösségi profilok",
                                supporting: store.value.sharesSocial
                                    ? nil
                                    : "Az Adatláthatóságban most ki van kapcsolva.",
                                isOn: $store.value.showsSocial,
                                isEnabled: store.value.sharesSocial
                            )
                        }

                        Text("A megjelenés csak ezen a készüléken változik; a névjegyed adatai érintetlenek maradnak.")
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
            .navigationTitle("Kártya megjelenése")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Kész") { dismiss() }
                }
            }
        }
    }

    private var colorways: some View {
        VizitPanel {
            HStack(spacing: VizitSpace.sm) {
                ForEach(CardColorway.allCases, id: \.self) { colorway in
                    Button {
                        store.value.colorway = colorway
                    } label: {
                        ZStack {
                            Circle()
                                .fill(
                                    LinearGradient(
                                        colors: [
                                            Color(uiColor: UIColor(hex: colorway.gradient.start)),
                                            Color(uiColor: UIColor(hex: colorway.gradient.end))
                                        ],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                                .frame(width: 38, height: 38)
                            if store.value.colorway == colorway {
                                Circle()
                                    .stroke(VizitColor.primary, lineWidth: 2.5)
                                    .frame(width: 46, height: 46)
                                Image(systemName: "checkmark")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundStyle(Color(uiColor: UIColor(hex: colorway.accent)))
                            }
                        }
                        .frame(width: VizitMetrics.minTouchTarget, height: VizitMetrics.minTouchTarget)
                        .contentShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(colorway.label)
                    .accessibilityAddTraits(
                        store.value.colorway == colorway ? [.isButton, .isSelected] : .isButton
                    )
                }
            }
            .frame(maxWidth: .infinity)
        }
    }

    private func toggleRow(
        _ label: String,
        supporting: String? = nil,
        isOn: Binding<Bool>,
        isEnabled: Bool = true
    ) -> some View {
        VizitRow(label: label, supporting: supporting, isEnabled: isEnabled, showsChevron: false) {
            Toggle("", isOn: isOn)
                .labelsHidden()
                .disabled(!isEnabled)
        }
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
