#!/usr/bin/env bash
# Checks the stglass version derivation. Run from the repo root: bash glass/tests/test-version.sh
set -uo pipefail

cd "$(dirname "$0")/../.."
FAILED=0
fail() { FAILED=$((FAILED + 1)); echo "FAIL $1"; }
pass() { echo "PASS $1"; }

# Expected values follow upstream's defaultConfig, so the test stays valid after each sync.
UP_CODE=$(grep -E '^\s*versionCode [0-9]+' smarttubetv/build.gradle | head -n 1 | grep -oE '[0-9]+')
UP_NAME=$(grep -E '^\s*versionName "' smarttubetv/build.gradle | head -n 1 | sed -E 's/.*"(.*)".*/\1/')

out=$(./gradlew -q :smarttubetv:printGlassVersion 2>&1 | tr -d '\r')
expected="upstreamVersionName=$UP_NAME versionCode=$((UP_CODE * 100)) versionName=$UP_NAME-glass.0"
[ "$out" = "$expected" ] && pass rev0_default || fail "rev0_default: expected [$expected] got [$out]"

out=$(./gradlew -q :smarttubetv:printGlassVersion -PglassRevision=7 2>&1 | tr -d '\r')
expected="upstreamVersionName=$UP_NAME versionCode=$((UP_CODE * 100 + 7)) versionName=$UP_NAME-glass.7"
[ "$out" = "$expected" ] && pass rev7 || fail "rev7: expected [$expected] got [$out]"

out=$(./gradlew -q :smarttubetv:printGlassVersion -PglassRevision=100 2>&1)
if echo "$out" | grep -qF "glassRevision must be in [0, 99]"; then pass rev100_rejected; else fail "rev100_rejected: no range error in output"; fi

out=$(./gradlew -q :smarttubetv:printGlassVersion -PglassRevision=-1 2>&1)
if echo "$out" | grep -qF "glassRevision must be in [0, 99]"; then pass negative_rejected; else fail "negative_rejected: no range error in output"; fi

echo "---"
[ "$FAILED" -eq 0 ] && echo "all passed" || echo "$FAILED failed"
[ "$FAILED" -eq 0 ]
