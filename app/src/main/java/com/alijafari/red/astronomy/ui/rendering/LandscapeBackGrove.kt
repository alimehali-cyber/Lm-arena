package com.alijafari.red.astronomy.ui.rendering

/**
 * The far-bank back grove: the trees that stand behind the rest of the valley's far-bank
 * plantings, at the feet of Persepolis on the left and Mil-e Azhdaha on the right.
 *
 * Two staggered rows of twenty slots, alternating species, so the grove adds exactly
 * [PER_SPECIES] Paulownias and [PER_SPECIES] sugar maples — the "20 more of each" the landscape
 * asked for — plus [DEEP_PAULOWNIA] **big** Paulownias standing deeper still, at the back of the
 * forest. Nothing about the rendering changes to carry them: every row is the *same cached sprites*
 * every other planting blits ([PaulowniaSprite], [SugarMapleSprite]), so a slot costs one mirrored,
 * haze-faded bitmap draw on the vegetation replay that already runs, and the rows ask for no cast
 * shadow (a shadow ellipse at that distance would be invisible, and skipping it keeps the shadow
 * pass at its old length). There is no per-tree geometry, no extra bake, no new texture and no new
 * GL work.
 *
 * Heights are set so the band reads: the row nearer the river is the tallest of the two (it is
 * nearer), the row behind it a little smaller, and the six deep Paulownias taller than both — they
 * are the big trees of the far-bank woodland, seen at the back of the forest rather than as
 * saplings at its edge.
 *
 * The vertical placement is *not* in here: [SceneGroundRenderer] resolves each slot's y from the
 * river's own centreline and half-width, so the grove always sits behind the existing far-bank
 * trees and always clear of the water, at every aspect ratio. This object owns the deterministic
 * plan — where along the bank, which species, how tall, how hazy — so the counts are a contract a
 * JVM test can hold instead of a comment.
 */
internal object LandscapeBackGrove {

    /** How many plantings of *each* species the grove adds (20 Paulownia + 20 sugar maple). */
    const val PER_SPECIES = 20

    /** Rows of slots, back row first. */
    const val ROWS = 2

    /** The big Paulownias planted deeper than either grove row ("deep into the forest"). */
    const val DEEP_PAULOWNIA = 6

    /**
     * Each grove row's foot line, as a depth below the monuments' toe: back row first.
     *
     * Shared with [SceneGroundRenderer] so the deep row can be held behind the nearest one at any
     * canvas aspect — where wide water forces the rows up toward the toe, the rows would otherwise
     * meet and the "deep" trees could end up in front of them.
     */
    val rowToeOffsets: List<Float> = listOf(0.026f, 0.036f)

    /**
     * One planned planting.
     *
     * @param xFraction trunk foot x as a fraction of the canvas width.
     * @param heightFraction the design's tree height as a fraction of the canvas height — small,
     *   because these are the farthest trees in the valley.
     * @param aerial aerial perspective: how far the sprite is faded into the haze. The farther
     *   row is hazier and smaller than the nearer one, which is what gives the band its depth.
     * @param toeOffset how far below the monuments' toe the row's feet stand, as a fraction of the
     *   canvas height: the nearer row sits lower (closer to the viewer).
     * @param paulownia true for a Paulownia, false for a sugar maple.
     */
    internal class Slot(
        val xFraction: Float,
        val heightFraction: Float,
        val aerial: Float,
        val toeOffset: Float,
        val paulownia: Boolean
    )

