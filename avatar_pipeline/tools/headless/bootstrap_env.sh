#!/usr/bin/env bash
# Rebuild the local toolchain from nothing. Safe to re-run; each step is idempotent.
#
#   ./tools/headless/bootstrap_env.sh            # ~3-6 min on first run
#
# Why this exists: `.venv` and `stublibs/` are build artefacts and are excluded from
# snapshots, so a fresh sandbox has the code but no working `bpy`. This script brings the
# environment back to the state every report was measured in.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

# Pinned: bpy 5.0.1 is the Blender the pipeline was built and measured against
# (glTF exporter "Khronos glTF Blender I/O v5.0.21" is bundled with it).
BPY_VERSION="${BPY_VERSION:-5.0.1}"
# bpy 5.0.1 pins numpy 1.26.x; opencv must match that, not the newest numpy.
NUMPY_VERSION="${NUMPY_VERSION:-1.26.4}"
OPENCV_VERSION="${OPENCV_VERSION:-4.10.0.84}"

say() { printf '\n\033[1m== %s ==\033[0m\n' "$*"; }

if [ ! -x ".venv/bin/python" ]; then
  say "creating .venv (python $(python3 -V 2>&1 | cut -d' ' -f2))"
  python3 -m venv .venv
fi

say "installing pinned python deps into .venv"
.venv/bin/python -m pip install --quiet --upgrade pip wheel
# bpy is a ~374 MB wheel and pulls its own numpy-compatible runtime; install it first so
# the resolver does not try to upgrade it underneath us.
.venv/bin/python -m pip install --quiet "bpy==${BPY_VERSION}"
.venv/bin/python -m pip install --quiet "numpy==${NUMPY_VERSION}" "opencv-python-headless==${OPENCV_VERSION}"

say "regenerating linker stubs (bpy needs X11/GL SONAMEs this box does not have)"
python3 tools/headless/build_stubs_fixpoint.py "$ROOT/.venv/bin/python" | tail -5

say "smoke test"
# shellcheck disable=SC1091
source tools/headless/blender_env.sh
"$AVATAR_BLENDER_PY" - <<'PY'
import sys, bpy, numpy, cv2
print("python", sys.version.split()[0])
print("bpy", bpy.app.version_string, "| numpy", numpy.__version__, "| cv2", cv2.__version__)
print("addons: gltf exporter present:", hasattr(bpy.ops.export_scene, "gltf"))
PY

say "recording resolved versions"
.venv/bin/python -m pip freeze | grep -Ei "^(bpy|numpy|opencv)" > tools/headless/requirements.lock
cat tools/headless/requirements.lock

printf '\n\033[1;32mENVIRONMENT OK\033[0m  next: ./run_pipeline.sh working/test01\n'
