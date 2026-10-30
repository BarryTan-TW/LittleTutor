package com.littletutor.app.ui.photo

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.littletutor.app.ui.common.FullScreenDialog
import com.littletutor.app.ui.common.isWideLayout
import com.littletutor.app.ui.tutor.OcrToken
import com.littletutor.app.ui.tutor.PhotoBitmaps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface PhotoLoad {
    data object Loading : PhotoLoad
    data object Failed : PhotoLoad
    data class Ready(val bitmap: Bitmap) : PhotoLoad
}

/** Decodes a downsampled, EXIF-upright bitmap off the main thread. */
@Composable
private fun rememberPhotoBitmap(photoPath: String, maxDimension: Int): PhotoLoad {
    val state by produceState<PhotoLoad>(PhotoLoad.Loading, photoPath, maxDimension) {
        value = withContext(Dispatchers.IO) {
            PhotoBitmaps.decodeUpright(photoPath, maxDimension)?.let { PhotoLoad.Ready(it) } ?: PhotoLoad.Failed
        }
    }
    return state
}

@Composable
internal fun PhotoThumbnail(
    photoPath: String,
    isSelected: Boolean,
    onPreview: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val photo = rememberPhotoBitmap(photoPath, PhotoBitmaps.THUMBNAIL_MAX_DIMENSION)
    Card {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onPreview)
        ) {
            when (photo) {
                is PhotoLoad.Ready -> Image(
                    bitmap = remember(photo.bitmap) { photo.bitmap.asImageBitmap() },
                    contentDescription = "照片縮圖",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                PhotoLoad.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).align(Alignment.Center),
                    strokeWidth = 2.dp
                )
                PhotoLoad.Failed -> Text(text = "無法開啟", modifier = Modifier.align(Alignment.Center))
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
internal fun DirectSelectPreviewDialog(
    photoPath: String,
    previewTokens: List<OcrToken>,
    selectedTokenIndexes: Set<Int>,
    imageWidth: Int,
    imageHeight: Int,
    isLoading: Boolean,
    unitTitle: String,
    existingUnitTitles: List<String>,
    onToggleToken: (Int) -> Unit,
    onApplySelectedSet: (Set<Int>) -> Unit,
    onUnitTitleChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val lassoHistory = remember(photoPath) { mutableStateListOf<Set<Int>>() }
    val canSave = unitTitle.isNotBlank() && selectedTokenIndexes.isNotEmpty()

    val photoPane: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OcrStatusLine(isLoading = isLoading, tokenCount = previewTokens.size, selectedCount = selectedTokenIndexes.size)
            SelectablePhotoPreview(
                modifier = Modifier.fillMaxWidth().weight(1f),
                photoPath = photoPath,
                previewTokens = previewTokens,
                selectedTokenIndexes = selectedTokenIndexes,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                onToggleToken = onToggleToken,
                onLassoSelect = { hits ->
                    lassoHistory += selectedTokenIndexes
                    onApplySelectedSet(selectedTokenIndexes + hits)
                }
            )
            SelectionToolbar(
                enabled = previewTokens.isNotEmpty(),
                canUndo = lassoHistory.isNotEmpty(),
                onSelectAll = { onApplySelectedSet(previewTokens.indices.toSet()) },
                onInvert = { onApplySelectedSet(previewTokens.indices.toSet() - selectedTokenIndexes) },
                onClear = { onApplySelectedSet(emptySet()) },
                onUndo = { lassoHistory.removeLastOrNull()?.let(onApplySelectedSet) }
            )
        }
    }
    val detailsPane: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UnitTitleField(unitTitle = unitTitle, existingUnitTitles = existingUnitTitles, onUnitTitleChange = onUnitTitleChange)
            SelectedWords(previewTokens = previewTokens, selectedTokenIndexes = selectedTokenIndexes, onToggleToken = onToggleToken)
        }
    }

    FullScreenDialog(
        title = "圈選字詞",
        onDismiss = onDismiss,
        navigationIsBack = true,
        actions = { TextButton(onClick = onSave, enabled = canSave) { Text(text = "儲存") } }
    ) { modifier ->
        if (isWideLayout()) {
            Row(
                modifier = modifier.fillMaxSize().imePadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                photoPane(Modifier.weight(1.6f).fillMaxSize())
                detailsPane(Modifier.weight(1f).verticalScroll(rememberScrollState()))
            }
        } else {
            Column(
                modifier = modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                photoPane(Modifier.weight(1f).fillMaxWidth())
                HorizontalDivider()
                detailsPane(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()))
            }
        }
    }
}

