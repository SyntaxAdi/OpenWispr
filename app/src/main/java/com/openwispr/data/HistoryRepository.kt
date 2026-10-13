package com.openwispr.data

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class TranscriptRecord(
    val id: Long,
    val text: String,
    val appPackage: String,
    val timestampMs: Long,
    val durationMs: Long,
    val wordCount: Int
)

data class HomeStats(
    val totalWords: Long,
    val weekStreak: Int,
    val activeAppsCount: Int,
    val wordsPerMinute: Int
)

class HistoryRepository(context: Context) {
    private val dbHelper = HistoryDatabaseHelper(context.applicationContext)

    suspend fun insertTranscript(text: String, appPackage: String, durationMs: Long): Long = withContext(Dispatchers.IO) {
        val words = countWords(text)
        val cv = ContentValues().apply {
            put(HistoryDatabaseHelper.COL_TEXT, text)
            put(HistoryDatabaseHelper.COL_APP_PACKAGE, appPackage)
            put(HistoryDatabaseHelper.COL_TIMESTAMP_MS, System.currentTimeMillis())
            put(HistoryDatabaseHelper.COL_DURATION_MS, durationMs.coerceAtLeast(0L))
            put(HistoryDatabaseHelper.COL_WORD_COUNT, words)
        }
        val db = dbHelper.writableDatabase
        db.insert(HistoryDatabaseHelper.TABLE_TRANSCRIPTS, null, cv)
    }

    suspend fun deleteTranscript(id: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val rows = db.delete(
            HistoryDatabaseHelper.TABLE_TRANSCRIPTS,
            "${HistoryDatabaseHelper.COL_ID} = ?",
            arrayOf(id.toString())
        )
        rows > 0
    }

    suspend fun getAllTranscripts(): List<TranscriptRecord> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<TranscriptRecord>()
        val cursor = db.query(
            HistoryDatabaseHelper.TABLE_TRANSCRIPTS,
            null,
            null,
            null,
            null,
            null,
            "${HistoryDatabaseHelper.COL_TIMESTAMP_MS} DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(HistoryDatabaseHelper.COL_ID)
            val textIdx = c.getColumnIndexOrThrow(HistoryDatabaseHelper.COL_TEXT)
            val appIdx = c.getColumnIndexOrThrow(HistoryDatabaseHelper.COL_APP_PACKAGE)
            val timeIdx = c.getColumnIndexOrThrow(HistoryDatabaseHelper.COL_TIMESTAMP_MS)
            val durIdx = c.getColumnIndexOrThrow(HistoryDatabaseHelper.COL_DURATION_MS)
            val wordsIdx = c.getColumnIndexOrThrow(HistoryDatabaseHelper.COL_WORD_COUNT)

            while (c.moveToNext()) {
                list.add(
                    TranscriptRecord(
                        id = c.getLong(idIdx),
                        text = c.getString(textIdx),
                        appPackage = c.getString(appIdx),
                        timestampMs = c.getLong(timeIdx),
                        durationMs = c.getLong(durIdx),
                        wordCount = c.getInt(wordsIdx)
                    )
                )
            }
        }
        list
    }

    suspend fun getHomeStats(): HomeStats = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase

        // 1. Total words & total duration
        var totalWords = 0L
        var totalDurationMs = 0L
        val cursorWords = db.rawQuery(
            "SELECT SUM(${HistoryDatabaseHelper.COL_WORD_COUNT}), SUM(${HistoryDatabaseHelper.COL_DURATION_MS}) FROM ${HistoryDatabaseHelper.TABLE_TRANSCRIPTS}",
            null
        )
        cursorWords.use { c ->
            if (c.moveToFirst()) {
                totalWords = c.getLong(0)
                totalDurationMs = c.getLong(1)
            }
        }

        // 2. Active apps count
        var activeAppsCount = 0
        val cursorApps = db.rawQuery(
            "SELECT COUNT(DISTINCT ${HistoryDatabaseHelper.COL_APP_PACKAGE}) FROM ${HistoryDatabaseHelper.TABLE_TRANSCRIPTS}",
            null
        )
        cursorApps.use { c ->
            if (c.moveToFirst()) {
                activeAppsCount = c.getInt(0)
            }
        }

        // 3. Current week streak (distinct days active in current calendar week)
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekStartMs = cal.timeInMillis

        var weekStreak = 0
        val cursorStreak = db.rawQuery(
            """
            SELECT COUNT(DISTINCT strftime('%Y-%m-%d', ${HistoryDatabaseHelper.COL_TIMESTAMP_MS} / 1000, 'unixepoch', 'localtime'))
            FROM ${HistoryDatabaseHelper.TABLE_TRANSCRIPTS}
            WHERE ${HistoryDatabaseHelper.COL_TIMESTAMP_MS} >= ?
            """.trimIndent(),
            arrayOf(weekStartMs.toString())
        )
        cursorStreak.use { c ->
            if (c.moveToFirst()) {
                weekStreak = c.getInt(0)
            }
        }

        // 4. Words per minute (WPM)
        val wpm = if (totalDurationMs > 0 && totalWords > 0) {
            val minutes = totalDurationMs.toDouble() / 60000.0
            (totalWords / minutes).toInt().coerceIn(1, 400)
        } else if (totalWords > 0) {
            135 // reasonable speaking baseline if duration unrecorded
        } else {
            0
        }

        HomeStats(
            totalWords = totalWords,
            weekStreak = weekStreak,
            activeAppsCount = activeAppsCount,
            wordsPerMinute = wpm
        )
    }

    private fun countWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split("\\s+".toRegex()).size
    }
}
