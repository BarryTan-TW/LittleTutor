package com.littletutor.app.ui.photo

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
internal fun PhotoThumbnail(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DirectSelectPreviewDialog(
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
