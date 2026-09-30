# Physics Parameter Tuning Sheet — PhysicsWhiteboard v1.2

All constants live in `PhysicsConstants` (PhysicsSimulationEngine.kt) and the
scene setup in `loadScene`. Unit system: **100 px = 1 m**, time in seconds,
mass in symbol units, screen y down.

## World constants

| Parameter | Value | Real-world meaning | Reasoning |
|-----------|-------|--------------------|-----------|
| `pxPerMeter` | 100 | scale constant | Established scene geometry (0.5 m ≈ glyph sizes, 2–3 m canvas); now the single source of truth |
| `gravityAcceleration` | 9.81 × 100 = 981 px/s² | Earth gravity | Was 2200 px/s² (= 22 m/s², 2.2× real). Fixed to real g so fall times match textbook √(2h/g) within 0.2% |
| `nearVacuumDrag` | 0.018 /s | weak air resistance | Keeps thrown `v` tokens gliding "space-like" while giving arcs a subtle decay; costs ~2.9% projectile range (documented) |
| `frictionStrength` | 8.0 /s | μ-token damping rate | Exponential convention `v *= (1−k·dt)`; makes the friction symbol visibly die out in ~0.5 s without freezing mid-air |
| `groundRestitution` | 0.42 | coefficient of restitution e | Realistic hard-floor bounce (rubber-ball-like); produces geometric apex decay e² = 0.1764 per bounce |
| `wallRestitution` | 0.62 | side-wall e | Slightly livelier than the floor so side bounces stay readable but still lose energy |
| ground rest cutoff | 55 px/s | — | Below this incoming speed the body settles instead of micro-bouncing (kills jitter, tested < 2 px/frame) |
| ground tangential loss | ×0.88 per contact | sliding friction impulse | One-shot horizontal loss per bounce; simple and stable |
| dt clamp | [0.001, 0.033] s | — | 30–1000 Hz; keeps symplectic Euler stable and frame-rate independent |

## Black-hole attraction (new)

| Parameter | Value | Reasoning |
|-----------|-------|-----------|
| `G_SIM` | 5.0 × 10⁷ px³/(u·s²) | Chosen so a mass-3 hole gives a = G·M/r² ≈ 1670 px/s² at r = 300 px (~1.7 g): dramatic but controllable orbits on screen |
| softening ε | 60 px | Plummer-style softening: removes the singularity inside the hole; with G above it caps a(0) at ~13.9 g — strong, finite, explosion-free |
| acceleration clamp | 6000 px/s² | Second safety net for extreme masses; ≈ 6 g max |
| range | 900 px | Interaction radius; keeps the O(n²) loop cheap and limits far-field noise |
| hole dynamics | static | The hole is a scene anchor (like field sources); it is never accelerated by its own pull |

## Collision parameters

| Parameter | Value | Real-world meaning | Reasoning |
|-----------|-------|--------------------|-----------|
| restitution (body-body) e | 0.6 | mixed-material collision | Hard-coded in the impulse `j = (1+e)·v_rel/n`; between the wall (0.62) and floor (0.42) values |
| separation weighting | inverse-mass | contact mechanics standard | Was 50/50; now the heavier body moves less — realistic when `m` hits `M` |
| mass floor | 0.001 u | — | Guards division by zero for zero/near-zero masses (tested) |
| grace frames | 3 after fusion/split | — | Prevents freshly created bodies from re-colliding/absorbing instantly |

## Scene constants (tuned in v1.1, unchanged, re-verified)

| Scene | Parameter | Value | Physics it realizes |
|-------|-----------|-------|---------------------|
| Wien filter | E magnitude | 5880 | Fel = FL for v = 420 px/s: qE = qvB/100 with B = 1400 |
| Wien filter | B magnitude | 1400 | with the square E-region (halfWidthScale = 1) |
| Fadenstrahlrohr | B = 200·Iₛ, v = √(U_B/300)·380 | slider-driven | r = m·v/(q·B/100) orbit inside the 240 px Helmholtz field |
| Mass spectrometer | B = 2400, v = 360 | — | r₂₀ = 300 px vs r₂₂ = 330 px isotope separation |
| Deflection capacitor | E = 400 | a = qE/m ≈ 800 px/s² | textbook parabola across the plate region |
| Hall effect | B = 25 | gentle drift | carriers cross the 260 px plank while deflecting |

Note: apparatus scenes predate the pxPerMeter formalization; their field
magnitudes are sim-unit numbers tuned to produce the correct *qualitative*
experiment behavior and the quantitative relations (v = E/B, r = mv/qB)
already covered by PhysicsSimulationTest. Changing them to absolute SI values
is a v1.3 candidate and would require re-tuning the visual scene scale.
