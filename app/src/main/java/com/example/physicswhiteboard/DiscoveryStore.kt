package com.example.physicswhiteboard

import android.content.Context

/**
 * Persists discovered formulas in SharedPreferences so the history survives
 * app restarts. Each stored line is "formula|title|timestamp" - '|' never
 * appears in formula or title texts, so it is a safe separator.
 */
class DiscoveryStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Stored set has no order guarantee; returned newest-first. */
    fun load(): List<DiscoveryEntry> {
        val raw = prefs.getStringSet(KEY, emptySet()) ?: emptySet()
        return raw.map { line ->
            val parts = line.split(SEPARATOR)
            DiscoveryEntry(
                formula = parts.getOrElse(0) { "" },
                title = parts.getOrElse(1) { "" },
                timestampMs = parts.getOrNull(2)?.toLongOrNull() ?: 0L
            )
        }.sortedByDescending { it.timestampMs }
    }

    fun add(formula: String) {
        val stored = prefs.getStringSet(KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        stored += formula
        prefs.edit().putStringSet(KEY, stored).apply()
    }

    private companion object {
        const val PREFS_NAME = "physics_whiteboard_discovery"
        const val KEY = "discovered_formulas"
        const val SEPARATOR = "|"
    }
}
