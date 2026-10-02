// Imported from VizitTeljesIOS(1).zip (SHA-256 af2b2c86c1792b39c57815209538cb7823a10a943d32cd11ce6f321e224f5288).
// Only symbol names, production callbacks, accessibility, and iOS 16 compatibility are adapted.

// MARK: Original Wizard/WizardState.swift
import SwiftUI
import UIKit

/// A profil típusa: vállalkozói vagy magánszemély.
enum ZIPWizProfileType: Equatable {
    case business, individual
}

/// Egy blokk állapota: még nem, most, kész, kihagyva, újranyitva.
enum ZIPWizBlockStatus: Equatable {
    case todo, active, done, skip, open
}

/// A varázsló egy blokkja (SF Symbol ikon, színpár, az előnézet címkéi).
struct ZIPWizBlock: Identifiable {
    let id: String
    let title: String
    let icon: String
    let color: ZIPBlockColor
    let items: [String]
    var isOptional: Bool = false
}

/// A négy blokk, sorrendben. Magánszemélynél a „Céges adatok” kimarad.
let WIZ_BLOCKS: [ZIPWizBlock] = [
    ZIPWizBlock(id: "personal", title: "Személyes adatok", icon: "person", color: ZIPBlockColors.b1,
             items: ["Profilkép", "Név", "Telefon"]),
    ZIPWizBlock(id: "company", title: "Céges adatok", icon: "briefcase", color: ZIPBlockColors.b2,
             items: ["Logó", "Cégnév", "Beosztás", "Hely", "Bemutatkozás"]),
    ZIPWizBlock(id: "online", title: "Online elérés", icon: "globe", color: ZIPBlockColors.b3,
             items: ["E-mail", "Weboldal", "Közösségi profilok"], isOptional: true),
    ZIPWizBlock(id: "done", title: "Befejezés", icon: "checkmark", color: ZIPBlockColors.b4,
             items: ["Profil címe", "Szín", "Láthatóság"]),
]

let WIZ_EMAIL_ERROR = "Ez nem tűnik érvényes e-mail-címnek."

struct ZIPWizSlugState {
    let ok: Bool
    let msg: String
}

/// Görgetési kérés a blokk-lapozónak (a nonce miatt ugyanarra is újra lefut).
struct ZIPWizScrollReq: Equatable {
    let blockId: String
    let instant: Bool
    let nonce = UUID()
}

/// A varázsló teljes állapota és logikája. A felület csak ezt olvassa és ezen hív.
final class ZIPWizardState: ObservableObject {
    private let takenSlugs: () -> [String]

    @Published var profileType: ZIPWizProfileType? = nil
    @Published var started: Bool = false
    /// Van-e már beírt adat (bezáráskor megerősítést kér).
    private(set) var dirty: Bool = false

    @Published var name: String = ZIPACCOUNT_NAME
    @Published var phone: String = ""
    @Published var photo: ZIPPic? = nil
    @Published var company: String = ""
    @Published var role: String = ""
    @Published var place: String = ""
    @Published var bio: String = ""
    @Published var logo: ZIPPic? = nil
    @Published var email: String = ""
    @Published var web: String = ""
    @Published var socials: [String: String] = [:]
    @Published var socialOrder: [String] = []
    @Published var preset: String = "tinta"
    @Published var slug: String = ""
    private var slugEdited: Bool = false
    @Published var isPublic: Bool = true

    @Published var bs: [String: ZIPWizBlockStatus] = [
        "personal": .todo,
        "company": .todo,
        "online": .todo,
        "done": .todo,
    ]

    /// A „Kezdjük meg…” kezdőképernyő látszik-e.
    @Published var introShown: Bool = true
    @Published var confirmOpen: Bool = false

    /// Mezőhibák: name, email, photo, logo.
    @Published var errors: [String: String] = [:]

    @Published var scrollRequest: ZIPWizScrollReq? = nil
    @Published var focusRequest: String? = nil

    private var emailCheck: Int = 0

    private static let emailPattern = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$"
    private static let slugPattern = "^[a-z0-9]+(-[a-z0-9]+)*$"

    init(takenSlugs: @escaping () -> [String]) {
        self.takenSlugs = takenSlugs
    }

    // MARK: - Származtatott értékek

    var flow: [ZIPWizBlock] {
        let t = profileType
        return WIZ_BLOCKS.filter { t != .individual || $0.id != "company" }
    }

    func fidx(_ id: String) -> Int {
        flow.firstIndex(where: { $0.id == id }) ?? -1
    }

    var isBiz: Bool { profileType == .business }

    func status(_ id: String) -> ZIPWizBlockStatus { bs[id] ?? .todo }

    var activeBlock: ZIPWizBlock? {
        flow.first(where: { status($0.id) == .active })
    }

    /// Az adott blokk mezői sorrendben (a billentyűzet „Tovább” gombja a következőre ugrik).
    func fieldKeys(_ blockId: String) -> [String] {
        switch blockId {
        case "personal": return ["name", "phone"]
        case "company": return ["company", "role", "place", "bio"]
        case "online": return ["email", "web"] + socialOrder.map { "s-" + $0 }
        case "done": return ["slug"]
        default: return []
        }
    }

    /// Melyik blokkhoz tartozik a mező.
    func blockOfField(_ key: String) -> String? {
        flow.first(where: { fieldKeys($0.id).contains(key) })?.id
    }

    func emailOk(_ v: String) -> Bool {
        v.zipIsBlank || ZIPWizardState.matches(v.zipTrimmed, ZIPWizardState.emailPattern)
    }

    private static func matches(_ s: String, _ pattern: String) -> Bool {
        s.range(of: pattern, options: .regularExpression) != nil
    }

    private func taken() -> [String] {
        ["admin", "vizit"] + takenSlugs()
    }

    func initials(_ n: String? = nil) -> String {
        let src = n ?? name
        let words = src.split(whereSeparator: { $0.isWhitespace }).map { String($0) }
        guard let first = words.first else { return "" }
        var out = String(first.prefix(1))
        if words.count > 1, let last = words.last {
            out += String(last.prefix(1))
        }
        return out.uppercased()
    }

    private func slugify(_ s: String) -> String {
        var stripped = String.UnicodeScalarView()
        for u in s.decomposedStringWithCanonicalMapping.unicodeScalars {
            let v: UInt32 = u.value
            if v >= 0x300 && v <= 0x36F { continue }
            stripped.append(u)
        }
        let lower = String(stripped).lowercased()
        var out = String.UnicodeScalarView()
        var dash = false
        for u in lower.unicodeScalars {
            let v = u.value
            if (v >= 97 && v <= 122) || (v >= 48 && v <= 57) {
                if dash && !out.isEmpty {
                    out.append("-" as Unicode.Scalar)
                }
                dash = false
                out.append(u)
            } else {
                dash = true
            }
        }
        return String(String(out).prefix(40))
    }

    func suggested() -> String {
        let src = (isBiz && !company.zipIsBlank) ? company : name
        var base = slugify(src)
        if base.isEmpty { base = "nevjegy" }
        let t = taken()
        if !t.contains(base) { return base }
        var n = 2
        while t.contains("\(base)-\(n)") { n += 1 }
        return "\(base)-\(n)"
    }

    func slugState() -> ZIPWizSlugState {
        if slug.count < 3 { return ZIPWizSlugState(ok: false, msg: "Legalább 3 karakter") }
        if !ZIPWizardState.matches(slug, ZIPWizardState.slugPattern) { return ZIPWizSlugState(ok: false, msg: "Kisbetű, szám, kötőjel") }
        if taken().contains(slug) { return ZIPWizSlugState(ok: false, msg: "Foglalt") }
        return ZIPWizSlugState(ok: true, msg: "Formátum rendben")
    }

    /// Van-e már valami az opcionális blokkban (ha nincs: „Kihagyom”).
    func has(_ id: String) -> Bool {
        if id == "online" {
            if !email.zipIsBlank || !web.zipIsBlank { return true }
            return socialOrder.contains(where: { !(socials[$0] ?? "").zipIsBlank })
        }
        return true
    }

    /// Engedélyezett-e a blokk gombja.
    func valid(_ id: String) -> Bool {
        switch id {
        case "personal": return !name.zipIsBlank
        case "company": return !company.zipIsBlank
        case "online": return emailOk(email)
        case "done": return slugState().ok && !name.zipIsBlank && emailOk(email) && (!isBiz || !company.zipIsBlank)
        default: return true
        }
    }

    /// A blokk gombjának felirata („Tovább” / „Kihagyom” / „Publikálás” / „Mentés”).
    func actionLabel(_ b: ZIPWizBlock) -> String {
        if b.id == "done" { return isPublic ? "Publikálás" : "Mentés" }
        if b.isOptional && !has(b.id) { return "Kihagyom" }
        return "Tovább"
    }

    func isSkip(_ b: ZIPWizBlock) -> Bool { b.isOptional && !has(b.id) }

    func nextOf(_ id: String) -> ZIPWizBlock? {
        let fl = flow
        let i = fidx(id) + 1
        if i >= 0 && i < fl.count { return fl[i] }
        return nil
    }

    // MARK: - Műveletek

    func chooseType(_ t: ZIPWizProfileType) {
        if profileType == t { return }
        profileType = t
        dirty = true
        if t == .individual && status("company") == .active {
            bs["company"] = .todo
            if status("online") == .todo { bs["online"] = .active }
        }
        if t == .business && started && status("company") == .todo && status("personal") != .active {
            bs["company"] = .active
            for id in ["online", "done"] where status(id) == .active {
                bs[id] = .todo
            }
        }
        if !slugEdited && status("done") != .todo { slug = suggested() }
        // típusváltás után mindig legyen egy aktív blokk (különben nem lehetne publikálni)
        if started && activeBlock == nil {
            if let b = flow.first(where: { status($0.id) == .todo || status($0.id) == .skip }) {
                if b.id == "done" && (!slugEdited || slug.isEmpty) { slug = suggested() }
                bs[b.id] = .active
                scrollRequest = ZIPWizScrollReq(blockId: b.id, instant: true)
            }
        }
    }

    /// A kezdőképernyőről indulva: az első blokk aktív lesz, a lapozó odaugrik, a kezdőképernyő kicsúszik.
    func begin() {
        if !started {
            started = true
            bs["personal"] = .active
        }
        scrollRequest = ZIPWizScrollReq(blockId: activeBlock?.id ?? "personal", instant: true)
        introShown = false
    }

    /// „Tovább” / „Kihagyom”: a blokk kész (vagy kihagyva), a következő aktív, a lapozó odagörget.
    func next(_ id: String, typing: Bool) {
        let fl = flow
        let n = fidx(id)
        if n < 0 || n >= fl.count - 1 { return }
        let b = fl[n]
        bs[id] = (b.isOptional && !has(id)) ? .skip : .done
        // a következő még nem kész blokk lesz aktív (ha közben már kitöltött blokk jön, átugorjuk)
        let rest = fl[(n + 1)...]
        let nb = rest.first(where: { status($0.id) == .todo || status($0.id) == .skip }) ?? fl[n + 1]
        if nb.id == "done" && (!slugEdited || slug.isEmpty) { slug = suggested() }
        if status(nb.id) == .todo || status(nb.id) == .skip { bs[nb.id] = .active }
        if typing && nb.id != "done" { focusRequest = fieldKeys(nb.id).first }
        scrollRequest = ZIPWizScrollReq(blockId: nb.id, instant: false)
    }

