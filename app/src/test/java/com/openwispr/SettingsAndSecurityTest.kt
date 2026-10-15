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

            // Verify Strategic UX Optimizations
            assertTrue("SettingsScreen must contain unified AiCloudEngines section", content.contains("AiCloudEngines"))
            assertTrue("SettingsScreen must contain PASTE button", content.contains("\"PASTE\""))
            assertTrue("SettingsScreen must contain TEST LATENCY button", content.contains("TEST LATENCY"))
            assertTrue("SettingsScreen must display latency telemetry", content.contains("ms)"))
            assertTrue("SettingsScreen must eliminate manual save button", !content.contains("SAVE API KEY"))
            assertTrue("SettingsScreen must contain SIZE PRESETS", content.contains("SIZE PRESETS"))
            assertTrue("SettingsScreen must contain COMPACT preset", content.contains("\"COMPACT\""))
            assertTrue("SettingsScreen must contain BALANCED preset", content.contains("\"BALANCED\""))
            assertTrue("SettingsScreen must contain LARGE preset", content.contains("\"LARGE\""))
            assertTrue("SettingsScreen must trigger haptic feedback", content.contains("HapticFeedbackType.TextHandleMove"))
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

    @Test
    fun testSecurityAuditRemediations() {
        // 1. Verify QUERY_ALL_PACKAGES removed from AndroidManifest.xml
        val manifestFile = java.io.File("src/main/AndroidManifest.xml")
        if (manifestFile.exists()) {
            val manifestContent = manifestFile.readText()
            assertTrue("AndroidManifest must NOT contain QUERY_ALL_PACKAGES", !manifestContent.contains("QUERY_ALL_PACKAGES"))
        }

        // 2. Verify HTTPS enforcement in GroqRepository
        val validHttps = com.openwispr.data.GroqRepository.validateEndpointUrl("https://api.groq.com/openai/v1/")
        assertEquals("https://api.groq.com/openai/v1", validHttps)

        var threwInsecure = false
        try {
            com.openwispr.data.GroqRepository.validateEndpointUrl("http://insecure-api.groq.com/v1")
        } catch (_: IllegalArgumentException) {
            threwInsecure = true
        }
        assertTrue("GroqRepository must reject http:// scheme with IllegalArgumentException", threwInsecure)

        // 3. Verify Password field protection in TextInjector
        val textInjectorFile = java.io.File("src/main/java/com/openwispr/util/TextInjector.kt")
        if (textInjectorFile.exists()) {
            val tiContent = textInjectorFile.readText()
            assertTrue("TextInjector must implement isPasswordField", tiContent.contains("fun isPasswordField"))
            assertTrue("TextInjector must block password fields", tiContent.contains("Voice dictation disabled on password fields"))
            assertTrue("TextInjector must protect clipboard with EXTRA_IS_SENSITIVE", tiContent.contains("EXTRA_IS_SENSITIVE"))
        }

        // 4. Verify Orphan Audio Cache Purging in AudioRecorder
        val audioRecorderFile = java.io.File("src/main/java/com/openwispr/audio/AudioRecorder.kt")
        if (audioRecorderFile.exists()) {
            val arContent = audioRecorderFile.readText()
            assertTrue("AudioRecorder must contain cleanOrphanRecordings", arContent.contains("fun cleanOrphanRecordings"))
        }

        // 5. Verify Data Privacy & History Purge
        val historyRepoFile = java.io.File("src/main/java/com/openwispr/data/HistoryRepository.kt")
        if (historyRepoFile.exists()) {
            val hrContent = historyRepoFile.readText()
            assertTrue("HistoryRepository must contain clearAllTranscripts", hrContent.contains("fun clearAllTranscripts"))
        }

        val settingsFile = java.io.File("src/main/java/com/openwispr/ui/SettingsScreen.kt")
        if (settingsFile.exists()) {
            val sContent = settingsFile.readText()
            assertTrue("SettingsScreen must contain clearAllTranscripts", sContent.contains("clearAllTranscripts"))
            assertTrue("SettingsScreen must contain PURGE TRANSCRIPT HISTORY", sContent.contains("PURGE TRANSCRIPT HISTORY"))
        }
    }
}

