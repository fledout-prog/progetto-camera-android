package com.example.capturetrigger.probe

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

data class CameraProbeResult(
    val externalFeatureDeclared: Boolean,
    val cameras: List<CameraInfoProbe>
)

data class CameraInfoProbe(
    val cameraId: String,
    val lensFacingLabel: String,
    val hardwareLevelLabel: String,
    val backwardCompatible: Boolean
)

object CameraProbe {

    fun probe(context: Context): CameraProbeResult {
        val packageManager = context.packageManager
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val externalFeatureDeclared =
            packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_EXTERNAL)

        val results = mutableListOf<CameraInfoProbe>()

        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)

            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val hardwareLevel =
                characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            val capabilities =
                characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

            val backwardCompatible = capabilities?.contains(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
            ) ?: false

            results.add(
                CameraInfoProbe(
                    cameraId = cameraId,
                    lensFacingLabel = lensFacingToLabel(lensFacing),
                    hardwareLevelLabel = hardwareLevelToLabel(hardwareLevel),
                    backwardCompatible = backwardCompatible
                )
            )
        }

        return CameraProbeResult(
            externalFeatureDeclared = externalFeatureDeclared,
            cameras = results
        )
    }

    private fun lensFacingToLabel(value: Int?): String {
        return when (value) {
            CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
            CameraCharacteristics.LENS_FACING_BACK -> "BACK"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
            else -> "UNKNOWN"
        }
    }

    private fun hardwareLevelToLabel(value: Int?): String {
        return when (value) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
            else -> "UNKNOWN"
        }
    }
}