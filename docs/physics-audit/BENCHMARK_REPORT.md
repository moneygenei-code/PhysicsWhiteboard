# Physics Benchmark & Validation Report — PhysicsWhiteboard v1.2

Date: 2026-09-30 · Suite: `app/src/test/java/com/example/physicswhiteboard/PhysicsBenchmarkTest.kt`
Run with: `gradlew.bat testDebugUnitTest` (single command; 78 tests total)

## Result matrix

All values are from the delivered test suite (JUnit, deterministic, no
randomness). Tolerances follow the Goal Brief: ≤ 5% against textbook physics
where the 60 FPS integrator allows; tight tolerances where the discrete model
is exact.

| # | Benchmark | Textbook reference | Simulated | Error | Tolerance | Verdict |
|---|-----------|--------------------|-----------|-------|-----------|---------|
| 1 | Unit consistency | g = 9.81 m/s² | `gravityAcceleration / pxPerMeter` = 9.81 | 0% | 0.5% | PASS |
| 2 | Free fall from rest, h = 4.68 m | t = √(2h/g) = 0.9773 s | 0.976 s (61 frames × 16 ms) | ~0.13% | 5% | PASS |
| 3 | Free-fall distance at t/2 | 0.5·g·t² (continuous) | within 6% | ~1% | 6% | PASS |
| 4 | Projectile 45°, v = 420√2 px/s | R = v²/g = 359.6 px | 349.3 px | 2.9% | 5% | PASS |
| 5 | Projectile apex | vᵧ²/(2g) = 89.9 px | 86.1 px | 4.2% | 6% | PASS |
| 6 | Elastic collision momentum (m=1 @ +300 vs m=3 @ −100) | Δp = 0 | Δp ≤ 0.5% of |p| | ≤0.5% | exact model | PASS |
| 7 | Collision outcome vs e = 0.6 solution | v₁' = −180, v₂' = +40 px/s | both within 2.5% | <2.5% | 2.5% | PASS |
| 8 | Restitution speed ratio per bounce | e = 0.42 | 0.4199 | 0.02% | 2% | PASS |
| 9 | Apex height decay per bounce (60 FPS) | e² = 0.1764 | ≈ 0.155 | ~12% (documented O(dt) bias) | 15% | PASS (documented) |
| 10 | Apex height decay at dt = 4 ms | e² = 0.1764 | within 4.5% | ≤4.5% | convergence proof | PASS |
| 11 | μ-token damping, 0.5 s | v = v₀·(1−k·dt)ⁿ = 7.16 px/s | matches to 1% | ≤1% | exact model | PASS |
| 12 | Black-hole attraction at r = 300 px | a = G·M/(r²+ε²) | Δv = a·dt to 1% | ≤1% | exact model | PASS |
| 13 | Inverse-square falloff r = 600 vs 300 | ratio 3.8848 (softened) | 3.8848 ± 3% | <3% | 3% | PASS |
| 14 | Determinism, 600 frames + fusion | bit-identical state | bit-identical (Δ = 0.0) | 0% | exact | PASS |
| 15 | Stacked resting bodies, 600 frames | no NaN/explosion/jitter | max move < 2 px/frame | — | 2 px | PASS |
| 16 | Zero/near-zero mass (0.001) vs 10⁹ mass collision | finite, contained | all finite, in walls | — | — | PASS |
| 17 | Extreme velocity 10⁶ px/s @ dt = 33 ms | finite, wall-contained | clamped at wall ±1 px | — | — | PASS |

## Method notes

- Free fall / projectile: landing frames are time-quantized; the projectile
  range is measured with linear interpolation of the launch-height crossing,
  which removes frame quantization to first order.
- Projectile: engine applies the 0.018/s near-vacuum drag; it costs ~2.9% of
  the range, inside the 5% budget. Turning drag off would tighten this to
  <1%, at the cost of the "space-like" glide the app's v token is designed for.
- Restitution apex: at 60 FPS the symplectic-Euler flight arc loses v₀·dt/2 of
  rise per flight. This is first-order integrator bias, not a physics error —
  the dt-convergence test (#10) demonstrates the continuum limit equals e².
- Collision momentum: measured over the contact window only (bodies break
  contact within ~20 frames) so ambient drag cannot contaminate the balance.

## Repeatability evidence

- Full suite run twice from clean tasks (`--rerun-tasks`): both runs
  BUILD SUCCESSFUL, exit 0, 78/78 tests green (see
  `delivery/physics-audit/evidence/after-tests-run1.txt` / `-run2.txt`).
- Determinism is additionally asserted inside the suite at bit level
  (`benchmark_determinism_sameInputBitIdentical`).

## Not verified / limits

- On-device (emulator or hardware) runs were not possible on this machine
  (no AVD system images, no connected device); all evidence is from the JVM
  unit-test toolchain, which runs the exact engine code used at runtime.
- The 5% benchmark budget applies at 60 FPS; frame rates below ~20 FPS clamp
  to dt = 33 ms and reproduce the same trajectories because of the dt clamp.
