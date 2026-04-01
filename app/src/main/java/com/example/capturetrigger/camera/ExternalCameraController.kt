package com.example.capturetrigger.camera

interface ExternalCameraController {
    suspend fun initialize(previewEnabled: Boolean)
    suspend fun prepareCamera()
    suspend fun captureStill(): ByteArray
    fun isReady(): Boolean
    fun releaseCamera()
}