package com.reztek.whatifportfolio.ui.simulation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompareKeyTest {

    @Test
    fun localKey_parsesToLocalId() {
        assertEquals(CompareKey.Local(7L), CompareKey.parse("local-7"))
    }

    @Test
    fun remoteKey_parsesToRemoteId_includingHyphenatedIds() {
        assertEquals(CompareKey.Remote("abc-123"), CompareKey.parse("remote-abc-123"))
    }

    @Test
    fun localAndRemoteWithSameNumber_stayDifferent() {
        assertNotEquals(CompareKey.parse("local-7"), CompareKey.parse("remote-7"))
    }

    @Test
    fun leadingZerosInLocalKey_resolveToTheSameRow() {
        assertEquals(CompareKey.parse("local-7"), CompareKey.parse("local-007"))
    }

    @Test
    fun malformedKeys_areRejected() {
        listOf(
            "",
            "7",
            "local-",
            "local-abc",
            "local-0",
            "local--5",
            "local-+7",
            "local-7.5",
            "remote-",
            "remote-   ",
            "remote-a/b",
            "remote-a b",
            "other-5",
            "LOCAL-5"
        ).forEach { raw ->
            assertNull("'$raw' should be rejected", CompareKey.parse(raw))
        }
    }

    @Test
    fun outOfRangeLocalId_isRejected() {
        assertNull(CompareKey.parse("local-99999999999999999999"))
    }

    @Test
    fun typeLabel_doesNotContainTheId() {
        assertEquals("local", CompareKey.Local(42L).typeLabel)
        assertEquals("remote", CompareKey.Remote("secret-id").typeLabel)
    }
}
