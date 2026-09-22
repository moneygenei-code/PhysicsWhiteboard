package com.example.physicswhiteboard

enum class MathTokenType {
    CHAR,
    EXPONENT,
    FRACTION_BAR,
    SUBSCRIPT,
    PARENTHESIS,
    OPERATOR
}

data class RenderedGlyph(
    val text: String,
    val localX: Float,
    val localY: Float,
    val fontSizeSp: Float,
    val type: MathTokenType = MathTokenType.CHAR
)

data class RenderedFractionBar(
    val startX: Float,
    val endX: Float,
    val y: Float,
    val strokeWidth: Float = 2f
)

data class RenderedExpression(
    val formulaText: String,
    val title: String,
    val glyphs: List<RenderedGlyph>,
    val fractionBars: List<RenderedFractionBar>,
    val width: Float,
    val height: Float
)

// ──────────────────────────────────────────────────────────────────────────────
// FormulaTypesetter — renders physics formulas as positioned glyph lists.
// Each formula is hand-tuned for the parchment academic-paper aesthetic.
// Coordinates are relative to the body's (x,y) centre.
// ──────────────────────────────────────────────────────────────────────────────
object FormulaTypesetter {

    fun buildExpression(formula: String, title: String): RenderedExpression {
        val gl = mutableListOf<RenderedGlyph>()
        val fb = mutableListOf<RenderedFractionBar>()

        when {
            // ── Compact Newton / momentum products ────────────────────────
            // These are kept as complete equations. Previously the fallback
            // renderer stripped the left-hand side and placed the two glyphs
            // so close together that m + a / m + v looked like overlap.
            formula == "F = m·a" || formula == "p = m·v" -> {
                return linearFormula(formula, title, fontSize = 30f, spacing = 27f)
            }

            // ── Centripetal acceleration (must precede the generic fraction) ─
            formula == "a = v² / r" -> {
                val bs = 34f; val ss = 20f; val ny = -16f; val dy = 20f
                gl += g("a", -34f, 0f, bs)
                gl += g("=", -12f, 0f, 25f)
                gl += g("v", 12f, ny, bs); gl += g("2", 30f, ny - 8f, ss, MathTokenType.EXPONENT)
                fb += bar(2f, 46f, 0f, 2f)
                gl += g("r", 20f, dy, bs)
                return expr(formula, title, gl, fb, 92f, 70f)
            }

            // ── Fractions with v² / r ──────────────────────────────────────
            formula.contains("v² / r") || formula.contains("v²/r") -> {
                val bs = 36f; val ss = 22f; val ny = -18f; val dy = 22f
                gl += g("m", -28f, ny, bs); gl += g("v", 0f, ny, bs)
                gl += g("2", 18f, ny - 10f, ss, MathTokenType.EXPONENT)
                fb += bar(-38f, 32f, 0f, 2.2f)
                gl += g("r", -4f, dy, bs)
                return expr(formula, title, gl, fb, 84f, 74f)
            }

            // ── Kinetic energy ½mv² ───────────────────────────────────────
            formula.contains("½·m·v²") || formula.contains("½mv²") || formula.contains("1/2") -> {
                val bs = 34f; val ss = 20f
                gl += g("½", -40f, 0f, bs); gl += g("m", -12f, 0f, bs)
                gl += g("v", 14f, 0f, bs); gl += g("2", 30f, -10f, ss, MathTokenType.EXPONENT)
                return expr(formula, title, gl, fb, 96f, 48f)
            }

            // ── Gravitation F = GMm/r² ────────────────────────────────────
            formula.contains("GMm") || (formula.contains("G·M") && formula.contains("m")) -> {
                val bs = 32f; val ss = 20f; val ny = -18f; val dy = 22f
                gl += g("G", -40f, ny, bs); gl += g("M", -14f, ny, bs); gl += g("m", 16f, ny, bs)
                fb += bar(-50f, 40f, 0f, 2.2f)
                gl += g("r", -10f, dy, bs); gl += g("2", 8f, dy - 8f, ss, MathTokenType.EXPONENT)
                return expr(formula, title, gl, fb, 106f, 76f)
            }

            // ── F = G·M / r² (simplified gravity) ─────────────────────────
            formula.contains("G·M / r²") || (formula.contains("G·M") && formula.contains("r²")) -> {
                val bs = 32f; val ss = 20f; val ny = -18f; val dy = 22f
                gl += g("G", -20f, ny, bs); gl += g("M", 8f, ny, bs)
                fb += bar(-32f, 28f, 0f, 2.2f)
                gl += g("r", -8f, dy, bs); gl += g("2", 10f, dy - 8f, ss, MathTokenType.EXPONENT)
                return expr(formula, title, gl, fb, 78f, 76f)
            }

            // ── Schwarzschild radius 2GM/c² ───────────────────────────────
            formula.contains("2GM") || (formula.contains("c²") && formula.contains("G")) -> {
                val bs = 32f; val ss = 20f; val ny = -18f; val dy = 22f
                gl += g("2", -40f, ny, bs); gl += g("G", -18f, ny, bs); gl += g("M", 10f, ny, bs)
                fb += bar(-48f, 36f, 0f, 2.2f)
                gl += g("c", -10f, dy, bs); gl += g("2", 8f, dy - 8f, ss, MathTokenType.EXPONENT)
                return expr(formula, title, gl, fb, 100f, 76f)
            }

            // ── E = mc² ───────────────────────────────────────────────────
            formula.contains("m·c²") || formula.contains("mc²") -> {
                val bs = 36f; val ss = 22f
                gl += g("m", -20f, 0f, bs); gl += g("c", 8f, 0f, bs)
                gl += g("2", 24f, -10f, ss, MathTokenType.EXPONENT)
                return expr(formula, title, gl, fb, 72f, 48f)
            }

            // ── Wien Filter: v = E/B ──────────────────────────────────────
            formula == "v = E/B" || formula == "E/B" -> {
                val bs = 34f; val ny = -16f; val dy = 20f
                gl += g("E", -2f, ny, bs)
                fb += bar(-22f, 22f, 0f, 2f)
                gl += g("B", -2f, dy, bs)
                return expr(formula, title, gl, fb, 56f, 70f)
            }

            // ── r = mv/(qB) — Kreisbahnradius ─────────────────────────────
            formula.contains("m·v / (q·B)") || formula.contains("m·v/(q·B)") -> {
                val bs = 30f; val ny = -16f; val dy = 20f
                gl += g("m", -28f, ny, bs); gl += g("v", -6f, ny, bs)
                fb += bar(-36f, 28f, 0f, 2f)
                gl += g("q", -16f, dy, bs); gl += g("B", 8f, dy, bs)
                return expr(formula, title, gl, fb, 88f, 70f)
            }

            // ── T = 2πm/(qB) — Umlaufdauer ────────────────────────────────
            formula.contains("2π·m / (q·B)") || formula.contains("2πm/(qB)") -> {
                val bs = 28f; val ss = 18f; val ny = -14f; val dy = 18f
                gl += g("2", -42f, ny, bs); gl += g("π", -22f, ny, bs); gl += g("m", 0f, ny, bs)
                fb += bar(-50f, 18f, 0f, 1.8f)
                gl += g("q", -14f, dy, bs); gl += g("B", 8f, dy, bs)
                return expr(formula, title, gl, fb, 92f, 68f)
            }

            // ── UH = RH·I·B/d — Hall-Spannung ─────────────────────────────
            formula.contains("UH") || formula.contains("RH") -> {
                val bs = 28f; val ss = 18f; val ny = -14f; val dy = 18f
                // Numerator: RH · I · B
                gl += g("R", -44f, ny, bs)
                gl += g("H", -26f, ny + 6f, ss, MathTokenType.SUBSCRIPT)
                gl += g("I", -6f, ny, bs)
                gl += g("B", 14f, ny, bs)
                fb += bar(-52f, 30f, 0f, 1.8f)
                // Denominator: d
                gl += g("d", -8f, dy, bs)
                return expr(formula, title, gl, fb, 102f, 68f)
            }

            // ── FL = q·v·B ────────────────────────────────────────────────
            formula.contains("q·v·B") || formula.contains("FL") -> {
                val bs = 32f
                gl += g("q", -28f, 0f, bs); gl += g("v", -2f, 0f, bs); gl += g("B", 24f, 0f, bs)
                return expr(formula, title, gl, fb, 88f, 48f)
            }

            // ── F = q·E ───────────────────────────────────────────────────
            formula.contains("q·E") -> {
                val bs = 34f
                gl += g("q", -18f, 0f, bs); gl += g("E", 14f, 0f, bs)
                return expr(formula, title, gl, fb, 70f, 48f)
            }

            // ── v = √(2qU/m) — Elektronenkanone ──────────────────────────
            formula.contains("√(2qU") || formula.contains("2qU/m") -> {
                val bs = 26f; val ss = 16f
                gl += g("√", -48f, 0f, bs)
                gl += g("2", -28f, 0f, bs); gl += g("q", -10f, 0f, bs); gl += g("U", 10f, 0f, bs)
                gl += g("/", 26f, 0f, bs); gl += g("m", 40f, 0f, bs)
                return expr(formula, title, gl, fb, 112f, 48f)
            }

            // ── Linear fallback for simple products / single chars ────────
            else -> return linearFormula(formula, title)
        }
    }

