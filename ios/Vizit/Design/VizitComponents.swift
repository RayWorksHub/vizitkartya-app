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
                Text(title).font(VizitFont.button)
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
                .focused($focused)
                .disabled(!isEnabled)
                .submitLabel(submitLabel)
                .onSubmit { onSubmit?() }
                .vizitIdentifier(identifier)

                if isSecure {
                    VizitIconButton(
                        systemImage: revealed ? "eye.slash" : "eye",
                        accessibilityTitle: revealed ? "Jelszó elrejtése" : "Jelszó megjelenítése",
                        isEnabled: isEnabled
                    ) { revealed.toggle() }
                    .frame(width: 32)
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
            VStack(alignment: .leading, spacing: 1) {
                Text(label).font(VizitFont.body).foregroundStyle(foreground)
                if let supporting {
                    Text(supporting)
                        .font(VizitFont.bodySmall)
                        .foregroundStyle(VizitColor.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            Spacer(minLength: VizitSpace.xs)
            if let value {
                Text(value).font(VizitFont.bodySmall).foregroundStyle(VizitColor.textMuted)
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

/// Two-to-three option switch. Selection is carried by fill and weight, not
/// colour alone, and each segment is a real accessibility element.
struct VizitSegmentedControl: View {
    let options: [String]
    @Binding var selection: Int

    var body: some View {
        HStack(spacing: VizitSpace.xxs) {
            ForEach(Array(options.enumerated()), id: \.offset) { index, title in
                Button {
                    selection = index
                } label: {
                    Text(title)
                        .font(VizitFont.label)
                        .foregroundStyle(selection == index ? VizitColor.textPrimary : VizitColor.textSecondary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 36)
                        .background(selection == index ? VizitColor.surface : .clear)
                        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.sm + 1, style: .continuous))
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityValue(selection == index ? "Kiválasztva" : "Nincs kiválasztva")
                .accessibilityAddTraits(selection == index ? .isSelected : [])
                .accessibilityRemoveTraits(selection == index ? [] : .isSelected)
            }
        }
        .padding(VizitSpace.xxs)
        .background(VizitColor.controlTrack)
        .clipShape(RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous))
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
                .font(.system(size: 32, weight: .bold))
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
