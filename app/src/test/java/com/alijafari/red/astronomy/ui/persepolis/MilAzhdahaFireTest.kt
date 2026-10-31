package com.alijafari.red.astronomy.ui.persepolis

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards for the small fire in Mil-e Azhdaha's ruined top: it is lit only at night, it lives in the
 * chamber mouth on the tower's summit, and its light is a real point source on the surrounding stone
 * rather than a glow painted over the whole tower.
 *
 * The ramp and the flicker are pure functions, so they are tested as such; the geometry and the GL
 * wiring are guarded at the source level, because the relief needs a GL context the JVM does not have.
 */
class MilAzhdahaFireTest {

    @Test
    fun theFireIsOutByDayAndLitOnlyOnceTheSkyIsDark() {
        // No fire while the Sun is up, nor while civil twilight still carries light.
        assertEquals("noon", 0f, fireNightFactor(62f), 0f)
        assertEquals("setting", 0f, fireNightFactor(3f), 0f)
        assertEquals("at the horizon", 0f, fireNightFactor(0f), 0f)
        assertEquals("civil twilight", 0f, fireNightFactor(-2f), 0f)
        // Fully lit from nautical darkness down.
        assertEquals("nautical twilight", 1f, fireNightFactor(-10f), 1e-4f)
        assertEquals("astronomical night", 1f, fireNightFactor(-35f), 1e-4f)
        // And it comes up smoothly through the dusk rather than switching.
        val dusk = fireNightFactor(-6f)
        assertTrue("half lit at −6°: $dusk", dusk > 0.4f && dusk < 0.6f)
        assertEquals("smoothstep's symmetry", 0.5f, fireNightFactor(-6f), 0.05f)
        // Monotone in the dark: walking DOWN in altitude, the fire is never less lit.
        var previous = 0f
        var alt = 10f
        while (alt >= -30f) {
            val f = fireNightFactor(alt)
            assertTrue("monotone at $alt", f >= previous - 1e-6f)
            previous = f
            alt -= 0.5f
        }
    }

    @Test
    fun theFlickerIsAFlickerAndNotABlink() {
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        var sum = 0f
        var n = 0
        var t = 0f
        while (t < 40f) {
            val f = fireFlicker(t)
            assertTrue("flicker stays a flicker at $t: $f", f in 0.6f..1.1f)
            min = minOf(min, f); max = maxOf(max, f); sum += f; n++
            t += 0.037f
        }
        val mean = sum / n
        assertTrue("it moves (range ${max - min})", max - min > 0.12f)
        assertTrue("but never dips toward black (min $min)", min > 0.6f)
        assertTrue("and averages near its resting value ($mean)", mean in 0.78f..0.95f)
    }

    @Test
    fun theFireBurnsInTheChamberMouthOfTheRuinedTop() {
        val src = sceneSource()
        // The mouth sits on the tower's summit, not on the shaft and not in the air.
        assertTrue("the fire is anchored at the mouth's sill", src.contains("FIRE_LOCAL_Y = 5.70f + 0.02f"))
        assertTrue("it is centred in the opening", src.contains("FIRE_LOCAL_X = 0.11f"))
        // Inside the hole: behind the south face's dressed plane (≈1.90 local), in front of the
        // dark chamber wall (≈0.92 local).
        val mouthFloorLocalZ = 1.55f
        assertTrue("it hangs in the opening, not on the wall", mouthFloorLocalZ < 1.90f && mouthFloorLocalZ > 0.96f)
        assertTrue("the fire's z is that mid-wall value", src.contains("FIRE_LOCAL_Z = 1.55f"))
        // The tower's top course really is 5.70 local metres up: socle top 1.00 + the nine courses.
        val courses = Regex("val courseH = floatArrayOf\\(([^)]*)\\)")
            .find(src)?.groupValues?.get(1)
            ?.split(',')?.mapNotNull { it.trim().removeSuffix("f").toFloatOrNull() }
            ?: error("courseH not found in the tower builder")
        assertTrue("nine shaft courses", courses.size == 9)
        val topY = 1.00f + courses.sum()
        assertEquals("the fire stands on the top course", topY + 0.02f, 5.72f, 0.005f)
    }

