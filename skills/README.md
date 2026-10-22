# Skills

Agent-facing procedures for this repository. Each skill is a `SKILL.md` with YAML frontmatter
(`name`, `description`) plus `references/` for the detail that would otherwise bloat the main file.
They are written to be read by an agent at the start of a task, and by a person who wants to know how
a thing is actually done.

| Skill | Use it when |
|---|---|
| [`zig-home-scene`](zig-home-scene/SKILL.md) | changing the Home sky scene, its layers, clocks, palette or the observer |
| [`image-to-3d-avatar`](image-to-3d-avatar/SKILL.md) | turning reference photos into a rigged, animated avatar GLB |
| [`zig-avatar-runtime`](zig-avatar-runtime/SKILL.md) | putting that avatar into the app, grading it, keeping it fast |

## Installation status

`image-to-3d-avatar` wraps **AssetFurnace** (`Bingeljell/image-to-3dlab`, Apache-2.0). Install it with:

```bash
bash tools/avatar/install_skills.sh            # clones the pinned commit into vendor/, checks the machine
bash tools/avatar/install_skills.sh --check    # just report what is available
```

What the script installs here: the pinned source, its read-only GLB tools, the motion catalogue
metadata and the licence/provenance rules. What it cannot install on this machine — and says so — is
any of the six 3D backends: they need CUDA, MPS or ROCm, and this sandbox has none.

The route that runs with no GPU at all is `tools/avatar/` (authored mesh, rig and clips, written by
this repository). Both routes produce the same asset contract
([`zig-avatar-runtime/references/asset-contract.md`](zig-avatar-runtime/references/asset-contract.md)),
so the app does not care which one built a character.

## Third-party material

- AssetFurnace is Apache-2.0; keep its NOTICE and credit the project on redistribution (its section 4(d) asks the same).
- Model weights are not shipped and are never downloaded without an explicit choice.
- Motion clips in AssetFurnace are made with NVIDIA Kimodo (NVIDIA Open Model License); credit it
  wherever those clips are used, and check its terms before redistributing the raw `.npz` files.
