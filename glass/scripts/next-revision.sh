#!/usr/bin/env bash
# Usage: next-revision.sh <upstreamVersionName> < tags
# Prints the next glass revision for that upstream version: the number of
# existing v<upstreamVersionName>-glass.<n> tags. Fails past 99.
set -euo pipefail

name="${1:?usage: next-revision.sh <upstreamVersionName>}"
escaped=$(printf '%s' "$name" | sed 's/[][\.*^$]/\\&/g')
count=$(tr -d '\r' | grep -cE "^v${escaped}-glass\.[0-9]+$" || true)
if [ "$count" -ge 100 ]; then
  echo "glass revision would exceed 99 for upstream $name" >&2
  exit 1
fi
echo "$count"
