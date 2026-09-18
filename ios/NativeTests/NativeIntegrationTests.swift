import XCTest
import Contacts
import UIKit
import CoreImage
@testable import VIZIT

final class NativeIntegrationTests: XCTestCase {
    private func profile() -> ContactProfile {
        var p = ContactProfile()
        p.firstName = "Elek"
        p.lastName = "Őri"
        p.email = "test@example.com"
        p.phone = "+36201234567"
        p.company = "Minta; Kft."
        return p
    }

    func testAppleContactsReadsGeneratedVCard() throws {
        let p = profile()
        let contacts = try CNContactVCardSerialization.contacts(with: Data(VCard.encode(p).utf8))
        XCTAssertEqual(contacts.count, 1)
        XCTAssertEqual(contacts[0].givenName, "Elek")
        XCTAssertEqual(contacts[0].familyName, "Őri")
        XCTAssertEqual(contacts[0].organizationName, "Minta; Kft.")
        XCTAssertEqual(contacts[0].emailAddresses.first?.value as String?, "test@example.com")
    }

    func testAppleCoreImageDecodesGeneratedQR() throws {
        let payload = try VCard.qrPayload(profile())
        let rendered = try XCTUnwrap(QRImage.make(payload))
        let image = try XCTUnwrap(rendered.ciImage ?? rendered.cgImage.map { CIImage(cgImage: $0) })
        let detector = try XCTUnwrap(CIDetector(ofType: CIDetectorTypeQRCode,
                                                context: CIContext(options: [.useSoftwareRenderer: true]),
                                                options: [CIDetectorAccuracy: CIDetectorAccuracyHigh]))
        let decoded = detector.features(in: image).compactMap { ($0 as? CIQRCodeFeature)?.messageString }
        XCTAssertEqual(decoded.first, payload)
    }

    func testNativeContactContainsMatchingFields() {
        let contact = ContactBridge.contact(profile())
        XCTAssertEqual(contact.givenName, "Elek")
        XCTAssertEqual(contact.familyName, "Őri")
        XCTAssertEqual(contact.phoneNumbers.first?.value.stringValue, "+36201234567")
    }

    func testSecureSessionStorageRoundTripAndRemoval() throws {
        let storage = SecureSessionStorage(service: "hu.rayworks.vizit.tests.\(UUID().uuidString)")
        let key = "session"
        defer { try? storage.remove(key: key) }
        XCTAssertNil(try storage.retrieve(key: key))
        let value = Data("opaque-test-session".utf8)
        try storage.store(key: key, value: value)
        XCTAssertEqual(try storage.retrieve(key: key), value)
        try storage.remove(key: key)
        XCTAssertNil(try storage.retrieve(key: key))
    }

    func testSyncMetadataUsesProtectedRoundTrip() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("VIZIT-sync-test-\(UUID().uuidString)", isDirectory: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let store = ProfileSyncStore(directory: directory)
        let expected = ProfileSyncMetadata(profileID: UUID(), remoteUpdatedAt: "2026-09-17T12:00:00Z",
                                           pendingUpload: true, conflict: false)
        try store.save(expected)
        XCTAssertEqual(try store.load(), expected)
        try store.reset()
        XCTAssertEqual(try store.load(), ProfileSyncMetadata())
    }

    func testAppleContactsImportsEmbeddedProfilePhoto() throws {
        var p = profile()
        let image = UIGraphicsImageRenderer(size: CGSize(width: 32, height: 24)).image { context in
            UIColor.systemBlue.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 32, height: 24))
        }
        let jpeg = try XCTUnwrap(image.jpegData(compressionQuality: 0.8))
        p.photoBase64 = jpeg.base64EncodedString()
        let contacts = try CNContactVCardSerialization.contacts(with: Data(VCard.encode(p, includePhoto: true).utf8))
        XCTAssertEqual(contacts.count, 1)
        let imported = try XCTUnwrap(contacts.first?.imageData)
        XCTAssertNotNil(UIImage(data: imported))
        XCTAssertEqual(contacts.first?.givenName, "Elek")
    }

    func testOnlyDatabaseUniqueViolationIsRetryableAsSlugCollision() {
        XCTAssertTrue(CloudError.server(status: 409, code: "23505").isUniqueConstraintViolation)
        XCTAssertFalse(CloudError.server(status: 409, code: "PGRST116").isUniqueConstraintViolation)
        XCTAssertFalse(CloudError.server(status: 500, code: "23505").isUniqueConstraintViolation)
    }
    func testPendingPhotoSurvivesJournalReloadBeforeProfileFileWrite() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: directory) }
        let store = ProfileSyncStore(directory: directory)
        var p = profile()
        let image = UIGraphicsImageRenderer(size: CGSize(width: 20, height: 20)).image { context in
            UIColor.systemGreen.setFill(); context.fill(CGRect(x: 0, y: 0, width: 20, height: 20))
        }
        p.photoBase64 = try XCTUnwrap(image.jpegData(compressionQuality: 0.8)).base64EncodedString()
        var journal = ProfileSyncMetadata(); journal.pendingUpload = true; journal.pendingProfile = p
        try store.save(journal)
        XCTAssertEqual(try store.load().pendingProfile?.photoBase64, p.photoBase64)
        XCTAssertTrue(try store.load().pendingUpload)
        journal.pendingProfile?.photoBase64 = ""
        try store.save(journal)
        XCTAssertEqual(try store.load().pendingProfile?.photoBase64, "")
    }
    func testInconsistentJournalIsNotAcceptedAsSynced() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: directory) }
        var journal = ProfileSyncMetadata(); journal.pendingProfile = profile()
        XCTAssertThrowsError(try ProfileSyncStore(directory: directory).save(journal))
    }
    func testRemoteFingerprintTracksContentNotAnalyticsTimestamp() throws {
        let raw = #"{"id":"11111111-1111-4111-8111-111111111111","owner_id":"22222222-2222-4222-8222-222222222222","slug":"teszt-elek","display_name":"Teszt Elek","job_title":"","company":"","public_email":"a@b.test","phone":"123","website":"","address":"","is_public":true,"updated_at":"first","avatar_url":null}"#
        let decode = { (text: String) throws in try JSONDecoder().decode(RemoteProfile.self, from: Data(text.utf8)) }
        let first = try decode(raw)
        XCTAssertEqual(first.fingerprint, try decode(raw.replacingOccurrences(of: "first", with: "later")).fingerprint)
        XCTAssertNotEqual(first.fingerprint, try decode(raw.replacingOccurrences(of: "Teszt Elek", with: "Másik Név")).fingerprint)
        XCTAssertNotEqual(first.fingerprint, try decode(raw.replacingOccurrences(of: #""avatar_url":null"#, with: #""avatar_url":"data:image/jpeg;base64,/9j/""#)).fingerprint)
    }
}
