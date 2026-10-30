package com.littletutor.app.ui.writing

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littletutor.app.ui.common.ZhuyinText
import com.littletutor.app.ui.common.isWideLayout
import com.littletutor.app.ui.tutor.LittleTutorUiState
import com.littletutor.app.ui.tutor.TestingMode
import com.littletutor.app.ui.tutor.TtsStatus
import java.util.Locale

private val CorrectColor = Color(0xFF2E7D32)
private val WrongColor = Color(0xFFC62828)

@Composable
internal fun WritingTestScreen(
    uiState: LittleTutorUiState,
    innerPadding: PaddingValues,
    onReplayWord: () -> Unit,
    onToggleAnswerVisibility: () -> Unit,
    onEvaluateWritingBitmap: (Bitmap) -> Unit,
    onClearRecognition: () -> Unit,
    onNextWord: () -> Unit,
    onConfirmResult: (Boolean) -> Unit,
    onRestartRound: () -> Unit,
    onRetryWrongWords: () -> Unit,
    onOpenHistory: () -> Unit,
    onExitTest: () -> Unit,
    onBackHome: () -> Unit,
    onUpdateSpeechRate: (Float) -> Unit
) {
    if (uiState.isWritingRoundFinished) {
        RoundResult(
            uiState = uiState,
            innerPadding = innerPadding,
            onRetryWrongWords = onRetryWrongWords,
            onRestartRound = onRestartRound,
            onOpenHistory = onOpenHistory,
            onBackHome = onBackHome
        )
        return
    }

    val words = uiState.writingWordsRound
    val currentWord = words.getOrNull(uiState.writingCurrentIndex) ?: return
    val wide = isWideLayout()
    val boardMode = uiState.testingMode == TestingMode.WRITING_BOARD
    val isLast = uiState.writingCurrentIndex == words.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TestHeader(uiState = uiState, onExit = onExitTest)

        val prompt: @Composable ColumnScope.() -> Unit = {
            PromptCard(
                word = currentWord,
                showAnswer = uiState.writingShowAnswer,
                onToggleAnswer = onToggleAnswerVisibility
            )
            SpeakControls(
                speechRate = uiState.writingSpeechRate,
                ttsStatus = uiState.ttsStatus,
                onReplay = onReplayWord,
                onRateChange = onUpdateSpeechRate
            )
        }

        if (boardMode) {
            val board: @Composable (Modifier) -> Unit = { modifier ->
                WritingBoard(
                    modifier = modifier,
                    boardKey = "${uiState.writingWordsRound.hashCode()}-${uiState.writingCurrentIndex}",
                    onBitmapReady = onEvaluateWritingBitmap,
                    onClearRecognition = onClearRecognition
                )
            }
            val feedback: @Composable () -> Unit = {
                BoardFeedback(
                    uiState = uiState,
                    isLast = isLast,
                    onNext = onNextWord
                )
            }
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        prompt()
                        feedback()
                    }
                    board(Modifier.weight(1.3f).fillMaxHeight())
                }
            } else {
                prompt()
                board(Modifier.fillMaxWidth().weight(1f))
                feedback()
            }
        } else {
            val buttons: @Composable (Modifier) -> Unit = { modifier ->
                ManualConfirmButtons(
                    enabled = !uiState.writingIsAdvancing,
                    selected = uiState.isWritingAnswerCorrect,
                    modifier = modifier,
                    onConfirm = onConfirmResult
                )
            }
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) { prompt() }
                    buttons(Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    prompt()
                    Text(
                        text = "請孩子寫在紙上，寫完後對照答案，按下正確或錯誤。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                buttons(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TestHeader(uiState: LittleTutorUiState, onExit: () -> Unit) {
    val total = uiState.writingWordsRound.size.coerceAtLeast(1)
    val current = uiState.writingCurrentIndex + 1
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onExit) {
            Icon(Icons.Filled.Close, contentDescription = "結束測試")
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (uiState.writingIsPracticeRound) "${uiState.writingUnitTitle}・錯字練習" else uiState.writingUnitTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            LinearProgressIndicator(
                progress = { current.toFloat() / total },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = "$current / $total",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptCard(word: String, showAnswer: Boolean, onToggleAnswer: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "請寫出聽到的字詞", style = MaterialTheme.typography.labelLarge)
                if (showAnswer) {
                    ZhuyinText(text = word, fontSize = 32.sp)
                } else {
                    // One box per character tells the child how many characters to write.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(word.length) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "？", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onToggleAnswer, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(text = if (showAnswer) "隱藏答案" else "看答案")
            }
        }
    }
}

