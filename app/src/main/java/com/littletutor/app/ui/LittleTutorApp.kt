package com.littletutor.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.littletutor.app.ui.common.ConfirmDialog
import com.littletutor.app.ui.history.ScoreHistoryDialog
import com.littletutor.app.ui.home.HomeScreen
import com.littletutor.app.ui.photo.DirectSelectPreviewDialog
import com.littletutor.app.ui.settings.SettingsDialog
import com.littletutor.app.ui.tutor.TutorViewModel
import com.littletutor.app.ui.unit.AddTestUnitDialog
import com.littletutor.app.ui.unit.EditTestUnitDialog
import com.littletutor.app.ui.writing.WritingTestScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LittleTutorApp(viewModel: TutorViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserName = uiState.currentUser?.displayName ?: "訪客"
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        viewModel.onPhotoCaptured(it)
    }
    var pendingDeleteUnitId by rememberSaveable { mutableStateOf<String?>(null) }
    var showExitTestConfirm by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage ?: return@LaunchedEffect
        viewModel.consumeUserMessage()
        snackbarHostState.showSnackbar(message)
    }

    // System back during a test must never silently drop the child's progress.
    BackHandler(enabled = uiState.isWritingTestActive) {
        if (uiState.isWritingRoundFinished) viewModel.finishWritingTest() else showExitTestConfirm = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!uiState.isWritingTestActive) {
                TopAppBar(
                    title = { Text(text = "小小家教", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    actions = {
                        AssistChip(
                            onClick = viewModel::openSettings,
                            label = {
                                Text(
                                    text = currentUserName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 160.dp)
                                )
                            },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
                        )
                        IconButton(onClick = viewModel::openSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "設定")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (uiState.isWritingTestActive) {
            WritingTestScreen(
                uiState = uiState,
                innerPadding = innerPadding,
                onReplayWord = viewModel::replayCurrentWord,
                onToggleAnswerVisibility = viewModel::toggleWritingAnswerVisibility,
                onEvaluateWritingBitmap = viewModel::evaluateWritingBitmap,
                onClearRecognition = viewModel::clearWritingRecognition,
                onNextWord = viewModel::nextWritingWord,
                onConfirmResult = viewModel::confirmTestResult,
                onRestartRound = viewModel::restartWritingRound,
                onRetryWrongWords = viewModel::retryWrongWords,
                onOpenHistory = { viewModel.openScoreHistory(uiState.writingUnitId) },
                onExitTest = { showExitTestConfirm = true },
                onBackHome = viewModel::finishWritingTest,
                onUpdateSpeechRate = viewModel::updateWritingSpeechRate
            )
        } else {
            HomeScreen(
                testUnits = uiState.testUnits,
                latestAccuracyByUnitTitle = uiState.latestAccuracyByUnitTitle,
                innerPadding = innerPadding,
                onAddUnit = viewModel::openAddUnitDialog,
                onStartTest = viewModel::startUnitTest,
                onOpenHistory = viewModel::openScoreHistory,
                onEditUnit = viewModel::openEditUnit,
                onMoveUnit = viewModel::moveTestUnit,
                onDeleteUnit = { pendingDeleteUnitId = it }
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
                imageWidth = uiState.previewImageWidth,
                imageHeight = uiState.previewImageHeight,
                isLoading = uiState.isPreviewTokenLoading,
                unitTitle = uiState.previewUnitTitleInput,
                existingUnitTitles = uiState.testUnits.map { it.title },
                onToggleToken = viewModel::togglePreviewToken,
                onApplySelectedSet = viewModel::setPreviewSelectedTokenIndexes,
                onUnitTitleChange = viewModel::updatePreviewUnitTitleInput,
                onSave = viewModel::savePreviewSelectionToUnit,
                onDismiss = viewModel::closePhotoPreview
            )
        }

        if (uiState.isEditUnitDialogOpen) {
            EditTestUnitDialog(
                title = uiState.editingUnitTitleInput,
                words = uiState.editingUnitWordsInput,
                otherTitles = uiState.testUnits.filter { it.id != uiState.editingUnitId }.map { it.title },
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
                otherTitles = uiState.testUnits.map { it.title },
                isPhotoAreaOpen = uiState.isAddUnitPhotoAreaOpen,
                photoPaths = uiState.photoPaths,
                selectedPhotoPaths = uiState.selectedPhotoPaths,
                onTitleChange = viewModel::updateAddingUnitTitle,
                onWordsChange = viewModel::updateAddingUnitWords,
                onOpenPhotoArea = viewModel::openAddUnitPhotoArea,
                onClosePhotoArea = viewModel::closeAddUnitPhotoArea,
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
                unitTitle = uiState.scoreHistoryUnitId?.let { id -> uiState.testUnits.firstOrNull { it.id == id }?.title },
                testRecords = uiState.testRecords,
                wordStatistics = uiState.wordStatistics,
                onClearHistory = viewModel::clearScoreHistory,
                onDismiss = viewModel::closeScoreHistory
            )
        }

        pendingDeleteUnitId?.let { unitId ->
            val unitTitle = uiState.testUnits.firstOrNull { it.id == unitId }?.title.orEmpty()
            ConfirmDialog(
                title = "刪除「$unitTitle」？",
                message = "課文單元及其測試紀錄都會被刪除，這項動作無法復原。",
                confirmText = "確定刪除",
                onConfirm = { viewModel.deleteTestUnit(unitId) },
                onDismiss = { pendingDeleteUnitId = null }
            )
        }

        if (showExitTestConfirm) {
            ConfirmDialog(
                title = "結束測試？",
                message = "目前的作答進度將不會保存。",
                confirmText = "結束測試",
                dismissText = "繼續測試",
                onConfirm = viewModel::finishWritingTest,
                onDismiss = { showExitTestConfirm = false }
            )
        }
    }
}
