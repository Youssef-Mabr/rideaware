package com.example.safety

import com.example.detection.DetectionResult
import com.example.model.AlertDirection
import com.example.model.HazardType
import com.example.model.RiskLevel

data class SafetyEvent(
  val id: String,
  val hazardType: HazardType,
  val riskLevel: RiskLevel,
  val direction: AlertDirection,
  val detection: DetectionResult,
  val createdAtMillis: Long
)

interface SafetyEventRule {
  fun evaluate(detection: DetectionResult): SafetyEvent?
}

class DefaultSafetyEventRule : SafetyEventRule {
  override fun evaluate(detection: DetectionResult): SafetyEvent? {
    val hazard = when (detection.objectType) {
      com.example.detection.DetectedObjectType.PEDESTRIAN -> HazardType.PEDESTRIAN_CROSSING to (RiskLevel.MEDIUM to AlertDirection.FRONT_CENTER)
      com.example.detection.DetectedObjectType.MOTORCYCLE -> HazardType.BLIND_SPOT_OCCUPIED to (RiskLevel.HIGH to AlertDirection.BLIND_SPOT_LEFT)
      com.example.detection.DetectedObjectType.CAR -> if (detection.source == com.example.model.CameraSource.REAR) {
        HazardType.VEHICLE_APPROACHING_FAST to (RiskLevel.HIGH to AlertDirection.REAR_RIGHT)
      } else {
        HazardType.UNSAFE_FOLLOWING_DISTANCE to (RiskLevel.HIGH to AlertDirection.AHEAD)
      }
      else -> return null
    }
    return SafetyEvent(
      id = "sim-${detection.frameSequence}-${detection.timestampMillis}",
      hazardType = hazard.first,
      riskLevel = hazard.second.first,
      direction = hazard.second.second,
      detection = detection,
      createdAtMillis = detection.timestampMillis
    )
  }
}

class SafetyEventEvaluator(
  private val rules: List<SafetyEventRule> = listOf(DefaultSafetyEventRule())
) {
  fun evaluate(detections: List<DetectionResult>): List<SafetyEvent> = detections.mapNotNull { detection ->
    rules.firstNotNullOfOrNull { it.evaluate(detection) }
  }
}