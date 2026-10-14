package com.openwispr.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import com.openwispr.R
import com.openwispr.data.HistoryRepository
import com.openwispr.data.HomeStats
import com.openwispr.data.TranscriptRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    var stats by remember { mutableStateOf(HomeStats.EMPTY) }
    var transcripts by remember { mutableStateOf<List<TranscriptRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshData() {
        scope.launch {
            runCatching {
                val newStats = historyRepo.getHomeStats()
                val newRecent = historyRepo.getRecentTranscripts(limit = 2)
                stats = newStats
                transcripts = newRecent
            }
            isLoading = false
        }
    }

    LaunchedEffect(historyRepo) {
        refreshData()
        historyRepo.dataUpdates.collect {
            refreshData()
        }
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
                        val clip = ClipData.newPlainText("Transcription", text).apply {
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
                    },
                    onDelete = { id ->
                        scope.launch {
                            val result = runCatching { historyRepo.deleteTranscript(id) }
                            if (result.getOrDefault(false)) {
                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                            }
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
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = (breathingAlpha * strobeMultiplier.value).coerceIn(0.15f, 1.0f)
                }
            ) {
                DotMatrixText(
                    text = "Wispr",
                    dotSize = 3.4.dp,
                    dotSpacing = 1.1.dp,
                    charSpacing = 2.8.dp,
                    color = ColorRedAccent
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
                        val streakUnit = if (stats.weekStreak == 1) "Day" else "Days"
                        DotMatrixText(
                            text = streakUnit,
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

private fun loadAppIconBitmap(context: Context, packageName: String?): ImageBitmap? {
    if (packageName.isNullOrBlank() || packageName == "unknown") return null
    val pm = context.packageManager
    val drawable = try {
        pm.getApplicationIcon(packageName)
    } catch (e: Exception) {
        try {
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) pm.getActivityIcon(intent) else null
        } catch (e2: Exception) {
            null
        }
    }
    return drawable?.let { d ->
        try {
            val sizePx = 96
            val bmp = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            d.setBounds(0, 0, sizePx, sizePx)
            d.draw(canvas)
            bmp.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
private fun CardMostUsedApp(stats: HomeStats) {
    val context = LocalContext.current
    val appBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = stats.mostUsedAppPackage) {
        value = withContext(Dispatchers.IO) {
            loadAppIconBitmap(context, stats.mostUsedAppPackage)
        }
    }

    val nameLines = remember(stats.mostUsedAppName) {
        com.openwispr.data.HistoryRepository.splitAppNameForDisplay(stats.mostUsedAppName, maxLineChars = 11)
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

            // 2. Metric Row: App Icon (Left) + Percentage (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorCardElevated)
                        .border(1.dp, ColorBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appBitmap != null) {
                        Image(
                            bitmap = appBitmap!!,
                            contentDescription = stats.mostUsedAppName,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                    } else {
                        Text(
                            text = stats.mostUsedAppName.take(1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }

                DotMatrixText(
                    text = "${stats.mostUsedAppPercentage}%",
                    dotSize = 2.1.dp,
                    dotSpacing = 0.6.dp,
                    charSpacing = 1.0.dp,
                    color = Color.White
                )
            }

            // 3. Center Row: App Name (Centered Red Dot-Matrix)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                nameLines.forEach { line ->
                    DotMatrixText(
                        text = line,
                        dotSize = 1.9.dp,
                        dotSpacing = 0.5.dp,
                        charSpacing = 1.2.dp,
                        color = ColorRedAccent
                    )
                }
            }

            // 4. Dual-Row Dot Matrix Progress Bar
            DotMatrixDualProgressBar(
                progress = (stats.mostUsedAppPercentage / 100f).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth()
            )

            // 5. Subtitle: Pinned to bottom to match twin card baseline
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
            // Header Row: Red LED dot + Telemetry title + VIEW ALL action pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(ColorRedAccent)
                    )
                    Text(
                        text = "RECENT DICTATIONS",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF14151C))
                        .border(0.8.dp, ColorBorder, RoundedCornerShape(6.dp))
                        .clickable { onViewAll() }
                        .padding(horizontal = 8.dp, vertical = 3.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "VIEW ALL",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "→",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorRedAccent
                        )
                    }
                }
            }

            if (transcripts.isEmpty()) {
                // Nothing OS Telemetry Empty State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp, 44.dp)
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Box(modifier = Modifier.size(14.dp, 1.5.dp).background(Color(0x44FFFFFF)))
                                Box(modifier = Modifier.size(18.dp, 1.5.dp).background(Color(0x44FFFFFF)))
                                Box(modifier = Modifier.size(10.dp, 1.5.dp).background(Color(0x44FFFFFF)))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF14151C))
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

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "NO DICTATIONS LOGGED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Focus any text field to dictate. Transcripts appear here live.",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val recent = transcripts.take(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recent.forEach { item ->
                        key(item.id) {
                            RecentDictationItem(
                                item = item,
                                onCopy = onCopy,
                                onDelete = onDelete
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentDictationItem(
    item: TranscriptRecord,
    onCopy: (String) -> Unit,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    val timeFormat = remember(context) { android.text.format.DateFormat.getTimeFormat(context) }

    val appBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = item.appPackage) {
        value = withContext(Dispatchers.IO) {
            loadAppIconBitmap(context, item.appPackage)
        }
    }

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
            .clip(RoundedCornerShape(10.dp))
            .background(ColorCardElevated)
            .border(1.dp, ColorBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top Row: App Icon + App Label + Word Count Badge + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (appBitmap != null) {
                        Image(
                            bitmap = appBitmap!!,
                            contentDescription = null,
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF1E2028)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = appDisplayName.take(1),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

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

            // Dictation Text with Left Red/Dark Rail Guide
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
                    fontSize = 12.sp,
                    color = Color.White,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Bottom Micro-Actions Bar
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF14151C))
                            .border(0.8.dp, ColorBorder, RoundedCornerShape(5.dp))
                            .clickable { onCopy(item.text) }
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

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF1E1418))
                            .border(0.8.dp, ColorRedAccent.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                            .clickable { onDelete(item.id) }
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
