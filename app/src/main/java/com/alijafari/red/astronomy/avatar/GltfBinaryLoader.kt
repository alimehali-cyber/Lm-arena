package com.alijafari.red.astronomy.avatar

import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class SparseMorphTarget(
    val vertexIndices: IntArray,
    val positionDeltas: FloatArray,
)

data class MeshPrimitiveData(
    val name: String,
    val positions: FloatArray,
    val normals: FloatArray,
    val uvs: FloatArray,
    val joints: IntArray,
    val weights: FloatArray,
    val indices: IntArray,
    val morphTargets: List<SparseMorphTarget>,
) {
    val vertexCount: Int get() = positions.size / 3

    /** Applies all active morph targets into [outPositions] with zero heap allocation. */
    fun applyMorphWeights(morphWeights: FloatArray, outPositions: FloatArray) {
        positions.copyInto(outPositions)
        val count = minOf(morphTargets.size, morphWeights.size)
        for (m in 0 until count) {
            val w = morphWeights[m]
            if (w > -1e-4f && w < 1e-4f) continue
            val target = morphTargets[m]
            val vIdx = target.vertexIndices
            val deltas = target.positionDeltas
            for (k in vIdx.indices) {
                val dst = vIdx[k] * 3
                val src = k * 3
                outPositions[dst] += deltas[src] * w
                outPositions[dst + 1] += deltas[src + 1] * w
                outPositions[dst + 2] += deltas[src + 2] * w
            }
        }
    }
}

data class NodeData(
    val index: Int,
    val name: String,
    val children: List<Int>,
    var parent: Int = -1,
    val baseTranslation: FloatArray,
    val baseRotation: FloatArray,
    val baseScale: FloatArray,
    val meshIndex: Int?,
)

enum class ChannelPath { TRANSLATION, ROTATION, SCALE, WEIGHTS }

data class AnimSamplerData(
    val times: FloatArray,
    val values: FloatArray,
    val stride: Int,
)

data class AnimChannelData(
    val nodeIndex: Int,
    val path: ChannelPath,
    val sampler: AnimSamplerData,
)

data class AnimationClipData(
    val name: String,
    val durationSec: Float,
    val channels: List<AnimChannelData>,
)

data class AvatarModelAsset(
    val primitives: List<MeshPrimitiveData>,
    val nodes: List<NodeData>,
    val jointNodeIndices: List<Int>,
    val inverseBindMatrices: List<FloatArray>,
    val animations: Map<String, AnimationClipData>,
    val morphTargetCount: Int,
    val albedoPngBytes: ByteArray?,
    val ormPngBytes: ByteArray?,
    val normalPngBytes: ByteArray?,
) {
    val totalTriangles: Int get() = primitives.sumOf { it.indices.size / 3 }
}

/**
 * Pure-Kotlin zero-dependency glTF 2.0 binary (`.glb`) loader with compact sparse morph-target
 * extraction and embedded PNG texture extraction (`Java 11` & Android `minSdk 24` compatible).
 */
object GltfBinaryLoader {

    fun load(glbFile: File): AvatarModelAsset = loadBytes(glbFile.readBytes())

    fun load(stream: InputStream): AvatarModelAsset = stream.use { loadBytes(it.readBytes()) }

