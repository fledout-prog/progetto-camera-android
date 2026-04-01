package com.example.capturetrigger.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayOutputStream

class FakeCameraController(
    private val context: Context
) : ExternalCameraController {

    private var ready = false

    override suspend fun initialize(previewEnabled: Boolean) {
        ready = true
    }

    override suspend fun prepareCamera() {
        ready = true
    }

    override suspend fun captureStill(): ByteArray {
        if (!ready) throw IllegalStateException("Camera non pronta")

        val bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            isAntiAlias = true
        }

        canvas.drawText("FAKE CAMERA FRAME", 80f, 200f, paint)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        return out.toByteArray()
    }

    override fun isReady(): Boolean = ready

    override fun releaseCamera() {
        ready = false
    }
}