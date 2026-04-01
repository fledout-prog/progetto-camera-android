package com.example.capturetrigger.capture

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CaptureIdentity(
    val counter: Long,
    val watermarkText: String,
    val fileName: String,
    val timestampMillis: Long
)

object CaptureIdentityBuilder {

    fun build(counter: Long, timestampMillis: Long): CaptureIdentity {
        val dt = Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        val counterPart = String.format("%05d", counter)
        val timeDisplay = dt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val dateDisplay = dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val timeFile = dt.format(DateTimeFormatter.ofPattern("HHmmss"))
        val dateFile = dt.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        val watermark = "#$counterPart  $timeDisplay  $dateDisplay"
        val fileName = "${counterPart}_${timeFile}_${dateFile}.jpg"

        return CaptureIdentity(
            counter = counter,
            watermarkText = watermark,
            fileName = fileName,
            timestampMillis = timestampMillis
        )
    }
}