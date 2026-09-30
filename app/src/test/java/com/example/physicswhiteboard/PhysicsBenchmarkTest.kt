package com.example.physicswhiteboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Physics benchmark suite: checks the simulation against real-world classical
 * mechanics and guards stability and determinism edge cases.
 *
 * Unit system (see PhysicsConstants): 100 px = 1 m, time in seconds, g = 9.81 m/s^2.
 *
 * Tolerance rationale: the engine integrates with semi-implicit Euler at the app's
 * 60 FPS frame time (dt = 1/60 s). That scheme is first-order, so continuous
 * textbook values carry an O(dt) bias (documented per test below). Where the
 * discrete model is exact (geometric damping, restitution speed ratio, momentum)
 * the tolerances are tight. The friction glyph is the palette letter mu (U+03BC).
 */
class PhysicsBenchmarkTest {

    private val dt = 0.016f

    @Test
    fun benchmark_units_gravityIsRealG() {
        assertEquals(
            "gravity must be 9.81 m/s^2 in sim units (100 px = 1 m)",
            9.81f,
            PhysicsConstants.gravityAcceleration / PhysicsConstants.pxPerMeter,
            0.005f
        )
    }

    @Test
    fun benchmark_freeFall_matchesTextbook() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        engine.canvasWidth = 1000f
        engine.groundY = 800f
        val m = engine.createLetterBody("m", 500f, 300f)
        engine.bodies.add(m)
        val halfHeight = BodyGeometry.collisionHalfExtents(m).halfHeight
        val dropPx = engine.groundY - halfHeight - 300f
        val dropMeters = dropPx / PhysicsConstants.pxPerMeter
        val textbookTime = sqrt(2f * dropMeters / 9.81f)
        val gSim = PhysicsConstants.gravityAcceleration

        val yAfterStep = ArrayList<Float>()
        var steps = 0
        while (m.y + halfHeight < engine.groundY && steps < 600) {
            engine.step(dt)
            yAfterStep.add(m.y)
            steps++
        }
        val simulatedTime = steps * dt
        val timeError = abs(simulatedTime - textbookTime) / textbookTime
        assertTrue(
            "free-fall time $simulatedTime s vs textbook $textbookTime s (error $timeError)",
            timeError <= 0.05f
        )

