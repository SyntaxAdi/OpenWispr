package com.openwispr.data

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

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
    val wordsPerMinute: Int,
    val totalUsageMinutes: Long = 0L,
    val totalDurationMs: Long = 0L,
    val formattedTotalUsage: String = "0 s",
    val mostUsedAppPackage: String? = null,
    val mostUsedAppName: String = "--",
    val mostUsedAppWords: Long = 0L,
    val mostUsedAppPercentage: Int = 0,
    val weeklyDayCounts: Map<Int, Long> = emptyMap(),
    val activeWeekdays: Set<Int> = emptySet(), // 0=Sunday, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat
    val hourlyActivityCounts: Map<Int, Long> = emptyMap(), // 0..23 hours
    val peakHourRange: String = "--",
    val peakPercentageVsAvg: String = "--"
)

class HistoryRepository(private val context: Context) {
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

        // 2. Active apps count and most used app by word count
        var activeAppsCount = 0
        var mostUsedAppPackage: String? = null
        var mostUsedAppWords = 0L

        val cursorApps = db.rawQuery(
            """
            SELECT ${HistoryDatabaseHelper.COL_APP_PACKAGE}, SUM(${HistoryDatabaseHelper.COL_WORD_COUNT}) as total_app_words
            FROM ${HistoryDatabaseHelper.TABLE_TRANSCRIPTS}
            WHERE ${HistoryDatabaseHelper.COL_APP_PACKAGE} IS NOT NULL AND ${HistoryDatabaseHelper.COL_APP_PACKAGE} != ''
            GROUP BY ${HistoryDatabaseHelper.COL_APP_PACKAGE}
            ORDER BY total_app_words DESC
            """.trimIndent(),
            null
        )
        cursorApps.use { c ->
            val pkgIdx = c.getColumnIndex(HistoryDatabaseHelper.COL_APP_PACKAGE)
            val sumIdx = c.getColumnIndex("total_app_words")
            while (c.moveToNext()) {
                activeAppsCount++
                if (mostUsedAppPackage == null && pkgIdx >= 0 && sumIdx >= 0) {
                    mostUsedAppPackage = c.getString(pkgIdx)
                    mostUsedAppWords = c.getLong(sumIdx)
                }
            }
        }

