# Space Museum (Filament) — Investigation Report

**Status: investigation only, per instructions. No rendering code has been changed.**
Branch: `arena/01a0a369-lm-arena`, HEAD at `e665122` (`fix(test): update CameraRigTest default
radius 2.5->3.5 after blue screen fix`).

This report answers Tasks 1–3 in order, with evidence (file:line, git history, CI artifact
sizes). Task 2's root cause is confirmed mathematically, not just asserted.

---

## Task 1 — Current implementation vs. spec

### 1.1 Entry point / scope-drift check

- Lab → `SpaceMuseumRoot` (`feature/museum/.../SpaceMuseumGridScreen.kt:56`) owns a 3-state
  internal nav: grid (`SpaceMuseumGridScreen`) → viewer (`SpaceMuseumViewerScreen`, one object) →
  credits. `selectedObjectId: String?` gates grid vs. viewer; there is **no third state that shows
  more than one body at once**.
- `InspectorEngine` (singleton, `core/engine/InspectorEngine.kt:23`) holds exactly **one**
  `scene`, and `loadEllipsoidObject()` always calls `releaseCurrentObject()` first
  (line 451) before adding a new renderable. `addEntity` is called exactly twice in the whole
  engine: once for the single directional "sun" light (line 149) and once for the single object
  renderable (line 558). There is no orbit path, no multi-body scene graph, no ephemeris/orbit
  propagation code anywhere in `core/engine`, `core/model`, `feature/museum`, or `feature/viewer`.
- **Verdict: no scope drift.** The one-object-at-a-time requirement is honored structurally. This
  part of the spec is *not* the source of the reported bugs.

### 1.2 Object catalog vs. asset reality

`core/model/ObjectRegistry.kt` declares 13 objects. Cross-referencing against `manifests/*.json`
(13 manifest files, 38 declared assets total per `docs/SOURCES.md`) and the actual repo contents:

| Object | Manifest exists | Declared asset types (per manifest) | Real binary asset in repo? | Runtime asset status |
|---|---|---|---|---|
| Sun | ✅ sun.json (3 assets) | SDO/AIA+HMI KTX2, EUV map, corona LUT | ❌ none found | **placeholder primitive** (flat baseColor sphere) |
| Mercury | ✅ mercury.json (3) | MESSENGER BDR/DEM/MD3 KTX2 | ❌ none | **placeholder primitive** |
| Venus | ✅ venus.json (3) | Magellan radar/topo KTX2, atmosphere LUT | ❌ none | **placeholder primitive** |
| Earth | ✅ earth.json (4) | Blue Marble, Black Marble night, DEM, atmosphere LUT | ❌ none | **placeholder primitive** |
| Moon | ✅ moon.json (3) | LROC WAC, LOLA DEM, Apollo NAC | ❌ none | **placeholder primitive** |
| Mars | ✅ mars.json (3) | CTX mosaic, MOLA DEM, HiRISE | ❌ none | **placeholder primitive** |
| Jupiter | ✅ jupiter.json (3) | JunoCam map, wind LUT, atmosphere LUT | ❌ none | **placeholder primitive** |
| Saturn | ✅ saturn.json (3) | Cassini map, ring-tau LUT, atmosphere LUT | ❌ none | **placeholder primitive** |
| Uranus | ✅ uranus.json (3) | Voyager/Hubble map, ring-tau, atmosphere LUT | ❌ none | **placeholder primitive** |
| Neptune | ✅ neptune.json (3) | Voyager/Hubble/JWST map, ring-tau, atmosphere LUT | ❌ none | **placeholder primitive** |
| Milky Way | ✅ milkyway.json (3) | UCAC4/BSC5 star VBO, Tycho-2 deep-map skybox, constellations | ❌ none | **placeholder primitive (ellipsoid)** |
| ISS | ✅ iss.json (2) | NASA glTF model + KTX2, module labels | ❌ none | **placeholder primitive** |
| Black Hole | ✅ blackhole.json (2) | Precomputed lensing LUTs (D, U, blackbody), Tycho-2 deep map | ❌ none | **placeholder primitive** |

