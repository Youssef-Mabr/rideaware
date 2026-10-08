package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ReadinessTest {
  @Test fun disconnectedHelmetCannotReportReadyCamerasOrGps() {
    val checks = preRideChecks(HelmetStatus(isConnected = false), EmergencyContact())
    listOf("Helmet connection", "Front camera", "Rear camera", "GPS", "Audio").forEach { title ->
      assertFalse(title, checks.single { it.title == title }.ready)
    }
  }

  @Test fun batteryStorageAndContactFailuresBlockReadiness() {
    val checks = preRideChecks(HelmetStatus(batteryPercent = 20, storageFreeGb = 5), EmergencyContact(name = "", phoneNumber = "123"))
    listOf("Battery", "Storage", "Emergency contact").forEach { title ->
      assertFalse(title, checks.single { it.title == title }.ready)
    }
  }

  @Test fun unavailableSensorsAreNotReady() {
    val checks = preRideChecks(HelmetStatus(frontCameraReady = false, rearCameraReady = false, audioReady = false, gpsStatus = "No signal"), EmergencyContact())
    listOf("Front camera", "Rear camera", "Audio", "GPS").forEach { title ->
      assertFalse(title, checks.single { it.title == title }.ready)
    }
  }

  @Test fun completeSampleCanStart() {
    assertTrue(preRideChecks(HelmetStatus(), EmergencyContact()).all { it.ready })
  }

  @Test fun contactValidationAcceptsInternationalFormattingButRejectsIncompleteInput() {
    assertTrue(validContact("Alex", "+65 8123 4567"))
    assertFalse(validContact("   ", "+65 8123 4567"))
    assertFalse(validContact("Alex", "abc81234567"))
    assertFalse(validContact("Alex", "123"))
    assertFalse(validContact("Alex", "1234567890123456"))
  }
}
