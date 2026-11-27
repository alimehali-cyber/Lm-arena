#!/usr/bin/env bash
set -uo pipefail
timeout 720 ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.alijafari.red.astronomy.ShowcaseSmokeTest -Pgravity.ci.tests=false --no-daemon 2>&1 | tee showcase-gradle.log
status=${PIPESTATUS[0]}
adb logcat -d -b crash > showcase-crash.log || true
exit "$status"
