# CHANGELOG — Physics Whiteboard v1.1 (versionCode 2)

Date: 2026-09-28 · Baseline: commit `b1b013c` · This release: `95722bf` + `4d4c852`

## Commit 95722bf — Fix Wien filter drift (regression fix)

### `app/src/main/java/com/example/physicswhiteboard/PhysicsSimulationEngine.kt`
| Change | Reason |
| --- | --- |
| `createElectricFieldSource` gains optional `halfWidthScale: Float = 0.67f` param; `fieldHalfWidth = radius * halfWidthScale` | One source of truth for E-region shape; default keeps every existing scene byte-identical in behavior |
| Wien scene passes `halfWidthScale = 1f` (square E-region) | With the rotated-rectangle field rework, the old 0.67 scale left a 49.5px strip where the particle was inside B but outside E; there ω = 14 rad/s wrecked the trajectory (test drift 237px vs <45 bound). A square region is tangent to the B-circle along the beam axis, so the matched ray is always inside both fields |
| E-field force now applied as two half-kicks around the B rotation (Strang splitting) instead of one full kick before it | Sequential full-kick-then-rotate displaces the discrete velocity orbit by ≈ a·dt/2 (47px/s bias at the scene's a = 5880); the symmetric split is 2nd-order accurate, so qE and qvB cancel for the matched particle. Residual drift dropped from 51px → 0px measured by the test bound |

Verification: `testWienFilterEquilibrium` passes again; full suite green (first fully green run on this machine: 48/48 before the new features).

## Commit 4d4c852 — Discovery log, haptics, accessibility, icon, CI

### New files
| File | Purpose/Reason |
| --- | --- |
| `app/src/main/java/.../DiscoveryLog.kt` | Pure-Kotlin rolling log (max 30, dedup by formula text, newest first). Pure Kotlin = unit-testable without Android runtime |
| `app/src/main/java/.../DiscoveryStore.kt` | SharedPreferences persistence ("formula\|title\|timestamp" lines) so discoveries survive app restarts. Deliberately no DataStore dependency → no new libraries, no build risk |
| `FusionEvent` (inside PhysicsSimulationEngine.kt) | Engine→UI event emitted on every final (non-intermediate) PHYSIK fusion; intermediates and NORMAL-mode concatenations emit nothing |
| `app/src/test/java/.../DiscoveryLogTest.kt` | 5 tests: dedup, ordering, capacity eviction, restore order, blank rejection |
| `app/src/test/java/.../FusionEventDiscoveryTest.kt` | 4 tests: event emitted with complete formula + components, Einstein chain, NORMAL emits none, loadScene clears |
| `.github/workflows/android-ci.yml` | CI: JDK 17 temurin, cached Gradle, `testDebugUnitTest` + `assembleDebug`, uploads test XMLs + APK artifacts |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` | Adaptive icon entries |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Parchment-styled "F=ma" vector foreground (ink #26221C on #F4F1EA, amber arrow accent) |

### Modified files
| File | Change | Reason |
| --- | --- | --- |
| `MainActivity.kt` | `consumeFusionEvents()` bridges engine events → banner + persisted log; auto-dismissing "Neue Formel" banner (top-left); collapsible "Entdeckte Formeln" history panel (bottom-right, ƒ button); haptics on drag start / fusion / split (`LocalView.performHapticFeedback`); palette tiles 44→48dp + German `contentDescription`s (`DOCK_NAMES`); trash/history/banner semantics | Spec quick-wins (fusion feedback, formula history, persistence) + accessibility pass (≥48dp targets, screen-reader labels) |
| `MainActivity.kt` | `ApparatusScene.values()` → `ApparatusScene.entries`; explicit `mutableLongStateOf` import replaces inline FQN | Kotlin 1.9 modernization; removes deprecated API + inline FQN |
| `AndroidManifest.xml` | `android:roundIcon="@mipmap/ic_launcher_round"` | Round-icon support on API 26+ |
| `app/build.gradle.kts` | `versionCode 1→2`, `versionName 1.0→1.1` | Release the improvements |
| `res/values/colors.xml` | `ic_launcher_background` #F4F1EA | Adaptive icon background |
| `res/mipmap-{h,m,xh,xxh,xxx}dpi/ic_launcher.xml` | **Deleted** | Dead weight: minSdk 26 means anydpi-v26 adaptive icons serve every device; the legacy density vectors were never selected |
| `gradlew` | executable bit set in git index (100644→100755) | Required for `./gradlew` on Linux/CI |
| `.artifacts/ai_improvement_prompt.md` | tracked in git | Keeps the improvement spec with the repo (was untracked) |

### Intentional behavior changes (no regressions known)
1. Fusing symbols now shows a discovery banner the first time each formula is created and adds it to the persistent history. Fusion mechanics/physics untouched.
2. Launcher icon appearance changes (old purple circle → parchment F=ma).
3. Palette tiles are 4dp larger (44→48dp) to meet touch-target guidance.

### Verification
- `gradlew clean testDebugUnitTest assembleDebug` → exit 0; APK 12.1 MB; 57/57 tests green (3 BodyGeometry + 13 FormulaEngine + 32 PhysicsSimulation + 5 DiscoveryLog + 4 FusionEventDiscovery).
- Emulator/device run not possible on this machine (no AVD system images installed, no connected device) — see HANDOVER.md "First checks on device".


## Pass 2 (2026-09-28, commit 86329d2) — Usability improvements

Brainstormed 12 ideas, scored by impact/effort (see improvement-register.md), applied the top 5:

| File | Change | Reason |
| --- | --- | --- |
| PhysicsSimulationEngine.kt | canUndo/canRedo properties | UI needs the history state without touching private stacks |
| MainActivity.kt | Trash: single tap opens confirm dialog (Leeren / Szene zurcksetzen / Abbrechen) | Old double-tap wipe was invisible + destructive without warning |
| MainActivity.kt | Undo/redo: enabled bound to canUndo/canRedo, dimmed icons, semantics labels | Buttons lied about availability; screen readers had no names |
| MainActivity.kt | New 'Schnellzugriff' drawer tab: top-3 formulas per chapter + apparatus links | Core use case (exam prep) needed a fast path |
| FormulaChapterLinks.kt (new) | Maps 8 fusion formulas to curriculum chapters | Discovery history becomes a study loop |
| MainActivity.kt | Banner + history rows show '-> Kapitel N' | See above |
| MainActivity.kt | Pause chip overlay while paused | Frozen board gave no feedback |
| EngineHistoryStateTest.kt, FormulaChapterLinksTest.kt (new) | 9 tests | Lock the new engine flags and chapter mappings |

Suite after pass 2: 7 suites, 66 tests, 0 failures (was 57/5 suites). Evidence: pass2-verification.md.

## Creative pass (2026-09-28, commit fab05dc) — Signature fusion FX

Companion to the emc2-experience web prototype (see workspace DELIVERY/emc2-experience/):

| File | Change | Reason |
| --- | --- | --- |
| PhysicsSimulationEngine.kt | FusionEvent gains midpoint x/y | UI effects need the fusion point |
| MainActivity.kt | Pulse ring (0.9s) on every final fusion; warm radial bloom flash (0.8s) for E = m.c2 | The signature moment from the creative brief, in-app |
| FusionEventDiscoveryTest.kt | Midpoint assertions | Lock the event geometry |

Suite: 66 tests, 0 failures. APK rebuilt (12,294,986 bytes).
