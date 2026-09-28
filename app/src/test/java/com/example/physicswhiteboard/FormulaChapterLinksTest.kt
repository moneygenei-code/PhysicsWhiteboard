package com.example.physicswhiteboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaChapterLinksTest {

    @Test
    fun wienFilterMapsToChapterSeven() {
        assertEquals(7, FormulaChapterLinks.chapterFor("v = E/B"))
    }

    @Test
    fun hallFormulaByteMatchesCatalogEntry() {
        // Uses the exact string from the shared catalog, so the link cannot
        // silently drift from the formula the engine actually produces.
        val hall = SharedFormulaCatalog.rules.first { it.id == "hall_voltage" }
        assertEquals(10, FormulaChapterLinks.chapterFor(hall.formula))
    }

    @Test
    fun centripetalFormulasMapToChapterOne() {
        assertEquals(1, FormulaChapterLinks.chapterFor("a = v\u00b2 / r"))
        assertEquals(1, FormulaChapterLinks.chapterFor("F = m\u00b7v\u00b2 / r"))
    }

    @Test
    fun formulasOutsideTheCurriculumStayUnmapped() {
        assertNull(FormulaChapterLinks.chapterFor("E = m\u00b7c\u00b2"))
        assertNull(FormulaChapterLinks.chapterFor("unbekannt"))
    }

    @Test
    fun everyChapterNumberIsInsideTheCurriculum() {
        val mapped = listOf(
            "F = m\u00b7a", "a = v\u00b2 / r", "F = m\u00b7v\u00b2 / r", "v = E/B",
            "F = q\u00b7E", "v = \u221a(2qU/m)", "T = 2\u03c0\u00b7m / (q\u00b7B)", "UH = RH\u00b7I\u00b7B/d"
        )
        for (formula in mapped) {
            val chapter = FormulaChapterLinks.chapterFor(formula)
            assertNotNull("missing mapping for $formula", chapter)
            assertTrue("chapter $chapter out of range for $formula", chapter in 1..10)
        }
    }
}
