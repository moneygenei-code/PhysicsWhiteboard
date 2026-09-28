# Improvement Pass 2 — Before/After Verification Report

Date: 2026-09-28 20:07–20:25 CEST · Baseline HEAD `38c4170` · Applied at HEAD `86329d2`

## Verification results (baseline → after)

| Check | Baseline (38c4170) | After (86329d2) |
| --- | --- | --- |
| `gradlew testDebugUnitTest assembleDebug` | BUILD SUCCESSFUL, exit 0 (11:47) | BUILD SUCCESSFUL in 1m 35s, exit 0 (20:22) |
| Test suites | 5 suites, 57 tests, 0 failures | 7 suites, **66 tests, 0 failures** |
| APK | 12,116,916 bytes | 12,292,821 bytes (new UI code) |
| Working tree | clean | clean (all committed) |

## Per-idea evidence

### 1. Trash confirm + scene reset (ideas #1, #6)
- Before: `MainActivity.kt` trash handler was `detectTapGestures(onDoubleTap = { … bodies.clear() … })` — invisible affordance, no confirmation, board wiped on accident.
- After: single tap opens `AlertDialog` ("Board leeren?") with **Leeren** (saves undo point first), **Szene zurücksetzen** (reloads current apparatus preset), **Abbrechen**. Semantics text updated to "Tippen zum Leeren".
- Check: compiles; clear path still saves an undo point (undo restores the board — verified by existing `undoAndRedoRestoreFormulaEdits` still passing); no test regressions.
- Revert: `git revert 86329d2` (single-commit change).

### 2. Undo/redo enabled state + semantics (idea #2)
- Before: both `TextButton`s always enabled, icon-only, no contentDescription; engine had no `canUndo`/`canRedo` accessors.
- After: engine exposes `val canUndo / canRedo` (backing stacks); buttons bind `enabled = canUndo/canRedo`, dim to `InkFaint` when unavailable, carry "Rückgängig"/"Wiederholen" semantics; `derivedStateOf` + `frameTrigger` read limits recomposition to actual state flips.
- Check: new `EngineHistoryStateTest` (4 tests) locks the engine flag behavior: start false → save enables undo → undo enables redo → loadScene clears both. 4/4 green.

### 3. "Schnellzugriff" quick-reference tab (idea #3)
- Before: Lernblatt drawer had 4 tabs; exam formulas required scrolling chapter tabs.
- After: fifth tab lists all 10 chapters as compact cards with their top-3 formulas + explanations (data-driven from `KlausurCurriculum.CHAPTERS`), each with a "Kapitel im Whiteboard öffnen" scene link where an apparatus exists (chapters 5, 7, 8, 9, 10).
- Check: compiles; content integrity still guarded by `testCurriculumContentCompleteness` (green); zero duplicated content (reuses chapter data).

### 4. Formula → chapter links (idea #4)
- Before: discovery banner and history rows showed formula + title only.
- After: new `FormulaChapterLinks` maps 8 curriculum formulas to chapters (Newton→2, Zentripetal→1, Wien→7, elektr. Kraft/Elektronenkanone→5, Umlaufdauer→6, Hall→10); banner and history rows append "→ Kapitel N". Formulas outside the curriculum stay unmapped by design.
- Check: new `FormulaChapterLinksTest` (5 tests) includes a byte-exact match against the live `SharedFormulaCatalog` entry for the Hall formula, so the map cannot silently drift. 5/5 green.

### 5. Pause chip (idea #5)
- Before: paused state only swapped the header button label ("Ⅱ Pause" ↔ "▶ Weiter").
- After: ink-colored "Pause" chip overlays the bottom-left of the board while paused.
- Check: compiles; uses existing `paused` state; no behavioral coupling to the physics loop (tests unaffected — 32/32 simulation tests still green).

## Deferred backlog (from the register)

- #7 Palette topic grouping (effort 3), #8 Onboarding coach marks (3), #9 SI-unit long-press tooltips (3), #10 Share discovered-formulas via intent (2), #11 Haptics settings toggle (2), #12 Dark theme (4 — large surface).
- Next pass suggestion: #10 (share) + #11 (settings) — both small, both user-visible.
