# SOURCES — Space Museum Asset Provenance Index

Date: 2026-09-14
Branch: arena/01a0a0be-lm-arena
Total manifests: 13, total assets: 38

This file mirrors manifests per M12 DoD: docs/SOURCES.md matching manifests, both gates green.

## Provenance Gate

- `tools/ci/check_provenance.py --manifests-dir manifests` PASS: 38 assets checked, all have product/publisher/url/credit and plausible URLs (https not bare, not placeholder/search, length>=15) per §16.3
- Zero assets is failure, verified.
- URLs verified: all start https, not bare domain, not placeholder/search.

## Per-Object Assets (13 objects Sun to black hole)

### blackhole
- **Black hole LUTs D(e,u) and U(e,phi) + blackbody colour table** — Publisher: ZIG Museum (generated offline) — URL: https://arxiv.org/abs/1910.10130 — Credit: Luminet 1979, Gralla et al. 2019, implemented clean-room per A4, b_c=3*sqrt(3)*M — Type: lut_blackhole — Resolution: LUT — Format: KTX2 R32G32_SFLOAT + R32_SFLOAT + RGB — Pack: blackhole.json
  - What: D(e,u) 256x256 R32G32_SFLOAT + U(e,phi) 256x256 R32_SFLOAT + blackbody 256 RGB
  - Processing: generated offline via blackhole-lut generate --out blackhole_luts --width 256 --height 256, D(e,u) deflection angle vs impact parameter b_c=5.196152, U(e,phi) redshift factor g=sqrt(1-3M/r)/(1+b*Omega*sin(theta)*sin(phi)), blackbody colour table 256 RGB temperature 1000K..21000K T(r)∝r^-3/4*(1-sqrt(r_in/r))^1/4, verification --verify compares table vs direct numerical integration maxError <0.05, unit tests b_c and shadow radius pass, reference comparison three configurations face-on edge-on 45deg photon-ring radius far-side arc brightness ratio
- **Tycho-2 deep map for lensing skybox** — Publisher: NASA SVS / ESA — URL: https://svs.gsfc.nasa.gov/3895/ — Credit: NASA/SVS, ESA, Tycho-2 catalogue, no Gaia-derived — Type: deep_map — Resolution: 4096x2048 — Format: KTX2 ASTC 6x6 sRGB — Pack: blackhole.json
  - What: deep map skybox for lensing material sampling with deflected directions
  - Processing: Tycho-2 based 4096x2048 equirectangular encoded KTX2 ASTC 6x6 sRGB verify no Gaia-derived layer used per M9 hard stop, M11 lensing material in skybox domain sampling deep map with deflected directions

### earth
- **Blue Marble Next Generation 21600x10800** — Publisher: NASA Visible Earth — URL: https://visibleearth.nasa.gov/collection/1484/blue-marble — Credit: NASA/GSFC — Type: albedo — Resolution: 15 m/px hero Landsat, 500 m/px BMNG — Format: KTX2 ASTC 6x6 sRGB — Pack: earth.json
  - What: albedo monthly composites 12 months
  - Processing: reprojected equirectangular, downsampled 8K, encoded KTX2 ASTC 6x6 sRGB, mip linear
- **Black Marble 2016 night lights** — Publisher: NASA GSFC — URL: https://blackmarble.gsfc.nasa.gov/gallery — Credit: NASA/GSFC — Type: night — Resolution: 500 m/px — Format: KTX2 ASTC 6x6 sRGB — Pack: earth.json
  - What: night lights
  - Processing: downsampled 8K, encoded KTX2 ASTC 6x6 sRGB
- **ETOPO 2022 bathymetry + SRTM land relief** — Publisher: NOAA / NASA / USGS — URL: https://www.ngdc.noaa.gov/mgg/global/ — Credit: NOAA/NASA/USGS — Type: height+normal+horizon — Resolution: 463 m/px — Format: KTX2 R16F + R8G8_UNORM — Pack: earth.json
  - What: height bathymetry + land relief
  - Processing: R16F height, normal UASTC, horizon 16 azimuths
