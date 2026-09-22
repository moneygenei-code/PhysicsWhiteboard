# Physics Whiteboard — Upgrade and Improvement Plan

## Goal

Make symbol fusion feel deliberate, make mass symbols behave like physical objects, and keep the electric-field visuals and physics region synchronized. The first release of this plan focuses on the three reported problems:

1. `m + v` and `m + a` visually interpenetrate during/after fusion.
2. A mass symbol created from the palette does not fall under gravity.
3. The `E` arrows rotate, but the dashed field-box outline stays axis-aligned.

The implementation should preserve the parchment/whiteboard interaction model while making the simulation deterministic and easier to extend.

---

## Current diagnosis

### 1. Fusion overlap: P0

Relevant code:

- `MainActivity.kt`, drag handling and `onDragEnd`
- `PhysicsSimulationEngine.kt`, `tryFuseSymbols`, `fuseInto`, and `handleCollisions`
- `FormulaTypesetting.kt`, the linear fallback renderer

There are several independent causes:

- A dragged body is still part of the normal simulation/collision loop. It can be pushed by `handleCollisions` while the pointer is moving it.
- Fusion is accepted at a fixed distance of `< 60f`, while collision separation uses a fixed `48f` radius. Neither value knows the actual rendered width/height of a formula.
- `handleCollisions` only handles `dist in 1f..minDist`; two bodies at exactly the same position are never separated.
- `fuseInto` moves the result to the midpoint and removes the second body, but there is no snap/settle state that prevents the newly created formula from immediately being treated as an overlapping pair.
- The fallback typesetter removes `"F = "` and `"p = "`, then lays out only the component glyphs. For `m + a` and `m + v`, the glyphs are drawn close together at 36sp, so the result can look like two symbols pushed into one another rather than a complete formula.
- Fusion metadata is only partially merged. Gravity and velocity are copied, but thrust, friction, charge, and other dynamic properties are not consistently carried to the resulting body.

### 2. Missing gravity on `m`: P0

`createLetterBody("m", ...)` sets `mass = 1.0f`, but it does not set `hasGravity`. The free-sandbox preset compensates with a one-off `m.hasGravity = true`, so an `m` spawned from the palette behaves differently from the initial `m`.

`fuseInto` also only ORs the existing `hasGravity` flags. If both input symbols were created without gravity, the fused body remains weightless. The split path creates fresh symbols, so it must also use the same canonical physical defaults.

There is a product decision here: in real physics, `m` is a mass parameter, not automatically a separate object. For this interactive whiteboard, the requested behavior indicates that a visible `m` should be treated as a dynamic mass token. That behavior should be made explicit and tested rather than relying on the free-sandbox special case.

### 3. Electric-field outline does not follow the arrows: P0

Relevant code:

- `MainActivity.kt`, `drawElectricFieldBox`
- `PhysicsSimulationEngine.kt`, the electric-field containment check

The arrows use `fieldAngle` to calculate their direction. The outline is drawn with an axis-aligned `drawRect`, so it never rotates. The physics check also uses an axis-aligned square:

```kotlin
abs(body.x - ef.x) <= ef.fieldRadius &&
abs(body.y - ef.y) <= ef.fieldRadius
```

Consequently, rotating the handle changes the arrows but not the visual boundary, and the visible field area and simulated field area can disagree.

---

## Target behavior and acceptance criteria

### Fusion

- A body being dragged is not pushed by simulation collisions.
- When `m` is brought near `v` or `a`, a clear preview/snap state shows the valid fusion target.
- On release, the two inputs become exactly one body centered at the intended midpoint.
- The result visibly renders the complete formula: `p = m·v` or `F = m·a`.
- Formula glyphs have a readable minimum gap and do not overlap at normal canvas density.
- The fused body does not receive a second collision correction from the removed input.
- A dropped invalid combination remains separate and can be picked up again.
- Double-tapping a fused formula still splits it into the original symbols.

### Gravity

- A newly spawned `m` accelerates downward, collides with the ground, and settles/bounces according to the existing visual scale.
- `m + v` and `m + a` continue to have gravity after fusion.
- A split formula produces symbols with the same canonical defaults as palette-created symbols.
- The free-sandbox scene no longer needs a special `m.hasGravity = true` override.

### Electric field

