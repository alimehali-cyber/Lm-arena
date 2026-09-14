package com.zig.museum.core.model

/**
 * ObjectRegistry — the spine of Space Museum.
 * Thirteen objects, one at a time.
 * All numeric values cited from IAU WGCCRE 2018 (Archinal et al.) and NASA Planetary Fact Sheets.
 * https://nssdc.gsfc.nasa.gov/planetary/factsheet/
 * https://astropedia.astrogeology.usgs.gov/alfresco/d/d/workspace/SpacesStore/28fd9e81-1964-44d6-a58b-fbbf61e64e0e/WGCCRE2015reprint.pdf
 */
object ObjectRegistry {

    // Sun: mean radius 696340 km = 696340000 m (NASA Sun fact sheet)
    // Oblateness ~9e-06 (Sun fact sheet, very small)
    // Axial tilt 7.25 deg to ecliptic (IAU WGCCRE)
    // Rotation period ~609.12 hours (25.38 days sidereal at equator, NASA Sun fact sheet)
    private val SUN = ObjectSpec(
        id = "sun",
        displayNameEn = "Sun",
        displayNameFa = "خورشید",
        sceneRadiusMetres = 696340000.0, // NASA Sun fact sheet mean radius
        oblateness = 0.000009, // NASA Sun fact sheet
        axialTiltDeg = 7.25, // IAU WGCCRE 2015
        rotationPeriodHours = 609.12, // NASA Sun fact sheet sidereal 25.38 days
        packId = "sun",
        dataCeilingTextEn = "4K AIA, 4096px solar disk",
        dataCeilingTextFa = "۴K، قرص خورشید ۴۰۹۶ پیکسل"
    )

    // Mercury: mean radius 2439.7 km (NASA Mercury fact sheet)
    // Oblateness ~0 (near spherical, NASA)
    // Tilt 0.034 deg (IAU WGCCRE)
    // Rotation 1407.6 hours (58.646 days, NASA Mercury fact sheet)
    private val MERCURY = ObjectSpec(
        id = "mercury",
        displayNameEn = "Mercury",
        displayNameFa = "عطارد",
        sceneRadiusMetres = 2439700.0, // NASA Mercury fact sheet
        oblateness = 0.0, // NASA Mercury fact sheet ~0
        axialTiltDeg = 0.034, // IAU WGCCRE
        rotationPeriodHours = 1407.6, // NASA Mercury fact sheet
        packId = "mercury",
        dataCeilingTextEn = "166 m/px MESSENGER MDIS BDR",
        dataCeilingTextFa = "۱۶۶ متر بر پیکسل، مسنجر"
    )

    // Venus: mean radius 6051.8 km (NASA Venus fact sheet)
    // Oblateness 0
    // Tilt 177.36 deg (retrograde, IAU WGCCRE) — using 177.36
    // Rotation -5832.5 hours (243.025 days retrograde, negative per spec for retrograde, NASA Venus fact sheet)
    private val VENUS = ObjectSpec(
        id = "venus",
        displayNameEn = "Venus",
        displayNameFa = "زهره",
        sceneRadiusMetres = 6051800.0, // NASA Venus fact sheet
        oblateness = 0.0, // NASA Venus fact sheet
        axialTiltDeg = 177.36, // IAU WGCCRE retrograde
        rotationPeriodHours = -5832.5, // NASA Venus fact sheet retrograde
        packId = "venus",
        dataCeilingTextEn = "75 m/px Magellan SAR FMAP",
        dataCeilingTextFa = "۷۵ متر بر پیکسل، ماژلان"
    )

    // Earth: mean radius 6371.0 km (NASA Earth fact sheet, volumetric mean)
    // Oblateness 0.0033528 (WGS84, IAU WGCCRE)
    // Tilt 23.44 deg (IAU WGCCRE)
    // Rotation 23.934 hours sidereal (NASA Earth fact sheet)
    private val EARTH = ObjectSpec(
        id = "earth",
        displayNameEn = "Earth",
        displayNameFa = "زمین",
        sceneRadiusMetres = 6371000.0, // NASA Earth fact sheet volumetric mean
        oblateness = 0.0033528, // WGS84 / IAU WGCCRE
        axialTiltDeg = 23.44, // IAU WGCCRE
        rotationPeriodHours = 23.934, // NASA Earth fact sheet sidereal
        packId = "earth",
        dataCeilingTextEn = "15 m/px Landsat + night lights",
        dataCeilingTextFa = "۱۵ متر بر پیکسل، لندست"
    )

