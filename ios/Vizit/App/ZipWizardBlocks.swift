import SwiftUI
import PhotosUI

struct ZWBlockPage: View {
    @ObservedObject var w: ZWState
    let block: ZWBlock
    let index: Int
    let keyboardVisible: Bool
    let focus: FocusState<String?>.Binding
    @Binding var photoItem: PhotosPickerItem?
    @Binding var logoItem: PhotosPickerItem?
    let imageError: String?
    let onAction: () -> Void
    let onReopen: () -> Void

    private var status: ZWBlockStatus { w.status(block.id) }
    private var open: Bool { status == .active || status == .done || status == .open }

    var body: some View {
        VStack(spacing: 0) {
            header
            if open {
                ScrollView(.vertical, showsIndicators: false) {
                    bodyFields.padding(keyboardVisible ? 14 : 16)
                }
                .scrollDismissesKeyboard(.interactively)
                if status == .active && !keyboardVisible { actions }
            } else {
                teaser
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(ZW.surface)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(border)
        .opacity(status == .todo ? 0.7 : 1)
        .animation(.easeInOut(duration: 0.25), value: status)
    }

    private var header: some View {
        let grey = status == .todo || status == .skip
        let accent = status == .todo ? ZW.sub : block.color.color
        return HStack(spacing: 12) {
            Image(systemName: block.icon)
                .font(.system(size: keyboardVisible ? 16 : 19, weight: .semibold))
                .foregroundStyle(accent)
                .frame(width: keyboardVisible ? 32 : 42, height: keyboardVisible ? 32 : 42)
                .background(RoundedRectangle(cornerRadius: keyboardVisible ? 10 : 21).fill(ZW.surface))
                .shadow(color: ZW.shadow.opacity(0.14), radius: 1, x: 0, y: 1)
            VStack(alignment: .leading, spacing: 2) {
                if !keyboardVisible {
                    Text("\(index + 1). BLOKK").font(.system(size: 11, weight: .bold)).tracking(0.44).foregroundStyle(accent)
                }
                Text(block.title).font(.system(size: keyboardVisible ? 16 : 20, weight: .medium)).foregroundStyle(ZW.ink)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if !keyboardVisible { badge(accent) }
        }
        .padding(keyboardVisible ? EdgeInsets(top: 9, leading: 14, bottom: 9, trailing: 14) : EdgeInsets(top: 16, leading: 16, bottom: 14, trailing: 16))
        .background(alignment: .bottomTrailing) {
            if !keyboardVisible {
                Text("0\(index + 1)").font(.system(size: 76, weight: .heavy)).tracking(-3.8)
                    .foregroundStyle(accent.opacity(0.12)).offset(x: -8, y: 18).accessibilityHidden(true)
            }
        }
        .background(grey ? ZW.chip : block.color.soft)
        .clipped()
    }

    @ViewBuilder private func badge(_ accent: Color) -> some View {
        switch status {
        case .done: ZWPill(text: "Kész", background: ZW.surface, foreground: accent, icon: "checkmark")
        case .skip: ZWPill(text: "Kihagyva", background: ZW.surface, foreground: ZW.sub)
        case .active: ZWPill(text: "Most", background: accent, foreground: .white)
        default: EmptyView()
        }
    }

    private var teaser: some View {
        VStack(spacing: 16) {
            Image(systemName: block.icon).font(.system(size: 36, weight: .thin)).foregroundStyle(ZW.sub)
                .frame(width: 88, height: 88).background(Circle().fill(ZW.chip))
            FlexibleChips(values: block.items)
            if status == .skip {
                Button("Mégis kitöltöm", action: onReopen)
                    .font(.system(size: 13, weight: .semibold)).foregroundStyle(ZW.ink)
                    .padding(.horizontal, 14).frame(height: 38)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(ZW.outline, lineWidth: 1))
            }
        }
        .padding(.horizontal, 18).padding(.top, 20).padding(.bottom, 24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var actions: some View {
        VStack(spacing: 0) {
            Rectangle().fill(ZW.line).frame(height: 1)
            Button(action: onAction) {
                HStack(spacing: 8) {
                    VStack(spacing: 1) {
                        Text(w.actionLabel(block)).font(.system(size: 15, weight: .semibold))
                        if block.id != "done", let next = nextBlock {
                            Text(next.title).font(.system(size: 11)).opacity(0.75)
                        }
                    }
                    if block.id != "done" { Image(systemName: "chevron.down").font(.system(size: 11, weight: .bold)) }
                }
                .foregroundStyle(w.isSkip(block) ? ZW.blue : Color.white)
                .frame(maxWidth: .infinity).frame(height: 52)
                .background(w.isSkip(block) ? ZW.blueSoft : ZW.blueFill)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .buttonStyle(.plain).disabled(!w.valid(block.id)).opacity(w.valid(block.id) ? 1 : 0.45)
            .padding(.horizontal, 16).padding(.top, 12).padding(.bottom, 16)
            .accessibilityIdentifier("wizard.primary")
        }
    }

    private var nextBlock: ZWBlock? {
        guard let i = w.flow.firstIndex(where: { $0.id == block.id }), i + 1 < w.flow.count else { return nil }
        return w.flow[i + 1]
    }

    @ViewBuilder private var bodyFields: some View {
        VStack(alignment: .leading, spacing: keyboardVisible ? 12 : 14) {
            switch block.id {
            case "personal":
                if !keyboardVisible { media(isLogo: false) }
                field("name", label: "Név", required: true, contentType: .name, autocap: .words)
                field("phone", label: "Telefon", placeholder: "+36 30 123 4567", keyboard: .phonePad, contentType: .telephoneNumber)
            case "company":
                if !keyboardVisible { media(isLogo: true) }
                field("company", label: "Cégnév", required: true, contentType: .organizationName, autocap: .words)
                field("role", label: "Beosztás", contentType: .jobTitle)
                field("place", label: "Hely", contentType: .addressCity, autocap: .words)
                field("bio", label: "Bemutatkozás", placeholder: "Mivel foglalkozol?", multiline: true, counter: "\(w.bio.count)/420")
            case "online":
                field("email", label: "E-mail", placeholder: "nev@ceg.hu", keyboard: .emailAddress, contentType: .emailAddress, autocap: .never,
                      error: w.errors["email"])
                field("web", label: "Weboldal", placeholder: "ceg.hu", keyboard: .URL, contentType: .URL, autocap: .never)
                VStack(alignment: .leading, spacing: 6) {
                    Text("Közösségi profilok").font(.system(size: 13, weight: .medium)).foregroundStyle(ZW.sub)
                    FlexibleSocialChips(w: w)
                }
                ForEach(w.socialOrder, id: .self) { id in
                    if let social = ZW_SOCIALS.first(where: { $0.id == id }) {
                        field("s-" + id, label: social.label, placeholder: social.placeholder, keyboard: .URL, autocap: .never)
                    }
                }
            default:
                let state = w.slugState()
                field("slug", label: "Profil címe", prefix: "vizitkartyam.hu/", keyboard: .URL, autocap: .never, invalid: !state.ok)
                HStack { Spacer(); ZWPill(text: state.message, background: state.ok ? ZW.greenSoft : ZW.redSoft, foreground: state.ok ? ZW.green : ZW.red) }
                VStack(alignment: .leading, spacing: 8) {
                    Text("Szín").font(.system(size: 13, weight: .medium)).foregroundStyle(ZW.sub)
                    HStack(spacing: 12) {
                        ForEach([CardColorway.ink, .brand, .emerald, .amethyst], id: .self) { colorway in
                            ZWColorDot(colorway: colorway, selected: w.colorway == colorway) { w.colorway = colorway; w.dirty = true }
                        }
                        Text(w.colorway.label).font(.system(size: 14, weight: .semibold)).foregroundStyle(ZW.sub)
                    }
                }
                Toggle(isOn: $w.isPublic) { Text("Nyilvános profil").font(.system(size: 15, weight: .medium)).foregroundStyle(ZW.ink) }
                    .tint(ZW.green).frame(minHeight: 48)
                if let publishError = w.errors["publish"] { Text(publishError).font(.system(size: 12.5)).foregroundStyle(ZW.red) }
            }
        }
    }

    private func field(_ key: String, label: String, required: Bool = false, placeholder: String = "", prefix: String? = nil,
                       keyboard: UIKeyboardType = .default, contentType: UITextContentType? = nil,
                       autocap: TextInputAutocapitalization = .sentences, multiline: Bool = false,
                       error: String? = nil, invalid: Bool = false, counter: String? = nil) -> some View {
        ZWField(label: required ? label + " *" : label,
                text: Binding(get: { w.value(key) }, set: { w.input(key, $0); if key == "email" { validateEmail() } }),
                fieldKey: key, focus: focus, placeholder: placeholder, prefix: prefix,
                keyboard: keyboard, contentType: contentType, autocap: autocap, multiline: multiline,
                error: error, invalid: invalid, counter: counter)
    }

    private func validateEmail() {
        if w.emailValid(w.email) { w.errors["email"] = nil }
        else { w.errors["email"] = "Ez nem tűnik érvényes e-mail-címnek." }
    }

    private func media(isLogo: Bool) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 14) {
                ZWMediaPreview(base64: isLogo ? w.logoBase64 : w.photoBase64, initials: w.name, isLogo: isLogo)
                VStack(alignment: .leading, spacing: 2) {
                    Text(isLogo ? "Céges logó" : "Profilkép").font(.system(size: 15, weight: .medium)).foregroundStyle(ZW.ink)
                    if isLogo {
                        PhotosPicker(selection: $logoItem, matching: .images) { Text(w.logoBase64.isEmpty ? "Feltöltés" : "Csere") }
                            .font(.system(size: 14, weight: .medium)).foregroundStyle(ZW.blue)
                        if !w.logoBase64.isEmpty { Button("Törlés") { w.logoBase64 = "" }.font(.system(size: 14, weight: .medium)).foregroundStyle(ZW.blue) }
                    } else {
                        PhotosPicker(selection: $photoItem, matching: .images) { Text(w.photoBase64.isEmpty ? "Feltöltés" : "Csere") }
                            .font(.system(size: 14, weight: .medium)).foregroundStyle(ZW.blue)
                        if !w.photoBase64.isEmpty { Button("Törlés") { w.photoBase64 = "" }.font(.system(size: 14, weight: .medium)).foregroundStyle(ZW.blue) }
                    }
                }
            }
            if let err = w.errors[isLogo ? "logo" : "photo"] { Text(err).font(.system(size: 12.5)).foregroundStyle(ZW.red) }
        }
    }

    @ViewBuilder private var border: some View {
        let shape = RoundedRectangle(cornerRadius: 28, style: .continuous)
        switch status {
        case .active: shape.strokeBorder(block.color.color, lineWidth: 2)
        case .todo: shape.strokeBorder(ZW.line, style: StrokeStyle(lineWidth: 1, dash: [6, 5]))
        default: shape.strokeBorder(ZW.line, lineWidth: 1)
        }
    }
}

struct ZWField: View {
    let label: String
    @Binding var text: String
    let fieldKey: String
    let focus: FocusState<String?>.Binding
    var placeholder = ""
    var prefix: String? = nil
    var keyboard: UIKeyboardType = .default
    var contentType: UITextContentType? = nil
    var autocap: TextInputAutocapitalization = .sentences
    var multiline = false
    var error: String? = nil
    var invalid = false
    var counter: String? = nil

    private var focused: Bool { focus.wrappedValue == fieldKey }
    private var bad: Bool { error != nil || invalid }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.system(size: 13, weight: .medium)).foregroundStyle(ZW.sub)
            HStack(spacing: 1) {
                if let prefix { Text(prefix).font(.system(size: 14)).foregroundStyle(ZW.sub) }
                Group {
                    if multiline {
                        TextEditor(text: $text).frame(minHeight: 88)
                    } else {
                        TextField(placeholder, text: $text)
                    }
                }
                .font(.system(size: 16))
                .foregroundStyle(ZW.ink)
                .textInputAutocapitalization(autocap)
                .keyboardType(keyboard)
                .textContentType(contentType)
                .focused(focus, equals: fieldKey)
                .submitLabel(.next)
            }
            .padding(.horizontal, 12).padding(.vertical, multiline ? 6 : 0)
            .frame(minHeight: multiline ? 112 : 48, alignment: multiline ? .top : .center)
            .background(ZW.surface)
            .overlay(RoundedRectangle(cornerRadius: 6).stroke(bad ? ZW.red : (focused ? ZW.blue : ZW.outline), lineWidth: bad || focused ? 2 : 1))
            if let error { Text(error).font(.system(size: 12.5)).foregroundStyle(ZW.red) }
            if let counter { Text(counter).font(.system(size: 12.5)).foregroundStyle(ZW.sub).frame(maxWidth: .infinity, alignment: .trailing) }
        }
    }
}

