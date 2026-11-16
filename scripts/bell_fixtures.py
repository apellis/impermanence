#!/usr/bin/env python3
"""Generate docs/fixtures/bell-*.json: shared bell-timing test vectors.

This is an independent reference implementation of the bell semantics in
docs/BEHAVIOR.md, written with Python's zoneinfo instead of either app's code.
Both apps' unit tests load the generated fixtures and must produce exactly the
same bell events. If a rule changes, change it in docs/BEHAVIOR.md, here, and
in both apps, then regenerate:

    python3 scripts/bell_fixtures.py
"""
from __future__ import annotations

import json
import uuid
from datetime import date, datetime, time, timedelta, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

OUT = Path(__file__).resolve().parent.parent / "docs" / "fixtures"
CHIME_SPACING = 3  # seconds between chimes of one bell


def wall_clock(d: date, seconds_after_midnight: int, tz: ZoneInfo) -> datetime:
    """Wall-clock time on date d. fold=0 means: a time in a DST gap is read
    with the offset before the gap (it moves forward by the gap), and a
    repeated time is its first occurrence."""
    s = seconds_after_midnight % 86400
    naive = datetime.combine(d, time(s // 3600, (s % 3600) // 60, s % 60))
    return naive.replace(tzinfo=tz, fold=0).astimezone(timezone.utc)


def occurrence(day: dict, d: date, tz: ZoneInfo):
    start = wall_clock(d, day["startTimeSeconds"], tz)
    segs, cursor = [], start
    for seg in day["segments"]:
        end = cursor + timedelta(seconds=seg["durationSeconds"])
        segs.append((cursor, end))
        cursor = end
    return start, segs


def events(day: dict, d: date, tz: ZoneInfo):
    start, segs = occurrence(day, d, tz)
    out = [dict(instant=start, kind="start", segmentIndex=0, bell=day["startBell"])]
    for i, seg in enumerate(day["segments"]):
        bell = seg.get("customEndBell") or day["defaultBell"]
        out.append(dict(instant=segs[i][1], kind="end", segmentIndex=i, bell=bell))
    return out, segs[-1][1]


def session_events(day: dict, tz: ZoneInfo, session_start: datetime, until: datetime, loop: bool):
    local_today = session_start.astimezone(tz).date()
    _, yesterday_end = events(day, local_today - timedelta(days=1), tz)
    anchor = local_today - timedelta(days=1) if session_start < yesterday_end else local_today

    result = []
    d, previous_end = anchor, None
    while True:
        evs, end = events(day, d, tz)
        if evs[0]["instant"] > until and (previous_end or evs[0]["instant"]) > until:
            break
        for e in evs:
            if previous_end is not None and e["instant"] < previous_end:
                continue
            if session_start <= e["instant"] <= until:
                result.append(e)
        previous_end = max(previous_end or end, end)
        if not loop:
            break
        d += timedelta(days=1)
    result.sort(key=lambda e: e["instant"])
    return result


def iso(dt: datetime) -> str:
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def bell(rings=1, sound=0):
    return {"soundId": sound, "numRings": rings}


def make_day(name, start_hms, minutes, start_bell=None, default_bell=None, custom=None, theme="teal"):
    h, m = start_hms
    custom = custom or {}
    seed = uuid.uuid5(uuid.NAMESPACE_URL, "impermanence-fixture/" + name)
    return {
        "id": str(seed).upper(),
        "name": name,
        "startTimeSeconds": h * 3600 + m * 60,
        "segments": [
            {
                "id": str(uuid.uuid5(seed, str(i))).upper(),
                "name": f"Segment {i + 1}",
                "durationSeconds": mins * 60,
                **({"customEndBell": custom[i]} if i in custom else {}),
            }
            for i, mins in enumerate(minutes)
        ],
        "startBell": start_bell or bell(),
        "manualBell": default_bell or bell(),
        "defaultBell": default_bell or bell(),
        "theme": theme,
    }


FULL_DAY = [15, 45, 45, 30, 45, 30, 60, 30, 45, 45, 30, 45, 45, 60, 30, 45, 30, 45, 30, 90, 30, 45]
OPENING_DAY = [45, 30, 90, 30, 45]


def local(zone, y, mo, d, h, mi, s=0, fold=0):
    return datetime(y, mo, d, h, mi, s, tzinfo=ZoneInfo(zone), fold=fold).astimezone(timezone.utc)


CASES = [
    dict(name="plain-full-day", description="Full Day on an ordinary date, opened before it starts, no looping.",
         zone="America/New_York", loop=False, day=make_day("Full Day", (7, 0), FULL_DAY),
         start=local("America/New_York", 2026, 6, 10, 6, 30), until=local("America/New_York", 2026, 6, 11, 6, 0)),
    dict(name="dst-spring-forward-us", description="US spring forward (02:00 -> 03:00 on 2026-03-08). 07:00 must stay 07:00; segments spanning the gap last their real duration.",
         zone="America/Los_Angeles", loop=False, day=make_day("Night into spring", (1, 0), [45, 45, 60, 30]),
         start=local("America/Los_Angeles", 2026, 3, 8, 0, 30), until=local("America/Los_Angeles", 2026, 3, 8, 12, 0)),
    dict(name="dst-spring-forward-full-day", description="Full Day on the US spring-forward date: start bell at 07:00 PDT, not 08:00.",
         zone="America/Los_Angeles", loop=False, day=make_day("Full Day", (7, 0), FULL_DAY),
         start=local("America/Los_Angeles", 2026, 3, 8, 5, 0), until=local("America/Los_Angeles", 2026, 3, 9, 5, 0)),
    dict(name="dst-start-in-gap", description="Start time 02:30 does not exist on 2026-03-08 in Los Angeles; it moves forward by the gap to 03:30 PDT.",
         zone="America/Los_Angeles", loop=False, day=make_day("Gap start", (2, 30), [30, 30]),
         start=local("America/Los_Angeles", 2026, 3, 8, 0, 0), until=local("America/Los_Angeles", 2026, 3, 8, 12, 0)),
    dict(name="dst-fall-back-us", description="US fall back (02:00 -> 01:00 on 2026-11-01). Segments spanning the repeated hour last their real duration.",
         zone="America/Los_Angeles", loop=False, day=make_day("Night into fall", (0, 30), [45, 45, 60]),
         start=local("America/Los_Angeles", 2026, 11, 1, 0, 0), until=local("America/Los_Angeles", 2026, 11, 1, 12, 0)),
    dict(name="dst-start-ambiguous", description="Start time 01:30 happens twice on 2026-11-01 in Los Angeles; the first (PDT) is used.",
         zone="America/Los_Angeles", loop=False, day=make_day("Ambiguous start", (1, 30), [20, 20]),
         start=local("America/Los_Angeles", 2026, 11, 1, 0, 0), until=local("America/Los_Angeles", 2026, 11, 1, 12, 0)),
    dict(name="dst-fall-back-full-day-loop", description="Full Day looping across the US fall-back night: both days start at 07:00 local.",
         zone="America/Los_Angeles", loop=True, day=make_day("Full Day", (7, 0), FULL_DAY),
         start=local("America/Los_Angeles", 2026, 10, 31, 6, 0), until=local("America/Los_Angeles", 2026, 11, 1, 23, 0)),
    dict(name="dst-spring-forward-london", description="UK spring forward (01:00 -> 02:00 on 2026-03-29); start 01:30 becomes 02:30 BST.",
         zone="Europe/London", loop=False, day=make_day("London gap", (1, 30), [45, 45]),
         start=local("Europe/London", 2026, 3, 29, 0, 30), until=local("Europe/London", 2026, 3, 29, 12, 0)),
    dict(name="dst-half-hour-lord-howe", description="Lord Howe Island moves 30 minutes (02:00 -> 02:30 on 2026-10-04); start 02:15 becomes 02:45.",
         zone="Australia/Lord_Howe", loop=False, day=make_day("Half-hour shift", (2, 15), [30, 30]),
         start=local("Australia/Lord_Howe", 2026, 10, 4, 0, 0), until=local("Australia/Lord_Howe", 2026, 10, 4, 12, 0)),
    dict(name="no-dst-kolkata", description="A zone with a non-hour offset and no DST.",
         zone="Asia/Kolkata", loop=False, day=make_day("Opening Day", (18, 0), OPENING_DAY),
         start=local("Asia/Kolkata", 2026, 3, 8, 17, 0), until=local("Asia/Kolkata", 2026, 3, 9, 17, 0)),
    dict(name="crosses-midnight-opened-before", description="Day 22:00-01:00 opened at 23:30: rings the 00:00 and 01:00 bells only (no backfill).",
         zone="America/New_York", loop=False, day=make_day("Night sit", (22, 0), [60, 60, 60]),
         start=local("America/New_York", 2026, 6, 10, 23, 30), until=local("America/New_York", 2026, 6, 11, 12, 0)),
    dict(name="crosses-midnight-opened-after", description="Day 22:00-01:00 opened at 00:30: still yesterday's run, rings 01:00.",
         zone="America/New_York", loop=False, day=make_day("Night sit", (22, 0), [60, 60, 60]),
         start=local("America/New_York", 2026, 6, 11, 0, 30), until=local("America/New_York", 2026, 6, 11, 12, 0)),
    dict(name="crosses-midnight-loop", description="Looping day 22:00-01:00: keeps running past midnight and rolls to the next run only after 01:00.",
         zone="America/New_York", loop=True, day=make_day("Night sit", (22, 0), [60, 60, 60]),
         start=local("America/New_York", 2026, 6, 10, 21, 0), until=local("America/New_York", 2026, 6, 12, 2, 0)),
    dict(name="ends-at-midnight-loop", description="Looping day 23:00-00:00: the final bell at 00:00 rings, then the next run starts at 23:00.",
         zone="America/New_York", loop=True, day=make_day("Late sit", (23, 0), [60], default_bell=bell(3)),
         start=local("America/New_York", 2026, 6, 10, 22, 0), until=local("America/New_York", 2026, 6, 12, 0, 30)),
    dict(name="opened-mid-segment", description="Opening Day opened at 18:10, partway through the first segment: no start bell.",
         zone="America/Chicago", loop=False, day=make_day("Opening Day", (18, 0), OPENING_DAY),
         start=local("America/Chicago", 2026, 6, 10, 18, 10), until=local("America/Chicago", 2026, 6, 11, 18, 0)),
    dict(name="opened-after-day-complete", description="Non-looping day opened after it ended: nothing rings.",
         zone="America/Chicago", loop=False, day=make_day("Opening Day", (18, 0), OPENING_DAY),
         start=local("America/Chicago", 2026, 6, 10, 23, 0), until=local("America/Chicago", 2026, 6, 11, 23, 0)),
    dict(name="loop-rollover", description="Looping Opening Day opened mid-run: rest of today, then all of tomorrow.",
         zone="Europe/Berlin", loop=True, day=make_day("Opening Day", (18, 0), OPENING_DAY),
         start=local("Europe/Berlin", 2026, 6, 10, 19, 0), until=local("Europe/Berlin", 2026, 6, 12, 0, 0)),
    dict(name="custom-bells", description="Start bell, default bell and per-segment custom bells with several rings.",
         zone="America/New_York", loop=False,
         day=make_day("Bells", (9, 0), [10, 10, 10], start_bell=bell(2), default_bell=bell(1), custom={1: bell(5), 2: bell(12)}),
         start=local("America/New_York", 2026, 6, 10, 8, 0), until=local("America/New_York", 2026, 6, 10, 12, 0)),
    dict(name="longer-than-a-day-loop", description="A 25-hour looping day: the next run's bells only ring after the previous run ends.",
         zone="America/New_York", loop=True, day=make_day("Marathon", (6, 0), [600, 600, 300]),
         start=local("America/New_York", 2026, 6, 10, 5, 0), until=local("America/New_York", 2026, 6, 12, 12, 0)),
]


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("bell-*.json"):
        old.unlink()
    for case in CASES:
        tz = ZoneInfo(case["zone"])
        evs = session_events(case["day"], tz, case["start"], case["until"], case["loop"])
        fixture = {
            "name": case["name"],
            "description": case["description"],
            "zone": case["zone"],
            "loopDays": case["loop"],
            "sessionStart": iso(case["start"]),
            "until": iso(case["until"]),
            "day": case["day"],
            "expectedEvents": [
                {
                    "instant": iso(e["instant"]),
                    "local": e["instant"].astimezone(tz).strftime("%Y-%m-%d %H:%M:%S %Z"),
                    "kind": e["kind"],
                    "segmentIndex": e["segmentIndex"],
                    "numRings": e["bell"]["numRings"],
                    "soundId": e["bell"]["soundId"],
                }
                for e in evs
            ],
        }
        path = OUT / f"bell-{case['name']}.json"
        path.write_text(json.dumps(fixture, indent=2) + "\n")
        print(f"{path.name}: {len(evs)} events")


if __name__ == "__main__":
    main()