    /// „Mégis kitöltöm” egy kihagyott blokkon.
    func reopen(_ id: String) {
        bs[id] = .open
        scrollRequest = ZIPWizScrollReq(blockId: id, instant: false)
    }

    func jump(_ id: String) {
        scrollRequest = ZIPWizScrollReq(blockId: id, instant: false)
    }

    /// Publikálás: az új profil, vagy nil (és hibajelzés), ha hiányzik a név.
    func buildProfile() -> ZIPProfile? {
        if name.zipIsBlank {
            if status("personal") != .active { bs["personal"] = .open }
            errors["name"] = "Add meg a neved."
            scrollRequest = ZIPWizScrollReq(blockId: "personal", instant: false)
            return nil
        }
        var links: [String: String] = [:]
        for id in socialOrder {
            let v = ZIPWizardState.https(socials[id] ?? "")
            if !v.isEmpty { links[id] = v }
        }
        let biz = isBiz
        let millis = Int64(Date().timeIntervalSince1970 * 1000)
        return ZIPProfile(
            id: "n" + String(millis),
            label: (biz && !company.zipIsBlank) ? company.zipTrimmed : "Személyes",
            real: false,
            name: name.zipTrimmed,
            title: biz ? role.zipTrimmed : "",
            company: biz ? company.zipTrimmed : "",
            bio: biz ? bio.zipTrimmed : "",
            phone: phone.zipTrimmed,
            email: email.zipTrimmed,
            web: ZIPWizardState.https(web),
            address: biz ? place.zipTrimmed : "",
            socials: links,
            photo: photo,
            logo: biz ? logo : nil,
            isPublic: isPublic,
            slug: slug,
            presetId: preset,
            tag: "Új",
            fresh: true
        )
    }

    private static func https(_ v: String) -> String {
        let t = v.zipTrimmed
        if t.isEmpty || t.hasPrefix("https://") { return t }
        if t.hasPrefix("http://") { return "https://" + String(t.dropFirst(7)) }
        return "https://" + t
    }

    // MARK: - Bevitel

    private func autoSlug() {
        if !slugEdited && status("done") != .todo { slug = suggested() }
    }

    func input(_ key: String, _ v: String) {
        dirty = true
        if key == "slug" {
            slug = v.lowercased().replacingOccurrences(of: "\\s+", with: "-", options: .regularExpression)
            slugEdited = true
            return
        }
        if key.hasPrefix("s-") {
            socials[String(key.dropFirst(2))] = v
            return
        }
        switch key {
        case "name":
            name = v
            errors["name"] = nil
        case "phone":
            phone = v
        case "company":
            company = v
        case "role":
            role = v
        case "place":
            place = v
        case "bio":
            bio = String(v.prefix(420))
        case "email":
            email = v
            scheduleEmailCheck()
        case "web":
            web = v
        default:
            break
        }
        if key == "name" || key == "company" { autoSlug() }
    }

    func value(_ key: String) -> String {
        if key.hasPrefix("s-") { return socials[String(key.dropFirst(2))] ?? "" }
        switch key {
        case "name": return name
        case "phone": return phone
        case "company": return company
        case "role": return role
        case "place": return place
        case "bio": return bio
        case "email": return email
        case "web": return web
        case "slug": return slug
        default: return ""
        }
    }

    func toggleSocial(_ id: String) {
        if let at = socialOrder.firstIndex(of: id) {
            socialOrder.remove(at: at)
            socials[id] = nil
        } else {
            socialOrder.append(id)
            if socials[id] == nil { socials[id] = "" }
            focusRequest = "s-" + id
        }
        dirty = true
    }

    func setPic(_ kind: String, _ pic: ZIPPic?) {
        if kind == "photo" {
            photo = pic
        } else {
            logo = pic
        }
        if pic != nil { dirty = true }
        errors[kind] = nil
    }

    // MARK: - E-mail ellenőrzés

    /// Gépelés közben 0,7 s késleltetéssel jelez; érvényes címnél azonnal eltűnik a hiba.
    private func scheduleEmailCheck() {
        emailCheck += 1
        if emailOk(email) {
            errors["email"] = nil
            return
        }
        let gen = emailCheck
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) { [weak self] in
            guard let self = self, self.emailCheck == gen, !self.emailOk(self.email) else { return }
            self.errors["email"] = WIZ_EMAIL_ERROR
        }
    }

    /// A mező elhagyásakor azonnal.
    func checkEmailNow() {
        if emailOk(email) {
            errors["email"] = nil
        } else {
            errors["email"] = WIZ_EMAIL_ERROR
        }
    }
}


// MARK: Original Wizard/WizardView.swift
import SwiftUI
import UIKit
import PhotosUI

/// Az új profil varázsló (teljes képernyős nézet). Az állapot az `app.wizard`-ban él.
struct ZIPWizardView: View {
    @EnvironmentObject var app: ZIPWizardBridge

    var body: some View {
        if let w = app.wizard {
            ZIPWizScreen(w: w)
                .id(ObjectIdentifier(w))
        } else {
            Color.clear
        }
    }
}

/// A blokkok műveletei, amelyekhez a varázsló állapotán kívül fókusz és képválasztó is kell.
struct ZIPWizCtx {
    let focus: FocusState<String?>.Binding
    /// Billentyűzet közben tömör nézet.
    let kb: Bool
    let action: (ZIPWizBlock) -> Void
    let imeNext: (ZIPWizBlock, String) -> Void
    let pick: (String) -> Void
}

/// A kezdőképernyő ki- és becsúszása (0,38 s).
private let ZIPwizSlide = Animation.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.38)

private struct ZIPWizScreen: View {
    @EnvironmentObject var app: ZIPWizardBridge
    @ObservedObject var w: ZIPWizardState

    @FocusState private var focus: String?
    @State private var pageId: String? = nil
    @State private var previousFocus: String? = nil
    @State private var pickShown: Bool = false
    @State private var pickKind: String = "photo"
    @State private var pickItem: PhotosPickerItem? = nil

    // Saját init: a private @State/@FocusState tárolt tulajdonságok miatt a szintetizált
    // memberwise init private lenne, és a ZIPWizardView nem érné el.
    init(w: ZIPWizardState) {
        self.w = w
    }

    private var kb: Bool { focus != nil }

    private var pageIndex: Int {
        guard let id = pageId else { return 0 }
        return w.flow.firstIndex(where: { $0.id == id }) ?? 0
    }

    /// Az aktív blokk, ha éppen annak egyik mezőjében gépelünk.
    private var activeFocused: ZIPWizBlock? {
        guard let f = focus, let a = w.activeBlock else { return nil }
        return w.blockOfField(f) == a.id ? a : nil
    }

    private var ctx: ZIPWizCtx {
        ZIPWizCtx(
            focus: $focus,
            kb: kb,
            action: { b in action(b) },
            imeNext: { b, key in imeNext(b, key) },
            pick: { kind in pick(kind) }
        )
    }

    private var confirmBinding: Binding<Bool> {
        let state = w
        return Binding(
            get: { state.confirmOpen },
            set: { if !$0 { state.confirmOpen = false } }
        )
    }

    var body: some View {
        ZStack {
            main
                .accessibilityHidden(w.introShown)
            if w.introShown {
                ZIPWizIntro(w: w, onClose: { close() }, onPick: { t in pickType(t) })
                    .transition(.move(edge: .leading))
                    .zIndex(2)
            }
        }
        .animation(ZIPwizSlide, value: w.introShown)
        .statusBarHidden(w.introShown)
        .photosPicker(isPresented: $pickShown, selection: $pickItem, matching: .images)
        .onChange(of: pickItem) { item in picked(item) }
        .onChange(of: w.scrollRequest) { req in applyScroll(req) }
        .onChange(of: w.focusRequest) { req in focusLater(req) }
        .onChange(of: focus) { new in focusChanged(previousFocus, new); previousFocus = new }
        .alert("Kilépsz? Az adatok elvesznek.", isPresented: confirmBinding) {
            Button("Kilépés", role: .destructive) { app.closeWizard(force: true) }
            Button("Maradok", role: .cancel) { w.confirmOpen = false }
        }
    }

    private var main: some View {
        VStack(spacing: 0) {
            ZIPWizTopBar(
                w: w,
                k: pageIndex,
                kb: kb,
                active: activeFocused,
                onClose: { close() },
                onGo: { go() }
            )
            ZIPWizProgress(w: w, k: pageIndex, kb: kb)
            GeometryReader { geo in
                if #available(iOS 17.0, *) { pager(geo.size) }
                else { legacyPager(geo.size) }
            }
        }
        .background(ZIPV.bg.ignoresSafeArea())
    }

