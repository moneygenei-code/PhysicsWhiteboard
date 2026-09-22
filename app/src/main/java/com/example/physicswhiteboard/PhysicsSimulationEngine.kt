package com.example.physicswhiteboard

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.UUID

enum class FieldType {
    NONE,
    ELECTRIC_E,
    MAGNETIC_B
}

enum class ApparatusScene(val displayName: String, val chapterRef: String) {
    FREE_SANDBOX("Freestyle Whiteboard", "Freies Experimentieren"),
    WIEN_FILTER("Wien-Filter", "Kapitel 7 · v = E/B"),
    FADENSTRAHLROHR("Fadenstrahlrohr", "Kapitel 8 · e/m-Bestimmung"),
    MASS_SPECTROMETER("Massenspektrometer (Bainbridge)", "Kapitel 9 · Isotopentrennung"),
    DEFLECTION_CAPACITOR("E-Feld: Längs- & Querfeld", "Kapitel 5 · Parabelbahn"),
    HALL_EFFECT("Hall-Effekt", "Kapitel 10 · Hallspannung")
}

data class SimBody(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var mass: Float = 1.0f,
    var charge: Float = 0f,
    var hasGravity: Boolean = false,
    var hasThrust: Boolean = false,
    var thrustAngle: Float = 0f,
    var hasVelocity: Boolean = false,
    var hasFriction: Boolean = false,
    var isRod: Boolean = false,
    var rodLength: Float = 170f,
    var rodAngle: Float = 0f,
    var isFieldSource: Boolean = false,
    var fieldType: FieldType = FieldType.NONE,
    var fieldRadius: Float = 160f,
    var fieldAngle: Float = 0f, // For E field direction
    var fieldMagnitude: Float = 1500f,
    var bDirectionZ: Int = 1, // +1 for out (dots/⊙), -1 for in (crosses/⊗)
    var char: String = "",
    var renderedExpr: RenderedExpression? = null,
    val componentChars: MutableList<String> = mutableListOf(),
    var isBlackHole: Boolean = false,
    var blackHoleRadius: Float = 0f,
    var age: Float = 0f
)

data class ElectronBeamPoint(
    val x: Float,
    val y: Float,
    val alpha: Float = 1f
)

class PhysicsSimulationEngine {

    val bodies = mutableListOf<SimBody>()
    val electronBeam = mutableListOf<ElectronBeamPoint>()

    var groundY: Float = 800f
    var canvasWidth: Float = 1200f
    var canvasHeight: Float = 1000f

    var activeScene: ApparatusScene = ApparatusScene.FREE_SANDBOX

    // Interactive slider parameters for apparatus experiments
    var expVoltageUb: Float = 300f   // 100 V – 500 V (Fadenstrahlrohr / Wien)
    var expCurrentIs: Float = 0.60f  // 0.1 A – 1.5 A (Helmholtz coils)
    var expPlateVoltageUk: Float = 150f // 50 V – 300 V (Wien / Deflection)

    init {
        loadScene(ApparatusScene.FREE_SANDBOX)
    }

