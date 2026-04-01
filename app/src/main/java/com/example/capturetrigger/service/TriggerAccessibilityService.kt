package com.example.capturetrigger.service

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class TriggerAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Non ci serve elaborare eventi UI in questo step.
    }

    override fun onInterrupt() {
        // Nessuna azione necessaria per ora.
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_Q) {
            ServiceController.triggerCapture(applicationContext)
            return true
        }
        return super.onKeyEvent(event)
    }
}