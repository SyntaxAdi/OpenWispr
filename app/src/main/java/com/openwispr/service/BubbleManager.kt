package com.openwispr.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.openwispr.R
import com.openwispr.audio.AudioRecorder
import com.openwispr.data.FormalityMode
import com.openwispr.data.GroqRepository
import com.openwispr.data.GroqResult
import com.openwispr.data.HistoryRepository
import com.openwispr.data.SettingsRepository
import com.openwispr.ui.WaveformView
import com.openwispr.util.Constants
import com.openwispr.util.TextInjector
import kotlinx.coroutines.*
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
    private val historyRepo = HistoryRepository(service)
    private var recordingStartTimeMs = 0L
    private var bubbleView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var collapsedGroup: View? = null
    private var expandedGroup: View? = null
    private var waveform: WaveformView? = null
    private var statusText: TextView? = null
    private var tickIcon: ImageView? = null
    private var tickLoader: ProgressBar? = null
    private var tickWrap: FrameLayout? = null
    private var closeIcon: ImageView? = null
    private var actionGroup: View? = null
    private var actionDismissRunnable: Runnable? = null
    private var snapAnimator: ValueAnimator? = null
    private var state: State = State.Idle
    private var amplitudeRunnable: Runnable? = null
    private var timeoutRunnable: Runnable? = null
    private var isAttached = false
    private var isKeyboardOpen = false
    private var isLayoutUpdatePending = false
    private var initialAppPackage: String = "unknown"

    enum class State { Idle, Recording, Processing }

    fun isRecording() = state == State.Recording || state == State.Processing

    private fun dp(v: Int): Int = (v * service.resources.displayMetrics.density).toInt()

    fun onKeyboardOpened(uncoveredHeight: Int) {
        if (!isKeyboardOpen || !isAttached) {
            isKeyboardOpen = true
            showAtKeyboardCenter(uncoveredHeight)
        }
    }

    fun onKeyboardClosed() {
        isKeyboardOpen = false
        if (isRecording()) {
            // Keep recording uninterrupted even if keyboard minimizes
            return
        }
        hide()
    }

    private fun showAtKeyboardCenter(uncoveredHeight: Int) {
        if (state != State.Idle) return
        val dm = service.resources.displayMetrics
        val sizePx = dp(settings.bubbleDp)
        val centerY = (uncoveredHeight / 2) - (sizePx / 2)
        val targetY = centerY.coerceIn(dp(24), (uncoveredHeight - sizePx - dp(8)).coerceAtLeast(dp(24)))
        val targetX = (dm.widthPixels - sizePx - dp(8)).coerceAtLeast(0)

        if (bubbleView == null) {
            createView()
        }

        params?.let { p ->
            p.x = targetX
            p.y = targetY
            p.width = sizePx
            p.height = sizePx
        }

        showCollapsedInternal()

        if (!isAttached) {
            try {
                wm.addView(bubbleView, params)
                isAttached = true
            } catch (_: Exception) {}
        } else {
            try {
                wm.updateViewLayout(bubbleView, params)
            } catch (_: Exception) {}
        }
    }

    fun hide() {
        if (state != State.Idle) return
        if (!isAttached) return
        snapAnimator?.cancel()
        snapAnimator = null
        try { wm.removeView(bubbleView) } catch (_: Exception) {}
        isAttached = false
    }

    fun destroy() {
        actionDismissRunnable?.let { handler.removeCallbacks(it) }
        actionDismissRunnable = null
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        snapAnimator?.cancel()
        snapAnimator = null
        if (isAttached) try { wm.removeView(bubbleView) } catch (_: Exception) {}
        isAttached = false
    }

    private fun createView() {
        val ctx = service
        val sizePx = dp(settings.bubbleDp)
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = null
            clipToOutline = false
            elevation = 8f
            alpha = (settings.bubbleOpacity.coerceIn(com.openwispr.util.Constants.MIN_BUBBLE_OPACITY, com.openwispr.util.Constants.MAX_BUBBLE_OPACITY) / 100f)
        }
        val mic = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_bubble)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
            clipToOutline = true
            elevation = 0f
        }
        val expanded = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val close = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_close)
            background = ctx.getDrawable(R.drawable.bg_bubble_button)
            setPadding(dp(7), dp(7), dp(7), dp(7))
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            setOnClickListener { cancelRecording() }
        }
        val wave = WaveformView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(70), dp(26)).apply { setMargins(dp(4), 0, dp(4), 0) }
        }
        val tickHolder = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            background = ctx.getDrawable(R.drawable.bg_bubble_button)
        }
        val tick = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_tick)
            setPadding(dp(7), dp(7), dp(7), dp(7))
            layoutParams = FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER)
            isClickable = true
            isFocusable = true
        }
        val tickSpin = ProgressBar(ctx, null, android.R.attr.progressBarStyleSmall).apply {
            isIndeterminate = true
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER)
            indeterminateTintList = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
            indeterminateTintMode = android.graphics.PorterDuff.Mode.SRC_IN
            isDuplicateParentStateEnabled = false
        }
        tickHolder.addView(tick)
        tickHolder.addView(tickSpin)
        tickHolder.isClickable = true
        tickHolder.isFocusable = true
        val tickClick = View.OnClickListener {
            if (state != State.Recording) return@OnClickListener
            tick.visibility = View.GONE
            tickSpin.visibility = View.VISIBLE
            tickSpin.bringToFront()
            tickSpin.invalidate()
            tickHolder.isEnabled = false
            tickHolder.isClickable = false
            tickHolder.alpha = 0.9f
            closeIcon?.let { it.isEnabled = false; it.alpha = 0.35f; it.isClickable = false }
            tickHolder.invalidate()
            try { bubbleView?.let { v -> wm.updateViewLayout(v, params) } } catch (_: Exception) {}
            finishRecording()
        }
        tick.setOnClickListener(tickClick)
        tickHolder.setOnClickListener(tickClick)
        val status = TextView(ctx).apply {
            text = ""
            textSize = 10f
            setTextColor(0xFF8E95A2.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(8), dp(4), dp(8), 0)
        }
        expanded.addView(close)
        expanded.addView(wave)
        expanded.addView(tickHolder)

        val action = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(12), dp(4), dp(12), dp(4))
        }
        val actionDot = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(7), dp(7)).apply {
                setMargins(0, 0, dp(6), 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFFF2E2E.toInt())
            }
        }
        val actionText = TextView(ctx).apply {
            text = "KEY NOT SET • TAP TO FIX"
            textSize = 10.5f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }
        action.addView(actionDot)
        action.addView(actionText)
        action.setOnClickListener {
            try {
                val intent = android.content.Intent(service, com.openwispr.MainActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                service.startActivity(intent)
            } catch (_: Exception) {}
            showCollapsedInternal()
        }

        container.addView(mic)
        container.addView(expanded)
        container.addView(action)
        val outer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(container)
            addView(status)
        }
        collapsedGroup = mic
        expandedGroup = expanded
        actionGroup = action
        waveform = wave
        tickIcon = tick
        tickLoader = tickSpin
        tickWrap = tickHolder
        closeIcon = close
        statusText = status

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false
        var downTime = 0L
        val touchSlop = ViewConfiguration.get(ctx).scaledTouchSlop

        val touchListener = View.OnTouchListener { _, ev ->
            if (state != State.Idle) return@OnTouchListener false
            val p = params ?: return@OnTouchListener false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnimator?.cancel()
                    snapAnimator = null
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = ev.rawX
                    initialTouchY = ev.rawY
                    moved = false
                    downTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - initialTouchX).toInt()
                    val dy = (ev.rawY - initialTouchY).toInt()
                    if (!moved && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        moved = true
                    }
                    if (moved) {
                        val dm = service.resources.displayMetrics
                        val w = p.width
                        val h = p.height
                        val nx = (initialX + dx).coerceIn(0, (dm.widthPixels - w).coerceAtLeast(0))
                        val ny = (initialY + dy).coerceIn(dp(16), (dm.heightPixels - h - dp(16)).coerceAtLeast(0))
                        p.x = nx
                        p.y = ny
                        if (!isLayoutUpdatePending) {
                            isLayoutUpdatePending = true
                            bubbleView?.postOnAnimation {
                                isLayoutUpdatePending = false
                                if (isAttached) {
                                    try { wm.updateViewLayout(bubbleView, p) } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        val dm = service.resources.displayMetrics
                        val margin = dp(8)
                        val mid = dm.widthPixels / 2
                        val targetX = if (p.x + p.width / 2 < mid) margin else dm.widthPixels - p.width - margin
                        val startX = p.x
                        if (startX != targetX) {
                            snapAnimator = ValueAnimator.ofInt(startX, targetX).apply {
                                duration = 180
                                interpolator = DecelerateInterpolator()
                                addUpdateListener { a ->
                                    p.x = a.animatedValue as Int
                                    try { wm.updateViewLayout(bubbleView, p) } catch (_: Exception) {}
                                }
                            }
                            snapAnimator?.start()
                        }
                    } else {
                        if (System.currentTimeMillis() - downTime < 600) {
                            startRecording()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    moved = false
                    isLayoutUpdatePending = false
                    true
                }
                else -> false
            }
        }

        outer.setOnTouchListener(touchListener)
        bubbleView = outer

        val dm = service.resources.displayMetrics
        val defaultX = (dm.widthPixels - sizePx - dp(8)).coerceAtLeast(0)
        val defaultY = (dm.heightPixels / 2 - sizePx / 2).coerceAtLeast(dp(24))

        params = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = defaultX
            y = defaultY
        }
    }

    private fun applyDynamicSizes(bubbleDp: Int) {
        val h = dp(bubbleDp)
        val btnSize = (h * 0.72f).toInt().coerceAtLeast(dp(28))
        val btnPadding = (btnSize * 0.20f).toInt().coerceAtLeast(dp(4))
        val padH = (h * 0.12f).toInt().coerceAtLeast(dp(4))
        val padV = (h * 0.10f).toInt().coerceAtLeast(dp(3))
        val waveMargin = (h * 0.08f).toInt().coerceAtLeast(dp(3))
        val waveW = (h * 1.40f).toInt().coerceAtLeast(dp(54))
        val waveH = (h * 0.54f).toInt().coerceAtLeast(dp(20))
        val spinSize = (btnSize * 0.50f).toInt().coerceAtLeast(dp(14))

        expandedGroup?.setPadding(padH, padV, padH, padV)

        closeIcon?.apply {
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
            setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
        }

        waveform?.apply {
            layoutParams = LinearLayout.LayoutParams(waveW, waveH).apply {
                setMargins(waveMargin, 0, waveMargin, 0)
            }
        }

        tickWrap?.layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
        tickIcon?.apply {
            layoutParams = FrameLayout.LayoutParams(btnSize, btnSize, Gravity.CENTER)
            setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
        }
        tickLoader?.layoutParams = FrameLayout.LayoutParams(spinSize, spinSize, Gravity.CENTER)
    }

    private fun showCollapsedInternal() {
        actionDismissRunnable?.let { handler.removeCallbacks(it) }
        collapsedGroup?.visibility = View.VISIBLE
        expandedGroup?.visibility = View.GONE
        actionGroup?.visibility = View.GONE
        statusText?.visibility = View.GONE
        bubbleView?.background = null
        tickIcon?.apply { visibility = View.VISIBLE; alpha = 1f; isEnabled = true; isClickable = true }
        tickLoader?.apply { visibility = View.GONE; clearAnimation() }
        tickWrap?.apply { isEnabled = true; alpha = 1f; isClickable = true }
        closeIcon?.apply { isEnabled = true; alpha = 1f; isClickable = true }
        val bubbleDp = settings.bubbleDp.coerceIn(Constants.MIN_BUBBLE_DP, Constants.MAX_BUBBLE_DP)
        val sizePx = dp(bubbleDp)
        applyDynamicSizes(bubbleDp)
        collapsedGroup?.layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
        val dm = service.resources.displayMetrics
        params?.let { p ->
            p.width = sizePx
            p.height = sizePx
            p.x = p.x.coerceIn(0, (dm.widthPixels - sizePx).coerceAtLeast(0))
            if (isAttached) try { wm.updateViewLayout(bubbleView, p) } catch (_: Exception) {}
        }
        (bubbleView as? ViewGroup)?.getChildAt(0)?.background = null
        bubbleView?.alpha = (settings.bubbleOpacity.coerceIn(Constants.MIN_BUBBLE_OPACITY, Constants.MAX_BUBBLE_OPACITY) / 100f)
    }

    private fun expand() {
        actionDismissRunnable?.let { handler.removeCallbacks(it) }
        val p = params ?: return
        val bubbleDp = settings.bubbleDp.coerceIn(Constants.MIN_BUBBLE_DP, Constants.MAX_BUBBLE_DP)
        applyDynamicSizes(bubbleDp)

        val targetH = dp(bubbleDp)
        val padH = (targetH * 0.12f).toInt().coerceAtLeast(dp(4))
        val btnSize = (targetH * 0.72f).toInt().coerceAtLeast(dp(28))
        val waveMargin = (targetH * 0.08f).toInt().coerceAtLeast(dp(3))
        val waveW = (targetH * 1.40f).toInt().coerceAtLeast(dp(54))
        val targetW = padH * 2 + btnSize * 2 + waveMargin * 2 + waveW

        val startW = p.width
        val startH = p.height
        val startX = p.x
        val dm = service.resources.displayMetrics
        val targetX = if (startX + targetW > dm.widthPixels - dp(8)) {
            (dm.widthPixels - targetW - dp(8)).coerceAtLeast(0)
        } else startX

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 240
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val f = it.animatedValue as Float
                p.width = (startW + (targetW - startW) * f).toInt()
                p.height = (startH + (targetH - startH) * f).toInt()
                p.x = (startX + (targetX - startX) * f).toInt()
                try { wm.updateViewLayout(bubbleView, p) } catch (_: Exception) {}
            }
        }
        anim.start()
        collapsedGroup?.visibility = View.GONE
        expandedGroup?.visibility = View.VISIBLE
        actionGroup?.visibility = View.GONE
        val bg = (service.getDrawable(R.drawable.bg_expanded)?.mutate() as? GradientDrawable)
        bg?.cornerRadius = targetH / 2f
        (bubbleView as? ViewGroup)?.getChildAt(0)?.background = bg
    }

    private fun showKeyMissingActionBubble() {
        val p = params ?: return
        actionDismissRunnable?.let { handler.removeCallbacks(it) }
        try {
            bubbleView?.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
        Toast.makeText(service, "OpenWispr: Groq API key required. Tap bubble to set.", Toast.LENGTH_SHORT).show()

        val bubbleDp = settings.bubbleDp.coerceIn(Constants.MIN_BUBBLE_DP, Constants.MAX_BUBBLE_DP)
        val targetH = dp(bubbleDp)
        val targetW = dp(190).coerceAtMost((service.resources.displayMetrics.widthPixels * 0.85f).toInt())

        val startW = p.width
        val startH = p.height
        val startX = p.x
        val dm = service.resources.displayMetrics
        val targetX = if (startX + targetW > dm.widthPixels - dp(8)) {
            (dm.widthPixels - targetW - dp(8)).coerceAtLeast(0)
        } else startX

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val f = it.animatedValue as Float
                p.width = (startW + (targetW - startW) * f).toInt()
                p.height = (startH + (targetH - startH) * f).toInt()
                p.x = (startX + (targetX - startX) * f).toInt()
                try { wm.updateViewLayout(bubbleView, p) } catch (_: Exception) {}
            }
        }
        anim.start()

        collapsedGroup?.visibility = View.GONE
        expandedGroup?.visibility = View.GONE
        actionGroup?.visibility = View.VISIBLE
        val bg = (service.getDrawable(R.drawable.bg_expanded)?.mutate() as? GradientDrawable)
        bg?.cornerRadius = targetH / 2f
        bg?.setStroke(dp(1), 0x99FF2E2E.toInt())
        (bubbleView as? ViewGroup)?.getChildAt(0)?.background = bg

        actionDismissRunnable = Runnable {
            if (state == State.Idle) {
                showCollapsedInternal()
            }
        }
        handler.postDelayed(actionDismissRunnable!!, 2200L)
    }

    private fun startRecording() {
        if (state != State.Idle) return
        if (settings.apiKey.isBlank()) {
            showKeyMissingActionBubble()
            return
        }
        val file = recorder.start()
        if (file == null) { Toast.makeText(service, "Mic permission needed / failed to start", Toast.LENGTH_SHORT).show(); return }
        recordingStartTimeMs = System.currentTimeMillis()
        initialAppPackage = service.rootInActiveWindow?.packageName?.toString() ?: "unknown"
        state = State.Recording
        expand()
        if (!isAttached) { try { wm.addView(bubbleView, params); isAttached = true } catch (_: Exception) {} }
        startAmplitudeLoop()
        timeoutRunnable = Runnable { if (state == State.Recording) finishRecording() }
        handler.postDelayed(timeoutRunnable!!, settings.maxSec * 1000L)
        service.startListeningNotification()
    }

    private fun startAmplitudeLoop() {
        amplitudeRunnable = object : Runnable {
            override fun run() {
                if (state != State.Recording) return
                val amp = recorder.getMaxAmplitude()
                val cleanAmp = (amp - 200).coerceAtLeast(0)
                val linear = (cleanAmp / 7500f).coerceIn(0f, 1f)
                val perceptualLevel = kotlin.math.sqrt(linear.toDouble()).toFloat()
                waveform?.setLevel(perceptualLevel)
                handler.postDelayed(this, 50)
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
        hide()
    }

    private fun finishRecording() {
        if (state != State.Recording) return
        state = State.Processing
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        amplitudeRunnable?.let { handler.removeCallbacks(it) }
        tickIcon?.visibility = View.GONE
        tickLoader?.apply {
            visibility = View.VISIBLE
            bringToFront()
            indeterminateDrawable?.setVisible(true, true)
            isIndeterminate = true
            invalidate()
        }
        tickWrap?.apply { isEnabled = false; alpha = 0.9f; isClickable = false; invalidate() }
        closeIcon?.apply { isEnabled = false; alpha = 0.35f; isClickable = false }
        try { bubbleView?.let { v -> params?.let { p -> wm.updateViewLayout(v, p) } } } catch (_: Exception) {}
        val file = recorder.stop()
        waveform?.setIdle()
        statusText?.apply { text = "Transcribing..."; visibility = View.VISIBLE }
        if (file == null || !file.exists() || file.length() < 800) {
            android.util.Log.w("OpenWispr:Bubble", "Audio recording empty or too small (${file?.length() ?: 0} bytes)")
            Toast.makeText(service, "OpenWispr: No speech recorded", Toast.LENGTH_SHORT).show()
            statusText?.text = "No audio"
            tickIcon?.apply { visibility = View.VISIBLE; alpha = 1f }
            tickLoader?.visibility = View.GONE
            closeIcon?.apply { isEnabled = true; alpha = 1f; isClickable = true }
            handler.postDelayed({
                state = State.Idle
                service.stopListeningNotification()
                hide()
            }, 1000)
            file?.delete()
            return
        }
        val durationMs = (System.currentTimeMillis() - recordingStartTimeMs).coerceAtLeast(500L)
        val targetPkg = if (initialAppPackage != "unknown") initialAppPackage else (service.rootInActiveWindow?.packageName?.toString() ?: "unknown")
        val effectivePrompt = settings.getEffectiveSystemPrompt(targetPkg)
        val effectiveMode = settings.getEffectiveFormalityMode(targetPkg)
        scope.launch {
            val apiKey = settings.apiKey
            val baseUrl = settings.baseUrl
            val whisper = settings.whisper
            val llama = settings.llama
            if (apiKey.isBlank()) {
                withContext(Dispatchers.Main) {
                    android.util.Log.w("OpenWispr:Bubble", "Groq API key missing in Settings")
                    Toast.makeText(service, "OpenWispr: API key required in Settings", Toast.LENGTH_SHORT).show()
                    statusText?.text = "Set API key in Settings"
                    tickIcon?.apply { visibility = View.VISIBLE; alpha = 1f }
                    tickLoader?.visibility = View.GONE
                    closeIcon?.apply { isEnabled = true; alpha = 1f; isClickable = true }
                    handler.postDelayed({
                        state = State.Idle
                        service.stopListeningNotification()
                        hide()
                    }, 1400)
                }
                file.delete()
                return@launch
            }
            when (val tr = groq.transcribe(baseUrl, apiKey, whisper, file)) {
                is GroqResult.Error -> withContext(Dispatchers.Main) {
                    android.util.Log.e("OpenWispr:Bubble", "Transcription failed: ${tr.message}")
                    Toast.makeText(service, "Transcription failed: ${tr.message.take(35)}", Toast.LENGTH_SHORT).show()
                    statusText?.text = tr.message.take(60)
                    tickIcon?.apply { visibility = View.VISIBLE; alpha = 1f }
                    tickLoader?.visibility = View.GONE
                    closeIcon?.apply { isEnabled = true; alpha = 1f; isClickable = true }
                    handler.postDelayed({
                        state = State.Idle
                        service.stopListeningNotification()
                        hide()
                    }, 1500)
                }
                is GroqResult.Success -> {
                    val raw = tr.data
                    val temperature = if (effectiveMode == FormalityMode.CUSTOM) 0.7 else 0.2
                    when (val rf = groq.refine(baseUrl, apiKey, llama, raw, effectivePrompt, temperature)) {
                        is GroqResult.Error -> {
                            val safeRaw = settings.applyFormalityStyling(Constants.sanitizeOutput(raw), effectiveMode)
                            historyRepo.insertTranscript(safeRaw, targetPkg, durationMs)
                            withContext(Dispatchers.Main) {
                                val injected = TextInjector.inject(service, safeRaw)
                                statusText?.text = if (injected) "Pasted ✓" else "Copied"
                                tickIcon?.apply { visibility = View.VISIBLE; alpha = 1f }
                                tickLoader?.visibility = View.GONE
                                handler.postDelayed({
                                    state = State.Idle
                                    service.stopListeningNotification()
                                    hide()
                                }, 800)
                            }
                        }
                        is GroqResult.Success -> {
                            val clean = settings.applyFormalityStyling(rf.data, effectiveMode)
                            historyRepo.insertTranscript(clean, targetPkg, durationMs)
                            withContext(Dispatchers.Main) {
                                val injected = TextInjector.inject(service, clean)
                                statusText?.text = if (injected) "Pasted ✓" else "Copied"
                                tickIcon?.apply { visibility = View.VISIBLE; alpha = 1f }
                                tickLoader?.visibility = View.GONE
                                handler.postDelayed({
                                    state = State.Idle
                                    service.stopListeningNotification()
                                    hide()
                                }, 800)
                            }
                        }
                    }
                }
            }
            file.delete()
        }
    }
}
