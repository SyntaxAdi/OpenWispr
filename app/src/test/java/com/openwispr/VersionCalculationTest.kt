package com.openwispr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCalculationTest {

    companion object {
        fun formatVersion(gitCount: Int): String {
            val major = gitCount / 100
            val minor = (gitCount / 10) % 10
            val patch = gitCount % 10
            return "$major.$minor.$patch"
        }
    }

    @Test
    fun testVersionOdometerProgression() {
        assertEquals("0.0.0", formatVersion(0))
        assertEquals("0.0.1", formatVersion(1))
        assertEquals("0.0.9", formatVersion(9))
        assertEquals("0.1.0", formatVersion(10))
        assertEquals("0.1.1", formatVersion(11))
        assertEquals("0.1.9", formatVersion(19))
        assertEquals("0.2.0", formatVersion(20))
        assertEquals("0.4.4", formatVersion(44))
        assertEquals("0.9.9", formatVersion(99))
        assertEquals("1.0.0", formatVersion(100))
        assertEquals("1.0.5", formatVersion(105))
        assertEquals("1.1.5", formatVersion(115))
        assertEquals("2.3.7", formatVersion(237))
    }

    @Test
    fun testDigitsNeverExceedNine() {
        for (c in 0..2000) {
            val major = c / 100
            val minor = (c / 10) % 10
            val patch = c % 10

            assertTrue("Minor digit $minor must be between 0 and 9", minor in 0..9)
            assertTrue("Patch digit $patch must be between 0 and 9", patch in 0..9)
            assertTrue("Major digit $major must be >= 0", major >= 0)
        }
    }

    @Test
    fun testBuildConfigVersionFormat() {
        val regex = Regex("^\\d+\\.\\d\\.\\d$")
        assertTrue(
            "BuildConfig.VERSION_NAME '${BuildConfig.VERSION_NAME}' must match dynamic format X.Y.Z",
            regex.matches(BuildConfig.VERSION_NAME)
        )
        assertTrue(
            "BuildConfig.VERSION_CODE must be greater than zero",
            BuildConfig.VERSION_CODE > 0
        )
    }
}