- Rotating `E` rotates the dashed boundary, arrow lines, arrowheads, and rotation handle around the same center.
- Moving `E` translates the complete field graphic without leaving the outline behind.
- The simulated field membership matches the rotated visible rectangle.
- A square/quarter-turn edge case is covered; use a visibly rectangular field region so a 90-degree rotation is distinguishable.

---

## Implementation plan

### Phase 0 — Reproduce and instrument

1. Add a small debug-only state or logging hook for:
   - body id, component symbols, position, velocity, `hasGravity`;
   - fusion distance and rendered bounds;
   - electric-field local coordinates and inside/outside result.
2. Reproduce each report from a clean app state:
   - palette `m` + `v`;
   - palette `m` + `a`;
   - palette `m` left untouched for at least 0.5 seconds;
   - rotate `E` to 45 and 90 degrees, then move it.
3. Keep the debug hook removable; do not make production behavior depend on log timing.

### Phase 1 — Make fusion geometry and interaction robust

**A. Introduce one geometry source of truth**

Create a small geometry helper (for example `BodyGeometry.kt`) that supplies:

- visual bounds for a single symbol;
- visual bounds for a `RenderedExpression` using its `width` and `height`;
- padded hit bounds;
- fusion bounds;
- a stable fallback collision normal for zero-distance bodies.

Use it in pointer hit testing, fusion detection, and collision separation. Do not keep separate magic values of `40f`, `48f`, and `60f` for the same visual object.

**B. Lock dragged bodies out of simulation corrections**

Add an interaction state such as `draggedBodyId`/`isBeingDragged` to the engine or pass an exclusion set into `step`. While a body is being dragged:

- do not integrate its velocity;
- do not apply gravity/fields to it;
- do not include it in collision impulses;
- let the pointer own its position.

On release, calculate a deliberate release velocity, or reset it to zero for a whiteboard-like placement. This prevents a release from producing a large accidental impulse.

**C. Use a fusion preview and snap**

During drag, find a compatible target rather than only checking proximity at the end. When the target is within the fusion radius:

- show a subtle highlight/halo;
- snap the dragged symbol to a stable point near the target, without allowing the two glyph bounds to interpenetrate;
- on release, call fusion once and center the resulting expression.

If a preview is not desired in the first pass, at minimum compute the midpoint from the original target and dragged positions, assign it once, remove the target, and skip the next collision pass for that result.

**D. Fix zero-distance collision handling**

Change `handleCollisions` to handle `dist < minDist`, including `dist == 0`. Use a deterministic normal derived from the pair ids or a fixed `(1, 0)` fallback. This is important even after fusion changes because fast pointer movement can put two bodies at the same coordinates.

**E. Render complete base formulas**

Add explicit typesetter layouts for at least:

- `F = m·a`
- `p = m·v`

Do not strip the left-hand side in the fallback renderer. A general fallback should tokenize the full formula and use measured/known glyph widths plus a minimum spacing value. The existing `RenderedExpression.width` and `height` should be used by the interaction geometry.

**F. Merge dynamic properties intentionally**

Replace the partial flag copying in `fuseInto` with a documented merge policy. At minimum define behavior for:

- `hasGravity`;
- `hasThrust` and `thrustAngle`;
- `hasVelocity` and release velocity;
- friction;
- charge and mass;
- rod/field-source roles, which should not be silently inherited by a formula unless the formula rule explicitly says so.

Use canonical formula definitions rather than deriving physical behavior from whichever symbol happened to be dragged first. Preserve a canonical `componentChars` order so split results are stable regardless of drag direction.

### Phase 2 — Make gravity a first-class body property

1. Add shared simulation constants, for example `PhysicsConstants.gravityAcceleration`, instead of embedding `2200f` in `step`.
2. Set `hasGravity = true` in the canonical `m` creation path. Remove the free-sandbox-only assignment once the default is correct.
3. Ensure `fuseInto` sets gravity when the component set contains `m`, and ensure `splitFormula` goes through the same `createLetterBody` defaults.
4. Decide and document whether `g` is:
   - a gravity toggle/field token; or
   - a symbolic value that only participates in formulas.

   For this release, keep `g` as the gravity token and make `m` the dynamic mass token, unless the product owner wants gravity to require an explicit `g` attachment.
5. Add a `gravityScale` or capability model later if different bodies need to respond differently; do not overload `mass` to mean both visual size and gravity enablement.

### Phase 3 — Rotate the electric field as one object

