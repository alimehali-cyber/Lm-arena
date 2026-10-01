# XAPK native renderer provenance and distribution gate

Chal's ARM64 Vulkan backend currently reuses the renderer payload extracted from
`docs/BlackHole+-+Physics+Simulator_1.0.0_APKPure.xapk` (package `com.johnseong.blackhole`, version
1.0.0, XAPK SHA-256 `90b4a14ebe0038eeeb0c40d609b435e4f70a694f71ffe33f0170618caf3a49a8`).

| Packaged file | Source | SHA-256 | Size |
|---|---|---|---:|
| `app/src/main/assets/xapk-native/arm64-v8a/libblackhole.so` | XAPK ARM64 split | `2e5d6dd0d10d9c62e8263ec3f13cb6cb30f66f46e17392be1243e315959f4f11` | 1,271,888 bytes |
| `app/src/main/assets/xapk-native/arm64-v8a/libc++_shared.so` | Same XAPK ARM64 split; required `DT_NEEDED` dependency | `4397241b4bd20a8e579bfb41d21107857e12985f6a01ca0c2a5f83380d1270b4` | 1,292,904 bytes |

`libblackhole.so` directly depends on Android Vulkan/NDK libraries and `libc++_shared.so`. Its JNI exports name `com.orchestrsim.blackhole.NativeBridge`; Chal supplies that package/class contract and forwards a `Surface`, lifecycle, camera, telemetry, and parameter updates. The renderer is ARM64-only and the XAPK manifest requires Vulkan 1.1. Chal stores these files as private assets—not `jniLibs`—so packaging does not impose an ARM64 install filter. It checks for ARM64 and Vulkan 1.1 before extracting/loading them; other devices remain installable and keep the GLES renderer as a visibly disclosed fallback.

**Distribution is gated on license/provenance review.** The XAPK does not expose native renderer source or a verified redistribution license. This payload is included to validate the requested parity implementation in the repository, but do not publish or ship an app containing these binaries until the copyright owner has granted redistribution rights and the notices/terms have been reviewed. The bundled `libc++_shared.so` is the runtime shipped with that XAPK; retain its upstream LLVM/NDK license notice when distribution is approved.
