package com.alijafari.red.astronomy.fieldtrial

import com.alijafari.red.astronomy.fieldtrial.engine.GrayPng
import com.alijafari.red.astronomy.startracker.diagnostics.FailureReason
import com.alijafari.red.astronomy.fieldtrial.engine.FailureWording
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.CRC32

/**
 * G-2.5 prelude tests: pure parts of the debug field trial (PNG writer, failure
 * wording). Runs in CI testDebugUnitTest (debug variant compiles app/src/debug).
 */
class EnginePreludeTest {

    @Test
    fun `gray png is a structurally valid png with grayscale color type`() {
        val w = 8; val h = 4
        val gray = FloatArray(w * h) { (it * 7 % 256).toFloat() }
        val png = GrayPng.encode(w, h, gray)
        assertEquals(
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A).toList(),
            png.take(8).toList()
        )
        assertEquals("IHDR", String(png, 12, 4, Charsets.US_ASCII))
        assertEquals(w, readInt(png, 16)); assertEquals(h, readInt(png, 20))
        assertEquals(8, png[24].toInt())   // bit depth
        assertEquals(0, png[25].toInt())   // color type: grayscale
        val crc = CRC32().apply { update(png, 12, 17) }
        assertEquals(crc.value.toInt(), readInt(png, 29))
        val tail = png.size - 12
        assertEquals("IEND", String(png, tail + 4, 4, Charsets.US_ASCII))
        // IDAT decodes to filter-0 scanlines that round-trip the pixels
        var p = 8; var idat: ByteArray? = null
        while (p < png.size) {
            val len = readInt(png, p)
            val type = String(png, p + 4, 4, Charsets.US_ASCII)
            if (type == "IDAT") idat = png.copyOfRange(p + 8, p + 8 + len)
            p += 12 + len
        }
        val inflater = java.util.zip.Inflater()
        inflater.setInput(idat!!)
        val raw = ByteArray(h * (1 + w))
        assertEquals(raw.size, inflater.inflate(raw))
        for (row in 0 until h) assertEquals(0, raw[row * (1 + w)].toInt())
        for (col in 0 until w) assertEquals(gray[w + col].toInt(), raw[1 + (1 + w) + col].toInt())
    }

    @Test
    fun `every failure reason maps to a plain nonempty sentence without jargon`() {
        val reasons: List<FailureReason> = listOf(
            FailureReason.NoStarsDetected,
            FailureReason.TooFewStars(1),
            FailureReason.CatalogMatchFailed,
            FailureReason.AmbiguousSolution(0.9, 0.8, 1.1),
            FailureReason.SolverFailed,
            FailureReason.RansacFailed,
            FailureReason.AttitudeSolverNoConvergence,
            FailureReason.InsufficientDistribution,
            FailureReason.HighResidualError(2.5, 1.0),
            FailureReason.Timeout,
            FailureReason.GyroStale,
            FailureReason.Unknown
        )
        for (r in reasons) {
            val s = FailureWording.sentence(r)
            assertTrue("empty wording for $r", s.isNotBlank())
            assertTrue("jargon leak for $r: $s", !s.contains("Ransac") && !s.contains("Catalog") && !s.contains("enum"))
        }
        assertTrue(FailureWording.sentence(null).isNotEmpty())
    }

    private fun readInt(b: ByteArray, at: Int): Int =
        ((b[at].toInt() and 0xFF) shl 24) or ((b[at + 1].toInt() and 0xFF) shl 16) or
            ((b[at + 2].toInt() and 0xFF) shl 8) or (b[at + 3].toInt() and 0xFF)
}
