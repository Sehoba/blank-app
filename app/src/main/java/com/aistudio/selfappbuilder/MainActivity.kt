package com.aistudio.selfappbuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = Color.White,
                    surface = Color.White,
                    primary = Color(0xFF222222)
                )
            ) {
                DivinekoApp()
            }
        }
    }
}

enum class Screen { MENU, GAME }
enum class RoundResult { WIN, LOSE }

data class Hazard(
    val position: Offset,
    val velocity: Offset,
    val radius: Float
)

@Composable
private fun DivinekoApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.MENU) }
    var level by rememberSaveable { mutableIntStateOf(1) }
    var attempt by rememberSaveable { mutableIntStateOf(0) }

    when (screen) {
        Screen.MENU -> MenuScreen(
            level = level,
            onLevel = { level = it },
            onPlay = {
                attempt++
                screen = Screen.GAME
            }
        )

        Screen.GAME -> key(level, attempt) {
            GameScreen(
                level = level,
                onBack = { screen = Screen.MENU },
                onRetry = { attempt++ },
                onNext = {
                    level = if (level >= 8) 1 else level + 1
                    attempt++
                }
            )
        }
    }
}

@Composable
private fun MenuScreen(
    level: Int,
    onLevel: (Int) -> Unit,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawBackdrop()
            drawCat(Offset(size.width * 0.13f, size.height * 0.405f), size.width * 0.095f)
        }

        Text(
            text = "⚙",
            fontSize = 34.sp,
            color = Color(0xFF333333),
            modifier = Modifier
                .statusBarsPadding()
                .padding(20.dp)
                .align(Alignment.TopStart)
        )

        Text(
            text = "DIVINEKO",
            fontSize = 50.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.sp,
            color = Color(0xFF111111),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 185.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 170.dp)
        ) {
            Text(
                text = "CHAPTER I",
                color = Color.White,
                fontSize = 22.sp,
                letterSpacing = 2.sp
            )
            Text("⌄", color = Color.White, fontSize = 34.sp)

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..8).forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == level) 24.dp else 20.dp)
                            .background(
                                if (i == level) Color.White else Color.Transparent,
                                CircleShape
                            )
                            .then(
                                if (i == level) Modifier
                                else Modifier.background(Color.Transparent, CircleShape)
                            )
                            .clickable { onLevel(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (i != level) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color(0xFF666666),
                                    radius = size.minDimension / 2,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(64.dp))

            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .size(110.dp)
                    .clickable { onPlay() }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val p = Path().apply {
                        moveTo(size.width * 0.34f, size.height * 0.22f)
                        lineTo(size.width * 0.34f, size.height * 0.78f)
                        lineTo(size.width * 0.78f, size.height * 0.50f)
                        close()
                    }
                    drawPath(p, Color(0xFFF3F3F3))
                }
            }
        }
    }
}

