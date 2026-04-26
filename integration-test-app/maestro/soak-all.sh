#!/usr/bin/env bash
set -uo pipefail

# 통합 테스트 실행 스크립트 (AOS + iOS)
# Crontab 등록 예시: 0 3 * * * /path/to/soak-all.sh

MAESTRO_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$MAESTRO_DIR/common.sh"

echo "[$(date)] Starting integrated soak tests for all platforms..."

# 1. Android Soak Test (Background 실행 또는 순차 실행 선택 가능)
# 여기서는 순차 실행으로 구성합니다.
echo ">> Running Android Soak Test..."
"$MAESTRO_DIR/android-soak-60m.sh" || echo "Android soak script failed"

# 2. iOS Soak Test
echo ">> Running iOS Soak Test..."
"$MAESTRO_DIR/ios-soak-60m.sh" || echo "iOS soak script failed"

echo "[$(date)] Integrated soak tests completed."
