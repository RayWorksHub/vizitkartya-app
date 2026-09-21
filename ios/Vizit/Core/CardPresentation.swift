import Foundation

/// How the card looks and which of the owner's data it carries.
///
/// This is a presentation preference, not contact data: it lives on the device
/// next to the profile rather than inside it, so a card restyled here never
/// rewrites the synchronised profile record.
public struct CardPresentation: Codable, Equatable, Sendable {
    public var colorway: CardColorway = .ink
    public var layout: CardLayout = .portrait

    // Which elements the card surface itself shows.
    public var showsPhoto = true
    public var showsQR = false
    public var showsSocial = false
    public var companyLogoBase64 = ""
    public var sectionOrder: [CardSection] = CardSection.allCases

    // Which fields leave the device at all — on the shared card, in the QR and
    // in the vCard.
    public var sharesCompany = true
    public var sharesEmail = true
    public var sharesPhone = true
    public var sharesWebsite = true
    public var sharesSocial = true
    public var sharesAddress = true

    public init() {}

    private enum CodingKeys: String, CodingKey {
        case colorway, layout, showsPhoto, showsQR, showsSocial, companyLogoBase64, sectionOrder
        case sharesCompany, sharesEmail, sharesPhone, sharesWebsite, sharesSocial, sharesAddress
    }

    /// Every field defaults to its permissive value, so a preference file
    /// written by an older build never silently hides data the owner shares today.
    public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        let storedColorway = try values.decodeIfPresent(String.self, forKey: .colorway) ?? ""
        colorway = CardColorway(rawValue: storedColorway) ?? .ink
        let storedLayout = try values.decodeIfPresent(String.self, forKey: .layout) ?? ""
        // The previous two-way control called its horizontal option
        // `landscape`. Preserve that choice as the new classic composition.
        layout = storedLayout == "landscape"
            ? .classic
            : (CardLayout(rawValue: storedLayout) ?? .portrait)
        // Builds before 8.7 exposed three card-face switches that were never
        // part of the approved board. Keep decoding their keys for file
        // compatibility, but migrate every user to the fixed Figma surface.
        _ = try values.decodeIfPresent(Bool.self, forKey: .showsPhoto)
        _ = try values.decodeIfPresent(Bool.self, forKey: .showsQR)
        _ = try values.decodeIfPresent(Bool.self, forKey: .showsSocial)
        showsPhoto = true
        showsQR = false
        showsSocial = false
        companyLogoBase64 = try values.decodeIfPresent(String.self, forKey: .companyLogoBase64) ?? ""
        sectionOrder = try values.decodeIfPresent([CardSection].self, forKey: .sectionOrder) ?? CardSection.allCases
        sharesCompany = try values.decodeIfPresent(Bool.self, forKey: .sharesCompany) ?? true
        sharesEmail = try values.decodeIfPresent(Bool.self, forKey: .sharesEmail) ?? true
        sharesPhone = try values.decodeIfPresent(Bool.self, forKey: .sharesPhone) ?? true
        sharesWebsite = try values.decodeIfPresent(Bool.self, forKey: .sharesWebsite) ?? true
        sharesSocial = try values.decodeIfPresent(Bool.self, forKey: .sharesSocial) ?? true
        sharesAddress = try values.decodeIfPresent(Bool.self, forKey: .sharesAddress) ?? true
    }

    public func encode(to encoder: Encoder) throws {
        var values = encoder.container(keyedBy: CodingKeys.self)
        try values.encode(colorway.rawValue, forKey: .colorway)
        try values.encode(layout.rawValue, forKey: .layout)
        try values.encode(showsPhoto, forKey: .showsPhoto)
        try values.encode(showsQR, forKey: .showsQR)
        try values.encode(showsSocial, forKey: .showsSocial)
        try values.encode(companyLogoBase64, forKey: .companyLogoBase64)
        try values.encode(orderedSections, forKey: .sectionOrder)
        try values.encode(sharesCompany, forKey: .sharesCompany)
        try values.encode(sharesEmail, forKey: .sharesEmail)
        try values.encode(sharesPhone, forKey: .sharesPhone)
        try values.encode(sharesWebsite, forKey: .sharesWebsite)
        try values.encode(sharesSocial, forKey: .sharesSocial)
        try values.encode(sharesAddress, forKey: .sharesAddress)
    }

    /// A stable, complete ordering even after loading an older or duplicate list.
    public var orderedSections: [CardSection] {
        var result: [CardSection] = []
        for section in sectionOrder + CardSection.allCases where !result.contains(section) {
            result.append(section)
        }
        return result
    }

    /// The number of optional fields currently shared, for the Settings row.
    public var sharedFieldCount: Int {
        [sharesCompany, sharesEmail, sharesPhone, sharesWebsite, sharesSocial, sharesAddress]
            .filter { $0 }.count
    }

    public static let optionalFieldCount = 6
}

