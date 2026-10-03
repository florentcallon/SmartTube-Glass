#!/usr/bin/env bash
# Prints the subjects (newest first, max 10) of the commits since the last glass release
# that change the app, ignoring commits touching only docs, *.md files or the mockup.
set -euo pipefail

prev=$(git describe --tags --match 'v*-glass.*' --abbrev=0 HEAD 2>/dev/null || true)
git log --first-parent --no-merges --format=%s "${prev:+$prev..}HEAD" -- . \
  ':(exclude)docs' ':(exclude)*.md' ':(exclude)glass/mockup' ':(exclude).superpowers' | head -n 10
