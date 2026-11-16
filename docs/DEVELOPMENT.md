# Development setup

Impermanence has two native codebases in this repo:

| Path | Platform | Stack | Open with |
|------|----------|-------|-----------|
| `ios/Impermanence/` | iOS 16.4+ | Swift, SwiftUI | `Impermanence.xcodeproj` in Xcode |
| `android/` | Android 8.0+ (API 26) | Kotlin, Jetpack Compose, DataStore | the `android/` folder in Android Studio |

The two apps share no code; behaviour is kept in parity by hand (see the parity UI tests on each side).

## Where each kind of work runs

| Task | Cloud (Claude Code session, Linux) | GitHub Actions | Local Mac |
|------|------------------------------------|----------------|-----------|
| Edit Swift / Kotlin | Yes | | Yes |
| Android build, unit tests, lint | Yes (after `scripts/setup-android-sdk.sh`) | Yes, `ubuntu-latest` | Yes |
| Android emulator / instrumented tests | No (no KVM in the container) | Yes, emulator on `ubuntu-latest` | Yes |
| iOS build, unit + UI tests on Simulator | No (needs macOS) | Yes, `macos-15` | Yes |
| Install on a physical phone | No | No | Yes, see below |
| Release signing / store upload | No | Not set up yet | Yes |

In practice: write code in the cloud, push a branch, and let CI build both apps and
run every test suite on simulators/emulators. Use the Mac only when a real device is needed.

## Cloud environment (Claude Code on the web)

Use the **impermanence** cloud environment (personal). It allows `dl.google.com` on top
of the default package registries, which the Android SDK download needs.

In a session on that environment, install the SDK once per container:

```sh
scripts/setup-android-sdk.sh          # installs to $ANDROID_HOME or /opt/android-sdk
cd android && ./gradlew testDebugUnitTest lintDebug assembleDebug
```

Limits: there is no KVM in the container, so the Android emulator cannot run there, and
Xcode does not exist on Linux, so nothing iOS builds there. Both are covered by CI.

## Continuous integration

Workflows live in `.github/workflows/` and only run when their side of the repo changes.
The repo is public, so GitHub-hosted runners, including macOS, cost nothing.

* **Android** (`android.yml`)
  * `build`: unit tests, lint, debug APK (uploaded as the `impermanence-debug-apk` artifact).
  * `emulator`: `connectedDebugAndroidTest` on an API 34 x86_64 emulator with a cached AVD.
  * `cloud-sdk-script`: runs `scripts/setup-android-sdk.sh` on a bare Linux image to keep the
    cloud setup path working.
* **iOS** (`ios.yml`): `scripts/ios-test.sh` builds and runs the unit and UI tests on the newest
  available iPhone simulator, with code signing off. The `.xcresult` bundle is uploaded.

Run either by hand from the Actions tab (`workflow_dispatch`).

## Local work: flashing physical test devices

### Android phone

1. On the phone: Settings, About phone, tap *Build number* 7 times; then Developer options,
   enable *USB debugging* (or *Wireless debugging*).
2. Plug in and accept the RSA prompt. Check with `adb devices`.
3. Install and launch:

   ```sh
   cd android
   ./gradlew installDebug
   adb shell am start -n com.neversink.impermanence/.MainActivity
   ```

   Or grab the `impermanence-debug-apk` artifact from a CI run and `adb install -r app-debug.apk`.
4. Instrumented tests on the phone: `./gradlew connectedDebugAndroidTest`.

Debug builds use the auto-generated debug keystore, so no signing setup is needed.

### iPhone / iPad

1. Xcode, Settings, Accounts: sign in with the Apple ID on team `DBA7U5Z2F6` (the project
   uses automatic signing with that team).
2. Plug in the device, trust the computer, and on iOS 16+ enable Settings, Privacy &
   Security, *Developer Mode*.
3. Pick the device in Xcode's run destination and press Run. From the command line:

   ```sh
   cd ios/Impermanence
   xcrun devicectl list devices
   xcodebuild -project Impermanence.xcodeproj -scheme Impermanence \
     -destination 'platform=iOS,id=<device-udid>' -allowProvisioningUpdates build
   xcrun devicectl device install app --device <device-udid> \
     ~/Library/Developer/Xcode/DerivedData/Impermanence-*/Build/Products/Debug-iphoneos/Impermanence.app
   ```

Run the Simulator tests locally the same way CI does: `scripts/ios-test.sh`.

### Having Claude drive the Mac

From a Claude project thread, Claude can start a Remote Control session on the Mac in the
`~/impermanence` checkout and run the commands above (build, `installDebug`, `xcodebuild`,
`devicectl`) with your local tools and signing identity. The phone still needs to be
plugged in (or paired for wireless debugging) and unlocked.

## Not set up yet (needs a decision)

* Android release signing and Google Play upload (upload keystore + Play Console service account as GitHub secrets).
* iOS archive and TestFlight upload from CI (App Store Connect API key + distribution certificate as GitHub secrets).
