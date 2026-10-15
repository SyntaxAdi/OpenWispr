package com.openwispr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.pow
import kotlin.math.PI

/**
 * 5x7 Dot-matrix font glyph bitmasks.
 * Each 5-bit row from top to bottom (7 rows total).
 * Bit 4 is leftmost, bit 0 is rightmost.
 */
object DotMatrixFont {
    private val GLYPHS = mapOf(
        // Digits
        '0' to intArrayOf(0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110),
        '1' to intArrayOf(0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
        '2' to intArrayOf(0b01110, 0b10001, 0b00001, 0b00010, 0b00100, 0b01000, 0b11111),
        '3' to intArrayOf(0b11111, 0b00010, 0b00100, 0b00010, 0b00001, 0b10001, 0b01110),
        '4' to intArrayOf(0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010),
        '5' to intArrayOf(0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110),
        '6' to intArrayOf(0b00110, 0b01000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110),
        '7' to intArrayOf(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000),
        '8' to intArrayOf(0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110),
        '9' to intArrayOf(0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00010, 0b01100),

        // Uppercase
        'A' to intArrayOf(0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001),
        'B' to intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110),
        'C' to intArrayOf(0b01110, 0b10001, 0b10000, 0b10000, 0b10000, 0b10001, 0b01110),
        'D' to intArrayOf(0b11110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b11110),
        'E' to intArrayOf(0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111),
        'F' to intArrayOf(0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000),
        'G' to intArrayOf(0b01110, 0b10001, 0b10000, 0b10111, 0b10001, 0b10001, 0b01110),
        'H' to intArrayOf(0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001),
        'I' to intArrayOf(0b01110, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
        'J' to intArrayOf(0b00001, 0b00001, 0b00001, 0b00001, 0b10001, 0b10001, 0b01110),
        'K' to intArrayOf(0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001),
        'L' to intArrayOf(0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111),
        'M' to intArrayOf(0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001),
        'N' to intArrayOf(0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001),
        'O' to intArrayOf(0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110),
        'P' to intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000),
        'Q' to intArrayOf(0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101),
        'R' to intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001),
        'S' to intArrayOf(0b01111, 0b10000, 0b10000, 0b01110, 0b00001, 0b00001, 0b11110),
        'T' to intArrayOf(0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100),
        'U' to intArrayOf(0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110),
        'V' to intArrayOf(0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100),
        'W' to intArrayOf(0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b11011, 0b10001),
        'X' to intArrayOf(0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001),
        'Y' to intArrayOf(0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100, 0b00100),
        'Z' to intArrayOf(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111),

        // Lowercase
        'a' to intArrayOf(0b00000, 0b00000, 0b01110, 0b00001, 0b01111, 0b10001, 0b01111),
        'b' to intArrayOf(0b10000, 0b10000, 0b10110, 0b11001, 0b10001, 0b11001, 0b10110),
        'c' to intArrayOf(0b00000, 0b00000, 0b01110, 0b10001, 0b10000, 0b10001, 0b01110),
        'd' to intArrayOf(0b00001, 0b00001, 0b01101, 0b10011, 0b10001, 0b10011, 0b01101),
        'e' to intArrayOf(0b00000, 0b00000, 0b01110, 0b10001, 0b11111, 0b10000, 0b01110),
        'f' to intArrayOf(0b00110, 0b01001, 0b01000, 0b11100, 0b01000, 0b01000, 0b01000),
        'g' to intArrayOf(0b00000, 0b00000, 0b01110, 0b10010, 0b01110, 0b00010, 0b01100),
        'h' to intArrayOf(0b10000, 0b10000, 0b10110, 0b11001, 0b10001, 0b10001, 0b10001),
        'i' to intArrayOf(0b00100, 0b00000, 0b01100, 0b00100, 0b00100, 0b00100, 0b01110),
        'j' to intArrayOf(0b00010, 0b00000, 0b00110, 0b00010, 0b00010, 0b10010, 0b01100),
        'k' to intArrayOf(0b10000, 0b10000, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010),
        'l' to intArrayOf(0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
        'm' to intArrayOf(0b00000, 0b00000, 0b11010, 0b10101, 0b10101, 0b10101, 0b10001),
        'n' to intArrayOf(0b00000, 0b00000, 0b10110, 0b11001, 0b10001, 0b10001, 0b10001),
        'o' to intArrayOf(0b00000, 0b00000, 0b01110, 0b10001, 0b10001, 0b10001, 0b01110),
        'p' to intArrayOf(0b00000, 0b00000, 0b11110, 0b10001, 0b11110, 0b10000, 0b10000),
        'q' to intArrayOf(0b00000, 0b00000, 0b01110, 0b10010, 0b01111, 0b00010, 0b00010),
        'r' to intArrayOf(0b00000, 0b00000, 0b10110, 0b11001, 0b10000, 0b10000, 0b10000),
        's' to intArrayOf(0b00000, 0b00000, 0b01111, 0b10000, 0b01110, 0b00001, 0b11110),
        't' to intArrayOf(0b00100, 0b00100, 0b01110, 0b00100, 0b00100, 0b00101, 0b00010),
        'u' to intArrayOf(0b00000, 0b00000, 0b10001, 0b10001, 0b10001, 0b10011, 0b01101),
        'v' to intArrayOf(0b00000, 0b00000, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100),
        'w' to intArrayOf(0b00000, 0b00000, 0b10001, 0b10101, 0b10101, 0b11011, 0b01010),
        'x' to intArrayOf(0b00000, 0b00000, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001),
        'y' to intArrayOf(0b00000, 0b00000, 0b10001, 0b10001, 0b01111, 0b00001, 0b01110),
        'z' to intArrayOf(0b00000, 0b00000, 0b11111, 0b00010, 0b00100, 0b01000, 0b11111),

        // Symbols
        '-' to intArrayOf(0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000),
        '%' to intArrayOf(0b11001, 0b11010, 0b00100, 0b01000, 0b10011, 0b00011, 0b00000),
        ':' to intArrayOf(0b00000, 0b00100, 0b00000, 0b00000, 0b00100, 0b00000, 0b00000),
        '.' to intArrayOf(0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00100, 0b00100),
        ',' to intArrayOf(0b00000, 0b00000, 0b00000, 0b00000, 0b00100, 0b00100, 0b01000),
        ' ' to intArrayOf(0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000)
    )

