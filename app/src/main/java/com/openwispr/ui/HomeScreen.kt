package com.openwispr.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openwispr.R
import com.openwispr.data.HistoryRepository
import com.openwispr.data.HomeStats
import com.openwispr.data.TranscriptRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Nothing OS Dark Theme Palette
private val ColorDarkBg = Color(0xFF070709)
private val ColorCardBg = Color(0xFF0E0F14)
private val ColorBorder = Color(0xFF1E2028)
private val ColorRedAccent = Color(0xFFFF2E2E)
private val ColorGreenActive = Color(0xFF00E676)
private val ColorMuted = Color(0xFF9CA3AF)
private val ColorSubtle = Color(0xFF6B7280)

@Composable
fun HomeScreen(
    historyRepo: HistoryRepository,
    onOpenDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    isServiceActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var stats by remember { mutableStateOf(HomeStats(0L, 0, 0, 0)) }
    var transcripts by remember { mutableStateOf<List<TranscriptRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshData() {
        scope.launch {
            stats = historyRepo.getHomeStats()
            transcripts = historyRepo.getAllTranscripts()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorDarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Top Header Bar
            item {
                TopBarHeader(
                    onOpenDrawer = onOpenDrawer,
                    onOpenSettings = onNavigateToSettings,
                    isActive = isServiceActive
                )
            }

            // 2. Card 01: TOTAL WORDS SPOKEN (Hero Card)
            item {
                CardTotalWordsSpoken(stats = stats)
            }

            // 3. Row of 2 Cards: 02 WEEK STREAK & 03 APPS TRACKED
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CardWeekStreak(stats = stats)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CardAppsTracked(stats = stats)
                    }
                }
            }

            // 4. Row of 2 Cards: 04 SPEAKING SPEED & 05 TOTAL USAGE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CardSpeakingSpeed(stats = stats)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CardTotalUsage(stats = stats)
                    }
                }
            }

            // 5. Card 06: RECENT DICTATIONS
            item {
                CardRecentDictations(
                    transcripts = transcripts,
                    onViewAll = onNavigateToHistory,
                    onCopy = { text ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Transcription", text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { id ->
                        scope.launch {
                            historyRepo.deleteTranscript(id)
                            refreshData()
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // 6. Cards 07 & 08: Upcoming Teasers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CardAccuracyTeaser()
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CardProductivityTeaser()
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarHeader(
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    isActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left hamburger button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ColorCardBg)
                .border(1.dp, ColorBorder, RoundedCornerShape(10.dp))
                .clickable { onOpenDrawer() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_menu),
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Center: Dot-Matrix Title & Subtitle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DotMatrixText("Open ", dotSize = 2.0.dp, dotSpacing = 0.8.dp, charSpacing = 1.2.dp, color = Color.White)
                DotMatrixText("Wispr", dotSize = 2.0.dp, dotSpacing = 0.8.dp, charSpacing = 1.2.dp, color = ColorRedAccent)
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "YOUR AI VOICE COMPANION",
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace,
                color = ColorSubtle,
                letterSpacing = 1.2.sp
            )
        }

        // Right: ACTIVE pill & Settings button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status Pill
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ColorCardBg)
                    .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(if (isActive) ColorGreenActive else ColorRedAccent, CircleShape)
                )
                Text(
                    text = if (isActive) "ACTIVE" else "OFFLINE",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.8.sp
                )
            }

            // Settings Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                .background(ColorCardBg)
                .border(1.dp, ColorBorder, RoundedCornerShape(10.dp))
                .clickable { onOpenSettings() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
}

@Composable
private fun CardTotalWordsSpoken(stats: HomeStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "01 — TOTAL WORDS SPOKEN",
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorMuted,
                    letterSpacing = 0.5.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).background(ColorRedAccent, CircleShape))
                    Text(
                        text = "LIVE",
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ColorRedAccent,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Metric Value & Equalizer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    DotMatrixText(
                        text = stats.totalWords.toString(),
                        dotSize = 4.0.dp,
                        dotSpacing = 1.4.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Words spoken so far",
                        fontSize = 12.sp,
                        color = ColorMuted
                    )
                }

                // Weekly 7-day dot equalizer
                DotWeeklyEqualizer(
                    dayWordCounts = stats.weeklyDayCounts,
                    modifier = Modifier.width(118.dp),
                    dotSize = 2.4.dp,
                    dotSpacing = 1.2.dp
                )
            }
        }
    }
}

