import SwiftUI

// MARK: - Buttons

enum VizitButtonStyleKind { case primary, secondary, tertiary, destructive }

/// The single button primitive. Covers default / pressed / disabled / loading
/// from the Figma Button set; pressed feedback is a 0.97 scale plus a colour
/// shift, enough to confirm the tap and nothing more.
struct VizitButton: View {
    let title: String
    var systemImage: String?
    var kind: VizitButtonStyleKind = .primary
    var isLoading = false
    var isEnabled = true
    /// Fixed colours for "island" surfaces that do not follow the theme.
    var containerOverride: Color?
    var contentOverride: Color?
    let action: () -> Void

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var pressed = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var active: Bool { isEnabled && !isLoading }

    private var container: Color {
        if let containerOverride { return pressed ? containerOverride.opacity(0.86) : containerOverride }
        guard active else { return VizitColor.controlDisabled }
        switch kind {
        case .primary: return pressed ? VizitColor.primaryPressed : VizitColor.primary
        case .secondary: return pressed ? VizitColor.sunken : VizitColor.surface
        case .tertiary: return pressed ? VizitColor.primarySubtle : .clear
        case .destructive: return VizitColor.error
        }
    }

    private var foreground: Color {
        if let contentOverride { return contentOverride }
        guard active else { return VizitColor.textDisabled }
        switch kind {
        case .primary, .destructive: return VizitColor.textOnBrand
        case .secondary: return VizitColor.textPrimary
        case .tertiary: return VizitColor.primary
        }
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: VizitSpace.xs) {
                if isLoading {
                    ProgressView().tint(foreground).controlSize(.small)
                } else if let systemImage {
                    Image(systemName: systemImage).font(.system(size: 17, weight: .semibold))
                }
                Text(title)
                    .font(VizitFont.button)
                    .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 2)
                    .fixedSize(horizontal: false, vertical: true)
                    .multilineTextAlignment(.center)
            }
            .foregroundStyle(foreground)
            .frame(maxWidth: .infinity)
            .frame(minHeight: VizitMetrics.controlHeight)
            .padding(.horizontal, VizitSpace.lg)
            .background(container)
            .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
            .overlay {
                if kind == .secondary, active, containerOverride == nil {
                    RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous)
                        .stroke(VizitColor.borderStrong, lineWidth: 1)
                }
            }
            .scaleEffect(pressed && active && !reduceMotion ? 0.97 : 1)
            .animation(reduceMotion ? nil : .easeOut(duration: 0.12), value: pressed)
        }
        .buttonStyle(.plain)
        .disabled(!active)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in pressed = true }
                .onEnded { _ in pressed = false }
        )
    }
}

/// Icon-only action with a full 44pt target regardless of the glyph size.
struct VizitIconButton: View {
    let systemImage: String
    let accessibilityTitle: String
    var tint: Color?
    var isEnabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 18, weight: .medium))
                .foregroundStyle(isEnabled ? (tint ?? VizitColor.textSecondary) : VizitColor.textDisabled)
                .frame(width: VizitMetrics.minTouchTarget, height: VizitMetrics.minTouchTarget)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .accessibilityLabel(accessibilityTitle)
    }
}

// MARK: - Fields

/// Label above the box, 52pt field, hairline border that thickens to 2pt on
/// focus and turns to state/error with a helper message when invalid.
struct VizitTextField: View {
    let label: String
    @Binding var text: String
    var systemImage: String?
    var placeholder: String = ""
    var helper: String?
    var error: String?
    var isEnabled = true
    var keyboard: UIKeyboardType = .default
    var contentType: UITextContentType?
    var autocapitalization: TextInputAutocapitalization = .sentences
    var isSecure = false
    /// Applied to the field element itself so UI tests can address it by type.
    var identifier: String?
    var submitLabel: SubmitLabel = .return
    var onSubmit: (() -> Void)?

    @FocusState private var focused: Bool
    @State private var revealed = false

    private var hasError: Bool { error != nil }

    private var borderColor: Color {
        if !isEnabled { return VizitColor.border }
        if hasError { return VizitColor.error }
        return focused ? VizitColor.borderFocus : VizitColor.border
    }

