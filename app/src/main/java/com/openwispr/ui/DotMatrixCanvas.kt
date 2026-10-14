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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        'g' to intArrayOf(0b00000, 0b01101, 0b10011, 0b10011, 0b01101, 0b00001, 0b01110),
        'h' to intArrayOf(0b10000, 0b10000, 0b10110, 0b11001, 0b10001, 0b10001, 0b10001),
        'i' to intArrayOf(0b00100, 0b00000, 0b01100, 0b00100, 0b00100, 0b00100, 0b01110),
        'j' to intArrayOf(0b00010, 0b00000, 0b00110, 0b00010, 0b00010, 0b10010, 0b01100),
        'k' to intArrayOf(0b10000, 0b10000, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010),
        'l' to intArrayOf(0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
        'm' to intArrayOf(0b00000, 0b00000, 0b11010, 0b10101, 0b10101, 0b10101, 0b10001),
        'n' to intArrayOf(0b00000, 0b00000, 0b10110, 0b11001, 0b10001, 0b10001, 0b10001),
        'o' to intArrayOf(0b00000, 0b00000, 0b01110, 0b10001, 0b10001, 0b10001, 0b01110),
        'p' to intArrayOf(0b00000, 0b10110, 0b11001, 0b10001, 0b11110, 0b10000, 0b10000),
        'q' to intArrayOf(0b00000, 0b01101, 0b10011, 0b10001, 0b01111, 0b00001, 0b00001),
        'r' to intArrayOf(0b00000, 0b00000, 0b10110, 0b11001, 0b10000, 0b10000, 0b10000),
        's' to intArrayOf(0b00000, 0b00000, 0b01111, 0b10000, 0b01110, 0b00001, 0b11110),
        't' to intArrayOf(0b00100, 0b00100, 0b01110, 0b00100, 0b00100, 0b00101, 0b00010),
        'u' to intArrayOf(0b00000, 0b00000, 0b10001, 0b10001, 0b10001, 0b10011, 0b01101),
        'v' to intArrayOf(0b00000, 0b00000, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100),
        'w' to intArrayOf(0b00000, 0b00000, 0b10001, 0b10101, 0b10101, 0b11011, 0b01010),
        'x' to intArrayOf(0b00000, 0b00000, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001),
        'y' to intArrayOf(0b00000, 0b10001, 0b10001, 0b01111, 0b00001, 0b10001, 0b01110),
        'z' to intArrayOf(0b00000, 0b00000, 0b11111, 0b00010, 0b00100, 0b01000, 0b11111),

        // Symbols
        '-' to intArrayOf(0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000),
        '%' to intArrayOf(0b11001, 0b11010, 0b00100, 0b01000, 0b10011, 0b00011, 0b00000),
        ':' to intArrayOf(0b00000, 0b00100, 0b00000, 0b00000, 0b00100, 0b00000, 0b00000),
        '.' to intArrayOf(0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00100, 0b00100),
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
    val totalWidthDp = if (chars.isEmpty()) 0.dp else (charWidthDp * chars.size + charSpacing * (chars.size - 1))
    val totalHeightDp = dotSize * 7 + dotSpacing * 6

    Canvas(
        modifier = modifier.size(width = totalWidthDp, height = totalHeightDp)
    ) {
        val dSize = dotSize.toPx()
        val dSpacing = dotSpacing.toPx()
        val cSpacing = charSpacing.toPx()
        val radius = dSize / 2f
        val step = dSize + dSpacing

        var startX = 0f

        chars.forEach { ch ->
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
 * Weekly equalizer bars for Card 01 (MON, TUE, WED, THU, FRI, SAT, SUN)
 * Renders each day column and its label together, dynamically adapting to width without wrapping.
 */
@Composable
fun DotWeeklyEqualizer(
    dayWordCounts: Map<Int, Long>, // Monday=1 to Sunday=7
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.4.dp,
    dotSpacing: Dp = 1.2.dp,
    maxDots: Int = 10
) {
    val dayLabels = listOf(
        1 to "MON",
        2 to "TUE",
        3 to "WED",
        4 to "THU",
        5 to "FRI",
        6 to "SAT",
        7 to "SUN"
    )
    val maxCount = (dayWordCounts.values.maxOrNull() ?: 1L).coerceAtLeast(100L).toFloat()
    val colHeight = dotSize * maxDots + dotSpacing * (maxDots - 1)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        dayLabels.forEach { (dayIndex, dayLabel) ->
            val count = dayWordCounts[dayIndex] ?: 0L
            val litCount = if (count > 0) {
                ((count / maxCount) * maxDots).toInt().coerceIn(1, maxDots)
            } else {
                if (dayIndex == 5 || dayIndex == 6) 6 else 2 // aesthetic fallback bars
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Vertical dots canvas
                Canvas(modifier = Modifier.size(width = dotSize, height = colHeight)) {
                    val dSize = dotSize.toPx()
                    val dSpacing = dotSpacing.toPx()
                    val radius = dSize / 2f
                    val step = dSize + dSpacing
                    val cx = radius

                    for (rowFromBottom in 0 until maxDots) {
                        val rowFromTop = maxDots - 1 - rowFromBottom
                        val cy = rowFromTop * step + radius
                        val isLit = rowFromBottom < litCount
                        val isPeak = rowFromBottom >= (maxDots - 3) && (dayIndex == 5 || dayIndex == 6)

                        if (isLit) {
                            val dotColor = if (isPeak) Color(0xFFFF2E2E) else Color.White
                            drawCircle(color = dotColor, radius = radius, center = Offset(cx, cy))
                        } else {
                            drawCircle(color = Color(0x18FFFFFF), radius = radius, center = Offset(cx, cy))
                        }
                    }
                }

                // Middle indicator dot
                Box(
                    modifier = Modifier
                        .size(2.dp)
                        .background(Color(0xFF6B7280), CircleShape)
                )

                // Day Label (Monospace, single line, no wrapping)
                Text(
                    text = dayLabel,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF6B7280),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * 4x4 matrix for Card 03 (Apps Tracked) with active green LED dots
 */
@Composable
fun DotAppMatrix(
    activeCount: Int,
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.4.dp,
    dotSpacing: Dp = 1.6.dp
) {
    val totalWidth = dotSize * 4 + dotSpacing * 3
    val totalHeight = dotSize * 4 + dotSpacing * 3

    Canvas(modifier = modifier.size(totalWidth, totalHeight)) {
        val dSize = dotSize.toPx()
        val dSpacing = dotSpacing.toPx()
        val radius = dSize / 2f
        val step = dSize + dSpacing

        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val cx = col * step + radius
                val cy = row * step + radius
                val isActive = (row == 0 && col == 0) || (row == 1 && col == 0 && activeCount >= 2)

                if (isActive) {
                    drawCircle(color = Color(0xFF00E676), radius = radius, center = Offset(cx, cy))
                } else {
                    drawCircle(color = Color(0x1AFFFFFF), radius = radius, center = Offset(cx, cy))
                }
            }
        }
    }
}

/**
 * Waveform dotted apex for Card 04 (Speaking Speed)
 */
@Composable
fun DotWaveformApex(
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.dp
) {
    val totalWidth = 38.dp
    val totalHeight = 16.dp

    Canvas(modifier = modifier.size(totalWidth, totalHeight)) {
        val radius = dotSize.toPx() / 2f
        val yCoords = listOf(0.8f, 0.7f, 0.55f, 0.4f, 0.2f, 0.35f, 0.55f, 0.7f, 0.8f)
        val stepX = size.width / (yCoords.size - 1)

        yCoords.forEachIndexed { i, normY ->
            val cx = i * stepX
            val cy = normY * size.height
            val isApex = i == 4
            val color = if (isApex) Color(0xFFFF2E2E) else Color(0x66FF2E2E)
            drawCircle(color = color, radius = if (isApex) radius * 1.3f else radius, center = Offset(cx, cy))
        }
    }
}

/**
 * Level meters for Card 05 (Total Usage)
 */
@Composable
fun DotUsageMeters(
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.2.dp,
    dotSpacing: Dp = 1.2.dp,
    numCols: Int = 4,
    numDots: Int = 8
) {
    val totalWidth = dotSize * numCols + dotSpacing * (numCols - 1) * 2.5f
    val totalHeight = dotSize * numDots + dotSpacing * (numDots - 1)

    Canvas(modifier = modifier.size(totalWidth, totalHeight)) {
        val dSize = dotSize.toPx()
        val dSpacing = dotSpacing.toPx()
        val stepX = dSize + dSpacing * 2.5f
        val stepY = dSize + dSpacing
        val radius = dSize / 2f

        val colHeights = listOf(3, 5, 7, 8)

        for (col in 0 until numCols) {
            val cx = col * stepX + radius
            val litLimit = colHeights.getOrElse(col) { 4 }

            for (row in 0 until numDots) {
                val rowFromBottom = numDots - 1 - row
                val cy = row * stepY + radius
                val isLit = rowFromBottom < litLimit
                val isPeak = isLit && (col == numCols - 1)

                if (isLit) {
                    val colColor = if (isPeak) Color(0xFFFF2E2E) else Color.White
                    drawCircle(color = colColor, radius = radius, center = Offset(cx, cy))
                } else {
                    drawCircle(color = Color(0x12FFFFFF), radius = radius, center = Offset(cx, cy))
                }
            }
        }
    }
}
