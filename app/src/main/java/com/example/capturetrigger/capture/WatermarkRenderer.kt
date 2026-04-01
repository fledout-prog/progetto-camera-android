package com.example.capturetrigger.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import java.io.ByteArrayOutputStream

object WatermarkRenderer {

    fun applyWatermark(originalJpeg: ByteArray, text: String): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(originalJpeg, 0, originalJpeg.size)
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val bgPaint = Paint().apply {
            color = Color.argb(140, 0, 0, 0)
        }

        val padding = 24f
        val margin = 32f

        val bounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, bounds)

        val left = margin
        val bottom = mutableBitmap.height - margin
        val top = bottom - bounds.height() - padding
        val right = left + bounds.width() + padding * 2

        canvas.drawRect(left, top, right, bottom + 12f, bgPaint)
        canvas.drawText(text, left + padding, bottom.toFloat(), textPaint)

        val output = ByteArrayOutputStream()
        mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        return output.toByteArray()
    }
}