    var body: some View {
        VStack(alignment: .leading, spacing: VizitSpace.xxs + 2) {
            HStack(spacing: VizitSpace.xxs) {
                if let systemImage {
                    Image(systemName: systemImage)
                        .accessibilityHidden(true)
                }
                Text(label)
            }
            .font(VizitFont.label)
            .foregroundStyle(isEnabled ? VizitColor.textSecondary : VizitColor.textDisabled)

            HStack(spacing: VizitSpace.sm) {
                Group {
                    if isSecure && !revealed {
                        SecureField(placeholder, text: $text)
                    } else {
                        TextField(placeholder, text: $text)
                    }
                }
                .font(VizitFont.body)
                .foregroundStyle(isEnabled ? VizitColor.textPrimary : VizitColor.textDisabled)
                .keyboardType(keyboard)
                .textContentType(contentType)
                .textInputAutocapitalization(autocapitalization)
                .autocorrectionDisabled(keyboard == .emailAddress || keyboard == .URL)
                .accessibilityLabel(label)
                .focused($focused)
                .disabled(!isEnabled)
                .submitLabel(submitLabel)
                .onSubmit { onSubmit?() }
                .vizitIdentifier(identifier)

                if isSecure {
                    Button(revealed ? "Elrejt" : "Mutat") { revealed.toggle() }
                        .font(VizitFont.label)
                        .foregroundStyle(isEnabled ? VizitColor.primary : VizitColor.textDisabled)
                        .frame(minWidth: VizitMetrics.minTouchTarget, minHeight: VizitMetrics.minTouchTarget)
                        .buttonStyle(.plain)
                        .disabled(!isEnabled)
                        .accessibilityLabel(revealed ? "Jelszó elrejtése" : "Jelszó megjelenítése")
                }
            }
            .padding(.horizontal, VizitSpace.md)
            .frame(minHeight: VizitMetrics.fieldHeight)
            .background(isEnabled ? VizitColor.surface : VizitColor.controlDisabled)
            .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous)
                    .stroke(borderColor, lineWidth: (focused || hasError) && isEnabled ? 2 : 1)
            }

            if let message = error ?? helper {
                Text(message)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(hasError ? VizitColor.error : VizitColor.textMuted)
            }
        }
    }
}

// MARK: - Selection controls

/// Checkbox primitive from the Figma component library. Unlike a platform
/// switch it communicates explicit consent / selection and keeps the whole row
/// tappable with a minimum 44pt target.
struct VizitCheckbox: View {
    let title: String
    @Binding var isOn: Bool
    var isEnabled = true

    var body: some View {
        Button {
            guard isEnabled else { return }
            isOn.toggle()
        } label: {
            HStack(alignment: .top, spacing: VizitSpace.sm) {
                ZStack {
                    RoundedRectangle(cornerRadius: 5, style: .continuous)
                        .fill(isOn ? VizitColor.primary : VizitColor.surface)
                    RoundedRectangle(cornerRadius: 5, style: .continuous)
                        .stroke(
                            isOn ? VizitColor.primary : VizitColor.borderStrong,
                            lineWidth: isOn ? 1 : 1.5
                        )
                    if isOn {
                        Image(systemName: "checkmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(VizitColor.textOnBrand)
                    }
                }
                .frame(width: 22, height: 22)
                .padding(.vertical, 1)

                Text(title)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(isEnabled ? VizitColor.textSecondary : VizitColor.textDisabled)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)

                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, minHeight: VizitMetrics.minTouchTarget, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(isOn ? [.isButton, .isSelected] : .isButton)
    }
}

struct VizitSwitch: View {
    let title: String
    var supporting: String?
    @Binding var isOn: Bool
    var isEnabled = true
    var isError = false
    var isLoading = false

    var body: some View {
        HStack(spacing: VizitSpace.sm) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(VizitFont.body)
                    .foregroundStyle(isEnabled ? VizitColor.textPrimary : VizitColor.textDisabled)
                if let supporting {
                    Text(supporting)
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(isError ? VizitColor.error : VizitColor.textMuted)
                }
            }
            Spacer(minLength: VizitSpace.sm)
            if isLoading {
                ProgressView().controlSize(.small)
            } else {
                Toggle("", isOn: $isOn)
                    .labelsHidden()
                    .accessibilityLabel(title)
                    .accessibilityValue(isOn ? "Be" : "Ki")
                    .disabled(!isEnabled)
                    .tint(isError ? VizitColor.error : VizitColor.primary)
            }
        }
        .frame(minHeight: VizitMetrics.minTouchTarget)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }
}