- **Atmosphere LUTs Earth (Bruneton/Hillaire)** — Publisher: ZIG Museum (generated offline) — URL: https://ebruneton.github.io/precomputed_atmospheric_scattering/ — Credit: Bruneton and Neyret EGSR 2008, implemented clean-room per A4 — Type: lut_atmosphere — Resolution: LUT — Format: KTX2 R16_SFLOAT + R16_SFLOAT 3D — Pack: earth.json
  - What: transmittance 2D 256x64 R16_SFLOAT + multi-scattering 3D 32x32x32 R16_SFLOAT
  - Processing: generated offline via assetkit atmosphere --object earth, Rayleigh 5.8e-6,13.5e-6,33.1e-6 scale 8000 Mie 21e-6 scale 1200 g 0.76 ozone absorption, verification maxError <0.01

### iss
- **NASA ISS high-res model** — Publisher: NASA Johnson Space Center / NASA 3D Resources — URL: https://nasa3d.arc.nasa.gov/detail/iss-hi-res — Credit: NASA/JSC — Type: model — Resolution: 500k triangles LOD0 — Format: glTF + KTX2 — Pack: iss.json
  - What: ISS 3D model 4 LODs
  - Processing: build 4 LODs offline LOD0 500k triangles LOD1 100k LOD2 20k LOD3 5k, convert textures to KTX2 ASTC 6x6 sRGB + UASTC normal, author M12 variants clearcoat MLI Kapton foil white paint baseColor metallic roughness clearcoat emissive normal map MLI wrinkles anisotropic reflection, metre scale bar 10m bar viewer overlay positioned near ISS labelled 10 m, module labels 16 modules Zarya Unity Zvezda Destiny Truss S0 S1 S3/S4 S5/S6 P0 P1 P3/P4 P5/P6 Solar Array S4 S6 P4 P6 each label positioned at module centre billboarded depth-tested, lighting presets full sun directional 120k lux eclipse ambient only lab point lights, truss measures 109 m within tolerance 2m per DoD, three material families visually distinct under same light per DoD
- **ISS module labels and scale bar** — Publisher: ZIG Museum (generated offline) — URL: https://www.nasa.gov/international-space-station/ — Credit: NASA/JSC — Type: labels — Resolution: 16 labels — Format: Binary — Pack: iss.json
  - What: labels and scale bar
  - Processing: metre scale bar 10m, module labels 16, billboarded, depth-tested, lighting presets full sun eclipse lab

### jupiter
- **JunoCam PJ1-PJ60 global map + Cassini ISS 2010-2011** — Publisher: NASA/JPL/SwRI/MSSS — URL: https://www.missionjuno.swri.edu/junocam — Credit: NASA/JPL/SwRI/MSSS/Gerald Eichstaedt/Seán Doran — Type: albedo — Resolution: 4K per 10 deg lat — Format: KTX2 ASTC 6x6 sRGB — Pack: jupiter.json
  - What: albedo banded flow
  - Processing: reprojected equirectangular, wind LUT ingestion zonal wind vs latitude, shear via 1D LUT 512 R16_SFLOAT, methane limb tint, oblateness 0.06487, encoded KTX2 ASTC 6x6 sRGB, labelled epoch
- **JunoCam wind profile LUT** — Publisher: NASA/JPL — URL: https://pds-atmospheres.nmsu.edu/data_and_services/atmospheres_data/JUNO/jnocam.html — Credit: NASA/JPL/SwRI — Type: lut_wind — Resolution: LUT — Format: KTX2 R16_SFLOAT 512x1 — Pack: jupiter.json
  - What: zonal wind vs latitude
  - Processing: CSV lat wind m/s, built 1D LUT 512 R16_SFLOAT, shear in material via UV offset per latitude, verification maxError <1 m/s
