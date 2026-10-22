#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# One command, whole pipeline. Every stage is headless and deterministic.
#
#   ./run_pipeline.sh working/test01                      # workdir, then image
#   ./run_pipeline.sh working/zig01 art.png --heads-tall 5 --height-m 1.4
#   SKIP_RENDER=1 ./run_pipeline.sh working/test01         # geometry + export only
#
# Stage 2 (learned image->3D) is not available in this environment; see
# reports/FINAL_REPORT.md section 2 for what was tried and why.
# ---------------------------------------------------------------------------
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
# shellcheck disable=SC1091
source tools/headless/blender_env.sh

WORK="${1:-working/test01}"
ID="$(basename "$WORK")"
PHOTO="${2:-tests/fixtures/portrait-astronaut.jpg}"
shift $(( $# > 2 ? 2 : $# ))
EXTRA=("$@")          # forwarded to analyze_photo.py: --heads-tall, --height-m, --face-box
SAMPLES="${SAMPLES:-20}"
RES="${RES:-340}"
ANALYSIS="$WORK/01_analysis.json"

say() { printf '\n\033[1m== %s ==\033[0m\n' "$*"; }

if [ ! -f "$ANALYSIS" ] || [ "${FORCE_ANALYZE:-0}" = "1" ]; then
  say "1/10 analyze photo"
  "$AVATAR_BLENDER_PY" tools/analyze_photo.py --image "$PHOTO" --out "$ANALYSIS" "${EXTRA[@]+"${EXTRA[@]}"}"
else
  say "1/10 analyze photo (cached: $ANALYSIS)"
fi

say "3/10 build body + rig + morphs"
"$AVATAR_BLENDER_PY" blender/build_character.py \
  --analysis "$ANALYSIS" --out "$WORK/character.blend" --report "$WORK/03_build.json"

say "5/10 skinning gate"
"$AVATAR_BLENDER_PY" blender/check_skinning.py \
  --blend "$WORK/character.blend" --report "$WORK/05_skin.json" || {
    echo "SKINNING GATE FAILED - see $WORK/05_skin.json"; exit 1; }

say "6/10 animate (21 clips)"
"$AVATAR_BLENDER_PY" blender/animate_character.py \
  --blend "$WORK/character.blend" --out "$WORK/character_anim.blend" --report "$WORK/06_anim.json"

say "6b/10 lip-sync"
if [ -f "$WORK/voice.wav" ]; then
  "$AVATAR_BLENDER_PY" tools/lipsync.py --audio "$WORK/voice.wav" \
    --out "$WORK/07_visemes.json" --mode energy
else
  echo "no $WORK/voice.wav - skipped (the module is covered by tests/test_pipeline_units.py)"
fi

if [ "${SKIP_RENDER:-0}" != "1" ]; then
  say "7/10 review renders"
  "$AVATAR_BLENDER_PY" blender/render_review.py --blend "$WORK/character_anim.blend" \
    --outdir "renders/$ID" --shots body_front,body_side,body_rear,portrait_front,portrait_34 \
    --samples "$SAMPLES" --res "$RES" --report "renders/$ID/shotlist.json"
  say "7b/10 review suite"
  for m in expressions anim deform material; do
    "$AVATAR_BLENDER_PY" blender/review_suite.py --blend "$WORK/character_anim.blend" \
      --outdir "renders/$ID/suite" --mode "$m" --samples "$SAMPLES" --res "$RES"
  done
fi

say "8/10 export mobile GLB"
"$AVATAR_BLENDER_PY" blender/export_mobile.py --blend "$WORK/character_anim.blend" \
  --out "exports/$ID/avatar.glb" --report "exports/$ID/08_export.json"

say "9/10 validate GLB"
"$AVATAR_BLENDER_PY" tools/validate_glb.py --glb "exports/$ID/avatar.glb" \
  --report "exports/$ID/09_validate.json" || {
    echo "GLB VALIDATION FAILED - see exports/$ID/09_validate.json"; exit 1; }

say "10/10 manifest"
"$AVATAR_BLENDER_PY" tools/manifest.py --root "$ROOT" --id "$ID" --out "exports/$ID/manifest.json"

printf '\n\033[1;32mPIPELINE OK\033[0m  %s\n' "$ID"
echo "  blend  $WORK/character_anim.blend"
echo "  glb    exports/$ID/avatar.glb"
echo "  report exports/$ID/manifest.json"
