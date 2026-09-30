# Physics Audit Report — PhysicsWhiteboard v1.2

Date: 2026-09-30 · Auditor: AutoCoder · Scope: all physics behavior in
`PhysicsSimulationEngine.kt`, `BodyGeometry.kt` (+ legacy `FormulaEngine.kt` note)

## Unit system (established, now documented in code)

| Quantity | Unit | Note |
| --- | --- | --- |
| Length | px | **100 px = 1 m** (`PhysicsConstants.pxPerMeter = 100f`) |
| Time | s | engine `step(dt)`, dt clamped to [1 ms, 33 ms] |
| Mass | symbol mass units | palette `m` = 1, `M` = 3, `e` = 0.5, `G` = 2 |
| Velocity | px/s | screen y-axis points **down** (gravity +y) |
| Angles | radians | field/rod/thrust orientation |

## Physics behavior inventory

| # | Behavior | Implementation | Verdict before audit | Verdict after fixes |
|---|----------|----------------|----------------------|---------------------|
| 1 | Gravity | `vy += g·dt`, explicit kick | **high** — g = 2200 px/s² ≡ 22 m/s², 2.2× real g at the established scale | **fixed** — g = 9.81 m/s² (= 981 px/s²), unit-tested |
| 2 | Launch/throw velocity | `endDrag(vx, vy)` + `hasVelocity` tokens | correct | correct |
| 3 | Thrust | constant acceleration along `thrustAngle` | correct (idealized rocket) | correct |
| 4 | Electric field force | `a = qE/m` inside rotated rectangle region, Strang-split half-kicks | correct (2nd-order accurate) | correct |
| 5 | Magnetic Lorentz rotation | exact 2D velocity rotation `ω = qB/(100m)`, speed-conserving | correct | correct |
| 6 | Black-hole attraction | *none* — `isBlackHole` set by the `rs = 2GM/c²` fusion but never read by the integrator | **high** — the fused black hole behaved as inert decoration | **fixed** — softened Newtonian pull `a = G·M/(r²+ε²)`, static hole, deterministic |
| 7 | Body–body collisions | impulse `j = (1+e)·v_rel/n` with e = 0.6, equal-mass split | **medium** — impulse is textbook-correct, but positional overlap split was 50/50 regardless of mass | **fixed** — inverse-mass weighted separation |
| 8 | Restitution (walls/ground) | `v' = −e·v`, ground e = 0.42, wall e = 0.62, rest cutoff 55 px/s | correct (realistic inelastic bounce) | correct |
| 9 | Ground friction on bounce | `vx *= 0.88` per ground contact | correct (tangential loss) | correct |
| 10 | Linear drag ("near vacuum") | geometric `v *= (1 − 0.018·dt)` | correct (documented convention) | correct |
| 11 | Friction token (μ) | stronger geometric damping `k = 8/s` | correct (viscous-style damping, documented) | correct; benchmark now uses the true glyph U+03BC |
| 12 | Fusion mass merge | `max(m1, m2)` + momentum-weighted velocity | acceptable game convention (documented) | unchanged |
| 13 | Split mechanics | components fly apart, grace frames | correct | correct |
| 14 | Rod constraints | static guide planks (no dynamics by design) | documented design choice | unchanged |
| 15 | Electron-beam trails | cosmetic alpha decay | not physics | unchanged |
| 16 | Apparatus scenes (Wien, Fadenstrahlrohr, mass spec, capacitor, Hall) | scene-tuned parameters around the same integrator | correct (Wien balance regression fixed in v1.1) | re-verified green |

## Issues found and status

| ID | Severity | Issue | Evidence | Status |
|----|----------|-------|----------|--------|
| P1 | high | Gravity 2.2× real-world strength at the engine's own 100 px/m scale | old `gravityAcceleration = 2200f` | **fixed** (g = 981 px/s²) |
| P2 | high | Fused black hole exerted no gravity at all | `isBlackHole` only set in `fuseInto`, never read in `step` | **fixed** (softened Newtonian attraction) |
| P3 | medium | Collision separation ignored mass (50/50 push) | old `overlap * 0.5f` on both sides | **fixed** (inverse-mass weights) |
| P4 | low | Unit system implicit, undocumented | no `pxPerMeter` anywhere | **fixed** (documented constant + KDoc) |
| P5 | low | Black-hole flags had no simulation meaning | same as P2 | **fixed** (given meaning: attraction source) |

No other physics behavior deviates from its documented real-world model beyond
the tolerances in the benchmark report.

## Integrator notes (documented, not defects)

- Scheme: **semi-implicit (symplectic) Euler**, dt clamp [1, 33] ms —
  frame-rate independent within the clamp; 2nd-order field forces via Strang
  splitting; exact-rotation magnetic update.
- Known O(dt) bias: bounce apex heights read ~12% low at 60 FPS; converges to
  the textbook e² geometric decay as dt → 0 (proven by the dt-convergence test
  `benchmark_restitutionApex_convergesToTextbookAtFineDt`).
- Resting stability: restitution cutoff (55 px/s) + collision grace frames keep
  stacked bodies still within 2 px/frame (tested over 600 frames).
- Determinism: no randomness, no wall-clock reads, ordered list iteration —
  bit-identical repeat runs (tested over 600 frames including a fusion event).

## Legacy note

`FormulaEngine.kt` (pre-simulation board model) contains no integrator; its
tests still pass. It is scheduled for consolidation (UPGRADE_PLAN P1) and is
excluded from physics-correctness claims.
