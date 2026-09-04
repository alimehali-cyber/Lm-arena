#!/bin/bash
# Z-P1: compile ARProjectionEngine.kt + the never-run SkyOrientationProjectionTest.kt
# in the offline harness using ONLY compile-only Android stubs (see HARNESS_DISCLOSURE).
set -e
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
KT_HOME="${KT_HOME:-/usr/local/lib/node_modules/kotlin-compiler}"
JAVA_HOME="${JAVA_HOME:-$(python3 -c 'import jdk4py, os; print(os.path.dirname(jdk4py.__file__))')/java-runtime}"
export JAVA_HOME
export PATH="$KT_HOME/bin:$JAVA_HOME/bin:$PATH"
OUT="${OUT:-/tmp/kotlin-projection-build}"
rm -rf "$OUT" && mkdir -p "$OUT"
H="$REPO/tools/kotlin-harness/src"
ENGINE="$REPO/app/src/main/java/com/alijafari/red/astronomy/astro_engine"
TEST="$REPO/app/src/test/java/com/alijafari/red/astronomy"
kotlinc \
  "$H"/org/junit/Test.kt "$H"/org/junit/Assert.kt "$H"/runner/Main.kt \
  "$H"/androidx/compose/ui/geometry/Offset.kt \
  "$H"/android/content/Context.kt "$H"/android/graphics/GraphicsShim.kt \
  "$H"/android/hardware/camera2/Camera2Shim.kt "$H"/android/util/AndroidUtilShim.kt \
  "$H"/androidx/camera/view/PreviewView.kt \
  "$H"/com/alijafari/red/astronomy/BuildConfig.kt \
  "$ENGINE/ARProjectionEngine.kt" \
  "$TEST/SkyOrientationProjectionTest.kt" \
  -d "$OUT" 2>&1 | grep -v "^warning:" || true
test -d "$OUT" || { echo "COMPILE FAILED"; exit 1; }
echo "== compile OK — running SkyOrientationProjectionTest for the FIRST time =="
java -cp "$OUT:$KT_HOME/lib/kotlin-stdlib.jar" runner.MainKt com.alijafari.red.astronomy.SkyOrientationProjectionTest
