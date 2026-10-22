#!/usr/bin/env bash
# Canonical environment for every Blender stage.
#
#   source tools/headless/blender_env.sh
#   $AVATAR_BLENDER_PY blender/build_character.py ...
#
# Why this exists: bpy is a Python module that links a pile of windowing libraries a
# headless box does not have. tools/headless/stublibs supplies empty stand-ins so the
# module loads; libEGL is deliberately NOT stubbed, so no GPU context can appear and
# rendering must be Cycles on the CPU.
set -u
AVATAR_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export AVATAR_ROOT
export AVATAR_BLENDER_PY="$AVATAR_ROOT/.venv/bin/python"

# stub libraries first, then whatever the system already has
export LD_LIBRARY_PATH="$AVATAR_ROOT/tools/headless/stublibs${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"

# deterministic, modest resource use: this sandbox has 2 vCPUs
export OMP_NUM_THREADS="${OMP_NUM_THREADS:-2}"
export OPENBLAS_NUM_THREADS="${OMP_NUM_THREADS:-2}"
export PYTHONDONTWRITEBYTECODE=1
export PYTHONHASHSEED=0

blender_py() { "$AVATAR_BLENDER_PY" "$@"; }
export -f blender_py 2>/dev/null || true
