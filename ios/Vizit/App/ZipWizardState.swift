import SwiftUI
import Foundation


// Production adapter for the exact wizard delivered in VizitTeljesIOS.zip.
// The block structure, copy, spacing, colours and motion come from the ZIP;
// only persistence is delegated to the current AppStore.

enum ZWProfileType: Equatable { case business, individual }
enum ZWBlockStatus: Equatable { case todo, active, done, skip, open }

struct ZWBlock: Identifiable {
    let id: String
    let title: String
    let icon: String
    let color: ZWBlockColor
    let items: [String]
    var isOptional = false
}

struct ZWBlockColor {
    let color: Color
    let soft: Color
    init(_ light: UInt32, _ lightSoft: UInt32, _ dark: UInt32, _ darkSoft: UInt32) {
        color = ZW.dynamic(light, dark)
        soft = ZW.dynamic(lightSoft, darkSoft)
    }
}

enum ZW {
    static func dynamic(_ light: UInt32, _ dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
    static let bg = dynamic(0xF3F5F9, 0x0F1115)
    static let surface = dynamic(0xFFFFFF, 0x1B1E26)
    static let ink = dynamic(0x0E1733, 0xE4E7EF)
    static let sub = dynamic(0x677087, 0x9AA2B3)
    static let line = dynamic(0xE3E7EF, 0x2C313C)
    static let chip = dynamic(0xEEF1F6, 0x262B35)
    static let emptyBg = dynamic(0xF3F5F9, 0x20242C)
    static let placeholder = dynamic(0x8A90A0, 0x6E7587)
    static let blue = dynamic(0x2A5BD7, 0x8FAEFF)
    static let blueFill = dynamic(0x2A5BD7, 0x3F6FE8)
    static let blueSoft = dynamic(0xE8EEFC, 0x243150)
    static let green = dynamic(0x1F9D57, 0x5CCB8C)
    static let greenSoft = dynamic(0xE4F6EE, 0x1D3A2B)
    static let red = dynamic(0xD13B3B, 0xFF8A80)
    static let redSoft = dynamic(0xFDECEC, 0x45201F)
    static let outline = dynamic(0x79808F, 0x8C92A0)
    static let chev = dynamic(0xB3BAC8, 0x5B6272)
    static let switchOff = dynamic(0xE3E5EA, 0x2E333D)
    static let shadow = Color(uiColor: UIColor(hex: 0x0E1733))
    static let scrim = Color(uiColor: UIColor(hex: 0x080C18, alpha: 0.42))
    static let h1 = Color(uiColor: UIColor(hex: 0x0C2C63))
    static let h2 = Color(uiColor: UIColor(hex: 0x2A5BD7))
    static let h3 = Color(uiColor: UIColor(hex: 0x05163A))
    static let glow1 = Color(uiColor: UIColor(hex: 0x4FB3D9, alpha: 0.38))
    static let glow2 = Color(uiColor: UIColor(hex: 0x2A5BD7, alpha: 0.60))

    static let b1 = ZWBlockColor(0x2A5BD7, 0xE8EEFC, 0x7FA2FF, 0x22304F)
    static let b2 = ZWBlockColor(0xB86F0E, 0xFBF0DF, 0xE3A04A, 0x3A2B16)
    static let b3 = ZWBlockColor(0x0E8494, 0xDCF2F4, 0x3DC1D1, 0x163539)
    static let b4 = ZWBlockColor(0x1F9D57, 0xE4F6EE, 0x5CCB8C, 0x1D3A2B)
}

let ZW_BLOCKS = [
    ZWBlock(id: "personal", title: "Személyes adatok", icon: "person", color: ZW.b1,
            items: ["Profilkép", "Név", "Telefon"]),
    ZWBlock(id: "company", title: "Céges adatok", icon: "briefcase", color: ZW.b2,
            items: ["Logó", "Cégnév", "Beosztás", "Hely", "Bemutatkozás"]),
    ZWBlock(id: "online", title: "Online elérés", icon: "globe", color: ZW.b3,
            items: ["E-mail", "Weboldal", "Közösségi profilok"], isOptional: true),
    ZWBlock(id: "done", title: "Befejezés", icon: "checkmark", color: ZW.b4,
            items: ["Profil címe", "Szín", "Láthatóság"]),
]

let ZW_SOCIALS: [(id: String, label: String, placeholder: String)] = [
    ("linkedin", "LinkedIn", "linkedin.com/in/…"),
    ("facebook", "Facebook", "facebook.com/…"),
    ("instagram", "Instagram", "instagram.com/…"),
    ("tiktok", "TikTok", "tiktok.com/@…"),
    ("youtube", "YouTube", "youtube.com/@…"),
    ("x", "X", "x.com/…"),
    ("github", "GitHub", "github.com/…"),
    ("custom", "Egyéb", "https://…"),
]

struct ZWSlugState { let ok: Bool; let message: String }

@MainActor
final class ZWState: ObservableObject {
    @Published var profileType: ZWProfileType? = nil
    @Published var started = false
    @Published var dirty = false
    @Published var name = ""
    @Published var phone = ""
    @Published var photoBase64 = ""
    @Published var company = ""
    @Published var role = ""
    @Published var place = ""
    @Published var bio = ""
    @Published var logoBase64 = ""
    @Published var email = ""
    @Published var web = ""
    @Published var socials: [String: String] = [:]
    @Published var socialOrder: [String] = []
    @Published var colorway: CardColorway = .ink
    @Published var slug = ""
    @Published var isPublic = true
    @Published var statuses: [String: ZWBlockStatus] = [
        "personal": .todo, "company": .todo, "online": .todo, "done": .todo,
    ]
    @Published var introShown = true
    @Published var confirmOpen = false
    @Published var errors: [String: String] = [:]

