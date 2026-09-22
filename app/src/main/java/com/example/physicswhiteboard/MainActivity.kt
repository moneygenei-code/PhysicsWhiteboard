package com.example.physicswhiteboard

import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Theme Colors for Parchment Paper & Deep Ink
val ParchmentBackground = Color(0xFFF4F1EA)
val InkColor = Color(0xFF26221C)
val InkSecondary = Color(0x9926221C)
val InkFaint = Color(0x3326221C)
val WireframePanelBg = Color(0x80FFFFFF)
val WireframeTileBg = Color(0x73FFFFFF)

val DOCK_CHARS = listOf(
    listOf("m", "M", "g", "a"),
    listOf("v", "r", "½", "μ"),
    listOf("c", "G", "t", "B"),
    listOf("E", "q", "I", "e")
)

/**
 * Small bodies win over big field regions, so e.g. the Wien E-field can
 * still be grabbed from under the B-field circle. Stable for equal sizes,
 * so the most recently added body stays on top.
 */
private fun findBodyAt(bodies: List<SimBody>, x: Float, y: Float): SimBody? {
    return bodies.asReversed()
        .sortedBy { BodyGeometry.collisionRadius(it) }
        .firstOrNull { BodyGeometry.containsPoint(it, x, y) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PhysicsSandboxApp()
            }
        }
    }
}

