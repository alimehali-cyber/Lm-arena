#!/bin/bash
# Offline pure-Kotlin test harness for the startracker subset (and any other
# pure-Kotlin test files listed below). See docs/startracker/evidence/HARNESS_DISCLOSURE.md.
#
# One command reproduces the run:   bash tools/kotlin-harness/run_tests.sh
set -e

REPO="$(cd "$(dirname "$0")/../.." && pwd)"
KT_HOME="${KT_HOME:-/usr/local/lib/node_modules/kotlin-compiler}"
JAVA_HOME="${JAVA_HOME:-$(python3 -c 'import jdk4py, os; print(os.path.dirname(jdk4py.__file__))')/java-runtime}"
export JAVA_HOME
export PATH="$KT_HOME/bin:$JAVA_HOME/bin:$PATH"
OUT="${OUT:-/tmp/kotlin-harness-build}"
rm -rf "$OUT" && mkdir -p "$OUT/classes"

ST_MAIN="$REPO/app/src/main/java/com/alijafari/red/astronomy/startracker"
ST_TEST="$REPO/app/src/test/java/com/alijafari/red/astronomy/startracker"
HARNESS_SRC="$REPO/tools/kotlin-harness/src"

echo "== toolchain: $(kotlinc -version 2>&1) | java: $(java -version 2>&1 | head -1) =="

# Sources: harness shims + startracker main + the non-startracker pure-Kotlin deps of the
# startracker code + startracker tests + other pure-Kotlin tests runnable without Android.
kotlinc \
  "$HARNESS_SRC"/org/junit/Test.kt \
  "$HARNESS_SRC"/org/junit/Assert.kt \
  "$HARNESS_SRC"/runner/Main.kt \
  "$HARNESS_SRC"/androidx/compose/ui/geometry/Offset.kt \
  $(find "$ST_MAIN" -name '*.kt') \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/astro_engine/FrameTransformationEngine.kt" \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/astro_engine/AstroTime.kt" \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/astro_engine/MagneticDeclination.kt" \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/astro_engine/CoordinateEngine.kt" \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/astro_engine/CoordinateEngineLegacy.kt" \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/astro_engine/TimeEngine.kt" \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/astro_engine/SunEngine.kt" \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/astro_engine/MoonEngine.kt" \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/astro_engine/LunarSolarEngine.kt" \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/astro_engine/PlanetEngine.kt" \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/astro_engine/VSOP87Engine.kt" \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/domain/Models.kt" \
  "$HARNESS_SRC"/org/shredzone/commons/suncalc/MoonTimes.kt \
  "$HARNESS_SRC"/com/alijafari/red/astronomy/astro_engine/EclipseEngineShim.kt \
  "$HARNESS_SRC"/android/icu/util/IcuShim.kt \
  "$HARNESS_SRC"/android/os/Build.kt \
  "$HARNESS_SRC"/android/hardware/camera2/CameraCharacteristics.kt \
  $(find "$ST_TEST" -name '*.kt') \
  "$REPO/app/src/test/java/com/alijafari/red/astronomy/RefractionTest.kt" \
  "$REPO/app/src/test/java/com/alijafari/red/astronomy/CoordinateOracleTest.kt" \
  "$REPO/app/src/test/java/com/alijafari/red/astronomy/MagneticDeclinationTest.kt" \
  "$REPO/app/src/main/java/com/alijafari/red/astronomy/ui/rendering/HeroSkyProjection.kt" \
  "$REPO/app/src/test/java/com/alijafari/red/astronomy/HeroSkyProjectionTest.kt" \
  "$REPO/tools/kotlin-harness/tests/HardwareDistortionReaderTest.kt" \
  -d "$OUT/classes" 2>&1 | grep -E "error|warning" | head -40

echo "== compile done, running =="
CLASSES=$(cd "$OUT/classes" && find com/alijafari -name '*Test.class' ! -name '*$*' | sed -e 's#/#.#g' -e 's#\.class$##')
echo "-- test classes: $(echo "$CLASSES" | wc -l) --"
java -cp "$OUT/classes:$KT_HOME/lib/kotlin-stdlib.jar" runner.MainKt $CLASSES
