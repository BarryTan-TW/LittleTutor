package com.littletutor.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littletutor.app.ui.common.ConfirmDialog
import com.littletutor.app.ui.common.FullScreenDialog
import com.littletutor.app.ui.common.ZhuyinText
import com.littletutor.app.ui.common.isWideLayout
import com.littletutor.app.ui.tutor.TestRecord
import com.littletutor.app.ui.tutor.WordStatistics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GoodColor = Color(0xFF2E7D32)
private val WeakColor = Color(0xFFC62828)

@Composable
internal fun ScoreHistoryDialog(
    unitTitle: String?,
    testRecords: List<TestRecord>,
    wordStatistics: List<WordStatistics>,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    FullScreenDialog(
        title = if (unitTitle != null) "成績・$unitTitle" else "全部成績",
        onDismiss = onDismiss,
        actions = {
            if (testRecords.isNotEmpty()) {
                TextButton(onClick = { showClearConfirm = true }) {
                    Text(text = "清除紀錄", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (testRecords.isEmpty()) {
                EmptyHistory()
                return@Box
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1100.dp)
            ) {
                SummaryRow(testRecords, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                if (isWideLayout()) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentPadding = PaddingValues(start = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item { SectionTitle("考試紀錄（${testRecords.size}）") }
                            recordItems(testRecords)
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentPadding = PaddingValues(end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item { SectionTitle("字詞分析（錯最多的在前）") }
                            wordItems(wordStatistics)
                        }
                    }
                } else {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(text = "考試紀錄（${testRecords.size}）") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(text = "字詞分析") }
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedTab == 0) recordItems(testRecords) else wordItems(wordStatistics)
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "清除成績紀錄？",
            message = if (unitTitle != null) "將刪除「$unitTitle」的所有考試紀錄，無法復原。" else "將刪除所有考試紀錄，無法復原。",
            confirmText = "清除",
            onConfirm = onClearHistory,
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "📝", fontSize = 48.sp)
        Text(text = "還沒有考試紀錄", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "完成一輪聽寫後，成績會自動出現在這裡。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryRow(records: List<TestRecord>, modifier: Modifier) {
    val average = records.map { it.getAccuracy() }.average().toFloat()
    val best = records.maxOf { it.getAccuracy() }
    val latest = records.maxBy { it.timestamp }.getAccuracy()
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryTile("測驗次數", "${records.size}", Modifier.weight(1f))
        SummaryTile("最近", percent(latest), Modifier.weight(1f))
        SummaryTile("平均", percent(average), Modifier.weight(1f))
        SummaryTile("最佳", percent(best), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

private fun LazyListScope.recordItems(records: List<TestRecord>) {
    items(records, key = { it.id }) { record -> RecordCard(record) }
}

private fun LazyListScope.wordItems(stats: List<WordStatistics>) {
    items(stats, key = { it.word }) { stat -> WordStatCard(stat) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordCard(record: TestRecord) {
    var expanded by rememberSaveable(record.id) { mutableStateOf(false) }
    val wrongWords = record.results.filterNot { it.isCorrect }.map { it.word }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN) }
    val canExpand = record.results.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (canExpand) Modifier.clickable { expanded = !expanded } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.unitTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateFormat.format(Date(record.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = percent(record.getAccuracy()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (record.getAccuracy() >= 0.6f) GoodColor else WeakColor
                    )
                    Text(text = "${record.correctCount} / ${record.totalCount}", style = MaterialTheme.typography.labelMedium)
                }
                if (canExpand) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "收合" else "展開"
                    )
                }
            }
            LinearProgressIndicator(
                progress = { record.getAccuracy() },
                modifier = Modifier.fillMaxWidth(),
                color = if (record.getAccuracy() >= 0.6f) GoodColor else WeakColor
            )
            if (expanded) {
                if (wrongWords.isEmpty()) {
                    Text(text = "全部答對 🎉", style = MaterialTheme.typography.bodyMedium, color = GoodColor)
                } else {
                    Text(text = "寫錯的字詞", style = MaterialTheme.typography.labelLarge, color = WeakColor)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        wrongWords.forEach { word ->
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFEBEE)) {
                                Text(
                                    text = word,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordStatCard(stat: WordStatistics) {
    val wrong = stat.totalAttempts - stat.correctCount
    val weak = stat.getErrorRate() > 0.5f
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (wrong > 0) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZhuyinText(text = stat.word, fontSize = 22.sp, modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "錯 ${percent(stat.getErrorRate())}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (weak) WeakColor else if (wrong > 0) Color(0xFFE65100) else GoodColor
                )
                Text(text = "✓ ${stat.correctCount}　✗ $wrong", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun percent(value: Float): String = "${(value * 100).toInt()}%"
