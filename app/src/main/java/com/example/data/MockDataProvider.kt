package com.example.data

import com.example.model.AlertDirection
import com.example.model.CameraSource
import com.example.model.EmergencyContact
import com.example.model.GenieMessage
import com.example.model.HazardType
import com.example.model.HelmetStatus
import com.example.model.ProtectedClip
import com.example.model.RideSummary
import com.example.model.RiskLevel
import com.example.model.SafetyAlert
import com.example.model.VoiceConversation

object MockDataProvider {

  val defaultHelmetStatus = HelmetStatus(
    name = "Geni One",
    batteryPercent = 86,
    isCharging = false,
    storageFreeGb = 72,
    storageTotalGb = 128,
    isConnected = true,
    frontCameraReady = true,
    rearCameraReady = true,
    audioReady = true,
    gpsStatus = "Strong",
    bluetoothAudioConnected = true,
    wifiVideoConnected = true,
    firmwareVersion = "v2.4.1",
    serialNumber = "GENI-2026-98174"
  )

  val defaultEmergencyContact = EmergencyContact(
    name = "Mariam",
    relationship = "Family",
    phoneNumber = "+1 (555) 019-2849",
    phone = "+1 (555) 019-2849",
    autoShareGps = true
  )

  val emergencyContact = defaultEmergencyContact

  val sampleAlerts = listOf(
    SafetyAlert(
      id = "alt-1",
      title = "Vehicle approaching rear-right",
      description = "Closing speed +28 km/h. Detected in right rearview radar cone.",
      riskLevel = RiskLevel.HIGH,
      direction = AlertDirection.REAR_RIGHT,
      timestamp = "08:16:32",
      voiceWarning = "Caution: Fast vehicle approaching rear right",
      protectedClipId = "clip-1",
      hazardType = HazardType.VEHICLE_APPROACHING_FAST
    ),
    SafetyAlert(
      id = "alt-2",
      title = "Motorcycle in blind spot",
      description = "Two-wheeler detected in left mirror blind zone for >4 seconds.",
      riskLevel = RiskLevel.HIGH,
      direction = AlertDirection.BLIND_SPOT_LEFT,
      timestamp = "08:21:10",
      voiceWarning = "Alert: Motorcycle in your left blind spot",
      protectedClipId = "clip-2",
      hazardType = HazardType.BLIND_SPOT_OCCUPIED
    ),
    SafetyAlert(
      id = "alt-3",
      title = "Pedestrian ahead",
      description = "Pedestrian stepping into crossing lane 35 meters ahead.",
      riskLevel = RiskLevel.MEDIUM,
      direction = AlertDirection.FRONT_CENTER,
      timestamp = "08:27:45",
      voiceWarning = "Attention: Pedestrian ahead",
      protectedClipId = null,
      hazardType = HazardType.PEDESTRIAN_CROSSING
    ),
    SafetyAlert(
      id = "alt-4",
      title = "Unsafe closing distance",
      description = "Lead sedan decelerated abruptly. Separation dropped below 1.2s.",
      riskLevel = RiskLevel.HIGH,
      direction = AlertDirection.AHEAD,
      timestamp = "08:31:02",
      voiceWarning = "Brake: Distance closing rapidly",
      protectedClipId = null,
      hazardType = HazardType.UNSAFE_FOLLOWING_DISTANCE
    ),
    SafetyAlert(
      id = "alt-5",
      title = "Traffic light changing ahead",
      description = "Intersection camera detected signal phase transition to amber.",
      riskLevel = RiskLevel.LOW,
      direction = AlertDirection.FRONT_CENTER,
      timestamp = "08:34:12",
      voiceWarning = "Yellow signal ahead",
      protectedClipId = null,
      hazardType = HazardType.TRAFFIC_LIGHT_CHANGE
    )
  )

  val sampleClips = listOf(
    ProtectedClip(
      id = "clip-1",
      title = "Fast vehicle from behind",
      timestamp = "Today 10:14 AM",
      cameraSource = CameraSource.DUAL_SPLIT,
      durationSeconds = 30,
      durationSec = 30,
      alertType = "Auto-Protected on High Risk",
      hazardTag = "Fast closing vehicle from behind",
      riskLevel = "HIGH",
      speedAtEventKmH = 64,
      coordinates = "25.2048° N, 55.2708° E",
      thumbnailColor = 0xFF1E293B
    ),
    ProtectedClip(
      id = "clip-2",
      title = "Pedestrian near crosswalk",
      timestamp = "Today 10:28 AM",
      cameraSource = CameraSource.DUAL_SPLIT,
      durationSeconds = 30,
      durationSec = 30,
      alertType = "Pedestrian Crosswalk Warning",
      hazardTag = "Pedestrian warning near crosswalk",
      riskLevel = "MEDIUM",
      speedAtEventKmH = 38,
      coordinates = "25.2052° N, 55.2715° E",
      thumbnailColor = 0xFF0F2537
    ),
    ProtectedClip(
      id = "clip-3",
      title = "Blind spot merge evade",
      timestamp = "22 Aug 06:14 PM",
      cameraSource = CameraSource.DUAL_SPLIT,
      durationSeconds = 45,
      durationSec = 45,
      alertType = "Left Blind Spot Alert",
      hazardTag = "Vehicle in blind spot",
      riskLevel = "HIGH",
      speedAtEventKmH = 55,
      coordinates = "25.2010° N, 55.2690° E",
      thumbnailColor = 0xFF183B32
    ),
    ProtectedClip(
      id = "clip-4",
      title = "Sunset Coast Highway Pass",
      timestamp = "18 Aug 05:40 PM",
      cameraSource = CameraSource.DUAL_SPLIT,
      durationSeconds = 60,
      durationSec = 60,
      alertType = "Manual Voice Capture",
      hazardTag = "Scenic Voice Capture",
      riskLevel = "LOW",
      speedAtEventKmH = 78,
      coordinates = "25.1980° N, 55.2640° E",
      thumbnailColor = 0xFF352643
    )
  )

