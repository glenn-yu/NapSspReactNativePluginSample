#!/usr/bin/env bash
set -uo pipefail

# 통합 One-shot 검증 스크립트 (AOS + iOS)
# Crontab 등록 예시: 0 * * * * /path/to/validate-all.sh

MAESTRO_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$MAESTRO_DIR/common.sh"

echo "[$(date)] Starting integrated validation for all platforms..."

EXIT_CODE=0

# 1. Android Validation
if ! "$MAESTRO_DIR/android-validate.sh"; then
  echo "Android validation failed"
  EXIT_CODE=1
fi

# 2. iOS Validation
if ! "$MAESTRO_DIR/ios-validate.sh"; then
  echo "iOS validation failed"
  EXIT_CODE=1
fi

echo "[$(date)] Integrated validation completed."
exit $EXIT_CODE
