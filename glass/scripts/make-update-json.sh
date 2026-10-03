#!/usr/bin/env bash
# Usage: make-update-json.sh <versionName> <versionCode> <owner/repo> [changelog line...]
# Prints the update manifest read by SmartTube's AppVersionChecker.
set -euo pipefail

version_name="${1:?usage: make-update-json.sh <versionName> <versionCode> <repo> [changelog...]}"
version_code="${2:?missing versionCode}"
repo="${3:?missing repo}"
shift 3
base="https://github.com/${repo}/releases/download/latest"

json_string() {
  local s="$1" bs='\'
  s=${s//"$bs"/"$bs$bs"}
  s=${s//'"'/"$bs\""}
  s=${s//$'\t'/"${bs}t"}
  s=${s//$'\r'/}
  s=${s//$'\n'/"${bs}n"}
  printf '"%s"' "$s"
}

changelog=""
for line in "$@"; do
  [ -z "$changelog" ] || changelog+=", "
  changelog+=$(json_string "$line")
done

cat <<JSON
{
  "package": {
    "downloadUrl": "${base}/smarttube_glass.apk",
    "downloadUrlList_arm64-v8a": ["${base}/smarttube_glass_arm64-v8a.apk"],
    "downloadUrlList_armeabi-v7a": ["${base}/smarttube_glass_armeabi-v7a.apk"]
  },
  $(json_string "$version_name"): {
    "versionCode": ${version_code},
    "changelog": [${changelog}]
  }
}
JSON
