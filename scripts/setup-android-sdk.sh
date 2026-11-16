#!/usr/bin/env bash
# Install a headless Android SDK (command-line tools, platform, build tools) for
# building and unit-testing android/ on Linux, e.g. in a Claude Code cloud session.
# Idempotent. Needs network access to dl.google.com and maven.google.com.
# Usage: scripts/setup-android-sdk.sh            (installs to $ANDROID_HOME or /opt/android-sdk)
set -euo pipefail

SDK="${ANDROID_HOME:-/opt/android-sdk}"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-13114758_latest.zip"
PLATFORM="platforms;android-36"
BUILD_TOOLS="build-tools;36.0.0"

if [[ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]]; then
  tmp=$(mktemp -d)
  curl -fsSL -o "$tmp/tools.zip" "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
  mkdir -p "$SDK/cmdline-tools"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
fi

yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "platform-tools" "$PLATFORM" "$BUILD_TOOLS"

# Point the Gradle build at the SDK (local.properties is gitignored).
repo_root="$(cd "$(dirname "$0")/.." && pwd)"
echo "sdk.dir=$SDK" > "$repo_root/android/local.properties"
echo "Android SDK ready at $SDK"
