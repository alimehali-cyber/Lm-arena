package androidx.compose.ui.geometry

/**
 * Minimal pure-Kotlin stand-in for androidx.compose.ui.geometry.Offset, used ONLY by the
 * offline Kotlin test harness (tools/kotlin-harness) to compile HeroSkyProjection.kt and
 * HeroSkyProjectionTest.kt without the Android/Compose toolchain.
 *
 * Covers exactly the members those two files use: x, y, Zero, plus toString.
 * It is NOT the real Compose class: no operator functions (plus/minus/times), no
 * Distance/Rect helpers. Anything beyond x/y/Zero must not rely on this stub.
 */
class Offset(val x: Float, val y: Float) {
    override fun toString(): String = "Offset($x, $y)"

    companion object {
        val Zero = Offset(0f, 0f)
    }
}
