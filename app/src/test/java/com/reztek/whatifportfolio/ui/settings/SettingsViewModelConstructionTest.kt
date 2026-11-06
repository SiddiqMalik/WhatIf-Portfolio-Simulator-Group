package com.reztek.whatifportfolio.ui.settings

import android.app.Application
import org.junit.Assert.assertNotNull
import org.junit.Test

class SettingsViewModelConstructionTest {

    @Test
    fun exposesTheSingleApplicationConstructorUsedByAndroidViewModelFactory() {
        val constructor = SettingsViewModel::class.java.getConstructor(Application::class.java)

        assertNotNull(constructor)
    }
}
