# Impermanence behavior spec

This is the product behavior both apps implement. iOS is the reference design.
Each app should feel native on its platform, but a user moving between them
should find the same features, the same words for them, and the same results.

Bell timing (section 1) is the exception: there, both apps must behave
**identically**. The shared test vectors in `docs/fixtures/` pin it down, and
both apps' unit tests must pass them.

## 1. Bells (identical on both platforms)

Bells are the most important thing the app does. A bell must ring exactly when
it should and never when it shouldn't.

### 1.1 Schedule of a day

- A day has a **start time** (a wall-clock time: hour, minute, second) and an
  ordered list of **segments** (each a duration of 1 second to 24 hours, to
  the second). Import keeps durations as given, with no rounding.
- An **occurrence** of a day on calendar date D, in the device's current time
  zone, starts at that wall-clock time on D:
  - If the time doesn't exist on D (DST gap), it moves forward by the gap
    length (02:30 becomes 03:30 when 02:00 to 03:00 is skipped). This is the
    `java.time` `ZonedDateTime.of` default.
  - If the time happens twice (DST fall-back), the first occurrence is used.
- Segment boundaries are the start plus cumulative durations in **real
  elapsed seconds**. A 45-minute sit is always 45 real minutes, even across a
  DST change.
- **Events**:
  - the start bell, at the occurrence start;
  - each segment's end bell, at that segment's end. This is the segment's
    custom bell, or the day's default bell.
  - There is no separate bell when segment n+1 begins: segment n's end bell
    covers it.
- A bell is 1 to 12 **chimes**. Chime i rings at the event instant plus
  3 s × i.

### 1.2 Timing sessions

- Opening a day's active screen starts a **session**. Being on that screen
  *is* "timing this retreat day".
- **Leaving the screen ends the session.** No bell of that session rings
  afterwards. A chime that is already sounding plays out.
- Swiping the app away, or an explicit Stop action, also ends it.
- Being in the background, the screen turning off, or the system ending the
  process do **not** end it. The bells keep ringing, and the next launch
  resumes the session and reopens its screen.
- **Which occurrence**: when a session opens at instant T on local date D, it
  times D−1's occurrence if T is before that occurrence's end (a day crossing
  midnight that is still running). Otherwise it times D's.
- **No backfill**: nothing scheduled before the session opened ever rings.
  This includes the start bell when the day is opened partway through
  segment 1.
- **Loop days** (setting, on by default):
  - Off: the session times its one occurrence, then shows Complete.
  - On: the session moves to the occurrence on D+1 only once now is past the
    end of D's occurrence *and* the date is past D. A normal day shows
    Complete until midnight; a day crossing midnight keeps running.
  - Events at or before the rollover instant still ring, so a day ending at
    00:00 rings its final bell.
  - D+1's events ring only if they are strictly after the end of D's
    occurrence. This only matters for days of 24 h or more: for an exactly
    24-hour day, D's last end bell rings and D+1's start bell, at the same
    instant, does not.
- **Late bells**: if the app could not ring a bell on time (the process was
  suspended or restarted), only the most recent missed event may ring, and
  only if it is at most 60 s late. Its chimes keep their 3 s spacing from
  now. All chimes of an event are decided together, by the event's first
  chime. A bell the platform already rang (for example by notification or
  alarm) never rings again.
- **Clock or time-zone changes** recompute the schedule immediately. Wall-clock
  times follow the new zone, and events keep their identity, so nothing rings
  twice.

### 1.3 Quick Sit

- **Duration**: 1 to 180 minutes, default 15.
- **Bells**:
  - The start chimes ring immediately on Start.
  - The end chimes ring once, at start + duration, with the screen off too.
  - Both use one chosen sound; chime counts are 1 to 12, defaults 1 and 1.
- Cancel or Close ends it with no end bell.
- While a sit is running it can't be dismissed by accident; on iOS, the sheet
  can't be swiped down.
- While it runs, the screen shows a live "Time remaining" countdown
  (`HH:MM:SS`) and progress ring. There is no "Ends around" time.
- When the sit completes, the screen closes and the end chimes ring out.
- It resumes after the system ends the process, like a day session.

### 1.4 Always audible

