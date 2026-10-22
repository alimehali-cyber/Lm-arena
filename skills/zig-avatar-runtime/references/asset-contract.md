# Avatar asset contract (v1)

A single self-contained binary glTF (`.glb`) plus one JSON sidecar. If a file does not satisfy every
line here, the app must refuse it gracefully and keep the vector observer.

## File

| Rule | Value |
|---|---|
| Container | `.glb`, single buffer, no external URIs, no `KHR_draco_mesh_compression`, no KTX2 |
| Unit / axes | metres, Y up, **+Z forward** (character faces the camera), origin between the feet |
| Height | 1.55–1.95 m, feet at y = 0 |
| Orientation | rig in A-pose bind; arms ~45° from the body |
| Triangles | ≤ 12,000 (hard cap 20,000) |
| Materials | 1 opaque PBR metallic-roughness material; base colour texture ≤ 1024², or `COLOR_0` only |
| Textures | PNG or JPEG, embedded in the buffer |
| Animations | ≤ 8, 24 fps, quaternion rotation channels, `LINEAR` interpolation |

## Skeleton — exactly these bone names

```
hips
├─ spine ─ chest ─ neck ─ head
│           ├─ shoulder.L ─ upper_arm.L ─ forearm.L ─ hand.L
│           └─ shoulder.R ─ upper_arm.R ─ forearm.R ─ hand.R
├─ thigh.L ─ shin.L ─ foot.L
└─ thigh.R ─ shin.R ─ foot.R
```

- 19 bones, hierarchy as drawn, `hips` is the root of the skin.
- Side convention: `.L` is the character's left (screen right when facing the camera).
- ≤ 4 influences per vertex, weights normalised to 1 ± 0.001.
- `inverseBindMatrices` present, column-major.
- No extra leaf/helper bones; a jaw or eye bone is the only allowed addition, and only if named
  `jaw` / `eye.L` / `eye.R`.

## Clip names

| Clip | Loop | Length | Used when |
|---|---|---|---|
| `idle_breathe` | seamless | 8.0 s | always, underneath |
| `day_shade` | seamless | 8.0 s | Sun altitude > +3° |
| `dusk_phone` | seamless | 8.0 s | +3° … −7° |
| `night_point` | seamless | 8.0 s | < −7° |
| `wave_hello` | once | 3.0 s | optional, user-triggered |

A clip that loops must have `pose(t=0) == pose(t=length)` on every channel. Time starts at 0. The
first clip in the file is `idle_breathe`.

## Sidecar `<name>.glb.provenance.json`

```json
{
  "schema": "zig.avatar/1",
  "asset": "<name>",
  "created": "2026-10-22",
  "route": "assetfurnace | authored-fallback",
  "engine": { "name": "Pixal3D", "version": "...", "licence": "MIT" },
  "weights": [{ "name": "...", "licence": "...", "url": "..." }],
  "rig": { "tool": "SkinTokens", "licence": "MIT", "revision": "..." },
  "motions": [{ "name": "idle", "licence": "NVIDIA Open Model License" }],
  "source_photos": ["front.jpg", "side.jpg"],
  "consent": "user-owned photographs; subject consented to a 3D likeness",
  "stats": { "triangles": 8421, "bones": 19, "textures": 1, "bytes": 421337 }
}
```

## Rejection behaviour in the app

- Missing `idle_breathe` → keep the observer, log once.
- Unknown bones → bind what is named, ignore the rest, never crash.
- Scale out of range → normalise height to 1.70 m and note it.
- Texture > 2048² → downscale at import, don't refuse.
- Any parse failure → keep the observer; the Home scene must never show a hole where a person was.
