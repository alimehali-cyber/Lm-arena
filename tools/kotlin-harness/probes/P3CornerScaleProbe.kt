import com.alijafari.red.astronomy.astro_engine.ARProjectionEngine
import kotlin.math.*
fun attitude(azDeg: Double, altDeg: Double): FloatArray {
    val az = Math.toRadians(azDeg); val alt = Math.toRadians(altDeg)
    val px = cos(alt) * sin(az); val py = cos(alt) * cos(az); val pz = sin(alt)
    val rx = cos(az); val ry = -sin(az); val rz = 0.0
    val ux = -sin(alt) * sin(az); val uy = -sin(alt) * cos(az); val uz = cos(alt)
    val fx = -px; val fy = -py; val fz = -pz
    return floatArrayOf(rx.toFloat(), ux.toFloat(), fx.toFloat(), ry.toFloat(), uy.toFloat(), fy.toFloat(), rz.toFloat(), uz.toFloat(), fz.toFloat())
}
fun main() {
    val intr = ARProjectionEngine.getCameraIntrinsics(null); val m = attitude(0.0, 0.0)
    fun proj(az: Double, alt: Double) = ARProjectionEngine.projectAltAz(az, alt, m, 0.0, 0.0, 0.0, 1080f, 2400f, intr, 1.0f, null, 0)
    // 1) find sky point mapping to view corner (1080, 2400): coarse grid + local refine
    var best = Triple(0.0, 0.0, Double.MAX_VALUE)
    var az = 0.0; var alt = -80.0
    while (az <= 89) { alt = -80.0
        while (alt <= 5) {
            val p = proj(az, alt); if (p != null) {
                val e = abs(p.x - 1080.0) + abs(p.y - 2400.0)
                if (e < best.third) best = Triple(az, alt, e) }
            alt += 0.5 }
        az += 0.5 }
    var (baz, balt) = best.first to best.second
    var step = 0.05
    while (step > 1e-6) {
        var improved = false
        for (da in doubleArrayOf(-step, 0.0, step)) for (dl in doubleArrayOf(-step, 0.0, step)) {
            val p = proj(baz + da, balt + dl) ?: continue
            val e = abs(p.x - 1080.0) + abs(p.y - 2400.0)
            if (e < best.third - 1e-12) { best = Triple(baz + da, balt + dl, e); improved = true } }
        if (improved) { baz = best.first; balt = best.second } else step /= 2.0
    }
    val cp = proj(baz, balt)!!
    println("corner sky point: az=%.4f alt=%.4f -> px=%.2f py=%.2f (target 1080,2400)".format(baz, balt, cp.x, cp.y))
    val fview = intr.fx * 1.25
    val thC = atan(1316.22 / fview)
    println("corner central angle theta=%.3f deg (theory %.3f)".format(
        Math.toDegrees(acos(cos(Math.toRadians(baz)) * cos(Math.toRadians(balt)))), Math.toDegrees(thC)))
    // 2) radial direction in (az,alt) space = gradient of r=|p-center|; measure scales
    fun pt(a: Double, l: Double) = proj(a, l)!!
    fun rAt(a: Double, l: Double) = hypot(pt(a, l).x - 540.0, pt(a, l).y - 1200.0)
    val h = 1e-3
    val gAz = (rAt(baz + h, balt) - rAt(baz - h, balt)) / (2 * h)
    val gAlt = (rAt(baz, balt + h) - rAt(baz, balt - h)) / (2 * h)
    val gn = hypot(gAz, gAlt)
    val uRad = doubleArrayOf(gAz / gn, gAlt / gn)   // deg per deg
    val uTan = doubleArrayOf(-uRad[1], uRad[0])
    // angular step along great circle for direction (uAz,uAlt) at (baz,balt): |u| in rad ~ small
    fun skyStep(u: DoubleArray): Double { // arc length deg for 1 deg parameter change
        val (a1, l1) = baz to balt; val (a2, l2) = baz + u[0] to balt + u[1]
        val v1 = doubleArrayOf(cos(Math.toRadians(l1)) * sin(Math.toRadians(a1)), cos(Math.toRadians(l1)) * cos(Math.toRadians(a1)), sin(Math.toRadians(l1)))
        val v2 = doubleArrayOf(cos(Math.toRadians(l2)) * sin(Math.toRadians(a2)), cos(Math.toRadians(l2)) * cos(Math.toRadians(a2)), sin(Math.toRadians(l2)))
        return Math.toDegrees(acos((v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2]).coerceIn(-1.0, 1.0))) }
    fun scaleAlong(name: String, u: DoubleArray, theory: Double) {
        val e = 0.02
        val p1 = pt(baz - e * u[0], balt - e * u[1]); val p2 = pt(baz + e * u[0], balt + e * u[1])
        val pxPerDegParam = hypot(p2.x - p1.x, p2.y - p1.y) / (2 * e)
        val perArc = pxPerDegParam / skyStep(u)
        println("%-18s measured=%7.3f px/deg(arc)  theory=%7.3f  ratio=%.4f".format(name, perArc, theory, perArc / theory)) }
    scaleAlong("corner RADIAL", uRad, fview / (cos(thC) * cos(thC)) * Math.PI / 180)
    scaleAlong("corner TANGENTIAL", uTan, fview / cos(thC) * Math.PI / 180)
}
