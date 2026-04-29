#!/usr/bin/env bash
set -uo pipefail

# Android 1회 검증 스크립트 (Crontab/CI용)
MAESTRO_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$MAESTRO_DIR/common.sh"

OUT_DIR="$MAESTRO_DIR/results/android-validate-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT_DIR"
LOG="$OUT_DIR/validate.log"

export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$HOME/.maestro/bin:/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"

echo "[$(date)] Starting Android validation..."
/opt/homebrew/bin/adb -s emulator-5554 reverse tcp:8081 tcp:8081 >/dev/null 2>&1 || true
/opt/homebrew/bin/adb -s emulator-5554 shell am force-stop com.integrationtestapp >/dev/null 2>&1 || true
/opt/homebrew/bin/adb -s emulator-5554 shell am start -n com.integrationtestapp/com.integrationtestapp.MainActivity >/dev/null 2>&1 || true
sleep 3
set +e
maestro test "$MAESTRO_DIR/android-ad-validation.yaml" > "$LOG" 2>&1
EXIT_CODE=$?
set -e

MSG="*Result:* $( [ $EXIT_CODE -eq 0 ] && echo "PASS" || echo "FAIL" )\n*Log:* $LOG"

if [ $EXIT_CODE -eq 0 ]; then
  send_slack_notification "Android-Validate" "PASS" "$MSG"
else
  # 실패 시 스크린샷 캡처 시도
  /opt/homebrew/bin/adb -s emulator-5554 exec-out screencap -p > "$OUT_DIR/fail.png" 2>/dev/null || true
  send_slack_notification "Android-Validate" "FAIL" "$MSG"
fi

exit $EXIT_CODE
