package com.example

import com.example.camera.CameraDetectionPipeline
import com.example.camera.SimulatedCameraFrameSource
import com.example.camera.SimulationScenario
import com.example.detection.DetectedObjectType
import com.example.detection.SimulatedYoloDetector
import com.example.detection.YoloDetectionCandidate
import com.example.detection.YoloImagePreprocessor
import com.example.detection.YoloPostprocessor
import com.example.model.CameraSource
import com.example.model.HazardType
import com.example.safety.SafetyEventEvaluator
import com.example.safety.HazardEvaluator
import com.example.safety.HazardThresholds
import com.example.safety.AlertSeverity
import com.example.safety.HazardResult
import com.example.safety.SafetyAlertManager
import com.example.safety.SafetyAlertPolicy
import com.example.tracking.MovementDirection
import com.example.tracking.TemporalObjectTracker
import com.example.tracking.TrackedObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraDetectionSafetyTest {
  @Test fun simulatedRearFrameProducesTypedVehicleDetection() {
    val frame = SimulatedCameraFrameSource(CameraSource.REAR).nextFrame(1000L)
    val detection = SimulatedYoloDetector().detect(frame).single()

    assertEquals(DetectedObjectType.CAR, detection.objectType)
    assertEquals(1000L, detection.timestampMillis)
    assertTrue(detection.boundingBox.right <= 1f)
  }

  @Test fun simulationFeedsPedestrianDetectionIntoSafetyEventLayer() {
    val frame = SimulatedCameraFrameSource(
      CameraSource.FRONT,
      SimulationScenario.PEDESTRIAN_CROSSING
    ).nextFrame(2000L)
    val detection = SimulatedYoloDetector().detect(frame)
    val events = SafetyEventEvaluator().evaluate(detection)

    assertEquals(HazardType.PEDESTRIAN_CROSSING, events.single().hazardType)
    assertEquals(2000L, events.single().createdAtMillis)
  }

  @Test fun emptySimulationFrameProducesNoDetectionOrSafetyEvent() {
    val frame = SimulatedCameraFrameSource(
      CameraSource.FRONT,
      SimulationScenario.EMPTY_ROAD
    ).nextFrame(3000L)

    assertTrue(SimulatedYoloDetector().detect(frame).isEmpty())
    assertTrue(SafetyEventEvaluator().evaluate(emptyList()).isEmpty())
  }

  @Test fun pipelineConnectsSimulationDetectorAndSafetyLayers() {
    val analysis = CameraDetectionPipeline(
      frameSource = SimulatedCameraFrameSource(CameraSource.REAR),
      detector = SimulatedYoloDetector()
    ).analyzeNextFrame(4000L)

    assertEquals(1, analysis.detections.size)
    assertEquals(HazardType.VEHICLE_APPROACHING_FAST, analysis.safetyEvents.single().hazardType)
  }

  @Test fun yoloPostprocessorMapsPersonCarAndMotorcycleToContracts() {
    val results = YoloPostprocessor.candidatesToResults(
      candidates = listOf(
        YoloDetectionCandidate(0.2f, 0.3f, 0.2f, 0.3f, 0, 0.91f),
        YoloDetectionCandidate(0.5f, 0.4f, 0.3f, 0.3f, 2, 0.88f),
        YoloDetectionCandidate(0.8f, 0.5f, 0.2f, 0.2f, 3, 0.79f)
      ),
      timestampMillis = 5000L,
      source = CameraSource.FRONT,
      frameSequence = 7L
    )

    assertEquals(
      setOf(DetectedObjectType.PEDESTRIAN, DetectedObjectType.CAR, DetectedObjectType.MOTORCYCLE),
      results.map { it.objectType }.toSet()
    )
    assertTrue(results.all { it.confidence >= 0.79f && it.boundingBox.left in 0f..1f })
  }

  @Test fun sameCarAcrossFramesKeepsTrackIdAndReportsMovement() {
    val tracker = TemporalObjectTracker()
    val first = YoloPostprocessor.candidatesToResults(
      listOf(YoloDetectionCandidate(0.40f, 0.50f, 0.20f, 0.20f, 2, 0.90f)),
      1000L, CameraSource.FRONT, 1L
    )
    val second = YoloPostprocessor.candidatesToResults(
      listOf(YoloDetectionCandidate(0.50f, 0.50f, 0.20f, 0.20f, 2, 0.88f)),
      1100L, CameraSource.FRONT, 2L
    )

    val firstTrack = tracker.update(first).single()
    val secondTrack = tracker.update(second).single()

    assertEquals(firstTrack.trackId, secondTrack.trackId)
    assertEquals(MovementDirection.RIGHT, secondTrack.movementDirection)
    assertEquals(2L, secondTrack.frameSequence)
    assertTrue(secondTrack.previousPosition != null)
  }

  @Test fun hazardRulesRequirePersistenceAndDetectApproachRearSideBlindSpotAndFollowing() {
    val evaluator = HazardEvaluator(HazardThresholds(persistenceFrames = 2))
    val results = listOf(0.20f, 0.30f, 0.45f).flatMapIndexed { index, size ->
      evaluator.evaluate(listOf(tracked(7L, DetectedObjectType.CAR, CameraSource.REAR, 0.75f, size, index.toLong())))
    }

    assertTrue(results.any { it.hazardType == HazardType.VEHICLE_APPROACHING_FAST })
    assertTrue(results.any { it.source == CameraSource.REAR })

    val sideEvaluator = HazardEvaluator(HazardThresholds(persistenceFrames = 2))
    val sideResults = (0L..1L).flatMap { frame ->
      sideEvaluator.evaluate(listOf(tracked(8L, DetectedObjectType.MOTORCYCLE, CameraSource.FRONT, 0.15f, 0.55f, frame)))
    }
    assertTrue(sideResults.any { it.hazardType == HazardType.BLIND_SPOT_OCCUPIED })
    assertTrue(sideResults.any { it.reason.contains("left") })
  }

  @Test fun pedestrianPathEntryAndFollowingDistanceUseRelativeGrowth() {
    val evaluator = HazardEvaluator(HazardThresholds(persistenceFrames = 2))
    val pedestrianResults = (0L..2L).flatMap { frame ->
      evaluator.evaluate(listOf(tracked(9L, DetectedObjectType.PEDESTRIAN, CameraSource.FRONT, 0.50f, 0.10f + frame * 0.03f, frame)))
    }
    assertTrue(pedestrianResults.any { it.hazardType == HazardType.PEDESTRIAN_CROSSING })

    val followingEvaluator = HazardEvaluator(HazardThresholds(persistenceFrames = 2))
    val followingResults = listOf(0.10f, 0.14f, 0.20f).flatMapIndexed { frame, size ->
      followingEvaluator.evaluate(listOf(tracked(10L, DetectedObjectType.CAR, CameraSource.FRONT, 0.50f, size, frame.toLong())))
    }
    assertTrue(followingResults.any { it.hazardType == HazardType.UNSAFE_FOLLOWING_DISTANCE })
  }

  @Test fun oneNoisyFrameDoesNotCreateHazard() {
    val evaluator = HazardEvaluator(HazardThresholds(persistenceFrames = 3))
    val result = evaluator.evaluate(listOf(tracked(11L, DetectedObjectType.CAR, CameraSource.REAR, 0.80f, 0.80f, 1L)))
    assertTrue(result.isEmpty())
  }

  @Test fun safetyAlertsMapSeverityAndDebounceDuplicatesUntilReset() {
    val manager = SafetyAlertManager(SafetyAlertPolicy(cooldownMillis = 500L))
    val hazard = hazard(HazardType.VEHICLE_APPROACHING_FAST, 10L, 1L)

    val first = manager.onHazards(listOf(hazard)).singleOrNull()
    val duplicate = manager.onHazards(listOf(hazard.copy(timestampMillis = 100L, frameSequence = 2L))).singleOrNull()
    val afterDisappear = manager.onHazards(emptyList())
    val returned = manager.onHazards(listOf(hazard.copy(timestampMillis = 700L, frameSequence = 3L))).singleOrNull()

    assertEquals(com.example.model.RiskLevel.HIGH, first?.riskLevel)
    assertEquals(null, duplicate)
    assertTrue(afterDisappear.isEmpty())
    assertEquals("Vehicle approaching", returned?.title)
  }

  @Test fun differentHazardsCanCreateIndependentAlerts() {
    val manager = SafetyAlertManager()
    val alerts = manager.onHazards(listOf(
      hazard(HazardType.PEDESTRIAN_CROSSING, 1L, 1L),
      hazard(HazardType.BLIND_SPOT_OCCUPIED, 2L, 1L)
    ))

    assertEquals(AlertSeverity.CRITICAL, SafetyAlertPolicy().severityByHazard[HazardType.BLIND_SPOT_OCCUPIED])
    assertEquals(2, alerts.size)
    assertEquals("Blind spot warning", alerts.first().title)
  }

  private fun hazard(type: HazardType, trackId: Long, timestamp: Long) = HazardResult(
    hazardType = type,
    severity = com.example.model.RiskLevel.HIGH,
    trackId = trackId,
    objectType = if (type == HazardType.PEDESTRIAN_CROSSING) DetectedObjectType.PEDESTRIAN else DetectedObjectType.CAR,
    source = if (type == HazardType.BLIND_SPOT_OCCUPIED) CameraSource.REAR else CameraSource.FRONT,
    confidence = 0.9f,
    timestampMillis = timestamp,
    frameSequence = timestamp,
    reason = "test hazard"
  )

  private fun tracked(trackId: Long, type: DetectedObjectType, source: CameraSource, centerX: Float, size: Float, frame: Long) = TrackedObject(
    trackId = trackId,
    objectType = type,
    source = source,
    confidence = 0.9f,
    boundingBox = com.example.detection.NormalizedBoundingBox(
      (centerX - size / 2f).coerceIn(0.01f, 0.99f - size),
      0.45f - size / 2f,
      (centerX + size / 2f).coerceIn(size + 0.01f, 0.99f),
      0.45f + size / 2f
    ),
    timestampMillis = frame * 100L,
    frameSequence = frame,
    previousPosition = null,
    currentPosition = com.example.tracking.NormalizedPoint(centerX, 0.45f),
    movementDirection = MovementDirection.STATIONARY
  )

}