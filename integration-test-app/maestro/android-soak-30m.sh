#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/maestro/results/soak-30m-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT_DIR"

export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
export ADB_BIN="/opt/homebrew/bin/adb"
export PATH="$JAVA_HOME/bin:$HOME/.maestro/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"
export MAESTRO_CLI_NO_ANALYTICS=1
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true

FLOW="$ROOT_DIR/maestro/android-ad-validation.yaml"
END_TS=$(( $(date +%s) + 1800 ))
ITER=0
PASS=0
FAIL=0
SUMMARY="$OUT_DIR/summary.txt"
METRO_LOG="$OUT_DIR/metro.log"
ADB_LOG="$OUT_DIR/adb-status.txt"
HISTORY_LOG="$ROOT_DIR/maestro/results/maestro-soak-history.md"
RUN_START="$(date '+%Y-%m-%d %H:%M:%S')"
: > "$SUMMARY"
: > "$ADB_LOG"

mkdir -p "$(dirname "$HISTORY_LOG")"
if [ ! -f "$HISTORY_LOG" ]; then
  cat > "$HISTORY_LOG" <<'EOF'
# Maestro Soak Test History

This file accumulates Maestro soak-test results across runs.
EOF
fi

echo "out_dir=$OUT_DIR" | tee -a "$SUMMARY"
echo "flow=$FLOW" | tee -a "$SUMMARY"
echo "start_time=$RUN_START" | tee -a "$SUMMARY"

ensure_metro() {
  if ! lsof -ti tcp:8081 >/dev/null 2>&1; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] starting Metro on :8081" | tee -a "$SUMMARY"
    (
      cd "$ROOT_DIR"
      nohup npx react-native start --port 8081 >"$METRO_LOG" 2>&1 &
    )
    sleep 10
  fi
}

record_env() {
  {
    echo "===== $(date '+%Y-%m-%d %H:%M:%S') ====="
    "$ADB_BIN" devices
    "$ADB_BIN" -s emulator-5554 shell getprop ro.build.version.release || true
    "$ADB_BIN" -s emulator-5554 shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' || true
  } >> "$ADB_LOG" 2>&1
}

ensure_metro
record_env

while [ "$(date +%s)" -lt "$END_TS" ]; do
  ITER=$((ITER + 1))
  TS="$(date '+%Y-%m-%d %H:%M:%S')"
  LOG="$OUT_DIR/run-${ITER}.log"
  echo "[$TS] iteration=$ITER starting" | tee -a "$SUMMARY"
  "$ADB_BIN" -s emulator-5554 reverse tcp:8081 tcp:8081 >/dev/null 2>&1 || true
  "$ADB_BIN" -s emulator-5554 shell am force-stop com.integrationtestapp >/dev/null 2>&1 || true
  "$ADB_BIN" -s emulator-5554 shell am start -n com.integrationtestapp/com.integrationtestapp.MainActivity >/dev/null 2>&1 || true
  sleep 3
  if maestro test "$FLOW" >"$LOG" 2>&1; then
    PASS=$((PASS + 1))
    echo "[$TS] iteration=$ITER result=PASS" | tee -a "$SUMMARY"
  else
    FAIL=$((FAIL + 1))
    echo "[$TS] iteration=$ITER result=FAIL" | tee -a "$SUMMARY"
    tail -n 80 "$LOG" | sed 's/^/  /' | tee -a "$SUMMARY"
    "$ADB_BIN" -s emulator-5554 logcat -d -t 200 > "$OUT_DIR/logcat-fail-${ITER}.txt" 2>&1 || true
    "$ADB_BIN" -s emulator-5554 exec-out screencap -p > "$OUT_DIR/fail-${ITER}.png" 2>/dev/null || true
  fi
  sleep 5
done

RUN_END="$(date '+%Y-%m-%d %H:%M:%S')"
echo "end_time=$RUN_END" | tee -a "$SUMMARY"
echo "total_iterations=$ITER pass=$PASS fail=$FAIL" | tee -a "$SUMMARY"

{
  echo ""
  echo "## $RUN_START Android soak run"
  echo "- Started: $RUN_START (Asia/Seoul)"
  echo "- Ended: $RUN_END (Asia/Seoul)"
  echo "- Total iterations: $ITER"
  echo "- Pass: $PASS"
  echo "- Fail: $FAIL"
  echo "- Flow: \`integration-test-app/maestro/android-ad-validation.yaml\`"
  echo "- Output directory: \`$OUT_DIR\`"
  echo "- Summary: \`$SUMMARY\`"
  if [ "$FAIL" -gt 0 ]; then
    echo "- Failure artifacts present: yes"
  else
    echo "- Failure artifacts present: no"
  fi
} >> "$HISTORY_LOG"
