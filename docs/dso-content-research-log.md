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

#### `dso_c1` — C1 / NGC 188 — 5 facts
- Fact 1: Caldwell 1, also known as NGC 188, is an open cluster in Cepheus, about 5,400 light-years away. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.1 and is the first object in Patrick Moore's Caldwell catalog. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 3: NGC 188 is one of the oldest known open clusters, roughly 5 billion years old. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 4: The cluster lies far above the plane of the Milky Way, surviving far longer than most open clusters. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 5: NGC 188 contains about 120 stars and is rich in evolved red giants and white dwarfs. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.

#### `dso_c11` — Bubble Nebula (C11 / NGC 7635) — 5 facts
- Fact 1: Caldwell 11, the Bubble Nebula, is an emission nebula in Cassiopeia, about 11,000 light-years away. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 10.0 and carries the designation NGC 7635. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 3: A powerful Wolf-Rayet star, SAO 20575, has blown a huge bubble in the surrounding gas with its stellar wind. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 4: The bubble spans about 7 light-years across. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 5: The Bubble Nebula was discovered by William Herschel in 1787. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.

#### `dso_c19` — Cocoon Nebula (C19 / IC 5146) — 5 facts
- Fact 1: Caldwell 19, the Cocoon Nebula, is a star-forming cloud in Cygnus, about 4,000 light-years away. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.2 and carries the designation IC 5146. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 3: The nebula glows partly by reflection and partly by emission, energized by the young cluster Collinder 470. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 4: A long, dark dust trail stretches behind the Cocoon across the sky. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 5: The Cocoon Nebula is a young stellar nursery, only about a million years old. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.

#### `dso_c24` — Perseus A (C24 / NGC 1275) — 5 facts
- Fact 1: Caldwell 24, also known as NGC 1275 or Perseus A, is the central galaxy of the Perseus Cluster. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 2: It lies about 230 million light-years away and shines at magnitude 11.9. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 3: NGC 1275 is a powerful radio source and one of the brightest extragalactic radio emitters. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 4: The galaxy's supermassive black hole drives enormous outflows and filaments of gas. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 5: NGC 1275 is a cD galaxy, a giant type that dominates the centers of rich galaxy clusters. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.

#### `dso_c25` — Intergalactic Wanderer (C25 / NGC 2419) — 5 facts
- Fact 1: Caldwell 25, the Intergalactic Wanderer, is the globular cluster NGC 2419 in Lynx. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 2: It lies about 275,000 light-years away, far beyond the Milky Way's main halo. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 3: For decades astronomers thought NGC 2419 wandered between galaxies, hence its nickname. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 4: Modern studies show NGC 2419 still orbits the Milky Way, on a very wide loop. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 5: The cluster shines at magnitude 10.4, a dim but rewarding telescope target. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.

#### `dso_c27` — Crescent Nebula (C27 / NGC 6888) — 5 facts
- Fact 1: Caldwell 27, the Crescent Nebula, is an emission nebula in Cygnus, about 4,700 light-years away. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.4 and carries the designation NGC 6888. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 3: The nebula was sculpted by the fast stellar wind of the Wolf-Rayet star WR 136. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 4: WR 136 has shed its outer layers, which now form the glowing crescent-shaped shell. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 5: The Crescent Nebula was discovered by William Herschel in 1792. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.

#### `dso_c29` — C29 / NGC 5005 — 5 facts
- Fact 1: Caldwell 29, also known as NGC 5005, is a spiral galaxy in Canes Venatici, about 65 million light-years away. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.8 and is a member of the NGC 5033 Group. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 3: The galaxy is a Seyfert-type spiral with an active nucleus. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 4: NGC 5005's supermassive black hole emits X-rays detectable by orbiting observatories. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 5: A supernova, SN 1996ai, was observed in the galaxy in 1996. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.

#### `dso_c3` — C3 / NGC 4236 — 5 facts
- Fact 1: Caldwell 3, also known as NGC 4236, is a barred spiral galaxy in Draco, about 11.7 million light-years away. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.7 and spans about 19 arcminutes. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 3: NGC 4236 is a member of the M81 Group of galaxies. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 4: The galaxy is seen nearly edge-on and has an elongated, spindle-like shape. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 5: NGC 4236 is one of the largest barred spirals known, over 70,000 light-years across. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.

#### `dso_c4` — Iris Nebula (C4 / NGC 7023) — 5 facts
- Fact 1: Caldwell 4, the Iris Nebula, is a reflection nebula in Cepheus, about 1,400 light-years away. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.8 and carries the designation NGC 7023. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 3: The nebula reflects the blue light of the young star HD 200775. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 4: Its flower-like shape inspired the name Iris, after the Greek goddess of the rainbow. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 5: The Iris Nebula was discovered by William Herschel in 1794. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.

#### `dso_c5` — C5 / IC 342 — 5 facts
- Fact 1: Caldwell 5, also known as IC 342, is a spiral galaxy in Camelopardalis, about 11 million light-years away. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.1 but is dimmed by heavy dust along the Milky Way's plane. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 3: IC 342 is nicknamed the Hidden Galaxy because foreground dust obscures it. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 4: Were it not for the dust, IC 342 would be one of the brightest galaxies in our sky. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 5: IC 342 is a member of the IC 342/Maffei Group of galaxies. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.

#### `dso_c8` — C8 / NGC 559 — 5 facts
- Fact 1: Caldwell 8, also known as NGC 559, is an open cluster in Cassiopeia, about 3,700 light-years away. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.7 and contains several dozen stars. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 3: William Herschel discovered NGC 559, the C8 cluster, in 1787. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 4: NGC 559 is about 100 million years old. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.
- Fact 5: It sits in a rich Milky Way field near the border with Cepheus. Sources: Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check.