    @Test
    fun theLightIsAPointSourceThatDiesOutOverTheNearbyStone() {
        val src = sceneSource()
        // One point light, a metre-scale reach and a real falloff: not a flood on the tower.
        assertTrue("the stone shaders take a fire point light", src.contains("uniform vec3 uFirePos;"))
        assertTrue("with a reach in metres", src.contains("uniform float uFireRange;"))
        assertTrue("and a falloff", src.contains("1.0 / (1.0 + fd2 / (uFireRange * uFireRange))"))
        assertTrue("the reach is a few metres of stone, not the sky", src.contains("FIRE_LIGHT_RANGE_M = 3.20f"))
        assertTrue("the light travels with the group's bake", src.contains("milLocalToWorld(FIRE_LOCAL_X, FIRE_LOCAL_Y, FIRE_LOCAL_Z)"))
        // Both stone programs get the term (Persepolis is out of range; the shader is shared).
        assertTrue(
            "both stone shaders carry the term",
            src.split("uFireRange * uFireRange").size == 3
        )
    }

    @Test
    fun theFireIsGatedByNightAndNothingElse() {
        val src = sceneSource()
        // The gate is the Sun's altitude, inside setSun: no separate clock to fall out of step,
        // and no way for a host to forget to switch it on.
        assertTrue("the gate is derived in setSun", src.contains("fireVisible = fireNightFactor(altitudeDeg)"))
        assertTrue(
            "the fire's light is zero while the gate is closed",
            src.contains("val flick = if (fv > 0.001f) fireFlicker(fireTimeS) * fv else 0f")
        )
        assertTrue("the flame and its halo are skipped while it is out", src.contains("if (fv <= 0.001f) return"))
        // The host keeps the flame flickering only while it burns.
        val host = readMain("java/com/alijafari/red/astronomy/ui/persepolis/PersepolisTextureView.kt")
        assertTrue("the host asks whether the fire is lit", host.contains("scene.fireLit"))
        assertTrue("and stops ticking when it is not", host.contains("handler.removeCallbacks(fireTick)"))
        assertFalse("the tick is never unbounded", host.contains("while (true)"))
    }

    @Test
    fun theFlameIsDrawnIntoTheHoleRatherThanOverIt() {
        val src = sceneSource()
        // The flame is depth-tested (the piers and lintel cut it); only the scattered halo is not.
        assertTrue("blending is on for the fire", src.contains("GLES30.glEnable(GLES30.GL_BLEND)"))
        assertTrue("depth writes are off for the billboards", src.contains("GLES30.glDepthMask(false)"))
        assertTrue("the flame is drawn while depth testing", src.indexOf("GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)") <
            src.indexOf("GLES30.glDisable(GLES30.GL_DEPTH_TEST)"))
        assertTrue("and the pipeline is restored afterwards", src.contains("GLES30.glDepthMask(true)"))
        assertTrue("the fire is drawn after the stone", src.indexOf("drawFire()              // …and the fire") >
            src.indexOf("drawMesh(pM, chunksM)"))
        // Procedural, so nothing blurs at this size.
        assertFalse("no texture is sampled for the flame", src.contains("sampler2D"))
    }

    private fun sceneSource(): String = readMain("java/com/alijafari/red/astronomy/ui/persepolis/PersepolisMilAzhdaha.kt")

    private fun readMain(relative: String): String {
        val f = File(mainDir(), relative)
        assertTrue("missing source file $relative", f.isFile)
        return f.readText()
    }

    private fun mainDir(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "app/src/main")
            if (File(candidate, "java/com/alijafari/red/astronomy").isDirectory) return candidate
            val direct = File(dir, "src/main")
            if (File(direct, "java/com/alijafari/red/astronomy").isDirectory) return direct
            dir = dir.parentFile
        }
        throw AssertionError("could not locate the app main source set")
    }
}
