#!/usr/bin/env bash
# Device-check setup and diagnostics. Do not relaunch the app after a crash signal.
set -euo pipefail

run_stage() {
  local stage="$1"
  shift
  local logfile="$RUNNER_TEMP/chal-stage-output.log"
  set +e
  "$@" >"$logfile" 2>&1
  local status=$?
  set -e
  cat "$logfile"
  if [ "$status" -ne 0 ]; then
    python3 - "$stage" "$logfile" <<'PY'
import pathlib, sys
text = pathlib.Path(sys.argv[2]).read_text(errors="replace")[-5000:]
text = text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
print(f"::error title=Chal device setup::{sys.argv[1]} failed: {text}")
PY
    echo "No app relaunch was attempted."
    exit "$status"
  fi
}

# Use Homebrew's explicit resource path rather than relying on Swift Bundle.module discovery.
bridge_apk="$(brew --prefix sim-use)/libexec/SimUse_AndroidBackend.bundle/Resources/sim-use-device-bridge.apk"
if [ -f "$bridge_apk" ]; then
  run_stage "initialize sim-use Android bridge" sim-use android init --device emulator-5554 --apk-path "$bridge_apk"
else
  run_stage "initialize sim-use Android bridge" sim-use android init --device emulator-5554
fi
run_stage "learned skill preflight" python3 "$RUNNER_TEMP/sim-use/skills/sim-use/scripts/preflight.py" --device emulator-5554

# Build only after bridge/device preflight succeeds; this avoids a costly build on a blocked device.
run_stage "build verification APK" ./gradlew assembleDebug -Pgravity.ci.tests=false --no-daemon
apk="$(find app/build/outputs/apk/debug -maxdepth 1 -type f -name '*.apk' ! -name '*androidTest*' | head -n 1)"
test -n "$apk"
run_stage "install debug APK" adb install -r "$apk"
run_stage "prepare pixel verification" python3 -m venv "$RUNNER_TEMP/chal-device-python"
run_stage "install pixel verifier" "$RUNNER_TEMP/chal-device-python/bin/pip" install --quiet Pillow
run_stage "initial app launch" adb shell am start -W -n com.alijafari.red.astronomy/.MainActivity
sleep 8
run_stage "observe-act-verify Chal" "$RUNNER_TEMP/chal-device-python/bin/python" .github/scripts/chal_sim_use_check.py --device emulator-5554 --output "$RUNNER_TEMP/chal-device-images"
