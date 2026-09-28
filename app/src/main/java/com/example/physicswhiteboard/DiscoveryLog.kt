package com.example.physicswhiteboard

/**
 * One discovered formula. Stored newest-first in [DiscoveryLog.entries].
 */
data class DiscoveryEntry(
    val formula: String,
    val title: String,
    val timestampMs: Long
)

/**
 * Rolling log of formulas the user discovered by fusing symbols on the
 * whiteboard. Pure Kotlin (no Android imports) so it stays unit-testable;
 * persistence lives in DiscoveryStore on the UI side.
 */
class DiscoveryLog(
    private val maxEntries: Int = 30,
    initialEntries: List<DiscoveryEntry> = emptyList()
) {
    private val _entries = mutableListOf<DiscoveryEntry>()

    /** Newest first. */
    val entries: List<DiscoveryEntry> get() = _entries.toList()

    init {
        // Stored newest-first; re-adding in reverse keeps that order.
        initialEntries.reversed().forEach { remember(it.formula, it.title, it.timestampMs) }
    }

    /**
     * Records a discovery. Returns true when the formula is new, false when
     * it was already known (deduplicated by formula text) or blank.
     */
    fun remember(formula: String, title: String, timestampMs: Long): Boolean {
        if (formula.isBlank()) return false
        if (_entries.any { it.formula == formula }) return false
        _entries.add(0, DiscoveryEntry(formula, title, timestampMs))
        while (_entries.size > maxEntries) _entries.removeAt(_entries.size - 1)
        return true
    }

    fun contains(formula: String): Boolean = _entries.any { it.formula == formula }

    fun formulaTexts(): Set<String> = _entries.map { it.formula }.toSet()
}
