package com.example.physicswhiteboard

/**
 * Formula metadata shared by the interactive simulation and the legacy
 * BoardItem formula model. Keeping the symbol requirements here prevents the
 * two engines from silently accepting different base combinations.
 */
data class SharedFormulaRule(
    val id: String,
    val formula: String,
    val title: String,
    val requiredSymbols: Set<String>,
    val orderedSymbols: List<String>
)

object SharedFormulaCatalog {
    val rules: List<SharedFormulaRule> = listOf(
        SharedFormulaRule("f_ma", "F = m·a", "Newton 2. Gesetz", setOf("m", "a"), listOf("m", "a")),
        SharedFormulaRule("p_mv", "p = m·v", "Impuls", setOf("m", "v"), listOf("m", "v")),
        SharedFormulaRule("accel_centripetal", "a = v² / r", "Zentripetalbeschleunigung", setOf("v", "r"), listOf("v", "r")),
        SharedFormulaRule("kinetic_half", "E = ½·m·v²", "Kinetische Energie", setOf("m", "½"), listOf("½", "m")),
        SharedFormulaRule("kinetic_half_mv", "E = ½·m·v²", "Kinetische Energie", setOf("m", "v", "½"), listOf("½", "m", "v")),
        SharedFormulaRule("einstein", "E = m·c²", "Masse-Energie-Äquivalenz", setOf("m", "c"), listOf("m", "c")),
        SharedFormulaRule("wien_filter", "v = E/B", "Wien-Filter Durchlassbedingung", setOf("E", "B"), listOf("E", "B")),
        SharedFormulaRule("electric_force", "F = q·E", "Elektrische Kraft", setOf("q", "E"), listOf("q", "E")),
        SharedFormulaRule("electron_gun", "v = √(2qU/m)", "Elektronenkanone: v aus UB", setOf("q", "m"), listOf("q", "m")),
        SharedFormulaRule("period_magnetic", "T = 2π·m / (q·B)", "Umlaufdauer (v-unabhängig!)", setOf("m", "B"), listOf("m", "B")),
        SharedFormulaRule("period_magnetic_q", "T = 2π·m / (q·B)", "Umlaufdauer (v-unabhängig!)", setOf("m", "q", "B"), listOf("m", "q", "B")),
        SharedFormulaRule("hall_voltage", "UH = RH·I·B/d", "Hall-Spannung", setOf("I", "B"), listOf("I", "B")),
        SharedFormulaRule("gravity_simplified", "F = G·M / r²", "Schwerkraft (vereinfacht)", setOf("M", "r"), listOf("M", "r"))
    )

    fun exact(symbols: Set<String>): SharedFormulaRule? =
        rules.firstOrNull { it.requiredSymbols == symbols }
}