`find . -iname "*.ktx2" -o -iname "*.glb" -o -iname "*.gltf" -o -iname "*.dds" -o -iname "*.astc"`
returns **zero results** anywhere in the repo (`assets/`, `assets-built/`, `assets-src/` are all
empty except a `.gitignore` and one unrelated brand logo). `manifests/*.json` are pure metadata
(product name, URL, credit, target format) written to satisfy the CI **provenance gate**
(`tools/ci/check_provenance.py`), not actual shipped textures. `tools/assetkit/Main.kt` is a CLI
that could theoretically fetch/encode real NASA imagery into KTX2 if pointed at source files and
invoked, but nothing in CI or the app build ever runs it, and no output landed in
`assets-built/`.

**Every one of the 13 objects renders as the same generic UV-ellipsoid with a single uniform
`baseColor`** (see 1.3). None have a bound albedo/normal/emissive texture at runtime. The
manifests document real NASA/ESA/EHT sources correctly on paper, but zero of that data reaches
the renderer.

### 1.3 Where materials are built — MaterialBuilder call sites

There are **two independent, disconnected material systems** in this codebase:

**(a) The "real" material library — dead code, never loaded.**
`core/engine/src/main/materials/*.mat` (16 files: `regolithSurface`, `icySurface`,
`gasGiantSurface` (+tier2), `cloudDeck`, `ringTransmission`, `solarSurface`, `coronaShell`,
`nightLights`, `starSprite`, `atmosphereShell` (+tier2), `blackHoleLens`, `modelSurface`,
`patchSurface`, `regolithSurface_tier2`). These are legitimate hand-authored Filament `.mat`
sources with per-body shading models, `sampler2d` parameters for albedo/normal/height/LUTs, and
correct `culling` directives per body (e.g. `blackHoleLens.mat:23` sets `culling : back`,
`starSprite.mat:21` sets `culling : none`). `blackHoleLens.mat` even implements gravitational
lensing math (deflection LUT sampling, disk temperature, blackbody lookup) and `starSprite.mat`
implements flux-preserving point sprites for the galaxy.

  - `core/engine/build.gradle.kts:51-89` defines a `compileFilamat` Gradle task that shells out to
    `matc` to compile these `.mat` → `.filamat`, wired as a dependency of `mergeDebugAssets` /
    `mergeReleaseAssets`.
  - **But**: the task's own guard (`build.gradle.kts:57-63`) checks for `matc` at
    `$FILAMENT_MATC` or `/tmp/filament/bin/matc`; neither exists in CI (`FILAMENT_MATC` is never
    set anywhere in `.github/workflows/*.yml` — confirmed by grep), so the task prints "matc not
    found ... skipping offline compilation" and returns immediately. **No `.filamat` file has
    ever been produced** (`find . -name '*.filamat'` → empty).
  - `MaterialManager.kt` (the Kotlin object meant to load these) has `loadFilamatFromAssets()`
    hard-coded to `return null` (line 61) and `buildRuntimePlaceholder()` hard-coded to
    `return null` (line 84) — both are explicit stubs, never implemented.
  - **`InspectorEngine` — the only class that actually renders anything — never references
    `MaterialManager` at all** (`grep -n "MaterialManager" InspectorEngine.kt` → no matches).
  - Net effect: 16 authored, per-body-correct materials (including the one with actual black-hole
    lensing shader code) are **100% dead code**. They are exercised only by unit tests that check
    the `.mat` *text files* contain the string `"material {"` (`MaterialTest.kt`) — those tests
    pass, but they prove nothing about runtime rendering.

