import com.alijafari.red.astronomy.astro_engine.ARProjectionEngine
import kotlin.math.*

// Z-P3 probe: measures local linear scale (px per degree) through the REAL engine at
// center / horizontal edge / vertical edge / corner, on the 63.5deg FALLBACK tier,
// 1080x2400 canvas, sensorOrientation 90, displayRotation 0 (netRotation 90).
fun attitude(azDeg: Double, altDeg: Double): FloatArray {
    val az = Math.toRadians(azDeg); val alt = Math.toRadians(altDeg)
    val px = cos(alt) * sin(az); val py = cos(alt) * cos(az); val pz = sin(alt)
    val rx = cos(az); val ry = -sin(az); val rz = 0.0
    val ux = -sin(alt) * sin(az); val uy = -sin(alt) * cos(az); val uz = cos(alt)
    val fx = -px; val fy = -py; val fz = -pz
    return floatArrayOf(rx.toFloat(), ux.toFloat(), fx.toFloat(), ry.toFloat(), uy.toFloat(), fy.toFloat(), rz.toFloat(), uz.toFloat(), fz.toFloat())
}
fun proj(az: Double, alt: Double, m: FloatArray, intr: ARProjectionEngine.CameraIntrinsics) =
    ARProjectionEngine.projectAltAz(az, alt, m, 0.0, 0.0, 0.0, 1080f, 2400f, intr, 1.0f, null, 0)!!
fun main() {
    val intr = ARProjectionEngine.getCameraIntrinsics(null)
    println("tier=${intr.source} fx=${intr.fx} fy=${intr.fy} array=${intr.activeArrayWidth}x${intr.activeArrayHeight} sensorOri=${intr.sensorOrientation}")
    val m = attitude(0.0, 0.0)
    val d = 0.05 // deg finite difference
    fun scaleAlong(name: String, az: (Double) -> Double, alt: (Double) -> Double) {
        val p1 = proj(az(-d), alt(-d), m, intr); val p2 = proj(az(d), alt(d), m, intr)
        val pxPerDeg = hypot(p2.x - p1.x.toDouble(), p2.y - p1.y.toDouble()) / (2 * d)
        val pv = proj(az(0.0), alt(0.0), m, intr)
        val rad = hypot(pv.x - 540.0, pv.y - 1200.0)
        val theta = atan(rad / 1090.79)
        val predicted = 1090.79 / (cos(theta) * cos(theta)) * Math.PI / 180
        println("%-28s px/deg=%7.3f  radial=%7.1fpx  theta=%5.2fdeg  gnomonic f*sec^2=%7.3f  ratio=%.4f".format(name, pxPerDeg, rad, Math.toDegrees(theta), predicted, pxPerDeg / predicted))
    }
    scaleAlong("center", { 0.0 }, { 0.0 + it })
    scaleAlong("horizontal edge x=540", { 26.34 + it }, { 0.0 })
    scaleAlong("vertical edge y=1200", { 0.0 }, { 47.75 + it })
    scaleAlong("corner (540,1200)", { 22.0 + it * 0.5575 }, { 43.1 + it }) // approx along corner radial
    // FILL_CENTER arithmetic
    println("\nFILL_CENTER: netRot=90 -> wRot=1080 hRot=1920; scale=max(1080/1080,2400/1920)=1.25")
    println("shown sensor px: width 1080/1.25=864 of 1080 (crop 216 = 20%); height 1920/1.25=1536 of 1920 (no crop)")
    val fv = 1090.79
    println("view HFOV = 2*atan(540/$fv) = %.2f deg (not 63.5!); view VFOV = 2*atan(1200/$fv) = %.2f deg; diagonal = 2*atan(1316.2/$fv) = %.2f deg".format(
        Math.toDegrees(2 * atan(540 / fv)), Math.toDegrees(2 * atan(1200 / fv)), Math.toDegrees(2 * atan(1316.2 / fv))))
    println("sensor FOVs (pre-crop): short side 63.50 deg (by construction), long side 2*atan(960/871.75) = %.2f deg, diagonal %.2f deg".format(
        Math.toDegrees(2 * atan(960 / 871.75)), Math.toDegrees(2 * atan(hypot(960.0, 540.0) / 871.75))))
}
