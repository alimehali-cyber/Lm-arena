# Info page consistency audit

Date: 2026-09-12

This audit was performed before closing the remaining DSO fact gap. It traces the modal data path used by `ObjectDetailModal.kt`: caller selection is canonicalized through `MainViewModel.openObjectDetail`, `ObjectDetailModal` resolves the canonical object again defensively, and displayed rows are built from `CanonicalAstroCatalog.toCelestialObject` plus shared modal sections.

## Audited sample

### Original / pre-merge references

| Role | Canonical ID | Type |
| --- | --- | --- |
| Sun | `sun` | SUN |
| Moon | `moon` | MOON |
| Satellite | `sat_25544` | SATELLITE |
| Planet | `planet_mars` | PLANET |
| Original hand-authored DSO | `dso_m31_andromeda` | GALAXY |

### Newer merged / engine-derived references

| Role | Canonical ID | Type |
| --- | --- | --- |
| Curated Caldwell/NGC merge sample | `dso_ngc_6946` | GALAXY |
| Curated Caldwell/NGC merge sample | `dso_ngc_6543` | NEBULA |
| Previously uncovered sample | `dso_m1` | NEBULA |
| Previously uncovered sample | `dso_m51` | GALAXY |
| Previously uncovered sample | `dso_c11` | NEBULA |

## Section order and visual treatment

The modal uses one shared `LazyColumn` body for all objects. The section order is:

1. Header
2. Live Observability Score
3. Locate Object in Live Sky (AR)
4. Derived Physical Properties
5. Deep-Sky Catalog Data, only for legitimate deep-sky types
6. Precise Schedule Today
7. Facts card, only when facts are genuinely non-empty
8. Description
9. Astronomical Coordinates
10. Add to Observation Log

Cards use the same `Card`, `Surface`, `RoundedCornerShape`, typography, spacing, and icon treatment regardless of whether the object is hand-authored, merged Caldwell/NGC, or engine-derived. The only structural difference is legitimate by type: deep-sky objects receive the Deep-Sky Catalog Data card while Sun/Moon/planet/satellite objects do not.

## Data-depth findings

- Sun, Moon, Mars, and ISS use object-specific physical properties but still render the same four-row Derived Physical Properties card as DSOs: size/diameter, approximate mass, surface gravity/context, and live distance.
- Hand-authored DSOs and engine-derived DSOs use the same DSO-specific data-card rows where data exists: designations, type plus constellation, constellation code, magnitude, angular size, computed physical size, live/catalog distance, and best viewing month.
- The upcoming fact-fill objects were missing the facts card before Part 2 because their curated fact lists were empty. This was a real content gap, not a different visual code path.
- A defensive consistency issue was found in `ObjectDetailModal.kt`: several header/description/dialog/coordinate reads still referenced the incoming `obj` even though the modal had already built the canonicalized `celestialObj`. In normal ViewModel navigation this was usually masked because the ViewModel canonicalizes first, but direct modal invocation could display stale source fields. This was fixed so user-visible modal text now reads canonicalized `celestialObj` consistently.

## Bilingual tone/length findings

The audited descriptions are short, single-sentence, catalog-style summaries in both English and Persian. Observation tips are also short practical sentences. The engine-derived DSO descriptions and tips are not longer or visually distinct from the original hand-authored entries; after the fact coverage pass, their facts card uses the same count-aware header and numbered-row treatment as original objects.

## Test coverage added

`ObjectDetailModalConsistencyTest` now asserts:

- the ten audited reference/new objects have non-empty bilingual names, categories, descriptions, observation tips, and facts;
- comparable hand-authored and engine-derived DSOs of the same `ObjectType` produce the same modal section plan;
- facts headers use the same count-aware English and Persian formatting;
- audited DSOs expose the same deep-sky data depth needed by the modal rows.
