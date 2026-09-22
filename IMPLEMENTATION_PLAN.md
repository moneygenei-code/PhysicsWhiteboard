# Implementation Plan — Modes, Geräte Fixes, Drag-in, Throw Physics, Fusion Audit

Date: 2026-09-22. Source: user report ("normal mode where m+v=mv", Geräte/sliders
unusable, click-only palette, no throw momentum, broken fusions like m+c).

## 1. Debug findings

| # | Report | Root cause |
|---|--------|------------|
| 1 | No "normal" mode; `m+v` always becomes `p = m·v` | `fusionPlan()` unconditionally maps pairs to equations; no mode exists |
| 2 | `q+v+B → FL`, `m+v+q+B → r`, `G+M+m`, `G+M+c` unreachable | Fusion combines only two bodies at a time and no pairwise intermediate exists for these sets (verified by set analysis of `fusionPlan`) |
| 3 | `m+c → mc²` "doesn't work" | Fusion works, but the typesetter renders bare `mc²` without `E =`; same LHS-stripping for `FL`, `UH`, `v=E/B`, fractions, `2GM/c²` |
| 4 | Fadenstrahlrohr sliders can't be changed | `expVoltageUb`/`expCurrentIs` are plain engine vars, not Compose state → `Slider` never recomposes; only the first electron / B-field updated |
| 5 | Geräte bodies can't be moved (`E` under `B`) | Hit testing is z-order only; Wien `E`+`B` share one centre and `B` covers `E` |
| 6 | Palette is click-only | Tiles only use `clickable` → spawn at centre with random offset |
| 7 | Released bodies "stop and fall down" | `endDrag` always zeroes velocity (deliberate whiteboard placement, but no throw) |
| 8 | Dead `hasVelocity`; `v`/`μ` feel inert; no `e` in palette | Flag never read by `step()`; dock has an empty slot instead of the electron |
| 9 | `q+t → I` can't be split | Result stores `componentChars=["I"]` (size 1) → `splitFormula` bails out |
| 10 | Preview snap vs. fusion range | `fusionPreviewDistance` clamped to 30–48px while big formulas need more; could push pairs out of range or deep inside glyphs |

## 2. Implementation (done)

**`PhysicsSimulationEngine.kt`**
- New `enum FusionMode { PHYSIK, NORMAL }` + `var fusionMode` (default PHYSIK).
- NORMAL: `canConcatenate` (no fields, no rods) + `tryFuseSymbols` concatenates
  target-first (`m+v→mv`, duplicates kept so `m+m→mm`); tight 38px overlap
  threshold in `findFusionTarget` so adjacent placement still works.
- PHYSIK: `intermediateFusionPlan` — any strict subset (size ≥ 2) of the six
  multi-targets fuses into a canonical intermediate (`q·v`, `G·M`, `m·q·v`, …)
  so every documented formula is reachable pairwise.
- Intermediate plans are rejected when a field region is involved, so building
  a formula next to (or inside) apparatus fields can't absorb them; exact rules
  (`E+B`, `m+B`, `I+B`, final `containsAll` steps) still accept fields.
- New shared rule `{m,q,B} → T = 2π·m/(q·B)`; Schwarzschild renamed to the full
  `rs = 2GM/c²` (black-hole flag follows).
- `isFusedCurrent` flag: fused `I` splits back into `q`+`t` (incl. inside `UH`
  and groups); plain palette `I` stays atomic.
- `μ` now has gravity + friction (falls, damps visibly when thrown); `v`/`c`
  rest until thrown so formulas stay easy to build.

**`FormulaTypesetting.kt`** — every formula renders its complete equation
(`E = m·c²`, `FL = q·v·B`, `UH = …`, `v = E/B`, `r = …`, `T = …`, both
gravitation forms, `rs = 2GM/c²`, `v = √(2qU/m)`, `E = ½·m·v²`, `F = q·E`);
lone symbols centred on the body.

**`BodyGeometry.kt`** — `fusionPreviewDistance` now scales with both bodies but
is capped at 80% of the fusion range, so the snap can never break a fusion.

**`MainActivity.kt`**
- Header toggle `∑ Physik` / `abc Normal` + subtitle hint.
- `PaletteTile`: tap spawns at centre (as before); long-press drags the symbol
  onto the board with fusion preview, trash delete, and fling on release.
  Palette gains the electron (`e` replaces the empty slot).