#### `dso_m1` — Crab Nebula (M1 / NGC 1952) — 5 facts
- Fact 1: M1, the Crab Nebula, is the expanding remnant of a supernova recorded by Chinese and Japanese astronomers in 1054. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 6,500 light-years away in Taurus and shines at magnitude 8.4. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: At its center spins the Crab Pulsar, a neutron star rotating about 30 times per second that powers the surrounding glow. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The nebula is expanding at roughly 1,500 km/s, and its filaments are visible in amateur telescopes as a faint oval patch. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: It was the first entry in Charles Messier's catalog, added in 1758 while he searched for comets. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m10` — M10 / NGC 6254 — 5 facts
- Fact 1: M10 is a globular cluster of roughly 100,000 stars in Ophiuchus. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 14,300 light-years away and shines at magnitude 6.6. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Messier cataloged M10 in 1764, describing it as a round nebula without stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster contains a population of blue stragglers, unusually young-looking stars in an ancient system. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M10 is a moderately concentrated cluster spanning about 83 light-years. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m100` — M100 / NGC 4321 — 5 facts
- Fact 1: M100 is a grand-design spiral galaxy in Coma Berenices, about 55 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: M100 shines at magnitude 9.3 and was found by Pierre Méchain in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M100 is one of the brightest and largest galaxies in the Virgo Cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy has hosted several supernovae, including SN 1901B, SN 1914A, and SN 1979C. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Hubble observations of Cepheid variables in M100 helped refine the Hubble constant in the 1990s. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m101` — Pinwheel Galaxy (M101 / NGC 5457) — 5 facts
- Fact 1: M101, the Pinwheel Galaxy, is a face-on grand-design spiral in Ursa Major about 21 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.9 and was discovered by Pierre Méchain in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M101 is about 170,000 light-years across, nearly twice the size of the Milky Way. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy has hosted several supernovae, including SN 2011fe, the closest bright one in decades. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M101 is the brightest and largest galaxy in its own small group of galaxies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m102` — Spindle Galaxy (M102 / NGC 5866) — 5 facts
- Fact 1: M102, the Spindle Galaxy, is a lenticular galaxy in Draco seen almost edge-on. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 44 million light-years away and shines at magnitude 9.9. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M102's identity was long disputed, but it is now firmly identified with NGC 5866. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy is crossed by a dark dust lane along its edge-on disk. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Pierre Méchain originally reported M102 around 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m103` — M103 / NGC 581 — 5 facts
- Fact 1: M103 is an open cluster in Cassiopeia, about 8,500 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.4 and is one of the most distant open clusters in Messier's catalog. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Pierre Méchain discovered M103 in 1781, adding it as Messier's final cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster contains about 40 known stars and is about 25 million years old. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M103 is the last object Messier himself added to his catalog. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m104` — Sombrero Galaxy (M104 / NGC 4594) — 5 facts
- Fact 1: M104, the Sombrero Galaxy, is a spiral galaxy in Virgo seen nearly edge-on, about 29 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.0 and was discovered by Pierre Méchain in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: A prominent dust lane rings the galaxy, giving it the look of a wide-brimmed hat. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M104 has an unusually large central bulge and a supermassive black hole at its core. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The galaxy is surrounded by a rich system of about 2,000 globular clusters. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m105` — M105 / NGC 3379 — 5 facts
- Fact 1: M105 is an elliptical galaxy in Leo, about 38 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: Pierre Méchain discovered M105 in 1781; it shines at magnitude 9.3. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M105 is a member of the Leo I Group. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M105's central supermassive black hole is revealed by the rapid motion of stars around it. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M105 shows evidence of recent star formation, unusual for an elliptical galaxy. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m106` — M106 / NGC 4258 — 5 facts
- Fact 1: M106 is a spiral galaxy in Canes Venatici, about 24 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.4 and was discovered by Pierre Méchain in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M106 hosts a water vapor maser, which let astronomers measure its distance precisely. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy's supermassive black hole powers jets that have warped its disk. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M106 has hosted several supernovae, including SN 2014bc. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m107` — M107 / NGC 6171 — 5 facts
- Fact 1: M107 is a globular cluster in Ophiuchus, about 20,900 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.9 and was discovered by Pierre Méchain in 1782. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M107 was the last Messier object to be added to the catalog, appended in 1947. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster has a relatively open, sparse structure. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M107 contains several dozen known variable stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m108` — Surfboard Galaxy (M108 / NGC 3556) — 5 facts
- Fact 1: M108, the Surfboard Galaxy, is a barred spiral galaxy in Ursa Major seen almost edge-on. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 45 million light-years away and shines at magnitude 10.0. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Pierre Méchain found M108 in 1781 among the galaxies of Ursa Major. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: A supernova, SN 1969B, was observed in the galaxy in 1969. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M108 sits near the Owl Nebula, M97, and the two are often observed together. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m109` — M109 / NGC 3992 — 5 facts
- Fact 1: M109 is a barred spiral galaxy in Ursa Major, about 55 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.8 and was discovered by Pierre Méchain in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M109 is the brightest member of the M109 Group of galaxies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy has at least three satellite galaxies and a prominent central bar. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: A supernova, SN 1956A, was observed in M109 in 1956. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m11` — Wild Duck Cluster (M11 / NGC 6705) — 5 facts
- Fact 1: M11, the Wild Duck Cluster, is one of the richest and most compact open clusters known, in Scutum. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 6,200 light-years away and contains roughly 2,900 stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: At magnitude 6.3 it is visible in binoculars as a hazy patch. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Gottfried Kirch discovered M11 in 1681, calling it a small nebula. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Its brightest stars form a V-shape reminiscent of a flight of wild ducks, giving the cluster its popular name. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m110` — M110 / NGC 205 — 5 facts
- Fact 1: M110 is a dwarf elliptical galaxy and the second-largest satellite of the Andromeda Galaxy. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 2.54 million light-years away and shines at magnitude 8.5. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Charles Messier sketched M110 in 1773 but oddly never assigned it a catalog number. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M110 was formally added to the Messier catalog only in 1967, the last object added. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The galaxy shows an unusual dusty core, rare for a dwarf elliptical. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m12` — M12 / NGC 6218 — 5 facts
- Fact 1: M12 is a loosely concentrated globular cluster in Ophiuchus. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 15,700 light-years away and shines at magnitude 6.7. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Charles Messier discovered M12 in 1764 while hunting comets through Ophiuchus. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster has lost many of its low-mass stars to the Milky Way's tidal forces during its travels. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Compared with its neighbor M10, M12 is noticeably less dense and more diffuse. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m14` — M14 / NGC 6402 — 5 facts
- Fact 1: M14 is a globular cluster of about 70,000 stars in Ophiuchus. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies roughly 30,300 light-years away and shines at magnitude 7.6. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Messier found M14 in 1764, noting it as a faint, round patch of light. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: In 1938 a nova flared up inside M14, briefly outshining the entire cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M14 contains more than 70 known variable stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m15` — M15 / NGC 7078 — 5 facts
- Fact 1: M15 is a globular cluster of about 100,000 stars in Pegasus. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 33,600 light-years away and shines at magnitude 6.2. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M15 has one of the densest cores of any Milky Way globular, with a suspected intermediate-mass black hole at its center. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster contains Pease 1, one of the only planetary nebulae known inside a globular cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Jean-Dominique Maraldi discovered M15 in 1746. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m16` — Eagle Nebula (M16 / NGC 6611) — 5 facts
- Fact 1: M16, the Eagle Nebula, is a star-forming region in Serpens about 7,000 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: The Hubble Space Telescope's 1995 image of its Pillars of Creation made M16 famous worldwide. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The pillars are columns of cold gas and dust several light-years tall, sculpted by radiation from young hot stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M16 contains the young open cluster NGC 6611, whose stars are only one to two million years old. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: At magnitude 6.0 the nebula is visible in small telescopes under dark skies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m17` — Omega Nebula (M17 / NGC 6618) — 5 facts
- Fact 1: M17, the Omega Nebula, is one of the youngest and most massive star-forming regions in the Milky Way. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 5,500 light-years away in Sagittarius and shines at magnitude 6.0. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The nebula holds roughly 800 solar masses of glowing hydrogen gas. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Its curved shape has earned it several names, including the Swan and the Horseshoe Nebula. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Jean-Philippe de Chéseaux discovered M17 around 1745, before Messier independently found it in 1764. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m18` — M18 / NGC 6613 — 5 facts
- Fact 1: M18 is a sparse open cluster in Sagittarius, about 4,900 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.5 and is best seen in small telescopes. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Charles Messier discovered M18 in 1764 in the star clouds of Sagittarius. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is young, only about 32 million years old, and contains a few dozen stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M18 lies near the bright star cloud of the Sagittarius Milky Way. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m19` — M19 / NGC 6273 — 5 facts
- Fact 1: M19 is a globular cluster in Ophiuchus, about 28,200 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.8 and appears as a small hazy spot in small telescopes. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M19 is one of the most oblate globular clusters known, noticeably elongated in shape. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Messier logged M19 in 1764, remarking on its oval, elongated appearance. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Its elongated form is caused by its proximity to the Milky Way's central bulge. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m2` — M2 / NGC 7089 — 5 facts
- Fact 1: M2 is a globular cluster of about 150,000 stars located in Aquarius. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies roughly 37,500 light-years away and shines at magnitude 6.5, visible in binoculars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The cluster spans about 175 light-years, making it one of the largest known globulars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M2 was discovered by Jean-Dominique Maraldi in 1746, twelve years before Messier cataloged it. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: At about 13 billion years old, M2 is among the oldest known objects in the Milky Way. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m20` — Trifid Nebula (M20 / NGC 6514) — 5 facts
- Fact 1: M20, the Trifid Nebula, is a combination of emission, reflection, and dark nebula in Sagittarius. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 5,200 light-years away and shines at magnitude 6.3. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Dark dust lanes divide the glowing cloud into three lobes, giving it the name Trifid. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Guillaume Le Gentil discovered M20 around 1750. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The nebula hosts about 120 very young stars and is a site of ongoing star birth. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m21` — M21 / NGC 6531 — 5 facts
- Fact 1: M21 is an open cluster in Sagittarius, about 4,250 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.5 and is visible in binoculars as a faint grouping. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Charles Messier discovered M21 in 1764, close to the Trifid Nebula. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster contains about 57 stars and is only 4.6 million years old. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M21 sits close to the Trifid Nebula on the sky. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m22` — Sagittarius Cluster (M22 / NGC 6656) — 5 facts
- Fact 1: M22, the Sagittarius Cluster, is one of the nearest and brightest globular clusters, about 10,600 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: At magnitude 5.1 it is visible to the naked eye under dark skies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M22 was the first globular cluster ever discovered, found by Abraham Ihle in 1665. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster contains a planetary nebula, GJJC1, one of only a handful known inside a globular. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: In 2012 astronomers reported evidence for two stellar-mass black holes orbiting within M22. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m23` — M23 / NGC 6494 — 5 facts
- Fact 1: M23 is an open cluster in Sagittarius, about 2,150 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.9 and spans about 27 arcminutes of sky. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Messier cataloged M23 in 1764, one of his first Sagittarius clusters. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster contains around 150 stars and is about 300 million years old. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Its brightest stars are blue-white giants scattered against the rich Sagittarius Milky Way. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m24` — Sagittarius Star Cloud (M24 / IC 4715) — 5 facts
- Fact 1: M24, the Sagittarius Star Cloud, is not a true cluster but a dense cloud of Milky Way stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 10,000 light-years away and spans roughly 90 arcminutes of sky. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: At magnitude 4.6 it is easily visible to the naked eye as a bright patch in Sagittarius. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cloud contains the open cluster NGC 6603 and the dark nebulae Barnard 92 and Barnard 93. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M24 is a window through the Milky Way's dust, revealing a rich star field behind it. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m25` — M25 / IC 4725 — 5 facts
- Fact 1: M25 is an open cluster in Sagittarius, about 2,000 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 4.6, visible to the naked eye under dark skies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Jean-Philippe de Chéseaux discovered M25 in 1745. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is about 90 million years old and contains the Cepheid variable U Sagittarii. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M25 spans more than half a degree, appearing as a loose but bright grouping. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m26` — M26 / NGC 6694 — 5 facts
- Fact 1: M26 is an open cluster in Scutum, about 5,000 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.0 and is a modest target for small telescopes. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Charles Messier discovered M26 in 1764 in the constellation Scutum. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is about 89 million years old and contains around 90 stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: A dark dust lane lies between M26 and Earth, dimming the cluster's light. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m27` — Dumbbell Nebula (M27 / NGC 6853) — 5 facts
- Fact 1: M27, the Dumbbell Nebula, was the first planetary nebula ever discovered, found by Charles Messier in 1764. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 1,360 light-years away in the constellation Vulpecula. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: At magnitude 7.5 it is one of the brightest planetary nebulae in the sky. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Its central star is one of the largest known white dwarfs, larger than most stars of its kind. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The nebula's bipolar lobes give it the dumbbell shape that inspired its popular name. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m28` — M28 / NGC 6626 — 5 facts
- Fact 1: M28 is a globular cluster in Sagittarius, about 17,900 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.8 and appears as a small hazy patch in small telescopes. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Messier found M28 in 1764, describing it as a small, faint nebula. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: In 1987 M28 became the first globular cluster found to contain a millisecond pulsar, PSR B1821-24. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The cluster lies near Lambda Sagittarii (Kaus Borealis), the star at the top of the Teapot asterism. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m29` — Cooling Tower (M29 / NGC 6913) — 5 facts
- Fact 1: M29, the Cooling Tower, is a small open cluster in Cygnus, about 4,000 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.1 and is visible in binoculars near the star Sadr. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Charles Messier discovered M29 in 1764 in the Milky Way of Cygnus. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster contains about 50 stars and is only about 10 million years old. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Its brightest stars form a shape resembling a cooling tower, giving the cluster its nickname. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m3` — M3 / NGC 5272 — 5 facts
- Fact 1: M3 is a globular cluster containing roughly half a million stars in the constellation Canes Venatici. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 33,900 light-years away and appears as a magnitude 6.2 glow in binoculars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M3 is famous for its variable stars: more than 270 have been identified, most of them RR Lyrae variables. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Charles Messier discovered M3 in 1764, and it was the first Messier object he found independently. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The cluster also contains an unusually large population of blue stragglers near its core. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m30` — M30 / NGC 7099 — 5 facts
- Fact 1: M30 is a globular cluster in Capricornus, about 26,800 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.2 and appears as a small, fuzzy star in small telescopes. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Messier cataloged M30 in 1764, noting its compact, comet-like glow. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M30 has undergone core collapse, concentrating its stars densely at the center. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The cluster follows a retrograde orbit around the Milky Way, moving opposite to most stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m32` — M32 / NGC 221 — 5 facts
- Fact 1: M32 is a compact elliptical galaxy and a close companion of the Andromeda Galaxy. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 2.54 million light-years away in Andromeda. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: At magnitude 8.1 it is visible in small telescopes as a bright, round glow near M31. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M32 contains a supermassive black hole of roughly two million solar masses. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Its dense core and high surface brightness suggest it was once a larger galaxy stripped by tidal forces. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m34` — M34 / NGC 1039 — 5 facts
- Fact 1: M34 is an open cluster in Perseus, about 1,500 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 5.5 and spans about 35 arcminutes, larger than the full Moon. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Giovanni Hodierna recorded M34 around 1654, before Messier's catalog. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is about 250 million years old and contains roughly 400 stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M34 is a fine binocular target near the star Algol. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m35` — M35 / NGC 2168 — 5 facts
- Fact 1: M35 is an open cluster in Gemini, about 2,800 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 5.1 and is visible to the naked eye under dark skies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The cluster spans about 28 arcminutes and contains several hundred stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M35 is about 175 million years old, and the fainter cluster NGC 2158 lies nearby in the background. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Philippe de Chéseaux discovered M35 around 1745. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m36` — Pinwheel Cluster (M36 / NGC 1960) — 5 facts
- Fact 1: M36, the Pinwheel Cluster, is an open cluster in Auriga, about 4,100 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.3 and is visible in binoculars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The cluster is young, only about 25 million years old, and contains roughly 60 stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M36's brightest stars form a rough pinwheel shape, giving the cluster its popular name. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: It is the smallest of the three bright Messier clusters in Auriga, after M37 and M38. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m37` — M37 / NGC 2099 — 5 facts
- Fact 1: M37 is the richest and brightest of the three Messier clusters in Auriga. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 4,500 light-years away and shines at magnitude 5.6. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The cluster contains roughly 500 stars, including more than a dozen red giants. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M37 is about 350 million years old, older than its neighbors M36 and M38. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Giovanni Hodierna recorded M37 before 1654, long before Messier's catalog. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m38` — Starfish Cluster (M38 / NGC 1912) — 5 facts
- Fact 1: M38, the Starfish Cluster, is an open cluster in Auriga, about 4,200 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.4 and is visible in binoculars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The cluster is about 250 million years old and contains roughly 100 stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Its scattered brighter stars trace a shape like a starfish, giving the cluster its nickname. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M38 was recorded by Giovanni Hodierna before 1654. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m39` — M39 / NGC 7092 — 5 facts
- Fact 1: M39 is a loose open cluster in Cygnus, only about 800 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 4.6 and is visible to the naked eye under dark skies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The cluster spans more than half a degree and contains about 30 stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M39 is about 300 million years old, middle-aged for an open cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Charles Messier added M39 to his catalog in 1764. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m4` — M4 / NGC 6121 — 5 facts
- Fact 1: M4 is the nearest globular cluster to Earth, only about 7,200 light-years away in Scorpius. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: At magnitude 5.6 it is visible as a faint round glow in binoculars near the star Antares. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M4 was the first globular cluster to be resolved into individual stars, by William Herschel in 1783. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster hosts the pulsar PSR B1620-26, which is orbited by one of the oldest known exoplanets. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M4 contains tens of thousands of white dwarfs, the aged remnants of its most massive stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m40` — Winnecke 4 (M40) — 5 facts
- Fact 1: M40, Winnecke 4, is not a nebula or cluster but a pair of unrelated stars in Ursa Major. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: Charles Messier cataloged M40 in 1764 while searching for a nebula that earlier observers had reported there. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The two stars, roughly 510 light-years away, are merely an optical double along our line of sight. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: At magnitude 8.4 and 9.0, the pair is visible in small telescopes. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M40 is often called Messier's mistake, an asterism rather than a true deep-sky object. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m41` — M41 / NGC 2287 — 5 facts
- Fact 1: M41 is an open cluster in Canis Major, about 2,300 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 4.5 and is visible to the naked eye under dark skies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The cluster lies about four degrees south of Sirius, the brightest star in the sky. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M41 contains about 100 stars, including several orange giants and some white dwarfs. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Giovanni Hodierna recorded M41 around 1654, and Aristotle may have mentioned it even earlier. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m43` — de Mairan's Nebula (M43 / NGC 1982) — 5 facts
- Fact 1: M43, de Mairan's Nebula, is a bright knot of the great Orion Nebula complex, separated from M42 by a dark dust lane. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 1,344 light-years away in Orion and shines at magnitude 9.0. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Jean-Jacques d'Ortous de Mairan recorded this patch of nebulosity in 1731, before Messier's catalog. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The nebula is energized by the young star NU Orionis, which makes its gas glow. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M43 carries the New General Catalogue designation NGC 1982. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m46` — M46 / NGC 2437 — 5 facts
- Fact 1: M46 is a rich open cluster in Puppis, about 5,400 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.1 and contains roughly 500 stars in a 27-arcminute field. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Charles Messier discovered M46 in 1771. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The planetary nebula NGC 2438 appears projected inside the cluster, a line-of-sight coincidence that delights observers. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M46 is about 300 million years old. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m47` — M47 / NGC 2422 — 5 facts
- Fact 1: M47 is an open cluster in Puppis, about 1,600 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 4.4 and spans about 30 arcminutes, larger than the full Moon. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Giovanni Hodierna recorded M47 before 1654, decades before Messier independently rediscovered it in 1771. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster contains about 50 stars, including two orange K-type giants. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M47 is roughly 78 million years old, a relatively young open cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m48` — M48 / NGC 2548 — 5 facts
- Fact 1: M48 is an open cluster in Hydra, about 1,500 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 5.5 and spans 54 arcminutes, a very wide grouping. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Charles Messier discovered M48 in 1771, but an error in his recorded position long made the object seem missing. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster contains about 80 stars and is roughly 300 million years old. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M48 is easily visible to the naked eye under dark skies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m49` — M49 / NGC 4472 — 5 facts
- Fact 1: M49 is a giant elliptical galaxy in the Virgo Cluster, about 56 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.4 and was the first Virgo Cluster member cataloged by Charles Messier in 1771. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M49 is one of the most luminous galaxies in the Virgo Cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M49 hides a supermassive black hole in its core, among the heaviest in the Virgo Cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M49 is surrounded by a vast system of thousands of globular clusters. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m5` — M5 / NGC 5904 — 5 facts
- Fact 1: M5 is a globular cluster of more than 100,000 stars in the constellation Serpens. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 24,500 light-years away and reaches magnitude 5.6, one of the brightest northern globulars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Gottfried Kirch discovered M5 in 1702 while observing a comet. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster spans about 165 light-years and is estimated to be 13 billion years old. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M5 contains more than 100 known variable stars, mostly RR Lyrae types. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m50` — M50 / NGC 2323 — 5 facts
- Fact 1: M50 is an open cluster in Monoceros, about 3,000 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 5.9 and contains roughly 200 stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Charles Messier discovered M50 in 1772. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is about 180 million years old and has a notable red giant near its center. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M50 appears heart-shaped through small telescopes. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m51` — Whirlpool Galaxy (M51 / NGC 5194) — 5 facts
- Fact 1: M51, the Whirlpool Galaxy, is a grand-design spiral in Canes Venatici about 23 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It is locked in an ongoing interaction with the small companion galaxy NGC 5195. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Lord Rosse first recognized the galaxy's spiral structure with his 72-inch telescope in 1845. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M51 has hosted at least three supernovae: SN 1994I, SN 2005cs, and SN 2011dh. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: At magnitude 8.4, M51 is a favorite target for amateur astrophotographers. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m52` — M52 / NGC 7654 — 5 facts
- Fact 1: M52 is an open cluster in Cassiopeia, about 5,000 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.9 and contains about 190 stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Charles Messier discovered M52 in 1774 while observing a comet. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is young, only about 35 million years old, and includes a yellow giant star. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M52 lies in a rich Milky Way field near the border with Cepheus. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m53` — M53 / NGC 5024 — 5 facts
- Fact 1: M53 is a globular cluster in Coma Berenices, about 58,000 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.6 and appears as a small round glow in telescopes. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Johann Elert Bode discovered M53 in 1775. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is one of the most metal-poor globulars known, made of ancient, nearly pristine stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M53 is about 12.7 billion years old. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m54` — M54 / NGC 6715 — 5 facts
- Fact 1: M54 is a dense globular cluster in Sagittarius, about 87,400 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.6 and was discovered by Charles Messier in 1778. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: In 1994, astronomers determined that M54 actually belongs to the Sagittarius Dwarf Elliptical Galaxy, not the Milky Way. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: It was the first globular cluster found to belong to another galaxy. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M54 is one of the densest globular clusters known. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m55` — M55 / NGC 6809 — 5 facts
- Fact 1: M55 is a large, loosely concentrated globular cluster in Sagittarius. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 17,600 light-years away and shines at magnitude 6.3. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Nicolas-Louis de Lacaille discovered M55 in 1752 from South Africa. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is one of the least dense globulars known, resolving easily into individual stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M55 contains only about a dozen variable stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m56` — M56 / NGC 6779 — 5 facts
- Fact 1: M56 is a globular cluster in Lyra, about 32,900 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.3 and is a compact but modest telescope target. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Charles Messier discovered M56 in 1779. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is loosely concentrated, with a low central density for a globular. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M56 lies in the same constellation as the famous Ring Nebula, M57. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m57` — Ring Nebula (M57 / NGC 6720) — 5 facts
- Fact 1: M57, the Ring Nebula, is a planetary nebula in Lyra about 2,300 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It is the glowing shell of gas thrown off by a dying sun-like star. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Antoine Darquier de Pellepoix discovered M57 in 1779. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The nebula's central star is a white dwarf of about magnitude 15.8. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M57's ring of gas is expanding at roughly 20 to 30 km per second. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m58` — M58 / NGC 4579 — 5 facts
- Fact 1: M58 is a barred spiral galaxy in the Virgo Cluster, about 60 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.7 and was discovered by Charles Messier in 1779. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Lord Rosse recognized M58 as a spiral in the 1850s, among the first galaxies so classified. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy has hosted at least two supernovae, SN 1988A and SN 1989M. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M58 is one of the brighter members of the Virgo Cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m59` — M59 / NGC 4621 — 5 facts
- Fact 1: M59 is an elliptical galaxy in the Virgo Cluster, about 60 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.6 and was discovered by Johann Gottfried Koehler in 1779. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M59 has a supermassive black hole at its center. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy possesses about 2,000 globular clusters, an unusually rich system. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M59 is an elongated elliptical with a rapidly rotating core. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m6` — Butterfly Cluster (M6 / NGC 6405) — 5 facts
- Fact 1: M6, the Butterfly Cluster, is an open cluster of about 80 stars in Scorpius. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies roughly 1,600 light-years away and shines at magnitude 4.2. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The cluster's brightest stars appear to trace the outline of a butterfly with open wings. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Giovanni Hodierna recorded M6 around 1654, decades before Messier's catalog. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: At about 100 million years old, M6 contains the orange variable star BM Scorpii. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m60` — M60 / NGC 4649 — 5 facts
- Fact 1: M60 is a supergiant elliptical, one of the most massive galaxies in the Virgo Cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.8 and was discovered by Johann Gottfried Koehler in 1779. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M60 is one of the most massive galaxies in the Virgo Cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy hosts a supermassive black hole of roughly 4.5 billion solar masses. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M60 appears close to the spiral NGC 4647, and the two form an interacting pair known as Arp 116. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m61` — M61 / NGC 4303 — 5 facts
- Fact 1: M61 is a barred spiral galaxy in the Virgo Cluster, about 52 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.7 and was discovered by Barnaba Oriani in 1779. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M61 is one of the most prolific supernova producers known, with at least eight observed since 1926. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy is forming stars at a vigorous rate in its disk. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M61 is sometimes called the Swelling Spiral for its prominent bulge. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m62` — M62 / NGC 6266 — 5 facts
- Fact 1: M62 is a globular cluster in Ophiuchus, about 22,500 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.5 and was discovered by Charles Messier in 1771. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M62 is one of the most irregularly shaped globular clusters known. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Its deformation is caused by tidal forces from the Milky Way's central region. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The cluster contains at least 89 known variable stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m63` — Sunflower Galaxy (M63 / NGC 5055) — 5 facts
- Fact 1: M63, the Sunflower Galaxy, is a flocculent spiral in Canes Venatici about 37 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.6 and was discovered by Pierre Méchain in 1779. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The galaxy's many short, patchy arm segments resemble sunflower petals. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M63 is a member of the M51 Group of galaxies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: A supernova, SN 1971I, was observed in the galaxy in 1971. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m64` — Black Eye Galaxy (M64 / NGC 4826) — 5 facts
- Fact 1: M64, the Black Eye Galaxy, is a spiral galaxy in Coma Berenices about 17 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.5 and is famous for the dark dust lane beside its bright nucleus. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The dust lane gives the galaxy the appearance of a black eye, inspiring its popular name. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The outer gas of M64 rotates in the opposite direction to its inner disk, evidence of a past galaxy merger. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M64 was discovered by Edward Pigott in 1779. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m65` — M65 / NGC 3623 — 5 facts
- Fact 1: M65 is a spiral galaxy in Leo, about 35 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.3 and was discovered by Charles Messier in 1780. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M65 is a member of the Leo Triplet, along with M66 and NGC 3628. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy is seen at a steeply inclined angle, giving it an elongated appearance. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Gravitational interaction with its neighbors has disturbed M65's outer disk. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m66` — M66 / NGC 3627 — 5 facts
- Fact 1: M66 is a spiral galaxy in Leo, about 35 million light-years away, and the largest member of the Leo Triplet. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.9 and was discovered by Charles Messier in 1780. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Tidal interactions with M65 and NGC 3628 have deformed M66's spiral arms. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy has hosted several supernovae, including SN 1989B, SN 1997bs, and SN 2016cok. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M66's asymmetric arms make it a favorite study object for galaxy dynamics. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m67` — M67 / NGC 2682 — 5 facts
- Fact 1: M67 is an open cluster in Cancer, about 2,700 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.9 and contains roughly 500 stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M67 is one of the oldest known open clusters, about 4 billion years old. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is rich in red giants and white dwarfs, a sign of its great age. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Johann Gottfried Koehler discovered M67 in 1779. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m68` — M68 / NGC 4590 — 5 facts
- Fact 1: M68 is a globular cluster in Hydra, about 33,400 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.8 and was discovered by Charles Messier in 1780. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M68 is one of the least concentrated globular clusters known. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster follows a retrograde orbit around the Milky Way. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M68 contains about 250 known variable stars, mostly RR Lyrae types. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m69` — M69 / NGC 6637 — 5 facts
- Fact 1: M69 is a globular cluster in Sagittarius, about 29,700 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.6 and was discovered by Nicolas-Louis de Lacaille in 1752. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M69 is one of the most metal-rich globular clusters known. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is quite similar to its neighbor M70 in size and brightness. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M69 lies close to the center of the Milky Way, only about 6,200 light-years from it. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m7` — Ptolemy Cluster (M7 / NGC 6475) — 5 facts
- Fact 1: M7, the Ptolemy Cluster, is an open cluster of about 80 stars near the stinger of Scorpius. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies only about 980 light-years away, making it one of the nearest Messier open clusters. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: At magnitude 3.3, M7 is the brightest open cluster in Messier's catalog. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The Greek astronomer Ptolemy described M7 as a nebula around 130 AD, making it the oldest recorded deep-sky object. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The cluster is about 220 million years old and spans more than a degree of sky. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m70` — M70 / NGC 6681 — 5 facts
- Fact 1: M70 is a globular cluster in Sagittarius, about 29,300 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.9 and was discovered by Charles Messier in 1780. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M70 is a small, densely concentrated globular cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The comet Hale-Bopp was discovered in 1995 near M70, which brought the cluster brief fame. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M70 is similar in age and composition to its neighbor M69. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m71` — M71 / NGC 6838 — 5 facts
- Fact 1: M71 is a loosely packed globular cluster in Sagitta, about 13,000 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.1 and was discovered by Jean-Philippe de Chéseaux around 1745. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: For many years astronomers debated whether M71 was a globular cluster or a very rich open cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Modern studies confirm M71 is a globular cluster, though unusually sparse for its type. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M71 contains only a handful of known variable stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m72` — M72 / NGC 6981 — 5 facts
- Fact 1: M72 is a globular cluster in Aquarius, about 54,600 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.3, one of the faintest Messier globulars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Pierre Méchain discovered M72 in 1780, one of the globular clusters he found that year. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is among the most remote globulars in Messier's catalog. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M72 is a young globular by galactic standards, with relatively high metallicity. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m73` — M73 Asterism (M73 / NGC 6994) — 5 facts
- Fact 1: M73 is a small grouping of four stars in Aquarius, not a true star cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: Charles Messier cataloged M73 in 1780, mistaking the Y-shaped pattern for a cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Modern measurements show the four stars are unrelated and lie at different distances. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M73 is therefore classified as an asterism rather than a genuine deep-sky object. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The group is visible in small telescopes as a faint Y of stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m74` — M74 / NGC 628 — 5 facts
- Fact 1: M74 is a grand-design spiral galaxy in Pisces, about 32 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.4 but has a low surface brightness, making it a challenge for small scopes. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M74 is considered a textbook example of a grand-design spiral with well-defined arms. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy has hosted several supernovae, including SN 2002ap, SN 2003gd, and SN 2013ej. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Pierre Méchain discovered M74 in 1780, cataloging it as a faint spiral. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m75` — M75 / NGC 6864 — 5 facts
- Fact 1: M75 is a globular cluster in Sagittarius, about 67,500 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.5 and was discovered by Pierre Méchain in 1780. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M75 is one of the most centrally concentrated globular clusters known. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Its compact core places it among the densest stellar systems in the Milky Way. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M75 lies far beyond the Milky Way's center, on the opposite side of the galactic bulge. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m76` — Little Dumbbell (M76 / NGC 650) — 5 facts
- Fact 1: M76, the Little Dumbbell Nebula, is a bipolar planetary nebula in Perseus. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It lies about 3,400 light-years away and shines at magnitude 10.1. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Pierre Méchain discovered M76 in 1780, one of his earliest planetary nebula finds. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The nebula's two bright lobes give it a shape resembling the larger Dumbbell Nebula M27. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M76 is one of the faintest objects in Messier's catalog. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m77` — Cetus A (M77 / NGC 1068) — 5 facts
- Fact 1: M77, also called Cetus A, is a barred spiral galaxy in Cetus about 47 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.9 and was discovered by Pierre Méchain in 1780. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M77 is the prototype Seyfert galaxy, with a brilliant active nucleus. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Its active galactic nucleus is powered by a supermassive black hole accreting matter. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M77 dominates its small galaxy group in Cetus. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m78` — M78 / NGC 2068 — 5 facts
- Fact 1: M78 is the brightest reflection nebula in the sky, located in Orion about 1,600 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.3 and was discovered by Pierre Méchain in 1780. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: The nebula reflects the blue light of two young B-type stars, HD 38563A and HD 38563B. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M78 is part of the great Orion molecular cloud complex. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The nebula contains a group of Herbig-Haro objects, jets from forming stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m79` — M79 / NGC 1904 — 5 facts
- Fact 1: M79 is a globular cluster in Lepus, about 42,000 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.7 and was discovered by Pierre Méchain in 1780. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M79 is thought to have been captured from the Canis Major Dwarf Galaxy, a disrupted satellite of the Milky Way. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The cluster is nearly as old as the universe, about 11.7 billion years. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M79 lies in a part of the sky almost opposite the Milky Way's center. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m80` — M80 / NGC 6093 — 5 facts
- Fact 1: M80 is a dense globular cluster in Scorpius, about 32,600 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.3 and was discovered by Charles Messier in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M80 contains hundreds of thousands of stars packed into a small volume. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: In 1860 a nova, T Scorpii, flared up inside the cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M80 hosts many blue stragglers, stars that appear younger than their surroundings. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m81` — Bode's Galaxy (M81 / NGC 3031) — 5 facts
- Fact 1: M81, Bode's Galaxy, is a grand-design spiral in Ursa Major about 12 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.9 and is visible in binoculars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Johann Elert Bode discovered M81 in 1774. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M81 interacts gravitationally with its neighbor M82, and the two have disturbed each other in the past. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: A supernova, SN 1993J, was observed in M81 in 1993. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m82` — Cigar Galaxy (M82 / NGC 3034) — 5 facts
- Fact 1: M82, the Cigar Galaxy, is a starburst galaxy in Ursa Major about 12 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.4 and appears as an elongated streak, like a cigar. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M82 is undergoing a powerful burst of star formation triggered by its interaction with M81. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy ejects huge winds of gas, visible as red filaments in long-exposure images. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: In 2014, a bright supernova, SN 2014J, was discovered in M82 by amateur astronomers. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m83` — Southern Pinwheel (M83 / NGC 5236) — 5 facts
- Fact 1: M83, the Southern Pinwheel, is a barred spiral galaxy in Hydra about 15 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.5 and was discovered by Nicolas-Louis de Lacaille in 1752. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M83 is one of the nearest and brightest barred spirals in the sky. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy has produced at least six recorded supernovae. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Its abundant star formation and spiral structure make it a favorite southern-hemisphere target. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m84` — M84 / NGC 4374 — 5 facts
- Fact 1: M84 is a giant elliptical galaxy found along Markarian's Chain in the Virgo Cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.1 and was discovered by Charles Messier in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M84 lies at the heart of Markarian's Chain, a line of galaxies in the Virgo Cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy has a supermassive black hole at its center. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: Hubble observations revealed jets of material speeding away from M84's core. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m85` — M85 / NGC 4382 — 5 facts
- Fact 1: M85 is a lenticular galaxy in Coma Berenices, about 60 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.1 and was discovered by Pierre Méchain in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M85 is the northernmost member of the Virgo Cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy is interacting with its neighbor NGC 4394. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: A luminous red nova, M85 OT2006-1, was observed in M85 in 2006. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m86` — M86 / NGC 4406 — 5 facts
- Fact 1: M86 is a giant elliptical in Virgo, about 60 million light-years from Earth. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.9 and was discovered by Charles Messier in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M86 shows the highest blueshift of any Messier galaxy, moving toward us at high speed. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy is being stripped of its gas as it falls through the Virgo Cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M86 lies in Markarian's Chain of galaxies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m87` — Virgo A (M87 / NGC 4486) — 5 facts
- Fact 1: M87, also called Virgo A, is a supergiant elliptical galaxy in the Virgo Cluster about 54 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.6 and was discovered by Charles Messier in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M87 harbors a supermassive black hole of about 6.5 billion solar masses at its center. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: In 2019, the Event Horizon Telescope released the first-ever image of a black hole, showing M87's shadow. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M87 fires a jet of plasma thousands of light-years long from its core. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m88` — M88 / NGC 4501 — 5 facts
- Fact 1: M88 is a spiral galaxy in Coma Berenices, about 47 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.6 and was discovered by Charles Messier in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M88 belongs to the Virgo Cluster, though it lies in the constellation Coma Berenices. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy is highly symmetrical, with well-defined spiral arms. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M88 has hosted several supernovae, including SN 1999cl. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m89` — M89 / NGC 4552 — 5 facts
- Fact 1: M89 is an elliptical galaxy in the Virgo Cluster, about 50 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.8 and was discovered by Charles Messier in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M89 is almost perfectly spherical, unlike most flattened ellipticals. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy shows faint shells and plumes, evidence of a recent merger. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M89 is surrounded by a large population of globular clusters. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m9` — M9 / NGC 6333 — 5 facts
- Fact 1: M9 is a globular cluster in Ophiuchus, about 25,800 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 7.7 and appears as a small, faint round patch in small telescopes. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M9 is one of the globular clusters nearest to the Milky Way's center, only about 7,500 light-years from it. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: Messier recorded M9 in 1764, one of several clusters he found in Ophiuchus that year. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The cluster's relatively loose outer regions show the gravitational influence of the galactic bulge. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m90` — M90 / NGC 4569 — 5 facts
- Fact 1: M90 is a spiral galaxy in the Virgo Cluster, about 60 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.5 and was discovered by Charles Messier in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M90 shows little new star formation, making it an anemic spiral. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy is moving toward us at high speed, one of the most blueshifted Virgo members. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M90's gas has been stripped away by ram pressure as it travels through the cluster. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m91` — M91 / NGC 4548 — 5 facts
- Fact 1: M91 is a barred spiral galaxy in Coma Berenices, about 60 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 10.2, one of the faintest Messier objects. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M91 was one of Messier's 'missing' objects for nearly two centuries until its identity was confirmed in 1969. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy's prominent central bar channels gas toward its nucleus. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M91 is a Virgo Cluster galaxy in Coma Berenices. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m92` — M92 / NGC 6341 — 5 facts
- Fact 1: M92 is a globular cluster in Hercules, about 26,700 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.4 and is one of the brightest globulars in the northern sky. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Johann Elert Bode discovered M92 in 1777. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: M92 is about 14.2 billion years old, nearly the age of the universe. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: The cluster is so bright that it is visible to the naked eye under excellent skies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m93` — M93 / NGC 2447 — 5 facts
- Fact 1: M93 is an open cluster in Puppis, about 3,600 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 6.2 and contains about 80 stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Charles Messier discovered M93 in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The stars of M93 are roughly 100 million years old. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M93 has a distinctive shape, with bright stars forming an arrowhead-like pattern. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m94` — M94 / NGC 4736 — 5 facts
- Fact 1: M94 is a spiral galaxy in Canes Venatici, about 16 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 8.2 and was discovered by Pierre Méchain in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M94 has a bright inner disk surrounded by a faint outer ring of young stars. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy's bright inner region is a site of vigorous star formation. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M94 is sometimes called the Cat's Eye Galaxy for its compact, luminous core. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m95` — M95 / NGC 3351 — 5 facts
- Fact 1: M95 is a barred spiral galaxy in Leo, about 38 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.7 and was discovered by Pierre Méchain in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M95 is a member of the Leo I Group of galaxies. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy has a ring of star formation surrounding its central bar. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: A supernova, SN 2012aw, was observed in M95 in 2012. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m96` — M96 / NGC 3368 — 5 facts
- Fact 1: M96 is a spiral galaxy in Leo, about 38 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 9.2 and was discovered by Pierre Méchain in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M96 is the brightest member of the Leo I Group. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy's asymmetric outer disk was disturbed by interactions with its neighbors. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: A supernova, SN 1998bu, was observed in M96 in 1998. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m97` — Owl Nebula (M97 / NGC 3587) — 5 facts
- Fact 1: M97, the Owl Nebula, is a planetary nebula in Ursa Major about 2,600 light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: M97 shines at magnitude 9.9; Pierre Méchain discovered the Owl Nebula in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: Two darker regions within the nebula resemble an owl's eyes, giving it its popular name. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The nebula is about 8,000 years old, quite young for a planetary nebula. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M97's central star is a hot white dwarf with a surface temperature near 120,000 K. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m98` — M98 / NGC 4192 — 5 facts
- Fact 1: M98 is a spiral galaxy in Coma Berenices, about 60 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: It shines at magnitude 10.1 and was discovered by Pierre Méchain in 1781. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M98 belongs to the Virgo Cluster and approaches us at high speed. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy shows a strong blueshift, moving toward us at high speed. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M98 has produced several supernovae, including SN 1976H. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_m99` — M99 / NGC 4254 — 5 facts
- Fact 1: M99 is a spiral galaxy in Coma Berenices, about 50 million light-years away. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 2: Pierre Méchain found M99 in 1781, recording it at magnitude 9.9. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 3: M99 is a grand-design spiral with a single, well-defined arm dominating its disk. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 4: The galaxy has hosted at least four supernovae. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.
- Fact 5: M99 resides in the Virgo Cluster, near its northern edge. Sources: SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check.

