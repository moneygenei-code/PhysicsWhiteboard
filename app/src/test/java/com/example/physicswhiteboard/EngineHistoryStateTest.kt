package com.example.physicswhiteboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineHistoryStateTest {

    @Test
    fun canUndoStartsFalse() {
        val engine = PhysicsSimulationEngine()
        engine.clearUndoHistory()
        assertFalse(engine.canUndo)
    }

    @Test
    fun saveUndoPointEnablesUndo() {
        val engine = PhysicsSimulationEngine()
        engine.clearUndoHistory()
        engine.saveUndoPoint()
        assertTrue(engine.canUndo)
        assertFalse(engine.canRedo)
    }

    @Test
    fun undoEnablesRedoAndExhaustedUndoDisablesIt() {
        val engine = PhysicsSimulationEngine()
        engine.clearUndoHistory()
        engine.saveUndoPoint()
        assertTrue(engine.undo())
        assertTrue(engine.canRedo)
        assertFalse(engine.canUndo)
    }

    @Test
    fun loadSceneClearsHistoryFlags() {
        val engine = PhysicsSimulationEngine()
        engine.saveUndoPoint()
        engine.loadScene(ApparatusScene.WIEN_FILTER)
        assertFalse(engine.canUndo)
        assertFalse(engine.canRedo)
    }
}
