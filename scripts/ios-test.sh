#!/usr/bin/env bash
# Build and run the iOS unit + UI tests on a Simulator, without code signing.
# Usage: scripts/ios-test.sh ["platform=iOS Simulator,name=iPhone 16"]
# With no argument, picks the newest available iPhone simulator.
set -euo pipefail
cd "$(dirname "$0")/../ios/Impermanence"

DEST="${1:-}"
if [[ -z "$DEST" ]]; then
  UDID=$(xcrun simctl list devices available -j | /usr/bin/python3 -c '
import json, sys
data = json.load(sys.stdin)["devices"]
best = None
for runtime, devs in data.items():
    if "iOS" not in runtime:
        continue
    ver = tuple(int(x) for x in runtime.rsplit("iOS-", 1)[-1].split("-") if x.isdigit())
    for d in devs:
        if d["name"].startswith("iPhone") and (best is None or ver > best[0]):
            best = (ver, d["udid"], d["name"])
if not best:
    sys.exit("No available iPhone simulator found")
print(best[1])
')
  DEST="platform=iOS Simulator,id=$UDID"
fi
echo "Using destination: $DEST"

rm -rf build/TestResults.xcresult
xcodebuild test \
  -project Impermanence.xcodeproj \
  -scheme Impermanence \
  -destination "$DEST" \
  -derivedDataPath build/DerivedData \
  -resultBundlePath build/TestResults.xcresult \
  CODE_SIGNING_ALLOWED=NO
