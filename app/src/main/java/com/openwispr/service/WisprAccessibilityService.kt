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
import com.openwispr.data.HistoryRepository
import com.openwispr.data.SettingsRepository
import com.openwispr.util.TextInjector

class WisprAccessibilityService : AccessibilityService() {

    private lateinit var settings: SettingsRepository
    private lateinit var bubble: BubbleManager
    private val shakeDetector = com.openwispr.util.ShakeDetector {
        onShakeUnsnooze()
    }

    var lastForegroundPackage: String? = null
        private set

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
        val pkg = event.packageName?.toString()
        if (!HistoryRepository.isIgnoredPackage(pkg)) {
            lastForegroundPackage = pkg
        }
        if (bubble.isRecording()) return // locked while recording: never hide or interrupt

        if (settings.isSnoozed()) {
            if (!shakeDetector.isStarted()) {
                shakeDetector.start(this)
            }
            return
        } else {
            if (shakeDetector.isStarted()) {
                shakeDetector.stop()
            }
        }

        val ime = getImeBounds()
        if (ime != null) {
            val screenHeight = resources.displayMetrics.heightPixels
            val uncovered = ime.top.coerceIn(0, screenHeight)
            bubble.onKeyboardOpened(uncovered)
        } else {
            bubble.onKeyboardClosed()
        }
    }

    fun onBubbleSnoozed() {
        if (!shakeDetector.isStarted()) {
            shakeDetector.start(this)
        }
    }

    fun onShakeUnsnooze() {
        if (!settings.isSnoozed()) return
        settings.unsnooze()
        shakeDetector.stop()

        try {
            val vibrator = androidx.core.content.ContextCompat.getSystemService(this, android.os.Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(120, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}

        bubble.showUnsnoozeToast()

        val ime = getImeBounds()
        if (ime != null) {
            val screenHeight = resources.displayMetrics.heightPixels
            val uncovered = ime.top.coerceIn(0, screenHeight)
            bubble.onKeyboardOpened(uncovered)
        }
    }

    fun getForegroundAppPackage(): String? {
        val targetNodePkg = TextInjector.findTargetNode(this)?.packageName?.toString()
        if (!HistoryRepository.isIgnoredPackage(targetNodePkg)) return targetNodePkg

        val windowPkg = try {
            windows?.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }?.root?.packageName?.toString()
        } catch (_: Exception) { null }
        if (!HistoryRepository.isIgnoredPackage(windowPkg)) return windowPkg

        if (!HistoryRepository.isIgnoredPackage(lastForegroundPackage)) return lastForegroundPackage

        val rootPkg = try { rootInActiveWindow?.packageName?.toString() } catch (_: Exception) { null }
        if (!HistoryRepository.isIgnoredPackage(rootPkg)) return rootPkg

        return null
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        shakeDetector.stop()
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
        val ch = NotificationChannel("wispr_rec", getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = getString(R.string.notif_channel_desc)
        }
        (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
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