    fun loadBytes(bytes: ByteArray): AvatarModelAsset {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buf.int
        require(magic == 0x46546C67) { "Invalid GLB magic header: 0x${magic.toString(16)}" }
        val version = buf.int
        require(version == 2) { "Unsupported GLB version: $version" }
        val totalLen = buf.int

        var jsonText = ""
        var binChunk = ByteArray(0)
        while (buf.position() + 8 <= totalLen && buf.remaining() >= 8) {
            val chunkLen = buf.int
            val chunkType = buf.int
            val slice = ByteArray(chunkLen)
            buf.get(slice)
            if (chunkType == 0x4E4F534A) {
                jsonText = slice.toString(Charsets.UTF_8)
            } else if (chunkType == 0x004E4942) {
                binChunk = slice
            }
        }

        val root = MiniJson.parse(jsonText) as Map<*, *>
        val bufferViews = (root["bufferViews"] as? List<*>)?.map { it as Map<*, *> } ?: emptyList()
        val accessors = (root["accessors"] as? List<*>)?.map { it as Map<*, *> } ?: emptyList()
        val textures = (root["textures"] as? List<*>)?.map { it as Map<*, *> } ?: emptyList()
        val images = (root["images"] as? List<*>)?.map { it as Map<*, *> } ?: emptyList()
        val materials = (root["materials"] as? List<*>)?.map { it as Map<*, *> } ?: emptyList()
        val bin = ByteBuffer.wrap(binChunk).order(ByteOrder.LITTLE_ENDIAN)

        fun readBufferViewBytes(bvIdx: Int): ByteArray? {
            if (bvIdx !in bufferViews.indices) return null
            val bv = bufferViews[bvIdx]
            val off = (bv["byteOffset"] as? Number)?.toInt() ?: 0
            val len = (bv["byteLength"] as? Number)?.toInt() ?: return null
            if (off < 0 || off + len > binChunk.size) return null
            return binChunk.copyOfRange(off, off + len)
        }

        fun extractMaterialTextureBytes(texInfo: Map<*, *>?): ByteArray? {
            val texIdx = (texInfo?.get("index") as? Number)?.toInt() ?: return null
            val tex = textures.getOrNull(texIdx) ?: return null
            val imgIdx = (tex["source"] as? Number)?.toInt() ?: return null
            val img = images.getOrNull(imgIdx) ?: return null
            val bvIdx = (img["bufferView"] as? Number)?.toInt() ?: return null
            return readBufferViewBytes(bvIdx)
        }

        val mat0 = materials.firstOrNull()
        val pbr = mat0?.get("pbrMetallicRoughness") as? Map<*, *>
        val albedoBytes = extractMaterialTextureBytes(pbr?.get("baseColorTexture") as? Map<*, *>)
        val ormBytes = extractMaterialTextureBytes(pbr?.get("metallicRoughnessTexture") as? Map<*, *>)
        val normalBytes = extractMaterialTextureBytes(mat0?.get("normalTexture") as? Map<*, *>)

        fun componentCount(type: String): Int = when (type) {
            "SCALAR" -> 1
            "VEC2" -> 2
            "VEC3" -> 3
            "VEC4" -> 4
            "MAT4" -> 16
            else -> error("Unsupported glTF accessor type: $type")
        }

        fun componentBytes(compType: Int): Int = when (compType) {
            5120, 5121 -> 1
            5122, 5123 -> 2
            5125, 5126 -> 4
            else -> error("Unsupported glTF componentType: $compType")
        }

        fun readScalarAsFloat(bb: ByteBuffer, offset: Int, compType: Int): Float = when (compType) {
            5126 -> bb.getFloat(offset)
            5125 -> bb.getInt(offset).toLong().and(0xFFFF_FFFFL).toFloat()
            5123 -> bb.getShort(offset).toInt().and(0xFFFF).toFloat()
            5122 -> bb.getShort(offset).toFloat()
            5121 -> bb.get(offset).toInt().and(0xFF).toFloat()
            5120 -> bb.get(offset).toFloat()
            else -> error("Unsupported compType $compType")
        }

        fun readScalarAsInt(bb: ByteBuffer, offset: Int, compType: Int): Int = when (compType) {
            5125 -> bb.getInt(offset)
            5123 -> bb.getShort(offset).toInt().and(0xFFFF)
            5122 -> bb.getShort(offset).toInt()
            5121 -> bb.get(offset).toInt().and(0xFF)
            5120 -> bb.get(offset).toInt()
            else -> bb.getFloat(offset).toInt()
        }

        fun readFloatAccessor(accIdx: Int): FloatArray {
            val acc = accessors[accIdx]
            val count = (acc["count"] as Number).toInt()
            val comps = componentCount(acc["type"] as String)
            val compType = (acc["componentType"] as Number).toInt()
            val cBytes = componentBytes(compType)
            val out = FloatArray(count * comps)

            val bvIdx = (acc["bufferView"] as? Number)?.toInt()
            if (bvIdx != null) {
                val bv = bufferViews[bvIdx]
                val baseOff = ((bv["byteOffset"] as? Number)?.toInt() ?: 0) + ((acc["byteOffset"] as? Number)?.toInt() ?: 0)
                val stride = ((bv["byteStride"] as? Number)?.toInt()) ?: (comps * cBytes)
                for (i in 0 until count) {
                    val elemOff = baseOff + i * stride
                    for (c in 0 until comps) {
                        out[i * comps + c] = readScalarAsFloat(bin, elemOff + c * cBytes, compType)
                    }
                }
            }

            val sparse = acc["sparse"] as? Map<*, *>
            if (sparse != null) {
                val sCount = (sparse["count"] as Number).toInt()
                val idxMap = sparse["indices"] as Map<*, *>
                val valMap = sparse["values"] as Map<*, *>
                val idxBv = bufferViews[(idxMap["bufferView"] as Number).toInt()]
                val idxCompType = (idxMap["componentType"] as Number).toInt()
                val idxBytes = componentBytes(idxCompType)
                val idxBase = ((idxBv["byteOffset"] as? Number)?.toInt() ?: 0) + ((idxMap["byteOffset"] as? Number)?.toInt() ?: 0)

                val valBv = bufferViews[(valMap["bufferView"] as Number).toInt()]
                val valBase = ((valBv["byteOffset"] as? Number)?.toInt() ?: 0) + ((valMap["byteOffset"] as? Number)?.toInt() ?: 0)
                val valStride = ((valBv["byteStride"] as? Number)?.toInt()) ?: (comps * cBytes)

                for (s in 0 until sCount) {
                    val targetElem = readScalarAsInt(bin, idxBase + s * idxBytes, idxCompType)
                    val vOff = valBase + s * valStride
                    for (c in 0 until comps) {
                        out[targetElem * comps + c] = readScalarAsFloat(bin, vOff + c * cBytes, compType)
                    }
                }
            }
            return out
        }

        fun readSparseMorphAccessor(accIdx: Int): SparseMorphTarget {
            val acc = accessors[accIdx]
            val compType = (acc["componentType"] as Number).toInt()
            val cBytes = componentBytes(compType)
            val sparse = acc["sparse"] as? Map<*, *>
            if (sparse != null && acc["bufferView"] == null) {
                val sCount = (sparse["count"] as Number).toInt()
                val idxMap = sparse["indices"] as Map<*, *>
                val valMap = sparse["values"] as Map<*, *>
                val idxBv = bufferViews[(idxMap["bufferView"] as Number).toInt()]
                val idxCompType = (idxMap["componentType"] as Number).toInt()
                val idxBytes = componentBytes(idxCompType)
                val idxBase = ((idxBv["byteOffset"] as? Number)?.toInt() ?: 0) + ((idxMap["byteOffset"] as? Number)?.toInt() ?: 0)

                val valBv = bufferViews[(valMap["bufferView"] as Number).toInt()]
                val valBase = ((valBv["byteOffset"] as? Number)?.toInt() ?: 0) + ((valMap["byteOffset"] as? Number)?.toInt() ?: 0)
                val valStride = ((valBv["byteStride"] as? Number)?.toInt()) ?: (3 * cBytes)

                val indices = IntArray(sCount)
                val deltas = FloatArray(sCount * 3)
                for (s in 0 until sCount) {
                    indices[s] = readScalarAsInt(bin, idxBase + s * idxBytes, idxCompType)
                    val vOff = valBase + s * valStride
                    deltas[s * 3] = readScalarAsFloat(bin, vOff, compType)
                    deltas[s * 3 + 1] = readScalarAsFloat(bin, vOff + cBytes, compType)
                    deltas[s * 3 + 2] = readScalarAsFloat(bin, vOff + 2 * cBytes, compType)
                }
                return SparseMorphTarget(indices, deltas)
            }
            val dense = readFloatAccessor(accIdx)
            val vCount = dense.size / 3
            var nonZero = 0
            for (v in 0 until vCount) {
                val dx = dense[v * 3]; val dy = dense[v * 3 + 1]; val dz = dense[v * 3 + 2]
                if (dx * dx + dy * dy + dz * dz > 1e-12f) nonZero++
            }
            val indices = IntArray(nonZero)
            val deltas = FloatArray(nonZero * 3)
            var cursor = 0
            for (v in 0 until vCount) {
                val dx = dense[v * 3]; val dy = dense[v * 3 + 1]; val dz = dense[v * 3 + 2]
                if (dx * dx + dy * dy + dz * dz > 1e-12f) {
                    indices[cursor] = v
                    deltas[cursor * 3] = dx
                    deltas[cursor * 3 + 1] = dy
                    deltas[cursor * 3 + 2] = dz
                    cursor++
                }
            }
            return SparseMorphTarget(indices, deltas)
        }

        fun readIntAccessor(accIdx: Int): IntArray {
            val acc = accessors[accIdx]
            val count = (acc["count"] as Number).toInt()
            val comps = componentCount(acc["type"] as String)
            val compType = (acc["componentType"] as Number).toInt()
            val cBytes = componentBytes(compType)
            val out = IntArray(count * comps)
            val bvIdx = (acc["bufferView"] as? Number)?.toInt() ?: return out
            val bv = bufferViews[bvIdx]
            val baseOff = ((bv["byteOffset"] as? Number)?.toInt() ?: 0) + ((acc["byteOffset"] as? Number)?.toInt() ?: 0)
            val stride = ((bv["byteStride"] as? Number)?.toInt()) ?: (comps * cBytes)
            for (i in 0 until count) {
                val elemOff = baseOff + i * stride
                for (c in 0 until comps) {
                    out[i * comps + c] = readScalarAsInt(bin, elemOff + c * cBytes, compType)
                }
            }
            return out
        }

        val meshesJson = (root["meshes"] as? List<*>)?.map { it as Map<*, *> } ?: emptyList()
        val primitives = mutableListOf<MeshPrimitiveData>()
        var maxMorphCount = 0

        for (m in meshesJson) {
            val mName = (m["name"] as? String) ?: "Mesh"
            val prims = (m["primitives"] as? List<*>)?.map { it as Map<*, *> } ?: emptyList()
            for (p in prims) {
                val attrs = p["attributes"] as Map<*, *>
                val pos = readFloatAccessor((attrs["POSITION"] as Number).toInt())
                val vCount = pos.size / 3
                val nrm = (attrs["NORMAL"] as? Number)?.let { readFloatAccessor(it.toInt()) } ?: FloatArray(vCount * 3)
                val uv = (attrs["TEXCOORD_0"] as? Number)?.let { readFloatAccessor(it.toInt()) } ?: FloatArray(vCount * 2)
                val jnt = (attrs["JOINTS_0"] as? Number)?.let { readIntAccessor(it.toInt()) } ?: IntArray(vCount * 4)
                val wgt = (attrs["WEIGHTS_0"] as? Number)?.let { readFloatAccessor(it.toInt()) } ?: FloatArray(vCount * 4)
                val idx = (p["indices"] as? Number)?.let { readIntAccessor(it.toInt()) } ?: IntArray(vCount) { it }

                val targets = (p["targets"] as? List<*>)?.map { it as Map<*, *> } ?: emptyList()
                maxMorphCount = maxOf(maxMorphCount, targets.size)
                val sparseTargets = targets.map { t ->
                    val posIdx = (t["POSITION"] as? Number)?.toInt()
                    if (posIdx != null) readSparseMorphAccessor(posIdx) else SparseMorphTarget(IntArray(0), FloatArray(0))
                }
                primitives.add(MeshPrimitiveData(mName, pos, nrm, uv, jnt, wgt, idx, sparseTargets))
            }
        }

        val nodesJson = (root["nodes"] as? List<*>)?.map { it as Map<*, *> } ?: emptyList()
        val nodes = nodesJson.mapIndexed { idx, n ->
            val tr = (n["translation"] as? List<*>)?.map { (it as Number).toFloat() }?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f)
            val rot = (n["rotation"] as? List<*>)?.map { (it as Number).toFloat() }?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f, 1f)
            val scl = (n["scale"] as? Number)?.let { floatArrayOf(it.toFloat(), it.toFloat(), it.toFloat()) }
                ?: (n["scale"] as? List<*>)?.map { (it as Number).toFloat() }?.toFloatArray()
                ?: floatArrayOf(1f, 1f, 1f)
            val kids = (n["children"] as? List<*>)?.map { (it as Number).toInt() } ?: emptyList()
            NodeData(idx, (n["name"] as? String) ?: "Node_$idx", kids, -1, tr, rot, scl, (n["mesh"] as? Number)?.toInt())
        }
        for (n in nodes) {
            for (c in n.children) nodes[c].parent = n.index
        }

        val skinsJson = (root["skins"] as? List<*>)?.map { it as Map<*, *> } ?: emptyList()
        val jointNodeIndices: List<Int>
        val ibms: List<FloatArray>
        if (skinsJson.isNotEmpty()) {
            val s0 = skinsJson[0]
            jointNodeIndices = (s0["joints"] as List<*>).map { (it as Number).toInt() }
            val ibmFlat = readFloatAccessor((s0["inverseBindMatrices"] as Number).toInt())
            ibms = List(jointNodeIndices.size) { j -> ibmFlat.copyOfRange(j * 16, (j + 1) * 16) }
        } else {
            jointNodeIndices = emptyList()
            ibms = emptyList()
        }

        val animsJson = (root["animations"] as? List<*>)?.map { it as Map<*, *> } ?: emptyList()
        val animations = linkedMapOf<String, AnimationClipData>()
        for ((aIdx, a) in animsJson.withIndex()) {
            val aName = (a["name"] as? String) ?: "Anim_$aIdx"
            val samplersJson = (a["samplers"] as List<*>).map { it as Map<*, *> }
            val channelsJson = (a["channels"] as List<*>).map { it as Map<*, *> }
            var maxDur = 0f
            val channels = mutableListOf<AnimChannelData>()

            for (ch in channelsJson) {
                val target = ch["target"] as Map<*, *>
                val nodeIdx = (target["node"] as? Number)?.toInt() ?: continue
                val pathStr = target["path"] as String
                val path = when (pathStr) {
                    "translation" -> ChannelPath.TRANSLATION
                    "rotation" -> ChannelPath.ROTATION
                    "scale" -> ChannelPath.SCALE
                    "weights" -> ChannelPath.WEIGHTS
                    else -> continue
                }
                val sMap = samplersJson[(ch["sampler"] as Number).toInt()]
                val times = readFloatAccessor((sMap["input"] as Number).toInt())
                val vals = readFloatAccessor((sMap["output"] as Number).toInt())
                if (times.isNotEmpty()) maxDur = maxOf(maxDur, times.last())
                val stride = if (times.isNotEmpty()) vals.size / times.size else 1
                channels.add(AnimChannelData(nodeIdx, path, AnimSamplerData(times, vals, stride)))
            }
            animations[aName] = AnimationClipData(aName, maxDur, channels)
        }

        return AvatarModelAsset(
            primitives = primitives,
            nodes = nodes,
            jointNodeIndices = jointNodeIndices,
            inverseBindMatrices = ibms,
            animations = animations,
            morphTargetCount = maxMorphCount,
            albedoPngBytes = albedoBytes,
            ormPngBytes = ormBytes,
            normalPngBytes = normalBytes,
        )
    }
}

