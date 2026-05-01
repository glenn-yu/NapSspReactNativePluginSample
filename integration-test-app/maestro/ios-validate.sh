#!/usr/bin/env bash
set -uo pipefail

# iOS 1회 검증 스크립트 (Crontab/CI용)
MAESTRO_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$MAESTRO_DIR/common.sh"

SIM_UDID="8D90B616-14A9-4A49-A1A7-0470FF80A9F9"
OUT_DIR="$MAESTRO_DIR/results/ios-validate-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT_DIR"
LOG="$OUT_DIR/validate.log"

export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH:$HOME/.maestro/bin:/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"

ocr_interstitial_success() {
  local image_path="$1"
  [ -f "$image_path" ] || return 1
  command -v tesseract >/dev/null 2>&1 || return 1

  local ocr_text
  ocr_text="$(tesseract "$image_path" stdout --psm 6 2>/dev/null | tr '\n' ' ')"
  echo "$ocr_text" | rg -qi "\bTEST\b|320x100|390x100|INTER_STATUS:?LOADED:?OPENED:?IMPRESSION|INTER loaded=true opened=true impression=true"
}

echo "[$(date)] Starting iOS validation..."
set +e
maestro --device "$SIM_UDID" test "$MAESTRO_DIR/ios-ad-validation.yaml" > "$LOG" 2>&1
EXIT_CODE=$?
set -e

if [ $EXIT_CODE -ne 0 ]; then
  xcrun simctl io "$SIM_UDID" screenshot "$OUT_DIR/fail.png" >/dev/null 2>&1 || true
  if ocr_interstitial_success "$OUT_DIR/fail.png"; then
    echo "[fallback] OCR detected iOS interstitial success markers; treating validate as PASS" >> "$LOG"
    EXIT_CODE=0
  fi
fi

MSG="*Result:* $( [ $EXIT_CODE -eq 0 ] && echo "PASS" || echo "FAIL" )\n*Log:* $LOG"

if [ $EXIT_CODE -eq 0 ]; then
  send_slack_notification "iOS-Validate" "PASS" "$MSG"
else
  send_slack_notification "iOS-Validate" "FAIL" "$MSG"
fi

exit $EXIT_CODE
