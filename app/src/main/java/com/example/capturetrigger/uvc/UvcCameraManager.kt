package com.example.capturetrigger.uvc

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.SurfaceView
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.widget.IAspectRatio
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class UvcCameraManager(
    private val context: Context,
    private val listener: Listener? = null
) {

    interface Listener {
        fun onStatus(message: String)
        fun onError(message: String)
        fun onPhotoSaved(path: String)
        fun onPreviewBitmap(bitmap: Bitmap)
    }

    private var mCameraClient: CameraClient? = null
    private var previewSurfaceView: SurfaceView? = null
    private val tag = "UVC_MANAGER"

    private val lastBitmapRef = AtomicReference<Bitmap?>(null)
    private var lastPreviewDispatchMs = 0L

    private val previewCallback = object : IPreviewDataCallBack {
        override fun onPreviewData(
            data: ByteArray?,
            width: Int,
            height: Int,
            format: IPreviewDataCallBack.DataFormat
        ) {
            if (data == null || width <= 0 || height <= 0) return

            // Limitiamo il rendering UI a ~8 fps per non saturare il telefono
            val now = SystemClock.elapsedRealtime()
            if (now - lastPreviewDispatchMs < 120) return
            lastPreviewDispatchMs = now

            try {
                val bitmap = when (format) {
                    IPreviewDataCallBack.DataFormat.NV21 -> nv21ToBitmap(data, width, height)
                    IPreviewDataCallBack.DataFormat.RGBA -> rgbaToBitmap(data, width, height)
                } ?: return

                lastBitmapRef.getAndSet(bitmap)?.recycle()
                listener?.onPreviewBitmap(bitmap)
            } catch (t: Throwable) {
                Log.e(tag, "Errore conversione preview: ${t.message}", t)
            }
        }
    }

    init {
        try {
            val request = CameraRequest.Builder()
                .setPreviewWidth(320)
                .setPreviewHeight(240)
                .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_YUYV)
                .setRenderMode(CameraRequest.RenderMode.NORMAL)
                .setAspectRatioShow(true)
                .create()

            mCameraClient = CameraClient.newBuilder(context)
                .setEnableGLES(false)
                .setCameraStrategy(CameraUvcStrategy(context))
                .setCameraRequest(request)
                .setCameraStateCallback(object : ICameraStateCallBack {
                    override fun onCameraState(
                        self: MultiCameraClient.ICamera,
                        code: ICameraStateCallBack.State,
                        msg: String?
                    ) {
                        val text = "STATO: $code | MSG: ${msg ?: "null"}"
                        Log.d(tag, text)
                        listener?.onStatus(text)
                    }
                })
                .build()

            mCameraClient?.addPreviewDataCallBack(previewCallback)
        } catch (e: Exception) {
            val msg = "Errore init: ${e.message}"
            Log.e(tag, msg, e)
            listener?.onError(msg)
        }
    }

    fun openCamera(view: IAspectRatio?) {
        if (view == null) {
            listener?.onError("Preview view nulla")
            return
        }

        if (view !is SurfaceView) {
            listener?.onError("La preview view non è una SurfaceView")
            return
        }

        previewSurfaceView = view

        try {
            Log.d(tag, "Apertura camera...")
            listener?.onStatus("Apertura camera...")
            mCameraClient?.openCamera(view)
        } catch (e: Exception) {
            val msg = "Errore open: ${e.message}"
            Log.e(tag, msg, e)
            listener?.onError(msg)
        }
    }

    fun closeCamera() {
        try {
            Log.d(tag, "Chiusura camera...")
            mCameraClient?.closeCamera()
        } catch (e: Exception) {
            val msg = "Errore close: ${e.message}"
            Log.e(tag, msg, e)
            listener?.onError(msg)
        }
    }

    fun capturePhoto() {
        val bitmap = lastBitmapRef.get()
        if (bitmap == null) {
            listener?.onError("Nessun frame disponibile da salvare")
            return
        }
        saveBitmapToGallery(bitmap)
    }

    private fun nv21ToBitmap(data: ByteArray, width: Int, height: Int): Bitmap? {
        val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        val ok = yuvImage.compressToJpeg(Rect(0, 0, width, height), 92, out)
        if (!ok) return null
        val jpegBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    private fun rgbaToBitmap(data: ByteArray, width: Int, height: Int): Bitmap? {
        return try {
            val buffer = java.nio.ByteBuffer.wrap(data)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(buffer)
            bmp
        } catch (_: Throwable) {
            null
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val resolver = context.contentResolver
        val fileName = "USB_${System.currentTimeMillis()}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CaptureTrigger")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            listener?.onError("Impossibile creare il file in galleria")
            return
        }

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                if (!ok) throw IOException("Compressione JPEG fallita")
            } ?: throw IOException("OutputStream nullo")

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            val savedPath = "Pictures/CaptureTrigger/$fileName"
            Log.d(tag, "Foto salvata: $savedPath")
            listener?.onPhotoSaved(savedPath)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            val msg = "Errore salvataggio bitmap: ${e.message}"
            Log.e(tag, msg, e)
            listener?.onError(msg)
        }
    }
}