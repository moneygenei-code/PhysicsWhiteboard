package com.example.physicswhiteboard

import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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

object PhysicsConstants {
    const val gravityAcceleration = 2200f
    const val thrustAcceleration = 1300f
    const val nearVacuumDrag = 0.018f
    const val frictionStrength = 8f
    const val groundRestitution = 0.42f
    const val wallRestitution = 0.62f
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
    var fieldHalfWidth: Float = 100f,
    var fieldAngle: Float = 0f,
    var fieldMagnitude: Float = 1500f,
    var bDirectionZ: Int = 1,
    var char: String = "",
    var renderedExpr: RenderedExpression? = null,
    val componentChars: MutableList<String> = mutableListOf(),
    var isBlackHole: Boolean = false,
    var blackHoleRadius: Float = 0f,
    var age: Float = 0f,
    /** Pointer interaction owns this body while true; the physics loop leaves it alone. */
    var isBeingDragged: Boolean = false,
    /** A short grace period prevents a freshly fused body from being re-collided. */
    var collisionGraceFrames: Int = 0
)

data class ElectronBeamPoint(
    val x: Float,
    val y: Float,
    val alpha: Float = 1f
)

private data class FusionPlan(
    val formula: String,
    val title: String,
    val components: List<String>,
    val createsRod: Boolean = false,
    val createsCurrent: Boolean = false
)

data class SimulationSnapshot(
    val bodies: List<SimBody>,
    val electronBeam: List<ElectronBeamPoint>
)

class PhysicsSimulationEngine {

    val bodies = mutableListOf<SimBody>()
    val electronBeam = mutableListOf<ElectronBeamPoint>()

    var groundY: Float = 800f
    var canvasWidth: Float = 1200f
    var canvasHeight: Float = 1000f
    var activeScene: ApparatusScene = ApparatusScene.FREE_SANDBOX
    var isPaused: Boolean = false
        private set

    // Interactive slider parameters for apparatus experiments
    var expVoltageUb: Float = 300f
    var expCurrentIs: Float = 0.60f
    var expPlateVoltageUk: Float = 150f

    private val undoStack = ArrayDeque<SimulationSnapshot>()
    private val redoStack = ArrayDeque<SimulationSnapshot>()
    private val maxHistoryEntries = 40

    init {
        loadScene(ApparatusScene.FREE_SANDBOX)
    }