**(b) What actually renders — one generic runtime material, shared by all 13 objects.**
`InspectorEngine.createDefaultMaterialLit()` (line ~180) builds a single `MaterialBuilder`
at app startup:
```kotlin
.shading(MaterialBuilder.Shading.LIT)
.uniformParameter(UniformType.FLOAT3, "baseColor")
.material("""
    void material(inout MaterialInputs material) {
        prepareMaterial(material);
        material.baseColor = materialParams.baseColor;
        material.roughness = 0.8;
        material.metallic = 0.0;
    }
""")
```
This one `Material` is cached (`currentMaterial`) and reused for **every** object. There are
three further fallback builders (`createDefaultMaterialUnlit`, `...Simple`,
`...Reflection`) that only differ in shading model (UNLIT) or in using reflection to call the
same API — none of them add textures. `loadEllipsoidObject()` (line 450) creates one
`MaterialInstance` per object and sets exactly one parameter:
```kotlin
matInstance.setParameter("baseColor", srgb, color[0], color[1], color[2])
```
where `color` comes from a hardcoded `OBJECT_COLORS` map (`InspectorEngine.kt:74-88`) — e.g.
`"sun" to floatArrayOf(1.0f, 0.9f, 0.3f)`, `"earth" to floatArrayOf(0.2f, 0.5f, 0.9f)`,
`"black_hole" to floatArrayOf(0.05f, 0.05f, 0.05f)`. **No texture is ever bound. No
sampler/`sampler2d` parameter exists on this material at all.**

