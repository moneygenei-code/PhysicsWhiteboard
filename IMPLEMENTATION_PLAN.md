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

## 3. Verification

- Full set-reachability audit of `fusionPlan` (all 12 README fusions + legacy
  tests) done by review; branch-order audit of the typesetter for every engine
  formula string, intermediate, group, and single symbol.
- NOTE: the Gradle suite could not be executed here — the sandbox has no JDK /
  Android SDK and those hosts are unreachable. Please run on a dev machine:
  `bash ./gradlew testDebugUnitTest` and `bash ./gradlew assembleDebug`,
  then manually check on device: mode toggle, `q+v→q·v→+B→FL`, `m+c→E = m·c²`,
  palette long-press drag, fling/throw, Fadenstrahlrohr sliders, Wien `E` grab.
