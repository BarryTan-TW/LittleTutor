package com.littletutor.app.ui.common

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

// Draws a simple vertical scrollbar on the right edge of a scrollable Column.
internal fun Modifier.simpleVerticalScrollbar(
    state: androidx.compose.foundation.ScrollState,
    width: Dp = 4.dp,
    color: Color = Color(0xFF9E9E9E)
): Modifier = this.drawWithContent {
    drawContent()
    val contentHeight = state.maxValue + size.height
    if (state.maxValue <= 0) return@drawWithContent
    val thumbHeight = (size.height / contentHeight) * size.height
    val thumbTop = (state.value.toFloat() / state.maxValue) * (size.height - thumbHeight)
    val barWidth = width.toPx()
    drawRect(
        color = color.copy(alpha = 0.3f),
        topLeft = Offset(size.width - barWidth, 0f),
        size = androidx.compose.ui.geometry.Size(barWidth, size.height)
    )
    drawRect(
        color = color,
        topLeft = Offset(size.width - barWidth, thumbTop),
        size = androidx.compose.ui.geometry.Size(barWidth, thumbHeight.coerceAtLeast(24f))
    )
}

/**
 * 注音顯示組件
 * 每個中文字上方顯示對應的注音。
 */
@Composable
fun ZhuyinText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        text.forEach { char ->
            val zhuyin = ZhuyinHelper.getZhuyin(char)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.padding(horizontal = 1.dp)
            ) {
                if (zhuyin.isNotEmpty()) {
                    Text(
                        text = zhuyin,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = (fontSize.value * 0.4f).sp,
                        lineHeight = (fontSize.value * 0.4f).sp,
                        modifier = Modifier.padding(bottom = 0.dp)
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