    /**
     * Render every non-space character, including the left-hand side and
     * operators. This is intentionally conservative: it gives each glyph a
     * stable slot so the hit/collision bounds can match the visible result.
     */
    private fun linearFormula(
        formula: String,
        title: String,
        fontSize: Float = 30f,
        spacing: Float = 24f
    ): RenderedExpression {
        val tokens = formula.filterNot { it.isWhitespace() }.map { it.toString() }
        if (tokens.isEmpty()) return expr(formula, title, emptyList(), emptyList(), 50f, 48f)

        val totalWidth = maxOf(50f, (tokens.size - 1) * spacing + fontSize)
        val startX = -totalWidth / 2f + fontSize / 2f
        val glyphs = tokens.mapIndexed { index, token ->
            val isExponent = token == "²"
            g(
                text = if (isExponent) "2" else token,
                x = startX + index * spacing,
                y = if (isExponent) -fontSize * 0.32f else 0f,
                sz = if (isExponent) fontSize * 0.62f else fontSize,
                type = if (isExponent) MathTokenType.EXPONENT else MathTokenType.CHAR
            )
        }
        return expr(formula, title, glyphs, emptyList(), totalWidth + 12f, fontSize + 18f)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private fun g(text: String, x: Float, y: Float, sz: Float,
                  type: MathTokenType = MathTokenType.CHAR) = RenderedGlyph(text, x, y, sz, type)

    private fun bar(x1: Float, x2: Float, y: Float, sw: Float = 2f) =
        RenderedFractionBar(x1, x2, y, sw)

    private fun expr(formula: String, title: String,
                     glyphs: List<RenderedGlyph>, bars: List<RenderedFractionBar>,
                     w: Float, h: Float) =
        RenderedExpression(formula, title, glyphs, bars, w, h)
}