- Bells ring regardless of the silent/ring switch and of Focus or Do Not
  Disturb.
  - iOS: `.playback` audio session, background audio, and time-sensitive
    notifications as a fallback.
  - Android: alarm audio stream and exact alarms. While a session runs, the
    foreground service holds a timed partial wake lock and the app asks to be
    exempt from battery optimization, because some makers' battery savers
    delay alarms. Reliability wins over battery: a long sit or retreat day may
    use more power, plugged in or not.
- Bells are **just sounds**. They don't fill the notification list.
  Android's only notification is the ongoing timer notification, with Stop.
  iOS keeps a backup notification for each bell in the next 2 hours. It
  fires only if iOS has stopped the app or holds its audio (for example during
  a phone call, when iOS's own notification sound is the only way a bell can
  sound), plays only its sound when the app is in front, and is cleared from
  the notification list afterwards.
- While headphones or a Bluetooth audio device are connected, the timing
  screens show a small headphones icon, so it's clear where bells will sound.
  On iOS bells play only in the headphones; Android's alarm stream also plays
  on the phone speaker. The icon explains this when tapped.
- The app warns, on the timing screens, when something could still keep bells
  from being heard: notifications or alarms disallowed, very low volume (under
  a fifth), or (Android) battery optimization still on.
- "Ring now" plays the manual bell immediately and is not part of the
  schedule.

## 2. Features (same experience, native presentation)

Each item below should exist on both platforms with the same name and
meaning. Layout and navigation follow each platform's conventions.

| Area | Expected behavior |
| --- | --- |
| Seed data | First launch creates "Opening Day" (18:00, 5 segments, orange) and "Full Day" (07:00, 22 segments, poppy). |
| Days list | Shows each day's name and time range in its theme color; reorder and delete. Deleting removes the day at once and shows "“<name>” deleted" with Undo for 5 seconds (longer with a screen reader); Undo puts it back in the same place. An empty list says "No days yet. Tap New Day to begin." |
| Day detail | Start time and end time, theme, segments with times and chime counts, "Start or Resume Day", Edit. |
| Active day | An "End Day" button ends timing, like leaving the screen. Ending is cheap: bells are tied to clock times, so starting the same day again resumes it with no confirmation. Status (Empty, Day queued, Now, Complete), current segment, time remaining, next bell time, schedule with the current segment highlighted, manual bell ("Ring now", sound, chimes). |
| Editing | Name, start time, theme, default bell (which also sets the start and manual bells), segments (name, duration as minutes and seconds, custom or default bell; move by touch-and-hold drag, or with the screen reader's "Move up" and "Move down"; duplicate, placed right after the original; delete, which shows "“<name>” deleted", or "Segment deleted" for an unnamed one, with Undo for 5 seconds, putting it back in the same position). New segments: 15 min. A day needs a name and at least one segment; Add/Done stay disabled until it has both. Leaving the editor with unsaved changes asks "Discard changes?" first. |
| Lock screen | While a day is queued or running, the lock screen shows the day's name, the current segment (or "Day queued") and a live countdown to the next bell (or to the start), updated when the segment changes. A Quick Sit shows "Quick Sit" and "Time remaining". It disappears as soon as timing ends. Display only: bells never depend on it. iOS: a Live Activity, also in the Dynamic Island (iOS 16.2 and later). Android: the ongoing notification. |
| Settings | 24-hour clock (off), Loop days at midnight (on), Keep screen awake during active day (on), Import/Export Days. |
| Keep screen awake | While a day is queued or running, if the setting is on. |
| Import/export | Import first asks "Replace all days?", because it replaces every day. JSON `{ "version": 1, "days": [ ... ] }` with integer seconds (see `docs/fixtures/*.json` `day` for one day). File name `impermanence-days-YYYY-MM-DD.json`. Import also accepts the iOS native array. Unknown themes become teal; bells are clamped to valid values. |
| Themes | 16 named colors. Text is black, except white on indigo, magenta, navy, oxblood and purple. |
| Time display | `h:mm AM` or `HH:mm`; a time that isn't on a whole minute adds seconds (`7:15:30 AM` or `07:15:30`). Countdowns are `HH:MM:SS`. |
| Backup | The OS backup keeps days and settings; a running session and the bell log are never restored onto another device. |
| Unreadable days file | If the saved days can't be read, the app starts with an empty list but keeps a copy of the file and offers to save it, so nothing is lost. |

When a behavior changes on one platform, update this file in the same PR,
and track the matching change for the other platform.
