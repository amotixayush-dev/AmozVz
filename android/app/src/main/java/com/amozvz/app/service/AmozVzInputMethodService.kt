package com.amozvz.app.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.amozvz.app.AmozVzApplication
import com.amozvz.app.engine.OnDeviceSpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AmozVzInputMethodService : InputMethodService() {

    private lateinit var speechRecognizer: OnDeviceSpeechRecognizer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isRecording = false
    private var statusView: TextView? = null
    private var micButton: Button? = null

    override fun onCreate() {
        super.onCreate()
        speechRecognizer = OnDeviceSpeechRecognizer(this)
        setupSpeechRecognizer()
    }

    override fun onCreateInputView(): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1E1E2E.toInt())
            setPadding(16, 24, 16, 24)
        }

        statusView = TextView(this).apply {
            text = "Tap microphone to speak with AmozVz"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            gravity = android.view.Gravity.CENTER
        }

        micButton = Button(this).apply {
            text = "🎤 Start Dictation"
            setBackgroundColor(0xFF6750A4.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                toggleDictation()
            }
        }

        layout.addView(statusView)
        layout.addView(micButton)
        return layout
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.onFinalResult = { rawText ->
            statusView?.text = "Polishing with AI..."
            scope.launch {
                val engine = (application as AmozVzApplication).engine
                val result = engine.processSpeechText(rawText)
                currentInputConnection?.commitText(result.cleanedText, 1)
                statusView?.text = "Dictated! ✓"
                micButton?.text = "🎤 Start Dictation"
                micButton?.setBackgroundColor(0xFF6750A4.toInt())
                isRecording = false
            }
        }
        speechRecognizer.onError = { error ->
            statusView?.text = "Error: $error"
            micButton?.text = "🎤 Start Dictation"
            micButton?.setBackgroundColor(0xFF6750A4.toInt())
            isRecording = false
        }
    }

    private fun toggleDictation() {
        if (!isRecording) {
            isRecording = true
            statusView?.text = "Listening... Speak naturally"
            micButton?.text = "⏹ Stop & Insert"
            micButton?.setBackgroundColor(0xFFE53935.toInt())
            speechRecognizer.startListening()
        } else {
            statusView?.text = "Processing..."
            speechRecognizer.stopListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        scope.cancel()
    }
}