struct VizitChip: View {
    let text: String
    var selected = false
    var disabled = false
    var error = false
    var action: (() -> Void)?

    var body: some View {
        let foreground: Color = disabled
            ? VizitColor.textDisabled
            : (error ? VizitColor.error : (selected ? VizitColor.textOnBrand : VizitColor.textPrimary))
        let background: Color = disabled
            ? VizitColor.controlDisabled
            : (error ? VizitColor.errorSubtle : (selected ? VizitColor.primary : VizitColor.surface))

        Group {
            if let action {
                Button(action: action) { label(foreground: foreground, background: background) }
                    .buttonStyle(.plain)
                    .disabled(disabled)
            } else {
                label(foreground: foreground, background: background)
            }
        }
        .accessibilityAddTraits(selected ? [.isSelected] : [])
    }

    private func label(foreground: Color, background: Color) -> some View {
        Text(text)
            .font(VizitFont.label)
            .foregroundStyle(foreground)
            .padding(.horizontal, VizitSpace.sm)
            .padding(.vertical, VizitSpace.xs)
            .frame(minHeight: action == nil ? 0 : VizitMetrics.minTouchTarget)
            .background(background)
            .clipShape(Capsule())
            .overlay {
                Capsule().stroke(
                    error ? VizitColor.error : (selected ? VizitColor.primary : VizitColor.border),
                    lineWidth: 1
                )
            }
    }
}

struct VizitToast: View {
    let text: String
    var tone: VizitTone = .success

    var body: some View {
        HStack(spacing: VizitSpace.xs) {
            Image(systemName: tone.systemImage)
            Text(text).font(VizitFont.bodySmall)
            Spacer(minLength: 0)
        }
        .foregroundStyle(tone.foreground)
        .padding(.horizontal, VizitSpace.md)
        .padding(.vertical, VizitSpace.sm)
        .background(tone.background)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}

struct VizitSnackbar: View {
    let text: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        HStack(spacing: VizitSpace.sm) {
            Text(text)
                .font(VizitFont.bodySmall)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .frame(minHeight: VizitMetrics.minTouchTarget)
                    .font(VizitFont.label)
                    .foregroundStyle(.white)
                    .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, VizitSpace.md)
        .padding(.vertical, VizitSpace.sm)
        .background(VizitColor.ink)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))
    }
}

struct VizitPermissionCard: View {
    let systemImage: String
    let title: String
    let message: String
    let primaryTitle: String
    var secondaryTitle: String? = nil
    let primaryAction: () -> Void
    var secondaryAction: (() -> Void)? = nil

    var body: some View {
        VizitPanel {
            VStack(alignment: .leading, spacing: VizitSpace.md) {
                VizitIconChip(systemImage: systemImage, tint: VizitColor.primary, background: VizitColor.primarySubtle)
                Text(title).font(VizitFont.h3).foregroundStyle(VizitColor.textPrimary)
                Text(message)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(VizitColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                HStack(spacing: VizitSpace.sm) {
                    if let secondaryTitle, let secondaryAction {
                        VizitButton(title: secondaryTitle, kind: .secondary, action: secondaryAction)
                    }
                    VizitButton(title: primaryTitle, action: primaryAction)
                }
            }
        }
    }
}

struct VizitConfirmationCard: View {
    let title: String
    let message: String
    var cancelTitle = "Mégse"
    let confirmTitle: String
    var destructive = false
    var isLoading = false
    var isEnabled = true
    let onCancel: () -> Void
    let onConfirm: () -> Void

    var body: some View {
        VizitPanel {
            VStack(alignment: .leading, spacing: VizitSpace.md) {
                Text(title).font(VizitFont.h3).foregroundStyle(VizitColor.textPrimary)
                Text(message).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textSecondary)
                HStack(spacing: VizitSpace.sm) {
                    VizitButton(title: cancelTitle, kind: .secondary, isEnabled: !isLoading, action: onCancel)
                    VizitButton(
                        title: confirmTitle,
                        kind: destructive ? .destructive : .primary,
                        isLoading: isLoading,
                        isEnabled: isEnabled && !isLoading,
                        action: onConfirm
                    )
                }
            }
        }
    }
}

