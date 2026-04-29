#!/usr/bin/env bash
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT_DIR/maestro/common.sh"
OUT_DIR="$ROOT_DIR/maestro/results/android-adb-soak-60m-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT_DIR"

# Crontab 환경을 위한 절대 경로 확보
export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
export ADB_BIN="/opt/homebrew/bin/adb"
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"

END_TS=$(( $(date +%s) + 3600 ))
ITER=0
PASS=0
FAIL=0
REPEAT_FAIL_COUNT=0
LAST_FAIL_KEY=""
STOP_REASON=""
SUMMARY="$OUT_DIR/summary.txt"
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

echo "platform=android-adb" | tee -a "$SUMMARY"
echo "out_dir=$OUT_DIR" | tee -a "$SUMMARY"
echo "start_time=$RUN_START" | tee -a "$SUMMARY"

record_env() {
  {
    echo "===== $(date '+%Y-%m-%d %H:%M:%S') ====="
    "$ADB_BIN" devices
    "$ADB_BIN" -s emulator-5554 shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' || true
  } >> "$ADB_LOG" 2>&1
}

ui_dump() {
  local target="$1"
  "$ADB_BIN" -s emulator-5554 shell uiautomator dump /sdcard/uidump.xml >/dev/null 2>&1
  "$ADB_BIN" -s emulator-5554 shell cat /sdcard/uidump.xml > "$target"
}

