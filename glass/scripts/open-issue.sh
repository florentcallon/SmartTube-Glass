#!/usr/bin/env bash
# Usage: open-issue.sh <title> <body>
# Opens a GitHub issue unless an open issue with exactly this title already exists,
# so a problem that persists across daily runs is reported once. Needs gh + GH_TOKEN.
set -euo pipefail

title="${1:?usage: open-issue.sh <title> <body>}"
body="${2:-}"

if gh issue list --state open --limit 200 --json title --jq '.[].title' | tr -d '\r' | grep -qxF -- "$title"; then
  echo "issue already open: $title"
  exit 0
fi
gh issue create --title "$title" --body "$body"
