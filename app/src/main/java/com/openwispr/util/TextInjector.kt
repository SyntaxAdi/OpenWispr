package com.openwispr.util

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

object TextInjector {
    fun inject(service: AccessibilityService, text: String): Boolean {
        // Try direct insertion at focused node
        val focused = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: service.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && tryDirect(focused, text)) return true
        // Fallback: search any editable focused node via tree
        val fallback = findEditableFocused(service.rootInActiveWindow)
        if (fallback != null && tryDirect(fallback, text)) return true
        // Clipboard fallback
        return copyToClipboard(service, text)
    }

    private fun tryDirect(node: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            if (!node.isEditable) return false
            // Check password
            if ((node.inputType and 0x80) != 0) return false
            val existing = node.text?.toString() ?: ""
            // If we can get selection, insert; otherwise append
            val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, existing + text) }
            val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (ok) return true
            // Try clipboard paste as second attempt on same node
            false
        } catch (_: Exception) { false }
    }

    fun copyToClipboard(context: Context, text: String): Boolean {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Open Wispr", text))
            Toast.makeText(context, "Copied to clipboard — paste in text field", Toast.LENGTH_LONG).show()
            // Try paste via accessibility after copy if node exists (some keyboards accept paste)
            tryPasteFromClipboard(context)
            true
        } catch (_: Exception) { false }
    }

    private fun tryPasteFromClipboard(context: Context) {
        if (context !is AccessibilityService) return
        try {
            val focused = context.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: context.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
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