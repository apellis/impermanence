# Agent instructions

The instructions for coding agents working in this repository are in
[`CLAUDE.md`](CLAUDE.md), with per-app notes in [`ios/CLAUDE.md`](ios/CLAUDE.md) and
[`android/CLAUDE.md`](android/CLAUDE.md). The product behavior both apps implement is in
[`docs/BEHAVIOR.md`](docs/BEHAVIOR.md).

The short version:

* iOS is the reference app.
* Each app is idiomatic on its OS, and both give the same experience.
* Bell timing is identical on both and pinned by `docs/fixtures/`.
* Bells must never fail.
