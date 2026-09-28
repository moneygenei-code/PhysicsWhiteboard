package com.example.physicswhiteboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryLogTest {

    @Test
    fun remembersNewFormulasAndDeduplicates() {
        val log = DiscoveryLog()
        assertTrue(log.remember("F = m·a", "Newton 2. Gesetz", 1L))
        assertFalse(log.remember("F = m·a", "Newton 2. Gesetz", 2L))
        assertEquals(1, log.entries.size)
    }

    @Test
    fun newestEntryComesFirst() {
        val log = DiscoveryLog()
        log.remember("F = m·a", "Newton 2. Gesetz", 1L)
        log.remember("p = m·v", "Impuls", 2L)
        assertEquals("p = m·v", log.entries.first().formula)
        assertEquals("F = m·a", log.entries.last().formula)
    }

    @Test
    fun evictsOldestBeyondCapacity() {
        val log = DiscoveryLog(maxEntries = 3)
        for (i in 1..4) log.remember("F$i", "Titel $i", i.toLong())
        assertEquals(3, log.entries.size)
        assertFalse(log.contains("F1"))
        assertTrue(log.contains("F4"))
    }

    @Test
    fun restoresInitialEntriesNewestFirst() {
        val stored = listOf(
            DiscoveryEntry("p = m·v", "Impuls", 2L),
            DiscoveryEntry("F = m·a", "Newton 2. Gesetz", 1L)
        )
        val log = DiscoveryLog(initialEntries = stored)
        assertEquals(listOf("p = m·v", "F = m·a"), log.entries.map { it.formula })
        assertEquals(setOf("p = m·v", "F = m·a"), log.formulaTexts())
    }

    @Test
    fun blankFormulasAreIgnored() {
        val log = DiscoveryLog()
        assertFalse(log.remember("   ", "leer", 1L))
        assertTrue(log.entries.isEmpty())
    }
}
