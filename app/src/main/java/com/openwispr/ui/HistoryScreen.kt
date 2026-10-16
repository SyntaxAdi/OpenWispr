package com.openwispr.ui

import android.content.Context
import android.os.Build
import android.widget.Toast
import com.openwispr.util.TextInjector
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
private val ColorCardElevated = Color(0xFF101014)
private val ColorBorder = Color(0xFF141416)
private val ColorRedAccent = Color(0xFFFF2E2E)
private val ColorMuted = Color(0xFF8E95A2)
private val ColorSubtle = Color(0xFF555B66)


@Composable
fun HistoryScreen(
    historyRepo: HistoryRepository,
    onOpenDrawer: () -> Unit,
    onBackToHome: () -> Unit = onOpenDrawer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var transcripts by remember { mutableStateOf<List<TranscriptRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    androidx.activity.compose.BackHandler(onBack = onBackToHome)

    fun refresh() {
        scope.launch {
            try {
                transcripts = historyRepo.getAllTranscripts()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(historyRepo) {
        refresh()
        historyRepo.dataUpdates.collect {
            refresh()
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Left drawer menu button - 44dp squircle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ColorCardBg)
                            .border(1.dp, ColorBorder, RoundedCornerShape(10.dp))
                            .clickable { onOpenDrawer() }
                            .align(Alignment.CenterStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu),
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center Title: Dot Matrix HISTORY with safe padding (Zero Overlap Guaranteed)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 72.dp)
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        DotMatrixText(
                            text = "HISTORY",
                            dotSize = 2.8.dp,
                            dotSpacing = 0.9.dp,
                            charSpacing = 1.8.dp,
                            color = Color.White
                        )
                    }

                    // Right Badge: Telemetry record count
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ColorCardElevated)
                            .border(1.dp, ColorBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .align(Alignment.CenterEnd)
                    ) {
                        Text(
                            text = if (isLoading) "SYNCING..." else "${transcripts.size} RECORDS",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ColorSubtle,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }



            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ColorRedAccent,
                            strokeWidth = 2.dp
                        )
                    }
                }
            } else if (transcripts.isEmpty()) {
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
                    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                    val appDisplayName = remember(item.appPackage) {
                        if (item.appPackage.isBlank() || item.appPackage == "unknown") {
                            "SYSTEM"
                        } else {
                            val pm = context.packageManager
                            try {
                                val appInfo = pm.getApplicationInfo(item.appPackage, 0)
                                pm.getApplicationLabel(appInfo).toString().uppercase()
                            } catch (_: Exception) {
                                item.appPackage.substringAfterLast('.').uppercase()
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorCardBg)
                            .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Top Row: App Label + Word Count Badge + Timestamp
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = appDisplayName,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White,
                                        letterSpacing = 0.5.sp
                                    )

                                    if (item.wordCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF1A1B22))
                                                .border(0.6.dp, ColorBorder, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "${item.wordCount} WORDS",
                                                fontSize = 7.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = ColorRedAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = timeFormat.format(Date(item.timestampMs)),
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ColorSubtle
                                )
                            }

                            // Dictation Text with Left Red Rail Guide
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(ColorRedAccent.copy(alpha = 0.5f))
                                )

                                Text(
                                    text = item.text,
                                    fontSize = 12.5.sp,
                                    color = Color.White,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Bottom Micro-Actions Bar: Duration & Direct Copy / Delete Pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val durationSec = if (item.durationMs > 0) item.durationMs / 1000f else 0f
                                Text(
                                    text = if (durationSec > 0) "// AUDIO: ${"%.1f".format(durationSec)}S" else "// DIRECT INJECT",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ColorSubtle
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Copy Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFF14151C))
                                            .border(0.8.dp, ColorBorder, RoundedCornerShape(5.dp))
                                            .clickable {
                                                TextInjector.copyTranscription(context, item.text)
                                            }
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_copy),
                                                contentDescription = "Copy transcription",
                                                tint = ColorMuted,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "COPY",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorMuted
                                            )
                                        }
                                    }

                                    // Delete Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFF1E1418))
                                            .border(0.8.dp, ColorRedAccent.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                                            .clickable {
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
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_delete),
                                                contentDescription = "Delete transcription",
                                                tint = ColorRedAccent,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "DELETE",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorRedAccent
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
    val todayStartMs = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    return items.groupBy { if (it.timestampMs >= todayStartMs) "Today" else dateFormat.format(Date(it.timestampMs)) }
}
