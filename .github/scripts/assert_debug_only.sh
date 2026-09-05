#!/usr/bin/env bash
# G-4.3 (field-trial pass; supersedes the D4 script): prove every debug-only
# field-trial class is compiled into the DEBUG APK and ABSENT from the RELEASE APK,
# by counting dex class-package descriptors.
# The retired D-pass diagnostics package (com/.../debug/) was deleted with the
# overlay that owned it; the field-trial guide lives in com/.../fieldtrial/ (debug
# source set). The only main-source footprint is the inert FieldTrialHost seam,
# which is compiled in both builds but unreachable in release (BuildConfig.DEBUG
# guards); the guide classes themselves must not exist in release dex at all.
# Usage: assert_debug_only.sh <debug.apk> <release.apk>
set -euo pipefail
debug_apk="$1"
release_apk="$2"
# every debug-only package prefix that must be dex-proven
needles=(
  'Lcom/alijafari/red/astronomy/fieldtrial/'
)

occurrences() {
  local apk="$1" needle="$2" d n=0
  for d in $(unzip -l "$apk" | awk '{print $4}' | grep -E '^classes[0-9]*\.dex$'); do
    n=$(( n + $(unzip -p "$apk" "$d" | LC_ALL=C grep -a -o "$needle" | wc -l) ))
  done
  echo "$n"
}

fail=0
for needle in "${needles[@]}"; do
  dbg=$(occurrences "$debug_apk" "$needle")
  rel=$(occurrences "$release_apk" "$needle")
  echo "dex occurrences of '$needle': debug=$dbg release=$rel"
  if [ "$dbg" -eq 0 ]; then
    echo "FAIL: debug APK does not contain '$needle' (expected >= 1)"
    fail=1
  fi
  if [ "$rel" -ne 0 ]; then
    echo "FAIL: RELEASE APK contains '$needle' (expected 0)"
    fail=2
  fi
done
if [ "$fail" -ne 0 ]; then exit "$fail"; fi
echo "PASS: every field-trial debug package present in debug build, provably absent from release build"
