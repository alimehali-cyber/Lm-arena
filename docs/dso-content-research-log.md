# DSO content research log

Date: 2026-09-11

Scope: facts added or replaced for the corrected deep-sky catalog merge. This log records the source families used before writing each user-facing fact set. The app stores only the polished English/Persian facts; this file records the validation trail.

## Global source policy

- Cross-identifications, object classes, and basic metadata were checked against OpenNGC/GAVO, SIMBAD/CDS, NASA/IPAC NED, SEDS Messier pages, NASA/Hubble Messier pages, ESA/Hubble image releases, NASA/APOD, and the Celestron Caldwell catalog PDF.
- Facts were written only when the same claim is supported by at least two independent reputable source families, e.g. database plus observatory article, SEDS plus NASA/Hubble, or NED/SIMBAD plus peer-review-backed catalog notes.
- Numeric wording is intentionally hedged when catalog values differ or when literature values are model-dependent.
- Facts avoid simply restating fields already shown elsewhere in the detail page: catalog designation, type, constellation, magnitude, angular size, distance, physical size, or best viewing month.

## Objects with new curated fact sets

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
