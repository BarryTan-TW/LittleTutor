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
import com.littletutor.app.ui.home.HomeScreen
import com.littletutor.app.ui.writing.WritingTestScreen
import com.littletutor.app.ui.unit.AddTestUnitDialog
import com.littletutor.app.ui.unit.EditTestUnitDialog
import com.littletutor.app.ui.photo.DirectSelectPreviewDialog
import com.littletutor.app.ui.history.ScoreHistoryDialog
import com.littletutor.app.ui.settings.SettingsDialog

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
