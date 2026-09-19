import XCTest
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
    func testPhotoContactQRPayloadIsAContactNotAWebLink() throws {
        var profile = ContactProfile()
        profile.fullName = "Teszt Elek"
        profile.phone = "+36 20 123 4567"
        profile.photoBase64 = Data([0xff, 0xd8, 0xff]).base64EncodedString()
        let payload = try VCard.qrPayload(profile, includePhoto: true)
        XCTAssertTrue(payload.hasPrefix("BEGIN:VCARD\r\n"))
        XCTAssertTrue(payload.contains("PHOTO;ENCODING=b;TYPE=JPEG:"))
        XCTAssertFalse(payload.hasPrefix("https://"))
    }
    func testViewCounterTimestampDoesNotConflictWithUnchangedContent() {
        let id = UUID()
        XCTAssertTrue(ProfileRevisionPolicy.mayUpload(localID: id, localRevision: "old",
            localFingerprint: "same", remoteID: id, remoteRevision: "new", remoteFingerprint: "same"))
    }
    func testChangedPhotoConflictsEvenWhenTimestampMatches() {
        let id = UUID()
        XCTAssertFalse(ProfileRevisionPolicy.mayUpload(localID: id, localRevision: "same",
            localFingerprint: "photo-a", remoteID: id, remoteRevision: "same", remoteFingerprint: "photo-b"))
    }
    func testUnknownBaseAndOtherOwnerNeverPermitUpload() {
        let id = UUID()
        XCTAssertFalse(ProfileRevisionPolicy.mayUpload(localID: id, localRevision: nil,
            localFingerprint: nil, remoteID: id, remoteRevision: "new", remoteFingerprint: "same"))
        XCTAssertFalse(ProfileRevisionPolicy.mayUpload(localID: UUID(), localRevision: "same",
            localFingerprint: "same", remoteID: id, remoteRevision: "same", remoteFingerprint: "same"))
    }
    func testSharingBaseCannotInjectParametersOrFragments() throws {
        var p = ContactProfile(); p.isPublic = true; p.publicSlug = "teszt-elek"
        for address in ["https://example.test/p?x=1", "https://example.test/p#other"] {
            XCTAssertNil(PublicProfileLink.make(baseURL: try XCTUnwrap(URL(string: address)), slug: p.publicSlug))
        }
    }
}
