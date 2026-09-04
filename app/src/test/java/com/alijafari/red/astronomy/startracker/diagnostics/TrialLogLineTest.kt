package com.alijafari.red.astronomy.startracker.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D3: trial-log JSON line serialization (pure Kotlin; harness + CI). */
class TrialLogLineTest {

    private fun line(
        event: String = "step", step: Int? = 3,
        intrinsicsTier: String? = "CALIBRATED_HARDWARE",
        lock: String? = null, fail: String? = null,
        gpsLat: Double? = 40.7128, model: String = "Pixel 8"
    ) = TrialLogLine(
        epochMs = 1757000000123L, iso8601 = "2026-09-04T21:30:00Z", event = event, step = step,
        enabled = true, pipelineCameraFeed = true, trackerToOrientationPhase6 = false, projectionSelfCalibratedPhase7 = false,
        declinationDeg = -13.25,
        intrinsicsTier = intrinsicsTier, fx = 1520.5, fy = 1521.25, cx = 1079.5, cy = 2399.0,
        distortionTier = "NONE", k1 = null, k2 = null,
        sensorHz = 102.4, sensorTsDeltaMs = 9.77,
        trackerState = "NOT_WIRED", lockConfidence = lock, matched = null, detected = null,
        solveMs = null, discrepancyDeg = null, failureReason = fail,
        gpsLat = gpsLat, gpsLon = -74.006, gpsAccuracyM = 4.5,
        deviceModel = model, deviceSdk = 35
    )

    @Test
    fun `line is single-line JSON with the full fixed key order`() {
        val json = line().toJson()
        assertFalse(json.contains('\n'))
        val keys = Regex("\"(\\w+)\":").findAll(json).map { it.groupValues[1] }.toList()
        assertEquals(
            listOf("ts", "iso", "event", "step", "enabled", "pipelineCameraFeed", "trackerToOrientationPhase6",
                "projectionSelfCalibratedPhase7", "declinationDeg", "intrinsicsTier", "fx", "fy", "cx", "cy",
                "distortionTier", "k1", "k2", "sensorHz", "sensorTsDeltaMs", "trackerState", "lockConfidence",
                "matched", "detected", "solveMs", "discrepancyDeg", "failureReason", "gpsLat", "gpsLon",
                "gpsAccuracyM", "deviceModel", "deviceSdk"),
            keys
        )
        assertTrue(json.startsWith("{") && json.endsWith("}"))
        assertTrue("\"step\":3" in json)
        assertTrue("\"fx\":1520.5" in json)
        assertTrue("\"declinationDeg\":-13.25" in json)
    }

    @Test
    fun `nulls are explicit JSON nulls not omitted`() {
        val json = line().toJson()
        assertTrue("\"k1\":null" in json)
        assertTrue("\"k2\":null" in json)
        assertTrue("\"lockConfidence\":null" in json)
        assertTrue("\"failureReason\":null" in json)
        assertTrue("\"matched\":null" in json)
    }

    @Test
    fun `strings are sanitized so no escaping is ever needed`() {
        val json = line(fail = "quote\" back\\slash {brace} ctrl\u0001").toJson()
        // the raw dangerous characters never appear inside the value; only sanitized text
        assertTrue("quote_ back_slash _brace_ ctrl_" in json)
        assertFalse('\\' in json.dropLast(1)) // no backslashes anywhere except none exist
        val modelJson = line(model = "mo\"del").toJson()
        assertTrue("mo_del" in modelJson)
    }

    @Test
    fun `NaN and infinity render as quoted markers and numbers are trimmed`() {
        val json = line(gpsLat = Double.NaN).toJson()
        assertTrue("\"gpsLat\":\"NaN\"" in json)
        assertTrue("\"fy\":1521.25" in json)   // trailing zeros trimmed, no float noise
    }
}