    private var slugEdited = false
    var takenSlugs: [String] = []

    var flow: [ZWBlock] {
        ZW_BLOCKS.filter { profileType != .individual || $0.id != "company" }
    }
    var isBusiness: Bool { profileType == .business }
    func status(_ id: String) -> ZWBlockStatus { statuses[id] ?? .todo }
    var activeBlock: ZWBlock? { flow.first { status($0.id) == .active } }

    func fieldKeys(_ id: String) -> [String] {
        switch id {
        case "personal": return ["name", "phone"]
        case "company": return ["company", "role", "place", "bio"]
        case "online": return ["email", "web"] + socialOrder.map { "s-" + $0 }
        case "done": return ["slug"]
        default: return []
        }
    }

    func chooseType(_ type: ZWProfileType) {
        if profileType == type { return }
        profileType = type
        dirty = true
        if type == .individual && status("company") == .active {
            statuses["company"] = .todo
            if status("online") == .todo { statuses["online"] = .active }
        }
        if type == .business && started && status("company") == .todo && status("personal") != .active {
            statuses["company"] = .active
            for id in ["online", "done"] where status(id) == .active { statuses[id] = .todo }
        }
        if !slugEdited && status("done") != .todo { slug = suggestedSlug() }
        if started && activeBlock == nil,
           let b = flow.first(where: { status($0.id) == .todo || status($0.id) == .skip }) {
            if b.id == "done" && (!slugEdited || slug.isEmpty) { slug = suggestedSlug() }
            statuses[b.id] = .active
        }
    }

    func begin() {
        if !started {
            started = true
            statuses["personal"] = .active
        }
        introShown = false
    }

