import XCTest

final class VizitUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func capture(_ name: String, in app: XCUIApplication) {
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = name
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }

    private func reveal(_ element: XCUIElement, in app: XCUIApplication, maxSwipes: Int = 6) {
        if app.keyboards.firstMatch.exists {
            let done = app.buttons["profile.keyboardDone"]
            XCTAssertTrue(done.waitForExistence(timeout: 2))
            done.tap()
        }
        for _ in 0..<maxSwipes where !element.isHittable {
            let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.72))
            let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.32))
            start.press(forDuration: 0.05, thenDragTo: end)
        }
        XCTAssertTrue(element.waitForExistence(timeout: 2))
        XCTAssertTrue(element.isHittable)
    }

    private func revealHorizontally(_ element: XCUIElement, in app: XCUIApplication) {
        for _ in 0..<6 where !element.isHittable {
            let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.82, dy: 0.48))
            let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.18, dy: 0.48))
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

    private func launchReleaseAudit(_ extraArguments: [String] = []) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["--ui-testing", "--reset-test-profile", "--ui-testing-release-profile"] + extraArguments
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

        capture("VIZIT-auth-login", in: app)
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
        capture("VIZIT-auth-registration", in: app)
    }

    func testPasswordRecoveryIsVisibleAndKeepsTheCurrentBuildIdentifiable() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.buttons["auth.forgotPassword"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["auth.version"].exists)
        XCTAssertTrue(app.staticTexts["auth.version"].label.contains("VIZIT 8.1.0"))
        app.buttons["auth.forgotPassword"].tap()
        XCTAssertTrue(app.textFields["auth.reset.email"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["auth.reset.submit"].exists)
        capture("VIZIT-auth-password-recovery", in: app)
    }

    func testRegistrationConfirmationHasADedicatedDestination() {
        let app = XCUIApplication()
        app.launchArguments = ["--ui-testing", "--ui-testing-verification"]
        app.launch()

        XCTAssertTrue(app.staticTexts["Ellenőrizd az e-mail-fiókodat"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.staticTexts["auth.verification.email"].label, "teszt@vizit.hu")
        XCTAssertTrue(app.buttons["auth.verification.back"].exists)

        capture("VIZIT-email-verification", in: app)

        app.buttons["auth.verification.back"].tap()
        XCTAssertTrue(app.buttons["auth.submit"].waitForExistence(timeout: 5))
    }

    func testCreatePersistAndRequirePhotoBeforeSharing() {
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
        XCTAssertTrue(app.staticTexts["Profilkép szükséges"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.images["share.qr"].exists)
        capture("VIZIT-photo-required", in: app)
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

    func testBusinessPortalExposesAllFourFunctionalAreas() {
        let app = launchClean()
        let portal = app.buttons["home.businessPortal"]
        XCTAssertTrue(portal.waitForExistence(timeout: 10))
        portal.tap()

        for identifier in ["portal.vosz", "portal.education", "portal.help", "portal.toolkit"] {
            XCTAssertTrue(app.buttons[identifier].waitForExistence(timeout: 5), "Missing \(identifier)")
        }

        capture("VIZIT-business-portal", in: app)

        let destinations = [
            (identifier: "portal.vosz", title: "VOSZ forrásközpont"),
            (identifier: "portal.education", title: "Vállalkozói Edukáció"),
            (identifier: "portal.help", title: "Digitális segítség"),
            (identifier: "portal.toolkit", title: "Vállalkozói eszköztár"),
        ]
        for destination in destinations {
            let button = app.buttons[destination.identifier]
            revealHorizontally(button, in: app)
            button.tap()
            let navigationBar = app.navigationBars[destination.title]
            XCTAssertTrue(navigationBar.waitForExistence(timeout: 5), destination.title)
            navigationBar.buttons.firstMatch.tap()
            XCTAssertTrue(app.buttons["portal.vosz"].waitForExistence(timeout: 5))
        }
    }

    func testCardAppearanceExposesAndPersistsApprovedMaterialsAndLayouts() {
        let app = launchClean()
        app.tabBars.buttons["Névjegy"].tap()

        let appearance = app.buttons["card.appearance"]
        reveal(appearance, in: app)
        appearance.tap()

        for identifier in ["card.material.ink", "card.material.paper", "card.material.brand"] {
            XCTAssertTrue(app.buttons[identifier].waitForExistence(timeout: 5), "Missing \(identifier)")
        }
        for layout in ["Portré", "Minimál", "Klasszikus"] {
            XCTAssertTrue(app.buttons[layout].exists, "Missing \(layout)")
        }

        for material in [
            (identifier: "card.material.ink", attachment: "VIZIT-card-material-ink"),
            (identifier: "card.material.paper", attachment: "VIZIT-card-material-paper"),
            (identifier: "card.material.brand", attachment: "VIZIT-card-material-brand"),
        ] {
            let button = app.buttons[material.identifier]
            button.tap()
            XCTAssertTrue(button.isSelected)
            capture(material.attachment, in: app)
        }

        for layout in [
            (label: "Portré", attachment: "VIZIT-card-layout-portrait"),
            (label: "Minimál", attachment: "VIZIT-card-layout-minimal"),
            (label: "Klasszikus", attachment: "VIZIT-card-layout-classic"),
        ] {
            let button = app.buttons[layout.label]
            button.tap()
            XCTAssertTrue(button.isSelected)
            capture(layout.attachment, in: app)
        }

        app.buttons["card.material.paper"].tap()
        app.buttons["Klasszikus"].tap()

        for identifier in ["card.showPhoto", "card.showQR", "card.showSocial"] {
            let toggle = app.switches[identifier]
            reveal(toggle, in: app)
            let initial = toggle.value as? String
            toggle.tap()
            XCTAssertNotEqual(toggle.value as? String, initial, "Toggle did not change: \(identifier)")
            toggle.tap()
            XCTAssertEqual(toggle.value as? String, initial, "Toggle did not restore: \(identifier)")
        }

        app.buttons["Kész"].tap()
        reveal(appearance, in: app)
        appearance.tap()
        XCTAssertTrue(app.buttons["card.material.paper"].isSelected)
        XCTAssertTrue(app.buttons["Klasszikus"].isSelected)
    }

    func testReleaseAuditCoversPrimaryScreensAndPhotoQR() {
        let app = launchReleaseAudit()
        XCTAssertTrue(app.staticTexts["card.name"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.staticTexts["card.name"].label, "Csukárdi Rajmund")
        capture("VIZIT-home-complete-profile", in: app)

        app.tabBars.buttons["Névjegy"].tap()
        XCTAssertTrue(app.staticTexts["Névjegyem"].waitForExistence(timeout: 5))
        capture("VIZIT-card-complete-profile", in: app)

        app.tabBars.buttons["Megosztás"].tap()
        XCTAssertTrue(app.images["share.qr"].waitForExistence(timeout: 15))
        capture("VIZIT-share-photo-contact-qr", in: app)

        let fullScreen = app.buttons["share.fullscreen"]
        reveal(fullScreen, in: app)
        fullScreen.tap()
        XCTAssertTrue(app.buttons["Bezárás"].waitForExistence(timeout: 5))
        capture("VIZIT-share-fullscreen-qr", in: app)
        app.buttons["Bezárás"].tap()

        app.tabBars.buttons["Beállítások"].tap()
        XCTAssertTrue(app.staticTexts["Beállítások"].waitForExistence(timeout: 5))
        capture("VIZIT-settings-complete-profile", in: app)
    }

    func testReleaseAuditCoversProfileEditorAndVisibility() {
        let app = launchReleaseAudit()
        let edit = app.buttons["card.edit"]
        XCTAssertTrue(edit.waitForExistence(timeout: 10))
        edit.tap()
        XCTAssertTrue(app.navigationBars["Névjegy szerkesztése"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.textFields["profile.fullName"].exists)
        capture("VIZIT-profile-editor", in: app)
        app.buttons["Mégse"].tap()

        app.tabBars.buttons["Névjegy"].tap()
        let visibility = app.buttons["card.visibility"]
        reveal(visibility, in: app)
        visibility.tap()
        XCTAssertTrue(app.navigationBars["Adatláthatóság"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Teljes név"].exists)
        capture("VIZIT-data-visibility", in: app)
    }

    func testReleaseAuditCoversScannerEntry() {
        let app = launchReleaseAudit()
        let scanner = app.buttons["home.scan"]
        XCTAssertTrue(scanner.waitForExistence(timeout: 10))
        scanner.tap()
        XCTAssertTrue(app.navigationBars["Beolvasás"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["QR-kód beolvasása"].exists)
        capture("VIZIT-scanner-entry", in: app)
    }

    func testReleaseAuditCoversAllCustomDomainStates() {
        let states = [
            (arguments: [String](), label: "Tulajdonjog-ellenőrzés függőben", attachment: "VIZIT-domain-pending"),
            (arguments: ["--ui-testing-domain-verified"], label: "Ellenőrzött saját domain", attachment: "VIZIT-domain-verified"),
            (arguments: ["--ui-testing-domain-invalid"], label: "Formailag hibás domain", attachment: "VIZIT-domain-invalid"),
        ]

        for state in states {
            let app = launchReleaseAudit(state.arguments)
            let edit = app.buttons["card.edit"]
            XCTAssertTrue(edit.waitForExistence(timeout: 10))
            edit.tap()
            let domain = app.textFields["profile.customDomain"]
            reveal(domain, in: app, maxSwipes: 24)
            XCTAssertTrue(app.staticTexts[state.label].waitForExistence(timeout: 5), state.label)
            capture(state.attachment, in: app)
            app.terminate()
        }
    }
}
