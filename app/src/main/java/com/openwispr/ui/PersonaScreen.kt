package com.openwispr.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openwispr.R
import com.openwispr.data.AppPromptRule
import com.openwispr.data.FormalityMode
import com.openwispr.data.PromptProfile
import com.openwispr.data.SettingsRepository
import com.openwispr.util.Constants
import java.util.UUID

// Nothing OS Dark Theme Tokens
private val ColorDarkBg = Color(0xFF000000)
private val ColorCardBg = Color(0xFF070709)
private val ColorCardElevated = Color(0xFF101014)
private val ColorSurfaceInput = Color(0xFF0C0D12)
private val ColorBorder = Color(0xFF141416)
private val ColorBorderSubtle = Color(0xFF262832)
private val ColorRedAccent = Color(0xFFFF2E2E)
private val ColorTextPrimary = Color(0xFFEEEEEE)
private val ColorTextSecondary = Color(0xFF8E95A2)
private val ColorMuted = Color(0xFF5A606E)

@Composable
fun PersonaScreen(
    settings: SettingsRepository,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State bindings
    var globalMode by remember { mutableStateOf(settings.globalFormalityMode) }
    var basePromptText by remember { mutableStateOf(settings.customBaseSystemPrompt) }
    var profiles by remember { mutableStateOf(settings.getPromptProfiles()) }
    var activeProfileId by remember { mutableStateOf(settings.activeProfileId) }
    var appRules by remember { mutableStateOf(settings.getAppPromptRules()) }

    // Dialog controllers
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var showResetBaseConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorDarkBg)
    ) {
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
            // 1. Top Header Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
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

                    Spacer(modifier = Modifier.width(16.dp))

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DotMatrixText(
                            text = "PERSONA ",
                            dotSize = 3.4.dp,
                            dotSpacing = 1.1.dp,
                            charSpacing = 2.8.dp,
                            color = Color.White
                        )
                        DotMatrixText(
                            text = "& PROMPTS",
                            dotSize = 3.4.dp,
                            dotSpacing = 1.1.dp,
                            charSpacing = 2.8.dp,
                            color = ColorRedAccent
                        )
                    }
                }
            }

            // 2. Section: Global Formality Mode
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "// GLOBAL TONE & FORMALITY",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ColorMuted,
                        letterSpacing = 1.2.sp
                    )

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ColorBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Select baseline voice tone applied to dictations:",
                                fontSize = 12.sp,
                                color = ColorTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )

                            FormalityMode.values().forEach { mode ->
                                val isSelected = globalMode == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) ColorCardElevated else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSelected) ColorRedAccent else ColorBorderSubtle,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            globalMode = mode
                                            settings.globalFormalityMode = mode
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.displayName.uppercase(),
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else ColorTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = mode.description,
                                            fontSize = 11.sp,
                                            color = if (isSelected) ColorTextSecondary else ColorMuted
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) ColorRedAccent else Color.Transparent)
                                            .border(1.5.dp, if (isSelected) ColorRedAccent else ColorMuted, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Section: App-Specific Rules
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "// APP-SPECIFIC OVERRIDES",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ColorMuted,
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            text = "+ ADD RULE",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ColorRedAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showAddRuleDialog = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ColorBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (appRules.isEmpty()) {
                                Text(
                                    text = "No app rules active. Dictations follow global tone across all applications.",
                                    fontSize = 12.sp,
                                    color = ColorMuted,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                appRules.values.forEach { rule ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ColorCardElevated)
                                            .border(1.dp, ColorBorderSubtle, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rule.appName.uppercase(),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White
                                            )
                                            Text(
                                                text = rule.packageName,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = ColorMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Mode: ${rule.formalityMode.displayName}",
                                                fontSize = 11.sp,
                                                color = ColorRedAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                settings.deleteAppPromptRule(rule.packageName)
                                                appRules = settings.getAppPromptRules()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_delete),
                                                contentDescription = "Delete Rule",
                                                tint = ColorMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Section: Custom Prompt Profiles
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "// CUSTOM PROMPT PROFILES",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ColorMuted,
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            text = "+ NEW PROFILE",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ColorRedAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showAddProfileDialog = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ColorBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Multiple profiles can be saved. Exactly one active profile is used when Custom mode is selected:",
                                fontSize = 12.sp,
                                color = ColorTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )

                            if (profiles.isEmpty()) {
                                Text(
                                    text = "No custom profiles created yet. Tap '+ NEW PROFILE' to create one.",
                                    fontSize = 12.sp,
                                    color = ColorMuted,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                profiles.forEach { profile ->
                                    val isActive = activeProfileId == profile.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isActive) ColorCardElevated else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (isActive) ColorRedAccent else ColorBorderSubtle,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                activeProfileId = profile.id
                                                settings.activeProfileId = profile.id
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isActive) ColorRedAccent else Color.Transparent)
                                                        .border(1.5.dp, if (isActive) ColorRedAccent else ColorMuted, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = profile.name.uppercase(),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (isActive) Color.White else ColorTextSecondary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = profile.promptText,
                                                fontSize = 11.sp,
                                                color = ColorMuted,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                settings.deletePromptProfile(profile.id)
                                                profiles = settings.getPromptProfiles()
                                                activeProfileId = settings.activeProfileId
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_delete),
                                                contentDescription = "Delete Profile",
                                                tint = ColorMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Section: Base System Prompt Editor & Reset
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "// BASE SYSTEM PROMPT",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ColorMuted,
                        letterSpacing = 1.2.sp
                    )

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ColorBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Core prompt used by Default mode for gibberish and stutter removal. You can freely edit and reset anytime:",
                                fontSize = 12.sp,
                                color = ColorTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )

                            OutlinedTextField(
                                value = basePromptText,
                                onValueChange = { basePromptText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 220.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ColorTextPrimary,
                                    lineHeight = 18.sp
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = ColorSurfaceInput,
                                    unfocusedContainerColor = ColorSurfaceInput,
                                    focusedBorderColor = ColorRedAccent,
                                    unfocusedBorderColor = ColorBorderSubtle,
                                    cursorColor = ColorRedAccent
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Save Button
                                Button(
                                    onClick = {
                                        settings.customBaseSystemPrompt = basePromptText
                                        Toast.makeText(context, "System prompt saved", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorCardElevated)
                                ) {
                                    Text(
                                        text = "SAVE PROMPT",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }

                                // Reset to default button
                                OutlinedButton(
                                    onClick = { showResetBaseConfirmDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorRedAccent),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ColorRedAccent.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "RESET DEFAULT",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ColorRedAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog: Reset Base System Prompt Confirmation
        if (showResetBaseConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetBaseConfirmDialog = false },
                containerColor = ColorCardElevated,
                titleContentColor = Color.White,
                textContentColor = ColorTextSecondary,
                title = {
                    Text(
                        text = "Reset System Prompt?",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = "This will restore the original speech cleaner instructions. Your current customizations to the base prompt will be overwritten.",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            settings.resetBaseSystemPrompt()
                            basePromptText = settings.customBaseSystemPrompt
                            showResetBaseConfirmDialog = false
                            Toast.makeText(context, "Restored to default", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("RESET", color = ColorRedAccent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetBaseConfirmDialog = false }) {
                        Text("CANCEL", color = ColorTextSecondary)
                    }
                }
            )
        }

        // Dialog: Add App-Specific Rule
        if (showAddRuleDialog) {
            var selectedAppPreset by remember { mutableStateOf("Telegram") }
            var appPkg by remember { mutableStateOf("org.telegram.messenger") }
            var appNameInput by remember { mutableStateOf("Telegram") }
            var ruleMode by remember { mutableStateOf(FormalityMode.FRIENDLY) }
            var ruleProfileId by remember { mutableStateOf<String?>(profiles.firstOrNull()?.id) }

            val presets = listOf(
                "Telegram" to "org.telegram.messenger",
                "WhatsApp" to "com.whatsapp",
                "Slack" to "com.Slack",
                "Gmail" to "com.google.android.gm",
                "Chrome" to "com.android.chrome"
            )

            AlertDialog(
                onDismissRequest = { showAddRuleDialog = false },
                containerColor = ColorCardElevated,
                title = {
                    Text(
                        text = "Add App Override",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Quick Presets:",
                            fontSize = 11.sp,
                            color = ColorMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presets.take(3).forEach { (name, pkg) ->
                                val isSelected = appPkg == pkg
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) ColorRedAccent else ColorSurfaceInput)
                                        .clickable {
                                            selectedAppPreset = name
                                            appPkg = pkg
                                            appNameInput = name
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) Color.White else ColorTextSecondary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = appNameInput,
                            onValueChange = { appNameInput = it },
                            label = { Text("App Name (e.g. Telegram)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ColorSurfaceInput,
                                unfocusedContainerColor = ColorSurfaceInput,
                                focusedBorderColor = ColorRedAccent,
                                unfocusedBorderColor = ColorBorderSubtle
                            )
                        )

                        OutlinedTextField(
                            value = appPkg,
                            onValueChange = { appPkg = it.trim() },
                            label = { Text("Package (e.g. org.telegram.messenger)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ColorSurfaceInput,
                                unfocusedContainerColor = ColorSurfaceInput,
                                focusedBorderColor = ColorRedAccent,
                                unfocusedBorderColor = ColorBorderSubtle
                            )
                        )

                        Text(
                            text = "Formality Mode for this app:",
                            fontSize = 11.sp,
                            color = ColorMuted,
                            fontFamily = FontFamily.Monospace
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FormalityMode.values().forEach { m ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { ruleMode = m }
                                        .padding(vertical = 2.dp)
                                ) {
                                    RadioButton(
                                        selected = ruleMode == m,
                                        onClick = { ruleMode = m },
                                        colors = RadioButtonDefaults.colors(selectedColor = ColorRedAccent)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = m.displayName,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (appPkg.isNotBlank() && appNameInput.isNotBlank()) {
                                settings.saveAppPromptRule(
                                    AppPromptRule(
                                        packageName = appPkg,
                                        appName = appNameInput,
                                        formalityMode = ruleMode,
                                        customProfileId = if (ruleMode == FormalityMode.CUSTOM) ruleProfileId else null
                                    )
                                )
                                appRules = settings.getAppPromptRules()
                                showAddRuleDialog = false
                            }
                        }
                    ) {
                        Text("SAVE RULE", color = ColorRedAccent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddRuleDialog = false }) {
                        Text("CANCEL", color = ColorTextSecondary)
                    }
                }
            )
        }

        // Dialog: Create New Prompt Profile
        if (showAddProfileDialog) {
            var profileName by remember { mutableStateOf("") }
            var profilePrompt by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddProfileDialog = false },
                containerColor = ColorCardElevated,
                title = {
                    Text(
                        text = "New Prompt Profile",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            label = { Text("Profile Name (e.g. Work Email)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ColorSurfaceInput,
                                unfocusedContainerColor = ColorSurfaceInput,
                                focusedBorderColor = ColorRedAccent,
                                unfocusedBorderColor = ColorBorderSubtle
                            )
                        )

                        OutlinedTextField(
                            value = profilePrompt,
                            onValueChange = { profilePrompt = it },
                            label = { Text("System Instructions", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 180.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ColorSurfaceInput,
                                unfocusedContainerColor = ColorSurfaceInput,
                                focusedBorderColor = ColorRedAccent,
                                unfocusedBorderColor = ColorBorderSubtle
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (profileName.isNotBlank() && profilePrompt.isNotBlank()) {
                                val newProfile = PromptProfile(
                                    id = UUID.randomUUID().toString(),
                                    name = profileName.trim(),
                                    promptText = profilePrompt.trim()
                                )
                                settings.savePromptProfile(newProfile)
                                profiles = settings.getPromptProfiles()
                                activeProfileId = newProfile.id
                                showAddProfileDialog = false
                            }
                        }
                    ) {
                        Text("SAVE PROFILE", color = ColorRedAccent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddProfileDialog = false }) {
                        Text("CANCEL", color = ColorTextSecondary)
                    }
                }
            )
        }
    }
}
