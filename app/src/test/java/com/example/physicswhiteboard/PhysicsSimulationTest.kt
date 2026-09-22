package com.example.physicswhiteboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class PhysicsSimulationTest {

    @Test
    fun testWienFilterEquilibrium() {
        val engine = PhysicsSimulationEngine()
        engine.canvasWidth = 1000f
        engine.canvasHeight = 800f
        engine.loadScene(ApparatusScene.WIEN_FILTER)

        // Find the matching particle (vx = 420)
        val qMatched = engine.bodies.firstOrNull { it.char == "q" && abs(it.vx - 420f) < 1f }
        assertNotNull("Should have balanced particle in Wien Filter scene", qMatched)

        // Run simulation for several steps
        val initialY = qMatched!!.y
        for (i in 0..60) {
            engine.step(0.016f)
        }

        // Net vertical displacement should remain near zero because Fel and FL cancel
        val verticalDisplacement = abs(qMatched.y - initialY)
        assertTrue("Wien Filter: matched velocity should experience near zero net deflection, was $verticalDisplacement", verticalDisplacement < 45f)

    }

    @Test
    fun testElectricFieldAcceleration() {
        val engine = PhysicsSimulationEngine()
        val eField = SimBody(
            x = 300f,
            y = 300f,
            isFieldSource = true,
            fieldType = FieldType.ELECTRIC_E,
            fieldRadius = 200f,
            fieldAngle = 0f, // Points right along +X
            fieldMagnitude = 2000f
        )
        engine.bodies.add(eField)

        val q = engine.createLetterBody("q", 300f, 300f)
        q.charge = 1.0f
        q.vx = 0f
        engine.bodies.add(q)

        engine.step(0.05f)

        // Force = q * E -> vx must accelerate in +X
        assertTrue("Particle in E-field should accelerate along field direction", q.vx > 50f)
    }

    @Test
    fun rotatedElectricFieldAcceleratesAlongItsVisibleArrowDirection() {
        val engine = PhysicsSimulationEngine()
        val eField = SimBody(
            x = 300f,
            y = 300f,
            isFieldSource = true,
            fieldType = FieldType.ELECTRIC_E,
            fieldRadius = 200f,
            fieldHalfWidth = 100f,
            fieldAngle = (PI / 2).toFloat(),
            fieldMagnitude = 2000f
        )
        val q = engine.createLetterBody("q", 300f, 300f)
        engine.bodies.clear()
        engine.bodies.add(eField)
        engine.bodies.add(q)

        engine.step(0.05f)

        assertTrue("rotated E field should accelerate down the arrows", q.vy > 50f)
        assertTrue("rotated E field should not accelerate sideways", abs(q.vx) < 10f)
    }

    @Test
    fun testMagneticLorentzForceDeflection() {
        val engine = PhysicsSimulationEngine()
        val bField = SimBody(
            x = 300f,
            y = 300f,
            isFieldSource = true,
            fieldType = FieldType.MAGNETIC_B,
            fieldRadius = 200f,
            bDirectionZ = 1
        )
        engine.bodies.add(bField)

        val q = engine.createLetterBody("q", 300f, 300f)
        q.charge = 1.0f
        q.vx = 300f // moving in +X
        q.vy = 0f
        engine.bodies.add(q)

        engine.step(0.05f)

        // Lorentz force F = q(v x B) deflects velocity into Y component
        assertTrue("Lorentz force should deflect velocity perpendicular to motion", abs(q.vy) > 10f)
        // Kinetic speed should remain approximately conserved
        val finalSpeed = kotlin.math.hypot(q.vx, q.vy)
        assertEquals("Lorentz force should conserve kinetic speed", 300f, finalSpeed, 10f)
    }

    @Test
    fun testFormulaFusions() {
        val engine = PhysicsSimulationEngine()

        // 1. m + a -> F = m·a
        val m1 = engine.createLetterBody("m", 100f, 100f)
        val a1 = engine.createLetterBody("a", 105f, 105f)
        assertTrue(engine.tryFuseSymbols(m1, a1))
        assertEquals("F = m·a", m1.char)

        // 2. m + v -> p = m·v
        val m2 = engine.createLetterBody("m", 200f, 200f)
        val v2 = engine.createLetterBody("v", 205f, 205f)
        assertTrue(engine.tryFuseSymbols(m2, v2))
        assertEquals("p = m·v", m2.char)

        // 3. v + r -> a = v² / r
        val v3 = engine.createLetterBody("v", 300f, 300f)
        val r3 = engine.createLetterBody("r", 305f, 305f)
        assertTrue(engine.tryFuseSymbols(v3, r3))
        assertEquals("a = v² / r", v3.char)

        // 4. v + t -> Plank/Rod
        val v4 = engine.createLetterBody("v", 400f, 400f)
        val t4 = engine.createLetterBody("t", 405f, 405f)
        assertTrue(engine.tryFuseSymbols(v4, t4))
        assertTrue(v4.isRod)

        // 5. q + t -> I
        val q5 = engine.createLetterBody("q", 500f, 500f)
        val t5 = engine.createLetterBody("t", 505f, 505f)
        assertTrue(engine.tryFuseSymbols(q5, t5))
        assertEquals("I", q5.char)
    }

    @Test
    fun testFormulaSplitting() {
        val engine = PhysicsSimulationEngine()
        // Clear the default FREE_SANDBOX bodies so our count assertions are clean
        engine.bodies.clear()
        val m = engine.createLetterBody("m", 100f, 100f)
        val a = engine.createLetterBody("a", 105f, 105f)
        engine.bodies.add(m)
        engine.bodies.add(a)
        engine.tryFuseSymbols(m, a)

        assertEquals("F = m·a", m.char)
        assertEquals(1, engine.bodies.size)

        val splitResults = engine.splitFormula(m)
        assertEquals(2, splitResults.size)
        assertEquals(2, engine.bodies.size)
        assertTrue(splitResults.any { it.char == "m" })
        assertTrue(splitResults.any { it.char == "a" })
    }

    @Test
    fun paletteMassFallsAndFusedMassKeepsGravity() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        engine.canvasWidth = 1000f
        engine.canvasHeight = 800f
        engine.groundY = 760f

        val m = engine.createLetterBody("m", 300f, 100f)
        engine.bodies.add(m)
        val initialY = m.y
        engine.step(0.1f)
        assertTrue("A palette-created mass should fall", m.y > initialY)
        assertTrue(m.hasGravity)

        val v = engine.createLetterBody("v", 350f, 100f)
        engine.bodies.add(v)
        assertTrue(engine.tryFuseSymbols(m, v))
        assertTrue("Gravity must survive m + v", m.hasGravity)
        assertEquals("p = m·v", m.renderedExpr?.formulaText)
    }

    @Test
    fun fusedNewtonFormulaIsCenteredAndComplete() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        val m = engine.createLetterBody("m", 100f, 100f)
        val a = engine.createLetterBody("a", 140f, 100f)
        engine.bodies.add(m)
        engine.bodies.add(a)

        assertTrue(engine.tryFuseSymbols(m, a))
        assertEquals(1, engine.bodies.size)
        assertEquals(120f, m.x, 0.001f)
        assertEquals("F = m·a", m.renderedExpr?.formulaText)
        assertTrue(m.renderedExpr!!.glyphs.any { it.text == "F" })
        assertTrue(m.renderedExpr!!.glyphs.any { it.text == "a" })
    }

    @Test
    fun draggedBodyIsNotMovedByPhysicsUntilReleased() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        val m = engine.createLetterBody("m", 300f, 300f)
        val a = engine.createLetterBody("a", 300f, 300f)
        engine.bodies.add(m)
        engine.bodies.add(a)

        engine.beginDrag(m)
        engine.step(0.1f)
        assertEquals(300f, m.x, 0.001f)
        assertEquals(300f, m.y, 0.001f)
        engine.endDrag(m)
        engine.step(0.1f)
        assertTrue(m.y > 300f)
    }

    @Test
    fun undoAndRedoRestoreFormulaEdits() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        val m = engine.createLetterBody("m", 100f, 100f)
        val v = engine.createLetterBody("v", 160f, 100f)
        engine.bodies.add(m)
        engine.bodies.add(v)

        engine.saveUndoPoint()
        assertTrue(engine.tryFuseSymbols(m, v))
        assertEquals(1, engine.bodies.size)
        assertTrue(engine.undo())
        assertEquals(2, engine.bodies.size)
        assertTrue(engine.redo())
        assertEquals(1, engine.bodies.size)
        assertEquals("p = m·v", engine.bodies.single().char)
    }

    @Test
    fun normalModeConcatenatesInsteadOfFusing() {
        val engine = PhysicsSimulationEngine()
        engine.fusionMode = FusionMode.NORMAL
        engine.bodies.clear()
        val m = engine.createLetterBody("m", 100f, 100f)
        val v = engine.createLetterBody("v", 120f, 100f)
        engine.bodies.add(m)
        engine.bodies.add(v)

        // Drag v onto m: existing symbols first, dropped symbols appended.
        assertTrue(engine.tryFuseSymbols(v, m))
        assertEquals("mv", v.char)
        assertEquals(listOf("m", "v"), v.componentChars)
        assertEquals(1, engine.bodies.size)

        val split = engine.splitFormula(v)
        assertEquals(2, split.size)
        assertTrue(split.any { it.char == "m" })
        assertTrue(split.any { it.char == "v" })
    }

    @Test
    fun normalModeKeepsDuplicateSymbols() {
        val engine = PhysicsSimulationEngine()
        engine.fusionMode = FusionMode.NORMAL
        val first = engine.createLetterBody("m", 100f, 100f)
        val second = engine.createLetterBody("m", 120f, 100f)
        assertTrue(engine.tryFuseSymbols(second, first))
        assertEquals("mm", second.char)
    }

    @Test
    fun normalModeIgnoresFieldsAndRods() {
        val engine = PhysicsSimulationEngine()
        engine.fusionMode = FusionMode.NORMAL
        engine.bodies.clear()
        val e = engine.createLetterBody("E", 100f, 100f)
        val b = engine.createLetterBody("B", 120f, 100f)
        engine.bodies.add(e)
        engine.bodies.add(b)
        assertFalse(engine.canFuseSymbols(e, b))
        assertFalse(engine.tryFuseSymbols(e, b))

        val v = engine.createLetterBody("v", 200f, 200f)
        val t = engine.createLetterBody("t", 220f, 200f)
        engine.bodies.add(v)
        engine.bodies.add(t)
        assertFalse(engine.tryFuseSymbols(v, t))
        assertEquals(4, engine.bodies.size)
    }

    @Test
    fun physikIntermediatesCompleteLorentzForce() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        val q = engine.createLetterBody("q", 100f, 100f)
        val v = engine.createLetterBody("v", 120f, 100f)
        engine.bodies.add(q)
        engine.bodies.add(v)
        assertTrue(engine.tryFuseSymbols(q, v))
        assertEquals("q·v", q.char)

        val b = engine.createLetterBody("B", 140f, 100f)
        engine.bodies.add(b)
        assertTrue(engine.tryFuseSymbols(q, b))
        assertEquals("FL = q·v·B", q.char)
        assertTrue(q.renderedExpr!!.glyphs.any { it.text == "F" })
        assertTrue(q.renderedExpr!!.glyphs.any { it.text == "B" })
    }

    @Test
    fun physikIntermediatesNeverAbsorbFieldRegions() {
        val engine = PhysicsSimulationEngine()
        val v = engine.createLetterBody("v", 100f, 100f)
        val b = engine.createLetterBody("B", 120f, 100f)
        assertTrue(b.isFieldSource)
        assertFalse(engine.canFuseSymbols(v, b))
        assertFalse(engine.tryFuseSymbols(v, b))
        assertTrue(b.isFieldSource)
    }

    @Test
    fun orbitRadiusChainBuildsPairwise() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        val m = engine.createLetterBody("m", 100f, 100f)
        val v = engine.createLetterBody("v", 110f, 100f)
        engine.bodies.add(m)
        engine.bodies.add(v)
        assertTrue(engine.tryFuseSymbols(m, v))
        assertEquals("p = m·v", m.char)

        val q = engine.createLetterBody("q", 120f, 100f)
        engine.bodies.add(q)
        assertTrue(engine.tryFuseSymbols(m, q))
        assertEquals("m·q·v", m.char)

        val b = engine.createLetterBody("B", 130f, 100f)
        engine.bodies.add(b)
        assertTrue(engine.tryFuseSymbols(m, b))
        assertEquals("r = m·v / (q·B)", m.char)
        assertEquals(1, engine.bodies.size)
    }

    @Test
    fun gravitationAndSchwarzschildChainsBuildPairwise() {
        val engine = PhysicsSimulationEngine()
        val g = engine.createLetterBody("G", 100f, 100f)
        val bigM = engine.createLetterBody("M", 110f, 100f)
        assertTrue(engine.tryFuseSymbols(g, bigM))
        assertEquals("G·M", g.char)

        val m = engine.createLetterBody("m", 120f, 100f)
        assertTrue(engine.tryFuseSymbols(g, m))
        assertEquals("F = G·M·m / r²", g.char)

        val engine2 = PhysicsSimulationEngine()
        val g2 = engine2.createLetterBody("G", 100f, 100f)
        val bigM2 = engine2.createLetterBody("M", 110f, 100f)
        assertTrue(engine2.tryFuseSymbols(g2, bigM2))
        val c = engine2.createLetterBody("c", 120f, 100f)
        assertTrue(engine2.tryFuseSymbols(g2, c))
        assertEquals("rs = 2GM/c²", g2.char)
        assertTrue(g2.isBlackHole)
    }

    @Test
    fun einsteinFusionRendersCompleteEquation() {
        val engine = PhysicsSimulationEngine()
        val m = engine.createLetterBody("m", 100f, 100f)
        val c = engine.createLetterBody("c", 110f, 100f)
        assertTrue(engine.tryFuseSymbols(m, c))
        assertEquals("E = m·c²", m.char)
        val glyphs = m.renderedExpr!!.glyphs.map { it.text }
        assertTrue(glyphs.contains("E"))
        assertTrue(glyphs.contains("="))
        assertTrue(glyphs.contains("m"))
        assertTrue(glyphs.contains("c"))
    }

    @Test
    fun magneticPeriodFromMassAndField() {
        val engine = PhysicsSimulationEngine()
        val m = engine.createLetterBody("m", 100f, 100f)
        val b = engine.createLetterBody("B", 110f, 100f)
        assertTrue(engine.tryFuseSymbols(m, b))
        assertEquals("T = 2π·m / (q·B)", m.char)
        assertTrue(m.renderedExpr!!.glyphs.any { it.text == "T" })
    }

    @Test
    fun magneticPeriodAcceptsExplicitCharge() {
        val engine = PhysicsSimulationEngine()
        val m = engine.createLetterBody("m", 100f, 100f)
        val q = engine.createLetterBody("q", 110f, 100f)
        assertTrue(engine.tryFuseSymbols(m, q))
        val b = engine.createLetterBody("B", 120f, 100f)
        assertTrue(engine.tryFuseSymbols(m, b))
        assertEquals("T = 2π·m / (q·B)", m.char)
    }

    @Test
    fun hallVoltageChainRendersCompleteEquation() {
        val engine = PhysicsSimulationEngine()
        val q = engine.createLetterBody("q", 100f, 100f)
        val t = engine.createLetterBody("t", 110f, 100f)
        assertTrue(engine.tryFuseSymbols(q, t))
        assertEquals("I", q.char)
        val b = engine.createLetterBody("B", 120f, 100f)
        assertTrue(engine.tryFuseSymbols(q, b))
        assertEquals("UH = RH·I·B/d", q.char)
        assertTrue(q.renderedExpr!!.glyphs.any { it.text == "U" })
    }

    @Test
    fun fusedCurrentSplitsBackIntoChargeAndTime() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        val q = engine.createLetterBody("q", 100f, 100f)
        val t = engine.createLetterBody("t", 110f, 100f)
        engine.bodies.add(q)
        engine.bodies.add(t)
        assertTrue(engine.tryFuseSymbols(q, t))
        assertEquals("I", q.char)
        val split = engine.splitFormula(q)
        assertEquals(2, split.size)
        assertTrue(split.any { it.char == "q" })
        assertTrue(split.any { it.char == "t" })
    }

    @Test
    fun paletteCurrentStaysASingleSymbol() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        val i = engine.createLetterBody("I", 100f, 100f)
        engine.bodies.add(i)
        assertTrue(engine.splitFormula(i).isEmpty())
        assertEquals(1, engine.bodies.size)
    }

    @Test
    fun frictionTokenFallsAndDampsWhileVelocityTokenRests() {
        val engine = PhysicsSimulationEngine()
        val v = engine.createLetterBody("v", 100f, 100f)
        assertTrue(v.hasVelocity)
        assertEquals(0f, v.vx, 0.001f)
        val mu = engine.createLetterBody("μ", 100f, 100f)
        assertTrue(mu.hasFriction)
        assertTrue(mu.hasGravity)
    }

    @Test
    fun releasedBodyKeepsThrowVelocity() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        engine.canvasWidth = 2000f
        engine.groundY = 1500f
        val q = engine.createLetterBody("q", 300f, 300f)
        engine.bodies.add(q)
        engine.beginDrag(q)
        engine.moveDraggedBody(q, 400f, 300f)
        engine.endDrag(q, 500f, -200f)
        assertEquals(500f, q.vx, 0.001f)
        assertEquals(-200f, q.vy, 0.001f)
        engine.step(0.016f)
        assertTrue("a thrown body keeps moving after release", q.x > 400f && q.y < 300f)
    }

    @Test
    fun testCurriculumContentCompleteness() {
        assertEquals("Curriculum must have exactly 10 chapters", 10, KlausurCurriculum.CHAPTERS.size)
        assertEquals("Self-test must have 14 exam questions", 14, KlausurCurriculum.SELF_TEST.size)
        assertTrue("Strategy list must have actionable points", KlausurCurriculum.STRATEGY_15_POINTS.isNotEmpty())

        for (ch in KlausurCurriculum.CHAPTERS) {
            assertTrue("Chapter ${ch.number} title should be non-empty", ch.title.isNotBlank())
            assertTrue("Chapter ${ch.number} should have formulas", ch.formulas.isNotEmpty())
            assertTrue("Chapter ${ch.number} should have typical traps", ch.traps.isNotEmpty())
        }
    }
}

