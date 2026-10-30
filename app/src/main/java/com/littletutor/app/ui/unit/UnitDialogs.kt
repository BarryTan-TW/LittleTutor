package com.littletutor.app.ui.unit

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
import com.littletutor.app.ui.photo.PhotoThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddTestUnitDialog(
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
internal fun EditTestUnitDialog(
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
