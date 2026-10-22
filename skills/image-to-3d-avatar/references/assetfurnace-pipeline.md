# AssetFurnace (Bingeljell/image-to-3dlab) — the parts that matter

Cloned, pinned and inspected from `1a5093571075abce4d19cbd3f2c6fbc0faa9d6f9`. Apache-2.0, with a
NOTICE that asks to be kept and to credit the project (section 4(d)). Repo ~21 MB of code; every
model weight is downloaded separately, always after an explicit yes.

Formerly *Image to 3D Lab* — same repo, renamed. `install.sh` puts it in `~/image-to-3dlab`.

## Shape of the project

| Path | What is in it |
|---|---|
| `pipeline.py` | CLI entry point, sets the MPS/ROCm env, delegates to `image_to_3dlab.cli` |
| `image_to_3dlab/` | package: `cli.py`, `mcp_server.py`, one `*_backend.py` per engine, `glb_*.py` (read-only GLB tools), `photo_paint.py` (Pixel Match), `rig_sidecar.py`, `motions/` |
| `viewer/` | the studio: `serve.py` (HTTP API + static), `studio/*.js`, and one `*_api.py` per feature (`generate_api`, `finish_api`, `rig_api`, `skins_api`, …) |
| `scripts/` | every CLI tool; `scripts/README.md` indexes them all and a test fails if one is missing |
| `manifests/` | versioned run manifests (schema v1), the traceable way to run |
| `tests/` | pytest, one file per module; `PYTHONPATH=. pytest -q` |
| `hunyuan_mlx/` | Xiong's Hunyuan3D-MLX shape+paint port, MIT, tracked in-repo |
| `vendor/` | git-ignored third-party backends cloned by bootstrap scripts |
| `output/` | generated assets + `.provenance.json` sidecars, git-ignored |

`./lab` starts the studio; `./lab mcp` runs the MCP stdio server. `install.sh --dry-run` shows what
it would do; `--yes --no-start` is the non-interactive install. Installs code and Python 3.11 (~1–2
GB), **no weights**.

## Engines (the 3D step)

| Engine | Best for | Hardware | Licence | Download |
|---|---|---|---|---|
| Pixal3D (C++/GGML) ⭐ default | best results, one pass, keeps flat saturated colour | Mac, NVIDIA, AMD/ROCm | MIT code+flow weights, DINOv3 encoder | 8.4 GB |
| Hunyuan3D-MLX (full pipeline) | fast, clean | Mac | MIT code, Tencent community weights | separate |
| Hunyuan3D-MLX (dgrauet shape + Xiong paint) | cleanest shapes, manual setup | Mac | Tencent community | manual |
| Hunyuan3D-2.1 | Tencent's own shape + PBR, one run | NVIDIA Linux 24 GB+ | Tencent community | ~19.5 GB |
| TRELLIS.2 | highest fidelity, closest to upstream demo | Mac, NVIDIA | MIT + DINOv3 | ~15 GB |
| Stable Fast 3D | fastest, lowest fidelity | Mac | Stability AI community | gated |

On AMD, only Pixal3D (and the non-3D steps) are offered: TRELLIS.2 and Hunyuan need NVIDIA-only
compiled extensions upstream.

## The pipeline, as the studio runs it

Steps: `picture → back → model → finished → split → rigged → animated` (`skin` is a separate branch).
`planFor(have, want)` decides which run:

| what you have → what you want | steps |
|---|---|
| picture → moving character | `model, finished, rigged, animated` |
| picture → one prop | `model, finished` |
| picture → nine props | `model, split` |
| idea → character | `picture, model, finished, rigged, animated` |

`front_and_back` inserts `back` before `model`. `stop_after` / `up_to` ends the plan at any step.
Roughly 15–25 minutes for a character, start to finish, on their test Mac.

### Finish (the step that decides whether it looks good)

`finish_detail`: `light` (10k tri) | `game` (20k, default) | `more` (40k) | `high` (50k, surface
calmed where the picture paints) | `hero` (32k body + a head rebuilt from a close crop; needs a
Pixal3D model from a picture) | a count 1,000–200,000 | `full` (every triangle, for stills — cannot
be rigged).

`pixel_match` (default on) copies the real pixels of the source picture back onto every surface the
picture can see, so lettering, logos and faces survive the decimation. It is the difference between
"a red jacket" and "his jacket, with the badge".

Advanced knobs (`options` in MCP): `edge_fade` (off when hair gets specks or small eyes go dark),
`nudge` (off when a feature ghosts or slides on a turned head), `texture_size` (1024/2048/4096),
`rig_from` (`finished` or `model`), `speed`, `arms_away` (degrees, bulky characters), `keep_everything`.

### Rig and Animate

Auto-rig is **SkinTokens** (VAST-AI, MIT), cloned at setup, pinned commit
`273b691d35989d71cd17ff2895fdc735097b92d1`, checkpoints ~1.6 GB at HF revision
`79736cad0fd84de384d5eede659b4ebd24effe33`. Pure PyTorch, so it also runs on ROCm. Humanoids only
for now (a quadruped route exists separately, via Rigify).

Moves are 21 presets in `image_to_3dlab/motions/*.npz` (Kimodo, NVIDIA Open Model License; NVIDIA
claims no ownership of outputs), each fitted to the character so arms clear the body and feet stay on
the floor. `speed` and `arms_away` are the useful knobs.

## Agent interface (MCP)

`claude mcp add assetfurnace -- ~/image-to-3dlab/lab mcp` (or Codex / Kimi / JSON config; HTTP mode:
`./lab mcp --http` on 127.0.0.1:8779).

Tools: `status`, `list_engines`, `list_moves`, `create`, `continue_asset`, `run_status`, `cancel_run`,
`list_assets`, `options`, `list_versions`, `preview` (four sides, or four moments of a move),
`open_in_studio`, `model_info`, `show_in_folder`, `make_skin`, `skin_status`, `skin_picture`,
`approve_skin_picture`, `redo_skin_picture`, `build_skin`, `cancel_skin`, `report_bug`,
`install_backend`.

An agent cannot delete, hide or rename assets, and cannot paste an HF token. Runs continue after the
browser tab closes; `run_status` blocks, so don't poll it in a loop.

## Rules the project enforces on itself (adopt them)

- Every code addition gets a test; test the real artifact, import the logic, never `exec` a copy.
- Verify the environment **before** the expensive step — a missing import discovered after a 16-minute
  generation is a self-inflicted wound.
- Every run writes a `.provenance.json` sidecar recording every licence used; `validate_run_policy`
  gates generation on declared intent.
- Never download weights without the user's explicit choice.
- BRIA RMBG-2.0 must never be loaded by this repo's own generation pipeline; the TRELLIS backend
  refuses to run without the disable-patch present.
- Conventional Commits, small commits, no LLM co-author trailers, Keep-a-Changelog.

## What we cannot run here

This sandbox: 2 vCPU, 3 GB RAM, 20 GB disk, **no CUDA, no MPS, no ROCm, no Blender**. Every 3D
backend is therefore out of reach (Pixal3D needs compiled GGML + 8.4 GB; the rest need torch + a
GPU). `install_skills.sh` still installs the code, the pinned metadata and the read-only GLB tools,
and reports precisely which steps are available — which is the point: the *inspection* half
(`glb_stats`, `glb_turns`, provenance, motion catalogue) and the *authored-rig* fallback run here.
