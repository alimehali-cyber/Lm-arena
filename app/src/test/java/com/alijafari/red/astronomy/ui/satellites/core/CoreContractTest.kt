package com.alijafari.red.astronomy.ui.satellites.core

import org.junit.Test

class CoreContractTest {
    @Test fun measuredHud() = CoreContract.geometry()

    @Test fun physicalMotion() = CoreContract.heading()

    @Test fun catalogueAndPersistence() = CoreContract.catalogue()

    @Test fun passesAndLocale() = CoreContract.passesAndNumbers()
}
