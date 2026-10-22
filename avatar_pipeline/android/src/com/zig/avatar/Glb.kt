package com.zig.avatar

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Minimal reader for the GLB container and the glTF 2.0 subset this pipeline emits.
 *
 * Scope, deliberately narrow:
 *  - GLB only (no .gltf + external .bin, no data: URIs)
 *  - one buffer, one skin, triangle lists, POSITION/NORMAL/JOINTS_0/WEIGHTS_0
 *  - morph targets with POSITION deltas only
 *  - animations with LINEAR or STEP samplers on translation/rotation/scale/weights
 *
 * Anything outside that raises [GlbException] with the offending field. A silent
 * "loaded but wrong" is the failure mode this class exists to prevent.
 */
class GlbException(message: String) : RuntimeException(message)

internal class Glb(val json: JSONObject, val bin: ByteBuffer) {

    companion object {
        private const val MAGIC = 0x46546C67
        private const val CHUNK_JSON = 0x4E4F534A
        private const val CHUNK_BIN = 0x004E4942

        fun parse(bytes: ByteArray): Glb {
            if (bytes.size < 20) throw GlbException("file too small to be a GLB")
            val head = ByteBuffer.wrap(bytes, 0, 12).order(ByteOrder.LITTLE_ENDIAN)
            if (head.getInt(0) != MAGIC) throw GlbException("not a GLB (bad magic)")
            val version = head.getInt(4)
            if (version != 2) throw GlbException("unsupported GLB version $version")
            val total = head.getInt(8)
            if (total > bytes.size) throw GlbException("truncated GLB: header says $total, have ${bytes.size}")

            var off = 12
            var json: JSONObject? = null
            var bin: ByteBuffer? = null
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            while (off + 8 <= total) {
                val len = buf.getInt(off)
                val type = buf.getInt(off + 4)
                if (len < 0 || off + 8 + len > bytes.size) throw GlbException("chunk overruns file at $off")
                when (type) {
                    CHUNK_JSON -> json = JSONObject(String(bytes, off + 8, len, Charsets.UTF_8))
                    CHUNK_BIN -> bin = ByteBuffer.wrap(bytes, off + 8, len).slice().order(ByteOrder.LITTLE_ENDIAN)
                }
                off += 8 + len + ((4 - len % 4) % 4)
            }
            return Glb(json ?: throw GlbException("no JSON chunk"),
                bin ?: throw GlbException("no BIN chunk"))
        }
    }

    // ---- accessors ---------------------------------------------------------

    private fun accessor(i: Int) = json.getJSONArray("accessors").getJSONObject(i)

    private fun view(i: Int) = json.getJSONArray("bufferViews").getJSONObject(i)

    private fun componentSize(componentType: Int) = when (componentType) {
        5120, 5121 -> 1
        5122, 5123 -> 2
        5125, 5126 -> 4
        else -> throw GlbException("unsupported componentType $componentType")
    }

    private fun componentCount(type: String) = when (type) {
        "SCALAR" -> 1
        "VEC2" -> 2
        "VEC3" -> 3
        "VEC4" -> 4
        "MAT4" -> 16
        else -> throw GlbException("unsupported accessor type $type")
    }

    /** Byte offset of the first element, honouring both bufferView and accessor offsets. */
    private fun baseOffset(acc: JSONObject): Int {
        val v = view(acc.getInt("bufferView"))
        return v.optInt("byteOffset", 0) + acc.optInt("byteOffset", 0)
    }

    /** Reads a float accessor. Integer accessors are converted, never reinterpreted. */
    fun floats(index: Int): FloatArray {
        val acc = accessor(index)
        val n = acc.getInt("count") * componentCount(acc.getString("type"))
        val ct = acc.getInt("componentType")
        val stride = view(acc.getInt("bufferView")).optInt("byteStride", 0)
        val elemSize = componentSize(ct) * componentCount(acc.getString("type"))
        val out = FloatArray(n)
        val base = baseOffset(acc)
        if (stride == 0 || stride == elemSize) {
            val dup = bin.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            dup.position(base)
            when (ct) {
                5126 -> dup.asFloatBuffer().get(out)
                5125 -> for (k in out.indices) out[k] = dup.getInt(base + k * 4).toLong().and(0xFFFFFFFFL).toFloat()
                5123 -> for (k in out.indices) out[k] = dup.getShort(base + k * 2).toInt().and(0xFFFF).toFloat()
                5121 -> for (k in out.indices) out[k] = dup.get(base + k).toInt().and(0xFF).toFloat()
                else -> throw GlbException("float accessor with componentType $ct")
            }
        } else {
            val dup = bin.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            var o = base
            var k = 0
            val per = componentCount(acc.getString("type"))
            while (k < n) {
                for (c in 0 until per) {
                    out[k++] = when (ct) {
                        5126 -> dup.getFloat(o + c * 4)
                        5123 -> dup.getShort(o + c * 2).toInt().and(0xFFFF).toFloat()
                        5121 -> dup.get(o + c).toInt().and(0xFF).toFloat()
                        else -> throw GlbException("interleaved accessor with componentType $ct")
                    }
                }
                o += stride
            }
        }
        return out
    }

    /** Indices as ints; 5121/5123/5125 are all normalised to Int here. */
    fun indices(index: Int): IntArray {
        val acc = accessor(index)
        val count = acc.getInt("count")
        val ct = acc.getInt("componentType")
        val base = baseOffset(acc)
        val dup = bin.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val out = IntArray(count)
        when (ct) {
            5125 -> for (k in 0 until count) out[k] = dup.getInt(base + k * 4)
            5123 -> for (k in 0 until count) out[k] = dup.getShort(base + k * 2).toInt().and(0xFFFF)
            5121 -> for (k in 0 until count) out[k] = dup.get(base + k).toInt().and(0xFF)
            else -> throw GlbException("index accessor with componentType $ct")
        }
        return out
    }

    fun count(index: Int): Int = accessor(index).getInt("count")
}