@Composable
fun PhysicsSandboxApp() {
    val simEngine = remember { PhysicsSimulationEngine() }

    var widthPx by remember { mutableFloatStateOf(1600f) }
    var heightPx by remember { mutableFloatStateOf(1000f) }

    var activeScene by remember { mutableStateOf(simEngine.activeScene) }
    var showLernblattDrawer by remember { mutableStateOf(false) }
    var showSceneDropdown by remember { mutableStateOf(false) }

    // Grab & drag state. The simulation owns the drag lock so physics cannot
    // push a body while the pointer is positioning it.
    var draggedBodyId by remember { mutableStateOf<String?>(null) }
    var pointerPos by remember { mutableStateOf(Offset.Zero) }
    var dragGrabOffset by remember { mutableStateOf(Offset.Zero) }
    var rotateTargetBodyId by remember { mutableStateOf<String?>(null) }
    var fusionTargetId by remember { mutableStateOf<String?>(null) }
    var paused by remember { mutableStateOf(simEngine.isPaused) }
    var fusionMode by remember { mutableStateOf(simEngine.fusionMode) }

    // Drop zone for the trash can (bottom-right corner), sized to its visuals.
    val trashDropPx = with(LocalDensity.current) { 80.dp.toPx() }

    // Drag session shared by the canvas and palette drag-in gestures.
    val dragSamples = remember { mutableListOf<Pair<Long, Offset>>() }
    var paletteDragActive by remember { mutableStateOf(false) }
    var paletteDragDistance by remember { mutableFloatStateOf(0f) }

    // Apparatus sliders need Compose state; the engine only stores values.
    var ubSlider by remember { mutableFloatStateOf(simEngine.expVoltageUb) }
    var isSlider by remember { mutableFloatStateOf(simEngine.expCurrentIs) }

    var frameTrigger by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    val textPaint = remember {
        android.graphics.Paint().apply {
            color = InkColor.toArgb()
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    fun recordDragSample() {
        val now = System.nanoTime()
        dragSamples.add(now to pointerPos)
        val cutoff = now - 120_000_000L
        while (dragSamples.size > 2 && dragSamples.first().first < cutoff) {
            dragSamples.removeAt(0)
        }
    }

    // Throw/fling: release velocity from the last ~120ms of pointer movement.
    fun consumeReleaseVelocity(): Offset {
        if (dragSamples.size < 2) return Offset.Zero
        val first = dragSamples.first()
        val last = dragSamples.last()
        val dtSeconds = (last.first - first.first) / 1_000_000_000f
        if (dtSeconds < 0.016f) return Offset.Zero
        val vx = (last.second.x - first.second.x) / dtSeconds
        val vy = (last.second.y - first.second.y) / dtSeconds
        return Offset(vx.coerceIn(-2600f, 2600f), vy.coerceIn(-2600f, 2600f))
    }

    fun moveActiveDraggedBody() {
        val draggedBody = simEngine.bodies.firstOrNull { it.id == draggedBodyId } ?: return
        simEngine.moveDraggedBody(
            draggedBody,
            pointerPos.x - dragGrabOffset.x,
            pointerPos.y - dragGrabOffset.y
        )
        recordDragSample()
        val target = simEngine.findFusionTarget(draggedBody)
        // Normal mode fuses on deep overlap; steering the symbol away would
        // push it out of the tight fusion range again.
        if (target != null && simEngine.fusionMode == FusionMode.PHYSIK) {
            simEngine.snapDraggedBodyOutsideTarget(draggedBody, target)
        }
        fusionTargetId = target?.id
    }

    fun finishActiveDrag() {
        val draggedBody = simEngine.bodies.firstOrNull { it.id == draggedBodyId }
        if (draggedBody != null) {
            if (paletteDragActive && paletteDragDistance < 12f) {
                // Long-press without movement: the tap already spawned the symbol.
                simEngine.bodies.removeAll { it.id == draggedBody.id }
                simEngine.cancelDrag(draggedBody)
            } else if (draggedBody.x > widthPx - trashDropPx && draggedBody.y > heightPx - trashDropPx) {
                simEngine.bodies.removeAll { it.id == draggedBody.id }
                simEngine.cancelDrag(draggedBody)
            } else {
                val target = fusionTargetId
                    ?.let { targetId -> simEngine.bodies.firstOrNull { it.id == targetId } }
                    ?: simEngine.findFusionTarget(draggedBody)
                if (target != null) {
                    if (!simEngine.tryFuseSymbols(draggedBody, target)) {
                        simEngine.endDrag(draggedBody)
                    }
                } else {
                    val release = consumeReleaseVelocity()
                    simEngine.endDrag(draggedBody, release.x, release.y)
                }
            }
        }

        simEngine.bodies.firstOrNull { it.id == rotateTargetBodyId }?.let { simEngine.endDrag(it) }
        draggedBodyId = null
        rotateTargetBodyId = null
        fusionTargetId = null
        paletteDragActive = false
        dragSamples.clear()
    }

    fun cancelActiveDrag() {
        simEngine.bodies.firstOrNull { it.id == draggedBodyId }?.let { simEngine.cancelDrag(it) }
        simEngine.bodies.firstOrNull { it.id == rotateTargetBodyId }?.let { simEngine.cancelDrag(it) }
        draggedBodyId = null
        rotateTargetBodyId = null
        fusionTargetId = null
        paletteDragActive = false
        dragSamples.clear()
    }

    fun beginPaletteDrag(char: String, canvasPos: Offset) {
        if (draggedBodyId != null || rotateTargetBodyId != null) return
        simEngine.saveUndoPoint()
        val body = simEngine.createLetterBody(
            char,
            canvasPos.x.coerceIn(0f, widthPx),
            canvasPos.y.coerceIn(0f, heightPx)
        )
        simEngine.bodies.add(body)
        draggedBodyId = body.id
        dragGrabOffset = Offset.Zero
        pointerPos = Offset(body.x, body.y)
        fusionTargetId = null
        paletteDragActive = true
        paletteDragDistance = 0f
        dragSamples.clear()
        recordDragSample()
        simEngine.beginDrag(body)
    }

    // Real-time 60/120 FPS physics ticker
    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastTime != 0L) {
                    val dt = min(0.033f, max(0.004f, (now - lastTime) / 1_000_000_000f))
                    simEngine.step(dt)
                    frameTrigger = now
                }
                lastTime = now
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ParchmentBackground)
            .onGloballyPositioned { coordinates ->
                widthPx = coordinates.size.width.toFloat()
                heightPx = coordinates.size.height.toFloat()
                simEngine.canvasWidth = widthPx
                simEngine.canvasHeight = heightPx
                simEngine.groundY = heightPx * 0.80f
            }
    ) {
        // Main Canvas Layer (Ground line, E/B fields, beams, mathematical formulas)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            val hit = findBodyAt(simEngine.bodies, tapOffset.x, tapOffset.y)
                            if (hit != null && (hit.componentChars.size > 1 || hit.isFusedCurrent)) {
                                simEngine.saveUndoPoint()
                                simEngine.splitFormula(hit)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            pointerPos = startOffset

                            val rotateHit = simEngine.bodies.asReversed().firstOrNull { body ->
                                BodyGeometry.isOnRotationHandle(body, startOffset.x, startOffset.y)
                            }
                            if (rotateHit != null) {
                                simEngine.saveUndoPoint()
                                rotateTargetBodyId = rotateHit.id
                                simEngine.beginDrag(rotateHit)
                                return@detectDragGestures
                            }

                            val bodyHit = findBodyAt(simEngine.bodies, startOffset.x, startOffset.y)
                            if (bodyHit != null) {
                                draggedBodyId = bodyHit.id
                                dragGrabOffset = Offset(
                                    startOffset.x - bodyHit.x,
                                    startOffset.y - bodyHit.y
                                )
                                fusionTargetId = null
                                paletteDragActive = false
                                paletteDragDistance = 0f
                                dragSamples.clear()
                                recordDragSample()
                                simEngine.saveUndoPoint()
                                simEngine.beginDrag(bodyHit)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            pointerPos += dragAmount

                            val rotBody = simEngine.bodies.firstOrNull { it.id == rotateTargetBodyId }
                            if (rotBody != null) {
                                val dx = pointerPos.x - rotBody.x
                                val dy = pointerPos.y - rotBody.y
                                // The handle is positioned at (sin(theta), -cos(theta)).
                                val angle = atan2(dx, -dy)
                                if (rotBody.isFieldSource && rotBody.fieldType == FieldType.ELECTRIC_E) {
                                    rotBody.fieldAngle = angle
                                } else if (rotBody.isRod) {
                                    rotBody.rodAngle = angle
                                } else {
                                    rotBody.thrustAngle = angle
                                }
                                return@detectDragGestures
                            }

                            moveActiveDraggedBody()
                        },
                        onDragEnd = {
                            finishActiveDrag()
                        },
                        onDragCancel = {
                            cancelActiveDrag()
                        }
                    )
                }
        ) {
            // Read frameTrigger so Compose registers dependency and continuously invalidates/draws at screen refresh rate
            val _t = frameTrigger
            val canvasNative = drawContext.canvas.nativeCanvas

            // 1. Draw minimal Ground Line
            drawLine(
                color = InkColor.copy(alpha = 0.65f),
                start = Offset(0f, simEngine.groundY),
                end = Offset(size.width, simEngine.groundY),
                strokeWidth = 2.dp.toPx()
            )

            // 2. Draw Electric Field Box (E) with Parallel Arrows
            val eFields = simEngine.bodies.filter { it.isFieldSource && it.fieldType == FieldType.ELECTRIC_E }
            for (ef in eFields) {
                drawElectricFieldBox(ef)
            }

            // 3. Draw Magnetic Field Region (B) with Dots
            val bFields = simEngine.bodies.filter { it.isFieldSource && it.fieldType == FieldType.MAGNETIC_B }
            for (bf in bFields) {
                drawMagneticFieldCircle(bf)
            }

            // Fusion target feedback: the highlighted body is the only valid
            // formula target under the dragged symbol.
            val fusionTarget = fusionTargetId
                ?.let { targetId -> simEngine.bodies.firstOrNull { it.id == targetId } }
            if (fusionTarget != null) {
                drawCircle(
                    color = Color(0xFF8A6D3B).copy(alpha = 0.18f),
                    radius = BodyGeometry.collisionRadius(fusionTarget) + 10f,
                    center = Offset(fusionTarget.x, fusionTarget.y),
                    style = Stroke(width = 2.5f)
                )
            }

            // 4. Draw Electron Beam Glow Trails
            for (pt in simEngine.electronBeam) {
                drawCircle(
                    color = Color(0xFF2E7D32).copy(alpha = pt.alpha * 0.45f),
                    radius = 3.5f,
                    center = Offset(pt.x, pt.y)
                )
            }

            // 5. Draw T-Rod Planks
            val rods = simEngine.bodies.filter { it.isRod }
            for (rod in rods) {
                val cosR = cos(rod.rodAngle)
                val sinR = sin(rod.rodAngle)
                val hLen = rod.rodLength / 2f
                drawLine(
                    color = InkColor,
                    start = Offset(rod.x - cosR * hLen, rod.y - sinR * hLen),
                    end = Offset(rod.x + cosR * hLen, rod.y + sinR * hLen),
                    strokeWidth = 3.5f
                )
            }

            // 6. Draw Mathematical Formulas and Symbols
            for (b in simEngine.bodies) {
                val expr = b.renderedExpr ?: FormulaTypesetter.buildExpression(b.char, b.char)

                // Draw fraction bars
                for (bar in expr.fractionBars) {
                    drawLine(
                        color = InkColor,
                        start = Offset(b.x + bar.startX, b.y + bar.y),
                        end = Offset(b.x + bar.endX, b.y + bar.y),
                        strokeWidth = bar.strokeWidth
                    )
                }

                // Draw math glyphs
                for (glyph in expr.glyphs) {
                    textPaint.textSize = glyph.fontSizeSp.sp.toPx()
                    canvasNative.drawText(
                        glyph.text,
                        b.x + glyph.localX,
                        b.y + glyph.localY + (glyph.fontSizeSp * 0.35f),
                        textPaint
                    )
                }

                // Draw rotation handle from the same geometry used by hit testing.
                val handle = BodyGeometry.rotationHandle(b)
                if (handle != null) {
                    val handleOffset = Offset(handle.x, handle.y)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f),
                        radius = 12f,
                        center = handleOffset
                    )
                    drawCircle(
                        color = InkColor.copy(alpha = 0.5f),
                        radius = 12f,
                        center = handleOffset,
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }

        // Top Header: Minimal Title, Preset Switcher, and Klausur Guide
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Physik 12 · Klausurvorbereitung",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = InkColor
                )
                Text(
                    text = if (fusionMode == FusionMode.NORMAL) {
                        "${activeScene.chapterRef} · Normal: m+v → mv"
                    } else {
                        activeScene.chapterRef
                    },
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    color = InkSecondary
                )
            }

            // Scrolls internally on narrow phones instead of pushing the title off-screen.
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    paused = !paused
                    simEngine.setPaused(paused)
                }) {
                    Text(
                        text = if (paused) "▶ Weiter" else "Ⅱ Pause",
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        color = InkColor
                    )
                }
                TextButton(onClick = {
                    if (simEngine.undo()) frameTrigger = System.nanoTime()
                }) {
                    Text(text = "↶", fontSize = 20.sp, color = InkColor)
                }
                TextButton(onClick = {
                    if (simEngine.redo()) frameTrigger = System.nanoTime()
                }) {
                    Text(text = "↷", fontSize = 20.sp, color = InkColor)
                }
                TextButton(onClick = {
                    simEngine.loadScene(activeScene)
                    paused = false
                }) {
                    Text(
                        text = "Reset",
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        color = InkColor
                    )
                }
                TextButton(onClick = {
                    fusionMode = if (fusionMode == FusionMode.PHYSIK) FusionMode.NORMAL else FusionMode.PHYSIK
                    simEngine.fusionMode = fusionMode
                }) {
                    Text(
                        text = if (fusionMode == FusionMode.PHYSIK) "∑ Physik" else "abc Normal",
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        color = InkColor
                    )
                }

                // Scene Selector Button
                Box {
                    Surface(
                        color = WireframePanelBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, InkFaint),
                        modifier = Modifier.clickable { showSceneDropdown = true }
                    ) {
                        Text(
                            text = "Gerät: ${activeScene.displayName}",
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            color = InkColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSceneDropdown,
                        onDismissRequest = { showSceneDropdown = false }
                    ) {
                        ApparatusScene.entries.forEach { scene ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = scene.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(text = scene.chapterRef, fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    activeScene = scene
                                    simEngine.loadScene(scene)
                                    paused = false
                                    showSceneDropdown = false
                                }
                            )
                        }
                    }
                }

                // Klausur Lernblatt Drawer Button
                Surface(
                    color = InkColor,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { showLernblattDrawer = true }
                ) {
                    Text(
                        text = "📖 Lernblatt & Selbsttest",
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        color = ParchmentBackground,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Top-Right Floating 4x4 Palette Dock (matching reference)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 24.dp)
                .background(WireframePanelBg, RoundedCornerShape(14.dp))
                .border(1.dp, InkFaint, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in DOCK_CHARS) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (ch in row) {
                            if (ch.isNotEmpty()) {
                                PaletteTile(
                                    ch = ch,
                                    onTapSpawn = {
                                        simEngine.saveUndoPoint()
                                        // Spawn directly near center of canvas
                                        val newBody = simEngine.createLetterBody(
                                            ch,
                                            widthPx * 0.45f + kotlin.random.Random.nextInt(-50, 50),
                                            heightPx * 0.45f + kotlin.random.Random.nextInt(-50, 50)
                                        )
                                        simEngine.bodies.add(newBody)
                                    },
                                    onDragStart = { canvasPos -> beginPaletteDrag(ch, canvasPos) },
                                    onDrag = { dragAmount ->
                                        pointerPos += dragAmount
                                        paletteDragDistance += dragAmount.getDistance()
                                        moveActiveDraggedBody()
                                    },
                                    onDragEnd = { finishActiveDrag() },
                                    onDragCancel = { cancelActiveDrag() }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(44.dp))
                            }
                        }
                    }
                }
            }
        }

        // Bottom-Right Minimal Wireframe Trash Can
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(48.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (simEngine.bodies.isNotEmpty()) {
                                simEngine.saveUndoPoint()
                                simEngine.bodies.clear()
                                simEngine.electronBeam.clear()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8f
                val color = InkColor.copy(alpha = 0.5f)

                // Lid
                drawLine(color, Offset(8f, 12f), Offset(40f, 12f), strokeW)
                drawLine(color, Offset(20f, 8f), Offset(28f, 8f), strokeW)

                // Body
                val path = Path().apply {
                    moveTo(12f, 12f)
                    lineTo(15f, 40f)
                    lineTo(33f, 40f)
                    lineTo(36f, 12f)
                }
                drawPath(path, color, style = Stroke(width = strokeW))

                // Vertical lines
                drawLine(color, Offset(20f, 18f), Offset(21f, 34f), strokeW)
                drawLine(color, Offset(28f, 18f), Offset(27f, 34f), strokeW)
            }
        }

        // Interactive Live Sliders for Apparatus Experiments (Fadenstrahlrohr / Wien)
        if (activeScene == ApparatusScene.FADENSTRAHLROHR) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .background(WireframePanelBg, RoundedCornerShape(12.dp))
                    .border(1.dp, InkFaint, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.width(260.dp)) {
                    Text(
                        text = "Fadenstrahlrohr Regler",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = InkColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Spannung UB = ${ubSlider.toInt()} V",
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        color = InkSecondary
                    )
                    Slider(
                        value = ubSlider,
                        onValueChange = {
                            ubSlider = it
                            simEngine.expVoltageUb = it
                            // Adjust emitted electron speed
                            simEngine.bodies
                                .filter { b -> b.char == "e" }
                                .forEach { e -> e.vy = -sqrt(it / 300f) * 380f }
                        },
                        valueRange = 100f..500f,
                        colors = SliderDefaults.colors(thumbColor = InkColor, activeTrackColor = InkColor)
                    )

                    Text(
                        text = "Spulenstrom IS = ${(isSlider * 100).toInt() / 100f} A",
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        color = InkSecondary
                    )
                    Slider(
                        value = isSlider,
                        onValueChange = {
                            isSlider = it
                            simEngine.expCurrentIs = it
                            // Adjust B-field (r = m·v/(|q|·B/100) sizing: 0.6 A
                            // gives the r ≈ 158px orbit inside the 240px field)
                            simEngine.bodies
                                .filter { it.fieldType == FieldType.MAGNETIC_B }
                                .forEach { b -> b.fieldMagnitude = it * 200f }
                        },
                        valueRange = 0.2f..1.2f,
                        colors = SliderDefaults.colors(thumbColor = InkColor, activeTrackColor = InkColor)
                    )
                }
            }
        }

        // Slide-out Klausur Lernblatt & Selbsttest Drawer
        AnimatedVisibility(
            visible = showLernblattDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            LernblattDrawer(
                onClose = { showLernblattDrawer = false },
                onLoadApparatus = { scene ->
                    activeScene = scene
                    simEngine.loadScene(scene)
                    paused = false
                    showLernblattDrawer = false
                }
            )
        }
    }
}

