# Satellite review captures

No screenshots are checked in or claimed as inspected. The debug fixtures in `SatellitePreviews.kt` use the production HUD and panel composables with a deterministic clock, sample ephemeris and a **stub** Earth: they do not prove native GLES rendering, gesture behavior, performance, licence clearance or real orbital positions. Glass is deliberately flat in fixtures.

On a machine with the Android SDK, JDK 21 and the project's Gradle dependencies installed:

```sh
SAT_REVIEW_CAPTURE=1 ./gradlew :app:testDebugUnitTest \
  --tests '*SatelliteReviewPackTest*' -Pgravity.ci.tests=false --rerun-tasks
```

The optional capture test writes PNGs under `review/generated/<skin>/<theme>/<locale>/scale-<percent>/`. This directory is ignored by Git. The default matrix uses 360 × 800 dp, English/Persian, 100%/130% text, all three skins and themes and each fixture page. It is intentionally large; select a subset using comma-separated environment variables, e.g.:

```sh
SAT_REVIEW_CAPTURE=1 SAT_REVIEW_SKINS=default SAT_REVIEW_THEMES=dark \
 SAT_REVIEW_LANGUAGES=en,fa SAT_REVIEW_SCALES=100,130 \
 SAT_REVIEW_PAGES=rest,recenter,passes,iss \
 ./gradlew :app:testDebugUnitTest --tests '*SatelliteReviewPackTest*' -Pgravity.ci.tests=false --rerun-tasks
```

Set `SAT_REVIEW_SIZE_SWEEP=1` to add 360 × 640, 412 × 915 and 768 × 1024 dp Rest/Recenter captures at 100%, 115% and 130%. `SatelliteGeometryRenderTest` checks fixture bounds separately; `SatelliteMirrorRenderTest` compares physical/time-axis chart rendering. Run these tests explicitly before reviewing captures. The test suite does not automatically accept or compare golden screenshots.

Use `DEVICE_CHECKLIST.md` for the native renderer, real data, accessibility, persistence and device-specific checks. Keep the human review record and any legally cleared source material separate from generated captures; `LICENSE_AUDIT.md` is the authority for admitted satellite imagery.