    fun loadScene(scene: ApparatusScene) {
        activeScene = scene
        bodies.clear()
        electronBeam.clear()

        when (scene) {
            ApparatusScene.FREE_SANDBOX -> {
                val m = createLetterBody("m", canvasWidth * 0.35f, groundY - 30f)
                m.hasGravity = true
                bodies.add(m)
                val v = createLetterBody("v", canvasWidth * 0.45f, groundY - 30f)
                bodies.add(v)
            }

            ApparatusScene.WIEN_FILTER -> {
                val centerX = canvasWidth * 0.5f
                val centerY = canvasHeight * 0.45f

                // Downward E-field (plates above/below beam path)
                // E-Field and B-field sharing the exact same region (fieldRadius = 150f)
                val eField = SimBody(
                    x = centerX, y = centerY,
                    isFieldSource = true, fieldType = FieldType.ELECTRIC_E,
                    fieldRadius = 150f,
                    fieldAngle = (PI / 2).toFloat(), // Points downward (+Y in screen coords)
                    fieldMagnitude = 1400f,
                    char = "E",
                    renderedExpr = FormulaTypesetter.buildExpression("E", "Elektrisches Feld")
                )
                bodies.add(eField)

                // B out-of-page (⊙) — Lorentz force on +q moving right is upward (-Y)
                val bField = SimBody(
                    x = centerX, y = centerY,
                    isFieldSource = true, fieldType = FieldType.MAGNETIC_B,
                    fieldRadius = 150f, bDirectionZ = 1,
                    fieldMagnitude = 1400f,
                    char = "B",
                    renderedExpr = FormulaTypesetter.buildExpression("B", "Magnetisches Feld")
                )
                bodies.add(bField)

                // Balanced particle: Fel = FL → a_E = 1400, a_L = omega * vx = (1400 / 100) * vx = 14 * vx
                // Equilibrium velocity: vx = 1400 / 14 = 100f (or scaled appropriately)
                // Let's set fieldMagnitude:
                // For vx = 420f: we want omega * 420 = a_E
                // (B_mag / 100) * 420 = E_mag. If E_mag = 1400, then B_mag = 1400 * 100 / 420 = 333.33f
                // Or if B_mag = 1400, then omega = 14. E_mag = 14 * 420 = 5880f!
                // Let's set E_mag = 5880f and B_mag = 1400f, so for vx = 420: a_E = 5880, FL_acc = 14 * 420 = 5880 -> exact balance!
                eField.fieldMagnitude = 5880f
                bField.fieldMagnitude = 1400f

                // Start particles at the entrance of the field region
                val qPassed = createLetterBody("q", centerX - 140f, centerY)
                qPassed.charge = 1f; qPassed.vx = 420f
                bodies.add(qPassed)

                // Fast particle: FL > Fel → deflects upward (-Y)
                val qFast = createLetterBody("q", centerX - 140f, centerY - 30f)
                qFast.charge = 1f; qFast.vx = 650f
                bodies.add(qFast)

                // Slow particle: Fel > FL → deflects downward (+Y)
                val qSlow = createLetterBody("q", centerX - 140f, centerY + 30f)
                qSlow.charge = 1f; qSlow.vx = 220f
                bodies.add(qSlow)
            }

            ApparatusScene.FADENSTRAHLROHR -> {
                val centerX = canvasWidth * 0.5f
                val centerY = canvasHeight * 0.48f

                val bField = SimBody(
                    x = centerX, y = centerY,
                    isFieldSource = true, fieldType = FieldType.MAGNETIC_B,
                    fieldRadius = 240f, bDirectionZ = 1,
                    fieldMagnitude = expCurrentIs * 2000f,
                    char = "B",
                    renderedExpr = FormulaTypesetter.buildExpression("B", "Helmholtz-Feld")
                )
                bodies.add(bField)

                // Electron from gun — v₀ = √(2eUB/me), enters perpendicular to B
                val electron = createLetterBody("e", centerX - 120f, centerY)
                electron.charge = -1f
                electron.vy = -sqrt(expVoltageUb / 300f) * 380f
                electron.hasVelocity = true
                bodies.add(electron)
            }

            ApparatusScene.MASS_SPECTROMETER -> {
                val startX = canvasWidth * 0.28f
                val startY = canvasHeight * 0.42f

                val eFilter = SimBody(
                    x = startX, y = startY,
                    isFieldSource = true, fieldType = FieldType.ELECTRIC_E,
                    fieldRadius = 120f, fieldAngle = (PI / 2).toFloat(),
                    fieldMagnitude = 1200f,
                    char = "E",
                    renderedExpr = FormulaTypesetter.buildExpression("E", "Filter E-Feld")
                )
                bodies.add(eFilter)

                val analyzerX = startX + 220f
                val analyzerY = startY + 60f
                val bAnalyzer = SimBody(
                    x = analyzerX, y = analyzerY,
                    isFieldSource = true, fieldType = FieldType.MAGNETIC_B,
                    fieldRadius = 220f, bDirectionZ = 1,
                    fieldMagnitude = 1200f,
                    char = "B",
                    renderedExpr = FormulaTypesetter.buildExpression("B", "Analysator-Feld")
                )
                bodies.add(bAnalyzer)

                // ²⁰Ne — lighter, smaller radius r₁
                val ionNe20 = createLetterBody("²⁰Ne", startX - 160f, startY)
                ionNe20.charge = 1f; ionNe20.mass = 20f; ionNe20.vx = 360f
                bodies.add(ionNe20)

                // ²²Ne — heavier, larger radius r₂ > r₁
                val ionNe22 = createLetterBody("²²Ne", startX - 160f, startY - 15f)
                ionNe22.charge = 1f; ionNe22.mass = 22f; ionNe22.vx = 360f
                bodies.add(ionNe22)
            }

            ApparatusScene.DEFLECTION_CAPACITOR -> {
                val centerX = canvasWidth * 0.45f
                val centerY = canvasHeight * 0.45f

                val eField = SimBody(
                    x = centerX, y = centerY,
                    isFieldSource = true, fieldType = FieldType.ELECTRIC_E,
                    fieldRadius = 180f, fieldAngle = (PI / 2).toFloat(),
                    fieldMagnitude = 1600f,
                    char = "E",
                    renderedExpr = FormulaTypesetter.buildExpression("E", "Ablenkfeld")
                )
                bodies.add(eField)

                // Electron fired horizontally — parabolic trajectory inside capacitor
                val electron = createLetterBody("e", centerX - 240f, centerY)
                electron.charge = -1f; electron.vx = 480f
                bodies.add(electron)
            }

            ApparatusScene.HALL_EFFECT -> {
                val centerX = canvasWidth * 0.48f
                val centerY = canvasHeight * 0.45f

                // Conducting plate
                val conductor = SimBody(
                    x = centerX, y = centerY,
                    isRod = true, rodLength = 260f, rodAngle = 0f,
                    char = "t",
                    renderedExpr = FormulaTypesetter.buildExpression("I", "Leiterplättchen")
                )
                bodies.add(conductor)

                // Perpendicular B-field (into page)
                val bField = SimBody(
                    x = centerX, y = centerY,
                    isFieldSource = true, fieldType = FieldType.MAGNETIC_B,
                    fieldRadius = 160f, bDirectionZ = -1, // Into page (×)
                    fieldMagnitude = 1800f,
                    char = "B",
                    renderedExpr = FormulaTypesetter.buildExpression("B", "Magnetfeld ⊗")
                )
                bodies.add(bField)

                // Mobile electrons drifting rightward along conductor (conventional I is rightward)
                for (i in 0..5) {
                    val e = createLetterBody("e", centerX - 120f + (i * 40f), centerY)
                    e.charge = -1f; e.vx = 85f
                    bodies.add(e)
                }
            }
        }
    }