    fun getRows(c: Char): IntArray {
        return GLYPHS[c] ?: GLYPHS[' ']!!
    }
}

@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.dp,
    dotSpacing: Dp = 1.2.dp,
    charSpacing: Dp = 3.dp,
    color: Color = Color.White,
    ghostColor: Color? = Color(0x14FFFFFF)
) {
    val chars = remember(text) { text.toCharArray() }

    // 5 dots wide, 7 dots high per character
    val charWidthDp = dotSize * 5 + dotSpacing * 4
    val spaceWidthDp = dotSize * 2.5f + dotSpacing * 1.5f
    val totalWidthDp = chars.fold(0.dp) { acc, c ->
        acc + if (c == ' ') spaceWidthDp + charSpacing else charWidthDp + charSpacing
    } - if (chars.isNotEmpty()) charSpacing else 0.dp
    val totalHeightDp = dotSize * 7 + dotSpacing * 6

    Canvas(
        modifier = modifier
            .size(width = totalWidthDp.coerceAtLeast(0.dp), height = totalHeightDp)
            .semantics { contentDescription = text }
    ) {
        val dSize = dotSize.toPx()
        val dSpacing = dotSpacing.toPx()
        val cSpacing = charSpacing.toPx()
        val radius = dSize / 2f
        val step = dSize + dSpacing

        var startX = 0f

        chars.forEach { ch ->
            if (ch == ' ') {
                startX += (step * 2.5f) + cSpacing
                return@forEach
            }

            val rows = DotMatrixFont.getRows(ch)
            for (row in 0 until 7) {
                val mask = rows[row]
                val cy = row * step + radius
                for (col in 0 until 5) {
                    val isLit = ((mask shr (4 - col)) and 1) == 1
                    val cx = startX + col * step + radius

                    if (isLit) {
                        drawCircle(color = color, radius = radius, center = Offset(cx, cy))
                    } else if (ghostColor != null) {
                        drawCircle(color = ghostColor, radius = radius, center = Offset(cx, cy))
                    }
                }
            }
            startX += (5 * step - dSpacing) + cSpacing
        }
    }
}

/**
 * 24-Hour Activity Timeline graph for Card 01 (12 AM, 6 AM, 12 PM, 6 PM, 12 AM)
 * Renders full-width LED dot-matrix activity distribution, peak red apex highlights, and axis ticks.
 */
