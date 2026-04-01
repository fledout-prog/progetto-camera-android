package com.example.capturetrigger.usb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException
import java.nio.charset.StandardCharsets

class UsbTriggerManager(
    private val context: Context,
    private val listener: Listener
) {

    interface Listener {
        fun onTriggerReceived()
        fun onUsbStatusChanged(status: String)
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.example.capturetrigger.USB_PERMISSION"
        private const val TAG = "UsbTriggerManager"

        private const val RP_VENDOR_ID = 0x2E8A

        private val COMMON_RP_PIDS = setOf(
            0x0005,
            0x000A,
            0x000B,
            0x000C,
            0x000F,
            0x00C0
        )
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var port: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    private val lineBuffer = StringBuilder()

    fun start() {
        rescanAndConnect()
    }

    fun rescanAndConnect() {
        closeCurrentConnection()

        val deviceList = usbManager.deviceList
        if (deviceList.isEmpty()) {
            listener.onUsbStatusChanged("Nessun device USB collegato")
            return
        }

        for ((_, device) in deviceList) {
            listener.onUsbStatusChanged(
                "USB: VID=${hex(device.vendorId)} PID=${hex(device.productId)} IF=${device.interfaceCount}"
            )
        }

        val selectedDevice = selectBestDevice(deviceList.values.toList())
        if (selectedDevice == null) {
            listener.onUsbStatusChanged("Nessun device USB adatto trovato")
            return
        }

        listener.onUsbStatusChanged(
            "Selezionato: VID=${hex(selectedDevice.vendorId)} PID=${hex(selectedDevice.productId)}"
        )

        if (!usbManager.hasPermission(selectedDevice)) {
            requestPermission(selectedDevice)
            listener.onUsbStatusChanged("Permesso USB richiesto")
            return
        }

        val connection = usbManager.openDevice(selectedDevice)
        if (connection == null) {
            listener.onUsbStatusChanged("Impossibile aprire il device USB")
            return
        }

        try {
            val driver = buildDriverForDevice(selectedDevice)
            if (driver == null) {
                listener.onUsbStatusChanged("Driver seriale non trovato per il device")
                return
            }

            val serialPort = driver.ports.firstOrNull()
            if (serialPort == null) {
                listener.onUsbStatusChanged("Nessuna porta seriale disponibile")
                return
            }

            serialPort.open(connection)
            serialPort.setParameters(
                115200,
                8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )

            try {
                serialPort.dtr = true
            } catch (_: Exception) {
            }

            try {
                serialPort.rts = true
            } catch (_: Exception) {
            }

            port = serialPort

            val manager = SerialInputOutputManager(serialPort, object : SerialInputOutputManager.Listener {
                override fun onNewData(data: ByteArray) {
                    handleIncomingData(data)
                }

                override fun onRunError(e: Exception) {
                    Log.e(TAG, "Serial IO error", e)
                    listener.onUsbStatusChanged("Errore seriale: ${e.message}")
                    closeCurrentConnection()
                }
            })

            manager.readTimeout = 100
            manager.start()

            ioManager = manager

            listener.onUsbStatusChanged(
                "Seriale connessa: VID=${hex(selectedDevice.vendorId)} PID=${hex(selectedDevice.productId)} DTR/RTS ON"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Errore apertura seriale", e)
            listener.onUsbStatusChanged("Errore apertura seriale: ${e.message}")
            closeCurrentConnection()
        }
    }

    private fun selectBestDevice(devices: List<UsbDevice>): UsbDevice? {
        val rpDevice = devices.firstOrNull { it.vendorId == RP_VENDOR_ID }
        if (rpDevice != null) return rpDevice

        val defaultDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (defaultDrivers.isNotEmpty()) {
            return defaultDrivers.first().device
        }

        return devices.firstOrNull()
    }

    private fun buildDriverForDevice(device: UsbDevice): UsbSerialDriver? {
        if (device.vendorId == RP_VENDOR_ID) {
            return CdcAcmSerialDriver(device)
        }

        val defaultDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        val match = defaultDrivers.firstOrNull {
            it.device.deviceId == device.deviceId
        }
        if (match != null) return match

        val customTable = ProbeTable().apply {
            for (pid in COMMON_RP_PIDS) {
                addProduct(RP_VENDOR_ID, pid, CdcAcmSerialDriver::class.java)
            }
        }
        val customDrivers = UsbSerialProber(customTable).findAllDrivers(usbManager)
        return customDrivers.firstOrNull {
            it.device.deviceId == device.deviceId
        }
    }

    private fun requestPermission(device: UsbDevice) {
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
        usbManager.requestPermission(device, pendingIntent)
    }

    private fun handleIncomingData(data: ByteArray) {
        val text = String(data, StandardCharsets.UTF_8)

        synchronized(lineBuffer) {
            lineBuffer.append(text)

            while (true) {
                val newlineIndex = lineBuffer.indexOf("\n")
                if (newlineIndex == -1) break

                val rawLine = lineBuffer.substring(0, newlineIndex)
                lineBuffer.delete(0, newlineIndex + 1)

                val line = rawLine.trim()
                if (line.isEmpty()) continue

                Log.d(TAG, "RX: $line")
                listener.onUsbStatusChanged("RX: $line")

                if (line == "TRIGGER") {
                    listener.onTriggerReceived()
                }
            }
        }
    }

    fun stop() {
        closeCurrentConnection()
    }

    private fun closeCurrentConnection() {
        try {
            ioManager?.stop()
        } catch (_: Exception) {
        }
        ioManager = null

        try {
            port?.close()
        } catch (_: IOException) {
        }
        port = null
    }

    private fun hex(value: Int): String {
        return "0x" + value.toString(16).uppercase()
    }
}