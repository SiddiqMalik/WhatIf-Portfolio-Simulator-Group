package com.reztek.whatifportfolio.ui.simulation

/**
 * Pure rules for the Compare selection screen: exactly two different
 * simulations must be chosen. Kept free of Compose so it can be unit tested.
 */
object CompareSelectionRules {
    const val REQUIRED_COUNT = 2

    /** Adds [key] when there is room, removes it when already selected, ignores a third pick. */
    fun toggle(current: List<String>, key: String): List<String> = when {
        key in current -> current.filterNot { it == key }
        current.size < REQUIRED_COUNT -> current + key
        else -> current
    }

    /** True only for exactly two distinct selections. */
    fun canCompare(selected: List<String>): Boolean =
        selected.size == REQUIRED_COUNT && selected.distinct().size == REQUIRED_COUNT

    /** True when a tap on [key] was refused because two items are already selected. */
    fun isBlockedThirdPick(current: List<String>, key: String): Boolean =
        key !in current && current.size >= REQUIRED_COUNT

    /** Drops selections whose item no longer exists (for example, deleted meanwhile). */
    fun pruneTo(selected: List<String>, availableKeys: Set<String>): List<String> =
        selected.filter { it in availableKeys }
}
