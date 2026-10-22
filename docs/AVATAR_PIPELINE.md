# Avatar pipeline — reference photos in, an animated character on Home out

Three skills hold the procedure; this page is the map, the current state, and the exact commands.

- [`skills/image-to-3d-avatar/`](../skills/image-to-3d-avatar/SKILL.md) — photo intake → mesh → rig →
  move → export, with the AssetFurnace engine map.
- [`skills/zig-home-scene/`](../skills/zig-home-scene/SKILL.md) — the scene the character stands in.
- [`skills/zig-avatar-runtime/`](../skills/zig-avatar-runtime/SKILL.md) — how a GLB gets drawn in the
  observer slot, and the asset contract.

`docs/HOME_SCENE_ANALYSIS.md` is the source-level analysis of Home, the sky canvas and the current
placeholder.

---

## The two routes

| | **AssetFurnace** (`Bingeljell/image-to-3dlab`) | **Authored route** (`tools/avatar/`) |
|---|---|---|
| Needs | Apple Silicon / NVIDIA 24 GB+ / AMD ROCm, Blender, 8–20 GB of weights | numpy + Pillow |
| Produces | textured mesh from the photo, SkinTokens rig, 21 motion presets | stylised low-poly mesh, authored rig, 5 scene clips |
| Fidelity | photographic — Pixel Match puts the photo's own pixels back | proportions and palette from the photo, geometry stylised |
| Status here | source installed and inspected; **cannot run** (no GPU) | **runs, tested, verified** |

Both write the same contract (`skills/zig-avatar-runtime/references/asset-contract.md`), so the app
does not care which one built a character. Say which route you used, every time.

## Commands

```bash
# 0. what can this machine do?
bash tools/avatar/install_skills.sh --check

# 1. measure a photo: matte, proportions, palette, quality checks
python3 tools/avatar/photo_ingest.py front.jpg --out work/brief.json --matte work/front_cut.png

# 2. build, rig, animate, export (and render the check sheet)
python3 tools/avatar/build_avatar.py --photo front.jpg --name aram --out out/avatars/aram.glb

# 3. verify the file against the contract
python3 tools/avatar/glb_check.py out/avatars/aram.glb --strict

# 4. tests
python3 -m unittest discover -s tools/avatar/tests -v
```

`tools/avatar/make_test_photo.py` draws a stand-in reference card, so the whole chain can be exercised
before a real shoot. `tools/avatar/README.md` lists every tool and its one job.

## What has been proved here

The authored route was built and run end to end on a CPU-only box:

- **1612 triangles, 904 vertices, 19 bones, 1.68 m tall, 352 KB** — inside every budget in the
  contract, with 25 unit tests covering bone names, weights, normals, quaternion aiming, seamless
  loops, the GLB's own bytes and the photo measurements.
- The three Home beats are posed by a two-bone IK solver (`solve_arm`), not by guessed Euler angles —
  a test asserts the night hand is above the head and the noon hand is at the brow, because those are
  the two poses a viewer judges in half a second.
- The contact sheet renders each clip and one tile at the size the character really appears on Home
  (`work/out/testcard_sheet.png`).

What is **not** proved: the Android runtime integration. No GLES3 sidecar exists in the app yet; that
is the next code step (`skills/zig-avatar-runtime/SKILL.md` §Where it goes), and it needs a device or
an emulator with a GPU.

## The photographs that work

Front, full body, A-pose, plain background, soft even light, feet in frame, eyes open, hair kept clear
of the face. Optional extras that improve the result: 3/4 front, side, back, and a close head crop for
`finish_detail: hero`. The rejection checklist is in
`skills/image-to-3d-avatar/references/photo-intake.md`.

Name files by view (`front.jpg`, `threequarter.jpg`, `side.jpg`, `back.jpg`, `head.jpg`) and keep the
originals: the provenance sidecar records them, and a rebuild should always be possible from them.

## Budget for the app slot

The observer is 0.19 × the scene height — about **170 px** on a 900 px scene — with its feet at 0.76
and its centre at 0.745 of the width. That is the whole reason the budget is tight: at that size, a
12k-triangle character is generous, and a 4k texture is wasted.
