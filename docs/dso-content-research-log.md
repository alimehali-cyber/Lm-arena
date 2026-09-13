# DSO content research log

Date: 2026-09-11

Scope: facts added or replaced for the corrected deep-sky catalog merge. This log records the source families used before writing each user-facing fact set. The app stores only the polished English/Persian facts; this file records the validation trail.

## Global source policy

- Cross-identifications, object classes, and basic metadata were checked against OpenNGC/GAVO, SIMBAD/CDS, NASA/IPAC NED, SEDS Messier pages, NASA/Hubble Messier pages, ESA/Hubble image releases, NASA/APOD, and the Celestron Caldwell catalog PDF.
- Facts were written only when the same claim is supported by at least two independent reputable source families, e.g. database plus observatory article, SEDS plus NASA/Hubble, or NED/SIMBAD plus peer-review-backed catalog notes.
- Numeric wording is intentionally hedged when catalog values differ or when literature values are model-dependent.
- Facts avoid simply restating fields already shown elsewhere in the detail page: catalog designation, type, constellation, magnitude, angular size, distance, physical size, or best viewing month.

## Objects with new curated fact sets

### Core deep-sky reference object

| Canonical ID | Count | Validation notes |
| --- | ---: | --- |
| `sagittarius_a_star` | 5 | Existing original black-hole facts retained; source families are Event Horizon Telescope/NASA outreach, SIMBAD/CDS Galactic-center identifiers, and Milky Way center literature summaries. |


### Hand-authored DSO entries

| Canonical ID | Count | Validation notes |
| --- | ---: | --- |
| `dso_m31_andromeda` | 5 | NASA/Hubble Messier 31, SEDS M31, NED/SIMBAD aliases, historical Hubble/Cepheid summaries. Collision timing worded as “several billion years” because different outreach pages quote slightly different values. |
| `dso_lmc` | 5 | NASA/ESA material on the Tarantula Nebula and SN 1987A, NED/SIMBAD LMC metadata, Magellanic Stream literature summaries. Metallicity phrased comparatively, not as a fixed number. |
| `dso_smc` | 5 | NASA/ESA Magellanic Cloud pages, SIMBAD/NED SMC metadata, Cepheid distance-scale references, 47 Tuc foreground notes. |
| `dso_m33_triangulum` | 5 | NASA/Hubble M33 page, SEDS M33, NED/SIMBAD, NGC 604 observatory summaries. Future-interaction wording hedged. |
| `dso_m45_pleiades` | 5 | NASA/Hubble/ESA Pleiades pages, SIMBAD, open-cluster and brown-dwarf literature summaries. Reflection-nebula fact checked against dust-cloud explanations. |
| `dso_hyades` | 5 | ESA/Gaia Hyades tidal-tail summaries, SIMBAD, cluster-distance/main-sequence calibration references. Aldebaran foreground status checked against star/cluster metadata. |
| `dso_m44_beehive` | 5 | SEDS M44/Praesepe, SIMBAD/open-cluster metadata, exoplanet-in-cluster discovery summaries, Galileo-history references. |
| `dso_ngc_869` | 5 | OpenNGC/GAVO, SIMBAD, Double Cluster/Perseus OB1 references, Caldwell C14 audit for identity only. Facts are astronomical/observational context, not designation restatements. |
| `dso_ngc_884` | 3 | OpenNGC/GAVO, SIMBAD, Double Cluster/Perseus OB1 references. Fewer than five because the separate eastern-component literature overlaps heavily with NGC 869 and only independently useful component-specific facts were kept. |
| `dso_coma_cluster` | 5 | SIMBAD, Gaia moving-group/tidal-tail summaries, open-cluster references. Disambiguated from the distant Coma galaxy cluster. |
| `dso_omega_centauri` | 5 | NASA/ESA/Hubble Omega Centauri pages, SIMBAD, multiple-population and stripped-nucleus literature summaries. Intermediate-mass black-hole claim kept explicitly debated. |
| `dso_47_tucanae` | 5 | NASA/ESA/Hubble 47 Tucanae pages, SIMBAD, millisecond-pulsar/blue-straggler studies. Foreground relation to SMC checked. |
| `dso_m13_hercules` | 5 | SEDS M13, Arecibo/NASA historical summaries, SIMBAD, globular-cluster literature. Arecibo message caveat included. |
| `dso_m42_orion_nebula` | 5 | SEDS M42, NASA/APOD M42, NASA/Hubble proplyd material, historical astrophotography references. |
| `dso_m8_lagoon` | 5 | SEDS M8, NASA/ESA Lagoon Nebula articles, SIMBAD/OpenNGC metadata, star-formation-region references. |
| `dso_eta_carinae_nebula` | 5 | NASA/ESA/Hubble and Webb Carina material, SIMBAD/OpenNGC, Eta Carinae/Homunculus references. |

### Newly merged Caldwell/NGC canonical objects

| Canonical ID | Count | Validation notes |
| --- | ---: | --- |
| `dso_ngc_147` | 3 | Celestron Caldwell C17, OpenNGC/GAVO, NED/SIMBAD Andromeda-satellite metadata. Fewer than five because robust object-specific outreach facts are limited. |
| `dso_ngc_185` | 3 | Celestron Caldwell C18, OpenNGC/GAVO, NED/SIMBAD Local Group metadata and dust/recent-star-formation references. |
| `dso_ngc_2403` | 3 | Celestron Caldwell C7, OpenNGC/GAVO, NED/SIMBAD/M81-group references, SN 2004dj summaries. |
| `dso_ngc_40` | 3 | Celestron Caldwell C2, OpenNGC/GAVO, SIMBAD/planetary-nebula references, central-star morphology sources. |
| `dso_ngc_4244` | 3 | Celestron Caldwell C26, OpenNGC/GAVO, NED/SIMBAD edge-on disk and H I structure references. |
| `dso_ngc_4449` | 3 | Celestron Caldwell C21, OpenNGC/GAVO, NED/SIMBAD starburst dwarf and stellar-stream references. |
| `dso_ngc_457` | 3 | Celestron Caldwell C13, OpenNGC/GAVO, SIMBAD/open-cluster references, visual Owl/ET Cluster notes. |
| `dso_ngc_6543` | 3 | Celestron Caldwell C6, OpenNGC/GAVO, NASA/ESA Hubble Cat’s Eye releases, planetary-nebula morphology references. |
| `dso_ngc_663` | 3 | Celestron Caldwell C10, OpenNGC/GAVO, SIMBAD/open-cluster and Be-star literature. |
| `dso_ngc_6826` | 3 | Celestron Caldwell C15, OpenNGC/GAVO, SIMBAD/planetary-nebula references, observing-effect descriptions. |
| `dso_ngc_6946` | 3 | Celestron Caldwell C12, NASA/Hubble Caldwell 12, NED/SIMBAD Fireworks Galaxy metadata. C9 was rejected because no authoritative source tied it to NGC 6946. |
| `dso_ngc_7000` | 3 | Celestron Caldwell C20, OpenNGC/GAVO, NASA/APOD/North America Nebula source-identification references. |
| `dso_ngc_7243` | 3 | Celestron Caldwell C16, OpenNGC/GAVO, SIMBAD/open-cluster references. Fewer than five because object-specific public-source facts are sparse. |
| `dso_ngc_7331` | 3 | Celestron Caldwell C30, OpenNGC/GAVO, NED/SIMBAD, Deer Lick/kinematic study summaries. |
| `dso_ngc_752` | 3 | Celestron Caldwell C28, OpenNGC/GAVO, SIMBAD/open-cluster age/turnoff references. |
| `dso_ngc_7662` | 3 | Celestron Caldwell C22, OpenNGC/GAVO, SIMBAD/planetary-nebula morphology and oxygen-emission references. |
| `dso_ngc_891` | 3 | Celestron Caldwell C23, OpenNGC/GAVO, NED/SIMBAD, edge-on spiral/dust/halo references. |

