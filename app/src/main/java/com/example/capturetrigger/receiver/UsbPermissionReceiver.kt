package com.example.capturetrigger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import com.example.capturetrigger.service.ServiceController
import com.example.capturetrigger.usb.UsbTriggerManager

class UsbPermissionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == UsbTriggerManager.ACTION_USB_PERMISSION) {
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            if (granted) {
                ServiceController.usbRescan(context)
            }
        }
    }
}