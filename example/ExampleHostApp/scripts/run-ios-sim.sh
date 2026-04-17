#!/usr/bin/env bash
set -euo pipefail

# Usage: ./run-ios-sim.sh
# Requires: macOS, Xcode, CocoaPods

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

if ! command -v xcodebuild >/dev/null 2>&1; then
  echo "xcodebuild is not available. Install Xcode command line tools first."
  exit 1
fi

if ! command -v pod >/dev/null 2>&1; then
  echo "CocoaPods is not installed. Install pod first, then rerun this script."
  exit 1
fi

echo "Installing pods"
cd ios
pod install --repo-update
cd ..

echo "Running on iOS simulator (iPhone 14)"
npx react-native run-ios --simulator "iPhone 14"

echo "Done."