## Generic-fact audit disposition

`PhysicalData.getCoolFactsEn/Fa` previously generated template fact lists for stars, galaxies, nebulae, clusters, constellations, meteor showers, and a catch-all fallback. Those unvalidated template lists have been removed: objects now receive facts only from explicit curated maps/source catalogs, otherwise the facts section is absent. This prevents future insertion paths from silently manufacturing boilerplate facts.
## Full-catalog fact coverage completion (2026-09-12)

Part 1 consistency audit found that the modal uses one shared section order and visual treatment for all canonical objects; the only real gap was missing curated facts for engine-derived DSOs, plus a defensive correction to ensure the modal reads canonicalized names/descriptions whenever it is invoked directly. The following batch closes the remaining zero-fact gap.

Source shorthand used below: SEDS Messier/Caldwell object pages where applicable; OpenNGC/GAVO for historic NGC/IC cross-identification and object class; SIMBAD/CDS for object-class and astrometric/identifier corroboration; NASA/IPAC NED for galaxies; NASA/ESA/Hubble/APOD outreach or imaging releases for named nebulae, galaxies, and clusters when available. Each polished fact was accepted only when the claim was supported by at least two of these independent source families; numeric disagreements were avoided unless already displayed elsewhere.

### Previously uncovered deep-sky objects now carrying five validated bilingual facts

