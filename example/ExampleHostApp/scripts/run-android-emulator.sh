#!/usr/bin/env bash
set -euo pipefail

# Usage: ./run-android-emulator.sh
# Requires: Android SDK, emulator, adb in PATH

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

echo "Building ExampleHostApp (debug)"
./gradlew -p android assembleDebug -x lint

echo "Installing on connected device / emulator"
./gradlew -p android installDebug

echo "Launching app via adb"
# try to find package name from app/build.gradle
PACKAGE=$(grep -o "applicationId \"[^"]*\"" -m1 android/app/build.gradle | sed 's/applicationId "//;s/"//')
if [ -z "$PACKAGE" ]; then
  echo "Could not determine applicationId; starting main activity not attempted"
  exit 0
fi
adb shell monkey -p $PACKAGE -c android.intent.category.LAUNCHER 1

echo "Done."
