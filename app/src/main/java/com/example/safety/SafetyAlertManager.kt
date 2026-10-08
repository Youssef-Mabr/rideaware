package com.example.safety

import com.example.model.AlertDirection
import com.example.model.HazardType
import com.example.model.RiskLevel
import com.example.model.SafetyAlert

enum class AlertSeverity { INFO, WARNING, CRITICAL }

data class SafetyAlertPolicy(
  val cooldownMillis: Long = 5_000L,
  val severityByHazard: Map<HazardType, AlertSeverity> = defaultSeverityByHazard()
)

interface SafetyAlertAudio {
  fun announce(alert: SafetyAlert)
}

class SimulatedSafetyAlertAudio : SafetyAlertAudio {
  override fun announce(alert: SafetyAlert) = Unit
}

class SafetyAlertManager(
  private val policy: SafetyAlertPolicy = SafetyAlertPolicy(),
  private val audio: SafetyAlertAudio = SimulatedSafetyAlertAudio()
) {
  private val lastEmittedAt = mutableMapOf<String, Long>()
  private val activeKeys = mutableSetOf<String>()

  fun onHazards(hazards: List<HazardResult>): List<SafetyAlert> {
    val keys = hazards.map { it.key() }.toSet()
    activeKeys.retainAll(keys)
    val candidates = hazards
      .sortedByDescending { policy.severityByHazard[it.hazardType].priority() }
      .filter { hazard ->
        val key = hazard.key()
        val last = lastEmittedAt[key]
        key !in activeKeys && (last == null || hazard.timestampMillis - last >= policy.cooldownMillis)
      }
    return candidates.map { candidate ->
      val key = candidate.key()
      activeKeys += key
      lastEmittedAt[key] = candidate.timestampMillis
      candidate.toSafetyAlert(policy.severityByHazard.getValue(candidate.hazardType)).also(audio::announce)
    }
  }

  fun reset() {
    activeKeys.clear()
    lastEmittedAt.clear()
  }

  private fun HazardResult.key() = "$hazardType:$trackId"

  private fun HazardResult.toSafetyAlert(severity: AlertSeverity) = SafetyAlert(
    id = "hazard-$trackId-$frameSequence",
    title = titleFor(hazardType),
    description = reason,
    riskLevel = when (severity) {
      AlertSeverity.INFO -> RiskLevel.LOW
      AlertSeverity.WARNING -> RiskLevel.MEDIUM
      AlertSeverity.CRITICAL -> RiskLevel.HIGH
    },
    direction = directionFor(this),
    timestamp = timestampMillis.toString(),
    voiceWarning = voiceFor(hazardType),
    hazardType = hazardType
  )

  private fun directionFor(hazard: HazardResult) = when (hazard.hazardType) {
    HazardType.BLIND_SPOT_OCCUPIED -> if (hazard.reason.contains("right", true)) AlertDirection.BLIND_SPOT_RIGHT else AlertDirection.BLIND_SPOT_LEFT
    HazardType.VEHICLE_APPROACHING_FAST -> if (hazard.source == com.example.model.CameraSource.REAR) AlertDirection.REAR_RIGHT else AlertDirection.AHEAD
    HazardType.PEDESTRIAN_CROSSING -> AlertDirection.FRONT_CENTER
    else -> AlertDirection.AHEAD
  }

  private fun titleFor(type: HazardType) = when (type) {
    HazardType.VEHICLE_APPROACHING_FAST -> "Vehicle approaching"
    HazardType.BLIND_SPOT_OCCUPIED -> "Blind spot warning"
    HazardType.UNSAFE_FOLLOWING_DISTANCE -> "Following distance warning"
    HazardType.PEDESTRIAN_CROSSING -> "Pedestrian ahead"
    HazardType.TRAFFIC_LIGHT_CHANGE -> "Traffic light warning"
    HazardType.ROAD_HAZARD -> "Road hazard"
  }

  private fun voiceFor(type: HazardType) = when (type) {
    HazardType.VEHICLE_APPROACHING_FAST -> "Caution: vehicle approaching"
    HazardType.BLIND_SPOT_OCCUPIED -> "Alert: vehicle in blind spot"
    HazardType.UNSAFE_FOLLOWING_DISTANCE -> "Brake: following distance closing"
    HazardType.PEDESTRIAN_CROSSING -> "Attention: pedestrian ahead"
    HazardType.TRAFFIC_LIGHT_CHANGE -> "Traffic light changing"
    HazardType.ROAD_HAZARD -> "Caution: road hazard"
  }

  private fun AlertSeverity?.priority() = when (this) {
    AlertSeverity.CRITICAL -> 3
    AlertSeverity.WARNING -> 2
    AlertSeverity.INFO -> 1
    null -> 0
  }
}

fun defaultSeverityByHazard() = mapOf(
  HazardType.VEHICLE_APPROACHING_FAST to AlertSeverity.CRITICAL,
  HazardType.BLIND_SPOT_OCCUPIED to AlertSeverity.CRITICAL,
  HazardType.UNSAFE_FOLLOWING_DISTANCE to AlertSeverity.WARNING,
  HazardType.PEDESTRIAN_CROSSING to AlertSeverity.WARNING,
  HazardType.TRAFFIC_LIGHT_CHANGE to AlertSeverity.INFO,
  HazardType.ROAD_HAZARD to AlertSeverity.WARNING
)