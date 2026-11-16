#!/usr/bin/env python3
"""Builds the multi-chime bell sounds used by iOS backup notifications.

A notification can play one sound file of at most 30 s. bell_0_x<N>.caf is
N strikes of bell_0.caf, 3 s apart, mixed into one file (N = 2..6; 6 strikes
last 29.7 s). The app plays each strike itself; these files are only for
the notification that rings if iOS has stopped the app.

Needs ffmpeg and numpy. Run from the repo root: python3 scripts/make_bell_sounds.py
"""
import os
import subprocess
import tempfile

import numpy as np

RES = "ios/Impermanence/Impermanence/Resources"
RATE = 44100
SPACING_S = 3
MAX_STRIKES = 6


def main():
    src = os.path.join(RES, "bell_0.caf")
    with tempfile.TemporaryDirectory() as tmp:
        raw = os.path.join(tmp, "bell.raw")
        subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", src,
                        "-f", "s16le", "-ac", "1", "-ar", str(RATE), raw], check=True)
        strike = np.fromfile(raw, dtype="<i2").astype(np.float64)
        for n in range(2, MAX_STRIKES + 1):
            offset = SPACING_S * RATE
            mix = np.zeros(offset * (n - 1) + len(strike))
            for i in range(n):
                mix[i * offset:i * offset + len(strike)] += strike
            peak = np.max(np.abs(mix))
            if peak > 32767:
                mix *= 32767 / peak
            out_raw = os.path.join(tmp, f"x{n}.raw")
            mix.astype("<i2").tofile(out_raw)
            out = os.path.join(RES, f"bell_0_x{n}.caf")
            subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                            "-f", "s16le", "-ar", str(RATE), "-ac", "1", "-i", out_raw,
                            "-c:a", "adpcm_ima_qt", out], check=True)
            print(out, f"{len(mix) / RATE:.1f} s")


if __name__ == "__main__":
    main()
