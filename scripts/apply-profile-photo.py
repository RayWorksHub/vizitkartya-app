from pathlib import Path

# One-shot, anchored changes to the existing secure beta. Fail closed if the
# source moved; never regenerate auth, overwrite unrelated code or force-push.
def edit(path, old, new, count=1):
    p=Path(path); text=p.read_text()
    assert text.count(old)==count, f'Unexpected source in {path}: {old[:65]!r}'
    p.write_text(text.replace(old,new))

core='ios/Vizit/Core/ContactProfile.swift'
edit(core,'    public var photoBase64 = ""','    public var photoBase64 = ""\n    public var photoSyncInitialized = false')
edit(core,'case website, address, linkedIn, photoBase64, publicSlug, isPublic','case website, address, linkedIn, photoBase64, photoSyncInitialized, publicSlug, isPublic')
edit(core,'        photoBase64 = try values.decodeIfPresent(String.self, forKey: .photoBase64) ?? ""','        photoBase64 = try values.decodeIfPresent(String.self, forKey: .photoBase64) ?? ""\n        photoSyncInitialized = try values.decodeIfPresent(Bool.self, forKey: .photoSyncInitialized) ?? false')
with Path(core).open('a') as f:
    f.write(r'''

/// A bounded JPEG is stored atomically with the owner's RLS-protected profile.
/// Private photos are not uploaded to the globally public avatar bucket.
public enum ProfilePhoto {
    public static func inlineURL(_ base64: String) throws -> String {
        guard !base64.isEmpty else { return "" }
        guard let bytes = Data(base64Encoded: base64), bytes.count <= 256 * 1024,
              bytes.count >= 3, Array(bytes.prefix(3)) == [0xff, 0xd8, 0xff],
              bytes.base64EncodedString() == base64 else { throw ProfileError.invalidPhoto }
        return "data:image/jpeg;base64," + base64
    }

    public static func trustedStorageURL(_ text: String, origin: URL) -> URL? {
        guard let url = SafeLink.https(text), url.host == origin.host, url.port == origin.port,
              url.query == nil, url.fragment == nil,
              !url.path.contains(".."),
              url.path.range(of: #"^/storage/v1/object/public/avatars/[a-fA-F0-9-]{36}/[a-zA-Z0-9_.-]+$"#,
                             options: .regularExpression) != nil else { return nil }
        return url
    }
}

public enum ContactQRLink {
    public static func make(publicURL: URL) -> URL? {
        guard SafeLink.https(publicURL.absoluteString) != nil else { return nil }
        var parts = URLComponents(url: publicURL, resolvingAgainstBaseURL: false)
        parts?.queryItems = [URLQueryItem(name: "contact", value: "1")]
        parts?.fragment = nil
        return parts?.url
    }
}
''')

cloud='ios/Vizit/App/CloudService.swift'
edit(cloud,'import Supabase','import Supabase\nimport UIKit\nimport ImageIO')
edit(cloud,'    let updatedAt: String','    let updatedAt: String\n    let avatarURL: String?')
edit(cloud,'        case updatedAt = "updated_at"','        case updatedAt = "updated_at"\n        case avatarURL = "avatar_url"')
edit(cloud,'    let isPublic: Bool\n\n    enum CodingKeys','    let isPublic: Bool\n    let avatarURL: String\n\n    enum CodingKeys')
# CodingKeys appears in both read and write models.
edit(cloud,'        case isPublic = "is_public"','        case isPublic = "is_public"\n        case avatarURL = "avatar_url"',2)
edit(cloud,'        case updatedAt = "updated_at"\n        case avatarURL = "avatar_url"','        case updatedAt = "updated_at"')
old='id,owner_id,slug,display_name,job_title,company,public_email,phone,website,address,is_public,updated_at'
edit(cloud,old,old+',avatar_url',3)
edit(cloud,'        profile.isPublic = remote.isPublic\n        return (remote, profile)',r'''        profile.isPublic = remote.isPublic
        if let avatar = remote.avatarURL {
            profile.photoBase64 = try await readProfilePhoto(avatar)
            profile.photoSyncInitialized = true
        } else if local.photoSyncInitialized {
            // A later deletion from the web must not resurrect an old local image.
            profile.photoBase64 = ""
        }
        return (remote, profile)''')
