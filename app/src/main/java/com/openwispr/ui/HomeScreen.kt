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
import androidx.compose.animation.core.*
import com.openwispr.R
import com.openwispr.data.HistoryRepository
import com.openwispr.data.HomeStats
import com.openwispr.data.TranscriptRecord
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Deep OLED Pitch Black Palette
private val ColorDarkBg = Color(0xFF000000)
private val ColorCardBg = Color(0xFF070709)
private val ColorCardElevated = Color(0xFF101014)
private val ColorBorder = Color(0xFF141416)
private val ColorRedAccent = Color(0xFFFF2E2E)
private val ColorGreenAccent = Color(0xFF00E676)
private val ColorMuted = Color(0xFF8E95A2)
private val ColorSubtle = Color(0xFF555B66)

@Composable
fun HomeScreen(
    historyRepo: HistoryRepository,
    onOpenDrawer: () -> Unit,
    onNavigateToHistory: () -> Unit,
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
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(
                top = statusBarPadding + 10.dp,
                bottom = navBarPadding + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Top Header Bar
            item {
                TopBarHeader(
                    onOpenDrawer = onOpenDrawer
                )
            }

            // 2. Card 01: TOTAL WORDS SPOKEN (Hero Card with 24h Peak Activity)
            item {
                CardTotalWordsSpoken(stats = stats)
            }

            // 3. Row of 2 Cards: WEEK STREAK & APPS TRACKED (Twin Heights)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        CardWeekStreak(stats = stats)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        CardMostUsedApp(stats = stats)
                    }
                }
            }

            // 4. Row of 2 Cards: SPEAKING SPEED & TOTAL USAGE (Twin Heights)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        CardSpeakingSpeed(stats = stats)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
        }
    }
}

@Composable
private fun TopBarHeader(
    onOpenDrawer: () -> Unit
) {
    // 1. Smooth Breathing cycle (sine wave oscillation)
    val infiniteTransition = rememberInfiniteTransition(label = "WisprBreathing")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingAlpha"
    )

    // 2. Randomized Strobing / Micro-flicker bursts (gaming keyboard effect)
    val strobeMultiplier = remember { Animatable(1.0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(kotlin.random.Random.nextLong(2800L, 6500L))
            // Quick neon/keyboard strobe burst sequence
            strobeMultiplier.snapTo(0.20f)
            delay(30)
            strobeMultiplier.snapTo(0.95f)
            delay(40)
            strobeMultiplier.snapTo(0.15f)
            delay(25)
            strobeMultiplier.snapTo(1.0f)
            delay(45)
            strobeMultiplier.snapTo(0.35f)
            delay(35)
            strobeMultiplier.animateTo(1.0f, tween(100))
        }
    }

    val finalRedAlpha = (breathingAlpha * strobeMultiplier.value).coerceIn(0.15f, 1.0f)
    val glowingRed = ColorRedAccent.copy(alpha = finalRedAlpha)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left hamburger button - enlarged to 44dp with 20dp icon
        Box(
            modifier = Modifier
                .size(44.dp)
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
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title: Dot-Matrix Title with Breathing & Strobing Red LEDs stretched towards right
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DotMatrixText(
                text = "Open ",
                dotSize = 3.4.dp,
                dotSpacing = 1.1.dp,
                charSpacing = 2.8.dp,
                color = Color.White
            )
            DotMatrixText(
                text = "Wispr",
                dotSize = 3.4.dp,
                dotSpacing = 1.1.dp,
                charSpacing = 2.8.dp,
                color = glowingRed
            )
        }
    }
}