center_for_label() {
  local xml="$1"
  local label="$2"
  python3 - <<'PY' "$xml" "$label"
import re, sys
xml=open(sys.argv[1], 'r', encoding='utf-8').read()
label=sys.argv[2]
patterns = [
    re.compile(r'content-desc="%s".*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % re.escape(label)),
    re.compile(r'text="%s".*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % re.escape(label)),
]
for pat in patterns:
    m = pat.search(xml)
    if m:
        x1,y1,x2,y2=map(int,m.groups())
        print((x1+x2)//2, (y1+y2)//2)
        sys.exit(0)
sys.exit(1)
PY
}

has_text() {
  local xml="$1"
  local needle="$2"
  grep -q "$needle" "$xml"
}

wait_for_text() {
  local xml_target="$1"
  local needle="$2"
  local timeout_seconds="${3:-15}"
  local interval="${4:-1}"
  local elapsed=0

  while [ "$elapsed" -lt "$timeout_seconds" ]; do
    ui_dump "$xml_target"
    if has_text "$xml_target" "$needle"; then
      return 0
    fi
    sleep "$interval"
    elapsed=$((elapsed + interval))
  done

  return 1
}

wait_for_success_after_tap() {
  local xml_before="$1"
  local xml_target="$2"

  sleep 10
  ui_dump "$xml_target"
  if has_text "$xml_target" 'SDK: success'; then
    return 0
  fi

  if has_text "$xml_target" 'SDK: idle'; then
    tap_label "$xml_before" 'SDK 초기화' || true
    sleep 10
    ui_dump "$xml_target"
    if has_text "$xml_target" 'SDK: success'; then
      return 0
    fi
  fi

  sleep 5
  ui_dump "$xml_target"
  has_text "$xml_target" 'SDK: success'
}

tap_label() {
  local xml="$1"
  local label="$2"
  local coords x y

  if [ "$label" = 'SDK 초기화' ]; then
    "$ADB_BIN" -s emulator-5554 shell input tap 540 1777 >/dev/null 2>&1
    return 0
  fi

  coords=$(center_for_label "$xml" "$label") || return 1
  x=$(echo "$coords" | awk '{print $1}')
  y=$(echo "$coords" | awk '{print $2}')
  "$ADB_BIN" -s emulator-5554 shell input tap "$x" "$y" >/dev/null 2>&1
}

classify_failure() {
  local xml="$1"
  if ! has_text "$xml" 'SDK 초기화'; then
    echo 'android_missing_init_button'
  elif has_text "$xml" 'SDK: failed'; then
    echo 'android_sdk_failed'
  elif ! has_text "$xml" 'SDK: success'; then
    echo 'android_init_not_success'
  elif ! has_text "$xml" '이벤트 로그'; then
    echo 'android_event_log_not_visible'
  else
    echo 'android_other_failure'
  fi
}

record_env

while [ "$(date +%s)" -lt "$END_TS" ]; do
  ITER=$((ITER + 1))
  TS="$(date '+%Y-%m-%d %H:%M:%S')"
  XML_BEFORE="$OUT_DIR/ui-before-${ITER}.xml"
  XML_AFTER_INIT="$OUT_DIR/ui-after-init-${ITER}.xml"
  echo "[$TS] iteration=$ITER starting" | tee -a "$SUMMARY"

  "$ADB_BIN" wait-for-device >/dev/null 2>&1 || true
  "$ADB_BIN" -s emulator-5554 reverse tcp:8081 tcp:8081 >/dev/null 2>&1 || true
  "$ADB_BIN" -s emulator-5554 shell am force-stop com.integrationtestapp >/dev/null 2>&1 || true
  "$ADB_BIN" -s emulator-5554 shell pm clear com.integrationtestapp >/dev/null 2>&1 || true
  "$ADB_BIN" -s emulator-5554 shell am start -n com.integrationtestapp/.MainActivity >/dev/null 2>&1 || true

  if ! wait_for_text "$XML_BEFORE" 'SDK 초기화' 20 2; then
    FAIL=$((FAIL + 1))
    FAIL_KEY='android_missing_init_button'
  else
    tap_label "$XML_BEFORE" 'SDK 초기화' || true
    if wait_for_success_after_tap "$XML_BEFORE" "$XML_AFTER_INIT"; then
      PASS=$((PASS + 1))
      REPEAT_FAIL_COUNT=0
      LAST_FAIL_KEY=""
      echo "[$TS] iteration=$ITER result=PASS" | tee -a "$SUMMARY"
      sleep 5
      continue
    else
      FAIL=$((FAIL + 1))
      FAIL_KEY="$(classify_failure "$XML_AFTER_INIT")"
    fi
  fi

  if [ "$FAIL_KEY" = "$LAST_FAIL_KEY" ]; then
    REPEAT_FAIL_COUNT=$((REPEAT_FAIL_COUNT + 1))
  else
    LAST_FAIL_KEY="$FAIL_KEY"
    REPEAT_FAIL_COUNT=1
  fi
  echo "[$TS] iteration=$ITER result=FAIL key=$FAIL_KEY repeat=$REPEAT_FAIL_COUNT" | tee -a "$SUMMARY"
  "$ADB_BIN" -s emulator-5554 exec-out screencap -p > "$OUT_DIR/fail-${ITER}.png" 2>/dev/null || true
  "$ADB_BIN" -s emulator-5554 logcat -d -t 200 > "$OUT_DIR/logcat-fail-${ITER}.txt" 2>&1 || true

  if [ "$REPEAT_FAIL_COUNT" -ge 3 ]; then
    STOP_REASON="repeat_failure_threshold:$FAIL_KEY"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] stopping early due to repeated failure key=$FAIL_KEY count=$REPEAT_FAIL_COUNT" | tee -a "$SUMMARY"
    break
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
  echo "## $RUN_START Android adb soak run"
  echo "- Started: $RUN_START (Asia/Seoul)"
  echo "- Ended: $RUN_END (Asia/Seoul)"
  echo "- Total iterations: $ITER"
  echo "- Pass: $PASS"
  echo "- Fail: $FAIL"
  echo "- Mode: \`adb + uiautomator dump + logcat\`"
  echo "- Output directory: \`$OUT_DIR\`"
  echo "- Summary: \`$SUMMARY\`"
  if [ -n "$STOP_REASON" ]; then
    echo "- Stop reason: \`$STOP_REASON\`"
  fi
} >> "$HISTORY_LOG"

# Slack Notification
SLACK_MSG="*Mode:* ADB Fallback\n*Result:* PASS=$PASS / FAIL=$FAIL (Total $ITER)\n*Log:* $OUT_DIR"
if [ -n "$STOP_REASON" ]; then
  SLACK_MSG="$SLACK_MSG\n*Stop Reason:* $STOP_REASON"
fi

if [ "$FAIL" -gt 0 ]; then
  send_slack_notification "Android-ADB" "FAIL" "$SLACK_MSG"
else
  send_slack_notification "Android-ADB" "PASS" "$SLACK_MSG"
fi