struct VizitSearchEmptyState: View {
    let query: String
    let clearAction: () -> Void

    var body: some View {
        VizitEmptyState(
            systemImage: "magnifyingglass",
            title: "Nincs találat erre: „\(query)”",
            message: "Ellenőrizd az írásmódot, vagy próbálj rövidebb kifejezést.",
            actionTitle: "Keresés törlése",
            action: clearAction
        )
    }
}

struct VizitPermissionDeniedState: View {
    let openSettings: () -> Void

    var body: some View {
        VizitEmptyState(
            systemImage: "camera.fill",
            title: "A kamera nincs engedélyezve",
            message: "A beolvasáshoz engedélyezd a kamerát a Beállításokban.",
            actionTitle: "Beállítások megnyitása",
            action: openSettings,
            tone: .error
        )
    }
}

struct VizitSessionExpiredState: View {
    let login: () -> Void

    var body: some View {
        VizitEmptyState(
            systemImage: "lock.fill",
            title: "A munkameneted lejárt",
            message: "Biztonsági okból kiléptettünk. A megkezdett módosításaid megmaradtak ezen a készüléken.",
            actionTitle: "Bejelentkezés",
            action: login,
            tone: .error
        )
    }
}

struct VizitSyncConflictState: View {
    let localDate: String
    let remoteDate: String
    let chooseLocal: () -> Void
    let chooseRemote: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: VizitSpace.md) {
            Text("Két helyen is módosítottad")
                .font(VizitFont.h3)
                .foregroundStyle(VizitColor.textPrimary)
            VStack(spacing: VizitSpace.sm) {
                Button(action: chooseLocal) {
                    VizitPanel {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Ezen a telefonon").font(VizitFont.label)
                                Text("Módosítva: \(localDate)").font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)
                            }
                            Spacer()
                            Image(systemName: "checkmark").foregroundStyle(VizitColor.primary)
                        }
                    }
                }.buttonStyle(.plain)
                Button(action: chooseRemote) {
                    VizitPanel {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Egy másik eszközön").font(VizitFont.label)
                            Text("Módosítva: \(remoteDate)").font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)
                        }
                    }
                }.buttonStyle(.plain)
            }
        }
    }
}

struct VizitSearchField: View {
    @Binding var text: String
    var placeholder = "Keresés"
    var resultCount: Int? = nil
    @FocusState private var isFocused: Bool

    var body: some View {
        HStack(spacing: VizitSpace.sm) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(VizitColor.textMuted)
            TextField(placeholder, text: $text)
                .font(VizitFont.body)
                .focused($isFocused)
                .accessibilityLabel(placeholder)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            if let resultCount {
                VizitStatusPill(text: "\(resultCount) találat", tone: .info)
            }
            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(VizitColor.textMuted)
                }
                .buttonStyle(.plain)
                .frame(width: VizitMetrics.minTouchTarget, height: VizitMetrics.minTouchTarget)
                .accessibilityLabel("Keresés törlése")
            }
        }
        .padding(.horizontal, VizitSpace.md)
        .frame(minHeight: VizitMetrics.fieldHeight)
        .background(VizitColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous)
                .stroke(isFocused ? VizitColor.borderFocus : VizitColor.border, lineWidth: isFocused ? 2 : 1)
        }
    }
}

struct VizitProgressCard: View {
    let title: String
    let current: Int
    let total: Int
    var supporting: String? = nil

    private var progress: Double {
        guard total > 0 else { return 0 }
        return min(max(Double(current) / Double(total), 0), 1)
    }

    var body: some View {
        VizitPanel {
            VStack(alignment: .leading, spacing: VizitSpace.sm) {
                HStack {
                    Text(title).font(VizitFont.label).foregroundStyle(VizitColor.textPrimary)
                    Spacer()
                    Text("\(min(max(current, 0), max(total, 0))) / \(max(total, 0))")
                        .font(VizitFont.label)
                        .foregroundStyle(VizitColor.primary)
                }
                ProgressView(value: progress)
                    .tint(VizitColor.primary)
                    .accessibilityLabel(title)
                    .accessibilityValue("\(Int(progress * 100)) százalék")
                if let supporting {
                    Text(supporting)
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(VizitColor.textMuted)
                }
            }
        }
    }
}

