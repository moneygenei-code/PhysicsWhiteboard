# AI Improvement Prompt for Physics Whiteboard App

Use this prompt with an AI assistant to systematically improve the Physics Whiteboard app. The app is a Jetpack Compose Android app where users drag physics symbols (m, a, F, v, t, E, p, ω, r, T) onto a canvas and combine them to reveal physics formulas.

---

## 🎯 **High-Level Goal**
Transform the Physics Whiteboard into a polished, educational, and delightful physics formula discovery tool that works reliably on phones, tablets, and foldables.

---

## 📋 **PHASE 1: Fix Critical Build & Runtime Issues**

### 1.1 Resolve All Compilation Errors
- Fix unresolved references: `SnapshotStateList`, `roundToInt`, `roundToIntOffset`, `detectDragGestures`, `offset` modifier
- Fix `detectDragGestures` signature (requires `onDrag` parameter)
- Fix `forEach` ambiguity on `SnapshotStateList`
- Remove unused imports (`Rect`, `RoundedRect`, `LayoutDirection`, `LayoutSize`)
- Ensure Kotlin/Compose version compatibility (Kotlin 1.9.24 + Compose Compiler 1.5.14)

### 1.2 Resource & Theme Fixes
- Verify `Theme.PhysicsWhiteboard` uses `Theme.MaterialComponents.DayNight.NoActionBar` parent
- Ensure all mipmap densities have `ic_launcher.xml` (vector drawable)
- Add adaptive icon support (`ic_launcher_foreground.xml` + `ic_launcher_background.xml`)

### 1.3 Gradle Configuration
```kotlin
// app/build.gradle.kts - verify these are correct:
kotlinOptions { jvmTarget = "1.8" }
composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
dependencies { implementation("com.google.android.material:material:1.11.0") }
```

---

## 📋 **PHASE 2: Core Interaction Polish**

### 2.1 Drag & Drop System
- [ ] **Smooth drag preview** - Show semi-transparent symbol following finger with spring animation
- [ ] **Drop zones / snap-to-grid** - Optional grid overlay for alignment
- [ ] **Multi-touch support** - Drag multiple symbols simultaneously
- [ ] **Drag from palette creates copy** - Don't remove from palette; allow unlimited instances
- [ ] **Long-press to duplicate** on canvas
- [ ] **Swipe to delete** or trash zone at bottom

### 2.2 Combination Logic
- [ ] **Visual feedback** when symbols combine (particle effect, glow, animation)
- [ ] **Formula display** - Show discovered formula in a toast, bottom sheet, or persistent "formula history" panel
- [ ] **Bidirectional combinations** - `ma` and `am` both yield `F = m·a`
- [ ] **Multi-symbol combinations** - e.g., `m` + `v` + `r` → angular momentum
- [ ] **Combination validation** - Only show formula if physically meaningful
- [ ] **Educational mode** - Tap formula to see explanation/derivation

### 2.3 Symbol System
- [ ] **Symbol categories** (Mechanics, Thermodynamics, Electromagnetism, Quantum)
- [ ] **Expandable palette** - Collapsible categories with search/filter
- [ ] **Custom symbols** - User can add own symbols via text input
- [ ] **Subscripts/superscripts** - Render `v²`, `ω₀`, `Δt` properly
- [ ] **Units display** - Show SI units on long-press (kg, m/s, N, J, etc.)

---

## 📋 **PHASE 3: UX/UI Excellence**

### 3.1 Layout & Responsiveness
- [ ] **Adaptive layout** using `WindowSizeClass`:
  - Phone: Palette bottom sheet, full-screen canvas
  - Tablet/Foldable: Side-by-side palette + canvas (Navigation3 List-Detail)
  - Landscape: Palette on left, canvas on right
- [ ] **Edge-to-edge** support with proper inset handling
- [ ] **Keyboard/IME support** - Text input for custom symbols

### 3.2 Visual Design
- [ ] **Material 3 theming** with dynamic color (Material You)
- [ ] **Custom symbol shapes** - Not just circles; squares for scalars, vectors with arrows, tensors with double borders
- [ ] **Color coding by category** - Consistent semantic colors
- [ ] **Dark/light theme** - Proper contrast ratios
- [ ] **Animations** - `animate*AsState`, `AnimatedVisibility`, `Crossfade`
- [ ] **Haptic feedback** on drag start, combination success, errors

### 3.3 Accessibility
- [ ] **Content descriptions** for all symbols
- [ ] **Touch target sizes** ≥ 48dp
- [ ] **Screen reader support** - Announce combinations
- [ ] **High contrast mode** support
- [ ] **Reduced motion** respect

---

## 📋 **PHASE 4: Features & Content**

### 4.1 Formula Engine
- [ ] **Parsable formula DSL** - Store formulas as structured data, not strings
- [ ] **Unit analysis** - Automatically compute result units from inputs
- [ ] **Dimensional analysis** - Prevent invalid combinations (e.g., `m` + `t` = meaningless)
- [ ] **Formula variants** - Show `F=ma`, `a=F/m`, `m=F/a` on repeated combination

### 4.2 Learning Features
- [ ] **Guided tutorials** - "Discover Newton's Second Law" walkthrough
- [ ] **Challenge mode** - "Find 3 formulas using only m, a, v, t"
- [ ] **Progress tracking** - Persist discovered formulas across sessions
- [ ] **Export/Share** - Export canvas as PNG, PDF, or LaTeX
- [ ] **Undo/Redo** - Full history with swipe gestures

