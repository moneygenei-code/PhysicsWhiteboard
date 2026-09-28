# Improvement Pass 2 — Baseline & Idea Register

Date: 2026-09-28 20:07 CEST · Target: C:\Users\Danie\PhysicsWhiteboard (Android, Kotlin/Compose)

## Baseline (captured before any change)

- Git: HEAD `38c4170e5d9d7b79a24bd5620294ba6411d34f07`, working tree clean (0 dirty files).
- Last verified build at this HEAD: `gradlew clean testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL, exit 0 (11:47), APK 12,116,916 bytes, 57 tests / 0 failures / 0 errors (5 suites).
- App version: versionCode 2, versionName 1.1.
- Known usability facts verified in code:
  - Trash can clears the board only on double-tap (`detectTapGestures(onDoubleTap = …)`), with no visible affordance or label.
  - Undo/redo TextButtons always enabled (no disabled state), icon-only (↶/↷ glyphs), no contentDescription.
  - DiscoveryLog already caps at 30 entries, dedups by formula text — history feature healthy.
  - Lernblatt drawer: 4 tabs; chapters 1–10 have `title/subtitle` only in tab 0.

## Idea register (impact 1–5, effort 1–5; score = impact/effort)

| # | Idea | Problem | Proposed change | Impact | Effort | Score | Status |
|---|------|---------|-----------------|--------|--------|-------|--------|
| 1 | Trash single-tap + confirm | Clearing needs a hidden double-tap; mis-taps wipe the board without warning | Single tap → small confirm dialog ("Board leeren?") with Abbrechen/Leeren; single tap = visible affordance | 4 | 1 | 4.0 | **selected** |
| 2 | Undo/redo enabled state + semantics | Buttons look active even when history is empty; screen readers get no names | Expose `canUndo`/`canRedo` from engine (backing property listeners via state), disable buttons, add descriptions + text labels | 4 | 2 | 2.0 | **selected** |
| 3 | "Wichtigste Formeln" quick-reference section | Students must scroll all 10 chapters to find exam formulas | Add a fifth drawer tab with the top exam formulas (one compact row per chapter, reusing chapter data) | 4 | 2 | 2.0 | **selected** |
| 4 | Formula→chapter link in discovery history | A discovered formula shows a title but not where to study it | Static map formula-text → chapter number; history rows + banner get a "→ Kapitel N" hint | 3 | 2 | 1.5 | **selected** |
| 5 | Pause overlay indicator | When paused, the board looks frozen but nothing says so | Small "Pause" chip overlay when `paused` is true | 2 | 1 | 2.0 | **selected** |
| 6 | Clear preserves scene preset | After trash clear, empty board; user must know Reset restores the apparatus | Add "Szene zurücksetzen" action inside the trash confirm dialog (one extra TextButton) | 3 | 1 | 3.0 | **selected** (implemented with #1) |
| 7 | Palette category grouping | 16 tiles in 4 rows is dense for new users | Group tiles by topic with subtle dividers | 2 | 3 | 0.7 | deferred |
| 8 | Onboarding coach marks | New users don't know long-press-drag exists | First-run tooltip overlay | 3 | 3 | 1.0 | deferred |
| 9 | Unit labels on long-press | Symbols don't show SI units on the board | Long-press tooltip with unit (kg, m/s, N…) | 3 | 3 | 1.0 | deferred |
| 10 | WebView-free formula share/export | Can't export the discovered formula list | Share intent with plain-text list from history panel | 3 | 2 | 1.5 | deferred |
| 11 | Haptic setting toggle | Some users dislike constant haptics | Settings switch persisted in SharedPreferences | 2 | 2 | 1.0 | deferred |
| 12 | Dark theme | Parchment theme is light-only | Material dark palette for the drawer/chrome | 4 | 4 | 1.0 | deferred (large surface) |

## Shortlist (this pass)

1. **#1+#6 Trash confirm + scene reset** — removes a data-loss trap and makes Reset discoverable; cheapest high-impact fix.
2. **#2 Undo/redo enabled state + semantics** — correctness of UI state with almost no risk; also an accessibility win.
3. **#3 Quick-reference tab** — directly serves the app's core promise (Klausurvorbereitung) using existing data only.
4. **#4 Formula→chapter links** — turns the discovery feature into a study loop; static map, unit-testable.
5. **#5 Pause chip** — tiny clarity win while touching the same files anyway.

## Rollback

Every item is one git commit on top of `38c4170`; revert with `git revert <sha>`. No data files, no schema, no irreversible actions.
