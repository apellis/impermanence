#!/usr/bin/env bash
# Build and run the iOS unit + UI tests on a Simulator, without code signing.
# Usage: scripts/ios-test.sh ["platform=iOS Simulator,name=iPhone 16"]
# With no argument, picks the newest available iPhone simulator whose iOS version
# matches the active Xcode's simulator SDK (newer runtimes may be installed on CI
# images but are not supported by that Xcode).
set -euo pipefail
cd "$(dirname "$0")/../ios/Impermanence"

DEST="${1:-}"
if [[ -z "$DEST" ]]; then
  SDK_VERSION=$(xcrun --sdk iphonesimulator --show-sdk-version)
  UDID=$(xcrun simctl list devices available -j | SDK_VERSION="$SDK_VERSION" /usr/bin/python3 -c '
import json, os, sys
sdk = tuple(int(x) for x in os.environ["SDK_VERSION"].split("."))
data = json.load(sys.stdin)["devices"]
best = None
for runtime, devs in data.items():
    if "iOS" not in runtime:
        continue
    ver = tuple(int(x) for x in runtime.rsplit("iOS-", 1)[-1].split("-") if x.isdigit())
    if ver[:2] > sdk[:2]:
        continue
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
status=0
xcodebuild test \
  -project Impermanence.xcodeproj \
  -scheme Impermanence \
  -destination "$DEST" \
  -derivedDataPath build/DerivedData \
  -resultBundlePath build/TestResults.xcresult \
  -parallel-testing-enabled NO \
  CODE_SIGNING_ALLOWED=NO || status=$?

if [[ $status -ne 0 && -d build/TestResults.xcresult ]]; then
  echo "---- Failing test details ----"
  xcrun xcresulttool get test-results tests --path build/TestResults.xcresult \
    | /usr/bin/python3 -c '
import json, sys
def walk(node, path):
    name = node.get("name", "")
    if node.get("nodeType") == "Failure Message":
        print(" / ".join(path) + ": " + name)
    for child in node.get("children", []):
        walk(child, path + [name] if node.get("nodeType") == "Test Case" else path)
for root in json.load(sys.stdin).get("testNodes", []):
    walk(root, [])
' || true
fi
exit $status
