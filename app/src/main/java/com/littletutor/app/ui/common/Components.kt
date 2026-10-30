package com.littletutor.app.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.littletutor.app.ui.tutor.ZhuyinHelper

/** Material window width classes (compact < 600dp <= medium < 840dp <= expanded). */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

@Composable
@ReadOnlyComposable
fun currentWidthClass(): WidthClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> WidthClass.COMPACT
        widthDp < 840 -> WidthClass.MEDIUM
        else -> WidthClass.EXPANDED
    }
}

/** True when there is room to lay panes side by side (tablets, or phones in landscape). */
@Composable
@ReadOnlyComposable
fun isWideLayout(): Boolean {
    val config = LocalConfiguration.current
    return currentWidthClass() != WidthClass.COMPACT || config.screenWidthDp > config.screenHeightDp
}

// Draws a simple vertical scrollbar on the right edge of a scrollable Column.
fun Modifier.simpleVerticalScrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    color: Color = Color(0xFF9E9E9E)
): Modifier = this.drawWithContent {
    drawContent()
    if (state.maxValue <= 0) return@drawWithContent
    val contentHeight = state.maxValue + size.height
    val thumbHeight = ((size.height / contentHeight) * size.height).coerceAtLeast(24f)
    val thumbTop = (state.value.toFloat() / state.maxValue) * (size.height - thumbHeight)
    val barWidth = width.toPx()
    drawRect(
        color = color.copy(alpha = 0.3f),
        topLeft = Offset(size.width - barWidth, 0f),
        size = Size(barWidth, size.height)
    )
    drawRect(
        color = color,
        topLeft = Offset(size.width - barWidth, thumbTop),
        size = Size(barWidth, thumbHeight)
    )
}

/**
 * 注音顯示組件：每個中文字上方顯示對應的注音，過長時自動換行。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZhuyinText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    color: Color = Color.Unspecified
) {
    val zhuyinSize = (fontSize.value * 0.4f).sp
    FlowRow(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        text.forEach { char ->
            val zhuyin = ZhuyinHelper.getZhuyin(char)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .padding(horizontal = 1.dp)
            ) {
                if (zhuyin.isNotEmpty()) {
                    Text(
                        text = zhuyin,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = zhuyinSize,
                        lineHeight = zhuyinSize
                    )
                }
                Text(
                    text = char.toString(),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = fontSize,
                    color = color
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
    dismissText: String = "取消"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(
                    text = confirmText,
                    color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = dismissText) } }
    )
}

/** Full-screen dialog with a top app bar; system bars and IME insets are handled by the Scaffold. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenDialog(
    title: String,
    onDismiss: () -> Unit,
    navigationIsBack: Boolean = false,
    onNavigation: () -> Unit = onDismiss,
    actions: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onNavigation) {
                            if (navigationIsBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            } else {
                                Icon(Icons.Filled.Close, contentDescription = "關閉")
                            }
                        }
                    },
                    actions = { actions() }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            content(Modifier.padding(innerPadding))
        }
    }
}