/** Minimal zero-dependency RFC-8259 JSON parser for pure-Kotlin standalone & Android use. */
object MiniJson {
    fun parse(src: String): Any? = Parser(src).parseValue()

    private class Parser(private val s: String) {
        private var i = 0
        private fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun parseValue(): Any? {
            skipWs()
            return when (val c = s[i]) {
                '{' -> parseObj()
                '[' -> parseArr()
                '"' -> parseStr()
                't' -> {
                    i += 4
                    true
                }
                'f' -> {
                    i += 5
                    false
                }
                'n' -> {
                    i += 4
                    null
                }
                else -> if (c == '-' || c.isDigit()) parseNum() else error("Unexpected JSON char '$c' at $i")
            }
        }

        private fun parseObj(): Map<String, Any?> {
            val map = LinkedHashMap<String, Any?>()
            i++
            skipWs()
            if (i < s.length && s[i] == '}') {
                i++
                return map
            }
            while (i < s.length) {
                skipWs()
                val k = parseStr()
                skipWs()
                check(s[i] == ':') { "Expected ':' at $i" }
                i++
                val v = parseValue()
                map[k] = v
                skipWs()
                if (s[i] == ',') {
                    i++
                } else if (s[i] == '}') {
                    i++
                    break
                }
            }
            return map
        }

