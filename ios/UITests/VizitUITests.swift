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
        XCTAssertTrue(element.waitForExistence(timeout: 2))
        let viewport = app.frame
        for _ in 0..<6 {
            let frame = element.frame
            let hasUsableFrame = !frame.isNull
                && !frame.isInfinite
                && frame.minX.isFinite
                && frame.midX.isFinite
                && frame.midY.isFinite
                && frame.width > 1
                && frame.height > 1
            if hasUsableFrame,
               viewport.contains(CGPoint(x: frame.midX, y: frame.midY)) {
                break
            }
            let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.82, dy: 0.48))
            let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.18, dy: 0.48))
            start.press(forDuration: 0.05, thenDragTo: end)
        }
        let frame = element.frame
        XCTAssertTrue(
            !frame.isNull
                && !frame.isInfinite
                && frame.midX.isFinite
                && frame.midY.isFinite
                && viewport.contains(CGPoint(x: frame.midX, y: frame.midY))
        )
        XCTAssertTrue(element.isHittable)
    }

    private func assertSelected(
        _ element: XCUIElement,
        timeout: TimeInterval = 2,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let selected = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "selected == true"),
            object: element
        )
        XCTAssertEqual(
            XCTWaiter.wait(for: [selected], timeout: timeout),
            .completed,
            "Selection did not reach the accessibility tree",
            file: file,
            line: line
        )
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

    private func launchAuthentication(_ extraArguments: [String] = []) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["--ui-testing", "--ui-testing-signed-out"] + extraArguments
        app.launch()
        return app
    }

    private func assertColorScheme(_ expected: String, in app: XCUIApplication) {
        let probe = app.staticTexts["app.colorScheme"]
        XCTAssertTrue(probe.waitForExistence(timeout: 5))
        XCTAssertEqual(probe.label, expected)
    }

    func testConfiguredAppShowsAuthentication() {
        let app = launchAuthentication()
        XCTAssertTrue(app.buttons["auth.submit"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["Üdv újra!"].exists)
        XCTAssertTrue(app.textFields["auth.email"].exists)
        XCTAssertTrue(app.secureTextFields["auth.password"].exists)

        capture("VIZIT-auth-login", in: app)
    }

    func testRegistrationFlowShowsRequiredFieldsAndLegalConsent() {
        let app = launchAuthentication()
        XCTAssertTrue(app.buttons["Regisztráció"].waitForExistence(timeout: 10))
        app.buttons["Regisztráció"].tap()
        XCTAssertTrue(app.textFields["auth.name"].exists)
        XCTAssertTrue(app.textFields["auth.email"].exists)
        XCTAssertTrue(app.secureTextFields["auth.password"].exists)
        XCTAssertFalse(app.secureTextFields["auth.confirmation"].exists)
        XCTAssertTrue(app.buttons["Jelszó megjelenítése"].exists)
        XCTAssertTrue(app.staticTexts["Legalább 8 karakter, benne kis- és nagybetű, valamint szám."].exists)
        XCTAssertTrue(app.buttons["auth.submit"].exists)
        capture("VIZIT-auth-registration", in: app)
    }

    func testPasswordRecoveryIsVisibleAndKeepsTheCurrentBuildIdentifiable() {
        let app = launchAuthentication()
        XCTAssertTrue(app.buttons["auth.forgotPassword"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["auth.version"].exists)
        XCTAssertTrue(app.staticTexts["auth.version"].label.contains("VIZIT 8.7.0"))
        app.buttons["auth.forgotPassword"].tap()
        XCTAssertTrue(app.textFields["auth.reset.email"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["auth.reset.submit"].exists)
        capture("VIZIT-auth-password-recovery", in: app)
    }

    func testRegistrationConfirmationHasADedicatedDestination() {
        let app = XCUIApplication()
        app.launchArguments = ["--ui-testing", "--ui-testing-verification"]
        app.launch()

        XCTAssertTrue(app.staticTexts["Ellenőrizd a postaládádat"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.staticTexts["auth.verification.email"].label, "teszt@vizit.hu")
        XCTAssertTrue(app.buttons["auth.verification.back"].exists)
        XCTAssertTrue(app.buttons["auth.verification.resend"].exists)

        capture("VIZIT-email-verification", in: app)

        app.buttons["auth.verification.back"].tap()
        XCTAssertTrue(app.buttons["auth.submit"].waitForExistence(timeout: 5))
    }

    func testContactQRWorksWithoutPhotoAndPhotoModeExplainsWhatIsMissing() {
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
        XCTAssertTrue(app.images["share.qr"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["Olvastasd be a másik telefonnal"].exists)
        capture("VIZIT-share-contact-without-photo", in: app)

        let modes = app.segmentedControls["share.qrMode"]
        XCTAssertTrue(modes.waitForExistence(timeout: 5))
        modes.buttons["Fényképes"].tap()
        XCTAssertTrue(app.staticTexts["Nincs még profilképed"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Profilkép hozzáadása"].exists)
        capture("VIZIT-share-photo-missing", in: app)
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
            (identifier: "portal.vosz", title: "VOSZ forrásközpont", attachment: "VIZIT-portal-vosz"),
            (identifier: "portal.education", title: "Vállalkozói Edukáció", attachment: "VIZIT-portal-education"),
            (identifier: "portal.help", title: "Digitális segítség", attachment: "VIZIT-portal-help"),
            (identifier: "portal.toolkit", title: "Vállalkozói eszköztár", attachment: "VIZIT-portal-toolkit"),
        ]
        for destination in destinations {
            let button = app.buttons[destination.identifier]
            revealHorizontally(button, in: app)
            button.tap()
            let navigationBar = app.navigationBars[destination.title]
            XCTAssertTrue(navigationBar.waitForExistence(timeout: 5), destination.title)
            capture(destination.attachment, in: app)
            navigationBar.buttons.firstMatch.tap()
            XCTAssertTrue(app.buttons["portal.vosz"].waitForExistence(timeout: 5))
        }
    }

    func testBusinessEducationCoursePlayerIsImplemented() {
        let app = launchReleaseAudit()
        XCTAssertTrue(app.buttons["home.businessPortal"].waitForExistence(timeout: 10))
        app.buttons["home.businessPortal"].tap()
        let education = app.buttons["portal.education"]
        revealHorizontally(education, in: app)
        education.tap()
        XCTAssertTrue(app.navigationBars["Vállalkozói Edukáció"].waitForExistence(timeout: 5))

        let course = app.buttons["portal.course.ai"]
        XCTAssertTrue(course.waitForExistence(timeout: 5))
        course.tap()
        XCTAssertTrue(app.staticTexts["AI a mindennapi vállalkozásban"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["Vissza a kurzusokhoz"].exists)
        capture("VIZIT-portal-course-player", in: app)
    }

    func testCardAppearanceExposesAndPersistsApprovedMaterialsAndLayouts() {
        let app = launchClean()
        app.tabBars.buttons["Beállítások"].tap()

        let appearance = app.buttons["card.appearance"]
        reveal(appearance, in: app)
        appearance.tap()

        for identifier in ["card.material.ink", "card.material.paper", "card.material.brand"] {
            XCTAssertTrue(app.buttons[identifier].waitForExistence(timeout: 5), "Missing \(identifier)")
        }
        let layoutControl = app.segmentedControls["card.layout"]
        XCTAssertTrue(layoutControl.waitForExistence(timeout: 5))
        for label in ["Portré", "Minimál", "Klasszikus"] {
            let button = layoutControl.buttons[label]
            XCTAssertTrue(button.exists, "Missing layout: \(label)")
            XCTAssertTrue(button.isHittable, "Not hittable: \(label)")
        }

        for material in [
            (identifier: "card.material.ink", attachment: "VIZIT-card-material-ink"),
            (identifier: "card.material.paper", attachment: "VIZIT-card-material-paper"),
            (identifier: "card.material.brand", attachment: "VIZIT-card-material-brand"),
        ] {
            let button = app.buttons[material.identifier]
            button.tap()
            assertSelected(button)
            capture(material.attachment, in: app)
        }

        for layout in [
            (label: "Portré", attachment: "VIZIT-card-layout-portrait"),
            (label: "Minimál", attachment: "VIZIT-card-layout-minimal"),
            (label: "Klasszikus", attachment: "VIZIT-card-layout-classic"),
        ] {
            let button = layoutControl.buttons[layout.label]
            button.tap()
            assertSelected(button)
            capture(layout.attachment, in: app)
        }

        app.buttons["card.material.paper"].tap()
        layoutControl.buttons["Klasszikus"].tap()
        XCTAssertFalse(app.switches["card.showPhoto"].exists)
        XCTAssertFalse(app.switches["card.showQR"].exists)
        XCTAssertFalse(app.switches["card.showSocial"].exists)

        app.buttons["card.appearance.save"].tap()
        reveal(appearance, in: app)
        appearance.tap()
        let reopenedLayoutControl = app.segmentedControls["card.layout"]
        XCTAssertTrue(reopenedLayoutControl.waitForExistence(timeout: 5))
        assertSelected(app.buttons["card.material.paper"])
        assertSelected(reopenedLayoutControl.buttons["Klasszikus"])
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
        let modes = app.segmentedControls["share.qrMode"]
        XCTAssertTrue(modes.waitForExistence(timeout: 5))
        for label in ["Kontakt", "Fényképes", "Profil"] {
            XCTAssertTrue(modes.buttons[label].exists, "Missing QR mode: \(label)")
        }
        capture("VIZIT-share-contact-qr", in: app)

        modes.buttons["Fényképes"].tap()
        XCTAssertTrue(app.images["share.qr"].waitForExistence(timeout: 15))
        capture("VIZIT-share-photo-qr", in: app)

        modes.buttons["Profil"].tap()
        XCTAssertTrue(app.images["share.qr"].waitForExistence(timeout: 15))
        capture("VIZIT-share-profile-qr", in: app)

        let fullScreen = app.buttons["share.fullscreen"]
        reveal(fullScreen, in: app)
        fullScreen.tap()
        XCTAssertTrue(app.buttons["Bezárás"].waitForExistence(timeout: 5))
        capture("VIZIT-share-fullscreen-qr", in: app)
        app.buttons["Bezárás"].tap()

        app.tabBars.buttons["Beállítások"].tap()
        XCTAssertTrue(app.staticTexts["Beállítások"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["Szinkronizálás"].exists)
        XCTAssertFalse(app.staticTexts["Adatvédelem"].exists)
        capture("VIZIT-settings-complete-profile", in: app)
    }

    func testQRModesExposeTheirRequiredFailureStates() {
        let photoApp = launchReleaseAudit(["--ui-testing-photo-empty"])
        photoApp.tabBars.buttons["Megosztás"].tap()
        let photoModes = photoApp.segmentedControls["share.qrMode"]
        XCTAssertTrue(photoModes.waitForExistence(timeout: 5))
        photoModes.buttons["Fényképes"].tap()
        XCTAssertTrue(photoApp.staticTexts["Nincs még profilképed"].waitForExistence(timeout: 5))
        XCTAssertTrue(photoApp.buttons["Profilkép hozzáadása"].exists)
        capture("VIZIT-share-photo-unavailable", in: photoApp)
        photoApp.terminate()

        let profileApp = launchReleaseAudit(["--ui-testing-profile-qr-unavailable"])
        profileApp.tabBars.buttons["Megosztás"].tap()
        let profileModes = profileApp.segmentedControls["share.qrMode"]
        XCTAssertTrue(profileModes.waitForExistence(timeout: 5))
        profileModes.buttons["Profil"].tap()
        XCTAssertTrue(profileApp.staticTexts["Nincs még publikus profil"].waitForExistence(timeout: 5))
        XCTAssertTrue(profileApp.buttons["Kontakt QR megnyitása"].exists)
        capture("VIZIT-share-profile-unavailable", in: profileApp)
    }

    func testDarkAppearanceCoversEveryPrimaryDestination() {
        let app = launchReleaseAudit(["--ui-testing-theme-dark"])
        XCTAssertTrue(app.staticTexts["card.name"].waitForExistence(timeout: 10))
        assertColorScheme("DARK", in: app)
        capture("VIZIT-home-dark", in: app)

        app.tabBars.buttons["Névjegy"].tap()
        XCTAssertTrue(app.staticTexts["Névjegyem"].waitForExistence(timeout: 5))
        capture("VIZIT-card-dark", in: app)

        app.tabBars.buttons["Megosztás"].tap()
        XCTAssertTrue(app.images["share.qr"].waitForExistence(timeout: 10))
        capture("VIZIT-share-dark", in: app)

        app.tabBars.buttons["Beállítások"].tap()
        XCTAssertTrue(app.staticTexts["Beállítások"].waitForExistence(timeout: 5))
        capture("VIZIT-settings-dark", in: app)
    }

    func testAuthenticationDarkAppearance() {
        let app = launchAuthentication(["--ui-testing-theme-dark"])
        XCTAssertTrue(app.buttons["auth.submit"].waitForExistence(timeout: 10))
        assertColorScheme("DARK", in: app)
        capture("VIZIT-auth-login-dark", in: app)
    }

    func testSettingsThemeAndShareControlsAreActionable() {
        let app = launchReleaseAudit()
        app.tabBars.buttons["Beállítások"].tap()
        let theme = app.buttons["settings.theme.open"]
        XCTAssertTrue(theme.waitForExistence(timeout: 5))
        theme.tap()
        let control = app.segmentedControls["settings.theme"]
        XCTAssertTrue(control.waitForExistence(timeout: 5))
        for label in ["Világos", "Sötét", "Rendszer"] {
            XCTAssertTrue(control.buttons[label].exists)
        }
        control.buttons["Sötét"].tap()
        assertSelected(control.buttons["Sötét"])
        assertColorScheme("DARK", in: app)
        app.buttons["Kész"].tap()

        app.tabBars.buttons["Megosztás"].tap()
        XCTAssertTrue(app.buttons["NFC"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Link"].exists)
        XCTAssertTrue(app.buttons["Mentés"].exists)
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

        app.tabBars.buttons["Beállítások"].tap()
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
        XCTAssertTrue(app.staticTexts["Irányítsd a kamerát a másik kódra"].exists)
        XCTAssertTrue(app.buttons["scanner.photo"].exists)
        XCTAssertTrue(app.buttons["Bezárás"].exists)
        XCTAssertTrue(app.buttons["Vaku bekapcsolása"].exists)
        capture("VIZIT-scanner-entry", in: app)
    }

    func testReleaseAuditCoversAllCustomDomainStates() {
        let states = [
            (arguments: ["--ui-testing-domain-empty"], label: "Nincs megadva", attachment: "VIZIT-domain-default"),
            (arguments: [String](), label: "Megadva, ellenőrzésre vár", attachment: "VIZIT-domain-pending"),
            (arguments: ["--ui-testing-domain-verified"], label: "Ellenőrzött", attachment: "VIZIT-domain-verified"),
            (arguments: ["--ui-testing-domain-invalid"], label: "Formailag hibás", attachment: "VIZIT-domain-invalid"),
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
