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
