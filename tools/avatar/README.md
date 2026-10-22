# tools/avatar — the no-GPU avatar route, and the photo logic both routes share

Every tool here does one job and is importable, so the pipeline can be tested without a GPU, a
browser or Blender. The heavier, photographic route is AssetFurnace; this is the route that always
runs and the measurements that route shares.

| Tool | What it does |
|---|---|
| `install_skills.sh` | Clone AssetFurnace at the pinned commit into `vendor/` (code only, never weights) and report what this machine can actually run. |
| `photo_ingest.py` | Matte a reference photo by flooding the border, measure the silhouette, sample a five-colour palette, write a reproducible brief. |
| `character.py` | Build a named character from a spec: proportions, palette, and the geometry of a face that still reads at 170 px. |
| `build_character.py` | Build that character into a GLB plus its `.provenance.json`, render the check sheet, and optionally install the asset into the app. |
| `review.py` | Four-turnaround renders, a head close-up and a tile at the size the Home scene draws — the sheet a look is judged on. |
| `bake_frames.py` | Render every clip from the same mesh into per-clip sprite atlases plus `meta.json` — this is what the app draws. |
| `build_avatar.py` | Build, rig, animate and export an avatar GLB from a photo or a brief, write the provenance sidecar, render the check sheet. |
| `avatar_core.py` | The 21-bone skeleton, the parametric humanoid mesh and the distance-based skinning. |
| `motion.py` | Quaternions, a two-bone IK solver, and the five clips (`idle_breathe`, `day_shade`, `dusk_phone`, `night_point`, `wave_hello`). |
| `glb_export.py` | Write a single-mesh, single-skin, five-clip GLB — binary glTF 2.0, no libraries. |
| `glb_check.py` | Read a GLB back and assert the asset contract: joints, weights, inverse bind matrices, clip targets, budgets. |
| `preview.py` | Skinning plus a numpy z-buffer rasteriser: a four-pose contact sheet and a tile at Home scale. |
| `make_test_photo.py` | Draw a stand-in reference card in A-pose, so the chain can be run before a real shoot. |
| `tests/test_avatar.py` | 27 tests: skeleton, mesh, weights, non-degenerate geometry, skinning reach, quaternions, IK poses, seamless loops, GLB bytes, photo measurement. |

```bash
python3 tools/avatar/make_test_photo.py --out work/test_card.png
python3 tools/avatar/photo_ingest.py work/test_card.png --out work/brief.json --matte work/cut.png
python3 tools/avatar/build_avatar.py --photo work/test_card.png --name demo --out out/avatars/demo.glb
python3 tools/avatar/glb_check.py out/avatars/demo.glb --strict
python3 -m unittest discover -s tools/avatar/tests -v
```

Professor Z, whose spec lives in `specs/professor_z.json`:

```bash
python3 tools/avatar/build_character.py --spec tools/avatar/specs/professor_z.json \
    --out out/avatars/professor_z.glb                  # GLB + provenance + review sheet
python3 tools/avatar/review.py --spec tools/avatar/specs/professor_z.json --out out/avatars
python3 tools/avatar/bake_frames.py --spec tools/avatar/specs/professor_z.json \
    --out app/src/main/assets/avatar/professor_z      # the frames the Home scene draws
```

`out/` and `work/` are git-ignored: a character is content, not the product. What ships is the
pipeline and the contract — plus the baked frames, which the app cannot build at runtime.
