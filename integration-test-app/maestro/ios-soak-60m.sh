#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/maestro/results/ios-soak-60m-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT_DIR"

export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH:$HOME/.maestro/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"
export MAESTRO_CLI_NO_ANALYTICS=1
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true

FLOW="$ROOT_DIR/maestro/ios-ad-validation.yaml"
SIM_UDID="8D90B616-14A9-4A49-A1A7-0470FF80A9F9"
END_TS=$(( $(date +%s) + 3600 ))
ITER=0
PASS=0
FAIL=0
SUMMARY="$OUT_DIR/summary.txt"
METRO_LOG="$OUT_DIR/metro.log"
SIM_LOG="$OUT_DIR/sim-status.txt"
HISTORY_LOG="$ROOT_DIR/maestro/results/maestro-soak-history.md"
RUN_START="$(date '+%Y-%m-%d %H:%M:%S')"
: > "$SUMMARY"
: > "$SIM_LOG"

mkdir -p "$(dirname "$HISTORY_LOG")"
if [ ! -f "$HISTORY_LOG" ]; then
  cat > "$HISTORY_LOG" <<'EOF'
# Maestro Soak Test History

This file accumulates Maestro soak-test results across runs.
EOF
fi

echo "platform=ios" | tee -a "$SUMMARY"
echo "out_dir=$OUT_DIR" | tee -a "$SUMMARY"
echo "flow=$FLOW" | tee -a "$SUMMARY"
echo "sim_udid=$SIM_UDID" | tee -a "$SUMMARY"
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

ensure_simulator() {
  xcrun simctl boot "$SIM_UDID" >/dev/null 2>&1 || true
  xcrun simctl install "$SIM_UDID" /tmp/NapSspIntegrationDerivedData/Build/Products/Debug-iphonesimulator/IntegrationTestApp.app >/dev/null 2>&1 || true
}

record_env() {
  {
    echo "===== $(date '+%Y-%m-%d %H:%M:%S') ====="
    xcrun simctl list devices "$SIM_UDID"
    xcrun simctl launch "$SIM_UDID" org.reactjs.native.example.IntegrationTestApp || true
  } >> "$SIM_LOG" 2>&1
}

ensure_metro
ensure_simulator
record_env

while [ "$(date +%s)" -lt "$END_TS" ]; do
  ITER=$((ITER + 1))
  TS="$(date '+%Y-%m-%d %H:%M:%S')"
  LOG="$OUT_DIR/run-${ITER}.log"
  echo "[$TS] iteration=$ITER starting" | tee -a "$SUMMARY"
  if maestro --device "$SIM_UDID" test "$FLOW" >"$LOG" 2>&1; then
    PASS=$((PASS + 1))
    echo "[$TS] iteration=$ITER result=PASS" | tee -a "$SUMMARY"
  else
    FAIL=$((FAIL + 1))
    echo "[$TS] iteration=$ITER result=FAIL" | tee -a "$SUMMARY"
    tail -n 80 "$LOG" | sed 's/^/  /' | tee -a "$SUMMARY"
    xcrun simctl io "$SIM_UDID" screenshot "$OUT_DIR/fail-${ITER}.png" >/dev/null 2>&1 || true
  fi
  sleep 5
done

RUN_END="$(date '+%Y-%m-%d %H:%M:%S')"
echo "end_time=$RUN_END" | tee -a "$SUMMARY"
echo "total_iterations=$ITER pass=$PASS fail=$FAIL" | tee -a "$SUMMARY"

{
  echo ""
  echo "## $RUN_START iOS soak run"
  echo "- Started: $RUN_START (Asia/Seoul)"
  echo "- Ended: $RUN_END (Asia/Seoul)"
  echo "- Total iterations: $ITER"
  echo "- Pass: $PASS"
  echo "- Fail: $FAIL"
  echo "- Flow: \`integration-test-app/maestro/ios-ad-validation.yaml\`"
  echo "- Simulator UDID: \`$SIM_UDID\`"
  echo "- Output directory: \`$OUT_DIR\`"
  echo "- Summary: \`$SUMMARY\`"
  if [ "$FAIL" -gt 0 ]; then
    echo "- Failure artifacts present: yes"
  else
    echo "- Failure artifacts present: no"
  fi
} >> "$HISTORY_LOG"
