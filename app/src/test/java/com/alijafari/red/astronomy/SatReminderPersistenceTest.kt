package com.alijafari.red.astronomy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.notification.AstroNotificationManager
import com.alijafari.red.astronomy.notification.AstroNotificationStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SatReminderPersistenceTest {
    @Test
    fun manualLeadReplacesRollingCoverageAndSurvivesDisablingMonitoring() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sat = SatelliteCatalog.getById("iss_zarya")
        val observer = UserLocation(timezoneId = "Europe/London")
        val rise = System.currentTimeMillis() + 3 * 3_600_000L
        val pass = ISSEngine.ISSPass(rise, rise + 180_000L, rise + 360_000L, 47.0, 418.0, 180.0, 120.0,
            240.0, -2.7, 360, -12.0, true, true, classification = ISSEngine.PassClassification.GOOD,
            visibilityScore = 68, summaryReasonEn = "", summaryReasonFa = "", detailedReasonsEn = emptyList(), detailedReasonsFa = emptyList(),
            isVisible = true)
        val manager = AstroNotificationManager
        val store = AstroNotificationStore
        store.save(context, requireNotNull(manager.createScheduledPassItem(sat, pass, observer, 10)))
        assertTrue(manager.scheduleSpecificPassAlarm(context, sat, pass, observer, 30))
        val manual = store.getAll(context).single()
        assertTrue(manual.manualPass)
        assertEquals(rise - 30 * 60_000L, manual.triggerTimeMs)
        assertEquals(sat.noradId, manual.satelliteNoradId)
        assertFalse(manual.titleFa.any { it in '0'..'9' })

        manager.configureSatelliteMonitoring(context, false, observer, setOf(sat.id), 10)
        manager.refreshSatellitePassSchedulesIfEnabled(context)
        assertEquals(listOf(manual), store.getAll(context))
        manager.cancelSpecificPassAlarm(context, sat, rise)
        assertTrue(store.getAll(context).isEmpty())
    }
}
