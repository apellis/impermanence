# Impermanence

Meditation and retreat timer with two separate native apps in one repo:
`ios/Impermanence/` (Swift/SwiftUI, Xcode project) and `android/` (Kotlin/Compose, Gradle).
Full setup notes: `docs/DEVELOPMENT.md`. Product behavior: `docs/BEHAVIOR.md`.

## Two apps, one experience

iOS is the reference implementation and design. Android follows it.

* **Idiomatic on each OS.** Use each platform's own navigation, controls, system
  integrations and conventions: SwiftUI and HIG on iOS, Compose and Material on
  Android. Don't port an iOS pattern to Android, or the reverse, when the
  platform has its own way.
* **Same experience and expectations.** A user moving between the apps finds the
  same features, the same names and wording, the same defaults, and the same
  results. They should have nothing to relearn except the platform's own
  navigation basics. `docs/BEHAVIOR.md` lists what must match.
* **Bell timing is identical, not just equivalent.** When bells ring, which ones
  ring, DST and time-zone handling, session start and end, and catch-up rules all
  follow `docs/BEHAVIOR.md` section 1 exactly. Both apps' unit tests load the
  shared vectors in `docs/fixtures/bell-*.json` and must pass all of them.
  To change a rule, change it in `docs/BEHAVIOR.md` and `scripts/bell_fixtures.py`,
  regenerate the fixtures (`python3 scripts/bell_fixtures.py`), and update both apps.
* **Bells must never fail.** They must ring on time with the screen off, on silent,
  and in Focus or Do Not Disturb, and never after the user leaves the timing
  screen. Don't trade this for simplicity, battery or code size without asking Alex.

### Every change that affects users

1. Check `docs/BEHAVIOR.md`. If behavior changes, update it in the same PR.
2. Make the change on iOS first, unless it is an Android-only bug or platform
   integration.
3. For the other platform, include the matching change in the same PR, or say in
   the PR description what it needs and make sure someone tracks it. In a
   project thread, message the other platform's thread.
4. A deliberate platform difference (for example, a system-integration feature
   that exists on only one OS) is noted in `docs/BEHAVIOR.md`, with the reason.

## Commands

* Bell-timing core and fixtures, on Linux or macOS:
  `cd ios && swift test` (in a cloud container with no Swift:
  `docker run --rm -v "$PWD":/src -w /src/ios swift:6.0-noble swift test`,
  using `mirror.gcr.io/library/swift:6.0-noble` if Docker Hub rate-limits).
* Regenerate the shared fixtures: `python3 scripts/bell_fixtures.py`.
* Android SDK in a Linux cloud session: `scripts/setup-android-sdk.sh` (once per container).
* Android: `cd android && ./gradlew testDebugUnitTest lintDebug assembleDebug`.
* Android instrumented tests need an emulator or device: `./gradlew connectedDebugAndroidTest`.
* iOS app (macOS only): `scripts/ios-test.sh`.

## Notes for cloud sessions

* No Xcode and no KVM in the container: iOS builds and the Android emulator run only in
  GitHub Actions (`.github/workflows/`) or on the Mac. Push a branch, open a PR and read
  the CI results. Pure-Foundation iOS logic runs on Linux through `ios/Package.swift`.
* Physical-device installs happen on the Mac; see `docs/DEVELOPMENT.md`.
* Testing strategy and what still needs a device: `docs/TESTING.md`.

Per-app notes: `ios/CLAUDE.md`, `android/CLAUDE.md`.
