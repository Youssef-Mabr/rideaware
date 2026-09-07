package com.example.model

enum class RiskLevel {
  HIGH,
  MEDIUM,
  LOW
}

enum class HazardType {
  VEHICLE_APPROACHING_FAST,
  BLIND_SPOT_OCCUPIED,
  PEDESTRIAN_CROSSING,
  UNSAFE_FOLLOWING_DISTANCE,
  TRAFFIC_LIGHT_CHANGE,
  ROAD_HAZARD
}

enum class AlertDirection {
  REAR_RIGHT,
  FRONT_CENTER,
  BLIND_SPOT_LEFT,
  BLIND_SPOT_RIGHT,
  REAR_LEFT,
  AHEAD
}

enum class CameraSource {
  FRONT,
  REAR,
  SPLIT,
  DUAL_SPLIT
}

enum class GenieState {
  IDLE,
  LISTENING,
  THINKING,
  SPEAKING
}

data class VoiceConversation(
  val userQuery: String,
  val genieResponse: String
)

data class SafetyAlert(
  val id: String,
  val title: String,
  val description: String,
  val riskLevel: RiskLevel,
  val direction: AlertDirection,
  val timestamp: String,
  val voiceWarning: String,
  val protectedClipId: String? = null,
  val hazardType: HazardType = HazardType.VEHICLE_APPROACHING_FAST
)

data class ProtectedClip(
  val id: String,
  val title: String,
  val timestamp: String,
  val cameraSource: CameraSource = CameraSource.DUAL_SPLIT,
  val durationSeconds: Int = 30,
  val durationSec: Int = 30,
  val alertType: String = "Auto-Protected on High Risk",
  val hazardTag: String = "Fast closing vehicle from behind",
  val riskLevel: String = "HIGH",
  val speedAtEventKmH: Int = 64,
  val coordinates: String = "25.2048° N, 55.2708° E",
  val frontThumbnail: String = "thumb_front",
  val rearThumbnail: String = "thumb_rear",
  val thumbnailColor: Long = 0xFF162033
)

data class RideSummary(
  val id: String,
  val title: String = "Morning Commute",
  val date: String,
  val time: String = "08:12 AM",
  val startTime: String = "08:12 AM",
  val endTime: String = "08:36 AM",
  val durationMinutes: Int,
  val distanceKm: Float,
  val avgSpeedKmH: Int = 42,
  val averageSpeedKmH: Int = 42,
  val maxSpeedKmH: Int = 68,
  val safetyScore: Int = 92,
  val alertCount: Int = 2,
  val safetyEventCount: Int = 2,
  val protectedClipsCount: Int = 2,
  val batteryUsedPercent: Int = 8,
  val batteryConsumedPercent: Int = 8,
  val safetyBadge: String = "4 Alerts Logged",
  val isSafeRide: Boolean = false,
  val alerts: List<SafetyAlert> = emptyList(),
  val clips: List<ProtectedClip> = emptyList(),
  val protectedClips: List<ProtectedClip> = emptyList()
)

data class HelmetStatus(
  val name: String = "Geni One",
  val batteryPercent: Int = 86,
  val isCharging: Boolean = false,
  val storageFreeGb: Int = 72,
  val storageTotalGb: Int = 128,
  val isConnected: Boolean = true,
  val isSearching: Boolean = false,
  val frontCameraReady: Boolean = true,
  val rearCameraReady: Boolean = true,
  val audioReady: Boolean = true,
  val gpsStatus: String = "Strong",
  val bluetoothAudioConnected: Boolean = true,
  val wifiVideoConnected: Boolean = true,
  val firmwareVersion: String = "v2.4.1",
  val serialNumber: String = "GENI-2026-98174"
)

data class EmergencyContact(
  val name: String = "Mariam",
  val relationship: String = "Family",
  val phoneNumber: String = "+1 (555) 019-2849",
  val phone: String = "+1 (555) 019-2849",
  val autoShareGps: Boolean = true
)

data class GenieMessage(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val time: String,
  val badge: String? = null
)

enum class AppDestination {
  WELCOME,
  INTRO,
  FEATURE_INTRO,
  PERMISSIONS,
  PAIRING,
  PAIR_HELMET,
  HELMET_SETUP,
  HOME,
  PRE_RIDE,
  PRE_RIDE_CHECK,
  ACTIVE_RIDE,
  LIVE_CAMERA,
  LIVE_CAMERAS,
  CRASH_DETECTION_SOS,
  RIDE_SUMMARY,
  RIDES,
  RIDE_DETAIL,
  RIDE_DETAILS,
  VIDEO_PLAYBACK,
  GENIE,
  HELMET,
  SETTINGS
}

enum class ErrorDemoType(val title: String, val description: String, val actionLabel: String) {
  NONE("Normal State", "All systems operational", "OK"),
  HELMET_DISCONNECTED("Helmet Disconnected", "Geni One lost Bluetooth connection.", "Reconnect Helmet"),
  SEARCHING_HELMET("Searching for Helmet", "Scanning nearby low-energy Bluetooth signals...", "Cancel Scan"),
  BLUETOOTH_DISABLED("Bluetooth Disabled", "Phone Bluetooth is switched off.", "Enable Bluetooth"),
  WIFI_UNAVAILABLE("Helmet Wi-Fi Unavailable", "High-speed video stream requires helmet Wi-Fi.", "Retry Wi-Fi"),
  FRONT_CAM_UNAVAILABLE("Front Camera Unavailable", "Lens obstructed or connection timeout.", "Diagnose Camera"),
  REAR_CAM_UNAVAILABLE("Rear Camera Unavailable", "Rear camera feed dropped. Checking cable link.", "Retry Connection"),
  LOW_BATTERY("Low Helmet Battery", "Battery level is at 12%. Connect charger soon.", "Power Saver Mode"),
  STORAGE_ALMOST_FULL("Storage Almost Full", "Only 2.4 GB remaining on helmet memory.", "Manage Storage"),
  STORAGE_FULL("Storage Full", "Recording paused. Protected memory card is full.", "Clear Old Clips"),
  NO_PREVIOUS_RIDES("No Previous Rides", "Complete your first ride to view safety statistics.", "Start First Ride"),
  NO_PROTECTED_CLIPS("No Protected Clips", "No incidents or manually saved clips yet.", "Learn How to Save"),
  PERMISSION_DENIED("Required Permission Denied", "Nearby devices permission required for helmet link.", "Open App Settings"),
  NO_INTERNET("No Internet Connection", "Cloud translation offline. Core helmet safety active.", "Continue Offline"),
  AI_UNAVAILABLE("Genie AI Unavailable", "Could not reach speech synthesis cloud service.", "Retry Connection"),
  PAIRING_FAILED("Helmet Pairing Failed", "Pairing token expired or helmet was not in pairing mode.", "Retry QR Scan")
}
