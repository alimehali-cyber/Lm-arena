# Static comparison: BlackHole XAPK 1.0.0 vs. Chal in Lab

**Reviewed:** 2026-10-01

**Scope:** static reverse engineering plus implementation of an XAPK-compatible native Chal backend and a capability-gated GLES fallback. No Android device run was possible.

## Executive summary

The Lab route now prefers the XAPK’s original ARM64 Vulkan renderer through a JNI-compatible `com.orchestrsim.blackhole.NativeBridge`, forwards its `Surface` lifecycle, sends its 16-float/four-int contract, and ports the XAPK camera drag/pinch behavior and physical presets. It starts with the XAPK’s defaults and persists its physical controls and quality under `bh.params`; camera state starts from the XAPK defaults and uses its gesture contract. Devices that do not report Vulkan 1.1 or lack ARM64 use Chal’s GLES renderer with an explicit fallback notice.

The XAPK itself remains an imperfect **UI-readout** oracle: its Compose telemetry sends `mass × spin` to Rust Kerr helpers that expect normalized spin. The new adapter deliberately sends dimensionless spin to those helpers, matching the XAPK’s separate mass/spin renderer state without copying the readout bug. The bundled SPIR-V remains opaque; using the exact same native renderer is the practical path to rendering parity, but static inspection still cannot prove the native shader’s underlying physics.

The GLES fallback also received targeted fixes: normalized spin is no longer multiplied by mass twice, the frame-drag control is wired into the force, descending-edge `smoothstep` calls are defined, GL variants are invalidated across context loss and failures are surfaced, and redshift uses `z=1/g-1`. Its effective force model and some coordinate approximations remain; on that fallback path Chal is **not pixel/physics-identical** to the XAPK.

## What was inspected and limits

