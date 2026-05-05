#!/usr/bin/env bash
set -uo pipefail

# Android 1회 검증 스크립트 (Crontab/CI용)
MAESTRO_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$MAESTRO_DIR/common.sh"

OUT_DIR="$MAESTRO_DIR/results/android-validate-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT_DIR"
LOG="$OUT_DIR/validate.log"

ANDROID_DEVICE_ID="${ANDROID_DEVICE_ID:-emulator-5554}"

export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$HOME/.maestro/bin:/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"

echo "[$(date)] Starting Android validation on $ANDROID_DEVICE_ID..."
if ! /opt/homebrew/bin/adb -s "$ANDROID_DEVICE_ID" get-state >/dev/null 2>&1; then
  echo "Android device '$ANDROID_DEVICE_ID' is not connected. Set ANDROID_DEVICE_ID or start the emulator first." | tee "$LOG"
  send_slack_notification "Android-Validate" "FAIL" "*Result:* FAIL\n*Reason:* Android device '$ANDROID_DEVICE_ID' is not connected.\n*Log:* $LOG"
  exit 1
fi

/opt/homebrew/bin/adb -s "$ANDROID_DEVICE_ID" reverse tcp:8081 tcp:8081 >/dev/null 2>&1 || true
/opt/homebrew/bin/adb -s "$ANDROID_DEVICE_ID" shell am force-stop com.integrationtestapp >/dev/null 2>&1 || true
/opt/homebrew/bin/adb -s "$ANDROID_DEVICE_ID" shell am start -n com.integrationtestapp/com.integrationtestapp.MainActivity >/dev/null 2>&1 || true
sleep 3
set +e
maestro --device "$ANDROID_DEVICE_ID" test "$MAESTRO_DIR/android-ad-validation.yaml" > "$LOG" 2>&1
EXIT_CODE=$?
set -e

MSG="*Result:* $( [ $EXIT_CODE -eq 0 ] && echo "PASS" || echo "FAIL" )\n*Log:* $LOG"

if [ $EXIT_CODE -eq 0 ]; then
  send_slack_notification "Android-Validate" "PASS" "$MSG"
else
  # 실패 시 스크린샷 캡처 시도
  /opt/homebrew/bin/adb -s "$ANDROID_DEVICE_ID" exec-out screencap -p > "$OUT_DIR/fail.png" 2>/dev/null || true
  send_slack_notification "Android-Validate" "FAIL" "$MSG"
fi

exit $EXIT_CODE
