import XCTest
import Contacts
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

    func testOnlyDatabaseUniqueViolationIsRetryableAsSlugCollision() {
        XCTAssertTrue(CloudError.server(status: 409, code: "23505").isUniqueConstraintViolation)
        XCTAssertFalse(CloudError.server(status: 409, code: "PGRST116").isUniqueConstraintViolation)
        XCTAssertFalse(CloudError.server(status: 500, code: "23505").isUniqueConstraintViolation)
    }
}
