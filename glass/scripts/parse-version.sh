#!/usr/bin/env bash
# Usage: ./gradlew -q :smarttubetv:printGlassVersion | parse-version.sh <upstreamVersionName|versionCode|versionName>
# Extracts one field from the printGlassVersion line, ignoring anything else on stdin
# (a cold Gradle wrapper prints download progress even with -q). Exits 1 if the line is missing.
set -euo pipefail

field="${1:?usage: parse-version.sh <upstreamVersionName|versionCode|versionName>}"
line=$(tr -d '\r' | grep -E '^upstreamVersionName=' | tail -n 1 || true)
[ -n "$line" ] || { echo "printGlassVersion line not found" >&2; exit 1; }
value=$(printf '%s\n' "$line" | tr ' ' '\n' | sed -n "s/^${field}=//p")
[ -n "$value" ] || { echo "field $field not found in: $line" >&2; exit 1; }
echo "$value"
