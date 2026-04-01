package com.example.capturetrigger.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.capturetrigger.capture.CaptureOrchestrator
import com.example.capturetrigger.data.CounterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CaptureForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var orchestrator: CaptureOrchestrator

    override fun onCreate() {
        super.onCreate()
        val repository = CounterRepository(applicationContext)
        orchestrator = CaptureOrchestrator(repository)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            orchestrator.triggerCapture()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}