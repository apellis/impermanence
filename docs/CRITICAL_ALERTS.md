# Critical Alerts (iOS)

## What it does

A Critical Alert is a notification that plays its sound at full volume even
when the phone is on silent, in Do Not Disturb or in any Focus, and even if the
user has turned notification sounds off for the app. Apple grants it only to
apps that need it (health, safety, security), on request.

Impermanence rings its bells itself while it runs, and that already works on
silent and in Focus. Its backup notifications, which ring a bell if iOS has
suspended or ended the app, are Time Sensitive today: they show through Focus,
but **on silent they make no sound**. With Critical Alerts, the backups would
ring on silent too, so a bell is never missed even in the worst case. The user
still chooses: iOS asks separately for Critical Alerts, and they can be turned
off in Settings.

## The code is ready

`BellEngine` already asks for `.criticalAlert` permission, and when iOS reports
Critical Alerts as enabled it sends the backups as critical, with the bell
sound at full volume. Without the entitlement, iOS reports them as unsupported
and nothing changes. So turning it on is only the entitlement.

## Steps for Alex

1. Sign in at https://developer.apple.com/contact/request/notifications-critical-alerts-entitlement/
   with the Apple ID that is Account Holder or Admin of team **DBA7U5Z2F6**.
2. Fill in the form. App name: Impermanence. Bundle ID:
   `com.neversink.Meditation-Retreat-Timer`. Paste the text below for the use
   case and justification.
3. Wait for Apple's email (usually days to a few weeks). Apple may ask
   follow-up questions.
4. When approved, at https://developer.apple.com/account/resources/identifiers/
   open the app ID and check that **Critical Alerts** now appears under
   Capabilities (Apple adds it as an "Additional Capability"); enable it if needed.
5. Tell Claude. The change is one line in
   `ios/Impermanence/Impermanence/Impermanence.entitlements`:
   `com.apple.developer.usernotifications.critical-alerts` = `true`.
   Adding it before approval breaks signing, so it waits.
6. In Xcode, automatic signing then regenerates the provisioning profile on
   the next build (Product > Build, signed in as the team). Install on a
   device. At the next session start iOS asks to allow Critical Alerts; allow
   it, then run SIL-1 with the app suspended to confirm the backup rings on
   silent.

## Text for the request form

**Use case (short):** Meditation retreat bell timer. Scheduled bells mark the
start and end of each meditation period during silent retreats, and must
sound even when the phone is on silent.

**Justification:**

Impermanence is a meditation and retreat timer. During a meditation retreat,
practitioners follow a fixed schedule of sitting and walking periods for a
whole day or several days. Each period starts and ends with a bell, and the
app replaces the physical bell or the retreat manager who would ring it. On
retreat, people keep their phones on silent and in Do Not Disturb so that
nothing else interrupts them, often for days at a time, and they cannot look
at the phone to check the time. A missed bell means a practitioner sits
through the next period or misses a scheduled talk, meal or interview with a
teacher.

The app rings bells itself while it is running, using a background audio
session. It also schedules a notification for each bell as a backup, in case
iOS suspends or ends the app. Today these backups are Time Sensitive, so they
are silent when the phone is on silent, which is exactly the situation the app
is used in. We are asking to send only these backup bell notifications as
Critical Alerts.

They are sent only while the user is actively timing a retreat day or a
meditation session they started themselves, only at the scheduled bell times
the user configured, and they are cancelled as soon as the user leaves the
timing screen. The app never uses Critical
Alerts for reminders, marketing or anything else. Users are asked for
permission separately and can turn Critical Alerts off at any time in
Settings.
