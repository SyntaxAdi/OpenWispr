package com.openwispr.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.random.Random

class WaveformView @JvmOverloads constructor(c: Context, attrs: AttributeSet? = null) : View(c, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); strokeCap = Paint.Cap.ROUND; strokeWidth = 5f }
    private val paintDim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFFFFF; strokeCap = Paint.Cap.ROUND; strokeWidth = 5f }
    private var level = 0f // 0..1
    private var phase = 0f
    private val bars = 28
    private var animating = false
    private val idleHeights = FloatArray(bars) { 0.15f + Random.nextFloat() * 0.15f }

    fun setLevel(amplitude01: Float) {
        // smooth lerp
        level = level * 0.7f + amplitude01.coerceIn(0f, 1f) * 0.3f
        phase += 0.22f
        invalidate()
    }

    fun setIdle() { level = 0f; animating = false; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f || h == 0f) return
        val barW = (w / bars * 0.55f)
        val gap = w / bars
        val centerY = h / 2f
        for (i in 0 until bars) {
            val x = gap * i + gap * 0.5f
            // Voice-reactive: base idle + wave + level
            val posFactor = kotlin.math.sin(phase + i * 0.55f) * 0.15f
            val voiceFactor = level * (0.9f + kotlin.math.sin(i * 1.2f) * 0.25f)
            val norm = (idleHeights[i] + voiceFactor + posFactor).coerceIn(0.08f, 1f)
            val barH = norm * h * 0.78f
            val top = centerY - barH / 2f
            val bottom = centerY + barH / 2f
            val p = if (norm > 0.5f) paint else paintDim
            canvas.drawLine(x, top, x, bottom, p)
        }
        // keep animating while visible
        if (level > 0.01f || animating) {
            postInvalidateDelayed(55)
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); animating = true }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); animating = false }
}