@Composable
private fun OcrStatusLine(isLoading: Boolean, tokenCount: Int, selectedCount: Int) {
    when {
        isLoading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(text = "正在辨識文字…")
        }
        tokenCount == 0 -> Text(
            text = "照片中找不到文字，請在光線充足處把課本拍清楚再試一次。",
            color = MaterialTheme.colorScheme.error
        )
        else -> Text(
            text = "點一下選字，或用手指畫圈一次選多個字（已選 $selectedCount 個）",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionToolbar(
    enabled: Boolean,
    canUndo: Boolean,
    onSelectAll: () -> Unit,
    onInvert: () -> Unit,
    onClear: () -> Unit,
    onUndo: () -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = onSelectAll, enabled = enabled) { Text(text = "全選") }
        TextButton(onClick = onInvert, enabled = enabled) { Text(text = "反選") }
        TextButton(onClick = onClear, enabled = enabled) { Text(text = "清除") }
        TextButton(onClick = onUndo, enabled = canUndo) { Text(text = "復原圈選") }
    }
}

@Composable
private fun UnitTitleField(unitTitle: String, existingUnitTitles: List<String>, onUnitTitleChange: (String) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    val trimmed = unitTitle.trim()
    val supporting = when {
        trimmed.isEmpty() -> "請輸入新課文名稱，或從既有課文中選擇"
        existingUnitTitles.any { it.trim() == trimmed } -> "將加入到既有課文"
        else -> "將建立新課文"
    }
    Box {
        OutlinedTextField(
            value = unitTitle,
            onValueChange = onUnitTitleChange,
            label = { Text(text = "存到課文") },
            singleLine = true,
            isError = trimmed.isEmpty(),
            supportingText = { Text(text = supporting) },
            trailingIcon = if (existingUnitTitles.isEmpty()) null else {
                {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "選擇既有課文")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            existingUnitTitles.forEach { title ->
                DropdownMenuItem(
                    text = { Text(text = title) },
                    onClick = { onUnitTitleChange(title); menuExpanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedWords(previewTokens: List<OcrToken>, selectedTokenIndexes: Set<Int>, onToggleToken: (Int) -> Unit) {
    val selected = selectedTokenIndexes.sorted().filter { it in previewTokens.indices }
    Text(text = "已選字詞（${selected.size}）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    if (selected.isEmpty()) {
        Text(text = "尚未選取，點選照片上的文字即可加入", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            selected.forEach { index ->
                FilterChip(
                    selected = true,
                    onClick = { onToggleToken(index) },
                    label = { Text(text = previewTokens[index].text) },
                    trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "移除", modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
private fun SelectablePhotoPreview(
    modifier: Modifier = Modifier,
    photoPath: String,
    previewTokens: List<OcrToken>,
    selectedTokenIndexes: Set<Int>,
    imageWidth: Int,
    imageHeight: Int,
    onToggleToken: (Int) -> Unit,
    onLassoSelect: (Set<Int>) -> Unit
) {
    val photo = rememberPhotoBitmap(photoPath, PhotoBitmaps.PREVIEW_MAX_DIMENSION)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (photo) {
            PhotoLoad.Loading -> CircularProgressIndicator()
            PhotoLoad.Failed -> Text(text = "無法開啟此照片。")
            is PhotoLoad.Ready -> {
                val bitmap = photo.bitmap
                // Token boxes are relative to the OCR bitmap, which may be sized differently from this preview.
                val refWidth = (if (imageWidth > 0) imageWidth else bitmap.width).toFloat()
                val refHeight = (if (imageHeight > 0) imageHeight else bitmap.height).toFloat()
                TokenCanvas(
                    bitmap = bitmap,
                    refWidth = refWidth,
                    refHeight = refHeight,
                    previewTokens = previewTokens,
                    selectedTokenIndexes = selectedTokenIndexes,
                    onToggleToken = onToggleToken,
                    onLassoSelect = onLassoSelect
                )
            }
        }
    }
}

@Composable
private fun TokenCanvas(
    bitmap: Bitmap,
    refWidth: Float,
    refHeight: Float,
    previewTokens: List<OcrToken>,
    selectedTokenIndexes: Set<Int>,
    onToggleToken: (Int) -> Unit,
    onLassoSelect: (Set<Int>) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var lassoPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val selectedColor = MaterialTheme.colorScheme.primary
    val lassoColor = Color(0xFFFFA000)

    fun tokenRects(): List<Rect> {
        if (canvasSize.width == 0 || canvasSize.height == 0) return emptyList()
        val frame = imageFrameInCanvas(canvasSize.width.toFloat(), canvasSize.height.toFloat(), refWidth, refHeight)
        return previewTokens.map { frame.map(it) }
    }

    Box(modifier = Modifier.fillMaxSize().onSizeChanged { canvasSize = it }) {
        Image(bitmap = imageBitmap, contentDescription = "照片預覽", contentScale = ContentScale.Fit, modifier = Modifier.matchParentSize())
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(previewTokens, canvasSize, refWidth, refHeight) {
                    detectTapGestures { tap ->
                        val rects = tokenRects()
                        val hit = rects.indexOfLast { it.contains(tap) }.takeIf { it >= 0 }
                            ?: rects.withIndex()
                                .map { (i, r) -> i to distancePointToRect(tap, r) }
                                .filter { it.second <= TAP_TOLERANCE_PX }
                                .minByOrNull { it.second }?.first
                        if (hit != null) onToggleToken(hit)
                    }
                }
                .pointerInput(previewTokens, selectedTokenIndexes, canvasSize, refWidth, refHeight) {
                    detectDragGestures(
                        onDragStart = { lassoPoints = listOf(it) },
                        onDrag = { change, _ -> lassoPoints = lassoPoints + change.position; change.consume() },
                        onDragEnd = {
                            val polygon = lassoPoints
                            lassoPoints = emptyList()
                            if (polygon.size >= 3) {
                                val hits = tokenRects().withIndex()
                                    .filter { (_, r) -> isTokenHitByLasso(r, polygon) }
                                    .map { it.index }
                                    .toSet()
                                if (hits.isNotEmpty()) onLassoSelect(hits)
                            }
                        },
                        onDragCancel = { lassoPoints = emptyList() }
                    )
                }
        ) {
            val frame = imageFrameInCanvas(size.width, size.height, refWidth, refHeight)
            previewTokens.forEachIndexed { index, token ->
                val r = frame.map(token)
                val isSelected = index in selectedTokenIndexes
                if (isSelected) {
                    drawRect(color = selectedColor.copy(alpha = 0.25f), topLeft = r.topLeft, size = r.size)
                }
                drawRect(
                    color = if (isSelected) selectedColor else Color(0x664A90E2),
                    topLeft = r.topLeft,
                    size = r.size,
                    style = Stroke(width = if (isSelected) 4f else 2f)
                )
            }
            if (lassoPoints.size >= 2) {
                for (i in 0 until lassoPoints.lastIndex) {
                    drawLine(color = lassoColor, start = lassoPoints[i], end = lassoPoints[i + 1], strokeWidth = 6f)
                }
            }
        }
    }
}

private data class ImageFrame(val left: Float, val top: Float, val scale: Float) {
    fun map(token: OcrToken) = Rect(
        left + token.left * scale,
        top + token.top * scale,
        left + token.right * scale,
        top + token.bottom * scale
    )
}

private fun imageFrameInCanvas(canvasWidth: Float, canvasHeight: Float, imageWidth: Float, imageHeight: Float): ImageFrame {
    val scale = minOf(canvasWidth / imageWidth, canvasHeight / imageHeight)
    return ImageFrame((canvasWidth - imageWidth * scale) / 2f, (canvasHeight - imageHeight * scale) / 2f, scale)
}

private const val TAP_TOLERANCE_PX = 24f
