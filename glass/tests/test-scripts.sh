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

# --- parse-version.sh ---
# A cold Gradle wrapper prints download progress on stdout even with -q.
noisy=$'Downloading https://services.gradle.org/distributions/gradle-7.5-bin.zip\n..........\nUnzipping /tmp/gradle-7.5-bin.zip\n\nupstreamVersionName=32.56 versionCode=244607 versionName=32.56-glass.7\n'
assert_eq parse_version_upstream_name "32.56" "$(printf '%s' "$noisy" | bash "$SCRIPTS/parse-version.sh" upstreamVersionName)"
assert_eq parse_version_code "244607" "$(printf '%s' "$noisy" | bash "$SCRIPTS/parse-version.sh" versionCode)"
assert_eq parse_version_name "32.56-glass.7" "$(printf '%s' "$noisy" | bash "$SCRIPTS/parse-version.sh" versionName)"
out=$(printf 'BUILD FAILED\n' | bash "$SCRIPTS/parse-version.sh" versionCode 2>/dev/null); code=$?
assert_eq parse_version_missing "1:" "$code:$out"

# --- should-release.sh ---
repo=$(mktemp -d)
(
  cd "$repo" || exit 1
  git init -q . && git config user.email t@t && git config user.name t && git config commit.gpgsign false
  echo a > app.java && git add -A && git commit -qm code
  bash "$SCRIPTS/should-release.sh" > /dev/null; echo "should_release_first_build|0|$?"
  git tag v32.56-glass.0
  bash "$SCRIPTS/should-release.sh" > /dev/null; echo "should_release_head_already_tagged|1|$?"
  mkdir -p docs glass/mockup && echo d > docs/plan.md && echo r > README.md && echo m > glass/mockup/index.html
  git add -A && git commit -qm docs
  bash "$SCRIPTS/should-release.sh" > /dev/null; echo "should_release_docs_only|1|$?"
  echo b >> app.java && git add -A && git commit -qm code2
  bash "$SCRIPTS/should-release.sh" > /dev/null; echo "should_release_code_changed|0|$?"
) > "$repo.results" 2>&1
while IFS='|' read -r name expected actual; do
  assert_eq "$name" "$expected" "$actual"
done < "$repo.results"
rm -rf "$repo" "$repo.results"

# --- open-issue.sh (with a fake gh on PATH) ---
fake=$(mktemp -d)
cat > "$fake/gh" <<'GH'
#!/usr/bin/env bash
echo "$*" >> "$FAKE_GH_LOG"
if [ "$1 $2" = "issue list" ]; then printf '%s' "${FAKE_OPEN_TITLES:-}"; fi
GH
chmod +x "$fake/gh"
export FAKE_GH_LOG="$fake/log"

: > "$FAKE_GH_LOG"
FAKE_OPEN_TITLES="" PATH="$fake:$PATH" bash "$SCRIPTS/open-issue.sh" "Sync upstream 32.60s : conflit" "body" > /dev/null
assert_eq open_issue_creates_when_none "1" "$(grep -c '^issue create' "$FAKE_GH_LOG")"

: > "$FAKE_GH_LOG"
FAKE_OPEN_TITLES=$'Other\nSync upstream 32.60s : conflit\n' PATH="$fake:$PATH" bash "$SCRIPTS/open-issue.sh" "Sync upstream 32.60s : conflit" "body" > /dev/null
assert_eq open_issue_skips_duplicate "0" "$(grep -c '^issue create' "$FAKE_GH_LOG")"

: > "$FAKE_GH_LOG"
FAKE_OPEN_TITLES=$'Sync upstream 32.60s : conflit (old)\n' PATH="$fake:$PATH" bash "$SCRIPTS/open-issue.sh" "Sync upstream 32.60s : conflit" "body" > /dev/null
assert_eq open_issue_exact_title_only "1" "$(grep -c '^issue create' "$FAKE_GH_LOG")"
rm -rf "$fake"

# --- workflows ---
for wf in build-release; do
  if grep -q 'chmod +x gradlew' "$ROOT/.github/workflows/$wf.yml"; then pass "${wf}_makes_gradlew_executable"
  else fail "${wf}_makes_gradlew_executable" "gradlew is 100644 in git; CI must chmod it"; fi
done

echo "---"
echo "$PASSED passed, $FAILED failed"
[ "$FAILED" -eq 0 ]
