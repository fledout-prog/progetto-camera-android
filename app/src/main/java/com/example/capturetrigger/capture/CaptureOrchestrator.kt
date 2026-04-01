package com.example.capturetrigger.capture

import com.example.capturetrigger.data.CounterRepository
import kotlinx.coroutines.flow.first
import android.util.Log

class CaptureOrchestrator(private val repository: CounterRepository) {
    suspend fun triggerCapture() {
        val currentCounter = repository.counterFlow.first()
        Log.d("Capture", "Scatto in corso. Counter attuale: $currentCounter")

        // Logica di scatto qui...

        repository.incrementCounter()
    }
}