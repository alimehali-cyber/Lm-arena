package com.zig.avatar

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The subset of glTF this character needs, decoded once at load time.
 *
 * Everything is stored in flat arrays: a phone should not allocate per vertex, and the
 * renderer's per-frame work should be arithmetic over arrays, not JSON lookups.
 */
class AvatarModel private constructor(
    val jointCount: Int,
    val jointNames: Array<String>,
    val parentOf: IntArray,             // -1 for roots
    val nodeOfJoint: IntArray,          // joint index -> glTF node index
    val inverseBind: FloatArray,        // 16 floats per joint, column major
    val restLocal: FloatArray,          // 10 floats per joint: t(3) r(4) s(3)
    val primitives: List<Primitive>,
    val morphNames: Array<String>,
    val clips: Map<String, Clip>,
    val bounds: FloatArray,             // x0 y0 z0 x1 y1 z1 in model space
) {

    /** One draw call: a material and the vertex/index data that shares it. */
    class Primitive(
        val positions: FloatArray,
        val normals: FloatArray,
        val joints: IntArray,           // 4 per vertex
        val weights: FloatArray,        // 4 per vertex, sums to 1
        val indices: IntArray,
        val morphDeltas: Array<FloatArray>, // [target][vertex*3], POSITION only
        val material: Material,
    ) {
        val vertexCount: Int get() = positions.size / 3
    }

    class Material(val name: String, val rgba: FloatArray, val roughness: Float, val metallic: Float)

    /** A channel targets either a node TRS or the mesh morph weights. */
    class Sampler(
        val times: FloatArray,
        val values: FloatArray,         // stride depends on path
        val stride: Int,
        val step: Boolean,
    )

    class Channel(val node: Int, val path: String, val sampler: Sampler)

    class Clip(val name: String, val duration: Float, val channels: List<Channel>)

    companion object {
        private const val PATH_T = "translation"
        private const val PATH_R = "rotation"
        private const val PATH_S = "scale"
        private const val PATH_W = "weights"

        fun load(bytes: ByteArray): AvatarModel {
            val glb = Glb.parse(bytes)
            val js = glb.json
            val nodes = js.getJSONArray("nodes")

            val skins = js.optJSONArray("skins") ?: throw GlbException("no skin")
            if (skins.length() != 1) throw GlbException("expected 1 skin, found ${skins.length()}")
            val skin = skins.getJSONObject(0)
            val jointArray = skin.getJSONArray("joints")
            val jointCount = jointArray.length()
            val jointNames = Array(jointCount) { i -> nodes.getJSONObject(jointArray.getInt(i)).optString("name", "joint$i") }
            val nodeOfJoint = IntArray(jointCount) { i -> jointArray.getInt(i) }

            val ibm = glb.floats(skin.getInt("inverseBindMatrices"))
            if (ibm.size != jointCount * 16) throw GlbException("inverse bind matrix count mismatch")

            // Parent map from the node tree, then mapped into joint space.
            val nodeParent = IntArray(nodes.length()) { -1 }
            for (i in 0 until nodes.length()) {
                val children = nodes.getJSONObject(i).optJSONArray("children") ?: continue
                for (c in 0 until children.length()) nodeParent[children.getInt(c)] = i
            }
            val jointIndexOfNode = HashMap<Int, Int>(jointCount * 2)
            for (j in 0 until jointCount) jointIndexOfNode[nodeOfJoint[j]] = j
            val parentOf = IntArray(jointCount) { j ->
                var p = nodeParent[nodeOfJoint[j]]
                while (p >= 0 && !jointIndexOfNode.containsKey(p)) p = nodeParent[p]
                if (p >= 0) jointIndexOfNode[p]!! else -1
            }

            val restLocal = FloatArray(jointCount * 10)
            for (j in 0 until jointCount) {
                val n = nodes.getJSONObject(nodeOfJoint[j])
                val t = n.optJSONArray("translation")
                val r = n.optJSONArray("rotation")
                val s = n.optJSONArray("scale")
                val o = j * 10
                restLocal[o + 0] = t?.optDouble(0, 0.0)?.toFloat() ?: 0f
                restLocal[o + 1] = t?.optDouble(1, 0.0)?.toFloat() ?: 0f
                restLocal[o + 2] = t?.optDouble(2, 0.0)?.toFloat() ?: 0f
                restLocal[o + 3] = r?.optDouble(0, 0.0)?.toFloat() ?: 0f
                restLocal[o + 4] = r?.optDouble(1, 0.0)?.toFloat() ?: 0f
                restLocal[o + 5] = r?.optDouble(2, 0.0)?.toFloat() ?: 0f
                restLocal[o + 6] = r?.optDouble(3, 1.0)?.toFloat() ?: 1f
                restLocal[o + 7] = s?.optDouble(0, 1.0)?.toFloat() ?: 1f
                restLocal[o + 8] = s?.optDouble(1, 1.0)?.toFloat() ?: 1f
                restLocal[o + 9] = s?.optDouble(2, 1.0)?.toFloat() ?: 1f
            }

            val materials = ArrayList<Material>()
            val mats = js.optJSONArray("materials")
            if (mats != null) {
                for (i in 0 until mats.length()) {
                    val m = mats.getJSONObject(i)
                    val pbr = m.optJSONObject("pbrMetallicRoughness") ?: JSONObject()
                    val c = pbr.optJSONArray("baseColorFactor")
                    val rgba = floatArrayOf(
                        c?.optDouble(0, 1.0)?.toFloat() ?: 1f,
                        c?.optDouble(1, 1.0)?.toFloat() ?: 1f,
                        c?.optDouble(2, 1.0)?.toFloat() ?: 1f,
                        c?.optDouble(3, 1.0)?.toFloat() ?: 1f,
                    )
                    materials.add(Material(m.optString("name", "mat$i"), rgba,
                        pbr.optDouble("roughnessFactor", 0.6).toFloat(),
                        pbr.optDouble("metallicFactor", 0.0).toFloat()))
                }
            }

            val prims = ArrayList<Primitive>()
            var morphNames: Array<String> = emptyArray()
            val meshes = js.getJSONArray("meshes")
            for (mi in 0 until meshes.length()) {
                val mesh = meshes.getJSONObject(mi)
                val names = mesh.optJSONObject("extras")?.optJSONArray("targetNames")
                if (names != null && names.length() > 0) {
                    morphNames = Array(names.length()) { names.getString(it) }
                }
                val arr = mesh.getJSONArray("primitives")
                for (pi in 0 until arr.length()) {
                    val p = arr.getJSONObject(pi)
                    val attrs = p.getJSONObject("attributes")
                    for (req in listOf("POSITION", "NORMAL", "JOINTS_0", "WEIGHTS_0")) {
                        if (!attrs.has(req)) throw GlbException("primitive $mi/$pi lacks $req")
                    }
                    val pos = glb.floats(attrs.getInt("POSITION"))
                    val nor = glb.floats(attrs.getInt("NORMAL"))
                    val jf = glb.floats(attrs.getInt("JOINTS_0"))
                    val wf = glb.floats(attrs.getInt("WEIGHTS_0"))
                    val joints = IntArray(jf.size) { jf[it].toInt() }
                    val idx = if (p.has("indices")) glb.indices(p.getInt("indices"))
                    else IntArray(pos.size / 3) { it }

                    val targets = ArrayList<FloatArray>()
                    val ta = p.optJSONArray("targets")
                    if (ta != null) {
                        for (t in 0 until ta.length()) {
                            val tj = ta.getJSONObject(t)
                            val acc = tj.optInt("POSITION", -1)
                            if (acc < 0) throw GlbException("morph target without POSITION")
                            val d = glb.floats(acc)
                            if (d.size != pos.size) throw GlbException("morph delta size mismatch")
                            targets.add(d)
                        }
                    }
                    val mat = if (p.has("material")) materials[p.getInt("material")]
                    else Material("default", floatArrayOf(0.8f, 0.8f, 0.8f, 1f), 0.6f, 0f)
                    prims.add(Primitive(pos, nor, joints, wf, idx, targets.toTypedArray(), mat))
                }
            }

            // Bounds over every primitive in model space, used to frame the camera and to
            // place the character on the ground without guessing.
            var x0 = Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var z0 = Float.MAX_VALUE
            var x1 = -Float.MAX_VALUE; var y1 = -Float.MAX_VALUE; var z1 = -Float.MAX_VALUE
            for (p in prims) {
                var i = 0
                while (i < p.positions.size) {
                    val x = p.positions[i]; val y = p.positions[i + 1]; val z = p.positions[i + 2]
                    if (x < x0) x0 = x; if (x > x1) x1 = x
                    if (y < y0) y0 = y; if (y > y1) y1 = y
                    if (z < z0) z0 = z; if (z > z1) z1 = z
                    i += 3
                }
            }

            val clips = HashMap<String, Clip>()
            val anims = js.optJSONArray("animations")
            if (anims != null) {
                for (ai in 0 until anims.length()) {
                    val a = anims.getJSONObject(ai)
                    val name = a.optString("name", "clip$ai")
                    val samplers = a.getJSONArray("samplers")
                    val chans = a.getJSONArray("channels")
                    val out = ArrayList<Channel>(chans.length())
                    var duration = 0f
                    for (ci in 0 until chans.length()) {
                        val ch = chans.getJSONObject(ci)
                        val target = ch.getJSONObject("target")
                        val path = target.getString("path")
                        if (path == "weights" && target.has("node")) {
                            // weights target the mesh node; keep the node for completeness
                        }
                        val s = samplers.getJSONObject(ch.getInt("sampler"))
                        val times = glb.floats(s.getInt("input"))
                        val values = glb.floats(s.getInt("output"))
                        val stride = when (path) {
                            PATH_T, PATH_S -> 3
                            PATH_R -> 4
                            PATH_W -> if (times.size > 0 && values.size % times.size == 0)
                                values.size / times.size else morphNames.size
                            else -> continue
                        }
                        val step = s.optString("interpolation", "LINEAR") == "STEP"
                        if (times.isNotEmpty() && times[times.size - 1] > duration) duration = times[times.size - 1]
                        out.add(Channel(if (target.has("node")) target.getInt("node") else -1, path,
                            Sampler(times, values, stride, step)))
                    }
                    clips[name] = Clip(name, duration, out)
                }
            }

            return AvatarModel(jointCount, jointNames, parentOf, nodeOfJoint, ibm, restLocal,
                prims, morphNames, clips, floatArrayOf(x0, y0, z0, x1, y1, z1))
        }
    }

    fun morphIndex(name: String): Int = morphNames.indexOf(name)

    fun hasClip(name: String): Boolean = clips.containsKey(name)
}