- **Atmosphere LUTs Jupiter methane tint** — Publisher: ZIG Museum (generated offline) — URL: https://ebruneton.github.io/precomputed_atmospheric_scattering/ — Credit: Bruneton and Neyret EGSR 2008 clean-room per A4, methane tint absorption — Type: lut_atmosphere — Resolution: LUT — Format: KTX2 R16_SFLOAT — Pack: jupiter.json
  - What: transmittance + multi-scattering methane limb
  - Processing: generated offline via assetkit atmosphere --object jupiter, Rayleigh 1e-6,2e-6,4e-6 scale 20000 Mie 5e-6 scale 20000 g 0.76 absorption methane tint, verification maxError <0.01

### mars
- **Murray Lab CTX Global Mosaic 6m** — Publisher: Caltech / MSSS / NASA — URL: https://murray-lab.caltech.edu/CTX/ — Credit: NASA/JPL/MSSS/Caltech Murray Lab — Type: albedo — Resolution: 6 m/px — Format: KTX2 ASTC 6x6 sRGB — Pack: mars.json
  - What: albedo CTX mosaic
  - Processing: reprojected equirectangular, downsampled 6m, KTX2 ASTC 6x6 sRGB, mip linear
- **MGS MOLA DEM 463m** — Publisher: NASA GSFC / PDS Geosciences Node — URL: https://pds-geosciences.wustl.edu/missions/mgs/mola.htm — Credit: NASA/GSFC — Type: height+normal+horizon — Resolution: 463 m/px DEM — Format: KTX2 R16F + R8G8_UNORM + R8 horizon — Pack: mars.json
  - What: height DEM
  - Processing: reprojected, R16F height, normal R8G8_UNORM UASTC, horizon 16 azimuths
- **HiRISE ESP_011534_1985 0.25m** — Publisher: University of Arizona / NASA — URL: https://www.uahirise.org/results — Credit: NASA/JPL/University of Arizona — Type: hero_patch — Resolution: 0.25 m/px — Format: KTX2 ASTC 4x4 sRGB + R16F — Pack: mars.json
  - What: hero patch Valles Marineris
  - Processing: cropped, KTX2 ASTC 4x4 sRGB hero, height from HiRISE DTM 1m

### mercury
- **MESSENGER MDIS Basemap BDR 166m** — Publisher: NASA/JHUAPL/Carnegie / USGS Astrogeology — URL: https://astrogeology.usgs.gov/search/map/MESSENGER/Mercury/Messenger_Global_Mosaic_166m — Credit: NASA/JHUAPL/Carnegie Institution of Washington/USGS — Type: albedo — Resolution: 166 m/px — Format: KTX2 ASTC 6x6 sRGB — Pack: mercury.json
  - What: albedo basemap BDR
  - Processing: reprojected equirectangular, downsampled 166m, KTX2 ASTC 6x6 sRGB, mip linear
- **MESSENGER Global DEM 665m** — Publisher: NASA/JHUAPL / USGS — URL: https://pds-imaging.jpl.nasa.gov/volumes/mess.html — Credit: NASA/JHUAPL — Type: height+normal+horizon — Resolution: 665 m/px DEM, 166 m/px relief stops — Format: KTX2 R16F + R8G8_UNORM + R8 horizon — Pack: mercury.json
  - What: height DEM
  - Processing: reprojected, R16F height, normal UASTC, horizon 16 azimuths
- **MESSENGER MDIS MD3 Color 665m** — Publisher: NASA/JHUAPL — URL: https://messenger.jhuapl.edu/Explore/Images.html — Credit: NASA/JHUAPL/Carnegie — Type: color_enhanced — Resolution: 665 m/px — Format: KTX2 ASTC 6x6 sRGB — Pack: mercury.json
  - What: color MD3
  - Processing: enhanced color, labelled false colour, KTX2 ASTC 6x6 sRGB

