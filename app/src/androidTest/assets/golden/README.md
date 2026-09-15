# Golden images (Foundational Rebuild Phase 1.4)

This directory is intentionally empty of any `*_golden.png` file as of this commit.

`SpaceMuseumRenderingInstrumentedTest.compareOrBootstrapGoldenImage()` looks here for
`earth_golden.png` (Phase 1's object, `InspectorEngine.PHASE1_OBJECT_ID`). No file has been
placed here because this development environment has no Android SDK/emulator available to
actually run the app and produce a real, human-reviewed Filament render to check in — see the
Phase 0+1 report for the explicit "cannot verify without a real device/CI run" statement this
corresponds to.

**Do not add a golden PNG here that was not captured from an actual passing CI run of this test
and reviewed by a human.** Fabricating a "reference" image would defeat the entire purpose of this
check and repeat the exact mistake (`docs/audit/MILESTONE_AUDIT.md`, and this project's own
prior false-PASS history) this rebuild exists to correct.

## How to promote the next CI run's output to a real golden image

1. Push this branch and let `.github/workflows/instrumented.yml` run
   `SpaceMuseumRenderingInstrumentedTest` on the CI emulator.
2. Download the `space-museum-screenshots` artifact from that run.
3. Open `earth_golden_candidate.png` (written by the bootstrap-mode branch of
   `compareOrBootstrapGoldenImage()`) and confirm by eye that it is a correct, non-regressed
   render of Earth (visible sphere, non-uniform directional shading, not a solid-color fallback).
4. Copy it into this directory as `earth_golden.png` and commit it.
5. On the next CI run, the test automatically switches from BOOTSTRAP mode (sanity checks only)
   to COMPARE mode (real per-pixel diff against this file, tolerance documented in the test).
