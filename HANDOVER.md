# HANDOVER — Physics Whiteboard

How to open, build, run, and verify the v1.1 improvements.

## Open
1. Android Studio (Jellyfish or newer; the project uses AGP 8.4.2).
2. `File → Open…` → select `C:\Users\Danie\PhysicsWhiteboard` (it is already a linked Studio project — `.idea` and `local.properties` are present).
3. Let Studio sync; the Gradle wrapper (8.10.2) and JDK are picked up automatically (Studio JBR works; a standalone JDK 17 was used for verification).

## Build & test (command line)
```bat
cd C:\Users\Danie\PhysicsWhiteboard
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot
gradlew.bat testDebugUnitTest      :: 57 tests, expect all green
gradlew.bat assembleDebug          :: APK at app\build\outputs\apk\debug\app-debug.apk
```

## Install & run
```bat
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
App ID `com.example.physicswhiteboard`, versionName 1.1 (versionCode 2). Landscape-only by manifest (unchanged behavior).

## What changed since v1.0 (user-visible)
1. **Formula discovery feedback** — fusing symbols in Physik mode now pops a brief "✓ Neue Formel" banner (top-left) the first time each formula is created.
2. **Formula history** — new ƒ button left of the trash (bottom-right) opens "Entdeckte Formeln"; the list persists across app restarts (SharedPreferences). Empty state tells the user what to do.
3. **Haptics** — short tick on grabbing a symbol, long-press buzz on fusion success and on split.
4. **Accessibility** — palette tiles are 48dp with German content descriptions; trash/history/banner are screen-reader labeled.
5. **New launcher icon** — parchment "F=ma" adaptive icon (round variant included).
6. **Physics fix** — Wien filter: the balanced particle now stays balanced (regression from the rotated-E-field rework is fixed; see CHANGELOG).

## First checks on device (not yet done — no AVD/device was available on this machine)
1. Launch on FREE_SANDBOX → grab `m` → falls; fuse `m`+`v` → banner "p = m·v" appears once; open ƒ panel → entry present; force-stop + relaunch → entry still there.
2. Toggle "abc Normal" → `m`+`v` concatenates to `mv`, no banner.
3. Gerät: Wien-Filter → matched q particle crosses horizontally ≈ level; Fadenstrahlrohr sliders open/tighten the orbit.
4. Double-tap a formula → splits; haptic fires.
5. Rotate `E` (Wien) → dashed box, arrows, handle rotate together (pre-existing feature, unchanged).

## Verification status (evidence)
- `gradlew clean testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL, exit 0 (2026-09-28 11:34).
- 57/57 unit tests green across 5 suites (XML reports under `app/build/test-results/testDebugUnitTest/`).
- APK produced: `app/build/outputs/apk/debug/app-debug.apk` (12.1 MB, versionCode 2).
- Emulator: impossible locally — SDK has `emulator/` + `platform-tools/` but **no system images** and **no cmdline-tools**, so no AVD could be created. CI (GitHub Actions) will run the same build + tests on push.

## Known limitations / next steps
- Formula history is capped at 30 entries and stores formula text + title (timestamps only in-memory session).
- `FormulaEngine.kt` (legacy BoardItem model) remains only exercised by its own tests; consolidating it with the simulation engine is the UPGRADE_PLAN P1 item.
- CI requires the repo to be pushed to GitHub; the workflow file is ready at `.github/workflows/android-ci.yml`.
