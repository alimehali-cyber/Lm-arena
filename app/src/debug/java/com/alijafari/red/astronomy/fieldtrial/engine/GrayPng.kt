package com.alijafari.red.astronomy.fieldtrial.engine

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * G-2.5 (frame capture): minimal grayscale PNG writer (8-bit, filter 0), pure Kotlin,
 * so raw analysis frames can be stored in the trial zip without Android bitmap code.
 * Verified by decoding the header/chunk structure in unit tests.
 */
object GrayPng {

    /** Encode a [0,255] grayscale image as a valid PNG. */
    fun encode(width: Int, height: Int, gray: FloatArray): ByteArray {
        require(width > 0 && height > 0 && gray.size == width * height) { "bad dimensions" }
        val out = ByteArrayOutputStream(width * height / 2 + 256)
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))

        // IHDR: width, height, bit depth 8, color type 0 (grayscale)
        val ihdr = ByteArray(13)
        putInt(ihdr, 0, width)
        putInt(ihdr, 4, height)
        ihdr[8] = 8   // bit depth
        ihdr[9] = 0   // color type grayscale
        ihdr[10] = 0  // compression
        ihdr[11] = 0  // filter
        ihdr[12] = 0  // interlace
        chunk(out, "IHDR", ihdr)

        // IDAT: raw scanlines with filter byte 0, zlib-deflated
        val raw = ByteArray(height * (1 + width))
        var p = 0
        for (row in 0 until height) {
            raw[p++] = 0
            val base = row * width
            for (col in 0 until width) {
                val v = gray[base + col].toInt().coerceIn(0, 255)
                raw[p++] = v.toByte()
            }
        }
        val deflater = Deflater(Deflater.BEST_SPEED)
        deflater.setInput(raw)
        deflater.finish()
        val buf = ByteArray(1 shl 15)
        val idat = ByteArrayOutputStream(raw.size / 2 + 64)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            idat.write(buf, 0, n)
        }
        deflater.end()
        chunk(out, "IDAT", idat.toByteArray())

        chunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun chunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
        val len = ByteArray(4)
        putInt(len, 0, data.size)
        out.write(len)
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        out.write(typeBytes)
        out.write(data)
        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data)
        val crcBytes = ByteArray(4)
        putInt(crcBytes, 0, crc.value.toInt())
        out.write(crcBytes)
    }

    private fun putInt(b: ByteArray, at: Int, v: Int) {
        b[at] = (v ushr 24).toByte()
        b[at + 1] = (v ushr 16).toByte()
        b[at + 2] = (v ushr 8).toByte()
        b[at + 3] = v.toByte()
    }
}
