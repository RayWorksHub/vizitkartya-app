import XCTest

final class VizitUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func reveal(_ element: XCUIElement, in app: XCUIApplication) {
        if app.keyboards.firstMatch.exists {
            let done = app.buttons["profile.keyboardDone"]
            XCTAssertTrue(done.waitForExistence(timeout: 2))
            done.tap()
        }
        for _ in 0..<6 where !element.isHittable {
            let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.72))
            let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.55))
            start.press(forDuration: 0.05, thenDragTo: end)
        }
        XCTAssertTrue(element.waitForExistence(timeout: 2))
        XCTAssertTrue(element.isHittable)
    }

    private func launchClean() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["--ui-testing", "--reset-test-profile"]
        app.launch()
        return app
    }

    func testConfiguredAppShowsAuthentication() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.buttons["auth.submit"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["Üdv újra!"].exists)
        XCTAssertTrue(app.textFields["auth.email"].exists)
        XCTAssertTrue(app.secureTextFields["auth.password"].exists)

        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "VIZIT-auth-redesign"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }

    func testRegistrationFlowShowsRequiredFieldsAndLegalConsent() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.buttons["Regisztráció"].waitForExistence(timeout: 10))
        app.buttons["Regisztráció"].tap()
        XCTAssertTrue(app.textFields["auth.name"].exists)
        XCTAssertTrue(app.textFields["auth.email"].exists)
        XCTAssertTrue(app.secureTextFields["auth.password"].exists)
        XCTAssertTrue(app.secureTextFields["auth.confirmation"].exists)
        XCTAssertTrue(app.buttons["auth.submit"].exists)
    }

    func testPasswordRecoveryIsVisibleAndKeepsTheCurrentBuildIdentifiable() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.buttons["auth.forgotPassword"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["auth.version"].exists)
        XCTAssertTrue(app.staticTexts["auth.version"].label.contains("VIZIT 10.0.1"))
        app.buttons["auth.forgotPassword"].tap()
        XCTAssertTrue(app.textFields["auth.reset.email"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["auth.reset.submit"].exists)
    }


    func testRegistrationSuccessDoesNotKeepFormFields() {
        let app = XCUIApplication()
        app.launchArguments = ["--ui-test-verification"]
        app.launch()
        XCTAssertTrue(app.buttons["auth.verification.login"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.textFields["auth.name"].exists)
        XCTAssertFalse(app.secureTextFields["auth.password"].exists)
        XCTAssertFalse(app.secureTextFields["auth.confirmation"].exists)
        app.buttons["auth.verification.login"].tap()
        XCTAssertTrue(app.buttons["auth.submit"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Üdv újra!"].exists)
        XCTAssertEqual(app.textFields["auth.email"].value as? String, "nev@pelda.hu")
    }

    func testCreatePersistAndShowQR() {
        let app = launchClean()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 10))
        app.buttons["wizard.private"].tap()
        let name = app.textFields["wizard.fullName"]
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        name.tap(); name.typeText("Teszt Elek")
        app.buttons["wizard.action.personal"].firstMatch.tap()
        let online = app.buttons["wizard.action.online"].firstMatch
        XCTAssertTrue(online.waitForExistence(timeout: 5)); online.tap()
        let publish = app.buttons["wizard.action.done"].firstMatch
        XCTAssertTrue(publish.waitForExistence(timeout: 5)); publish.tap()
        XCTAssertTrue(app.staticTexts["card.name"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.staticTexts["card.name"].label, "Teszt Elek")
        app.terminate(); app.launchArguments = ["--ui-testing"]; app.launch()
        XCTAssertTrue(app.staticTexts["card.name"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.staticTexts["card.name"].label, "Teszt Elek")
        app.buttons["Megosztás"].tap()
        XCTAssertTrue(app.images["share.qr"].waitForExistence(timeout: 5))
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = "ZIP-wizard-created-contact-QR"; shot.lifetime = .keepAlways; add(shot)
    }

    func testEmptyProfileShowsValidation() {
        let app = launchClean()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 10))
        app.buttons["wizard.private"].tap()
        let next = app.buttons["wizard.action.personal"].firstMatch
        XCTAssertTrue(next.waitForExistence(timeout: 5))
        XCTAssertFalse(next.isEnabled)
        XCTAssertTrue(app.textFields["wizard.field.phone"].exists)
        XCTAssertFalse(app.textFields["wizard.field.company"].exists)
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = "ZIP-wizard-personal-block"; shot.lifetime = .keepAlways; add(shot)
    }

    func testBackKeepsDraftWithoutPublishing() {
        let app = launchClean()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 10))
        app.buttons["wizard.private"].tap()
        let name = app.textFields["wizard.fullName"]
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        name.tap(); name.typeText("Nem mentett adat")
        app.buttons["Bezárás"].tap()
        XCTAssertTrue(app.buttons["Maradok"].waitForExistence(timeout: 5))
        app.buttons["Maradok"].tap()
        XCTAssertEqual(name.value as? String, "Nem mentett adat")
        app.terminate(); app.launchArguments = ["--ui-testing"]; app.launch()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["card.name"].exists)
    }
}
