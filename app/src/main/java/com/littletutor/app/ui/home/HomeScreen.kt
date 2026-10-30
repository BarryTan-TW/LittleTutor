package com.littletutor.app.ui.home

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
internal fun HomeScreen(
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