// A palette tile spawns on tap and can also be dragged onto the board with
// a long-press. The root coordinates match the full-size canvas coordinates.
@Composable
fun PaletteTile(
    ch: String,
    onTapSpawn: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var tileOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(WireframeTileBg, RoundedCornerShape(10.dp))
            .onGloballyPositioned { tileOrigin = it.positionInRoot() }
            .clickable { onTapSpawn() }
            .pointerInput(ch) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> onDragStart(tileOrigin + offset) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ch,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = InkColor
        )
    }
}

// Draw the electric field as one rotated object: boundary, arrows, and
// physics membership all use FieldGeometry's local rectangle.
fun DrawScope.drawElectricFieldBox(ef: SimBody) {
    val boundary = FieldGeometry.corners(ef).map { Offset(it.x, it.y) }
    val boundaryPath = Path().apply {
        moveTo(boundary[0].x, boundary[0].y)
        for (corner in boundary.drop(1)) lineTo(corner.x, corner.y)
        close()
    }
    drawPath(
        path = boundaryPath,
        color = InkColor.copy(alpha = 0.16f),
        style = Stroke(
            width = 1.4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f), 0f)
        )
    )

    val halfLength = FieldGeometry.halfLength(ef)
    val halfWidth = FieldGeometry.halfWidth(ef)
    val n = 7
    val arrowHeadLength = 10f
    val direction = ef.fieldAngle
    val dx = cos(direction)
    val dy = sin(direction)
    val arrowAngle = atan2(dy, dx)

    for (i in 0 until n) {
        val localOffset = (i - (n - 1) / 2f) * (2f * halfWidth / n)
        val start = FieldGeometry.localToWorld(ef, -halfLength + 10f, localOffset)
        val end = FieldGeometry.localToWorld(ef, halfLength - 10f, localOffset)
        val startOffset = Offset(start.x, start.y)
        val endOffset = Offset(end.x, end.y)

        drawLine(
            color = InkColor.copy(alpha = 0.18f),
            start = startOffset,
            end = endOffset,
            strokeWidth = 1.5f
        )

        val p1 = Offset(
            end.x - arrowHeadLength * cos(arrowAngle - 0.42f),
            end.y - arrowHeadLength * sin(arrowAngle - 0.42f)
        )
        val p2 = Offset(
            end.x - arrowHeadLength * cos(arrowAngle + 0.42f),
            end.y - arrowHeadLength * sin(arrowAngle + 0.42f)
        )
        drawLine(color = InkColor.copy(alpha = 0.22f), start = endOffset, end = p1, strokeWidth = 1.5f)
        drawLine(color = InkColor.copy(alpha = 0.22f), start = endOffset, end = p2, strokeWidth = 1.5f)
    }
}

