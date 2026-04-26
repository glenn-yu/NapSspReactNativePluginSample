#!/usr/bin/env bash

# Slack Webhook URL (필요 시 환경변수로 설정하거나 직접 기입)
SLACK_WEBHOOK_URL="${MAESTRO_SLACK_WEBHOOK_URL:-""}"

send_slack_notification() {
  local platform="$1"
  local status="$2"
  local message="$3"
  
  if [ -z "$SLACK_WEBHOOK_URL" ]; then
    echo "[Skip Slack] Webhook URL not set."
    return 0
  fi

  local color="#36a64f" # green
  if [ "$status" = "FAIL" ]; then
    color="#FF0000" # red
  fi

  payload=$(cat <<EOF
{
  "attachments": [
    {
      "fallback": "Maestro Soak Test Result: $status",
      "color": "$color",
      "title": "🎭 Maestro Soak Test [$platform] - $status",
      "text": "$message",
      "footer": "Maestro Automation",
      "ts": $(date +%s)
    }
  ]
}
EOF
)

  curl -X POST -H 'Content-type: application/json' --data "$payload" "$SLACK_WEBHOOK_URL" >/dev/null 2>&1 || true
}