### 4.3 Advanced Physics
- [ ] **Vector symbols** - Directional arrows for `F`, `v`, `a`
- [ ] **Coordinate system** - Show x/y axes, grid
- [ ] **Free-body diagrams** - Auto-arrange forces on an object
- [ ] **Simulation mode** - Animate `x(t)`, `v(t)` for given initial conditions

---

## 📋 **PHASE 5: Architecture & Code Quality**

### 5.1 State Management
- [ ] **ViewModel + StateFlow** - Move state out of composables
- [ ] **Unidirectional data flow** - Events → ViewModel → UI State
- [ ] **Persistent storage** - Room/Proto DataStore for formulas, preferences

### 5.2 Modularization
```
app/
├── core:common          # Data models (Symbol, Formula, Combination)
├── core:formula-engine  # Combination logic, dimensional analysis
├── feature:palette      # Palette UI, symbol registry
├── feature:canvas       # Canvas, drag-drop, placement
├── feature:formulas     # Formula display, history, export
└── feature:learn        # Tutorials, challenges, progress
```

### 5.3 Testing
- [ ] **Unit tests** - Formula engine, combination logic, dimensional analysis
- [ ] **Compose UI tests** - Drag-drop, palette interaction, formula display
- [ ] **Screenshot tests** - Golden images for all screen sizes/themes
- [ ] **Integration tests** - End-to-end formula discovery flows

### 5.4 CI/CD
- [ ] **GitHub Actions** - Build, lint, test on every PR
- [ ] **Firebase App Distribution** - Auto-deploy debug builds
- [ ] **Play Console** - Internal testing track for releases

---

## 📋 **PHASE 6: Performance & Polish**

### 6.1 Performance
- [ ] **Lazy layout** for palette (large symbol sets)
- [ ] **Recomposition optimization** - `derivedStateOf`, `remember`, stable keys
- [ ] **Canvas rendering** - Consider `androidx.compose.ui.graphics.Canvas` for 100+ symbols
- [ ] **Memory leaks** - LeakCanary in debug

### 6.2 App Quality
- [ ] **Splash screen** - Animated physics symbol
- [ ] **App shortcuts** - "New Canvas", "Recent Formulas"
- [ ] **Widgets** - "Formula of the Day" home screen widget
- [ ] **Shortcuts.xml** - App Actions for Assistant ("Hey Google, open Physics Whiteboard")

---

## 🛠 **Suggested AI Workflow**

```markdown
## For each session, ask the AI to:

1. **Analyze current state** - "Read these files and tell me what's broken"
2. **Fix one category** - "Fix all drag-drop compilation errors"
3. **Verify** - "Build and run tests"
4. **Iterate** - "Now implement the formula history panel"
5. **Review** - "Show me the diff and explain architecture decisions"
```

---

## 📁 **Key Files to Share with AI**

```
app/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/example/physicswhiteboard/
│   │   ├── MainActivity.kt          # Main entry, composables
│   │   ├── model/                   # NEW: Symbol, Formula, Combination
│   │   ├── viewmodel/               # NEW: PhysicsWhiteboardViewModel
│   │   ├── ui/
│   │   │   ├── palette/             # NEW: Palette composables
│   │   │   ├── canvas/              # NEW: Canvas, drag-drop
│   │   │   ├── formulas/            # NEW: Formula display/history
│   │   │   └── theme/               # Theme, Color, Type
│   │   └── util/                    # Extensions, constants
│   └── res/
│       ├── values/themes.xml
│       ├── values/colors.xml
│       ├── values/strings.xml
│       ├── mipmap-*/ic_launcher.xml
│       └── xml/backup_rules.xml
```

---

## 🎯 **Quick Wins (Do First)**

1. ✅ Fix all compilation errors → working APK
2. ✅ Palette as bottom sheet (Material3 `ModalBottomSheet`)
3. ✅ Formula toast/bottom sheet on combination
4. ✅ Persist discovered formulas (DataStore)
5. ✅ Adaptive layout for tablet
6. ✅ Unit tests for combination logic

---

## 💡 **Creative Ideas to Explore**

| Idea | Description |
|------|-------------|
| **AR Mode** | Place symbols on real surfaces via ARCore |
| **Collaborative** | Real-time multi-user canvas (Firebase Realtime DB) |
| **AI Tutor** | Gemini integration: "Explain this formula" |
| **Equation Keyboard** | Custom keyboard for typing formulas |
| **Physics Engine** | Simulate projectile motion, oscillations |
| **Quiz Generator** | Auto-generate problems from discovered formulas |

---

## ✅ **Definition of Done**

- [ ] Clean build (`./gradlew assembleDebug` succeeds)
- [ ] Installs & launches on phone + tablet
- [ ] Drag-drop works smoothly (60fps)
- [ ] All 10 base symbols combinable → 15+ formulas
- [ ] Formulas persist across app restarts
- [ ] No crashes in 10-min monkey test
- [ ] Accessibility scanner passes
- [ ] Screenshot tests pass on 3 device configs

---

*Use this as a living document. Check off items, add notes, and share progress with your AI assistant.*