    /// Blokk-lapozó: a szomszédos blokkok széle kilátszik, a lapok a bal szélhez igazodnak.
    @available(iOS 17.0, *)
    private func pager(_ size: CGSize) -> some View {
        let topPad: CGFloat = kb ? 2 : 6
        let bottomPad: CGFloat = kb ? 12 : 18
        let pageW: CGFloat = max(size.width - 56, 120)
        let pageH: CGFloat = max(size.height - topPad - bottomPad, 120)
        let c = ctx
        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(Array(w.flow.enumerated()), id: \.element.id) { item in
                    ZIPWizBlockPage(w: w, ctx: c, b: item.element, n: item.offset)
                        .frame(width: pageW, height: pageH)
                        .id(item.element.id)
                }
            }
            .scrollTargetLayout()
            .padding(.top, topPad)
            .padding(.bottom, bottomPad)
        }
        .scrollTargetBehavior(.viewAligned)
        .scrollPosition(id: $pageId, anchor: .center)
        .safeAreaPadding(.horizontal, 28)
    }

    private func legacyPager(_ size: CGSize) -> some View {
        TabView(selection: $pageId) {
            ForEach(Array(w.flow.enumerated()), id: \.element.id) { item in
                ZIPWizBlockPage(w: w, ctx: ctx, b: item.element, n: item.offset)
                    .padding(.horizontal, 28)
                    .padding(.top, kb ? 2 : 6)
                    .padding(.bottom, kb ? 12 : 18)
                    .tag(Optional(item.element.id))
            }
        }.tabViewStyle(.page(indexDisplayMode: .never))
    }

    // MARK: - Műveletek

    /// A blokk gombja: „Tovább/Kihagyom” → next(), „Publikálás/Mentés” → publikálás.
    private func action(_ b: ZIPWizBlock) {
        if b.id == "done" {
            publish()
        } else {
            w.next(b.id, typing: focus != nil)
        }
    }

    private func publish() {
        guard let p = w.buildProfile() else { return }
        focus = nil
        app.addPublished(p, isPublic: w.isPublic)
    }

    private func close() {
        focus = nil
        app.closeWizard(force: false)
    }

    /// „Tovább” a billentyűzeten: következő mező a blokkban, az utolsón a blokk gombja.
    private func imeNext(_ b: ZIPWizBlock, _ key: String) {
        let keys = w.fieldKeys(b.id)
        if let i = keys.firstIndex(of: key), i < keys.count - 1 {
            focus = keys[i + 1]
            return
        }
        if w.status(b.id) == .active && w.valid(b.id) {
            if b.id == "done" {
                publish()
            } else {
                w.next(b.id, typing: true)
            }
        } else {
            focus = nil
        }
    }

    /// A jobb felső gomb billentyűzet közben: a blokk gombja, vagy „Kész” = billentyűzet le.
    private func go() {
        if let a = activeFocused {
            if w.valid(a.id) { action(a) }
        } else {
            focus = nil
        }
    }

    private func pick(_ kind: String) {
        focus = nil
        pickKind = kind
        pickShown = true
    }

    private func pickType(_ t: ZIPWizProfileType) {
        w.chooseType(t)
        w.begin()
    }

    private func picked(_ item: PhotosPickerItem?) {
        guard let item = item else { return }
        let kind = pickKind
        pickItem = nil
        ZIPImagePick.load(item, maxSide: kind == "photo" ? 640 : 320) { img, err in
            if let img = img {
                w.setPic(kind, .image(img))
            } else if let err = err {
                w.errors[kind] = err
            }
        }
    }

    private func applyScroll(_ req: ZIPWizScrollReq?) {
        guard let r = req, w.flow.contains(where: { $0.id == r.blockId }) else { return }
        if r.instant {
            var t = Transaction()
            t.disablesAnimations = true
            withTransaction(t) {
                pageId = r.blockId
            }
        } else {
            withAnimation(.zipMotion) {
                pageId = r.blockId
            }
        }
    }

    /// A kért mező fókuszt kap, amint megjelent.
    private func focusLater(_ req: String?) {
        guard let key = req else { return }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
            focus = key
            w.focusRequest = nil
        }
    }

    private func focusChanged(_ old: String?, _ new: String?) {
        if old == "email" && new != "email" {
            w.checkEmailNow()
        }
    }
}

// MARK: - Felső sáv

/// Bezárás · (típus · n/N ⌄ / aktuális blokk színes pöttyel) · billentyűzet közben a blokk gombja.
private struct ZIPWizTopBar: View {
    @ObservedObject var w: ZIPWizardState
    let k: Int
    let kb: Bool
    let active: ZIPWizBlock?
    let onClose: () -> Void
    let onGo: () -> Void

    private var block: ZIPWizBlock? {
        let fl = w.flow
        if fl.isEmpty { return nil }
        return fl[min(max(k, 0), fl.count - 1)]
    }

    private var typeLabel: String {
        w.profileType == .individual ? "Magánszemély" : "Vállalkozói"
    }

