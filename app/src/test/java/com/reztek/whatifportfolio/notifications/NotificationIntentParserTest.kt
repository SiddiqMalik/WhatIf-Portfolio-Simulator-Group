package com.reztek.whatifportfolio.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationIntentParserTest {

    @Test
    fun `reads simulationId from data map`() {
        val id = NotificationIntentParser.simulationIdFrom(
            mapOf("simulationId" to "abc123", "type" to "simulation_ready")
        )
        assertEquals("abc123", id)
    }

    @Test
    fun `blank simulationId returns null`() {
        assertNull(NotificationIntentParser.simulationIdFrom(mapOf("simulationId" to "  ")))
        assertNull(NotificationIntentParser.simulationIdFrom(emptyMap()))
        assertNull(NotificationIntentParser.simulationIdFrom(null))
    }
}
