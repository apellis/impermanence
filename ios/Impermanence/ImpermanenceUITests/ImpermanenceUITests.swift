//
//  ImpermanenceUITests.swift
//  ImpermanenceUITests
//
//  Created by Alex Ellis on 8/5/23.
//

import XCTest

final class ImpermanenceUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testStartDayFlowShowsActiveScreenAndManualBellMenu() throws {
        let app = launchApp()
        openSeedDay(in: app)

        let startButton = app.buttons["Start or Resume Day"]
        XCTAssertTrue(startButton.waitForExistence(timeout: 5))
        startButton.tap()

        let manualBellButton = app.navigationBars.buttons["Manual bell controls"]
        XCTAssertTrue(manualBellButton.waitForExistence(timeout: 5))

        manualBellButton.tap()
        XCTAssertTrue(app.buttons["Ring now"].waitForExistence(timeout: 2))
    }

    func testStartDayFlowWithPersistedDaysStaysResponsive() throws {
        let app = XCUIApplication()
        app.launch()
        openSeedDay(in: app)

        let startButton = app.buttons["Start or Resume Day"]
        XCTAssertTrue(startButton.waitForExistence(timeout: 5))
        startButton.tap()

        let manualBellButton = app.navigationBars.buttons["Manual bell controls"]
        XCTAssertTrue(manualBellButton.waitForExistence(timeout: 8))

        Thread.sleep(forTimeInterval: 2)
        XCTAssertTrue(manualBellButton.isHittable)

        manualBellButton.tap()
        XCTAssertTrue(app.buttons["Ring now"].waitForExistence(timeout: 2))
    }

    func testNewDayDefaultsAndControlsAreVisible() throws {
        let app = launchApp()
        let newDayButton = app.buttons["New Day"]
        XCTAssertTrue(newDayButton.waitForExistence(timeout: 5))
        newDayButton.tap()

        XCTAssertTrue(app.navigationBars["New Day"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Default bell"].exists)
        XCTAssertTrue(app.staticTexts["Theme"].exists)
        XCTAssertTrue(hasNodeWithText("12:00 AM", app: app) || hasNodeWithText("00:00", app: app))

        let dismissButton = app.buttons["Dismiss"]
        XCTAssertTrue(dismissButton.exists)
        dismissButton.tap()
    }

    func testQuickSitDisablesCloseWhileRunning() throws {
        let app = launchApp()
        let quickSitButton = app.buttons["Quick Sit"]
        XCTAssertTrue(quickSitButton.waitForExistence(timeout: 5))
        quickSitButton.tap()

        XCTAssertTrue(app.navigationBars["Quick Sit"].waitForExistence(timeout: 5))

        let startButton = app.buttons["Start"]
        XCTAssertTrue(startButton.exists)
        startButton.tap()

        let closeButton = app.buttons["Close"]
        XCTAssertTrue(closeButton.exists)
        XCTAssertFalse(closeButton.isEnabled)

        let cancelButton = app.buttons["Cancel"]
        XCTAssertTrue(cancelButton.exists)
        cancelButton.tap()

        XCTAssertTrue(closeButton.isEnabled)
        closeButton.tap()
    }

    private func launchApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["--uitesting-reset-days"]
        app.launch()
        return app
    }

    private func openSeedDay(in app: XCUIApplication) {
        if app.staticTexts["Opening Day"].waitForExistence(timeout: 5) {
            app.staticTexts["Opening Day"].tap()
            return
        }
        if app.staticTexts["Full Day"].waitForExistence(timeout: 2) {
            app.staticTexts["Full Day"].tap()
            return
        }
        XCTFail("No seeded day found.")
    }

    private func hasNodeWithText(_ text: String, app: XCUIApplication) -> Bool {
        app.staticTexts[text].exists || app.buttons[text].exists
    }
}