struct ZWPill: View {
    let text: String
    let background: Color
    let foreground: Color
    var icon: String? = nil
    var body: some View {
        HStack(spacing: 5) {
            if let icon { Image(systemName: icon).font(.system(size: 10, weight: .bold)) }
            Text(text).font(.system(size: 12, weight: .semibold))
        }
        .foregroundStyle(foreground).padding(.horizontal, 10).padding(.vertical, 5).background(Capsule().fill(background))
    }
}

struct FlexibleChips: View {
    let values: [String]
    var body: some View {
        HStack(spacing: 6) {
            ForEach(values, id: .self) { value in
                Text(value).font(.system(size: 12.5, weight: .medium)).foregroundStyle(ZW.sub)
                    .padding(.horizontal, 10).padding(.vertical, 5).background(Capsule().fill(ZW.chip))
            }
        }
        .lineLimit(1).minimumScaleFactor(0.65)
    }
}

struct FlexibleSocialChips: View {
    @ObservedObject var w: ZWState
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(ZW_SOCIALS.chunked(into: 3).enumerated()), id: .offset) { _, row in
                HStack(spacing: 8) {
                    ForEach(row, id: .id) { social in
                        let on = w.socialOrder.contains(social.id)
                        Button {
                            w.toggleSocial(social.id)
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: on ? "checkmark" : "plus").font(.system(size: 11, weight: .semibold))
                                Text(social.label).font(.system(size: 13, weight: on ? .semibold : .medium))
                            }
                            .foregroundStyle(on ? ZW.blue : ZW.ink)
                            .padding(.horizontal, 10).frame(height: 36)
                            .background(Capsule().fill(on ? ZW.blueSoft : ZW.surface))
                            .overlay(Capsule().stroke(on ? Color.clear : ZW.outline, lineWidth: 1))
                        }.buttonStyle(.plain)
                    }
                }
            }
        }
    }
}

