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

            // ── Centripetal force F = m·v² / r ─────────────────────────────
            formula.contains("v² / r") || formula.contains("v²/r") -> {
                val bs = 34f; val ss = 20f; val ny = -18f; val dy = 22f
                gl += g("F", -66f, 0f, bs); gl += g("=", -44f, 0f, 25f)
                gl += g("m", -16f, ny, bs); gl += g("v", 10f, ny, bs)
                gl += g("2", 28f, ny - 10f, ss, MathTokenType.EXPONENT)
                fb += bar(-28f, 40f, 0f, 2.2f)
                gl += g("r", 4f, dy, bs)
                return expr(formula, title, gl, fb, 128f, 74f)
            }

            // ── Kinetic energy E = ½·m·v² ─────────────────────────────────
            formula.contains("½·m·v²") || formula.contains("½mv²") || formula.contains("1/2") -> {
                val bs = 32f; val ss = 20f
                gl += g("E", -76f, 0f, bs); gl += g("=", -54f, 0f, 25f)
                gl += g("½", -30f, 0f, bs); gl += g("·", -14f, 0f, bs)
                gl += g("m", 2f, 0f, bs); gl += g("·", 18f, 0f, bs)
                gl += g("v", 34f, 0f, bs); gl += g("2", 50f, -10f, ss, MathTokenType.EXPONENT)
                return expr(formula, title, gl, fb, 158f, 48f)
            }

            // ── Gravitation F = G·M·m / r² ────────────────────────────────
            // Kept exact: looser matches would hijack G·M·m-style products
            // and normal-mode "GMm" groups into this fraction layout.
            formula.contains("G·M·m / r²") -> {
                val bs = 30f; val ss = 18f; val ny = -18f; val dy = 22f
                gl += g("F", -78f, 0f, bs); gl += g("=", -56f, 0f, 24f)
                gl += g("G", -30f, ny, bs); gl += g("M", -4f, ny, bs); gl += g("m", 24f, ny, bs)
                fb += bar(-40f, 48f, 0f, 2.2f)
                gl += g("r", -2f, dy, bs); gl += g("2", 16f, dy - 8f, ss, MathTokenType.EXPONENT)
                return expr(formula, title, gl, fb, 148f, 76f)
            }

            // ── F = G·M / r² (simplified gravity) ─────────────────────────
            formula.contains("G·M / r²") -> {
                val bs = 30f; val ss = 18f; val ny = -18f; val dy = 22f
                gl += g("F", -60f, 0f, bs); gl += g("=", -38f, 0f, 24f)
                gl += g("G", -12f, ny, bs); gl += g("M", 14f, ny, bs)
                fb += bar(-22f, 36f, 0f, 2.2f)
                gl += g("r", 2f, dy, bs); gl += g("2", 20f, dy - 8f, ss, MathTokenType.EXPONENT)
                return expr(formula, title, gl, fb, 118f, 76f)
            }

            // ── Schwarzschild radius rs = 2GM/c² ──────────────────────────
            formula.contains("2GM") || (formula.contains("c²") && formula.contains("G")) -> {
                val bs = 30f; val ss = 18f; val ny = -18f; val dy = 22f
                gl += g("r", -72f, 0f, bs)
                gl += g("s", -56f, 6f, ss, MathTokenType.SUBSCRIPT)
                gl += g("=", -38f, 0f, 24f)
                gl += g("2", -14f, ny, bs); gl += g("G", 8f, ny, bs); gl += g("M", 34f, ny, bs)
                fb += bar(-24f, 56f, 0f, 2.2f)
                gl += g("c", 10f, dy, bs); gl += g("2", 28f, dy - 8f, ss, MathTokenType.EXPONENT)
                return expr(formula, title, gl, fb, 150f, 76f)
            }

            // ── E = m·c² ──────────────────────────────────────────────────
            formula.contains("m·c²") || formula.contains("mc²") -> {
                val bs = 34f; val ss = 20f
                gl += g("E", -60f, 0f, bs); gl += g("=", -38f, 0f, 25f)
                gl += g("m", -14f, 0f, bs); gl += g("·", 4f, 0f, bs)
                gl += g("c", 22f, 0f, bs)
                gl += g("2", 38f, -10f, ss, MathTokenType.EXPONENT)
                return expr(formula, title, gl, fb, 130f, 48f)
            }

            // ── Wien Filter: v = E/B ──────────────────────────────────────
            formula == "v = E/B" || formula == "E/B" -> {
                val bs = 34f; val ny = -16f; val dy = 20f
                gl += g("v", -32f, 0f, bs); gl += g("=", -10f, 0f, 25f)
                gl += g("E", 16f, ny, bs)
                fb += bar(-4f, 36f, 0f, 2f)
                gl += g("B", 16f, dy, bs)
                return expr(formula, title, gl, fb, 92f, 70f)
            }

            // ── r = m·v / (q·B) — Kreisbahnradius ─────────────────────────
            formula.contains("m·v / (q·B)") || formula.contains("m·v/(q·B)") -> {
                val bs = 30f; val ny = -16f; val dy = 20f
                gl += g("r", -62f, 0f, bs); gl += g("=", -40f, 0f, 25f)
                gl += g("m", -12f, ny, bs); gl += g("v", 12f, ny, bs)
                fb += bar(-22f, 34f, 0f, 2f)
                gl += g("q", -2f, dy, bs); gl += g("B", 22f, dy, bs)
                return expr(formula, title, gl, fb, 120f, 70f)
            }

            // ── T = 2π·m / (q·B) — Umlaufdauer ────────────────────────────
            formula.contains("2π·m / (q·B)") || formula.contains("2πm/(qB)") -> {
                val bs = 28f; val ny = -14f; val dy = 18f
                gl += g("T", -66f, 0f, bs); gl += g("=", -44f, 0f, 24f)
                gl += g("2", -20f, ny, bs); gl += g("π", 0f, ny, bs); gl += g("m", 22f, ny, bs)
                fb += bar(-30f, 40f, 0f, 1.8f)
                gl += g("q", -4f, dy, bs); gl += g("B", 18f, dy, bs)
                return expr(formula, title, gl, fb, 130f, 68f)
            }

            // ── UH = RH·I·B/d — Hall-Spannung ─────────────────────────────
            formula.contains("UH") || formula.contains("RH") -> {
                val bs = 26f; val ss = 17f; val ny = -14f; val dy = 18f
                gl += g("U", -88f, 0f, 28f)
                gl += g("H", -70f, 6f, 18f, MathTokenType.SUBSCRIPT)
                gl += g("=", -50f, 0f, 24f)
                // Numerator: RH · I · B
                gl += g("R", -28f, ny, bs)
                gl += g("H", -12f, ny + 6f, ss, MathTokenType.SUBSCRIPT)
                gl += g("I", 4f, ny, bs)
                gl += g("B", 24f, ny, bs)
                fb += bar(-36f, 40f, 0f, 1.8f)
                // Denominator: d
                gl += g("d", 0f, dy, bs)
                return expr(formula, title, gl, fb, 152f, 68f)
            }

            // ── FL = q·v·B ────────────────────────────────────────────────
            formula.contains("q·v·B") || formula.contains("FL") -> {
                val bs = 32f
                gl += g("F", -80f, 0f, bs)
                gl += g("L", -62f, 6f, 20f, MathTokenType.SUBSCRIPT)
                gl += g("=", -42f, 0f, 25f)
                gl += g("q", -18f, 0f, bs); gl += g("·", -2f, 0f, bs)
                gl += g("v", 14f, 0f, bs); gl += g("·", 30f, 0f, bs)
                gl += g("B", 46f, 0f, bs)
                return expr(formula, title, gl, fb, 158f, 48f)
            }

            // ── F = q·E ───────────────────────────────────────────────────
            formula.contains("q·E") -> {
                val bs = 34f
                gl += g("F", -52f, 0f, bs); gl += g("=", -30f, 0f, 25f)
                gl += g("q", -6f, 0f, bs); gl += g("·", 10f, 0f, bs)
                gl += g("E", 28f, 0f, bs)
                return expr(formula, title, gl, fb, 112f, 48f)
            }

            // ── v = √(2qU/m) — Elektronenkanone ──────────────────────────
            formula.contains("√(2qU") || formula.contains("2qU/m") -> {
                val bs = 26f
                gl += g("v", -76f, 0f, bs); gl += g("=", -54f, 0f, 24f)
                gl += g("√", -32f, 0f, bs)
                gl += g("2", -12f, 0f, bs); gl += g("q", 6f, 0f, bs); gl += g("U", 24f, 0f, bs)
                gl += g("/", 40f, 0f, bs); gl += g("m", 56f, 0f, bs)
                return expr(formula, title, gl, fb, 162f, 48f)
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
                // A lone symbol sits exactly on the body centre.
                x = if (tokens.size == 1) 0f else startX + index * spacing,
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
