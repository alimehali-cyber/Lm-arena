#!/usr/bin/env bash
# Device-check setup and diagnostics. Do not relaunch the app after a crash signal.
set -euo pipefail
stage="device setup"
trap 'status=$?; echo "::error title=Chal device check stage::${stage} failed with exit code ${status}. No app relaunch was attempted."; exit "$status"' ERR

stage="select debug APK"
apk="$(find app/build/outputs/apk/debug -maxdepth 1 -type f -name '*.apk' ! -name '*androidTest*' | head -n 1)"
test -n "$apk"
echo "Installing built APK: $apk"
stage="install debug APK"
adb install -r "$apk"

stage="initialize sim-use Android bridge"
sim-use android init --device emulator-5554
stage="learned skill preflight"
python3 "$RUNNER_TEMP/sim-use/skills/sim-use/scripts/preflight.py" --device emulator-5554

stage="prepare pixel verification"
python3 -m venv "$RUNNER_TEMP/chal-device-python"
"$RUNNER_TEMP/chal-device-python/bin/pip" install --quiet Pillow
stage="initial app launch"
adb shell am start -W -n com.alijafari.red.astronomy/.MainActivity
sleep 8
stage="observe-act-verify Chal"
"$RUNNER_TEMP/chal-device-python/bin/python" .github/scripts/chal_sim_use_check.py --device emulator-5554 --output "$RUNNER_TEMP/chal-device-images"
