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
  ordered list of **segments** (each a duration of 1 to 1440 whole minutes).
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
- When the sit completes, the screen closes and the end chimes ring out.
- It resumes after the system ends the process, like a day session.

### 1.4 Always audible

- Bells ring regardless of the silent/ring switch and of Focus or Do Not
  Disturb.
  - iOS: `.playback` audio session, background audio, and time-sensitive
    notifications as a fallback.
  - Android: alarm audio stream and exact alarms.
- The app warns, on the timing screens, when something could still keep bells
  from being heard: notifications or alarms disallowed, or very low volume.
- "Ring now" plays the manual bell immediately and is not part of the
  schedule.

## 2. Features (same experience, native presentation)

Each item below should exist on both platforms with the same name and
meaning. Layout and navigation follow each platform's conventions.

| Area | Expected behavior |
| --- | --- |
| Seed data | First launch creates "Opening Day" (18:00, 5 segments, orange) and "Full Day" (07:00, 22 segments, poppy). |
| Days list | Shows each day's name and time range in its theme color; reorder and delete. |
| Day detail | Start time and end time, theme, segments with times and chime counts, "Start or Resume Day", Edit. |
| Active day | Status (Empty, Day queued, Now, Complete), current segment, time remaining, next bell time, schedule with the current segment highlighted, manual bell ("Ring now", sound, chimes). |
| Editing | Name, start time, theme, default bell (which also sets the start and manual bells), segments (name, minutes, custom or default bell; move, duplicate, delete). New segments: 15 min. A day needs a name and at least one segment; Add/Done stay disabled until it has both. Leaving the editor with unsaved changes asks "Discard changes?" first. |
| Settings | 24-hour clock (off), Loop days at midnight (on), Keep screen awake during active day (on), Import/Export Days. |
| Keep screen awake | While a day is queued or running, if the setting is on. |
| Import/export | Import first asks "Replace all days?", because it replaces every day. JSON `{ "version": 1, "days": [ ... ] }` with integer seconds (see `docs/fixtures/*.json` `day` for one day). File name `impermanence-days-YYYY-MM-DD.json`. Import also accepts the iOS native array. Unknown themes become teal; bells are clamped to valid values. |
| Themes | 16 named colors. Text is black, except white on indigo, magenta, navy, oxblood and purple. |
| Time display | `h:mm AM` or `HH:mm`; durations `HH:MM:SS`. |

When a behavior changes on one platform, update this file in the same PR,
and track the matching change for the other platform.
