package com.littletutor.app.ui.unit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littletutor.app.ui.common.ConfirmDialog
import com.littletutor.app.ui.common.FullScreenDialog
import com.littletutor.app.ui.photo.PhotoThumbnail
import com.littletutor.app.ui.tutor.UnitInput

@Composable
internal fun AddTestUnitDialog(
    title: String,
    words: String,
    otherTitles: List<String>,
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
    if (isPhotoAreaOpen) {
        FullScreenDialog(
            title = "從課本照片建立",
            onDismiss = onClosePhotoArea,
            navigationIsBack = true
        ) { modifier ->
            PhotoLibrary(
                modifier = modifier,
                photoPaths = photoPaths,
                selectedPhotoPaths = selectedPhotoPaths,
                onTakePhoto = onTakePhoto,
                onOpenPhotoPreview = onOpenPhotoPreview,
                onTogglePhotoSelection = onTogglePhotoSelection,
                onDeleteSelectedPhotos = onDeleteSelectedPhotos
            )
        }
        return
    }

    val canSave = UnitInput.titleError(title, otherTitles) == null && UnitInput.parseWords(words).isNotEmpty()
    FullScreenDialog(
        title = "新增課文",
        onDismiss = onDismiss,
        actions = { TextButton(onClick = onSave, enabled = canSave) { Text(text = "儲存") } }
    ) { modifier ->
        UnitForm(
            modifier = modifier,
            title = title,
            words = words,
            otherTitles = otherTitles,
            onTitleChange = onTitleChange,
            onWordsChange = onWordsChange,
            header = {
                Card(
                    onClick = onOpenPhotoArea,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "📷", fontSize = 32.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "拍課本，直接圈選字詞", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "自動辨識照片中的文字，點選或圈選即可加入課文", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Text(text = "或手動輸入", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        )
    }
}

@Composable
internal fun EditTestUnitDialog(
    title: String,
    words: String,
    otherTitles: List<String>,
    onTitleChange: (String) -> Unit,
    onWordsChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val canSave = UnitInput.titleError(title, otherTitles) == null && UnitInput.parseWords(words).isNotEmpty()
    FullScreenDialog(
        title = "編輯課文",
        onDismiss = onDismiss,
        actions = { TextButton(onClick = onSave, enabled = canSave) { Text(text = "儲存") } }
    ) { modifier ->
        UnitForm(
            modifier = modifier,
            title = title,
            words = words,
            otherTitles = otherTitles,
            onTitleChange = onTitleChange,
            onWordsChange = onWordsChange
        )
    }
}

@Composable
private fun UnitForm(
    modifier: Modifier,
    title: String,
    words: String,
    otherTitles: List<String>,
    onTitleChange: (String) -> Unit,
    onWordsChange: (String) -> Unit,
    header: @Composable () -> Unit = {}
) {
    // Only flag the title once the user has typed something, so a fresh form isn't red.
    val titleError = UnitInput.titleError(title, otherTitles).takeIf { title.isNotEmpty() }
    val wordCount = UnitInput.parseWords(words).size

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            header()
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(text = "課文名稱") },
                placeholder = { Text(text = "例如：第一課 春天來了") },
                singleLine = true,
                isError = titleError != null,
                supportingText = titleError?.let { { Text(text = it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = words,
                onValueChange = onWordsChange,
                label = { Text(text = "字詞") },
                placeholder = { Text(text = "春天、花朵\n蝴蝶") },
                supportingText = { Text(text = "用「、」「，」或換行分隔　目前 $wordCount 個字詞") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                minLines = 8
            )
        }
    }
}

@Composable
private fun PhotoLibrary(
    modifier: Modifier,
    photoPaths: List<String>,
    selectedPhotoPaths: Set<String>,
    onTakePhoto: () -> Unit,
    onOpenPhotoPreview: (String) -> Unit,
    onTogglePhotoSelection: (String) -> Unit,
    onDeleteSelectedPhotos: () -> Unit
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onTakePhoto, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "拍照")
            }
            if (selectedPhotoPaths.isNotEmpty()) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "刪除 ${selectedPhotoPaths.size} 張")
                }
            }
        }

        if (photoPaths.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Text(text = "📖", fontSize = 56.sp)
                Text(
                    text = "按「拍照」拍下課本頁面，\n拍完會自動開啟圈選畫面。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = "點照片開始圈選字詞；勾選右上角可刪除照片",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 112.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photoPaths, key = { it }) { photoPath ->
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

    if (confirmDelete) {
        ConfirmDialog(
            title = "刪除照片？",
            message = "將刪除 ${selectedPhotoPaths.size} 張照片（已建立的字詞不受影響）。",
            confirmText = "刪除",
            onConfirm = onDeleteSelectedPhotos,
            onDismiss = { confirmDelete = false }
        )
    }
}
