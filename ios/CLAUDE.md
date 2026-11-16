# iOS app notes

The iOS app is the reference implementation. Read the root `CLAUDE.md` and
`docs/BEHAVIOR.md` first.

## Layout

* `Impermanence/Impermanence.xcodeproj` is a classic Xcode project, not
  folder-synchronized. A new source or resource file must be added to
  `project.pbxproj`: a file reference, a group entry, and a build-phase entry.
* `Impermanence/Impermanence/Models/BellSchedule.swift` holds all bell-timing
  rules. It is pure Foundation, with no UIKit, SwiftUI or AVFoundation.
* `Models/BellPlayer.swift` contains `BellEngine`, the only code that makes bell
  sound. It runs one session at a time (an active day or a Quick Sit), keeps the
  app alive in the background with a silent audio loop, rings each chime itself
  at its exact instant, and keeps time-sensitive notifications as a safety net.
  UI code never plays bells directly. Use `BellEngine.shared.startOrResumeDay`,
  `startQuickSit`, `endSession` and `ringNow`.
* `Models/DayTimer.swift` holds display state only.
* `ios/Package.swift` builds `Bell`, `BellCatalog`, `Day`, `Theme` and
  `BellSchedule` as a SwiftPM package, so the timing core and
  `ios/CoreTests/` run with `swift test` on Linux. Keep those files free of
  Apple-only frameworks, or guard them with `#if canImport(SwiftUI)`. The same
  test file also runs in the Xcode unit-test target.

## Rules

* Leaving the active-day screen (`DayActiveView.onDisappear`) or Quick Sit must
  end the bell session. Don't add a path that leaves bells scheduled after the
  user leaves.
* Every schedule computation goes through `DaySchedule`, in the current time
  zone, with wall-clock start times. Never use `startOfDay + seconds`: it is
  wrong on DST days.
* Bell sounds are `.caf`, under 30 s, so notifications can use them.
* UI tests launch with `--uitesting`. That shows a `bellEngineStatus` label
  ("day|quicksit|idle pending=N live=N", live being Lock Screen Live Activities) that tests read to check the engine's state.
* `Info.plist` holds only `UIBackgroundModes` (audio); other keys are generated
  from build settings. `Impermanence.entitlements` holds Time Sensitive
  Notifications. Critical Alerts would need Apple's approval first.

## Testing

* `cd ios && swift test` runs the core tests, including all shared fixtures,
  on Linux or macOS.
* `scripts/ios-test.sh` runs the unit and UI tests on a simulator (macOS only;
  CI runs it on every PR).
* See `docs/TESTING.md` for what CI covers and what still needs a device.