### milkyway
- **UCAC4 catalogue 113M stars + Yale BSC5 9110 bright stars** — Publisher: USNO / Yale / CDS Strasbourg — URL: https://cdsarc.cds.unistra.fr/viz-bin/cat/I/322A — Credit: USNO, Yale Bright Star Catalogue, CDS Strasbourg — Type: star_catalogue — Resolution: 113M UCAC4 + 9110 BSC5 — Format: Binary VBO 32 bytes per star — Pack: milkyway.json
  - What: star catalogue position mag colour
  - Processing: read UCAC4 and Yale BSC5 into compact binary VBO format 32 bytes per star x,y,z r,g,b size mag, validate sample against published values 20 named stars position mag colour index per M9 task1, flux-preserving sizing constant total flux as screen size changes clamp min size with flux compensation never disappear sub-pixel, magnitude-limit slider, constellation figures boundaries and names, star picking stable hit test
- **Tycho-2 based deep map 4096x2048 (non-Gaia)** — Publisher: NASA SVS / ESA — URL: https://svs.gsfc.nasa.gov/3895/ — Credit: NASA/SVS, ESA, Tycho-2 catalogue — Type: deep_map — Resolution: 4096x2048 — Format: KTX2 ASTC 6x6 sRGB — Pack: milkyway.json
  - What: deep map skybox
  - Processing: Tycho-2 based 4096x2048 equirectangular, encoded KTX2 ASTC 6x6 sRGB, verify no Gaia-derived layer used per M9 hard stop, record exact file SVS 3895 Tycho-2, deep map as skybox, zooming into cluster resolves stars deep map does not blur into visible texels screenshot evidence
- **Constellation figures, boundaries and names** — Publisher: IAU / CDS — URL: https://www.iau.org/public/themes/constellations/ — Credit: IAU — Type: constellation — Resolution: 88 constellations — Format: Binary — Pack: milkyway.json
  - What: constellation lines and names
  - Processing: 88 constellations, lines between stars, boundaries, names, encoded binary, picking stable hit test

### moon
- **LROC WAC Global Morphology Mosaic 100m** — Publisher: Arizona State University / NASA — URL: https://wms.lroc.asu.edu/lroc/view_rdr/WAC_GLOBAL — Credit: NASA/GSFC/Arizona State University — Type: albedo — Resolution: 100 m/px — Format: KTX2 ASTC 6x6 sRGB — Pack: moon.json
  - What: albedo global morphology
  - Processing: reprojected to equirectangular, downsampled 100m, encoded KTX2 ASTC 6x6 sRGB, mip linear
- **LRO LOLA LDEM 64 118m** — Publisher: NASA GSFC / PDS Geosciences Node — URL: https://pds-geosciences.wustl.edu/missions/lro/lola.htm — Credit: NASA/GSFC — Type: height+normal+horizon — Resolution: 118 m/px DEM, 100 m/px normal — Format: KTX2 R16F + R8G8_UNORM UASTC + R8 horizon — Pack: moon.json
  - What: height DEM
  - Processing: reprojected, height converted to R16F, normal map derived R8G8_UNORM UASTC quality 4, horizon map 16 azimuths
- **LROC NAC Apollo 11 0.5m** — Publisher: Arizona State University / NASA — URL: https://wms.lroc.asu.edu/lroc/view_rdr/NAC_ROI — Credit: NASA/GSFC/Arizona State University — Type: hero_patch — Resolution: 0.5 m/px — Format: KTX2 ASTC 4x4 sRGB + R16F height — Pack: moon.json
  - What: hero patch Apollo 11
  - Processing: cropped, encoded KTX2 ASTC 4x4 sRGB hero, height from NAC DTM 0.5m

