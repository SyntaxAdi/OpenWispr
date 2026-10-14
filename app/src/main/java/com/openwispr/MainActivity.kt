package com.openwispr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.openwispr.data.GroqRepository
import com.openwispr.data.HistoryRepository
import com.openwispr.data.SettingsRepository
import com.openwispr.ui.HistoryScreen
import com.openwispr.ui.HomeScreen
import com.openwispr.ui.InteractiveDrawer
import com.openwispr.ui.SettingsScreen
import com.openwispr.ui.SideNavDrawer
import kotlinx.coroutines.launch

enum class NavTab {
    Home, History, Stats, Settings
}

class MainActivity : ComponentActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var groq: GroqRepository
    private lateinit var history: HistoryRepository

    private val micPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        settings = SettingsRepository(applicationContext)
        groq = GroqRepository()
        history = HistoryRepository(applicationContext)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF000000),
                    surface = Color(0xFF070709),
                    primary = Color(0xFFFF2E2E),
                    onPrimary = Color.White
                )
            ) {
                var isDrawerOpen by remember { mutableStateOf(false) }
                var currentTab by remember { mutableStateOf(NavTab.Home) }

                InteractiveDrawer(
                    isOpen = isDrawerOpen,
                    onOpen = { isDrawerOpen = true },
                    onClose = { isDrawerOpen = false },
                    drawerWidth = 300.dp,
                    drawerContent = {
                        SideNavDrawer(
                            currentTab = currentTab,
                            onSelectTab = { tab ->
                                currentTab = tab
                                isDrawerOpen = false
                            },
                            onClose = { isDrawerOpen = false },
                            isAccessibilityActive = isAccessibilityEnabled(),
                            hasMicPermission = hasMicPermission()
                        )
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF000000))
                    ) {
                        when (currentTab) {
                            NavTab.Home, NavTab.Stats -> HomeScreen(
                                historyRepo = history,
                                onOpenDrawer = { isDrawerOpen = true },
                                onNavigateToHistory = { currentTab = NavTab.History }
                            )
                            NavTab.History -> HistoryScreen(
                                historyRepo = history,
                                onBackToHome = { currentTab = NavTab.Home }
                            )
                            NavTab.Settings -> SettingsScreen(
                                settings = settings,
                                groq = groq,
                                onRequestMic = { requestMic() },
                                onRequestNotif = { requestNotif() },
                                onOpenAccessibility = { openAccessibility() },
                                onOpenOverlay = { openOverlay() },
                                isAccessibilityEnabled = { isAccessibilityEnabled() },
                                hasMicPermission = { hasMicPermission() },
                                canDrawOverlays = { Settings.canDrawOverlays(this@MainActivity) },
                                hasNotifPermission = { hasNotifPermission() },
                                onOpenDrawer = { isDrawerOpen = true }
                            )
                        }
                    }
                }
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