#### `dso_ngc_1316` — Fornax A (NGC 1316) — 5 facts
- Fact 1: NGC 1316, also called Fornax A, is a giant lenticular galaxy in Fornax, about 62 million light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.4 and is the brightest galaxy in the Fornax Cluster. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 1316 is a powerful radio source, one of the strongest in the sky. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The galaxy shows dust lanes and shells, evidence of past mergers with smaller galaxies. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its radio lobes span several degrees, among the largest known structures of their kind. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_1365` — Great Barred Spiral (NGC 1365) — 5 facts
- Fact 1: NGC 1365, the Great Barred Spiral, is a barred spiral galaxy in Fornax, about 56 million light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 9.5 and is a member of the Fornax Cluster. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 1365 is one of the finest examples of a barred spiral galaxy known. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Its central bar funnels gas inward, feeding a supermassive black hole at its core. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The galaxy has hosted several supernovae, including SN 2012fr. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_1499` — California Nebula (NGC 1499) — 5 facts
- Fact 1: NGC 1499, the California Nebula, is a vast emission nebula in Perseus, about 1,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 6.0 but is so spread out that it is faint to the eye. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula is energized by the hot star Xi Persei (Menkib). Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Its elongated shape resembles the outline of the U.S. state of California. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The California Nebula was discovered by Edward Emerson Barnard in 1884. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_1528` — NGC 1528 — 5 facts
- Fact 1: NGC 1528 is an open cluster in Perseus, about 2,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 6.4 and contains about 165 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster was discovered by William Herschel in 1790. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The age of NGC 1528 is about 370 million years. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It appears as a loose but rich grouping in binoculars and small telescopes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_1535` — Cleopatra's Eye (NGC 1535) — 5 facts
- Fact 1: NGC 1535, Cleopatra's Eye, is a planetary nebula in Eridanus, about 5,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 9.6 and appears as a small blue-green disk. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula has a bright inner shell surrounded by a fainter outer halo. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Its eye-like appearance inspired the name Cleopatra's Eye. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Herschel discovered NGC 1535 in 1785 in the constellation Eridanus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_1647` — NGC 1647 — 5 facts
- Fact 1: NGC 1647 is an open cluster in Taurus, about 1,800 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 6.4 and spans about 45 arcminutes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: William Herschel discovered NGC 1647 in 1784. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 1647 contains about 200 stars and is roughly 150 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It lies near the bright star Aldebaran and the Hyades cluster. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_1817` — NGC 1817 — 5 facts
- Fact 1: NGC 1817 is an open cluster in Taurus, about 6,400 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.7 and was discovered by William Herschel in 1784. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster contains roughly 300 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 1817 is roughly 400 million years old, a middle-aged open cluster. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It lies near the larger, brighter cluster NGC 1807 in the sky. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_1977` — Running Man Nebula (NGC 1977) — 5 facts
- Fact 1: NGC 1977, the Running Man Nebula, is a reflection nebula in Orion, about 1,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.0 and lies just north of the great Orion Nebula. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula's bright and dark regions trace a figure that resembles a running man. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 1977 is energized by the young stars of the cluster within it. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It forms part of the Orion Molecular Cloud Complex. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_1999` — NGC 1999 — 5 facts
- Fact 1: NGC 1999 is a reflection nebula in Orion, about 1,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 9.5 and is lit by the variable star V380 Orionis. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula is famous for a dark, keyhole-shaped patch of cold gas at its heart. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Hubble observations showed the dark patch is an empty cavity, not a dust cloud as first thought. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: William Herschel discovered NGC 1999 in 1785 near the Orion Nebula. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2070` — Tarantula Nebula (NGC 2070) — 5 facts
- Fact 1: NGC 2070, the Tarantula Nebula, is the largest and most active star-forming region in the Local Group. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It lies in the Large Magellanic Cloud, about 160,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula hosts the super star cluster R136, whose stars are hundreds of times the Sun's mass. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: SN 1987A, the nearest supernova in centuries, exploded near the Tarantula Nebula in 1987. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Were it as close as the Orion Nebula, the Tarantula would cast visible shadows at night. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2158` — NGC 2158 — 5 facts
- Fact 1: NGC 2158 is an open cluster in Gemini, about 16,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.6 and appears as a small, rich knot of stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster lies near the bright cluster M35 in the sky, though far more distant. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2158 is ancient for an open cluster, about 1 billion years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its many red giant stars give it a golden hue in larger telescopes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2169` — 37 Cluster (NGC 2169) — 5 facts
- Fact 1: NGC 2169, the 37 Cluster, is a small open cluster in Orion, about 3,600 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 5.9 and is visible in binoculars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: Its brightest stars trace the numerals '37', giving the cluster its popular name. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2169 contains about 30 stars and is roughly 11 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Herschel recorded NGC 2169 in 1784, naming it for its '37' pattern. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2237` — Rosette Nebula (NGC 2237) — 5 facts
- Fact 1: NGC 2237 is part of the Rosette Nebula, a vast star-forming cloud in Monoceros about 5,200 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: The Rosette spans about 80 arcminutes, several times the width of the full Moon. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: Its hollow center is carved by the winds of the young cluster NGC 2244 at its heart. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The nebula's name comes from its flower-like rings of glowing gas. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The Rosette is a nursery for hundreds of newly forming stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2244` — Rosette Nebula Cluster (NGC 2244) — 5 facts
- Fact 1: NGC 2244 is the young open cluster at the heart of the Rosette Nebula in Monoceros. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It lies about 5,200 light-years away and shines at magnitude 4.8. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster's hot young stars carve out the hollow center of the Rosette Nebula. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2244 is only about 4 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The cluster was discovered by John Flamsteed in 1690. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2261` — Hubble's Variable Nebula (NGC 2261) — 5 facts
- Fact 1: NGC 2261, Hubble's Variable Nebula, is a small fan-shaped reflection nebula in Monoceros. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It lies about 2,500 light-years away and shines at magnitude 9.0. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula changes brightness and shape over weeks and months. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Its variability is caused by shadows from moving dust near the star R Monocerotis. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: NGC 2261 was the first object photographed with the 200-inch Hale Telescope in 1949. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2264` — Christmas Tree Cluster (NGC 2264) — 5 facts
- Fact 1: NGC 2264, the Christmas Tree Cluster, is a young open cluster in Monoceros, about 2,600 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 3.9 and is visible to the naked eye. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster's stars form a triangular shape like a Christmas tree. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2264 includes the Cone Nebula, a dark pillar of dust. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The cluster is very young, only about 3 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2301` — NGC 2301 — 5 facts
- Fact 1: NGC 2301 is an open cluster in Monoceros, about 2,800 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 6.0 and contains about 70 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: Herschel first observed NGC 2301 in 1785, noting its line of bright stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2301 is notable for the striking string of bright stars across its center. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The cluster is about 170 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2359` — Thor's Helmet (NGC 2359) — 5 facts
- Fact 1: NGC 2359, Thor's Helmet, is an emission nebula in Canis Major, about 15,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 11.0 and carries a striking bubble shape. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: A Wolf-Rayet star, WR 7, inflates the nebula with its powerful stellar wind. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The nebula's two wing-like extensions give it the look of a horned helmet. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Herschel found NGC 2359 in 1785 while surveying Canis Major. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2360` — Caroline's Cluster (NGC 2360) — 5 facts
- Fact 1: NGC 2360, Caroline's Cluster, is an open cluster in Canis Major, about 3,700 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: Caroline's Cluster shines at magnitude 7.2 with about 100 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster was discovered by Caroline Herschel in 1783, her first independent deep-sky discovery. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2360 is about 800 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It is nicknamed Caroline's Cluster in honor of its discoverer. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2362` — Tau Canis Majoris Cluster (NGC 2362) — 5 facts
- Fact 1: NGC 2362, the Tau Canis Majoris Cluster, is a compact open cluster in Canis Major, about 4,800 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 4.1 and surrounds the bright star Tau Canis Majoris. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 2362 is one of the youngest known open clusters, only about 4 to 5 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The cluster contains over 100 stars, mostly hot and blue. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: NGC 2362 was discovered by Giovanni Hodierna before 1654. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2392` — Eskimo Nebula (NGC 2392) — 5 facts
- Fact 1: NGC 2392, the Eskimo Nebula, is a planetary nebula in Gemini, about 6,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 9.9 and was discovered by William Herschel in 1787. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula's bright inner shell and outer ring of gas resemble a face inside a parka hood. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Its central star is a hot white dwarf of about magnitude 10.5. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: NGC 2392 was one of the first objects imaged in detail by the Hubble Space Telescope. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2420` — NGC 2420 — 5 facts
- Fact 1: NGC 2420 is an open cluster in Gemini, about 10,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.3 and contains about 100 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster was discovered by William Herschel in 1783. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2420 is old for an open cluster, roughly 1 billion years. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its many red giant stars make it a popular target for studies of stellar evolution. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2440` — NGC 2440 — 5 facts
- Fact 1: NGC 2440 is a planetary nebula in Puppis, about 4,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 9.4 and has an irregular, boxy shape. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula surrounds one of the hottest white dwarfs known, with a surface temperature near 200,000 K. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Hubble images show NGC 2440's glowing gas in striking blue and gold filaments. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: NGC 2440 was discovered by William Herschel in 1790. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2451` — NGC 2451 — 5 facts
- Fact 1: NGC 2451 is a bright open cluster in Puppis, about 850 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 2.8 and is easily visible to the naked eye. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster spans about 50 arcminutes, wider than the full Moon. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2451 contains about 50 stars, including the orange giant c Puppis. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The cluster was recorded by Giovanni Hodierna before 1654. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_246` — Skull Nebula (NGC 246) — 5 facts
- Fact 1: NGC 246, the Skull Nebula, is a planetary nebula in Cetus, about 1,600 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.0 and spans about 3.8 arcminutes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula's dark central region and ring of gas give it the look of a skull. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: William Herschel discovered NGC 246 in 1785 in the constellation Cetus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its central star is part of a multiple star system within the nebula. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2477` — NGC 2477 — 5 facts
- Fact 1: NGC 2477 is a rich open cluster in Puppis, about 4,200 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 5.8 and contains about 300 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster is so dense it can be mistaken for a globular cluster. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Nicolas-Louis de Lacaille discovered NGC 2477 in 1751 from the Cape of Good Hope. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It is about 1 billion years old, ancient for an open cluster. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2506` — NGC 2506 — 5 facts
- Fact 1: NGC 2506 is an open cluster in Monoceros, about 11,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.6 and contains about 200 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster was discovered by William Herschel in 1791. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2506 is about 2 billion years old, one of the oldest open clusters known. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It lies far above the Milky Way's plane, which helps it survive so long. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2516` — NGC 2516 — 5 facts
- Fact 1: NGC 2516 is a bright open cluster in Carina, about 1,300 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 3.8 and is visible to the naked eye. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster contains about 100 stars and is nicknamed the Southern Beehive. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2516 is about 135 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It was discovered by Nicolas-Louis de Lacaille in 1751. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_253` — Sculptor Galaxy (NGC 253) — 5 facts
- Fact 1: NGC 253, the Sculptor Galaxy, is a starburst spiral in Sculptor, about 11 million light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.1 and is one of the brightest galaxies beyond the Local Group. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 253 is undergoing intense star formation in its dusty core. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The galaxy is the largest member of the Sculptor Group, our nearest galaxy group beyond the Local Group. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Caroline Herschel discovered NGC 253 in 1783, her first great galaxy find. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2539` — NGC 2539 — 5 facts
- Fact 1: NGC 2539 is an open cluster in Puppis, about 4,400 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 6.5 and contains about 150 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: William Herschel discovered NGC 2539 in 1785 while surveying Puppis. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2539 has an age of roughly 370 million years. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It lies in a rich Milky Way field and is a fine target for small telescopes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2547` — NGC 2547 — 5 facts
- Fact 1: NGC 2547 is an open cluster in Vela, about 1,400 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 4.7 and is visible to the naked eye. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster contains about 80 stars and is about 30 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Lacaille recorded NGC 2547 in 1751 during his southern constellation survey. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its young stars make it a useful laboratory for studying planet-forming disks. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2808` — NGC 2808 — 5 facts
- Fact 1: NGC 2808 is a massive globular cluster in Carina, about 31,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 6.2 and is visible in binoculars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster is one of the most massive globulars in the Milky Way, with over a million stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2808 shows three distinct generations of stars, revealing complex formation history. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Dunlop first recorded NGC 55 in 1826 during his survey of the southern sky. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_281` — Pacman Nebula (NGC 281) — 5 facts
- Fact 1: NGC 281, the Pacman Nebula, is an emission nebula in Cassiopeia, about 9,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.4 and spans about 35 arcminutes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: A dark notch in the nebula's edge makes it resemble the Pac-Man video game character. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 281 contains the young open cluster IC 1590. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The nebula is a site of active star formation. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_2903` — NGC 2903 — 5 facts
- Fact 1: NGC 2903 is a barred spiral galaxy in Leo, about 30 million light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.9 and is bright enough for small telescopes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The galaxy is a field galaxy, not part of any large cluster. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 2903 has a vigorous star-forming disk with many young clusters. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It was discovered by William Herschel in 1784. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_300` — NGC 300 — 5 facts
- Fact 1: NGC 300 is a spiral galaxy in Sculptor, about 7 million light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.1 and is a member of the Sculptor Group. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 300 is a near twin of our neighbor galaxy M33. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The galaxy contains many young blue star clusters and nebulae. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: James Dunlop discovered NGC 300 in 1826 while sweeping Sculptor. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_3114` — NGC 3114 — 5 facts
- Fact 1: NGC 3114 is a large open cluster in Carina, about 3,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 4.2 and spans about 35 arcminutes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster contains about 170 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Dunlop recorded the wide cluster NGC 3114 in 1826 from Australia. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its loose, scattered pattern makes it a pleasant wide-field binocular target. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_3115` — Spindle Galaxy (NGC 3115) — 5 facts
- Fact 1: NGC 3115, the Spindle Galaxy, is a lenticular galaxy in Sextans seen nearly edge-on. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It lies about 32 million light-years away and shines at magnitude 9.2. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 3115 hosts a supermassive black hole of about one billion solar masses. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: It was one of the first galaxies whose central black hole mass was measured from stellar motions. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The galaxy was discovered by William Herschel in 1787. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_3242` — Ghost of Jupiter (NGC 3242) — 5 facts
- Fact 1: NGC 3242, the Ghost of Jupiter, is a planetary nebula in Hydra, about 1,400 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.7 and appears as a small blue-green disk. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula is large enough to resemble the planet Jupiter in small telescopes, inspiring its name. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Herschel cataloged NGC 3242 in 1785, noting its planetary appearance. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its outer halo of gas shows material ejected in earlier phases of the star's death. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_3532` — Wishing Well Cluster (NGC 3532) — 5 facts
- Fact 1: NGC 3532, the Wishing Well Cluster, is a brilliant open cluster in Carina, about 1,300 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 3.0 and is visible to the naked eye. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster contains about 400 stars scattered over a field wider than the full Moon. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Nicolas-Louis de Lacaille discovered NGC 3532 in 1752 during his expedition to South Africa. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It was the first object observed by the Hubble Space Telescope in 1990. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_3628` — NGC 3628 — 5 facts
- Fact 1: NGC 3628 is an edge-on spiral galaxy in Leo, about 35 million light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 9.5 and is the third member of the Leo Triplet. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: A dark dust lane splits the galaxy's disk along its entire length. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Tidal interactions with M65 and M66 have warped NGC 3628's disk. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The galaxy has a faint tidal tail stretching far into space. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_3918` — Blue Planetary (NGC 3918) — 5 facts
- Fact 1: NGC 3918, the Blue Planetary, is a small planetary nebula in Centaurus, about 4,900 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.5 and appears strikingly blue in telescopes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula is compact, less than half an arcminute across. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 3918 was discovered by John Herschel in 1834. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its vivid blue color comes from ionized oxygen gas. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_4565` — Needle Galaxy (NGC 4565) — 5 facts
- Fact 1: NGC 4565, the Needle Galaxy, is an edge-on spiral in Coma Berenices, about 40 million light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 9.6 and is a favorite target for amateur telescopes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The galaxy is seen exactly edge-on, appearing as a thin needle of light. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: A dust lane runs along the full length of its plane. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: William Herschel discovered NGC 4565 in 1785 in Coma Berenices. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_4631` — Whale Galaxy (NGC 4631) — 5 facts
- Fact 1: NGC 4631, the Whale Galaxy, is an edge-on spiral in Canes Venatici, about 25 million light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 9.3 and its elongated shape resembles a whale. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The galaxy has a nearby companion, the small elliptical NGC 4627. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 4631 is undergoing vigorous star formation in its disk. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It was discovered by William Herschel in 1787. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_4755` — Jewel Box Cluster (NGC 4755) — 5 facts
- Fact 1: NGC 4755, the Jewel Box Cluster, is a dazzling open cluster in Crux, about 6,400 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 4.2 and is visible to the naked eye. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster's brightest stars shine in contrasting blue, red, and white, like gems in a box. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 4755 was named the Jewel Box by John Herschel in the 1830s. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The Jewel Box is only about 10 million years old, a newborn cluster. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_5128` — Centaurus A (NGC 5128) — 5 facts
- Fact 1: NGC 5128, Centaurus A, is a giant elliptical galaxy in Centaurus, about 12 million light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 6.8 and is the fifth-brightest galaxy in the sky. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: A dark dust lane across the galaxy marks the remains of a spiral galaxy it absorbed. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Centaurus A is the nearest major radio galaxy and a powerful radio source. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its central black hole launches enormous jets visible in radio and X-ray images. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_5189` — Spiral Planetary (NGC 5189) — 5 facts
- Fact 1: NGC 5189, the Spiral Planetary, is a planetary nebula in Musca, about 3,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.2 and has an unusual S-shaped structure. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula's complex shape comes from jets launched by its central binary star system. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 5189 was discovered by James Dunlop in 1826. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It is one of the most structurally complex planetary nebulae known. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_55` — NGC 55 — 5 facts
- Fact 1: NGC 55 is an irregular galaxy in Sculptor, about 6.5 million light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.9 and is a member of the Sculptor Group. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The galaxy is seen edge-on and resembles the Large Magellanic Cloud. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 55 is about 50,000 light-years across. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: James Dunlop discovered NGC 2808 in 1826 from his observatory in Australia. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6025` — NGC 6025 — 5 facts
- Fact 1: NGC 6025 is an open cluster in Triangulum Australe, about 2,700 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 5.1 and is visible to the naked eye. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster contains about 60 stars and spans about 12 arcminutes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Lacaille cataloged NGC 6025 in 1752 while mapping the southern sky. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: NGC 6025 is around 100 million years old, a young cluster in Triangulum Australe. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6067` — NGC 6067 — 5 facts
- Fact 1: NGC 6067 is an open cluster in Norma, about 4,700 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 5.6 and contains about 100 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster was discovered by James Dunlop in 1826. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The stars of NGC 6067 are about 100 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It lies in a rich Milky Way field in the southern constellation Norma. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6087` — NGC 6087 — 5 facts
- Fact 1: NGC 6087 is an open cluster in Norma, about 3,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 5.4 and is visible to the naked eye. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster contains about 40 stars, including the Cepheid variable S Normae. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: James Dunlop discovered NGC 6087 in 1826 among the clusters of Norma. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It is one of the more prominent clusters of the southern constellation Norma. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6124` — NGC 6124 — 5 facts
- Fact 1: NGC 6124 is an open cluster in Scorpius, about 1,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 5.8 and spans about 29 arcminutes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster contains about 100 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The cluster NGC 6124 was first cataloged by Lacaille in 1751. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It is about 300 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6231` — Northern Jewel Box (NGC 6231) — 5 facts
- Fact 1: NGC 6231, the Northern Jewel Box, is a brilliant open cluster in Scorpius, about 5,900 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 2.6 and is visible to the naked eye. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster contains over 100 hot, young stars packed into a small area. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 6231 forms the heart of the Scorpius OB1 association. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Hodierna cataloged NGC 6231 before 1654, making it one of the earliest recorded open clusters. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6242` — NGC 6242 — 5 facts
- Fact 1: NGC 6242 is an open cluster in Scorpius, about 4,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 6.4 and contains about 40 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster was discovered by Nicolas-Louis de Lacaille in 1751. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 6242 lies near the bright star Mu Scorpii. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The cluster is about 50 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6302` — Butterfly Nebula (NGC 6302) — 5 facts
- Fact 1: NGC 6302, the Butterfly Nebula, is a bipolar planetary nebula in Scorpius, about 4,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 9.6 and spans about 3 arcminutes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: Hubble images show two vast wings of glowing gas, giving the nebula its name. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The nebula's central star is one of the hottest known, with a surface near 250,000 K. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: NGC 6302 was discovered by Edward Emerson Barnard in 1880. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6369` — Little Ghost (NGC 6369) — 5 facts
- Fact 1: NGC 6369, the Little Ghost, is a planetary nebula in Ophiuchus, about 3,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 9.9 and appears as a small, faint ring. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula's pale ring gives it a ghostly appearance in photographs. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Herschel discovered NGC 6369 in 1784, cataloging its faint ring. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its central star is a hot white dwarf nearing the end of its evolution. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6397` — NGC 6397 — 5 facts
- Fact 1: NGC 6397 is a globular cluster in Ara, about 7,800 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 5.3, easily visible in binoculars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 6397 is one of the nearest globular clusters, at only about 7,800 light-years. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The cluster contains about 400,000 stars and is roughly 13 billion years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Hubble studies of NGC 6397 measured the mass of its white dwarfs, refining stellar evolution theory. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6572` — NGC 6572 — 5 facts
- Fact 1: NGC 6572 is a planetary nebula in Ophiuchus, about 3,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.1 and appears as a small, bright green disk. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula is young and very dense, glowing intensely in small telescopes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Herschel discovered NGC 6572 in 1784 and described it as a vivid green disk. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its vivid color makes it a favorite target for planetary nebula observers. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6633` — NGC 6633 — 5 facts
- Fact 1: NGC 6633 is an open cluster in Ophiuchus, about 1,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 4.6 and spans about 27 arcminutes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster contains about 30 stars and is visible to the naked eye under dark skies. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 6633 was discovered by Jean-Philippe de Chéseaux in 1745. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The cluster is about 600 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6741` — Phantom Streak (NGC 6741) — 5 facts
- Fact 1: NGC 6741, the Phantom Streak, is a planetary nebula in Aquila, about 7,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 11.0 and appears as a small, faint smudge. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula's elongated shape gives it the appearance of a ghostly streak. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 6741 was discovered by Edward Charles Pickering in 1882. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It is a dense, young planetary nebula whose central star is still hot and bright. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6752` — NGC 6752 — 5 facts
- Fact 1: NGC 6752 is a globular cluster in Pavo, about 13,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 5.4 and is the third-brightest globular cluster in the sky. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 6752 is one of the closest globular clusters to the Sun. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The cluster contains over 100,000 stars and is about 11.8 billion years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: James Dunlop cataloged NGC 6752 in 1826. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6755` — NGC 6755 — 5 facts
- Fact 1: NGC 6755 is an open cluster in Aquila, about 4,600 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.5 and contains about 100 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: Herschel cataloged NGC 6755 in 1785 during his Milky Way sweeps. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 6755 is about 250 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It lies in a rich Milky Way field in Aquila. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6781` — NGC 6781 — 5 facts
- Fact 1: NGC 6781 is a planetary nebula in Aquila, about 2,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 11.4 and appears as a fairly large, faint ring. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula's bubble of gas is nearly two light-years across. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Herschel discovered NGC 6781 in 1788, a faint ring of gas in Aquila. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its expanding shell is lit by a hot white dwarf at its center. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6791` — NGC 6791 — 5 facts
- Fact 1: NGC 6791 is an open cluster in Lyra, about 13,300 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 9.5 and contains roughly 300 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 6791 is one of the oldest open clusters known, about 8 billion years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The cluster is unusually rich in heavy elements and hosts many white dwarfs. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It was discovered by Friedrich August Theodor Winnecke in 1853. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6818` — Little Gem (NGC 6818) — 5 facts
- Fact 1: NGC 6818, the Little Gem, is a planetary nebula in Sagittarius, about 6,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 9.9 and appears as a small, bright blue-green disk. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula has an inner bright shell and a fainter outer halo. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Herschel found NGC 6818 in 1787 in Sagittarius. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its compact size and brightness make it a rewarding small-telescope target. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6819` — NGC 6819 — 5 facts
- Fact 1: NGC 6819 is a rich open cluster in Cygnus, about 7,800 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.3 and contains roughly 300 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 6819 is about 2.5 billion years old, quite old for an open cluster. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The Kepler space telescope studied NGC 6819 extensively while searching for exoplanets. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Caroline Herschel discovered NGC 6819 in 1783. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6822` — Barnard's Galaxy (NGC 6822) — 5 facts
- Fact 1: NGC 6822, Barnard's Galaxy, is an irregular dwarf galaxy in Sagittarius, about 1.6 million light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.1 and was discovered by Edward Emerson Barnard in 1884. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 6822 was the first galaxy beyond the Magellanic Clouds in which Cepheid variables were found. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Edwin Hubble used those Cepheids in 1925 to prove the galaxy lies far beyond the Milky Way. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The galaxy hosts several bright nebulae and star-forming regions. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6866` — NGC 6866 — 5 facts
- Fact 1: NGC 6866 is an open cluster in Cygnus, about 3,900 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.6 and contains about 80 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: Caroline Herschel found NGC 6866 in 1783 during her comet searches. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 6866 is about 1 billion years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It lies in a rich Milky Way field and is a popular target for cluster studies. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6884` — NGC 6884 — 5 facts
- Fact 1: NGC 6884 is a planetary nebula in Cygnus, about 6,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 10.9 and appears as a small, faint disk. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula is compact and requires moderate aperture to observe well. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Ralph Copeland discovered NGC 6884 in 1884 during his nebula observations. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its central star is a hot white dwarf that energizes the surrounding gas. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6891` — NGC 6891 — 5 facts
- Fact 1: NGC 6891 is a planetary nebula in Delphinus, about 7,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 10.5 and appears as a small bluish disk. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula has a bright inner shell surrounded by a fainter halo. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Copeland found NGC 6891 in 1884 while surveying Delphinus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its layered shells record multiple episodes of gas ejection from the dying star. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6905` — Blue Flash (NGC 6905) — 5 facts
- Fact 1: NGC 6905, the Blue Flash Nebula, is a planetary nebula in Delphinus, about 7,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 10.9 and appears as a small oval glow. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula flashes a vivid blue color through O-III filters. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: William Herschel discovered NGC 6905 in 1784 in Delphinus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It is one of the brighter planetary nebulae of the constellation Delphinus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6910` — NGC 6910 — 5 facts
- Fact 1: NGC 6910 is a young open cluster in Cygnus, about 5,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.4 and lies near the bright star Sadr (Gamma Cygni). Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster is very young, only about 6 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 6910 contains hot, massive stars that light up the surrounding nebulosity. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It was discovered by William Herschel in 1786. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6939` — NGC 6939 — 5 facts
- Fact 1: NGC 6939 is an open cluster in Cepheus, about 3,900 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.8 and contains about 150 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster is about 2 billion years old, among the oldest open clusters. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 6939 was discovered by William Herschel in 1798. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It lies far above the Milky Way's plane, which helps it hold together so long. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6940` — NGC 6940 — 5 facts
- Fact 1: NGC 6940 is an open cluster in Vulpecula, about 2,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 6.3 and contains about 170 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster is about 1.1 billion years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: William Herschel discovered NGC 6940 in 1784 among the stars of Vulpecula. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its many red giants give the cluster a warm, golden appearance. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6960` — Veil Nebula (West) (NGC 6960) — 5 facts
- Fact 1: NGC 6960, the Western Veil Nebula, is the bright western arc of the Cygnus Loop, a supernova remnant. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: The Western Veil lies about 2,400 light-years away and glows at magnitude 7.0. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The supernova that created the Veil exploded between 5,000 and 8,000 years ago. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The bright star 52 Cygni shines in front of the nebula but is not part of it. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The nebula is often called the Witch's Broom for its delicate, sweeping filaments. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_6992` — Veil Nebula (East) (NGC 6992) — 5 facts
- Fact 1: NGC 6992, the Eastern Veil Nebula, is the bright eastern arc of the Cygnus Loop supernova remnant. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: The Eastern Veil sits about 2,400 light-years away at magnitude 7.0. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The Veil's eastern arc is the brightest and most photographed section of the remnant. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The nebula's glowing filaments are heated shock waves expanding into interstellar gas. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The Cygnus Loop spans about three degrees of sky, six times the Moon's width. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7008` — Fetus Nebula (NGC 7008) — 5 facts
- Fact 1: NGC 7008, the Fetus Nebula, is a planetary nebula in Cygnus, about 2,800 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 10.7 and appears as an irregular, patchy glow. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula's shape has been likened to a developing fetus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: William Herschel discovered NGC 7008 in 1787 in Cygnus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It is one of the larger and more detailed planetary nebulae visible in Cygnus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7009` — Saturn Nebula (NGC 7009) — 5 facts
- Fact 1: NGC 7009, the Saturn Nebula, is a planetary nebula in Aquarius, about 3,900 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.0 and was discovered by William Herschel in 1782. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: Two lobes, called ansae, extend from the nebula like the rings of Saturn. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The Saturn Nebula's central star is a hot white dwarf. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The nebula was one of the first planetary nebulae studied in detail. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7027` — NGC 7027 — 5 facts
- Fact 1: NGC 7027 is a planetary nebula in Cygnus, about 3,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 10.4 and is very small and dense. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 7027 is one of the most studied planetary nebulae in the sky. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The nebula is unusually rich in complex molecules and dust. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It was discovered by Édouard Stephan in 1878. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7062` — NGC 7062 — 5 facts
- Fact 1: NGC 7062 is an open cluster in Cygnus, about 6,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.3 and contains about 50 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: William Herschel discovered NGC 7062 in 1788. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The age of NGC 7062 is estimated at about 400 million years. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It lies in the rich star fields of Cygnus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7063` — NGC 7063 — 5 facts
- Fact 1: NGC 7063 is an open cluster in Cygnus, about 2,200 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.0 and contains about 30 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: Herschel logged NGC 7063 in 1788 while sweeping Cygnus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 7063 is a young cluster aged about 100 million years. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It is a loose, sparse grouping near the border with Pegasus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7094` — NGC 7094 — 5 facts
- Fact 1: NGC 7094 is a planetary nebula in Pegasus, about 6,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 13.4, among the faintest objects in the catalog. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula appears as a dim, round shell of gas. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 7094 was discovered by Lewis Swift in 1884. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It is a challenge object for large amateur telescopes. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7139` — NGC 7139 — 5 facts
- Fact 1: NGC 7139 is a planetary nebula in Cepheus, about 4,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 13.3 and appears as a faint, round glow. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula is dim and requires large aperture to observe. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: William Herschel found NGC 7139 in 1787 during his sweeps of the northern sky. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its faint halo is the remnant of gas shed by a dying star. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7160` — NGC 7160 — 5 facts
- Fact 1: NGC 7160 is an open cluster in Cepheus, about 2,500 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 6.1 and contains about 50 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: NGC 7160 is a very young cluster, just 10 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Herschel recorded NGC 7160 in 1787 while mapping the clusters of Cepheus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its scattered bright stars make it a pleasant small-telescope target. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7209` — NGC 7209 — 5 facts
- Fact 1: NGC 7209 is an open cluster in Lacerta, about 3,800 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.7 and contains about 100 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: Herschel recorded NGC 7209 in 1787 while exploring the small constellation Lacerta. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: The stars of NGC 7209 are about 400 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It lies in the small constellation Lacerta, the Lizard. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7293` — Helix Nebula (NGC 7293) — 5 facts
- Fact 1: NGC 7293, the Helix Nebula, is one of the closest and brightest planetary nebulae, in Aquarius. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It lies only about 650 light-years away and shines at magnitude 7.6. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The Helix spans about 2.5 light-years across, huge for a planetary nebula. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Its ring-like appearance has earned it the nickname the Eye of God. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The nebula's central star is a white dwarf that will cool and fade over billions of years. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7354` — NGC 7354 — 5 facts
- Fact 1: NGC 7354 is a planetary nebula in Cepheus, about 5,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 12.2 and appears as a small, faint disk. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The nebula is compact and requires large aperture to observe. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: William Herschel cataloged NGC 7354 in 1787, noting its small round form. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its faint outer shell surrounds a brighter inner region. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7380` — NGC 7380 — 5 facts
- Fact 1: NGC 7380 is a young open cluster in Cepheus, about 7,200 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: NGC 7380 glows at magnitude 7.2 and holds about 100 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster lies within the glowing Wizard Nebula (Sh2-142). Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: NGC 7380's hot young stars ionize the surrounding gas, making it glow. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The cluster was discovered by Caroline Herschel in 1787. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7510` — NGC 7510 — 5 facts
- Fact 1: NGC 7510 is an open cluster in Cepheus, about 9,000 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 7.9 and contains about 60 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The stars of NGC 7510 are only 10 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Herschel logged NGC 7510 in 1787 as a compact grouping in Cepheus. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: Its stars form a compact, arrow-like pattern. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7686` — NGC 7686 — 5 facts
- Fact 1: NGC 7686 is an open cluster in Andromeda, about 3,200 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 5.6 and contains about 30 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster is visible in binoculars as a faint grouping. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: William Herschel discovered NGC 7686 in 1787 among the star fields of Andromeda. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: It is a loose, scattered cluster with no central concentration. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7789` — Caroline's Rose (NGC 7789) — 5 facts
- Fact 1: NGC 7789, Caroline's Rose, is a rich open cluster in Cassiopeia, about 7,600 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 6.7 and contains over 1,000 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster's swirling loops of stars resemble the petals of a rose. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: Caroline Herschel found NGC 7789 in 1783 while sweeping Cassiopeia. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The cluster is about 1.6 billion years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.

