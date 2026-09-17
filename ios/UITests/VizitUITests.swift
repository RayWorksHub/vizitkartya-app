import XCTest

final class VizitUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func reveal(_ element: XCUIElement, in app: XCUIApplication) {
        if app.keyboards.firstMatch.exists {
            app.swipeDown(velocity: .slow)
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

    func testCreatePersistAndShowQR() {
        let app = launchClean()
        let edit = app.buttons["card.edit"]
        XCTAssertTrue(edit.waitForExistence(timeout: 10))
        edit.tap()
        let name = app.textFields["profile.fullName"]
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        name.tap(); name.typeText("Teszt Elek")
        let phone = app.textFields["profile.phone"]
        reveal(phone, in: app)
        phone.tap(); phone.typeText("06201234567")
        app.buttons["profile.save"].tap()
        XCTAssertTrue(app.staticTexts["card.name"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.staticTexts["card.name"].label, "Teszt Elek")
        app.terminate()
        app.launchArguments = ["--ui-testing"]
        app.launch()
        XCTAssertTrue(app.staticTexts["card.name"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.staticTexts["card.name"].label, "Teszt Elek")
        app.tabBars.buttons["Megosztás"].tap()
        XCTAssertTrue(app.images["share.qr"].waitForExistence(timeout: 5))
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "VIZIT-contact-QR"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }

    func testEmptyProfileShowsValidation() {
        let app = launchClean()
        XCTAssertTrue(app.buttons["card.edit"].waitForExistence(timeout: 10))
        app.buttons["card.edit"].tap()
        app.buttons["profile.save"].tap()
        XCTAssertTrue(app.alerts["A névjegy nem menthető"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.alerts.staticTexts["Add meg a nevedet."].exists)
    }

    func testCancelDoesNotSaveDraft() {
        let app = launchClean()
        XCTAssertTrue(app.buttons["card.edit"].waitForExistence(timeout: 10))
        app.buttons["card.edit"].tap()
        let name = app.textFields["profile.fullName"]
        name.tap(); name.typeText("Nem mentett adat")
        app.buttons["Mégse"].tap()
        XCTAssertTrue(app.buttons["card.edit"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["card.name"].exists)
    }
}
