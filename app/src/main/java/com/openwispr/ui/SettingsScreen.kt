package com.openwispr.ui

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
    CloudCredentials("Groq Cloud Credentials", "02 CREDENTIALS"),
    AiInferenceEngines("AI Inference Engines", "03 AI ENGINES"),
    BubbleAndInterface("Floating Bubble & Interface", "04 INTERFACE"),
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
                section = SettingsSection.CloudCredentials
            ),
            SearchableSetting(
                title = "Speech-to-Text Model (ASR)",
                description = "Cloud inference model: whisper-large-v3-turbo or whisper-large-v3",
                keywords = listOf("whisper", "speech", "text", "asr", "transcribe", "model", "stt"),
                section = SettingsSection.AiInferenceEngines
            ),
            SearchableSetting(
                title = "Text Refinement Model (LLM)",
                description = "Intelligent grammar & stutter cleaner: Llama 3.1 / 3.3 models",
                keywords = listOf("llama", "llm", "refine", "grammar", "cleaner", "model", "post-process"),
                section = SettingsSection.AiInferenceEngines
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
                title = "System & Runtime Telemetry",
                description = "App kernel version, Jetpack Compose runtime, KeyStore AES-256 specs",
                keywords = listOf("telemetry", "system", "kernel", "build", "runtime", "keystore", "specs", "version"),
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
                    onReset = if (currentSection == SettingsSection.BubbleAndInterface) {
                        {
                            bubbleDp = Constants.DEFAULT_BUBBLE_DP
                            settings.bubbleDp = Constants.DEFAULT_BUBBLE_DP
                            bubbleOpacity = Constants.DEFAULT_BUBBLE_OPACITY
                            settings.bubbleOpacity = Constants.DEFAULT_BUBBLE_OPACITY
                            maxSec = Constants.DEFAULT_MAX_SEC
                            settings.maxSec = Constants.DEFAULT_MAX_SEC
                        }
                    } else null
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

                        // 2. Groq Cloud Credentials
                        SettingsCategoryCard(
                            sectionNumber = "02",
                            title = "GROQ CLOUD CREDENTIALS",
                            subtitle = "API key authentication & KeyStore security",
                            statusTag = if (apiKey.isNotBlank()) "CONFIGURED" else "REQUIRED",
                            onClick = { currentSection = SettingsSection.CloudCredentials }
                        )

                        // 3. AI Inference Engines
                        SettingsCategoryCard(
                            sectionNumber = "03",
                            title = "AI INFERENCE ENGINES",
                            subtitle = "Whisper Large v3 (ASR) & Llama 3.1 (Refiner)",
                            statusTag = "ONLINE",
                            onClick = { currentSection = SettingsSection.AiInferenceEngines }
                        )

                        // 4. Floating Bubble & Timing
                        SettingsCategoryCard(
                            sectionNumber = "04",
                            title = "FLOATING BUBBLE & TIMING",
                            subtitle = "Bubble diameter, opacity, live preview & max record time",
                            statusTag = "${bubbleDp}DP • ${bubbleOpacity}%",
                            onClick = { currentSection = SettingsSection.BubbleAndInterface }
                        )

                        // 5. System Telemetry
                        SettingsCategoryCard(
                            sectionNumber = "05",
                            title = "SYSTEM TELEMETRY",
                            subtitle = "Kernel version, Compose runtime & TLS specs",
                            statusTag = "v1.0.0",
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

                        SettingsSection.CloudCredentials -> {
                            OledSettingsCard {
                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = {
                                        apiKey = it
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
                                    },
                                    textStyle = LocalTextStyle.current.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    ),
                                    singleLine = true
                                )

                                Text(
                                    text = "Encrypted on-device using Android KeyStore hardware backing.",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ColorSubtle
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Test Button
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                validating = true
                                                validateMsg = null
                                                validateOk = null
                                                settings.apiKey = apiKey.trim()
                                                val res = groq.validateKey(Constants.DEFAULT_BASE_URL, apiKey.trim())
                                                validating = false
                                                when (res) {
                                                    is GroqResult.Success -> {
                                                        validateOk = true
                                                        validateMsg = "✓ KEY VERIFIED"
                                                    }
                                                    is GroqResult.Error -> {
                                                        validateOk = false
                                                        validateMsg = "✗ ${res.message.take(35)}"
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
                                                text = "TEST",
                                                fontSize = 10.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }

                                    // Save Button
                                    Button(
                                        onClick = {
                                            settings.apiKey = apiKey.trim()
                                            validateMsg = "✓ SAVED TO KEYSTORE"
                                            validateOk = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ColorRedAccent,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "SAVE",
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    // Result Pill
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
                            }
                        }

                        SettingsSection.AiInferenceEngines -> {
                            OledSettingsCard {
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

                                ModelDropdown(
                                    label = "Text Refinement Model",
                                    category = "POST-PROCESSOR",
                                    selectedValue = llama,
                                    options = Constants.LLAMA_MODELS,
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
                                            text = "${maxSec} SEC",
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
                                        steps = 7,
                                        leftLabel = "15s",
                                        rightLabel = "120s"
                                    )
                                }
                            }
                        }

                        SettingsSection.SystemTelemetry -> {
                            OledSettingsCard {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TelemetryRow(label = "APP KERNEL", value = "OpenWispr v1.0.0 (Release)")
                                    HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    TelemetryRow(label = "UI RUNTIME", value = "Jetpack Compose + RenderThread")
                                    HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    TelemetryRow(label = "KEY STORAGE", value = "Android KeyStore Hardware AES-256")
                                    HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    TelemetryRow(label = "CLOUD ROUTE", value = "Groq Cloud API (TLS 1.3)")
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
        // Left back button - 44dp tactile squircle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ColorCardBg)
                .border(1.dp, ColorBorder, RoundedCornerShape(10.dp))
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
                .padding(horizontal = if (onReset != null) 72.dp else 52.dp)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Optional Right Action: RESET button (matching Flow Bubble Size reference)
        if (onReset != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ColorCardElevated)
                    .border(1.dp, ColorBorderSubtle, RoundedCornerShape(6.dp))
                    .clickable { onReset() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .align(Alignment.CenterEnd)
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
    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f)
    val fraction = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

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
            value = if (whisperModel.contains("turbo")) "WHISPER TURBO" else "WHISPER V3"
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(22.dp)
                .background(ColorBorderSubtle)
        )
        TelemetryStripItem(
            label = "REFINER",
            value = if (llamaModel.contains("70b")) "LLAMA 70B" else "LLAMA 8B"
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
                value = selectedValue,
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
                                Text(
                                    text = model,
                                    color = if (isSelected) ColorRedAccent else Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
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