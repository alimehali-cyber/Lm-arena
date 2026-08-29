package com.alijafari.red.astronomy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.alijafari.red.astronomy.astro_engine.ARCalibrationManager
import com.alijafari.red.astronomy.astro_engine.CalibrationState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ARCalibrationPromptTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ARCalibrationManager.init(context)
    }

    @Test
    fun testAutoPromptDefaultAndToggle() {
        // Default must be true (ON)
        assertTrue("Auto-prompt must default to true", ARCalibrationManager.isAutoPromptEnabled())
        assertTrue("Auto-prompt flow value must default to true", ARCalibrationManager.autoPromptEnabledFlow.value)

        // Toggle to false (OFF)
        ARCalibrationManager.setAutoPromptEnabled(false, context)
        assertFalse("Auto-prompt must be false after disabling", ARCalibrationManager.isAutoPromptEnabled())
        assertFalse("Auto-prompt flow value must be false after disabling", ARCalibrationManager.autoPromptEnabledFlow.value)

        // Re-enable (ON)
        ARCalibrationManager.setAutoPromptEnabled(true, context)
        assertTrue("Auto-prompt must be true after re-enabling", ARCalibrationManager.isAutoPromptEnabled())
        assertTrue("Auto-prompt flow value must be true after re-enabling", ARCalibrationManager.autoPromptEnabledFlow.value)
    }

    @Test
    fun testManualCalibrationMathPreserved() {
        val offsets = ARCalibrationManager.getOffsets()
        assertNotNull(offsets)

        // Verify rotation matrix generation remains completely functional
        val matrix = ARCalibrationManager.createCalibrationRotationMatrix(10f, -5f, 2f)
        assertEquals(9, matrix.size)

        // Identity check
        val identity = ARCalibrationManager.createCalibrationRotationMatrix(0f, 0f, 0f)
        assertEquals(1f, identity[0], 1e-5f)
        assertEquals(1f, identity[4], 1e-5f)
        assertEquals(1f, identity[8], 1e-5f)
        assertEquals(0f, identity[1], 1e-5f)
        assertEquals(0f, identity[2], 1e-5f)
    }

    @Test
    fun testCalibrationStateEnumIntegrity() {
        val states = CalibrationState.values()
        assertTrue(states.contains(CalibrationState.EXCELLENT))
        assertTrue(states.contains(CalibrationState.GOOD))
        assertTrue(states.contains(CalibrationState.POOR))
        assertTrue(states.contains(CalibrationState.NEEDS_CALIBRATION))
        assertTrue(states.contains(CalibrationState.UNCALIBRATED))
    }
}
