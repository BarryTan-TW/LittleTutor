package com.littletutor.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littletutor.app.ui.tutor.TestUnit
import kotlin.math.roundToInt

@Composable
internal fun HomeScreen(
    testUnits: List<TestUnit>,
    latestAccuracyByUnitTitle: Map<String, Float>,
    innerPadding: PaddingValues,
    onAddUnit: () -> Unit,
    onStartTest: (String) -> Unit,
    onOpenHistory: (String) -> Unit,
    onEditUnit: (String) -> Unit,
    onMoveUnit: (unitId: String, up: Boolean) -> Unit,
    onDeleteUnit: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        if (testUnits.isEmpty()) {
            EmptyHome(onAddUnit = onAddUnit, modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "我的課文（${testUnits.size}）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            itemsIndexed(testUnits, key = { _, unit -> unit.id }) { index, unit ->
                UnitCard(
                    unit = unit,
                    latestAccuracy = latestAccuracyByUnitTitle[unit.title],
                    canMoveUp = index > 0,
                    canMoveDown = index < testUnits.lastIndex,
                    onStart = { onStartTest(unit.id) },
                    onHistory = { onOpenHistory(unit.id) },
                    onEdit = { onEditUnit(unit.id) },
                    onMoveUp = { onMoveUnit(unit.id, true) },
                    onMoveDown = { onMoveUnit(unit.id, false) },
                    onDelete = { onDeleteUnit(unit.id) }
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddUnit,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(text = "新增課文") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun EmptyHome(onAddUnit: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "📚", fontSize = 64.sp)
        Text(text = "還沒有課文", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = "新增一個課文單元，可以手動輸入字詞，或拍下課本直接圈選。",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onAddUnit, modifier = Modifier.heightIn(min = 56.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = "新增課文單元", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun UnitCard(
    unit: TestUnit,
    latestAccuracy: Float?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onStart: () -> Unit,
    onHistory: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = unit.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多操作")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        MenuItem("測試歷史", Icons.Filled.List) { menuExpanded = false; onHistory() }
                        MenuItem("編輯內容", Icons.Filled.Edit) { menuExpanded = false; onEdit() }
                        MenuItem("上移", Icons.Filled.KeyboardArrowUp, enabled = canMoveUp) { menuExpanded = false; onMoveUp() }
                        MenuItem("下移", Icons.Filled.KeyboardArrowDown, enabled = canMoveDown) { menuExpanded = false; onMoveDown() }
                        HorizontalDivider()
                        MenuItem("刪除", Icons.Filled.Delete, tint = MaterialTheme.colorScheme.error) {
                            menuExpanded = false; onDelete()
                        }
                    }
                }
            }
            Text(
                text = unit.words.joinToString("、"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 12.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp, end = 12.dp)
            ) {
                Text(text = "${unit.words.size} 個字詞", style = MaterialTheme.typography.labelLarge)
                if (latestAccuracy != null) {
                    ScoreBadge(accuracy = latestAccuracy, onClick = onHistory)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onStart, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(text = "開始", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun ScoreBadge(accuracy: Float, onClick: () -> Unit) {
    val percent = (accuracy * 100).roundToInt()
    val (container, content) = when {
        percent >= 90 -> Color(0xFFC8E6C9) to Color(0xFF1B5E20)
        percent >= 60 -> Color(0xFFFFF3C4) to Color(0xFF8D6E00)
        else -> Color(0xFFFFCDD2) to Color(0xFFB71C1C)
    }
    Surface(onClick = onClick, color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(
            text = "最近 $percent%",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text = text, color = if (tint != Color.Unspecified && enabled) tint else Color.Unspecified) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = if (tint != Color.Unspecified) tint else MaterialTheme.colorScheme.onSurfaceVariant) },
        enabled = enabled,
        onClick = onClick
    )
}