    var body: some View {
        HStack(spacing: 0) {
            ZIPRoundIconButton("xmark", label: "Bezárás", plain: true, tint: ZIPV.sub, action: onClose)
                .frame(maxWidth: .infinity, alignment: .leading)
            center
                .layoutPriority(1)
            trailing
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .padding(.horizontal, 8)
        .padding(.top, 2)
        .padding(.bottom, kb ? 2 : 6)
    }

    private var center: some View {
        VStack(spacing: 1) {
            if !kb {
                typeButton
            }
            if let b = block {
                HStack(spacing: 7) {
                    Circle()
                        .fill(b.color.c)
                        .frame(width: 9, height: 9)
                    Text(b.title)
                        .font(.zipFont(16, .medium))
                        .foregroundColor(ZIPV.ink)
                        .lineLimit(1)
                }
                .animation(.easeInOut(duration: 0.2), value: b.id)
            }
        }
    }

    private var typeButton: some View {
        Button {
            w.introShown = true
        } label: {
            HStack(spacing: 3) {
                Text(typeLabel + " · \(k + 1)/\(w.flow.count)")
                    .font(.zipFont(11.5, .semibold))
                    .foregroundColor(ZIPV.sub)
                    .lineLimit(1)
                Image(systemName: "chevron.down")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundColor(ZIPV.sub)
            }
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder private var trailing: some View {
        if kb {
            ZIPWizGoButton(w: w, active: active, onGo: onGo)
        } else {
            Color.clear.frame(width: 1, height: 1)
        }
    }
}

/// Billentyűzet közben: az aktív blokk gombja („Tovább” / „Kihagyom” / „Publikálás”), különben „Kész”.
private struct ZIPWizGoButton: View {
    @ObservedObject var w: ZIPWizardState
    let active: ZIPWizBlock?
    let onGo: () -> Void

    private var label: String {
        if let a = active { return w.actionLabel(a) }
        return "Kész"
    }

    private var soft: Bool {
        if let a = active { return w.isSkip(a) }
        return true
    }

    private var enabled: Bool {
        if let a = active { return w.valid(a.id) }
        return true
    }

    private var bg: Color {
        if !enabled { return ZIPV.switchOff }
        return soft ? ZIPV.blueSoft : ZIPV.blueFill
    }

    private var fg: Color {
        if !enabled { return ZIPV.sub }
        return soft ? ZIPV.blue : ZIPV.onBlueFill
    }

    var body: some View {
        Button(action: onGo) {
            Text(label)
                .font(.zipFont(14, .semibold))
                .foregroundColor(fg)
                .lineLimit(1)
                .padding(.horizontal, 14)
                .frame(height: 36)
                .background(Capsule().fill(bg))
        }
        .accessibilityIdentifier(active.map { "wizard.action." + $0.id } ?? "wizard.keyboardDone")
        .buttonStyle(ZIPPressStyle())
        .disabled(!enabled)
    }
}

// MARK: - Haladásjelző

/// Blokkonként egy csík (kész/újranyitott = blokk színe, kihagyott = szürke, aktív = 45%-ig színes).
/// Az éppen látott blokk csíkja vastagabb; koppintásra odagörget.
private struct ZIPWizProgress: View {
    @ObservedObject var w: ZIPWizardState
    let k: Int
    let kb: Bool

    var body: some View {
        HStack(spacing: 4) {
            ForEach(Array(w.flow.enumerated()), id: \.element.id) { item in
                ZIPWizProgressBar(b: item.element, status: w.status(item.element.id), here: item.offset == k) {
                    w.jump(item.element.id)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, kb ? 6 : 10)
    }
}

private struct ZIPWizProgressBar: View {
    let b: ZIPWizBlock
    let status: ZIPWizBlockStatus
    let here: Bool
    let onTap: () -> Void

    private var base: Color {
        switch status {
        case .done, .open: return b.color.c
        case .skip: return ZIPV.chev
        default: return ZIPV.switchOff
        }
    }

    var body: some View {
        GeometryReader { g in
            ZStack(alignment: .leading) {
                Rectangle().fill(base)
                if status == .active {
                    Rectangle()
                        .fill(b.color.c)
                        .frame(width: g.size.width * 0.45)
                }
            }
        }
        .frame(height: here ? 8 : 4)
        .clipShape(RoundedRectangle(cornerRadius: here ? 3 : 1))
        .frame(maxWidth: .infinity)
        .frame(height: 14)
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
        .animation(.easeInOut(duration: 0.25), value: status)
        .animation(.easeInOut(duration: 0.2), value: here)
    }
}


// MARK: Original Wizard/WizardBlocks.swift
import SwiftUI
import UIKit

// MARK: - Blokk kártya

/// Egy blokk a lapozóban: színes fejléc, alatta vagy a mezők (aktív / kész / újranyitott),
/// vagy az előnézet (még nem / kihagyva). Aktív blokk alján a gomb; billentyűzet közben tömör nézet.
struct ZIPWizBlockPage: View {
    @ObservedObject var w: ZIPWizardState
    let ctx: ZIPWizCtx
    let b: ZIPWizBlock
    let n: Int

    private var s: ZIPWizBlockStatus { w.status(b.id) }

    private var isOpen: Bool { s == .active || s == .done || s == .open }

    private var bodyInsets: EdgeInsets {
        if ctx.kb { return EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14) }
        return EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16)
    }

    var body: some View {
        VStack(spacing: 0) {
            ZIPWizBlockHead(b: b, n: n, s: s, kb: ctx.kb)
            if isOpen {
                fields
                if s == .active && !ctx.kb {
                    ZIPWizBlockActions(w: w, b: b, onAction: { ctx.action(b) })
                }
            } else {
                ZIPWizBlockTeaser(b: b, s: s, onReopen: { w.reopen(b.id) })
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(ZIPV.surface)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(cardBorder)
        .opacity(s == .todo ? 0.7 : 1)
        .animation(.easeInOut(duration: 0.25), value: s)
    }

    private var fields: some View {
        ScrollView(.vertical, showsIndicators: false) {
            ZIPWizBlockBody(w: w, ctx: ctx, b: b)
                .padding(bodyInsets)
        }
        .scrollDismissesKeyboard(.interactively)
        .frame(maxHeight: .infinity)
    }

    @ViewBuilder private var cardBorder: some View {
        let shape = RoundedRectangle(cornerRadius: 28, style: .continuous)
        switch s {
        case .active:
            shape.strokeBorder(b.color.c, lineWidth: 2)
        case .todo:
            shape.strokeBorder(ZIPV.line, style: StrokeStyle(lineWidth: 1, dash: [6, 5]))
        default:
            shape.strokeBorder(ZIPV.line, lineWidth: 1)
        }
    }
}

/// Fejléc: fehér ikon kör, „1. BLOKK”, cím, állapot jelvény, halvány nagy sorszám a sarokban.
private struct ZIPWizBlockHead: View {
    let b: ZIPWizBlock
    let n: Int
    let s: ZIPWizBlockStatus
    let kb: Bool

    private var todo: Bool { s == .todo }
    private var grey: Bool { s == .todo || s == .skip }
    private var accent: Color { todo ? ZIPV.sub : b.color.c }

    private var insets: EdgeInsets {
        if kb { return EdgeInsets(top: 9, leading: 14, bottom: 9, trailing: 14) }
        return EdgeInsets(top: 16, leading: 16, bottom: 14, trailing: 16)
    }

    var body: some View {
        HStack(spacing: 12) {
            iconBox
            titles
            if !kb {
                ZIPWizStatusBadge(s: s, color: b.color.c)
            }
        }
        .padding(insets)
        .frame(maxWidth: .infinity)
        .background(alignment: .bottomTrailing) { bigNumber }
        .background(grey ? ZIPV.chip : b.color.soft)
        .clipped()
    }

    private var iconBox: some View {
        let size: CGFloat = kb ? 32 : 42
        let radius: CGFloat = kb ? 10 : 21
        let iconSize: CGFloat = kb ? 16 : 19
        return Image(systemName: b.icon)
            .font(.system(size: iconSize, weight: .semibold))
            .foregroundColor(accent)
            .frame(width: size, height: size)
            .background(RoundedRectangle(cornerRadius: radius).fill(ZIPV.surface))
            .shadow(color: ZIPV.shadow.opacity(0.14), radius: 1, x: 0, y: 1)
    }

    private var titles: some View {
        VStack(alignment: .leading, spacing: 2) {
            if !kb {
                Text("\(n + 1). BLOKK")
                    .font(.zipFont(11, .bold))
                    .kerning(0.44)
                    .foregroundColor(accent)
                    .lineLimit(1)
            }
            Text(b.title)
                .font(.zipFont(kb ? 16 : 20, .medium))
                .foregroundColor(ZIPV.ink)
                .lineLimit(2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder private var bigNumber: some View {
        if !kb {
            Text("0\(n + 1)")
                .font(.zipFont(76, .heavy))
                .kerning(-3.8)
                .foregroundColor(accent.opacity(0.12))
                .lineLimit(1)
                .fixedSize()
                .offset(x: -8, y: 18)
                .accessibilityHidden(true)
        }
    }
}

private struct ZIPWizStatusBadge: View {
    let s: ZIPWizBlockStatus
    let color: Color

    var body: some View {
        switch s {
        case .done:
            ZIPPill("Kész", background: ZIPV.surface, color: color, icon: "checkmark")
        case .skip:
            ZIPPill("Kihagyva", background: ZIPV.surface, color: ZIPV.sub)
        case .active:
            ZIPPill("Most", background: color, color: ZIPV.onAccent)
        default:
            EmptyView()
        }
    }
}

/// Előnézet: nagy ikon és a blokk tartalmának címkéi; kihagyott blokknál „Mégis kitöltöm”.
private struct ZIPWizBlockTeaser: View {
    let b: ZIPWizBlock
    let s: ZIPWizBlockStatus
    let onReopen: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: b.icon)
                .font(.system(size: 36, weight: .thin))
                .foregroundColor(ZIPV.sub)
                .frame(width: 88, height: 88)
                .background(Circle().fill(ZIPV.chip))
            chips
            if s == .skip {
                ZIPSmallOutlineButton("Mégis kitöltöm", action: onReopen)
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 20)
        .padding(.bottom, 24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var chips: some View {
        let layout = ZIPWizCenterFlow(gap: 6, lineGap: 6)
        return layout {
            ForEach(b.items, id: \.self) { item in
                Text(item)
                    .font(.zipFont(12.5, .medium))
                    .foregroundColor(ZIPV.sub)
                    .lineLimit(1)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Capsule().fill(ZIPV.chip))
            }
        }
    }
}

/// Alsó gombsor vonallal: „Tovább · következő blokk ⌄” / „Kihagyom” / „Publikálás”.
private struct ZIPWizBlockActions: View {
    @ObservedObject var w: ZIPWizardState
    let b: ZIPWizBlock
    let onAction: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(ZIPV.line)
                .frame(height: 1)
            button
                .accessibilityIdentifier("wizard.action." + b.id)
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 16)
        }
    }

    @ViewBuilder private var button: some View {
        if b.id == "done" {
            ZIPPrimaryButton(w.actionLabel(b), enabled: w.valid(b.id), action: onAction)
        } else {
            ZIPPrimaryButton(
                w.actionLabel(b),
                trailingIcon: "chevron.down",
                secondaryText: w.nextOf(b.id)?.title,
                enabled: w.valid(b.id),
                soft: w.isSkip(b),
                action: onAction
            )
        }
    }
}

// MARK: - Blokkok tartalma

private struct ZIPWizBlockBody: View {
    @ObservedObject var w: ZIPWizardState
    let ctx: ZIPWizCtx
    let b: ZIPWizBlock

    var body: some View {
        let spacing: CGFloat = ctx.kb ? 12 : 14
        VStack(alignment: .leading, spacing: spacing) {
            switch b.id {
            case "personal":
                personal
            case "company":
                company
            case "online":
                online
            default:
                done
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: Személyes adatok

    @ViewBuilder private var personal: some View {
        if !ctx.kb {
            ZIPWizMedia(w: w, kind: "photo", onPick: { ctx.pick("photo") })
        }
        field("name", "Név", required: true, content: .name, autocap: .words, error: w.errors["name"])
        field("phone", "Telefon", placeholder: "+36 30 123 4567", keyboard: .phonePad, content: .telephoneNumber)
    }

    // MARK: Céges adatok

    @ViewBuilder private var company: some View {
        if !ctx.kb {
            ZIPWizMedia(w: w, kind: "logo", onPick: { ctx.pick("logo") })
        }
        field("company", "Cégnév", required: true, content: .organizationName, autocap: .words)
        field("role", "Beosztás", content: .jobTitle)
        field("place", "Hely", content: .addressCity, autocap: .words)
        field("bio", "Bemutatkozás", placeholder: "Mivel foglalkozol?", multiline: true, counter: "\(w.bio.count)/420")
    }

    // MARK: Online elérés

    @ViewBuilder private var online: some View {
        field("email", "E-mail", placeholder: "nev@ceg.hu", keyboard: .emailAddress, content: .emailAddress,
              autocap: .never, error: w.errors["email"])
        field("web", "Weboldal", placeholder: "ceg.hu", keyboard: .URL, content: .URL, autocap: .never)
        VStack(alignment: .leading, spacing: 6) {
            ZIPWizLabel(text: "Közösségi profilok")
            socialChips
        }
        ForEach(w.socialOrder, id: \.self) { id in
            socialField(id)
        }
    }

    private var socialChips: some View {
        let layout = ZIPFlowLayout(spacing: 8, lineSpacing: 8)
        return layout {
            ForEach(ZIPSOCIALS) { s in
                ZIPWizSocialChip(label: s.label, on: w.socialOrder.contains(s.id)) {
                    w.toggleSocial(s.id)
                }
            }
        }
    }

    @ViewBuilder private func socialField(_ id: String) -> some View {
        if let s = ZIPSOCIALS.first(where: { $0.id == id }) {
            field("s-" + id, s.label, placeholder: s.placeholder, keyboard: .URL, autocap: .never)
        }
    }

    // MARK: Befejezés

    @ViewBuilder private var done: some View {
        let st = w.slugState()
        VStack(alignment: .leading, spacing: 6) {
            field("slug", "Profil címe", prefix: "vizitkartyam.hu/", keyboard: .URL, autocap: .never,
                  invalid: !st.ok, submit: .done)
            HStack {
                Spacer(minLength: 0)
                ZIPPill(st.msg, background: st.ok ? ZIPV.greenSoft : ZIPV.redSoft, color: st.ok ? ZIPV.green : ZIPV.red)
            }
        }
        VStack(alignment: .leading, spacing: 6) {
            ZIPWizLabel(text: "Szín")
            ZIPWizSwatches(w: w)
        }
        Toggle(isOn: $w.isPublic) {
            Text("Nyilvános profil")
                .font(.zipFont(15, .medium))
                .foregroundColor(ZIPV.ink)
        }
        .tint(ZIPV.green)
        .frame(minHeight: 48)
    }

    // MARK: Mező

    private func field(_ key: String, _ label: String, required: Bool = false, placeholder: String = "",
                       prefix: String? = nil, keyboard: UIKeyboardType = .default,
                       content: UITextContentType? = nil, autocap: TextInputAutocapitalization = .sentences,
                       multiline: Bool = false, error: String? = nil, invalid: Bool = false,
                       counter: String? = nil, submit: SubmitLabel = .next) -> some View {
        let state = w
        let block = b
        let next = ctx.imeNext
        return ZIPWizField(
            label: required ? label + " *" : label,
            text: Binding(get: { state.value(key) }, set: { state.input(key, $0) }),
            fieldKey: key,
            focus: ctx.focus,
            placeholder: placeholder,
            prefix: prefix,
            keyboard: keyboard,
            contentType: content,
            autocap: autocap,
            multiline: multiline,
            error: error,
            invalid: invalid,
            counter: counter,
            submit: submit,
            submitAction: { next(block, key) }
        )
    }
}

/// Mezőcímke (kis, szürke).
private struct ZIPWizLabel: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.zipFont(13, .semibold))
            .foregroundColor(ZIPV.sub)
            .padding(.leading, 4)
    }
}

/// Beviteli mező a varázslóban: címke fölötte, lekerekített mező (fókuszban kék, hibánál piros),
/// alatta hibaszöveg vagy számláló. A fókusz a varázsló közös fókuszállapotához kötött.
private struct ZIPWizField: View {
    let label: String
    let text: Binding<String>
    let fieldKey: String
    let focus: FocusState<String?>.Binding
    var placeholder: String = ""
    var prefix: String? = nil
    var keyboard: UIKeyboardType = .default
    var contentType: UITextContentType? = nil
    var autocap: TextInputAutocapitalization = .sentences
    var multiline: Bool = false
    var error: String? = nil
    var invalid: Bool = false
    var counter: String? = nil
    var submit: SubmitLabel = .next
    var submitAction: () -> Void = {}

    private var isFocused: Bool { focus.wrappedValue == fieldKey }
    private var bad: Bool { error != nil || invalid }

    private var borderColor: Color {
        if bad { return ZIPV.red }
        if isFocused { return ZIPV.blue }
        return ZIPV.line
    }

    private var prompt: Text {
        Text(placeholder).foregroundColor(ZIPV.placeholder)
    }

    var body: some View {
        let minH: CGFloat = multiline ? 112 : 50
        let lineW: CGFloat = (isFocused || bad) ? 1.5 : 1
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.zipFont(13, .semibold))
                .foregroundColor(bad ? ZIPV.red : ZIPV.sub)
                .padding(.leading, 4)
            HStack(alignment: multiline ? .top : .center, spacing: 2) {
                if let prefix = prefix {
                    Text(prefix)
                        .font(.zipFont(15))
                        .foregroundColor(ZIPV.sub)
                        .lineLimit(1)
                        .fixedSize()
                }
                input
            }
            .padding(.horizontal, 14)
            .padding(.vertical, multiline ? 12 : 0)
            .frame(minHeight: minH, alignment: multiline ? .top : .center)
            .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(ZIPV.surface))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(borderColor, lineWidth: lineW)
            )
            footer
        }
    }

    private var input: some View {
        Group {
            if multiline {
                TextField("", text: text, prompt: prompt, axis: .vertical)
                    .lineLimit(3...8)
            } else {
                TextField("", text: text, prompt: prompt)
            }
        }
        .font(.zipFont(16))
        .foregroundColor(ZIPV.ink)
        .keyboardType(keyboard)
        .textContentType(contentType)
        .textInputAutocapitalization(autocap)
        .autocorrectionDisabled(keyboard != .default)
        .submitLabel(multiline ? .return : submit)
        .onSubmit {
            if !multiline { submitAction() }
        }
        .focused(focus, equals: fieldKey)
        .accessibilityIdentifier(fieldKey == "name" ? "wizard.fullName" : "wizard.field." + fieldKey)
    }

