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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.openwispr.data.GroqRepository
import com.openwispr.data.HistoryRepository
import com.openwispr.data.SettingsRepository
import com.openwispr.ui.HistoryScreen
import com.openwispr.ui.HomeScreen
import com.openwispr.ui.PersonaScreen
import com.openwispr.ui.SettingsScreen
import com.openwispr.ui.SideNavDrawer
import kotlinx.coroutines.launch

enum class NavTab {
    Home, History, Persona, Settings
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
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var currentTab by remember { mutableStateOf(NavTab.Home) }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color(0xFF000000),
                            drawerContentColor = Color.White,
                            modifier = Modifier.width(300.dp)
                        ) {
                            SideNavDrawer(
                                currentTab = currentTab,
                                onSelectTab = { tab ->
                                    currentTab = tab
                                    scope.launch { drawerState.close() }
                                },
                                onClose = { scope.launch { drawerState.close() } },
                                isAccessibilityActive = isAccessibilityEnabled(),
                                hasMicPermission = hasMicPermission(),
                                isOpen = drawerState.isOpen
                            )
                        }
                    },
                    scrimColor = Color.Black.copy(alpha = 0.65f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF000000))
                    ) {
                        when (currentTab) {
                            NavTab.Home -> HomeScreen(
                                historyRepo = history,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNavigateToHistory = { currentTab = NavTab.History }
                            )
                            NavTab.History -> HistoryScreen(
                                historyRepo = history,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onBackToHome = { currentTab = NavTab.Home }
                            )
                            NavTab.Persona -> PersonaScreen(
                                settings = settings,
                                onOpenDrawer = { scope.launch { drawerState.open() } }
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
                                onOpenDrawer = { scope.launch { drawerState.open() } }
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