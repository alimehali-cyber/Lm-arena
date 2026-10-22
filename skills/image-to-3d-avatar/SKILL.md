---
name: image-to-3d-avatar
description: Turn character reference photos into a rigged, animated 3D avatar and ship it as a GLB for the ZIG Home scene. Use when the user supplies character photos or asks for a 3D avatar, a rig, an animation clip or a GLB export. Covers photo intake, matte, 3D build, finish, rig, move, export, provenance, and the Android-side budget.
---

# Image → rigged, animated avatar

One photograph in, one animated character out. This skill is the whole chain, from what to ask the
user for to the exact GLB that the Home scene can draw.

The heavy 3D step is **AssetFurnace** (`Bingeljell/image-to-3dlab`, Apache-2.0, formerly *Image to
3D Lab*): six local backends behind one studio, CLI and MCP server, plus SkinTokens auto-rigging and
21 motion-clip presets. `tools/avatar/install_skills.sh` installs it pinned; read
`references/assetfurnace-pipeline.md` for its engines, knobs and tools.

Because that route needs a GPU (Apple Silicon / NVIDIA / AMD) and this workspace has none, there is a
second, always-available route in `tools/avatar/` that builds a stylised low-poly avatar from the same
measurements on the CPU. Same contract, same clip names, same export. Pick the route, don't fudge it.

## 0. Decide the route before touching a file

| Situation | Route | What you get |
|---|---|---|
| Apple Silicon, or NVIDIA 24 GB+, or AMD with ROCm | **AssetFurnace** `character` plan | textured, ~5–20k tri, auto-rigged, 21 moves |
| CPU only, or no GPU, or a quick iteration | **`tools/avatar/`** fallback | stylised low-poly, authored rig, 5 scene clips |
| User wants their exact face at hero quality | AssetFurnace `finish_detail: hero` | 32k body + a head rebuilt from a face crop |

Say which route you are taking, and why, before you start. Never pretend a CPU build is the GPU one.

## 1. Ask for the right photographs

The picture decides most of the result. Full rules and a rejection checklist:
`references/photo-intake.md`.

Minimum for the fallback route: **one front, full body, A-pose, plain background, soft even light.**
For AssetFurnace: front plus, ideally, the generated back shot (`front_and_back`), and for faces a
tight head crop.

Reject and ask again when: the subject is cut off at the ankles, arms are in a T-pose, hands are in
fists, the background is busy, the light is one-sided hard flash, the image is under ~800 px on the
long edge, or the face is turned past 3/4.

## 2. Ingest and measure

```bash
python3 tools/avatar/photo_ingest.py front.jpg --out work/brief.json --matte work/front_cut.png
```

Produces: a background-matted cutout, the subject box, head/shoulder/hip proportions, a five-colour
palette (skin, hair, top, trousers, shoes), and a head crop. Always look at the cutout before going
on — a bad matte is the single most common cause of a bad avatar.

## 3. Build the mesh

AssetFurnace:

```bash
# needs the studio's venv; --yes is the guard that allows code install, not weights
python3 scripts/bootstrap_autorig.py --check
./lab            # studio at http://127.0.0.1:8765, or drive it over MCP
```

Then `create(have="picture", want="character", picture=..., finish_detail="game",
pixel_match=true, front_and_back=true)` and `run_status()` until it is done. Details, tool list and
knobs in `references/assetfurnace-pipeline.md`.

Fallback:

```bash
python3 tools/avatar/build_avatar.py --front work/front_cut.png --brief work/brief.json \
        --out out/avatars/<name>.glb --preview out/avatars/<name>_sheet.png
```

## 4. Rig

- AssetFurnace: the `rigged` step (SkinTokens, MIT, pure PyTorch) is part of the character plan. Rig
  **after** Finish; never rig `finish_detail: full` (the plan refuses it, and it is right).
- Fallback: the rig is authored with the mesh — 19 bones, humanoid names, A-pose bind. It always
  matches the export contract.

Bone names are the app contract: `hips, spine, chest, neck, head, shoulder.L/R, upper_arm.L/R,
forearm.L/R, hand.L/R, thigh.L/R, shin.L/R, foot.L/R`. See
`../zig-avatar-runtime/references/asset-contract.md`.

## 5. Animate

The Home scene needs three beats, chosen by the real Sun altitude, inherited from
`ObserverRenderer.poseFor`:

| Scene condition | Clip | Bone intent |
|---|---|---|
| Sun > +3° (day) | `day_shade` | hand to brow, weight on one leg |
| +3° … −7° (twilight) | `dusk_phone` | both elbows in, head down 6–8°, phone in the right hand |
| < −7° (night) | `night_point` | right arm up and out, head tilted up 8–12°, left arm relaxed |
| always underneath | `idle_breathe` | breath 4 s, weight shift 8 s, both dividing the 8 s clip exactly |

AssetFurnace gives 21 presets (idle, walk, run, wave, jump, …) — `list_moves`, then
`continue_asset(asset_id, move="idle")`. Full catalogue with durations:
`references/motion-catalogue.md`. Its moves are generic locomotion; the three scene beats above are
poses you still have to author or blend.

## 6. Export and verify

```bash
python3 tools/avatar/check_glb.py out/avatars/<name>.glb --min-bones 18 --require-clip idle_breathe
```

The check reads the file, not a summary of it: triangles, bones, clip names and durations, texture
sizes, weights summing to 1, joint indices in range. A GLB that fails is not delivered.

Beside every avatar write `<name>.glb.provenance.json` — engine, weights, licences, source photos,
clip licences. AssetFurnace does this itself; the fallback route writes it too. Do not strip it.

## 7. Hand-off budget for the app

The observer slot is ~170 px tall on a 900 px scene and repaints at ~24 fps inside the motion layer:

- ≤ 12k triangles, one material, one 512–1024 px texture (or vertex colours only);
- bones ≤ 32, 4 influences per vertex;
- clips ≤ 8 s each, 24 fps, quaternions;
- origin between the feet, +Z forward, Y up, metres, ~1.7 m tall;
- no embedded animation controllers, no external files, no KTX2.

## Hard rules

1. **Weights are never downloaded without an explicit yes.** AssetFurnace enforces it; so do we.
2. **Provenance travels with the file.** Every GLB gets a sidecar naming every licence behind it.
3. **Never claim a pose or a cloud implies weather.** The scene has no weather feed.
4. **Look at the render before declaring success.** Build the preview sheet and actually inspect it.
5. **Keep the placeholder working.** It is the fallback on every device without the avatar.
