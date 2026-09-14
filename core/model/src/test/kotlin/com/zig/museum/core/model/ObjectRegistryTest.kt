package com.zig.museum.core.model

import org.junit.Test
import org.junit.Assert.*

class ObjectRegistryTest {

    @Test
    fun testThirteenObjects() {
        assertEquals(13, ObjectRegistry.all.size)
    }

    @Test
    fun testAllIdsPresent() {
        val expected = setOf("sun", "mercury", "venus", "earth", "moon", "mars", "jupiter", "saturn", "uranus", "neptune", "milky_way", "iss", "black_hole")
        val actual = ObjectRegistry.all.map { it.id }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun testSceneRadiusPositive() {
        ObjectRegistry.all.forEach { spec ->
            assertTrue("Radius must be positive for ${spec.id}", spec.sceneRadiusMetres > 0)
        }
    }

    @Test
    fun testOblatenessRange() {
        ObjectRegistry.all.forEach { spec ->
            assertTrue("Oblateness must be 0..1 for ${spec.id}", spec.oblateness >= 0.0 && spec.oblateness < 1.0)
        }
    }

    @Test
    fun testRotationPeriod() {
        // Check retrograde for Venus and Uranus per spec (signed for retrograde)
        val venus = ObjectRegistry.byId("venus")
        assertNotNull(venus)
        assertTrue("Venus retrograde should be negative", venus!!.rotationPeriodHours < 0)

        val uranus = ObjectRegistry.byId("uranus")
        assertNotNull(uranus)
        assertTrue("Uranus retrograde should be negative", uranus!!.rotationPeriodHours < 0)
    }

    @Test
    fun testDataCeilingTextPresent() {
        ObjectRegistry.all.forEach { spec ->
            assertTrue("Ceiling text En must not be blank for ${spec.id}", spec.dataCeilingTextEn.isNotBlank())
            assertTrue("Ceiling text Fa must not be blank for ${spec.id}", spec.dataCeilingTextFa.isNotBlank())
        }
    }
}
