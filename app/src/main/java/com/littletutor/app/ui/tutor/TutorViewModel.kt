package com.littletutor.app.ui.tutor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.core.graphics.scale

enum class TestingMode {
    WRITING_BOARD,  // 畫板書寫測試
    MANUAL_CONFIRM  // 自行按鈕確認測試
}

data class WritingTestResult(
    val word: String,
    val isCorrect: Boolean
)

data class TestRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val unitTitle: String = "",
    val totalCount: Int = 0,
    val correctCount: Int = 0,
    val results: List<WritingTestResult> = emptyList()
) {
    fun getAccuracy(): Float = if (totalCount == 0) 0f else (correctCount.toFloat() / totalCount.toFloat())
}

data class WordStatistics(
    val word: String,
    val totalAttempts: Int = 0,
    val correctCount: Int = 0
) {
    fun getAccuracy(): Float = if (totalAttempts == 0) 0f else (correctCount.toFloat() / totalAttempts.toFloat())
    fun getErrorRate(): Float = 1f - getAccuracy()
}

data class LittleTutorUiState(
    val users: List<UserSpace> = emptyList(),
    val currentUser: UserSpace? = null,
    val isSettingsOpen: Boolean = false,
    val newUserNameInput: String = "",
    val testingMode: TestingMode = TestingMode.MANUAL_CONFIRM,
    val photoPaths: List<String> = emptyList(),
    val selectedPhotoPaths: Set<String> = emptySet(),
    val testUnits: List<TestUnit> = emptyList(),
    val latestAccuracyByUnitTitle: Map<String, Float> = emptyMap(),
    val isAddUnitDialogOpen: Boolean = false,
    val isAddUnitPhotoAreaOpen: Boolean = false,
    val addingUnitTitleInput: String = "",
    val addingUnitWordsInput: String = "",
    val isEditUnitDialogOpen: Boolean = false,
    val editingUnitId: String? = null,
    val editingUnitTitleInput: String = "",
    val editingUnitWordsInput: String = "",
    val isPhotoPreviewOpen: Boolean = false,
    val previewPhotoPath: String? = null,
    val previewTokens: List<OcrToken> = emptyList(),
    val selectedPreviewTokenIndexes: Set<Int> = emptySet(),
    val isPreviewTokenLoading: Boolean = false,
    val previewUnitTitleInput: String = "課文單詞句",
    val isWritingTestActive: Boolean = false,
    val writingUnitId: String? = null,
    val writingUnitTitle: String = "",
    val writingWordsRound: List<String> = emptyList(),
    val writingCurrentIndex: Int = 0,
    val writingShowAnswer: Boolean = false,
    val recognizedWritingText: String = "",
    val isWritingAnswerCorrect: Boolean? = null,
    val isWritingRoundFinished: Boolean = false,
    val isWritingTestSaved: Boolean = false,
    val writingCorrectCount: Int = 0,
    val writingSpeechRate: Float = 1.0f,
    val writingTestResults: List<WritingTestResult> = emptyList(),
    val testRecords: List<TestRecord> = emptyList(),
    val wordStatistics: List<WordStatistics> = emptyList(),
    val isScoreHistoryOpen: Boolean = false,
    val scoreHistoryUnitId: String? = null,
    val userMessage: String? = null
)

class TutorViewModel(application: Application) : AndroidViewModel(application) {
    private val userSpaceManager = UserSpaceManager.withAppFilesDir(application.filesDir)

    private val _uiState = MutableStateFlow(LittleTutorUiState())
    val uiState: StateFlow<LittleTutorUiState> = _uiState.asStateFlow()
    private var pendingCaptureFile: File? = null
    private val textRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private val writingResultsByIndex = mutableMapOf<Int, Boolean>()

    init {
        reloadFromStorage()
        ensureTextToSpeech()
    }

