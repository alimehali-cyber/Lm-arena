#!/usr/bin/env bash
# Reproducible sandbox-local bootstrap script for the ZIG 3D Avatar Pipeline (`avatar_pipeline`).
# Installs Python 3.11 `.venv`, Blender 4.2 (`bpy==4.2.0`) + snapshot-persisted X11/GLX shims,
# `scikit-image` + `image-to-3dlab` (`AssetFurnace v0.5.1`), OpenJDK 21 (`jdk4py`), and `kotlinc 2.2.10`.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
mkdir -p "$HOME/.local/bin" "$HOME/.local/lib" "$HOME/.local/opt"

cp -a "$REPO_ROOT/avatar_pipeline/tools/_x11_stubs/"* "$HOME/.local/lib/"

if [ ! -d "$REPO_ROOT/.venv" ]; then
  python3 -m venv "$REPO_ROOT/.venv"
fi

"$REPO_ROOT/.venv/bin/pip" install --quiet uv
"$REPO_ROOT/.venv/bin/uv" pip install --python "$REPO_ROOT/.venv/bin/python" \
  "numpy==1.26.4" \
  "scipy==1.15.2" \
  "scikit-image==0.25.2" \
  "requests==2.32.3" \
  "einops==0.8.1" \
  "omegaconf==2.3.0" \
  "Pillow==11.1.0" \
  "opencv-python-headless==4.11.0.86" \
  "mediapipe==0.10.21" \
  "trimesh==4.6.4" \
  "pygltflib==1.16.3" \
  "pocketsphinx==5.1.1" \
  "jdk4py==21.0.8.0" \
  "bpy==4.2.0"

# MediaPipe pulls `opencv-contrib-python` (linked against libGL.so.1); overlay headless `cv2` cleanly so
# every CLI script and unittest runs with zero system X11/OpenGL shared-library requirements.
"$REPO_ROOT/.venv/bin/uv" pip uninstall --python "$REPO_ROOT/.venv/bin/python" --quiet opencv-contrib-python opencv-python-headless
"$REPO_ROOT/.venv/bin/uv" pip install --python "$REPO_ROOT/.venv/bin/python" --no-deps "opencv-python-headless==4.11.0.86"

SITE_PKGS=$("$REPO_ROOT/.venv/bin/python" -c "import site; print(site.getsitepackages()[0])")
cat > "$SITE_PKGS/zig_3d_repos.pth" <<EOF
$REPO_ROOT/avatar_pipeline/models/repos/image-to-3dlab
$REPO_ROOT/avatar_pipeline/models/repos/TripoSR
$REPO_ROOT/avatar_pipeline/models/repos/stable-fast-3d
EOF

# Iteration-3 neural depth weights: Depth-Anything-V2-Small fp16 ONNX (Apache-2.0 upstream, MIT-packaged)
# pinned to ForJiang/image-to-splat@05d10fc34056a825192c88cc243c9a592f5d0ff1 via git sparse checkout (github.com only).
DA2_DIR="$REPO_ROOT/avatar_pipeline/models/depth_anything_v2_small"
if [ ! -s "$DA2_DIR/model_fp16.onnx" ]; then
  mkdir -p "$DA2_DIR"
  TMPDA2=$(mktemp -d)
  git clone --quiet --depth 1 --filter=blob:none --sparse https://github.com/ForJiang/image-to-splat.git "$TMPDA2/repo"
  git -C "$TMPDA2/repo" sparse-checkout set models/depth-anything-v2-small/onnx
  cp -f "$TMPDA2/repo/models/depth-anything-v2-small/onnx/model_fp16.onnx" "$DA2_DIR/model_fp16.onnx"
  rm -rf "$TMPDA2"
fi

JAVA_HOME_DIR=$("$REPO_ROOT/.venv/bin/python" -c "import jdk4py; print(jdk4py.JAVA_HOME)")
ln -sf "$JAVA_HOME_DIR/bin/java" "$HOME/.local/bin/java"
ln -sf "$JAVA_HOME_DIR/bin/javac" "$HOME/.local/bin/javac"
ln -sf "$JAVA_HOME_DIR/bin/jar" "$HOME/.local/bin/jar"
ln -sf "$REPO_ROOT/avatar_pipeline/tools/_blender_runner.py" "$HOME/.local/bin/blender"
chmod +x "$REPO_ROOT/avatar_pipeline/tools/_blender_runner.py"

if [ ! -x "$HOME/.local/bin/kotlinc" ]; then
  curl -fsSL https://github.com/JetBrains/kotlin/releases/download/v2.2.10/kotlin-compiler-2.2.10.zip -o /tmp/kotlinc.zip || true
  if [ ! -s /tmp/kotlinc.zip ]; then
    npm --prefix "$HOME/.local/opt/kt_pkg" install --silent kotlin-compiler@2.2.10
    ln -sf "$HOME/.local/opt/kt_pkg/node_modules/kotlin-compiler/bin/kotlinc" "$HOME/.local/bin/kotlinc"
    ln -sf "$HOME/.local/opt/kt_pkg/node_modules/kotlin-compiler/bin/kotlin" "$HOME/.local/bin/kotlin"
  else
    unzip -q -o /tmp/kotlinc.zip -d "$HOME/.local/opt/"
    ln -sf "$HOME/.local/opt/kotlinc/bin/kotlinc" "$HOME/.local/bin/kotlinc"
    ln -sf "$HOME/.local/opt/kotlinc/bin/kotlin" "$HOME/.local/bin/kotlin"
  fi
fi

# Restore agent skill symlinks into ~/.agents/skills & ~/.claude/skills if snapshot reset HOME
mkdir -p "$HOME/.agents/skills" "$HOME/.claude/skills" "$REPO_ROOT/.agents/skills"
for sdir in "$REPO_ROOT/avatar_pipeline/skills/"*/*; do
  if [ -f "$sdir/SKILL.md" ]; then
    sname="$(basename "$sdir")"
    ln -sfn "$sdir" "$REPO_ROOT/.agents/skills/$sname"
    ln -sfn "$sdir" "$HOME/.agents/skills/$sname"
    ln -sfn "$sdir" "$HOME/.claude/skills/$sname"
  fi
done

echo "Environment verified at $REPO_ROOT/.venv and $HOME/.local/bin"