public enum CardColorway: String, CaseIterable, Codable, Sendable {
    case ink, paper, brand

    public var label: String {
        switch self {
        case .ink: return "Tinta"
        case .paper: return "Papír"
        case .brand: return "Márkakék"
        }
    }

    /// Top-leading → bottom-trailing material gradient stops. Text colour is
    /// selected independently so the paper material keeps accessible contrast.
    public var gradient: (start: UInt32, mid: UInt32, end: UInt32) {
        switch self {
        case .ink: return (0x0C2C63, 0x071F4C, 0x05163A)
        case .paper: return (0xFFFFFF, 0xF8FAFC, 0xEEF2F7)
        case .brand: return (0x1668F0, 0x0B5CE8, 0x0742A8)
        }
    }

    /// The single edge accent and the wordmark colour on that material.
    public var accent: UInt32 {
        switch self {
        case .ink: return 0x0FBEE6
        case .paper: return 0x0B5CE8
        case .brand: return 0x7FD9FF
        }
    }

    public var isLight: Bool { self == .paper }
}

public enum CardSection: String, CaseIterable, Codable, Sendable, Identifiable {
    case identity, company, contact, social, address

    public var id: String { rawValue }

    public var label: String {
        switch self {
        case .identity: return "Személyes adatok"
        case .company: return "Munkahely"
        case .contact: return "Elérhetőségek"
        case .social: return "Közösségi profilok"
        case .address: return "Cím"
        }
    }
}

public enum CardLayout: String, CaseIterable, Codable, Sendable {
    case portrait, minimal, classic

    public var label: String {
        switch self {
        case .portrait: return "Portré"
        case .minimal: return "Minimal"
        case .classic: return "Klasszikus"
        }
    }
}

public extension ContactProfile {
    /// The profile as the recipient sees it: everything the owner switched off
    /// in Adatláthatóság is removed before the card, the QR or the vCard is built.
    func visible(through presentation: CardPresentation) -> ContactProfile {
        var value = self
        if !presentation.sharesCompany { value.company = ""; value.jobTitle = "" }
        if !presentation.sharesEmail { value.email = "" }
        if !presentation.sharesPhone { value.phone = "" }
        if !presentation.sharesWebsite { value.website = "" }
        if !presentation.sharesAddress { value.address = "" }
        if !presentation.sharesSocial {
            for platform in SocialPlatform.allCases { value.setSocialURL("", for: platform) }
        }
        return value
    }
}

/// The owner's presentation choices, persisted in the app's own defaults so a
/// profile reset never takes the card styling with it.
@MainActor
public final class CardPresentationStore: ObservableObject {
    private static let key = "card.presentation.v1"

    @Published public var value: CardPresentation {
        didSet { persist() }
    }

    private let defaults: UserDefaults

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        // Keep UI-test launches independent while preserving real users'
        // presentation choices across every normal update and restart.
        if ProcessInfo.processInfo.arguments.contains("--reset-test-profile") {
            defaults.removeObject(forKey: Self.key)
        }
        if let data = defaults.data(forKey: Self.key),
           let decoded = try? JSONDecoder().decode(CardPresentation.self, from: data) {
            value = decoded
        } else {
            value = CardPresentation()
        }
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(value) else { return }
        defaults.set(data, forKey: Self.key)
    }
}