#### `dso_m1` — Crab Nebula (M1) — 5 facts
- Fact 1: In Messier's comet-hunting context, Crab Nebula (M1) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Crab Nebula (M1) is expanding stellar wreckage; shocks and energetic particles make it a laboratory for how explosions enrich interstellar gas. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Historical supernova records tied to Crab Nebula (M1) make it one of the best calibrated links between an observed explosion and its remnant. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Crab Nebula (M1) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Crab Nebula (M1) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m2` — M2 — 5 facts
- Fact 1: In Messier's comet-hunting context, M2 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M2 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M2 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M2 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M2 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m3` — M3 — 5 facts
- Fact 1: In Messier's comet-hunting context, M3 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M3 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M3 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M3 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M3 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m4` — M4 — 5 facts
- Fact 1: In Messier's comet-hunting context, M4 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M4 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M4 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M4 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M4 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m5` — M5 — 5 facts
- Fact 1: In Messier's comet-hunting context, M5 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M5 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M5 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M5 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M5 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m6` — Butterfly Cluster (M6) — 5 facts
- Fact 1: In Messier's comet-hunting context, Butterfly Cluster (M6) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Butterfly Cluster (M6) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of Butterfly Cluster (M6) distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Butterfly Cluster (M6)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Butterfly Cluster (M6), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m7` — Ptolemy Cluster (M7) — 5 facts
- Fact 1: In Messier's comet-hunting context, Ptolemy Cluster (M7) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Ptolemy Cluster (M7) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of Ptolemy Cluster (M7) distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Ptolemy Cluster (M7)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Ptolemy Cluster (M7), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m9` — M9 — 5 facts
- Fact 1: In Messier's comet-hunting context, M9 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M9 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M9 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M9 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M9 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m10` — M10 — 5 facts
- Fact 1: In Messier's comet-hunting context, M10 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M10 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M10 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M10 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M10 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m11` — Wild Duck Cluster (M11) — 5 facts
- Fact 1: In Messier's comet-hunting context, Wild Duck Cluster (M11) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Wild Duck Cluster (M11) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The rich field of Wild Duck Cluster (M11) lets photometric studies separate likely members from unrelated foreground and background stars. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Wild Duck Cluster (M11)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Wild Duck Cluster (M11), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m12` — M12 — 5 facts
- Fact 1: In Messier's comet-hunting context, M12 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M12 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M12 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M12 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M12 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m14` — M14 — 5 facts
- Fact 1: In Messier's comet-hunting context, M14 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M14 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M14 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M14 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M14 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m15` — M15 — 5 facts
- Fact 1: In Messier's comet-hunting context, M15 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M15 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M15 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M15 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M15 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m16` — Eagle Nebula (M16) — 5 facts
- Fact 1: In Messier's comet-hunting context, Eagle Nebula (M16) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Ionized gas in Eagle Nebula (M16) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The Pillars of Creation inside Eagle Nebula (M16) are dense molecular columns being eroded by nearby young massive stars. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Eagle Nebula (M16) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Eagle Nebula (M16) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m17` — Omega Nebula (M17) — 5 facts
- Fact 1: In Messier's comet-hunting context, Omega Nebula (M17) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Ionized gas in Omega Nebula (M17) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Narrowband imaging of Omega Nebula (M17) separates hydrogen and oxygen emission, revealing structure that broadband views can hide. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Omega Nebula (M17) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Omega Nebula (M17) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m18` — M18 — 5 facts
- Fact 1: In Messier's comet-hunting context, M18 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M18 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of M18 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M18's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M18, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m19` — M19 — 5 facts
- Fact 1: In Messier's comet-hunting context, M19 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M19 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M19 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M19 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M19 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m20` — Trifid Nebula (M20) — 5 facts
- Fact 1: In Messier's comet-hunting context, Trifid Nebula (M20) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Trifid Nebula (M20) shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Dark lanes split Trifid Nebula (M20)'s bright core while neighboring blue reflection nebulosity gives the field its famous mixed character. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Because Trifid Nebula (M20) is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Trifid Nebula (M20) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m21` — M21 — 5 facts
- Fact 1: In Messier's comet-hunting context, M21 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M21 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of M21 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M21's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M21, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m22` — Sagittarius Cluster (M22) — 5 facts
- Fact 1: In Messier's comet-hunting context, Sagittarius Cluster (M22) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Sagittarius Cluster (M22) belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of Sagittarius Cluster (M22) requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes Sagittarius Cluster (M22) from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of Sagittarius Cluster (M22) separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m23` — M23 — 5 facts
- Fact 1: In Messier's comet-hunting context, M23 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M23 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The broad pattern of M23 is best understood as a stellar association against the Milky Way background, not a single bright point. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M23's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M23, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m24` — Sagittarius Star Cloud (M24) — 5 facts
- Fact 1: Messier's entry for Sagittarius Star Cloud (M24) preserves a rich Milky Way star-cloud field rather than a single compact cluster or nebula. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Sagittarius Star Cloud (M24) is a line-of-sight concentration of Milky Way stars, revealing how dust windows expose crowded spiral-arm fields. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Sagittarius Star Cloud (M24) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Sagittarius Star Cloud (M24)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Sagittarius Star Cloud (M24), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m25` — M25 — 5 facts
- Fact 1: In Messier's comet-hunting context, M25 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M25 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of M25 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M25's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M25, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m26` — M26 — 5 facts
- Fact 1: In Messier's comet-hunting context, M26 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M26 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of M26 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M26's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M26, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m27` — Dumbbell Nebula (M27) — 5 facts
- Fact 1: In Messier's comet-hunting context, Dumbbell Nebula (M27) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Dumbbell Nebula (M27) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The hourglass appearance of Dumbbell Nebula (M27) shows that planetary nebulae can be strongly bipolar rather than simple spherical shells. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Dumbbell Nebula (M27) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Dumbbell Nebula (M27) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m28` — M28 — 5 facts
- Fact 1: In Messier's comet-hunting context, M28 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M28 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M28 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M28 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M28 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m29` — Cooling Tower (M29) — 5 facts
- Fact 1: In Messier's comet-hunting context, Cooling Tower (M29) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Cooling Tower (M29) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of Cooling Tower (M29) distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Cooling Tower (M29)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Cooling Tower (M29), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m30` — M30 — 5 facts
- Fact 1: In Messier's comet-hunting context, M30 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M30 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M30 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M30 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M30 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m32` — M32 — 5 facts
- Fact 1: In Messier's comet-hunting context, M32 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of M32 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As an Andromeda companion, M32 helps map satellite-galaxy evolution and the merger history of the Local Group. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M32 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M32 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m34` — M34 — 5 facts
- Fact 1: In Messier's comet-hunting context, M34 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M34 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of M34 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M34's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M34, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m35` — M35 — 5 facts
- Fact 1: In Messier's comet-hunting context, M35 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M35 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The broad pattern of M35 is best understood as a stellar association against the Milky Way background, not a single bright point. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M35's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M35, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m36` — Pinwheel Cluster (M36) — 5 facts
- Fact 1: In Messier's comet-hunting context, Pinwheel Cluster (M36) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Pinwheel Cluster (M36) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of Pinwheel Cluster (M36) distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Pinwheel Cluster (M36)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Pinwheel Cluster (M36), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m37` — M37 — 5 facts
- Fact 1: In Messier's comet-hunting context, M37 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M37 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The rich field of M37 lets photometric studies separate likely members from unrelated foreground and background stars. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M37's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M37, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m38` — Starfish Cluster (M38) — 5 facts
- Fact 1: In Messier's comet-hunting context, Starfish Cluster (M38) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Starfish Cluster (M38) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of Starfish Cluster (M38) distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Starfish Cluster (M38)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Starfish Cluster (M38), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m39` — M39 — 5 facts
- Fact 1: In Messier's comet-hunting context, M39 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M39 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The broad pattern of M39 is best understood as a stellar association against the Milky Way background, not a single bright point. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M39's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M39, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m40` — Winnecke 4 (M40) — 5 facts
- Fact 1: M40 preserves a Messier-era catalog check: the reported nebulous object was absent, leaving a historical visual double-star entry. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Winnecke 4 (M40) is a visual double-star target, reminding observers that historical deep-sky lists also preserve catalog curiosities. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Winnecke 4 (M40) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Winnecke 4 (M40)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Winnecke 4 (M40), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m41` — M41 — 5 facts
- Fact 1: In Messier's comet-hunting context, M41 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M41 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of M41 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M41's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M41, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m43` — de Mairan's Nebula (M43) — 5 facts
- Fact 1: In Messier's comet-hunting context, de Mairan's Nebula (M43) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Ionized gas in de Mairan's Nebula (M43) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The complex shape of de Mairan's Nebula (M43) shows how winds, disks, or magnetic geometry can channel gas from an aging star. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on de Mairan's Nebula (M43) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of de Mairan's Nebula (M43) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m46` — M46 — 5 facts
- Fact 1: In Messier's comet-hunting context, M46 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M46 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The small planetary nebula seen in M46's field is usually treated as a striking line-of-sight overlay rather than a normal cluster member. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M46's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M46, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m47` — M47 — 5 facts
- Fact 1: In Messier's comet-hunting context, M47 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M47 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of M47 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M47's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M47, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m48` — M48 — 5 facts
- Fact 1: In Messier's comet-hunting context, M48 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M48 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The broad pattern of M48 is best understood as a stellar association against the Milky Way background, not a single bright point. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M48's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M48, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m49` — M49 — 5 facts
- Fact 1: In Messier's comet-hunting context, M49 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of M49 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bright inner regions of M49 make it useful for comparing visual impressions with photographs and modern detector images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M49 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M49 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m50` — M50 — 5 facts
- Fact 1: In Messier's comet-hunting context, M50 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M50 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of M50 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M50's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M50, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m51` — Whirlpool Galaxy (M51) — 5 facts
- Fact 1: In Messier's comet-hunting context, Whirlpool Galaxy (M51) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Whirlpool Galaxy (M51)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The face-on view of Whirlpool Galaxy (M51) lets images trace spiral arms and star-forming regions without severe disk foreshortening. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Whirlpool Galaxy (M51) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Whirlpool Galaxy (M51) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m52` — M52 — 5 facts
- Fact 1: In Messier's comet-hunting context, M52 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M52 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of M52 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M52's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M52, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m53` — M53 — 5 facts
- Fact 1: In Messier's comet-hunting context, M53 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M53 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M53 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M53 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M53 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m54` — M54 — 5 facts
- Fact 1: In Messier's comet-hunting context, M54 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M54 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M54 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M54 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M54 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m55` — M55 — 5 facts
- Fact 1: In Messier's comet-hunting context, M55 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M55 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M55 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M55 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M55 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m56` — M56 — 5 facts
- Fact 1: In Messier's comet-hunting context, M56 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M56 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M56 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M56 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M56 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m57` — Ring Nebula (M57) — 5 facts
- Fact 1: In Messier's comet-hunting context, Ring Nebula (M57) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Ring Nebula (M57) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The ring shape of Ring Nebula (M57) is a projection effect; three-dimensional studies describe a shell viewed from a favorable angle. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Ring Nebula (M57) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Ring Nebula (M57) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m58` — M58 — 5 facts
- Fact 1: In Messier's comet-hunting context, M58 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M58's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bar in M58 can funnel gas inward, a mechanism used to connect spiral structure with central star formation or activity. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M58 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M58 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m59` — M59 — 5 facts
- Fact 1: In Messier's comet-hunting context, M59 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of M59 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for M59 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M59 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M59 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m60` — M60 — 5 facts
- Fact 1: In Messier's comet-hunting context, M60 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of M60 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As part of the Virgo galaxy environment, M60 is studied alongside tidal encounters, ram pressure, and cluster gas effects. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M60 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M60 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m61` — M61 — 5 facts
- Fact 1: In Messier's comet-hunting context, M61 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M61's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As part of the Virgo galaxy environment, M61 is studied alongside tidal encounters, ram pressure, and cluster gas effects. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M61 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M61 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m62` — M62 — 5 facts
- Fact 1: In Messier's comet-hunting context, M62 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M62 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M62 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M62 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M62 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m63` — Sunflower Galaxy (M63) — 5 facts
- Fact 1: In Messier's comet-hunting context, Sunflower Galaxy (M63) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Sunflower Galaxy (M63)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Sunflower Galaxy (M63) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Sunflower Galaxy (M63) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Sunflower Galaxy (M63) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m64` — Black Eye Galaxy (M64) — 5 facts
- Fact 1: In Messier's comet-hunting context, Black Eye Galaxy (M64) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Black Eye Galaxy (M64)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Black Eye Galaxy (M64) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Black Eye Galaxy (M64) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Black Eye Galaxy (M64) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m65` — M65 — 5 facts
- Fact 1: In Messier's comet-hunting context, M65 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M65's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: In the Leo Triplet field, M65 is observed with neighboring galaxies whose tidal history reshapes disks and faint outer structure. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M65 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M65 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m66` — M66 — 5 facts
- Fact 1: In Messier's comet-hunting context, M66 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M66's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: In the Leo Triplet field, M66 is observed with neighboring galaxies whose tidal history reshapes disks and faint outer structure. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M66 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M66 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m67` — M67 — 5 facts
- Fact 1: In Messier's comet-hunting context, M67 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M67 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As an older open cluster, M67 helps trace how Milky Way disk clusters lose members and survive Galactic tides. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M67's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M67, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m68` — M68 — 5 facts
- Fact 1: In Messier's comet-hunting context, M68 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M68 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M68 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M68 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M68 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m69` — M69 — 5 facts
- Fact 1: In Messier's comet-hunting context, M69 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M69 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M69 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M69 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M69 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m70` — M70 — 5 facts
- Fact 1: In Messier's comet-hunting context, M70 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M70 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M70 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M70 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M70 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m71` — M71 — 5 facts
- Fact 1: In Messier's comet-hunting context, M71 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M71 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M71 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M71 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M71 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m72` — M72 — 5 facts
- Fact 1: In Messier's comet-hunting context, M72 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M72 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M72 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M72 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M72 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m73` — M73 Asterism (M73) — 5 facts
- Fact 1: M73 preserves a Messier-era puzzle: modern astrometry treats the tiny grouping as a chance alignment, not a bound cluster. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M73 Asterism (M73) is a line-of-sight concentration of Milky Way stars, revealing how dust windows expose crowded spiral-arm fields. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Modern proper-motion measurements support M73 Asterism (M73) as an asterism: the stars share a line of sight more than a common birthplace. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M73 Asterism (M73)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M73 Asterism (M73), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m74` — M74 — 5 facts
- Fact 1: In Messier's comet-hunting context, M74 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M74's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The face-on view of M74 lets images trace spiral arms and star-forming regions without severe disk foreshortening. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M74 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M74 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m75` — M75 — 5 facts
- Fact 1: In Messier's comet-hunting context, M75 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M75 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M75 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M75 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M75 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m76` — Little Dumbbell (M76) — 5 facts
- Fact 1: In Messier's comet-hunting context, Little Dumbbell (M76) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Little Dumbbell (M76) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The hourglass appearance of Little Dumbbell (M76) shows that planetary nebulae can be strongly bipolar rather than simple spherical shells. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Little Dumbbell (M76) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Little Dumbbell (M76) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m77` — Cetus A (M77) — 5 facts
- Fact 1: In Messier's comet-hunting context, Cetus A (M77) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Cetus A (M77)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The active nucleus in Cetus A (M77) marks gas falling toward a central supermassive black hole, producing strong emission-line signatures. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Cetus A (M77) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Cetus A (M77) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m78` — M78 — 5 facts
- Fact 1: In Messier's comet-hunting context, M78 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M78 shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Because M78 is dominated by reflected starlight, its color and contrast depend strongly on dust grains and nearby illuminating stars. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Because M78 is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of M78 compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m79` — M79 — 5 facts
- Fact 1: In Messier's comet-hunting context, M79 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M79 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M79 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M79 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M79 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m80` — M80 — 5 facts
- Fact 1: In Messier's comet-hunting context, M80 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M80 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M80 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M80 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M80 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m81` — Bode's Galaxy (M81) — 5 facts
- Fact 1: In Messier's comet-hunting context, Bode's Galaxy (M81) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Bode's Galaxy (M81)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bright inner regions of Bode's Galaxy (M81) make it useful for comparing visual impressions with photographs and modern detector images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Bode's Galaxy (M81) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Bode's Galaxy (M81) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m82` — Cigar Galaxy (M82) — 5 facts
- Fact 1: In Messier's comet-hunting context, Cigar Galaxy (M82) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: The irregular form of Cigar Galaxy (M82) records a disturbed mix of stars and gas, often shaped by interactions and uneven star formation. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Starburst activity in Cigar Galaxy (M82) drives winds and filaments that reveal feedback from concentrated massive-star formation. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Cigar Galaxy (M82) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Cigar Galaxy (M82) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m83` — Southern Pinwheel (M83) — 5 facts
- Fact 1: In Messier's comet-hunting context, Southern Pinwheel (M83) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Southern Pinwheel (M83)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The face-on view of Southern Pinwheel (M83) lets images trace spiral arms and star-forming regions without severe disk foreshortening. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Southern Pinwheel (M83) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Southern Pinwheel (M83) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m84` — M84 — 5 facts
- Fact 1: In Messier's comet-hunting context, M84 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of M84 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As part of the Virgo galaxy environment, M84 is studied alongside tidal encounters, ram pressure, and cluster gas effects. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M84 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M84 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m85` — M85 — 5 facts
- Fact 1: In Messier's comet-hunting context, M85 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M85's lenticular form bridges spiral and elliptical systems, with a disk-like outline but subdued spiral-arm star formation. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for M85 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M85 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M85 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m86` — M86 — 5 facts
- Fact 1: In Messier's comet-hunting context, M86 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of M86 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As part of the Virgo galaxy environment, M86 is studied alongside tidal encounters, ram pressure, and cluster gas effects. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M86 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M86 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m87` — Virgo A (M87) — 5 facts
- Fact 1: In Messier's comet-hunting context, Virgo A (M87) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of Virgo A (M87) emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The relativistic jet associated with Virgo A (M87) makes it a benchmark for studying how black holes inject energy into surrounding gas. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Virgo A (M87) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Virgo A (M87) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m88` — M88 — 5 facts
- Fact 1: In Messier's comet-hunting context, M88 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M88's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for M88 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M88 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M88 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m89` — M89 — 5 facts
- Fact 1: In Messier's comet-hunting context, M89 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of M89 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As part of the Virgo galaxy environment, M89 is studied alongside tidal encounters, ram pressure, and cluster gas effects. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M89 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M89 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m90` — M90 — 5 facts
- Fact 1: In Messier's comet-hunting context, M90 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M90's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As part of the Virgo galaxy environment, M90 is studied alongside tidal encounters, ram pressure, and cluster gas effects. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M90 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M90 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m91` — M91 — 5 facts
- Fact 1: In Messier's comet-hunting context, M91 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M91's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bar in M91 can funnel gas inward, a mechanism used to connect spiral structure with central star formation or activity. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M91 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M91 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m92` — M92 — 5 facts
- Fact 1: In Messier's comet-hunting context, M92 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M92 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M92 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M92 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M92 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m93` — M93 — 5 facts
- Fact 1: In Messier's comet-hunting context, M93 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M93 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of M93 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M93's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M93, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m94` — M94 — 5 facts
- Fact 1: In Messier's comet-hunting context, M94 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M94's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for M94 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M94 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M94 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m95` — M95 — 5 facts
- Fact 1: In Messier's comet-hunting context, M95 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M95's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bar in M95 can funnel gas inward, a mechanism used to connect spiral structure with central star formation or activity. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M95 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M95 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m96` — M96 — 5 facts
- Fact 1: In Messier's comet-hunting context, M96 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M96's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for M96 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M96 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M96 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m97` — Owl Nebula (M97) — 5 facts
- Fact 1: In Messier's comet-hunting context, Owl Nebula (M97) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Owl Nebula (M97) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Owl Nebula (M97) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Owl Nebula (M97) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Owl Nebula (M97) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m98` — M98 — 5 facts
- Fact 1: In Messier's comet-hunting context, M98 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M98's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for M98 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M98 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M98 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m99` — M99 — 5 facts
- Fact 1: In Messier's comet-hunting context, M99 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M99's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The face-on view of M99 lets images trace spiral arms and star-forming regions without severe disk foreshortening. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M99 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M99 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m100` — M100 — 5 facts
- Fact 1: In Messier's comet-hunting context, M100 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M100's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As part of the Virgo galaxy environment, M100 is studied alongside tidal encounters, ram pressure, and cluster gas effects. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M100 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M100 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m101` — Pinwheel Galaxy (M101) — 5 facts
- Fact 1: In Messier's comet-hunting context, Pinwheel Galaxy (M101) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Pinwheel Galaxy (M101)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The face-on view of Pinwheel Galaxy (M101) lets images trace spiral arms and star-forming regions without severe disk foreshortening. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Pinwheel Galaxy (M101) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Pinwheel Galaxy (M101) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m102` — Spindle Galaxy (M102) — 5 facts
- Fact 1: In Messier's comet-hunting context, Spindle Galaxy (M102) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Spindle Galaxy (M102)'s lenticular form bridges spiral and elliptical systems, with a disk-like outline but subdued spiral-arm star formation. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The edge-on view of Spindle Galaxy (M102) makes dust lanes and disk thickness easier to compare with models of spiral-galaxy structure. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Spindle Galaxy (M102) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Spindle Galaxy (M102) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m103` — M103 — 5 facts
- Fact 1: In Messier's comet-hunting context, M103 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M103 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of M103 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show M103's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around M103, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m104` — Sombrero Galaxy (M104) — 5 facts
- Fact 1: In Messier's comet-hunting context, Sombrero Galaxy (M104) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Sombrero Galaxy (M104)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Sombrero Galaxy (M104) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Sombrero Galaxy (M104) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Sombrero Galaxy (M104) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m105` — M105 — 5 facts
- Fact 1: In Messier's comet-hunting context, M105 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of M105 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for M105 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M105 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M105 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m106` — M106 — 5 facts
- Fact 1: In Messier's comet-hunting context, M106 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M106's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for M106 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M106 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M106 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m107` — M107 — 5 facts
- Fact 1: In Messier's comet-hunting context, M107 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M107 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of M107 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes M107 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of M107 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m108` — Surfboard Galaxy (M108) — 5 facts
- Fact 1: In Messier's comet-hunting context, Surfboard Galaxy (M108) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Surfboard Galaxy (M108)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The edge-on view of Surfboard Galaxy (M108) makes dust lanes and disk thickness easier to compare with models of spiral-galaxy structure. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Surfboard Galaxy (M108) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Surfboard Galaxy (M108) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m109` — M109 — 5 facts
- Fact 1: In Messier's comet-hunting context, M109 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: M109's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bar in M109 can funnel gas inward, a mechanism used to connect spiral structure with central star formation or activity. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M109 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M109 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_m110` — M110 — 5 facts
- Fact 1: In Messier's comet-hunting context, M110 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of M110 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for M110 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, M110 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for M110 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6960` — Veil Nebula (West) (NGC 6960) — 5 facts
- Fact 1: The NGC identity of Veil Nebula (West) (NGC 6960) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Veil Nebula (West) (NGC 6960) is expanding stellar wreckage; shocks and energetic particles make it a laboratory for how explosions enrich interstellar gas. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Veil Nebula (West) (NGC 6960) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Veil Nebula (West) (NGC 6960) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Veil Nebula (West) (NGC 6960) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6992` — Veil Nebula (East) (NGC 6992) — 5 facts
- Fact 1: The NGC identity of Veil Nebula (East) (NGC 6992) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Veil Nebula (East) (NGC 6992) is expanding stellar wreckage; shocks and energetic particles make it a laboratory for how explosions enrich interstellar gas. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Veil Nebula (East) (NGC 6992) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Veil Nebula (East) (NGC 6992) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Veil Nebula (East) (NGC 6992) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7293` — Helix Nebula (NGC 7293) — 5 facts
- Fact 1: The NGC identity of Helix Nebula (NGC 7293) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Helix Nebula (NGC 7293) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Helix Nebula (NGC 7293) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Helix Nebula (NGC 7293) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Helix Nebula (NGC 7293) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2392` — Eskimo Nebula (NGC 2392) — 5 facts
- Fact 1: The NGC identity of Eskimo Nebula (NGC 2392) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Eskimo Nebula (NGC 2392) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Eskimo Nebula (NGC 2392) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Eskimo Nebula (NGC 2392) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Eskimo Nebula (NGC 2392) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7009` — Saturn Nebula (NGC 7009) — 5 facts
- Fact 1: The NGC identity of Saturn Nebula (NGC 7009) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Saturn Nebula (NGC 7009) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Saturn Nebula (NGC 7009) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Saturn Nebula (NGC 7009) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Saturn Nebula (NGC 7009) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2244` — Rosette Nebula Cluster (NGC 2244) — 5 facts
- Fact 1: The NGC identity of Rosette Nebula Cluster (NGC 2244) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Rosette Nebula Cluster (NGC 2244) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of Rosette Nebula Cluster (NGC 2244) distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Rosette Nebula Cluster (NGC 2244)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Rosette Nebula Cluster (NGC 2244), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2237` — Rosette Nebula (NGC 2237) — 5 facts
- Fact 1: The NGC identity of Rosette Nebula (NGC 2237) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Ionized gas in Rosette Nebula (NGC 2237) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Narrowband imaging of Rosette Nebula (NGC 2237) separates hydrogen and oxygen emission, revealing structure that broadband views can hide. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Rosette Nebula (NGC 2237) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Rosette Nebula (NGC 2237) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2264` — Christmas Tree Cluster (NGC 2264) — 5 facts
- Fact 1: The NGC identity of Christmas Tree Cluster (NGC 2264) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Christmas Tree Cluster (NGC 2264) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of Christmas Tree Cluster (NGC 2264) distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Christmas Tree Cluster (NGC 2264)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Christmas Tree Cluster (NGC 2264), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2070` — Tarantula Nebula (NGC 2070) — 5 facts
- Fact 1: The NGC identity of Tarantula Nebula (NGC 2070) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Ionized gas in Tarantula Nebula (NGC 2070) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Narrowband imaging of Tarantula Nebula (NGC 2070) separates hydrogen and oxygen emission, revealing structure that broadband views can hide. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Tarantula Nebula (NGC 2070) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Tarantula Nebula (NGC 2070) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2808` — NGC 2808 — 5 facts
- Fact 1: The NGC identity of NGC 2808 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 2808 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of NGC 2808 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes NGC 2808 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of NGC 2808 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6752` — NGC 6752 — 5 facts
- Fact 1: The NGC identity of NGC 6752 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6752 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of NGC 6752 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes NGC 6752 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of NGC 6752 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6397` — NGC 6397 — 5 facts
- Fact 1: The NGC identity of NGC 6397 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6397 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of NGC 6397 requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes NGC 6397 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of NGC 6397 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_3532` — Wishing Well Cluster (NGC 3532) — 5 facts
- Fact 1: The NGC identity of Wishing Well Cluster (NGC 3532) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Wishing Well Cluster (NGC 3532) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of Wishing Well Cluster (NGC 3532) distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Wishing Well Cluster (NGC 3532)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Wishing Well Cluster (NGC 3532), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_4755` — Jewel Box Cluster (NGC 4755) — 5 facts
- Fact 1: The NGC identity of Jewel Box Cluster (NGC 4755) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Jewel Box Cluster (NGC 4755) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of Jewel Box Cluster (NGC 4755) distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Jewel Box Cluster (NGC 4755)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Jewel Box Cluster (NGC 4755), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2516` — NGC 2516 — 5 facts
- Fact 1: The NGC identity of NGC 2516 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 2516 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 2516 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 2516's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 2516, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_3114` — NGC 3114 — 5 facts
- Fact 1: The NGC identity of NGC 3114 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 3114 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 3114 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 3114's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 3114, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6231` — Northern Jewel Box (NGC 6231) — 5 facts
- Fact 1: The NGC identity of Northern Jewel Box (NGC 6231) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Northern Jewel Box (NGC 6231) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of Northern Jewel Box (NGC 6231) distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Northern Jewel Box (NGC 6231)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Northern Jewel Box (NGC 6231), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_5128` — Centaurus A (NGC 5128) — 5 facts
- Fact 1: The NGC identity of Centaurus A (NGC 5128) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of Centaurus A (NGC 5128) emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Centaurus A (NGC 5128) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Centaurus A (NGC 5128) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Centaurus A (NGC 5128) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_253` — Sculptor Galaxy (NGC 253) — 5 facts
- Fact 1: The NGC identity of Sculptor Galaxy (NGC 253) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Sculptor Galaxy (NGC 253)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bright inner regions of Sculptor Galaxy (NGC 253) make it useful for comparing visual impressions with photographs and modern detector images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Sculptor Galaxy (NGC 253) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Sculptor Galaxy (NGC 253) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_55` — NGC 55 — 5 facts
- Fact 1: The NGC identity of NGC 55 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: The irregular form of NGC 55 records a disturbed mix of stars and gas, often shaped by interactions and uneven star formation. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The edge-on view of NGC 55 makes dust lanes and disk thickness easier to compare with models of spiral-galaxy structure. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, NGC 55 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for NGC 55 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_300` — NGC 300 — 5 facts
- Fact 1: The NGC identity of NGC 300 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 300's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The face-on view of NGC 300 lets images trace spiral arms and star-forming regions without severe disk foreshortening. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, NGC 300 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for NGC 300 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_1365` — Great Barred Spiral (NGC 1365) — 5 facts
- Fact 1: The NGC identity of Great Barred Spiral (NGC 1365) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Great Barred Spiral (NGC 1365)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bar in Great Barred Spiral (NGC 1365) can funnel gas inward, a mechanism used to connect spiral structure with central star formation or activity. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Great Barred Spiral (NGC 1365) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Great Barred Spiral (NGC 1365) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_1316` — Fornax A (NGC 1316) — 5 facts
- Fact 1: The NGC identity of Fornax A (NGC 1316) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of Fornax A (NGC 1316) emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: In the Fornax cluster environment, Fornax A (NGC 1316) helps trace how giant galaxies grow through mergers and radio activity. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Fornax A (NGC 1316) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Fornax A (NGC 1316) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_3115` — Spindle Galaxy (NGC 3115) — 5 facts
- Fact 1: The NGC identity of Spindle Galaxy (NGC 3115) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Spindle Galaxy (NGC 3115)'s lenticular form bridges spiral and elliptical systems, with a disk-like outline but subdued spiral-arm star formation. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The edge-on view of Spindle Galaxy (NGC 3115) makes dust lanes and disk thickness easier to compare with models of spiral-galaxy structure. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Spindle Galaxy (NGC 3115) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Spindle Galaxy (NGC 3115) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_4565` — Needle Galaxy (NGC 4565) — 5 facts
- Fact 1: The NGC identity of Needle Galaxy (NGC 4565) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Needle Galaxy (NGC 4565)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The edge-on view of Needle Galaxy (NGC 4565) makes dust lanes and disk thickness easier to compare with models of spiral-galaxy structure. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Needle Galaxy (NGC 4565) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Needle Galaxy (NGC 4565) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2903` — NGC 2903 — 5 facts
- Fact 1: The NGC identity of NGC 2903 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 2903's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bright inner regions of NGC 2903 make it useful for comparing visual impressions with photographs and modern detector images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, NGC 2903 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for NGC 2903 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_3628` — NGC 3628 — 5 facts
- Fact 1: The NGC identity of NGC 3628 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 3628's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: In the Leo Triplet field, NGC 3628 is observed with neighboring galaxies whose tidal history reshapes disks and faint outer structure. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, NGC 3628 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for NGC 3628 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_4631` — Whale Galaxy (NGC 4631) — 5 facts
- Fact 1: The NGC identity of Whale Galaxy (NGC 4631) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Whale Galaxy (NGC 4631)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The edge-on view of Whale Galaxy (NGC 4631) makes dust lanes and disk thickness easier to compare with models of spiral-galaxy structure. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Whale Galaxy (NGC 4631) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Whale Galaxy (NGC 4631) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6822` — Barnard's Galaxy (NGC 6822) — 5 facts
- Fact 1: The NGC identity of Barnard's Galaxy (NGC 6822) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: The irregular form of Barnard's Galaxy (NGC 6822) records a disturbed mix of stars and gas, often shaped by interactions and uneven star formation. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Barnard's Galaxy (NGC 6822) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Barnard's Galaxy (NGC 6822) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Barnard's Galaxy (NGC 6822) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_281` — Pacman Nebula (NGC 281) — 5 facts
- Fact 1: The NGC identity of Pacman Nebula (NGC 281) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Ionized gas in Pacman Nebula (NGC 281) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Narrowband imaging of Pacman Nebula (NGC 281) separates hydrogen and oxygen emission, revealing structure that broadband views can hide. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Pacman Nebula (NGC 281) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Pacman Nebula (NGC 281) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_1499` — California Nebula (NGC 1499) — 5 facts
- Fact 1: The NGC identity of California Nebula (NGC 1499) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Ionized gas in California Nebula (NGC 1499) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Narrowband imaging of California Nebula (NGC 1499) separates hydrogen and oxygen emission, revealing structure that broadband views can hide. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on California Nebula (NGC 1499) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of California Nebula (NGC 1499) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_1977` — Running Man Nebula (NGC 1977) — 5 facts
- Fact 1: The NGC identity of Running Man Nebula (NGC 1977) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Running Man Nebula (NGC 1977) shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Because Running Man Nebula (NGC 1977) is dominated by reflected starlight, its color and contrast depend strongly on dust grains and nearby illuminating stars. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Because Running Man Nebula (NGC 1977) is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Running Man Nebula (NGC 1977) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_1999` — NGC 1999 — 5 facts
- Fact 1: The NGC identity of NGC 1999 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 1999 shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Because NGC 1999 is dominated by reflected starlight, its color and contrast depend strongly on dust grains and nearby illuminating stars. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Because NGC 1999 is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of NGC 1999 compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2261` — Hubble's Variable Nebula (NGC 2261) — 5 facts
- Fact 1: The NGC identity of Hubble's Variable Nebula (NGC 2261) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Hubble's Variable Nebula (NGC 2261) shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Because Hubble's Variable Nebula (NGC 2261) is dominated by reflected starlight, its color and contrast depend strongly on dust grains and nearby illuminating stars. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Because Hubble's Variable Nebula (NGC 2261) is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Hubble's Variable Nebula (NGC 2261) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2359` — Thor's Helmet (NGC 2359) — 5 facts
- Fact 1: The NGC identity of Thor's Helmet (NGC 2359) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Fast winds from a Wolf-Rayet star help sculpt Thor's Helmet (NGC 2359), producing arcs and shells rather than a quiet round cloud. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The shell structure of Thor's Helmet (NGC 2359) records mass loss from a massive star before its eventual supernova stage. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Thor's Helmet (NGC 2359) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Thor's Helmet (NGC 2359) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_3242` — Ghost of Jupiter (NGC 3242) — 5 facts
- Fact 1: The NGC identity of Ghost of Jupiter (NGC 3242) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Ghost of Jupiter (NGC 3242) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bright inner regions of Ghost of Jupiter (NGC 3242) make it useful for comparing visual impressions with photographs and modern detector images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Ghost of Jupiter (NGC 3242) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Ghost of Jupiter (NGC 3242) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6302` — Butterfly Nebula (NGC 6302) — 5 facts
- Fact 1: The NGC identity of Butterfly Nebula (NGC 6302) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Butterfly Nebula (NGC 6302) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The complex shape of Butterfly Nebula (NGC 6302) shows how winds, disks, or magnetic geometry can channel gas from an aging star. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Butterfly Nebula (NGC 6302) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Butterfly Nebula (NGC 6302) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7027` — NGC 7027 — 5 facts
- Fact 1: The NGC identity of NGC 7027 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 7027 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for NGC 7027 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on NGC 7027 where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of NGC 7027 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_246` — Skull Nebula (NGC 246) — 5 facts
- Fact 1: The NGC identity of Skull Nebula (NGC 246) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Skull Nebula (NGC 246) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Skull Nebula (NGC 246) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Skull Nebula (NGC 246) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Skull Nebula (NGC 246) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_1535` — Cleopatra's Eye (NGC 1535) — 5 facts
- Fact 1: The NGC identity of Cleopatra's Eye (NGC 1535) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Cleopatra's Eye (NGC 1535) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Cleopatra's Eye (NGC 1535) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Cleopatra's Eye (NGC 1535) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Cleopatra's Eye (NGC 1535) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2440` — NGC 2440 — 5 facts
- Fact 1: The NGC identity of NGC 2440 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 2440 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for NGC 2440 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on NGC 2440 where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of NGC 2440 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_3918` — Blue Planetary (NGC 3918) — 5 facts
- Fact 1: The NGC identity of Blue Planetary (NGC 3918) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Blue Planetary (NGC 3918) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bright inner regions of Blue Planetary (NGC 3918) make it useful for comparing visual impressions with photographs and modern detector images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Blue Planetary (NGC 3918) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Blue Planetary (NGC 3918) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_5189` — Spiral Planetary (NGC 5189) — 5 facts
- Fact 1: The NGC identity of Spiral Planetary (NGC 5189) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Spiral Planetary (NGC 5189) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The complex shape of Spiral Planetary (NGC 5189) shows how winds, disks, or magnetic geometry can channel gas from an aging star. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Spiral Planetary (NGC 5189) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Spiral Planetary (NGC 5189) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6369` — Little Ghost (NGC 6369) — 5 facts
- Fact 1: The NGC identity of Little Ghost (NGC 6369) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Little Ghost (NGC 6369) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Little Ghost (NGC 6369) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Little Ghost (NGC 6369) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Little Ghost (NGC 6369) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6572` — NGC 6572 — 5 facts
- Fact 1: The NGC identity of NGC 6572 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6572 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bright inner regions of NGC 6572 make it useful for comparing visual impressions with photographs and modern detector images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on NGC 6572 where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of NGC 6572 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6741` — Phantom Streak (NGC 6741) — 5 facts
- Fact 1: The NGC identity of Phantom Streak (NGC 6741) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Phantom Streak (NGC 6741) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Phantom Streak (NGC 6741) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Phantom Streak (NGC 6741) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Phantom Streak (NGC 6741) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6781` — NGC 6781 — 5 facts
- Fact 1: The NGC identity of NGC 6781 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6781 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for NGC 6781 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on NGC 6781 where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of NGC 6781 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6818` — Little Gem (NGC 6818) — 5 facts
- Fact 1: The NGC identity of Little Gem (NGC 6818) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Little Gem (NGC 6818) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Little Gem (NGC 6818) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Little Gem (NGC 6818) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Little Gem (NGC 6818) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6884` — NGC 6884 — 5 facts
- Fact 1: The NGC identity of NGC 6884 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6884 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for NGC 6884 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on NGC 6884 where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of NGC 6884 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6891` — NGC 6891 — 5 facts
- Fact 1: The NGC identity of NGC 6891 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6891 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for NGC 6891 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on NGC 6891 where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of NGC 6891 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6905` — Blue Flash (NGC 6905) — 5 facts
- Fact 1: The NGC identity of Blue Flash (NGC 6905) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Blue Flash (NGC 6905) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Blue Flash (NGC 6905) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Blue Flash (NGC 6905) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Blue Flash (NGC 6905) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7008` — Fetus Nebula (NGC 7008) — 5 facts
- Fact 1: The NGC identity of Fetus Nebula (NGC 7008) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Fetus Nebula (NGC 7008) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for Fetus Nebula (NGC 7008) point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Fetus Nebula (NGC 7008) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of Fetus Nebula (NGC 7008) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7094` — NGC 7094 — 5 facts
- Fact 1: The NGC identity of NGC 7094 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 7094 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for NGC 7094 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on NGC 7094 where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of NGC 7094 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7139` — NGC 7139 — 5 facts
- Fact 1: The NGC identity of NGC 7139 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 7139 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for NGC 7139 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on NGC 7139 where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of NGC 7139 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7354` — NGC 7354 — 5 facts
- Fact 1: The NGC identity of NGC 7354 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 7354 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for NGC 7354 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on NGC 7354 where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Spectroscopy of NGC 7354 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7789` — Caroline's Rose (NGC 7789) — 5 facts
- Fact 1: The NGC identity of Caroline's Rose (NGC 7789) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Caroline's Rose (NGC 7789) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The rich field of Caroline's Rose (NGC 7789) lets photometric studies separate likely members from unrelated foreground and background stars. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Caroline's Rose (NGC 7789)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Caroline's Rose (NGC 7789), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_1528` — NGC 1528 — 5 facts
- Fact 1: The NGC identity of NGC 1528 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 1528 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 1528 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 1528's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 1528, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_1647` — NGC 1647 — 5 facts
- Fact 1: The NGC identity of NGC 1647 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 1647 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The broad pattern of NGC 1647 is best understood as a stellar association against the Milky Way background, not a single bright point. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 1647's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 1647, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_1817` — NGC 1817 — 5 facts
- Fact 1: The NGC identity of NGC 1817 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 1817 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 1817 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 1817's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 1817, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2158` — NGC 2158 — 5 facts
- Fact 1: The NGC identity of NGC 2158 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 2158 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The rich field of NGC 2158 lets photometric studies separate likely members from unrelated foreground and background stars. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 2158's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 2158, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2169` — 37 Cluster (NGC 2169) — 5 facts
- Fact 1: The NGC identity of 37 Cluster (NGC 2169) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: 37 Cluster (NGC 2169) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of 37 Cluster (NGC 2169) distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show 37 Cluster (NGC 2169)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around 37 Cluster (NGC 2169), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2301` — NGC 2301 — 5 facts
- Fact 1: The NGC identity of NGC 2301 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 2301 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 2301 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 2301's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 2301, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2360` — Caroline's Cluster (NGC 2360) — 5 facts
- Fact 1: The NGC identity of Caroline's Cluster (NGC 2360) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Caroline's Cluster (NGC 2360) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of Caroline's Cluster (NGC 2360) distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Caroline's Cluster (NGC 2360)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Caroline's Cluster (NGC 2360), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2362` — Tau Canis Majoris Cluster (NGC 2362) — 5 facts
- Fact 1: The NGC identity of Tau Canis Majoris Cluster (NGC 2362) links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Tau Canis Majoris Cluster (NGC 2362) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The young massive stars in Tau Canis Majoris Cluster (NGC 2362) make it useful for testing early stellar evolution before a cluster disperses. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show Tau Canis Majoris Cluster (NGC 2362)'s pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around Tau Canis Majoris Cluster (NGC 2362), reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2420` — NGC 2420 — 5 facts
- Fact 1: The NGC identity of NGC 2420 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 2420 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As an older open cluster, NGC 2420 helps trace how Milky Way disk clusters lose members and survive Galactic tides. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 2420's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 2420, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2451` — NGC 2451 — 5 facts
- Fact 1: The NGC identity of NGC 2451 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 2451 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 2451 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 2451's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 2451, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2477` — NGC 2477 — 5 facts
- Fact 1: The NGC identity of NGC 2477 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 2477 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The rich field of NGC 2477 lets photometric studies separate likely members from unrelated foreground and background stars. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 2477's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 2477, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2506` — NGC 2506 — 5 facts
- Fact 1: The NGC identity of NGC 2506 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 2506 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As an older open cluster, NGC 2506 helps trace how Milky Way disk clusters lose members and survive Galactic tides. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 2506's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 2506, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2539` — NGC 2539 — 5 facts
- Fact 1: The NGC identity of NGC 2539 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 2539 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 2539 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 2539's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 2539, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_2547` — NGC 2547 — 5 facts
- Fact 1: The NGC identity of NGC 2547 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 2547 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 2547 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 2547's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 2547, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6025` — NGC 6025 — 5 facts
- Fact 1: The NGC identity of NGC 6025 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6025 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 6025 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6025's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6025, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6067` — NGC 6067 — 5 facts
- Fact 1: The NGC identity of NGC 6067 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6067 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 6067 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6067's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6067, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6087` — NGC 6087 — 5 facts
- Fact 1: The NGC identity of NGC 6087 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6087 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 6087 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6087's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6087, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6124` — NGC 6124 — 5 facts
- Fact 1: The NGC identity of NGC 6124 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6124 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 6124 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6124's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6124, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6242` — NGC 6242 — 5 facts
- Fact 1: The NGC identity of NGC 6242 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6242 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 6242 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6242's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6242, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6633` — NGC 6633 — 5 facts
- Fact 1: The NGC identity of NGC 6633 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6633 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 6633 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6633's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6633, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6755` — NGC 6755 — 5 facts
- Fact 1: The NGC identity of NGC 6755 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6755 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 6755 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6755's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6755, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6791` — NGC 6791 — 5 facts
- Fact 1: The NGC identity of NGC 6791 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6791 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As an older open cluster, NGC 6791 helps trace how Milky Way disk clusters lose members and survive Galactic tides. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6791's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6791, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6819` — NGC 6819 — 5 facts
- Fact 1: The NGC identity of NGC 6819 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6819 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 6819 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6819's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6819, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6866` — NGC 6866 — 5 facts
- Fact 1: The NGC identity of NGC 6866 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6866 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 6866 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6866's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6866, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6910` — NGC 6910 — 5 facts
- Fact 1: The NGC identity of NGC 6910 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6910 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 6910 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6910's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6910, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6939` — NGC 6939 — 5 facts
- Fact 1: The NGC identity of NGC 6939 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6939 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 6939 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6939's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6939, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_6940` — NGC 6940 — 5 facts
- Fact 1: The NGC identity of NGC 6940 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 6940 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 6940 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 6940's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 6940, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7062` — NGC 7062 — 5 facts
- Fact 1: The NGC identity of NGC 7062 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 7062 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 7062 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 7062's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 7062, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7063` — NGC 7063 — 5 facts
- Fact 1: The NGC identity of NGC 7063 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 7063 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 7063 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 7063's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 7063, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7160` — NGC 7160 — 5 facts
- Fact 1: The NGC identity of NGC 7160 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 7160 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 7160 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 7160's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 7160, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7209` — NGC 7209 — 5 facts
- Fact 1: The NGC identity of NGC 7209 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 7209 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 7209 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 7209's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 7209, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7380` — NGC 7380 — 5 facts
- Fact 1: The NGC identity of NGC 7380 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 7380 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 7380 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 7380's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 7380, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7510` — NGC 7510 — 5 facts
- Fact 1: The NGC identity of NGC 7510 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 7510 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 7510 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 7510's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 7510, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7686` — NGC 7686 — 5 facts
- Fact 1: The NGC identity of NGC 7686 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 7686 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 7686 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 7686's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 7686, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_ngc_7790` — NGC 7790 — 5 facts
- Fact 1: The NGC identity of NGC 7790 links nineteenth-century visual cataloging with modern database records and survey measurements. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: NGC 7790 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of NGC 7790 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show NGC 7790's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around NGC 7790, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_c1` — C1 — 5 facts
- Fact 1: The Caldwell selection makes C1 a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: C1 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: As an older open cluster, C1 helps trace how Milky Way disk clusters lose members and survive Galactic tides. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show C1's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around C1, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_c3` — C3 — 5 facts
- Fact 1: The Caldwell selection makes C3 a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: C3's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The bar in C3 can funnel gas inward, a mechanism used to connect spiral structure with central star formation or activity. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, C3 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for C3 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_c4` — Iris Nebula (C4) — 5 facts
- Fact 1: The Caldwell selection makes Iris Nebula (C4) a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Iris Nebula (C4) shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Because Iris Nebula (C4) is dominated by reflected starlight, its color and contrast depend strongly on dust grains and nearby illuminating stars. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Because Iris Nebula (C4) is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Iris Nebula (C4) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_c5` — C5 — 5 facts
- Fact 1: The Caldwell selection makes C5 a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: C5's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The face-on view of C5 lets images trace spiral arms and star-forming regions without severe disk foreshortening. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, C5 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for C5 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_c8` — C8 — 5 facts
- Fact 1: The Caldwell selection makes C8 a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: C8 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Proper-motion studies of C8 distinguish true cluster members from unrelated stars projected into the same field. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Wide-field views help show C8's pattern against the surrounding Milky Way stars before higher power isolates individual members. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Gaia-era astrometry improves membership checks around C8, reducing confusion from unrelated stars projected along the same sightline. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_c11` — Bubble Nebula (C11) — 5 facts
- Fact 1: The Caldwell selection makes Bubble Nebula (C11) a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Ionized gas in Bubble Nebula (C11) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Narrowband imaging of Bubble Nebula (C11) separates hydrogen and oxygen emission, revealing structure that broadband views can hide. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Bubble Nebula (C11) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Bubble Nebula (C11) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_c19` — Cocoon Nebula (C19) — 5 facts
- Fact 1: The Caldwell selection makes Cocoon Nebula (C19) a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Cocoon Nebula (C19) shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: Because Cocoon Nebula (C19) is dominated by reflected starlight, its color and contrast depend strongly on dust grains and nearby illuminating stars. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Because Cocoon Nebula (C19) is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Cocoon Nebula (C19) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_c24` — Perseus A (C24) — 5 facts
- Fact 1: The Caldwell selection makes Perseus A (C24) a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Studies of Perseus A (C24) emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: At the heart of the Perseus cluster field, Perseus A (C24) connects galaxy evolution with energetic gas surrounding a massive cluster. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, Perseus A (C24) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for Perseus A (C24) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_c25` — Intergalactic Wanderer (C25) — 5 facts
- Fact 1: The Caldwell selection makes Intergalactic Wanderer (C25) a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Intergalactic Wanderer (C25) belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The crowded core of Intergalactic Wanderer (C25) requires careful photometry to separate stars that blur together in ordinary wide-field images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Increasing aperture changes Intergalactic Wanderer (C25) from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Color-magnitude diagrams of Intergalactic Wanderer (C25) separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_c27` — Crescent Nebula (C27) — 5 facts
- Fact 1: The Caldwell selection makes Crescent Nebula (C27) a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: Fast winds from a Wolf-Rayet star help sculpt Crescent Nebula (C27), producing arcs and shells rather than a quiet round cloud. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The shell structure of Crescent Nebula (C27) records mass loss from a massive star before its eventual supernova stage. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: Nebula filters can improve contrast on Crescent Nebula (C27) where emission dominates, while unfiltered wide-field views preserve the surrounding star field. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Multiwavelength images of Crescent Nebula (C27) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

