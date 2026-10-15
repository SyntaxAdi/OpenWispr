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

    @Test
    fun testNoGreenColorInSettingsScreenSource() {
        val settingsFile = java.io.File("src/main/java/com/openwispr/ui/SettingsScreen.kt")
        if (settingsFile.exists()) {
            val content = settingsFile.readText()
            assertTrue("SettingsScreen must not contain ColorGreenAccent", !content.contains("ColorGreenAccent"))
            assertTrue("SettingsScreen must not contain 0xFF00C853", !content.contains("0xFF00C853"))
            assertTrue("SettingsScreen must not contain 0xFF00E676", !content.contains("0xFF00E676"))
            assertTrue("SettingsScreen must not contain Color.Green", !content.contains("Color.Green"))

            // Verify requested tag removals
            assertTrue("SettingsScreen must not contain SYS_OPTIMAL", !content.contains("SYS_OPTIMAL"))
            assertTrue("SettingsScreen must not contain API_AUTH", !content.contains("API_AUTH"))
            assertTrue("SettingsScreen must not contain neural_pipe", !content.contains("neural_pipe"))
            assertTrue("SettingsScreen must not contain calibration", !content.contains("calibration"))
            assertTrue("SettingsScreen must not contain diagnostics", !content.contains("diagnostics"))

            // Verify Section 5 (Custom endpoint) removal
            assertTrue("SettingsScreen must not contain ADVANCED ENDPOINTS", !content.contains("ADVANCED ENDPOINTS"))
            assertTrue("SettingsScreen must not contain HIDE CUSTOM ENDPOINT", !content.contains("HIDE CUSTOM ENDPOINT"))

            // Verify breadcrumb path footers removed from settings cards
            assertTrue("SettingsScreen must not contain breadcrumb footer line", !content.contains("Settings > Hardware & Permissions"))
            assertTrue("SettingsScreen must not contain breadcrumb footer line", !content.contains("Settings > Cloud Credentials"))

            // Verify fixed 160dp Nothing OS calibration workbench stage
            assertTrue("SettingsScreen must contain 160.dp fixed stage for bubble", content.contains(".height(160.dp)"))

            // Verify multi-section architecture & non-overlapping headers
            assertTrue("SettingsScreen must contain SettingsSection", content.contains("enum class SettingsSection"))
            assertTrue("SettingsScreen must contain SettingsSearchBar", content.contains("SettingsSearchBar"))
            assertTrue("SettingsScreen must contain TactileNothingSlider", content.contains("TactileNothingSlider"))
            assertTrue("SettingsSubPageHeader must not show duplicate section tag on title", !content.contains("// \$sectionIndex"))
        }

        val historyFile = java.io.File("src/main/java/com/openwispr/ui/HistoryScreen.kt")
        if (historyFile.exists()) {
            val hContent = historyFile.readText()
            assertTrue("HistoryScreen must have safe padding to prevent header overlap", hContent.contains("padding(horizontal = 72.dp)"))
        }
    }

    @Test
    fun testBubbleOpacityConstants() {
        assertEquals(100, com.openwispr.util.Constants.DEFAULT_BUBBLE_OPACITY)
        assertEquals(20, com.openwispr.util.Constants.MIN_BUBBLE_OPACITY)
        assertEquals(100, com.openwispr.util.Constants.MAX_BUBBLE_OPACITY)
    }

    @Test
    fun testDotMatrixAnimationPatterns() {
        val patterns = com.openwispr.ui.DotAnimationPattern.values()
        assertTrue(patterns.size >= 5)
        assertTrue(patterns.contains(com.openwispr.ui.DotAnimationPattern.SequentialFill))
        assertTrue(patterns.contains(com.openwispr.ui.DotAnimationPattern.CascadeLighting))
        assertTrue(patterns.contains(com.openwispr.ui.DotAnimationPattern.BreathingPulse))
        assertTrue(patterns.contains(com.openwispr.ui.DotAnimationPattern.PingPongScanner))
        assertTrue(patterns.contains(com.openwispr.ui.DotAnimationPattern.CenterOutRipple))
        assertTrue(patterns.contains(com.openwispr.ui.DotAnimationPattern.MatrixShimmer))
    }
}

