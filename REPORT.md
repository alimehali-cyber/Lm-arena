# Satellite redesign · implementation and evidence

## Scope

The shared satellite stage now has a measured globe/HUD, focus and time controls, a catalogue with persistent collections, pass and alert panels, map layers, spacecraft vector plates, Detail media scaffolding and English/Persian resources. Three skins share the same state and data integration. The native globe remains backed by the existing GLES renderer. Detail falls back to a labeled vector: **zero satellite photos or official renders have been approved or bundled**. See `LICENSE_AUDIT.md` and `fetch_media.md`.

## Evidence and limitations

| Evidence | Status |
|---|---|
| Standalone Android-free Kotlin core (geometry, projection/motion, search, rules, persistence codec, passes and numbers) | `tools/satellites/check-core.sh` passed with Kotlin 2.2.10 and JDK 21 on 18 November 2026. |
| Shape generator/manifest geometry | `python3 tools/satellites/build_shapes.py --check` passed for 11 plates. This is not a visual/reference verification; every `refVerified` flag is false. |
| Media audit | `python3 tools/satellites/audit.py` passed for **0 images**; four Python structural-audit tests passed. No individual source or licence was verified. |
| Resources | Previous XML/reference scan found no missing or duplicate strings, missing Persian satellite strings or placeholder mismatches. This is not a native-speaker review. |
| Android compilation and unit tests | [GitHub Actions run 44355372155](https://github.com/alimehali-cyber/Lm-arena/actions/runs/44355372155) passed on commit `8d6afa3`: 939 tests, 0 failures/errors, 2 skipped. Local Gradle bootstrap remained blocked before Gradle started. |
| Fixture geometry and chart mirroring | The same CI run passed the multi-size/scale/locale/skin fixture geometry test and an in-memory chart-path comparison. These do not exercise native GLES or constitute visual sign-off. |
| Release APK | The same run built the signed release, checked its package/version and verified it was not debug-key signed, then uploaded `ZIG-real-app-apk`. It was not installed or run on a device. |
| Screenshots or visual comparisons | **No review screenshots were saved or visually inspected.** The optional review-pack generator was not run; see `review/README.md`. |
| GLES alignment, gestures, performance and accessibility | **Not tested on an emulator or device.** Use `DEVICE_CHECKLIST.md`. |
| Licensed media, art recognition and Persian copy | **Not approved**; require individual source/rights review, visual reference review and native-speaker review. |

The status sheet currently reports the last location fix as unknown because no reliable fix timestamp is available in the shared location model. Pass calculation uses background work but longer windows and cancellation behavior require device profiling. No screenshot, device validation, copyright determination or performance sign-off should be inferred from structural checks.
