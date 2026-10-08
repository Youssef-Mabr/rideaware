package com.example.safety

import com.example.detection.DetectedObjectType
import com.example.model.AlertDirection
import com.example.model.CameraSource
import com.example.model.HazardType
import com.example.model.RiskLevel
import com.example.tracking.TrackedObject
import kotlin.math.max

data class HazardThresholds(
  val persistenceFrames: Int = 3,
  val approachGrowthRatio: Float = 0.12f,
  val followingDistanceGrowthRatio: Float = 0.25f,
  val dangerZoneLeftMax: Float = 0.35f,
  val dangerZoneRightMin: Float = 0.65f,
  val pathCenterMin: Float = 0.35f,
  val pathCenterMax: Float = 0.65f,
  val pathEntryGrowthRatio: Float = 0.08f,
  val minimumConfidence: Float = 0.45f
)

data class HazardResult(
  val hazardType: HazardType,
  val severity: RiskLevel,
  val trackId: Long,
  val objectType: DetectedObjectType,
  val source: CameraSource,
  val confidence: Float,
  val timestampMillis: Long,
  val frameSequence: Long,
  val reason: String
)

class HazardEvaluator(
  private val thresholds: HazardThresholds = HazardThresholds()
) {
  private data class TrackHistory(
    var previousArea: Float,
    var approachFrames: Int = 0,
    var rearFrames: Int = 0,
    var leftFrames: Int = 0,
    var rightFrames: Int = 0,
    var blindSpotFrames: Int = 0,
    var followingFrames: Int = 0,
    var pedestrianPathFrames: Int = 0
  )

  private val history = mutableMapOf<Long, TrackHistory>()

  fun evaluate(trackedObjects: List<TrackedObject>): List<HazardResult> {
    val activeIds = trackedObjects.map { it.trackId }.toSet()
    history.keys.retainAll(activeIds)
    return trackedObjects.flatMap { tracked -> evaluateTrack(tracked) }
  }

  fun reset() = history.clear()

  private fun evaluateTrack(tracked: TrackedObject): List<HazardResult> {
    if (tracked.confidence < thresholds.minimumConfidence) return emptyList()
    val vehicle = tracked.objectType == DetectedObjectType.CAR || tracked.objectType == DetectedObjectType.MOTORCYCLE
    val area = tracked.boundingBox.area()
    val state = history.getOrPut(tracked.trackId) { TrackHistory(area) }
    val growth = if (state.previousArea <= 0f) 0f else (area - state.previousArea) / state.previousArea
    state.previousArea = area
    val results = mutableListOf<HazardResult>()

    if (vehicle) {
      state.approachFrames = if (growth >= thresholds.approachGrowthRatio) state.approachFrames + 1 else 0
      if (state.approachFrames >= thresholds.persistenceFrames) {
        results += result(tracked, HazardType.VEHICLE_APPROACHING_FAST, RiskLevel.HIGH,
          "Bounding-box area grew ${(growth * 100).toInt()}% for ${state.approachFrames} frames.")
      }

      if (tracked.source == CameraSource.REAR) {
        state.rearFrames++
        if (state.rearFrames >= thresholds.persistenceFrames) {
          results += result(tracked, HazardType.VEHICLE_APPROACHING_FAST, RiskLevel.HIGH, "Vehicle persisted in rear-camera view.", AlertDirection.REAR_RIGHT)
        }
      } else {
        state.rearFrames = 0
      }

      val centerX = tracked.currentPosition.x
      val left = centerX < thresholds.dangerZoneLeftMax
      val right = centerX > thresholds.dangerZoneRightMin
      state.leftFrames = if (left) state.leftFrames + 1 else 0
      state.rightFrames = if (right) state.rightFrames + 1 else 0
      state.blindSpotFrames = if (left || right) state.blindSpotFrames + 1 else 0
      if (state.leftFrames >= thresholds.persistenceFrames) {
        results += result(tracked, HazardType.BLIND_SPOT_OCCUPIED, RiskLevel.HIGH, "Vehicle persisted in left danger zone.", AlertDirection.BLIND_SPOT_LEFT)
      }
      if (state.rightFrames >= thresholds.persistenceFrames) {
        results += result(tracked, HazardType.BLIND_SPOT_OCCUPIED, RiskLevel.HIGH, "Vehicle persisted in right danger zone.", AlertDirection.BLIND_SPOT_RIGHT)
      }
      if (state.blindSpotFrames >= thresholds.persistenceFrames) {
        results += result(tracked, HazardType.BLIND_SPOT_OCCUPIED, RiskLevel.HIGH, "Vehicle remained in a side blind spot for ${state.blindSpotFrames} frames.", if (left) AlertDirection.BLIND_SPOT_LEFT else AlertDirection.BLIND_SPOT_RIGHT)
      }

      state.followingFrames = if (growth >= thresholds.followingDistanceGrowthRatio) state.followingFrames + 1 else 0
      if (state.followingFrames >= thresholds.persistenceFrames) {
        results += result(tracked, HazardType.UNSAFE_FOLLOWING_DISTANCE, RiskLevel.MEDIUM, "Relative bounding-box growth exceeded following-distance threshold.", AlertDirection.AHEAD)
      }
    } else if (tracked.objectType == DetectedObjectType.PEDESTRIAN && tracked.source == CameraSource.FRONT) {
      val inPath = tracked.currentPosition.x in thresholds.pathCenterMin..thresholds.pathCenterMax
      state.pedestrianPathFrames = if (inPath && growth >= thresholds.pathEntryGrowthRatio) state.pedestrianPathFrames + 1 else 0
      if (state.pedestrianPathFrames >= thresholds.persistenceFrames) {
        results += result(tracked, HazardType.PEDESTRIAN_CROSSING, RiskLevel.MEDIUM, "Pedestrian entered the center path while its relative box grew.", AlertDirection.FRONT_CENTER)
      }
    }
    return results.distinctBy { it.hazardType to it.trackId }
  }

  private fun result(tracked: TrackedObject, type: HazardType, severity: RiskLevel, reason: String, direction: AlertDirection = defaultDirection(tracked)) = HazardResult(
    hazardType = type, severity = severity, trackId = tracked.trackId, objectType = tracked.objectType,
    source = tracked.source, confidence = tracked.confidence, timestampMillis = tracked.timestampMillis,
    frameSequence = tracked.frameSequence, reason = reason
  )

  private companion object {
    fun defaultDirection(tracked: TrackedObject) = when {
      tracked.source == CameraSource.REAR -> AlertDirection.REAR_RIGHT
      tracked.currentPosition.x < 0.35f -> AlertDirection.BLIND_SPOT_LEFT
      tracked.currentPosition.x > 0.65f -> AlertDirection.BLIND_SPOT_RIGHT
      else -> AlertDirection.AHEAD
    }
  }
}

private fun com.example.detection.NormalizedBoundingBox.area() = max(0f, right - left) * max(0f, bottom - top)