struct ZWMediaPreview: View {
    let base64: String
    let initials: String
    let isLogo: Bool
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: isLogo ? 16 : 32).fill(base64.isEmpty ? ZW.emptyBg : Color.white)
            if let data = Data(base64Encoded: base64), let image = UIImage(data: data) {
                Image(uiImage: image).resizable().scaledToFill().clipShape(RoundedRectangle(cornerRadius: isLogo ? 16 : 32))
            } else if !isLogo {
                Text(initialsFor(initials)).font(.system(size: 22, weight: .bold)).foregroundStyle(ZW.sub)
            } else {
                Image(systemName: "plus").foregroundStyle(ZW.sub)
            }
        }.frame(width: 64, height: 64)
    }
    private func initialsFor(_ name: String) -> String {
        let parts = name.split(whereSeparator: { $0.isWhitespace })
        return parts.prefix(2).compactMap(.first).map { String($0).uppercased() }.joined()
    }
}

struct ZWColorDot: View {
    let colorway: CardColorway
    let selected: Bool
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Circle().fill(gradient)
                .frame(width: 36, height: 36)
                .overlay(Circle().stroke(Color.white.opacity(0.18), lineWidth: 2))
                .padding(selected ? 4 : 0)
                .overlay {
                    if selected { Circle().stroke(ZW.blue, lineWidth: 3) }
                }
        }.buttonStyle(.plain)
    }
    private var gradient: LinearGradient {
        let g = colorway.gradient
        return LinearGradient(colors: [Color(uiColor: UIColor(hex: g.start)), Color(uiColor: UIColor(hex: g.end))], startPoint: .topLeading, endPoint: .bottomTrailing)
    }
}