struct VizitContactRow: View {
    let title: String
    let subtitle: String?
    var systemImage: String? = nil
    var value: String? = nil
    var state: VizitTone? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        VizitRow(
            label: title,
            systemImage: systemImage,
            value: value,
            supporting: subtitle,
            action: action
        ) {
            if let state {
                Circle()
                    .fill(state.foreground)
                    .frame(width: 8, height: 8)
                    .accessibilityHidden(true)
            }
        }
    }
}

struct VizitTabHeader: View {
    let title: String
    var trailingTitle: String?
    var trailingAction: (() -> Void)?

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title)
                .font(VizitFont.h1)
                .foregroundStyle(VizitColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
            Spacer()
            if let trailingTitle, let trailingAction {
                Button(trailingTitle, action: trailingAction)
                    .font(VizitFont.label)
                    .foregroundStyle(VizitColor.primary)
                    .buttonStyle(.plain)
                    .frame(minHeight: VizitMetrics.minTouchTarget)
            }
        }
    }
}

struct VizitInlineMessage: View {
    let text: String
    var tone: VizitTone = .info

    var body: some View {
        HStack(alignment: .top, spacing: VizitSpace.xs) {
            Image(systemName: tone.systemImage)
                .font(.system(size: 13, weight: .semibold))
            Text(text)
                .font(VizitFont.bodySmall)
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(tone.foreground)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Surfaces

/// Restrained panel: hairline border, no shadow. The app is not built out of
/// identical floating cards.
struct VizitPanel<Content: View>: View {
    var padding: CGFloat = VizitSpace.md
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(VizitColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous)
                    .stroke(VizitColor.border, lineWidth: 1)
            }
    }
}

struct VizitSectionHeader: View {
    let title: String
    var tone: Color?

    var body: some View {
        Text(title)
            .vizitOverline()
            .foregroundStyle(tone ?? VizitColor.textMuted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, VizitSpace.xxs)
            .accessibilityAddTraits(.isHeader)
    }
}

/// Grouped container for rows: one rounded box, hairline dividers, no per-row
/// card. `danger` outlines the group so destructive actions stand apart.
struct VizitGroup<Content: View>: View {
    var danger = false
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(spacing: 0) { content() }
            .background(VizitColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous)
                    .stroke(danger ? VizitColor.error : VizitColor.border, lineWidth: 1)
            }
    }
}

struct VizitDivider: View {
    var body: some View {
        Rectangle()
            .fill(VizitColor.divider)
            .frame(height: 1)
    }
}

struct VizitIconChip: View {
    let systemImage: String
    var tint: Color?
    var background: Color?
    var size: CGFloat = 36

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: VizitRadius.sm + 2, style: .continuous)
                .fill(background ?? VizitColor.iconSurface)
            if background == nil {
                RoundedRectangle(cornerRadius: VizitRadius.sm + 2, style: .continuous)
                    .stroke(VizitColor.border, lineWidth: 1)
            }
            Image(systemName: systemImage)
                .font(.system(size: size * 0.48, weight: .medium))
                .foregroundStyle(tint ?? VizitColor.textSecondary)
        }
        .frame(width: size, height: size)
    }
}

/// A row inside a `VizitGroup`. The whole row is the control and carries the
/// accessibility label, so VoiceOver announces label and value together.
struct VizitRow<Trailing: View>: View {
    let label: String
    var systemImage: String?
    var value: String?
    var supporting: String?
    var destructive = false
    var isEnabled = true
    var showsChevron = true
    var action: (() -> Void)?
    @ViewBuilder var trailing: () -> Trailing

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private var foreground: Color {
        if !isEnabled { return VizitColor.textDisabled }
        return destructive ? VizitColor.error : VizitColor.textPrimary
    }

