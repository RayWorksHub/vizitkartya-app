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
        case colorway, layout, showsPhoto, showsQR, showsSocial
        case sharesCompany, sharesEmail, sharesPhone, sharesWebsite, sharesSocial, sharesAddress
    }

    /// Every field defaults to its permissive value, so a preference file
    /// written by an older build never silently hides data the owner shares today.
    public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        colorway = CardColorway(rawValue: try values.decodeIfPresent(String.self, forKey: .colorway) ?? "") ?? .ink
        layout = CardLayout(rawValue: try values.decodeIfPresent(String.self, forKey: .layout) ?? "") ?? .portrait
        showsPhoto = try values.decodeIfPresent(Bool.self, forKey: .showsPhoto) ?? true
        showsQR = try values.decodeIfPresent(Bool.self, forKey: .showsQR) ?? false
        showsSocial = try values.decodeIfPresent(Bool.self, forKey: .showsSocial) ?? false
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
        try values.encode(sharesCompany, forKey: .sharesCompany)
        try values.encode(sharesEmail, forKey: .sharesEmail)
        try values.encode(sharesPhone, forKey: .sharesPhone)
        try values.encode(sharesWebsite, forKey: .sharesWebsite)
        try values.encode(sharesSocial, forKey: .sharesSocial)
        try values.encode(sharesAddress, forKey: .sharesAddress)
    }

    /// The number of optional fields currently shared, for the Settings row.
    public var sharedFieldCount: Int {
        [sharesCompany, sharesEmail, sharesPhone, sharesWebsite, sharesSocial, sharesAddress]
            .filter { $0 }.count
    }

    public static let optionalFieldCount = 6
}

public enum CardColorway: String, CaseIterable, Codable, Sendable {
    case ink, brand, emerald, amethyst, copper, graphite

    public var label: String {
        switch self {
        case .ink: return "Tinta"
        case .brand: return "Márkakék"
        case .emerald: return "Smaragd"
        case .amethyst: return "Ametiszt"
        case .copper: return "Réz"
        case .graphite: return "Grafit"
        }
    }

    /// Top-leading → bottom-trailing gradient stops, dark enough in every
    /// variant that white type stays above the 4.5:1 contrast floor.
    public var gradient: (start: UInt32, mid: UInt32, end: UInt32) {
        switch self {
        case .ink: return (0x0C2C63, 0x071F4C, 0x05163A)
        case .brand: return (0x1668F0, 0x0B5CE8, 0x0742A8)
        case .emerald: return (0x0E6B4A, 0x0A5138, 0x063526)
        case .amethyst: return (0x5B2E9E, 0x452278, 0x2C1550)
        case .copper: return (0x9A4A18, 0x7A3A12, 0x50250B)
        case .graphite: return (0x2B3240, 0x1D222D, 0x12161E)
        }
    }

    /// The single edge accent and the wordmark colour on that material.
    public var accent: UInt32 {
        switch self {
        case .ink: return 0x0FBEE6
        case .brand: return 0x7FD9FF
        case .emerald: return 0x4FE0A8
        case .amethyst: return 0xC9A6FF
        case .copper: return 0xFFBE7A
        case .graphite: return 0x8FD8F0
        }
    }
}

public enum CardLayout: String, CaseIterable, Codable, Sendable {
    case portrait, landscape

    public var label: String { self == .portrait ? "Álló" : "Fekvő" }
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
