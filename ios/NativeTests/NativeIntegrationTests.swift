import XCTest
import Contacts
import Vision
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

    func testVisionDecodesGeneratedQR() throws {
        let payload = try VCard.qrPayload(profile())
        let image = try XCTUnwrap(QRImage.make(payload)?.cgImage)
        let request = VNDetectBarcodesRequest()
        request.symbologies = [.qr]
        try VNImageRequestHandler(cgImage: image, options: [:]).perform([request])
        XCTAssertEqual(request.results?.first?.payloadStringValue, payload)
    }

    func testNativeContactContainsMatchingFields() {
        let contact = ContactBridge.contact(profile())
        XCTAssertEqual(contact.givenName, "Elek")
        XCTAssertEqual(contact.familyName, "Őri")
        XCTAssertEqual(contact.phoneNumbers.first?.value.stringValue, "+36201234567")
    }
}
