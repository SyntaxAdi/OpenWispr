package com.openwispr.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.openwispr.R
import com.openwispr.audio.AudioRecorder
import com.openwispr.data.GroqRepository
import com.openwispr.data.SettingsRepository

class WisprAccessibilityService : AccessibilityService() {

    private lateinit var settings: SettingsRepository
    private lateinit var bubble: BubbleManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        settings = SettingsRepository(applicationContext)
        bubble = BubbleManager(this, settings, GroqRepository(), AudioRecorder(applicationContext))
        createNotifChannel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (bubble.isRecording()) return // locked while recording
        val type = event.eventType
        if (type == AccessibilityEvent.TYPE_VIEW_FOCUSED
            || type == AccessibilityEvent.TYPE_VIEW_CLICKED
            || type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            val node = event.source ?: rootInActiveWindow
            if (isEditableFocused(node)) {
                bubble.showCollapsed()
            } else {
                // double-check with tree search, debounce small delay
                val focused = findEditableFocused(rootInActiveWindow)
                if (focused != null) bubble.showCollapsed() else bubble.hide()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        bubble.destroy()
        super.onDestroy()
    }

    fun requestHideCheck() {
        if (bubble.isRecording()) return
        val focused = findEditableFocused(rootInActiveWindow)
        if (focused == null) bubble.hide() else bubble.showCollapsed()
    }

    private fun isEditableFocused(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val cls = node.className?.toString() ?: ""
        val isEdit = cls.contains("EditText") || node.isEditable
        if (!isEdit) return false
        if (node.isPassword) return false
        return node.isFocused || node.isEditable && node.isFocused
    }

    private fun findEditableFocused(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isEditable && root.isFocused && !root.isPassword) return root
        // also check EditText class
        val cls = root.className?.toString() ?: ""
        if (cls.contains("EditText") && root.isFocused && !root.isPassword) return root
        for (i in 0 until root.childCount) {
            val c = root.getChild(i) ?: continue
            val r = findEditableFocused(c)
            if (r != null) return r
        }
        return null
    }

    // Notification helpers for foreground listening
    private fun createNotifChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel("wispr_rec", getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.notif_channel_desc)
            }
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
        }
    }

    fun startListeningNotification() {
        val notif = NotificationCompat.Builder(this, "wispr_rec")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("Open Wispr")
            .setContentText(getString(R.string.notif_listening))
            .setOngoing(true)
            .setSilent(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else if (Build.VERSION.SDK_INT >= 29) {
                startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(1, notif)
            }
        } catch (_: Exception) {}
    }

    fun stopListeningNotification() {
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Exception) {}
    }
}