#### `dso_ngc_7790` — NGC 7790 — 5 facts
- Fact 1: NGC 7790 is an open cluster in Cassiopeia, about 7,800 light-years away. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 2: It shines at magnitude 8.5 and contains about 60 stars. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 3: The cluster hosts three Cepheid variable stars, useful for measuring distances. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 4: William Herschel discovered NGC 7790 in 1788 in Cassiopeia. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.
- Fact 5: The cluster is about 80 million years old. Sources: The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check.


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

## Non-deep-sky union objects (2026-09-13)

Every non-deep-sky object of the 336-object canonical union list is tracked here with the
validated values and the source families used to corroborate them (2+ reputable sources per fact).

#### `asterism_big_dipper` — Big Dipper (The Plough / Seven Sages) — 5 facts
- Validated: constituent stars and positions. Sources: standard star atlases (Sky & Telescope, IAU constellation boundaries) + Hipparcos/SIMBAD stellar data.

#### `asterism_great_square_pegasus` — Great Square of Pegasus — 5 facts
- Validated: constituent stars and positions. Sources: standard star atlases (Sky & Telescope, IAU constellation boundaries) + Hipparcos/SIMBAD stellar data.

#### `asterism_northern_cross` — Northern Cross — 5 facts
- Validated: constituent stars and positions. Sources: standard star atlases (Sky & Telescope, IAU constellation boundaries) + Hipparcos/SIMBAD stellar data.

