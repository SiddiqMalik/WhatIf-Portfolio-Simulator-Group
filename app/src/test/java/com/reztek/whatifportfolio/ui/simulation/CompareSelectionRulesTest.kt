package com.reztek.whatifportfolio.ui.simulation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareSelectionRulesTest {

    @Test
    fun toggle_addsUpToTwoItems() {
        val one = CompareSelectionRules.toggle(emptyList(), "local-1")
        val two = CompareSelectionRules.toggle(one, "remote-a")

        assertEquals(listOf("local-1", "remote-a"), two)
    }

    @Test
    fun toggle_ignoresAThirdItem() {
        val full = listOf("local-1", "remote-a")

        assertEquals(full, CompareSelectionRules.toggle(full, "local-2"))
    }

    @Test
    fun toggle_removesAnAlreadySelectedItem() {
        val selected = listOf("local-1", "remote-a")

        assertEquals(listOf("remote-a"), CompareSelectionRules.toggle(selected, "local-1"))
    }

    @Test
    fun canCompare_requiresExactlyTwo() {
        assertFalse(CompareSelectionRules.canCompare(emptyList()))
        assertFalse(CompareSelectionRules.canCompare(listOf("local-1")))
        assertTrue(CompareSelectionRules.canCompare(listOf("local-1", "remote-a")))
        assertFalse(CompareSelectionRules.canCompare(listOf("local-1", "remote-a", "local-2")))
    }

    @Test
    fun canCompare_rejectsTheSameItemTwice() {
        assertFalse(CompareSelectionRules.canCompare(listOf("local-1", "local-1")))
    }

    @Test
    fun isBlockedThirdPick_onlyWhenFullAndItemNotSelected() {
        val full = listOf("local-1", "remote-a")

        assertTrue(CompareSelectionRules.isBlockedThirdPick(full, "local-2"))
        assertFalse(CompareSelectionRules.isBlockedThirdPick(full, "local-1"))
        assertFalse(CompareSelectionRules.isBlockedThirdPick(listOf("local-1"), "local-2"))
    }

    @Test
    fun pruneTo_dropsItemsThatNoLongerExist() {
        val pruned = CompareSelectionRules.pruneTo(
            selected = listOf("local-1", "remote-a"),
            availableKeys = setOf("remote-a", "local-3")
        )

        assertEquals(listOf("remote-a"), pruned)
    }
}
