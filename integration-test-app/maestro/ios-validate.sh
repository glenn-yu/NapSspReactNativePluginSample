#!/usr/bin/env bash
set -uo pipefail

# iOS 1회 검증 스크립트 (Crontab/CI용)
MAESTRO_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$MAESTRO_DIR/common.sh"

SIM_UDID="8D90B616-14A9-4A49-A1A7-0470FF80A9F9"
OUT_DIR="$MAESTRO_DIR/results/ios-validate-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT_DIR"
LOG="$OUT_DIR/validate.log"

export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH:$HOME/.maestro/bin:/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"

echo "[$(date)] Starting iOS validation..."
set +e
maestro --device "$SIM_UDID" test "$MAESTRO_DIR/ios-ad-validation.yaml" > "$LOG" 2>&1
EXIT_CODE=$?
set -e

MSG="*Result:* $( [ $EXIT_CODE -eq 0 ] && echo "PASS" || echo "FAIL" )\n*Log:* $LOG"

if [ $EXIT_CODE -eq 0 ]; then
  send_slack_notification "iOS-Validate" "PASS" "$MSG"
else
  # 실패 시 스크린샷 캡처 시도
  xcrun simctl io "$SIM_UDID" screenshot "$OUT_DIR/fail.png" >/dev/null 2>&1 || true
  send_slack_notification "iOS-Validate" "FAIL" "$MSG"
fi

exit $EXIT_CODE
