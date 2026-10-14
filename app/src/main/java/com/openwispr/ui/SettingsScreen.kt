package com.openwispr.ui

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openwispr.R
import com.openwispr.data.GroqRepository
import com.openwispr.data.GroqResult
import com.openwispr.data.SettingsRepository
import com.openwispr.util.Constants
import kotlinx.coroutines.launch

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

    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var baseUrl by remember { mutableStateOf(settings.baseUrl) }
    var whisper by remember { mutableStateOf(settings.whisper) }
    var llama by remember { mutableStateOf(settings.llama) }
    var bubbleDp by remember { mutableIntStateOf(settings.bubbleDp) }
    var maxSec by remember { mutableIntStateOf(settings.maxSec) }
    var showKey by remember { mutableStateOf(false) }
    var validating by remember { mutableStateOf(false) }
    var validateMsg by remember { mutableStateOf<String?>(null) }
    var validateOk by remember { mutableStateOf<Boolean?>(null) }
    var showAdvanced by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = topInset + 10.dp, bottom = bottomInset + 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Top Bar Header
            SettingsTopBarHeader(onOpenDrawer = onOpenDrawer)

            // 2. Telemetry Overview Bar
            SettingsTelemetryStrip(
                whisperModel = whisper,
                llamaModel = llama,
                activePerms = activePerms,
                totalPerms = totalPerms
            )

            // 3. Card 01: Hardware & System Access Permissions
            OledSettingsCard(
                sectionNumber = "01",
                title = "HARDWARE & SYSTEM ACCESS",
                tag = if (activePerms == totalPerms) "SYS_OPTIMAL" else "REQUIRES_AUTH"
            ) {
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

            // 4. Card 02: Groq Cloud Inference Credentials
            OledSettingsCard(
                sectionNumber = "02",
                title = "GROQ CLOUD CREDENTIALS",
                tag = "API_AUTH"
            ) {
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
                    // Validate Button
                    Button(
                        onClick = {
                            scope.launch {
                                validating = true
                                validateMsg = null
                                validateOk = null
                                settings.apiKey = apiKey.trim()
                                settings.baseUrl = baseUrl.trim()
                                val res = groq.validateKey(baseUrl.trim(), apiKey.trim())
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
                            settings.baseUrl = baseUrl.trim()
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

            // 5. Card 03: Neural Inference Models
            OledSettingsCard(
                sectionNumber = "03",
                title = "AI INFERENCE ENGINES",
                tag = "NEURAL_PIPE"
            ) {
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

            // 6. Card 04: Floating Bubble & Timing Tuning
            OledSettingsCard(
                sectionNumber = "04",
                title = "INTERFACE & HARDWARE TUNING",
                tag = "CALIBRATION"
            ) {
                // Parameter 1: Bubble Diameter
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FLOATING BUBBLE DIAMETER",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ColorMuted,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Screen footprint of the tactile dictation trigger",
                                fontSize = 10.sp,
                                color = ColorSubtle
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ColorCardElevated)
                                .border(1.dp, ColorBorderSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${bubbleDp} DP",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Slider(
                        value = bubbleDp.toFloat(),
                        onValueChange = { bubbleDp = it.toInt() },
                        onValueChangeFinished = { settings.bubbleDp = bubbleDp },
                        valueRange = Constants.MIN_BUBBLE_DP.toFloat()..Constants.MAX_BUBBLE_DP.toFloat(),
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = ColorRedAccent,
                            activeTrackColor = ColorRedAccent,
                            inactiveTrackColor = ColorBorderSubtle
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${Constants.MIN_BUBBLE_DP} DP (COMPACT)",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorSubtle
                        )
                        Text(
                            text = "${Constants.MAX_BUBBLE_DP} DP (EXPANDED)",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorSubtle
                        )
                    }
                }

                HorizontalDivider(color = ColorBorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

                // Parameter 2: Max Recording Duration
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MAX RECORDING DURATION",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ColorMuted,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Hardware audio buffer safety ceiling",
                                fontSize = 10.sp,
                                color = ColorSubtle
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ColorCardElevated)
                                .border(1.dp, ColorBorderSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${maxSec} SEC",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Slider(
                        value = maxSec.toFloat(),
                        onValueChange = { maxSec = it.toInt() },
                        onValueChangeFinished = { settings.maxSec = maxSec },
                        valueRange = Constants.MIN_REC_SEC.toFloat()..Constants.MAX_REC_SEC.toFloat(),
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = ColorRedAccent,
                            activeTrackColor = ColorRedAccent,
                            inactiveTrackColor = ColorBorderSubtle
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${Constants.MIN_REC_SEC} SEC",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorSubtle
                        )
                        Text(
                            text = "${Constants.MAX_REC_SEC} SEC",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorSubtle
                        )
                    }
                }
            }

            // 7. Card 05: Advanced Network Endpoints
            OledSettingsCard(
                sectionNumber = "05",
                title = "NETWORK ROUTING & PROXIES",
                tag = "ROUTING"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showAdvanced) "[-] HIDE CUSTOM ENDPOINT" else "[+] CONFIGURE CUSTOM ENDPOINT",
                        color = ColorRedAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (showAdvanced) "OPEN" else "COLLAPSED",
                        color = ColorSubtle,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (showAdvanced) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = {
                            Text(
                                text = "Base URL Endpoint",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "DEFAULT: ${Constants.DEFAULT_BASE_URL}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ColorSubtle
                    )
                    Button(
                        onClick = { settings.baseUrl = baseUrl.trim() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorCardElevated,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.border(1.dp, ColorBorderSubtle, RoundedCornerShape(8.dp)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SAVE ENDPOINT",
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 8. Card 06: Device & Runtime Telemetry
            OledSettingsCard(
                sectionNumber = "06",
                title = "SYSTEM & RUNTIME TELEMETRY",
                tag = "DIAGNOSTICS"
            ) {
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

        // Center Title: Dot Matrix SETTINGS aligned to same level as menu button
        DotMatrixText(
            text = "SETTINGS",
            dotSize = 3.0.dp,
            dotSpacing = 1.0.dp,
            charSpacing = 2.2.dp,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
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
    sectionNumber: String,
    title: String,
    tag: String? = null,
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ColorMuted,
                        letterSpacing = 0.8.sp
                    )
                }

                if (tag != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ColorCardElevated)
                            .border(1.dp, ColorBorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorSubtle,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

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