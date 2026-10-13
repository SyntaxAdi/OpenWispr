package com.openwispr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import com.openwispr.data.GroqRepository
import com.openwispr.data.SettingsRepository
import com.openwispr.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var groq: GroqRepository

    private val micPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository(applicationContext)
        groq = GroqRepository()
        setContent {
            MaterialTheme {
                SettingsScreen(
                    settings = settings,
                    groq = groq,
                    onRequestMic = { requestMic() },
                    onRequestNotif = { requestNotif() },
                    onOpenAccessibility = { openAccessibility() },
                    onOpenOverlay = { openOverlay() },
                    isAccessibilityEnabled = { isAccessibilityEnabled() },
                    hasMicPermission = { hasMicPermission() },
                    canDrawOverlays = { Settings.canDrawOverlays(this) },
                    hasNotifPermission = { hasNotifPermission() }
                )
            }
        }
    }

    private fun hasMicPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    private fun hasNotifPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    private fun requestMic() { micPerm.launch(Manifest.permission.RECORD_AUDIO) }
    private fun requestNotif() { if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) }
    private fun openAccessibility() { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
    private fun openOverlay() { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
    private fun isAccessibilityEnabled(): Boolean {
        val expected = "$packageName/${packageName}.service.WisprAccessibilityService"
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) if (splitter.next().equals(expected, true)) return true
        return false
    }
}