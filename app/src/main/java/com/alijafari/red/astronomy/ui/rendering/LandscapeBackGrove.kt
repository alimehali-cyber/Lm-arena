package com.alijafari.red.astronomy.ui.rendering

/**
 * The far-bank back grove: the trees that stand behind the rest of the valley's far-bank
 * plantings, at the feet of Persepolis on the left and Mil-e Azhdaha on the right.
 *
 * Two staggered rows of twenty slots, alternating species, so the grove adds exactly
 * [PER_SPECIES] Paulownias and [PER_SPECIES] sugar maples — the "20 more of each" the landscape
 * asked for. Nothing about the rendering changes to carry them: both rows are the *same cached
 * sprites* every other planting blits ([PaulowniaSprite], [SugarMapleSprite]), so a slot costs one
 * mirrored, haze-faded bitmap draw on the vegetation replay that already runs, and the rows ask for
 * no cast shadow (these are the smallest trees in the valley and are already mixed most of the way
 * into the haze; a shadow ellipse at that size would be invisible, and skipping it keeps the shadow
 * pass at its old length). There is no per-tree geometry, no extra bake, no new texture and no new
 * GL work.
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
        val rowAerial = floatArrayOf(0.94f, 0.88f)
        // Deep enough to stand behind every other far-bank tree (the riparian gallery's feet start
        // 0.047–0.061 h below the toe), shallow enough that even the tallest crown stays clear of
        // the monuments' own terrace line at 0.798 h.
        val rowToeOffset = floatArrayOf(0.026f, 0.036f)
        val rowBaseHeight = floatArrayOf(0.014f, 0.015f)
        val rowHeightSpread = floatArrayOf(0.007f, 0.008f)
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

    /** How many Paulownia slots the grove contains (equals [PER_SPECIES]). */
    val paulowniaCount: Int get() = slots.count { it.paulownia }

    /** How many sugar maple slots the grove contains (equals [PER_SPECIES]). */
    val sugarMapleCount: Int get() = slots.size - paulowniaCount
}
