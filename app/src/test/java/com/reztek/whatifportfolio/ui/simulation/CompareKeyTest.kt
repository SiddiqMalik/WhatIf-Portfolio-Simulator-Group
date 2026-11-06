package com.reztek.whatifportfolio.ui.simulation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompareKeyTest {

    @Test
    fun parsesRemoteKeysKeepingTheFullId() {
        assertEquals(CompareKey.Remote("abc-123"), CompareKey.parse("remote-abc-123"))
    }

    @Test
    fun parsesLocalKeysToNumericIds() {
        assertEquals(CompareKey.Local(42L), CompareKey.parse("local-42"))
    }

    @Test
    fun rejectsMalformedKeys() {
        assertNull(CompareKey.parse("remote-"))
        assertNull(CompareKey.parse("local-abc"))
        assertNull(CompareKey.parse("local-"))
        assertNull(CompareKey.parse("42"))
        assertNull(CompareKey.parse(""))
    }

    @Test
    fun roundTripsTheKeysUsedByTheSavedList() {
        assertEquals(CompareKey.Local(7L), CompareKey.parse("local-${7L}"))
        assertEquals(CompareKey.Remote("r1"), CompareKey.parse("remote-r1"))
    }
}
