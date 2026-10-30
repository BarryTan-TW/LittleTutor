package com.littletutor.app.ui.settings

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
internal fun SettingsDialog(
    users: List<UserSpace>,
    currentUserId: String?,
    newUserName: String,
    testingMode: TestingMode,
    onNewUserNameChange: (String) -> Unit,
    onAddUser: () -> Unit,
    onSwitchUser: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    onSetTestingMode: (TestingMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss, 
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "設定", style = MaterialTheme.typography.headlineSmall)
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState)
                            .simpleVerticalScrollbar(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(value = newUserName, onValueChange = onNewUserNameChange, label = { Text("新使用者名稱") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = onAddUser, modifier = Modifier.fillMaxWidth()) { Text(text = "新增使用者") }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "切換 / 刪除使用者", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        users.forEach { user ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = user.displayName + if (user.id == currentUserId) "（目前）" else "", modifier = Modifier.weight(1f))
                                TextButton(onClick = { onSwitchUser(user.id) }) { Text(text = "切換") }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(onClick = { onDeleteUser(user.id) }) { Text(text = "刪除") }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "測試方式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onSetTestingMode(TestingMode.WRITING_BOARD) },
                                modifier = Modifier.weight(1f),
                                colors = if (testingMode == TestingMode.WRITING_BOARD) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                            ) { Text(text = "畫板書寫") }
                            Button(
                                onClick = { onSetTestingMode(TestingMode.MANUAL_CONFIRM) },
                                modifier = Modifier.weight(1f),
                                colors = if (testingMode == TestingMode.MANUAL_CONFIRM) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                            ) { Text(text = "自行確認") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "關於", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = "作者：Barry Tan")
                        Text(text = "Email：shihhong.tan@gmail.com")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismiss) { Text(text = "完成") }
                    }
                }
            }
        }
    }
}