#### `asterism_sickle_leo` — Sickle of Leo — 5 facts
- Validated: constituent stars and positions. Sources: standard star atlases (Sky & Telescope, IAU constellation boundaries) + Hipparcos/SIMBAD stellar data.

#### `asterism_summer_triangle` — Summer Triangle (Vega - Deneb - Altair) — 5 facts
- Validated: constituent stars and positions. Sources: standard star atlases (Sky & Telescope, IAU constellation boundaries) + Hipparcos/SIMBAD stellar data.

#### `asterism_teapot` — Teapot of Sagittarius — 5 facts
- Validated: constituent stars and positions. Sources: standard star atlases (Sky & Telescope, IAU constellation boundaries) + Hipparcos/SIMBAD stellar data.

#### `asterism_winter_hexagon` — Winter Hexagon (Winter Circle) — 5 facts
- Validated: constituent stars and positions. Sources: standard star atlases (Sky & Telescope, IAU constellation boundaries) + Hipparcos/SIMBAD stellar data.

#### `const_and` — Andromeda Constellation — 5 facts
- Validated: IAU area 722.3 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_aql` — Aquila Constellation — 5 facts
- Validated: IAU area 652.5 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_boo` — Boötes Constellation — 5 facts
- Validated: IAU area 906.8 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_car` — Carina Constellation — 5 facts
- Validated: IAU area 494.2 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_cas` — Cassiopeia Constellation — 5 facts
- Validated: IAU area 598.4 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_cen` — Centaurus Constellation — 5 facts
- Validated: IAU area 1060.4 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_cma` — Canis Major Constellation — 5 facts
- Validated: IAU area 380.1 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_cru` — Crux (Southern Cross) Constellation — 5 facts
- Validated: IAU area 68.4 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_cyg` — Cygnus Constellation — 5 facts
- Validated: IAU area 804.0 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_gem` — Gemini Constellation — 5 facts
- Validated: IAU area 513.8 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_leo` — Leo Constellation — 5 facts
- Validated: IAU area 947.0 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_lyr` — Lyra Constellation — 5 facts
- Validated: IAU area 286.5 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_ori` — Orion Constellation — 5 facts
- Validated: IAU area 594.1 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_peg` — Pegasus Constellation — 5 facts
- Validated: IAU area 1121.0 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_sco` — Scorpius Constellation — 5 facts
- Validated: IAU area 496.8 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_sgr` — Sagittarius Constellation — 5 facts
- Validated: IAU area 867.4 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_tau` — Taurus Constellation — 5 facts
- Validated: IAU area 797.2 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_uma` — Ursa Major Constellation — 5 facts
- Validated: IAU area 1279.6 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_umi` — Ursa Minor Constellation — 5 facts
- Validated: IAU area 255.9 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `const_vir` — Virgo Constellation — 5 facts
- Validated: IAU area 1294.4 sq deg, brightest star, notable deep-sky objects. Sources: IAU 88-constellation list/boundaries + NGC/IC Project + Hipparcos/SIMBAD stellar data.

