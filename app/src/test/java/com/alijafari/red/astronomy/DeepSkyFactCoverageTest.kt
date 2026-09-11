package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSkyFactCoverageTest {

    @Test
    fun handAuthoredAndSplitDeepSkyObjectsHaveCuratedBilingualFacts() {
        val displayedHandContentIds = listOf(
            "dso_m31_andromeda",
            "dso_lmc",
            "dso_smc",
            "dso_m33_triangulum",
            "dso_m45_pleiades",
            "dso_hyades",
            "dso_m44_beehive",
            "dso_ngc_869",
            "dso_ngc_884",
            "dso_coma_cluster",
            "dso_omega_centauri",
            "dso_47_tucanae",
            "dso_m13_hercules",
            "dso_m42_orion_nebula",
            "dso_m8_lagoon",
            "dso_eta_carinae_nebula"
        )

        displayedHandContentIds.forEach { id ->
            assertCuratedFacts(id, minimum = if (id == "dso_ngc_884") 3 else 5)
        }
    }

    @Test
    fun mergedCaldwellNgcObjectsHaveCuratedBilingualFacts() {
        val mergedNgcIds = listOf(
            "dso_ngc_147",
            "dso_ngc_185",
            "dso_ngc_2403",
            "dso_ngc_40",
            "dso_ngc_4244",
            "dso_ngc_4449",
            "dso_ngc_457",
            "dso_ngc_6543",
            "dso_ngc_663",
            "dso_ngc_6826",
            "dso_ngc_6946",
            "dso_ngc_7000",
            "dso_ngc_7243",
            "dso_ngc_7331",
            "dso_ngc_752",
            "dso_ngc_7662",
            "dso_ngc_891"
        )

        mergedNgcIds.forEach { id -> assertCuratedFacts(id, minimum = 3) }
    }

    private fun assertCuratedFacts(canonicalId: String, minimum: Int) {
        val obj = CanonicalAstroCatalog.getCanonicalObject(canonicalId)
        assertNotNull("Missing canonical object $canonicalId", obj)
        val factsEn = obj!!.observationalInfo.verifiedFactsEn + obj.observationalInfo.funFactsEn
        val factsFa = obj.observationalInfo.verifiedFactsFa + obj.observationalInfo.funFactsFa

        assertTrue("Expected at least $minimum English facts for $canonicalId but got ${factsEn.size}", factsEn.size >= minimum)
        assertTrue("Expected at least $minimum Persian facts for $canonicalId but got ${factsFa.size}", factsFa.size >= minimum)
        assertTrue("Mismatched bilingual fact count for $canonicalId", factsEn.size == factsFa.size)
    }
}