    fun togglePaused() {
        isPaused = !isPaused
    }

    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    fun loadScene(scene: ApparatusScene) {
        activeScene = scene
        isPaused = false
        bodies.clear()
        electronBeam.clear()
        clearUndoHistory()

        when (scene) {
            ApparatusScene.FREE_SANDBOX -> {
                val m = createLetterBody("m", canvasWidth * 0.35f, groundY - 30f)
                bodies.add(m)
                bodies.add(createLetterBody("v", canvasWidth * 0.45f, groundY - 30f))
            }

            ApparatusScene.WIEN_FILTER -> {
                val centerX = canvasWidth * 0.5f
                val centerY = canvasHeight * 0.45f

                val eField = createElectricFieldSource(
                    x = centerX,
                    y = centerY,
                    radius = 150f,
                    angle = (PI / 2).toFloat(),
                    magnitude = 5880f,
                    label = "E",
                    title = "Elektrisches Feld"
                )
                bodies.add(eField)

                val bField = SimBody(
                    x = centerX, y = centerY,
                    isFieldSource = true, fieldType = FieldType.MAGNETIC_B,
                    fieldRadius = 150f, bDirectionZ = 1,
                    fieldMagnitude = 1400f,
                    char = "B",
                    componentChars = mutableListOf("B"),
                    renderedExpr = FormulaTypesetter.buildExpression("B", "Magnetisches Feld")
                )
                bodies.add(bField)

                // Balanced particle: Fel = FL for vx = 420 in the scaled scene.
                val qPassed = createLetterBody("q", centerX - 100f, centerY)
                qPassed.charge = 1f
                qPassed.vx = 420f
                bodies.add(qPassed)

                val qFast = createLetterBody("q", centerX - 100f, centerY - 110f)
                qFast.charge = 1f
                qFast.vx = 650f
                bodies.add(qFast)

                val qSlow = createLetterBody("q", centerX - 100f, centerY + 110f)
                qSlow.charge = 1f
                qSlow.vx = 220f
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
                    componentChars = mutableListOf("B"),
                    renderedExpr = FormulaTypesetter.buildExpression("B", "Helmholtz-Feld")
                )
                bodies.add(bField)

                val electron = createLetterBody("e", centerX - 120f, centerY)
                electron.charge = -1f
                electron.vy = -sqrt(expVoltageUb / 300f) * 380f
                electron.hasVelocity = true
                bodies.add(electron)
            }

            ApparatusScene.MASS_SPECTROMETER -> {
                val startX = canvasWidth * 0.28f
                val startY = canvasHeight * 0.42f

                bodies.add(
                    createElectricFieldSource(
                        x = startX,
                        y = startY,
                        radius = 120f,
                        angle = (PI / 2).toFloat(),
                        magnitude = 1200f,
                        label = "E",
                        title = "Filter E-Feld"
                    )
                )

                val analyzerX = startX + 220f
                val analyzerY = startY + 60f
                bodies.add(
                    SimBody(
                        x = analyzerX, y = analyzerY,
                        isFieldSource = true, fieldType = FieldType.MAGNETIC_B,
                        fieldRadius = 220f, bDirectionZ = 1,
                        fieldMagnitude = 1200f,
                        char = "B",
                        componentChars = mutableListOf("B"),
                        renderedExpr = FormulaTypesetter.buildExpression("B", "Analysator-Feld")
                    )
                )

                val ionNe20 = createLetterBody("²⁰Ne", startX - 160f, startY)
                ionNe20.charge = 1f
                ionNe20.mass = 20f
                ionNe20.vx = 360f
                bodies.add(ionNe20)

                val ionNe22 = createLetterBody("²²Ne", startX - 160f, startY - 15f)
                ionNe22.charge = 1f
                ionNe22.mass = 22f
                ionNe22.vx = 360f
                bodies.add(ionNe22)
            }

            ApparatusScene.DEFLECTION_CAPACITOR -> {
                val centerX = canvasWidth * 0.45f
                val centerY = canvasHeight * 0.45f
                bodies.add(
                    createElectricFieldSource(
                        x = centerX,
                        y = centerY,
                        radius = 180f,
                        angle = (PI / 2).toFloat(),
                        magnitude = 1600f,
                        label = "E",
                        title = "Ablenkfeld"
                    )
                )

                val electron = createLetterBody("e", centerX - 240f, centerY)
                electron.charge = -1f
                electron.vx = 480f
                bodies.add(electron)
            }

            ApparatusScene.HALL_EFFECT -> {
                val centerX = canvasWidth * 0.48f
                val centerY = canvasHeight * 0.45f

                bodies.add(
                    SimBody(
                        x = centerX, y = centerY,
                        isRod = true, rodLength = 260f, rodAngle = 0f,
                        char = "t",
                        renderedExpr = FormulaTypesetter.buildExpression("I", "Leiterplättchen")
                    )
                )

                bodies.add(
                    SimBody(
                        x = centerX, y = centerY,
                        isFieldSource = true, fieldType = FieldType.MAGNETIC_B,
                        fieldRadius = 160f, bDirectionZ = -1,
                        fieldMagnitude = 1800f,
                        char = "B",
                        componentChars = mutableListOf("B"),
                        renderedExpr = FormulaTypesetter.buildExpression("B", "Magnetfeld ⊗")
                    )
                )

                for (i in 0..5) {
                    val electron = createLetterBody("e", centerX - 120f + (i * 40f), centerY)
                    electron.charge = -1f
                    electron.vx = 85f
                    bodies.add(electron)
                }
            }
        }
    }

    private fun createElectricFieldSource(
        x: Float,
        y: Float,
        radius: Float,
        angle: Float,
        magnitude: Float,
        label: String,
        title: String
    ): SimBody {
        return SimBody(
            x = x,
            y = y,
            isFieldSource = true,
            fieldType = FieldType.ELECTRIC_E,
            fieldRadius = radius,
            // A rectangular region makes a quarter-turn visible; unlike the
            // old drawRect, this same geometry also drives force membership.
            fieldHalfWidth = radius * 0.67f,
            fieldAngle = angle,
            fieldMagnitude = magnitude,
            char = label,
            componentChars = mutableListOf(label),
            renderedExpr = FormulaTypesetter.buildExpression(label, title)
        )
    }

    fun createLetterBody(char: String, x: Float, y: Float): SimBody {
        val expr = FormulaTypesetter.buildExpression(char, char)
        val body = SimBody(
            x = x,
            y = y,
            char = char,
            renderedExpr = expr,
            componentChars = mutableListOf(char)
        )

        when (char) {
            "m" -> {
                body.mass = 1.0f
                body.hasGravity = true
            }
            "M" -> {
                body.mass = 3.0f
                body.hasGravity = true
            }
            "g" -> body.hasGravity = true
            "a" -> {
                body.hasThrust = true
                body.thrustAngle = 0f
            }
            "v" -> body.hasVelocity = true
            "r" -> Unit
            "μ" -> body.hasFriction = true
            "t" -> {
                body.isRod = true
                body.rodLength = 170f
            }
            "q" -> body.charge = 1.0f
            "e" -> {
                body.charge = -1.0f
                body.mass = 0.5f
            }
            "B" -> {
                body.isFieldSource = true
                body.fieldType = FieldType.MAGNETIC_B
                body.fieldRadius = 150f
            }
            "E" -> {
                body.isFieldSource = true
                body.fieldType = FieldType.ELECTRIC_E
                body.fieldRadius = 160f
                body.fieldHalfWidth = 100f
                body.fieldAngle = 0f
            }
            "I", "½", "c" -> Unit
            "G" -> body.mass = 2.0f
        }
        return body
    }

    fun beginDrag(body: SimBody) {
        body.isBeingDragged = true
        body.vx = 0f
        body.vy = 0f
        body.collisionGraceFrames = 0
    }

    fun moveDraggedBody(body: SimBody, x: Float, y: Float) {
        if (!body.isBeingDragged) return
        body.x = x
        body.y = y
        body.vx = 0f
        body.vy = 0f
    }

    /** Keep the two glyphs readable during the last few pixels before fusion. */
    fun snapDraggedBodyOutsideTarget(dragged: SimBody, target: SimBody) {
        if (!dragged.isBeingDragged) return
        val dx = dragged.x - target.x
        val dy = dragged.y - target.y
        val distance = hypot(dx, dy)
        val minimumDistance = BodyGeometry.fusionPreviewDistance(dragged, target)
        if (distance >= minimumDistance) return

        val (nx, ny) = if (distance > 0.001f) {
            dx / distance to dy / distance
        } else if (((dragged.id.hashCode() xor target.id.hashCode()) and 1) == 0) {
            1f to 0f
        } else {
            0f to 1f
        }
        dragged.x = target.x + nx * minimumDistance
        dragged.y = target.y + ny * minimumDistance
        dragged.vx = 0f
        dragged.vy = 0f
    }

    fun endDrag(body: SimBody, releaseVx: Float = 0f, releaseVy: Float = 0f) {
        body.isBeingDragged = false
        body.vx = releaseVx
        body.vy = releaseVy
        body.collisionGraceFrames = max(body.collisionGraceFrames, 2)
    }

    fun cancelDrag(body: SimBody) {
        endDrag(body)
    }

    fun findFusionTarget(dragged: SimBody): SimBody? {
        return bodies
            .asSequence()
            .filter { it.id != dragged.id }
            .filter { canFuseSymbols(dragged, it) }
            .filter { BodyGeometry.isWithinFusionDistance(dragged, it) }
            .minByOrNull { hypot(dragged.x - it.x, dragged.y - it.y) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Physics step — stable for 60/120 FPS and deterministic in unit tests.
    // ──────────────────────────────────────────────────────────────────────────
    fun step(dt: Float) {
        if (isPaused) return
        val safeDt = dt.coerceIn(0.001f, 0.033f)
        val bFields = bodies.filter { it.isFieldSource && it.fieldType == FieldType.MAGNETIC_B }
        val eFields = bodies.filter { it.isFieldSource && it.fieldType == FieldType.ELECTRIC_E }

        if (electronBeam.isNotEmpty()) {
            val iter = electronBeam.iterator()
            while (iter.hasNext()) {
                if (iter.next().alpha <= 0.08f) iter.remove()
            }
            for (i in electronBeam.indices) {
                electronBeam[i] = electronBeam[i].copy(
                    alpha = (electronBeam[i].alpha - safeDt * 2.5f).coerceAtLeast(0f)
                )
            }
        }

        for (body in bodies) {
            body.age += safeDt
            if (body.collisionGraceFrames > 0) body.collisionGraceFrames--
            if (body.isFieldSource || body.isRod || body.isBeingDragged) continue

            if (body.hasGravity) body.vy += PhysicsConstants.gravityAcceleration * safeDt

            if (body.hasThrust) {
                body.vx += PhysicsConstants.thrustAcceleration * cos(body.thrustAngle) * safeDt
                body.vy += PhysicsConstants.thrustAcceleration * sin(body.thrustAngle) * safeDt
            }

            if (body.charge != 0f) {
                for (ef in eFields) {
                    if (FieldGeometry.contains(ef, body.x, body.y)) {
                        val acceleration = (ef.fieldMagnitude * body.charge /
                                body.mass.coerceAtLeast(0.001f)) * safeDt
                        body.vx += cos(ef.fieldAngle) * acceleration
                        body.vy += sin(ef.fieldAngle) * acceleration
                    }
                }

                for (bf in bFields) {
                    val dist = hypot(body.x - bf.x, body.y - bf.y)
                    if (dist <= bf.fieldRadius && hypot(body.vx, body.vy) > 1f) {
                        val omega = (bf.fieldMagnitude / 100f) *
                                body.charge * bf.bDirectionZ.toFloat() /
                                        body.mass.coerceAtLeast(0.001f)
                        val deltaAngle = -omega * safeDt
                        val c = cos(deltaAngle)
                        val s = sin(deltaAngle)
                        val nextVx = c * body.vx - s * body.vy
                        val nextVy = s * body.vx + c * body.vy
                        body.vx = nextVx
                        body.vy = nextVy
                    }
                }
            }

            if (body.hasFriction) {
                val factor = (1f - PhysicsConstants.frictionStrength * safeDt).coerceAtLeast(0.01f)
                body.vx *= factor
                body.vy *= factor
            } else {
                val drag = 1f - PhysicsConstants.nearVacuumDrag * safeDt
                body.vx *= drag
                body.vy *= drag
            }

            body.x += body.vx * safeDt
            body.y += body.vy * safeDt

            if (body.charge != 0f && hypot(body.vx, body.vy) > 25f && electronBeam.size < 120) {
                electronBeam.add(ElectronBeamPoint(body.x, body.y, 0.88f))
            }

            val extents = BodyGeometry.collisionHalfExtents(body)
            val halfHeight = extents.halfHeight
            val halfWidth = extents.halfWidth
            if (body.y + halfHeight > groundY) {
                body.y = groundY - halfHeight
                body.vy = -body.vy * PhysicsConstants.groundRestitution
                if (abs(body.vy) < 55f) body.vy = 0f
                body.vx *= 0.88f
            }
            if (body.x - halfWidth < 10f) {
                body.x = 10f + halfWidth
                body.vx = -body.vx * PhysicsConstants.wallRestitution
            } else if (body.x + halfWidth > canvasWidth - 10f) {
                body.x = canvasWidth - 10f - halfWidth
                body.vx = -body.vx * PhysicsConstants.wallRestitution
            }
            if (body.y - halfHeight < 10f) {
                body.y = 10f + halfHeight
                body.vy = -body.vy * PhysicsConstants.wallRestitution
            }
        }

        handleCollisions()
    }

    private fun handleCollisions() {
        for (i in bodies.indices) {
            val first = bodies[i]
            if (first.isFieldSource || first.isBeingDragged || first.collisionGraceFrames > 0) continue

            for (j in i + 1 until bodies.size) {
                val second = bodies[j]
                if (second.isFieldSource || second.isBeingDragged || second.collisionGraceFrames > 0) continue

                val dx = second.x - first.x
                val dy = second.y - first.y
                val distance = hypot(dx, dy)
                val minimumDistance = BodyGeometry.collisionRadius(first) +
                        BodyGeometry.collisionRadius(second)
                if (distance >= minimumDistance) continue

                val (nx, ny) = if (distance > 0.001f) {
                    dx / distance to dy / distance
                } else {
                    // Pointer movement can place two bodies at exactly one point.
                    // A stable fallback makes the separation deterministic.
                    if (((first.id.hashCode() xor second.id.hashCode()) and 1) == 0) {
                        1f to 0f
                    } else {
                        0f to 1f
                    }
                }

                val overlap = minimumDistance - distance
                first.x -= nx * overlap * 0.5f
                first.y -= ny * overlap * 0.5f
                second.x += nx * overlap * 0.5f
                second.y += ny * overlap * 0.5f

                val relativeVelocity = (first.vx - second.vx) * nx +
                        (first.vy - second.vy) * ny
                if (relativeVelocity > 0f) {
                    val totalMass = (first.mass + second.mass).coerceAtLeast(0.001f)
                    val impulse = 1.6f * relativeVelocity / totalMass
                    first.vx -= impulse * second.mass * nx
                    first.vy -= impulse * second.mass * ny
                    second.vx += impulse * first.mass * nx
                    second.vy += impulse * first.mass * ny
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Symbol fusion — drag symbols together to form physics equations.
    // ──────────────────────────────────────────────────────────────────────────
    fun canFuseSymbols(first: SimBody, second: SimBody): Boolean =
        fusionPlan((first.componentChars + second.componentChars).toSet()) != null

    fun tryFuseSymbols(first: SimBody, second: SimBody): Boolean {
        if (first.id == second.id) return false
        val componentSet = (first.componentChars + second.componentChars).toSet()
        val plan = fusionPlan(componentSet) ?: return false
        val components = mergeComponents(plan.components, componentSet)
        fuseInto(first, second, plan, components)
        return true
    }

    private fun catalogFusionPlan(chars: Set<String>): FusionPlan? =
        SharedFormulaCatalog.exact(chars)?.let { rule ->
            FusionPlan(rule.formula, rule.title, rule.orderedSymbols)
        }

    private fun fusionPlan(chars: Set<String>): FusionPlan? {
        return when {
            // More specific combinations must come before their subsets.
            chars.containsAll(listOf("m", "v", "q", "B")) ->
                FusionPlan("r = m·v / (q·B)", "Kreisbahnradius im B-Feld", listOf("m", "v", "q", "B"))

            chars.containsAll(listOf("q", "v", "B")) ->
                FusionPlan("FL = q·v·B", "Lorentzkraft", listOf("q", "v", "B"))

            chars.containsAll(listOf("m", "v", "r")) ->
                FusionPlan("F = m·v² / r", "Zentripetalkraft", listOf("m", "v", "r"))

            chars.containsAll(listOf("G", "M", "c")) ->
                FusionPlan("2GM / c²", "Schwarzschild-Radius", listOf("G", "M", "c"))

            chars.containsAll(listOf("G", "M", "m")) ->
                FusionPlan("F = G·M·m / r²", "Gravitationsgesetz", listOf("G", "M", "m"))

            chars == setOf("m", "a") ->
                catalogFusionPlan(chars)

            chars == setOf("m", "v") ->
                catalogFusionPlan(chars)

            chars == setOf("v", "r") ->
                catalogFusionPlan(chars)

            chars == setOf("m", "½") || chars == setOf("m", "v", "½") ->
                catalogFusionPlan(chars)

            chars == setOf("m", "c") ->
                catalogFusionPlan(chars)

            chars == setOf("E", "B") ->
                catalogFusionPlan(chars)

            chars == setOf("m", "B") ->
                catalogFusionPlan(chars)

            chars == setOf("q", "E") ->
                catalogFusionPlan(chars)

            chars == setOf("q", "m") ->
                catalogFusionPlan(chars)

            chars == setOf("I", "B") ->
                catalogFusionPlan(chars)

            chars == setOf("M", "r") ->
                catalogFusionPlan(chars)

            chars.containsAll(listOf("v", "t")) ->
                FusionPlan("t", "Stab / Leiter", listOf("v", "t"), createsRod = true)

            chars.containsAll(listOf("q", "t")) ->
                FusionPlan("I", "Stromstärke (I = q/t)", listOf("I"), createsCurrent = true)

            else -> null
        }
    }

    private fun mergeComponents(preferred: List<String>, componentSet: Set<String>): List<String> {
        return (preferred + componentSet.filter { it !in preferred }).distinct()
    }

    private fun fuseInto(
        first: SimBody,
        second: SimBody,
        plan: FusionPlan,
        components: List<String>
    ) {
        val midpointX = (first.x + second.x) / 2f
        val midpointY = (first.y + second.y) / 2f
        val firstMass = first.mass.coerceAtLeast(0.001f)
        val secondMass = second.mass.coerceAtLeast(0.001f)
        val totalMass = firstMass + secondMass
        val wasDragged = first.isBeingDragged || second.isBeingDragged
        val firstHadThrust = first.hasThrust
        val secondHadThrust = second.hasThrust

        first.x = midpointX
        first.y = midpointY
        if (!wasDragged) {
            first.vx = (first.vx * firstMass + second.vx * secondMass) / totalMass
            first.vy = (first.vy * firstMass + second.vy * secondMass) / totalMass
        } else {
            first.vx = 0f
            first.vy = 0f
        }

        first.char = plan.formula
        first.renderedExpr = FormulaTypesetter.buildExpression(plan.formula, plan.title)
        first.componentChars.clear()
        first.componentChars.addAll(components)
        first.mass = max(first.mass, second.mass)
        first.hasGravity = first.hasGravity || second.hasGravity || components.contains("m")
        first.hasVelocity = first.hasVelocity || second.hasVelocity || components.contains("v")
        first.hasThrust = firstHadThrust || secondHadThrust || components.contains("a")
        if (!firstHadThrust && secondHadThrust) first.thrustAngle = second.thrustAngle
        first.hasFriction = first.hasFriction || second.hasFriction || components.contains("μ")
        if (first.charge == 0f) first.charge = second.charge

        first.isFieldSource = false
        first.fieldType = FieldType.NONE
        first.isBlackHole = plan.formula == "2GM / c²"
        first.blackHoleRadius = if (first.isBlackHole) 55f else 0f
        first.isRod = plan.createsRod
        if (plan.createsRod) {
            first.char = "t"
            first.renderedExpr = FormulaTypesetter.buildExpression("t", plan.title)
            first.rodLength = 180f
            first.componentChars.clear()
            first.componentChars.addAll(components)
        } else if (plan.createsCurrent) {
            first.char = "I"
            first.renderedExpr = FormulaTypesetter.buildExpression("I", plan.title)
            first.componentChars.clear()
            first.componentChars.add("I")
            first.isRod = false
        }

        first.isBeingDragged = false
        first.collisionGraceFrames = 3
        second.isBeingDragged = false
        bodies.removeAll { it.id == second.id }
    }

    fun splitFormula(body: SimBody): List<SimBody> {
        if (body.componentChars.size <= 1) return emptyList()
        bodies.removeAll { it.id == body.id }
        val result = mutableListOf<SimBody>()
        val count = body.componentChars.size
        for (i in 0 until count) {
            val component = body.componentChars[i]
            val offsetX = (i - (count - 1) / 2f) * 44f
            val newBody = createLetterBody(component, body.x + offsetX, body.y - 10f)
            newBody.vx = (i - (count - 1) / 2f) * 120f
            newBody.vy = -140f
            bodies.add(newBody)
            result.add(newBody)
        }
        return result
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lightweight undo/redo for fusion, splitting, deletion, and clearing.
    // ──────────────────────────────────────────────────────────────────────────
    fun saveUndoPoint() {
        undoStack.addLast(snapshot())
        while (undoStack.size > maxHistoryEntries) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        redoStack.addLast(snapshot())
        restore(undoStack.removeLast())
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        undoStack.addLast(snapshot())
        restore(redoStack.removeLast())
        return true
    }

    fun clearUndoHistory() {
        undoStack.clear()
        redoStack.clear()
    }

    private fun snapshot(): SimulationSnapshot = SimulationSnapshot(
        bodies = bodies.map { it.copy(componentChars = it.componentChars.toMutableList()) },
        electronBeam = electronBeam.toList()
    )

    private fun restore(snapshot: SimulationSnapshot) {
        bodies.clear()
        bodies.addAll(snapshot.bodies.map {
            it.copy(
                componentChars = it.componentChars.toMutableList(),
                isBeingDragged = false,
                collisionGraceFrames = 2
            )
        })
        electronBeam.clear()
        electronBeam.addAll(snapshot.electronBeam)
    }
}
