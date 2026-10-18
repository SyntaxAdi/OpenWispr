package com.openwispr.util

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast

object TextInjector {
    fun isPasswordField(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isPassword) return true
        val it = node.inputType
        val variation = it and 0x00000ff0 // InputType.TYPE_MASK_VARIATION
        return (it and 0x80) != 0 ||
            variation == 0x00000080 || // TYPE_TEXT_VARIATION_PASSWORD
            variation == 0x00000090 || // TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            variation == 0x000000e0 || // TYPE_TEXT_VARIATION_WEB_PASSWORD
            variation == 0x00000010    // TYPE_NUMBER_VARIATION_PASSWORD
    }

    fun isInputNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isEditable) return true
        val cls = node.className?.toString().orEmpty()
        if (cls.contains("EditText", ignoreCase = true)) return true
        return supportsAction(node, AccessibilityNodeInfo.ACTION_SET_TEXT) ||
            supportsAction(node, AccessibilityNodeInfo.ACTION_PASTE)
    }

    private fun supportsAction(node: AccessibilityNodeInfo, actionId: Int): Boolean {
        return try {
            node.actionList?.any { it.id == actionId } == true
        } catch (_: Exception) { false }
    }

    fun findTargetNode(service: AccessibilityService): AccessibilityNodeInfo? {
        try {
            val f = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (f != null && isInputNode(f)) return f
        } catch (_: Exception) {}

        val windowRoots = mutableListOf<AccessibilityNodeInfo>()
        try {
            val wins = service.windows
            if (!wins.isNullOrEmpty()) {
                wins.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                    .mapNotNull { it.root }
                    .forEach { windowRoots.add(it) }
                wins.filter { it.type != AccessibilityWindowInfo.TYPE_APPLICATION }
                    .mapNotNull { it.root }
                    .forEach { windowRoots.add(it) }
            }
        } catch (_: Exception) {}

        try {
            service.rootInActiveWindow?.let { if (!windowRoots.contains(it)) windowRoots.add(0, it) }
        } catch (_: Exception) {}

        for (root in windowRoots) {
            try {
                val f = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (f != null && isInputNode(f)) return f
            } catch (_: Exception) {}
            val found = findFocusedInput(root)
            if (found != null) return found
        }

        for (root in windowRoots) {
            val anyInput = findAnyInput(root)
            if (anyInput != null) return anyInput
        }

        return null
    }

    private fun findFocusedInput(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isFocused && isInputNode(root)) return root
        for (i in 0 until root.childCount) {
            val c = root.getChild(i) ?: continue
            val res = findFocusedInput(c)
            if (res != null) return res
        }
        return null
    }

    private fun findAnyInput(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isVisibleToUser && isInputNode(root) && !isPasswordField(root)) return root
        for (i in 0 until root.childCount) {
            val c = root.getChild(i) ?: continue
            val res = findAnyInput(c)
            if (res != null) return res
        }
        return null
    }

    fun inject(service: AccessibilityService, text: String): Boolean {
        val target = findTargetNode(service)

        // Security check: Refuse injection and suppress clipboard fallback on secure credential fields
        if (isPasswordField(target)) {
            Toast.makeText(service, "Voice dictation disabled on password fields", Toast.LENGTH_SHORT).show()
            return false
        }

        var safe = Constants.sanitizeOutput(text)
        val hintTarget = target?.let { getHint(it) }
        if (!hintTarget.isNullOrBlank()) {
            safe = stripHintPrefix(safe, hintTarget)
            safe = Constants.sanitizeOutput(safe)
        }

        if (target != null && tryDirect(service, target, safe)) return true
        return copyToClipboard(service, safe)
    }

    private fun tryDirect(service: AccessibilityService, node: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            if (isPasswordField(node)) return false

            if (!node.isFocused) {
                try { node.performAction(AccessibilityNodeInfo.ACTION_FOCUS) } catch (_: Exception) {}
            }

            val existing = effectiveExisting(node)
            val hint = getHint(node)
            var clean = stripHintPrefix(text, hint)
            clean = Constants.sanitizeOutput(clean)
            if (clean.isEmpty()) return false

            val needsSpace = !existing.isNullOrEmpty() &&
                !existing.endsWith(" ") && !existing.endsWith("\n") &&
                clean.isNotEmpty() && !clean.startsWith(" ") && !clean.startsWith("\n") &&
                !clean.startsWith(",") && !clean.startsWith(".") && !clean.startsWith("!") &&
                !clean.startsWith("?") && !clean.startsWith(":") && !clean.startsWith(";")
            val toSet = if (existing.isNullOrEmpty()) clean else existing + (if (needsSpace) " " else "") + clean

            // Attempt 1: Direct ACTION_SET_TEXT (standard Android EditText)
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, toSet.trimStart())
            }
            val setTextOk = try {
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            } catch (_: Exception) { false }

            if (setTextOk) return true

            // Attempt 2: Direct ACTION_PASTE on node (Compose BasicTextField, React Native, custom inputs)
            val cm = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (cm != null) {
                val clip = ClipData.newPlainText("OpenWispr", clean).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        description.extras = android.os.PersistableBundle().apply {
                            putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
                        }
                    }
                }
                cm.setPrimaryClip(clip)
                val pasteOk = try {
                    node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                } catch (_: Exception) { false }
                if (pasteOk) return true
            }

            false
        } catch (_: Exception) { false }
    }

    private fun getHint(node: AccessibilityNodeInfo): String? {
        return try {
            val h = node.hintText?.toString()
            if (!h.isNullOrBlank()) h else null
        } catch (_: Exception) { null }
    }

    private fun effectiveExisting(node: AccessibilityNodeInfo): String? {
        val raw = node.text?.toString()
        if (raw.isNullOrEmpty()) return null
        val hint = getHint(node)
        if (!hint.isNullOrBlank()) {
            val r = raw.trim()
            val h = hint.trim()
            if (r.equals(h, ignoreCase = true)) return null
            val rNorm = r.trimEnd('.', ' ', '\u2026')
            val hNorm = h.trimEnd('.', ' ', '\u2026')
            if (rNorm.equals(hNorm, ignoreCase = true)) return null
        } else {
            val normalized = raw.trim()
            val commonHints = setOf(
                "Message", "Message...", "Type a message", "Type a message...", "Type your message",
                "Search", "Search...", "Ask anything", "Write a message", "Enter message", "Type something"
            )
            if (normalized in commonHints) return null
        }
        return raw
    }

    private fun stripHintPrefix(text: String, hint: String?): String {
        if (hint.isNullOrBlank()) return text
        val h = hint.trim()
        if (h.isEmpty()) return text
        val tTrim = text.trim()
        if (tTrim.length <= h.length) return text
        if (tTrim.startsWith(h, ignoreCase = true)) {
            val after = tTrim.substring(h.length)
            if (after.isEmpty()) return ""
            val firstChar = after[0]
            if (firstChar == ' ' || firstChar == ':' || firstChar == '-' || firstChar == ',' || firstChar == '\n') {
                return after.trimStart(' ', ':', '-', ',', '\n').trimStart()
            }
        }
        return text
    }

    fun copyTranscription(context: Context, text: String): Boolean {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val safe = Constants.sanitizeOutput(text).trim()
            val clip = ClipData.newPlainText("Transcription", safe).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    description.extras = android.os.PersistableBundle().apply {
                        putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
                    }
                }
            }
            cm.setPrimaryClip(clip)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            true
        } catch (_: Exception) { false }
    }

    fun copyToClipboard(context: Context, text: String): Boolean {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val safe = Constants.sanitizeOutput(text).trim()
            val clip = ClipData.newPlainText("Open Wispr", safe).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    description.extras = android.os.PersistableBundle().apply {
                        putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
                    }
                }
            }
            cm.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard - paste in text field", Toast.LENGTH_LONG).show()
            tryPasteFromClipboard(context)
            true
        } catch (_: Exception) { false }
    }

    private fun tryPasteFromClipboard(context: Context) {
        if (context !is AccessibilityService) return
        try {
            val target = findTargetNode(context)
            target?.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        } catch (_: Exception) {}
    }
}