# v1.1 Verification Evidence

## Before (baseline, commit b1b013c, 2026-09-28 ~10:56)

- Gradle run: `gradlew.bat testDebugUnitTest assembleDebug` → **BUILD FAILED**, exit 1
  - `48 tests completed, 1 failed`
  - `PhysicsSimulationTest > testWienFilterEquilibrium FAILED`
  - Failure message (test XML): `Wien Filter: matched velocity should experience near zero net deflection, was 237.34381`
- Last APK in tree: 2026-09-22 22:10, 12,167,228 bytes (build predates this session; suite had never been run on this machine)
- Intermediate check after geometry fix alone: failure residual `was 51.20822` → integrator drift identified

## After (HEAD b9c814c, 2026-09-28 11:34)

- Gradle run: `gradlew.bat clean testDebugUnitTest assembleDebug` → **BUILD SUCCESSFUL**, exit 0
- APK: `app/build/outputs/apk/debug/app-debug.apk`, 12,116,916 bytes, versionCode 2 / versionName 1.1
- Test summary (XML reports, `app/build/test-results/testDebugUnitTest/`):

| Suite | Tests | Failures | Errors |
| --- | --- | --- | --- |
| BodyGeometryTest | 3 | 0 | 0 |
| DiscoveryLogTest *(new)* | 5 | 0 | 0 |
| FormulaEngineTest | 13 | 0 | 0 |
| FusionEventDiscoveryTest *(new)* | 4 | 0 | 0 |
| PhysicsSimulationTest | 32 | 0 | 0 |
| **Total** | **57** | **0** | **0** |

- Wien fix progression: 237.3px drift → 51.2px (square E-region) → PASS (Strang splitting)

## Environment notes

- JDK: Microsoft JDK 17.0.20.8 (JAVA_HOME) · Gradle wrapper 8.10.2 · AGP 8.4.2 · Kotlin 1.9.24
- Emulator: not runnable on this machine — SDK contains `emulator/` + `platform-tools/` but no system images and no cmdline-tools, so no AVD can be created; `adb devices` empty at session start and end. Device-side checks are listed in HANDOVER.md.
