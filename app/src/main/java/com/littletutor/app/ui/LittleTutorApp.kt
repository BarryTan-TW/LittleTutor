package com.littletutor.app.ui

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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.littletutor.app.ui.tutor.Question
import com.littletutor.app.ui.tutor.TestUnit
import com.littletutor.app.ui.tutor.TestingMode
import com.littletutor.app.ui.tutor.TutorViewModel
import com.littletutor.app.ui.tutor.UserSpace
import com.littletutor.app.ui.tutor.ZhuyinHelper

// Draws a simple vertical scrollbar on the right edge of a scrollable Column.
private fun Modifier.simpleVerticalScrollbar(
    state: androidx.compose.foundation.ScrollState,
    width: Dp = 4.dp,
    color: Color = Color(0xFF9E9E9E)
): Modifier = this.drawWithContent {
    drawContent()
    val contentHeight = state.maxValue + size.height
    if (state.maxValue <= 0) return@drawWithContent
    val thumbHeight = (size.height / contentHeight) * size.height
    val thumbTop = (state.value.toFloat() / state.maxValue) * (size.height - thumbHeight)
    val barWidth = width.toPx()
    drawRect(
        color = color.copy(alpha = 0.3f),
        topLeft = Offset(size.width - barWidth, 0f),
        size = androidx.compose.ui.geometry.Size(barWidth, size.height)
    )
    drawRect(
        color = color,
        topLeft = Offset(size.width - barWidth, thumbTop),
        size = androidx.compose.ui.geometry.Size(barWidth, thumbHeight.coerceAtLeast(24f))
    )
}

/**
 * 注音顯示組件
 * 每個中文字上方顯示對應的注音。
 */
