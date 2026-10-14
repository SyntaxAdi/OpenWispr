package com.openwispr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

private val ColorDarkBg = Color(0xFF000000)
private val ColorCardBg = Color(0xFF14151C)
private val ColorSurfaceInput = Color(0xFF0C0D12)
private val ColorBorder = Color(0xFF262832)
private val ColorRedAccent = Color(0xFFE02E2E)
private val ColorGreenAccent = Color(0xFF00C853)
private val ColorMuted = Color(0xFF8A909E)
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
        unfocusedBorderColor = ColorBorder,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = ColorRedAccent,
        unfocusedLabelColor = ColorMuted,
        cursorColor = ColorRedAccent
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = topInset + 12.dp, bottom = bottomInset + 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ColorCardBg)
                            .border(1.dp, ColorBorder, RoundedCornerShape(10.dp))
                            .clickable { onOpenDrawer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu),
                            contentDescription = "Open Drawer",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "SETTINGS",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "// SYSTEM CONFIGURATION",
                            fontSize = 9.5.sp,
                            color = ColorRedAccent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Permissions Card
            OledSettingsCard(title = "// HARDWARE & PERMISSIONS") {
                val accOk = isAccessibilityEnabled()
                PermissionRow(
                    label = "Accessibility Service",
                    isGranted = accOk,
                    actionText = if (accOk) "Active" else "Enable",
                    onAction = onOpenAccessibility
                )

                HorizontalDivider(color = ColorBorder.copy(alpha = 0.4f), thickness = 0.5.dp)

                val micOk = hasMicPermission()
                PermissionRow(
                    label = "Microphone Access",
                    isGranted = micOk,
                    actionText = if (micOk) "Granted" else "Grant",
                    onAction = onRequestMic
                )

                HorizontalDivider(color = ColorBorder.copy(alpha = 0.4f), thickness = 0.5.dp)

                val overlayOk = canDrawOverlays()
                PermissionRow(
                    label = "Display Over Apps",
                    isGranted = overlayOk,
                    actionText = if (overlayOk) "Active" else "Allow",
                    onAction = onOpenOverlay
                )

                val notifOk = hasNotifPermission()
                if (!notifOk) {
                    HorizontalDivider(color = ColorBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                    PermissionRow(
                        label = "Foreground Keep-Alive",
                        isGranted = false,
                        actionText = "Allow",
                        onAction = onRequestNotif
                    )
                }
            }

            // Cloud API Credentials
            OledSettingsCard(title = "// GROQ CLOUD CREDENTIALS") {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Groq API Key (gsk_...)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(
                                if (showKey) "HIDE" else "SHOW",
                                color = ColorMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    },
                    singleLine = true
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                        validateMsg = "✓ Valid Key"
                                    }
                                    is GroqResult.Error -> {
                                        validateOk = false
                                        validateMsg = "✗ ${res.message.take(40)}"
                                    }
                                }
                            }
                        },
                        enabled = !validating && apiKey.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorCardBg,
                            contentColor = Color.White,
                            disabledContainerColor = ColorCardBg.copy(alpha = 0.5f),
                            disabledContentColor = ColorMuted
                        ),
                        modifier = Modifier.border(1.dp, ColorBorder, RoundedCornerShape(8.dp))
                    ) {
                        if (validating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = ColorRedAccent
                            )
                        } else {
                            Text("VALIDATE", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Button(
                        onClick = {
                            settings.apiKey = apiKey.trim()
                            settings.baseUrl = baseUrl.trim()
                            validateMsg = "✓ Saved"
                            validateOk = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorRedAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Text("SAVE", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    validateMsg?.let {
                        Text(
                            text = it,
                            color = if (validateOk == true) ColorGreenAccent else ColorRedAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Model Selection Card
            OledSettingsCard(title = "// AI INFERENCE MODELS") {
                ModelDropdown(
                    label = "Speech-to-Text Model",
                    selectedValue = whisper,
                    options = Constants.WHISPER_MODELS,
                    textFieldColors = textFieldColors
                ) {
                    whisper = it
                    settings.whisper = it
                }

                ModelDropdown(
                    label = "Text Refinement Model",
                    selectedValue = llama,
                    options = Constants.LLAMA_MODELS,
                    textFieldColors = textFieldColors
                ) {
                    llama = it
                    settings.llama = it
                }
            }

            // Dictation Bubble & Audio Tuning
            OledSettingsCard(title = "// INTERFACE & TIMING TUNING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FLOATING BUBBLE DIAMETER",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ColorMuted
                    )
                    Text(
                        text = "${bubbleDp} DP",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
                        inactiveTrackColor = ColorBorder
                    )
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MAX RECORDING TIME",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ColorMuted
                    )
                    Text(
                        text = "${maxSec} SECONDS",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
                        inactiveTrackColor = ColorBorder
                    )
                )
            }

            // Advanced Options
            OledSettingsCard(title = "// ADVANCED ENDPOINTS") {
                TextButton(
                    onClick = { showAdvanced = !showAdvanced },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (showAdvanced) "[-] HIDE CUSTOM ENDPOINT" else "[+] SHOW CUSTOM ENDPOINT",
                        color = ColorRedAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (showAdvanced) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL Endpoint") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )
                    Text(
                        text = "DEFAULT: https://api.groq.com/openai/v1",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ColorSubtle
                    )
                    Button(
                        onClick = { settings.baseUrl = baseUrl.trim() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorCardBg,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.border(1.dp, ColorBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text("SAVE ENDPOINT", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun OledSettingsCard(
    title: String,
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
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = ColorRedAccent,
                letterSpacing = 0.5.sp
            )
            content()
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    isGranted: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isGranted) ColorGreenAccent else ColorRedAccent)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isGranted) Color(0xFF1B2A20) else Color(0xFF2D1818))
                .border(
                    1.dp,
                    if (isGranted) ColorGreenAccent.copy(alpha = 0.5f) else ColorRedAccent.copy(alpha = 0.5f),
                    RoundedCornerShape(6.dp)
                )
                .clickable { onAction() }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = actionText.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isGranted) ColorGreenAccent else ColorRedAccent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    textFieldColors: TextFieldColors,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(10.dp),
            colors = textFieldColors
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(ColorCardBg)
        ) {
            options.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                    onClick = {
                        onSelect(model)
                        expanded = false
                    }
                )
            }
        }
    }
}