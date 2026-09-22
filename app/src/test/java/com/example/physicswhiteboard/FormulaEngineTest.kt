package com.example.physicswhiteboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FormulaEngineTest {

    private fun makeSingle(char: String, x: Float = 0f, y: Float = 0f): BoardItem.Single {
        val sym = getSymbolByChar(char) ?: error("Symbol '$char' not found in palette")
        return BoardItem.Single(
            id = char,
            x = x,
            y = y,
            symbol = sym
        )
    }

    @Test
    fun testNewtonSecondLaw() {
        val m = makeSingle("m", 10f, 20f)
        val a = makeSingle("a", 30f, 40f)

        val result = tryCombineItems(m, a)
        assertNotNull("m + a should form a formula", result)
        assertEquals("F = m·a", result!!.formula)
        assertEquals("Newton's 2nd Law", result.title)
        assertEquals(20f, result.x, 0.001f)
        assertEquals(30f, result.y, 0.001f)

        // Reverse order test
        val reverse = tryCombineItems(a, m)
        assertNotNull("a + m should also form F = m·a", reverse)
        assertEquals("F = m·a", reverse!!.formula)
    }

    @Test
    fun testLinearMomentum() {
        val m = makeSingle("m")
        val v = makeSingle("v")

        val result = tryCombineItems(m, v)
        assertNotNull("m + v should form momentum formula", result)
        assertEquals("p = m·v", result!!.formula)
    }

    @Test
    fun testMechanicalPower() {
        val f = makeSingle("F")
        val v = makeSingle("v")

        val result = tryCombineItems(f, v)
        assertNotNull("F + v should form P = F·v", result)
        assertEquals("P = F·v", result!!.formula)
    }

    @Test
    fun testPowerFromEnergyAndTime() {
        val e = makeSingle("E")
        val t = makeSingle("t")

        val result = tryCombineItems(e, t)
        assertNotNull("E + t should form P = E / t", result)
        assertEquals("P = E / t", result!!.formula)
    }

    @Test
    fun testCentripetalAcceleration() {
        val v = makeSingle("v")
        val r = makeSingle("r")

        val result = tryCombineItems(v, r)
        assertNotNull("v + r should form a = v² / r", result)
        assertEquals("a = v² / r", result!!.formula)
    }

    @Test
    fun testTangentialVelocity() {
        val omega = makeSingle("ω")
        val r = makeSingle("r")

        val result = tryCombineItems(omega, r)
        assertNotNull("ω + r should form v = ω·r", result)
        assertEquals("v = ω·r", result!!.formula)
    }

    @Test
    fun testAngularFrequencyAndPeriod() {
        val omega = makeSingle("ω")
        val period = makeSingle("T")

        val result = tryCombineItems(omega, period)
        assertNotNull("ω + T should form ω = 2π / T", result)
        assertEquals("ω = 2π / T", result!!.formula)
    }

    @Test
    fun testMomentOfInertia() {
        val m = makeSingle("m")
        val r = makeSingle("r")

        val result = tryCombineItems(m, r)
        assertNotNull("m + r should form I = m·r²", result)
        assertEquals("I = m·r²", result!!.formula)
    }

    @Test
    fun testCentripetalForceMultiSymbolCombination() {
        // Step 1: v + r -> a = v² / r
        val v = makeSingle("v")
        val r = makeSingle("r")
        val accelCentripetal = tryCombineItems(v, r)
        assertNotNull(accelCentripetal)

        // Step 2: m + (a = v²/r) -> F = m·v²/r
        val m = makeSingle("m")
        val centripetalForce = tryCombineItems(m, accelCentripetal!!)
        assertNotNull("m + (v²/r) should form F = m·v²/r", centripetalForce)
        assertEquals("F = m·v²/r", centripetalForce!!.formula)
        assertEquals("Centripetal Force", centripetalForce.title)
    }

    @Test
    fun testRotationalCentripetalForceMultiSymbolCombination() {
        // Step 1: ω + r -> v = ω·r
        val omega = makeSingle("ω")
        val r = makeSingle("r")
        val angularVel = tryCombineItems(omega, r)
        assertNotNull(angularVel)

        // Step 2: m + (v = ω·r) -> F = m·ω²·r
        val m = makeSingle("m")
        val rotForce = tryCombineItems(m, angularVel!!)
        assertNotNull("m + (ω·r) should form F = m·ω²·r", rotForce)
        assertEquals("F = m·ω²·r", rotForce!!.formula)
    }

    @Test
    fun testAngularMomentumMultiSymbolCombination() {
        // Step 1: m + v -> p = m·v
        val m = makeSingle("m")
        val v = makeSingle("v")
        val momentum = tryCombineItems(m, v)
        assertNotNull(momentum)

        // Step 2: r + (p = m·v) -> L = m·v·r
        val r = makeSingle("r")
        val angMomentum = tryCombineItems(r, momentum!!)
        assertNotNull("r + (p = m·v) should form L = m·v·r", angMomentum)
        assertEquals("L = m·v·r", angMomentum!!.formula)
    }

    @Test
    fun testFieldCombinationUsesSharedCatalog() {
        val e = makeSingle("E")
        val b = makeSingle("B")

        val result = tryCombineItems(e, b)
        assertNotNull("E + B should use the shared field rule", result)
        assertEquals("v = E/B", result!!.formula)
    }

    @Test
    fun testInvalidCombinationReturnsNull() {
        val m = makeSingle("m")
        val period = makeSingle("T")

        val result = tryCombineItems(m, period)
        assertNull("m + T does not form a base formula", result)
    }
}