@Composable
fun DotHourlyTimelineGraph(
    hourlyActivityCounts: Map<Int, Long>, // 0..23 hours
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.4.dp,
    dotSpacing: Dp = 1.4.dp,
    maxDots: Int = 6
) {
    val maxCount = (hourlyActivityCounts.values.maxOrNull() ?: 0L).toFloat()
    val peakHour = if (hourlyActivityCounts.values.any { it > 0 }) {
        hourlyActivityCounts.maxByOrNull { it.value }?.key
    } else {
        null
    }

    Column(modifier = modifier) {
        // Section Subheader Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HOURLY ACTIVITY DISTRIBUTION",
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF6B7280),
                letterSpacing = 0.8.sp
            )
            Text(
                text = "24-HOUR CYCLE",
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF6B7280),
                letterSpacing = 0.8.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Full-Width Dot Graph & Axis Canvas
        val graphHeight = dotSize * maxDots + dotSpacing * (maxDots - 1) + 8.dp
        Canvas(modifier = Modifier.fillMaxWidth().height(graphHeight)) {
            val dSize = dotSize.toPx()
            val dSpacing = dotSpacing.toPx()
            val radius = dSize / 2f
            val stepY = dSize + dSpacing
            val numCols = 25
            val stepX = (size.width - dSize) / (numCols - 1)
            val axisY = maxDots * stepY + 2f

            for (col in 0 until numCols) {
                val hr = col % 24
                val count = hourlyActivityCounts[hr] ?: 0L
                val litCount = if (count > 0 && maxCount > 0) {
                    ((count / maxCount) * maxDots).toInt().coerceIn(1, maxDots)
                } else {
                    0
                }

                val cx = col * stepX + radius
                val isPeakCol = peakHour != null && hr == peakHour

                for (rowFromBottom in 0 until maxDots) {
                    val rowFromTop = maxDots - 1 - rowFromBottom
                    val cy = rowFromTop * stepY + radius
                    val isLit = rowFromBottom < litCount
                    val isPeakDot = isPeakCol && isLit && (rowFromBottom >= litCount - 2)

                    if (isLit) {
                        val color = if (isPeakDot) Color(0xFFFF2E2E) else Color.White
                        drawCircle(color = color, radius = radius, center = Offset(cx, cy))
                    } else {
                        drawCircle(color = Color(0x18FFFFFF), radius = radius, center = Offset(cx, cy))
                    }
                }

                // Axis tick marks at 0, 6, 12, 18, 24
                if (col % 6 == 0) {
                    drawLine(
                        color = Color(0x66FFFFFF),
                        start = Offset(cx, axisY),
                        end = Offset(cx, axisY + 4f),
                        strokeWidth = 1.2f
                    )
                }
            }

            // Continuous horizontal timeline axis line
            drawLine(
                color = Color(0x2BFFFFFF),
                start = Offset(0f, axisY),
                end = Offset(size.width, axisY),
                strokeWidth = 1f
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Time Axis Labels Spanning Full Width (12 AM, 6 AM, 12 PM, 6 PM, 12 AM)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("12 AM", "6 AM", "12 PM", "6 PM", "12 AM").forEach { label ->
                Text(
                    text = label,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

/**
 * 4x4 matrix for Card 03 (Apps Tracked) with active green LED dots
 */
/**
 * Dotted Nothing OS Flame for Card 02 (Week Streak).
 * Permanent design matching reference mockup with red LED dot matrix silhouette.
 */
@Composable
fun DotMatrixFlame(
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.4.dp,
    dotSpacing: Dp = 1.6.dp
) {
    val numCols = 7
    val numRows = 9
    val totalWidth = dotSize * numCols + dotSpacing * (numCols - 1)
    val totalHeight = dotSize * numRows + dotSpacing * (numRows - 1)

    // Flame pattern bitmask (1 = flame dot, 0 = empty)
    val flameGrid = arrayOf(
        intArrayOf(0, 0, 0, 1, 0, 0, 0), // Row 0
        intArrayOf(0, 0, 1, 1, 0, 0, 0), // Row 1
        intArrayOf(0, 1, 1, 1, 1, 0, 0), // Row 2
        intArrayOf(0, 1, 1, 1, 1, 1, 0), // Row 3
        intArrayOf(1, 1, 1, 0, 1, 1, 1), // Row 4
        intArrayOf(1, 1, 0, 0, 0, 1, 1), // Row 5 (hollow core)
        intArrayOf(1, 1, 0, 0, 0, 1, 1), // Row 6
        intArrayOf(0, 1, 1, 0, 1, 1, 0), // Row 7
        intArrayOf(0, 0, 1, 1, 1, 0, 0)  // Row 8 (base)
    )

    Canvas(modifier = modifier.size(totalWidth, totalHeight)) {
        val dSize = dotSize.toPx()
        val dSpacing = dotSpacing.toPx()
        val radius = dSize / 2f
        val stepX = dSize + dSpacing
        val stepY = dSize + dSpacing

        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                if (flameGrid[row][col] == 1) {
                    val cx = col * stepX + radius
                    val cy = row * stepY + radius
                    val color = if (row <= 2) Color(0xFFFF5252) else Color(0xFFFF2E2E)
                    drawCircle(color = color, radius = radius, center = Offset(cx, cy))
                }
            }
        }
    }
}

/**
 * Dot-matrix display for progressive usage duration (0s, 0m 0s, 1 day, etc.)
 * Dynamically renders numeric digits in White and unit/time markers in Signal Red.
 */
@Composable
fun DotMatrixUsageDisplay(
    formattedText: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 1.8.dp,
    dotSpacing: Dp = 0.6.dp,
    charSpacing: Dp = 0.8.dp
) {
    val tokens = remember(formattedText) {
        val list = mutableListOf<Pair<String, Boolean>>()
        val matcher = java.util.regex.Pattern.compile("(\\d+|\\D+)").matcher(formattedText)
        while (matcher.find()) {
            val chunk = matcher.group(1) ?: ""
            val isNum = chunk.all { it.isDigit() }
            list.add(chunk to isNum)
        }
        list
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tokens.forEach { (chunk, isNum) ->
            DotMatrixText(
                text = chunk,
                dotSize = dotSize,
                dotSpacing = dotSpacing,
                charSpacing = charSpacing,
                color = if (isNum) Color.White else Color(0xFFFF2E2E)
            )
        }
    }
}

/**
 * 6-Column High-Density Equalizer Meters for Card 05 (Total Usage).
 * Features stepped frequency columns, horizontal decibel reference ticks,
 * and Signal Red overload warning apexes.
 */
@Composable
fun DotUsageMeters(
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.2.dp,
    dotSpacing: Dp = 1.3.dp,
    numCols: Int = 6,
    numDots: Int = 10
) {
    val totalWidth = dotSize * numCols + dotSpacing * (numCols - 1) * 2.2f
    val totalHeight = dotSize * numDots + dotSpacing * (numDots - 1)
    val colHeights = listOf(2, 4, 5, 7, 9, 10)

    Canvas(modifier = modifier.size(totalWidth, totalHeight)) {
        val dSize = dotSize.toPx()
        val dSpacing = dotSpacing.toPx()
        val stepX = dSize + dSpacing * 2.2f
        val stepY = dSize + dSpacing
        val radius = dSize / 2f

        for (col in 0 until numCols) {
            val cx = col * stepX + radius
            val litCount = colHeights[col]

            for (row in 0 until numDots) {
                val rowFromBottom = numDots - 1 - row
                val cy = row * stepY + radius
                val isLit = rowFromBottom < litCount
                val isOverloadPeak = isLit && (
                    (col == numCols - 1 && rowFromBottom >= 6) ||
                    (col == numCols - 2 && rowFromBottom >= 7)
                )

                if (isLit) {
                    val colColor = if (isOverloadPeak) Color(0xFFFF2E2E) else Color(0xDDFFFFFF)
                    drawCircle(color = colColor, radius = radius, center = Offset(cx, cy))
                } else {
                    // Ghost grid dot
                    drawCircle(color = Color(0x15FFFFFF), radius = radius * 0.85f, center = Offset(cx, cy))
                }

                // Horizontal reference tick at 50% (row 4 from bottom)
                if (rowFromBottom == 4 && !isLit) {
                    drawCircle(color = Color(0x35FFFFFF), radius = radius * 0.5f, center = Offset(cx, cy))
                }
            }
        }
    }
}

/**
 * Compact diagonal 8-dot ascending speed streak for Speaking Speed.
 * Matches Image 2: gently ascending from bottom-left to top-right with
 * dim white base dots and bright Signal Red apex dots.
 */
@Composable
fun DotSpeedAscentStreak(
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.4.dp
) {
    val totalWidth = 32.dp
    val totalHeight = 14.dp

    Canvas(modifier = modifier.size(totalWidth, totalHeight)) {
        val radius = dotSize.toPx() / 2f
        val numPoints = 8
        val stepX = (size.width - radius * 2f) / (numPoints - 1)
        val stepY = (size.height - radius * 2f) / (numPoints - 1)

        for (i in 0 until numPoints) {
            val cx = radius + i * stepX
            val cy = size.height - radius - i * stepY
            val color = when {
                i >= 5 -> Color(0xFFFF2E2E) // Bright Signal Red apex
                i in 3..4 -> Color(0xBBFFFFFF) // Mid white
                else -> Color(0x55FFFFFF) // Dim white base
            }
            drawCircle(color = color, radius = radius, center = Offset(cx, cy))
        }
    }
}

enum class DotAnimationPattern {
    SequentialFill,
    CascadeLighting,
    BreathingPulse,
    PingPongScanner,
    CenterOutRipple,
    MatrixShimmer
}

/**
 * Dual-row dot matrix progress bar.
 * In static mode, renders dots according to progress percentage.
 * In animated mode, renders multi-phase hardware LED telemetry animations.
 */
@Composable
fun DotMatrixDualProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    numDots: Int = 22,
    dotSize: Dp = 2.4.dp,
    rowSpacing: Dp = 3.dp,
    isAnimated: Boolean = false,
    animationPattern: DotAnimationPattern = DotAnimationPattern.SequentialFill,
    animationProgress: Float = 0f
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(dotSize * 2 + rowSpacing)
    ) {
        val dSize = dotSize.toPx()
        val radius = dSize / 2f
        val rSpacing = rowSpacing.toPx()
        val stepX = if (numDots > 1) (size.width - dSize) / (numDots - 1) else 0f
        val baseRed = Color(0xFFFF2E2E)
        val ghostColor = Color(0x22FFFFFF)

        for (row in 0..1) {
            val cy = row * (dSize + rSpacing) + radius
            for (col in 0 until numDots) {
                val cx = col * stepX + radius
                val dotColor: Color = if (!isAnimated) {
                    val activeCount = (progress.coerceIn(0f, 1f) * numDots).roundToInt()
                    if (col < activeCount) baseRed else ghostColor
                } else {
                    val p = animationProgress.coerceIn(0f, 1f)
                    when (animationPattern) {
                        DotAnimationPattern.SequentialFill -> {
                            // Phase A (0.0 to 0.5): Fill left to right; Phase B (0.5 to 1.0): Drain left to right
                            if (p <= 0.5f) {
                                val fillThreshold = (p * 2f * numDots)
                                if (col <= fillThreshold) baseRed else ghostColor
                            } else {
                                val drainThreshold = ((p - 0.5f) * 2f * numDots)
                                if (col >= drainThreshold) baseRed else ghostColor
                            }
                        }
                        DotAnimationPattern.CascadeLighting -> {
                            // Comet head sweeps across dots with a fading phosphor tail
                            val headPos = p * (numDots + 8) - 4
                            val distance = headPos - col
                            if (distance in 0f..7f) {
                                val alphaFactor = (1f - (distance / 7f)).pow(1.6f)
                                baseRed.copy(alpha = (0.15f + 0.85f * alphaFactor).coerceIn(0.15f, 1f))
                            } else {
                                ghostColor
                            }
                        }
                        DotAnimationPattern.BreathingPulse -> {
                            // Synchronous breathing sine wave across entire LED bank
                            val wave = (sin(p * 2 * Math.PI.toFloat()) + 1f) / 2f
                            baseRed.copy(alpha = (0.22f + 0.78f * wave).coerceIn(0.22f, 1f))
                        }
                        DotAnimationPattern.PingPongScanner -> {
                            // Dual beam ping-pong bounce left to right and back
                            val pos = if (p < 0.5f) p * 2f * (numDots - 1) else (1f - p) * 2f * (numDots - 1)
                            val dist = abs(col - pos)
                            if (dist < 3.2f) {
                                val alpha = (1f - dist / 3.2f).coerceIn(0f, 1f)
                                baseRed.copy(alpha = (0.18f + 0.82f * alpha).coerceIn(0.18f, 1f))
                            } else {
                                ghostColor
                            }
                        }
                        DotAnimationPattern.CenterOutRipple -> {
                            // Wave originating from center expanding outward
                            val center = (numDots - 1) / 2f
                            val distCenter = abs(col - center)
                            val wavePos = p * (center + 2)
                            val distWave = abs(distCenter - wavePos)
                            if (distWave < 2.5f) {
                                val alpha = (1f - distWave / 2.5f).coerceIn(0f, 1f)
                                baseRed.copy(alpha = (0.18f + 0.82f * alpha).coerceIn(0.18f, 1f))
                            } else {
                                ghostColor
                            }
                        }
                        DotAnimationPattern.MatrixShimmer -> {
                            // Stochastic sparkle across dot grid
                            val seed = ((col * 31 + row * 19 + (p * 28).toInt() * 11) % 100)
                            if (seed > 38) {
                                val shimmerAlpha = (0.45f + 0.55f * ((seed % 50) / 50f))
                                baseRed.copy(alpha = shimmerAlpha)
                            } else {
                                ghostColor
                            }
                        }
                    }
                }
                drawCircle(color = dotColor, radius = radius, center = Offset(cx, cy))
            }
        }
    }
}