1. Give an electric field a rectangular local geometry, such as half-length and half-width, instead of treating `fieldRadius` as an axis-aligned square for every purpose.
2. Add shared coordinate helpers:
   - world point to field-local point by rotating by `-fieldAngle`;
   - field-local point back to world coordinates by rotating by `fieldAngle`;
   - `containsElectricFieldPoint` using local rectangle bounds.
3. Update `drawElectricFieldBox` to draw the four rotated corners (or draw inside a `rotate` transform). Use the same local dimensions and angle for the arrows, boundary, and handle.
4. Update the electric-force test in `PhysicsSimulationEngine.step` to use the rotated local bounds. This avoids a visual field rotated one way while the force remains in an old axis-aligned square.
5. Add tests for angle `0`, `PI / 4`, and `PI / 2`, including a point that is inside the rotated rectangle but outside the old axis-aligned check.
6. Keep the field vector direction convention explicit: screen `+Y` is downward, so the visual arrow direction and electric acceleration must use the same sign convention.

### Phase 4 — Regression coverage and cleanup

Add tests in `PhysicsSimulationTest.kt` for:

- palette-created `m` moves downward after several steps;
- fused `m + v` retains gravity;
- fused `m + a` retains gravity and has readable component metadata;
- `m + v` and `m + a` create one centered body;
- complete formula text is present in the resulting `RenderedExpression`;
- overlapping bodies at identical coordinates are separated;
- dragged-body exclusion prevents collision impulses during a drag/release;
- electric-field containment at multiple rotations;
- electric acceleration follows the field angle for positive and negative charges.

Add Compose/UI tests where feasible for:

- dragging `m` onto `v` and `a`;
- double-tap split after each fusion;
- rotating and moving `E`.

Run the existing suite plus the new tests with `./gradlew testDebugUnitTest` and build an APK with `./gradlew assembleDebug` before release. The current checkout also has a non-executable `gradlew` and requires a configured JDK; make the wrapper executable or invoke it with `bash ./gradlew`, and document the JDK 17 prerequisite in setup instructions.

---

## Recommended file changes

| File | Planned changes |
| --- | --- |
| `app/src/main/java/com/example/physicswhiteboard/PhysicsSimulationEngine.kt` | Canonical body defaults, gravity constants, drag exclusion, collision zero-distance handling, fusion merge policy, rotated-field containment |
| `app/src/main/java/com/example/physicswhiteboard/MainActivity.kt` | Fusion preview/drag state, geometry-aware hit testing, rotated field drawing, consistent release behavior |
| `app/src/main/java/com/example/physicswhiteboard/FormulaTypesetting.kt` | Full `F = m·a` and `p = m·v` layouts, non-overlapping fallback, reliable expression bounds |
| `app/src/test/java/com/example/physicswhiteboard/PhysicsSimulationTest.kt` | Regression tests for all three reported bugs and collision/fusion edge cases |
| `app/src/main/java/com/example/physicswhiteboard/BodyGeometry.kt` *(new)* | Shared visual/hit/fusion bounds and rotated rectangle helpers |
| `app/src/test/java/com/example/physicswhiteboard/BodyGeometryTest.kt` *(new)* | Pure geometry tests without Compose rendering |
| `README.md` | Document interaction rules, gravity behavior, field rotation, and JDK/wrapper setup |

There are currently two formula systems: `PhysicsSimulationEngine`/`SimBody` and the separate `FormulaEngine.kt`/`BoardItem` model. They should either be consolidated or given shared formula definitions. Otherwise a formula can be valid in one engine and unavailable or differently titled in the other. This is a follow-up architecture task, but the new fusion tests should make the active engine's behavior authoritative first.

---

## Delivery order

1. **P0:** Fix complete formula rendering, dragged-body exclusion, midpoint snapping, and zero-distance collision handling.
2. **P0:** Make palette-created and fused `m` bodies gravitational.
3. **P0:** Rotate the electric boundary and use the same rotated geometry for force membership.
4. **P1:** Add regression/UI tests and remove one-off scene behavior.
5. **P1:** Consolidate the two formula engines and replace remaining magic geometry values.
6. **P2:** Add undo/redo for fusion and split, a reset button, and an optional physics pause/step control for studying.

## Definition of done

The upgrade is complete when the three acceptance sections above pass in automated tests where possible and in a manual APK check on a phone/tablet. In particular, a user must be able to spawn `m`, leave it alone and see it fall; combine it with `v` or `a` and get a readable, single centered formula; and rotate `E` while seeing the boundary, arrows, and simulated force region stay aligned.
