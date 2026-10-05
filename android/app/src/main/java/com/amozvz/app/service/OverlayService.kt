package com.amozvz.app.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.amozvz.app.AmozVzApplication
import com.amozvz.app.R
import com.amozvz.app.engine.OnDeviceSpeechRecognizer
import com.amozvz.app.utils.ClipboardHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private lateinit var speechRecognizer: OnDeviceSpeechRecognizer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isRecording = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        speechRecognizer = OnDeviceSpeechRecognizer(this)
        setupSpeechCallbacks()
        createFloatingOverlay()
    }

    private fun setupSpeechCallbacks() {
        speechRecognizer.onFinalResult = { rawText ->
            processSpokenText(rawText)
        }
        speechRecognizer.onError = { error ->
            updateOverlayState(false, error)
            mainHandler.postDelayed({
                updateOverlayState(false, "Tap to speak")
            }, 3000)
        }

        DictationForegroundService.onStopRequested = {
            if (isRecording) {
                toggleRecording()
            }
        }
        DictationForegroundService.onCancelRequested = {
            if (isRecording) {
                speechRecognizer.stopListening()
                DictationForegroundService.stop(this)
                updateOverlayState(false, "Cancelled")
                mainHandler.postDelayed({
                    updateOverlayState(false, "Tap to speak")
                }, 1500)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun createFloatingOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_bubble_layout, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        val pillRoot = overlayView?.findViewById<View>(R.id.overlay_pill)
        val btnMic = overlayView?.findViewById<View>(R.id.btn_mic_container)
        val btnClose = overlayView?.findViewById<ImageView>(R.id.btn_close_overlay)

        btnClose?.setOnClickListener {
            stopSelf()
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        pillRoot?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isClick = false
                    }
                    layoutParams?.x = initialX + dx
                    layoutParams?.y = initialY + dy
                    layoutParams?.let { windowManager?.updateViewLayout(overlayView, it) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        toggleRecording()
                    }
                    true
                }
                else -> false
            }
        }

        btnMic?.setOnClickListener {
            toggleRecording()
        }

        windowManager?.addView(overlayView, layoutParams)
    }

    private fun toggleRecording() {
        performHapticFeedback()
        if (!isRecording) {
            isRecording = true
            updateOverlayState(true, "Listening...")
            DictationForegroundService.start(this)
            speechRecognizer.startListening()
        } else {
            isRecording = false
            updateOverlayState(false, "Polishing with AI...")
            DictationForegroundService.stop(this)
            speechRecognizer.stopListening()
        }
    }

    private fun processSpokenText(rawText: String) {
        if (rawText.isBlank()) {
            updateOverlayState(false, "No speech detected")
            mainHandler.postDelayed({ updateOverlayState(false, "Tap to speak") }, 2000)
            return
        }

        updateOverlayState(false, "Polishing with AI...")

        serviceScope.launch {
            val engine = (application as AmozVzApplication).engine
            val result = engine.processSpeechText(rawText)
            val cleaned = result.cleanedText

            // Attempt injection directly into active app via Accessibility
            val inserted = AmozVzAccessibilityService.injectText(cleaned)

            if (inserted) {
                updateOverlayState(false, "Inserted! ✓")
            } else {
                ClipboardHelper.copyToClipboard(this@OverlayService, cleaned, showToast = false)
                updateOverlayState(false, "Copied to clipboard! 📋")
            }

            mainHandler.postDelayed({
                updateOverlayState(false, "Tap to speak")
            }, 2500)
        }
    }

    private fun updateOverlayState(recording: Boolean, statusText: String) {
        overlayView?.findViewById<TextView>(R.id.txt_overlay_status)?.text = statusText
        val micContainer = overlayView?.findViewById<FrameLayout>(R.id.btn_mic_container)

        if (recording) {
            micContainer?.setBackgroundColor(Color.parseColor("#E53935")) // Red recording indicator
        } else {
            micContainer?.setBackgroundColor(Color.parseColor("#313244")) // Default state
        }
    }

    private fun performHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        serviceScope.cancel()
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }
    }
}
