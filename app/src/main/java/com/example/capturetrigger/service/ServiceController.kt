package com.example.capturetrigger.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object ServiceController {

    fun start(context: Context) {
        val intent = Intent(context, CaptureForegroundService::class.java).apply {
            action = ServiceActions.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun triggerCapture(context: Context) {
        val intent = Intent(context, CaptureForegroundService::class.java).apply {
            action = ServiceActions.ACTION_TRIGGER_CAPTURE
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun usbRescan(context: Context) {
        val intent = Intent(context, CaptureForegroundService::class.java).apply {
            action = ServiceActions.ACTION_USB_RESCAN
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, CaptureForegroundService::class.java).apply {
            action = ServiceActions.ACTION_STOP
        }
        context.startService(intent)
    }
}