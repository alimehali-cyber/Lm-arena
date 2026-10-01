#!/usr/bin/env bash
# Used ONLY by the optional device check, through SIM_USE_ADB. Every adb command is forwarded
# unchanged, with a single exception: when sim-use asks to re-install its own bridge APK and the
# job already installed that identical APK (without sim-use's fixed 60-second install timeout),
# report success instead of paying the cold-emulator package-optimization cost a second time.
set -u
real_adb="${CHAL_REAL_ADB:?CHAL_REAL_ADB must name the real adb binary}"
bridge_package="com.linecorp.simuse.devicebridge"

args=("$@")
is_install=false
is_bridge_apk=false
serial=""
for ((i = 0; i < ${#args[@]}; i++)); do
  case "${args[i]}" in
    install) is_install=true ;;
    -s) serial="${args[i + 1]:-}" ;;
    *sim-use-device-bridge.apk) is_bridge_apk=true ;;
  esac
done

if $is_install && $is_bridge_apk && [ -n "$serial" ]; then
  if "$real_adb" -s "$serial" shell pm list packages "$bridge_package" 2>/dev/null | tr -d '\r' | grep -qx "package:${bridge_package}"; then
    echo "Success (bridge was already installed by this job from the same APK)"
    exit 0
  fi
fi
exec "$real_adb" "$@"
