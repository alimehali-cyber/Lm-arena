#!/usr/bin/env bash
# Runs inside android-emulator-runner (the emulator is already booted). The debug APK and the
# Pillow virtualenv are prepared by earlier workflow steps, so Gradle never competes with the
# emulator for CPU. sim-use crash protocol: after a crash signal STOP and report; never relaunch.
set -euo pipefail
cd "$(dirname "$0")/../.."

images="$RUNNER_TEMP/chal-device-images"
mkdir -p "$images"
real_adb="$(command -v adb || echo "${ANDROID_SDK_ROOT:-}/platform-tools/adb")"

diagnostics() {
  local status=$?
  if [ "$status" -ne 0 ]; then
    echo "::group::Diagnostics (read-only; the app is NOT relaunched)"
    "$real_adb" logcat -d -b crash 2>/dev/null | tail -n 150 || true
    "$real_adb" logcat -d 2>/dev/null | grep -E "FATAL EXCEPTION|AndroidRuntime|Chal|libblackhole|Vulkan" | tail -n 200 || true
    echo "--- accessibility / bridge state ---"
    "$real_adb" shell settings get secure accessibility_enabled 2>/dev/null || true
    "$real_adb" shell settings get secure enabled_accessibility_services 2>/dev/null || true
    "$real_adb" shell pm list packages com.linecorp.simuse.devicebridge 2>/dev/null || true
    "$real_adb" shell pidof com.linecorp.simuse.devicebridge 2>/dev/null || echo "bridge process is not running"
    "$real_adb" logcat -d 2>/dev/null | grep -iE "simuse|devicebridge" | tail -n 60 || true
    echo "::endgroup::"
  fi
  exit "$status"
}
trap diagnostics EXIT

# Streams output live (a timeout must not swallow the log) and turns a failure into an annotation.
run_stage() {
  local stage="$1"
  shift
  local logfile="$RUNNER_TEMP/chal-stage-output.log"
  echo "::group::${stage}"
  set +e
  "$@" 2>&1 | tee "$logfile"
  local status="${PIPESTATUS[0]}"
  set -e
  echo "::endgroup::"
  if [ "$status" -ne 0 ]; then
    python3 - "$stage" "$logfile" <<'PY'
import pathlib, sys
text = pathlib.Path(sys.argv[2]).read_text(errors="replace")[-4000:]
text = text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
print(f"::error title=Chal device check::{sys.argv[1]} failed: {text}")
PY
    echo "No app relaunch was attempted."
    exit "$status"
  fi
}

run_stage "emulator is ready" bash -c '
  "$0" wait-for-device
  until [ "$("$0" shell getprop sys.boot_completed | tr -d "\r")" = "1" ]; do sleep 2; done
  "$0" shell wm dismiss-keyguard || true
' "$real_adb"

bridge_apk="$(brew --prefix sim-use)/libexec/SimUse_AndroidBackend.bundle/Resources/sim-use-device-bridge.apk"
test -f "$bridge_apk"
chmod +x .github/scripts/chal_adb_wrapper.sh

# A cold CI emulator can spend over a minute optimizing the first installed package, which exceeds
# sim-use's fixed 60 s adb-install timeout. Install once here without that limit; the wrapper then
# lets `init` skip only its redundant re-install of the identical APK. Everything else is real adb.
run_stage "install sim-use bridge (no CLI timeout)" "$real_adb" install --no-streaming -r -g "$bridge_apk"
# The first bootstrap on a cold emulator can lose a race: Android registers a freshly installed
# accessibility service asynchronously, and sim-use then reports an opaque "network connection was
# lost". `init` is idempotent (the wrapper makes its re-install a no-op), so settle, then retry.
init_bridge() {
  local attempt
  for attempt in 1 2 3 4 5; do
    echo "sim-use android init: attempt ${attempt}/5"
    if env SIM_USE_ADB="$PWD/.github/scripts/chal_adb_wrapper.sh" CHAL_REAL_ADB="$real_adb" \
        sim-use android init --device emulator-5554 --apk-path "$bridge_apk"; then
      return 0
    fi
    echo "init failed; letting the system settle before retrying"
    sleep 20
  done
  return 1
}
run_stage "let the freshly installed bridge package settle" sleep 20
run_stage "sim-use android init (idempotent, with retries)" init_bridge
run_stage "learned-skill preflight" python3 "$RUNNER_TEMP/sim-use/skills/sim-use/scripts/preflight.py" --device emulator-5554

apk="$(find app/build/outputs/apk/debug -maxdepth 1 -type f -name '*.apk' ! -name '*androidTest*' | head -n 1)"
test -n "$apk"
run_stage "install debug APK" "$real_adb" install -r "$apk"
run_stage "launch app once" "$real_adb" shell am start -W -n com.alijafari.red.astronomy/.MainActivity
sleep 8
run_stage "observe, act and verify Chal with sim-use" "$RUNNER_TEMP/chal-device-python/bin/python" \
  .github/scripts/chal_sim_use_check.py --device emulator-5554 --output "$images"