@Composable
private fun CardWeekStreak(stats: HomeStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "02 — WEEK STREAK",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorMuted
                )
                Icon(
                    painter = painterResource(R.drawable.ic_flame),
                    contentDescription = null,
                    tint = ColorRedAccent,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Value Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                DotMatrixText(
                    text = "${stats.weekStreak} ",
                    dotSize = 2.2.dp,
                    dotSpacing = 0.7.dp,
                    charSpacing = 1.2.dp,
                    color = Color.White
                )
                DotMatrixText(
                    text = "Days",
                    dotSize = 2.2.dp,
                    dotSpacing = 0.7.dp,
                    charSpacing = 1.2.dp,
                    color = ColorRedAccent
                )
            }

            Text(
                text = "Keep going!",
                fontSize = 10.5.sp,
                color = ColorMuted
            )

            // S M T W T F S Dots (0=Sun, 1=Mon, ..., 6=Sat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("S", "M", "T", "W", "T", "F", "S")
                for (i in 0..6) {
                    val isDayActive = stats.activeWeekdays.contains(i)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = days[i],
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorSubtle
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .border(1.dp, if (isDayActive) ColorRedAccent else ColorSubtle, CircleShape)
                                .background(if (isDayActive) ColorRedAccent else Color.Transparent, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardAppsTracked(stats: HomeStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "03 — APPS TRACKED",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorMuted
                )
                DotAppMatrix(activeCount = stats.activeAppsCount)
            }

            // Value Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                DotMatrixText(
                    text = "${stats.activeAppsCount} ",
                    dotSize = 2.2.dp,
                    dotSpacing = 0.7.dp,
                    charSpacing = 1.2.dp,
                    color = Color.White
                )
                DotMatrixText(
                    text = "apps",
                    dotSize = 2.2.dp,
                    dotSpacing = 0.7.dp,
                    charSpacing = 1.2.dp,
                    color = ColorRedAccent
                )
            }

            Text(
                text = "Number of apps\nyou are flowing in",
                fontSize = 10.5.sp,
                color = ColorMuted,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun CardSpeakingSpeed(stats: HomeStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "04 — SPEAKING SPEED",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorMuted
                )
                DotWaveformApex()
            }

            // Value Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                DotMatrixText(
                    text = "${stats.wordsPerMinute} ",
                    dotSize = 2.2.dp,
                    dotSpacing = 0.7.dp,
                    charSpacing = 1.2.dp,
                    color = Color.White
                )
                DotMatrixText(
                    text = "WPM",
                    dotSize = 2.2.dp,
                    dotSpacing = 0.7.dp,
                    charSpacing = 1.2.dp,
                    color = ColorRedAccent
                )
            }

            Text(
                text = "Total words per minute",
                fontSize = 10.5.sp,
                color = ColorMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CardTotalUsage(stats: HomeStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_clock),
                        contentDescription = null,
                        tint = ColorMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "05 TOTAL USAGE",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ColorMuted
                    )
                }
                DotUsageMeters()
            }

            // Value Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                DotMatrixText(
                    text = "${stats.totalUsageMinutes} ",
                    dotSize = 2.2.dp,
                    dotSpacing = 0.7.dp,
                    charSpacing = 1.2.dp,
                    color = Color.White
                )
                DotMatrixText(
                    text = "m",
                    dotSize = 2.2.dp,
                    dotSpacing = 0.7.dp,
                    charSpacing = 1.2.dp,
                    color = ColorRedAccent
                )
            }

            Text(
                text = "Total dictation time",
                fontSize = 10.5.sp,
                color = ColorMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CardRecentDictations(
    transcripts: List<TranscriptRecord>,
    onViewAll: () -> Unit,
    onCopy: (String) -> Unit,
    onDelete: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "06 — RECENT DICTATIONS",
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "View All →",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.clickable { onViewAll() }
                )
            }

            if (transcripts.isEmpty()) {
                // Exact Empty State from Screenshot
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dotted document graphic with overlapping mic badge
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Dotted outline document
                        Box(
                            modifier = Modifier
                                .size(50.dp, 60.dp)
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(18.dp, 2.dp).background(Color(0x66FFFFFF)))
                                Box(modifier = Modifier.size(24.dp, 2.dp).background(Color(0x66FFFFFF)))
                                Box(modifier = Modifier.size(14.dp, 2.dp).background(Color(0x66FFFFFF)))
                            }
                        }

                        // Red circular mic badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1C1D24))
                                .border(1.dp, ColorRedAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_mic),
                                contentDescription = null,
                                tint = ColorRedAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = "No dictations yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Tap the floating bubble in any text field to dictate.\nYour transcription history and speech metrics\nwill appear here.",
                        fontSize = 11.sp,
                        color = ColorMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            } else {
                // Show most recent 2 dictation items
                val recent = transcripts.take(2)
                val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recent.forEach { item ->
                        var menuOpen by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF14151C))
                                .border(1.dp, Color(0xFF262832), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = item.text,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    lineHeight = 18.sp
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_copy),
                                            contentDescription = "Copy",
                                            tint = ColorMuted,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onCopy(item.text) }
                                        )
                                        Box {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_more_vert),
                                                contentDescription = "More",
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
                                                        onDelete(item.id)
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
}

@Composable
private fun CardAccuracyTeaser() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "07 ACCURACY (SOON)",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorSubtle
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                DotMatrixText(
                    text = "--",
                    dotSize = 2.2.dp,
                    dotSpacing = 0.7.dp,
                    charSpacing = 1.2.dp,
                    color = ColorRedAccent
                )
                DotMatrixText(
                    text = "%",
                    dotSize = 2.2.dp,
                    dotSpacing = 0.7.dp,
                    charSpacing = 1.2.dp,
                    color = ColorRedAccent
                )
            }
            Text(
                text = "Transcription accuracy",
                fontSize = 10.5.sp,
                color = ColorSubtle
            )
        }
    }
}

@Composable
private fun CardProductivityTeaser() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "08 PRODUCTIVITY (SOON)",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorSubtle
                )
            }
            DotMatrixText(
                text = "--",
                dotSize = 2.2.dp,
                dotSpacing = 0.7.dp,
                charSpacing = 1.2.dp,
                color = Color.White
            )
            Text(
                text = "Words per session",
                fontSize = 10.5.sp,
                color = ColorSubtle
            )
        }
    }
}