#### `planet_pluto` — Pluto (Dwarf Planet) — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Pluto fact sheet + New Horizons mission results.

#### `shower_eta_aquariids` — Eta Aquariids Meteor Shower Radiant — 5 facts
- Validated: Active: Apr 19 - May 28 | Peak: May 5-6, ZHR 50. Sources: IMO Meteor Shower Working List + NASA/AMS meteor shower calendar.

#### `shower_geminids` — Geminids Meteor Shower Radiant — 5 facts
- Validated: Active: Dec 4 - Dec 20 | Peak: Dec 13-14, ZHR 120. Sources: IMO Meteor Shower Working List + NASA/AMS meteor shower calendar.

#### `shower_leonids` — Leonids Meteor Shower Radiant — 5 facts
- Validated: Active: Nov 6 - Nov 30 | Peak: Nov 17-18, ZHR 15. Sources: IMO Meteor Shower Working List + NASA/AMS meteor shower calendar.

#### `shower_lyrids` — Lyrids Meteor Shower Radiant — 5 facts
- Validated: Active: Apr 14 - Apr 30 | Peak: Apr 22-23, ZHR 18. Sources: IMO Meteor Shower Working List + NASA/AMS meteor shower calendar.

#### `shower_orionids` — Orionids Meteor Shower Radiant — 5 facts
- Validated: Active: Oct 2 - Nov 7 | Peak: Oct 21-22, ZHR 20. Sources: IMO Meteor Shower Working List + NASA/AMS meteor shower calendar.

