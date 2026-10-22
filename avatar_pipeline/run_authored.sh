#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# The shared pipeline stages for an AUTHORED character (no photo analysis).
#
#   ./run_authored.sh working/profz blender/build_professor_z.py
#
# Stage 1 of run_pipeline.sh turns a photograph into proportions. Professor Z has no
# photograph: the character sheet supplies the measurements and build_professor_z.py
# encodes them. Everything after the build is identical, and deliberately so - the rig,
# the 18 morph targets, the 21 clips, the skinning gate, the export and the GLB
# validator are the parts that were already verified.
# ---------------------------------------------------------------------------
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
# shellcheck disable=SC1091
source tools/headless/blender_env.sh

WORK="${1:?usage: run_authored.sh <workdir> <builder.py> [target-tris]}"
BUILDER="${2:?usage: run_authored.sh <workdir> <builder.py> [target-tris]}"
TRIS="${3:-34000}"
ID="$(basename "$WORK")"
SAMPLES="${SAMPLES:-18}"
RES="${RES:-360}"

say() { printf '\n\033[1m== %s\033[0m\n' "$*"; }

say "1/8 build character (authored)"
"$AVATAR_BLENDER_PY" "$BUILDER" --out "$WORK/character.blend" \
  --report "$WORK/03_build.json"

say "2/8 skinning gate"
"$AVATAR_BLENDER_PY" blender/check_skinning.py \
  --blend "$WORK/character.blend" --report "$WORK/05_skin.json" || {
    echo "SKINNING GATE FAILED - see $WORK/05_skin.json"; exit 1; }

say "3/8 animate (21 clips)"
"$AVATAR_BLENDER_PY" blender/animate_character.py \
  --blend "$WORK/character.blend" --out "$WORK/character_anim.blend" \
  --report "$WORK/06_anim.json"

say "4/8 review renders"
"$AVATAR_BLENDER_PY" blender/render_review.py --blend "$WORK/character_anim.blend" \
  --outdir "renders/$ID" --shots body_front,body_side,body_rear,body_34,portrait_front,portrait_34,portrait_side \
  --samples "$SAMPLES" --res "$RES" --report "renders/$ID/shotlist.json"

say "5/8 review suite (expressions / anim / deform / material)"
for m in expressions anim deform material; do
  "$AVATAR_BLENDER_PY" blender/review_suite.py --blend "$WORK/character_anim.blend" \
    --outdir "renders/$ID/suite" --mode "$m" --samples "$SAMPLES" --res "$RES"
done

say "6/8 export mobile GLB"
"$AVATAR_BLENDER_PY" blender/export_mobile.py --blend "$WORK/character_anim.blend" \
  --out "exports/$ID/avatar.glb" --report "exports/$ID/08_export.json"

say "7/8 validate GLB"
"$AVATAR_BLENDER_PY" tools/validate_glb.py --glb "exports/$ID/avatar.glb" \
  --report "exports/$ID/09_validate.json" || {
    echo "GLB VALIDATION FAILED - see exports/$ID/09_validate.json"; exit 1; }

say "8/8 manifest"
"$AVATAR_BLENDER_PY" tools/manifest.py --root "$ROOT" --id "$ID" \
  --out "exports/$ID/manifest.json" --identity authored

printf '\n\033[1;32mAUTHORED PIPELINE OK\033[0m  %s\n' "$ID"
echo "  blend  $WORK/character_anim.blend"
echo "  glb    exports/$ID/avatar.glb"
echo "  report exports/$ID/manifest.json"