@Composable
fun ZhuyinText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        text.forEach { char ->
            val zhuyin = ZhuyinHelper.getZhuyin(char)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.padding(horizontal = 1.dp)
            ) {
                if (zhuyin.isNotEmpty()) {
                    Text(
                        text = zhuyin,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = (fontSize.value * 0.4f).sp,
                        lineHeight = (fontSize.value * 0.4f).sp,
                        modifier = Modifier.padding(bottom = 0.dp)
                    )
                }
                Text(
                    text = char.toString(),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = fontSize,
                    color = color
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LittleTutorApp(viewModel: TutorViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserName = uiState.currentUser?.displayName ?: "訪客"
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        viewModel.onPhotoCaptured(it)
    }
    var showDeleteUnitConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (!uiState.isWritingTestActive) {
                TopAppBar(
                    title = { Text(text = "小小家教 - 目前使用者：$currentUserName") },
                    actions = {
                        TextButton(onClick = viewModel::openSettings) {
                            Text(text = "設定")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (uiState.isWritingTestActive) {
            WritingTestScreen(
                viewModel = viewModel,
                uiState = uiState,
                innerPadding = innerPadding,
                onReplayWord = viewModel::replayCurrentWord,
                onToggleAnswerVisibility = viewModel::toggleWritingAnswerVisibility,
                onEvaluateWritingBitmap = viewModel::evaluateWritingBitmap,
                onClearRecognition = viewModel::clearWritingRecognition,
                onNextWord = viewModel::nextWritingWord,
                onRestartRound = viewModel::restartWritingRound,
                onFinishTest = viewModel::finishWritingTest,
                onUpdateSpeechRate = viewModel::updateWritingSpeechRate
            )
        } else if (uiState.isLessonActive) {
            if (uiState.tutorState.isFinished) {
                ResultScreen(
                    score = uiState.tutorState.score,
                    totalQuestions = uiState.tutorState.totalQuestions,
                    innerPadding = innerPadding,
                    onRestart = viewModel::restart,
                    onBackHome = viewModel::goHome
                )
            } else {
                QuizScreen(
                    uiState = uiState.tutorState,
                    innerPadding = innerPadding,
                    onSelectOption = viewModel::selectOption,
                    onCheckAnswer = viewModel::checkAnswer,
                    onNextQuestion = viewModel::nextQuestion
                )
            }
        } else {
            HomeScreen(
                viewModel = viewModel,
                uiState = uiState,
                innerPadding = innerPadding,
                onToggleTestUnitSelection = viewModel::toggleTestUnitSelection,
                onMoveUp = viewModel::moveTestUnitUp,
                onMoveDown = viewModel::moveTestUnitDown,
                onAddUnit = viewModel::openAddUnitDialog,
                onEditSelectedUnit = viewModel::openEditSelectedUnit,
                onDeleteSelectedUnits = { showDeleteUnitConfirm = true },
                onStartSelectedUnitsTest = viewModel::startSelectedUnitsTest
            )
        }

        if (uiState.isSettingsOpen) {
            SettingsDialog(
                users = uiState.users,
                currentUserId = uiState.currentUser?.id,
                newUserName = uiState.newUserNameInput,
                testingMode = uiState.testingMode,
                onNewUserNameChange = viewModel::updateNewUserName,
                onAddUser = viewModel::addUser,
                onSwitchUser = viewModel::switchUser,
                onDeleteUser = viewModel::deleteUser,
                onSetTestingMode = viewModel::setTestingMode,
                onDismiss = viewModel::closeSettings
            )
        }

        val previewPhotoPath = uiState.previewPhotoPath
        if (uiState.isPhotoPreviewOpen && previewPhotoPath != null) {
            DirectSelectPreviewDialog(
                photoPath = previewPhotoPath,
                previewTokens = uiState.previewTokens,
                selectedTokenIndexes = uiState.selectedPreviewTokenIndexes,
                isLoading = uiState.isPreviewTokenLoading,
                unitTitle = uiState.previewUnitTitleInput,
                existingUnitTitles = uiState.testUnits.map { it.title }.distinct().sorted(),
                onToggleToken = viewModel::togglePreviewToken,
                onSetTokenSelected = viewModel::setPreviewTokenSelected,
                onApplySelectedSet = viewModel::setPreviewSelectedTokenIndexes,
                onUnitTitleChange = viewModel::updatePreviewUnitTitleInput,
                onSave = viewModel::savePreviewSelectionToUnit,
                onDismiss = {
                    viewModel.closePhotoPreview()
                    viewModel.closeAddUnitDialog()
                }
            )
        }

        if (uiState.isEditUnitDialogOpen) {
            EditTestUnitDialog(
                title = uiState.editingUnitTitleInput,
                words = uiState.editingUnitWordsInput,
                onTitleChange = viewModel::updateEditingUnitTitle,
                onWordsChange = viewModel::updateEditingUnitWords,
                onSave = viewModel::saveEditingUnit,
                onDismiss = viewModel::closeEditUnitDialog
            )
        }

        if (uiState.isAddUnitDialogOpen) {
            AddTestUnitDialog(
                title = uiState.addingUnitTitleInput,
                words = uiState.addingUnitWordsInput,
                isPhotoAreaOpen = uiState.isAddUnitPhotoAreaOpen,
                photoPaths = uiState.photoPaths,
                selectedPhotoPaths = uiState.selectedPhotoPaths,
                onTitleChange = viewModel::updateAddingUnitTitle,
                onWordsChange = viewModel::updateAddingUnitWords,
                onOpenPhotoArea = viewModel::openAddUnitPhotoArea,
                onClosePhotoArea = viewModel::closeAddUnitDialog,
                onTakePhoto = {
                    viewModel.preparePhotoCaptureUri()?.let { uri ->
                        cameraLauncher.launch(uri)
                    }
                },
                onOpenPhotoPreview = viewModel::openPhotoPreview,
                onTogglePhotoSelection = viewModel::togglePhotoSelection,
                onDeleteSelectedPhotos = viewModel::deleteSelectedPhotos,
                onSave = viewModel::saveAddedUnit,
                onDismiss = viewModel::closeAddUnitDialog
            )
        }

        if (uiState.isScoreHistoryOpen) {
            ScoreHistoryDialog(
                testRecords = uiState.testRecords,
                wordStatistics = uiState.wordStatistics,
                onClearHistory = viewModel::clearScoreHistory,
                onDismiss = viewModel::closeScoreHistory
            )
        }

        if (showDeleteUnitConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteUnitConfirm = false },
                title = { Text(text = "確認刪除") },
                text = { Text(text = "您確定要刪除選取的課文單元嗎？這項動作無法復原。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteSelectedTestUnits()
                            showDeleteUnitConfirm = false
                        }
                    ) {
                        Text(text = "確定刪除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteUnitConfirm = false }) {
                        Text(text = "取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    viewModel: TutorViewModel,
    uiState: com.littletutor.app.ui.tutor.LittleTutorUiState,
    innerPadding: PaddingValues,
    onToggleTestUnitSelection: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddUnit: () -> Unit,
    onEditSelectedUnit: () -> Unit,
    onDeleteSelectedUnits: () -> Unit,
    onStartSelectedUnitsTest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TestUnitSection(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            viewModel = viewModel,
            testUnits = uiState.testUnits,
            selectedUnitIds = uiState.selectedTestUnitIds,
            onToggleSelection = onToggleTestUnitSelection,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onAddUnit = onAddUnit,
            onEditSelectedUnit = onEditSelectedUnit,
            onDeleteSelectedUnits = onDeleteSelectedUnits,
            onStartSelectedUnitsTest = onStartSelectedUnitsTest
        )
    }
}

@Composable
private fun WritingTestScreen(
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
                        val selectedUnitId = uiState.selectedTestUnitIds.firstOrNull()
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

@Composable
private fun PhotoThumbnail(
    photoPath: String,
    isSelected: Boolean,
    onPreview: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val bitmap = remember(photoPath) { BitmapFactory.decodeFile(photoPath)?.asImageBitmap() }
    if (bitmap != null) {
        Card {
            Box {
                Image(
                    bitmap = bitmap,
                    contentDescription = "照片縮圖",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable(onClick = onPreview)
                )
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
private fun TestUnitSection(
    modifier: Modifier = Modifier,
    viewModel: TutorViewModel,
    testUnits: List<TestUnit>,
    selectedUnitIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddUnit: () -> Unit,
    onEditSelectedUnit: () -> Unit,
    onDeleteSelectedUnits: () -> Unit,
    onStartSelectedUnitsTest: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "課文列表與測試", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = onAddUnit) { Text(text = "新增") }
            }
            if (testUnits.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "目前沒有課文單元，請先按新增。", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = onAddUnit,
                        modifier = Modifier.fillMaxWidth(0.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = "新增課文單元")
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(testUnits) { unit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSelection(unit.id) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = selectedUnitIds.contains(unit.id),
                                    onClick = { onToggleSelection(unit.id) }
                                )
                                Text(text = "${unit.title}（${unit.words.size} 個字詞）")
                            }
                            TextButton(onClick = { viewModel.openScoreHistory(unit.id) }) {
                                Text(text = "測試歷史")
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onStartSelectedUnitsTest, enabled = selectedUnitIds.isNotEmpty(), modifier = Modifier.weight(0.30f)) {
                        Text(text = "開始測試")
                    }
                    Button(onClick = onEditSelectedUnit, enabled = selectedUnitIds.size == 1, modifier = Modifier.weight(0.30f)) {
                        Text(text = "編輯內容")
                    }
                    Button(onClick = onMoveUp, enabled = selectedUnitIds.size == 1, modifier = Modifier.weight(0.15f)) {
                        Text(text = "上移")
                    }
                    Button(onClick = onMoveDown, enabled = selectedUnitIds.size == 1, modifier = Modifier.weight(0.15f)) {
                        Text(text = "下移")
                    }
                    Button(onClick = onDeleteSelectedUnits, enabled = selectedUnitIds.isNotEmpty(), modifier = Modifier.weight(0.10f)) {
                        Text(text = "刪除")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTestUnitDialog(
    title: String,
    words: String,
    isPhotoAreaOpen: Boolean,
    photoPaths: List<String>,
    selectedPhotoPaths: Set<String>,
    onTitleChange: (String) -> Unit,
    onWordsChange: (String) -> Unit,
    onOpenPhotoArea: () -> Unit,
    onClosePhotoArea: () -> Unit,
    onTakePhoto: () -> Unit,
    onOpenPhotoPreview: (String) -> Unit,
    onTogglePhotoSelection: (String) -> Unit,
    onDeleteSelectedPhotos: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (isPhotoAreaOpen) "題庫建立區域" else "新增測試項目")
                            if (!isPhotoAreaOpen) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onOpenPhotoArea,
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(text = "從照片建立")
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        if (isPhotoAreaOpen) {
                            TextButton(onClick = onClosePhotoArea) {
                                Text(text = "返回")
                            }
                        }
                    },
                    actions = {
                        if (!isPhotoAreaOpen) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onSave,
                                    enabled = title.isNotBlank() && words.isNotBlank()
                                ) {
                                    Text(text = "儲存")
                                }
                                TextButton(onClick = onDismiss) {
                                    Text(text = "取消")
                                }
                            }
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            if (isPhotoAreaOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Button(onClick = onTakePhoto) {
                                        Text(text = "拍照")
                                    }
                                    Button(
                                        onClick = onDeleteSelectedPhotos,
                                        enabled = selectedPhotoPaths.isNotEmpty()
                                    ) {
                                        Text(text = "刪除照片")
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(3f)
                                        .fillMaxSize(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    if (photoPaths.isEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(text = "尚無照片")
                                        }
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(4),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(photoPaths.size) { index ->
                                                val photoPath = photoPaths[index]
                                                PhotoThumbnail(
                                                    photoPath = photoPath,
                                                    isSelected = selectedPhotoPaths.contains(photoPath),
                                                    onPreview = { onOpenPhotoPreview(photoPath) },
                                                    onToggleSelection = { onTogglePhotoSelection(photoPath) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text(text = "課文單元名稱") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = words,
                        onValueChange = onWordsChange,
                        label = { Text(text = "字詞（可用 、 或換行分隔）") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 10
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTestUnitDialog(
    title: String,
    words: String,
    onTitleChange: (String) -> Unit,
    onWordsChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "編輯內容") },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) { Text(text = "取消") }
                    },
                    actions = {
                        TextButton(
                            onClick = onSave,
                            enabled = title.isNotBlank() && words.isNotBlank()
                        ) {
                            Text(text = "儲存")
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(text = "課文單元名稱") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = words,
                    onValueChange = onWordsChange,
                    label = { Text(text = "字詞（可用 、 或換行分隔）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 10
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectSelectPreviewDialog(
    photoPath: String,
    previewTokens: List<com.littletutor.app.ui.tutor.OcrToken>,
    selectedTokenIndexes: Set<Int>,
    isLoading: Boolean,
    unitTitle: String,
    existingUnitTitles: List<String>,
    onToggleToken: (Int) -> Unit,
    onSetTokenSelected: (Int, Boolean) -> Unit,
    onApplySelectedSet: (Set<Int>) -> Unit,
    onUnitTitleChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var titleMenuExpanded by remember { mutableStateOf(false) }
    val lassoSelectionHistory = remember { mutableStateListOf<Set<Int>>() }

    Dialog(
        onDismissRequest = onDismiss, 
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "照片預覽 - 直接圈選") },
                    navigationIcon = { TextButton(onClick = onDismiss) { Text(text = "關閉") } },
                    actions = {
                        TextButton(onClick = onSave, enabled = unitTitle.isNotBlank() && selectedTokenIndexes.isNotEmpty()) {
                            Text(text = "儲存到課文")
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding() // 增加鍵盤避讓
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left side: Photo Preview
                Column(
                    modifier = Modifier.weight(2.5f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(text = "正在辨識文字...")
                        }
                    }
                    SelectablePhotoPreview(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        photoPath = photoPath,
                        previewTokens = previewTokens,
                        selectedTokenIndexes = selectedTokenIndexes,
                        onToggleToken = onToggleToken,
                        onSetTokenSelected = onSetTokenSelected,
                        onLassoApplied = { previousSelectedSet -> lassoSelectionHistory += previousSelectedSet }
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onApplySelectedSet(previewTokens.indices.toSet() - selectedTokenIndexes) }) {
                            Text(text = "圈選後反選")
                        }
                        TextButton(onClick = { onApplySelectedSet(emptySet()) }) { Text(text = "清空選取") }
                        TextButton(
                            onClick = {
                                val previous = lassoSelectionHistory.lastOrNull() ?: return@TextButton
                                onApplySelectedSet(previous)
                                lassoSelectionHistory.removeAt(lassoSelectionHistory.lastIndex)
                            },
                            enabled = lassoSelectionHistory.isNotEmpty()
                        ) { Text(text = "撤銷上一筆曲線") }
                    }
                    Text(text = "提示：可點擊框選單字，優化：長按可圈選文字", style = MaterialTheme.typography.bodySmall)
                }

                // Right side: Controls and Text
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()), // 增加捲動能力，確保橫屏時鍵盤彈出仍能按到所有按鈕
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = unitTitle,
                        onValueChange = onUnitTitleChange,
                        label = { Text("課文單元名稱") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { titleMenuExpanded = true }) { Text(text = "選取既有課文單元") }
                        DropdownMenu(expanded = titleMenuExpanded, onDismissRequest = { titleMenuExpanded = false }) {
                            existingUnitTitles.forEach { title ->
                                DropdownMenuItem(
                                    text = { Text(text = title) },
                                    onClick = { onUnitTitleChange(title); titleMenuExpanded = false }
                                )
                            }
                        }
                    }
                    val selectedTexts = previewTokens.filterIndexed { index, _ -> selectedTokenIndexes.contains(index) }.joinToString(separator = "、") { it.text }
                    OutlinedTextField(
                        value = selectedTexts,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = "圈選後自動轉文字") },
                        placeholder = { Text(text = "尚未選取文字") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 300.dp), // 改用 heightIn 代替 weight，確保在捲動容器中顯示正常
                        minLines = 5
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectablePhotoPreview(
    modifier: Modifier = Modifier,
    photoPath: String,
    previewTokens: List<com.littletutor.app.ui.tutor.OcrToken>,
    selectedTokenIndexes: Set<Int>,
    onToggleToken: (Int) -> Unit,
    onSetTokenSelected: (Int, Boolean) -> Unit,
    onLassoApplied: (Set<Int>) -> Unit
) {
    val bitmap = remember(photoPath) { BitmapFactory.decodeFile(photoPath) }
    if (bitmap == null) { Text(text = "無法開啟此照片。"); return }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var lassoPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val lassoColor = Color(0xFFFFA000)

    Box(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight().onSizeChanged { canvasSize = it }) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "照片預覽", contentScale = ContentScale.Fit, modifier = Modifier.matchParentSize())
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(previewTokens, selectedTokenIndexes, canvasSize) {
                        detectTapGestures { tapOffset ->
                            if (canvasSize.width == 0 || canvasSize.height == 0) return@detectTapGestures
                            val imageFrame = imageFrameInCanvas(canvasSize.width.toFloat(), canvasSize.height.toFloat(), bitmap.width.toFloat(), bitmap.height.toFloat())
                            val hitIndex = previewTokens.indexOfLast { token ->
                                Rect(
                                    imageFrame.left + token.left * imageFrame.scale,
                                    imageFrame.top + token.top * imageFrame.scale,
                                    imageFrame.left + token.right * imageFrame.scale,
                                    imageFrame.top + token.bottom * imageFrame.scale
                                ).contains(tapOffset)
                            }
                            val tolerantHitIndex = if (hitIndex >= 0) hitIndex else {
                                previewTokens.indexOfLast { token ->
                                    distancePointToRect(tapOffset, Rect(
                                        imageFrame.left + token.left * imageFrame.scale,
                                        imageFrame.top + token.top * imageFrame.scale,
                                        imageFrame.left + token.right * imageFrame.scale,
                                        imageFrame.top + token.bottom * imageFrame.scale
                                    )) <= TAP_TOLERANCE_PX
                                }
                            }
                            if (tolerantHitIndex >= 0) onToggleToken(tolerantHitIndex)
                        }
                    }
                    .pointerInput(previewTokens, selectedTokenIndexes, canvasSize) {
                        detectDragGestures(
                            onDragStart = { lassoPoints = listOf(it) },
                            onDrag = { change, _ -> lassoPoints = lassoPoints + change.position; change.consume() },
                            onDragEnd = {
                                val polygon = lassoPoints
                                if (polygon.size >= 3 && canvasSize.width > 0 && canvasSize.height > 0) {
                                    onLassoApplied(selectedTokenIndexes)
                                    val imageFrame = imageFrameInCanvas(canvasSize.width.toFloat(), canvasSize.height.toFloat(), bitmap.width.toFloat(), bitmap.height.toFloat())
                                    previewTokens.forEachIndexed { index, token ->
                                        val center = Offset(
                                            imageFrame.left + ((token.left + token.right) / 2f) * imageFrame.scale,
                                            imageFrame.top + ((token.top + token.bottom) / 2f) * imageFrame.scale
                                        )
                                        val tokenRect = Rect(
                                            imageFrame.left + token.left * imageFrame.scale,
                                            imageFrame.top + token.top * imageFrame.scale,
                                            imageFrame.left + token.right * imageFrame.scale,
                                            imageFrame.top + token.bottom * imageFrame.scale
                                        )
                                        if (isPointInPolygon(center, polygon)) onSetTokenSelected(index, true)
                                    }
                                }
                                lassoPoints = emptyList()
                            },
                            onDragCancel = { lassoPoints = emptyList() }
                        )
                    }
            ) {
                val imageFrame = imageFrameInCanvas(size.width, size.height, bitmap.width.toFloat(), bitmap.height.toFloat())
                previewTokens.forEachIndexed { index, token ->
                    if (!selectedTokenIndexes.contains(index)) return@forEachIndexed
                    val left = imageFrame.left + token.left * imageFrame.scale
                    val top = imageFrame.top + token.top * imageFrame.scale
                    val right = imageFrame.left + token.right * imageFrame.scale
                    val bottom = imageFrame.top + token.bottom * imageFrame.scale
                    drawRect(
                        color = Color(0xAA4CAF50),
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )
                }
                if (lassoPoints.size >= 2) {
                    for (i in 0 until lassoPoints.lastIndex) {
                        drawLine(color = lassoColor, start = lassoPoints[i], end = lassoPoints[i + 1], strokeWidth = 4f)
                    }
                }
            }
        }
    }
}

private data class ImageFrame(val left: Float, val top: Float, val scale: Float)

private fun imageFrameInCanvas(canvasWidth: Float, canvasHeight: Float, bitmapWidth: Float, bitmapHeight: Float): ImageFrame {
    val scale = minOf(canvasWidth / bitmapWidth, canvasHeight / bitmapHeight)
    return ImageFrame((canvasWidth - bitmapWidth * scale) / 2f, (canvasHeight - bitmapHeight * scale) / 2f, scale)
}

private const val TAP_TOLERANCE_PX = 18f

private fun isTokenHitByLasso(tokenRect: Rect, tokenCenter: Offset, polygon: List<Offset>): Boolean {
    if (isPointInPolygon(tokenCenter, polygon)) return true
    val corners = listOf(Offset(tokenRect.left, tokenRect.top), Offset(tokenRect.right, tokenRect.top), Offset(tokenRect.left, tokenRect.bottom), Offset(tokenRect.right, tokenRect.bottom))
    return corners.count { isPointInPolygon(it, polygon) } >= 2
}

private fun distancePointToRect(point: Offset, rect: Rect): Float {
    val dx = when { point.x < rect.left -> rect.left - point.x; point.x > rect.right -> point.x - rect.right; else -> 0f }
    val dy = when { point.y < rect.top -> rect.top - point.y; point.y > rect.bottom -> point.y - rect.bottom; else -> 0f }
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

private fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val xi = polygon[i].x; val yi = polygon[i].y
        val xj = polygon[j].x; val yj = polygon[j].y
        val intersects = ((yi > point.y) != (yj > point.y)) && (point.x < (xj - xi) * (point.y - yi) / ((yj - yi).coerceAtLeast(0.0001f)) + xi)
        if (intersects) inside = !inside
        j = i
    }
    return inside
}

@Composable
private fun QuizScreen(
    uiState: com.littletutor.app.ui.tutor.TutorUiState,
    innerPadding: PaddingValues,
    onSelectOption: (Int) -> Unit,
    onCheckAnswer: () -> Unit,
    onNextQuestion: () -> Unit
) {
    val currentQuestion = uiState.currentQuestion ?: return
    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "題目 ${uiState.currentQuestionNumber}/${uiState.totalQuestions}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (uiState.isAnswerChecked) {
            ZhuyinText(text = currentQuestion.prompt, fontSize = MaterialTheme.typography.titleLarge.fontSize)
        } else {
            Text(text = currentQuestion.prompt, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        OptionList(question = currentQuestion, selectedOptionIndex = uiState.selectedOptionIndex, isAnswerChecked = uiState.isAnswerChecked, onSelectOption = onSelectOption)
        Button(onClick = { if (uiState.isAnswerChecked) onNextQuestion() else onCheckAnswer() }, enabled = uiState.selectedOptionIndex != null, modifier = Modifier.fillMaxWidth()) {
            Text(text = if (uiState.isAnswerChecked) "下一題" else "確認")
        }
        Text(text = "分數：${uiState.score}", style = MaterialTheme.typography.bodyLarge)
        if (uiState.isAnswerChecked) {
            Text(text = if (uiState.isCurrentAnswerCorrect) "太棒了，答對了！" else "還差一點，再試一次！", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OptionList(question: Question, selectedOptionIndex: Int?, isAnswerChecked: Boolean, onSelectOption: (Int) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(question.options) { index, optionText ->
            val isSelected = selectedOptionIndex == index
            val isCorrect = index == question.correctOptionIndex
            val cardColor = when {
                isAnswerChecked && isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
                isAnswerChecked && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Card(onClick = { if (!isAnswerChecked) onSelectOption(index) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor)) {
                Box(modifier = Modifier.padding(16.dp)) {
                    if (isAnswerChecked) {
                        ZhuyinText(text = optionText, fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                    } else {
                        Text(text = optionText, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(score: Int, totalQuestions: Int, innerPadding: PaddingValues, onRestart: () -> Unit, onBackHome: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "本次課程完成！", style = MaterialTheme.typography.headlineMedium)
        Text(text = "你的分數：$score / $totalQuestions", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text(text = "重新開始") }
        Button(onClick = onBackHome, modifier = Modifier.fillMaxWidth()) { Text(text = "回到首頁") }
    }
}

@Composable
private fun SettingsDialog(
    users: List<UserSpace>,
    currentUserId: String?,
    newUserName: String,
    testingMode: TestingMode,
    onNewUserNameChange: (String) -> Unit,
    onAddUser: () -> Unit,
    onSwitchUser: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    onSetTestingMode: (TestingMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss, 
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "設定", style = MaterialTheme.typography.headlineSmall)
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState)
                            .simpleVerticalScrollbar(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(value = newUserName, onValueChange = onNewUserNameChange, label = { Text("新使用者名稱") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = onAddUser, modifier = Modifier.fillMaxWidth()) { Text(text = "新增使用者") }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "切換 / 刪除使用者", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        users.forEach { user ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = user.displayName + if (user.id == currentUserId) "（目前）" else "", modifier = Modifier.weight(1f))
                                TextButton(onClick = { onSwitchUser(user.id) }) { Text(text = "切換") }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(onClick = { onDeleteUser(user.id) }) { Text(text = "刪除") }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "測試方式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onSetTestingMode(TestingMode.WRITING_BOARD) },
                                modifier = Modifier.weight(1f),
                                colors = if (testingMode == TestingMode.WRITING_BOARD) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                            ) { Text(text = "畫板書寫") }
                            Button(
                                onClick = { onSetTestingMode(TestingMode.MANUAL_CONFIRM) },
                                modifier = Modifier.weight(1f),
                                colors = if (testingMode == TestingMode.MANUAL_CONFIRM) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                            ) { Text(text = "自行確認") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "關於", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = "作者：Barry Tan")
                        Text(text = "Email：shihhong.tan@gmail.com")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismiss) { Text(text = "完成") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreHistoryDialog(
    testRecords: List<com.littletutor.app.ui.tutor.TestRecord>,
    wordStatistics: List<com.littletutor.app.ui.tutor.WordStatistics>,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val leftScrollState = rememberScrollState()
    val rightScrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClearHistory) {
                    Text(text = "刪除歷史資料", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text(text = "關閉") }
            }
        },
        title = { Text(text = "歷史成績與分析") },
        modifier = Modifier.fillMaxWidth(0.95f).heightIn(max = 700.dp),
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 左欄：考試記錄
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(
                            text = "📚 考試記錄 (${testRecords.size}次)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(leftScrollState)
                                    .simpleVerticalScrollbar(leftScrollState),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (testRecords.isNotEmpty()) {
                                    testRecords.forEach { record ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                val dateFormat =
                                                    java.text.SimpleDateFormat("MM/dd HH:mm")
                                                Text(
                                                    text = dateFormat.format(record.timestamp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = record.unitTitle,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "成績：${record.correctCount}/${record.totalCount} (${
                                                        String.format(
                                                            "%.1f%%",
                                                            record.getAccuracy() * 100
                                                        )
                                                    })",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "暫無考試記錄",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 右欄：需要加強的字詞
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(
                            text = "📊 需要加強的字詞",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "（按失敗率排序）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rightScrollState)
                                    .simpleVerticalScrollbar(rightScrollState),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (wordStatistics.isNotEmpty()) {
                                    wordStatistics.forEach { stats ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFFFF3E0)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically) {
                                                ZhuyinText(
                                                    text = stats.word,
                                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "✓${stats.correctCount} ✗${stats.totalAttempts - stats.correctCount}",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                    Text(
                                                        text = "失敗率: ${
                                                            String.format(
                                                                "%.1f%%",
                                                                stats.getErrorRate() * 100
                                                            )
                                                        }",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (stats.getErrorRate() > 0.5f) Color(
                                                            0xFFC62828
                                                        ) else Color(0xFF2E7D32)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "暫無字詞統計",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