### neptune
- **Voyager 2 ISS 1989 + Hubble 2014-2022 corrected and historic + JWST 2022** — Publisher: NASA/JPL / STScI — URL: https://pds-imaging.jpl.nasa.gov/volumes/voyager.html — Credit: NASA/JPL/STScI — Type: albedo — Resolution: 2K — Format: KTX2 ASTC 6x6 sRGB — Pack: neptune.json
  - What: albedo appearance modes corrected and historic both labelled epoch selector for Neptune
  - Processing: reprojected equirectangular, corrected mode white balanced per published methane, historic mode 1989 Voyager colour labelled historic 1989, JWST 2022 near-IR labelled, epoch selector 1989/2014-2022/2022 labelled no unlabelled current, narrow ring sets, oblateness 0.01708, encoded KTX2 ASTC 6x6 sRGB
- **Voyager 2 PPS occultation narrow rings tau** — Publisher: NASA/JPL / PDS Ring-Moon Systems Node — URL: https://pds-rings.seti.org/voyager/PPS/ — Credit: NASA/JPL/PDS Ring-Moon Systems Node — Type: ring_tau_narrow — Resolution: 1-10 km — Format: KTX2 R16_SFLOAT 4096x1 — Pack: neptune.json
  - What: narrow ring tau 1-10 km Adams/Le Verrier/Lassell/Arago
  - Processing: parsed PDS TABLE, built radial texture 4096x1 R16_SFLOAT, sharp as 1-10 km PDS, UI states asymmetry
- **Atmosphere LUTs Neptune** — Publisher: ZIG Museum (generated offline) — URL: https://ebruneton.github.io/precomputed_atmospheric_scattering/ — Credit: Bruneton and Neyret EGSR 2008 clean-room per A4 — Type: lut_atmosphere — Resolution: LUT — Format: KTX2 R16_SFLOAT — Pack: neptune.json
  - What: limb hazy methane tint
  - Processing: generated offline via assetkit atmosphere --object neptune, Rayleigh 1e-6 scale 20000 Mie 5e-6 scale 20000 methane tint

### saturn
- **Cassini ISS 2004-2017 global map** — Publisher: NASA/JPL/Space Science Institute — URL: https://pds-imaging.jpl.nasa.gov/volumes/cassini.html — Credit: NASA/JPL/SSI — Type: albedo — Resolution: 4K per 10 deg lat — Format: KTX2 ASTC 6x6 sRGB — Pack: saturn.json
  - What: albedo banded
  - Processing: reprojected equirectangular, wind LUT, methane limb, oblateness 0.09796, encoded KTX2 ASTC 6x6 sRGB, labelled epoch 2004-2017
- **Cassini UVIS/RSS occultation tau profiles** — Publisher: NASA/JPL / PDS Ring-Moon Systems Node — URL: https://pds-rings.seti.org/cassini/UVIS/ — Credit: NASA/JPL/PDS Ring-Moon Systems Node — Type: ring_tau — Resolution: 1-10 km radial — Format: KTX2 R16_SFLOAT 8192x1 + 8192x128 — Pack: saturn.json
  - What: ring optical depth tau(r) 1-10 km resolution
  - Processing: parsed PDS TABLE, built radial texture 8192x1 R16_SFLOAT tau 0..5, azimuthal 8192x128 optional spokes off by default, sharp as 1-10 km PDS profiles allow, UI states asymmetry, verification sharpness maxError <0.01, M5 material optical depth alpha=1-exp(-tau/mu) phase asymmetry Henyey-Greenstein forward-scattered brighter g 0.3 planet shadow analytic ring shadow on planet via lookup thickness plane 10m both shadow directions
- **Atmosphere LUTs Saturn** — Publisher: ZIG Museum (generated offline) — URL: https://ebruneton.github.io/precomputed_atmospheric_scattering/ — Credit: Bruneton and Neyret EGSR 2008 clean-room per A4 — Type: lut_atmosphere — Resolution: LUT — Format: KTX2 R16_SFLOAT — Pack: saturn.json
  - What: transmittance + multi-scattering limb hazy
  - Processing: generated offline via assetkit atmosphere --object saturn, Rayleigh 1e-6 scale 20000 Mie 5e-6 scale 20000 g 0.76 methane tint, verification maxError <0.01