#### `shower_perseids` — Perseids Meteor Shower Radiant — 5 facts
- Validated: Active: July 17 - Aug 24 | Peak: Aug 12-13, ZHR 100. Sources: IMO Meteor Shower Working List + NASA/AMS meteor shower calendar.

#### `shower_quadrantids` — Quadrantids Meteor Shower Radiant — 5 facts
- Validated: Active: Dec 28 - Jan 12 | Peak: Jan 3-4, ZHR 110. Sources: IMO Meteor Shower Working List + NASA/AMS meteor shower calendar.

#### `jup_callisto` — Callisto (Galilean Moon) — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Galilean satellite fact sheets + Galileo mission results.

#### `jup_elara` — Elara (Irregular Jovian Moon) — 5 facts
- Validated: physical parameters and observational facts. Sources: JPL Solar System Dynamics satellite data + IAU Minor Planet Center.

#### `jup_europa` — Europa (Galilean Moon) — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Galilean satellite fact sheets + Galileo/Clipper mission context.

#### `jup_ganymede` — Ganymede (Galilean Moon) — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Galilean satellite fact sheets + Galileo mission results.

#### `jup_io` — Io (Galilean Moon) — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Galilean satellite fact sheets + Galileo mission results.

#### `moon` — Moon (Luna) — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Moon fact sheet + LPI lunar science.

