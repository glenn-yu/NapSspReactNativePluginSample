#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/maestro/results"
mkdir -p "$OUT_DIR"

export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
export ADB_BIN="/opt/homebrew/bin/adb"
export PATH="$JAVA_HOME/bin:$HOME/.maestro/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"
export MAESTRO_CLI_NO_ANALYTICS=1
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true

FLOW="$ROOT_DIR/maestro/android-ad-validation.yaml"
END_TS=$(( $(date +%s) + 3600 ))
ITER=0
PASS=0
FAIL=0
SUMMARY="$OUT_DIR/summary.txt"
METRO_LOG="$OUT_DIR/metro.log"
: > "$SUMMARY"

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

while [ "$(date +%s)" -lt "$END_TS" ]; do
  ITER=$((ITER + 1))
  TS="$(date '+%Y-%m-%d %H:%M:%S')"
  LOG="$OUT_DIR/run-${ITER}.log"
  ensure_metro
  "$ADB_BIN" reverse tcp:8081 tcp:8081 >/dev/null 2>&1 || true
  echo "[$TS] iteration=$ITER starting" | tee -a "$SUMMARY"
  if maestro test "$FLOW" >"$LOG" 2>&1; then
    PASS=$((PASS + 1))
    echo "[$TS] iteration=$ITER result=PASS" | tee -a "$SUMMARY"
  else
    FAIL=$((FAIL + 1))
    echo "[$TS] iteration=$ITER result=FAIL" | tee -a "$SUMMARY"
    tail -n 40 "$LOG" | sed 's/^/  /' | tee -a "$SUMMARY"
  fi
  sleep 5
done

echo "total_iterations=$ITER pass=$PASS fail=$FAIL" | tee -a "$SUMMARY"
