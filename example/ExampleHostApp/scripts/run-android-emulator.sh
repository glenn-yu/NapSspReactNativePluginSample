#!/usr/bin/env bash
set -euo pipefail

# Usage: ./run-android-emulator.sh
# Requires: Android SDK, emulator, adb in PATH, Java/JDK installed

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

if ! command -v java >/dev/null 2>&1 || ! java -version >/dev/null 2>&1; then
  echo "Java runtime/JDK is not available. Install a JDK before running Android builds."
  exit 1
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "adb is not available on PATH. Install Android platform-tools and retry."
  exit 1
fi

if [ ! -x "android/gradlew" ]; then
  echo "android/gradlew is missing or not executable."
  exit 1
fi

echo "Building ExampleHostApp (debug)"
./android/gradlew -p android assembleDebug -x lint

echo "Installing on connected device / emulator"
./android/gradlew -p android installDebug

echo "Launching app via adb"
PACKAGE=$(grep -o 'applicationId "[^"]*"' -m1 android/app/build.gradle | sed 's/applicationId "//;s/"//')
if [ -z "$PACKAGE" ]; then
  echo "Could not determine applicationId; starting main activity not attempted"
  exit 0
fi
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1

echo "Done."
