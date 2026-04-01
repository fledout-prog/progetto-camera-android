package com.example.capturetrigger

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jiangdg.usb.USBMonitor
import com.jiangdg.uvc.UVCCamera

class MainActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var spinnerResolution: Spinner
    private lateinit var spinnerFormat: Spinner
    private lateinit var btnApply: Button

    private var camera: UVCCamera? = null
    private var usbMonitor: USBMonitor? = null
    private var ctrlBlock: USBMonitor.UsbControlBlock? = null

    @Volatile
    private var surfaceReady = false

    @Volatile
    private var previewStarted = false

    private val tag = "UVC_STABLE"

    private val resolutions = listOf(
        "320x240",
        "640x480",
        "1280x720",
        "1920x1080"
    )

    private val formats = listOf(
        "YUYV",
        "MJPEG"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView = findViewById(R.id.surfaceView)
        spinnerResolution = findViewById(R.id.spinnerResolution)
        spinnerFormat = findViewById(R.id.spinnerFormat)
        btnApply = findViewById(R.id.btnApply)

        spinnerResolution.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resolutions)

        spinnerFormat.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, formats)

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(tag, "surfaceCreated")
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d(tag, "surfaceChanged: ${width}x$height")

                if (width < 100 || height < 100) {
                    Log.d(tag, "Surface troppo piccola, aspetto...")
                    return
                }

                surfaceReady = true
                tryStartPreview()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(tag, "surfaceDestroyed")
                surfaceReady = false
                previewStarted = false
            }
        })

        btnApply.setOnClickListener {
            applySettings()
        }

        usbMonitor = USBMonitor(this, object : USBMonitor.OnDeviceConnectListener {

            override fun onAttach(device: UsbDevice) {
                Log.d(tag, "onAttach: ${device.deviceName}")
                usbMonitor?.requestPermission(device)
            }

            override fun onDetach(device: UsbDevice) {
                Log.d(tag, "onDetach: ${device.deviceName}")
                releaseCamera()
            }

            override fun onConnect(
                device: UsbDevice,
                ctrlBlock: USBMonitor.UsbControlBlock,
                createNew: Boolean
            ) {
                Log.d(tag, "onConnect: ${device.deviceName}")
                this@MainActivity.ctrlBlock = ctrlBlock
                openCamera(ctrlBlock)
            }

            override fun onDisconnect(
                device: UsbDevice,
                ctrlBlock: USBMonitor.UsbControlBlock
            ) {
                Log.d(tag, "onDisconnect: ${device.deviceName}")
                releaseCamera()
            }

            override fun onCancel(device: UsbDevice?) {
                Log.e(tag, "Permesso USB negato/annullato")
                Toast.makeText(
                    this@MainActivity,
                    "Permesso USB negato o annullato",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        try {
            usbMonitor?.register()

            val devices = usbMonitor?.deviceList ?: emptyList()
            if (devices.isNotEmpty()) {
                Log.d(tag, "Device già presente: ${devices.first().deviceName}")
                usbMonitor?.requestPermission(devices.first())
            }
        } catch (t: Throwable) {
            Log.e(tag, "Errore register: ${t.message}", t)
        }
    }

    override fun onStop() {
        try {
            usbMonitor?.unregister()
        } catch (t: Throwable) {
            Log.e(tag, "Errore unregister: ${t.message}", t)
        }

        releaseCamera()
        super.onStop()
    }

    override fun onDestroy() {
        try {
            usbMonitor?.destroy()
        } catch (t: Throwable) {
            Log.e(tag, "Errore destroy: ${t.message}", t)
        }

        releaseCamera()
        super.onDestroy()
    }

    private fun openCamera(block: USBMonitor.UsbControlBlock) {
        try {
            releaseCamera()

            val cam = UVCCamera()
            cam.open(block)
            camera = cam

            Log.d(tag, "Camera aperta. Supported sizes: ${cam.supportedSize}")

            // Configurazione iniziale: la più stabile trovata
            try {
                cam.setPreviewSize(320, 240, UVCCamera.FRAME_FORMAT_YUYV)
                Log.d(tag, "Preview iniziale impostata: 320x240 YUYV")
            } catch (e: Exception) {
                Log.e(tag, "Errore preview iniziale: ${e.message}", e)
            }

            tryStartPreview()
        } catch (t: Throwable) {
            Log.e(tag, "Errore openCamera: ${t.message}", t)
            Toast.makeText(
                this,
                "Errore apertura camera: ${t.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun tryStartPreview() {
        val cam = camera ?: return

        if (!surfaceReady) {
            Log.d(tag, "Surface non pronta")
            return
        }

        if (previewStarted) {
            Log.d(tag, "Preview già avviata")
            return
        }

        try {
            cam.setPreviewDisplay(surfaceView.holder)
            cam.startPreview()
            previewStarted = true
            Log.d(tag, "PREVIEW OK")
        } catch (e: Exception) {
            Log.e(tag, "Errore startPreview: ${e.message}", e)
            Toast.makeText(
                this,
                "Errore preview: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun applySettings() {
        val cam = camera ?: run {
            Toast.makeText(this, "Camera non aperta", Toast.LENGTH_SHORT).show()
            return
        }

        if (!surfaceReady) {
            Toast.makeText(this, "Surface non pronta", Toast.LENGTH_SHORT).show()
            return
        }

        val resolution = spinnerResolution.selectedItem.toString()
        val formatName = spinnerFormat.selectedItem.toString()

        val parts = resolution.split("x")
        if (parts.size != 2) {
            Toast.makeText(this, "Risoluzione non valida", Toast.LENGTH_SHORT).show()
            return
        }

        val width = parts[0].toIntOrNull()
        val height = parts[1].toIntOrNull()

        if (width == null || height == null) {
            Toast.makeText(this, "Risoluzione non valida", Toast.LENGTH_SHORT).show()
            return
        }

        val format = if (formatName == "MJPEG") {
            UVCCamera.FRAME_FORMAT_MJPEG
        } else {
            UVCCamera.FRAME_FORMAT_YUYV
        }

        try {
            previewStarted = false

            try {
                cam.stopPreview()
            } catch (_: Throwable) {
            }

            cam.setPreviewSize(width, height, format)
            cam.setPreviewDisplay(surfaceView.holder)
            cam.startPreview()

            previewStarted = true

            Log.d(tag, "APPLICATO: ${width}x$height - $formatName")
            Toast.makeText(
                this,
                "OK: ${width}x$height - $formatName",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e(tag, "Configurazione non supportata: ${e.message}", e)
            Toast.makeText(this, "NON SUPPORTATO", Toast.LENGTH_SHORT).show()
        }
    }

    private fun releaseCamera() {
        previewStarted = false

        try {
            camera?.stopPreview()
        } catch (_: Throwable) {
        }

        try {
            camera?.destroy()
        } catch (_: Throwable) {
        }

        camera = null

        try {
            ctrlBlock?.close()
        } catch (_: Throwable) {
        }

        ctrlBlock = null
    }
}