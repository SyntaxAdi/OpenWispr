package com.openwispr.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openwispr.R
import com.openwispr.data.HistoryRepository
import com.openwispr.data.TranscriptRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ColorDarkBg = Color(0xFF000000)
private val ColorCardBg = Color(0xFF070709)
private val ColorBorder = Color(0xFF141416)
private val ColorRedAccent = Color(0xFFFF2E2E)
private val ColorMuted = Color(0xFF8E95A2)
private val ColorSubtle = Color(0xFF555B66)

@Composable
fun HistoryScreen(
    historyRepo: HistoryRepository,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var transcripts by remember { mutableStateOf<List<TranscriptRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        historyRepo.dataUpdates.collect {
            try {
                transcripts = historyRepo.getAllTranscripts()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    val grouped = remember(transcripts) {
        groupTranscriptsByDate(transcripts)
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorDarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = topInset + 16.dp, bottom = bottomInset + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ColorCardBg)
                                .border(1.dp, ColorBorder, RoundedCornerShape(10.dp))
                                .clickable { onBackToHome() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "←",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "DICTATION HISTORY",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "${transcripts.size} records",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ColorSubtle
                    )
                }
            }

            if (!isLoading && transcripts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ColorCardBg)
                            .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No history records found.",
                            fontSize = 13.sp,
                            color = ColorMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Grouped items
            grouped.forEach { (dateHeader, itemsForDate) ->
                item {
                    Text(
                        text = dateHeader,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ColorRedAccent,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }

                items(itemsForDate, key = { it.id }) { item ->
                    var menuOpen by remember { mutableStateOf(false) }
                    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ColorCardBg)
                            .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = item.text,
                                fontSize = 13.5.sp,
                                color = Color.White,
                                lineHeight = 19.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timeFormat.format(Date(item.timestampMs)),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ColorSubtle
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_copy),
                                        contentDescription = "Copy",
                                        tint = ColorMuted,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Transcription", item.text).apply {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        description.extras = PersistableBundle().apply {
                                                            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                                                        }
                                                    }
                                                }
                                                clipboard.setPrimaryClip(clip)
                                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    )
                                    Box {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_more_vert),
                                            contentDescription = "Options",
                                            tint = ColorMuted,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { menuOpen = true }
                                        )
                                        DropdownMenu(
                                            expanded = menuOpen,
                                            onDismissRequest = { menuOpen = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Delete", color = ColorRedAccent) },
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_delete),
                                                        contentDescription = null,
                                                        tint = ColorRedAccent
                                                    )
                                                },
                                                onClick = {
                                                    menuOpen = false
                                                    scope.launch {
                                                        try {
                                                            historyRepo.deleteTranscript(item.id)
                                                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                            Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun groupTranscriptsByDate(items: List<TranscriptRecord>): Map<String, List<TranscriptRecord>> {
    val calToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayStartMs = calToday.timeInMillis

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    val map = linkedMapOf<String, MutableList<TranscriptRecord>>()

    items.forEach { record ->
        val header = if (record.timestampMs >= todayStartMs) {
            "Today"
        } else {
            dateFormat.format(Date(record.timestampMs))
        }
        map.getOrPut(header) { mutableListOf() }.add(record)
    }

    return map
}
