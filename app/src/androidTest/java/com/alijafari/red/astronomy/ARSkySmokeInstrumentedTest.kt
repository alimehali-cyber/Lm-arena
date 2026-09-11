package com.alijafari.red.astronomy

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.alijafari.red.astronomy.astro_engine.CelestialSearchEngine
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.domain.ObjectType
import com.alijafari.red.astronomy.ui.components.objectDetailFactsHeader
import com.alijafari.red.astronomy.ui.components.objectDetailSectionPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator smoke coverage for the AR Sky screen. This intentionally launches the real Activity so
 * CameraX/sensor/GPS-null paths and the AR destination composition are exercised on an installed
 * debug APK. Catalog search/detail payload assertions are kept data-driven here because the live AR
 * screen continuously updates sensor state and can otherwise trigger false Compose-idle timeouts on
 * headless CI emulators.
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
        // The AR scene continuously changes with sensor/camera state; pausing the Compose test
        // clock prevents false ComposeNotIdle timeouts on headless CI emulators.
        composeRule.mainClock.autoAdvance = false

        waitForTag("main_bottom_navigation", timeoutMillis = 60_000L)
        // Let the splash overlay finish so the bottom navigation can receive the click.
        composeRule.mainClock.advanceTimeBy(5_000L)
        Thread.sleep(1_000L)
        openArSkyScreen()

        assertCatalogCountsAndDoubleClusterResolution()
        assertSearchAndDetailPayloadsForRepresentativeTargets()
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

        assertEquals(336, allObjects.size)
        assertEquals(239, deepSkyCount)
        assertEquals("dso_ngc_869", CanonicalAstroCatalog.getCanonicalObject("NGC 869")?.canonicalId)
        assertEquals("dso_ngc_884", CanonicalAstroCatalog.getCanonicalObject("NGC 884")?.canonicalId)
        assertEquals("dso_ngc_869", CanonicalAstroCatalog.getCanonicalObject("C14")?.canonicalId)
        assertEquals("dso_ngc_6946", CanonicalAstroCatalog.getCanonicalObject("C12")?.canonicalId)
        assertNull(CanonicalAstroCatalog.getCanonicalObject("C9"))
        assertNull(CanonicalAstroCatalog.getCanonicalObject("dso_double_cluster"))
    }

    private fun assertSearchAndDetailPayloadsForRepresentativeTargets() {
        val sirius = CelestialSearchEngine.search("Sirius", userLat = 35.6892, userLon = 51.3890)
            .firstOrNull { it.celestialObject.id == "star_cma_sirius" }
        val mars = CelestialSearchEngine.search("Mars", userLat = 35.6892, userLon = 51.3890)
            .firstOrNull { it.celestialObject.id == "planet_mars" }
        val m101 = CelestialSearchEngine.search("M101", userLat = 35.6892, userLon = 51.3890)
            .firstOrNull { it.celestialObject.id == "dso_m101" }
        val c12 = CelestialSearchEngine.search("C12", userLat = 35.6892, userLon = 51.3890)
            .firstOrNull { it.celestialObject.id == "dso_ngc_6946" }

        assertTrue("Sirius should be searchable from AR search payloads", sirius != null)
        assertTrue("Mars should be searchable from AR search payloads", mars != null)
        assertTrue("M101 should be searchable from AR search payloads", m101 != null)
        assertTrue("C12 should resolve to NGC 6946 in AR search payloads", c12 != null)

        val fireworks = CanonicalAstroCatalog.toCelestialObject(
            CanonicalAstroCatalog.getCanonicalObject("NGC 6946")!!
        )
        assertTrue(fireworks.nameFa.contains("کهکشان آتش‌بازی"))
        assertFalse(fireworks.nameFa.contains("Fireworks"))
        assertEquals(3, fireworks.funFactsEn.size)
        assertEquals(3, fireworks.funFactsFa.size)

        listOf("dso_m1", "dso_m51", "dso_c11").forEach { id ->
            val target = CanonicalAstroCatalog.toCelestialObject(
                CanonicalAstroCatalog.getCanonicalObject(id)!!
            )
            assertEquals("Previously uncovered target $id should now have five English facts", 5, target.funFactsEn.size)
            assertEquals("Previously uncovered target $id should now have five Persian facts", 5, target.funFactsFa.size)
            assertTrue(
                "Detail-modal section plan should include a facts card for $id",
                objectDetailSectionPlan(target.type, target.funFactsEn.size).contains("facts_card")
            )
            assertEquals("5 Verified Facts & Stories", objectDetailFactsHeader(target.funFactsEn.size, isFa = false))
            assertEquals("۵ حقیقت شگفت‌انگیز و علمی", objectDetailFactsHeader(target.funFactsFa.size, isFa = true))
        }
    }

    private fun openArSkyScreen() {
        val deadline = System.currentTimeMillis() + 90_000L
        var found = hasNodeWithTag("ar_pill_search")
        while (!found && System.currentTimeMillis() < deadline) {
            try {
                composeRule.onNodeWithTag("nav_item_arsky", useUnmergedTree = true).performClick()
            } catch (_: Throwable) {
                // The splash overlay or a transient recomposition can briefly hide the nav item.
            }
            composeRule.mainClock.advanceTimeBy(1_000L)
            Thread.sleep(250L)
            found = hasNodeWithTag("ar_pill_search")
        }
        assertTrue("Expected AR sky screen search pill after opening AR navigation", found)
    }

    private fun waitForTag(tag: String, timeoutMillis: Long = 10_000L) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var found = hasNodeWithTag(tag)
        while (!found && System.currentTimeMillis() < deadline) {
            composeRule.mainClock.advanceTimeBy(250L)
            Thread.sleep(50L)
            found = hasNodeWithTag(tag)
        }
        assertTrue("Expected Compose node with tag '$tag'", found)
    }

    private fun hasNodeWithTag(tag: String): Boolean {
        return try {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        } catch (_: Throwable) {
            false
        }
    }
}
