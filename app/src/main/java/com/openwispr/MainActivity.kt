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
import com.openwispr.ui.BottomNavDock
import com.openwispr.ui.HistoryScreen
import com.openwispr.ui.HomeScreen
import com.openwispr.ui.NavTab
import com.openwispr.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var groq: GroqRepository
    private lateinit var history: HistoryRepository

    private val micPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository(applicationContext)
        groq = GroqRepository()
        history = HistoryRepository(applicationContext)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF070709),
                    surface = Color(0xFF0E0F14),
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
                            modifier = Modifier.width(280.dp),
                            drawerContainerColor = Color(0xFF0E0F14)
                        ) {
                            // Drawer Header
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(
                                                color = Color(0xFF1E2028),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_mic),
                                            contentDescription = null,
                                            tint = Color(0xFFFF2E2E),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Open Wispr",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "YOUR AI VOICE COMPANION",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF9CA3AF)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                color = Color(0xFF1E2028)
                            )

                            Spacer(Modifier.height(8.dp))

                            // Navigation Items
                            NavigationDrawerItem(
                                label = { Text("Home", fontWeight = FontWeight.SemiBold) },
                                selected = currentTab == NavTab.Home,
                                onClick = {
                                    currentTab = NavTab.Home
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color(0xFF1E2028),
                                    selectedTextColor = Color(0xFFFF2E2E),
                                    unselectedTextColor = Color(0xFFD1D5DB)
                                )
                            )

                            NavigationDrawerItem(
                                label = { Text("History", fontWeight = FontWeight.SemiBold) },
                                selected = currentTab == NavTab.History,
                                onClick = {
                                    currentTab = NavTab.History
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color(0xFF1E2028),
                                    selectedTextColor = Color(0xFFFF2E2E),
                                    unselectedTextColor = Color(0xFFD1D5DB)
                                )
                            )

                            NavigationDrawerItem(
                                label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                                selected = currentTab == NavTab.Settings,
                                onClick = {
                                    currentTab = NavTab.Settings
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color(0xFF1E2028),
                                    selectedTextColor = Color(0xFFFF2E2E),
                                    unselectedTextColor = Color(0xFFD1D5DB)
                                )
                            )
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF070709))
                    ) {
                        when (currentTab) {
                            NavTab.Home, NavTab.Stats -> HomeScreen(
                                historyRepo = history,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNavigateToSettings = { currentTab = NavTab.Settings },
                                onNavigateToHistory = { currentTab = NavTab.History },
                                isServiceActive = isAccessibilityEnabled()
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
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }

                        // Bottom Navigation Dock
                        BottomNavDock(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it },
                            onMicClick = {
                                if (!hasMicPermission()) {
                                    requestMic()
                                } else if (!isAccessibilityEnabled()) {
                                    openAccessibility()
                                } else {
                                    Toast.makeText(this@MainActivity, "Open Wispr is active. Tap any text field to speak.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
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