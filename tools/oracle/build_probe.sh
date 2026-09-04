#!/bin/bash
# Builds + runs the F-A3 coordinate oracle probe against the app's real engines.
# Reusable: the sandbox /tmp and global toolchain are volatile — this script rebuilds everything.
set -e
cd "$(dirname "$0")/../.."
export JAVA_HOME=${JAVA_HOME:-$(python3 -c 'import jdk4py, os; print(os.path.dirname(jdk4py.__file__))')/java-runtime}
export PATH="/usr/local/lib/node_modules/kotlin-compiler/bin:$JAVA_HOME/bin:$PATH"
command -v kotlinc >/dev/null || npm install -g kotlin-compiler >/dev/null 2>&1
python3 -c "import jdk4py" 2>/dev/null || pip install --quiet --break-system-packages jdk4py
M=app/src/main/java/com/alijafari/red/astronomy
H=tools/kotlin-harness/src
mkdir -p /tmp/oracle_build /tmp/oracle_probe
kotlinc tools/kotlin-harness/probes/CoordinateOracleProbe.kt \
  $H/org/shredzone/commons/suncalc/MoonTimes.kt \
  $H/com/alijafari/red/astronomy/astro_engine/EclipseEngineShim.kt \
  $H/android/icu/util/IcuShim.kt \
  $M/astro_engine/CoordinateEngine.kt $M/astro_engine/CoordinateEngineLegacy.kt \
  $M/astro_engine/TimeEngine.kt $M/astro_engine/AstroTime.kt \
  $M/astro_engine/FrameTransformationEngine.kt $M/astro_engine/SunEngine.kt \
  $M/astro_engine/MoonEngine.kt $M/astro_engine/LunarSolarEngine.kt \
  $M/astro_engine/PlanetEngine.kt $M/astro_engine/VSOP87Engine.kt \
  $M/data/catalog/StarCatalog.kt $M/data/catalog/DeepSkyCatalog.kt $M/domain/Models.kt \
  -d /tmp/oracle_build 2>&1 | grep -E "error" | head -5 || true
java -cp "/tmp/oracle_build:/usr/local/lib/node_modules/kotlin-compiler/lib/kotlin-stdlib.jar" \
  probes.CoordinateOracleProbeKt
