# Testing strategy

The goal is to catch as much as possible before anyone picks up a phone. Each
layer below runs without a person; only the last needs a device.

**When each layer runs.** Every PR runs layers 1 to 4, which take minutes.
Layers 5 to 8 are heavier. Layer 5 runs with the unit tests once it exists.
Layers 6 to 8 run on demand from GitHub Actions ("Run workflow"): before a
release, after a major change to bells or timing, or when a PR asks for them.
Layer 9 uses the 1 or 2 phones per OS that Alex has, plus simulators and
emulators. There is no device farm.

## Layers

| Layer | Where it runs | What it proves | Status |
| --- | --- | --- | --- |
| 1. Shared bell fixtures | `scripts/bell_fixtures.py` (Python zoneinfo, independent of both apps) generates `docs/fixtures/bell-*.json` | The intended schedule for DST, time zones, midnight crossing, looping, no-backfill | Done, 19 cases |
| 2. Core unit tests on Linux | `cd ios && swift test` (Docker in cloud sessions; CI job "core") | The iOS timing core matches every fixture; tick-by-tick planner simulation plays every chime once, on time; every IANA zone's DST transitions 2026–27; catch-up and safety-net rules | Done |
| 3. Android JVM tests | `./gradlew testDebugUnitTest` (cloud and CI) | The Android core matches the same fixtures | Android thread |
| 4. iOS simulator unit + UI tests | CI `macos-15`, `scripts/ios-test.sh` | The app builds; bundled sounds and background mode exist; entering an active day schedules safety-net notifications and leaving removes them; Quick Sit cancel stops bells; a killed app resumes Quick Sit and an active day; a 1-minute sit completes and stops itself | Done |
| 5. Engine tests with a fake clock | Simulator unit tests | `BellEngine` decisions under time jumps, interruptions, time-zone changes and relaunch, deterministic and fast | Next: inject a clock and fake audio/notification back ends |
| 6. Simulator end-to-end bell checks | CI, on demand | With the app in the background (home button) or killed, the bell plays on time (engine log with timestamps) or the notification is delivered (Springboard banner query) | Next |
| 7. UI matrix and accessibility | CI, on demand | iOS 16/17/18; small iPhone, large iPhone, iPad; dark mode; largest Dynamic Type; `performAccessibilityAudit()`; screenshots uploaded as artifacts and reviewed | Next |
| 8. Soak | CI, on demand (pre-release) | A multi-hour backgrounded session in the simulator: no drift, no missed or extra chimes | Next |
| 9. Device check | Alex's 1–2 devices per OS, about 30 minutes, once layers 1–8 are green | The things simulators can't model (list below) | Later |

## What only a device can check

* The Ring/Silent switch and Focus/Do Not Disturb (simulators don't model them).
* Real background lifetime: screen locked for an hour, low-power mode, memory
  pressure from other apps.
* Audio routes: Bluetooth headphones connected or disconnected mid-session, and
  a phone call or Siri interrupting a session.
* Time Sensitive notification delivery with a Focus on.
* Notification sound volume and loudness compared with the in-app bell.

A short scripted device pass covers these once everything above is green.
Ideally that's one pass per release.
