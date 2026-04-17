#!/usr/bin/env bash
set -euo pipefail

# Usage: ./run-ios-sim.sh
# Requires: macOS, Xcode, CocoaPods

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

echo "Installing pods"
cd ios
pod install --repo-update
cd ..

echo "Running on iOS simulator (iPhone 14)"
npx react-native run-ios --simulator "iPhone 14"

echo "Done."
