#!/usr/bin/env bash
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT_DIR/maestro/common.sh"
OUT_DIR="$ROOT_DIR/maestro/results/android-soak-60m-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT_DIR"

# Crontab 환경을 위한 절대 경로 확보
export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
export ADB_BIN="/opt/homebrew/bin/adb"
export PATH="$JAVA_HOME/bin:$HOME/.maestro/bin:/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"
export MAESTRO_CLI_NO_ANALYTICS=1
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true

FLOW="$ROOT_DIR/maestro/android-ad-validation.yaml"
DURATION_SECONDS="${SOAK_DURATION_SECONDS:-3600}"
END_TS=$(( $(date +%s) + DURATION_SECONDS ))
ITER=0
PASS=0
FAIL=0
REPEAT_FAIL_COUNT=0
LAST_FAIL_KEY=""
STOP_REASON=""
SUMMARY="$OUT_DIR/summary.txt"
METRO_LOG="$OUT_DIR/metro.log"
ADB_LOG="$OUT_DIR/adb-status.txt"
HISTORY_LOG="$ROOT_DIR/maestro/maestro-soak-history.md"
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
echo "duration_seconds=$DURATION_SECONDS" | tee -a "$SUMMARY"

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

recover_android_maestro() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] android recovery: restarting maestro + adb/app bridge" | tee -a "$SUMMARY"
  pkill -f maestro >/dev/null 2>&1 || true
  "$ADB_BIN" kill-server >/dev/null 2>&1 || true
  "$ADB_BIN" start-server >/dev/null 2>&1 || true
  sleep 2
  "$ADB_BIN" wait-for-device >/dev/null 2>&1 || true
  "$ADB_BIN" -s emulator-5554 reverse --remove-all >/dev/null 2>&1 || true
  "$ADB_BIN" -s emulator-5554 reverse tcp:8081 tcp:8081 >/dev/null 2>&1 || true
  "$ADB_BIN" -s emulator-5554 shell am force-stop com.integrationtestapp >/dev/null 2>&1 || true
  sleep 2
  lsof -i :7001 >> "$ADB_LOG" 2>&1 || true
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
recover_android_maestro
record_env

while [ "$(date +%s)" -lt "$END_TS" ]; do
  ITER=$((ITER + 1))
  TS="$(date '+%Y-%m-%d %H:%M:%S')"
  LOG="$OUT_DIR/run-${ITER}.log"
  echo "[$TS] iteration=$ITER starting" | tee -a "$SUMMARY"
  "$ADB_BIN" -s emulator-5554 reverse tcp:8081 tcp:8081 >/dev/null 2>&1 || true
  "$ADB_BIN" -s emulator-5554 shell am force-stop com.integrationtestapp >/dev/null 2>&1 || true
  "$ADB_BIN" -s emulator-5554 shell am start -n com.integrationtestapp/.MainActivity >/dev/null 2>&1 || true
  sleep 2
  set +e
  maestro test \
    --debug-output "$OUT_DIR/debug-$ITER" \
    --flatten-debug-output \
    --test-output-dir "$OUT_DIR/test-output-$ITER" \
    "$FLOW" >"$LOG" 2>&1
  CMD_EXIT=$?
  set -e
  echo "[$TS] iteration=$ITER maestro_exit=$CMD_EXIT" >> "$SUMMARY"
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
    "$ADB_BIN" -s emulator-5554 logcat -d -t 200 > "$OUT_DIR/logcat-fail-${ITER}.txt" 2>&1 || true
    "$ADB_BIN" -s emulator-5554 exec-out screencap -p > "$OUT_DIR/fail-${ITER}.png" 2>/dev/null || true
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

# Slack Notification
SLACK_MSG="*Result:* PASS=$PASS / FAIL=$FAIL (Total $ITER)\n*Log:* $OUT_DIR"
if [ -n "$STOP_REASON" ]; then
  SLACK_MSG="$SLACK_MSG\n*Stop Reason:* $STOP_REASON"
fi

if [ "$FAIL" -gt 0 ]; then
  send_slack_notification "Android" "FAIL" "$SLACK_MSG"
else
  send_slack_notification "Android" "PASS" "$SLACK_MSG"
fi
