package com.example.physicswhiteboard

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class Symbol(
    val char: String,
    val color: Color,
    val name: String
)

sealed class BoardItem(
    open val id: String,
    open var x: Float,
    open var y: Float
) {
    data class Single(
        override val id: String,
        override var x: Float,
        override var y: Float,
        val symbol: Symbol
    ) : BoardItem(id, x, y)

    data class Formula(
        override val id: String,
        override var x: Float,
        override var y: Float,
        val formula: String,
        val title: String,
        val componentSymbols: List<Symbol>,
        val color: Color
    ) : BoardItem(id, x, y)
}

data class FormulaDefinition(
    val id: String,
    val formula: String,
    val title: String,
    val description: String,
    val requiredSymbols: Set<String>,
    val color: Color
)

val BASE_FORMULAS = listOf(
    FormulaDefinition("f_ma", "F = m·a", "Newton's 2nd Law", "Force equals mass times acceleration", setOf("m", "a"), Color(0xFFEA4335)),
    FormulaDefinition("p_mv", "p = m·v", "Linear Momentum", "Momentum equals mass times velocity", setOf("m", "v"), Color(0xFF795548)),
    FormulaDefinition("power_fv", "P = F·v", "Mechanical Power", "Power equals force times velocity", setOf("F", "v"), Color(0xFFE65100)),
    FormulaDefinition("power_et", "P = E / t", "Power from Energy", "Power equals energy consumed per unit time", setOf("E", "t"), Color(0xFF00838F)),
    FormulaDefinition("accel_centripetal", "a = v² / r", "Centripetal Accel.", "Radial acceleration in circular motion", setOf("v", "r"), Color(0xFF8E24AA)),
    FormulaDefinition("angular_vel", "v = ω·r", "Tangential Velocity", "Linear speed from angular velocity and radius", setOf("ω", "r"), Color(0xFF3949AB)),
    FormulaDefinition("period_freq", "ω = 2π / T", "Angular Frequency", "Angular velocity relationship with period", setOf("ω", "T"), Color(0xFF2E7D32)),
    FormulaDefinition("inertia", "I = m·r²", "Moment of Inertia", "Rotational mass distribution", setOf("m", "r"), Color(0xFFC2185B)),
    FormulaDefinition("torque", "τ = F·r", "Torque", "Rotational turning force", setOf("F", "r"), Color(0xFFD84315)),
    FormulaDefinition("impulse", "J = F·t", "Impulse", "Change in linear momentum", setOf("F", "t"), Color(0xFF4E342E)),
    FormulaDefinition("kinematics_v", "v = a·t", "Velocity from Accel.", "Final speed from constant acceleration", setOf("a", "t"), Color(0xFF1565C0)),
    FormulaDefinition("kinematics_d", "d = v·t", "Displacement", "Distance traveled at constant velocity", setOf("v", "t"), Color(0xFF00695C)),
    FormulaDefinition("einstein", "E = m·c²", "Mass-Energy Equiv.", "Equivalence of mass and energy", setOf("E", "m"), Color(0xFF0097A7)),
    FormulaDefinition("force_momentum", "F = Δp / Δt", "Force from Momentum", "Rate of change of momentum", setOf("p", "t"), Color(0xFF5D4037)),
    FormulaDefinition("kinetic_pv", "E = ½·p·v", "Kinetic Energy", "Kinetic energy from momentum and speed", setOf("p", "v"), Color(0xFF6A1B9A))
)

val PALETTE_SYMBOLS = listOf(
    Symbol("m", Color(0xFF1A73E8), "mass"),
    Symbol("a", Color(0xFF34A853), "acceleration"),
    Symbol("F", Color(0xFFEA4335), "force"),
    Symbol("v", Color(0xFF9C27B0), "velocity"),
    Symbol("t", Color(0xFFFF9800), "time"),
    Symbol("E", Color(0xFF00BCD4), "energy"),
    Symbol("p", Color(0xFF795548), "momentum"),
    Symbol("ω", Color(0xFF607D8B), "angular vel."),
    Symbol("r", Color(0xFFE91E63), "radius"),
    Symbol("T", Color(0xFF4CAF50), "period")
)

fun getSymbolByChar(char: String): Symbol? = PALETTE_SYMBOLS.firstOrNull { it.char == char }

fun tryCombineItems(item1: BoardItem, item2: BoardItem): BoardItem.Formula? {
    // Case 1: Two single symbols
    if (item1 is BoardItem.Single && item2 is BoardItem.Single) {
        val charSet = setOf(item1.symbol.char, item2.symbol.char)
        val matchedDef = BASE_FORMULAS.firstOrNull { it.requiredSymbols == charSet }
        if (matchedDef != null) {
            val midX = (item1.x + item2.x) / 2f
            val midY = (item1.y + item2.y) / 2f
            return BoardItem.Formula(
                id = UUID.randomUUID().toString(),
                x = midX,
                y = midY,
                formula = matchedDef.formula,
                title = matchedDef.title,
                componentSymbols = listOf(item1.symbol, item2.symbol),
                color = matchedDef.color
            )
        }
    }

    // Case 2: One Single and one Formula (multi-symbol combinations)
    val single = (if (item1 is BoardItem.Single) item1 else if (item2 is BoardItem.Single) item2 else null)
    val formula = (if (item1 is BoardItem.Formula) item1 else if (item2 is BoardItem.Formula) item2 else null)

    if (single != null && formula != null) {
        val midX = (item1.x + item2.x) / 2f
        val midY = (item1.y + item2.y) / 2f

        // m + (a = v²/r) -> F = m·v²/r
        if (single.symbol.char == "m" && formula.formula.contains("v² / r")) {
            return BoardItem.Formula(
                id = UUID.randomUUID().toString(),
                x = midX,
                y = midY,
                formula = "F = m·v²/r",
                title = "Centripetal Force",
                componentSymbols = formula.componentSymbols + single.symbol,
                color = Color(0xFF673AB7)
            )
        }

        // m + (v = ω·r) -> F = m·ω²·r
        if (single.symbol.char == "m" && formula.formula.contains("ω·r")) {
            return BoardItem.Formula(
                id = UUID.randomUUID().toString(),
                x = midX,
                y = midY,
                formula = "F = m·ω²·r",
                title = "Centripetal Force (Rotational)",
                componentSymbols = formula.componentSymbols + single.symbol,
                color = Color(0xFF3F51B5)
            )
        }

        // r + (p = m·v) -> L = m·v·r
        if (single.symbol.char == "r" && formula.formula.contains("m·v")) {
            return BoardItem.Formula(
                id = UUID.randomUUID().toString(),
                x = midX,
                y = midY,
                formula = "L = m·v·r",
                title = "Angular Momentum",
                componentSymbols = formula.componentSymbols + single.symbol,
                color = Color(0xFF009688)
            )
        }

        // v + (F = m·a) -> P = m·a·v
        if (single.symbol.char == "v" && formula.formula.contains("m·a")) {
            return BoardItem.Formula(
                id = UUID.randomUUID().toString(),
                x = midX,
                y = midY,
                formula = "P = m·a·v",
                title = "Mechanical Power (Dynamic)",
                componentSymbols = formula.componentSymbols + single.symbol,
                color = Color(0xFFFF5722)
            )
        }
    }

    return null
}