    @ViewBuilder private var footer: some View {
        if let error = error {
            Text(error)
                .font(.zipFont(12.5))
                .foregroundColor(ZIPV.red)
                .padding(.leading, 4)
        }
        if let counter = counter {
            Text(counter)
                .font(.zipFont(12.5))
                .foregroundColor(ZIPV.sub)
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
    }
}

// MARK: - Kép (profilkép / logó)

/// Kerek profilkép / szögletes logó helye, mellette „Mostani kép · Feltöltés” vagy „Csere · Törlés”.
private struct ZIPWizMedia: View {
    @ObservedObject var w: ZIPWizardState
    let kind: String
    let onPick: () -> Void

    private var isPhoto: Bool { kind == "photo" }
    private var pic: ZIPPic? { isPhoto ? w.photo : w.logo }
    private var radius: CGFloat { isPhoto ? 32 : 16 }

    private var boxBg: Color {
        if pic == nil { return ZIPV.emptyBg }
        return isPhoto ? ZIPV.blueSoft : Color.white
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 14) {
                Button(action: onPick) { box }
                    .buttonStyle(ZIPPressStyle())
                    .accessibilityLabel(isPhoto ? "Profilkép" : "Céges logó")
                VStack(alignment: .leading, spacing: 2) {
                    Text(isPhoto ? "Profilkép" : "Céges logó")
                        .font(.zipFont(15, .medium))
                        .foregroundColor(ZIPV.ink)
                    links
                }
            }
            if let err = w.errors[kind] {
                Text(err)
                    .font(.zipFont(12.5))
                    .foregroundColor(ZIPV.red)
            }
        }
    }

    private var box: some View {
        ZStack {
            RoundedRectangle(cornerRadius: radius).fill(boxBg)
            boxContent
        }
        .frame(width: 64, height: 64)
        .clipShape(RoundedRectangle(cornerRadius: radius))
        .overlay(dash)
    }

    @ViewBuilder private var boxContent: some View {
        let initials = w.initials()
        if let p = pic {
            ZIPPicImage(pic: p, fit: !isPhoto)
                .frame(width: 64, height: 64)
        } else if isPhoto && !initials.isEmpty {
            Text(initials)
                .font(.zipFont(22, .bold))
                .foregroundColor(ZIPV.sub)
        } else {
            Image(systemName: isPhoto ? "camera" : "plus")
                .font(.system(size: 22, weight: .regular))
                .foregroundColor(ZIPV.sub)
        }
    }

    @ViewBuilder private var dash: some View {
        if pic == nil {
            RoundedRectangle(cornerRadius: radius)
                .strokeBorder(ZIPV.chev, style: StrokeStyle(lineWidth: 1.5, dash: [4, 4]))
        }
    }

    @ViewBuilder private var links: some View {
        HStack(spacing: 16) {
            if pic != nil {
                link("Csere", onPick)
                link("Törlés") { w.setPic(kind, nil) }
            } else if isPhoto {
                link("Feltöltés", onPick)
            } else {
                link("Feltöltés", onPick)
            }
        }
    }

    private func link(_ title: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.zipFont(14, .semibold))
                .foregroundColor(ZIPV.blue)
                .padding(.vertical, 4)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Közösségi profil kapcsoló

private struct ZIPWizSocialChip: View {
    let label: String
    let on: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                Image(systemName: on ? "checkmark" : "plus")
                    .font(.system(size: 13, weight: .semibold))
                Text(label)
                    .font(.zipFont(14, on ? .semibold : .regular))
                    .lineLimit(1)
            }
            .foregroundColor(on ? ZIPV.blue : ZIPV.ink)
            .padding(.leading, 10)
            .padding(.trailing, 12)
            .frame(height: 34)
            .background(RoundedRectangle(cornerRadius: 8, style: .continuous).fill(on ? ZIPV.blueSoft : Color.clear))
            .overlay(RoundedRectangle(cornerRadius: 8, style: .continuous).stroke(on ? Color.clear : ZIPV.line, lineWidth: 1))
            .contentShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .buttonStyle(ZIPPressStyle())
    }
}

// MARK: - Színvilág választó

/// Az első négy színvilág körökben (kiválasztva kék gyűrű), mellette a neve.
private struct ZIPWizSwatches: View {
    @ObservedObject var w: ZIPWizardState

    var body: some View {
        HStack(spacing: 12) {
            ForEach(Array(ZIPPRESETS.prefix(4))) { pr in
                swatch(pr)
            }
            Text(ZIPpresetById(w.preset).name)
                .font(.zipFont(14, .semibold))
                .foregroundColor(ZIPV.sub)
                .padding(.leading, 2)
        }
        .padding(.vertical, 4)
        .padding(.leading, 4)
    }

    private func swatch(_ pr: ZIPPreset) -> some View {
        let checked = w.preset == pr.id
        let state = w
        return Button {
            state.preset = pr.id
        } label: {
            Circle()
                .fill(LinearGradient(colors: [pr.c1, pr.c3], startPoint: .topLeading, endPoint: .bottomTrailing))
                .overlay(Circle().strokeBorder(Color.white.opacity(0.18), lineWidth: 2))
                .frame(width: 36, height: 36)
                .background(ring(checked))
        }
        .buttonStyle(ZIPPressStyle())
        .accessibilityLabel(pr.name)
    }

    private func ring(_ checked: Bool) -> some View {
        ZStack {
            Circle().fill(ZIPV.blue).frame(width: 44, height: 44)
            Circle().fill(ZIPV.surface).frame(width: 40, height: 40)
        }
        .opacity(checked ? 1 : 0)
    }
}

// MARK: - Középre igazított sortörő elrendezés

/// Kapszulák sortöréssel, soronként középre igazítva (az előnézet címkéi).
private struct ZIPWizCenterFlow: Layout {
    var gap: CGFloat = 6
    var lineGap: CGFloat = 6

    private func rows(_ maxW: CGFloat, _ sizes: [CGSize]) -> [[Int]] {
        var out: [[Int]] = [[]]
        var x: CGFloat = 0
        for (i, sz) in sizes.enumerated() {
            let lastEmpty = out[out.count - 1].isEmpty
            if !lastEmpty && x + gap + sz.width > maxW + 0.5 {
                out.append([])
                x = sz.width
            } else if lastEmpty {
                x = sz.width
            } else {
                x += gap + sz.width
            }
            out[out.count - 1].append(i)
        }
        return out
    }

    private func lineWidth(_ row: [Int], _ sizes: [CGSize]) -> CGFloat {
        var w: CGFloat = 0
        for i in row { w += sizes[i].width }
        return w + gap * CGFloat(max(row.count - 1, 0))
    }

    private func lineHeight(_ row: [Int], _ sizes: [CGSize]) -> CGFloat {
        var h: CGFloat = 0
        for i in row { h = max(h, sizes[i].height) }
        return h
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let sizes = subviews.map { $0.sizeThatFits(.unspecified) }
        let maxW = proposal.width ?? CGFloat.greatestFiniteMagnitude
        let rs = rows(maxW, sizes)
        var h: CGFloat = 0
        var widest: CGFloat = 0
        for (ri, r) in rs.enumerated() {
            widest = max(widest, lineWidth(r, sizes))
            h += lineHeight(r, sizes) + (ri > 0 ? lineGap : 0)
        }
        return CGSize(width: widest, height: h)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let sizes = subviews.map { $0.sizeThatFits(.unspecified) }
        let rs = rows(bounds.width, sizes)
        var y = bounds.minY
        for r in rs {
            let lw = lineWidth(r, sizes)
            let lh = lineHeight(r, sizes)
            var x = bounds.minX + (bounds.width - lw) / 2
            for i in r {
                let sz = sizes[i]
                subviews[i].place(at: CGPoint(x: x, y: y + (lh - sz.height) / 2), proposal: ProposedViewSize(sz))
                x += sz.width + gap
            }
            y += lh + lineGap
        }
    }
}


// MARK: Original Wizard/WizardIntro.swift
import SwiftUI

/// „Kezdjük meg a profilod létrehozását!” – teljes képernyős sötétkék kezdőlap.
/// A két típusgomb (Vállalkozói / Magánszemély) alatt színes csíkok mutatják, hány blokk jön.
/// Mindkét témában sötét.
struct ZIPWizIntro: View {
    @ObservedObject var w: ZIPWizardState
    let onClose: () -> Void
    let onPick: (ZIPWizProfileType) -> Void

    var body: some View {
        ZStack(alignment: .topLeading) {
            GeometryReader { geo in
                ZIPWizIntroBackground(size: geo.size)
            }
            .ignoresSafeArea()
            Color.clear
                .overlay(alignment: .topTrailing) {
                    ZIPWizIntroWaves()
                        .frame(width: 260, height: 283.6)
                        .offset(x: 40, y: 70)
                }
                .clipped()
                .allowsHitTesting(false)
            content
        }
        .contentShape(Rectangle())
    }

    private var content: some View {
        VStack(spacing: 0) {
            topRow
            GeometryReader { g in
                ScrollView(.vertical, showsIndicators: false) {
                    texts
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                        .padding(.bottom, 28)
                        .frame(maxWidth: .infinity, minHeight: g.size.height, alignment: .bottomLeading)
                }
                .modifier(ZIPScrollBounceCompatibility())
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var topRow: some View {
        HStack(spacing: 0) {
            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(Circle().fill(Color.white.opacity(0.10)))
                    .contentShape(Circle())
            }
            .buttonStyle(ZIPPressStyle())
            .accessibilityLabel("Bezárás")
            Text("VIZIT")
                .font(.zipFont(13, .heavy))
                .kerning(3.9)
                .foregroundColor(Color.white.opacity(0.65))
                .frame(maxWidth: .infinity)
            Color.clear
                .frame(width: 44, height: 44)
        }
        .padding(.horizontal, 10)
        .padding(.top, 8)
    }

    private var texts: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Kezdjük meg a profilod létrehozását!")
                .font(.zipFont(34, .medium))
                .kerning(-0.34)
                .foregroundColor(.white)
                .fixedSize(horizontal: false, vertical: true)
            Text("Milyen profil lesz?")
                .font(.zipFont(14, .medium))
                .foregroundColor(Color.white.opacity(0.7))
                .padding(.top, 6)
                .padding(.bottom, 2)
            VStack(spacing: 10) {
                ZIPWizTypePick(
                    checked: w.profileType == .business,
                    profile: .business,
                    icon: "briefcase",
                    label: "Vállalkozói",
                    onTap: { onPick(.business) }
                )
                ZIPWizTypePick(
                    checked: w.profileType == .individual,
                    profile: .individual,
                    icon: "person",
                    label: "Magánszemély",
                    onTap: { onPick(.individual) }
                )
            }
        }
    }
}

/// Áttetsző kártya; kiválasztva fehér, kék ikonnal. Nyomásra 98,5%-ra húzódik össze.
private struct ZIPWizTypePick: View {
    let checked: Bool
    let profile: ZIPWizProfileType
    let icon: String
    let label: String
    let onTap: () -> Void

