package com.example.model

data class ReadinessCheck(val title: String, val detail: String, val ready: Boolean)

fun validContact(name: String, phone: String): Boolean =
  name.trim().isNotEmpty() && phone.count(Char::isDigit) in 7..15 &&
    phone.all { it.isDigit() || it in "+ -()." }

fun preRideChecks(helmet: HelmetStatus, contact: EmergencyContact): List<ReadinessCheck> = listOf(
  ReadinessCheck("Helmet connection", if (helmet.isConnected) "Helmet connected" else "Pair your helmet first", helmet.isConnected),
  ReadinessCheck("Front camera", if (helmet.frontCameraReady) "Sample camera ready" else "Camera unavailable", helmet.isConnected && helmet.frontCameraReady),
  ReadinessCheck("Rear camera", if (helmet.rearCameraReady) "Sample camera ready" else "Camera unavailable", helmet.isConnected && helmet.rearCameraReady),
  ReadinessCheck("Battery", "${helmet.batteryPercent}% · more than 20% required", helmet.batteryPercent > 20),
  ReadinessCheck("Storage", "${helmet.storageFreeGb} GB free · more than 5 GB required", helmet.storageFreeGb > 5),
  ReadinessCheck("GPS", helmet.gpsStatus, helmet.isConnected && helmet.gpsStatus.equals("Strong", ignoreCase = true)),
  ReadinessCheck("Audio", if (helmet.audioReady) "Sample audio ready" else "Audio unavailable", helmet.isConnected && helmet.audioReady),
  ReadinessCheck("Emergency contact", contact.name.ifBlank { "Add a name and phone number in Settings" }, validContact(contact.name, contact.phoneNumber))
)
