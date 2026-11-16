# iOS standard QA checklist

Run on a real iPhone with the release build installed. Before you start, set up
two test days with **New Day**:

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
* **INS-3-ios** An upgrade over the previous release keeps every day and
  setting.

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

## Silent, Do Not Disturb and Focus

* **SIL-1** With the ringer on silent, bells ring on time, both with the
  screen on and locked.
* **SIL-2** With Do Not Disturb, and with a Focus or mode that doesn't allow
  Impermanence, bells still ring.
* **SIL-3-ios** With notifications turned off for Impermanence in Settings,
  the active-day screen warns about it, and bells still ring while the app is
  running.
* **SIL-4** With the volume very low, the timing screen warns about it.

## Leaving ends timing

* **LEAVE-1** "End Day" closes the active day at once, with no confirmation.
  No bell rings after that, even with the phone locked past the next bell
  time.
* **LEAVE-2** Starting the same day again resumes it at the right segment and
  time remaining.
* **LEAVE-3-ios** Swiping the app away in the app switcher during a day, then
  reopening it, returns to the active day with bells still on time. A bell due
  while the app was closed rings from its backup notification.

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
* **EDIT-7-ios** Swipe right on a segment to duplicate it, swipe left
  to delete it, and use Edit to reorder.
* **EDIT-8** Touch and hold a segment row, then drag it to a new place. The
  other rows move aside, the list scrolls when you reach its edge, and after
  the drop every segment's times are correct.
* **EDIT-9** With VoiceOver or TalkBack on, the "Move up" and "Move down"
  actions on a segment row move it one place and announce its new position.

## Import and export

* **IO-1** Export Days saves `impermanence-days-YYYY-MM-DD.json`.
* **IO-2** Import that file. It asks "Replace all days?" first, then restores
  the same days.
* **IO-3** Importing a file that isn't a day plan fails with a clear message
  and changes nothing.

## Settings

* **SET-1** The 24-hour clock setting changes every time shown.
* **SET-2** With "Keep screen awake" on, the screen stays on during a queued or
  running day. With it off, the screen locks as usual.
* **SET-3** "Loop days at midnight" works: a day that ends after midnight
  starts again the next day only while the setting is on.
* **SET-4-ios** About (in Settings) shows the version and the privacy policy
  link.

## Recovery after kill or reboot

* **REC-1** Restart the phone during a day. After unlocking, the next bells
  still ring. Opening the app returns to the day.
* **REC-2** After a crash or an update, the days are intact.
* **REC-3-ios** After the restart in REC-1, and before you open the app, the
  bells ring from their backup notifications.

## Accessibility

* **A11Y-1** With VoiceOver or TalkBack on, every control on Days, the active day, Quick
  Sit and the editor has a clear label, and a new segment is announced.
* **A11Y-2** With the largest text size, nothing important is cut off.
* **A11Y-3** Dark mode is readable on every theme.

## Battery and power

* **PWR-1** With Low Power Mode or Battery Saver on, bells still ring on time
  with the phone locked.
* **PWR-2** A locked 1-hour session uses a reasonable amount of battery. Note
  the percentage.
* **PWR-3-ios** With headphones connected, the headphones icon shows on the
  timing screen, and bells play only in the headphones. Disconnecting them
  mid-session moves the bells to the speaker.
* **PWR-4-ios** A phone call during a bell: the bell rings from its backup, or
  right after the call ends. It never rings twice.

## QA-only settings (Debug builds)

Settings shows a "QA" section in Debug builds only. Release builds ignore
these settings.

* **Backup notifications without text**: backup notifications get no title
  or text. This checks whether iOS then plays the sound with almost no banner
  on the lock screen. It takes effect from the next session.