@Composable
private fun CardTotalWordsSpoken(stats: HomeStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
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
                    text = "TOTAL WORDS SPOKEN",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorMuted,
                    letterSpacing = 0.8.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(modifier = Modifier.size(5.dp).background(ColorRedAccent, CircleShape))
                    Text(
                        text = "LIVE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ColorRedAccent,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Hero Volume Row: Left Dot-Matrix Word Count + Right Peak Habit Insight Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    DotMatrixText(
                        text = NumberFormat.getNumberInstance(Locale.US).format(stats.totalWords),
                        dotSize = 3.8.dp,
                        dotSpacing = 1.2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Words spoken across all apps",
                        fontSize = 10.5.sp,
                        color = ColorMuted
                    )
                }

                // Peak Habit Insight Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorCardElevated)
                        .border(1.dp, ColorBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "PEAK",
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ColorMuted,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = stats.peakHourRange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val pctText = if (stats.peakPercentageVsAvg != "--") {
                            "▲ ${stats.peakPercentageVsAvg} vs avg"
                        } else {
                            "-- vs avg"
                        }
                        Text(
                            text = pctText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (stats.peakPercentageVsAvg != "--") ColorGreenAccent else ColorMuted
                        )
                    }
                }
            }

            // Subtle 1px divider separating aggregate metric from 24h temporal habit timeline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorBorder)
            )

            // Bottom Row: Full-Width 24-Hour Timeline Graph
            DotHourlyTimelineGraph(
                hourlyActivityCounts = stats.hourlyActivityCounts,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CardWeekStreak(stats: HomeStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Header Row
                Text(
                    text = "WEEK STREAK",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorMuted
                )

                // Level-aligned enlarged Metric and Flame moved towards right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DotMatrixText(
                            text = "${stats.weekStreak} ",
                            dotSize = 2.4.dp,
                            dotSpacing = 0.8.dp,
                            charSpacing = 1.2.dp,
                            color = Color.White
                        )
                        DotMatrixText(
                            text = "Days",
                            dotSize = 2.4.dp,
                            dotSpacing = 0.8.dp,
                            charSpacing = 1.2.dp,
                            color = ColorRedAccent
                        )
                    }

                    DotMatrixFlame()
                }

                Text(
                    text = "Keep going!",
                    fontSize = 9.sp,
                    color = ColorMuted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

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
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = days[i],
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorSubtle
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
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
private fun CardMostUsedApp(stats: HomeStats) {
    val context = LocalContext.current
    val appIconDrawable = remember(stats.mostUsedAppPackage) {
        stats.mostUsedAppPackage?.let { pkg ->
            try {
                context.packageManager.getApplicationIcon(pkg)
            } catch (e: Exception) {
                null
            }
        }
    }
    val appBitmap = remember(appIconDrawable) {
        appIconDrawable?.let { d ->
            val w = d.intrinsicWidth.coerceAtLeast(1)
            val h = d.intrinsicHeight.coerceAtLeast(1)
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            d.setBounds(0, 0, w, h)
            d.draw(canvas)
            bmp.asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Header
            Text(
                text = "MOST USED APP",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = ColorMuted
            )

            // 2. Middle Row: App Icon + Name (Red) + Percentage (White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ColorCardElevated)
                            .border(1.dp, ColorBorder, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appBitmap != null) {
                            Image(
                                bitmap = appBitmap,
                                contentDescription = stats.mostUsedAppName,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = stats.mostUsedAppName.take(1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    DotMatrixText(
                        text = stats.mostUsedAppName.take(8),
                        dotSize = 2.0.dp,
                        dotSpacing = 0.6.dp,
                        charSpacing = 1.0.dp,
                        color = ColorRedAccent
                    )
                }

                DotMatrixText(
                    text = "${stats.mostUsedAppPercentage}%",
                    dotSize = 2.0.dp,
                    dotSpacing = 0.6.dp,
                    charSpacing = 1.0.dp,
                    color = Color.White
                )
            }

            // 3. Dual-Row Dot Matrix Progress Bar
            DotMatrixDualProgressBar(
                progress = stats.mostUsedAppPercentage / 100f,
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Subtitle
            Text(
                text = "Of total words spoken",
                fontSize = 8.5.sp,
                color = ColorMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CardSpeakingSpeed(stats: HomeStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Text(
                text = "SPEAKING SPEED",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = ColorMuted
            )

            // Level-aligned Metric (horizontally stretched WPM) and Diagonal Ascending Dotted Streak
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotMatrixText(
                        text = "${stats.wordsPerMinute} ",
                        dotSize = 2.2.dp,
                        dotSpacing = 0.8.dp,
                        charSpacing = 1.2.dp,
                        color = Color.White
                    )
                    DotMatrixText(
                        text = "WPM",
                        dotSize = 2.2.dp,
                        dotSpacing = 0.8.dp,
                        charSpacing = 1.2.dp,
                        color = ColorRedAccent
                    )
                }

                DotSpeedAscentStreak()
            }

            // Subtitle
            Text(
                text = "Total words per minute",
                fontSize = 8.5.sp,
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
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with Clock Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_clock),
                    contentDescription = null,
                    tint = ColorMuted,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "TOTAL USAGE",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorMuted
                )
            }

            // Level-aligned Metric and Equalizer Meters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DotMatrixUsageDisplay(
                    formattedText = stats.formattedTotalUsage,
                    dotSize = 2.0.dp,
                    dotSpacing = 0.7.dp,
                    charSpacing = 0.9.dp
                )

                DotUsageMeters()
            }

            // Subtitle
            Text(
                text = "Total dictation time",
                fontSize = 8.5.sp,
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
            .clip(RoundedCornerShape(14.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT DICTATIONS",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "View All →",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.clickable { onViewAll() }
                )
            }

            if (transcripts.isEmpty()) {
                // Exact Empty State from Screenshot - Modern Compact Proportions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp, 44.dp)
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Box(modifier = Modifier.size(14.dp, 1.5.dp).background(Color(0x66FFFFFF)))
                                Box(modifier = Modifier.size(18.dp, 1.5.dp).background(Color(0x66FFFFFF)))
                                Box(modifier = Modifier.size(10.dp, 1.5.dp).background(Color(0x66FFFFFF)))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1C1D24))
                                .border(1.dp, ColorRedAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_mic),
                                contentDescription = null,
                                tint = ColorRedAccent,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    Text(
                        text = "No dictations yet",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Tap the floating bubble in any text field to dictate.\nYour transcription history and speech metrics\nwill appear here.",
                        fontSize = 9.5.sp,
                        color = ColorMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 13.5.sp
                    )
                }
            } else {
                val recent = transcripts.take(2)
                val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recent.forEach { item ->
                        var menuOpen by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF14151C))
                                .border(1.dp, Color(0xFF262832), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = item.text,
                                    fontSize = 11.5.sp,
                                    color = Color.White,
                                    lineHeight = 15.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = timeFormat.format(Date(item.timestampMs)),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ColorSubtle
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_copy),
                                            contentDescription = "Copy",
                                            tint = ColorMuted,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { onCopy(item.text) }
                                        )
                                        Box {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_more_vert),
                                                contentDescription = "More",
                                                tint = ColorMuted,
                                                modifier = Modifier
                                                    .size(14.dp)
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