- XAPK: [`docs/BlackHole+-+Physics+Simulator_1.0.0_APKPure.xapk`](BlackHole+-+Physics+Simulator_1.0.0_APKPure.xapk), SHA-256 `90b4a14ebe0038eeeb0c40d609b435e4f70a694f71ffe33f0170618caf3a49a8`.
- Extracted the base and configuration APKs, raw `classes.dex`, and the ARM64 `libblackhole.so`. The base DEX lists **2,008 class descriptors**. The app-facing `MainActivity`, `NativeBridge`, surface/settings classes, and targeted Compose code were decompiled with `droidasc`, following the [apk-reverse](https://github.com/newliver666/apk-reverse) toolchain guidance. `Capstone`/`pyelftools` were used to inspect the AArch64 ELF and JNI/render call paths.
- Validated eight embedded SPIR-V instruction streams structurally: SPIR-V 1.3 headers, bounded instruction lengths, `main` entry points; two vertex and six fragment modules. The modules are stripped of useful `OpName` symbols. No `spirv-dis`/validator was available, so this is **not** a semantic disassembly or proof of the shader’s physics.
- The `ddc` release download failed with an EOF from the asset host. Therefore, this was a targeted decompilation of the app logic, not a source dump of all 2,008 DEX classes and bundled libraries. Native C++/Rust source cannot be reconstructed from the stripped `.so`; symbol tables and selected disassembly were inspected instead.
- Implementation changed the Lab surface host, JNI bridge, native parameter adapter, XAPK settings/presets, controls, GLES fallback, manifest, and tests. This sandbox has no Java/JDK and cannot download Gradle 9.3.1, but GitHub Actions run [36811438201](https://github.com/alimehali-cyber/Lm-arena/actions/runs/36811438201) successfully ran the JVM suite and assembled the signed release APK. No Android device run was available.

## Architecture comparison

| Layer | BlackHole XAPK 1.0.0 | Chal in this repository |
|---|---|---|
| Android/UI | Compose `MainActivity`; an Android `SurfaceView` forwards lifecycle and gestures to JNI. | Lab calls [`ChalRoot`](../app/src/main/java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt#L113); `ChalSurfaceHost` selects an XAPK-backed `SurfaceView` or the retained GLES `GLSurfaceView`. |
| Graphics | Manifest requires Vulkan API 1.1; the XAPK ARM64 split contains a C++ Vulkan renderer and eight SPIR-V modules. | On ARM64 devices reporting Vulkan 1.1 (runtime gate: Android 10+), Chal extracts the same `libblackhole.so` and matching `libc++_shared.so` from private assets; other ABIs remain installable and use the GLES 3 fallback. |
| Native/physics | `NativeBridge` loads `libblackhole.so`. The library contains the Vulkan renderer, Rust `gravitas` Kerr/tensor/disk routines, and JNI Kerr helpers. | `com.orchestrsim.blackhole.NativeBridge` matches the XAPK JNI exports. `XapkChalRenderer` forwards surface, pause, camera, telemetry, and physical state; Kotlin/GLSL remain only as a disclosed compatibility fallback. |
| State | Persists physical settings in `SharedPreferences("bh.params")`; sends 16 floats and four ints. Quality is 32/64/128/256 steps with scales .5/.75/1/1. Includes Stellar, Sgr A* proxy, Maximal Spin, and Schwarzschild scenarios. | `XapkSimulationSettingsStore` reuses the keys/defaults; `XapkRendererContract` maps mass/spin, seven feature bits, quality, bloom, camera yaw/pitch/distance, and scenario values to the native contract. |

The XAPK’s manifest identifies package `com.johnseong.blackhole`, version `1.0.0`, min SDK 26, target SDK 35, and a required Vulkan 1.1 feature. This differs from the repository app’s optional GLES 3 feature declaration and `GLSurfaceView` request. These are architecture/platform choices, not evidence that one renderer’s physics is correct.

## Implementation now in the branch

- The app packages the XAPK's ARM64 `libblackhole.so` and its matching `libc++_shared.so` under `app/src/main/assets/xapk-native/arm64-v8a/`. JNI class/package/method names match the native exports exactly.
- `ChalSurfaceHost` selects Vulkan only when the device is ARM64 and PackageManager reports Vulkan 1.1 (runtime version checking is available on Android 10+); only then does `NativeBridge` extract and load the private-asset binaries. Other ABIs remain installable and receive the GLES fallback notice. The manifest marks Vulkan 1.1 optional so unsupported devices can still install.
- The native adapter reproduces the XAPK 16-float block: `[mass, chi, lens, frameDrag, density, temperature, diskSize, scaleHeight, bloomThreshold, bloomIntensity, autoSpin, yaw, pitch, distance, renderScale, 0]`. The four integers are `[32/64/128/256 steps, featureMask, mediumFlag, highFlag]`; mask bits are lens=1, disk=2, Doppler=4, photon glow=8, stars=16, jets=32, redshift=64. Spin remains dimensionless and separate from mass.
- XAPK defaults are mass 1, spin .5, lens .7, frame drag 2, density 4, temperature 9500 K, disk size 50, scale height .2, bloom threshold 1, intensity .45, auto-spin .005, yaw .5, pitch .539, and distance 100. Device memory selects Low/Medium/High at the original cutoffs. Four physical scenario values, quality scales, camera gestures, pause/resume, resize/destroy, and telemetry were ported from the targeted DEX decompilations.
- The native Kerr readout helpers are called with normalized spin (unlike the XAPK's Compose call site); this preserves renderer input semantics while correcting the visible mass-scaling mistake. Unit tests were added for the parameter array, quality levels, camera clamp, and physical presets, but could not run without Java/Gradle.
- **Rights gate:** the XAPK exposes no verified license for the proprietary native renderer. Hashes and source provenance are recorded in [`xapk-native-renderer-provenance.md`](xapk-native-renderer-provenance.md). Do not publish or ship these binaries until redistribution rights and required notices are approved.

## Findings and current status

### 1. Fixed: Chal GLES spin handoff was multiplying mass twice

Before this change, `ChalRenderer` bound `params.spin * params.mass` while the shader already computed `a = u_spin * M`, yielding `a = chi * M²`. The GLES fallback now binds dimensionless `params.spin`; the shader performs the single `M * chi` conversion. On Vulkan-capable devices the original XAPK renderer receives mass and chi in separate slots.

| M, chi=.5 | Correct horizon | Old Chal GLES | XAPK Compose readout |
|---:|---:|---:|---:|
| 0.1 | 0.18660 | 0.19987 | 0.19987 |
| 1 | 1.86603 | 1.86603 | 1.86603 |
| 10 | 18.66025 | 10.00000 | 10.00000 |

The XAPK Compose bug is deliberately not copied: native Kerr helper readouts in Chal receive normalized spin, not `mass * spin`. `XapkRendererContractTest` checks that the renderer block retains separate mass and spin values.

### 2. Known GLES-only limitation: Chal's compatibility ray force is approximate

The GLES fallback's `kerr_geodesic_accel` is an effective radial/angular-momentum force plus a frame-drag term; it is not a full Kerr-Schild null-Hamiltonian integrator and no longer claims to be one. The normal path on supported devices bypasses it and calls the XAPK native renderer. The XAPK SPIR-V is stripped, so its underlying shader physics still cannot be independently verified from this static work.

### 3. Known GLES-only limitation: radial coordinate usage is inconsistent

The GLES shader still uses Euclidean `length(p)` for parts of the horizon, escape, and disk tests while computing a Kerr radial coordinate in other parts. That approximation is confined to the fallback; the native path executes the original XAPK renderer. A full correction would need a coherent coordinate convention throughout the GLES ray loop.

### 4. Negative spin is no longer exposed to the XAPK-compatible UI

The XAPK UI supports prograde spin 0 through 0.99, so Chal's shared physical control and persisted input are now clamped to that range. The GLES physics helpers still accept signed spin for formula tests, and direct non-UI construction with negative spin can still reach the old HUD/shader branch mismatch. Native XAPK parity is not affected because the settings store clamps to the XAPK range.

### 5. Fixed: descending-edge `smoothstep` calls in the GLES shader

Disk radial falloff, shadow and disk masks, horizon refinement, and shadow-guide edges now use ascending edges with explicit inversion. The shader no longer relies on undefined descending-edge behavior across GLES drivers.

### 6. Redshift sign fixed in the GLES HUD; the fallback observable remains Schwarzschild-only

The fallback now computes `z = 1/g - 1` from its Schwarzschild time-dilation factor rather than reporting `g - 1`. This is conventional gravitational redshift to infinity for the assumed static Schwarzschild observer, not a general Kerr redshift. The XAPK-matched HUD displays its FPS/scale/horizon/photon/ISCO readouts instead of this Chal-only redshift field.

### 7. Fixed in the GLES fallback: EGL cache invalidation and shader-variant errors

On each new EGL context, Chal discards stale program IDs without deleting names in the new context. Failed feature variants are recorded to avoid retrying every frame and expose an error while retaining the last working program. The original app marks GLES 3 optional, so the fallback still depends on ES 3 availability; the native Vulkan route is separately gated by API/ABI/Vulkan version.

### 8. Fixed: persisted XAPK controls and physical scenario presets

`XapkSimulationSettingsStore` uses the `bh.params` keys/defaults and device-memory quality selection. Chal now exposes the four XAPK physical scenarios and all mapped numeric controls, including frame dragging and Bloom. Native mode hides controls that have no equivalent in the XAPK (Chal benchmark/cinematics, Kerr-shadow guide, spacetime visualizer, pause toggle, and the GLES render-scale slider).

## Test coverage and remaining verification

The JVM suite now includes contract tests for the 16 floats/four ints, quality steps/flags/scales, camera clamps, and all four XAPK physical presets; existing physics tests were updated for the matching XAPK control ranges/default camera distance. GitHub Actions run [36811438201](https://github.com/alimehali-cyber/Lm-arena/actions/runs/36811438201), on commit `6f836c4`, passed **775 tests** (0 failures/errors/skips), assembled and verified the signed release APK, and uploaded the `ZIG-real-app-apk` artifact. The local sandbox still lacks Java/JDK and its Gradle 9.3.1 download failed with `SSL_ERROR_SYSCALL` to `services.gradle.org`.

Remaining work before release:

1. Run `:app:testDebugUnitTest` and assemble/install on an ARM64 Vulkan 1.1 device; verify JNI loading, surface lifecycle, gestures, output image, telemetry, and all parameter updates against the XAPK.
2. Test an unsupported/non-ARM64 or pre-Android-10 device and confirm the GLES fallback notice and rendering are clear and stable.
3. Obtain written redistribution rights for `libblackhole.so` and its bundled runtime notices before publishing an APK or shipping the repository payload.
4. If the fallback is expected to be scientifically equivalent, replace its remaining effective-force and coordinate approximations and validate conserved quantities; otherwise keep it clearly labeled as a non-identical compatibility renderer.

**Bottom line:** On devices passing the Vulkan 1.1/ARM64 gate, Chal now runs the XAPK's actual native renderer with its camera/state/preset contract and a corrected normalized-spin UI readout. Exact device-level behavior is still unverified because no Android runtime was available. On unsupported hardware, Chal remains an explicitly non-identical GLES fallback. The bundled native binaries must not be distributed until licensing is cleared.