    /**
     * The grove plan, back row first.
     *
     * Slots are spread across the whole width (≈0.08 … 0.95 after the jitter) so the band frames
     * *both* monuments and the plain between them, species alternate within a row so neither reads
     * as a plantation, and the nearer row is offset by half a slot so the two rows interleave. The
     * jitter is the landscape's usual deterministic hash, so the grove is identical on every draw
     * and every device — a fixed piece of the composition, not a random scatter.
     */
    val slots: List<Slot> = buildList {
        // Row 0 is the back row: farthest, smallest, haziest.
        val rowXSeed = intArrayOf(1283, 1301)
        val rowHeightSeed = intArrayOf(1291, 1307)
        // The nearer row (row 1) is also the less hazed one: aerial perspective is what keeps the
        // back row behind it even where their crowns meet.
        val rowAerial = floatArrayOf(0.92f, 0.84f)
        // Deep enough to stand behind every other far-bank tree (the riparian gallery's feet start
        // 0.047–0.061 h below the toe).
        val rowToeOffset = rowToeOffsets.toFloatArray()
        // Heights: the row nearer the river was too small when the band was first planted — its
        // trees read as scrub beside the gallery. They are now two thirds of a gallery tree
        // (0.030–0.040 h against the gallery's 0.030–0.048 h), and the back row is 0.020–0.026 h,
        // so the two rows still step down with distance instead of matching it.
        val rowBaseHeight = floatArrayOf(0.020f, 0.030f)
        val rowHeightSpread = floatArrayOf(0.006f, 0.010f)
        for (row in 0 until ROWS) {
            for (i in 0 until PER_SPECIES) {
                // The nearer row is offset by half a slot, so its trunks stand in the gaps of the
                // row behind it instead of directly in front of them — the depth cue that makes two
                // rows read as a grove rather than as one thick treeline.
                val slot = (i + 0.5f + 0.5f * row) / PER_SPECIES
                val x = 0.055f + 0.89f * slot +
                    (LandscapeProfile.hash01(rowXSeed[row], i * 7 + 1) - 0.5f) * 0.012f
                val hgt = rowBaseHeight[row] + rowHeightSpread[row] *
                    LandscapeProfile.hash01(rowHeightSeed[row], i * 11 + 5)
                add(
                    Slot(
                        xFraction = x,
                        heightFraction = hgt,
                        aerial = rowAerial[row],
                        toeOffset = rowToeOffset[row],
                        // Alternate the species along the row, offset per row so the two rows do
                        // not stack one species behind the other.
                        paulownia = (i + row) % 2 == 0
                    )
                )
            }
        }
    }

    /**
     * The six big Paulownias at the back of the far-bank forest.
     *
     * "Way behind the far bank" means higher on the canvas than either grove row, and *big* means
     * the tallest trees on that side of the river: 0.048–0.058 h before the landscape's global tree
     * scale, which is the near-bank frame trees' order of size, against the grove's 0.020–0.040 h.
     * Their crowns therefore rise over the whole band — the tall trees of the woodland, seen at its
     * back — instead of being blurs at the horizon. They are planted through the same two-sprite
     * pipeline, with no cast shadow, like the rest of the grove.
     */
    val deepSlots: List<Slot> = buildList {
        for (i in 0 until DEEP_PAULOWNIA) {
            // Spread across the bank rather than clumped: 0.12 … 0.88, jittered by the same hash.
            val x = 0.12f + 0.76f * (i + 0.5f) / DEEP_PAULOWNIA +
                (LandscapeProfile.hash01(1319, i * 7 + 3) - 0.5f) * 0.030f
            add(
                Slot(
                    xFraction = x,
                    heightFraction = 0.048f + 0.010f * LandscapeProfile.hash01(1321, i * 11 + 5),
                    aerial = 0.90f,
                    // Deeper than either grove row: nearer the monuments' toe, farther from the
                    // water, so the grove stands in front of them.
                    toeOffset = 0.014f + 0.010f * LandscapeProfile.hash01(1327, i * 5 + 1),
                    paulownia = true
                )
            )
        }
    }

    /** How many Paulownia slots the grove contains (equals [PER_SPECIES], deep row excluded). */
    val paulowniaCount: Int get() = slots.count { it.paulownia }

    /** How many sugar maple slots the grove contains (equals [PER_SPECIES]). */
    val sugarMapleCount: Int get() = slots.size - paulowniaCount
}
