package com.alijafari.red.astronomy.ui.rendering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contracts for the near bank's arrangement and the landscape's global tree scale.
 *
 * The near-bank row is a hand-placed composition — which tree stands where on the viewer's own
 * bank — and the plantings themselves are private to [SceneGroundRenderer], so the positions it
 * was re-arranged into are exposed as constants and held here: a move that is asked for once should
 * not quietly drift back in a later edit.
 */
class NearBankTreesTest {

    @Test
    fun everyTreeInTheValleyIsTenPercentBigger() {
        // Applied once, inside the two planters, so a planting's height, its cast shadow and the
        // sprite bake's tallest-tree sizing cannot disagree about it. These tests cannot run the
        // Compose draw, so this is the one place the factor itself is asserted.
        assertEquals("global tree scale", 1.10f, SceneGroundRenderer.TREE_HEIGHT_SCALE, 1e-6f)
    }

    @Test
    fun theRearrangedClusterIsMapleThenPaulowniaThenPaulownia() {
        // The maple that framed the left edge and the Paulownia that stood second from the left were
        // moved right, first beside one another and then beside the Paulownia at 0.34 W.
        val maple = SceneGroundRenderer.NEAR_BANK_MAPLE_X
        val paulownia = SceneGroundRenderer.NEAR_BANK_PAULOWNIA_MID_X
        assertTrue("maple left of the Paulownia ($maple < $paulownia)", maple < paulownia)
        assertTrue("both left of the 0.34 Paulownia ($paulownia < 0.34)", paulownia < 0.34f)
        // They stay a tree apart — distinct trunks, not one on top of the other — and both are on
        // the bank rather than in the bank frames' old corners.
        assertTrue("distinct trunks", paulownia - maple > 0.010f)
        assertTrue("clear of the frame corner", maple > 0.20f)
    }

    @Test
    fun theSecondTreeFromTheRightIsTheFarRightTreeOfTheScreen() {
        // The Paulownia that stood second from the right (0.78 W) is now the rightmost tree of the
        // near bank. Right of the 0.875 W frame, still on screen so the whole crown is visible.
        val right = SceneGroundRenderer.NEAR_BANK_RIGHT_X
        assertTrue("right of the frame ($right > 0.875)", right > 0.875f)
        assertTrue("on screen ($right)", right < 0.98f)
        // ...and nothing else on the bank stands further right.
        listOf(
            SceneGroundRenderer.NEAR_BANK_MAPLE_X,
            SceneGroundRenderer.NEAR_BANK_PAULOWNIA_MID_X,
            0.34f, 0.40f, 0.55f, 0.63f, 0.66f, 0.70f, 0.875f
        ).forEach { assertTrue("$it is left of $right", it < right) }
    }
}
