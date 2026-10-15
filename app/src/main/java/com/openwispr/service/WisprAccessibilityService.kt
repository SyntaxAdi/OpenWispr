package com.openwispr.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
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
        val recorder = AudioRecorder(applicationContext)
        recorder.cleanOrphanRecordings()
        settings = SettingsRepository(applicationContext)
        bubble = BubbleManager(this, settings, GroqRepository(), recorder)
        createNotifChannel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (bubble.isRecording()) return // locked while recording: never hide or interrupt

        val ime = getImeBounds()
        if (ime != null) {
            val screenHeight = resources.displayMetrics.heightPixels
            val uncovered = ime.top.coerceIn(0, screenHeight)
            bubble.onKeyboardOpened(uncovered)
        } else {
            bubble.onKeyboardClosed()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        bubble.destroy()
        super.onDestroy()
    }

    fun requestHideCheck() {
        if (bubble.isRecording()) return
        if (getImeBounds() == null) {
            bubble.hide()
        }
    }

    private fun getImeBounds(): Rect? {
        val windowList = try { windows } catch (_: Exception) { null } ?: return null
        val screenHeight = resources.displayMetrics.heightPixels
        val r = Rect()
        for (w in windowList) {
            if (w.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                w.getBoundsInScreen(r)
                if (r.height() > 100 && r.width() > 0 && r.top < screenHeight) {
                    return r
                }
            }
        }
        return null
    }

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
            if (Build.VERSION.SDK_INT >= 29) {
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