    private var bars: [Color] {
        let t = profile
        return WIZ_BLOCKS.filter { t != .individual || $0.id != "company" }.map { $0.color.c }
    }

    private var bubble: Color {
        checked ? ZIPV.h2 : Color.white.opacity(0.15)
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(Circle().fill(bubble))
                labels
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 30, height: 30)
                    .background(Circle().fill(bubble))
            }
            .padding(.leading, 14)
            .padding(.trailing, 12)
            .padding(.vertical, 14)
            .background(card)
            .contentShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        }
        .accessibilityIdentifier(profile == .individual ? "wizard.private" : "wizard.business")
        .buttonStyle(ZIPWizPickStyle())
        .animation(.easeOut(duration: 0.15), value: checked)
    }

    private var labels: some View {
        VStack(alignment: .leading, spacing: 9) {
            Text(label)
                .font(.zipFont(17, .medium))
                .foregroundColor(checked ? ZIPV.ink : Color.white)
            HStack(spacing: 4) {
                ForEach(Array(bars.enumerated()), id: \.offset) { item in
                    RoundedRectangle(cornerRadius: 3, style: .continuous)
                        .fill(item.element)
                        .frame(width: 22, height: 5)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var card: some View {
        let shape = RoundedRectangle(cornerRadius: 20, style: .continuous)
        return shape
            .fill(checked ? ZIPV.surface : Color.white.opacity(0.10))
            .overlay(shape.stroke(checked ? ZIPV.surface : Color.white.opacity(0.22), lineWidth: 1))
    }
}

private struct ZIPWizPickStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.985 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

/// linear-gradient(165deg, h1 0%, h1 45%, h2 felé) + két elmosódó fényfolt (bal alul, jobb felül).
private struct ZIPWizIntroBackground: View {
    let size: CGSize

    var body: some View {
        let w = size.width
        let h = size.height
        ZStack(alignment: .topLeading) {
            ZIPV.h1
            LinearGradient(
                stops: [
                    Gradient.Stop(color: ZIPV.h2.opacity(0), location: 0.45),
                    Gradient.Stop(color: ZIPV.h2.opacity(0.65), location: 1),
                ],
                startPoint: UnitPoint(x: 0.37, y: 0),
                endPoint: UnitPoint(x: 0.63, y: 1)
            )
            glow(ZIPV.glow2, stop: 0.65)
                .frame(width: 1.8 * w, height: 1.2 * h)
                .position(x: -0.1 * w, y: 1.05 * h)
            glow(ZIPV.glow1, stop: 0.6)
                .frame(width: 2.4 * w, height: 1.4 * h)
                .position(x: 1.05 * w, y: -0.05 * h)
        }
        .frame(width: w, height: h)
        .clipped()
    }

    private func glow(_ c: Color, stop: CGFloat) -> some View {
        EllipticalGradient(
            gradient: Gradient(stops: [
                Gradient.Stop(color: c, location: 0),
                Gradient.Stop(color: c.opacity(0), location: stop),
            ]),
            center: .center,
            startRadiusFraction: 0,
            endRadiusFraction: 0.5
        )
    }
}

/// A kezdőlap jobb felső sarkának halvány hullámai (pötty + három ív, 15% fehér).
private struct ZIPWizIntroWaves: View {
    var body: some View {
        ZStack {
            ZIPWizWaveDot()
                .fill(Color.white.opacity(0.15))
            ZIPWizWaveArcs()
                .stroke(
                    Color.white.opacity(0.15),
                    style: StrokeStyle(lineWidth: CGFloat(14.2), lineCap: .round, lineJoin: .round)
                )
        }
    }
}

/// Az eredeti 220×240-es rajz pöttye (40;120, r = 13).
private struct ZIPWizWaveDot: Shape {
    func path(in rect: CGRect) -> Path {
        let s = rect.width / 220
        return Path(ellipseIn: CGRect(x: rect.minX + 27 * s, y: rect.minY + 107 * s, width: 26 * s, height: 26 * s))
    }
}

/// A három ív: függőleges húr (x; y0…y1) fölött r sugarú, jobbra domborodó körív.
private struct ZIPWizWaveArcs: Shape {
    func path(in rect: CGRect) -> Path {
        let s = rect.width / 220
        let arcs: [(x: CGFloat, y0: CGFloat, y1: CGFloat, r: CGFloat)] = [
            (x: 78, y0: 62, y1: 178, r: 82),
            (x: 112, y0: 30, y1: 210, r: 124),
            (x: 146, y0: -2, y1: 242, r: 166),
        ]
        var p = Path()
        for a in arcs {
            let half = (a.y1 - a.y0) / 2
            let d = (a.r * a.r - half * half).squareRoot()
            let cx = a.x - d
            let cy = (a.y0 + a.y1) / 2
            let sweep = atan2(Double(half), Double(d))
            let steps = 40
            for i in 0...steps {
                let t = -sweep + 2 * sweep * Double(i) / Double(steps)
                let px = rect.minX + (cx + a.r * CGFloat(cos(t))) * s
                let py = rect.minY + (cy + a.r * CGFloat(sin(t))) * s
                if i == 0 {
                    p.move(to: CGPoint(x: px, y: py))
                } else {
                    p.addLine(to: CGPoint(x: px, y: py))
                }
            }
        }
        return p
    }
}


// MARK: Original Design/Tokens.swift
import SwiftUI
import UIKit

/// Világos / sötét / rendszer – a Beállítások › Téma választója.
enum ZIPThemeMode: Int, CaseIterable {
    case light, dark, system

    var label: String {
        switch self {
        case .light: return "Világos"
        case .dark: return "Sötét"
        case .system: return "Rendszer"
        }
    }

    var scheme: ColorScheme? {
        switch self {
        case .light: return .light
        case .dark: return .dark
        case .system: return nil
        }
    }
}

extension UIColor {
    convenience init(zipHex hex: UInt32, alpha: CGFloat = 1) {
        let r = CGFloat((hex >> 16) & 0xFF) / 255
        let g = CGFloat((hex >> 8) & 0xFF) / 255
        let b = CGFloat(hex & 0xFF) / 255
        self.init(red: r, green: g, blue: b, alpha: alpha)
    }
}

extension Color {
    init(zipHex hex: UInt32, alpha: Double = 1) {
        self.init(uiColor: UIColor(zipHex: hex, alpha: CGFloat(alpha)))
    }

    /// Témától függő szín: világos és sötét változat.
    static func zipDyn(_ light: UInt32, _ dark: UInt32) -> Color {
        Color(uiColor: UIColor { tc in
            tc.userInterfaceStyle == .dark ? UIColor(zipHex: dark) : UIColor(zipHex: light)
        })
    }
}

/// A prototípus iPhone-os színei (a CSS `--a-*` tokenjei) világos és sötét változatban.
enum ZIPV {
    // Alap felületek
    static let bg = Color.zipDyn(0xF3F5F9, 0x0F1115)
    static let surface = Color.zipDyn(0xFFFFFF, 0x1B1E26)
    static let ink = Color.zipDyn(0x0E1733, 0xE4E7EF)
    static let sub = Color.zipDyn(0x677087, 0x9AA2B3)
    static let line = Color.zipDyn(0xE3E7EF, 0x2C313C)
    static let chip = Color.zipDyn(0xEEF1F6, 0x262B35)
    static let emptyBg = Color.zipDyn(0xF3F5F9, 0x20242C)
    static let placeholder = Color.zipDyn(0x8A90A0, 0x6E7587)

    // Márka és állapot
    static let blue = Color.zipDyn(0x2A5BD7, 0x8FAEFF)
    static let blueFill = Color.zipDyn(0x2A5BD7, 0x3F6FE8)
    static let onBlueFill = Color.white
    static let blueSoft = Color.zipDyn(0xE8EEFC, 0x243150)
    static let green = Color.zipDyn(0x1F9D57, 0x5CCB8C)
    static let greenSoft = Color.zipDyn(0xE4F6EE, 0x1D3A2B)
    static let red = Color.zipDyn(0xD13B3B, 0xFF8A80)
    static let redFill = Color.zipDyn(0xD13B3B, 0xC8453F)
    static let redSoft = Color.zipDyn(0xFDECEC, 0x45201F)
    static let warn = Color.zipDyn(0xB86F0E, 0xF0B35C)
    static let warnSoft = Color.zipDyn(0xFBF0DF, 0x3D2E17)
    static let cyan = Color(zipHex: 0x4FB3D9)
    static let qr = Color(zipHex: 0x0B1330)
    static let qrPlate = Color.white
    static let chev = Color.zipDyn(0xB3BAC8, 0x5B6272)
    static let switchOff = Color.zipDyn(0xE3E5EA, 0x2E333D)
    static let dot = Color.zipDyn(0xC7CCD8, 0x4A5060)
    static let grab = Color.zipDyn(0xC9CED9, 0x4A5060)
    static let onAccent = Color.zipDyn(0xFFFFFF, 0x111318)
    static let toast = Color.zipDyn(0x141C33, 0xE4E7EF)
    static let onToast = Color.zipDyn(0xFFFFFF, 0x141C33)
    static let tabSelected = Color.zipDyn(0x0E1733, 0xFFFFFF).opacity(0.07)
    static let shadow = Color(zipHex: 0x0E1733)

    // Grafikon (validált pár: világos #2A5BD7 / #EB6834, sötét #5A86F0 / #E0703E)
    static let series1 = Color.zipDyn(0x2A5BD7, 0x5A86F0)
    static let series2 = Color.zipDyn(0xEB6834, 0xE0703E)
    static let grid = Color.zipDyn(0xE3E7EF, 0x2C313C)

    // Átfedések
    static let scrim = Color(zipHex: 0x080C18, alpha: 0.42)
    static let cam1 = Color(zipHex: 0x2C3448)
    static let cam2 = Color(zipHex: 0x0A0E18)

    // Varázsló kezdőképernyő (mindkét témában sötét)
    static let h1 = Color(zipHex: 0x0C2C63)
    static let h2 = Color(zipHex: 0x2A5BD7)
    static let h3 = Color(zipHex: 0x05163A)
    static let accent = Color(zipHex: 0x0FBEE6)
    static let glow1 = Color(zipHex: 0x4FB3D9, alpha: 0.38)
    static let glow2 = Color(zipHex: 0x2A5BD7, alpha: 0.6)

    // Kurzuslejátszó videó felülete
    static let player = Color(zipHex: 0x0B0B0F)
}

/// A varázsló blokkjainak színpárja: erős szín + lágy háttér, témánként.
struct ZIPBlockColor {
    let c: Color
    let soft: Color

    init(_ l: UInt32, _ ls: UInt32, _ d: UInt32, _ ds: UInt32) {
        c = Color.zipDyn(l, d)
        soft = Color.zipDyn(ls, ds)
    }
}

enum ZIPBlockColors {
    static let b1 = ZIPBlockColor(0x2A5BD7, 0xE8EEFC, 0x7FA2FF, 0x22304F) // Személyes adatok
    static let b2 = ZIPBlockColor(0xB86F0E, 0xFBF0DF, 0xE3A04A, 0x3A2B16) // Céges adatok
    static let b3 = ZIPBlockColor(0x0E8494, 0xDCF2F4, 0x3DC1D1, 0x163539) // Online elérés
    static let b4 = ZIPBlockColor(0x1F9D57, 0xE4F6EE, 0x5CCB8C, 0x1D3A2B) // Befejezés
}

/// A lapok, oldalak és a varázsló mozgása: cubic-bezier(.2,.8,.2,1).
extension Animation {
    static let zipMotion = Animation.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.32)
}

/// Betűk: az iPhone-os prototípus méretei (SF Pro).
extension Font {
    static func zipFont(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight)
    }
}


// MARK: Original Design/Components.swift
import SwiftUI
import UIKit

struct ZIPPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .opacity(configuration.isPressed ? 0.85 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

struct ZIPPrimaryButton: View {
    let title: String
    var icon: String?
    var trailingIcon: String?
    var secondaryText: String?
    var enabled: Bool
    var dimmed: Bool
    var soft: Bool
    var danger: Bool
    var height: CGFloat
    let action: () -> Void

    init(_ title: String, icon: String? = nil, trailingIcon: String? = nil, secondaryText: String? = nil,
         enabled: Bool = true, dimmed: Bool = false, soft: Bool = false, danger: Bool = false,
         height: CGFloat = 54, action: @escaping () -> Void) {
        self.title = title
        self.icon = icon
        self.trailingIcon = trailingIcon
        self.secondaryText = secondaryText
        self.enabled = enabled
        self.dimmed = dimmed
        self.soft = soft
        self.danger = danger
        self.height = height
        self.action = action
    }

    private var bg: Color {
        if !enabled { return ZIPV.switchOff }
        if danger { return ZIPV.redFill }
        if soft { return ZIPV.blueSoft }
        return ZIPV.blueFill
    }

    private var fg: Color {
        if !enabled { return ZIPV.sub }
        if soft { return ZIPV.blue }
        return ZIPV.onBlueFill
    }

    private var glow: Color {
        (enabled && !soft && !danger && !dimmed) ? Color(zipHex: 0x2A5BD7, alpha: 0.35) : Color.clear
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                if let icon = icon {
                    Image(systemName: icon).font(.system(size: 18, weight: .semibold))
                }
                Text(title).font(.zipFont(17, .bold)).lineLimit(1)
                if let secondaryText = secondaryText {
                    Text("· " + secondaryText).font(.zipFont(17, .semibold)).opacity(0.8).lineLimit(1)
                }
                if let trailingIcon = trailingIcon {
                    Image(systemName: trailingIcon).font(.system(size: 15, weight: .semibold))
                }
            }
            .foregroundColor(fg)
            .padding(.horizontal, 16)
            .frame(maxWidth: .infinity)
            .frame(height: height)
            .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(bg))
            .shadow(color: glow, radius: 8, x: 0, y: 6)
        }
        .buttonStyle(ZIPPressStyle())
        .disabled(!enabled || dimmed)
        .opacity(dimmed ? 0.4 : 1)
    }
}