    // Moon: mean radius 1737.4 km (NASA Moon fact sheet)
    // Oblateness 0.0012 (NASA)
    // Tilt 6.68 deg to orbit, 1.54 to ecliptic (IAU WGCCRE) — using 6.68
    // Rotation 655.72 hours (27.321661 days synchronous, NASA Moon fact sheet)
    private val MOON = ObjectSpec(
        id = "moon",
        displayNameEn = "Moon",
        displayNameFa = "ماه",
        sceneRadiusMetres = 1737400.0, // NASA Moon fact sheet
        oblateness = 0.0012, // NASA Moon fact sheet
        axialTiltDeg = 6.68, // IAU WGCCRE to orbit
        rotationPeriodHours = 655.72, // NASA Moon fact sheet synchronous
        packId = "moon",
        dataCeilingTextEn = "0.5 m/px at Apollo sites (NAC)",
        dataCeilingTextFa = "۰٫۵ متر بر پیکسل در سایت‌های آپولو"
    )

    // Mars: mean radius 3389.5 km (NASA Mars fact sheet)
    // Oblateness 0.00648 (IAU WGCCRE)
    // Tilt 25.19 deg (IAU WGCCRE)
    // Rotation 24.6229 hours (NASA Mars fact sheet)
    private val MARS = ObjectSpec(
        id = "mars",
        displayNameEn = "Mars",
        displayNameFa = "مریخ",
        sceneRadiusMetres = 3389500.0, // NASA Mars fact sheet
        oblateness = 0.00648, // IAU WGCCRE
        axialTiltDeg = 25.19, // IAU WGCCRE
        rotationPeriodHours = 24.6229, // NASA Mars fact sheet
        packId = "mars",
        dataCeilingTextEn = "6 m/px CTX mosaic, 0.25 m/px HiRISE hero",
        dataCeilingTextFa = "۶ متر بر پیکسل CTX، ۰٫۲۵ متر HiRISE"
    )

    // Jupiter: mean radius 69911 km (NASA Jupiter fact sheet volumetric mean)
    // Oblateness 0.06487 (IAU WGCCRE)
    // Tilt 3.13 deg (IAU WGCCRE)
    // Rotation 9.925 hours (NASA Jupiter fact sheet)
    private val JUPITER = ObjectSpec(
        id = "jupiter",
        displayNameEn = "Jupiter",
        displayNameFa = "مشتری",
        sceneRadiusMetres = 69911000.0, // NASA Jupiter fact sheet
        oblateness = 0.06487, // IAU WGCCRE
        axialTiltDeg = 3.13, // IAU WGCCRE
        rotationPeriodHours = 9.925, // NASA Jupiter fact sheet
        packId = "jupiter",
        dataCeilingTextEn = "0.1°/px Hubble OPAL",
        dataCeilingTextFa = "۰٫۱ درجه بر پیکسل، هابل OPAL"
    )

    // Saturn: mean radius 58232 km (NASA Saturn fact sheet)
    // Oblateness 0.09796 (IAU WGCCRE)
    // Tilt 26.73 deg (IAU WGCCRE)
    // Rotation 10.656 hours (NASA Saturn fact sheet, internal)
    private val SATURN = ObjectSpec(
        id = "saturn",
        displayNameEn = "Saturn",
        displayNameFa = "زحل",
        sceneRadiusMetres = 58232000.0, // NASA Saturn fact sheet
        oblateness = 0.09796, // IAU WGCCRE
        axialTiltDeg = 26.73, // IAU WGCCRE
        rotationPeriodHours = 10.656, // NASA Saturn fact sheet
        packId = "saturn",
        dataCeilingTextEn = "Cassini ISS maps + ring tau",
        dataCeilingTextFa = "نقشه‌های کاسینی + حلقه‌ها"
    )

    // Uranus: mean radius 25362 km (NASA Uranus fact sheet)
    // Oblateness 0.02293 (IAU WGCCRE)
    // Tilt 97.77 deg (IAU WGCCRE)
    // Rotation -17.24 hours retrograde (NASA Uranus fact sheet)
    private val URANUS = ObjectSpec(
        id = "uranus",
        displayNameEn = "Uranus",
        displayNameFa = "اورانوس",
        sceneRadiusMetres = 25362000.0, // NASA Uranus fact sheet
        oblateness = 0.02293, // IAU WGCCRE
        axialTiltDeg = 97.77, // IAU WGCCRE
        rotationPeriodHours = -17.24, // NASA Uranus fact sheet retrograde
        packId = "uranus",
        dataCeilingTextEn = "0.1°/px OPAL, 13 narrow rings",
        dataCeilingTextFa = "۰٫۱ درجه OPAL، ۱۳ حلقه باریک"
    )

