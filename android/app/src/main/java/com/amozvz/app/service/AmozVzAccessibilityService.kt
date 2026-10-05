package com.amozvz.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AmozVzAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val source = event.source ?: return

        if (source.isEditable || source.isFocused) {
            currentFocusedNode?.recycle()
            currentFocusedNode = source
        }
    }

    override fun onInterrupt() {
        currentFocusedNode?.recycle()
        currentFocusedNode = null
    }

    override fun onDestroy() {
        super.onDestroy()
        currentFocusedNode?.recycle()
        currentFocusedNode = null
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        private var instance: AmozVzAccessibilityService? = null
        private var currentFocusedNode: AccessibilityNodeInfo? = null

        val isServiceRunning: Boolean
            get() = instance != null

        /**
         * Injects text directly into the actively focused input field across any app.
         * Returns true if successfully inserted, false if no editable field was in focus.
         */
        fun injectText(text: String): Boolean {
            val node = currentFocusedNode ?: instance?.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

            if (node != null && node.isEditable) {
                // Method 1: ACTION_SET_TEXT
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val setSuccess = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                if (setSuccess) return true

                // Method 2: ACTION_PASTE
                val pasteSuccess = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                if (pasteSuccess) return true
            }

            return false
        }
    }
}