@Composable
private fun SpeakControls(
    speechRate: Float,
    ttsStatus: TtsStatus,
    onReplay: () -> Unit,
    onRateChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledTonalButton(
                onClick = onReplay,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
            ) {
                Text(text = "🔊 再聽一次", style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "語速 ${String.format(Locale.US, "%.1f", speechRate)}x",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = speechRate,
                    onValueChange = onRateChange,
                    valueRange = 0.5f..2.0f,
                    steps = 5
                )
            }
        }
        if (ttsStatus == TtsStatus.UNAVAILABLE) {
            Text(
                text = "⚠ 無法朗讀：請到系統設定安裝中文語音，或由家長唸出題目。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun BoardFeedback(uiState: LittleTutorUiState, isLast: Boolean, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                uiState.writingIsEvaluating -> {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(text = "辨識中…", style = MaterialTheme.typography.titleMedium)
                }
                uiState.isWritingAnswerCorrect == true -> FeedbackText("✓ 寫對了！", uiState.recognizedWritingText, CorrectColor)
                uiState.isWritingAnswerCorrect == false -> FeedbackText("再試試看", uiState.recognizedWritingText, WrongColor)
                else -> Text(
                    text = "在格子裡寫字，寫完按下一題",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(onClick = onNext, modifier = Modifier.heightIn(min = 56.dp)) {
            Text(text = if (isLast) "完成" else "下一題 →", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FeedbackText(label: String, recognized: String, color: Color) {
    Column {
        Text(text = label, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (recognized.isNotBlank()) {
            Text(
                text = "辨識：$recognized",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ManualConfirmButtons(
    enabled: Boolean,
    selected: Boolean?,
    modifier: Modifier,
    onConfirm: (Boolean) -> Unit
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(true to "✓ 正確", false to "✗ 錯誤").forEach { (value, label) ->
            val color = if (value) CorrectColor else WrongColor
            Button(
                onClick = { onConfirm(value) },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 72.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = color,
                    disabledContainerColor = if (selected == value) color else color.copy(alpha = 0.3f),
                    disabledContentColor = Color.White
                )
            ) {
                Text(text = label, fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun WritingBoard(
    modifier: Modifier,
    boardKey: Any,
    onBitmapReady: (Bitmap) -> Unit,
    onClearRecognition: () -> Unit
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 6.dp.toPx() }
    val ocrStrokeWidthPx = with(density) { 8.dp.toPx() }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var activeStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val inkColor = Color(0xFF1B1B1F)

    // Runs for each new question and when the board is recreated (e.g. rotation),
    // so feedback never refers to strokes that are no longer shown.
    LaunchedEffect(boardKey) {
        strokes.clear()
        activeStroke = emptyList()
        onClearRecognition()
    }

    fun submit() {
        if (strokes.isEmpty()) onClearRecognition()
        else onBitmapReady(renderStrokes(boardSize, strokes.toList(), ocrStrokeWidthPx))
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { boardSize = it }
                    // Raw pointer tracking (no touch slop) so short dots like 、 are captured.
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            onClearRecognition()
                            activeStroke = listOf(down.position)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                activeStroke = activeStroke + change.position
                                change.consume()
                            }
                            strokes += activeStroke
                            activeStroke = emptyList()
                            submit()
                        }
                    }
            ) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                drawLine(guideColor, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2f, pathEffect = dash)
                drawLine(guideColor, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2f, pathEffect = dash)

                val style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                (strokes + listOf(activeStroke)).forEach { points ->
                    when {
                        points.isEmpty() -> Unit
                        points.size == 1 -> drawCircle(inkColor, radius = strokeWidthPx / 2, center = points[0])
                        else -> drawPath(
                            Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                            },
                            color = inkColor,
                            style = style
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = {
                        if (strokes.isNotEmpty()) {
                            strokes.removeAt(strokes.lastIndex)
                            submit()
                        }
                    },
                    enabled = strokes.isNotEmpty()
                ) { Text(text = "↶ 復原") }
                TextButton(
                    onClick = {
                        strokes.clear()
                        activeStroke = emptyList()
                        onClearRecognition()
                    },
                    enabled = strokes.isNotEmpty()
                ) { Text(text = "清空") }
            }
            if (strokes.isEmpty() && activeStroke.isEmpty()) {
                Text(
                    text = "在這裡寫字",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.headlineSmall,
                    color = guideColor
                )
            }
        }
    }
}

private fun renderStrokes(size: IntSize, strokes: List<List<Offset>>, strokeWidth: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width.coerceAtLeast(1), size.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        this.strokeWidth = strokeWidth
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    strokes.forEach { stroke ->
        if (stroke.size == 1) {
            canvas.drawPoint(stroke[0].x, stroke[0].y, paint)
            return@forEach
        }
        val path = AndroidPath().apply { moveTo(stroke.first().x, stroke.first().y) }
        stroke.drop(1).forEach { path.lineTo(it.x, it.y) }
        canvas.drawPath(path, paint)
    }
    return bitmap
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoundResult(
    uiState: LittleTutorUiState,
    innerPadding: PaddingValues,
    onRetryWrongWords: () -> Unit,
    onRestartRound: () -> Unit,
    onOpenHistory: () -> Unit,
    onBackHome: () -> Unit
) {
    val results = uiState.writingTestResults
    val correct = results.filter { it.isCorrect }
    val wrong = results.filterNot { it.isCorrect }
    val percent = if (results.isEmpty()) 0 else correct.size * 100 / results.size
    val wide = isWideLayout()
    val shortScreen = LocalConfiguration.current.screenHeightDp < 480

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 840.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when {
                            percent >= 90 -> "🏆"
                            percent >= 60 -> "👍"
                            else -> "💪"
                        },
                        fontSize = if (shortScreen) 36.sp else 56.sp
                    )
                    Text(
                        text = if (wrong.isEmpty()) "全部答對，太棒了！" else "本輪完成！",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "答對 ${correct.size} / ${results.size}（$percent%）",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (uiState.writingIsPracticeRound) "錯字練習不列入成績" else "成績已自動儲存",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (wrong.isNotEmpty()) {
                    WordSection("需要加強（${wrong.size}）", wrong.map { it.word }, WrongColor, Color(0xFFFFEBEE))
                }
                if (correct.isNotEmpty()) {
                    WordSection("答對（${correct.size}）", correct.map { it.word }, CorrectColor, Color(0xFFE8F5E9))
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = if (wide) 4 else 2
            ) {
                val buttonModifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                if (wrong.isNotEmpty()) {
                    Button(onClick = onRetryWrongWords, modifier = buttonModifier) {
                        Text(text = "練習錯字（${wrong.size}）")
                    }
                }
                if (wrong.isEmpty()) {
                    Button(onClick = onRestartRound, modifier = buttonModifier) { Text(text = "再測一輪") }
                } else {
                    OutlinedButton(onClick = onRestartRound, modifier = buttonModifier) { Text(text = "再測一輪") }
                }
                OutlinedButton(onClick = onOpenHistory, modifier = buttonModifier) { Text(text = "查看成績") }
                OutlinedButton(onClick = onBackHome, modifier = buttonModifier) { Text(text = "回到首頁") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordSection(title: String, words: List<String>, accent: Color, background: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            words.forEach { word ->
                Surface(shape = RoundedCornerShape(12.dp), color = background) {
                    ZhuyinText(
                        text = word,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}
