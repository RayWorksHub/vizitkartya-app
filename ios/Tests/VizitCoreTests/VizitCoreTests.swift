import XCTest
@testable import VizitCore

final class VizitCoreTests: XCTestCase {
    private func sample() -> ContactProfile {
        var value = ContactProfile()
        value.fullName = "Teszt Elek"
        value.firstName = "Elek"
        value.lastName = "Teszt"
        value.phone = "+36 20 123 4567"
        value.email = "teszt@example.com"
        return value
    }
    private func directory() -> URL {
        FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
    }

    func testExplicitNameWins() {
        var p = sample(); p.fullName = "  Másik Név  "
        XCTAssertEqual(p.displayName, "Másik Név")
    }
    func testHungarianNameOrder() {
        var p = sample(); p.fullName = ""
        XCTAssertEqual(p.displayName, "Teszt Elek")
        XCTAssertEqual(p.initials, "TE")
    }
    func testBlankNameFails() {
        XCTAssertThrowsError(try ContactProfile().validate()) { XCTAssertEqual($0 as? ProfileError, .missingName) }
    }
    func testContactMethodRequired() {
        var p = sample(); p.phone = ""; p.email = ""
        XCTAssertThrowsError(try p.validate()) { XCTAssertEqual($0 as? ProfileError, .missingContact) }
        XCTAssertEqual(ProfileError.missingPhoto.errorDescription,
                       "A névjegy megosztásához tölts fel profilképet.")
    }
    func testEmailWithoutPhoneWorks() throws {
        var p = sample(); p.phone = ""; try p.validate()
    }
    func testInvalidEmailFails() {
        var p = sample(); p.email = "email@"
        XCTAssertThrowsError(try p.validate()) { XCTAssertEqual($0 as? ProfileError, .invalidEmail) }
    }
    func testInvalidPhoneFails() {
        var p = sample(); p.phone = "call-me"
        XCTAssertThrowsError(try p.validate()) { XCTAssertEqual($0 as? ProfileError, .invalidPhone) }
    }
    func testMissingURLSchemeFails() {
        var p = sample(); p.website = "example.com"
        XCTAssertThrowsError(try p.validate()) { XCTAssertEqual($0 as? ProfileError, .invalidURL) }
    }
    func testSocialProfilesRoundTripAndVCardExport() throws {
        var p = sample()
        p.linkedIn = "https://linkedin.com/in/teszt"
        p.facebook = "https://facebook.com/teszt"
        p.instagram = "https://instagram.com/teszt"
        p.tiktok = "https://tiktok.com/@teszt"
        p.youtube = "https://youtube.com/@teszt"
        let restored = try JSONDecoder().decode(ContactProfile.self, from: JSONEncoder().encode(p))
        XCTAssertEqual(restored, p)
        let card = try VCard.encode(restored)
        for platform in SocialPlatform.allCases {
            XCTAssertTrue(card.contains("X-SOCIALPROFILE;TYPE=\(platform.rawValue):"))
        }
    }
    func testInsecureSocialProfileFailsValidation() {
        var p = sample(); p.instagram = "http://instagram.com/teszt"
        XCTAssertThrowsError(try p.validate()) { XCTAssertEqual($0 as? ProfileError, .invalidURL) }
    }
    func testSafeLinksRejectExecutableAndCredentialURLs() {
        for value in ["javascript:alert(1)", "file:///etc/passwd", "http://example.com", "https://user:password@example.com", "https:///", "https://exa mple.com", "https://example.com\n"] {
            XCTAssertNil(SafeLink.https(value), value)
        }
        XCTAssertEqual(SafeLink.https("https://example.com/profile?q=a%20b")?.host, "example.com")
    }
    func testOversizedFieldFails() {
        var p = sample(); p.company = String(repeating: "ő", count: 300)
        XCTAssertThrowsError(try p.validate()) { XCTAssertEqual($0 as? ProfileError, .invalidField) }
    }
    func testVCardInjectionFailsValidation() {
        var p = sample(); p.company = "Company\r\nURL:https://evil.example"
        XCTAssertThrowsError(try p.validate()) { XCTAssertEqual($0 as? ProfileError, .invalidField) }
    }
    func testVCardEscaping() {
        XCTAssertEqual(VCard.escape("A;B,C\\D\r\nE"), "A\\;B\\,C\\\\D\\nE")
    }
    func testVCardEnvelopeAndFields() throws {
        var p = sample(); p.company = "Cég; Kft."; p.address = "Budapest, Teszt utca 1."
        let card = try VCard.encode(p)
        XCTAssertTrue(card.hasPrefix("BEGIN:VCARD\r\nVERSION:3.0\r\n"))
        XCTAssertTrue(card.contains("FN:Teszt Elek\r\n"))
        XCTAssertTrue(card.contains("N:Teszt;Elek;;;\r\n"))
        XCTAssertTrue(card.contains("ORG:Cég\\; Kft.\r\n"))
        XCTAssertTrue(card.contains("ADR;TYPE=WORK:;;Budapest\\, Teszt utca 1.;;;;\r\n"))
        XCTAssertTrue(card.hasSuffix("END:VCARD\r\n"))
    }
    func testStandardQRDoesNotIncludePhotoButExplicitPhotoQRDoes() throws {
        var p = sample(); p.photoBase64 = Data([0xff, 0xd8, 0xff]).base64EncodedString()
        XCTAssertFalse(try VCard.qrPayload(p).contains("PHOTO"))
        XCTAssertTrue(try VCard.qrPayload(p, includePhoto: true).contains("PHOTO;ENCODING=b;TYPE=JPEG:"))
        XCTAssertTrue(try VCard.encode(p, includePhoto: true).contains("PHOTO;ENCODING=b;TYPE=JPEG:"))
    }
    func testOversizedQRIsNotSilentlyTruncated() {
        var p = sample()
        p.fullName = String(repeating: "d", count: 80)
        p.firstName = String(repeating: "f", count: 100)
        p.lastName = String(repeating: "l", count: 100)
        p.company = String(repeating: "a", count: 100)
        p.jobTitle = String(repeating: "b", count: 100)
        p.address = String(repeating: "c", count: 180)
        p.phone = "+" + String(repeating: "1", count: 39)
        p.email = String(repeating: "e", count: 64) + "@"
            + String(repeating: "m", count: 63) + "."
            + String(repeating: "n", count: 63) + ".hu"
        p.website = "https://example.com/" + String(repeating: "w", count: 280)
        p.linkedIn = "https://linkedin.com/in/" + String(repeating: "x", count: 276)
        p.facebook = "https://facebook.com/" + String(repeating: "y", count: 279)
        p.instagram = "https://instagram.com/" + String(repeating: "z", count: 278)
        p.tiktok = "https://tiktok.com/@" + String(repeating: "t", count: 279)
        p.youtube = "https://youtube.com/@" + String(repeating: "u", count: 279)
        XCTAssertNoThrow(try p.validate(), "The profile itself must stay inside every database field limit")
        XCTAssertThrowsError(try VCard.qrPayload(p)) { XCTAssertEqual($0 as? ProfileError, .oversizedQR) }
    }
    func testUTF8FoldingPreservesEveryScalar() {
        for value in [String(repeating: "Őű😀;", count: 70), String(repeating: "a", count: 76), "", "Rövid"] {
            let folded = VCard.fold(value)
            XCTAssertEqual(folded.replacingOccurrences(of: "\r\n ", with: ""), value)
            for line in folded.components(separatedBy: "\r\n") { XCTAssertLessThanOrEqual(line.utf8.count, 75) }
        }
    }
    func testEmptyStoreReturnsEmptyProfile() throws {
        XCTAssertEqual(try ProfileFileStore(directory: directory()).load(), ContactProfile())
    }
    func testSaveLoadAndNormalization() throws {
        let dir = directory(); defer { try? FileManager.default.removeItem(at: dir) }
        let store = ProfileFileStore(directory: dir)
        var p = sample(); p.fullName = "  Teszt Elek  "
        try store.save(p)
        XCTAssertEqual(try store.load(), p.normalized)
    }
    func testInvalidSavePreservesExistingProfile() throws {
        let dir = directory(); defer { try? FileManager.default.removeItem(at: dir) }
        let store = ProfileFileStore(directory: dir)
        try store.save(sample())
        XCTAssertThrowsError(try store.save(ContactProfile()))
        XCTAssertEqual(try store.load(), sample())
    }
    func testCorruptFileIsNotOverwritten() throws {
        let dir = directory(); defer { try? FileManager.default.removeItem(at: dir) }
        let store = ProfileFileStore(directory: dir)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        let original = Data("not-json".utf8)
        try original.write(to: store.fileURL)
        XCTAssertThrowsError(try store.load())
        XCTAssertThrowsError(try store.save(sample()))
        XCTAssertEqual(try Data(contentsOf: store.fileURL), original)
    }
    func testNewerSchemaIsNotOverwritten() throws {
        let dir = directory(); defer { try? FileManager.default.removeItem(at: dir) }
        let store = ProfileFileStore(directory: dir)
        try store.save(sample())
        let text = String(decoding: try Data(contentsOf: store.fileURL), as: UTF8.self)
            .replacingOccurrences(of: "\"schemaVersion\":1", with: "\"schemaVersion\":999")
        try Data(text.utf8).write(to: store.fileURL)
        XCTAssertThrowsError(try store.load()) { XCTAssertEqual($0 as? ProfileError, .unsupportedFile) }
        XCTAssertThrowsError(try store.save(sample()))
    }
    func testExplicitResetRemovesProfile() throws {
        let dir = directory(); defer { try? FileManager.default.removeItem(at: dir) }
        let store = ProfileFileStore(directory: dir)
        try store.save(sample()); try store.reset()
        XCTAssertEqual(try store.load(), ContactProfile())
        try store.reset()
    }
    func testInvalidAndOversizedPhotosFail() {
        var p = sample(); p.photoBase64 = "not_base64"
        XCTAssertThrowsError(try p.validate())
        p.photoBase64 = Data(repeating: 1, count: 256 * 1024 + 1).base64EncodedString()
        XCTAssertThrowsError(try p.validate()) { XCTAssertEqual($0 as? ProfileError, .invalidPhoto) }
    }

