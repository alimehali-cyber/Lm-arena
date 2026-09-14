# SOURCES — Space Museum Asset Provenance Index

Date: 2026-09-14
M-1 reconnaissance — planned assets, product, publisher, URL (traceability, not licence record per §2.3 A1, A2, §27)

Every shipped asset will have manifest entry with product name, publisher, credit string, URL, what data means, processing applied. This file is the human-readable index that mirrors manifests.

## Selection Principle (§27)

- Choose best available asset per object on quality alone (§27.1)
- Licensing is not agent's problem (§27.1 A1) — owner clears rights afterwards
- Record provenance: product, publisher, URL, credit string in pack manifest (§27.2 A2)
- Never invent asset, dataset, product name or URL (§27.2 A3) — if URL unverifiable, report S1 rather than guessing
- For this M-1 doc, URLs are planned/best-known product pages, to be verified during fetch. Where URL not yet verified, marked "TO VERIFY".

## Per-Object Best Available (per §27.4 ranking, quality-first)

### 1. Sun
- **SDO/AIA and HMI frames** — Product: SDO/AIA Level 1 4096x4096, HMI continuum — Publisher: NASA/SDO, JSOC — URL: https://jsoc.stanford.edu/ and https://sdo.gsfc.nasa.gov/data/ — Credit: "Courtesy of NASA/SDO and the AIA and HMI science teams" — Notes: CC0 waiver per JSOC, requested credit
- **Carrington EUV map** — Product: NASA SVS item 30362 EUV Carrington — Publisher: NASA SVS — URL: https://svs.gsfc.nasa.gov/30362/ — TO VERIFY exact file — Notes: used only in labelled EUV mode
- **Full AIA series** (bigger budget) — Product: AIA 94,131,171,193,211,304,335,1600,1700 — Publisher: NASA/SDO — URL: same as above — Notes: EUV mode real data

### 2. Mercury
- **MESSENGER MDIS BDR** — Product: MESSENGER MDIS Basemap Reduced Data Record, 166 m/px global mosaic — Publisher: NASA/JHUAPL, USGS Astrogeology — URL: https://pds-imaging.jpl.nasa.gov/volumes/mess.html and https://astrogeology.usgs.gov/search/map/MESSENGER/Mercury/Messenger_Global_Mosaic_166m — TO VERIFY — Credit: NASA/JHUAPL/Carnegie
- DEM: MESSENGER MLA + stereo DTM where available — Publisher: NASA — URL: PDS Geosciences Node — TO VERIFY

### 3. Venus
- **Magellan FMAP** — Product: Magellan SAR FMAP global mosaic, 75m/px radar — Publisher: NASA/JPL, USGS — URL: https://pds-imaging.jpl.nasa.gov/volumes/magellan.html — Credit: NASA/JPL
- **Akatsuki UV** — Product: Akatsuki UVI 283nm cloud maps — Publisher: JAXA/ISAS — URL: https://darts.isas.jaxa.jp/planet/project/akatsuki/ — TO VERIFY — Notes: enhanced colour mode, labelled
- **Venus topography** — Product: Magellan GTDR — Publisher: NASA — URL: same PDS — Notes: for horizon maps if used

### 4. Earth
- **Landsat albedo** — Product: Landsat 8/9 Natural Color, 15m/px — Publisher: NASA/USGS — URL: https://earthexplorer.usgs.gov/ and https://landsat.gsfc.nasa.gov/ — Credit: NASA/USGS
- **Blue Marble** — Product: Blue Marble Next Generation — Publisher: NASA Visible Earth — URL: https://visibleearth.nasa.gov/collection/1484/blue-marble — Notes: base fallback
- **Night lights** — Product: NASA Black Marble / VIIRS DNB — Publisher: NASA/GSFC — URL: https://blackmarble.gsfc.nasa.gov/ — Credit: NASA
- **SRTM/ETOPO DEM** — Product: SRTM 30m + ETOPO 2022 — Publisher: NASA/USGS/NOAA — URL: https://earthexplorer.usgs.gov/ and https://www.ngdc.noaa.gov/mgg/global/ — Notes: height + horizon maps
- **Clouds** — Product: NASA MODIS cloud composite or similar — Publisher: NASA — URL: TO VERIFY

### 5. Moon
- **LRO WAC 100m** — Product: LRO WAC Global Mosaic 100m/px — Publisher: NASA/GSFC, ASU — URL: https://wms.lroc.asu.edu/lroc/ and https://pds.lroc.asu.edu/ — Credit: NASA/GSFC/Arizona State University
- **LOLA DEM** — Product: LRO LOLA DEM 118m/px — Publisher: NASA — URL: https://pds-geosciences.wustl.edu/missions/lro/lola.htm — Notes: height, normal, horizon
- **NAC hero sites 0.5m/px** — Product: LRO NAC images for Apollo sites — Publisher: NASA/ASU — URL: same WMS — Notes: hero patches, separate manifest entries, metre-scale
- **Polar coverage** — Product: LRO WAC polar mosaics — Publisher: same — URL: same