    // Neptune: mean radius 24622 km (NASA Neptune fact sheet)
    // Oblateness 0.01708 (IAU WGCCRE)
    // Tilt 28.32 deg (IAU WGCCRE)
    // Rotation 16.11 hours (NASA Neptune fact sheet)
    private val NEPTUNE = ObjectSpec(
        id = "neptune",
        displayNameEn = "Neptune",
        displayNameFa = "نپتون",
        sceneRadiusMetres = 24622000.0, // NASA Neptune fact sheet
        oblateness = 0.01708, // IAU WGCCRE
        axialTiltDeg = 28.32, // IAU WGCCRE
        rotationPeriodHours = 16.11, // NASA Neptune fact sheet
        packId = "neptune",
        dataCeilingTextEn = "0.1°/px OPAL, Adams arcs",
        dataCeilingTextFa = "۰٫۱ درجه OPAL، کمان‌های آدامز"
    )

    // Milky Way: inside-out sphere, user inside. Scene radius for normalization still 1.0,
    // but real radius ~ 5e20 m (50000 ly = 4.73e20 m, NASA). Using 5e20.
    // Oblateness 0, tilt 0 (galactic), rotation 0 (not applicable, use 0)
    // Source: NASA Milky Way size fact sheet
    private val MILKY_WAY = ObjectSpec(
        id = "milky_way",
        displayNameEn = "Milky Way",
        displayNameFa = "کهکشان راه شیری",
        sceneRadiusMetres = 5.0e20, // NASA approx 50000 ly radius = 4.73e20 m
        oblateness = 0.0,
        axialTiltDeg = 0.0,
        rotationPeriodHours = 0.0,
        packId = "milky_way",
        dataCeilingTextEn = "Gigapixel all-sky + Gaia DR3",
        dataCeilingTextFa = "آسمان گیگاپیکسلی + گایا"
    )

    // ISS: truss span 109 m (NASA VTAD), so half-span ~54.5 m as scene radius for camera normalization
    // Oblateness 0, tilt 51.6 deg orbit inclination (use as axial tilt for display), rotation 0 (not rotating, but keep 0)
    // Source: NASA ISS fact sheet, NASA VTAD model
    private val ISS = ObjectSpec(
        id = "iss",
        displayNameEn = "ISS",
        displayNameFa = "ایستگاه فضایی",
        sceneRadiusMetres = 54.5, // NASA ISS fact sheet truss span 109 m /2
        oblateness = 0.0,
        axialTiltDeg = 51.6, // ISS orbit inclination, NASA ISS fact sheet
        rotationPeriodHours = 0.0, // not a rotating body, static model
        packId = "iss",
        dataCeilingTextEn = "109 m truss, NASA VTAD glTF",
        dataCeilingTextFa = "خرپای ۱۰۹ متری، مدل ناسا"
    )

    // Black Hole: M87* mass 6.5e9 solar masses, Schwarzschild radius ~1.9e13 m (EHT 2019)
    // Using M87* as reference for scene radius, per EHT M87* shadow 42±3 µas
    // Oblateness 0 (Schwarzschild), tilt variable (inclination), rotation 0 (spin handled in material)
    // Source: EHT Collaboration 2019 ApJ 875 L1, EHT 2022 Sgr A* release
    private val BLACK_HOLE = ObjectSpec(
        id = "black_hole",
        displayNameEn = "Black Hole",
        displayNameFa = "سیاه‌چاله",
        sceneRadiusMetres = 1.9e13, // EHT M87* Schwarzschild radius ~1.9e13 m
        oblateness = 0.0,
        axialTiltDeg = 0.0, // inclination controlled in viewer, not fixed
        rotationPeriodHours = 0.0, // spin parameter a/M handled in lensing material, not rotation period
        packId = "black_hole",
        dataCeilingTextEn = "EHT M87* 42 µas shadow, unlimited zoom",
        dataCeilingTextFa = "سایه ۴۲ میکروثانیه M87*، زوم نامحدود"
    )

    val all: List<ObjectSpec> = listOf(
        SUN,
        MERCURY,
        VENUS,
        EARTH,
        MOON,
        MARS,
        JUPITER,
        SATURN,
        URANUS,
        NEPTUNE,
        MILKY_WAY,
        ISS,
        BLACK_HOLE
    )

    fun byId(id: String): ObjectSpec? = all.find { it.id == id }
}