edit(cloud,'let payload = write(profile,','let payload = try write(profile,',2)
edit(cloud,'slug: String) -> ProfileWrite {','slug: String) throws -> ProfileWrite {')
edit(cloud,'                            isPublic: p.isPublic)','                            isPublic: p.isPublic, avatarURL: try ProfilePhoto.inlineURL(p.photoBase64))')
anchor='    private func request<Response: Decodable>'
addition=r'''    private func readProfilePhoto(_ avatar: String) async throws -> String {
        if avatar.isEmpty { return "" }
        if avatar.hasPrefix("data:image/jpeg;base64,") {
            let base64 = String(avatar.dropFirst("data:image/jpeg;base64,".count))
            _ = try ProfilePhoto.inlineURL(base64)
            return base64
        }
        guard let url = ProfilePhoto.trustedStorageURL(avatar, origin: configuration.supabaseURL)
        else { throw ProfileError.invalidPhoto }
        var request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 8)
        request.setValue("image/jpeg,image/png,image/webp", forHTTPHeaderField: "Accept")
        let (stream, response) = try await http.bytes(for: request, delegate: PhotoRedirectBlocker())
        guard let response = response as? HTTPURLResponse, response.statusCode == 200,
              response.expectedContentLength <= 3 * 1024 * 1024 else { throw ProfileError.invalidPhoto }
        var bytes = Data()
        for try await byte in stream {
            if bytes.count >= 3 * 1024 * 1024 { throw ProfileError.invalidPhoto }
            bytes.append(byte)
        }
        try Task.checkCancellation()
        guard let source = CGImageSourceCreateWithData(bytes as CFData, nil),
              CGImageSourceGetCount(source) == 1,
              let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              let width = properties[kCGImagePropertyPixelWidth] as? NSNumber,
              let height = properties[kCGImagePropertyPixelHeight] as? NSNumber,
              width.doubleValue * height.doubleValue <= 16_000_000,
              let thumbnail = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: 512
              ] as CFDictionary),
              let jpeg = UIImage(cgImage: thumbnail).jpegData(compressionQuality: 0.8),
              jpeg.count <= 256 * 1024 else { throw ProfileError.invalidPhoto }
        return jpeg.base64EncodedString()
    }

'''
edit(cloud,anchor,addition+anchor)
with Path(cloud).open('a') as f:
    f.write(r'''

private final class PhotoRedirectBlocker: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    func urlSession(_ session: URLSession, task: URLSessionTask,
                    willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest,
                    completionHandler: @escaping (URLRequest?) -> Void) {
        completionHandler(nil)
    }
}
''')

app='ios/Vizit/App/VizitApp.swift'
edit(app,'            if metadata.pendingUpload {',r'''            // Upgrade the previous local-only photo only when the exact same
            // cloud revision is still current. Existing conflict protection stays.
            if !metadata.pendingUpload, let (remote, _) = remoteBundle,
               remote.avatarURL == nil, !localProfile.photoSyncInitialized,
               !localProfile.photoBase64.isEmpty,
               metadata.profileID == remote.id, metadata.remoteUpdatedAt == remote.updatedAt {
                metadata.pendingUpload = true
                try metadataStore.save(metadata)
            }

            if metadata.pendingUpload {''')
edit(app,'                metadata.pendingUpload = false',r'''                var photoSynced = profile
                photoSynced.photoSyncInitialized = true
                try storage.save(photoSynced)
                profile = photoSynced
                metadata.pendingUpload = false''')

screens='ios/Vizit/App/Screens.swift'
edit(screens,'Text("Kontakt QR").tag(false)','Text(photoContactURL != nil ? "Fényképes QR" : "Kontakt QR").tag(false)')
edit(screens,'Text(usePublicProfile ? "Nyilvános VIZIT-profil" : "Kontakt QR")','Text(usePublicProfile ? "Nyilvános VIZIT-profil" : (photoContactURL != nil ? "Névjegy profilképpel" : "Offline Kontakt QR"))')
edit(screens,'                                             : "A fogadó telefon kamerája közvetlenül névjegyként tudja felismerni.")','                                             : (photoContactURL != nil\n                                                ? "Beolvasás után a névjegyoldalon a képpel együtt menthető a kontakt."\n                                                : "Közvetlen, internet nélküli névjegyátadás – profilkép nélkül."))')
edit(screens,'A nevet, telefonszámot, e-mail-címet, céget, beosztást, címet és hivatkozásokat. A profilkép a QR-ban nincs benne, az AirDroppal küldött névjegyben viszont igen.','A szinkronizált, nyilvános profil fényképes QR-ja megnyitja a névjegyoldalt, ahonnan a kép is elmenthető. Ehhez a fogadó telefonnak internet kell. A nem nyilvános vagy még nem szinkronizált profil Offline Kontakt QR-ja csak szöveget ad át. AirDroppal a kép közvetlenül a névjegyfájlban érkezik.')
edit(screens,'        return try? VCard.qrPayload(store.profile)','        if let url = photoContactURL { return url.absoluteString }\n        return try? VCard.qrPayload(store.profile)')
edit(screens,'        guard let base = store.configuration?.publicProfileBaseURL else { return nil }','        guard store.profile.isPublic, store.syncStatus == .synced,\n              let base = store.configuration?.publicProfileBaseURL else { return nil }')
edit(screens,'    private func secondaryAction(title:',r'''    private var photoContactURL: URL? {
        guard !store.profile.photoBase64.isEmpty, store.profile.photoSyncInitialized,
              let url = publicURL else { return nil }
        return ContactQRLink.make(publicURL: url)
    }

    private func secondaryAction(title:''')
