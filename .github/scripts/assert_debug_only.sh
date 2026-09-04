#!/usr/bin/env bash
# D4 (debug-diagnostics pass): prove the debug-only screen is compiled into the DEBUG
# APK and ABSENT from the RELEASE APK, by counting its dex class-package descriptor.
# Usage: assert_debug_only.sh <debug.apk> <release.apk>
set -euo pipefail
debug_apk="$1"
release_apk="$2"
needle='Lcom/alijafari/red/astronomy/debug/'

occurrences() {
  local apk="$1" d n=0
  for d in $(unzip -l "$apk" | awk '{print $4}' | grep -E '^classes[0-9]*\.dex$'); do
    n=$(( n + $(unzip -p "$apk" "$d" | LC_ALL=C grep -a -o "$needle" | wc -l) ))
  done
  echo "$n"
}

dbg=$(occurrences "$debug_apk")
rel=$(occurrences "$release_apk")
echo "dex occurrences of '$needle': debug=$dbg release=$rel"

if [ "$dbg" -eq 0 ]; then
  echo "FAIL: debug APK does not contain the debug-only screen (expected >= 1)"
  exit 1
fi
if [ "$rel" -ne 0 ]; then
  echo "FAIL: RELEASE APK contains debug-only screen classes (expected 0)"
  exit 2
fi
echo "PASS: debug-only screen present in debug build, provably absent from release build"