// Draw the Magnetic Field Circle: dots (⊙) for out-of-page, crosses (⊗)
// for into-page, following bDirectionZ.
fun DrawScope.drawMagneticFieldCircle(bf: SimBody) {
    val r = bf.fieldRadius
    val cx = bf.x
    val cy = bf.y

    // Dotted field circle boundary
    drawCircle(
        color = InkColor.copy(alpha = 0.12f),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = 1.4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
    )

    // Field marker grid inside circle
    val step = 28f
    val crossHalf = 3.5f
    var gx = -r + 14f
    while (gx <= r - 14f) {
        var gy = -r + 14f
        while (gy <= r - 14f) {
            if (gx * gx + gy * gy <= (r - 12f) * (r - 12f)) {
                if (bf.bDirectionZ < 0) {
                    drawLine(
                        color = InkColor.copy(alpha = 0.20f),
                        start = Offset(cx + gx - crossHalf, cy + gy - crossHalf),
                        end = Offset(cx + gx + crossHalf, cy + gy + crossHalf),
                        strokeWidth = 1.6f
                    )
                    drawLine(
                        color = InkColor.copy(alpha = 0.20f),
                        start = Offset(cx + gx - crossHalf, cy + gy + crossHalf),
                        end = Offset(cx + gx + crossHalf, cy + gy - crossHalf),
                        strokeWidth = 1.6f
                    )
                } else {
                    drawCircle(
                        color = InkColor.copy(alpha = 0.20f),
                        radius = 2.0f,
                        center = Offset(cx + gx, cy + gy)
                    )
                }
            }
            gy += step
        }
        gx += step
    }
}

