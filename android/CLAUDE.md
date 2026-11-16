# Android app notes

Kotlin and Jetpack Compose app under `android/`. Read the root `CLAUDE.md` first.

## Parity bar

The Android app should feel like a well made Android app, and a person who knows one version should
never have to relearn the other. That means:

* **Idiomatic Android.** Use Material 3, Android navigation (system back, top app bar), Android
  notifications, permissions and settings flows. Don't copy iOS controls or layouts.
* **Same experience and expectations.** Same features, same names for things, same defaults, same
  data (portable day plans import on both), and the same outcome for the same action.
* **Bell timing is identical, not just similar.** When a bell rings, which bell, and when it does not
  ring must match iOS exactly. See below.

When you change a feature on one platform, check the other and either make the matching change or
say in the PR why it doesn't apply.

## Bell semantics (must match iOS)

The spec is `docs/BEHAVIOR.md` section 1. The shared reference `scripts/bell_fixtures.py`
generates `docs/fixtures/bell-*.json`, and both apps run every fixture in their unit tests
(`SharedBellFixturesTest` here). To change a rule, change the spec and the reference, regenerate
the fixtures, then update both apps. In short:

* A day's start is its wall-clock time on the date. A start inside a DST gap moves forward by the
  gap; an ambiguous start uses the earlier offset. Segments are real elapsed durations.
* A session rings each bell at most once. A bell rings if it is at most 60 s late; older missed
  bells are skipped and logged, never rung late. Opening a day rings a bell due at exactly that
  instant, nothing before it.
* Bells ring on the alarm stream, so they sound in silent mode and Do Not Disturb.
* Leaving the active day screen, or cancelling Quick Sit, ends timing: no more bells. Swiping the
  app away from recents, or Stop in the notification, counts as leaving.
* A day that crosses midnight belongs to the date it started. Looping moves to the next date once
  the day is over and the calendar date has changed.

## How bell timing works

`SessionEngine` (in `service/`) is the only code that rings scheduled bells. Everything that can
make a bell due calls `SessionEngine.process`: the exact alarm (`BellAlarmReceiver`), the
foreground service's ticker (`TimingService`), boot and clock changes (`SystemEventReceiver`), and
the UI. The pure decision logic is `SessionPlanner` plus `DaySchedule` (no Android imports), and
the session is persisted with `commit()` in device-protected storage so it survives process death
and direct boot. The alarm uses `setAlarmClock`, which is exact in Doze and can start the
foreground service from the background. Never ring a bell from the UI layer or from a coroutine
timer alone.

## Testing

* `./gradlew testDebugUnitTest` runs the pure logic tests, the shared fixtures and the Robolectric
  tests for the engine, alarms, receivers and service.
* `BellReliabilityDeviceTest` (instrumented) checks real alarm timing with the screen off and the
  device forced into Doze. It runs in CI on the emulator.
* If the cloud container can't download the Android SDK, the pure logic under `domain/` and `model/`
  can still be compiled and tested with a plain Kotlin JVM build; push a branch and let CI run the
  rest.
