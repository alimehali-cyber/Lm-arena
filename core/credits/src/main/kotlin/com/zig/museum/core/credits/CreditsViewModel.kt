package com.zig.museum.core.credits

/**
 * Credits ViewModel per M5 task 5
 * Sources & Credits surface generated from installed manifests; unit tests for verbatim credit strings
 */

data class CreditRow(
    val product: String,
    val publisher: String,
    val url: String,
    val credit: String,
    val what: String,
    val packId: String,
    val objectId: String
)

object CreditsViewModel {

    fun fromManifests(manifests: List<PackManifest>): List<CreditRow> {
        return manifests.flatMap { manifest ->
            manifest.assets.map { asset ->
                CreditRow(
                    product = asset.product,
                    publisher = asset.publisher,
                    url = asset.url,
                    credit = asset.credit,
                    what = asset.what,
                    packId = manifest.packId,
                    objectId = manifest.objectId
                )
            }
        }
    }

    /**
     * Unit test: Sources & Credits surface renders one row per asset in installed manifests, with credit string verbatim
     */
    fun verifyVerbatim(manifests: List<PackManifest>, rows: List<CreditRow>): List<String> {
        val errors = mutableListOf<String>()
        val expectedCount = manifests.sumOf { it.assets.size }
        if (rows.size != expectedCount) {
            errors.add("Row count ${rows.size} != asset count $expectedCount")
        }
        manifests.forEach { manifest ->
            manifest.assets.forEach { asset ->
                val matching = rows.find { it.product == asset.product && it.packId == manifest.packId }
                if (matching == null) {
                    errors.add("Missing row for ${asset.product} in ${manifest.packId}")
                } else {
                    if (matching.credit != asset.credit) {
                        errors.add("Credit string not verbatim for ${asset.product}: expected '${asset.credit}' got '${matching.credit}'")
                    }
                    if (matching.url != asset.url) {
                        errors.add("URL not verbatim for ${asset.product}")
                    }
                }
            }
        }
        return errors
    }
}
