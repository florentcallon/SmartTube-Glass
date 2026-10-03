#!/usr/bin/env bash
# Decides whether HEAD needs a new release. Run inside the repo, with tags fetched.
# Exit 0: release. Exit 1: skip (HEAD already released, or only docs changed since the last release).
set -euo pipefail

if git tag --points-at HEAD | grep -q -- '-glass\.'; then
  echo "skip: HEAD is already released ($(git tag --points-at HEAD | tr '\n' ' '))"
  exit 1
fi

prev=$(git describe --tags --match 'v*-glass.*' --abbrev=0 HEAD 2>/dev/null || true)
if [ -z "$prev" ]; then
  echo "release: no previous glass release"
  exit 0
fi

if git diff --quiet "$prev" HEAD -- . \
    ':(exclude)docs' ':(exclude)*.md' ':(exclude)glass/mockup' ':(exclude).superpowers'; then
  echo "skip: only documentation changed since $prev"
  exit 1
fi

echo "release: app changes since $prev"
exit 0
