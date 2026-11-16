# Roadmap

## Now: device review of 1.4

The 1.4 changes to bells, alarms, reliability and performance get a full
device QA pass on both apps first (`docs/qa/`). Nothing larger is built on
top of them until that pass shows they are robust.

## Next point release (after the device review)

Planned for the next versioned release, in both apps:

* **Larger-scale editing.** Multi-day retreat plans, cloning days, and other
  operations across many days or segments. Starts with a UX interview with
  Alex; rethinking the set of screens and how they relate is in scope.
* **Quick Sit as a first-class feature**, not an afterthought.
* **Critical Alerts on iOS**, once Apple grants the entitlement
  (`docs/CRITICAL_ALERTS.md`).

Each item updates `docs/BEHAVIOR.md` and both QA checklists in the same PR.
