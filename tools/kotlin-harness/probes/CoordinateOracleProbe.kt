package probes

/**
 * F-A3 Coordinate Oracle Probe — drives the app's REAL live-path coordinate functions on the
 * deterministic A2 case grid and emits CSV for the astropy oracle diff.
 *
 * Live-path call sites reproduced here (CompassARScreen / engines as called from it):
 *  - ROUTE star_live:   TimeEngine.getLAST(jd, lon) + CoordinateEngine.equatorialToHorizontal(eq, last, lat)
 *                       = CompassARScreen.kt:469-471 + :930 / :2546 (elevation arg omitted => 0.0 default)
 *  - ROUTE star_live_elev: same but observerElevationM = location height (what the live call WOULD do
 *                       if it passed uiState.userLocation.elevationMeters — it does not; kept for diagnosis)
 *  - ROUTE fte:         FrameTransformationEngine.equatorialToHorizontal (full precession+nutation+Bennett
 *                       pipeline) — ZERO live callers; reference route inside the app
 *  - ROUTE sun:         SunEngine.getSunAltAz(jd, lat, lon)  (CompassARScreen.kt:473-476 sunHoriz)
 *  - ROUTE moon:        MoonEngine.calculateMoon(jd, lat, lon, elev) (CompassARScreen.kt:477-484 moonHoriz)
 *  - ROUTE planet_<P>:  PlanetEngine.calculatePlanet + CoordinateEngine.equatorialToHorizontal — the
 *                       overlay path for planets via AstronomyCatalog/AstroDispatchEngine (dispatch uses
 *                       pos.raDeg/decDeg; CompassARScreen.kt:930 transforms them exactly like star_live)
 *
 * Also emits: objects.csv (display-catalog inputs), times.csv (GMST/GAST per instant), and the
 * refraction ladder (A6): app refraction extracted end-to-end at alts 0/2/5/10/20/45/89 deg.
 */
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.data.catalog.StarCatalog
import com.alijafari.red.astronomy.data.catalog.DeepSkyCatalog
import com.alijafari.red.astronomy.domain.ObjectType
import java.io.File
import kotlin.math.*

