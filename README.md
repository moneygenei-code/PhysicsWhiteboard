# Physics Whiteboard — Physik 12 Klausurvorbereitung

An interactive physics sandbox and Grade 12 curriculum study suite for Android (Jetpack Compose). Designed with an academic paper parchment aesthetic (`#F4F1EA`) and deep ink typography (`#26221C`).

---

## Features

### 1. Physics Sandbox (60 / 120 FPS)
- **Living Physical Properties**:
  - `g`: Gravitational acceleration
  - `a`: Directional propulsion / thrust with rotatable handle
  - `v`: Linear momentum and velocity
  - `r`: Radius for circular motion
  - `μ`: Sliding friction / damping
  - `t`: Rigid rods and planks
  - `q`, `e`: Electric charges (positive ions & electrons)
  - `E`: Rotatable electric field boxes with directional vector field lines
  - `B`: Circular magnetic field regions with field indicators ($\odot$ out-of-page, $\otimes$ into-page)
- **Exact Lorentz Force Physics**:
  - $\vec{F}_{\mathrm{L}} = q(\vec{v} \times \vec{B})$ integrated via exact 2D velocity vector rotation, guaranteeing speed conservation to machine precision without numerical drift.
  - Correct circular orbits: $r = \frac{m \cdot v}{q \cdot B}$, $T = \frac{2\pi m}{q \cdot B}$.
  - Fading electron beam glow trails for all moving charged particles.

### 2. Fusion Modes (Physik / Normal)
The header toggle switches how symbols combine:
- **∑ Physik**: symbols fuse into physics equations (`m + v → p = m·v`). Multi-symbol formulas build stepwise through visible intermediate products, e.g. `q + v → q·v`, then `+ B → FL = q·v·B`. Intermediates never absorb field regions, so apparatus setups stay intact while a formula is built next to them.
- **abc Normal**: symbols only concatenate into plain groups (`m + v → mv`, `m + m → mm`). Fusion needs a deliberate deep overlap, so symbols can still be placed side by side. Fields and rods never fuse in this mode.

Double-tap any formula or group to split it back into component symbols (a fused current `I` splits back into `q` + `t`).

### 3. Formula Fusion & Typesetting (Physik-Modus)
Drag symbols together on the whiteboard to fuse them into formulas. Every result renders as a complete equation:
- $m + a \to F = m \cdot a$
- $m + v \to p = m \cdot v$
- $v + r \to a = \frac{v^2}{r}$
- $m + v + r \to F = \frac{m \cdot v^2}{r}$
- $m + \frac{1}{2} \to E = \frac{1}{2} m v^2$
- $m + c \to E = m \cdot c^2$
- $E + B \to v = \frac{E}{B}$ (Wien Filter condition)
- $q + v + B \to F_{\mathrm{L}} = q \cdot v \cdot B$
- $m + v + q + B \to r = \frac{m \cdot v}{q \cdot B}$
- $m + B \to T = \frac{2\pi \cdot m}{q \cdot B}$
- $I + B \to U_{\mathrm{H}} = R_{\mathrm{H}} \cdot \frac{I \cdot B}{d}$ (Hall effect)
- $v + t \to$ Rotatable physical rod
- $q + t \to I$ (Electric current $I = q/t$)

- Stepwise chains: $q + v \to q·v \to +B \to F_L$, $m + v + q + B \to r$, $G + M \to G·M \to +m$ (gravitation) or $+c$ ($r_s = 2GM/c²$)

### 4. Apparatus Presets
Quickly load classic experimental setups:
1. **Wien-Filter**: Crossed $E$ and $B$ fields with velocity-dependent deflection ($v = E/B$).
2. **Fadenstrahlrohr**: Electron gun in Helmholtz magnetic field with real-time sliders for accelerating voltage $U_{\mathrm{B}}$ and coil current $I_{\mathrm{S}}$.
3. **Massenspektrometer (Bainbridge)**: Velocity filter stage followed by magnetic analyzer stage showing isotope separation ($^{20}\mathrm{Ne}$ vs $^{22}\mathrm{Ne}$) with $2r$ detector spacing.
4. **E-Feld Ablenkung (Querfeld & Längsfeld)**: Parabolic electron trajectories inside deflection capacitor plates.
5. **Hall-Effekt**: Conductor plank with drifting charge carriers showing Hall voltage build-up.

### 5. Klausurvorbereitung Suite (LEIFIphysik)
- **10 Core Curriculum Chapters**: Complete summaries, derivations, typical exam pitfalls, and 15-point tips.
- **Formulas Tab**: Compact formula collection with units and usage rules.
- **Self-Test**: 14 exam-style problems across AFBs I, II, and III with expandable model solutions.
- **15-Punkte Strategy**: Anforderungsbereiche weighting, formulation catalogue, and exam day checklist.

---

## Build & Run

### Prerequisites
- JDK 17+
- Android SDK 34 (minSdk 26)

### Running Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Building the APK
```bash
./gradlew assembleDebug
```
The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Installing to Device / Tablet
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Sandbox interaction rules

- **Drag** a symbol or field to move it. While it is held, physics and collision impulses are paused for that body so it cannot slide into another symbol.
- **Throw/fling**: releasing a body with speed keeps it moving with the release velocity instead of stopping dead.
- **Fuse** compatible symbols by releasing them when the highlighted target appears. The result snaps to the midpoint and uses the full rendered equation, for example `p = m·v` or `E = m·c²`. In Normal mode, fusion needs a deep overlap and only concatenates (`mv`).
- **Double-tap** a formula to split it back into its component symbols.
- A visible **`m` mass falls under gravity** by default, including after fusion. The ground and walls use the app's visual simulation scale rather than SI metres.
- **Palette**: tap a tile to spawn near the board centre, or long-press and drag a tile to place the symbol exactly (including electrons `e` and fields `E`/`B`).
- Grabbing prefers small bodies over big field regions, so the Wien `E`-field can be pulled out from under the `B`-circle.
- Rotate `a`, rods, and `E` with their circular handle. An electric field's dashed rectangle, arrows, and force region rotate together.
- The header provides **Pause**, **Undo**, **Redo**, **Reset**, and the **∑ Physik / abc Normal** mode toggle. Tap the trash can to clear the board, or drag a single symbol onto it to delete just that one; both actions can be undone.
- The layout adapts to phones: the header controls scroll horizontally and the study drawer fills at most 94% of narrow screens.

The Gradle wrapper requires a configured JDK 17+ installation. If the wrapper file is not executable on a Unix checkout, run it with:

```bash
bash ./gradlew testDebugUnitTest
```
