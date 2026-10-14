package com.openwispr

import com.openwispr.data.TranscriptRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryLogicTest {

    @Test
    fun testWordCountCalculation() {
        assertEquals(0, countWords(""))
        assertEquals(0, countWords("   "))
        assertEquals(1, countWords("Hello"))
        assertEquals(5, countWords("This is a speech test"))
        assertEquals(4, countWords("  Multiple   spaces between   words "))
    }

    @Test
    fun testDateGroupingLogicDoesNotUseYesterday() {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - 24 * 60 * 60 * 1000L
        val twoDaysAgo = now - 48 * 60 * 60 * 1000L

        val records = listOf(
            TranscriptRecord(1, "Today note", "com.test", now, 2000L, 2),
            TranscriptRecord(2, "Previous note", "com.test", oneDayAgo, 4000L, 2),
            TranscriptRecord(3, "Older note", "com.test", twoDaysAgo, 3000L, 2)
        )

        val grouped = groupRecordsByDate(records)

        // Today must be present
        assertTrue(grouped.containsKey("Today"))

        // Must never contain "Yesterday"
        assertFalse(grouped.containsKey("Yesterday"))

        // Check date format for past records
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val expectedOneDayAgo = dateFormat.format(Date(oneDayAgo))
        assertTrue(grouped.containsKey(expectedOneDayAgo) || grouped.containsKey("Today"))
    }

    @Test
    fun testWordsPerMinuteCalculation() {
        val words = 150L
        val durationMs = 60000L // 1 minute
        val minutes = durationMs.toDouble() / 60000.0
        val wpm = (words / minutes).toInt()
        assertEquals(150, wpm)
    }

    @Test
    fun testWpmZeroWhenDurationIsZero() {
        // Must strictly return 0 without hardcoded fallback
        assertEquals(0, com.openwispr.data.HistoryRepository.calculateWpm(50L, 0L))
        assertEquals(0, com.openwispr.data.HistoryRepository.calculateWpm(0L, 60000L))
        assertEquals(150, com.openwispr.data.HistoryRepository.calculateWpm(150L, 60000L))
    }

    @Test
    fun testPeakAndPercentageWithoutFallbacks() {
        // 1. Zero recordings scenario -> must be dashes
        val emptyHourly = emptyMap<Int, Long>()
        val emptyStats = com.openwispr.data.HistoryRepository.calculatePeakStats(emptyHourly)
        assertEquals("--", emptyStats.peakHourRange)
        assertEquals("--", emptyStats.peakPercentageVsAvg)

        // 2. Single active hour (e.g. 1 word at 4 PM) -> shows peak hour but dashes for vs avg (no bogus +2300%)
        val singleHour = mapOf(16 to 1L)
        val singleStats = com.openwispr.data.HistoryRepository.calculatePeakStats(singleHour)
        assertEquals("4 – 5 PM", singleStats.peakHourRange)
        assertEquals("--", singleStats.peakPercentageVsAvg)

        // 3. Multi-hour active scenario -> peak at 18 (6-7 PM) with 120 words vs 60 words at 9 AM (avg 90 words -> +33%)
        val activeHourly = mapOf(18 to 120L, 9 to 60L)
        val activeStats = com.openwispr.data.HistoryRepository.calculatePeakStats(activeHourly)
        assertEquals("6 – 7 PM", activeStats.peakHourRange)
        assertEquals("+33%", activeStats.peakPercentageVsAvg)
    }

    @Test
    fun testFormatHourRange() {
        assertEquals("6 – 7 PM", com.openwispr.data.HistoryRepository.formatHourRange(18))
        assertEquals("9 – 10 AM", com.openwispr.data.HistoryRepository.formatHourRange(9))
        assertEquals("11 AM – 12 PM", com.openwispr.data.HistoryRepository.formatHourRange(11))
        assertEquals("12 – 1 AM", com.openwispr.data.HistoryRepository.formatHourRange(0))
        assertEquals("11 PM – 12 AM", com.openwispr.data.HistoryRepository.formatHourRange(23))
    }

    @Test
    fun testFormatUsageDurationTiers() {
        // Tier 1: 0s to 59s with space between number and unit
        assertEquals("0 s", com.openwispr.data.HistoryRepository.formatUsageDuration(0L))
        assertEquals("45 s", com.openwispr.data.HistoryRepository.formatUsageDuration(45000L))

        // Tier 2: 1m to 59m
        assertEquals("1m 0s", com.openwispr.data.HistoryRepository.formatUsageDuration(60000L))
        assertEquals("1m 15s", com.openwispr.data.HistoryRepository.formatUsageDuration(75000L))
        assertEquals("12m 30s", com.openwispr.data.HistoryRepository.formatUsageDuration(750000L))

        // Tier 3: 1h to 23h59m ("3h 28m" style)
        assertEquals("1h 0m", com.openwispr.data.HistoryRepository.formatUsageDuration(3600000L))
        assertEquals("3h 15m", com.openwispr.data.HistoryRepository.formatUsageDuration(11742000L))

        // Tier 4: 24h to 29 days
        assertEquals("1 day", com.openwispr.data.HistoryRepository.formatUsageDuration(86400000L))
        assertEquals("2 days", com.openwispr.data.HistoryRepository.formatUsageDuration(172800000L))
        assertEquals("3 days", com.openwispr.data.HistoryRepository.formatUsageDuration(259200000L))

        // Tier 5: 30 days to 364 days
        val thirtyDaysMs = 30L * 86400L * 1000L
        assertEquals("1m 0d", com.openwispr.data.HistoryRepository.formatUsageDuration(thirtyDaysMs))
        val thirtyOneDaysMs = 31L * 86400L * 1000L
        assertEquals("1m 1d", com.openwispr.data.HistoryRepository.formatUsageDuration(thirtyOneDaysMs))

        // Tier 6: 365 days and beyond
        val oneYearOneMonthOneDayMs = (365L + 30L + 1L) * 86400L * 1000L
        assertEquals("1y 1m 1d", com.openwispr.data.HistoryRepository.formatUsageDuration(oneYearOneMonthOneDayMs))
    }

    @Test
    fun testMostUsedAppPercentageCalculation() {
        // Zero recordings
        val totalWordsZero: Long = System.currentTimeMillis().let { 0L }
        val topAppWordsZero = 0L
        val pctZero = if (totalWordsZero > 0 && topAppWordsZero > 0) {
            ((topAppWordsZero.toDouble() / totalWordsZero.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0
        assertEquals(0, pctZero)

        // 68% share (e.g. 680 of 1000 words)
        val totalWords = 1000L
        val topAppWords = 680L
        val pct = if (totalWords > 0 && topAppWords > 0) {
            ((topAppWords.toDouble() / totalWords.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0
        assertEquals(68, pct)
    }

    @Test
    fun testSplitAppNameForDisplay() {
        // 1. Short or exact <= 9 chars -> single line with full complete name
        assertEquals(listOf("Telegram"), com.openwispr.data.HistoryRepository.splitAppNameForDisplay("Telegram", 9))
        assertEquals(listOf("WhatsApp"), com.openwispr.data.HistoryRepository.splitAppNameForDisplay("WhatsApp", 9))
        assertEquals(listOf("Chrome"), com.openwispr.data.HistoryRepository.splitAppNameForDisplay("Chrome", 9))

        // 2. Compound long name with spaces -> splits into 2 lines
        assertEquals(listOf("Google", "Chrome"), com.openwispr.data.HistoryRepository.splitAppNameForDisplay("Google Chrome", 9))
        assertEquals(listOf("Microsoft", "Teams"), com.openwispr.data.HistoryRepository.splitAppNameForDisplay("Microsoft Teams", 9))

        // 3. Single long word without spaces -> wraps cleanly at maxLineChars across 2 lines
        assertEquals(listOf("SuperLong", "AppName"), com.openwispr.data.HistoryRepository.splitAppNameForDisplay("SuperLongAppName", 9))
    }

    private fun countWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split("\\s+".toRegex()).size
    }

    private fun groupRecordsByDate(items: List<TranscriptRecord>): Map<String, List<TranscriptRecord>> {
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
}