    fun createLetterBody(char: String, x: Float, y: Float): SimBody {
        val expr = FormulaTypesetter.buildExpression(char, char)
        val body = SimBody(x = x, y = y, char = char, renderedExpr = expr,
            componentChars = mutableListOf(char))

        when (char) {
            "m"  -> body.mass = 1.0f
            "M"  -> body.mass = 3.0f
            "g"  -> body.hasGravity = true
            "a"  -> { body.hasThrust = true; body.thrustAngle = 0f }
            "v"  -> body.hasVelocity = true
            "r"  -> {} // Radius symbol — no special physics
            "μ"  -> body.hasFriction = true
            "t"  -> { body.isRod = true; body.rodLength = 170f }
            "q"  -> body.charge = 1.0f
            "e"  -> { body.charge = -1.0f; body.mass = 0.5f }
            "B"  -> { body.isFieldSource = true; body.fieldType = FieldType.MAGNETIC_B; body.fieldRadius = 150f }
            "E"  -> { body.isFieldSource = true; body.fieldType = FieldType.ELECTRIC_E; body.fieldRadius = 160f; body.fieldAngle = 0f }
            "I"  -> {} // Current — visual only
            "½"  -> {} // Half coefficient
            "c"  -> {} // Speed of light coefficient
            "G"  -> body.mass = 2.0f // Gravitational constant — heavier for visual
        }
        return body
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Physics step — 60 FPS, dt ≈ 0.016 s
    // ──────────────────────────────────────────────────────────────────────────
    fun step(dt: Float) {
        val bFields = bodies.filter { it.isFieldSource && it.fieldType == FieldType.MAGNETIC_B }
        val eFields = bodies.filter { it.isFieldSource && it.fieldType == FieldType.ELECTRIC_E }

        // 4. Fade beam trail efficiently
        if (electronBeam.isNotEmpty()) {
            val iter = electronBeam.iterator()
            while (iter.hasNext()) {
                val pt = iter.next()
                if (pt.alpha <= 0.08f) {
                    iter.remove()
                }
            }
            for (i in electronBeam.indices) {
                electronBeam[i] = electronBeam[i].copy(alpha = electronBeam[i].alpha - dt * 2.5f)
            }
        }

        for (body in bodies) {
            body.age += dt
            if (body.isFieldSource || body.isRod) continue

            // 1. Gravity: F = mg
            if (body.hasGravity) body.vy += 2200f * dt

            // 2. Thrust: F = ma in thrust direction
            if (body.hasThrust) {
                body.vx += 1300f * cos(body.thrustAngle) * dt
                body.vy += 1300f * sin(body.thrustAngle) * dt
            }

            // 3. Electromagnetic forces (only if charged)
            if (body.charge != 0f) {
                // Electric force: F = qE
                for (ef in eFields) {
                    if (Math.abs(body.x - ef.x) <= ef.fieldRadius &&
                        Math.abs(body.y - ef.y) <= ef.fieldRadius) {
                        val acc = (ef.fieldMagnitude * body.charge / body.mass) * dt
                        body.vx += cos(ef.fieldAngle) * acc
                        body.vy += sin(ef.fieldAngle) * acc
                    }
                }

                // Magnetic Lorentz force: F = q(v × B)
                // In right-handed coords: v=(vx, 0, 0), B=(0, 0, B_z) -> F = q*(0, -vx*B_z, 0).
                // In screen coords (+y downward), B out of screen (bz = +1):
                // v x B gives upward force (-y direction).
                // For exact rotation preserving |v|:
                // dvy/dt = -omega * vx -> rotation angle dθ = -omega * dt.
                for (bf in bFields) {
                    val dist = hypot(body.x - bf.x, body.y - bf.y)
                    if (dist <= bf.fieldRadius && hypot(body.vx, body.vy) > 1f) {
                        val omega = (bf.fieldMagnitude / 100f) *
                                body.charge * bf.bDirectionZ.toFloat() / body.mass
                        val dθ = -omega * dt
                        val cθ = cos(dθ); val sθ = sin(dθ)
                        val nvx = cθ * body.vx - sθ * body.vy
                        val nvy = sθ * body.vx + cθ * body.vy
                        body.vx = nvx; body.vy = nvy
                    }
                }
            }

            // 4. Damping
            if (body.hasFriction) {
                // Kinetic friction (μ): significant sliding drag
                val factor = (1f - 8f * dt).coerceAtLeast(0.01f)
                body.vx *= factor; body.vy *= factor
            } else {
                // Near-vacuum: ~0.3% speed loss per frame @60fps
                val drag = 1f - 0.018f * dt
                body.vx *= drag; body.vy *= drag
            }

            // 5. Euler integration
            body.x += body.vx * dt
            body.y += body.vy * dt

            // Emit glow trail for moving charges (cap tightly at 120 points to preserve 120 FPS performance)
            if (body.charge != 0f && hypot(body.vx, body.vy) > 25f && electronBeam.size < 120) {
                electronBeam.add(ElectronBeamPoint(body.x, body.y, 0.88f))
            }

            // 6. Boundary collisions
            val hH = 22f; val hW = 24f
            if (body.y + hH > groundY) {
                body.y = groundY - hH
                body.vy = -body.vy * 0.42f
                if (Math.abs(body.vy) < 55f) body.vy = 0f
                body.vx *= 0.88f
            }
            if (body.x - hW < 10f) { body.x = 10f + hW; body.vx = -body.vx * 0.62f }
            else if (body.x + hW > canvasWidth - 10f) { body.x = canvasWidth - 10f - hW; body.vx = -body.vx * 0.62f }
            if (body.y - hH < 10f) { body.y = 10f + hH; body.vy = -body.vy * 0.62f }
        }

        handleCollisions()
    }

    private fun handleCollisions() {
        for (i in bodies.indices) {
            val b1 = bodies[i]
            if (b1.isFieldSource) continue
            for (j in i + 1 until bodies.size) {
                val b2 = bodies[j]
                if (b2.isFieldSource) continue
                val dx = b2.x - b1.x; val dy = b2.y - b1.y
                val dist = hypot(dx, dy); val minDist = 48f
                if (dist in 1f..minDist) {
                    val nx = dx / dist; val ny = dy / dist
                    val overlap = minDist - dist
                    b1.x -= nx * overlap * 0.5f; b1.y -= ny * overlap * 0.5f
                    b2.x += nx * overlap * 0.5f; b2.y += ny * overlap * 0.5f
                    val relVel = (b1.vx - b2.vx) * nx + (b1.vy - b2.vy) * ny
                    if (relVel > 0) {
                        val totalMass = b1.mass + b2.mass
                        val impulse = 1.6f * relVel / totalMass
                        b1.vx -= impulse * b2.mass * nx; b1.vy -= impulse * b2.mass * ny
                        b2.vx += impulse * b1.mass * nx; b2.vy += impulse * b1.mass * ny
                    }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Symbol fusion — drag two symbols together to form physics equations
    // All key Klausur formulas are covered here.
    // ──────────────────────────────────────────────────────────────────────────
    fun tryFuseSymbols(b1: SimBody, b2: SimBody): Boolean {
        val chars = (b1.componentChars + b2.componentChars).distinct()
        val cs = chars.toSet()

        when {
            // ── Newton / Kinematics ──────────────────────────────────────────
            cs == setOf("m", "a") ->
                fuseInto(b1, b2, "F = m·a", "Newton 2. Gesetz", chars)

            cs == setOf("m", "v") ->
                fuseInto(b1, b2, "p = m·v", "Impuls", chars)

            cs == setOf("v", "r") ->
                fuseInto(b1, b2, "a = v² / r", "Zentripetalbeschleunigung", chars)

            cs.containsAll(listOf("m", "v", "r")) ->
                fuseInto(b1, b2, "F = m·v² / r", "Zentripetalkraft", chars)

            // ── Energy ──────────────────────────────────────────────────────
            (cs == setOf("m", "½") || cs == setOf("m", "v", "½")) ->
                fuseInto(b1, b2, "E = ½·m·v²", "Kinetische Energie", chars)

            cs == setOf("m", "c") ->
                fuseInto(b1, b2, "E = m·c²", "Masse-Energie-Äquivalenz", chars)

            cs.containsAll(listOf("G", "M", "m")) ->
                fuseInto(b1, b2, "F = G·M·m / r²", "Gravitationsgesetz", chars)

            cs.containsAll(listOf("G", "M", "c")) -> {
                fuseInto(b1, b2, "2GM / c²", "Schwarzschild-Radius", chars)
                b1.isBlackHole = true; b1.blackHoleRadius = 55f
            }

            // ── Electricity & Magnetism — Klausur core ────────────────────
            // Wien-Filter: v = E/B
            cs == setOf("E", "B") ->
                fuseInto(b1, b2, "v = E/B", "Wien-Filter Durchlassbedingung", chars)

            // Lorentz force: FL = qvB
            cs.containsAll(listOf("q", "v", "B")) ->
                fuseInto(b1, b2, "FL = q·v·B", "Lorentzkraft", chars)

            // Kreisbahn: r = mv/(qB)
            cs.containsAll(listOf("m", "v", "q", "B")) ->
                fuseInto(b1, b2, "r = m·v / (q·B)", "Kreisbahnradius im B-Feld", chars)

            // Umlaufzeit: T = 2πm/(qB)  [m + B → T = 2πm/qB]
            cs == setOf("m", "B") ->
                fuseInto(b1, b2, "T = 2π·m / (q·B)", "Umlaufdauer (v-unabhängig!)", chars)

            // Electric force: F = qE
            cs == setOf("q", "E") ->
                fuseInto(b1, b2, "F = q·E", "Elektrische Kraft", chars)

            // Electron gun: v = √(2qU/m)  [q + m]
            cs == setOf("q", "m") ->
                fuseInto(b1, b2, "v = √(2qU/m)", "Elektronenkanone: v aus UB", chars)

            // Hall voltage: UH = RH·IB/d  [I + B]
            cs == setOf("I", "B") ->
                fuseInto(b1, b2, "UH = RH·I·B/d", "Hall-Spannung", chars)

            // Centripetal force from gravity: FG = FZP  [M + r]
            cs == setOf("M", "r") ->
                fuseInto(b1, b2, "F = G·M / r²", "Schwerkraft (vereinfacht)", chars)

            // ── Physical objects ──────────────────────────────────────────
            cs.containsAll(listOf("v", "t")) -> {
                b1.isRod = true; b1.rodLength = 180f; b1.char = "t"
                b1.renderedExpr = FormulaTypesetter.buildExpression("t", "Stab / Leiter")
                bodies.remove(b2)
            }

            cs.containsAll(listOf("q", "t")) -> {
                b1.char = "I"
                b1.renderedExpr = FormulaTypesetter.buildExpression("I", "Stromstärke (I = q/t)")
                b1.componentChars.clear(); b1.componentChars.add("I")
                bodies.remove(b2)
            }

            else -> return false
        }
        return true
    }

    private fun fuseInto(b1: SimBody, b2: SimBody, formula: String, title: String, allChars: List<String>) {
        val midX = (b1.x + b2.x) / 2f; val midY = (b1.y + b2.y) / 2f
        b1.x = midX; b1.y = midY
        b1.char = formula
        b1.renderedExpr = FormulaTypesetter.buildExpression(formula, title)
        b1.componentChars.clear(); b1.componentChars.addAll(allChars)
        b1.mass = maxOf(b1.mass, b2.mass)
        b1.hasGravity = b1.hasGravity || b2.hasGravity
        b1.hasVelocity = b1.hasVelocity || b2.hasVelocity
        bodies.remove(b2)
    }

    fun splitFormula(body: SimBody): List<SimBody> {
        val result = mutableListOf<SimBody>()
        if (body.componentChars.size <= 1) return result
        bodies.remove(body)
        val n = body.componentChars.size
        for (i in 0 until n) {
            val ch = body.componentChars[i]
            val offX = (i - (n - 1) / 2f) * 44f
            val newBody = createLetterBody(ch, body.x + offX, body.y - 10f)
            newBody.vx = (i - (n - 1) / 2f) * 120f
            newBody.vy = -140f
            bodies.add(newBody)
            result.add(newBody)
        }
        return result
    }
}
