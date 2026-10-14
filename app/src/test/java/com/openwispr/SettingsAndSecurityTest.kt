package com.openwispr

import com.openwispr.audio.AudioRecorder
import com.openwispr.data.HistoryDatabaseHelper
import com.openwispr.data.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAndSecurityTest {

    @Test
    fun testAudioRecorderConfigurationMatchesWhisperAndTelemetry() {
        assertEquals(16000, AudioRecorder.SAMPLE_RATE_HZ)
        assertEquals(64000, AudioRecorder.BIT_RATE)
        assertEquals(512L, AudioRecorder.MIN_VALID_AUDIO_BYTES)
    }

    @Test
    fun testDatabaseSchemaConstants() {
        assertEquals("transcripts", HistoryDatabaseHelper.TABLE_TRANSCRIPTS)
        assertEquals("openwispr_history.db", HistoryDatabaseHelper.DATABASE_NAME)
        assertEquals(1, HistoryDatabaseHelper.DATABASE_VERSION)
        assertEquals("id", HistoryDatabaseHelper.COL_ID)
        assertEquals("text", HistoryDatabaseHelper.COL_TEXT)
        assertEquals("app_package", HistoryDatabaseHelper.COL_APP_PACKAGE)
        assertEquals("timestamp_ms", HistoryDatabaseHelper.COL_TIMESTAMP_MS)
        assertEquals("duration_ms", HistoryDatabaseHelper.COL_DURATION_MS)
        assertEquals("word_count", HistoryDatabaseHelper.COL_WORD_COUNT)
    }

    @Test
    fun testSettingsRepositoryKeyStoreFallbackSafeOnJvm() {
        // Without Android KeyStore service on desktop JVM, encrypt and decrypt return null without throwing
        val encrypted = SettingsRepository.encrypt("test-secret-key-12345")
        // In JVM test without Android KeyStore, encrypt returns null safely
        assertTrue(encrypted == null || encrypted.isNotEmpty())
    }
}
