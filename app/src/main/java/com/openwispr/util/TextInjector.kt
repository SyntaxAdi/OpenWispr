package com.openwispr.util

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
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

    fun inject(service: AccessibilityService, text: String): Boolean {
        val focused = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: service.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findEditableFocused(service.rootInActiveWindow)

        // Security check: Refuse injection and suppress clipboard fallback on secure credential fields
        if (isPasswordField(focused)) {
            Toast.makeText(service, "Voice dictation disabled on password fields", Toast.LENGTH_SHORT).show()
            return false
        }

        var safe = sanitize(text)
        val hintFocused = focused?.let { getHint(it) }
        if (!hintFocused.isNullOrBlank()) {
            safe = stripHintPrefix(safe, hintFocused)
            safe = sanitize(safe)
        }
        if (focused != null && tryDirect(focused, safe)) return true
        return copyToClipboard(service, safe)
    }

    private fun tryDirect(node: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            if (!node.isEditable) return false
            if (isPasswordField(node)) return false
            val existing = effectiveExisting(node)
            val hint = getHint(node)
            var clean = stripHintPrefix(text, hint)
            clean = sanitize(clean)
            if (clean.isEmpty()) return false
            val needsSpace = !existing.isNullOrEmpty() &&
                !existing.endsWith(" ") && !existing.endsWith("\n") &&
                clean.isNotEmpty() && !clean.startsWith(" ") && !clean.startsWith("\n") &&
                !clean.startsWith(",") && !clean.startsWith(".") && !clean.startsWith("!") &&
                !clean.startsWith("?") && !clean.startsWith(":") && !clean.startsWith(";")
            val toSet = if (existing.isNullOrEmpty()) clean else existing + (if (needsSpace) " " else "") + clean
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, toSet.trimStart())
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
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
            // trim trailing dots and ellipsis - avoid unicode ellipsis char to keep file ascii-safe
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

    private fun sanitize(t: String): String = Constants.sanitizeOutput(t)

    fun copyToClipboard(context: Context, text: String): Boolean {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            var safe = sanitize(text).trim()
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
            val focused = context.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: context.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            focused?.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        } catch (_: Exception) {}
    }

    private fun findEditableFocused(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isEditable && root.isFocused) return root
        for (i in 0 until root.childCount) {
            val c = root.getChild(i) ?: continue
            val r = findEditableFocused(c)
            if (r != null) return r
        }
        return null
    }
}