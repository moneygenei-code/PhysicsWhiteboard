package com.example.physicswhiteboard

/**
 * Maps discoverable fusion formulas to their curriculum chapter so the
 * discovery history can point students to where the formula is taught.
 * Only formulas that are genuinely part of the Physik-12 curriculum get a
 * chapter; everything else stays unmapped (no hint shown) instead of
 * guessing a wrong chapter.
 */
object FormulaChapterLinks {

    private val links: Map<String, Int> = mapOf(
        "F = m\u00b7a" to 2,                 // Newton 2. Gesetz → Kraftansätze
        "a = v\u00b2 / r" to 1,              // Zentripetalbeschleunigung → Kreisbewegung
        "F = m\u00b7v\u00b2 / r" to 1,       // Zentripetalkraft → Kreisbewegung
        "v = E/B" to 7,                      // Wien-Filter Durchlassbedingung
        "F = q\u00b7E" to 5,                 // Elektrische Kraft → Teilchen im E-Feld
        "v = \u221a(2qU/m)" to 5,            // Elektronenkanone → Teilchen im E-Feld
        "T = 2\u03c0\u00b7m / (q\u00b7B)" to 6, // Umlaufdauer → Teilchen im B-Feld
        "UH = RH\u00b7I\u00b7B/d" to 10      // Hall-Spannung → Hall-Effekt
    )

    /** Curriculum chapter (1–10) teaching this formula, or null. */
    fun chapterFor(formula: String): Int? = links[formula]
}