  val sampleProtectedClips = sampleClips

  val mockRides = listOf(
    RideSummary(
      id = "ride-1",
      title = "Morning Coast Commute",
      date = "Today",
      time = "8:12 AM",
      startTime = "8:12 AM",
      endTime = "8:36 AM",
      durationMinutes = 24,
      distanceKm = 6.3f,
      avgSpeedKmH = 42,
      averageSpeedKmH = 42,
      maxSpeedKmH = 68,
      safetyScore = 94,
      alertCount = 2,
      safetyEventCount = 2,
      protectedClipsCount = 2,
      batteryUsedPercent = 8,
      batteryConsumedPercent = 8,
      safetyBadge = "2 Alerts Logged",
      isSafeRide = false,
      alerts = sampleAlerts.take(2),
      clips = sampleClips.take(2),
      protectedClips = sampleClips.take(2)
    ),
    RideSummary(
      id = "ride-2",
      title = "Dubai Marina Circuit",
      date = "22 August",
      time = "6:04 PM",
      startTime = "6:04 PM",
      endTime = "6:42 PM",
      durationMinutes = 38,
      distanceKm = 11.8f,
      avgSpeedKmH = 49,
      averageSpeedKmH = 49,
      maxSpeedKmH = 82,
      safetyScore = 89,
      alertCount = 1,
      safetyEventCount = 1,
      protectedClipsCount = 1,
      batteryUsedPercent = 14,
      batteryConsumedPercent = 14,
      safetyBadge = "1 Alert Logged",
      isSafeRide = false,
      alerts = sampleAlerts.take(1),
      clips = listOf(sampleClips[2]),
      protectedClips = listOf(sampleClips[2])
    ),
    RideSummary(
      id = "ride-3",
      title = "Al Qudra Highway Cruise",
      date = "18 August",
      time = "5:15 PM",
      startTime = "5:15 PM",
      endTime = "6:07 PM",
      durationMinutes = 52,
      distanceKm = 24.1f,
      avgSpeedKmH = 58,
      averageSpeedKmH = 58,
      maxSpeedKmH = 94,
      safetyScore = 98,
      alertCount = 0,
      safetyEventCount = 0,
      protectedClipsCount = 1,
      batteryUsedPercent = 19,
      batteryConsumedPercent = 19,
      safetyBadge = "Safe Ride",
      isSafeRide = true,
      alerts = emptyList(),
      clips = listOf(sampleClips[3]),
      protectedClips = listOf(sampleClips[3])
    )
  )

  val initialRides = mockRides

  val sampleConversations = listOf(
    VoiceConversation(
      userQuery = "Is anything behind me?",
      genieResponse = "A silver sedan is approaching from your rear-right at approximately forty-five kilometers per hour."
    ),
    VoiceConversation(
      userQuery = "How is my helmet battery?",
      genieResponse = "Your helmet battery is at eighty-six percent. You have roughly five hours of ride time remaining."
    ),
    VoiceConversation(
      userQuery = "Find the nearest petrol station.",
      genieResponse = "The nearest fuel stop is 1.8 km ahead on the right."
    ),
    VoiceConversation(
      userQuery = "What is my current safety score?",
      genieResponse = "Your current safety score is ninety-four. Smooth ride so far."
    ),
    VoiceConversation(
      userQuery = "Translate: 'Where is the workshop?' into Arabic.",
      genieResponse = "'Ayna warshat al-tasleeh?'"
    )
  )

  val initialGenieMessages = listOf(
    GenieMessage(
      id = "msg-1",
      isUser = false,
      text = "Good morning Youssef. Geni One is fully synchronized. Voice commands are armed and ready.",
      time = "08:12 AM"
    ),
    GenieMessage(
      id = "msg-2",
      isUser = true,
      text = "What is behind me?",
      time = "08:14 AM"
    ),
    GenieMessage(
      id = "msg-3",
      isUser = false,
      text = "Rear radar detects a silver sedan trailing at 24 meters with matching speed. Right overtaking lane is open.",
      time = "08:14 AM",
      badge = "Rear Vision"
    )
  )

  fun getGenieResponse(query: String, isOffline: Boolean): String {
    val lower = query.lowercase()
    return when {
      "behind" in lower -> "A silver sedan is approaching from your rear-right at approximately 45 km/h."
      "save" in lower || "moment" in lower -> "Protected clip captured! 30-second dual-angle video saved to helmet vault."
      "battery" in lower -> "Your helmet battery is at 86%. You have roughly 5 hours of ride time remaining."
      "petrol" in lower || "fuel" in lower || "gas" in lower -> "The nearest fuel stop is 1.8 km ahead on the right."
      "score" in lower || "safety" in lower -> "Your current safety score is 94. Smooth ride so far."
      "translate" in lower || "arabic" in lower -> "'Ayna warshat al-tasleeh?'"
      else -> "Genie is monitoring in real-time. Say 'behind me', 'battery', or 'save moment' hands-free."
    }
  }
}
