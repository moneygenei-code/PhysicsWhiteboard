package com.example.physicswhiteboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FusionEventDiscoveryTest {

    @Test
    fun finalFusionEmitsDiscoveryEvent() {
        val engine = PhysicsSimulationEngine()
        val m = engine.createLetterBody("m", 100f, 100f)
        val a = engine.createLetterBody("a", 105f, 105f)
        assertTrue(engine.tryFuseSymbols(m, a))
        assertEquals(1, engine.fusionEvents.size)
        val event = engine.fusionEvents.first()
        assertEquals("F = m·a", event.formula)
        assertEquals(listOf("m", "a"), event.components)
    }

    @Test
    fun einsteinFusionEmitsCompleteEquation() {
        val engine = PhysicsSimulationEngine()
        val m = engine.createLetterBody("m", 100f, 100f)
        val c = engine.createLetterBody("c", 105f, 105f)
        assertTrue(engine.tryFuseSymbols(m, c))
        assertEquals("E = m·c²", engine.fusionEvents.single().formula)
    }

    @Test
    fun normalModeConcatenationEmitsNoEvent() {
        val engine = PhysicsSimulationEngine()
        engine.fusionMode = FusionMode.NORMAL
        val m = engine.createLetterBody("m", 100f, 100f)
        val v = engine.createLetterBody("v", 105f, 105f)
        assertTrue(engine.tryFuseSymbols(m, v))
        assertTrue(engine.fusionEvents.isEmpty())
    }

    @Test
    fun loadSceneClearsPendingEvents() {
        val engine = PhysicsSimulationEngine()
        val m = engine.createLetterBody("m", 100f, 100f)
        val c = engine.createLetterBody("c", 105f, 105f)
        assertTrue(engine.tryFuseSymbols(m, c))
        assertEquals(1, engine.fusionEvents.size)
        engine.loadScene(ApparatusScene.FREE_SANDBOX)
        assertTrue(engine.fusionEvents.isEmpty())
    }
}
