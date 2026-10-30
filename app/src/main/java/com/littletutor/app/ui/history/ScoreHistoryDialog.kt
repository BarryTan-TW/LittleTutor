package com.littletutor.app.ui.history

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
internal fun ScoreHistoryDialog(
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
