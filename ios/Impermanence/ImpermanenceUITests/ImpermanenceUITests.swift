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
        allowNotificationsIfAsked()

        let manualBellButton = app.navigationBars.buttons["Manual bell controls"]
        XCTAssertTrue(manualBellButton.waitForExistence(timeout: 5))

        tapToolbarItem(manualBellButton, in: app)
        XCTAssertTrue(app.buttons["Ring now"].waitForExistence(timeout: 3))
    }

    func testStartDayFlowWithPersistedDaysStaysResponsive() throws {
        let app = XCUIApplication()
        app.launchArguments += ["--uitesting", "--uitesting-reset-session"]
        app.launch()
        openSeedDay(in: app)

        let startButton = app.buttons["Start or Resume Day"]
        XCTAssertTrue(startButton.waitForExistence(timeout: 5))
        startButton.tap()
        allowNotificationsIfAsked()

        let manualBellButton = app.navigationBars.buttons["Manual bell controls"]
        XCTAssertTrue(manualBellButton.waitForExistence(timeout: 8))

        Thread.sleep(forTimeInterval: 2)
        XCTAssertTrue(isOnScreen(manualBellButton, in: app), "frame \(manualBellButton.frame)")

        tapToolbarItem(manualBellButton, in: app)
        XCTAssertTrue(app.buttons["Ring now"].waitForExistence(timeout: 3))
    }

    func testNewDayDefaultsAndControlsAreVisible() throws {
        let app = launchApp()
        let newDayButton = app.buttons["New Day"]
        XCTAssertTrue(newDayButton.waitForExistence(timeout: 5))
        newDayButton.tap()

        XCTAssertTrue(app.navigationBars["New Day"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Default bell"].exists)
        XCTAssertTrue(app.staticTexts["Theme"].exists)
        // A new day starts at midnight. iOS writes "12:00 AM" with a narrow
        // no-break space, so match on the digits, in a label or a value.
        let midnight = NSPredicate(format: "label CONTAINS '12:00' OR label CONTAINS '00:00' OR value CONTAINS '12:00' OR value CONTAINS '00:00'")
        XCTAssertTrue(app.descendants(matching: .any).matching(midnight).firstMatch.waitForExistence(timeout: 3),
                      app.debugDescription)

        let cancelButton = app.buttons["Cancel"]
        XCTAssertTrue(cancelButton.exists)
        cancelButton.tap()
        XCTAssertFalse(app.navigationBars["New Day"].waitForExistence(timeout: 1))
    }

    func testQuickSitCanOnlyBeCancelledWhileRunning() throws {
        let app = launchApp()
        let quickSitButton = app.buttons["Quick Sit"]
        XCTAssertTrue(quickSitButton.waitForExistence(timeout: 5))
        quickSitButton.tap()

        XCTAssertTrue(app.navigationBars["Quick Sit"].waitForExistence(timeout: 5))

        let startButton = app.buttons["Start"]
        XCTAssertTrue(startButton.exists)
        startButton.tap()
        allowNotificationsIfAsked()

        // While it runs: a live countdown, and Cancel is the only way out.
        XCTAssertTrue(app.staticTexts["Time remaining"].waitForExistence(timeout: 3))
        XCTAssertFalse(app.buttons["Close"].exists)
        let cancelButton = app.buttons["Cancel"]
        XCTAssertTrue(cancelButton.exists)
        cancelButton.tap()

        let closeButton = app.buttons["Close"]
        XCTAssertTrue(closeButton.waitForExistence(timeout: 3))
        XCTAssertTrue(closeButton.isEnabled)
        closeButton.tap()
    }

    // MARK: Bell engine lifecycle

    func testActiveDaySchedulesBellsAndLeavingStopsThem() throws {
        let app = launchApp()
        openSeedDay(in: app)
        app.buttons["Start or Resume Day"].tap()
        allowNotificationsIfAsked()
        XCTAssertTrue(waitForStatus(app, prefix: "day pending=", timeout: 10) { $0 > 0 },
                      "safety-net notifications not scheduled: \(status(app))")

        app.buttons["End Day"].tap()
        XCTAssertTrue(waitForStatus(app, prefix: "idle pending=", timeout: 10) { $0 == 0 },
                      "bells still scheduled after leaving: \(status(app))")
    }

    func testQuickSitCancelStopsBells() throws {
        let app = launchApp()
        app.buttons["Quick Sit"].tap()
        app.buttons["Start"].tap()
        allowNotificationsIfAsked()
        XCTAssertTrue(waitForStatus(app, prefix: "quicksit pending=", timeout: 10) { $0 == 1 },
                      "end bell not scheduled: \(status(app))")
        app.buttons["Cancel"].tap()
        XCTAssertTrue(waitForStatus(app, prefix: "idle pending=", timeout: 10) { $0 == 0 },
                      "bells still scheduled after cancel: \(status(app))")
    }

    func testQuickSitResumesAfterTheSystemEndsTheApp() throws {
        var app = launchApp()
        app.buttons["Quick Sit"].tap()
        app.buttons["Start"].tap()
        allowNotificationsIfAsked()
        XCTAssertTrue(waitForStatus(app, prefix: "quicksit pending=", timeout: 10) { $0 == 1 })

        // terminate() kills the process without the normal termination
        // callback, like iOS ending a background app.
        app.terminate()
        app = launchApp(keepSession: true)
        XCTAssertTrue(app.buttons["Cancel"].waitForExistence(timeout: 10), "Quick Sit not resumed")
        XCTAssertTrue(waitForStatus(app, prefix: "quicksit pending=", timeout: 10) { $0 == 1 })
        app.buttons["Cancel"].tap()
        XCTAssertTrue(waitForStatus(app, prefix: "idle pending=", timeout: 10) { $0 == 0 })
    }

    func testActiveDayResumesAfterTheSystemEndsTheApp() throws {
        var app = launchApp()
        openSeedDay(in: app)
        app.buttons["Start or Resume Day"].tap()
        allowNotificationsIfAsked()
        XCTAssertTrue(waitForStatus(app, prefix: "day pending=", timeout: 10) { $0 > 0 })

        // Give the debounced save a moment, then relaunch from the saved days
        // (resetting would create new day IDs).
        Thread.sleep(forTimeInterval: 1)
        app.terminate()
        app = XCUIApplication()
        app.launchArguments += ["--uitesting"]
        app.launch()
        XCTAssertTrue(app.navigationBars.buttons["Manual bell controls"].waitForExistence(timeout: 10),
                      "active day not resumed")
        XCTAssertTrue(waitForStatus(app, prefix: "day pending=", timeout: 10) { $0 > 0 })
        app.buttons["End Day"].tap()
        XCTAssertTrue(waitForStatus(app, prefix: "idle pending=", timeout: 10) { $0 == 0 })
    }

    func testQuickSitCompletesAndEndsItself() throws {
        let app = launchApp()
        app.buttons["Quick Sit"].tap()
        let subtract = app.buttons["Subtract 5 minutes"]
        for _ in 0..<3 { subtract.tap() }
        XCTAssertTrue(app.staticTexts["1 minute"].exists)
        app.buttons["Start"].tap()
        allowNotificationsIfAsked()
        XCTAssertTrue(waitForStatus(app, prefix: "quicksit pending=", timeout: 10) { $0 == 1 })
        // The sheet closes at the end; the engine stops after the end chime.
        // ("Quick Sit" exists behind the sheet the whole time, so wait for
        // the sheet's Cancel button to go away instead.)
        let closed = expectation(for: NSPredicate(format: "exists == false"),
                                 evaluatedWith: app.buttons["Cancel"])
        wait(for: [closed], timeout: 80)
        XCTAssertTrue(waitForStatus(app, prefix: "idle pending=", timeout: 15) { $0 == 0 })
    }

    // MARK: Helpers

    /// Launches with the seed days. Unless `keepSession`, a bell session left
    /// by an earlier test (XCUITest kills the app between tests, which looks
    /// like iOS ending it, so the session would resume) is discarded.
    // MARK: Segment editing

    /// Opens the Opening Day editor (Sit, Walk, Dharma Talk, Walk, Sit).
    private func openSegmentEditor(in app: XCUIApplication) {
        openSeedDay(in: app)
        let edit = app.navigationBars.buttons["Edit"]
        XCTAssertTrue(edit.waitForExistence(timeout: 5))
        edit.tap()
        XCTAssertTrue(app.buttons.matching(identifier: "segmentRow").firstMatch.waitForExistence(timeout: 5))
    }

    /// Segment rows, top to bottom.
    private func segmentRows(_ app: XCUIApplication) -> [XCUIElement] {
        app.buttons.matching(identifier: "segmentRow").allElementsBoundByIndex
            .filter { $0.exists && !$0.frame.isEmpty }
            .sorted { $0.frame.minY < $1.frame.minY }
    }

    func testSegmentsReorderByDraggingWithoutEditMode() throws {
        let app = launchApp()
        openSegmentEditor(in: app)
        var rows = segmentRows(app)
        XCTAssertGreaterThanOrEqual(rows.count, 3)
        XCTAssertTrue(rows[0].label.hasPrefix("Sit"), rows[0].label)
        XCTAssertTrue(rows[2].label.hasPrefix("Dharma Talk"), rows[2].label)

        // Touch and hold Dharma Talk, then drag it above the first row.
        let from = rows[2].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        let to = rows[0].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.1))
        from.press(forDuration: 1.0, thenDragTo: to, withVelocity: .slow, thenHoldForDuration: 0.5)

        let deadline = Date().addingTimeInterval(5)
        repeat {
            rows = segmentRows(app)
            if rows.first?.label.hasPrefix("Dharma Talk") == true { break }
            Thread.sleep(forTimeInterval: 0.25)
        } while Date() < deadline
        XCTAssertTrue(rows.first?.label.hasPrefix("Dharma Talk") == true,
                      rows.map(\.label).joined(separator: " | "))
        XCTAssertEqual(rows.count, 5, "dragging moves a segment, it doesn't add or drop one")
    }

    func testSwipeToDuplicateASegmentPutsTheCopyRightAfterIt() throws {
        let app = launchApp()
        openSegmentEditor(in: app)
        let rows = segmentRows(app)
        XCTAssertEqual(rows.count, 5)
        rows[2].swipeRight()
        let duplicate = app.buttons["Duplicate"]
        XCTAssertTrue(duplicate.waitForExistence(timeout: 3))
        duplicate.tap()

        let after = segmentRows(app)
        XCTAssertEqual(after.count, 6)
        XCTAssertTrue(after[2].label.hasPrefix("Dharma Talk") && after[3].label.hasPrefix("Dharma Talk"),
                      after.map(\.label).joined(separator: " | "))
    }

    private func launchApp(keepSession: Bool = false) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["--uitesting-reset-days", "--uitesting-days-start-soon", "--uitesting"]
        if !keepSession { app.launchArguments.append("--uitesting-reset-session") }
        app.launch()
        return app
    }

    private func allowNotificationsIfAsked() {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let allow = springboard.buttons["Allow"]
        if allow.waitForExistence(timeout: 5) {
            allow.tap()
        }
    }

    /// Whether the element's frame lies inside the app window.
    private func isOnScreen(_ element: XCUIElement, in app: XCUIApplication) -> Bool {
        let frame = element.frame
        return !frame.isEmpty && app.windows.firstMatch.frame.contains(CGPoint(x: frame.midX, y: frame.midY))
    }

    /// Navigation-bar menus sometimes report not hittable, and XCUITest's
    /// scroll-to-visible fails on them, although they are on screen and work.
    /// Tap the element's center then.
    private func tapToolbarItem(_ element: XCUIElement, in app: XCUIApplication) {
        if element.isHittable {
            element.tap()
        } else {
            XCTAssertTrue(isOnScreen(element, in: app), "not on screen: \(element.frame)")
            element.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        }
    }

    private func status(_ app: XCUIApplication) -> String {
        // Both the days list and the Quick Sit sheet show it; they agree.
        let element = app.staticTexts.matching(identifier: "bellEngineStatus").firstMatch
        return element.exists ? element.label : "(no status)"
    }

    private func waitForStatus(_ app: XCUIApplication, prefix: String, timeout: TimeInterval,
                               _ check: (Int) -> Bool) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        repeat {
            let s = status(app)
            if s.hasPrefix(prefix), let n = Int(s.dropFirst(prefix.count)), check(n) {
                return true
            }
            Thread.sleep(forTimeInterval: 0.5)
        } while Date() < deadline
        return false
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
