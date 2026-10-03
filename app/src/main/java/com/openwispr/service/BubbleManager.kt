package com.openwispr.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.openwispr.R
import com.openwispr.audio.AudioRecorder
import com.openwispr.data.GroqRepository
import com.openwispr.data.GroqResult
import com.openwispr.data.SettingsRepository
import com.openwispr.ui.WaveformView
import com.openwispr.util.TextInjector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.math.abs

class BubbleManager(
    private val service: WisprAccessibilityService,
    private val settings: SettingsRepository,
    private val groq: GroqRepository,
    private val recorder: AudioRecorder
) {
    private val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var bubbleView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var collapsedGroup: View? = null
    private var expandedGroup: View? = null
    private var waveform: WaveformView? = null
    private var statusText: TextView? = null
    private var progress: ProgressBar? = null
    private var state: State = State.Idle
    private var amplitudeRunnable: Runnable? = null
    private var timeoutRunnable: Runnable? = null
    private var isAttached = false
    enum class State { Idle, Recording, Processing }
    fun isRecording() = state == State.Recording || state == State.Processing
    fun isShowing() = isAttached
    private fun dp(v: Int): Int = (v * service.resources.displayMetrics.density).toInt()
    fun showCollapsed() {
        if (isAttached && state == State.Idle) return
        if (state != State.Idle) return
        if (isAttached) { showCollapsedInternal(); return }
        createView()
        showCollapsedInternal()
        try { wm.addView(bubbleView, params); isAttached = true } catch (_: Exception) {}
    }
    fun hide() {
        if (state != State.Idle) return
        if (!isAttached) return
        try { wm.removeView(bubbleView) } catch (_: Exception) {}
        isAttached = false
    }
    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        if (isAttached) try { wm.removeView(bubbleView) } catch (_: Exception) {}
        isAttached = false
    }
    private fun createView() {
        val ctx = service
        val sizePx = dp(56)
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            background = ctx.getDrawable(R.drawable.bg_collapsed)
            clipToOutline = true
            elevation = 10f
        }
        val collapsed = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(8,8,8,8)
        }
        val mic = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_mic)
            layoutParams = LinearLayout.LayoutParams(sizePx - 18, sizePx - 18)
        }
        collapsed.addView(mic)
        val expanded = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            visibility = View.GONE
            setPadding(6,6,6,6)
        }
        val close = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_close)
            setPadding(10,10,10,10)
            layoutParams = LinearLayout.LayoutParams(dp(44),dp(44))
            setOnClickListener { cancelRecording() }
        }
        val wave = WaveformView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(36)).apply { weight = 1f; setMargins(6,0,6,0) }
        }
        val tick = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_tick)
            setPadding(10,10,10,10)
            layoutParams = LinearLayout.LayoutParams(dp(44),dp(44))
            setOnClickListener { finishRecording() }
        }
        val status = TextView(ctx).apply {
            text = ""; textSize = 11f; setTextColor(0xCCFFFFFF.toInt()); visibility = View.GONE
            setPadding(8,0,8,0)
        }
        val prog = ProgressBar(ctx).apply { visibility = View.GONE; isIndeterminate = true }
        expanded.addView(close)
        expanded.addView(wave)
        expanded.addView(tick)
        container.addView(collapsed)
        container.addView(expanded)
        val outer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            addView(container)
            addView(status)
            addView(prog)
        }
        collapsedGroup = collapsed
        expandedGroup = expanded
        waveform = wave
        statusText = status
        progress = prog
        collapsed.setOnClickListener { startRecording() }
        mic.setOnClickListener { startRecording() }
        container.setOnClickListener { if (state == State.Idle) startRecording() }
        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f; var moved = false
        val touchListener = View.OnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { initialX = params!!.x; initialY = params!!.y; initialTouchX = ev.rawX; initialTouchY = ev.rawY; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - initialTouchX).toInt()
                    val dy = (ev.rawY - initialTouchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    if (moved) { params!!.x = initialX + dx; params!!.y = initialY + dy; try { wm.updateViewLayout(bubbleView, params) } catch (_: Exception) {} }
                    true
                }
                MotionEvent.ACTION_UP -> true
                else -> false
            }
        }
        outer.setOnTouchListener(touchListener)
        bubbleView = outer
        params = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.END; x = 24; y = 220 }
    }
    private fun showCollapsedInternal() {
        collapsedGroup?.visibility = View.VISIBLE
        expandedGroup?.visibility = View.GONE
        statusText?.visibility = View.GONE
        progress?.visibility = View.GONE
        bubbleView?.background = service.getDrawable(R.drawable.bg_collapsed)
        scope.launch {
            val dpVal = settings.bubbleDpFlow.first()
            handler.post {
                params?.let { p -> p.width = dp(dpVal); p.height = dp(dpVal); if (isAttached) try { wm.updateViewLayout(bubbleView, p) } catch (_: Exception) {} }
            }
        }
        (bubbleView as? ViewGroup)?.getChildAt(0)?.background = service.getDrawable(R.drawable.bg_collapsed)
    }
    private fun expand() {
        val p = params ?: return
        val targetW = dp(280)
        val startW = p.width
        val startH = p.height
        scope.launch {
            val actualH = dp(settings.bubbleDpFlow.first())
            handler.post {
                val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 220; interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        val f = it.animatedValue as Float
                        p.width = (startW + (targetW - startW) * f).toInt()
                        p.height = (startH + (actualH - startH) * f).toInt()
                        try { wm.updateViewLayout(bubbleView, p) } catch (_: Exception) {}
                    }
                }
                anim.start()
            }
        }
        collapsedGroup?.visibility = View.GONE
        expandedGroup?.visibility = View.VISIBLE
        (bubbleView as? ViewGroup)?.getChildAt(0)?.background = service.getDrawable(R.drawable.bg_expanded)
    }
    private fun startRecording() {
        if (state != State.Idle) return
        val file = recorder.start()
        if (file == null) { Toast.makeText(service, "Mic permission needed / failed to start", Toast.LENGTH_SHORT).show(); return }
        state = State.Recording
        expand()
        if (!isAttached) { try { wm.addView(bubbleView, params); isAttached = true } catch (_: Exception) {} }
        startAmplitudeLoop()
        scope.launch {
            val maxSec = settings.maxSecFlow.first()
            timeoutRunnable = Runnable { if (state == State.Recording) finishRecording() }
            handler.postDelayed(timeoutRunnable!!, maxSec * 1000L)
        }
        service.startListeningNotification()
    }
    private fun startAmplitudeLoop() {
        amplitudeRunnable = object : Runnable {
            override fun run() {
                if (state != State.Recording) return
                val amp = recorder.getMaxAmplitude()
                val norm = (amp / 32767f).coerceIn(0f, 1f)
                waveform?.setLevel(norm)
                handler.postDelayed(this, 70)
            }
        }
        handler.post(amplitudeRunnable!!)
    }
    private fun cancelRecording() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        amplitudeRunnable?.let { handler.removeCallbacks(it) }
        recorder.cancelAndDelete()
        waveform?.setIdle()
        state = State.Idle
        service.stopListeningNotification()
        showCollapsedInternal()
        service.requestHideCheck()
    }
    private fun finishRecording() {
        if (state != State.Recording) return
        state = State.Processing
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        amplitudeRunnable?.let { handler.removeCallbacks(it) }
        val file = recorder.stop()
        waveform?.setIdle()
        statusText?.apply { text = "Transcribing..."; visibility = View.VISIBLE }
        progress?.visibility = View.VISIBLE
        if (file == null || !file.exists() || file.length() < 800) {
            statusText?.text = "No audio"
            handler.postDelayed({ state = State.Idle; service.stopListeningNotification(); showCollapsedInternal(); service.requestHideCheck() }, 1200)
            file?.delete(); return
        }
        scope.launch {
            val apiKey = settings.apiKeyFlow.first()
            val baseUrl = settings.baseUrlFlow.first()
            val whisper = settings.whisperFlow.first()
            val llama = settings.llamaFlow.first()
            if (apiKey.isBlank()) {
                withContext(Dispatchers.Main) {
                    statusText?.text = "Set API key in Settings"
                    progress?.visibility = View.GONE
                    handler.postDelayed({ state = State.Idle; service.stopListeningNotification(); showCollapsedInternal() }, 1600)
                }
                file.delete(); return@launch
            }
            when (val tr = groq.transcribe(baseUrl, apiKey, whisper, file)) {
                is GroqResult.Error -> withContext(Dispatchers.Main) {
                    statusText?.text = tr.message.take(60)
                    progress?.visibility = View.GONE
                    handler.postDelayed({ state = State.Idle; service.stopListeningNotification(); showCollapsedInternal(); service.requestHideCheck() }, 1800)
                }
                is GroqResult.Success -> {
                    val raw = tr.data
                    when (val rf = groq.refine(baseUrl, apiKey, llama, raw)) {
                        is GroqResult.Error -> withContext(Dispatchers.Main) {
                            val safeRaw = com.openwispr.util.Constants.sanitizeOutput(raw)
                            val injected = TextInjector.inject(service, safeRaw)
                            statusText?.text = if (injected) "Pasted ✓" else "Copied"
                            progress?.visibility = View.GONE
                            handler.postDelayed({ state = State.Idle; service.stopListeningNotification(); showCollapsedInternal(); service.requestHideCheck() }, 1000)
                        }
                        is GroqResult.Success -> withContext(Dispatchers.Main) {
                            val clean = rf.data
                            val injected = TextInjector.inject(service, clean)
                            statusText?.text = if (injected) "Pasted ✓" else "Copied"
                            progress?.visibility = View.GONE
                            handler.postDelayed({ state = State.Idle; service.stopListeningNotification(); showCollapsedInternal(); service.requestHideCheck() }, 900)
                        }
                    }
                }
            }
            file.delete()
        }
    }
    fun onBubbleSizeChanged() {
        if (!isAttached) return
        scope.launch {
            val dpVal = settings.bubbleDpFlow.first()
            handler.post { params?.width = dp(dpVal); params?.height = dp(dpVal); if (state == State.Idle) try { wm.updateViewLayout(bubbleView, params) } catch (_: Exception) {} }
        }
    }
}
