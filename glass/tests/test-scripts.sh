#!/usr/bin/env bash
# Tests for the CI helper scripts in glass/scripts. Run: bash glass/tests/test-scripts.sh
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCRIPTS="$ROOT/glass/scripts"
PASSED=0
FAILED=0

pass() { PASSED=$((PASSED + 1)); echo "PASS $1"; }
fail() { FAILED=$((FAILED + 1)); echo "FAIL $1: $2"; }
assert_eq() { # name expected actual
  if [ "$2" = "$3" ]; then pass "$1"; else fail "$1" "expected [$2] got [$3]"; fi
}

# --- latest-stable-tag.sh ---
out=$(printf '32.10\n32.56s\n32.47s\n32.59\nbeta\nlatest\nnotification2\n32.100s\n' | bash "$SCRIPTS/latest-stable-tag.sh")
assert_eq latest_stable_picks_highest "32.100s" "$out"

out=$(printf '32.59\nbeta\n' | bash "$SCRIPTS/latest-stable-tag.sh"); code=$?
assert_eq latest_stable_none "1:" "$code:$out"

# --- next-revision.sh ---
out=$(printf 'v32.56-glass.0\nv32.56-glass.1\nv32.5-glass.0\nv32.56-glass.x\nv132.56-glass.0\n' | bash "$SCRIPTS/next-revision.sh" 32.56)
assert_eq next_revision_counts_exact_prefix "2" "$out"

out=$(for i in $(seq 0 99); do echo "v32.56-glass.$i"; done | bash "$SCRIPTS/next-revision.sh" 32.56 2>/dev/null); code=$?
assert_eq next_revision_overflow "1:" "$code:$out"

out=$(printf '' | bash "$SCRIPTS/next-revision.sh" 32.56)
assert_eq next_revision_first_is_zero "0" "$out"

# --- make-update-json.sh ---
json=$(bash "$SCRIPTS/make-update-json.sh" 32.56-glass.0 244600 florentcallon/SmartTube-Glass "Upstream 32.56s" 'Say "hi"')
out=$(printf '%s' "$json" | node -e '
  const j = JSON.parse(require("fs").readFileSync(0, "utf8"));
  const v = j["32.56-glass.0"], p = j.package;
  const ok = v.versionCode === 244600
    && v.changelog[0] === "Upstream 32.56s"
    && v.changelog[1] === "Say \"hi\""
    && p["downloadUrlList_arm64-v8a"][0] === "https://github.com/florentcallon/SmartTube-Glass/releases/download/latest/smarttube_glass_arm64-v8a.apk"
    && p["downloadUrlList_armeabi-v7a"][0] === "https://github.com/florentcallon/SmartTube-Glass/releases/download/latest/smarttube_glass_armeabi-v7a.apk"
    && p.downloadUrl === "https://github.com/florentcallon/SmartTube-Glass/releases/download/latest/smarttube_glass.apk";
  console.log(ok ? "ok" : JSON.stringify(j));
' 2>&1)
assert_eq update_json_shape "ok" "$out"

json=$(bash "$SCRIPTS/make-update-json.sh" 32.56-glass.3 244603 florentcallon/SmartTube-Glass)
out=$(printf '%s' "$json" | node -e '
  const j = JSON.parse(require("fs").readFileSync(0, "utf8"));
  console.log(Array.isArray(j["32.56-glass.3"].changelog) && j["32.56-glass.3"].changelog.length === 0 ? "ok" : JSON.stringify(j));
' 2>&1)
assert_eq update_json_no_changelog "ok" "$out"

json=$(bash "$SCRIPTS/make-update-json.sh" 32.56-glass.0 244600 r/r 'back\slash')
out=$(printf '%s' "$json" | node -e '
  const j = JSON.parse(require("fs").readFileSync(0, "utf8"));
  console.log(j["32.56-glass.0"].changelog[0] === "back\\slash" ? "ok" : JSON.stringify(j));
' 2>&1)
assert_eq update_json_escapes_backslash "ok" "$out"

# --- check-overrides.sh ---
tmp=$(mktemp -d)
(
  cd "$tmp" || exit 1
  echo '<layout/>' > a.xml
  { echo '# sha1  upstream-path'; sha1sum a.xml; } > lock

  out=$(bash "$SCRIPTS/check-overrides.sh" lock); code=$?
  echo "overrides_clean|0:|$code:$out"

  echo '<layout id="x"/>' > a.xml
  out=$(bash "$SCRIPTS/check-overrides.sh" lock); code=$?
  echo "overrides_changed|2:a.xml|$code:$out"

  rm a.xml
  out=$(bash "$SCRIPTS/check-overrides.sh" lock 2>/dev/null); code=$?
  echo "overrides_missing|2:a.xml|$code:$out"

  echo '# sha1  upstream-path' > only-comments
  out=$(bash "$SCRIPTS/check-overrides.sh" only-comments); code=$?
  echo "overrides_comments_ignored|0:|$code:$out"

  out=$(bash "$SCRIPTS/check-overrides.sh" does-not-exist 2>/dev/null); code=$?
  echo "overrides_unreadable_lockfile|1:|$code:$out"

  # A file checked out with CRLF on Windows must hash like its LF copy on the CI runner.
  printf '<a/>\r\n<b/>\r\n' > crlf.xml
  : > lock2
  bash "$SCRIPTS/check-overrides.sh" --add crlf.xml lock2
  printf '<a/>\n<b/>\n' > crlf.xml
  out=$(bash "$SCRIPTS/check-overrides.sh" lock2); code=$?
  echo "overrides_add_ignores_crlf|0:|$code:$out"
) > "$tmp/results"
while IFS='|' read -r name expected actual; do
  assert_eq "$name" "$expected" "$actual"
done < "$tmp/results"
rm -rf "$tmp"

echo "---"
echo "$PASSED passed, $FAILED failed"
[ "$FAILED" -eq 0 ]
