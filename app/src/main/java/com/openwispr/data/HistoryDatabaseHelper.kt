package com.openwispr.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HistoryDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TRANSCRIPTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TEXT TEXT NOT NULL,
                $COL_APP_PACKAGE TEXT NOT NULL,
                $COL_TIMESTAMP_MS INTEGER NOT NULL,
                $COL_DURATION_MS INTEGER NOT NULL DEFAULT 0,
                $COL_WORD_COUNT INTEGER NOT NULL
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transcripts_timestamp ON $TABLE_TRANSCRIPTS($COL_TIMESTAMP_MS DESC);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transcripts_app ON $TABLE_TRANSCRIPTS($COL_APP_PACKAGE);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Simple schema migration strategy
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSCRIPTS")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "openwispr_history.db"
        const val DATABASE_VERSION = 1

        const val TABLE_TRANSCRIPTS = "transcripts"
        const val COL_ID = "id"
        const val COL_TEXT = "text"
        const val COL_APP_PACKAGE = "app_package"
        const val COL_TIMESTAMP_MS = "timestamp_ms"
        const val COL_DURATION_MS = "duration_ms"
        const val COL_WORD_COUNT = "word_count"
    }
}
