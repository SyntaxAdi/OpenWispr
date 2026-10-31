package com.openwispr.ui

import com.openwispr.BuildConfig
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openwispr.R
import com.openwispr.data.GroqRepository
import com.openwispr.data.GroqResult
import com.openwispr.data.SettingsRepository
import com.openwispr.util.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Deep OLED Pitch Black Palette (Identical to HomeScreen Nothing OS Spec)
private val ColorDarkBg = Color(0xFF000000)
private val ColorCardBg = Color(0xFF070709)
private val ColorCardElevated = Color(0xFF101014)
private val ColorSurfaceInput = Color(0xFF0C0D12)
private val ColorBorder = Color(0xFF141416)
private val ColorBorderSubtle = Color(0xFF262832)
private val ColorRedAccent = Color(0xFFFF2E2E)
private val ColorMuted = Color(0xFF8E95A2)
private val ColorSubtle = Color(0xFF555B66)

enum class SettingsSection(val title: String, val sectionIndex: String) {
    HardwarePermissions("Hardware & Permissions", "01 HARDWARE"),
    AiCloudEngines("Groq Cloud & AI Engines", "02 AI CLOUD"),
    BubbleAndInterface("Floating Bubble & Timing", "03 INTERFACE"),
    SnoozeSettings("Snooze & Dismissal", "04 SNOOZE"),
    SystemTelemetry("System & Runtime Telemetry", "05 TELEMETRY")
}

