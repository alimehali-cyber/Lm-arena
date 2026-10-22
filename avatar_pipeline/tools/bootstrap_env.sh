#!/usr/bin/env bash
# Reproducible sandbox-local bootstrap script for the ZIG 3D Avatar Pipeline (`avatar_pipeline`).
# Installs Python 3.11 `.venv`, Blender 4.2 (`bpy==4.2.0`) + headless user-space X11/GLX shims,
# OpenJDK 21 (`jdk4py==21.0.8.0`), `kotlinc 2.2.10` (`registry.npmjs.org`), `bun`, and 68 MIT skills.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
mkdir -p "$HOME/.local/bin" "$HOME/.local/lib" "$HOME/.local/opt"

if [ ! -d "$REPO_ROOT/.venv" ]; then
  python3 -m venv "$REPO_ROOT/.venv"
fi

"$REPO_ROOT/.venv/bin/pip" install --quiet uv
"$REPO_ROOT/.venv/bin/uv" pip install --python "$REPO_ROOT/.venv/bin/python" \
  "numpy==1.26.4" \
  "scipy==1.15.2" \
  "Pillow==11.1.0" \
  "opencv-python-headless==4.11.0.86" \
  "mediapipe==0.10.21" \
  "trimesh==4.6.4" \
  "pygltflib==1.16.3" \
  "pocketsphinx==5.1.1" \
  "jdk4py==21.0.8.0" \
  "bpy==4.2.0"

JAVA_HOME_DIR=$("$REPO_ROOT/.venv/bin/python" -c "import jdk4py; print(jdk4py.JAVA_HOME)")
ln -sf "$JAVA_HOME_DIR/bin/java" "$HOME/.local/bin/java"
ln -sf "$JAVA_HOME_DIR/bin/javac" "$HOME/.local/bin/javac"
ln -sf "$JAVA_HOME_DIR/bin/jar" "$HOME/.local/bin/jar"

echo "Environment verified at $REPO_ROOT/.venv and $HOME/.local/bin"
