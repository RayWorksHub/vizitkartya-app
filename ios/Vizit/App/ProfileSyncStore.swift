import Foundation

struct ProfileSyncMetadata: Codable, Equatable {
    var profileID: UUID?
    var remoteUpdatedAt: String?
    var remoteFingerprint: String?
    var pendingProfile: ContactProfile?
    var pendingUpload = false
    var conflict = false
}

struct ProfileSyncStore {
    let fileURL: URL

    init(directory: URL) {
        fileURL = directory.appendingPathComponent("sync-v1.json", isDirectory: false)
    }

    func load() throws -> ProfileSyncMetadata {
        guard FileManager.default.fileExists(atPath: fileURL.path) else { return ProfileSyncMetadata() }
        let data = try Data(contentsOf: fileURL)
        guard data.count <= 524_288 else { throw ProfileError.damagedFile }
        let value = try JSONDecoder().decode(ProfileSyncMetadata.self, from: data)
        try value.pendingProfile?.validateIfPresent()
        guard value.pendingProfile == nil || value.pendingUpload else { throw ProfileError.damagedFile }
        return value
    }

    func save(_ value: ProfileSyncMetadata) throws {
        try value.pendingProfile?.validateIfPresent()
        guard value.pendingProfile == nil || value.pendingUpload else { throw ProfileError.damagedFile }
        try FileManager.default.createDirectory(at: fileURL.deletingLastPathComponent(), withIntermediateDirectories: true)
        var resource = URLResourceValues()
        resource.isExcludedFromBackup = true
        var directory = fileURL.deletingLastPathComponent()
        try directory.setResourceValues(resource)
        try JSONEncoder().encode(value).write(to: fileURL, options: [.atomic, .completeFileProtection])
    }

    func reset() throws {
        if FileManager.default.fileExists(atPath: fileURL.path) { try FileManager.default.removeItem(at: fileURL) }
    }
}

/// A signed-in account's local card index. Contact data and sync journals stay
/// in per-card directories; this file only decides which owned cards exist on
/// the device and which one is active. Keeping the owner on every row prevents
/// a stale index from a previous session being accepted for another account.
struct BusinessCardCatalogEntry: Codable, Equatable, Identifiable, Sendable {
    let profileID: UUID
    let ownerID: UUID
    var isPrimary: Bool
    var createdAt: String
    var updatedAt: String
    var presentation: CardPresentation

    var id: UUID { profileID }
}

struct BusinessCardCatalog: Codable, Equatable, Sendable {
    let ownerID: UUID
    var cards: [BusinessCardCatalogEntry]
    var activeProfileID: UUID?

    init(ownerID: UUID, cards: [BusinessCardCatalogEntry] = [], activeProfileID: UUID? = nil) {
        self.ownerID = ownerID
        self.cards = cards
        self.activeProfileID = activeProfileID
    }

    mutating func normalize() {
        var seen = Set<UUID>()
        cards = cards.filter { $0.ownerID == ownerID && seen.insert($0.profileID).inserted }
        cards.sort {
            if $0.isPrimary != $1.isPrimary { return $0.isPrimary && !$1.isPrimary }
            if $0.createdAt != $1.createdAt { return $0.createdAt < $1.createdAt }
            return $0.profileID.uuidString < $1.profileID.uuidString
        }
        if let activeProfileID, cards.contains(where: { $0.profileID == activeProfileID }) { return }
        activeProfileID = cards.first(where: \.isPrimary)?.profileID ?? cards.first?.profileID
    }
}

struct BusinessCardCatalogStore {
    let ownerID: UUID
    let directory: URL
    let fileURL: URL

    init(directory: URL, ownerID: UUID) {
        self.ownerID = ownerID
        self.directory = directory
        fileURL = directory.appendingPathComponent("business-cards-v1.json", isDirectory: false)
    }

    func cardDirectory(_ profileID: UUID) -> URL {
        directory
            .appendingPathComponent("cards", isDirectory: true)
            .appendingPathComponent(profileID.uuidString.lowercased(), isDirectory: true)
    }

    func load() throws -> BusinessCardCatalog {
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            return BusinessCardCatalog(ownerID: ownerID)
        }
        let data = try Data(contentsOf: fileURL)
        guard data.count <= 1_048_576 else { throw ProfileError.damagedFile }
        var value = try JSONDecoder().decode(BusinessCardCatalog.self, from: data)
        guard value.ownerID == ownerID,
              value.cards.allSatisfy({ $0.ownerID == ownerID }) else {
            throw ProfileError.damagedFile
        }
        value.normalize()
        return value
    }

    func save(_ value: BusinessCardCatalog) throws {
        guard value.ownerID == ownerID,
              value.cards.allSatisfy({ $0.ownerID == ownerID }) else {
            throw ProfileError.damagedFile
        }
        var normalized = value
        normalized.normalize()
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        var resource = URLResourceValues()
        resource.isExcludedFromBackup = true
        var protectedDirectory = directory
        try protectedDirectory.setResourceValues(resource)
        try JSONEncoder().encode(normalized).write(
            to: fileURL,
            options: [.atomic, .completeFileProtection]
        )
    }

    func reset() throws {
        if FileManager.default.fileExists(atPath: fileURL.path) {
            try FileManager.default.removeItem(at: fileURL)
        }
        let cardsDirectory = directory.appendingPathComponent("cards", isDirectory: true)
        if FileManager.default.fileExists(atPath: cardsDirectory.path) {
            try FileManager.default.removeItem(at: cardsDirectory)
        }
    }
}

struct OwnedBusinessCard: Identifiable, Equatable, Sendable {
    let ownerID: UUID
    let profileID: UUID
    var profile: ContactProfile
    var fingerprint: String?
    var isPrimary: Bool
    var createdAt: String
    var updatedAt: String
    var presentation: CardPresentation

    var id: UUID { profileID }
}