- `findBodyAt`: size-priority hit testing (small bodies win; stable for ties).
- Throw/fling: ~120ms pointer-velocity window, clamped to ±2600 px/s, passed
  to `endDrag` when no fusion happens. Shared `move/finish/cancelActiveDrag`
  helpers for canvas + palette gestures.
- Fadenstrahlrohr sliders hoisted to Compose state (`ubSlider`/`isSlider`) and
  applied to all electrons / B-fields.

**Tests** (`PhysicsSimulationTest.kt`) — 12 new cases: normal concat (+duplicates,
field/rod exclusion), Lorentz / radius / gravitation / Schwarzschild chains,
field-absorption guard, complete `E = m·c²` / `T` (both variants) / `UH`
rendering, `I` split vs. palette `I`, `μ` physics, throw-velocity release.

**`README.md`** — modes, stepwise chains, drag-in, fling, `e` palette, sliders.

## 3. Follow-up physics audit (same day) — "make sure everything functions"

**Critical find: Lorentz force was mirrored.** The B-field velocity rotation used
`-omega·dt`; the y-flip between physics and screen coordinates means the same
matrix needs `+omega·dt`. A positive charge moving +x with B out-of-page
deflected screen-up instead of screen-down (verified 4 ways, incl. right-hand
rule and rotation-matrix mapping). Fix: sign corrected in `step()`; Wien E
flipped to screen-up so `qE` still cancels `FL` for v = 420; B drawing now
shows ⊗ for into-page (`bDirectionZ < 0`, e.g. Hall) and ⊙ otherwise.

**Scene retunes** (all measured in a faithful JS port of the integrator first):
- Fadenstrahlrohr: B was 10× too strong (32px micro-orbit). Now `Is·200`:
  r ≈ 158px at defaults; weak Is lets the beam escape, strong Is winds it
  tight; injection mirrored to +120px so the corrected orbit stays contained.
- Hall: B = 1800 froze carriers in 2px orbits AND 6 electrons spawned overlapped
  (blasted apart frame 1) AND the rod was shoved away by collisions. Now B = 25,
  3 carriers at 120px spacing with vx = 120, rods excluded from collisions
  (static guides).
- Deflection: E = 1600 shot the beam off-screen (−838px). Now E = 400: textbook
  parabola exiting the right side at (125, −110).
- Massenspektrometer: analyzer 1200 → 2400 (separation 17px → 58px); ²²Ne
  injected 170px behind ²⁰Ne (pulsed source, no spawn overlap).
- Wien: unchanged magnitudes, still balanced (drift −12px < 45px test bound).

**Also fixed:** gravitation typesetter branches tightened to exact matches (a
`G·M·m`-style product or normal-mode `GMm` group hijacked the fraction
layout); `{G,M,m,r}` added as multi-target so gravitation accepts an explicit
`r` stepwise.

**New tests (6):** right-hand-rule sign lock (q+ down, e− up), Fadenstrahlrohr
orbit (loop span, containment, speed conservation), Hall drift (plank static,
carriers drift + deflect + stay fast), deflection parabola (right exit between
plates, upward arc), mass-spec separation (solo runs, sep > 40), gravitation
with explicit r.

## 4. Verification

- **Syntax:** all 10 Kotlin files parsed clean with tree-sitter-kotlin
  (validator itself checked against broken + valid samples).
- **Check A (real code):** every `FusionPlan` + catalog formula from the actual
  sources routes to its intended typesetter branch; all 40 intermediates,
  normal groups, and single symbols fall through to the linear renderer.
- **Check B (physics):** faithful port of `step()` + scenes measured every
  apparatus quantitatively before/after (orbit radii, drifts, separations,
  exit points, RHR sign) — all numbers in §3.
- **Check C (real rules):** rule sets extracted from `fusionPlan` via regex;
  all 25 documented fusion steps reachable pairwise, field/intermediate guards
  and invalid combos verified blocked.
- Every new unit test's exact parameters (dt, steps, canvas) were pre-run in
  the harness with margins of 2–10× on each assertion.
- NOTE: the Gradle suite itself could not be executed here — the sandbox has
  no JDK / Android SDK and those hosts are unreachable. Please run on a dev
  machine: `bash ./gradlew testDebugUnitTest` and
  `bash ./gradlew assembleDebug`, then manually check on device: mode toggle,
  `q+v→q·v→+B→FL`, `m+c→E = m·c²`, palette long-press drag, fling/throw,
  Fadenstrahlrohr sliders (orbit opens/tightens), Wien `E` grab, Hall ⊗.
