package com.openwispr.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Nothing OS audio visualizer matching hardware recorder aesthetic:
 * 1. Solid Nothing Red circular recording indicator dot on the far left.
 * 2. Continuous dark grey ambient waveform across the entire track.
 * 3. Dynamic pure white voice waveform bars that surge directly over the grey waveform
 *    in real time matching live vocal cadence.
 */
class WaveformView @JvmOverloads constructor(c: Context, attrs: AttributeSet? = null) : View(c, attrs) {
    private val density = resources.displayMetrics.density

    // Nothing OS Palette
    private val paintRed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF2E2E.toInt()
        style = Paint.Style.FILL
    }
    private val paintGrey = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF464952.toInt()
        strokeCap = Paint.Cap.ROUND
    }
    private val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        strokeCap = Paint.Cap.ROUND
    }

    private var targetLevel = 0f
    private var level = 0f
    private var phase = 0f
    private val bars = 18
    private var animating = false

    fun setLevel(amplitude01: Float) {
        targetLevel = amplitude01.coerceIn(0f, 1f)
        animating = true
        postInvalidateOnAnimation()
    }

    fun setIdle() {
        targetLevel = 0f
        level = 0f
        animating = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Smooth frame interpolation for vocal responsiveness
        level += (targetLevel - level) * 0.30f
        phase += 0.08f + (level * 0.16f)

        val centerY = h / 2f
        val barWidth = 2.0f * density
        paintGrey.strokeWidth = barWidth
        paintWhite.strokeWidth = barWidth

        // 1. Solid Nothing Red Recording Indicator Dot
        val ledRadius = 2.8f * density
        val ledX = ledRadius + 1.5f * density
        canvas.drawCircle(ledX, centerY, ledRadius, paintRed)

        // 2. Bar Geometry & Spacing
        val barsStartX = ledX + ledRadius + 7.5f * density
        val availableW = (w - barsStartX - 1.5f * density).coerceAtLeast(0f)
        val gap = ((availableW - (bars * barWidth)) / (bars - 1)).coerceAtLeast(1.1f * density)

        val isSpeaking = level > 0.06f

        // Pass 1: Draw continuous dark grey baseline waveform across all 18 bars
        for (i in 0 until bars) {
            val x = barsStartX + i * (barWidth + gap) + barWidth / 2f

            // Dual-peak wave contour matching Nothing OS reference audio track:
            // Peak 1 around i = 6.5, Peak 2 around i = 12.5, subtle dots at extremes
            val p1 = exp(-((i - 6.5f) * (i - 6.5f)) / 10f).toFloat()
            val p2 = exp(-((i - 12.5f) * (i - 12.5f)) / 12f).toFloat() * 0.65f
            val ambient = sin(phase + i * 0.45f).toFloat() * 0.05f
            val greyNorm = (0.11f + (0.34f * (p1 * 0.5f + p2)) + ambient).coerceIn(0.08f, 0.52f)

            val greyH = greyNorm * (h * 0.74f)
            val topGrey = centerY - greyH / 2f
            val bottomGrey = centerY + greyH / 2f
            canvas.drawLine(x, topGrey, x, bottomGrey, paintGrey)

            // Pass 2: Draw brilliant white voice waveform directly OVER the grey waveform
            if (isSpeaking) {
                val distFromPeak = abs(i - 6.8f)
                val voiceBell = (1.0f - (distFromPeak / 3.0f)).coerceAtLeast(0f)
                if (voiceBell > 0.05f) {
                    val vocalFluctuation = sin(phase * 2.2f + i * 1.5f).toFloat() * 0.14f
                    val voiceNorm = (level * voiceBell * (0.92f + vocalFluctuation)).coerceIn(0f, 1f)
                    if (voiceNorm > 0.20f) {
                        val whiteH = (voiceNorm * (h * 0.88f)).coerceAtLeast(greyH + 2.5f * density)
                        val topWhite = centerY - whiteH / 2f
                        val bottomWhite = centerY + whiteH / 2f
                        canvas.drawLine(x, topWhite, x, bottomWhite, paintWhite)
                    }
                }
            }
        }

        // Hardware animation loop: continue 60fps/120fps render while active
        if (animating || level > 0.01f || targetLevel > 0.01f) {
            postInvalidateOnAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animating = false
    }
}