struct ZIPSmallOutlineButton: View {
    let title: String
    var icon: String?
    let action: () -> Void

    init(_ title: String, icon: String? = nil, action: @escaping () -> Void) {
        self.title = title
        self.icon = icon
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if let icon = icon {
                    Image(systemName: icon).font(.system(size: 13, weight: .semibold))
                }
                Text(title).font(.zipFont(14, .semibold))
            }
            .foregroundColor(ZIPV.ink)
            .padding(.horizontal, 14)
            .frame(height: 38)
            .background(Capsule().fill(ZIPV.surface))
            .overlay(Capsule().stroke(ZIPV.line, lineWidth: 1))
        }
        .buttonStyle(ZIPPressStyle())
    }
}

struct ZIPRoundIconButton: View {
    let icon: String
    var label: String
    var size: CGFloat
    var plain: Bool
    var tint: Color
    let action: () -> Void

    init(_ icon: String, label: String, size: CGFloat = 44, plain: Bool = false, tint: Color = ZIPV.blue, action: @escaping () -> Void) {
        self.icon = icon
        self.label = label
        self.size = size
        self.plain = plain
        self.tint = tint
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: size * 0.42, weight: .semibold))
                .foregroundColor(tint)
                .frame(width: size, height: size)
                .background(Circle().fill(plain ? Color.clear : ZIPV.surface))
                .overlay(Circle().stroke(plain ? Color.clear : ZIPV.line, lineWidth: 1))
                .contentShape(Circle())
        }
        .buttonStyle(ZIPPressStyle())
        .accessibilityLabel(label)
    }
}

struct ZIPPill: View {
    let text: String
    var background: Color
    var color: Color
    var icon: String?
    var fontSize: CGFloat

    init(_ text: String, background: Color, color: Color, icon: String? = nil, fontSize: CGFloat = 12) {
        self.text = text
        self.background = background
        self.color = color
        self.icon = icon
        self.fontSize = fontSize
    }

    var body: some View {
        HStack(spacing: 4) {
            if let icon = icon {
                Image(systemName: icon).font(.system(size: fontSize, weight: .semibold))
            }
            Text(text).font(.zipFont(fontSize, .semibold)).lineLimit(1)
        }
        .foregroundColor(color)
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(Capsule().fill(background))
    }
}

struct ZIPSpinner: View {
    var color: Color = ZIPV.blue
    var body: some View {
        ProgressView().tint(color)
    }
}

struct ZIPPicImage: View {
    let pic: ZIPPic
    var fit: Bool = false

    var body: some View {
        if let img = pic.uiImage {
            Image(uiImage: img)
                .resizable()
                .aspectRatio(contentMode: fit ? .fit : .fill)
        } else {
            Color.clear
        }
    }
}

struct ZIPFlowLayout: Layout {
    var spacing: CGFloat = 8
    var lineSpacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxW = proposal.width ?? CGFloat.greatestFiniteMagnitude
        var x: CGFloat = 0
        var y: CGFloat = 0
        var lineH: CGFloat = 0
        var widest: CGFloat = 0
        for s in subviews {
            let size = s.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > maxW {
                y += lineH + lineSpacing
                x = 0
                lineH = 0
            }
            x += size.width
            widest = max(widest, x)
            x += spacing
            lineH = max(lineH, size.height)
        }
        return CGSize(width: proposal.width ?? widest, height: y + lineH)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var lineH: CGFloat = 0
        for s in subviews {
            let size = s.sizeThatFits(.unspecified)
            if x > bounds.minX && x + size.width > bounds.maxX {
                y += lineH + lineSpacing
                x = bounds.minX
                lineH = 0
            }
            s.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            lineH = max(lineH, size.height)
        }
    }
}


// MARK: Original Data/Model.swift
import SwiftUI
import UIKit

/// Melyik elrendezés fut: B = egy képernyő (alapértelmezett), A = három fül.
enum ZIPDesignLayout: String, CaseIterable {
    case a, b
}

enum ZIPDesignConfig {
    /// Itt lehet átváltani. Futás közben: hosszan nyomd a VIZIT feliratot → Prototípus lap.
    static let defaultLayout: ZIPDesignLayout = .b
}

enum ZIPHomeTab: Int, CaseIterable {
    case share, profile, more
}

/// Alulról felcsúszó lapok.
enum ZIPSheetKind: String, Identifiable {
    case menu, profiles, edit, share, prototype
    var id: String { rawValue }
}

/// Teljes képernyős, eltakaró nézetek (QR, beolvasás, varázsló).
enum ZIPCoverKind: Identifiable, Hashable {
    case qr(Int)
    case scanner
    case wizard

    var id: String {
        switch self {
        case .qr(let i): return "qr\(i)"
        case .scanner: return "scanner"
        case .wizard: return "wizard"
        }
    }
}

/// Kép: beépített (Mostani kép / Minta logó) vagy a képtárból választott.
enum ZIPPic: Equatable {
    case asset(String)
    case image(UIImage)

    var uiImage: UIImage? {
        switch self {
        case .asset(let name): return UIImage(named: name)
        case .image(let img): return img
        }
    }
}

enum ZIPCardLayout: Int, CaseIterable {
    case portrait, landscape

    var label: String { self == .portrait ? "Álló" : "Fekvő" }
}

enum ZIPQrMode: Int, CaseIterable {
    case profile, contact, photo

    var label: String {
        switch self {
        case .profile: return "Profil"
        case .contact: return "Kontakt"
        case .photo: return "Fényképes"
        }
    }
}

/// A profil szinkron állapota (éles: LOCAL_ONLY, SYNCED, PENDING, SYNCING, RETRY_SCHEDULED, CONFLICT).
enum ZIPSyncStatus: CaseIterable {
    case synced, pending, syncing, retry, conflict, localOnly

    var label: String {
        switch self {
        case .synced: return "Rendben"
        case .pending: return "Függőben"
        case .syncing: return "Folyamatban"
        case .retry: return "Újra"
        case .conflict: return "Ütközés"
        case .localOnly: return "Helyi"
        }
    }
}

/// Kártyaszínvilágok (éles: 6 séma, a varázsló az első 4-et kínálja).
struct ZIPPreset: Identifiable, Equatable {
    let id: String
    let name: String
    let c1: Color
    let c2: Color
    let c3: Color
    let acc: Color

    var gradient: LinearGradient {
        LinearGradient(colors: [c1, c2, c3], startPoint: .topLeading, endPoint: .bottomTrailing)
    }
}

let ZIPPRESETS: [ZIPPreset] = [
    ZIPPreset(id: "tinta", name: "Tinta", c1: Color(zipHex: 0x0C2C63), c2: Color(zipHex: 0x071F4C), c3: Color(zipHex: 0x05163A), acc: Color(zipHex: 0x0FBEE6)),
    ZIPPreset(id: "markakek", name: "Márkakék", c1: Color(zipHex: 0x1668F0), c2: Color(zipHex: 0x0B5CE8), c3: Color(zipHex: 0x0742A8), acc: Color(zipHex: 0x7FD9FF)),
    ZIPPreset(id: "smaragd", name: "Smaragd", c1: Color(zipHex: 0x0E6B4A), c2: Color(zipHex: 0x0A5138), c3: Color(zipHex: 0x063526), acc: Color(zipHex: 0x4FE0A8)),
    ZIPPreset(id: "ametiszt", name: "Ametiszt", c1: Color(zipHex: 0x5B2E9E), c2: Color(zipHex: 0x452278), c3: Color(zipHex: 0x2C1550), acc: Color(zipHex: 0xC9A6FF)),
    ZIPPreset(id: "rez", name: "Réz", c1: Color(zipHex: 0x9A4A18), c2: Color(zipHex: 0x7A3A12), c3: Color(zipHex: 0x50250B), acc: Color(zipHex: 0xFFBE7A)),
    ZIPPreset(id: "grafit", name: "Grafit", c1: Color(zipHex: 0x2B3240), c2: Color(zipHex: 0x1D222D), c3: Color(zipHex: 0x12161E), acc: Color(zipHex: 0x8FD8F0)),
]

