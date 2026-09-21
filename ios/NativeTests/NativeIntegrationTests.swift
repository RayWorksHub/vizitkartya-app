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
        var p = profile()
        let source = UIGraphicsImageRenderer(size: CGSize(width: 640, height: 640)).image { context in
            UIColor.systemBlue.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 640, height: 640))
        }
        p.photoBase64 = try XCTUnwrap(source.jpegData(compressionQuality: 0.95)).base64EncodedString()
        let payload = try XCTUnwrap(PhotoContactQR.payload(p))
        XCTAssertTrue(payload.contains("PHOTO;ENCODING=b;TYPE=JPEG:"))
        let rendered = try XCTUnwrap(QRImage.make(payload))
        let image = try XCTUnwrap(rendered.ciImage ?? rendered.cgImage.map { CIImage(cgImage: $0) })
        let detector = try XCTUnwrap(CIDetector(ofType: CIDetectorTypeQRCode,
                                                context: CIContext(options: [.useSoftwareRenderer: true]),
                                                options: [CIDetectorAccuracy: CIDetectorAccuracyHigh]))
        let decoded = detector.features(in: image).compactMap { ($0 as? CIQRCodeFeature)?.messageString }
        XCTAssertEqual(decoded.first, payload)
    }

    func testNativeContactContainsMatchingFields() {
        var source = profile()
        source.linkedIn = ""
        source.facebook = "https://facebook.com/teszt"
        source.youtube = "https://youtube.com/@teszt"
        let contact = ContactBridge.contact(source)
        XCTAssertEqual(contact.givenName, "Elek")
        XCTAssertEqual(contact.familyName, "Őri")
        XCTAssertEqual(contact.phoneNumbers.first?.value.stringValue, "+36201234567")
        XCTAssertEqual(contact.urlAddresses.map { $0.value as String }, [
            "https://facebook.com/teszt", "https://youtube.com/@teszt"
        ])
        let contactFile = try ContactBridge.shareFile(source)
        defer { ContactBridge.removeShareFile(contactFile.url) }
        let contactPayload = try String(contentsOf: contactFile.url, encoding: .utf8)
        XCTAssertFalse(contactPayload.contains("PHOTO;ENCODING=b;TYPE=JPEG:"))
        XCTAssertThrowsError(try ContactBridge.shareFile(source, includePhoto: true)) {
            XCTAssertEqual($0 as? ProfileError, .missingPhoto)
        }
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
        let payload = try XCTUnwrap(PhotoContactQR.payload(p))
        let contacts = try CNContactVCardSerialization.contacts(with: Data(payload.utf8))
        XCTAssertEqual(contacts.count, 1)
        let imported = try XCTUnwrap(contacts.first?.imageData)
        XCTAssertNotNil(UIImage(data: imported))
        XCTAssertEqual(contacts.first?.givenName, "Elek")

        let file = try ContactBridge.shareFile(p, includePhoto: true)
        defer { ContactBridge.removeShareFile(file.url) }
        let shared = try String(contentsOf: file.url, encoding: .utf8)
        XCTAssertTrue(shared.contains("PHOTO;ENCODING=b;TYPE=JPEG:"))
    }

    func testOnlyDatabaseUniqueViolationIsRetryableAsSlugCollision() {
        XCTAssertTrue(CloudError.server(status: 409, code: "23505").isUniqueConstraintViolation)
        XCTAssertFalse(CloudError.server(status: 409, code: "PGRST116").isUniqueConstraintViolation)
        XCTAssertFalse(CloudError.server(status: 500, code: "23505").isUniqueConstraintViolation)
    }

    func testRESTURLBuilderPercentEncodesTimestampOffsetPlus() throws {
        let timestamp = "eq.2026-09-18T17:12:04.733081+00:00"
        let url = try XCTUnwrap(RESTURLBuilder.make(
            baseURL: URL(string: "https://example.supabase.co")!,
            path: ["rest", "v1", "profiles"],
            query: [URLQueryItem(name: "updated_at", value: timestamp)]
        ))
        XCTAssertTrue(url.absoluteString.contains("%2B00"))
        XCTAssertFalse(url.absoluteString.contains("+00"))
        XCTAssertEqual(URLComponents(url: url, resolvingAgainstBaseURL: false)?
            .queryItems?.first(where: { $0.name == "updated_at" })?.value, timestamp)
    }

    func testServerErrorIncludesSafeDiagnosticCode() {
        XCTAssertEqual(
            CloudError.server(status: 400, code: "22007").errorDescription,
            "A VIZIT kiszolgáló elutasította a kérést (HTTP 400, kód: 22007)."
        )
    }

    func testOnlyTransientCloudFailuresRetryAutomatically() {
        XCTAssertTrue(CloudError.networkUnavailable.isRetryable)
        XCTAssertTrue(CloudError.server(status: 503, code: nil).isRetryable)
        XCTAssertTrue(CloudError.server(status: 429, code: nil).isRetryable)
        XCTAssertFalse(CloudError.server(status: 400, code: "23514").isRetryable)
        XCTAssertFalse(CloudError.authenticationRequired.isRetryable)
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
        var first = try decode(raw)
        XCTAssertEqual(first.fingerprint, try decode(raw.replacingOccurrences(of: "first", with: "later")).fingerprint)
        XCTAssertNotEqual(first.fingerprint, try decode(raw.replacingOccurrences(of: "Teszt Elek", with: "Másik Név")).fingerprint)
        XCTAssertNotEqual(first.fingerprint, try decode(raw.replacingOccurrences(of: #""avatar_url":null"#, with: #""avatar_url":"data:image/jpeg;base64,/9j/""#)).fingerprint)
        let originalFingerprint = first.fingerprint
        first.facebook = "https://facebook.com/teszt"
        XCTAssertNotEqual(first.fingerprint, originalFingerprint)
    }

    func testHiddenFieldsNeverReachTheSharedVCard() throws {
        var p = profile()
        p.website = "https://pelda.hu"
        p.address = "Budapest, Fő utca 1."
        p.linkedIn = "https://linkedin.com/in/teszt"

        var presentation = CardPresentation()
        presentation.sharesEmail = false
        presentation.sharesAddress = false
        presentation.sharesSocial = false

        let shared = p.visible(through: presentation)
        XCTAssertEqual(shared.email, "")
        XCTAssertEqual(shared.address, "")
        XCTAssertEqual(shared.linkedIn, "")
        // Everything left switched on survives untouched.
        XCTAssertEqual(shared.phone, p.phone)
        XCTAssertEqual(shared.company, p.company)
        XCTAssertEqual(shared.website, p.website)

        let payload = try VCard.encode(shared)
        XCTAssertFalse(payload.contains("test@example.com"))
        XCTAssertFalse(payload.contains("linkedin.com"))
        XCTAssertFalse(payload.contains("ADR;"))
        XCTAssertTrue(payload.contains("+36201234567"))
    }

    func testPresentationDefaultsShareEverythingAndSurviveARoundTrip() throws {
        let fresh = CardPresentation()
        XCTAssertEqual(fresh.sharedFieldCount, CardPresentation.optionalFieldCount)
        XCTAssertEqual(profile().visible(through: fresh), profile())

        var changed = CardPresentation()
        changed.colorway = .paper
        changed.layout = .classic
        changed.showsQR = true
        changed.sharesPhone = false
        let decoded = try JSONDecoder().decode(
            CardPresentation.self,
            from: try JSONEncoder().encode(changed)
        )
        XCTAssertEqual(decoded.colorway, changed.colorway)
        XCTAssertEqual(decoded.layout, changed.layout)
        XCTAssertFalse(decoded.showsQR)
        XCTAssertTrue(decoded.showsPhoto)
        XCTAssertFalse(decoded.showsSocial)
        XCTAssertEqual(decoded.sharesPhone, changed.sharesPhone)
        XCTAssertEqual(decoded.sharedFieldCount, CardPresentation.optionalFieldCount - 1)

        // A preference file written before these keys existed must not start
        // hiding data the owner is sharing today.
        let legacy = try JSONDecoder().decode(CardPresentation.self, from: Data("{}".utf8))
        XCTAssertEqual(legacy, CardPresentation())
    }
}