data class SearchableSetting(
    val title: String,
    val description: String,
    val keywords: List<String>,
    val section: SettingsSection
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    groq: GroqRepository,
    onRequestMic: () -> Unit,
    onRequestNotif: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    isAccessibilityEnabled: () -> Boolean,
    hasMicPermission: () -> Boolean,
    canDrawOverlays: () -> Boolean,
    hasNotifPermission: () -> Boolean,
    onOpenDrawer: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current

    var currentSection by remember { mutableStateOf<SettingsSection?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    androidx.activity.compose.BackHandler(enabled = currentSection != null) {
        currentSection = null
    }

    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var whisper by remember { mutableStateOf(settings.whisper) }
    var llama by remember { mutableStateOf(settings.llama) }
    var bubbleDp by remember { mutableIntStateOf(settings.bubbleDp) }
    var bubbleOpacity by remember { mutableIntStateOf(settings.bubbleOpacity) }
    var maxSec by remember { mutableIntStateOf(settings.maxSec) }
    var snoozeMinutes by remember { mutableIntStateOf(settings.snoozeDurationMinutes) }
    var isSnoozed by remember { mutableStateOf(settings.isSnoozed()) }
    var showKey by remember { mutableStateOf(false) }
    var validating by remember { mutableStateOf(false) }
    var validateMsg by remember { mutableStateOf<String?>(null) }
    var validateOk by remember { mutableStateOf<Boolean?>(null) }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = ColorSurfaceInput,
        unfocusedContainerColor = ColorSurfaceInput,
        disabledContainerColor = ColorSurfaceInput,
        focusedBorderColor = ColorRedAccent,
        unfocusedBorderColor = ColorBorderSubtle,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = ColorRedAccent,
        unfocusedLabelColor = ColorMuted,
        cursorColor = ColorRedAccent
    )

    val accOk = isAccessibilityEnabled()
    val micOk = hasMicPermission()
    val overlayOk = canDrawOverlays()
    val notifOk = hasNotifPermission()

    val totalPerms = if (Build.VERSION.SDK_INT >= 33) 4 else 3
    val activePerms = listOf(accOk, micOk, overlayOk).count { it } +
        if (Build.VERSION.SDK_INT >= 33 && notifOk) 1 else 0

    val allSearchable = remember {
        listOf(
            SearchableSetting(
                title = "Accessibility Service",
                description = "Window monitoring & automated text injection into target apps",
                keywords = listOf("accessibility", "service", "permission", "inject", "window", "hardware"),
                section = SettingsSection.HardwarePermissions
            ),
            SearchableSetting(
                title = "Microphone Access",
                description = "16kHz hardware voice buffer streaming for live dictation",
                keywords = listOf("microphone", "mic", "audio", "record", "permission", "voice"),
                section = SettingsSection.HardwarePermissions
            ),
            SearchableSetting(
                title = "Display Over Other Apps",
                description = "Tactile floating dictation widget layer on top of apps",
                keywords = listOf("overlay", "display", "floating", "window", "permission", "draw"),
                section = SettingsSection.HardwarePermissions
            ),
            SearchableSetting(
                title = "Foreground Service Keep-Alive",
                description = "Prevents Android OS process eviction during background audio streaming",
                keywords = listOf("foreground", "keep-alive", "notification", "battery", "service"),
                section = SettingsSection.HardwarePermissions
            ),
            SearchableSetting(
                title = "Groq Cloud API Key",
                description = "Encrypted on-device using Android KeyStore hardware backing (gsk_...)",
                keywords = listOf("groq", "api", "key", "token", "auth", "credentials", "cloud", "keystore"),
                section = SettingsSection.AiCloudEngines
            ),
            SearchableSetting(
                title = "Speech-to-Text Model (ASR)",
                description = "Cloud inference model: whisper-large-v3-turbo or whisper-large-v3",
                keywords = listOf("whisper", "speech", "text", "asr", "transcribe", "model", "stt"),
                section = SettingsSection.AiCloudEngines
            ),
            SearchableSetting(
                title = "Text Refinement Model (LLM)",
                description = "Intelligent grammar & stutter cleaner: Llama 3.1 / 3.3 models",
                keywords = listOf("llama", "llm", "refine", "grammar", "cleaner", "model", "post-process"),
                section = SettingsSection.AiCloudEngines
            ),
            SearchableSetting(
                title = "Floating Bubble Diameter",
                description = "Tactile screen footprint of dictation trigger (40 DP to 100 DP)",
                keywords = listOf("bubble", "diameter", "size", "footprint", "widget", "floating", "trigger"),
                section = SettingsSection.BubbleAndInterface
            ),
            SearchableSetting(
                title = "Floating Bubble Opacity",
                description = "Visual transparency of dictation trigger (20% to 100%)",
                keywords = listOf("opacity", "alpha", "transparency", "bubble", "visibility"),
                section = SettingsSection.BubbleAndInterface
            ),
            SearchableSetting(
                title = "Max Recording Duration",
                description = "Hardware audio buffer safety ceiling (15s to 120s)",
                keywords = listOf("recording", "duration", "time", "max", "seconds", "limit", "audio", "buffer"),
                section = SettingsSection.BubbleAndInterface
            ),
            SearchableSetting(
                title = "Snooze Duration",
                description = "Configurable floating bubble snooze timeout (10m default up to 24 hours)",
                keywords = listOf("snooze", "duration", "timer", "minutes", "hours", "dismiss", "hide"),
                section = SettingsSection.SnoozeSettings
            ),
            SearchableSetting(
                title = "Shake to Unsnooze",
                description = "Hardware accelerometer trigger to immediately bring back snoozed bubble",
                keywords = listOf("shake", "accelerometer", "unsnooze", "sensor", "gesture", "wake"),
                section = SettingsSection.SnoozeSettings
            ),
            SearchableSetting(
                title = "System & Runtime Telemetry",
                description = "App kernel version, Jetpack Compose runtime, KeyStore AES-256 specs",
                keywords = listOf("telemetry", "system", "kernel", "build", "runtime", "keystore", "specs", "version"),
                section = SettingsSection.SystemTelemetry
            ),
            SearchableSetting(
                title = "Data Privacy & Purge",
                description = "Permanently purge all voice dictation transcripts from local database",
                keywords = listOf("privacy", "purge", "clear", "delete", "history", "database", "transcripts"),
                section = SettingsSection.SystemTelemetry
            )
        )
    }

    val searchResults = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            allSearchable.filter { item ->
                item.title.lowercase().contains(q) ||
                item.description.lowercase().contains(q) ||
                item.keywords.any { it.contains(q) } ||
                item.section.title.lowercase().contains(q)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(top = topInset + 10.dp, bottom = bottomInset + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Hub Header vs Sub-page Breadcrumb Header
            if (currentSection == null) {
                SettingsTopBarHeader(onOpenDrawer = onOpenDrawer)

                // Search Bar
                SettingsSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClear = { searchQuery = "" }
                )
            } else {
                SettingsSubPageHeader(
                    title = currentSection!!.title,
                    onBack = { currentSection = null },
                    onReset = when (currentSection) {
                        SettingsSection.BubbleAndInterface -> {
                            {
                                bubbleDp = Constants.DEFAULT_BUBBLE_DP
                                settings.bubbleDp = Constants.DEFAULT_BUBBLE_DP
                                bubbleOpacity = Constants.DEFAULT_BUBBLE_OPACITY
                                settings.bubbleOpacity = Constants.DEFAULT_BUBBLE_OPACITY
                                maxSec = Constants.DEFAULT_MAX_SEC
                                settings.maxSec = Constants.DEFAULT_MAX_SEC
                            }
                        }
                        else -> null
                    }
                )
            }

            // Main Content Body: Search Results vs Category List vs Sub-Page Detail
            if (currentSection == null) {
                if (searchQuery.isNotBlank()) {
                    // Search Results List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "// SEARCH RESULTS (${searchResults.size})",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ColorRedAccent,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        if (searchResults.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "NO MATCHING SETTINGS FOUND",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ColorSubtle
                                    )
                                }
                            }
                        } else {
                            items(searchResults) { result ->
                                SearchResultCard(
                                    result = result,
                                    onClick = {
                                        currentSection = result.section
                                        searchQuery = ""
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Hub Category Cards
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Diagnostic Telemetry Strip
                        SettingsTelemetryStrip(
                            whisperModel = whisper,
                            llamaModel = llama,
                            activePerms = activePerms,
                            totalPerms = totalPerms
                        )

                        // 1. Hardware & System Access
                        SettingsCategoryCard(
                            sectionNumber = "01",
                            title = "HARDWARE & PERMISSIONS",
                            subtitle = "Accessibility, Microphone, Display Overlay",
                            statusTag = "$activePerms/$totalPerms ACTIVE",
                            onClick = { currentSection = SettingsSection.HardwarePermissions }
                        )

                        // 2. Groq Cloud & AI Engines
                        SettingsCategoryCard(
                            sectionNumber = "02",
                            title = "GROQ CLOUD & AI ENGINES",
                            subtitle = "API key, Whisper STT & ${Constants.formatShortModelName(llama)} refiner",
                            statusTag = if (apiKey.isNotBlank()) "ONLINE" else "KEY REQUIRED",
                            onClick = { currentSection = SettingsSection.AiCloudEngines }
                        )

                        // 3. Floating Bubble & Timing
                        SettingsCategoryCard(
                            sectionNumber = "03",
                            title = "FLOATING BUBBLE & TIMING",
                            subtitle = "Bubble diameter, opacity, live preview & max record time",
                            statusTag = "${bubbleDp}DP • ${bubbleOpacity}%",
                            onClick = { currentSection = SettingsSection.BubbleAndInterface }
                        )

                        // 4. Snooze & Dismissal
                        SettingsCategoryCard(
                            sectionNumber = "04",
                            title = "SNOOZE & DISMISSAL",
                            subtitle = "Drag bubble to snooze, quick presets (10m-24h) & shake unsnooze",
                            statusTag = SettingsRepository.formatSnoozeDuration(snoozeMinutes).uppercase(),
                            onClick = { currentSection = SettingsSection.SnoozeSettings }
                        )

                        // 5. System Telemetry
                        SettingsCategoryCard(
                            sectionNumber = "05",
                            title = "SYSTEM TELEMETRY",
                            subtitle = "Kernel version, Compose runtime & TLS specs",
                            statusTag = "v${BuildConfig.VERSION_NAME}",
                            onClick = { currentSection = SettingsSection.SystemTelemetry }
                        )
                    }
                }
            } else {
                // Sub-Page Dedicated View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (currentSection!!) {
                        SettingsSection.HardwarePermissions -> {
                            OledSettingsCard {
                                PermissionRow(
                                    title = "Accessibility Service",
                                    subtitle = "Window monitoring & automated text injection",
                                    isGranted = accOk,
                                    actionText = if (accOk) "Active" else "Enable",
                                    onAction = onOpenAccessibility
                                )

                                HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

                                PermissionRow(
                                    title = "Microphone Access",
                                    subtitle = "16kHz hardware voice buffer streaming",
                                    isGranted = micOk,
                                    actionText = if (micOk) "Granted" else "Grant",
                                    onAction = onRequestMic
                                )

                                HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

                                PermissionRow(
                                    title = "Display Over Other Apps",
                                    subtitle = "Tactile floating dictation widget layer",
                                    isGranted = overlayOk,
                                    actionText = if (overlayOk) "Active" else "Allow",
                                    onAction = onOpenOverlay
                                )

                                if (Build.VERSION.SDK_INT >= 33 && !notifOk) {
                                    HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    PermissionRow(
                                        title = "Foreground Service Keep-Alive",
                                        subtitle = "Prevents Android OS process eviction",
                                        isGranted = false,
                                        actionText = "Allow",
                                        onAction = onRequestNotif
                                    )
                                }
                            }
                        }

                        SettingsSection.AiCloudEngines -> {
                            OledSettingsCard {
                                // Groq Cloud API Key Input with Auto-Save
                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = {
                                        apiKey = it
                                        settings.apiKey = it.trim()
                                        validateMsg = null
                                        validateOk = null
                                    },
                                    label = {
                                        Text(
                                            text = "Groq API Key (gsk_...)",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = textFieldColors,
                                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            // One-Tap Clipboard Paste Button
                                            val clipText = clipboardManager.getText()?.text.orEmpty()
                                            val hasGskClip = clipText.trim().startsWith("gsk_")
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(ColorCardElevated)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (hasGskClip) ColorRedAccent else ColorBorderSubtle,
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable {
                                                        val sanitized = clipText.trim()
                                                            .replace("\n", "")
                                                            .replace("\r", "")
                                                            .replace("\"", "")
                                                            .replace("'", "")
                                                        if (sanitized.isNotBlank()) {
                                                            apiKey = sanitized
                                                            settings.apiKey = sanitized
                                                            validateMsg = "✓ PASTED & STORED"
                                                            validateOk = true
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "PASTE",
                                                    color = if (hasGskClip) ColorRedAccent else Color.White,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Show / Hide Toggle
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(ColorCardElevated)
                                                    .border(1.dp, ColorBorderSubtle, RoundedCornerShape(6.dp))
                                                    .clickable { showKey = !showKey }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (showKey) "HIDE" else "SHOW",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    },
                                    textStyle = LocalTextStyle.current.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    ),
                                    singleLine = true
                                )

                                Text(
                                    text = "Encrypted on-device using Android KeyStore hardware backing (Auto-Saved).",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ColorSubtle
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Test Button with Latency Benchmark
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                validating = true
                                                validateMsg = null
                                                validateOk = null
                                                settings.apiKey = apiKey.trim()
                                                val startMs = SystemClock.elapsedRealtime()
                                                val res = groq.validateKey(Constants.DEFAULT_BASE_URL, apiKey.trim())
                                                val elapsedMs = SystemClock.elapsedRealtime() - startMs
                                                validating = false
                                                when (res) {
                                                    is GroqResult.Success -> {
                                                        validateOk = true
                                                        validateMsg = "✓ VERIFIED (${elapsedMs}ms)"
                                                        if (res.data.isNotEmpty()) {
                                                            settings.availableLlamaModels = res.data
                                                            if (!res.data.contains(settings.llama)) {
                                                                settings.llama = res.data.first()
                                                                llama = res.data.first()
                                                            }
                                                        }
                                                    }
                                                    is GroqResult.Error -> {
                                                        validateOk = false
                                                        val errSnippet = res.message.take(28)
                                                        validateMsg = "✗ (${elapsedMs}ms) $errSnippet"
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !validating && apiKey.isNotBlank(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ColorCardElevated,
                                            contentColor = Color.White,
                                            disabledContainerColor = ColorCardElevated.copy(alpha = 0.4f),
                                            disabledContentColor = ColorSubtle
                                        ),
                                        modifier = Modifier.border(1.dp, ColorBorderSubtle, RoundedCornerShape(8.dp)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        if (validating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.8.dp,
                                                color = ColorRedAccent
                                            )
                                        } else {
                                            Text(
                                                text = "TEST LATENCY",
                                                fontSize = 10.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }

                                    // Real-time Result Pill
                                    validateMsg?.let { msg ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (validateOk == true) ColorCardElevated else ColorRedAccent.copy(alpha = 0.12f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (validateOk == true) ColorBorderSubtle else ColorRedAccent.copy(alpha = 0.7f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = msg,
                                                color = if (validateOk == true) Color.White else ColorRedAccent,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

                                // Speech-to-Text Model Selection
                                ModelDropdown(
                                    label = "Speech-to-Text Model",
                                    category = "ASR ENGINE",
                                    selectedValue = whisper,
                                    options = Constants.WHISPER_MODELS,
                                    textFieldColors = textFieldColors
                                ) {
                                    whisper = it
                                    settings.whisper = it
                                }

                                HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

                                // Text Refinement Model Selection
                                ModelDropdown(
                                    label = "Text Refinement Model",
                                    category = "POST-PROCESSOR",
                                    selectedValue = llama,
                                    options = settings.availableLlamaModels.ifEmpty { Constants.LLAMA_MODELS },
                                    textFieldColors = textFieldColors
                                ) {
                                    llama = it
                                    settings.llama = it
                                }
                            }
                        }

                        SettingsSection.BubbleAndInterface -> {
                            // Live Precision Nothing OS Bubble Calibration Stage (Fixed Bounds: Zero Layout Shift)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ColorCardBg)
                                    .border(1.dp, ColorBorder, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Precision Reticle & Calibration Canvas
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val cx = w / 2f
                                    val cy = h / 2f
                                    val crosshairColor = ColorBorderSubtle.copy(alpha = 0.6f)
                                    val redTickColor = ColorRedAccent.copy(alpha = 0.5f)

                                    // Center crosshair ticks
                                    drawLine(crosshairColor, Offset(cx - 16.dp.toPx(), cy), Offset(cx + 16.dp.toPx(), cy), 1.dp.toPx())
                                    drawLine(crosshairColor, Offset(cx, cy - 16.dp.toPx()), Offset(cx, cy + 16.dp.toPx()), 1.dp.toPx())

                                    // Corner brackets (L-ticks)
                                    val pad = 12.dp.toPx()
                                    val len = 8.dp.toPx()
                                    // Top-Left
                                    drawLine(redTickColor, Offset(pad, pad), Offset(pad + len, pad), 1.5.dp.toPx())
                                    drawLine(redTickColor, Offset(pad, pad), Offset(pad, pad + len), 1.5.dp.toPx())
                                    // Top-Right
                                    drawLine(crosshairColor, Offset(w - pad, pad), Offset(w - pad - len, pad), 1.5.dp.toPx())
                                    drawLine(crosshairColor, Offset(w - pad, pad), Offset(w - pad, pad + len), 1.5.dp.toPx())
                                    // Bottom-Left
                                    drawLine(crosshairColor, Offset(pad, h - pad), Offset(pad + len, h - pad), 1.5.dp.toPx())
                                    drawLine(crosshairColor, Offset(pad, h - pad), Offset(pad, h - pad - len), 1.5.dp.toPx())
                                    // Bottom-Right
                                    drawLine(redTickColor, Offset(w - pad, h - pad), Offset(w - pad - len, h - pad), 1.5.dp.toPx())
                                    drawLine(redTickColor, Offset(w - pad, h - pad), Offset(w - pad, h - pad - len), 1.5.dp.toPx())

                                    // Subtle allocation boundary reference ring
                                    drawCircle(
                                        color = ColorBorderSubtle.copy(alpha = 0.3f),
                                        radius = 54.dp.toPx(),
                                        center = Offset(cx, cy),
                                        style = Stroke(
                                            width = 1.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                        )
                                    )
                                }

                                // Live Proportional Bubble
                                Box(
                                    modifier = Modifier
                                        .size(bubbleDp.dp)
                                        .clip(RoundedCornerShape((bubbleDp * 0.28f).dp))
                                        .background(ColorCardElevated.copy(alpha = (bubbleOpacity / 100f).coerceIn(0.2f, 1.0f)))
                                        .border(
                                            width = 1.dp,
                                            color = ColorRedAccent.copy(alpha = (bubbleOpacity / 100f).coerceAtLeast(0.35f)),
                                            shape = RoundedCornerShape((bubbleDp * 0.28f).dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_bubble),
                                        contentDescription = "Bubble Preview",
                                        tint = ColorRedAccent.copy(alpha = (bubbleOpacity / 100f).coerceIn(0.25f, 1.0f)),
                                        modifier = Modifier.size((bubbleDp * 0.44f).dp)
                                    )
                                }

                                // Telemetry readout pinned to bottom of fixed stage
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ColorDarkBg.copy(alpha = 0.85f))
                                        .border(0.5.dp, ColorBorderSubtle, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "// SCALE 1:1 • ${bubbleDp}DP • ${bubbleOpacity}% OPACITY",
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorMuted,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                            }

                            // Sliders Card
                            OledSettingsCard {
                                // Quick Size Presets
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SIZE PRESETS",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorMuted,
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "QUICK SELECT",
                                            fontSize = 8.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = ColorSubtle
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val presets = listOf(
                                            "COMPACT" to 48,
                                            "BALANCED" to 56,
                                            "LARGE" to 72
                                        )
                                        presets.forEach { (name, size) ->
                                            val isSelected = bubbleDp == size
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) ColorCardElevated else ColorDarkBg)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) ColorRedAccent else ColorBorderSubtle,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        bubbleDp = size
                                                        settings.bubbleDp = size
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = name,
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) ColorRedAccent else ColorSubtle,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                    Text(
                                                        text = "${size} DP",
                                                        fontSize = 10.5.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else ColorMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

                                // 1. Bubble Diameter Slider
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "FLOATING BUBBLE SIZE",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorMuted,
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "${bubbleDp} DP",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    TactileNothingSlider(
                                        value = bubbleDp.toFloat(),
                                        onValueChange = {
                                            bubbleDp = it.roundToInt()
                                            settings.bubbleDp = bubbleDp
                                        },
                                        valueRange = Constants.MIN_BUBBLE_DP.toFloat()..Constants.MAX_BUBBLE_DP.toFloat(),
                                        steps = 6,
                                        leftLabel = "A",
                                        rightLabel = "A"
                                    )
                                }

                                HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

                                // 2. Bubble Opacity Slider
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "BUBBLE OPACITY",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorMuted,
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "${bubbleOpacity}%",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    TactileNothingSlider(
                                        value = bubbleOpacity.toFloat(),
                                        onValueChange = {
                                            bubbleOpacity = it.roundToInt()
                                            settings.bubbleOpacity = bubbleOpacity
                                        },
                                        valueRange = Constants.MIN_BUBBLE_OPACITY.toFloat()..Constants.MAX_BUBBLE_OPACITY.toFloat(),
                                        steps = 8,
                                        leftLabel = "20%",
                                        rightLabel = "100%"
                                    )
                                }

                                HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

                                // 3. Max Recording Duration Slider (Redesigned per Req 8)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val durationLabel = if (maxSec < 60) "${maxSec} SEC" else if (maxSec % 60 == 0) "${maxSec / 60} MIN" else "${maxSec / 60}M ${maxSec % 60}S"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "MAX RECORDING DURATION",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorMuted,
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = durationLabel,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    TactileNothingSlider(
                                        value = maxSec.toFloat(),
                                        onValueChange = {
                                            maxSec = it.roundToInt()
                                            settings.maxSec = maxSec
                                        },
                                        valueRange = Constants.MIN_REC_SEC.toFloat()..Constants.MAX_REC_SEC.toFloat(),
                                        steps = 38,
                                        leftLabel = "15s",
                                        rightLabel = "10m"
                                    )
                                }
                            }
                        }

                        SettingsSection.SnoozeSettings -> {
                            var customHours by remember(snoozeMinutes) { mutableIntStateOf(snoozeMinutes / 60) }
                            var customMins by remember(snoozeMinutes) { mutableIntStateOf(snoozeMinutes % 60) }
                            var isCustomExpanded by remember { mutableStateOf(snoozeMinutes !in Constants.PRESET_SNOOZE_MINUTES) }

                            var remainingSeconds by remember(settings.snoozeUntilMs, isSnoozed) {
                                mutableLongStateOf(((settings.snoozeUntilMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L))
                            }
                            LaunchedEffect(settings.snoozeUntilMs, isSnoozed) {
                                if (isSnoozed) {
                                    while (true) {
                                        val rem = ((settings.snoozeUntilMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                                        remainingSeconds = rem
                                        if (rem <= 0L) {
                                            isSnoozed = false
                                            break
                                        }
                                        delay(1000L)
                                    }
                                }
                            }

                            // 1. Current Snooze Timeout & Dot Matrix Pattern Card
                            OledSettingsCard {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CURRENT SNOOZE TIMEOUT",
                                            fontSize = 9.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorMuted,
                                            letterSpacing = 0.6.sp
                                        )

                                        if (isSnoozed) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x33D71921))
                                                    .border(1.dp, ColorRedAccent, RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        settings.unsnooze()
                                                        isSnoozed = false
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(ColorRedAccent)
                                                    )
                                                    Text(
                                                        text = "UNSNOOZE",
                                                        fontSize = 8.5.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ColorRedAccent,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Status Indicator Row
                                    if (isSnoozed) {
                                        val remMins = remainingSeconds / 60
                                        val remSecs = remainingSeconds % 60
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(ColorRedAccent)
                                            )
                                            Text(
                                                text = String.format("STATUS: ACTIVE • %02d:%02d REMAINING", remMins, remSecs),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorRedAccent,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(ColorSubtle)
                                            )
                                            Text(
                                                text = "STATUS: INACTIVE • SNOOZE READY",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Medium,
                                                color = ColorSubtle,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DotMatrixText(
                                            text = SettingsRepository.formatSnoozeDuration(snoozeMinutes).uppercase(),
                                            dotSize = 2.8.dp,
                                            dotSpacing = 1.dp,
                                            charSpacing = 1.4.dp,
                                            color = Color.White
                                        )

                                        SnoozeTimeoutGlyphMatrix(isSnoozed = isSnoozed)
                                    }
                                }
                            }

                            // 2. Quick Duration Presets Card
                            OledSettingsCard {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "QUICK DURATION PRESETS",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorMuted,
                                        letterSpacing = 0.6.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Constants.PRESET_SNOOZE_MINUTES.forEach { preset ->
                                            val isSelected = snoozeMinutes == preset
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(96.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isSelected) Color(0x22D71921) else ColorDarkBg)
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 1.dp,
                                                        color = if (isSelected) ColorRedAccent else ColorBorderSubtle,
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable {
                                                        snoozeMinutes = preset
                                                        settings.snoozeDurationMinutes = preset
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    SnoozeClockDialIcon(isSelected = isSelected)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = SettingsRepository.formatSnoozeDuration(preset).uppercase(),
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color.White else ColorMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Custom Timeout Trigger Button
                            val arrowRotation by animateFloatAsState(
                                targetValue = if (isCustomExpanded) 180f else 0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "CustomExpandRotation"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ColorCardBg)
                                    .border(
                                        width = if (isCustomExpanded) 1.2.dp else 1.dp,
                                        color = if (isCustomExpanded) ColorRedAccent else ColorBorderSubtle,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        isCustomExpanded = !isCustomExpanded
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isCustomExpanded) ColorRedAccent else ColorSubtle)
                                        )
                                        Text(
                                            text = "CUSTOM TIMEOUT",
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCustomExpanded) Color.White else ColorMuted,
                                            letterSpacing = 0.8.sp
                                        )
                                    }

                                    Text(
                                        text = "▼",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isCustomExpanded) ColorRedAccent else ColorSubtle,
                                        modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
                                    )
                                }
                            }

                            // 4. Custom Duration Picker Card (Animated Reveal)
                            AnimatedVisibility(
                                visible = isCustomExpanded,
                                enter = expandVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(animationSpec = tween(250)),
                                exit = shrinkVertically(
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeOut(animationSpec = tween(200))
                            ) {
                                OledSettingsCard {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "CUSTOM DURATION",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorMuted,
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "Set snooze time from 1 minute to 24 hours",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = ColorSubtle
                                        )
                                    }

                                    // Top Header Labels Row aligned directly above capsules
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Spacer(modifier = Modifier.width(28.dp))
                                        Box(modifier = Modifier.width(84.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "HOURS",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorSubtle
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(24.dp))
                                        Box(modifier = Modifier.width(84.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "MINUTES",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorSubtle
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(28.dp))
                                    }

                                    // Flanking Arc Gauges & Number Drums (148dp identical height, Top aligned)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Left Arc Gauge (148.dp)
                                        FlankingArcGauge(isLeft = true)

                                        // Hours Drum Wheel (148.dp)
                                        val hoursValues = remember { (0..24).toList() }
                                        SnoozeWheelDrum(
                                            selectedValue = customHours,
                                            values = hoursValues,
                                            onValueChange = { newHr ->
                                                customHours = newHr
                                                if (customHours == 24) customMins = 0
                                                val total = (customHours * 60 + customMins).coerceIn(1, 1440)
                                                snoozeMinutes = total
                                                settings.snoozeDurationMinutes = total
                                            }
                                        )

                                        // Center Twin Dots (aligned vertically with the capsules)
                                        Box(
                                            modifier = Modifier.height(148.dp),
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(top = 26.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                )
                                            }
                                        }

                                        // Minutes Drum Wheel (148.dp)
                                        val minutesValues = remember { (0..59).toList() }
                                        SnoozeWheelDrum(
                                            selectedValue = customMins,
                                            values = minutesValues,
                                            onValueChange = { newMin ->
                                                customMins = newMin
                                                if (customHours == 24) customHours = 0
                                                val total = (customHours * 60 + customMins).coerceIn(1, 1440)
                                                snoozeMinutes = total
                                                settings.snoozeDurationMinutes = total
                                            }
                                        )

                                        // Right Arc Gauge (148.dp)
                                        FlankingArcGauge(isLeft = false)
                                    }

                                    // Live Dotted Countdown Timer (shown when snoozed)
                                    AnimatedVisibility(
                                        visible = isSnoozed,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        SnoozeActiveProgressTimer(
                                            snoozeUntilMs = settings.snoozeUntilMs,
                                            totalDurationMinutes = snoozeMinutes,
                                            onExpire = {
                                                isSnoozed = false
                                            }
                                        )
                                    }


                                    // SET DURATION Pill Action Button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .clip(RoundedCornerShape(25.dp))
                                            .background(ColorRedAccent)
                                            .clickable {
                                                val total = (customHours * 60 + customMins).coerceIn(1, 1440)
                                                snoozeMinutes = total
                                                settings.snoozeDurationMinutes = total
                                                settings.snooze(total)
                                                isSnoozed = true
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Spacer(modifier = Modifier.width(18.dp))
                                            Text(
                                                text = "SET DURATION",
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF060709),
                                                letterSpacing = 1.2.sp
                                            )
                                            Text(
                                                text = "→",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF060709)
                                            )
                                        }
                                    }
                                }
                            }
                            }
                            Spacer(modifier = Modifier.height(56.dp))
                        }

                        SettingsSection.SystemTelemetry -> {
                            OledSettingsCard {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TelemetryRow(label = "APP KERNEL", value = "OpenWispr v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
                                    HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    TelemetryRow(label = "UI RUNTIME", value = "Jetpack Compose + RenderThread")
                                    HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    TelemetryRow(label = "KEY STORAGE", value = "Android KeyStore Hardware AES-256")
                                    HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    TelemetryRow(label = "CLOUD ROUTE", value = "Groq Cloud API (TLS 1.3)")
                                }
                            }

                            // Data Privacy & Purge Card
                            OledSettingsCard {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "DATA PRIVACY & STORAGE",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorMuted,
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "LOCAL ONLY",
                                            fontSize = 8.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = ColorSubtle
                                        )
                                    }

                                    Text(
                                        text = "Voice dictations are stored in private on-device SQLite database. Purging permanently removes all transcript records.",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ColorMuted,
                                        lineHeight = 14.sp
                                    )

                                    var purging by remember { mutableStateOf(false) }
                                    var purgeConfirmed by remember { mutableStateOf(false) }
                                    val historyRepo = remember { com.openwispr.data.HistoryRepository(context) }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ColorRedAccent.copy(alpha = 0.12f))
                                                .border(1.dp, ColorRedAccent.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                                .clickable(enabled = !purging) {
                                                    purging = true
                                                    scope.launch {
                                                        historyRepo.clearAllTranscripts()
                                                        purging = false
                                                        purgeConfirmed = true
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (purging) "PURGING..." else "PURGE TRANSCRIPT HISTORY",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorRedAccent,
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        if (purgeConfirmed) {
                                            Text(
                                                text = "✓ PURGED",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBarHeader(
    onOpenDrawer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        // Left hamburger button - matching HomeScreen 44dp
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ColorCardBg)
                .border(1.dp, ColorBorder, RoundedCornerShape(10.dp))
                .clickable { onOpenDrawer() }
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_menu),
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Center Title: Dot Matrix SETTINGS with safe padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 56.dp)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            DotMatrixText(
                text = "SETTINGS",
                dotSize = 2.8.dp,
                dotSpacing = 0.9.dp,
                charSpacing = 1.8.dp,
                color = Color.White
            )
        }

        // Right Optical Balance Spacer (44dp)
        Spacer(
            modifier = Modifier
                .size(44.dp)
                .align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun SettingsSubPageHeader(
    title: String,
    onBack: () -> Unit,
    onReset: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        // Left back button - 40dp circular button matching Nothing design
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ColorCardBg)
                .border(1.dp, ColorBorderSubtle, CircleShape)
                .clickable { onBack() }
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "←",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Center: Clean single title (Zero Overlap Guaranteed)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (onReset != null) 74.dp else 52.dp)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Optional Right Action: RESET pill button with red border
        if (onReset != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ColorDarkBg)
                    .border(1.dp, ColorRedAccent, RoundedCornerShape(20.dp))
                    .clickable { onReset() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .align(Alignment.CenterEnd),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RESET",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ColorRedAccent,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ColorCardElevated)
            .border(1.dp, ColorBorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "⌕",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (query.isNotBlank()) ColorRedAccent else ColorSubtle
            )

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search settings, models, bubble...",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ColorSubtle
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(ColorCardBg)
                        .clickable { onClear() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        fontSize = 11.sp,
                        color = ColorMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    sectionNumber: String,
    title: String,
    subtitle: String,
    statusTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header Row: Section Number + Category Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "// $sectionNumber",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ColorRedAccent,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = title,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.6.sp
                    )
                }

                // Subtitle Technical Scope
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = ColorMuted,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Action Group: Status Pill + Sleek Chevron Squircle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ColorCardElevated)
                        .border(1.dp, ColorBorderSubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusTag,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ColorMuted,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ColorCardElevated)
                        .border(1.dp, ColorBorderSubtle, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        fontSize = 12.sp,
                        color = ColorRedAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchableSetting,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Text(
                    text = "→",
                    fontSize = 13.sp,
                    color = ColorRedAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = result.description,
                fontSize = 10.sp,
                color = ColorMuted
            )

            Text(
                text = "// ${result.section.sectionIndex} › ${result.title.uppercase()}",
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                color = ColorRedAccent,
                letterSpacing = 0.4.sp
            )
        }
    }
}