@Composable
private fun GameScreen(
    level: Int,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit
) {
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    var pathPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var inkUsed by remember { mutableStateOf(0f) }
    var running by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(7f) }
    var result by remember { mutableStateOf<RoundResult?>(null) }
    var hazards by remember { mutableStateOf<List<Hazard>>(emptyList()) }

    LaunchedEffect(boardSize) {
        if (boardSize != IntSize.Zero && hazards.isEmpty()) {
            hazards = spawnHazards(level, boardSize)
        }
    }

    LaunchedEffect(running, boardSize) {
        if (!running || boardSize == IntSize.Zero) return@LaunchedEffect

        var lastFrame = 0L
        while (isActive && running && result == null) {
            withFrameNanos { now ->
                if (lastFrame == 0L) {
                    lastFrame = now
                    return@withFrameNanos
                }

                val dt = min((now - lastFrame) / 1_000_000_000f, 0.033f)
                lastFrame = now
                timeLeft = max(0f, timeLeft - dt)

                val cat = catCenter(boardSize)
                val catRadius = boardSize.width * 0.058f
                val strokeRadius = max(9f, boardSize.width * 0.014f)

                hazards = hazards.map {
                    moveHazard(
                        hazard = it,
                        dt = dt,
                        board = boardSize,
                        barrier = pathPoints,
                        barrierRadius = strokeRadius
                    )
                }

                if (hazards.any { distance(it.position, cat) < it.radius + catRadius }) {
                    result = RoundResult.LOSE
                    running = false
                } else if (timeLeft <= 0f) {
                    result = RoundResult.WIN
                    running = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { boardSize = it }
                .pointerInput(result, running, boardSize) {
                    detectDragGestures(
                        onDragStart = { p ->
                            if (!running && result == null && p.y < boardSize.height * 0.50f) {
                                pathPoints = listOf(p)
                                inkUsed = 0f
                            }
                        },
                        onDrag = { change, _ ->
                            if (!running && result == null && pathPoints.isNotEmpty()) {
                                val p = change.position
                                val last = pathPoints.last()
                                val extra = distance(last, p)
                                val limit = boardSize.width * 1.15f
                                if (p.y < boardSize.height * 0.50f && inkUsed + extra <= limit) {
                                    pathPoints = pathPoints + p
                                    inkUsed += extra
                                }
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            if (!running && result == null && pathPoints.size > 1) {
                                running = true
                            }
                        }
                    )
                }
        ) {
            drawBackdrop()

            val progressY = size.height * 0.066f
            val x1 = size.width * 0.25f
            val x2 = size.width * 0.75f
            drawLine(Color(0xFFF1F1F1), Offset(x1, progressY), Offset(x2, progressY), 6f, StrokeCap.Round)
            val done = ((7f - timeLeft) / 7f).coerceIn(0f, 1f)
            drawLine(Color(0xFF222222), Offset(x1, progressY), Offset(x1 + (x2 - x1) * done, progressY), 6f, StrokeCap.Round)

            drawCat(catCenter(boardSize), size.width * 0.086f)
            hazards.forEach { drawHazard(it) }

            if (pathPoints.size > 1) {
                for (i in 0 until pathPoints.lastIndex) {
                    drawLine(
                        color = Color(0xFF111111),
                        start = pathPoints[i],
                        end = pathPoints[i + 1],
                        strokeWidth = max(18f, size.width * 0.028f),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Surface(
            color = Color(0xFFC9C9C9),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .statusBarsPadding()
                .padding(14.dp)
                .align(Alignment.TopEnd)
                .clickable { onBack() }
        ) {
            Text(
                text = "✕",
                fontSize = 30.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
            )
        }

        if (!running && result == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 58.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = "ZEICHNE DEN SCHUTZ",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Ziehe mit dem Finger eine Linie vor die Katze. Sobald du loslässt, beginnt der Angriff.",
                    color = Color(0xFFBDBDBD),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        }

        result?.let { state ->
            Surface(
                color = Color(0xEEFFFFFF),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(28.dp)
                ) {
                    Text(
                        text = if (state == RoundResult.WIN) "KATZE GERETTET" else "ERWISCHT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color(0xFF222222)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (state == RoundResult.WIN) "Level $level geschafft." else "Die Schutzlinie war nicht stark genug.",
                        color = Color(0xFF555555)
                    )
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = if (state == RoundResult.WIN) onNext else onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
                    ) {
                        Text(if (state == RoundResult.WIN) "NÄCHSTES LEVEL" else "NOCHMAL")
                    }
                    TextButton(onClick = onBack) {
                        Text("MENÜ", color = Color(0xFF444444))
                    }
                }
            }
        }
    }
}

private fun spawnHazards(level: Int, board: IntSize): List<Hazard> {
    if (board == IntSize.Zero) return emptyList()
    val w = board.width.toFloat()
    val h = board.height.toFloat()
    val cat = catCenter(board)
    val count = min(4, 1 + (level - 1) / 2)
    val speed = w * (0.13f + level * 0.008f)

    return List(count) { i ->
        val pos = Offset(
            x = w * (0.73f + i * 0.07f),
            y = h * (0.34f + (i % 3) * 0.035f)
        )
        val dir = normalized(Offset(cat.x - pos.x, cat.y - pos.y))
        Hazard(
            position = pos,
            velocity = Offset(dir.x * speed, dir.y * speed),
            radius = w * (0.044f - min(i, 2) * 0.003f)
        )
    }
}

private fun moveHazard(
    hazard: Hazard,
    dt: Float,
    board: IntSize,
    barrier: List<Offset>,
    barrierRadius: Float
): Hazard {
    val w = board.width.toFloat()
    val h = board.height.toFloat()
    var pos = Offset(
        hazard.position.x + hazard.velocity.x * dt,
        hazard.position.y + hazard.velocity.y * dt
    )
    var vel = hazard.velocity
    val r = hazard.radius
    val top = h * 0.12f
    val bottom = h * 0.49f

    if (pos.x > w - r) {
        pos = Offset(w - r, pos.y)
        vel = Offset(-kotlin.math.abs(vel.x), vel.y)
    }
    if (pos.y < top + r) {
        pos = Offset(pos.x, top + r)
        vel = Offset(vel.x, kotlin.math.abs(vel.y))
    }
    if (pos.y > bottom - r) {
        pos = Offset(pos.x, bottom - r)
        vel = Offset(vel.x, -kotlin.math.abs(vel.y))
    }

    if (barrier.size > 1) {
        for (i in 0 until barrier.lastIndex) {
            val a = barrier[i]
            val b = barrier[i + 1]
            val closest = closestPoint(pos, a, b)
            val dx = pos.x - closest.x
            val dy = pos.y - closest.y
            val d = sqrt(dx * dx + dy * dy)
            val minD = r + barrierRadius

            if (d < minD) {
                var nx: Float
                var ny: Float
                if (d > 0.001f) {
                    nx = dx / d
                    ny = dy / d
                } else {
                    val sx = b.x - a.x
                    val sy = b.y - a.y
                    val len = max(0.001f, sqrt(sx * sx + sy * sy))
                    nx = -sy / len
                    ny = sx / len
                }

                val dot = vel.x * nx + vel.y * ny
                if (dot < 0f) {
                    vel = Offset(
                        vel.x - 2f * dot * nx,
                        vel.y - 2f * dot * ny
                    )
                }
                pos = Offset(
                    closest.x + nx * minD,
                    closest.y + ny * minD
                )
                break
            }
        }
    }

    return hazard.copy(position = pos, velocity = vel)
}

private fun closestPoint(p: Offset, a: Offset, b: Offset): Offset {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val denom = abx * abx + aby * aby
    if (denom <= 0.0001f) return a
    val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / denom).coerceIn(0f, 1f)
    return Offset(a.x + abx * t, a.y + aby * t)
}

private fun catCenter(board: IntSize): Offset = Offset(
    board.width * 0.135f,
    board.height * 0.405f
)

private fun normalized(v: Offset): Offset {
    val len = sqrt(v.x * v.x + v.y * v.y)
    return if (len < 0.0001f) Offset.Zero else Offset(v.x / len, v.y / len)
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun DrawScope.drawBackdrop() {
    val w = size.width
    val h = size.height
    drawRect(Color.White)
    drawRect(
        color = Color(0xFF242424),
        topLeft = Offset(0f, h * 0.50f),
        size = Size(w, h * 0.50f)
    )

    val big = Path().apply {
        moveTo(0f, h * 0.29f)
        lineTo(w * 0.14f, h * 0.045f)
        lineTo(w * 0.39f, h * 0.29f)
        close()
    }
    drawPath(big, Color(0xFF242424))

    val small = Path().apply {
        moveTo(w * 0.45f, h * 0.29f)
        lineTo(w * 0.54f, h * 0.19f)
        lineTo(w * 0.63f, h * 0.29f)
        close()
    }
    drawPath(small, Color(0xFF242424))
}

private fun DrawScope.drawCat(center: Offset, scale: Float) {
    val black = Color(0xFF050505)
    val s = scale

    drawRoundRect(
        color = black,
        topLeft = Offset(center.x - s * 0.34f, center.y - s * 0.26f),
        size = Size(s * 0.68f, s * 0.72f),
        cornerRadius = CornerRadius(s * 0.18f, s * 0.18f)
    )
    drawCircle(black, s * 0.34f, Offset(center.x, center.y - s * 0.27f))

    val leftEar = Path().apply {
        moveTo(center.x - s * 0.29f, center.y - s * 0.43f)
        lineTo(center.x - s * 0.18f, center.y - s * 0.68f)
        lineTo(center.x - s * 0.04f, center.y - s * 0.42f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(center.x + s * 0.29f, center.y - s * 0.43f)
        lineTo(center.x + s * 0.18f, center.y - s * 0.68f)
        lineTo(center.x + s * 0.04f, center.y - s * 0.42f)
        close()
    }
    drawPath(leftEar, black)
    drawPath(rightEar, black)

    val tail = Path().apply {
        moveTo(center.x - s * 0.30f, center.y + s * 0.32f)
        cubicTo(
            center.x - s * 0.70f, center.y + s * 0.28f,
            center.x - s * 0.75f, center.y - s * 0.15f,
            center.x - s * 0.56f, center.y - s * 0.35f
        )
    }
    drawPath(
        path = tail,
        color = black,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = s * 0.14f, cap = StrokeCap.Round)
    )

    drawCircle(Color(0xFFD6E1E6), s * 0.045f, Offset(center.x - s * 0.11f, center.y - s * 0.27f))
    drawCircle(Color(0xFFD6E1E6), s * 0.045f, Offset(center.x + s * 0.11f, center.y - s * 0.27f))
}

private fun DrawScope.drawHazard(hazard: Hazard) {
    drawCircle(Color(0xFF050505), hazard.radius, hazard.position)
    drawLine(
        color = Color.White,
        start = Offset(hazard.position.x - hazard.radius * 0.45f, hazard.position.y - hazard.radius * 0.45f),
        end = Offset(hazard.position.x + hazard.radius * 0.45f, hazard.position.y + hazard.radius * 0.45f),
        strokeWidth = max(5f, hazard.radius * 0.12f),
        cap = StrokeCap.Round
    )
}
