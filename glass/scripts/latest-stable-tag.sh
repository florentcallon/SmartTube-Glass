#!/usr/bin/env bash
# Reads tag names on stdin and prints the highest upstream stable tag (X.YYs).
# Exits 1 with no output when there is none.
set -euo pipefail

tag=$(tr -d '\r' | grep -E '^[0-9]+\.[0-9]+s$' | sort -V | tail -n 1 || true)
[ -n "$tag" ] || exit 1
echo "$tag"