    func has(_ id: String) -> Bool {
        guard id == "online" else { return true }
        if !email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            !web.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return true }
        return socialOrder.contains { !(socials[$0] ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    }

    func valid(_ id: String) -> Bool {
        switch id {
        case "personal": return !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        case "company": return !company.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        case "online": return emailValid(email)
        case "done": return slugState().ok && valid("personal") && emailValid(email) && (!isBusiness || valid("company"))
        default: return true
        }
    }

    func isSkip(_ block: ZWBlock) -> Bool { block.isOptional && !has(block.id) }
    func actionLabel(_ block: ZWBlock) -> String {
        if block.id == "done" { return isPublic ? "Publikálás" : "Mentés" }
        return isSkip(block) ? "Kihagyom" : "Tovább"
    }

    func next(_ id: String) {
        guard let index = flow.firstIndex(where: { $0.id == id }), index < flow.count - 1 else { return }
        let block = flow[index]
        statuses[id] = block.isOptional && !has(id) ? .skip : .done
        let next = flow.dropFirst(index + 1).first(where: { status($0.id) == .todo || status($0.id) == .skip }) ?? flow[index + 1]
        if next.id == "done" && (!slugEdited || slug.isEmpty) { slug = suggestedSlug() }
        if status(next.id) == .todo || status(next.id) == .skip { statuses[next.id] = .active }
    }

    func reopen(_ id: String) { statuses[id] = .open }

    func toggleSocial(_ id: String) {
        dirty = true
        if let index = socialOrder.firstIndex(of: id) {
            socialOrder.remove(at: index)
            socials[id] = nil
        } else {
            socialOrder.append(id)
            socials[id] = socials[id] ?? ""
        }
    }

    func input(_ key: String, _ value: String) {
        dirty = true
        errors[key] = nil
        switch key {
        case "name": name = value
        case "phone": phone = value
        case "company": company = value
        case "role": role = value
        case "place": place = value
        case "bio": bio = String(value.prefix(420))
        case "email": email = value
        case "web": web = value
        case "slug": slugEdited = true; slug = cleanSlug(value)
        default:
            if key.hasPrefix("s-") { socials[String(key.dropFirst(2))] = value }
        }
    }

    func value(_ key: String) -> String {
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
        default: return key.hasPrefix("s-") ? socials[String(key.dropFirst(2))] ?? "" : ""
        }
    }

    func emailValid(_ value: String) -> Bool {
        let v = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if v.isEmpty { return true }
        return v.range(of: #"^[^s@]+@[^s@]+.[^s@]{2,}$"#, options: .regularExpression) != nil
    }

    func slugState() -> ZWSlugState {
        if slug.count < 3 { return ZWSlugState(ok: false, message: "Legalább 3 karakter") }
        if slug.range(of: #"^[a-z0-9]+(-[a-z0-9]+)*$"#, options: .regularExpression) == nil {
            return ZWSlugState(ok: false, message: "Kisbetű, szám, kötőjel")
        }
        if (["admin", "vizit"] + takenSlugs).contains(slug) { return ZWSlugState(ok: false, message: "Foglalt") }
        return ZWSlugState(ok: true, message: "Szabad")
    }

    func suggestedSlug() -> String {
        var base = cleanSlug(isBusiness && !company.isEmpty ? company : name)
        if base.isEmpty { base = "nevjegy" }
        let taken = Set(["admin", "vizit"] + takenSlugs)
        if !taken.contains(base) { return base }
        var n = 2
        while taken.contains("(base)-(n)") { n += 1 }
        return "(base)-(n)"
    }

    private func cleanSlug(_ value: String) -> String {
        let folded = value.folding(options: [.diacriticInsensitive, .widthInsensitive], locale: .current).lowercased()
        return String(folded.replacingOccurrences(of: #"[^a-z0-9]+"#, with: "-", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "-")).prefix(40))
    }

    func profile() -> ContactProfile? {
        guard valid("done") else { return nil }
        var p = ContactProfile()
        p.fullName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        p.phone = phone.trimmingCharacters(in: .whitespacesAndNewlines)
        p.photoBase64 = photoBase64
        p.company = company.trimmingCharacters(in: .whitespacesAndNewlines)
        p.jobTitle = role.trimmingCharacters(in: .whitespacesAndNewlines)
        p.address = place.trimmingCharacters(in: .whitespacesAndNewlines)
        p.bio = bio.trimmingCharacters(in: .whitespacesAndNewlines)
        p.logoBase64 = logoBase64
        p.email = email.trimmingCharacters(in: .whitespacesAndNewlines)
        p.website = normalizedURL(web)
        for social in ZW_SOCIALS {
            p.setSocialURL(normalizedURL(socials[social.id] ?? ""), for: platform(social.id))
        }
        p.publicSlug = slug
        p.isPublic = isPublic
        return p
    }

    private func platform(_ id: String) -> SocialPlatform {
        switch id {
        case "linkedin": return .linkedin
        case "facebook": return .facebook
        case "instagram": return .instagram
        case "tiktok": return .tiktok
        case "youtube": return .youtube
        case "x": return .x
        case "github": return .github
        default: return .custom
        }
    }

    private func normalizedURL(_ value: String) -> String {
        let text = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return "" }
        return text.contains("://") ? text : "https://" + text
    }
}

extension UIColor {
    convenience init(hex: UInt32, alpha: CGFloat) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: alpha
        )
    }
}
