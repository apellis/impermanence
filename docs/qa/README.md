# Release QA

Every release gets a hands-on QA pass on a real device for each platform,
after CI and the on-demand suites in `docs/TESTING.md` are green. Alex has 1
or 2 phones per OS and no device farm, so the lists aim for about 45 minutes
per platform, most of it waiting with the screen locked.

## Files

* `ios.md` and `android.md` are the standard checklists. They have the same
  sections in the same order, and an item that checks the same thing on both
  platforms has the same ID and wording. A platform-only item's ID ends in
  `-ios` or `-android`.
* Section prefixes: INS install and first launch, BON bells with the screen
  on, BOFF bells with the screen off or locked, SIL silent, Do Not Disturb and
  Focus, LEAVE leaving ends timing, QS Quick Sit, EDIT days and editor, IO
  import and export, SET settings, REC recovery after kill or reboot, A11Y
  accessibility, PWR battery and power.
* When a behavior changes, update the checklist in the same PR as
  `docs/BEHAVIOR.md`.

## Each QA pass

Claude writes one page (an Artifact) per platform per release, with three
parts:

1. **What's new**: a short overview of the release, in user terms, with links
   to the PRs.
2. **Special checks**: what this release could have broken, or what CI can't
   prove about its changes. Each check gives its steps and the expected
   result.
3. **Standard checklist**: every item from `ios.md` or `android.md`, with
   pass, fail or skip and a note.

The page header names the build being tested (version, commit, device and OS
version). Results are saved on the page itself, so the thread can read them
back. Any failure becomes a fix, or a known issue with Alex's say-so, before
the release ships. If a special check turns out to be worth repeating,
move it into the standard list.

## Preparing a build

* iOS: build and install from Alex's Mac with Xcode, or with
  `docs/DEVELOPMENT.md`. A Debug build shows the QA-only settings listed in
  `ios.md`.
* Android: `./gradlew installDebug` with the phone on USB, or the APK from the
  PR's CI artifacts.