        // Height covered at half the fall time against the continuous 0.5*g*t^2:
        // semi-implicit Euler runs about dt/2 ahead, which stays inside 6%.
        val nHalf = (textbookTime / (2f * dt)).roundToInt()
        val tHalf = nHalf * dt
        val dSim = yAfterStep[nHalf - 1] - 300f
        val dCont = 0.5f * gSim * tHalf * tHalf
        val distanceError = abs(dSim - dCont) / dCont
        assertTrue(
            "half-time fall distance $dSim px vs continuous $dCont px (error $distanceError)",
            distanceError <= 0.06f
        )
    }

    @Test
    fun benchmark_projectileRange_matchesTextbook() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        engine.canvasWidth = 1.0e6f
        engine.groundY = 1.0e6f
        val m = engine.createLetterBody("m", 400f, 300f)
        // 45-degree launch with component speed 420 px/s in x and -420 px/s in y.
        m.vx = 420f
        m.vy = -420f
        engine.bodies.add(m)

        val vComponent = 420f
        // Textbook 45-degree projectile: R = 2*v^2/g and apex = v^2/(2g),
        // with v the total launch speed (= sqrt(2) * component) and g in sim units.
        val vTotal = vComponent * sqrt(2f)
        // sin(2 * 45 deg) = 1, so R = v^2 / g (not 2v^2/g).
        val textbookRange = vTotal * vTotal / PhysicsConstants.gravityAcceleration
        val textbookApex = vComponent * vComponent / (2f * PhysicsConstants.gravityAcceleration)

        val launchY = m.y
        val launchX = m.x
        var apexHeight = 0f
        var range = 0f
        var prevY = m.y
        var prevX = m.x
        for (i in 0 until 200) {
            engine.step(dt)
            apexHeight = maxOf(apexHeight, launchY - m.y)
            if (m.vy > 0f && m.y >= launchY && prevY < launchY) {
                // Interpolate the crossing of the launch height to remove the
                // frame-quantization error from the range measurement.
                val fraction = (prevY - launchY) / (prevY - m.y)
                range = prevX + fraction * (m.x - prevX) - launchX
                break
            }
            prevY = m.y
            prevX = m.x
        }
        // The 0.018/s near-vacuum drag costs ~2.9% of the range; the 5% budget
        // covers drag plus the remaining interpolation residual.
        val rangeError = abs(range - textbookRange) / textbookRange
        assertTrue(
            "projectile range $range px vs textbook $textbookRange px (error $rangeError)",
            rangeError <= 0.05f
        )
        val apexError = abs(apexHeight - textbookApex) / textbookApex
        assertTrue(
            "projectile apex $apexHeight px vs textbook $textbookApex px (error $apexError)",
            apexError <= 0.06f
        )
    }

    @Test
    fun benchmark_elasticCollision_conservesMomentum() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        engine.canvasWidth = 1.0e6f
        engine.groundY = 1.0e6f
        val light = engine.createLetterBody("r", 500f, 400f)
        val heavy = engine.createLetterBody("r", 620f, 400f)
        light.mass = 1f
        heavy.mass = 3f
        light.vx = 300f
        heavy.vx = -100f
        engine.bodies.add(light)
        engine.bodies.add(heavy)

        val momentumBefore = light.mass * light.vx + heavy.mass * heavy.vx
        val speedBefore = abs(light.mass * light.vx) + abs(heavy.mass * heavy.vx)
        val e = 0.6f
        val m1 = light.mass
        val m2 = heavy.mass
        val v1 = light.vx
        val v2 = heavy.vx
        val textbookV1 = ((m1 - e * m2) * v1 + (1f + e) * m2 * v2) / (m1 + m2)
        val textbookV2 = ((m2 - e * m1) * v2 + (1f + e) * m1 * v1) / (m1 + m2)

        val minimumDistance =
            BodyGeometry.collisionRadius(light) + BodyGeometry.collisionRadius(heavy)
        // Step until the pair has made contact and separated again (~20 frames
        // total), so the long-run drag decay and the side walls cannot
        // contaminate the collision measurement.
        var contactMade = false
        var separated = false
        for (i in 0 until 240) {
            engine.step(dt)
            val distance = abs(heavy.x - light.x)
            // One step can close ~6.4 px and rebound ~4 px, so the discrete
            // contact dip never shows after a step; use motion-aware windows.
            if (!contactMade && distance <= minimumDistance + 8f) contactMade = true
            if (contactMade && distance > minimumDistance + 10f) {
                separated = true
                break
            }
        }
        assertTrue("bodies must collide and separate after collision", separated)

        val momentumAfter = light.mass * light.vx + heavy.mass * heavy.vx
        assertTrue(
            "momentum must be conserved: before $momentumBefore after $momentumAfter",
            abs(momentumAfter - momentumBefore) <= 0.005f * speedBefore
        )
        // Post-impulse drag decays both velocities by the same ~1% factor over
        // the short approach, inside the 2.5% budget around the e = 0.6 solution.
        assertEquals("light body velocity vs e=0.6 solution", textbookV1, light.vx, 0.025f * abs(textbookV1))
        assertEquals("heavy body velocity vs e=0.6 solution", textbookV2, heavy.vx, 0.025f * abs(textbookV2))
    }

    @Test
    fun benchmark_restitutionBounce_geometricDecay() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        engine.canvasWidth = 1000f
        engine.groundY = 800f
        val m = engine.createLetterBody("m", 500f, 300f)
        engine.bodies.add(m)
        val halfHeight = BodyGeometry.collisionHalfExtents(m).halfHeight
        val restY = engine.groundY - halfHeight

        val reboundSpeeds = ArrayList<Float>()
        val impactSpeeds = ArrayList<Float>()
        val apexHeights = ArrayList<Float>()
        var previousVy = m.vy
        var bounceCount = 0
        for (i in 0 until 900) {
            engine.step(dt)
            if (m.vy < 0f && previousVy > 0f && bounceCount < 3) {
                // Ground contact happened this frame: previous frame's vy plus
                // this frame's gravity kick is the impact speed.
                impactSpeeds.add(previousVy + PhysicsConstants.gravityAcceleration * dt)
                reboundSpeeds.add(-m.vy)
                bounceCount++
            } else if (bounceCount > 0 && m.vy >= 0f && previousVy < 0f && apexHeights.size < bounceCount) {
                apexHeights.add(restY - m.y)
            }
            if (bounceCount >= 3 && apexHeights.size >= 2) break
            previousVy = m.vy
        }

        assertEquals(3, bounceCount)
        assertEquals(2, apexHeights.size)
        // Speed ratio across a bounce must reproduce the restitution constant
        // exactly (the discrete impulse is exact): observed 0.4199 vs 0.42.
        for (i in 0 until 2) {
            val ratio = reboundSpeeds[i] / impactSpeeds[i]
            assertEquals(
                "restitution speed ratio bounce $i",
                PhysicsConstants.groundRestitution,
                ratio,
                0.02f * PhysicsConstants.groundRestitution
            )
        }
        // Apex heights decay by e^2 = 0.1764 per bounce in the continuum. At
        // 60 FPS the semi-implicit Euler flight arc loses v0*dt/2 of rise per
        // flight (O(dt) bias) plus drag, so the frame-boundary apex ratio
        // measures ~0.155. benchmark_restitutionApex_convergesToTextbookAtFineDt
        // proves this is pure discretization: at dt = 0.004 the same scenario
        // lands within 4.5% of e^2.
        val heightRatio = apexHeights[1] / apexHeights[0]
        val expected = PhysicsConstants.groundRestitution * PhysicsConstants.groundRestitution
        assertEquals(
            "apex height ratio at 60 FPS (documented O(dt) bias)",
            expected,
            heightRatio,
            0.15f * expected
        )
    }

    @Test
    fun benchmark_restitutionApex_convergesToTextbookAtFineDt() {
        fun apexRatioAt(stepDt: Float): Float {
            val engine = PhysicsSimulationEngine()
            engine.bodies.clear()
            engine.canvasWidth = 1000f
            engine.groundY = 800f
            val m = engine.createLetterBody("m", 500f, 300f)
            engine.bodies.add(m)
            val halfHeight = BodyGeometry.collisionHalfExtents(m).halfHeight
            val restY = engine.groundY - halfHeight
            val apexHeights = ArrayList<Float>()
            var previousVy = m.vy
            var bounceCount = 0
            var frames = 0
            while ((bounceCount < 3 || apexHeights.size < 2) && frames < 6000) {
                engine.step(stepDt)
                if (m.vy < 0f && previousVy > 0f) bounceCount++
                else if (bounceCount > 0 && m.vy >= 0f && previousVy < 0f &&
                    apexHeights.size < bounceCount
                ) apexHeights.add(restY - m.y)
                previousVy = m.vy
                frames++
            }
            return apexHeights[1] / apexHeights[0]
        }

        val expected = PhysicsConstants.groundRestitution * PhysicsConstants.groundRestitution
        val fineDtRatio = apexRatioAt(0.004f)
        assertEquals(
            "apex ratio must converge to e^2 as dt shrinks (textbook geometric decay)",
            expected,
            fineDtRatio,
            0.045f * expected
        )
    }

    @Test
    fun benchmark_frictionToken_exponentialDecay() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        engine.groundY = 1.0e6f
        val mu = engine.createLetterBody("\u03bc", 400f, 300f)
        mu.vx = 500f
        engine.bodies.add(mu)
        assertTrue("mu token must carry the friction flag", mu.hasFriction)

        val steps = 31 // 0.5 s of simulation time
        repeat(steps) { engine.step(dt) }
        // The engine damps geometrically per step: v = v0 * (1 - k*dt)^n.
        val k = PhysicsConstants.frictionStrength
        val expected = 500f * Math.pow((1f - k * dt).toDouble(), steps.toDouble()).toFloat()
        assertEquals(
            "friction token velocity should decay geometrically",
            expected,
            mu.vx,
            0.01f * expected
        )
    }

    @Test
    fun edge_zeroMassCollision_isStableAndFinite() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        val tinyA = engine.createLetterBody("r", 400f, 300f)
        val tinyB = engine.createLetterBody("r", 560f, 300f)
        val massive = engine.createLetterBody("r", 480f, 500f)
        tinyA.mass = 0.001f
        tinyB.mass = 0.001f
        massive.mass = 1.0e9f
        tinyA.vx = 2000f
        tinyB.vx = -2000f
        engine.bodies.add(tinyA)
        engine.bodies.add(tinyB)
        engine.bodies.add(massive)

        repeat(300) { engine.step(dt) }
        for (body in engine.bodies) {
            assertTrue("x must be finite", body.x.isFinite())
            assertTrue("y must be finite", body.y.isFinite())
            assertTrue("vx must be finite", body.vx.isFinite())
            assertTrue("vy must be finite", body.vy.isFinite())
            assertTrue("body must stay inside the right wall", body.x <= engine.canvasWidth)
            assertTrue("body must stay inside the left wall", body.x >= 0f)
        }
    }

    @Test
    fun edge_extremeVelocity_isContained() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        val body = engine.createLetterBody("r", 600f, 400f)
        body.vx = 1.0e6f
        engine.bodies.add(body)
        val halfWidth = BodyGeometry.collisionHalfExtents(body).halfWidth

        repeat(60) { engine.step(0.033f) }
        assertTrue("vx must stay finite", body.vx.isFinite())
        assertTrue("x must stay finite", body.x.isFinite())
        assertTrue(
            "wall clamp must contain the body, was x=${body.x}",
            body.x >= 10f + halfWidth - 1f && body.x <= engine.canvasWidth - 10f - halfWidth + 1f
        )
    }

    @Test
    fun benchmark_blackHoleAttraction_inverseSquare() {
        fun deltaVAt(radius: Float): Pair<Float, Float> {
            val engine = PhysicsSimulationEngine()
            engine.bodies.clear()
            engine.canvasWidth = 5000f
            val hole = engine.createLetterBody("m", 600f, 400f)
            hole.isBlackHole = true
            hole.blackHoleRadius = 55f
            hole.mass = 3f
            // Probe sits on the +x side, so the pull points toward -x.
            val probe = engine.createLetterBody("r", 600f + radius, 400f)
            engine.bodies.add(hole)
            engine.bodies.add(probe)
            engine.step(dt)
            return probe.vx to hole.x
        }

        val gSim = 5.0e7f
        val softening = 60f
        val (dv300, holeX300) = deltaVAt(300f)
        val (dv600, holeX600) = deltaVAt(600f)
        val expected300 = gSim * 3f / (300f * 300f + softening * softening) * dt
        val expected600 = gSim * 3f / (600f * 600f + softening * softening) * dt
        assertEquals("attraction at 300 px vs softened Newtonian a = G*M/(r^2+eps^2)",
            -expected300, dv300, 0.01f * expected300)
        assertEquals("attraction at 600 px vs softened Newtonian a = G*M/(r^2+eps^2)",
            -expected600, dv600, 0.01f * expected600)
        // Softened falloff ratio for r=600 vs r=300: (600^2+60^2)/(300^2+60^2) = 3.8848.
        val ratio = abs(dv300) / abs(dv600)
        assertEquals("inverse-square falloff ratio", 3.8848f, ratio, 0.03f * 3.8848f)
        assertEquals("black hole must not move", 600f, holeX300, 0.0f)
        assertEquals("black hole must not move", 600f, holeX600, 0.0f)
        assertTrue(dv300.isFinite() && dv600.isFinite())
    }

    @Test
    fun benchmark_determinism_sameInputBitIdentical() {
        fun runScript(): List<Float> {
            val engine = PhysicsSimulationEngine()
            val marks = ArrayList<Float>()
            for (frame in 0 until 600) {
                if (frame == 100) {
                    val m = engine.bodies.first { it.char == "m" }
                    val v = engine.bodies.first { it.char == "v" }
                    assertTrue(engine.tryFuseSymbols(m, v))
                }
                engine.step(dt)
                if (frame == 99 || frame == 100 || frame == 101 || frame == 300 || frame == 599) {
                    marks.add(engine.bodies.size.toFloat())
                    for (body in engine.bodies) {
                        marks.add(body.x)
                        marks.add(body.y)
                        marks.add(body.vx)
                        marks.add(body.vy)
                    }
                }
            }
            return marks
        }

        val first = runScript()
        val second = runScript()
        assertEquals("identical scripts must produce identical state sequences",
            first.size, second.size)
        for (i in first.indices) {
            assertEquals("state element $i must match bit-for-bit", first[i], second[i], 0.0f)
        }
    }

    @Test
    fun edge_stackedBodies_noExplosionOrJitter() {
        val engine = PhysicsSimulationEngine()
        engine.bodies.clear()
        engine.canvasWidth = 1200f
        engine.groundY = 800f
        val bottom = engine.createLetterBody("m", 500f, 774f)
        val middle = engine.createLetterBody("m", 500f, 702.8f)
        val top = engine.createLetterBody("m", 500f, 631.6f)
        engine.bodies.add(bottom)
        engine.bodies.add(middle)
        engine.bodies.add(top)

        var maxMove = 0f
        val previous = ArrayList<Pair<Float, Float>>()
        for (frame in 0 until 600) {
            engine.step(dt)
            if (frame >= 500) {
                for (i in engine.bodies.indices) {
                    val body = engine.bodies[i]
                    assertTrue("body $i must stay finite", body.x.isFinite() && body.y.isFinite())
                    assertTrue("body $i must not sink below ground", body.y <= engine.groundY)
                    assertTrue("body $i must not escape upward", body.y >= 0f)
                    if (i < previous.size) {
                        val move = hypot(body.x - previous[i].first, body.y - previous[i].second)
                        maxMove = maxOf(maxMove, move)
                    }
                }
            }
            previous.clear()
            for (body in engine.bodies) previous.add(body.x to body.y)
        }
        assertTrue(
            "resting stack must not jitter more than 2 px per frame (max $maxMove)",
            maxMove < 2f
        )
    }
}
