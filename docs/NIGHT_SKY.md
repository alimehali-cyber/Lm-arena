# The night sky

**Scope:** the Sky canvas at night. The Milky Way plate was always there; everything around it used to
be a flat vertical gradient with one airglow band, identical at noon and at midnight. It is now the
sky an observer would actually see from the ground, and the galaxy is the only part still drawn from
the plate.

![The night sky in five states](night-sky-states.png)

*Rendered from the shipped shader with the bundled galaxy plate: a pristine moonless night, a
moonlit night, a city night, deep twilight and nautical twilight. The reproduction harness is
`tools/sky-panorama/preview_night_sky.py`; it is a scratch tool, not part of the app.*

---

## 1. What the plate is, and what it is not

`milkyway_2020_4k.jpg` is NASA SVS *Deep Star Maps 2020* (see
`tools/sky-panorama/provenance`): a calibrated map of the **Galaxy**, not a photograph of a night
sky. It has no airglow, no light pollution, no zodiacal light, no moonlight and no twilight, and its
background is a flat grey field (≈ 0.105 in sRGB).

So the plate is used for exactly what it is good at — the dust lanes, the star clouds, the clusters
— and everything else is synthesised around it from the same physics the AR sky uses
(`ar/core/SkyRenderModel`, `ar/core/SkyAtmosphere`). The plate's grey pedestal is subtracted and the
residual is confined to the galactic band, so the flat background never shows as grey haze.

## 2. The model

`ui/skypanorama/SkyNightModel.kt` — pure Kotlin, no Android and no GL, so it is unit-tested on the JVM
(`SkyNightModelTest`). Everything is in absolute photometry (cd/m² or V mag/arcsec²) so the whole sky
can be summed and tone-mapped once.

| Term | Where it comes from |
|---|---|
| Natural airglow | zenith 21.95 mag/arcsec² (`SkyRenderModel.NATURAL_ZENITH_MAG_ARCSEC2`), brightening towards the horizon as `X·T^(X−1)` (Krisciunas & Schaefer 1991) |
| Light pollution | the observer's Bortle class' zenith brightness minus the natural sky, concentrated in domes near the horizon |
| Zodiacal light, Gegenschein | 23.0 and 23.8 V mag/arcsec² peaks, along the ecliptic and at the anti-solar point |
| Moonlight | Krisciunas & Schaefer lunar scattering, scaled by illuminated fraction, altitude and the extinction on the Moon's own path; a full Moon at the zenith gives an 18.2 mag/arcsec² sky |
| Twilight | the classic twilight ladder, ~1.9 mag/arcsec² per degree of solar depression (21.9 at 18°, 10.5 at 12°, ~2 at 6°) |
| Extinction | per-channel optical depth (blue lost ~3× faster than red), Kasten & Young airmass |
| Exposure | `SkyRenderModel` display key on the adaptation luminance, with the twilight included — the eye adapts to the sky it is under |
| Dark adaptation | `SkyRenderModel.scotopicWeight` on that same key: the Purkinje shift, at half weight because the brightest parts of a night sky stay cone-mediated |

The observer's Bortle class reaches the sky through `SkySceneFrame.bortle` (Home) and
`LiveSkyFrameRequest.bortle` (the Live Sky backdrop).

## 3. The shader

`ui/skypanorama/SkyPanoramaShaders.kt` builds the night once per pixel and tone-maps the sum:

1. **Galaxy** — the plate, sRGB-decoded, pedestal subtracted, masked to the band, tinted by
   population (warm bulge, cool outer arms), reddened by the same extinction as everything else and
   drawn only above the horizon. Its dust lanes and star clouds are untouched.
2. **Airglow** — the physical path term, with slow large-scale patchiness, in the cold blue-green of
   the natural sky: the 557.7 nm OI line plus the integrated starlight around it.
3. **Light pollution** — a broad floor plus three domes, sodium/LED tinted, hugging the horizon.
4. **Zodiacal light and Gegenschein** — extinguished by airmass like everything else.
5. **Moonlight** — the scattering function, cool, strongest near the Moon.
6. **Twilight** — shaped around the absolute zenith luminance for the instant: hugging the horizon,
   Henyey-Greenstein forward scattering towards the Sun (warm near it, cool away from it), darkened
   in the Earth's shadow opposite.
7. **Tone mapping** — one exposure, ACES, the Purkinje shift, then triangular dither against banding
   in the very dark gradients.

The uniforms are pinned by `SkyNightModelTest.theShaderImplementsEveryTermTheModelSupplies` and
`theRendererSendsEveryNightUniformItLocates`, so a term cannot be added to the model and silently
never reach the GPU.

## 4. The procedural canvas

The themes that do not use the panorama (everything except Real Sky) paint the night with
`ui/rendering/NightSkyRenderer.kt` from the same model:

- airglow that brightens towards the horizon,
- a radial light dome on the horizon,
- a moonlight wash centred on the Moon,
- and a Milky Way whose visibility is the same `galaxyVisibility` the model computes, so under
  Bortle 8 the galaxy is simply not there.

Each theme keeps its own night (`NightSkyThemeTones`): Real Sky is blue-black, Monochrome is neutral,
Observatory is deep red for the dark-adapted eye, Watercolour is indigo, Papercraft is slate.

## 5. What is deliberately not modelled

- **Scintillation** is per-star and lives in the star renderer (`StarRenderer`,
  `ArSkyOverlay`), not in the sky itself: it modulates the stars, not the background.
- **Weather and transparency.** ZIG has no measurement of them, so extinction is the clear-sky value
  (`SkyPanoramaSkyModel.EXTINCTION_V = 0.83`) everywhere rather than an invented number.
- **Airglow's real variability** (solar cycle, seasons, gravity waves). The mean-activity zenith value
  is used, with a slow patchiness for structure.
