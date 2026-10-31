package com.openwispr.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * High-reliability hardware shake detector with dual-spike confirmation
 * and 1.2s cooldown to prevent spurious activations.
 */
class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var isListening = false

    private var lastShakeTime = 0L
    private var shakeCount = 0
    private var lastSpikeTime = 0L

    fun start(context: Context): Boolean {
        if (isListening) return true
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
        sensorManager = sm
        isListening = sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        shakeCount = 0
        lastSpikeTime = 0L
        return isListening
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        sensorManager = null
        isListening = false
        shakeCount = 0
    }

    fun isStarted(): Boolean = isListening

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0] / SensorManager.GRAVITY_EARTH
        val y = event.values[1] / SensorManager.GRAVITY_EARTH
        val z = event.values[2] / SensorManager.GRAVITY_EARTH

        val gForce = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        if (now - lastShakeTime < 1200L) return

        if (gForce > 2.4f) {
            if (now - lastSpikeTime < 450L) {
                shakeCount++
                if (shakeCount >= 2) {
                    lastShakeTime = now
                    shakeCount = 0
                    lastSpikeTime = 0L
                    onShake()
                }
            } else {
                shakeCount = 1
            }
            lastSpikeTime = now
        } else if (now - lastSpikeTime > 750L) {
            shakeCount = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
