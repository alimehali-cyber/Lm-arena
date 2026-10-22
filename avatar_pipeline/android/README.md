# Avatar runtime for ZIG — isolated integration

**Status: written, never compiled, never run on a device.** There is no emulator and no
phone in this environment, and no Android SDK, so this Kotlin has not been through a
compiler or a GPU. Treat it as a reviewed draft with a documented contract, not as
working app code. `reports/FINAL_REPORT.md` §8 records this honestly.

Nothing in this folder is wired into ZIG, and it is still not the shipping path.

As of the Professor Z work, the Home scene draws the character as a **baked sprite strip**
(`app/src/main/res/drawable-nodpi/hero_profz_idle.png`, rendered from the same `.blend` by
`blender/bake_hero_sprite.py`) instead of a live GL surface. Reason: a GLSurfaceView inside a
Compose hero in a scrolling card is exactly the kind of change that fails invisibly — black
surface, wrong z-order, EGL context loss — and there is no device here to catch it. The GLB
runtime below remains the path to take once a device can run it; the GLB it needs is
`exports/profz/avatar.glb`.

---

## What is here

```
android/
├── assets/                       put avatar.glb here (see below)
└── src/com/zig/avatar/
    ├── Glb.kt                    GLB container + accessor decoding (no dependencies)
    ├── AvatarModel.kt            mesh/skin/morph/animation decode, bounds
    ├── M4.kt                     4x4 math, quaternions, easing (no allocations/frame)
    ├── AvatarPlayer.kt           clip sampling, cross-fade, skin matrices, morph weights
    ├── VisemeTimeline.kt         reads tools/lipsync.py output, drives visemes
    ├── AvatarRenderer.kt         GLES 3.0 renderer: CPU skinning, CPU morph, 1 shader
    └── AvatarView.kt             GLSurfaceView + Choreographer frame loop + reduce-motion
```

Seven files, one package, no dependencies outside the Android framework and `org.json`.
Nothing references `com.alijafari.red.astronomy`, so deleting the package cannot break
the app.

## The smallest safe way to try it

1. Copy `android/src/com/zig/avatar/` into `app/src/main/java/com/zig/avatar/`.
   (A new path, no existing file moved.)
2. Copy `exports/test01/avatar.glb` to `app/src/main/assets/avatar/avatar.glb`.
3. Add one screen — a new file, new route, not reachable from the current navigation
   unless you add it yourself:

```kotlin
@Composable
fun AvatarPreviewScreen() {
    AndroidView(
        factory = { ctx ->
            AvatarView(ctx).apply {
                load("avatar/avatar.glb")
                playClip("Idle_Breathing")
            }
        },
        update = { /* no-op */ },
    )
}
```

4. Host lifecycle hooks (only if the screen can be backgrounded):

```kotlin
val v = remember { AvatarView(context) }
DisposableEffect(Unit) {
    onDispose { v.onHostPause() }
}
```

To undo: delete the package folder, the asset, and the screen. There is no migration, no
database, no manifest entry, no gradle change.

### Deliberately not done

- No `GLSurfaceView` inserted into the existing astronomy `TextureView` pipeline. ZIG
  renders astronomy with GLES30 in its own surface; mixing two EGL contexts inside one
  screen is the riskiest possible first step.
- No changes to the release build, signing config, `minSdk`, `targetSdk`, manifest, or
  any dependency block.
- No Vulkan, no NDK, no native code, no C++.
- No Compose animation, no third-party glTF library (a new dependency would be the one
  thing that makes the change expensive to reverse).

## Runtime contract (verified against the shipped GLB)

The loader assumes exactly what `tools/validate_glb.py` enforces, and fails loudly if a
future export breaks it:

| Assumption | Value in `exports/test01/avatar.glb` |
|---|---|
| GLB container, glTF 2.0 | yes, single BIN chunk |
| Skins | exactly 1 |
| Joints | 31, all named, `root` present |
| Primitives | 7 (one per material) |
| Attributes per primitive | `POSITION`, `NORMAL`, `JOINTS_0`, `WEIGHTS_0` |
| Morph targets | 17 per primitive, POSITION deltas only |
| Morph names | carried in `mesh.extras.targetNames` |
| Animations | 21, LINEAR on every channel that changes, STEP on constants |
| Vertices / triangles | 17,089 / 33,988 |
| External URIs | none |

Numbers come from `exports/test01/09_validate.json` and `manifest.json`, not from
memory; `tools/validate_glb.py` fails the build if they drift.

## Clip names the runtime expects

```
Idle_Breathing  Idle_LookAround  Blink_Natural
Expression_Happy  Expression_Sad  Expression_Surprised
Expression_Thinking  Expression_Curious  Expression_Frustrated
Gesture_Explain  Gesture_Greet  Gesture_Point  Gesture_Emphasize
Talk_Neutral  Talk_Expressive  Walk  Turn  React_Positive  React_Negative
```

`AvatarPlayer.play()` returns `false` for an unknown name instead of throwing, so a
missing clip degrades to "nothing happens" rather than a crash.

## Performance reasoning (not measurements)

No frame timings were taken on hardware; there is none. The budget below is arithmetic.

- CPU skinning: 17,089 vertices × 4 influences ≈ 68k matrix-vector products per frame.
  On any phone that can run ZIG's astronomy shader, this is a few hundred microseconds.
- Morph blending only touches targets with weight > 0.002, so a neutral face costs one
  branch per target instead of 17 adds.
- Upload: 2 × 17,089 × 3 floats ≈ 410 KB per frame via `glBufferSubData`. That is the
  real cost, and the reason to move skinning to the vertex shader if profiling ever says
  so.
- Fragments: 34k triangles, one draw call per material (7), no texture binds.

If it is too slow on a target device, the first fix is vertex-shader skinning with a
`mat4[31]` uniform (496 floats, within the GLES 3.0 guaranteed 256 vec4 budget), not a
new renderer.

## Reduce motion

`AvatarView` reads `Settings.Global.ANIMATOR_DURATION_SCALE` (default-value overload) and
re-reads it on resume. At 0 the player stops advancing time, so the character holds its
pose instead of looping. This matches Android's own animator behaviour and the web's
`prefers-reduced-motion`.

## Known gaps before this could ship

1. Never compiled; expect typos. There is no CI for it here.
2. `GLSurfaceView` inside a Compose screen can conflict with the existing `TextureView`
   surface if both are visible at once. Keep them on separate screens.
3. Asset loading is synchronous on the GL thread (~1.6 MB, milliseconds) — acceptable,
   but a first-frame stall is possible on very slow storage.
4. No texture support: materials are flat colours. The export has no textures, so this
   is consistent, not a regression.
5. No shadows, no ground plane, no anti-aliasing config. A widget-sized character will
   want at least MSAA.
