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

    func testCreatePersistAndShowQR() {
        let app = launchClean()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 10))
        app.buttons["wizard.private"].tap()
        let name = app.textFields["wizard.fullName"]
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        name.tap(); name.typeText("Teszt Elek")
        app.buttons["wizard.primary"].tap()
        for _ in 0..<3 { app.buttons["wizard.skip"].tap() }
        app.buttons["wizard.primary"].tap()
        app.buttons["wizard.primary"].tap()
        XCTAssertTrue(app.buttons["wizard.finish"].waitForExistence(timeout: 5))
        app.buttons["wizard.finish"].tap()
        XCTAssertTrue(app.staticTexts["card.name"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.staticTexts["card.name"].label, "Teszt Elek")
        app.terminate()
        app.launchArguments = ["--ui-testing"]
        app.launch()
        XCTAssertTrue(app.staticTexts["card.name"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.staticTexts["card.name"].label, "Teszt Elek")
        app.buttons["Megosztás"].tap()
        XCTAssertTrue(app.images["share.qr"].waitForExistence(timeout: 5))
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "VIZIT-contact-QR"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }

    func testEmptyProfileShowsValidation() {
        let app = launchClean()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 10))
        app.buttons["wizard.private"].tap()
        let next = app.buttons["wizard.primary"]
        XCTAssertTrue(next.waitForExistence(timeout: 5))
        XCTAssertFalse(next.isEnabled)
    }

    func testBackKeepsDraftWithoutPublishing() {
        let app = launchClean()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 10))
        app.buttons["wizard.private"].tap()
        let name = app.textFields["wizard.fullName"]
        name.tap(); name.typeText("Nem mentett adat")
        app.buttons["Vissza"].tap()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 5))
        app.buttons["wizard.private"].tap()
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        XCTAssertEqual(name.value as? String, "Nem mentett adat")
        app.terminate()
        app.launchArguments = ["--ui-testing"]
        app.launch()
        XCTAssertTrue(app.buttons["wizard.private"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["card.name"].exists)
    }
}