// Klausurvorbereitung Lernblatt & Selbsttest Drawer
@Composable
fun LernblattDrawer(
    onClose: () -> Unit,
    onLoadApparatus: (ApparatusScene) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("10 Kapitel", "Formeln", "Selbsttest", "15-Punkte")

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.94f)
            .widthIn(max = 520.dp),
        color = Color(0xFFFAF8F5),
        shadowElevation = 16.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, InkFaint)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Physik 12 · Lernblatt",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = InkColor
                    )
                    Text(
                        text = "Bewegung von Teilchen in Feldern (LEIFIphysik)",
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        color = InkSecondary
                    )
                }

                TextButton(onClick = onClose) {
                    Text(text = "Schließen", color = InkColor, fontFamily = FontFamily.Serif)
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFF1EFEA),
                contentColor = InkColor
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontFamily = FontFamily.Serif,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> ChaptersTab(onLoadApparatus)
                1 -> FormulasTab()
                2 -> SelfTestTab()
                3 -> StrategyTab()
            }
        }
    }
}

@Composable
fun ChaptersTab(onLoadApparatus: (ApparatusScene) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(KlausurCurriculum.CHAPTERS) { ch ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kapitel ${ch.number}: ${ch.title}",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = InkColor
                    )
                    Text(
                        text = ch.subtitle,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        color = InkSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Das Wichtigste:",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = InkColor
                    )
                    ch.summary.forEach { point ->
                        Text(
                            text = "• $point",
                            fontFamily = FontFamily.Serif,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kernformeln:",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = InkColor
                    )
                    ch.formulas.forEach { (form, exp) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9F7F3))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = form, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = exp, fontFamily = FontFamily.Serif, fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Typische Fehler: ${ch.traps.joinToString(" ")}",
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        color = Color(0xFFC62828)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "15-Punkte-Tipp: ${ch.tips15Points}",
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32)
                    )

                    // Shortcut buttons for apparatus scenes
                    when (ch.number) {
                        5 -> SceneLinkButton("E-Querfeld im Whiteboard öffnen") { onLoadApparatus(ApparatusScene.DEFLECTION_CAPACITOR) }
                        7 -> SceneLinkButton("Wien-Filter im Whiteboard öffnen") { onLoadApparatus(ApparatusScene.WIEN_FILTER) }
                        8 -> SceneLinkButton("Fadenstrahlrohr im Whiteboard öffnen") { onLoadApparatus(ApparatusScene.FADENSTRAHLROHR) }
                        9 -> SceneLinkButton("Massenspektrometer im Whiteboard öffnen") { onLoadApparatus(ApparatusScene.MASS_SPECTROMETER) }
                        10 -> SceneLinkButton("Hall-Effekt im Whiteboard öffnen") { onLoadApparatus(ApparatusScene.HALL_EFFECT) }
                    }
                }
            }
        }
    }
}