    fun consumeUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(userMessage = message) }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun setTestingMode(mode: TestingMode) {
        _uiState.update { it.copy(testingMode = mode) }
    }

    fun updateNewUserName(input: String) {
        _uiState.update { it.copy(newUserNameInput = input) }
    }

    fun addUser() {
        val name = _uiState.value.newUserNameInput.trim()
        if (name.isEmpty()) {
            return
        }

        userSpaceManager.addUser(name)
        _uiState.update { it.copy(newUserNameInput = "") }
        reloadFromStorage()
    }

    fun switchUser(userId: String) {
        userSpaceManager.switchUser(userId)
        reloadFromStorage()
    }

    fun deleteUser(userId: String) {
        userSpaceManager.deleteUser(userId)
        reloadFromStorage()
    }

    fun preparePhotoCaptureUri(): Uri? {
        val currentUser = _uiState.value.currentUser ?: return null
        val photoFile = userSpaceManager.createPhotoFile(currentUser.id)
        pendingCaptureFile = photoFile
        val authority = "${getApplication<Application>().packageName}.fileprovider"
        return FileProvider.getUriForFile(getApplication(), authority, photoFile)
    }

    fun onPhotoCaptured(success: Boolean) {
        val currentUser = _uiState.value.currentUser ?: return
        val pendingFile = pendingCaptureFile
        if (!success) {
            pendingFile?.delete()
        }
        pendingCaptureFile = null
        _uiState.update {
            it.copy(
                photoPaths = userSpaceManager.listPhotos(currentUser.id).map(File::getAbsolutePath),
                selectedPhotoPaths = emptySet()
            )
        }
    }

    fun togglePhotoSelection(photoPath: String) {
        _uiState.update {
            val next = it.selectedPhotoPaths.toMutableSet()
            if (!next.add(photoPath)) {
                next.remove(photoPath)
            }
            it.copy(selectedPhotoPaths = next)
        }
    }

    fun deleteSelectedPhotos() {
        val currentUser = _uiState.value.currentUser ?: return
        val selectedPhotos = _uiState.value.selectedPhotoPaths
        if (selectedPhotos.isEmpty()) {
            return
        }

        userSpaceManager.deletePhotos(currentUser.id, selectedPhotos)
        reloadFromStorage()
    }

    fun openPhotoPreview(photoPath: String) {
        _uiState.update {
            it.copy(
                isPhotoPreviewOpen = true,
                previewPhotoPath = photoPath,
                previewTokens = emptyList(),
                selectedPreviewTokenIndexes = emptySet(),
                isPreviewTokenLoading = true,
                previewUnitTitleInput = "課文單詞句"
            )
        }

        viewModelScope.launch {
            val tokens = extractTokensFromPhoto(photoPath)
            _uiState.update {
                it.copy(
                    previewTokens = tokens,
                    selectedPreviewTokenIndexes = emptySet(),
                    isPreviewTokenLoading = false
                )
            }
        }
    }

    fun closePhotoPreview() {
        _uiState.update {
            it.copy(
                isPhotoPreviewOpen = false,
                previewPhotoPath = null,
                previewTokens = emptyList(),
                selectedPreviewTokenIndexes = emptySet(),
                isPreviewTokenLoading = false
            )
        }
    }

    fun togglePreviewToken(index: Int) {
        _uiState.update {
            val next = it.selectedPreviewTokenIndexes.toMutableSet()
            if (!next.add(index)) {
                next.remove(index)
            }
            it.copy(selectedPreviewTokenIndexes = next)
        }
    }

    fun setPreviewTokenSelected(index: Int, selected: Boolean) {
        _uiState.update {
            val next = it.selectedPreviewTokenIndexes.toMutableSet()
            if (selected) {
                next.add(index)
            } else {
                next.remove(index)
            }
            it.copy(selectedPreviewTokenIndexes = next)
        }
    }

    fun setPreviewSelectedTokenIndexes(indexes: Set<Int>) {
        _uiState.update { it.copy(selectedPreviewTokenIndexes = indexes) }
    }

    fun updatePreviewUnitTitleInput(input: String) {
        _uiState.update { it.copy(previewUnitTitleInput = input) }
    }

    fun savePreviewSelectionToUnit() {
        val currentUser = _uiState.value.currentUser ?: return
        val photoPath = _uiState.value.previewPhotoPath ?: return
        val selectedWords = _uiState.value.previewTokens
            .filterIndexed { index, _ -> _uiState.value.selectedPreviewTokenIndexes.contains(index) }
            .map { it.text }
            .filter { it.isNotBlank() }
            .distinct()
        if (_uiState.value.previewUnitTitleInput.isBlank() || selectedWords.isEmpty()) {
            return
        }

        userSpaceManager.appendToTestUnitByTitle(
            userId = currentUser.id,
            title = _uiState.value.previewUnitTitleInput,
            words = selectedWords,
            photoPaths = listOf(photoPath)
        )
        reloadFromStorage()
        closePhotoPreview()
    }

    fun moveTestUnit(unitId: String, up: Boolean) {
        val currentUser = _uiState.value.currentUser ?: return
        val index = _uiState.value.testUnits.indexOfFirst { it.id == unitId }
        if (index < 0) return
        val target = if (up) index - 1 else index + 1
        if (userSpaceManager.moveTestUnit(currentUser.id, index, target)) {
            reloadFromStorage()
        }
    }

    fun startUnitTest(unitId: String) {
        val unit = _uiState.value.testUnits.firstOrNull { it.id == unitId } ?: return
        val words = unit.words
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .shuffled(Random(System.currentTimeMillis()))

        if (words.isEmpty()) {
            return
        }

        writingResultsByIndex.clear()

        _uiState.update {
            it.copy(
                isWritingTestActive = true,
                writingUnitId = unit.id,
                writingUnitTitle = unit.title,
                writingWordsRound = words,
                writingCurrentIndex = 0,
                writingShowAnswer = false,
                recognizedWritingText = "",
                isWritingAnswerCorrect = null,
                isWritingRoundFinished = false,
                isWritingTestSaved = false,
                writingCorrectCount = 0
            )
        }

        speakCurrentWord()
    }

    fun replayCurrentWord() {
        speakCurrentWord()
    }

    fun updateWritingSpeechRate(rate: Float) {
        _uiState.update { it.copy(writingSpeechRate = rate.coerceIn(0.5f, 2.0f)) }
        textToSpeech?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    fun toggleWritingAnswerVisibility() {
        _uiState.update { it.copy(writingShowAnswer = !it.writingShowAnswer) }
    }

    fun clearWritingRecognition() {
        _uiState.update {
            it.copy(
                recognizedWritingText = "",
                isWritingAnswerCorrect = null
            )
        }
    }

    fun evaluateWritingBitmap(bitmap: Bitmap) {
        val currentWord = _uiState.value.writingWordsRound.getOrNull(_uiState.value.writingCurrentIndex) ?: return
        viewModelScope.launch {
            val candidates = recognizeTextCandidates(bitmap)
            val isCorrect = isTargetMatched(candidates, currentWord)
            val recognized = pickBestCandidate(candidates, currentWord)
            val index = _uiState.value.writingCurrentIndex
            writingResultsByIndex[index] = isCorrect

            _uiState.update {
                it.copy(
                    recognizedWritingText = recognized,
                    isWritingAnswerCorrect = isCorrect,
                    writingCorrectCount = writingResultsByIndex.values.count { value -> value }
                )
            }
        }
    }

    fun nextWritingWord() {
        val state = _uiState.value
        if (!state.isWritingTestActive) {
            return
        }

        val nextIndex = state.writingCurrentIndex + 1
        if (nextIndex >= state.writingWordsRound.size) {
            // 生成測試結果列表
            val results = state.writingWordsRound.mapIndexed { index, word ->
                WritingTestResult(word, writingResultsByIndex[index] ?: false)
            }
            _uiState.update {
                it.copy(
                    isWritingRoundFinished = true,
                    recognizedWritingText = "",
                    isWritingAnswerCorrect = null,
                    writingTestResults = results
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                writingCurrentIndex = nextIndex,
                writingShowAnswer = false,
                recognizedWritingText = "",
                isWritingAnswerCorrect = null
            )
        }
        speakCurrentWord()
    }

    fun confirmTestResult(isCorrect: Boolean) {
        val state = _uiState.value
        if (!state.isWritingTestActive) {
            return
        }

        val index = state.writingCurrentIndex
        writingResultsByIndex[index] = isCorrect

        _uiState.update {
            it.copy(
                isWritingAnswerCorrect = isCorrect,
                writingCorrectCount = writingResultsByIndex.values.count { value -> value }
            )
        }

        // 自動進行到下一題
        viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            nextWritingWord()
        }
    }

    fun restartWritingRound() {
        val words = _uiState.value.writingWordsRound
        if (words.isEmpty()) {
            return
        }

        writingResultsByIndex.clear()
        _uiState.update {
            it.copy(
                writingWordsRound = words.shuffled(Random(System.currentTimeMillis())),
                writingCurrentIndex = 0,
                writingShowAnswer = false,
                recognizedWritingText = "",
                isWritingAnswerCorrect = null,
                isWritingRoundFinished = false,
                isWritingTestSaved = false,
                writingCorrectCount = 0
            )
        }
        speakCurrentWord()
    }

    fun finishWritingTest() {
        _uiState.update {
            it.copy(
                isWritingTestActive = false,
                isWritingRoundFinished = false,
                writingWordsRound = emptyList(),
                writingCurrentIndex = 0,
                writingShowAnswer = false,
                recognizedWritingText = "",
                isWritingAnswerCorrect = null,
                writingCorrectCount = 0,
                writingTestResults = emptyList(),
                isWritingTestSaved = false
            )
        }
        writingResultsByIndex.clear()
    }

    fun saveTestRecord() {
        var shouldProceed = false
        _uiState.update { 
            if (it.isWritingTestSaved) return@update it
            shouldProceed = true
            it.copy(isWritingTestSaved = true)
        }
        if (!shouldProceed) return
        
        val state = _uiState.value
        val currentUser = state.currentUser ?: return
        if (state.writingTestResults.isEmpty()) return

        val timestamp = System.currentTimeMillis()

        // 僅儲存一筆完整的測試紀錄（包含所有選取的課文名稱）
        val combinedRecord = TestRecord(
            timestamp = timestamp,
            unitTitle = state.writingUnitTitle,
            totalCount = state.writingTestResults.size,
            correctCount = state.writingCorrectCount,
            results = state.writingTestResults
        )
        userSpaceManager.saveTestRecord(currentUser.id, combinedRecord)
        _uiState.update { it.copy(latestAccuracyByUnitTitle = latestAccuracyByUnitTitle(currentUser.id)) }

        loadScoreHistory()
    }

    fun loadScoreHistory(unitId: String? = null) {
        val currentUser = _uiState.value.currentUser ?: return
        var records = userSpaceManager.loadTestRecords(currentUser.id)

        // 若指定 unitId，則顯示標題中精確包含該課文名稱的紀錄
        if (unitId != null) {
            val targetUnit = _uiState.value.testUnits.firstOrNull { it.id == unitId }
            if (targetUnit != null) {
                records = records.filter { record ->
                    UserSpaceManager.splitRecordTitles(record.unitTitle).contains(targetUnit.title)
                }
            }
        }

        // 彙整字詞統計
        val wordStats = mutableMapOf<String, Pair<Int, Int>>()  // word -> (total, correct)
        records.forEach { record ->
            record.results.forEach { result ->
                val (total, correct) = wordStats[result.word] ?: (0 to 0)
                wordStats[result.word] = ((total + 1) to (correct + if (result.isCorrect) 1 else 0))
            }
        }

        val statistics = wordStats.map { (word, counts) ->
            WordStatistics(word, counts.first, counts.second)
        }.sortedByDescending { it.getErrorRate() }

        _uiState.update {
            it.copy(
                testRecords = records.sortedByDescending { it.timestamp },
                wordStatistics = statistics
            )
        }
    }

    fun openScoreHistory(unitId: String? = null) {
        loadScoreHistory(unitId)
        _uiState.update { it.copy(isScoreHistoryOpen = true, scoreHistoryUnitId = unitId) }
    }

    fun closeScoreHistory() {
        _uiState.update { it.copy(isScoreHistoryOpen = false, scoreHistoryUnitId = null) }
    }

    fun clearScoreHistory() {
        val state = _uiState.value
        val currentUser = state.currentUser ?: return
        val unitId = state.scoreHistoryUnitId
        val unitTitle = if (unitId != null) {
            state.testUnits.firstOrNull { it.id == unitId }?.title
        } else null
        
        userSpaceManager.clearTestRecords(currentUser.id, unitTitle)
        _uiState.update { it.copy(latestAccuracyByUnitTitle = latestAccuracyByUnitTitle(currentUser.id)) }
        loadScoreHistory(unitId)
    }

    fun openEditUnit(unitId: String) {
        val target = _uiState.value.testUnits.firstOrNull { it.id == unitId } ?: return
        _uiState.update {
            it.copy(
                isEditUnitDialogOpen = true,
                editingUnitId = target.id,
                editingUnitTitleInput = target.title,
                editingUnitWordsInput = target.words.joinToString(separator = "、")
            )
        }
    }

    fun openAddUnitDialog() {
        _uiState.update {
            it.copy(
                isAddUnitDialogOpen = true,
                isAddUnitPhotoAreaOpen = false,
                addingUnitTitleInput = "",
                addingUnitWordsInput = ""
            )
        }
    }

    fun closeAddUnitDialog() {
        _uiState.update {
            it.copy(
                isAddUnitDialogOpen = false,
                isAddUnitPhotoAreaOpen = false,
                addingUnitTitleInput = "",
                addingUnitWordsInput = ""
            )
        }
    }

    fun openAddUnitPhotoArea() {
        _uiState.update { it.copy(isAddUnitPhotoAreaOpen = true) }
    }

    fun closeAddUnitPhotoArea() {
        _uiState.update { it.copy(isAddUnitPhotoAreaOpen = false) }
    }

    fun updateAddingUnitTitle(input: String) {
        _uiState.update { it.copy(addingUnitTitleInput = input) }
    }

    fun updateAddingUnitWords(input: String) {
        _uiState.update { it.copy(addingUnitWordsInput = input) }
    }

    fun saveAddedUnit() {
        val currentUser = _uiState.value.currentUser ?: return
        val parsedWords = _uiState.value.addingUnitWordsInput
            .split("[、，,\n\r]+".toRegex())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val title = _uiState.value.addingUnitTitleInput.trim()
        if (title.isEmpty() || parsedWords.isEmpty()) {
            return
        }

        userSpaceManager.saveTestUnit(
            userId = currentUser.id,
            title = title,
            words = parsedWords,
            photoPaths = emptyList()
        )
        reloadFromStorage()
        closeAddUnitDialog()
    }

    fun closeEditUnitDialog() {
        _uiState.update {
            it.copy(
                isEditUnitDialogOpen = false,
                editingUnitId = null,
                editingUnitTitleInput = "",
                editingUnitWordsInput = ""
            )
        }
    }

    fun updateEditingUnitTitle(input: String) {
        _uiState.update { it.copy(editingUnitTitleInput = input) }
    }

    fun updateEditingUnitWords(input: String) {
        _uiState.update { it.copy(editingUnitWordsInput = input) }
    }

    fun saveEditingUnit() {
        val currentUser = _uiState.value.currentUser ?: return
        val editingId = _uiState.value.editingUnitId ?: return
        val parsedWords = _uiState.value.editingUnitWordsInput
            .split("[、，,\n\r]+".toRegex())
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val updated = userSpaceManager.updateTestUnit(
            userId = currentUser.id,
            unitId = editingId,
            title = _uiState.value.editingUnitTitleInput,
            words = parsedWords
        )
        if (!updated) {
            return
        }

        reloadFromStorage()
        closeEditUnitDialog()
    }

    fun deleteTestUnit(unitId: String) {
        val currentUser = _uiState.value.currentUser ?: return
        userSpaceManager.deleteTestUnits(currentUser.id, setOf(unitId))
        reloadFromStorage()
    }


    private fun latestAccuracyByUnitTitle(userId: String): Map<String, Float> {
        val latest = mutableMapOf<String, TestRecord>()
        userSpaceManager.loadTestRecords(userId).forEach { record ->
            UserSpaceManager.splitRecordTitles(record.unitTitle).forEach { title ->
                val previous = latest[title]
                if (previous == null || record.timestamp >= previous.timestamp) latest[title] = record
            }
        }
        return latest.mapValues { it.value.getAccuracy() }
    }

    private fun reloadFromStorage() {
        val users = userSpaceManager.listUsers()
        val currentUser = userSpaceManager.currentUser()
        _uiState.update {
            it.copy(
                users = users,
                currentUser = currentUser,
                photoPaths = userSpaceManager.listPhotos(currentUser.id).map(File::getAbsolutePath),
                selectedPhotoPaths = emptySet(),
                testUnits = userSpaceManager.loadTestUnits(currentUser.id),
                latestAccuracyByUnitTitle = latestAccuracyByUnitTitle(currentUser.id)
            )
        }
    }

    fun getZhuyin(char: Char): String {
        return ZhuyinHelper.getZhuyin(char)
    }

    private suspend fun extractTokensFromPhoto(photoPath: String): List<OcrToken> {
        val image = runCatching {
            InputImage.fromFilePath(getApplication(), Uri.fromFile(File(photoPath)))
        }.getOrNull() ?: return emptyList()

        return runCatching {
            textRecognizer.process(image).await()
                .textBlocks
                .flatMap { block -> block.lines }
                .flatMap { line -> line.elements }
                .mapNotNull { element ->
                    val box = element.boundingBox ?: return@mapNotNull null
                    val text = element.text.trim()
                    if (text.isEmpty()) {
                        return@mapNotNull null
                    }
                    OcrToken(
                        text = text,
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom
                    )
                }
        }.getOrDefault(emptyList())
    }

    private suspend fun recognizeTextFromBitmap(bitmap: Bitmap): String {
        return runCatching {
            val image = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(image).await().text.trim()
        }.getOrDefault("")
    }

    private suspend fun recognizeTextCandidates(bitmap: Bitmap): List<String> {
        val variants = mutableListOf<Bitmap>()
        variants += bitmap

        val binary = toBinaryBitmap(bitmap)
        variants += binary

        val cropped = cropToInk(binary)
        variants += cropped

        val scaled = scaleUpIfNeeded(cropped)
        variants += scaled

        val normalizedCanvas = normalizeInkToCanvas(cropped)
        variants += normalizedCanvas

        val results = variants
            .map { recognizeTextFromBitmap(it) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return if (results.isEmpty()) listOf("") else results
    }

    private fun toBinaryBitmap(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = androidx.core.graphics.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val luminance = (0.299f * r + 0.587f * g + 0.114f * b)
            pixels[i] = if (luminance < 210f) Color.BLACK else Color.WHITE
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun cropToInk(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = pixels[y * width + x]
                if (Color.red(color) < 240 || Color.green(color) < 240 || Color.blue(color) < 240) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return source
        }

        val padding = 24
        val cropLeft = (minX - padding).coerceAtLeast(0)
        val cropTop = (minY - padding).coerceAtLeast(0)
        val cropRight = (maxX + padding).coerceAtMost(width - 1)
        val cropBottom = (maxY + padding).coerceAtMost(height - 1)

        val cropWidth = (cropRight - cropLeft + 1).coerceAtLeast(1)
        val cropHeight = (cropBottom - cropTop + 1).coerceAtLeast(1)
        return Bitmap.createBitmap(source, cropLeft, cropTop, cropWidth, cropHeight)
    }

    private fun scaleUpIfNeeded(source: Bitmap): Bitmap {
        val maxSide = maxOf(source.width, source.height)
        if (maxSide >= 1400) {
            return source
        }

        val scale = 1400f / maxSide.toFloat()
        val newWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (source.height * scale).toInt().coerceAtLeast(1)
        return source.scale(newWidth, newHeight, true)
    }

    private fun normalizeInkToCanvas(source: Bitmap): Bitmap {
        val targetSize = 1400
        val output = androidx.core.graphics.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        canvas.drawColor(Color.WHITE)

        val maxSide = maxOf(source.width, source.height).toFloat().coerceAtLeast(1f)
        val contentSize = targetSize * 0.78f
        val scale = contentSize / maxSide
        val drawW = source.width * scale
        val drawH = source.height * scale
        val left = (targetSize - drawW) / 2f
        val top = (targetSize - drawH) / 2f
        val dst = android.graphics.RectF(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(source, null, dst, null)
        return output
    }

    private fun pickBestCandidate(candidates: List<String>, targetWord: String): String {
        if (candidates.isEmpty()) {
            return ""
        }

        val normalizedTarget = normalizeWord(targetWord)
        return candidates.minByOrNull { candidate ->
            levenshteinDistance(normalizeWord(candidate), normalizedTarget)
        } ?: candidates.first()
    }

    private fun isTargetMatched(candidates: List<String>, targetWord: String): Boolean {
        val normalizedTarget = normalizeWord(targetWord)
        if (normalizedTarget.isEmpty()) {
            return false
        }

        return candidates.any { raw ->
            val candidate = normalizeWord(raw)
            if (candidate.isEmpty()) {
                return@any false
            }

            // 完全匹配
            if (candidate == normalizedTarget) {
                return@any true
            }

            // 只允許極小的編輯距離（1-2個字的差異）
            // 不允許子字符串匹配
            val distance = levenshteinDistance(candidate, normalizedTarget)

            // 字數必須相同或相差1
            val lengthDiff = candidate.length - normalizedTarget.length
            if (lengthDiff.coerceIn(-1, 1) != lengthDiff) {
                return@any false
            }

            // 根據目標字數設定容許的編輯距離
            val tolerance = when {
                normalizedTarget.length == 1 -> 0  // 單字：必須完全一致
                normalizedTarget.length == 2 -> 1  // 雙字：容許1個字差異
                else -> 1  // 三字以上：容許1個字差異
            }
            distance <= tolerance
        }
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)

        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,
                    curr[j - 1] + 1,
                    prev[j - 1] + cost
                )
            }
            for (j in 0..b.length) {
                prev[j] = curr[j]
            }
        }

        return prev[b.length]
    }

    private fun normalizeWord(value: String): String {
        return value
            .lowercase(Locale.getDefault())
            .replace("\\s+".toRegex(), "")
            .replace("[，。！？、；：,.!?;:\\-_=+\\[\\]{}()（）『』「」\"'`~]".toRegex(), "")
    }

    private fun speakCurrentWord() {
        val word = _uiState.value.writingWordsRound.getOrNull(_uiState.value.writingCurrentIndex) ?: return
        ensureTextToSpeech()
        if (!ttsReady) {
            return
        }
        textToSpeech?.setSpeechRate(_uiState.value.writingSpeechRate)
        textToSpeech?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word_${System.currentTimeMillis()}")
    }

    private fun ensureTextToSpeech() {
        if (textToSpeech != null) {
            return
        }

        textToSpeech = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val tts = textToSpeech ?: return@TextToSpeech
                val localeResult = tts.setLanguage(Locale.TRADITIONAL_CHINESE)
                if (localeResult == TextToSpeech.LANG_MISSING_DATA || localeResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
                }
                ttsReady = true
                // 如果在初始化完成時已經在測試中，補上第一次朗讀
                if (_uiState.value.isWritingTestActive && !_uiState.value.isWritingRoundFinished) {
                    speakCurrentWord()
                }
            }
        }
    }

    override fun onCleared() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textRecognizer.close()
        super.onCleared()
    }
}
