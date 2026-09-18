import Foundation

public struct ProfileFileStore {
    private struct Envelope: Codable {
        var schemaVersion: Int
        var profile: ContactProfile
    }
    public let fileURL: URL

    public init(directory: URL) {
        fileURL = directory.appendingPathComponent("profile-v1.json", isDirectory: false)
    }

    public func load() throws -> ContactProfile {
        guard FileManager.default.fileExists(atPath: fileURL.path) else { return ContactProfile() }
        let bytes = try Data(contentsOf: fileURL)
        guard bytes.count <= 1_048_576 else { throw ProfileError.damagedFile }
        guard let envelope = try? JSONDecoder().decode(Envelope.self, from: bytes) else {
            throw ProfileError.damagedFile
        }
        guard envelope.schemaVersion == 1 else { throw ProfileError.unsupportedFile }
        do { try envelope.profile.validate() } catch { throw ProfileError.damagedFile }
        return envelope.profile
    }

    public func save(_ profile: ContactProfile) throws {
        try profile.validate()
        // Do not silently overwrite a corrupt or newer-schema file.
        _ = try load()
        try FileManager.default.createDirectory(at: fileURL.deletingLastPathComponent(), withIntermediateDirectories: true)
        #if os(iOS)
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var directory = fileURL.deletingLastPathComponent()
        try directory.setResourceValues(values)
        #endif
        let data = try JSONEncoder().encode(Envelope(schemaVersion: 1, profile: profile.normalized))
        #if os(iOS)
        try data.write(to: fileURL, options: [.atomic, .completeFileProtection])
        #else
        try data.write(to: fileURL, options: .atomic)
        #endif
    }

    /// Called only after the user confirms local-data deletion.
    public func reset() throws {
        if FileManager.default.fileExists(atPath: fileURL.path) {
            try FileManager.default.removeItem(at: fileURL)
        }
    }
}