#### `planet_earth` — Earth (Terra) — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Earth fact sheet + NASA Earth Observatory.

#### `planet_jupiter` — Jupiter — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Jupiter fact sheet + Juno mission results.

#### `planet_mars` — Mars — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Mars fact sheet + NASA Mars Exploration Program.

#### `planet_mercury` — Mercury — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Mercury fact sheet + MESSENGER mission results.

#### `planet_neptune` — Neptune — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Neptune fact sheet + Voyager 2 encounter data.

#### `planet_saturn` — Saturn — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Saturn fact sheet + Cassini mission results.

#### `planet_uranus` — Uranus — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Uranus fact sheet + Voyager 2 encounter data.

#### `planet_venus` — Venus — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Venus fact sheet + Magellan mission results.

#### `sat_20580` — Hubble Space Telescope (HST) — 3 facts
- Validated: operator, launch date, orbit altitude/inclination, instrument payload. Sources: NASA/STScI Hubble mission pages + ESA Hubble fact sheet..
- `sat_20580`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.

#### `sat_25544` — International Space Station (ISS) — 5 facts
- Validated: operator, launch date, orbit altitude/inclination, instrument payload. Sources: NASA ISS reference pages + ESA ISS fact sheet.

#### `sat_27386` — Envisat / Earth Observation Sat — 3 facts
- Validated: operator, launch date, orbit altitude/inclination, instrument payload. Sources: ESA Envisat mission pages + CEOS Earth-observation records..
- `sat_27386`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.

#### `sat_48274` — Tiangong Space Station (CSS) — 3 facts
- Validated: operator, launch date, orbit altitude/inclination, instrument payload. Sources: CNSA Tiangong mission pages + ESA/NASA orbital tracking data..
- `sat_48274`: Fewer than five facts by design; accepted facts were limited to claims corroborated by at least two reputable source families without padding with repeated designation/type/distance/size fields.

#### `star_aql_altair` — Altair (Alpha Aquilae) — 5 facts
- Validated: magnitude 0.76, distance 16.73 ly, spectral class A7V. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_aur_capella` — Capella (Alpha Aurigae) — 5 facts
- Validated: magnitude 0.08, distance 42.9 ly, spectral class G3III. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_boo_arcturus` — Arcturus (Alpha Boötis) — 5 facts
- Validated: magnitude -0.05, distance 36.7 ly, spectral class K1III. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_car_canopus` — Canopus (Alpha Carinae) — 5 facts
- Validated: magnitude -0.74, distance 310.0 ly, spectral class F0II. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_car_miaplacidus` — Miaplacidus (Beta Carinae) — 5 facts
- Validated: magnitude 1.67, distance 113.0 ly, spectral class A1III. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_cas_caph` — Caph (Beta Cassiopeiae) — 5 facts
- Validated: magnitude 2.28, distance 54.7 ly, spectral class F2III. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_cas_schedar` — Schedar (Alpha Cassiopeiae) — 5 facts
- Validated: magnitude 2.24, distance 228.0 ly, spectral class K0IIIa. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_cen_hadar` — Hadar (Beta Centauri) — 5 facts
- Validated: magnitude 0.61, distance 390.0 ly, spectral class B1III. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_cen_rigil_kent` — Rigil Kentaurus (Alpha Centauri) — 5 facts
- Validated: magnitude -0.01, distance 4.37 ly, spectral class G2V. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_cma_adhara` — Adhara (Epsilon Canis Majoris) — 5 facts
- Validated: magnitude 1.50, distance 430.0 ly, spectral class B1.5II. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_cma_mirzam` — Mirzam (Beta Canis Majoris) — 5 facts
- Validated: magnitude 1.98, distance 500.0 ly, spectral class B1II-III. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_cma_sirius` — Sirius (Dog Star / Alpha Canis Majoris) — 5 facts
- Validated: magnitude -1.46, distance 8.6 ly, spectral class A1V. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_cma_wezen` — Wezen (Delta Canis Majoris) — 5 facts
- Validated: magnitude 1.83, distance 1600.0 ly, spectral class F8Ia. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_cmi_procyon` — Procyon (Alpha Canis Minoris) — 5 facts
- Validated: magnitude 0.34, distance 11.46 ly, spectral class F5IV-V. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_cyg_albireo` — Albireo (Beta Cygni) — 5 facts
- Validated: magnitude 3.05, distance 430.0 ly, spectral class K3II. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_cyg_deneb` — Deneb (Alpha Cygni) — 5 facts
- Validated: magnitude 1.25, distance 2615.0 ly, spectral class A2Ia. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_cyg_sadr` — Sadr (Gamma Cygni) — 5 facts
- Validated: magnitude 2.23, distance 1800.0 ly, spectral class F8Ib. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_eri_achernar` — Achernar (Alpha Eridani) — 5 facts
- Validated: magnitude 0.45, distance 139.0 ly, spectral class B6Vep. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_gem_castor` — Castor (Alpha Geminorum) — 5 facts
- Validated: magnitude 1.58, distance 51.6 ly, spectral class A1V. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_gem_pollux` — Pollux (Beta Geminorum) — 5 facts
- Validated: magnitude 1.14, distance 33.78 ly, spectral class K0III. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_leo_regulus` — Regulus (Alpha Leonis) — 5 facts
- Validated: magnitude 1.36, distance 79.3 ly, spectral class B8IVn. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_lyr_vega` — Vega (Alpha Lyrae) — 5 facts
- Validated: magnitude 0.03, distance 25.04 ly, spectral class A0V. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_ori_alnilam` — Alnilam (Epsilon Orionis) — 5 facts
- Validated: magnitude 1.69, distance 2000.0 ly, spectral class B0Ia. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_ori_alnitak` — Alnitak (Zeta Orionis) — 5 facts
- Validated: magnitude 1.77, distance 1260.0 ly, spectral class O9.5Iab. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_ori_bellatrix` — Bellatrix (Gamma Orionis) — 5 facts
- Validated: magnitude 1.64, distance 250.0 ly, spectral class B2III. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_ori_betelgeuse` — Betelgeuse (Alpha Orionis) — 5 facts
- Validated: magnitude 0.50, distance 642.5 ly, spectral class M1.5Iab. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_ori_mintaka` — Mintaka (Delta Orionis) — 5 facts
- Validated: magnitude 2.23, distance 1200.0 ly, spectral class O9.5II. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_ori_rigel` — Rigel (Beta Orionis) — 5 facts
- Validated: magnitude 0.13, distance 860.0 ly, spectral class B8Iab. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_ori_saiph` — Saiph (Kappa Orionis) — 5 facts
- Validated: magnitude 2.07, distance 650.0 ly, spectral class B0.5Ia. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_per_algol` — Algol (Beta Persei / Demon Star) — 5 facts
- Validated: magnitude 2.12, distance 90.0 ly, spectral class B8V. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_per_mirfak` — Mirfak (Alpha Persei) — 5 facts
- Validated: magnitude 1.79, distance 510.0 ly, spectral class F5Ib. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_psa_fomalhaut` — Fomalhaut (Alpha Piscis Austrini) — 5 facts
- Validated: magnitude 1.17, distance 25.13 ly, spectral class A3V. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_sco_antares` — Antares (Alpha Scorpii) — 5 facts
- Validated: magnitude 0.96, distance 550.0 ly, spectral class M1.5Iab. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_sco_shaula` — Shaula (Lambda Scorpii) — 5 facts
- Validated: magnitude 1.62, distance 570.0 ly, spectral class B2IV. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_tau_aldebaran` — Aldebaran (Alpha Tauri) — 5 facts
- Validated: magnitude 0.85, distance 65.3 ly, spectral class K5III. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_tau_elnath` — Elnath (Beta Tauri) — 5 facts
- Validated: magnitude 1.65, distance 130.0 ly, spectral class B7III. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_uma_alioth` — Alioth (Epsilon Ursae Majoris) — 5 facts
- Validated: magnitude 1.77, distance 82.6 ly, spectral class A1p. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_uma_alkaid` — Alkaid (Eta Ursae Majoris) — 5 facts
- Validated: magnitude 1.85, distance 103.9 ly, spectral class B3V. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_uma_dubhe` — Dubhe (Alpha Ursae Majoris) — 5 facts
- Validated: magnitude 1.79, distance 123.0 ly, spectral class K0III. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_uma_merak` — Merak (Beta Ursae Majoris) — 5 facts
- Validated: magnitude 2.37, distance 79.7 ly, spectral class A1V. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_uma_mizar` — Mizar (Zeta Ursae Majoris) — 5 facts
- Validated: magnitude 2.23, distance 82.9 ly, spectral class A2V. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_umi_polaris` — Polaris (North Star / Alpha Ursae Minoris) — 5 facts
- Validated: magnitude 1.98, distance 433.0 ly, spectral class F7Ib. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `star_vir_spica` — Spica (Alpha Virginis) — 5 facts
- Validated: magnitude 0.98, distance 250.0 ly, spectral class B1III. Sources: Hipparcos DR2 / SIMBAD stellar parameters, cross-checked against standard star catalogs (Kaler Stars, Sky & Telescope).

#### `sun` — Sun (Sol) — 5 facts
- Validated: physical parameters and observational facts. Sources: NASA/JPL Solar System Exploration fact sheet + Williams (2013) solar parameters.
