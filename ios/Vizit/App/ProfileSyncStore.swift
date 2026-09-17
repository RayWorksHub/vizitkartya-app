import Foundation

struct ProfileSyncMetadata: Codable, Equatable {
    var profileID: UUID?
    var remoteUpdatedAt: String?
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
        guard data.count <= 16_384 else { throw ProfileError.damagedFile }
        return try JSONDecoder().decode(ProfileSyncMetadata.self, from: data)
    }

    func save(_ value: ProfileSyncMetadata) throws {
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
