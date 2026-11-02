package com.reztek.whatifportfolio.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleHelperTest {

    @Test
    fun `display names for supported tags`() {
        assertEquals("English", LocaleHelper.displayNameFor(""))
        assertEquals("English", LocaleHelper.displayNameFor("en"))
        assertEquals("Afrikaans", LocaleHelper.displayNameFor("af"))
        assertEquals("isiXhosa", LocaleHelper.displayNameFor("xh"))
    }
}
