package com.example.capturetrigger.capture

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

object PhotoSaver {

    fun saveToGallery(context: Context, jpegBytes: ByteArray, fileName: String): Uri {
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CaptureTrigger")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Impossibile creare record MediaStore")

        resolver.openOutputStream(uri)?.use { output ->
            output.write(jpegBytes)
            output.flush()
        } ?: throw IllegalStateException("Impossibile aprire output stream")

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        return uri
    }
}