# Existing CI config is the source of the public profile domain in device builds.
workflow='.github/workflows/ios.yml'
edit(workflow,'VIZIT_PUBLIC_PROFILE_BASE_URL: https://vizit.hu/p','VIZIT_PUBLIC_PROFILE_BASE_URL: https://vizit-kartya.hu/p')
edit(workflow,'branches: [feature/ios-secure-beta]','branches: [feature/ios-secure-beta, feature/vizit-public-profile-photo]')

Path('ios/Tests/VizitCoreTests/ProfilePhotoTests.swift').write_text(r'''import XCTest
@testable import VizitCore

final class ProfilePhotoTests: XCTestCase {
    func testInlinePhotoRoundTrip() throws {
        let bytes = Data([0xff, 0xd8, 0xff, 0x01, 0x02, 0x03])
        XCTAssertEqual(try ProfilePhoto.inlineURL(bytes.base64EncodedString()),
                       "data:image/jpeg;base64," + bytes.base64EncodedString())
    }
    func testEmptyPhotoMeansExplicitRemoval() throws {
        XCTAssertEqual(try ProfilePhoto.inlineURL(""), "")
    }
    func testInvalidAndOversizedPhotosAreRejected() {
        XCTAssertThrowsError(try ProfilePhoto.inlineURL("not base64"))
        XCTAssertThrowsError(try ProfilePhoto.inlineURL(Data([1, 2, 3]).base64EncodedString()))
        XCTAssertThrowsError(try ProfilePhoto.inlineURL(Data(repeating: 255, count: 262145).base64EncodedString()))
    }
    func testLegacyLocalPhotoCanBeMigratedWithoutDiscardingIt() throws {
        let old = Data(#"{"fullName":"Teszt Elek","phone":"123","photoBase64":"/9j/"}"#.utf8)
        let p = try JSONDecoder().decode(ContactProfile.self, from: old)
        XCTAssertFalse(p.photoSyncInitialized)
        XCTAssertEqual(p.photoBase64, "/9j/")
    }
    func testPhotoSyncMarkerPersists() throws {
        var p = ContactProfile(); p.photoSyncInitialized = true
        let restored = try JSONDecoder().decode(ContactProfile.self, from: JSONEncoder().encode(p))
        XCTAssertTrue(restored.photoSyncInitialized)
    }
    func testStorageOriginAndPathAreBounded() throws {
        let origin = try XCTUnwrap(URL(string: "https://example.supabase.co"))
        let path = "/storage/v1/object/public/avatars/12345678-1234-1234-1234-123456789abc/avatar.jpg"
        XCTAssertNotNil(ProfilePhoto.trustedStorageURL(origin.absoluteString + path, origin: origin))
        for url in ["https://attacker.test" + path, "http://example.supabase.co" + path,
                    "https://example.supabase.co/rest/v1/profiles", origin.absoluteString + path + "?redirect=1"] {
            XCTAssertNil(ProfilePhoto.trustedStorageURL(url, origin: origin))
        }
    }
    func testPhotoContactQRIsShortAndContainsNoPrivatePayload() throws {
        let url = try XCTUnwrap(URL(string: "https://vizit-kartya.hu/p/teszt-elek"))
        let link = try XCTUnwrap(ContactQRLink.make(publicURL: url))
        XCTAssertEqual(link.absoluteString, "https://vizit-kartya.hu/p/teszt-elek?contact=1")
        XCTAssertFalse(link.absoluteString.contains("PHOTO"))
        XCTAssertLessThan(link.absoluteString.utf8.count, 100)
    }
}
''')
print('Applied scoped profile photo changes; authentication and RLS unchanged.')
