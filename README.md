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

### 2. Formula Fusion & Typesetting
Drag symbols together on the whiteboard to fuse them into formulas. Double-tap any formula to split it back into component symbols:
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

### 3. Apparatus Presets
Quickly load classic experimental setups:
1. **Wien-Filter**: Crossed $E$ and $B$ fields with velocity-dependent deflection ($v = E/B$).
2. **Fadenstrahlrohr**: Electron gun in Helmholtz magnetic field with real-time sliders for accelerating voltage $U_{\mathrm{B}}$ and coil current $I_{\mathrm{S}}$.
3. **Massenspektrometer (Bainbridge)**: Velocity filter stage followed by magnetic analyzer stage showing isotope separation ($^{20}\mathrm{Ne}$ vs $^{22}\mathrm{Ne}$) with $2r$ detector spacing.
4. **E-Feld Ablenkung (Querfeld & Längsfeld)**: Parabolic electron trajectories inside deflection capacitor plates.
5. **Hall-Effekt**: Conductor plank with drifting charge carriers showing Hall voltage build-up.

### 4. Klausurvorbereitung Suite (LEIFIphysik)
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
