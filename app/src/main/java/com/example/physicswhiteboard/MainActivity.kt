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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
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
    listOf("E", "q", "I", "")
)

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

    // Grab & drag state
    var draggedBodyId by remember { mutableStateOf<String?>(null) }
    var draggedDockChar by remember { mutableStateOf<String?>(null) }
    var pointerPos by remember { mutableStateOf(Offset.Zero) }
    var rotateTargetBodyId by remember { mutableStateOf<String?>(null) }

    var frameTrigger by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    val textPaint = remember {
        android.graphics.Paint().apply {
            color = InkColor.toArgb()
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
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
                            // Double tap on body splits it
                            val hit = simEngine.bodies.firstOrNull { b ->
                                !b.isFieldSource && hypot(b.x - tapOffset.x, b.y - tapOffset.y) < 40f
                            }
                            if (hit != null && hit.componentChars.size > 1) {
                                simEngine.splitFormula(hit)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            pointerPos = startOffset

                            // Check rotate handle on directional bodies
                            val rotateHit = simEngine.bodies.firstOrNull { b ->
                                if (b.hasThrust || (b.isFieldSource && b.fieldType == FieldType.ELECTRIC_E) || b.isRod) {
                                    val angle = if (b.isFieldSource) b.fieldAngle else if (b.isRod) b.rodAngle else b.thrustAngle
                                    val hx = b.x + sin(angle) * 44f
                                    val hy = b.y - cos(angle) * 44f
                                    hypot(startOffset.x - hx, startOffset.y - hy) < 28f
                                } else false
                            }

                            if (rotateHit != null) {
                                rotateTargetBodyId = rotateHit.id
                                return@detectDragGestures
                            }

                            // Check body hit
                            val bodyHit = simEngine.bodies.lastOrNull { b ->
                                val halfW = if (b.isFieldSource) b.fieldRadius else 40f
                                val halfH = if (b.isFieldSource) b.fieldRadius else 30f
                                abs(b.x - startOffset.x) < halfW && abs(b.y - startOffset.y) < halfH
                            }

                            if (bodyHit != null) {
                                draggedBodyId = bodyHit.id
                                bodyHit.vx = 0f
                                bodyHit.vy = 0f
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            pointerPos += dragAmount

                            val rotBody = simEngine.bodies.firstOrNull { it.id == rotateTargetBodyId }
                            if (rotBody != null) {
                                val dx = pointerPos.x - rotBody.x
                                val dy = pointerPos.y - rotBody.y
                                val angle = atan2(dy, dx)
                                if (rotBody.isFieldSource) rotBody.fieldAngle = angle
                                else if (rotBody.isRod) rotBody.rodAngle = angle
                                else rotBody.thrustAngle = angle
                                return@detectDragGestures
                            }

                            val draggedBody = simEngine.bodies.firstOrNull { it.id == draggedBodyId }
                            if (draggedBody != null) {
                                draggedBody.x += dragAmount.x
                                draggedBody.y += dragAmount.y
                                draggedBody.vx = dragAmount.x * 40f
                                draggedBody.vy = dragAmount.y * 40f
                            }
                        },
                        onDragEnd = {
                            val draggedBody = simEngine.bodies.firstOrNull { it.id == draggedBodyId }
                            if (draggedBody != null) {
                                // Check if dropped in trash zone (bottom right)
                                if (draggedBody.x > widthPx - 100f && draggedBody.y > heightPx - 100f) {
                                    simEngine.bodies.remove(draggedBody)
                                } else {
                                    // Check fusion with other bodies
                                    val other = simEngine.bodies.firstOrNull { o ->
                                        o.id != draggedBody.id && !o.isFieldSource &&
                                                hypot(draggedBody.x - o.x, draggedBody.y - o.y) < 60f
                                    }
                                    if (other != null) {
                                        simEngine.tryFuseSymbols(draggedBody, other)
                                    }
                                }
                            }

                            draggedBodyId = null
                            rotateTargetBodyId = null
                        },
                        onDragCancel = {
                            draggedBodyId = null
                            rotateTargetBodyId = null
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

                // Draw rotation handle if body has thrust or field angle
                if (b.hasThrust || (b.isFieldSource && b.fieldType == FieldType.ELECTRIC_E) || b.isRod) {
                    val angle = if (b.isFieldSource) b.fieldAngle else if (b.isRod) b.rodAngle else b.thrustAngle
                    val hx = b.x + sin(angle) * 44f
                    val hy = b.y - cos(angle) * 44f

                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f),
                        radius = 12f,
                        center = Offset(hx, hy)
                    )
                    drawCircle(
                        color = InkColor.copy(alpha = 0.5f),
                        radius = 12f,
                        center = Offset(hx, hy),
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
                    text = activeScene.chapterRef,
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    color = InkSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        ApparatusScene.values().forEach { scene ->
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
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(WireframeTileBg, RoundedCornerShape(10.dp))
                                        .clickable {
                                            // Spawn directly near center of canvas
                                            val newBody = simEngine.createLetterBody(
                                                ch,
                                                widthPx * 0.45f + kotlin.random.Random.nextInt(-50, 50),
                                                heightPx * 0.45f + kotlin.random.Random.nextInt(-50, 50)
                                            )
                                            simEngine.bodies.add(newBody)
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
                        onDoubleTap = {
                            simEngine.bodies.clear()
                            simEngine.electronBeam.clear()
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
                        text = "Spannung UB = ${simEngine.expVoltageUb.toInt()} V",
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        color = InkSecondary
                    )
                    Slider(
                        value = simEngine.expVoltageUb,
                        onValueChange = {
                            simEngine.expVoltageUb = it
                            // Adjust emitted electron speed
                            val e = simEngine.bodies.firstOrNull { b -> b.char == "e" }
                            if (e != null) {
                                e.vy = -sqrt(it / 300f) * 380f
                            }
                        },
                        valueRange = 100f..500f,
                        colors = SliderDefaults.colors(thumbColor = InkColor, activeTrackColor = InkColor)
                    )

                    Text(
                        text = "Spulenstrom IS = ${(simEngine.expCurrentIs * 100).toInt() / 100f} A",
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        color = InkSecondary
                    )
                    Slider(
                        value = simEngine.expCurrentIs,
                        onValueChange = {
                            simEngine.expCurrentIs = it
                            // Adjust B-field
                            val b = simEngine.bodies.firstOrNull { it.fieldType == FieldType.MAGNETIC_B }
                            if (b != null) {
                                b.fieldMagnitude = it * 2000f
                            }
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
                    showLernblattDrawer = false
                }
            )
        }
    }
}

// Draw the Electric Field Box with parallel directional arrows
fun DrawScope.drawElectricFieldBox(ef: SimBody) {
    val hs = ef.fieldRadius
    val cx = ef.x
    val cy = ef.y
    val th = ef.fieldAngle
    val dx = cos(th)
    val dy = sin(th)
    val px = -dy
    val py = dx

    // Dotted boundary rectangle
    drawRect(
        color = InkColor.copy(alpha = 0.12f),
        topLeft = Offset(cx - hs, cy - hs),
        size = androidx.compose.ui.geometry.Size(hs * 2, hs * 2),
        style = Stroke(width = 1.4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f), 0f))
    )

    // 9 Long Parallel Arrows
    val n = 7
    val al = 10f
    val ah = atan2(dy, dx)
    for (i in 0 until n) {
        val off = (i - (n - 1) / 2f) * (2f * hs / n)
        val ax = cx + px * off - dx * (hs - 10f)
        val ay = cy + py * off - dy * (hs - 10f)
        val bx = cx + px * off + dx * (hs - 10f)
        val by = cy + py * off + dy * (hs - 10f)

        drawLine(
            color = InkColor.copy(alpha = 0.18f),
            start = Offset(ax, ay),
            end = Offset(bx, by),
            strokeWidth = 1.5f
        )

        // Arrow head (using lines instead of allocating Path objects)
        val p1 = Offset(bx - al * cos(ah - 0.42f), by - al * sin(ah - 0.42f))
        val p2 = Offset(bx - al * cos(ah + 0.42f), by - al * sin(ah + 0.42f))
        drawLine(color = InkColor.copy(alpha = 0.22f), start = Offset(bx, by), end = p1, strokeWidth = 1.5f)
        drawLine(color = InkColor.copy(alpha = 0.22f), start = Offset(bx, by), end = p2, strokeWidth = 1.5f)
    }
}

// Draw the Magnetic Field Circle with field dots (⊙)
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

    // Field dots grid inside circle
    val step = 28f
    var gx = -r + 14f
    while (gx <= r - 14f) {
        var gy = -r + 14f
        while (gy <= r - 14f) {
            if (gx * gx + gy * gy <= (r - 12f) * (r - 12f)) {
                drawCircle(
                    color = InkColor.copy(alpha = 0.20f),
                    radius = 2.0f,
                    center = Offset(cx + gx, cy + gy)
                )
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
            .width(520.dp),
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