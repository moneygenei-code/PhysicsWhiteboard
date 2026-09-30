# Handover — Physics Audit & Realism Pass (v1.2)

What changed, how to verify it, and what to know before touching the engine.

## What this pass did

1. **Real gravity.** `gravityAcceleration` went from 2200 px/s² (≈ 22 m/s² at
   the engine's own 100 px = 1 m scale) to `9.81f * pxPerMeter` = 981 px/s².
   Falling masses now take textbook time t = √(2h/g).
2. **Black holes became physical.** Fusing `G + M + c` (Schwarzschild radius)
   previously only changed the label; the resulting body had no gravitational
   effect. It now attracts every dynamic body with softened Newtonian gravity
   a = G·M/(r² + ε²), clamped, deterministic, hole stays static.
3. **Mass-aware contact.** Two colliding bodies used to be pushed apart 50/50
   regardless of mass. Separation is now inverse-mass weighted (heavy body
   barely moves).
4. **Documented unit system** in code (`pxPerMeter`, KDoc on PhysicsConstants).
5. **New benchmark suite** `PhysicsBenchmarkTest` (12 tests): free fall,
   projectile, elastic collision momentum + e = 0.6 outcome, restitution
   decay incl. dt-convergence, μ-damping, black-hole inverse square,
   bit-level determinism, zero-mass/extreme-velocity/stacking edge cases.

## How to run the tests

```bat
cd C:\Users\Danie\PhysicsWhiteboard
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot
gradlew.bat testDebugUnitTest      :: 78 tests, expect all green
gradlew.bat assembleDebug          :: APK at app\build\outputs\apk\debug\app-debug.apk
```

Single command, no device or emulator needed — the engine is pure Kotlin and
runs on the JVM. CI (GitHub Actions) runs the same suite on every push.

## How to read the reports

- `docs/physics-audit/AUDIT_REPORT.md` — every physics behavior in the engine,
  what it does, verdict before/after, issue list with severities (P1–P5, all fixed).
- `docs/physics-audit/BENCHMARK_REPORT.md` — simulated vs textbook numbers with
  error margins (free fall 0.13%, projectile 2.9%, momentum ≤ 0.5%, etc.) and
  the tolerance rationale.
- `docs/physics-audit/TUNING_SHEET.md` — every constant, its value, its
  real-world meaning, and why it was chosen.

## Interpreting the one "big" deviation

At 60 FPS, bounce apex heights decay by ~0.155 per bounce instead of the
textbook e² = 0.1764 (~12% low). This is the documented first-order bias of
the symplectic-Euler integrator (each flight arc loses v₀·dt/2 of rise), not a
physics bug: `benchmark_restitutionApex_convergesToTextbookAtFineDt` shows the
same scenario converging to within 4.5% of e² at dt = 4 ms. If you ever need
continuum-exact apexes at 60 FPS, switch to velocity Verlet — out of scope here.

## Known limitations

- No on-device verification was possible on this machine (no AVD system
  images, no attached device); all evidence is JVM-level. The engine code is
  identical at runtime, but a smoke test on a real tablet is recommended.
- Apparatus-scene field magnitudes are sim-unit-tuned (qualitatively correct
  and relation-verified), not absolute SI values — see tuning sheet.
- Legacy `FormulaEngine.kt` has no physics integrator and is excluded from
  physics claims (UPGRADE_PLAN P1 consolidation still open).
- The 5% benchmark budget holds for any frame rate thanks to the dt clamp;
  below ~20 FPS trajectories remain identical but slow-motion.

## Where the evidence lives

- Test XML summaries: `app/build/test-results/testDebugUnitTest/`
- Captured full-run logs: `delivery/physics-audit/evidence/` (both runs)
- Git: single commit with engine diff + tests + docs (message lists everything).
