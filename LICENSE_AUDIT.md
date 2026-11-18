# Satellite Detail media audit · 18 November 2026

**Photographic/render assets admitted: 0.** No supplied licensed spacecraft-media folder was found in the checkout. No individual NASA, Commons, ESA or SpaceX asset page or licence was checked for this delivery. This session could reach npm, PyPI and GitHub; that does not establish the availability or licence status of any agency asset. No attribution or licence has been inferred.

| Asset | Author | Licence | Individual source page | Kind | Rights checked | Decision |
|---|---|---|---|---|---|---|
| None | — | — | — | — | No | No eligible asset individually reviewed; nothing bundled or linked. |

The manifest leaves `iss`, `tiangong`, `hubble` and the single shared `starlink` family image as `TODO` with empty image arrays. All Detail heroes therefore use an XL vector plate and label the absence of a photo. Existing debug globe textures are Earth imagery, not spacecraft photos, and are not included in the Detail media library.

The original diagrams in `tools/satellites/shapes/` are not official renders or photographs. Their `refVerified: false` flags reflect that independent recognition/reference verification has not been performed; the aspect/geometry checks do not confer that verification.

`python3 tools/satellites/audit.py` enforces structural admission and local-file limits. It is **not** a copyright determination and does not fetch, inspect or verify a source page. Follow `fetch_media.md` and document each actual approval here before changing any manifest entry to `VERIFIED`.
