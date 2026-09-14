package com.zig.museum.core.credits

import org.junit.Assert.*
import org.junit.Test

class CreditsTest {

    @Test
    fun testCreditsFromManifests() {
        val manifest = PackManifest(
            packId = "moon_base",
            version = "1",
            objectId = "moon",
            assets = listOf(
                AssetEntry(
                    product = "LROC WAC Global Morphology Mosaic 100m",
                    publisher = "Arizona State University / NASA",
                    url = "https://wms.lroc.asu.edu/lroc/view_rdr/WAC_GLOBAL",
                    credit = "NASA/GSFC/Arizona State University",
                    what = "albedo",
                    processing = "reprojected to equirectangular",
                    sha256 = "abc",
                    format = "KTX2 ASTC 6x6 sRGB",
                    resolution = "100 m/px",
                    type = "albedo"
                ),
                AssetEntry(
                    product = "LOLA LDEM 64",
                    publisher = "NASA GSFC",
                    url = "https://pds-geosciences.wustl.edu/missions/lro/lola.htm",
                    credit = "NASA/GSFC",
                    what = "height",
                    processing = "downsampled",
                    sha256 = "def",
                    format = "KTX2 R16F",
                    resolution = "59 m/px",
                    type = "height"
                )
            )
        )

        val rows = CreditsViewModel.fromManifests(listOf(manifest))
        assertEquals(2, rows.size)
        assertEquals("LROC WAC Global Morphology Mosaic 100m", rows[0].product)
        assertEquals("NASA/GSFC/Arizona State University", rows[0].credit)
        println("Credit rows: $rows")

        val errors = CreditsViewModel.verifyVerbatim(listOf(manifest), rows)
        assertTrue("Verbatim check should pass, got $errors", errors.isEmpty())
    }

    @Test
    fun testCreditsVerbatimFailsOnMismatch() {
        val manifest = PackManifest(
            packId = "test",
            version = "1",
            objectId = "moon",
            assets = listOf(
                AssetEntry(
                    product = "Test Product",
                    publisher = "Test Publisher",
                    url = "https://example.com/product",
                    credit = "Exact Credit String",
                    what = "albedo",
                    processing = "",
                    sha256 = "",
                    format = "",
                    resolution = "",
                    type = ""
                )
            )
        )
        val rows = listOf(
            CreditRow(
                product = "Test Product",
                publisher = "Test Publisher",
                url = "https://example.com/product",
                credit = "Different Credit", // not verbatim
                what = "albedo",
                packId = "test",
                objectId = "moon"
            )
        )
        val errors = CreditsViewModel.verifyVerbatim(listOf(manifest), rows)
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("verbatim") })
        println("Verbatim failure correctly detected: $errors")
    }

    @Test
    fun testManifestReader() {
        val json = """
            {
                "packId": "moon_base",
                "version": "1",
                "objectId": "moon",
                "assets": [
                    {
                        "product": "LROC WAC Global Morphology Mosaic 100m",
                        "publisher": "ASU / NASA",
                        "url": "https://wms.lroc.asu.edu/lroc/view_rdr/WAC_GLOBAL",
                        "credit": "NASA/GSFC/ASU",
                        "what": "albedo",
                        "processing": "reprojected",
                        "sha256": "abc",
                        "format": "KTX2",
                        "resolution": "100 m/px",
                        "type": "albedo"
                    }
                ]
            }
        """.trimIndent()
        val manifest = ManifestReader.parse(json)
        assertEquals("moon_base", manifest.packId)
        assertEquals(1, manifest.assets.size)
        val errors = ManifestReader.validate(manifest)
        assertTrue("Should be valid, got $errors", errors.isEmpty())
    }
}
