# Android standard QA checklist

Run on a real Android phone with the minified release build installed
(`./gradlew assembleRelease`, or the APK from CI), so the check also covers
R8. Before you start, set up two test days with **New Day**:

* **QA short**: starts 2 minutes from now, with three segments of 1 min,
  1 min 30 s and 45 s. Give the second segment a custom end bell of 3 chimes.
* **QA later**: starts 10 minutes from now, with one 5-minute segment.

"Rings" means you hear every chime, 3 s apart, within about a second of the
time shown.

## Install and first launch

* **INS-1** A clean install opens on Days with "Opening Day" and "Full Day".
* **INS-2** Opening a day for the first time asks for notifications. Allow
  them, and fix anything the timing screen warns about. Then no warning shows
  on the active-day screen.
* **INS-3-android** An upgrade over the previous release keeps every day and
  setting.
* **INS-4-android** On Android 12 and later, the app's colors follow the
  wallpaper (dynamic color). Day theme colors don't change.

## Bells with screen on

* **BON-1** Start **QA short** before its start time. The status shows "Day
  queued", then the start bell rings on time.
* **BON-2** Each segment's end bell rings on time with its own chime count,
  and the current segment is highlighted.
* **BON-3** "Ring now" plays the manual bell at once and doesn't change the
  schedule.
* **BON-4** The live time remaining counts down every second.

## Bells with screen off or locked

* **BOFF-1** Start **QA later** and lock the phone. Every bell rings on time
  while the phone is locked.
* **BOFF-2** Leave the app with the home gesture, without ending the day. The
  bells still ring on time.
* **BOFF-3** After the bells, unlock. The notification list holds no bell
  notifications.
* **BOFF-4-android** Start **Full Day** (or any day with a bell an hour or
  more away), unplug the phone and leave it still and locked for over an hour,
  so it enters Doze. The next bell rings on time.
* **BOFF-5-android** While a day runs, the only notification is the ongoing
  timer notification, showing the day and the next bell.

## Silent, Do Not Disturb and Focus

* **SIL-1** With the ringer on silent, bells ring on time, both with the
  screen on and locked.
* **SIL-2** With Do Not Disturb, and with a Focus or mode that doesn't allow
  Impermanence, bells still ring.
* **SIL-3-android** With exact alarms turned off for Impermanence (Android 12
  only: Settings, Apps, Special app access, Alarms and reminders), the timing
  screen warns about it, and Allow opens that setting.
* **SIL-4** With the volume very low, the timing screen warns about it.
* **SIL-5-android** With headphones connected, the headphones icon shows on
  the timing screen. Tapping it says bells play in the headphones and on the
  phone speaker, and they do.

## Leaving ends timing

* **LEAVE-1** "End Day" closes the active day at once, with no confirmation.
  No bell rings after that, even with the phone locked past the next bell
  time.
* **LEAVE-2** Starting the same day again resumes it at the right segment and
  time remaining.
* **LEAVE-3-android** The back arrow or the back gesture on the active day
  ends it the same way as "End Day".
* **LEAVE-4-android** Swiping the app away from Recents during a day ends it.
  No bell rings after that.
* **LEAVE-5-android** Stop in the timer notification ends the day. No bell
  rings after that, and the notification goes away.

## Quick Sit

* **QS-1** Start a 1-minute sit. The start chimes ring, then a "Time remaining"
  countdown shows in the ring.
* **QS-2** The end bell rings at the end, and the Quick Sit screen closes on
  its own.
* **QS-3** Cancel during a sit ends it, and no end bell rings later.
* **QS-4** A running sit can't be dismissed by accident (swipe down on iOS,
  back on Android).

## Days and editor

* **EDIT-1** New Day: Add (iOS) or Save (Android) stays disabled until the day
  has a name and a segment.
* **EDIT-2** A segment's duration can be set to minutes and seconds, such as
  2 min 30 s. The row and the day's times show the seconds.
* **EDIT-3** Reorder, duplicate and delete segments. The last segment can't be
  deleted.
* **EDIT-4** Leaving the editor with changes asks "Discard changes?". "Keep
  Editing" keeps them, and Discard drops them.
* **EDIT-5** Deleting a day asks "Delete “name”?" first.
* **EDIT-6** The default bell also sets the start bell and the manual bell.
* **EDIT-7-android** On Days, each card's menu (⋮) moves the day up or down
  and deletes it. A long day name wraps, and the menu stays visible.

## Import and export

* **IO-1** Export Days saves `impermanence-days-YYYY-MM-DD.json`.
* **IO-2** Import that file. It asks "Replace all days?" first, then restores
  the same days.
* **IO-3** Importing a file that isn't a day plan fails with a clear message
  and changes nothing.
* **IO-4-android** A file exported from the iOS app imports with the same
  days, times and bells.

## Settings

* **SET-1** The 24-hour clock setting changes every time shown.
* **SET-2** With "Keep screen awake" on, the screen stays on during a queued or
  running day. With it off, the screen locks as usual.
* **SET-3** "Loop days at midnight" works: a day that ends after midnight
  starts again the next day only while the setting is on.

## Recovery after kill or reboot

* **REC-1** Restart the phone during a day. After unlocking, the next bells
  still ring. Opening the app returns to the day.
* **REC-2** After a crash or an update, the days are intact.
* **REC-3-android** Force stop the app (Settings, Apps, Impermanence) during a
  day, then open it. It returns to the day, and no bell rings that was due
  more than a minute ago.
* **REC-4-android** Change the time zone during a day. The bells follow the
  day's clock times in the new zone.

## Accessibility

* **A11Y-1** With VoiceOver or TalkBack on, every control on Days, the active
  day, Quick Sit and the editor has a clear label, and a new segment is
  announced.
* **A11Y-2** With the largest text size, nothing important is cut off.
* **A11Y-3** Dark mode is readable on every theme.

## Battery and power

* **PWR-1** With Low Power Mode or Battery Saver on, bells still ring on time
  with the phone locked.
* **PWR-2** A locked 1-hour session uses a reasonable amount of battery. Note
  the percentage.
* **PWR-3-android** With battery optimization on for Impermanence (the default
  after install), the timing screen warns about it. "Turn off for
  Impermanence" opens the system prompt, and the warning goes away after you
  allow it.
* **PWR-4-android** On a Samsung or Pixel, with the maker's battery saving left
  at its default, a locked 1-hour session rings every bell on time.
