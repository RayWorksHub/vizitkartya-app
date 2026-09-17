import XCTest

final class VizitUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

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
        if !phone.isHittable { app.swipeUp() }
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
