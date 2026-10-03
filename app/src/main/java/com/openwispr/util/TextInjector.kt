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
    fun inject(service: AccessibilityService, text: String): Boolean {
        var safe = sanitize(text)
        val focused = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: service.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val hintFocused = focused?.let { getHint(it) }
        if (!hintFocused.isNullOrBlank()) {
            safe = stripHintPrefix(safe, hintFocused)
            safe = sanitize(safe)
        }
        if (focused != null && tryDirect(focused, safe)) return true
        val fallback = findEditableFocused(service.rootInActiveWindow)
        if (fallback != null && tryDirect(fallback, safe)) return true
        return copyToClipboard(service, safe)
    }

    private fun tryDirect(node: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            if (!node.isEditable) return false
            if ((node.inputType and 0x80) != 0) return false
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
            if (Build.VERSION.SDK_INT >= 26) {
                val h = node.hintText?.toString()
                if (!h.isNullOrBlank()) return h
            }
            null
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

    private fun sanitize(t: String): String {
        var s = t.trim()
        val re = Regex("""^(\s*(Message|Cleaned(\s*text)?|Transcription|Output|Result|Refined|Assistant)\s*[:\-]\s*)+""", RegexOption.IGNORE_CASE)
        s = re.replace(s, "")
        s = s.removeSurrounding("\"").removeSurrounding("'").trim()
        return s
    }

    fun copyToClipboard(context: Context, text: String): Boolean {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            var safe = sanitize(text)
            safe = safe.trim()
            cm.setPrimaryClip(ClipData.newPlainText("Open Wispr", safe))
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