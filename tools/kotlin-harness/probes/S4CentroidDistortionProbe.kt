package probes

import com.alijafari.red.astronomy.startracker.calibration.DistortionModel
import com.alijafari.red.astronomy.startracker.detection.BackgroundEstimator
import com.alijafari.red.astronomy.startracker.detection.Centroider
import com.alijafari.red.astronomy.startracker.detection.DetectedBlob
import com.alijafari.red.astronomy.startracker.detection.GrayscaleImage
import com.alijafari.red.astronomy.startracker.detection.StarBlobDetector
import kotlin.math.exp
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * S4: D4 (centroid accuracy: Gaussian PSF sigma {0.8,1.2,2.0} px x SNR {5,10,30},
 * measured sub-px error of the REAL Centroider, moment + Gaussian-fit) and
 * D5 (distortion displacement at k1=-0.05, 50/75/100% of field half-width,
 * 63.5 deg tier, in px, using the REAL DistortionModel, incl. round-trip error).
 * SYNTHETIC-SKY image simulation. Usage: probes.S4CentroidDistortionProbeKt
 */
fun main() {
    // ---------- D4: centroid sub-pixel accuracy ----------
    println("D4 - Gaussian PSF centroid error (mean |err| px over 50 deterministic sub-pixel placements)")
    println("%6s %4s | %10s %10s | %10s %10s".format("sigma", "SNR", "moment_px", "gaussfit_px", "bias_x_px", "bias_y_px"))
    val rng = Random(777005)
    val bgEst = BackgroundEstimator()
    val detector = StarBlobDetector()
    val centroider = Centroider()
    for (sigma in doubleArrayOf(0.8, 1.2, 2.0)) {
        for (snr in intArrayOf(5, 10, 30)) {
            var sumMom = 0.0; var sumGauss = 0.0; var biasX = 0.0; var biasY = 0.0
            var used = 0; var noDetect = 0; var wrongBlob = 0
            val n = 50
            repeat(n) {
                val tx = 20.0 + rng.nextDouble() * 24.0
                val ty = 20.0 + rng.nextDouble() * 24.0
                val peak = snr.toDouble() // noise sigma = 1 ADU, background 10 -> SNR = peak/noise
                val img = GrayscaleImage(64, 64, FloatArray(64 * 64))
                // per-pixel deterministic Gaussian noise (Box-Muller from the same stream)
                val noise = DoubleArray(64 * 64)
                var i = 0
                while (i < noise.size) {
                    val u1 = (rng.nextDouble()).coerceAtLeast(1e-12); val u2 = rng.nextDouble()
                    val r = sqrt(-2.0 * kotlin.math.ln(u1))
                    noise[i++] = r * kotlin.math.cos(2 * PI * u2)
                    if (i < noise.size) noise[i++] = r * kotlin.math.sin(2 * PI * u2)
                }
                for (y in 0 until 64) for (x in 0 until 64) {
                    val dx = x - tx; val dy = y - ty
                    img.set(x, y, (10.0 + peak * exp(-(dx * dx + dy * dy) / (2 * sigma * sigma)) + noise[y * 64 + x]).toFloat())
                }
                val bg = bgEst.estimate(img)
                val nSigma = bgEst.estimateNoiseSigma(img, bg)
                val blobs = detector.detect(img, bg, nSigma)
                // blob whose peak pixel is nearest the injected star
                val b = blobs.minByOrNull { hypot((it.peakX - tx), (it.peakY - ty).toDouble()) }
                if (b == null || hypot((b.peakX - tx), (b.peakY - ty).toDouble()) > 4.0) { noDetect++; return@repeat }
                val mom = centroider.centroid(img, bg, b)
                val gau = centroider.centroidGaussianFit(img, bg, b)
                sumMom += hypot(mom.x - tx, mom.y - ty)
                sumGauss += hypot(gau.x - tx, gau.y - ty)
                biasX += gau.x - tx; biasY += gau.y - ty
                used++
            }
            if (used == 0) {
                println("%6.1f %4d | NO DETECTIONS in 50 trials (below 5-sigma detector threshold)".format(sigma, snr))
            } else {
                println("%6.1f %4d | %10.4f %10.4f | %10.4f %10.4f | used=%d noDetect=%d".format(
                    sigma, snr, sumMom / used, sumGauss / used, biasX / used, biasY / used, used, noDetect))
            }
        }
    }
    // ---------- D5: distortion displacement ----------
    println()
    println("D5 - Brown-Conrady k1=-0.05 (k2=p=0) at 63.5deg tier: short-side half-width = 540 px, f = (1080/2)/tan(31.75deg) = 872.63 px")
    val f = (1080.0 / 2.0) / kotlin.math.tan(Math.toRadians(63.5) / 2.0)
    val dm = DistortionModel(k1 = -0.05)
    for (frac in doubleArrayOf(0.50, 0.75, 1.00)) {
        val rPx = frac * 540.0
        val rn = rPx / f // normalized radius
        // point on the x axis at this radius
        val (xd, yd) = dm.distortIdealToDistortedNormalized(rn, 0.0)
        val dispPx = (xd - rn) * f
        // round trip: undistort the distorted point back
        val (xr, yr) = dm.undistortDistortedToIdealNormalized(xd, yd)
        val roundTripPx = hypot(xr - rn, yr) * f
        println("half-width %3.0f%%: r=%6.1f px (r_norm=%.4f)  displacement=%7.3f px  roundtrip_err=%.6f px".format(frac * 100, rPx, rn, dispPx, roundTripPx))
    }
}
