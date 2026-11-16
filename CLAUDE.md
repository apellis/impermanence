# Impermanence

Meditation and retreat timer with two separate native apps in one repo:
`ios/Impermanence/` (Swift/SwiftUI, Xcode project) and `android/` (Kotlin/Compose, Gradle).
Each app should be idiomatic on its platform while giving the same experience and expectations;
bell timing semantics are identical on both (see `android/CLAUDE.md` and `docs/fixtures/`).
Full setup notes: `docs/DEVELOPMENT.md`.

## Commands

* Android SDK in a Linux cloud session: `scripts/setup-android-sdk.sh` (once per container).
* Android: `cd android && ./gradlew testDebugUnitTest lintDebug assembleDebug`.
* Android instrumented tests need an emulator or device: `./gradlew connectedDebugAndroidTest`.
* iOS (macOS only): `scripts/ios-test.sh`.

## Notes for cloud sessions

* No Xcode and no KVM in the container: iOS builds and the Android emulator run only in
  GitHub Actions (`.github/workflows/`) or on the Mac. Push a branch and read the CI results.
* Physical-device installs happen on the Mac; see `docs/DEVELOPMENT.md`.
