#!/usr/bin/env bash
# Usage: check-overrides.sh [lockfile]                report upstream files changed since they were overridden
#        check-overrides.sh --add <path> [lockfile]   record the current hash of an upstream file
# Lockfile lines: "<sha1>  <upstream-path>"; lines starting with # are ignored.
# Hashes ignore CR so a Windows (CRLF) checkout matches the CI's LF checkout.
# Exit: 0 nothing changed, 2 some file changed or disappeared, 1 lockfile unreadable.
set -euo pipefail

hash_of() { tr -d '\r' < "$1" | sha1sum | cut -d' ' -f1; }

if [ "${1:-}" = "--add" ]; then
  path="${2:?usage: check-overrides.sh --add <path> [lockfile]}"
  lock="${3:-glass/overrides.lock}"
  printf '%s  %s\n' "$(hash_of "$path")" "$path" >> "$lock"
  exit 0
fi

lock="${1:-glass/overrides.lock}"
[ -r "$lock" ] || { echo "cannot read $lock" >&2; exit 1; }

changed=0
while read -r sum path; do
  case "$sum" in ''|'#'*) continue ;; esac
  path="${path#\*}" # sha1sum binary-mode marker (Git for Windows)
  if [ ! -f "$path" ] || [ "$(hash_of "$path")" != "$sum" ]; then
    echo "$path"
    changed=1
  fi
done < <(tr -d '\r' < "$lock")
[ "$changed" -eq 0 ] || exit 2
