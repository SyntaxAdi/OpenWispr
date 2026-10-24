package com.openwispr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.openwispr.R
import com.openwispr.data.SettingsRepository
import com.openwispr.util.LanguageManager
import com.openwispr.util.LanguageOption

private val ColorDarkBg = Color(0xFF000000)
private val ColorCardBg = Color(0xFF070709)
private val ColorCardElevated = Color(0xFF101014)
private val ColorCardSelected = Color(0xFF16161E)
private val ColorBorder = Color(0xFF141416)
private val ColorBorderSubtle = Color(0xFF222228)
private val ColorMuted = Color(0xFF8E8E93)
private val ColorSubtle = Color(0xFF48484A)
private val ColorRedAccent = Color(0xFFFF2E2E)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranslationScreen(
    settings: SettingsRepository,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var spokenCode by remember { mutableStateOf(settings.translationSpokenLanguage) }
    var targetCode by remember { mutableStateOf(settings.translationTargetLanguage) }
    var recentSpoken by remember { mutableStateOf(settings.recentSpokenLanguages) }
    var recentTarget by remember { mutableStateOf(settings.recentTargetLanguages) }

    var showSpokenDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val isTranslating = targetCode != "off"
    val spokenDisplayName = LanguageManager.getDisplayName(spokenCode)
    val targetDisplayName = LanguageManager.getDisplayName(targetCode)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorDarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(
                top = statusBarPadding + 10.dp,
                bottom = navBarPadding + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Header Bar
            item(key = "header") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorCardElevated)
                            .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
                            .clickable { onOpenDrawer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu),
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp)
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        DotMatrixText(
                            text = "TRANSLATION",
                            dotSize = 2.4.dp,
                            dotSpacing = 0.8.dp,
                            charSpacing = 1.6.dp,
                            color = Color.White
                        )
                    }
                }
            }

            // 1. Diagnostic Telemetry Strip (Menu Bar)
            item(key = "telemetry_strip") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorCardBg)
                        .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showSpokenDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        TelemetryStripItem(
                            label = "SPOKEN INPUT",
                            value = if (spokenCode == "auto") "AUTO DETECT" else spokenDisplayName.uppercase()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(22.dp)
                            .background(ColorBorderSubtle)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTargetDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        TelemetryStripItem(
                            label = "TARGET OUTPUT",
                            value = if (targetCode == "off") "ORIGINAL" else targetDisplayName.uppercase(),
                            isAlert = isTranslating
                        )
                    }
                }
            }

            // 2. Section 01: Spoken Input Language
            item(key = "spoken_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ColorCardBg)
                        .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "// 01  SPOKEN INPUT LANGUAGE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ColorRedAccent,
                                letterSpacing = 0.8.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ColorCardElevated)
                                    .border(0.5.dp, ColorBorderSubtle, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = spokenCode.uppercase(),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            text = "Select the language you speak in. Leave on Auto-Detect to automatically detect speech.",
                            fontSize = 12.sp,
                            color = ColorMuted,
                            lineHeight = 16.sp
                        )

                        // Current Selected Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorCardElevated)
                                .border(1.dp, ColorBorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { showSpokenDialog = true }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_translate),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(ColorBorderSubtle)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spokenDisplayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (spokenCode == "auto") "Detects spoken language automatically" else "Fixed input language: $spokenDisplayName",
                                    fontSize = 11.sp,
                                    color = ColorMuted
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = "Select language",
                                tint = ColorRedAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Dynamic Past Choices Chips (Two-row non-swiping layout)
                        Text(
                            text = "QUICK SELECT:",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorSubtle
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            recentSpoken.take(8).forEach { code ->
                                val isSelected = spokenCode == code
                                val name = if (code == "auto") "Auto-Detect" else LanguageManager.getDisplayName(code)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ColorCardSelected else ColorCardElevated)
                                        .border(1.dp, if (isSelected) ColorRedAccent else ColorBorderSubtle, RoundedCornerShape(8.dp))
                                        .clickable {
                                            spokenCode = code
                                            settings.translationSpokenLanguage = code
                                            settings.recordSpokenLanguageChoice(code)
                                            recentSpoken = settings.recentSpokenLanguages
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else ColorMuted,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

            // 2. Section 02: Target Translation Language
            item(key = "target_section") {
                Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ColorCardBg)
                            .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "// 02  TARGET OUTPUT LANGUAGE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ColorRedAccent,
                                letterSpacing = 0.8.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isTranslating) ColorRedAccent.copy(alpha = 0.15f) else ColorCardElevated)
                                    .border(0.5.dp, if (isTranslating) ColorRedAccent else ColorBorderSubtle, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isTranslating) targetCode.uppercase() else "OFF",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isTranslating) ColorRedAccent else Color.White
                                )
                            }
                        }

                        Text(
                            text = "Converts spoken speech into this language using contextual AI. Preserves formality, tone, and custom prompts.",
                            fontSize = 12.sp,
                            color = ColorMuted,
                            lineHeight = 16.sp
                        )

                        // Current Selected Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorCardElevated)
                                .border(1.dp, ColorBorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { showTargetDialog = true }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_translate),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(ColorBorderSubtle)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (targetCode == "off") "Original (No Translation)" else targetDisplayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (targetCode == "off") "No translation (transcribe original speech)" else "Translates output into $targetDisplayName",
                                    fontSize = 11.sp,
                                    color = if (isTranslating) ColorRedAccent else ColorMuted
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = "Select target language",
                                tint = ColorRedAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Dynamic Past Choices Chips (Two-row non-swiping layout)
                        Text(
                            text = "QUICK SELECT:",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorSubtle
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            recentTarget.take(8).forEach { code ->
                                val isSelected = targetCode == code
                                val name = if (code == "off") "Original (Off)" else LanguageManager.getDisplayName(code)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ColorCardSelected else ColorCardElevated)
                                        .border(1.dp, if (isSelected) ColorRedAccent else ColorBorderSubtle, RoundedCornerShape(8.dp))
                                        .clickable {
                                            targetCode = code
                                            settings.translationTargetLanguage = code
                                            settings.recordTargetLanguageChoice(code)
                                            recentTarget = settings.recentTargetLanguages
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else ColorMuted,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

            // 3. Quick Reset
            item(key = "reset_section") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorCardElevated)
                        .border(1.dp, ColorBorderSubtle, RoundedCornerShape(12.dp))
                        .clickable {
                            spokenCode = "auto"
                            targetCode = "off"
                            settings.translationSpokenLanguage = "auto"
                            settings.translationTargetLanguage = "off"
                            settings.recordSpokenLanguageChoice("auto")
                            settings.recordTargetLanguageChoice("off")
                            recentSpoken = settings.recentSpokenLanguages
                            recentTarget = settings.recentTargetLanguages
                        }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RESET TO DEFAULT (AUTO-DETECT / NO TRANSLATION)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ColorMuted
                    )
                }
            }
        }
    }

    // Modal Search Dialog for Spoken Language
    if (showSpokenDialog) {
        LanguagePickerDialog(
            title = "Select Spoken Input Language",
            options = LanguageManager.getSpokenLanguages(),
            selectedCode = spokenCode,
            onSelect = {
                spokenCode = it
                settings.translationSpokenLanguage = it
                settings.recordSpokenLanguageChoice(it)
                recentSpoken = settings.recentSpokenLanguages
                showSpokenDialog = false
            },
            onDismiss = { showSpokenDialog = false }
        )
    }

    // Modal Search Dialog for Target Language
    if (showTargetDialog) {
        LanguagePickerDialog(
            title = "Select Target Output Language",
            options = LanguageManager.getTargetLanguages(),
            selectedCode = targetCode,
            onSelect = {
                targetCode = it
                settings.translationTargetLanguage = it
                settings.recordTargetLanguageChoice(it)
                recentTarget = settings.recentTargetLanguages
                showTargetDialog = false
            },
            onDismiss = { showTargetDialog = false }
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
private fun LanguagePickerDialog(
    title: String,
    options: List<LanguageOption>,
    selectedCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredOptions = remember(searchQuery, options) {
        if (searchQuery.isBlank()) {
            options
        } else {
            val q = searchQuery.trim().lowercase()
            options.filter {
                it.displayName.lowercase().contains(q) ||
                it.nativeName.lowercase().contains(q) ||
                it.code.lowercase().contains(q)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(18.dp))
                .background(ColorDarkBg)
                .border(1.dp, ColorBorderSubtle, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title.uppercase(),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ColorCardElevated)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", color = ColorMuted, fontSize = 12.sp)
                    }
                }

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search ${options.size} languages...", color = ColorSubtle, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorCardBg,
                        unfocusedContainerColor = ColorCardBg,
                        focusedBorderColor = ColorRedAccent,
                        unfocusedBorderColor = ColorBorderSubtle,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                )

                // List of Languages
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredOptions, key = { it.code }) { opt ->
                        val isSelected = opt.code == selectedCode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ColorCardSelected else ColorCardBg)
                                .border(0.5.dp, if (isSelected) ColorRedAccent else ColorBorder, RoundedCornerShape(8.dp))
                                .clickable { onSelect(opt.code) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = opt.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ColorRedAccent else Color.White
                                )
                                if (opt.nativeName.isNotBlank() && opt.nativeName != opt.displayName) {
                                    Text(
                                        text = opt.nativeName,
                                        fontSize = 10.sp,
                                        color = ColorMuted
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ColorCardElevated)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = opt.code.uppercase(),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ColorMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