    func testOlderProfileWithoutCloudFieldsStillDecodes() throws {
        let data = Data(#"{"fullName":"Teszt Elek","phone":"123","email":"","firstName":"","lastName":"","jobTitle":"","company":"","website":"","address":"","linkedIn":"","photoBase64":""}"#.utf8)
        let profile = try JSONDecoder().decode(ContactProfile.self, from: data)
        XCTAssertEqual(profile.fullName, "Teszt Elek")
        XCTAssertEqual(profile.publicSlug, "")
        XCTAssertFalse(profile.isPublic)
        XCTAssertEqual(profile.facebook, "")
        XCTAssertEqual(profile.instagram, "")
        XCTAssertEqual(profile.tiktok, "")
        XCTAssertEqual(profile.youtube, "")
        XCTAssertEqual(profile.customDomain, "")
        XCTAssertFalse(profile.customDomainVerified)
    }

    func testPublicProfileURLRequiresHTTPSAndStrictSlug() {
        let base = URL(string: "https://vizit.hu/p")!
        XCTAssertEqual(PublicProfileLink.make(baseURL: base, slug: "kovacs-anna")?.absoluteString,
                       "https://vizit.hu/p/kovacs-anna")
        for slug in ["ab", "../anna", "Anna", "anna_01", "-anna", "anna-"] {
            XCTAssertNil(PublicProfileLink.make(baseURL: base, slug: slug), slug)
        }
        XCTAssertNil(PublicProfileLink.make(baseURL: URL(string: "http://vizit.hu/p")!, slug: "anna-01"))
    }

    func testProfileCreationHasStableValidCollisionFallbacks() {
        let owner = UUID(uuidString: "E4B43DA4-4717-4E69-B945-7BF454CA9E34")!
        let values = ProfileSlug.creationCandidates(requested: "csukardi-rajmund", ownerID: owner)
        XCTAssertEqual(values, [
            "csukardi-rajmund",
            "csukardi-rajmund-e4b43da4",
            "vizit-e4b43da447174e69b9457bf454ca9e34"
        ])
        XCTAssertTrue(values.allSatisfy { PublicProfileLink.isValidSlug($0) })
        XCTAssertTrue(values.allSatisfy { $0.count <= 50 })
    }

    func testProfileCreationGeneratesReadableHungarianIdentifier() {
        let owner = UUID(uuidString: "E4B43DA4-4717-4E69-B945-7BF454CA9E34")!
        let values = ProfileSlug.creationCandidates(requested: "", displayName: "Csukárdi Rajmund", ownerID: owner)
        XCTAssertEqual(values.first, "csukardi-rajmund")
        XCTAssertTrue(values.allSatisfy { PublicProfileLink.isValidSlug($0) })
    }

    func testVerifiedCustomDomainOverridesCanonicalAddress() {
        let base = URL(string: "https://e-nevjegy.vercel.app/p")!
        XCTAssertEqual(
            PublicProfileLink.preferred(
                baseURL: base, slug: "teszt-elek", customDomain: "nevjegy.example.hu",
                customDomainVerified: true
            )?.absoluteString,
            "https://nevjegy.example.hu"
        )
        XCTAssertEqual(
            PublicProfileLink.preferred(
                baseURL: base, slug: "teszt-elek", customDomain: "nevjegy.example.hu",
                customDomainVerified: false
            )?.absoluteString,
            "https://e-nevjegy.vercel.app/p/teszt-elek"
        )
        XCTAssertFalse(CustomProfileDomain.isValid("https://example.hu/path"))
    }

    func testUnknownFirstUploadRevisionRequiresExplicitConflictResolution() {
        let remoteID = UUID()
        XCTAssertFalse(ProfileSyncPolicy.mayUploadPending(
            localProfileID: nil, localUpdatedAt: nil,
            remoteProfileID: remoteID, remoteUpdatedAt: "2026-09-17T17:00:00Z"
        ))
        XCTAssertFalse(ProfileSyncPolicy.mayUploadPending(
            localProfileID: remoteID, localUpdatedAt: nil,
            remoteProfileID: remoteID, remoteUpdatedAt: "2026-09-17T17:00:00Z"
        ))
        XCTAssertTrue(ProfileSyncPolicy.mayUploadPending(
            localProfileID: remoteID, localUpdatedAt: "2026-09-17T17:00:00Z",
            remoteProfileID: remoteID, remoteUpdatedAt: "2026-09-17T17:00:00Z"
        ))
        XCTAssertFalse(ProfileSyncPolicy.mayUploadPending(
            localProfileID: remoteID, localUpdatedAt: "2026-09-17T16:00:00Z",
            remoteProfileID: remoteID, remoteUpdatedAt: "2026-09-17T17:00:00Z"
        ))
    }

    func testAuthValidationMatchesMobilePolicy() {
        XCTAssertNil(AuthValidation.registration(name: "Teszt Elek", email: "teszt@vizit.hu",
                                                  password: "Titkos123", confirmation: "Titkos123",
                                                  legalAccepted: true))
        XCTAssertNotNil(AuthValidation.registration(name: "T", email: "rossz", password: "12345678",
                                                     confirmation: "12345678", legalAccepted: false))
        XCTAssertNotNil(AuthValidation.password("csakkisbetu1"))
        XCTAssertNotNil(AuthValidation.password("CSAKNAGYBETU1"))
        XCTAssertNil(AuthValidation.deletionPhrase(" törlés "))
        XCTAssertNotNil(AuthValidation.deletionPhrase("delete"))
    }

    func testAuthFailuresProduceActionableMessages() {
        XCTAssertEqual(
            AuthFailureMessage.text(operation: .login, errorCode: "invalid_credentials",
                                    httpStatus: 400, diagnostic: "Invalid login credentials"),
            "Hibás e-mail-cím vagy jelszó. Ha nem emlékszel a jelszóra, kérj újat."
        )
        XCTAssertTrue(
            AuthFailureMessage.text(operation: .callback, errorCode: "unknown", httpStatus: 400,
                                    diagnostic: "both auth code and code verifier should be non-empty")?
                .contains("régebbi VIZIT-verzió") == true
        )
        XCTAssertTrue(
            AuthFailureMessage.text(operation: .passwordResetRequest, errorCode: nil,
                                    httpStatus: 429, diagnostic: nil)?.contains("60 másodpercet") == true
        )
        XCTAssertNil(AuthFailureMessage.text(operation: .login, errorCode: "unexpected_failure",
                                              httpStatus: 500, diagnostic: "server error"))
    }

    func testPasswordResetCooldownCannotGoNegative() {
        let request = Date(timeIntervalSince1970: 100)
        XCTAssertEqual(PasswordResetPolicy.remainingSeconds(since: request,
                                                             now: Date(timeIntervalSince1970: 110)), 50)
        XCTAssertEqual(PasswordResetPolicy.remainingSeconds(since: request,
                                                             now: Date(timeIntervalSince1970: 161)), 0)
        XCTAssertEqual(PasswordResetPolicy.remainingSeconds(since: nil,
                                                             now: Date(timeIntervalSince1970: 110)), 0)
    }

    func testCallbackMustMatchExactRoute() {
        let expected = URL(string: "hu.rayworks.vizit.ios.dev.auth://auth-callback")!
        XCTAssertTrue(AuthCallback.accepts(URL(string: "hu.rayworks.vizit.ios.dev.auth://auth-callback?code=abc")!, expected: expected))
        XCTAssertFalse(AuthCallback.accepts(URL(string: "hu.rayworks.vizit.ios.dev.auth://evil?code=abc")!, expected: expected))
        XCTAssertFalse(AuthCallback.accepts(URL(string: "hu.rayworks.vizit.ios.dev.auth://auth-callback:443?code=abc")!, expected: expected))
        XCTAssertFalse(AuthCallback.accepts(URL(string: "hu.rayworks.vizit.ios.dev.auth://auth-callback/other?code=abc")!, expected: expected))
        XCTAssertFalse(AuthCallback.accepts(URL(string: "https://auth-callback?code=abc")!, expected: expected))
    }
}
