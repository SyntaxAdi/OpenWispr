package com.openwispr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.openwispr.R
import com.openwispr.data.GroqRepository
import com.openwispr.data.GroqResult
import com.openwispr.data.SettingsRepository
import com.openwispr.util.Constants
import kotlinx.coroutines.launch

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
    var whisperExpanded by remember { mutableStateOf(false) }
    var llamaExpanded by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = "Navigation menu"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Open Wispr — Groq Cloud Speech-to-Text", style = MaterialTheme.typography.titleMedium)
            Text("Tap the floating bubble in any text field to dictate. Bubble appears only when a text box is focused, stays while recording, then pastes directly.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            // Permissions cards
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Permissions", style = MaterialTheme.typography.titleSmall)
                    val accOk = isAccessibilityEnabled()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(if (accOk) R.drawable.ic_check_circle else R.drawable.ic_warning), null, tint = if (accOk) Color(0xFF00C853) else Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Accessibility Service", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(onClick = { onOpenAccessibility() }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text(if (accOk) "Enabled" else "Enable") }
                    }
                    val micOk = hasMicPermission()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(if (micOk) R.drawable.ic_check_circle else R.drawable.ic_warning), null, tint = if (micOk) Color(0xFF00C853) else Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Microphone", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(onClick = { onRequestMic() }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text(if (micOk) "Granted" else "Grant") }
                    }
                    val overlayOk = canDrawOverlays()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(if (overlayOk) R.drawable.ic_check_circle else R.drawable.ic_warning), null, tint = if (overlayOk) Color(0xFF00C853) else Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Display over apps (optional)", style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onClick = { onOpenOverlay() }) { Text(if (overlayOk) "Granted" else "Allow") }
                    }
                    val notifOk = hasNotifPermission()
                    if (!notifOk) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Notifications (keep mic alive)", style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { onRequestNotif() }) { Text("Allow") }
                        }
                    }
                }
            }

            // API Key
            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it },
                label = { Text("Groq API Key (gsk_...)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") } },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    scope.launch {
                        validating = true; validateMsg = null; validateOk = null
                        settings.apiKey = apiKey.trim()
                        settings.baseUrl = baseUrl.trim()
                        val res = groq.validateKey(baseUrl.trim(), apiKey.trim())
                        validating = false
                        when (res) {
                            is GroqResult.Success -> { validateOk = true; validateMsg = "✓ API key valid" }
                            is GroqResult.Error -> { validateOk = false; validateMsg = "✗ ${res.message}" }
                        }
                    }
                }, enabled = !validating && apiKey.isNotBlank()) {
                    if (validating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Validate Key")
                }
                Button(onClick = {
                    settings.apiKey = apiKey.trim()
                    settings.baseUrl = baseUrl.trim()
                }) { Text("Save") }
                validateMsg?.let { Text(it, color = if (validateOk == true) Color(0xFF00C853) else Color(0xFFFF5252), style = MaterialTheme.typography.bodySmall) }
            }

            // Models
            ExposedDropdownMenuBox(expanded = whisperExpanded, onExpandedChange = { whisperExpanded = !whisperExpanded }) {
                OutlinedTextField(value = whisper, onValueChange = {}, readOnly = true, label = { Text("Whisper Model") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(whisperExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = whisperExpanded, onDismissRequest = { whisperExpanded = false }) {
                    Constants.WHISPER_MODELS.forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { whisper = it; whisperExpanded = false; settings.whisper = it })
                    }
                }
            }
            ExposedDropdownMenuBox(expanded = llamaExpanded, onExpandedChange = { llamaExpanded = !llamaExpanded }) {
                OutlinedTextField(value = llama, onValueChange = {}, readOnly = true, label = { Text("LLaMA Refine Model") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(llamaExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = llamaExpanded, onDismissRequest = { llamaExpanded = false }) {
                    Constants.LLAMA_MODELS.forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { llama = it; llamaExpanded = false; settings.llama = it })
                    }
                }
            }

            // Bubble size
            Text("Floating bubble size: ${bubbleDp}dp", style = MaterialTheme.typography.bodyMedium)
            Slider(value = bubbleDp.toFloat(), onValueChange = { bubbleDp = it.toInt() }, onValueChangeFinished = { settings.bubbleDp = bubbleDp }, valueRange = Constants.MIN_BUBBLE_DP.toFloat()..Constants.MAX_BUBBLE_DP.toFloat(), steps = 5)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${Constants.MIN_BUBBLE_DP}dp", style = MaterialTheme.typography.labelSmall); Text("${Constants.MAX_BUBBLE_DP}dp", style = MaterialTheme.typography.labelSmall) }

            // Max duration
            Text("Max recording time: ${maxSec}s", style = MaterialTheme.typography.bodyMedium)
            Slider(value = maxSec.toFloat(), onValueChange = { maxSec = it.toInt() }, onValueChangeFinished = { settings.maxSec = maxSec }, valueRange = Constants.MIN_REC_SEC.toFloat()..Constants.MAX_REC_SEC.toFloat(), steps = 6)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${Constants.MIN_REC_SEC}s", style = MaterialTheme.typography.labelSmall); Text("${Constants.MAX_REC_SEC}s", style = MaterialTheme.typography.labelSmall) }

            // Advanced
            TextButton(onClick = { showAdvanced = !showAdvanced }) { Text(if (showAdvanced) "Hide advanced" else "Show advanced") }
            if (showAdvanced) {
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Default: https://api.groq.com/openai/v1", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Button(onClick = { settings.baseUrl = baseUrl.trim() }) { Text("Save Base URL") }
            }

            HorizontalDivider()
            Text("How to use", style = MaterialTheme.typography.titleSmall)
            Text("1) Enable Accessibility + Mic above.\n2) Tap any text field (WhatsApp, Chrome, Notes) — bubble appears.\n3) Tap bubble → it expands to [ X  ~~~  ✓ ]. Speak — waveform reacts to your voice.\n4) Tap ✓ to transcribe & paste. Tap X to cancel. Auto-stops after ${maxSec}s.\n5) If field lost focus while speaking, text is copied to clipboard.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Design: SRP • DRY • KISS • OCP - Tiny APK, no heavy deps.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF7C4DFF))
            Spacer(Modifier.height(24.dp))
        }
    }
}