        private fun parseArr(): List<Any?> {
            val list = ArrayList<Any?>()
            i++
            skipWs()
            if (i < s.length && s[i] == ']') {
                i++
                return list
            }
            while (i < s.length) {
                list.add(parseValue())
                skipWs()
                if (s[i] == ',') {
                    i++
                } else if (s[i] == ']') {
                    i++
                    break
                }
            }
            return list
        }

        private fun parseStr(): String {
            check(s[i] == '"') { "Expected '\"' at $i" }
            i++
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i++]
                if (c == '"') return sb.toString()
                if (c == '\\') {
                    when (val esc = s[i++]) {
                        '"', '\\', '/' -> sb.append(esc)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = s.substring(i, i + 4)
                            sb.append(hex.toInt(16).toChar())
                            i += 4
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }

        private fun parseNum(): Number {
            val start = i
            if (s[i] == '-') i++
            while (i < s.length && s[i].isDigit()) i++
            var isFloat = false
            if (i < s.length && s[i] == '.') {
                isFloat = true
                i++
                while (i < s.length && s[i].isDigit()) i++
            }
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                isFloat = true
                i++
                if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                while (i < s.length && s[i].isDigit()) i++
            }
            val sub = s.substring(start, i)
            return if (isFloat) sub.toDouble() else sub.toLong()
        }
    }
}