@Composable
fun SceneLinkButton(label: String, onClick: () -> Unit) {
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
        color = Color(0xFFEDE8E1),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = "⚡ $label",
            fontFamily = FontFamily.Serif,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun FormulasTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KlausurCurriculum.CHAPTERS.forEach { ch ->
            item {
                Text(
                    text = "Kapitel ${ch.number}: ${ch.title}",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = InkColor
                )
            }
            items(ch.formulas) { (formula, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formula,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = InkColor
                    )
                    Text(
                        text = desc,
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun SelfTestTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(KlausurCurriculum.SELF_TEST) { item ->
            var showSolution by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.id}: ${item.topic} (${item.points} P., AFB ${item.afb})",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = InkColor
                        )
                        Text(
                            text = if (showSolution) "▲ Lösung verbergen" else "▼ Lösung anzeigen",
                            fontFamily = FontFamily.Serif,
                            fontSize = 11.sp,
                            color = Color(0xFF1565C0),
                            modifier = Modifier.clickable { showSolution = !showSolution }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.question,
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    if (showSolution) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = InkFaint)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Musterlösung (15-Punkte-Stil):",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = item.solution,
                            fontFamily = FontFamily.Serif,
                            fontSize = 11.sp,
                            color = InkColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StrategyTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Klausurstrategie — Der Weg zu 15 Punkten",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = InkColor
        )
        Spacer(modifier = Modifier.height(10.dp))

        KlausurCurriculum.STRATEGY_15_POINTS.forEach { point ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = point,
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    color = InkColor,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

fun abs(v: Float): Float = Math.abs(v)
fun sqrt(v: Float): Float = Math.sqrt(v.toDouble()).toFloat()