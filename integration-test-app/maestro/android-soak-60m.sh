#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/maestro/results/android-soak-60m-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT_DIR"

export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH:$HOME/.maestro/bin"
export MAESTRO_CLI_NO_ANALYTICS=1
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true

FLOW="$ROOT_DIR/maestro/android-ad-validation.yaml"
END_TS=$(( $(date +%s) + 3600 ))
ITER=0
PASS=0
FAIL=0
REPEAT_FAIL_COUNT=0
LAST_FAIL_KEY=""
STOP_REASON=""
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

echo "platform=android" | tee -a "$SUMMARY"
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
    adb devices
    adb -s emulator-5554 shell getprop ro.build.version.release || true
    adb -s emulator-5554 shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' || true
  } >> "$ADB_LOG" 2>&1
}

recover_android_maestro() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] android recovery: restarting adb/app bridge" | tee -a "$SUMMARY"
  adb kill-server >/dev/null 2>&1 || true
  adb start-server >/dev/null 2>&1 || true
  sleep 2
  adb wait-for-device >/dev/null 2>&1 || true
  adb -s emulator-5554 reverse --remove-all >/dev/null 2>&1 || true
  adb -s emulator-5554 reverse tcp:8081 tcp:8081 >/dev/null 2>&1 || true
  adb -s emulator-5554 shell am force-stop com.integrationtestapp >/dev/null 2>&1 || true
  sleep 2
  record_env
}

classify_android_failure() {
  local log="$1"
  if grep -q 'Connection refused: localhost.*:7001\|StatusRuntimeException: UNAVAILABLE\|io.grpc.StatusRuntimeException\|Command failed (tcp:7001): closed' "$log"; then
    echo 'maestro_android_transport'
  elif grep -q 'Unable to launch app com.integrationtestapp' "$log"; then
    echo 'android_launch_failed'
  elif grep -q 'Assert that "SDK 초기화" is visible' "$log"; then
    echo 'android_sdk_init_visibility'
  elif grep -q 'Assert that "이벤트 로그" is visible' "$log"; then
    echo 'android_event_log_visibility'
  else
    echo 'android_other_failure'
  fi
}

ensure_metro
record_env

while [ "$(date +%s)" -lt "$END_TS" ]; do
  ITER=$((ITER + 1))
  TS="$(date '+%Y-%m-%d %H:%M:%S')"
  LOG="$OUT_DIR/run-${ITER}.log"
  echo "[$TS] iteration=$ITER starting" | tee -a "$SUMMARY"
  adb -s emulator-5554 reverse tcp:8081 tcp:8081 >/dev/null 2>&1 || true
  adb -s emulator-5554 shell am force-stop com.integrationtestapp >/dev/null 2>&1 || true
  adb -s emulator-5554 shell am start -n com.integrationtestapp/.MainActivity >/dev/null 2>&1 || true
  sleep 2
  maestro test "$FLOW" >"$LOG" 2>&1 || true
  if grep -q 'Assert that "이벤트 로그" is visible... COMPLETED' "$LOG"; then
    PASS=$((PASS + 1))
    REPEAT_FAIL_COUNT=0
    LAST_FAIL_KEY=""
    echo "[$TS] iteration=$ITER result=PASS" | tee -a "$SUMMARY"
  else
    FAIL=$((FAIL + 1))
    FAIL_KEY="$(classify_android_failure "$LOG")"
    if [ "$FAIL_KEY" = "$LAST_FAIL_KEY" ]; then
      REPEAT_FAIL_COUNT=$((REPEAT_FAIL_COUNT + 1))
    else
      LAST_FAIL_KEY="$FAIL_KEY"
      REPEAT_FAIL_COUNT=1
    fi
    echo "[$TS] iteration=$ITER result=FAIL key=$FAIL_KEY repeat=$REPEAT_FAIL_COUNT" | tee -a "$SUMMARY"
    tail -n 80 "$LOG" | sed 's/^/  /' | tee -a "$SUMMARY"
    adb -s emulator-5554 logcat -d -t 200 > "$OUT_DIR/logcat-fail-${ITER}.txt" 2>&1 || true
    adb -s emulator-5554 exec-out screencap -p > "$OUT_DIR/fail-${ITER}.png" 2>/dev/null || true
    if [ "$FAIL_KEY" = 'maestro_android_transport' ] || [ "$FAIL_KEY" = 'android_launch_failed' ]; then
      recover_android_maestro
    fi
    if [ "$REPEAT_FAIL_COUNT" -ge 3 ]; then
      STOP_REASON="repeat_failure_threshold:$FAIL_KEY"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] stopping early due to repeated failure key=$FAIL_KEY count=$REPEAT_FAIL_COUNT" | tee -a "$SUMMARY"
      break
    fi
  fi
  sleep 5
done

RUN_END="$(date '+%Y-%m-%d %H:%M:%S')"
echo "end_time=$RUN_END" | tee -a "$SUMMARY"
if [ -n "$STOP_REASON" ]; then
  echo "stop_reason=$STOP_REASON" | tee -a "$SUMMARY"
fi
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
  if [ -n "$STOP_REASON" ]; then
    echo "- Stop reason: \`$STOP_REASON\`"
  fi
  if [ "$FAIL" -gt 0 ]; then
    echo "- Failure artifacts present: yes"
  else
    echo "- Failure artifacts present: no"
  fi
} >> "$HISTORY_LOG"
