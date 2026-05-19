#!/usr/bin/env bash
set -euo pipefail

HOURS="${1:-1}"
PROJECT_DIR="/home/haris/Projects/olx-automation"
UNIT="code-review-oneshot"
LOG="/tmp/code-review-$(date +%Y%m%d-%H%M%S).log"

PROMPT='Invoke the code-reviewer subagent via the Agent tool (subagent_type: code-reviewer) on the diff between the current branch and main. Determine the current branch with: git rev-parse --abbrev-ref HEAD. Fetch the diff with: git diff main...<current-branch>. Report the verdict.'

systemctl --user stop "${UNIT}.timer" 2>/dev/null || true
systemctl --user reset-failed "${UNIT}.service" 2>/dev/null || true

systemd-run --user \
  --on-active="${HOURS}h" \
  --unit="${UNIT}" \
  --working-directory="${PROJECT_DIR}" \
  bash -c "claude -p \"${PROMPT}\" --dangerously-skip-permissions > \"${LOG}\" 2>&1"

echo "Scheduled: ${UNIT}"
echo "Fires in:  ${HOURS}h"
echo "Log:       ${LOG}"
echo "Cancel:    systemctl --user stop ${UNIT}.timer"