Per-object-type answer to the checklist:
- **Shading model used**: `LIT` (Filament's standard PBR shading, hence "correct shading
  model" is technically true), with a fallback chain down to `UNLIT` if LIT fails.
- **Textures bound?** No — never. Only a uniform `float3 baseColor`.
- **Black hole / Milky Way dedicated shader?** No. Both route through the exact same
  `loadEllipsoidObject()` → same LIT material → same `OBJECT_COLORS` lookup as every planet.
  `black_hole` gets `floatArrayOf(0.05, 0.05, 0.05)` (near-black flat color); `milky_way` gets
  `floatArrayOf(0.1, 0.1, 0.2)` (dark blue flat color). Neither uses `blackHoleLens.mat` or
  `starSprite.mat` (see (a) above — those files are unreachable dead code).

### 1.4 Is Milky Way a distinct render path?

**No.** `milky_way` in `ObjectRegistry` has `oblateness = 0.0`, so
`GeometryGenerator.generateEllipsoid()` (`Geometry.kt:33`) produces a plain UV sphere for it —
the exact same function used for every planet, just with `oblateness=0` instead of e.g. Saturn's
`0.09796`. It is loaded via the same `loadEllipsoidObject("milky_way", tier)` call, gets the same
LIT material, and is tinted `floatArrayOf(0.1, 0.1, 0.2)` from `OBJECT_COLORS`. This is precisely
the "generic 3D ball with a different color" failure mode the task warned about — confirmed, not
speculative. The Canvas fallback (`SpaceMuseumViewerScreen.kt`, used only when Filament fails)
does draw a moon/earth-style shaded circle for `milky_way` too, with no skybox/particle
distinction from any rocky body. There is no skybox, no star-catalog VBO rendering, no
particle/point-sprite system wired into the Filament path at all, despite `starSprite.mat` and
`StarCatalogIngestion.kt` (`tools/assetkit/`) existing as isolated, unconnected code.

### Task 1 summary table

| Check | Result |
|---|---|
| One-object-at-a-time (no orbit/solar-system scene) | ✅ Correct, no scope drift |
| 13 objects have real PBR textures | ❌ 0 of 13 — all placeholder primitives (flat baseColor ellipsoid) |
| Sun emissive | ❌ Not emissive; same LIT/baseColor path as every other body |
| Black hole: lensing/accretion disk | ❌ Flat near-black sphere; `blackHoleLens.mat` exists but is dead code |
| Milky Way: skybox/particle galaxy | ❌ Flat dark-blue sphere; `starSprite.mat`/star catalog exist but are dead code |
| Rotate via drag / pinch zoom | ✅ Implemented per-object in `CameraRig` + gesture handling in `SpaceMuseumViewerScreen` |

---

## Task 2 — Root-cause the blue/black-screen ("nothing renders") bug

Checked in the order requested. **Root cause: (1), the culling/winding mismatch — confirmed
mathematically below.** The team's own commit history shows they never actually found this;
instead they progressively patched symptoms (D-076 through D-080 in
`docs/decisions/DECISIONS.md`) and ultimately worked around the bug by **pulling the camera back
until the un-renderable geometry no longer dominated the frame** — exactly the anti-pattern this
task says not to accept.

### 2.1 Culling / winding — ROOT CAUSE, confirmed

`GeometryGenerator.generateEllipsoid()` (`core/engine/Geometry.kt:74-88`) builds indices as:
```kotlin
// first = lat*(lonSegments+1)+lon, second = first+lonSegments+1
indices.add(first);  indices.add(second);   indices.add(first + 1)   // triangle A
indices.add(second); indices.add(second+1); indices.add(first + 1)   // triangle B
```
I evaluated this winding numerically for a UV sphere at `latSegments=16, lonSegments=32`
(896 non-polar triangles) using the exact vertex formula in `generateEllipsoid()`
(`x=sinθcosφ, y=cosθ, z=sinθsinφ`) and computed each triangle's face normal via the right-hand
rule (`cross(B-A, C-A)`) against the true outward radial direction at its centroid:

```
outward-facing (normal · outward > 0) triangle count: 0
inward-facing  (normal · outward < 0) triangle count: 896
```

**100% of the mesh's triangles wind clockwise as seen from outside the sphere** — i.e. every
triangle's front face (by the right-hand rule / CCW convention) points *into* the sphere, not
out toward the camera. This is the exact inverse of OpenGL/Filament's default front-face
convention (CCW-in-window-space = front-facing), which is also the default assumed by
`blackHoleLens.mat` / `starSprite.mat`'s `culling : back|none` directives.

Filament materials default to `culling : back` (confirmed:
[Filament materials doc](https://google.github.io/filament/main/materials.html) — *"culling ...
Defaults to back"*), and the runtime `MaterialBuilder` path used by `InspectorEngine` never sets
`.culling(...)` at all, so it also inherits the `back`-culling default. With every triangle's
front face pointing inward, **every single triangle is classified as back-facing and discarded**
before rasterization — a textbook explanation for "hasActiveRenderable/hasMaterial true, zero
pixels drawn."

The engineers already stumbled onto the *culling* half of this without diagnosing winding:
```kotlin
// InspectorEngine.kt:549-552
// Try to disable culling via reflection to ensure visibility
val cullingMethod = builder.javaClass.getMethod("culling", Boolean::class.javaPrimitiveType)
cullingMethod.invoke(builder, false)
```
This calls `RenderableManager.Builder.culling(false)` — which the task brief correctly flags as
**a red herring**: that API disables *frustum* culling (whether the whole renderable is tested
against the view frustum), not per-triangle backface culling, which is a *material* property
(`MaterialBuilder.culling(CullingMode.NONE)` or the `.mat` file's `culling` block). Since the
runtime `MaterialBuilder` in `createDefaultMaterialLit()`/`...Unlit()`/`...Simple()` never sets
material-level culling, and the mesh winding is inverted, **this line has no effect on the actual
bug** — it "fixes" a different problem than the one present.

**Evidence this was never actually root-caused**: `docs/decisions/DECISIONS.md` D-076–D-080 walk
through five successive "black screen" hotfixes (rewriting `FilamentView`'s `UiHelper` wiring,
fixing a `setViewport(surface.hashCode(), surface.hashCode())` bug, fixing a `renderableCount`
bookkeeping bug that mislabelled "leaked:1", reordering the fallback `Canvas` to sit *behind* vs.
*in front of* the `SurfaceView`) — none of which mention winding order or per-triangle culling.
The actual shipped "fix" was in `CameraRigTest.kt` / `CameraRig.kt`:
```kotlin
// CameraState default, CameraRig.kt:18
val radius: Float = 3.5f, // increased from 2.5 to 3.5 to avoid filling screen with solid color (blue screen bug)
```
and the matching camera zoom-out limits (`maxRadius = 5.0f`, up from an implied smaller value).
**This is precisely the "shrink/adjust until something is guaranteedly visible" anti-pattern the
task instructs not to accept as a fix.** Because the mesh is fully culled regardless of distance,
moving the camera does not fix rendering — it only changes how much of the flat clear-color fills
the frame vs. background, which is why the team's own comment calls it "avoid filling screen with
solid color" rather than "fix invisible geometry." The geometry is still 100% invisible at any
camera distance; the change just made the failure less visually dominant in manual screenshots.

**Fix required (not applied by me, per instructions):** either (a) reverse the index winding
in `generateEllipsoid()` (swap the second and third index in each triangle — `first, first+1,
second` / `second, first+1, second+1`), or (b) set `.culling(CullingMode.NONE)` explicitly on the
runtime `MaterialBuilder` (matches what the authored-but-unused `.mat` files with `culling: none`
already assume for atmosphere/rings/stars) — while also fixing (a) for the opaque-body materials
that do want backface culling for overdraw/perf reasons, per §15.2's draw-call budgets.

### 2.2 Index/triangle count and offset in `RenderableManager.Builder.geometry()`

`InspectorEngine.kt` line ~554: `builder.geometry(0, RenderableManager.PrimitiveType.TRIANGLES,
vb, ib)` — this 4-argument overload uses the full index buffer (offset 0, count =
`ib.indexCount` as passed to `IndexBuffer.Builder().indexCount(indexCount)` at line ~503, which
is `mesh.indices.size`, itself `6 * latSegments * lonSegments`, i.e. exactly what
`generateEllipsoid()` produced). **No mismatch found here.** This is not a contributing cause.

### 2.3 VertexBuffer upload correctness

Buffer creation (`InspectorEngine.kt:478-486`):
```kotlin
val vertexBufferData = ByteBuffer.allocateDirect(vertexCount * vertexSize).order(ByteOrder.nativeOrder())
val floatBuffer = vertexBufferData.asFloatBuffer()
for (v in mesh.vertices) { floatBuffer.put(v.x); ... }
floatBuffer.flip()
```
`.flip()` is called *after* filling, *before* `setBufferAt` — correct order. `vertexSize = 32`
bytes (8 floats: pos xyz + normal xyz + uv). Attribute descriptors:
```kotlin
.attribute(POSITION, 0, FLOAT3, 0, vertexSize)
.attribute(UV0, 0, FLOAT2, 24, vertexSize)
```
Position offset 0 (3 floats = 12 bytes), UV0 offset 24 (bytes 24-31, i.e. floats 6-7 = u,v) — this
matches the interleaving order in the write loop (`x,y,z,nx,ny,nz,u,v`, so u,v are indeed at byte
offset 24). **However, NORMAL (bytes 12-23) is never declared as a vertex attribute at all** —
only POSITION and UV0 are registered on the `VertexBuffer.Builder`. This means the LIT material's
implicit shading (which needs `getWorldNormal()`/interpolated normal to compute N·L) has **no
normal input bound**, so even if culling were fixed, lighting would be flat/undefined per-vertex
rather than using the correctly-computed sphere normals already written into the buffer. This is
a **second, independent bug** that would surface immediately after fixing 2.1: geometry would
become visible, but shading would look wrong (uniform lit or black) because Filament has no
NORMAL attribute to read. It is not itself the "nothing renders" cause (materials with no bound
normal still rasterize), but it is directly relevant to the "correct relative shading from
directional light" requirement in the spec and should be fixed alongside 2.1.

`bufferCount(1)` and `vertexCount(vertexCount)` match the actual write (`mesh.vertices.size`
positions written). **No count/offset mismatch in the vertex path.**

### 2.4 Material parameter binding

```kotlin
matInstance = currentMaterial!!.createInstance()   // instance created first
matInstance.setParameter("baseColor", srgb, color[0], color[1], color[2])  // then set on instance
```
`setParameter` is called on the `MaterialInstance` returned by `createInstance()`, not on the
shared `Material`, and it's called after instance creation. **This is correct** — not a
contributing cause. (Reflection wrapping around `Colors.RgbType` with a plain-float fallback adds
complexity but doesn't change correctness here.)

### 2.5 `beginFrame` return value

```kotlin
val shouldRender = method2.invoke(r, sc, frameTimeNanos) as Boolean
if (shouldRender) { r.render(v); r.endFrame() }
```
(and the 1-arg fallback identically). The return value **is** checked, and `render`/`endFrame`
are only called when `true`. Ruled out explicitly, as requested — this is not a stable
"blue/black screen" cause; if `beginFrame` failed silently the symptom would be intermittent frame
drops, not a consistent uniform fill. **Confirmed not the cause.**

### Task 2 verdict

| Candidate cause | Verdict |
|---|---|
| **Culling/winding mismatch** | **✅ ROOT CAUSE.** 896/896 (100%) of sampled sphere triangles wind with inward-facing normals against Filament's CCW-front / back-cull default. Confirmed numerically. |
| Index/triangle count or offset | Ruled out — count and offset are correct |
| VertexBuffer upload (flip, strides) | Position/UV0 correct; **NORMAL attribute never registered** — separate real bug, will surface as wrong shading (not blackout) once (1) is fixed |
| Material parameter binding | Ruled out — correct instance, correct order |
| `beginFrame` return value | Ruled out — checked correctly |
| **Actual shipped "fix"** | Camera default radius raised 2.5→3.5 (`CameraRig.kt:18`, `CameraRigTest.kt`) — a distance/visibility workaround, not a geometry fix. The underlying culling bug is still present in the code inspected in this session (`HEAD=e665122`) — it was never fixed. |

---

## Task 3 — APK size jump (18.1 MB → 43.9 MB zip)

Local `gradle`/`java`/Android SDK tooling could not be run in this sandbox (no outbound network
access to `dl.google.com`, `services.gradle.org`, `repo1.maven.org` from inside the shell, and no
JDK preinstalled), and GitHub Actions artifact *contents* could not be downloaded either (the
Azure Blob Storage backend GitHub uses for artifact bytes,
`productionresultssa*.blob.core.windows.net`, is not reachable from this sandbox — TLS handshake
fails). However, the GitHub Actions **API** (`api.github.com`, which *is* reachable) exposes
exact artifact byte sizes without needing to download the blob, and Maven Central's `repo1.maven.org`
directory listings are also reachable, which together let this be root-caused precisely rather
than estimated.

### 3.1 Real before/after sizes (from GitHub Actions artifact metadata, not estimated)

| Build | Commit | CI run | Artifact (`ZIG-release.apk`, wrapped in GH's zip container) |
|---|---|---|---|
| **Before** (docs-only "M-1" commit, pre-Filament) | `e7f2279` | [34869156782](https://github.com/alimehali-cyber/Lm-arena/actions/runs/34869156782) | **19,019,983 bytes ≈ 18.14 MB** |
| **After** (first successful Space Museum build) | `e16d382` | [34883388991](https://github.com/alimehali-cyber/Lm-arena/actions/runs/34883388991) | **45,966,895 bytes ≈ 43.84 MB** |
| **Current HEAD** | `e665122` | [34905712995](https://github.com/alimehali-cyber/Lm-arena/actions/runs/34905712995) | **46,003,641 bytes ≈ 43.88 MB** |

These numbers match the task prompt's figures almost exactly (18 MB → 43 MB "zip" size — this is
GitHub's artifact-container zip, one level up from the APK itself, but since APKs are already
near-incompressible zips of compressed content, the two numbers track closely; both would round
to "18MB→43MB zip" and "≈19MB→≈46MB APK" as stated in the prompt). **The entire ~26 MB jump
happens in a single step**: the first commit that adds the `core:engine` module's Filament
dependencies (`174e591 feat(museum-m1): engine core, HDR, camera, first object`) through the
first successful build after all the M1–M12 feature work (`e16d382`) — i.e. it is attributable to
the Space Museum feature module as a whole, not to gradual drift across many unrelated commits.
Every subsequent "fix" commit (`8bab9b48` → `e6651227`) shows the APK oscillating by only
**tens of KB**, confirming the code-level bug fixes (WindowInsets deps, icon imports, reflection
wrappers, etc.) contributed negligibly — the jump is dominated by one thing added once.

### 3.2 `lib/` vs `assets/`/`res/` — which directory is responsible

Since the actual built APK's `lib/` contents could not be unzip-listed in this sandbox (no
network access to fetch the artifact blob, no local Gradle/Android toolchain available to build
one from source), I identified the specific native libraries pulled in by dependency inspection
of `core/engine/build.gradle.kts` and Maven Central's published artifact sizes (fetched directly,
not estimated):

```kotlin
// core/engine/build.gradle.kts
implementation("com.google.android.filament:filament-android:1.71.5")
implementation("com.google.android.filament:gltfio-android:1.71.5")
implementation("com.google.android.filament:filament-utils-android:1.71.5")
implementation("com.google.android.filament:filamat-android:1.71.5")
```

Published `.aar` sizes for these exact pinned versions (from `repo1.maven.org`, retrieved
directly):

| Artifact | `.aar` size (all 4 ABIs, unstripped-for-app-purposes) |
|---|---|
| `filament-android-1.71.5.aar` | 5,220,720 bytes (≈ 4.98 MB) |
| `gltfio-android-1.71.5.aar` | 7,058,504 bytes (≈ 6.73 MB) |
| `filament-utils-android-1.71.5.aar` | 1,198,245 bytes (≈ 1.14 MB) |
| **`filamat-android-1.71.5.aar`** | **13,059,036 bytes (≈ 12.45 MB)** |
| **Sum of all four `.aar`s** | **26,536,505 bytes ≈ 25.31 MB** |

**This sum (≈25.3 MB) alone accounts for essentially the entire observed 26 MB APK growth.**
`filamat-android` is by far the single biggest contributor at ~12.4 MB — it bundles `matc`'s
runtime shader-compiler stack (glslang + SPIRV-Tools + SPIRV-Cross, needed only to *compile*
`.mat`→`.filamat` at either build- or run-time) for **all four ABIs**
(`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`) inside one "universal" `.aar`, per Filament's own
build docs ("The AAR is a universal AAR that contains all supported build targets... To filter
out unneeded ABIs, rely on the `abiFilters`"). None of `app/build.gradle.kts` or
`core/engine/build.gradle.kts` in this repo declare any `ndk { abiFilters ... }` or `splits {
abi { ... } }` block (confirmed by `grep -rn "abiFilters\|splits" **/build.gradle.kts` → no
matches), so **the release build ships native code for all four ABIs, unfiltered, uncompressed
further, debug symbols not stripped** — the default AGP behaviour for AARs that don't provide
per-ABI split points on their own.

This is the concrete "specific file/library responsible" the task asked for, not a generic size
tip: **the app depends on `filamat-android` (the material-builder / shader-compiler package,
~12.4 MB across 4 ABIs) purely so `InspectorEngine.createDefaultMaterialLit()` can compile one
trivial LIT/baseColor shader *at runtime, on every app cold start* — see Task 1.3.** The
authored `.mat` files were meant to be compiled *offline* via `matc` into small precompiled
`.filamat` blobs (which is what `filamat-android` is normally used for during a desktop build,
*not* shipped inside the app) and loaded via the much smaller `filament-android` runtime alone.
Because `compileFilamat` never actually runs (§1.3/2.1 above — no `matc` binary present in CI),
the code fell back to bundling the full `filamat-android` **runtime** shader compiler inside the
shipped app, which is both (a) unnecessary bloat and (b) the reason a phone has to
recompile GLSL from a string literal on every single launch instead of loading a small
precompiled binary blob.

`gltfio-android` (~6.7 MB) is also fully unused at runtime: nothing in `InspectorEngine`,
`FilamentView`, or any viewer code ever calls into `gltfio` to load a `.glb`/`.gltf` model — the
ISS "asset" (`manifests/iss.json` declares a "NASA ISS high-res model... glTF + KTX2") is, like
every other object, actually rendered as the same procedural UV ellipsoid via
`loadEllipsoidObject()`. `gltfio-android` is dead weight pulled in only because it was declared in
the dependency block "for later," per the M1 commit message ("engine core... first object").

### 3.3 `assets/` / `res/` contribution

As established in Task 1.2, **zero texture/KTX2/glTF binary assets exist anywhere in the repo or
in `feature/museum/src/main/assets/manifests/`** (that directory holds only the 13 small JSON
manifest *text* files, copied from `manifests/`, a few KB each — not imagery). So there is no
"someone added an uncompressed 8K skybox" cause here; the manifests describing 8K/16K KTX2
textures are aspirational documentation with no corresponding files. **`assets/`/`res/` growth is
not a meaningful contributor to this size jump** — it is entirely a `lib/` (native library)
story, specifically the choice to depend on the full multi-ABI `filamat-android` package instead
of either (a) compiling materials offline in CI and dropping the runtime `filamat-android`
dependency from the shipped `:app` artifact, or (b) at minimum restricting `abiFilters` to the
1–2 ABIs actually needed for real-device testing (e.g. `arm64-v8a` only, or `arm64-v8a` +
`armeabi-v7a`), which per Filament's own docs and general Android ABI-split guidance would cut
the native-library contribution by roughly 50–75%.

### Task 3 summary

| Question | Answer |
|---|---|
| Before / after size (real, from CI, not estimated) | 19,019,983 B → 45,966,895 B (first museum build) → 46,003,641 B (current HEAD) |
| Where did it come from: `lib/`, `assets/`, or `res/`? | **`lib/`** — four new Filament Maven dependencies |
| Specific files responsible | `filamat-android-1.71.5.aar` (~12.4 MB, all 4 ABIs) is the single largest; `gltfio-android-1.71.5.aar` (~6.7 MB, entirely unused — no glTF ever loaded); `filament-android` (~5.0 MB, the only one actually needed) + `filament-utils-android` (~1.1 MB) round out the ≈25.3 MB total, matching the observed ≈26 MB delta |
| Why is `filamat-android` present at all? | Because offline `matc` compilation (`compileFilamat` Gradle task) silently no-ops in CI (no `matc` binary available), so the app falls back to compiling the one generic LIT/baseColor shader **at runtime** using the full shader-compiler package, instead of shipping small precompiled `.filamat` blobs |
| Is `gltfio-android` needed? | No — nothing in the render path calls into `gltfio`; it can be dropped entirely today with zero functional change |
| ABI filtering present? | No `abiFilters`/`splits` block anywhere in the Gradle config — all 4 ABIs (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`) ship unfiltered |

---

## Reporting-requirement checklist (per prompt)

- ✅ Blue/black-screen bug **not** reported fixed by shrinking geometry — instead, its actual
  current "fix" (camera radius 2.5→3.5, `CameraRig.kt:18`) is called out explicitly as the
  anti-pattern the task warned about, and the real cause (inverted mesh winding vs. Filament's
  back-cull default) is identified and left unpatched pending direction from you.
- ✅ The 409-passing-tests claim is **not** treated as sufflicient evidence of correctness:
  `MaterialTest.kt` only asserts `.mat` files are plain-text files containing the substring
  `"material {"` — it never invokes Filament, never binds a texture, never renders a pixel, and
  never even touches the runtime `MaterialBuilder` path in `InspectorEngine` that is actually
  used. `CameraRigTest.kt` only checks camera math, not what's on screen. There is **no
  screenshot/golden-image test, no texture-binding test, and no per-object-shader test anywhere
  in the suite** — the CI green checkmarks say nothing about whether Earth looks like Earth or a
  blue ball. This test suite is inadequate to catch this entire class of bug and should not be
  used as evidence that the feature is complete.
- ✅ Before/after APK *and* zip sizes reported with the specific files responsible for the delta
  (§3.1–3.2), sourced from GitHub's Actions API (exact byte counts) and Maven Central (exact
  published `.aar` sizes), not guessed.

## What was and wasn't done in this session

Per your instructions this was investigation-only — **no rendering code, Gradle config, or
material files were modified**. The concrete fixes implied by this report (reverse triangle
winding or set `.culling(NONE)` explicitly + bind the NORMAL attribute + wire `MaterialManager`'s
real `.mat` library into `InspectorEngine` per object + drop `gltfio-android` + either restrict
`abiFilters` or make offline `matc` compilation actually run in CI so `filamat-android` doesn't
need to ship in the release APK) are ready to be implemented as a follow-up once you confirm
you'd like me to proceed.