        val mostUsedPercentage = if (totalWords > 0 && mostUsedAppWords > 0) {
            ((mostUsedAppWords.toDouble() / totalWords.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val mostUsedAppName = resolveAppName(context, mostUsedAppPackage)

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

        // 4. Weekly distribution and active weekdays (0=Sun, 1=Mon, ..., 6=Sat)
        val weeklyDayCounts = mutableMapOf<Int, Long>()
        val activeWeekdays = mutableSetOf<Int>()
        val cursorWeekly = db.rawQuery(
            """
            SELECT strftime('%w', ${HistoryDatabaseHelper.COL_TIMESTAMP_MS} / 1000, 'unixepoch', 'localtime') AS day_of_week,
                   SUM(${HistoryDatabaseHelper.COL_WORD_COUNT})
            FROM ${HistoryDatabaseHelper.TABLE_TRANSCRIPTS}
            WHERE ${HistoryDatabaseHelper.COL_TIMESTAMP_MS} >= ?
            GROUP BY day_of_week
            """.trimIndent(),
            arrayOf(weekStartMs.toString())
        )
        cursorWeekly.use { c ->
            while (c.moveToNext()) {
                val wDay = c.getString(0).toIntOrNull() ?: continue
                val sumWords = c.getLong(1)
                activeWeekdays.add(wDay)
                val mondayBased = if (wDay == 0) 7 else wDay
                weeklyDayCounts[mondayBased] = sumWords
            }
        }

        // 5. Hourly activity distribution (0..23 hours)
        val hourlyCounts = mutableMapOf<Int, Long>()
        val cursorHourly = db.rawQuery(
            """
            SELECT CAST(strftime('%H', ${HistoryDatabaseHelper.COL_TIMESTAMP_MS} / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hr,
                   SUM(${HistoryDatabaseHelper.COL_WORD_COUNT})
            FROM ${HistoryDatabaseHelper.TABLE_TRANSCRIPTS}
            GROUP BY hr
            """.trimIndent(),
            null
        )
        cursorHourly.use { c ->
            while (c.moveToNext()) {
                val hr = c.getInt(0)
                val sum = c.getLong(1)
                hourlyCounts[hr] = sum
            }
        }

        val peakEntry = hourlyCounts.filter { it.value > 0 }.maxByOrNull { it.value }
        val peakHourRange = if (peakEntry != null) {
            formatHourRange(peakEntry.key)
        } else {
            "--"
        }

        val totalWordsRecorded = hourlyCounts.values.sum()
        val avgWordsPerHour = if (totalWordsRecorded > 0) {
            totalWordsRecorded.toDouble() / 24.0
        } else 0.0

        val peakPercentageVsAvg = if (peakEntry != null && avgWordsPerHour > 0) {
            val pct = (((peakEntry.value - avgWordsPerHour) / avgWordsPerHour) * 100).toInt()
            if (pct >= 0) "+$pct%" else "$pct%"
        } else {
            "--"
        }

        // 6. Words per minute (WPM) strictly from real recorded duration and words
        val wpm = calculateWpm(totalWords, totalDurationMs)

        val totalMinutes = (totalDurationMs / 60000L).coerceAtLeast(0L)
        val formattedUsage = formatUsageDuration(totalDurationMs)

        HomeStats(
            totalWords = totalWords,
            weekStreak = weekStreak,
            activeAppsCount = activeAppsCount,
            wordsPerMinute = wpm,
            totalUsageMinutes = totalMinutes,
            totalDurationMs = totalDurationMs,
            formattedTotalUsage = formattedUsage,
            mostUsedAppPackage = mostUsedAppPackage,
            mostUsedAppName = mostUsedAppName,
            mostUsedAppWords = mostUsedAppWords,
            mostUsedAppPercentage = mostUsedPercentage,
            weeklyDayCounts = weeklyDayCounts,
            activeWeekdays = activeWeekdays,
            hourlyActivityCounts = hourlyCounts,
            peakHourRange = peakHourRange,
            peakPercentageVsAvg = peakPercentageVsAvg
        )
    }

    companion object {
        fun resolveAppName(context: Context, packageName: String?): String {
            if (packageName.isNullOrBlank()) return "--"
            return try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                when {
                    packageName.contains("inputmethod.latin") -> "Gboard"
                    packageName.contains("whatsapp") -> "WhatsApp"
                    packageName.contains("chrome") -> "Chrome"
                    packageName.contains("telegram") -> "Telegram"
                    else -> packageName.substringAfterLast('.').replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                }
            }
        }

        fun calculateWpm(totalWords: Long, totalDurationMs: Long): Int {
            if (totalDurationMs <= 0L || totalWords <= 0L) return 0
            val minutes = totalDurationMs.toDouble() / 60000.0
            return (totalWords / minutes).toInt().coerceIn(1, 400)
        }

        fun formatHourRange(hour: Int): String {
            val startAmPm = if (hour < 12) "AM" else "PM"
            val endHour = (hour + 1) % 24
            val endAmPm = if (endHour < 12) "AM" else "PM"
            val displayStart = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val displayEnd = if (endHour == 0) 12 else if (endHour > 12) endHour - 12 else endHour
            return if (startAmPm == endAmPm) "$displayStart – $displayEnd $endAmPm" else "$displayStart $startAmPm – $displayEnd $endAmPm"
        }

        fun formatUsageDuration(durationMs: Long): String {
            val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
            return when {
                // Tier 1: 0 to 59 seconds -> "0 s", "45 s" (spaced)
                totalSeconds < 60L -> "$totalSeconds s"

                // Tier 2: 1 minute up to 59 minutes -> "1m 0s", "12m 30s"
                totalSeconds < 3600L -> {
                    val m = totalSeconds / 60L
                    val s = totalSeconds % 60L
                    "${m}m ${s}s"
                }

                // Tier 3: 1 hour up to 23 hours 59 minutes -> "1h 0m", "3h 28m"
                totalSeconds < 86400L -> {
                    val h = totalSeconds / 3600L
                    val m = (totalSeconds % 3600L) / 60L
                    "${h}h ${m}m"
                }

                // Tier 4: 24 hours up to 29 days -> "1 day", "2 days", "3 days"
                totalSeconds < 2592000L -> {
                    val days = totalSeconds / 86400L
                    if (days == 1L) "1 day" else "$days days"
                }

                // Tier 5: 30 days up to 364 days -> "1m 1d", "3m 12d"
                totalSeconds < 31536000L -> {
                    val months = totalSeconds / 2592000L
                    val days = (totalSeconds % 2592000L) / 86400L
                    "${months}m ${days}d"
                }

                // Tier 6: 365 days and beyond -> "1y 1m 1d"
                else -> {
                    val years = totalSeconds / 31536000L
                    val remAfterYear = totalSeconds % 31536000L
                    val months = remAfterYear / 2592000L
                    val days = (remAfterYear % 2592000L) / 86400L
                    "${years}y ${months}m ${days}d"
                }
            }
        }
    }

    private fun countWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split("\\s+".toRegex()).size
    }
}
