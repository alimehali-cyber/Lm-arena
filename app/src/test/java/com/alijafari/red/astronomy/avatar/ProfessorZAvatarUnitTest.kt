package com.alijafari.red.astronomy.avatar

import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessorZAvatarUnitTest {

    private fun assetsRoot(): File {
        val candidates = listOf(File("src/main/assets"), File("app/src/main/assets"))
        return candidates.firstOrNull { it.exists() } ?: error("Assets directory not found")
    }

    private fun sourceRoot(): File {
        val candidates = listOf(File("src/main/java"), File("app/src/main/java"))
        return candidates.firstOrNull { it.exists() } ?: error("Java/Kotlin source directory not found")
    }

    @Test
    fun bundledProfessorZAssetsExistAndRespectMobileBudgets() {
        val dir = File(assetsRoot(), "avatar")
        val lod0File = File(dir, "professor_z.glb")
        val lod1File = File(dir, "professor_z_lod1.glb")
        val faFile = File(dir, "professor_z_lipsync_fa.json")
        val enFile = File(dir, "professor_z_lipsync_en.json")

        assertTrue("Missing LOD0 GLB", lod0File.exists() && lod0File.length() in 500_000L..4_500_000L)
        assertTrue("Missing LOD1 GLB", lod1File.exists() && lod1File.length() in 300_000L..3_500_000L)
        assertTrue("LOD1 must be smaller than LOD0", lod1File.length() < lod0File.length())
        assertTrue("Missing Persian lip-sync JSON", faFile.exists() && faFile.length() > 500L)
        assertTrue("Missing English lip-sync JSON", enFile.exists() && enFile.length() > 500L)

        val lod0 = GltfBinaryLoader.load(lod0File)
        val lod1 = GltfBinaryLoader.load(lod1File)

        assertTrue("LOD0 triangle budget exceeded: ${lod0.totalTriangles}", lod0.totalTriangles in 10_000..25_000)
        assertTrue("LOD1 must have fewer triangles than LOD0", lod1.totalTriangles < lod0.totalTriangles)
        assertEquals(60, lod0.jointNodeIndices.size)
        assertEquals(60, lod1.jointNodeIndices.size)
        assertEquals(26, lod0.morphTargetCount)
        assertEquals(24, lod0.animations.size)

        assertNotNull("LOD0 embedded albedo PNG missing", lod0.albedoPngBytes)
        assertNotNull("LOD0 embedded ORM PNG missing", lod0.ormPngBytes)
        val albedoImg = ImageIO.read(ByteArrayInputStream(lod0.albedoPngBytes))
        val ormImg = ImageIO.read(ByteArrayInputStream(lod0.ormPngBytes))
        assertEquals(1024, albedoImg.width)
        assertEquals(1024, albedoImg.height)
        assertEquals(1024, ormImg.width)
        assertEquals(1024, ormImg.height)

        val lod1Albedo = ImageIO.read(ByteArrayInputStream(lod1.albedoPngBytes))
        assertEquals(512, lod1Albedo.width)
        assertEquals(512, lod1Albedo.height)
    }

    @Test
    fun restPoseBindMatricesAreIdentityAndCameraFramesUprightHeadToToe() {
        val lod0 = GltfBinaryLoader.load(File(assetsRoot(), "avatar/professor_z.glb"))
        val animator = AvatarAnimator(lod0)
        animator.evaluateRestPose()

        var maxErr = 0f
        for (j in 0 until animator.jointCount) {
            val base = j * 16
            for (r in 0..3) {
                for (c in 0..3) {
                    val expected = if (r == c) 1f else 0f
                    maxErr = maxOf(maxErr, abs(animator.skinMatrices[base + c * 4 + r] - expected))
                }
            }
        }
        assertTrue("Rest skin matrix identity error too high: $maxErr", maxErr < 1e-4f)

        // Verify world bounds and upright head-to-toe camera projection
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (prim in lod0.primitives) {
            val pos = prim.positions
            for (v in 0 until prim.vertexCount) {
                val y = pos[v * 3 + 1]
                val z = pos[v * 3 + 2]
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (z < minZ) minZ = z
                if (z > maxZ) maxZ = z
            }
        }
        assertEquals(0.0f, minY, 0.02f)
        assertEquals(1.73f, maxY, 0.05f)
        assertTrue("Front face must extend toward +Z camera ($maxZ)", maxZ > 0.08f)

        val vp = AvatarCameraAndLighting.computeViewProjMatrix(432, 636)
        val crownNdc = AvatarCameraAndLighting.projectWorldToNdc(vp, 0f, maxY, 0f)
        val solesNdc = AvatarCameraAndLighting.projectWorldToNdc(vp, 0f, minY, 0f)
        assertTrue("Crown NDC Y out of framing band: ${crownNdc[1]}", crownNdc[1] in 0.70f..0.97f)
        assertTrue("Soles NDC Y out of ground band: ${solesNdc[1]}", solesNdc[1] in -0.99f..-0.78f)
    }

    @Test
    fun sceneCrossFadesPersianLipSyncAndPureGles30SafeguardsPass() {
        val dir = File(assetsRoot(), "avatar")
        val lod0 = GltfBinaryLoader.load(File(dir, "professor_z.glb"))
        val animator = AvatarAnimator(lod0)
        animator.loadLipSyncCues(File(dir, "professor_z_lipsync_fa.json"), persian = true)
        animator.loadLipSyncCues(File(dir, "professor_z_lipsync_en.json"), persian = false)

        val body = lod0.primitives.first()
        val morphed = FloatArray(body.positions.size)
        var maxJawWeight = 0f

        animator.triggerReaction(AvatarAnimator.TAP_REACTIONS[0], nowSec = 0.2f)
        for (step in 1..60) {
            val t = step * 0.05f
            val sunAlt = when {
                step < 20 -> -24.0
                step < 40 -> -2.0
                else -> 32.0
            }
            animator.evaluateSceneFrame(timeSec = t, dtSec = 0.05f, sunAltitudeDeg = sunAlt, persian = true)
            maxJawWeight = maxOf(maxJawWeight, animator.morphWeights[AvatarAnimator.MORPH_INDEX.getValue("jawOpen")])
            for (v in animator.skinMatrices) {
                assertTrue("Non-finite skinning matrix element", v.isFinite())
            }
            body.applyMorphWeights(animator.morphWeights, morphed)
            for (v in morphed) {
                assertTrue("Non-finite morphed vertex coordinate", v.isFinite())
            }
        }
        assertTrue("Persian lip-sync did not open jaw during catchphrase: $maxJawWeight", maxJawWeight > 0.20f)

        // Enforce strict zero-Vulkan & zero-NDK safeguard across avatar runtime package
        val avatarPkg = File(sourceRoot(), "com/alijafari/red/astronomy/avatar")
        val kotlinFiles = avatarPkg.listFiles()?.filter { it.extension == "kt" }.orEmpty()
        assertEquals(6, kotlinFiles.size)
        for (f in kotlinFiles) {
            val src = f.readText()
            assertFalse("Forbidden Vulkan symbol in ${f.name}", src.contains("android.hardware.vulkan") || src.contains("VK_"))
            assertFalse("Forbidden JNI symbol in ${f.name}", src.contains("System.loadLibrary"))
        }
        assertTrue(AvatarShaders.VERTEX_SHADER_GLES30.startsWith("#version 300 es"))
        assertTrue(AvatarShaders.FRAGMENT_SHADER_GLES30.startsWith("#version 300 es"))
    }
}
