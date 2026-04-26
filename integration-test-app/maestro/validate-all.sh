#!/usr/bin/env bash
set -uo pipefail

# 통합 One-shot 검증 스크립트 (AOS + iOS)
# Crontab 등록 예시: 0 * * * * /path/to/validate-all.sh

MAESTRO_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$MAESTRO_DIR/common.sh"

echo "[$(date)] Starting integrated validation for all platforms..."

# 1. Android Validation
"$MAESTRO_DIR/android-validate.sh" || echo "Android validation failed"

# 2. iOS Validation
"$MAESTRO_DIR/ios-validate.sh" || echo "iOS validation failed"

echo "[$(date)] Integrated validation completed."