func ZIPpresetById(_ id: String) -> ZIPPreset {
    ZIPPRESETS.first { $0.id == id } ?? ZIPPRESETS[0]
}

struct ZIPSocial: Identifiable {
    let id: String
    let label: String
    let placeholder: String
}

let ZIPSOCIALS: [ZIPSocial] = [
    ZIPSocial(id: "linkedin", label: "LinkedIn", placeholder: "linkedin.com/in/…"),
    ZIPSocial(id: "facebook", label: "Facebook", placeholder: "facebook.com/…"),
    ZIPSocial(id: "instagram", label: "Instagram", placeholder: "instagram.com/…"),
    ZIPSocial(id: "tiktok", label: "TikTok", placeholder: "tiktok.com/@…"),
    ZIPSocial(id: "youtube", label: "YouTube", placeholder: "youtube.com/@…"),
    ZIPSocial(id: "x", label: "X", placeholder: "x.com/…"),
    ZIPSocial(id: "github", label: "GitHub", placeholder: "github.com/…"),
    ZIPSocial(id: "other", label: "Egyéb", placeholder: "https://…"),
]

/// Az Adatláthatóság hat kapcsolója (a teljes név mindig látszik).
let ZIPVIS_FIELDS: [(key: String, label: String)] = [
    ("company", "Cég és beosztás"),
    ("email", "E-mail-cím"),
    ("phone", "Telefonszám"),
    ("web", "Weboldal"),
    ("social", "Közösségi profilok"),
    ("address", "Lakcím"),
]

func ZIPdefaultVis() -> [String: Bool] {
    var m: [String: Bool] = [:]
    for f in ZIPVIS_FIELDS { m[f.key] = true }
    return m
}

struct ZIPProfile: Identifiable, Equatable {
    var id: String
    var label: String
    var real: Bool
    var name: String
    var title: String = ""
    var company: String = ""
    var bio: String = ""
    var phone: String = ""
    var email: String = ""
    var web: String = ""
    var address: String = ""
    var socials: [String: String] = [:]
    var photo: ZIPPic? = nil
    var logo: ZIPPic? = nil
    var isPublic: Bool = false
    var slug: String = ""
    var customDomain: String = ""
    var domainVerified: Bool = false
    var presetId: String = "tinta"
    var layout: ZIPCardLayout = .portrait
    var showPhoto: Bool = true
    var showQr: Bool = false
    var showSocial: Bool = false
    var vis: [String: Bool] = ZIPdefaultVis()
    var tag: String? = nil
    var fresh: Bool = false

    var preset: ZIPPreset { ZIPpresetById(presetId) }
    /// A címke pöttyének színe = a kártya színvilága.
    var color: Color { preset.c1 }
    var short: String { slug.isEmpty ? "vizitkartyam.hu/…" : "vizitkartyam.hu/p/\(slug)" }
    var url: String { "https://www.vizitkartyam.hu/p/\(slug)" }

    func hasValue(_ key: String) -> Bool {
        switch key {
        case "company": return !company.zipIsBlank || !title.zipIsBlank
        case "email": return !email.zipIsBlank
        case "phone": return !phone.zipIsBlank
        case "web": return !web.zipIsBlank
        case "social": return socials.values.contains { !$0.zipIsBlank }
        case "address": return !address.zipIsBlank
        default: return false
        }
    }

    /// Megosztáskor is látszik-e a mező (ki van töltve és nincs elrejtve).
    func shows(_ key: String) -> Bool { hasValue(key) && vis[key] != false }

    var visibleCount: Int { ZIPVIS_FIELDS.filter { vis[$0.key] != false }.count }

    var initials: String {
        let words = name.split(whereSeparator: { $0 == " " || $0 == "\n" || $0 == "\t" })
        if words.isEmpty { return "V" }
        return words.prefix(2).map { String($0.prefix(1)) }.joined().uppercased()
    }
}

extension String {
    var zipIsBlank: Bool { trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    var zipTrimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }
}

let ZIPACCOUNT_NAME = ""


// MARK: Production adapter (no prototype account, cloud, or publication simulation)
@MainActor
final class ZIPWizardBridge: ObservableObject {
    @Published var wizard: ZIPWizardState?
    @Published var saving = false
    @Published var error: String?
    var onPublish: ((ZIPProfile) throws -> Void)?
    var onExit: (() -> Void)?

    func addPublished(_ profile: ZIPProfile, isPublic: Bool) {
        guard !saving, let wizard, wizard.valid("done"), wizard.profileType != nil else { return }
        guard let onPublish else { error = "A mentés még nem áll készen."; return }
        saving = true
        defer { saving = false }
        do { try onPublish(profile) }
        catch { self.error = error.localizedDescription }
    }

    func closeWizard(force: Bool) {
        guard !saving, let wizard else { return }
        if wizard.dirty && !force { wizard.confirmOpen = true; return }
        onExit?()
    }
}

struct ProfileWizard: View {
    var isAdditional = false
    var onSaving: () -> Void = {}
    var onSaveFailed: () -> Void = {}
    var onFinished: () -> Void = {}
    var onCancel: (() -> Void)? = nil
    @EnvironmentObject private var store: AppStore
    @EnvironmentObject private var presentation: CardPresentationStore
    @StateObject private var bridge = ZIPWizardBridge()

    var body: some View {
        ZIPWizardView()
            .environmentObject(bridge)
            .disabled(bridge.saving)
            .overlay {
                if bridge.saving {
                    ZStack {
                        Color.black.opacity(0.15).ignoresSafeArea()
                        ProgressView("Mentés…").padding(24).background(.regularMaterial)
                    }
                }
            }
            .alert("A névjegy mentése nem sikerült", isPresented: Binding(
                get: { bridge.error != nil }, set: { if !$0 { bridge.error = nil } }
            )) {
                Button("Rendben") { bridge.error = nil }
            } message: { Text(bridge.error ?? "") }
            .task(id: store.accountID) { configure() }
    }

    private func configure() {
        let owner = store.accountID
        let slugs = store.businessCards.map { $0.profile.publicSlug }
        bridge.wizard = ZIPWizardState(takenSlugs: { slugs })
        bridge.onExit = {
            if let onCancel { onCancel() }
            else { bridge.wizard = ZIPWizardState(takenSlugs: { slugs }) }
        }
        bridge.onPublish = { value in
            // A draft never crosses an account boundary, even during a late callback.
            guard owner != nil, owner == store.accountID else {
                throw ZIPWizardSaveError(message: "A munkamenet megváltozott. Jelentkezz be újra.")
            }
            let profile = try ZIPWizardProduction.profile(value)
            var style = CardPresentation()
            style.colorway = ZIPWizardProduction.colorway(value.presetId)
            onSaving()
            do {
                if isAdditional { try store.createBusinessCard(profile, presentation: style) }
                else {
                    try store.save(profile)
                    store.updateActiveCardPresentation(style)
                }
                presentation.value = style
                // save() is local-first. The real home-screen sync status remains the
                // source of truth; no timer or simulated "published" success is used.
                onFinished()
            } catch {
                onSaveFailed()
                throw error
            }
        }
    }
}

private struct ZIPWizardSaveError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

private enum ZIPWizardProduction {
    static func colorway(_ id: String) -> CardColorway {
        switch id {
        case "markakek": return .brand
        case "smaragd": return .emerald
        case "ametiszt": return .amethyst
        default: return .ink
        }
    }

    static func profile(_ source: ZIPProfile) throws -> ContactProfile {
        var target = ContactProfile()
        target.fullName = source.name
        target.company = source.company
        target.jobTitle = source.title
        target.phone = source.phone
        target.email = source.email
        target.website = try url(source.web)
        target.address = source.address
        target.bio = source.bio
        target.publicSlug = source.slug
        target.isPublic = source.isPublic
        target.photoBase64 = try jpeg(source.photo)
        target.logoBase64 = try jpeg(source.logo)
        for platform in SocialPlatform.allCases {
            let key = platform.label == "Egyéb" ? "other" : platform.label.lowercased()
            target.setSocialURL(try url(source.socials[key] ?? ""), for: platform)
        }
        return target
    }

    private static func url(_ value: String) throws -> String {
        let value = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard value.isEmpty || SafeLink.https(value) != nil else {
            throw ZIPWizardSaveError(message: "Érvényes, https:// kezdetű webcímet adj meg.")
        }
        return value
    }

    private static func jpeg(_ pic: ZIPPic?) throws -> String {
        guard let pic else { return "" }
        guard let image = pic.uiImage, image.size.width > 0, image.size.height > 0 else {
            throw ZIPWizardSaveError(message: "A kiválasztott kép nem dolgozható fel.")
        }
        for side: CGFloat in [512, 384, 256] {
            let scale = min(1, side / max(image.size.width, image.size.height))
            let size = CGSize(width: image.size.width * scale, height: image.size.height * scale)
            let format = UIGraphicsImageRendererFormat()
            format.scale = 1
            let resized = UIGraphicsImageRenderer(size: size, format: format).image { _ in
                image.draw(in: CGRect(origin: .zero, size: size))
            }
            for quality: CGFloat in [0.82, 0.65, 0.45] {
                if let data = resized.jpegData(compressionQuality: quality), data.count <= 256 * 1024 {
                    return data.base64EncodedString()
                }
            }
        }
        throw ZIPWizardSaveError(message: "A kép túl nagy. Válassz másik képet.")
    }
}

import ImageIO
private enum ZIPImagePick {
    static func load(_ item: PhotosPickerItem, maxSide: CGFloat, done: @escaping (UIImage?, String?) -> Void) {
        _ = item.loadTransferable(type: Data.self) { result in
            var image: UIImage?
            var failure: String?
            switch result {
            case .success(let data):
                if let data, data.count <= 25 * 1024 * 1024,
                   let source = CGImageSourceCreateWithData(data as CFData, nil),
                   let thumb = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                       kCGImageSourceCreateThumbnailFromImageAlways: true,
                       kCGImageSourceCreateThumbnailWithTransform: true,
                       kCGImageSourceThumbnailMaxPixelSize: maxSide
                   ] as CFDictionary) {
                    image = UIImage(cgImage: thumb)
                } else { failure = "A kép nem nyitható meg, vagy nagyobb 25 MB-nál." }
            case .failure:
                failure = "A kép betöltése nem sikerült. Válassz másik képet."
            }
            let loaded = image
            let error = failure
            DispatchQueue.main.async { done(loaded, error) }
        }
    }
}

/// Preserve the source's size-aware scrolling where supported, without
/// raising the production app's iOS 16 minimum deployment target.
private struct ZIPScrollBounceCompatibility: ViewModifier {
    @ViewBuilder func body(content: Content) -> some View {
        if #available(iOS 16.4, *) {
            content.scrollBounceBehavior(.basedOnSize)
        } else {
            content
        }
    }
}