fun main() {
    val outDir = File("/tmp/oracle_probe")
    outDir.mkdirs()
    val objectsOut = StringBuilder("obj_id,obj_type,name_en,ra_deg,dec_deg,mag,hip\n")
    val routesOut = StringBuilder("loc,lat,lon,height_m,iso_utc,jd_utc,route,obj_id,az_deg,alt_deg,ra_deg,dec_deg,dist_km\n")
    val timesOut = StringBuilder("iso_utc,jd_utc,gmst_deg_kotlin,gast_deg_kotlin\n")
    val ladderOut = StringBuilder("loc,height_m,iso_utc,target_alt_deg,az_used_deg,ra_deg,dec_deg,legacy_elev0_alt_out,legacy_elev0_R_arcmin,legacy_elevH_alt_out,legacy_elevH_R_arcmin,fte_bennett_R_arcmin\n")

    data class Loc(val name: String, val lat: Double, val lon: Double, val h: Double)
    val locs = listOf(
        Loc("Tehran", 35.69, 51.39, 1200.0),
        Loc("Sydney", -33.87, 151.21, 40.0),
        Loc("Quito", 0.0, -78.5, 2850.0),
        Loc("Tromso", 69.65, 18.96, 10.0),
        Loc("CapeTown", -33.93, 18.42, 25.0),
        Loc("Honolulu", 21.31, -157.86, 5.0)
    )
    data class Inst(val iso: String, val y: Int, val mo: Int, val d: Int, val h: Int, val mi: Int, val s: Int)
    val instants = listOf(
        Inst("2026-09-04T20:00:00", 2026, 9, 4, 20, 0, 0),
        Inst("2026-03-20T12:00:00", 2026, 3, 20, 12, 0, 0),
        Inst("2000-01-01T12:00:00", 2000, 1, 1, 12, 0, 0),
        Inst("2030-06-21T00:00:00", 2030, 6, 21, 0, 0, 0),
        Inst("2015-12-31T23:59:59", 2015, 12, 31, 23, 59, 59),
        Inst("2026-07-15T02:30:00", 2026, 7, 15, 2, 30, 0)
    )

    // ---------- objects from the display catalogs ----------
    val stars = StarCatalog.getStars().filter { it.raDeg != 0.0 || it.decDeg != 0.0 }
    val dsos = DeepSkyCatalog.getDeepSkyObjects()
    for (o in stars + dsos) {
        objectsOut.append("\"${o.id}\",${o.type},\"${o.nameEn.replace("\"", "'")}\",${"%.6f".format(o.raDeg)},${"%.6f".format(o.decDeg)},${o.magnitude},${o.hipId ?: ""}\n")
    }
    System.err.println("objects: ${stars.size} stars + ${dsos.size} deep-sky")

    val fte = FrameTransformationEngine()

    fun Loc.str() = "$name,$lat,$lon,${"%.1f".format(h)}"
    fun row(sb: StringBuilder, loc: Loc, iso: String, jd: Double, route: String, objId: String,
            az: Double, alt: Double, ra: Double? = null, dec: Double? = null, dist: Double? = null) {
        sb.append("${loc.str()},$iso,${"%.9f".format(jd)},$route,\"$objId\",${"%.8f".format(az)},${"%.8f".format(alt)}" +
                (ra?.let { ",${"%.8f".format(it)}" } ?: ",") + (dec?.let { ",${"%.8f".format(it)}" } ?: ",") +
                (dist?.let { ",${"%.3f".format(it)}" } ?: ",") + "\n")
    }

    for (inst in instants) {
        val at = AstroTime.fromUtcDate(inst.y, inst.mo, inst.d, inst.h, inst.mi, inst.s)
        val jd = TimeEngine.getJulianDate(at.utcMs)
        timesOut.append("${inst.iso},${"%.9f".format(jd)},${"%.9f".format(TimeEngine.getGMST(jd))},${"%.9f".format(TimeEngine.getGAST(jd))}\n")

        for (loc in locs) {
            val lastDeg = TimeEngine.getLAST(jd, loc.lon)

            // ROUTE star_live = the FIXED live call (F-A5): precess J2000 catalog position to
            // the mean equator of date, then LAST + legacy horizontal transform (elevation arg
            // omitted exactly as CompassARScreen does). star_live_j2000raw preserves the
            // pre-fix behaviour for the before/after evidence.
            for (o in stars + dsos) {
                val eqDate = CoordinateEngine.precessJ2000EquatorialToDate(o.raDeg, o.decDeg, jd)
                val hz = CoordinateEngine.equatorialToHorizontal(eqDate, lastDeg, loc.lat)
                row(routesOut, loc, inst.iso, jd, "star_live", o.id, hz.azimuthDeg, hz.altitudeDeg)
                val hzE = CoordinateEngine.equatorialToHorizontal(eqDate, lastDeg, loc.lat, loc.h)
                row(routesOut, loc, inst.iso, jd, "star_live_elev", o.id, hzE.azimuthDeg, hzE.altitudeDeg)
                val hzRaw = CoordinateEngine.equatorialToHorizontal(CoordinateEngine.Equatorial(o.raDeg, o.decDeg), lastDeg, loc.lat)
                row(routesOut, loc, inst.iso, jd, "star_live_j2000raw", o.id, hzRaw.azimuthDeg, hzRaw.altitudeDeg)
                val hf = fte.equatorialToHorizontal(o.raDeg, o.decDeg, at, loc.lat, loc.lon, loc.h)
                row(routesOut, loc, inst.iso, jd, "fte", o.id, hf.azDeg, hf.altDeg)
            }

            // ROUTE sun
            val sunPos = SunEngine.calculatePosition(jd)
            val sunHz = SunEngine.getSunAltAz(jd, loc.lat, loc.lon)
            row(routesOut, loc, inst.iso, jd, "sun", "sun", sunHz.azimuthDeg, sunHz.altitudeDeg, sunPos.raDeg, sunPos.decDeg, sunPos.distanceAu * 149597870.7)

            // ROUTE moon (topocentric, as live)
            val md = MoonEngine.calculateMoon(jd = jd, latitude = loc.lat, longitude = loc.lon, elevationM = loc.h)
            row(routesOut, loc, inst.iso, jd, "moon", "moon", md.azimuthDeg, md.altitudeDeg, md.raDeg, md.decDeg, md.distanceKm)
            // moon geocentric (no parallax) for the parallax-defect check
            val mg = MoonEngine.calculateMoon(at)
            val mgHz = CoordinateEngine.equatorialToHorizontal(CoordinateEngine.Equatorial(mg.raDeg, mg.decDeg), lastDeg, loc.lat)
            row(routesOut, loc, inst.iso, jd, "moon_geocentric", "moon", mgHz.azimuthDeg, mgHz.altitudeDeg, mg.raDeg, mg.decDeg, mg.distanceKm)

            // ROUTE planets (live overlay path: dispatch ra/dec -> CoordinateEngine transform)
            for (p in PlanetEngine.PlanetType.entries) {
                val pos = PlanetEngine.calculatePlanet(p, at)
                val hz = CoordinateEngine.equatorialToHorizontal(CoordinateEngine.Equatorial(pos.raDeg, pos.decDeg), lastDeg, loc.lat)
                row(routesOut, loc, inst.iso, jd, "planet_${p.name.lowercase()}", "planet_${p.name.lowercase()}",
                    hz.azimuthDeg, hz.altitudeDeg, pos.raDeg, pos.decDeg, pos.distanceAU * 149597870.7)
            }
        }
    }

    // ---------- A6 refraction ladder: craft RA/Dec hitting target geometric alt/az ----------
    // Given alt h, az A (from N, E+): dec = asin(sinφ sinh + cosφ cosh cosA);
    // sinH = -cosh sinA / cosdec ; cosH = (sindec cosφ - cosh cosA sinφ)/cosdec ; RA = LAST - H
    for (loc in listOf(locs[0], locs[5])) {  // Tehran (1200 m) and Honolulu (5 m)
        val inst = instants[0]
        val at = AstroTime.fromUtcDate(inst.y, inst.mo, inst.d, inst.h, inst.mi, inst.s)
        val jd = TimeEngine.getJulianDate(at.utcMs)
        val lastDeg = TimeEngine.getLAST(jd, loc.lon)
        for (targetAlt in listOf(0.0, 2.0, 5.0, 10.0, 20.0, 45.0, 89.0)) {
            val az = 135.0
            val latR = Math.toRadians(loc.lat); val hR = Math.toRadians(targetAlt); val aR = Math.toRadians(az)
            val decR = asin(sin(latR) * sin(hR) + cos(latR) * cos(hR) * cos(aR))
            val sinH = -cos(hR) * sin(aR) / cos(decR)
            // cos(dec)cos(H) = sin(h)cos(phi) - cos(h)cos(A)sin(phi)   (az from North, East+)
            val cosH = (sin(hR) * cos(latR) - cos(hR) * cos(aR) * sin(latR)) / cos(decR)
            val HR = atan2(sinH, cosH)
            val raDeg = ((lastDeg - Math.toDegrees(HR)) % 360.0 + 360.0) % 360.0
            val decDeg = Math.toDegrees(decR)
            // Legacy (live) route with elevation 0 AND location height
            val out0 = CoordinateEngine.equatorialToHorizontal(CoordinateEngine.Equatorial(raDeg, decDeg), lastDeg, loc.lat, 0.0)
            val outH = CoordinateEngine.equatorialToHorizontal(CoordinateEngine.Equatorial(raDeg, decDeg), lastDeg, loc.lat, loc.h)
            val fteAlt = fte.applyRefraction(targetAlt)
            ladderOut.append("${loc.name},${"%.1f".format(loc.h)},${inst.iso},${"%.3f".format(targetAlt)},$az," +
                    "${"%.8f".format(raDeg)},${"%.8f".format(decDeg)}," +
                    "${"%.8f".format(out0.altitudeDeg)},${"%.6f".format((out0.altitudeDeg - targetAlt) * 60.0)}," +
                    "${"%.8f".format(outH.altitudeDeg)},${"%.6f".format((outH.altitudeDeg - targetAlt) * 60.0)}," +
                    "${"%.6f".format((fteAlt - targetAlt) * 60.0)}\n")
        }
    }

    File(outDir, "objects.csv").writeText(objectsOut.toString())
    File(outDir, "routes.csv").writeText(routesOut.toString())
    File(outDir, "times.csv").writeText(timesOut.toString())
    File(outDir, "ladder.csv").writeText(ladderOut.toString())
    System.err.println("probe done -> /tmp/oracle_probe")
}
