#!/usr/bin/env bash
# Install the image-to-3D skill's engine (AssetFurnace, formerly Image to 3D Lab) and report what this
# machine can actually run.
#
#   bash tools/avatar/install_skills.sh            install + probe
#   bash tools/avatar/install_skills.sh --check     probe only, no clone
#   bash tools/avatar/install_skills.sh --update    move the clone to the pinned commit
#
# It installs CODE, never model weights: AssetFurnace requires an explicit yes for every weight
# download (and so do we). Nothing here is committed — vendor/ is git-ignored.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VENDOR="$REPO_ROOT/vendor/image-to-3dlab"
UPSTREAM="https://github.com/Bingeljell/image-to-3dlab.git"
PIN="1a5093571075abce4d19cbd3f2c6fbc0faa9d6f9"

mode="install"
case "${1:-}" in
  --check) mode="check" ;;
  --update) mode="update" ;;
  "" ) ;;
  *) echo "unknown option: $1" >&2; exit 2 ;;
esac

say() { printf '%s\n' "$*"; }

probe_hardware() {
  say ""
  say "Machine"
  say "  arch        $(uname -m)   $(uname -s)"
  if command -v nvidia-smi >/dev/null 2>&1; then
    say "  NVIDIA      $(nvidia-smi --query-gpu=name --format=csv,noheader 2>/dev/null | head -1)"
    say "  CUDA        yes  -> Pixal3D / Hunyuan3D-2.1 / TRELLIS.2 possible"
  else
    say "  NVIDIA      none"
  fi
  if [ "$(uname -s)" = "Darwin" ] && [ "$(uname -m)" = "arm64" ]; then
    say "  Apple GPU   yes  -> Pixal3D / Hunyuan3D-MLX / TRELLIS.2 (Mac ports)"
  elif [ "$(uname -s)" = "Darwin" ]; then
    say "  Apple GPU   no (not Apple Silicon)"
  else
    say "  Apple GPU   n/a"
  fi
  if command -v rocminfo >/dev/null 2>&1; then
    say "  ROCm        yes  -> Pixal3D only"
  else
    say "  ROCm        none"
  fi
  if command -v blender >/dev/null 2>&1; then
    say "  Blender     $(blender --version 2>/dev/null | head -1)"
  else
    say "  Blender     none (Finish and Split need it)"
  fi
  say "  Python      $(python3 --version 2>&1)"
  say "  RAM         $(awk '/MemTotal/{printf "%.1f GB", $2/1048576}' /proc/meminfo 2>/dev/null || echo unknown)"
}

probe_steps() {
  local gpu="no"
  if command -v nvidia-smi >/dev/null 2>&1; then gpu="yes"; fi
  if [ "$(uname -s)" = "Darwin" ] && [ "$(uname -m)" = "arm64" ]; then gpu="yes"; fi
  if command -v rocminfo >/dev/null 2>&1; then gpu="yes"; fi

  say ""
  say "Pipeline steps on this machine"
  say "  picture / prompt        yes  (Qwen-Image needs its own weights on a GPU)"
  say "  matte                   yes  (flood-fill + Pillow; rembg optional)"
  say "  model (image -> 3D)     $([ "$gpu" = yes ] && echo 'yes  (weights download on request)' || echo 'NO   needs CUDA / MPS / ROCm')"
  say "  finish (Pixel Match)    $([ "$gpu" = yes ] && echo 'yes' || echo 'NO   follows the 3D step')"
  say "  rig (SkinTokens)        $([ "$gpu" = yes ] && echo 'yes  (CPU possible, slow)' || echo 'MAYBE pure PyTorch, ~2 GB of packages + 1.6 GB weights')"
  say "  animate (21 presets)    yes  (clips are prefit; export needs Blender)"
  say "  inspect GLB / stats     yes  (pure stdlib, works here)"
  say "  authored fallback       yes  (tools/avatar, numpy + Pillow only)"
}

if [ "$mode" != "check" ]; then
  if [ -d "$VENDOR/.git" ]; then
    say "AssetFurnace already at $VENDOR"
    if [ "$mode" = "update" ]; then
      git -C "$VENDOR" fetch --quiet origin "$PIN" || git -C "$VENDOR" fetch --quiet origin
      git -C "$VENDOR" checkout --quiet "$PIN"
    fi
  else
    say "Cloning AssetFurnace (code only, ~21 MB) ..."
    mkdir -p "$REPO_ROOT/vendor"
    git clone --quiet "$UPSTREAM" "$VENDOR"
    git -C "$VENDOR" fetch --quiet origin "$PIN" || true
    git -C "$VENDOR" checkout --quiet "$PIN"
  fi
  got="$(git -C "$VENDOR" rev-parse HEAD)"
  say "  commit      $got"
  [ "$got" = "$PIN" ] || say "  WARNING: expected $PIN"
  say "  licence     Apache-2.0 (see vendor/image-to-3dlab/LICENSE and NOTICE)"
  say ""
  say "Read-only GLB tools are importable without a GPU:"
  say "  PYTHONPATH=$VENDOR python3 -c 'from image_to_3dlab.glb_stats import glb_stats; print(\"ok\")'"
  if PYTHONPATH="$VENDOR" python3 -c 'from image_to_3dlab.glb_stats import glb_stats' 2>/dev/null; then
    say "  GLB stats   ok"
  else
    say "  GLB stats   unavailable (unexpected; check the clone)"
  fi
fi

probe_hardware
probe_steps

say ""
say "Next: skills/image-to-3d-avatar/SKILL.md"