### 6. Mars
- **CTX mosaic** — Product: Murray Lab CTX Global Mosaic 6m/px — Publisher: Caltech/MSSS, NASA — URL: https://murray-lab.caltech.edu/CTX/ and https://pds-imaging.jpl.nasa.gov/volumes/mro.html — Credit: NASA/JPL/MSSS
- **HRSC** — Product: Mars Express HRSC color mosaic — Publisher: ESA/DLR — URL: https://www.dlr.de/mars/ and https://psa.esa.int/ — TO VERIFY — Notes: color
- **HiRISE hero 0.25m** — Product: HiRISE images for hero regions — Publisher: NASA/UofA — URL: https://www.uahirise.org/ and https://hirise.lpl.arizona.edu/ — Notes: hero patches
- **MOLA DEM** — Product: MGS MOLA DEM — Publisher: NASA — URL: PDS Geosciences — Notes: height

### 7. Jupiter
- **OPAL maps** — Product: Hubble OPAL Jupiter maps 0.1 deg/px — Publisher: NASA/STScI — URL: https://archive.stsci.edu/hlsp/opal/ and https://www.stsci.edu/contents/news-releases/2024/news-2024-115 — TO VERIFY exact files — Credit: NASA, ESA, STScI
- **JunoCam** — Product: JunoCam enhanced — Publisher: NASA/JPL — URL: https://www.missionjuno.swri.edu/junocam — Notes: detail below data ceiling only
- **Wind LUT** — Derived from OPAL + Cassini wind measurements — Publisher: literature — URL: TO VERIFY paper — Notes: latitude -> angular velocity

### 8. Saturn
- **Cassini ISS maps** — Product: Cassini ISS global maps — Publisher: NASA/JPL, USGS — URL: https://pds-rings.seti.org/cassini/ and https://pds-imaging.jpl.nasa.gov/volumes/cassini.html — Credit: NASA/JPL/SSI
- **PDS tau profiles** — Product: Cassini UVIS/RSS ring optical depth profiles — Publisher: NASA/PDS Rings Node — URL: https://pds-rings.seti.org/ — Notes: for M5 ringTransmission
- **Ring color** — Product: Cassini ISS ring color mosaics — Publisher: same — URL: same

### 9. Uranus
- **OPAL Uranus** — Product: Hubble OPAL Uranus maps 0.1 deg/px — Publisher: NASA/STScI — URL: https://archive.stsci.edu/hlsp/opal/ — TO VERIFY — Credit: NASA, ESA
- **Irwin et al 2024 corrected colour** — Product: corrected colour per Irwin et al 2024 MNRAS — Publisher: Oxford/ESA — URL: https://doi.org/10.1093/mnras/stad3685 — TO VERIFY — Notes: corrected vs historic appearance, labelled
- **Voyager 2 archival** — Product: Voyager 2 Uranus images 1986 — Publisher: NASA/JPL — URL: https://pds-rings.seti.org/voyager/ — Notes: historic appearance mode
- **Rings** — Product: 13 narrow rings tau — Publisher: PDS Rings — URL: same

### 10. Neptune
- **OPAL Neptune** — Product: Hubble OPAL Neptune maps — Publisher: NASA/STScI — URL: same OPAL — TO VERIFY
- **Voyager 2 1989 mosaics** — Product: Voyager 2 Neptune images 1989 — Publisher: NASA/JPL — URL: https://pds-rings.seti.org/voyager/ and https://pds-imaging.jpl.nasa.gov/volumes/voyager.html — Notes: epoch selector labelled
- **Corrected colour per Irwin et al 2024** — Same as Uranus paper — URL: same DOI — Notes: default corrected palette, historic deep-blue labelled
- **Rings + Adams arcs** — Product: Adams ring arcs optical depth — Publisher: PDS Rings — URL: same — Notes: 4 arcs

### 11. Milky Way
- **Deep Star Maps 2020** — Product: NASA SVS item 4851 Deep Star Maps 2020 — Publisher: NASA SVS — URL: https://svs.gsfc.nasa.gov/4851/ — Credit: NASA — Notes: use only layers whose provenance is not Gaia-derived per brief; if chosen file bundles Gaia, choose different layer. Record exact file used. TO VERIFY layers.
- **Gaia DR3** (bigger budget, best available) — Product: Gaia DR3 catalogue — Publisher: ESA/Gaia/DPAC — URL: https://gea.esac.esa.int/archive/ — Notes: raises star count from thousands to millions, per §27.4
- **DSS2 + gigapixel panorama** — Product: Digitized Sky Survey 2 + gigapixel all-sky — Publisher: STScI, ESO — URL: https://archive.stsci.edu/dss/ and https://www.eso.org/public/images/eso0932a/ — TO VERIFY
- **UCAC4 + BSC5 bright stars** — Product: USNO UCAC4 and Yale Bright Star Catalogue 5th edition — Publisher: USNO, Yale — URL: https://vizier.cds.unistra.fr/viz-bin/VizieR?-source=I/322A (UCAC4) and https://heasarc.gsfc.nasa.gov/W3Browse/star-catalog/bsc5p.html (BSC5) — Notes: public domain / US Government works per brief
- **IAU constellations** — Product: IAU 1930 boundaries + constellation figures from SVS item, IAU Working Group on Star Names — Publisher: IAU — URL: https://www.iau.org/public/themes/constellations/ and https://www.pas.rochester.edu/~emamajek/WGSN/ — Notes: star names

