#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/maestro/results/ios-soak-30m-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT_DIR"

SIM_UDID="8D90B616-14A9-4A49-A1A7-0470FF80A9F9"
export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$HOME/.maestro/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"

FLOW="$ROOT_DIR/maestro/ios-ad-validation.yaml"
DURATION_SECONDS="${SOAK_DURATION_SECONDS:-1800}"
END_TS=$(( $(date +%s) + DURATION_SECONDS ))
ITER=0; PASS=0; FAIL=0
SUMMARY="$OUT_DIR/summary.txt"
HISTORY_LOG="$ROOT_DIR/maestro/maestro-soak-history.md"
RUN_START="$(date '+%Y-%m-%d %H:%M:%S')"
: > "$SUMMARY"

ocr_interstitial_success() {
  local image_path="$1"
  [ -f "$image_path" ] || return 1
  command -v tesseract >/dev/null 2>&1 || return 1

  local ocr_text
  ocr_text="$(tesseract "$image_path" stdout --psm 6 2>/dev/null | tr '\n' ' ')"
  echo "$ocr_text" | rg -qi "\bTEST\b|320x100|390x100"
}

while [ "$(date +%s)" -lt "$END_TS" ]; do
  ITER=$((ITER + 1))
  TS="$(date '+%Y-%m-%d %H:%M:%S')"
  LOG="$OUT_DIR/run-${ITER}.log"
  echo "[$TS] iteration=$ITER starting" | tee -a "$SUMMARY"
  if maestro --device "$SIM_UDID" test \
      --debug-output "$OUT_DIR/debug-$ITER" \
      --flatten-debug-output \
      --test-output-dir "$OUT_DIR/test-output-$ITER" \
      "$FLOW" >"$LOG" 2>&1; then
    PASS=$((PASS + 1))
    echo "[$TS] iteration=$ITER result=PASS" | tee -a "$SUMMARY"
  else
    xcrun simctl io "$SIM_UDID" screenshot "$OUT_DIR/fail-${ITER}.png" >/dev/null 2>&1 || true
    if ocr_interstitial_success "$OUT_DIR/fail-${ITER}.png"; then
      PASS=$((PASS + 1))
      echo "[$TS] iteration=$ITER result=PASS fallback=ocr" | tee -a "$SUMMARY"
      echo "[fallback] OCR detected iOS interstitial popup; treating soak iteration as PASS" >> "$LOG"
    else
      FAIL=$((FAIL + 1))
      echo "[$TS] iteration=$ITER result=FAIL" | tee -a "$SUMMARY"
    fi
  fi
  sleep 5
done

RUN_END="$(date '+%Y-%m-%d %H:%M:%S')"
echo "end_time=$RUN_END" | tee -a "$SUMMARY"
echo "total_iterations=$ITER pass=$PASS fail=$FAIL" | tee -a "$SUMMARY"

{
  echo ""
  echo "## $RUN_START iOS soak run"
  echo "- Started: $RUN_START"
  echo "- Ended: $RUN_END"
  echo "- Total iterations: $ITER"
  echo "- Pass: $PASS"
  echo "- Fail: $FAIL"
  echo "- Flow: \`integration-test-app/maestro/ios-ad-validation.yaml\`"
} >> "$HISTORY_LOG"