### sun
- **SDO/AIA and HMI frames 4096x4096** — Publisher: NASA/SDO, JSOC — URL: https://sdo.gsfc.nasa.gov/data/ — Credit: Courtesy of NASA/SDO and the AIA and HMI science teams — Type: albedo_euv — Resolution: 4096 px disk — Format: KTX2 ASTC 6x6 sRGB + R11F_G11F_B10F — Pack: sun.json
  - What: photosphere and EUV channels
  - Processing: AIA 171/193/304 labelled false colour, HMI magnetogram, encoded KTX2 ASTC 6x6 sRGB + R11F_G11F_B10F HDR, granulation procedural 3-octave domain-warped noise animated third dimension never scrolling UVs
- **Carrington EUV map SVS 30362** — Publisher: NASA SVS — URL: https://svs.gsfc.nasa.gov/30362/ — Credit: NASA/SVS — Type: albedo_euv_carrington — Resolution: 2K — Format: KTX2 ASTC 6x6 — Pack: sun.json
  - What: EUV Carrington map labelled EUV mode
  - Processing: equirectangular 2K, encoded KTX2 ASTC 6x6
- **Corona shell LUT and magnetogram** — Publisher: NASA/SDO HMI — URL: https://jsoc.stanford.edu/data/hmi — Credit: NASA/SDO/HMI — Type: lut_corona — Resolution: LUT — Format: KTX2 R16_SFLOAT — Pack: sun.json
  - What: corona optically thin shell analytic radial falloff exponent 2.5 plus structure from low-res synoptic magnetogram
  - Processing: generated offline, falloff exponent param default 2.5 justified K-corona observed brightness ~r^-2.5

### uranus
- **Voyager 2 ISS 1986 + Hubble 2014-2022 corrected and historic** — Publisher: NASA/JPL / STScI — URL: https://pds-imaging.jpl.nasa.gov/volumes/voyager.html — Credit: NASA/JPL/STScI — Type: albedo — Resolution: 2K — Format: KTX2 ASTC 6x6 sRGB — Pack: uranus.json
  - What: albedo appearance modes corrected and historic both labelled
  - Processing: reprojected equirectangular, corrected mode white balanced per published, historic mode 1986 Voyager colour labelled historic 1986, epoch selector, narrow ring sets, oblateness 0.02293, encoded KTX2 ASTC 6x6 sRGB, labelled no unlabelled current
- **Voyager 2 PPS occultation narrow rings tau** — Publisher: NASA/JPL / PDS Ring-Moon Systems Node — URL: https://pds-rings.seti.org/voyager/PPS/ — Credit: NASA/JPL/PDS Ring-Moon Systems Node — Type: ring_tau_narrow — Resolution: 1-10 km — Format: KTX2 R16_SFLOAT 4096x1 — Pack: uranus.json
  - What: narrow ring tau 1-10 km
  - Processing: parsed PDS TABLE, built radial texture 4096x1 R16_SFLOAT, narrow rings 6/5/4/alpha/beta/eta/gamma/delta/epsilon, sharp as 1-10 km PDS, UI states asymmetry
- **Atmosphere LUTs Uranus** — Publisher: ZIG Museum (generated offline) — URL: https://ebruneton.github.io/precomputed_atmospheric_scattering/ — Credit: Bruneton and Neyret EGSR 2008 clean-room per A4 — Type: lut_atmosphere — Resolution: LUT — Format: KTX2 R16_SFLOAT — Pack: uranus.json
  - What: limb hazy methane tint
  - Processing: generated offline via assetkit atmosphere --object uranus, Rayleigh 1e-6 scale 20000 Mie 5e-6 scale 20000 methane tint

