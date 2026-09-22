package com.example.physicswhiteboard

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyGeometryTest {

    @Test
    fun rotatedElectricFieldUsesTheSameLocalRectangleForAllAngles() {
        val field = SimBody(
            x = 200f,
            y = 180f,
            isFieldSource = true,
            fieldType = FieldType.ELECTRIC_E,
            fieldRadius = 120f,
            fieldHalfWidth = 30f,
            fieldAngle = (PI / 4.0).toFloat()
        )

        val inside = FieldGeometry.localToWorld(field, 90f, 0f)
        val outside = FieldGeometry.localToWorld(field, 0f, 60f)

        assertTrue(FieldGeometry.contains(field, inside.x, inside.y))
        assertFalse(FieldGeometry.contains(field, outside.x, outside.y))

        val corners = FieldGeometry.corners(field)
        assertEquals(4, corners.size)
        assertTrue("rotated rectangle must not remain axis aligned", abs(corners[0].x - corners[1].x) > 1f)
    }

    @Test
    fun formulaBoundsGrowWithTheCompleteRenderedEquation() {
        val m = SimBody(x = 0f, y = 0f, char = "m")
        val a = SimBody(x = 0f, y = 0f, char = "a")
        val formula = SimBody(
            x = 0f,
            y = 0f,
            char = "F = m·a",
            renderedExpr = FormulaTypesetter.buildExpression("F = m·a", "Newton 2. Gesetz"),
            componentChars = mutableListOf("m", "a")
        )

        assertTrue(BodyGeometry.visualHalfExtents(formula).halfWidth > BodyGeometry.visualHalfExtents(m).halfWidth)
        assertTrue(BodyGeometry.containsPoint(formula, 0f, 0f))
        assertFalse(BodyGeometry.containsPoint(formula, 1000f, 1000f))
        assertTrue(BodyGeometry.fusionDistance(m, a) >= 60f)
    }

    @Test
    fun exactOverlapIsSeparatedByTheSimulation() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        val first = engine.createLetterBody("q", 100f, 100f)
        val second = engine.createLetterBody("q", 100f, 100f)
        engine.bodies.add(first)
        engine.bodies.add(second)

        val distance = hypot(first.x - second.x, first.y - second.y)
        assertEquals(0f, distance, 0.001f)
        engine.step(0.016f)
        assertTrue("exactly overlapping symbols must be separated", hypot(first.x - second.x, first.y - second.y) > 0f)
        assertTrue(BodyGeometry.collisionRadius(first) > 0f)
    }
}
