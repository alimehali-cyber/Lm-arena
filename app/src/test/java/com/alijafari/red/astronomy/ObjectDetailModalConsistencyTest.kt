package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.domain.CanonicalAstroObject
import com.alijafari.red.astronomy.ui.components.objectDetailFactsHeader
import com.alijafari.red.astronomy.ui.components.objectDetailSectionPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectDetailModalConsistencyTest {

    @Test
    fun auditedReferenceAndNewObjectsHaveCompleteModalDataInBothLanguages() {
        val auditedIds = listOf(
            "sun",
            "moon",
            "sat_25544",
            "planet_mars",
            "dso_m31_andromeda",
            "dso_ngc_6946",
            "dso_ngc_6543",
            "dso_m1",
            "dso_m51",
            "dso_c11"
        )

        val incomplete = auditedIds.map(::canonicalObject).flatMap { obj ->
            val issues = mutableListOf<String>()
            if (obj.nameEn.isBlank()) issues += "nameEn"
            if (obj.nameFa.isBlank()) issues += "nameFa"
            if (obj.observationalInfo.categoryEn.isBlank()) issues += "categoryEn"
            if (obj.observationalInfo.categoryFa.isBlank()) issues += "categoryFa"
            if (obj.observationalInfo.descriptionEn.wordCount() !in 4..28) issues += "descriptionEn tone/length"
            if (obj.observationalInfo.descriptionFa.isBlank()) issues += "descriptionFa"
            if (obj.observationalInfo.observationTipEn.isBlank()) issues += "observationTipEn"
            if (obj.observationalInfo.observationTipFa.isBlank()) issues += "observationTipFa"
            if (factsEn(obj).isEmpty() || factsFa(obj).isEmpty()) issues += "facts"
            issues.map { "${obj.canonicalId}: $it" }
        }

        assertTrue("Audited modal objects have incomplete bilingual data: $incomplete", incomplete.isEmpty())
    }

    @Test
    fun sameTypeHandAuthoredAndEngineDerivedDsosUseSameModalSectionPlan() {
        val comparablePairs = listOf(
            "dso_m31_andromeda" to "dso_m51",
            "dso_m42_orion_nebula" to "dso_m1",
            "dso_m45_pleiades" to "dso_m6",
            "dso_m13_hercules" to "dso_m2"
        )

        comparablePairs.forEach { (handAuthoredId, engineDerivedId) ->
            val hand = canonicalObject(handAuthoredId)
            val engine = canonicalObject(engineDerivedId)
            assertEquals("Comparable objects must share the same ObjectType", hand.type, engine.type)

            val handPlan = objectDetailSectionPlan(hand.type, factsEn(hand).size)
            val enginePlan = objectDetailSectionPlan(engine.type, factsEn(engine).size)
            assertEquals(
                "Modal section plan should not depend on hand-authored vs engine-derived origin for ${hand.type}",
                handPlan,
                enginePlan
            )
            assertEquals(expectedDeepSkySectionPlan, handPlan)
        }
    }

    @Test
    fun factsHeaderIsCountAwareAndLocalizedTheSameWayForAllObjects() {
        assertEquals("5 Verified Facts & Stories", objectDetailFactsHeader(5, isFa = false))
        assertEquals("۳ حقیقت شگفت‌انگیز و علمی", objectDetailFactsHeader(3, isFa = true))
    }

    @Test
    fun engineDerivedAndHandAuthoredDsosExposeSameCatalogDataDepth() {
        val ids = listOf(
            "dso_m31_andromeda",
            "dso_ngc_6946",
            "dso_m1",
            "dso_m51",
            "dso_c11"
        )
        val sparse = ids.map(::canonicalObject).filter { obj ->
            obj.scientificIdentifiers.catalogDesignations.isEmpty() ||
                obj.scientificIdentifiers.constellationCode.isBlank() ||
                obj.physicalProperties.angularSizeArcmin == null ||
                obj.observationalInfo.bestViewingMonthEn.isBlank() ||
                obj.observationalInfo.bestViewingMonthFa.isBlank()
        }

        assertTrue("Audited DSOs must expose the same deep-sky catalog rows: ${sparse.map { it.canonicalId }}", sparse.isEmpty())
    }

    private fun canonicalObject(id: String): CanonicalAstroObject =
        requireNotNull(CanonicalAstroCatalog.getCanonicalObject(id)) { "Missing canonical object $id" }

    private fun factsEn(obj: CanonicalAstroObject): List<String> =
        obj.observationalInfo.verifiedFactsEn + obj.observationalInfo.funFactsEn

    private fun factsFa(obj: CanonicalAstroObject): List<String> =
        obj.observationalInfo.verifiedFactsFa + obj.observationalInfo.funFactsFa

    private fun String.wordCount(): Int = trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size

    private val expectedDeepSkySectionPlan = listOf(
        "header",
        "observability_score",
        "locate_in_ar",
        "derived_physical_properties",
        "deep_sky_catalog_data",
        "precise_schedule",
        "facts_card",
        "description",
        "coordinates",
        "observation_log"
    )
}