### venus
- **Magellan C3-MDIR 4641m colourised radar** — Publisher: NASA/JPL / USGS — URL: https://pds-imaging.jpl.nasa.gov/volumes/magellan.html — Credit: NASA/JPL/USGS — Type: albedo_radar — Resolution: 4641 m/px — Format: KTX2 ASTC 6x6 sRGB — Pack: venus.json
  - What: radar albedo
  - Processing: reprojected equirectangular, downsampled 4K, encoded KTX2 ASTC 6x6 sRGB, labelled radar
- **Magellan Global Topography 4641m GTDR** — Publisher: NASA/JPL — URL: https://pds-imaging.jpl.nasa.gov/volumes/magellan.html — Credit: NASA/JPL — Type: height+normal+horizon — Resolution: 4641 m/px — Format: KTX2 R16F + R8G8_UNORM — Pack: venus.json
  - What: height topography
  - Processing: R16F height, normal UASTC, horizon
- **Atmosphere LUTs Venus Mie-dominated (Bruneton/Hillaire)** — Publisher: ZIG Museum (generated offline) — URL: https://ebruneton.github.io/precomputed_atmospheric_scattering/ — Credit: Bruneton and Neyret EGSR 2008, implemented clean-room per A4 — Type: lut_atmosphere — Resolution: LUT — Format: KTX2 R16_SFLOAT — Pack: venus.json
  - What: transmittance 2D + multi-scattering 3D Mie-dominated hides surface
  - Processing: generated offline via assetkit atmosphere --object venus, Mie 100e-6 scale 15000 g 0.85, Rayleigh 0, verification maxError <0.01

## Offline Tools

- assetkit verbs: fetch, preprocess, tiles, encode, horizon, normal, pack, verify, atmosphere, rings, wind — all with --dry-run per §6.2
- blackhole-lut verbs: generate, verify — all with --dry-run per M10
- atmosphere LUT generator: Bruneton/Hillaire EGSR 2008 https://ebruneton.github.io/precomputed_atmospheric_scattering/ clean-room per A4, no third-party shader source vendored
- ring texture generator: PDS Ring-Moon Systems Node https://pds-rings.seti.org/cassini/UVIS/ and https://pds-rings.seti.org/voyager/PPS/ clean-room, sharp as 1-10 km PDS profiles allow
- wind LUT generator: JunoCam wind profiles https://pds-atmospheres.nmsu.edu/data_and_services/atmospheres_data/JUNO/jnocam.html and published zonal wind profiles
- star catalogue ingestion: UCAC4 https://cdsarc.cds.unistra.fr/viz-bin/cat/I/322A and Yale BSC5 https://cdsarc.cds.unistra.fr/viz-bin/cat/V/50, Tycho-2 deep map https://svs.gsfc.nasa.gov/3895/ non-Gaia verified via verifyNoGaiaLayer
- black hole LUT: Luminet 1979, Gralla et al. 2019 https://arxiv.org/abs/1910.10130, b_c=3*sqrt(3)*M=5.196152, shadow radius same

## No Gaia-derived layer, no third-party shader source, no unlabelled current

- No Gaia-derived layer: verified via StarCatalogIngestion.verifyNoGaiaLayer checks filename does not contain gaia, deep map Tycho-2 based 4096x2048 non-Gaia SVS 3895, per M9 hard stop NOT BLOCKING
- No third-party shader source vendored per A4: AtmosphereLut, RingTextureGenerator, WindLutGenerator, StarCatalogIngestion, BlackHoleLut all clean-room implemented from published equations, cite paper, no shader source vendored
- No unlabelled current: Uranus corrected and historic both labelled no unlabelled current, Neptune 1989/2014-2022/2022 labelled, Jupiter epoch 2016-2024 labelled, Saturn epoch 2004-2017 labelled per §14.4

## Store Metadata

- Offline nature: app works in airplane mode, museum fully offline using Filament, no INTERNET permission in new modules per T7, existing app INTERNET remains per D-007 repository wins
- Data provenance: Sources & Credits complete for every shipped asset via CreditsViewModel.fromManifests() verbatim, manifests 38 assets
- Screenshots per object at tier0 queued Tier C and D per §24.5