    private var content: some View {
        HStack(spacing: VizitSpace.sm + 2) {
            if let systemImage {
                VizitIconChip(
                    systemImage: systemImage,
                    tint: destructive ? VizitColor.error : VizitColor.textSecondary
                )
            }
            VStack(alignment: .leading, spacing: VizitSpace.xxs) {
                Text(label).font(VizitFont.body).foregroundStyle(foreground).fixedSize(horizontal: false, vertical: true)
                if let supporting {
                    Text(supporting)
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(VizitColor.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                }
                if dynamicTypeSize.isAccessibilitySize, let value {
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
            }
            trailing()
            if action != nil && showsChevron {
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(VizitColor.textMuted)
            }
        }
        .padding(.horizontal, VizitSpace.md)
        .padding(.vertical, VizitSpace.sm)
        .frame(minHeight: 60)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }

    var body: some View {
        if let action {
            Button(action: action) { content }
                .buttonStyle(.plain)
                .disabled(!isEnabled)
        } else {
            content
        }
    }
}

extension VizitRow where Trailing == EmptyView {
    init(
        label: String,
        systemImage: String? = nil,
        value: String? = nil,
        supporting: String? = nil,
        destructive: Bool = false,
        isEnabled: Bool = true,
        showsChevron: Bool = true,
        action: (() -> Void)? = nil
    ) {
        self.init(
            label: label,
            systemImage: systemImage,
            value: value,
            supporting: supporting,
            destructive: destructive,
            isEnabled: isEnabled,
            showsChevron: showsChevron,
            action: action,
            trailing: { EmptyView() }
        )
    }
}

// MARK: - Feedback

enum VizitTone {
    case success, warning, error, info

    var foreground: Color {
        switch self {
        case .success: return VizitColor.success
        case .warning: return VizitColor.warning
        case .error: return VizitColor.error
        case .info: return VizitColor.info
        }
    }

    var background: Color {
        switch self {
        case .success: return VizitColor.successSubtle
        case .warning: return VizitColor.warningSubtle
        case .error: return VizitColor.errorSubtle
        case .info: return VizitColor.infoSubtle
        }
    }

    var systemImage: String {
        switch self {
        case .success: return "checkmark.circle.fill"
        case .warning: return "exclamationmark.triangle.fill"
        case .error: return "xmark.octagon.fill"
        case .info: return "info.circle.fill"
        }
    }
}

/// Always pairs an icon with the text, so state is never carried by colour
/// alone (WCAG 1.4.1).
struct VizitStatusPill: View {
    let text: String
    let tone: VizitTone

    var body: some View {
        HStack(spacing: VizitSpace.xs) {
            Image(systemName: tone.systemImage).font(.system(size: 13, weight: .semibold))
            Text(text).font(VizitFont.label)
        }
        .foregroundStyle(tone.foreground)
        .padding(.horizontal, VizitSpace.sm)
        .padding(.vertical, VizitSpace.xs)
        .background(tone.background)
        .clipShape(Capsule())
        .accessibilityElement(children: .combine)
    }
}

struct VizitBanner: View {
    let text: String
    let tone: VizitTone
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        HStack(spacing: VizitSpace.sm) {
            Image(systemName: tone.systemImage).font(.system(size: 15, weight: .semibold))
            Text(text)
                .font(VizitFont.bodySmall)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .frame(minHeight: VizitMetrics.minTouchTarget)
                    .font(VizitFont.label)
                    .buttonStyle(.plain)
            }
        }
        .foregroundStyle(tone.foreground)
        .padding(.horizontal, VizitSpace.md)
        .padding(.vertical, VizitSpace.sm)
        .background(tone.background)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))
    }
}

/// Empty state: icon, what is missing, and the one action that fixes it.
struct VizitEmptyState: View {
    let systemImage: String
    let title: String
    let message: String
    var actionTitle: String?
    var action: (() -> Void)?
    var tone: VizitTone = .info

    var body: some View {
        VStack(spacing: VizitSpace.sm) {
            ZStack {
                RoundedRectangle(cornerRadius: VizitRadius.lg, style: .continuous)
                    .fill(tone.background)
                Image(systemName: systemImage)
                    .font(.system(size: 26, weight: .medium))
                    .foregroundStyle(tone.foreground)
            }
            .frame(width: 64, height: 64)

            Text(title)
                .font(VizitFont.h3)
                .foregroundStyle(VizitColor.textPrimary)
                .multilineTextAlignment(.center)
            Text(message)
                .font(VizitFont.body)
                .foregroundStyle(VizitColor.textSecondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)

            if let actionTitle, let action {
                VizitButton(title: actionTitle, kind: .secondary, action: action)
                    .padding(.top, VizitSpace.xxs)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, VizitSpace.xl)
        .padding(.vertical, VizitSpace.xxl)
    }
}

struct VizitErrorState: View {
    let title: String
    let message: String
    var retryTitle = "Újrapróbálás"
    var onRetry: (() -> Void)?

