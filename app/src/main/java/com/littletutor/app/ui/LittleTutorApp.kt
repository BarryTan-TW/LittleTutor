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
    var showDeleteUnitConfirm by rememberSaveable { mutableStateOf(false) }
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
                viewModel = viewModel,
                uiState = uiState,
                innerPadding = innerPadding,
                onReplayWord = viewModel::replayCurrentWord,
                onToggleAnswerVisibility = viewModel::toggleWritingAnswerVisibility,
                onEvaluateWritingBitmap = viewModel::evaluateWritingBitmap,
                onClearRecognition = viewModel::clearWritingRecognition,
                onNextWord = viewModel::nextWritingWord,
                onRestartRound = viewModel::restartWritingRound,
                onFinishTest = { showExitTestConfirm = true },
                onUpdateSpeechRate = viewModel::updateWritingSpeechRate
            )
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
            ConfirmDialog(
                title = "確認刪除",
                message = "確定要刪除選取的課文單元及其測試紀錄嗎？這項動作無法復原。",
                confirmText = "確定刪除",
                onConfirm = viewModel::deleteSelectedTestUnits,
                onDismiss = { showDeleteUnitConfirm = false }
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
