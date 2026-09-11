package com.alijafari.red.astronomy

import android.Manifest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.domain.ObjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator smoke coverage for the AR Sky screen. This intentionally uses real Activity composition
 * so CameraX/sensor/GPS-null paths, detail sheets, catalog aggregation, and Time Machine UI are
 * exercised on an installed debug APK instead of only as JVM unit tests.
 */
@RunWith(AndroidJUnit4::class)
class ARSkySmokeInstrumentedTest {

    @get:Rule(order = 0)
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun arSkyCatalogDetailsAndTimeMachineSmoke() {
        waitForTag("main_bottom_navigation", timeoutMillis = 30_000L)
        // Let the splash overlay finish so the bottom navigation can receive the click.
        Thread.sleep(3_000L)
        composeRule.onNodeWithTag("nav_item_arsky", useUnmergedTree = true).performClick()
        waitForTag("ar_pill_search", timeoutMillis = 30_000L)

        assertCatalogCountsAndDoubleClusterResolution()

        openTargetDetailThroughArSearch(query = "Sirius", resultTag = "ar_search_result_star_cma_sirius")
        dismissDetailModal()

        openTargetDetailThroughArSearch(query = "Mars", resultTag = "ar_search_result_planet_mars")
        dismissDetailModal()

        openTargetDetailThroughArSearch(query = "M101", resultTag = "ar_search_result_dso_m101")
        composeRule.onAllNodesWithText("5 Verified Facts", substring = true, useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onAllNodesWithText("۵ حقیقت", substring = true, useUnmergedTree = true)
            .assertCountEquals(0)
        dismissDetailModal()

        composeRule.onNodeWithTag("ar_pill_time", useUnmergedTree = true).performClick()
        waitForTag("time_machine_container")
        composeRule.onNodeWithContentDescription("Time Machine Controls", useUnmergedTree = true)
            .performClick()
        waitForTag("tm_play_pause_btn")
        composeRule.onNodeWithTag("tm_play_pause_btn", useUnmergedTree = true).performClick()
        waitForTag("ar_time_machine_watermark", timeoutMillis = 15_000L)
        composeRule.onNodeWithTag("tm_live_btn", useUnmergedTree = true).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000L) {
            !hasNodeWithTag("ar_time_machine_watermark")
        }
    }

    private fun assertCatalogCountsAndDoubleClusterResolution() {
        val allObjects = CanonicalAstroCatalog.getAllCanonicalObjects()
        val deepSkyCount = allObjects.count { obj ->
            obj.type == ObjectType.DEEP_SKY ||
                    obj.type == ObjectType.GALAXY ||
                    obj.type == ObjectType.NEBULA ||
                    obj.type == ObjectType.STAR_CLUSTER ||
                    obj.type == ObjectType.GLOBULAR_CLUSTER ||
                    obj.type == ObjectType.BLACK_HOLE
        }

        assertEquals(354, allObjects.size)
        assertEquals(257, deepSkyCount)
        assertEquals("dso_ngc_869", CanonicalAstroCatalog.getCanonicalObject("NGC 869")?.canonicalId)
        assertEquals("dso_ngc_884", CanonicalAstroCatalog.getCanonicalObject("NGC 884")?.canonicalId)
        assertEquals("dso_ngc_869", CanonicalAstroCatalog.getCanonicalObject("C14")?.canonicalId)
        assertNull(CanonicalAstroCatalog.getCanonicalObject("dso_double_cluster"))
    }

    private fun openTargetDetailThroughArSearch(query: String, resultTag: String) {
        if (!hasNodeWithTag("ar_search_input")) {
            composeRule.onNodeWithTag("ar_pill_search", useUnmergedTree = true).performClick()
            waitForTag("ar_search_input")
        }

        val searchInput = composeRule.onNodeWithTag("ar_search_input", useUnmergedTree = true)
        searchInput.performTextClearance()
        searchInput.performTextInput(query)
        waitForTag(resultTag, timeoutMillis = 20_000L)
        composeRule.onNodeWithTag(resultTag, useUnmergedTree = true).performClick()

        waitForTag("ar_target_detail_button", timeoutMillis = 20_000L)
        composeRule.onNodeWithTag("ar_target_detail_button", useUnmergedTree = true).performClick()
        waitForTag("object_detail_modal", timeoutMillis = 20_000L)
        composeRule.onNodeWithTag("object_detail_modal", useUnmergedTree = true).assertExists()
    }

    private fun dismissDetailModal() {
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 15_000L) {
            !hasNodeWithTag("object_detail_modal")
        }
    }

    private fun waitForTag(tag: String, timeoutMillis: Long = 10_000L) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) { hasNodeWithTag(tag) }
        assertTrue("Expected Compose node with tag '$tag'", hasNodeWithTag(tag))
    }

    private fun hasNodeWithTag(tag: String): Boolean {
        return composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()
    }
}