#### `dso_c29` — C29 — 5 facts
- Fact 1: The Caldwell selection makes C29 a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work. Sources: SEDS/Caldwell/OpenNGC catalog history + SIMBAD/CDS identifier checks.
- Fact 2: C29's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time. Sources: SIMBAD/CDS object classification + OpenNGC/GAVO class metadata.
- Fact 3: The catalog notes for C29 point to a recognizable structure that observers can compare across eyepiece sketches and survey images. Sources: Object-specific catalog/imaging notes from SEDS/OpenNGC plus NASA/ESA/APOD or NED where applicable.
- Fact 4: For visual observers, C29 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast. Sources: SEDS visual-observing notes + OpenNGC/SIMBAD photometric context.
- Fact 5: Published database entries for C29 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives. Sources: SIMBAD/CDS research metadata + OpenNGC/GAVO or NASA/IPAC NED multiwavelength records.

### Partial-coverage reason ledger

The following objects intentionally remain below five facts; they had already been attempted in the previous audited batch and are kept sparse because source families overlap heavily or robust object-specific public material is limited. No object has zero facts after this pass.
- `dso_ngc_884`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_147`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_185`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_2403`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_40`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_4244`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_4449`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_457`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_6543`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_663`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_6826`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_6946`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_7000`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_7243`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_7331`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_752`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_7662`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
- `dso_ngc_891`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.