/**
 * Tactile Custom Nothing Slider matching user reference image.
 * Features pill-shaped track, active red accent bar, discrete step dots, and vertical divider thumb.
 */
@Composable
fun TactileNothingSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    leftLabel: String,
    rightLabel: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f)
    val fraction = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
    var lastSnappedSlot by remember { mutableIntStateOf(-1) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left endpoint label (e.g. "A" or "40 DP")
        Text(
            text = leftLabel,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = ColorSubtle
        )

        // Custom Slider Track Box
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, steps)
                }
                .pointerInput(valueRange, steps) {
                    fun updateFromOffset(x: Float) {
                        val newFraction = (x / size.width).coerceIn(0f, 1f)
                        val totalSlots = steps + 1
                        val snappedSlot = (newFraction * totalSlots).roundToInt()
                        if (snappedSlot != lastSnappedSlot) {
                            lastSnappedSlot = snappedSlot
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        val snappedFraction = (snappedSlot.toFloat() / totalSlots).coerceIn(0f, 1f)
                        val newValue = valueRange.start + snappedFraction * rangeSpan
                        onValueChange(newValue)
                    }

                    detectTapGestures { offset ->
                        updateFromOffset(offset.x)
                    }
                }
                .pointerInput(valueRange, steps) {
                    fun updateFromOffset(x: Float) {
                        val newFraction = (x / size.width).coerceIn(0f, 1f)
                        val totalSlots = steps + 1
                        val snappedSlot = (newFraction * totalSlots).roundToInt()
                        if (snappedSlot != lastSnappedSlot) {
                            lastSnappedSlot = snappedSlot
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        val snappedFraction = (snappedSlot.toFloat() / totalSlots).coerceIn(0f, 1f)
                        val newValue = valueRange.start + snappedFraction * rangeSpan
                        onValueChange(newValue)
                    }

                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        updateFromOffset(change.position.x)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val trackWidth = maxWidth
            val thumbOffset = trackWidth * fraction

            // Inactive Track Background Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(ColorCardElevated)
                    .border(1.dp, ColorBorderSubtle, RoundedCornerShape(7.dp))
            )

            // Active Track Fill Pill
            Box(
                modifier = Modifier
                    .width(thumbOffset.coerceAtLeast(7.dp))
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(ColorRedAccent)
            )

            // Discrete Step Dots on Track
            val totalSlots = steps + 1
            for (stepIdx in 1 until totalSlots) {
                val stepFraction = stepIdx.toFloat() / totalSlots
                val dotX = trackWidth * stepFraction
                Box(
                    modifier = Modifier
                        .offset(x = dotX - 2.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (stepFraction <= fraction) Color.White.copy(alpha = 0.7f) else ColorSubtle)
                )
            }

            // Vertical Indicator Divider Thumb (Matching user uploaded image)
            Box(
                modifier = Modifier
                    .offset(x = (thumbOffset - 2.dp).coerceIn(0.dp, trackWidth - 4.dp))
                    .width(4.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
                    .border(0.5.dp, ColorRedAccent, RoundedCornerShape(2.dp))
            )
        }

        // Right endpoint label (e.g. "A" or "100 DP")
        Text(
            text = rightLabel,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun SettingsTelemetryStrip(
    whisperModel: String,
    llamaModel: String,
    activePerms: Int,
    totalPerms: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TelemetryStripItem(
            label = "ASR PIPELINE",
            value = Constants.formatShortModelName(whisperModel)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(22.dp)
                .background(ColorBorderSubtle)
        )
        TelemetryStripItem(
            label = "REFINER",
            value = Constants.formatShortModelName(llamaModel)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(22.dp)
                .background(ColorBorderSubtle)
        )
        TelemetryStripItem(
            label = "HARDWARE",
            value = "$activePerms/$totalPerms ACTIVE",
            isAlert = activePerms < totalPerms
        )
    }
}

@Composable
private fun TelemetryStripItem(
    label: String,
    value: String,
    isAlert: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = ColorSubtle,
            letterSpacing = 0.6.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 10.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isAlert) ColorRedAccent else Color.White,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun OledSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ColorCardBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Minimalist dot indicator (White when active, Red when action required)
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) Color.White.copy(alpha = 0.85f) else ColorRedAccent)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = 0.2.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 9.5.sp,
                    color = ColorMuted,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.2.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Tactile Action Button Pill (Monochrome white for active, Crimson for grant)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isGranted) ColorCardElevated else ColorRedAccent.copy(alpha = 0.12f)
                )
                .border(
                    width = 1.dp,
                    color = if (isGranted) ColorBorderSubtle else ColorRedAccent.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp)
                )
                .clickable { onAction() }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = actionText.uppercase(),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isGranted) Color.White else ColorRedAccent,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(
    label: String,
    category: String,
    selectedValue: String,
    options: List<String>,
    textFieldColors: TextFieldColors,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                color = ColorMuted,
                letterSpacing = 0.6.sp
            )
            Text(
                text = category,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                color = ColorSubtle
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = Constants.formatShortModelName(selectedValue),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(10.dp),
                colors = textFieldColors,
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(ColorCardElevated)
                    .border(1.dp, ColorBorderSubtle, RoundedCornerShape(8.dp))
            ) {
                options.forEach { model ->
                    val isSelected = model == selectedValue
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(ColorRedAccent)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = Constants.formatShortModelName(model),
                                        color = if (isSelected) ColorRedAccent else Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = model,
                                        color = ColorSubtle,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelect(model)
                            expanded = false
                        },
                        modifier = Modifier.background(
                            if (isSelected) ColorCardBg else Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontFamily = FontFamily.Monospace,
            color = ColorSubtle,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 10.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun SnoozeTimeoutGlyphMatrix(
    isSnoozed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GlyphMatrixAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSnoozed) 1600 else 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlyphPhase"
    )

    Canvas(modifier = modifier.size(54.dp)) {
        val cols = 7
        val rows = 7
        val dotRadius = 1.8.dp.toPx()
        val spacingX = size.width / (cols + 1)
        val spacingY = size.height / (rows + 1)
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxDist = size.width * 0.46f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = (c + 1) * spacingX
                val y = (r + 1) * spacingY
                val dist = (Offset(x, y) - center).getDistance()
                if (dist <= maxDist) {
                    val isRed = (c >= 4 && r >= 2) || (c == 6 && r >= 1)
                    val wave = (kotlin.math.sin(phase + (c * 0.7f + r * 0.5f)) + 1f) / 2f
                    if (isRed) {
                        val pulseScale = if (isSnoozed) 1.8f + 1.2f * wave else 2.0f + 0.4f * wave
                        val pulseAlpha = if (isSnoozed) 0.20f + 0.50f * wave else 0.25f + 0.20f * wave
                        drawCircle(
                            color = ColorRedAccent.copy(alpha = pulseAlpha),
                            radius = dotRadius * pulseScale,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = ColorRedAccent,
                            radius = dotRadius * (1f + 0.15f * wave),
                            center = Offset(x, y)
                        )
                    } else {
                        val ambientAlpha = 0.12f + 0.10f * wave
                        drawCircle(
                            color = Color.White.copy(alpha = ambientAlpha),
                            radius = dotRadius * 0.9f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SnoozeWheelDrum(
    selectedValue: Int,
    values: List<Int>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val currentSelectedValue by rememberUpdatedState(selectedValue)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val initialIndex = remember { values.indexOf(selectedValue).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeight = 44.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // Sync external changes (e.g. from preset buttons or slider) to listState
    LaunchedEffect(selectedValue) {
        val targetIndex = values.indexOf(selectedValue).coerceAtLeast(0)
        if (listState.firstVisibleItemIndex != targetIndex) {
            isProgrammaticScroll = true
            try {
                listState.animateScrollToItem(targetIndex)
            } finally {
                isProgrammaticScroll = false
            }
        }
    }

    // Emit value changes and haptic feedback when user scrolls directly to a new index
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (!isProgrammaticScroll && index in values.indices) {
                    val newValue = values[index]
                    if (newValue != currentSelectedValue) {
                        currentOnValueChange(newValue)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
    }

    Box(
        modifier = modifier
            .width(84.dp)
            .height(148.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0F13))
            .border(1.dp, ColorBorderSubtle, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(values) { index, itemValue ->
                val offsetPx = (index - listState.firstVisibleItemIndex) * itemHeightPx - listState.firstVisibleItemScrollOffset
                val fraction = offsetPx / itemHeightPx

                val scale = when {
                    fraction < 0f -> 1.0f
                    fraction <= 1f -> 1.0f - fraction * 0.286f
                    fraction <= 2f -> 0.714f - (fraction - 1f) * 0.179f
                    else -> 0.535f
                }

                val alpha = when {
                    fraction < 0f -> (1.0f + fraction * 1.5f).coerceIn(0f, 1f)
                    fraction <= 1f -> (1.0f - fraction * 0.75f).coerceIn(0.25f, 1f)
                    fraction <= 2f -> (0.25f - (fraction - 1f) * 0.15f).coerceIn(0.10f, 0.25f)
                    else -> (0.10f - (fraction - 2f) * 0.10f).coerceIn(0f, 0.10f)
                }

                val isSelected = fraction in -0.3f..0.3f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (index != listState.firstVisibleItemIndex) {
                                coroutineScope.launch {
                                    onValueChange(itemValue)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isProgrammaticScroll = true
                                    try {
                                        listState.animateScrollToItem(index)
                                    } finally {
                                        isProgrammaticScroll = false
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", itemValue),
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SnoozeClockDialIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.width * 0.38f
        val dotR = 1.1.dp.toPx()
        val color = if (isSelected) ColorRedAccent else Color(0x99FFFFFF)

        // 12 hour ticks around the dial
        for (i in 0 until 12) {
            val angleRad = (i * 30.0 - 90.0) * (Math.PI / 180.0)
            val dx = (center.x + r * Math.cos(angleRad)).toFloat()
            val dy = (center.y + r * Math.sin(angleRad)).toFloat()
            drawCircle(
                color = color,
                radius = dotR,
                center = Offset(dx, dy)
            )
        }

        // Pointer hand pointing to 12 o'clock
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y - r * 0.70f),
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center hub dot
        drawCircle(
            color = color,
            radius = dotR * 1.3f,
            center = center
        )
    }
}

@Composable
private fun FlankingArcGauge(
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.width(28.dp).height(148.dp)) {
            val dotCount = 13
            val dotR = 1.25.dp.toPx()
            val w = size.width
            val h = size.height

            for (i in 0 until dotCount) {
                val f = i.toFloat() / (dotCount - 1)
                val y = dotR + f * (h - 2 * dotR)
                val curve = Math.sin(f * Math.PI).toFloat()
                val x = if (isLeft) {
                    w * 0.85f - (curve * w * 0.65f)
                } else {
                    w * 0.15f + (curve * w * 0.65f)
                }

                val isActive = i == 6
                val activeColor = if (isLeft) ColorRedAccent else Color(0xFF2ECC71)

                if (isActive) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.35f),
                        radius = dotR * 2.8f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = activeColor,
                        radius = dotR * 1.8f,
                        center = Offset(x, y)
                    )
                } else {
                    drawCircle(
                        color = Color(0x33FFFFFF),
                        radius = dotR,
                        center = Offset(x, y)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isLeft) "0H" else "24H",
            fontSize = 8.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = ColorSubtle
        )
    }
}

@Composable
private fun SnoozeActiveProgressTimer(
    snoozeUntilMs: Long,
    totalDurationMinutes: Int,
    onExpire: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = (totalDurationMinutes * 60_000L).coerceAtLeast(1000L)
    val initialRemMs = (snoozeUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
    val initialFraction = (initialRemMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

    val animatedFraction = remember(snoozeUntilMs) { Animatable(initialFraction) }

    LaunchedEffect(snoozeUntilMs) {
        val remMs = (snoozeUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
        if (remMs > 0L) {
            animatedFraction.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = remMs.toInt().coerceAtLeast(1),
                    easing = LinearEasing
                )
            )
        } else {
            animatedFraction.snapTo(0f)
        }
        onExpire()
    }

    var remainingSeconds by remember(snoozeUntilMs) {
        mutableLongStateOf(((snoozeUntilMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L))
    }

    LaunchedEffect(snoozeUntilMs) {
        while (true) {
            val rem = ((snoozeUntilMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            remainingSeconds = rem
            if (rem <= 0L) break
            val delayMs = ((snoozeUntilMs - System.currentTimeMillis()) % 1000L).coerceAtLeast(50L)
            delay(delayMs)
        }
    }

    val remMins = remainingSeconds / 60
    val remSecs = remainingSeconds % 60

    val infiniteTransition = rememberInfiniteTransition(label = "TimerPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ColorRedAccent.copy(alpha = pulseAlpha))
                )
                Text(
                    text = "SNOOZE ACTIVE",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ColorRedAccent,
                    letterSpacing = 0.6.sp
                )
            }

            Text(
                text = String.format("%02d:%02d REMAINING", remMins, remSecs),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dotCount = 36
                val dotR = 1.4.dp.toPx()
                val cy = size.height / 2f
                val currentProgress = animatedFraction.value * dotCount

                for (i in 0 until dotCount) {
                    val dotF = i.toFloat() / (dotCount - 1)
                    val cx = dotF * size.width
                    val diff = currentProgress - i

                    when {
                        diff >= 1f -> {
                            drawCircle(
                                color = ColorRedAccent,
                                radius = dotR,
                                center = Offset(cx, cy)
                            )
                        }
                        diff > 0f -> {
                            val leadingAlpha = diff.coerceIn(0f, 1f)
                            drawCircle(
                                color = ColorRedAccent.copy(alpha = leadingAlpha * pulseAlpha * 0.5f),
                                radius = dotR * (1.5f + 1.5f * leadingAlpha),
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = leadingAlpha),
                                radius = dotR * (0.9f + 0.6f * leadingAlpha),
                                center = Offset(cx, cy)
                            )
                        }
                        else -> {
                            drawCircle(
                                color = Color(0x30FFFFFF),
                                radius = dotR * 0.9f,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            }
        }
    }
}