    var body: some View {
        VizitEmptyState(
            systemImage: "exclamationmark.triangle.fill",
            title: title,
            message: message,
            actionTitle: onRetry == nil ? nil : retryTitle,
            action: onRetry,
            tone: .error
        )
    }
}

struct VizitLoadingState: View {
    var message: String?

    var body: some View {
        VStack(spacing: VizitSpace.md) {
            ProgressView().controlSize(.large).tint(VizitColor.primary)
            if let message {
                Text(message)
                    .font(VizitFont.bodySmall)
                    .foregroundStyle(VizitColor.textSecondary)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(VizitSpace.xxl)
        .accessibilityElement(children: .combine)
    }
}

/// Skeleton placeholder. Honours Reduce Motion: it renders flat instead of
/// pulsing when the user has asked for less animation.
struct VizitSkeleton: View {
    var height: CGFloat = 16
    var cornerRadius: CGFloat = 8

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var dim = false

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .fill(VizitColor.skeletonBase)
            .frame(height: height)
            .opacity(reduceMotion ? 1 : (dim ? 0.45 : 1))
            .onAppear {
                guard !reduceMotion else { return }
                withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) {
                    dim = true
                }
            }
            .accessibilityHidden(true)
    }
}

/// Native two-to-three option switch. UIKit owns the hit regions and selected
/// semantics so every visual segment is the control VoiceOver activates.
struct VizitSegmentedControl: View {
    let options: [String]
    @Binding var selection: Int
    var accessibilityIdentifier: String? = nil

    var body: some View {
        Picker("Választó", selection: $selection) {
            ForEach(Array(options.enumerated()), id: \.offset) { index, title in
                Text(title).tag(index)
            }
        }
        .pickerStyle(.segmented)
        .labelsHidden()
        .controlSize(.large)
        .vizitIdentifier(accessibilityIdentifier)
    }
}


// MARK: - Helpers

/// Applies an accessibility identifier only when one is supplied, so a nil
/// value leaves whatever the surrounding view hierarchy already set.
private struct VizitOptionalIdentifier: ViewModifier {
    let identifier: String?

    func body(content: Content) -> some View {
        if let identifier {
            content.accessibilityIdentifier(identifier)
        } else {
            content
        }
    }
}

extension View {
    func vizitIdentifier(_ identifier: String?) -> some View {
        modifier(VizitOptionalIdentifier(identifier: identifier))
    }
}

/// The screen's own title, rendered the way the platform renders one: large,
/// left-aligned, with an optional action or identity chip on the trailing edge.
/// Primary screens carry the brand here instead of a separate header block, so
/// the brand costs nothing but the title line it already needed.
struct VizitLargeTitle<Trailing: View>: View {
    let title: String
    var tracking: CGFloat = 0
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        HStack(alignment: .center, spacing: VizitSpace.sm) {
            Text(title)
                .font(VizitFont.h1)
                .fixedSize(horizontal: false, vertical: true)
                .tracking(tracking)
                .foregroundStyle(VizitColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: VizitSpace.xs)
            trailing()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, VizitSpace.xs)
    }
}

extension VizitLargeTitle where Trailing == EmptyView {
    init(_ title: String, tracking: CGFloat = 0) {
        self.init(title: title, tracking: tracking) { EmptyView() }
    }
}

/// The signed-in person as a tappable chip beside the title. It replaces the
/// full greeting row on Home: same destination, a fraction of the height.
struct VizitIdentityChip: View {
    let profile: ContactProfile
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            VizitAvatar(profile: profile, size: 34)
                .overlay {
                    Circle().stroke(VizitColor.border, lineWidth: 1)
                }
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .frame(width: VizitMetrics.minTouchTarget, height: VizitMetrics.minTouchTarget)
        .accessibilityLabel(
            profile.displayName.isEmpty
                ? "Névjegy beállítása"
                : "Megnyitás: \(profile.displayName) névjegye"
        )
    }
}


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