### 12. ISS
- **NASA VTAD ISS_stationary.glb** — Product: ISS_stationary.glb 44.5 MB, 247547 triangles, 28 PBR materials, 26 PNG textures — Publisher: NASA VTAD (Virtual Tilted Asset Delivery? Actually NASA Visualization Technology Applications Development) — URL: https://nasa3d.arc.nasa.gov/detail/iss-stationary and https://github.com/nasa/NASA-3D-Resources — TO VERIFY exact package version — Credit: NASA VTAD — Notes: official geometry with PBR, most accurate, record package version downloaded
- **NASA 3D Resources ISS (B)** — Product: ISS model B — Publisher: NASA/Michael D. Carbajal — URL: https://nasa3d.arc.nasa.gov/models — Credit: NASA/Michael D. Carbajal — Notes: alternative official
- Scale verification: truss span 109 m — must be verified against real dimensions, corrected if needed

### 13. Black Hole
- **EHT M87* 2019** — Product: EHT M87* 2019 release images/data — Publisher: Event Horizon Telescope Collaboration — URL: https://eventhorizontelescope.org/ and https://doi.org/10.3847/2041-8213/ab0ec7 — Notes: appearance reference, fixes shadow diameter and lensing scale, 42±3 microarcseconds for M87*
- **EHT Sgr A* 2022** — Product: EHT Sgr A* 2022 release — Publisher: EHT Collaboration — URL: https://eventhorizontelescope.org/blog/astronomers-reveal-first-image-black-hole-heart-our-galaxy and https://doi.org/10.3847/2041-8213/ac6674 — Notes: second reference
- **Bruneton 2020 precomputed-deflection method** — Product: paper "A Non-Linear Beam Tracing Method for Physically-Based Rendering of the Black Hole Shadow" etc. — Publisher: Bruneton et al. — URL: https://ebruneton.github.io/ and https://arxiv.org/abs/2003.11089 — TO VERIFY — Notes: implement yourself per A4, cite paper, do not vendor source
- **James et al 2015** — Product: "Gravitational lensing by spinning black holes in astrophysics, and in the movie Interstellar" — Publisher: James et al. — URL: https://doi.org/10.1088/0264-9381/32/6/065001 — Notes: cinematic mode documented artistic variant a/M=0.6 6500K white balance, suppressing Doppler intensity change, labelled as such, never ship film asset, never describe as "the Interstellar black hole"

## Provenance Manifest Requirements (per §16.2, Appendix A)

Each pack's manifest.json must contain:

```json
{
  "packId": "moon",
  "version": "1",
  "objectId": "moon",
  "assets": [
    {
      "product": "LRO WAC Global Mosaic 100m",
      "publisher": "NASA/GSFC/ASU",
      "url": "https://wms.lroc.asu.edu/lroc/",
      "credit": "NASA/GSFC/Arizona State University",
      "what": "albedo base 100m/px equirectangular",
      "processing": "reprojected to equirectangular, linear-space mips, ASTC 6x6 sRGB",
      "sha256": "...",
      "format": "KTX2 ASTC 6x6 sRGB",
      "resolution": "100m/px",
      "type": "albedo"
    }
  ],
  "geometry": { "type": "ellipsoid", "oblateness": 0.0012, ... },
  "dataCeiling": { "textEn": "0.5 m/px at Apollo sites", "textFa": "...", "resolutionM": 0.5 },
  "tiers": { ... }
}
```

Every field product, publisher, url, credit required per A2, else gate fails per §16.3.

## What Is NOT in Scope for Licensing

Per §2.3 A1 and §27.1: No licence names, licence URLs, SPDX tags, legal text in app, manifests, or gate. Owner handles licensing after delivery with legal counsel. Agent makes no licence decisions, maintains no licence registry, verifies no permissions, raises no licence-based blocker.

This file is traceability only, so owner can identify, trace, clear every asset later.

## Status M-1

- No assets fetched yet (read-only milestone)
- This index is planned, to be extended as assets are added in M2, M6-M10
- For M0, packs will be placeholder (bundled test texture), not real data, so manifests will have test entries with provenance of test texture (generated)
- Real packs built from M2 onwards, with real provenance

## Verification

- `tools/ci/check_provenance.py` will check every manifest in `manifests/` and `assets-built/**/*.zigpack` for required fields and plausible URL (not bare domain, not search URL, not placeholder)
- Zero assets checked is failure
- `docs/SOURCES.md` must match manifests per M12 DoD
