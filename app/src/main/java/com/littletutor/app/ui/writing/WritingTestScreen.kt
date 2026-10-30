package com.littletutor.app.ui.writing

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.littletutor.app.ui.tutor.TestUnit
import com.littletutor.app.ui.tutor.TestingMode
import com.littletutor.app.ui.tutor.TutorViewModel
import com.littletutor.app.ui.tutor.UserSpace
import com.littletutor.app.ui.tutor.ZhuyinHelper
import com.littletutor.app.ui.common.ZhuyinText
import com.littletutor.app.ui.common.simpleVerticalScrollbar

@Composable
internal fun WritingTestScreen(
    viewModel: TutorViewModel,
    uiState: com.littletutor.app.ui.tutor.LittleTutorUiState,
    innerPadding: PaddingValues,
    onReplayWord: () -> Unit,
    onToggleAnswerVisibility: () -> Unit,
    onEvaluateWritingBitmap: (Bitmap) -> Unit,
    onClearRecognition: () -> Unit,
    onNextWord: () -> Unit,
    onRestartRound: () -> Unit,
    onFinishTest: () -> Unit,
    onUpdateSpeechRate: (Float) -> Unit
) {
    val words = uiState.writingWordsRound
    val currentWord = words.getOrNull(uiState.writingCurrentIndex)
    var lastClickTime by remember { mutableStateOf(0L) }
    val debounceInterval = 1000L // 1秒防抖

    fun handleConfirm(isCorrect: Boolean) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceInterval) {
            lastClickTime = currentTime
            viewModel.confirmTestResult(isCorrect)
        }
    }

    if (uiState.isWritingRoundFinished) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "本輪測試完成", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "正確 ${uiState.writingCorrectCount} / ${uiState.writingWordsRound.size}",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                    ) {
                        Text(
                            text = "✓ 正確 (${uiState.writingTestResults.count { it.isCorrect }})",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.writingTestResults.filter { it.isCorrect }) { result ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                ZhuyinText(
                                    text = result.word,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(
                            text = "✗ 錯誤 (${uiState.writingTestResults.count { !it.isCorrect }})",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.writingTestResults.filter { !it.isCorrect }) { result ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2))
                            ) {
                                ZhuyinText(
                                    text = result.word,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onRestartRound, modifier = Modifier.weight(1f)) {
                    Text(text = "再測一輪")
                }
                Button(
                    onClick = {
                        viewModel.saveTestRecord()
                        val selectedUnitId = uiState.writingUnitId
                        viewModel.openScoreHistory(selectedUnitId)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "查看成績")
                }
                Button(
                    onClick = {
                        viewModel.saveTestRecord()
                        onFinishTest()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "回到首頁")
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "測試進度 ${uiState.writingCurrentIndex + 1}/${words.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "請書寫：",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    if (uiState.writingShowAnswer && currentWord != null) {
                        ZhuyinText(
                            text = currentWord,
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize
                        )
                    } else {
                        val wordLength = currentWord?.length ?: 0
                        Text(
                            text = "？".repeat(wordLength),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onToggleAnswerVisibility,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(text = if (uiState.writingShowAnswer) "隱藏" else "答案")
                    }
                }
            }
            Button(onClick = onFinishTest) { Text(text = "退出") }
        }

        if (uiState.testingMode == TestingMode.WRITING_BOARD) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReplayWord,
                    modifier = Modifier.weight(2f)
                ) {
                    Text(text = "朗讀")
                }
                Button(
                    onClick = onNextWord,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "下一題")
                }
                Button(
                    onClick = onClearRecognition,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "清除比對")
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "播放速度", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = String.format("%.1f x", uiState.writingSpeechRate),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Slider(
                    value = uiState.writingSpeechRate,
                    onValueChange = onUpdateSpeechRate,
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            WritingBoard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                boardKey = uiState.writingCurrentIndex,
                onBitmapReady = onEvaluateWritingBitmap,
                onClearRecognition = onClearRecognition
            )

            OutlinedTextField(
                value = uiState.recognizedWritingText,
                onValueChange = {},
                readOnly = true,
                label = { Text(text = "辨識文字") },
                modifier = Modifier.fillMaxWidth()
            )

            val resultText = when (uiState.isWritingAnswerCorrect) {
                true -> "正確"
                false -> "錯誤"
                null -> "請開始書寫"
            }
            val resultColor = when (uiState.isWritingAnswerCorrect) {
                true -> Color(0xFF2E7D32)
                false -> Color(0xFFC62828)
                null -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(text = resultText, color = resultColor, style = MaterialTheme.typography.titleMedium)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReplayWord,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "朗讀")
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "播放速度", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = String.format("%.1f x", uiState.writingSpeechRate),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Slider(
                    value = uiState.writingSpeechRate,
                    onValueChange = onUpdateSpeechRate,
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { handleConfirm(true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text(text = "✓ 正確", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                }
                Button(
                    onClick = { handleConfirm(false) },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text(text = "✗ 錯誤", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                }
            }
        }
    }
}

@Composable
private fun WritingBoard(
    modifier: Modifier = Modifier,
    boardKey: Int = 0,
    onBitmapReady: (Bitmap) -> Unit,
    onClearRecognition: () -> Unit
) {
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var activeStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    LaunchedEffect(boardKey) {
        strokes.clear()
        activeStroke = emptyList()
    }

    fun buildBitmapFromStrokes(size: IntSize, allStrokes: List<List<Offset>>): Bitmap {
        val safeWidth = size.width.coerceAtLeast(1)
        val safeHeight = size.height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 22f
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        allStrokes.forEach { stroke ->
            if (stroke.isEmpty()) return@forEach
            val path = AndroidPath().apply { moveTo(stroke.first().x, stroke.first().y) }
            stroke.drop(1).forEach { point -> path.lineTo(point.x, point.y) }
            canvas.drawPath(path, paint)
        }
        return bitmap
    }

    Card(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { boardSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            activeStroke = offset.let { listOf(it) }
                            onClearRecognition()
                        },
                        onDrag = { change, _ ->
                            activeStroke = activeStroke + change.position
                            change.consume()
                        },
                        onDragEnd = {
                            if (activeStroke.isNotEmpty()) {
                                strokes += activeStroke
                                activeStroke = emptyList()
                                onBitmapReady(buildBitmapFromStrokes(boardSize, strokes.toList()))
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                fun drawStroke(points: List<Offset>) {
                    if (points.size < 2) {
                        points.firstOrNull()?.let { drawCircle(color = Color.Black, radius = 5f, center = it) }
                        return
                    }
                    for (i in 0 until points.lastIndex) {
                        drawLine(color = Color.Black, start = points[i], end = points[i + 1], strokeWidth = 16f)
                    }
                }
                strokes.forEach { drawStroke(it) }
                drawStroke(activeStroke)
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                TextButton(
                    onClick = {
                        strokes.clear()
                        activeStroke = emptyList()
                        onClearRecognition()
                    }
                ) {
                    Text(text = "清空畫板")
                }
            }
        }
    }
}
