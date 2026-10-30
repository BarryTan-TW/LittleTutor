package com.littletutor.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.littletutor.app.ui.common.ConfirmDialog
import com.littletutor.app.ui.common.FullScreenDialog
import com.littletutor.app.ui.tutor.TestingMode
import com.littletutor.app.ui.tutor.TutorViewModel
import com.littletutor.app.ui.tutor.UserSpace

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
    var pendingDelete by remember { mutableStateOf<UserSpace?>(null) }

    FullScreenDialog(
        title = "設定",
        onDismiss = onDismiss,
        actions = { TextButton(onClick = onDismiss) { Text(text = "完成") } }
    ) { contentModifier ->
        Box(
            modifier = contentModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                UsersSection(
                    users = users,
                    currentUserId = currentUserId,
                    newUserName = newUserName,
                    onNewUserNameChange = onNewUserNameChange,
                    onAddUser = onAddUser,
                    onSwitchUser = onSwitchUser,
                    onRequestDelete = { pendingDelete = it }
                )
                TestingModeSection(testingMode = testingMode, onSetTestingMode = onSetTestingMode)
                AboutSection()
            }
        }
    }

    pendingDelete?.let { user ->
        ConfirmDialog(
            title = "刪除「${user.displayName}」？",
            message = "這位使用者的所有課程、照片和成績都會一起刪除，而且無法復原。",
            confirmText = "刪除",
            onConfirm = { onDeleteUser(user.id) },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun UsersSection(
    users: List<UserSpace>,
    currentUserId: String?,
    newUserName: String,
    onNewUserNameChange: (String) -> Unit,
    onAddUser: () -> Unit,
    onSwitchUser: (String) -> Unit,
    onRequestDelete: (UserSpace) -> Unit
) {
    val trimmedName = newUserName.trim()
    val isDuplicate = trimmedName.isNotEmpty() && TutorViewModel.isDuplicateUserName(trimmedName, users)
    val canAdd = trimmedName.isNotEmpty() && !isDuplicate
    val canDelete = users.size > 1

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("使用者")
        Text(
            text = "每位使用者有自己的課程、照片和成績。點一下名字就能切換。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            users.forEach { user ->
                val isCurrent = user.id == currentUserId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = isCurrent, role = Role.RadioButton, onClick = { onSwitchUser(user.id) }),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isCurrent, onClick = null, modifier = Modifier.padding(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = user.displayName, style = MaterialTheme.typography.titleMedium)
                            if (isCurrent) {
                                Text(
                                    text = "使用中",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (canDelete) {
                            IconButton(onClick = { onRequestDelete(user) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "刪除 ${user.displayName}")
                            }
                        }
                    }
                }
            }
        }
        if (!canDelete) {
            Text(
                text = "至少要保留一位使用者。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newUserName,
                onValueChange = onNewUserNameChange,
                label = { Text("新使用者名稱") },
                singleLine = true,
                isError = isDuplicate,
                supportingText = if (isDuplicate) {
                    { Text("已經有這個名字了") }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canAdd) onAddUser() }),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onAddUser,
                enabled = canAdd,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .heightIn(min = 48.dp)
            ) { Text(text = "新增") }
        }
    }
}

@Composable
private fun TestingModeSection(testingMode: TestingMode, onSetTestingMode: (TestingMode) -> Unit) {
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("聽寫方式")
        ModeOption(
            title = "自行確認",
            description = "孩子寫在紙上，家長看過後按 ✓ 或 ✗。",
            selected = testingMode == TestingMode.MANUAL_CONFIRM,
            onClick = { onSetTestingMode(TestingMode.MANUAL_CONFIRM) }
        )
        ModeOption(
            title = "畫板書寫",
            description = "孩子直接在螢幕上寫，App 自動辨識對錯。",
            selected = testingMode == TestingMode.WRITING_BOARD,
            onClick = { onSetTestingMode(TestingMode.WRITING_BOARD) }
        )
    }
}

@Composable
private fun ModeOption(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    Column(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SectionTitle("關於")
        Text(text = "LittleTutor 小老師" + (versionName?.let { " v$it" } ?: ""))
        Text(text = "作者：Barry Tan")
        Text(text = "Email：shihhong.tan@gmail.com")
    }
}
