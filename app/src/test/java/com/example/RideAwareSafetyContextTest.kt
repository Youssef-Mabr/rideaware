package com.example

import android.app.Application
import com.example.ai.ContextFreshness
import com.example.ai.GeminiAssistantImpl
import com.example.ai.GeminiLiveVoiceAssistantImpl
import com.example.ai.RideAwareContextProvider
import com.example.camera.CameraAnalysis
import com.example.camera.CameraDetectionPipeline
import com.example.camera.CameraFrame
import com.example.camera.SimulatedCameraFrameSource
import com.example.camera.SimulationScenario
import com.example.detection.DetectedObjectType
import com.example.detection.DetectionResult
import com.example.detection.NormalizedBoundingBox
import com.example.detection.SimulatedYoloDetector
import com.example.model.AlertDirection
import com.example.model.CameraSource
import com.example.model.HazardType
import com.example.model.RiskLevel
import com.example.model.SafetyAlert
import com.example.safety.AlertSeverity as SafetyAlertSeverity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class RideAwareSafetyContextTest {

  @Test
  fun buildsRearLeftMotorcycleContextWithTrackDirectionAndCriticalSeverity() {
    val provider = RideAwareContextProvider(staleThresholdMillis = 5_000L)
    val pipeline = CameraDetectionPipeline(
      frameSource = SimulatedCameraFrameSource(
        source = CameraSource.REAR,
        scenario = SimulationScenario.LEFT_BLIND_SPOT_VEHICLE
      ),
      detector = SimulatedYoloDetector()
    )

    // Run 3 frames so tracker and hazard evaluator establish persistence
    var latestAnalysis: CameraAnalysis? = null
    for (step in 1..3) {
      val now = step * 1_000L
      val analysis = pipeline.analyzeNextFrame(now)
      latestAnalysis = analysis
      provider.recordCameraAnalysis(analysis, isSimulated = true, nowMillis = now)
    }

    assertNotNull(latestAnalysis)
    val snapshot = provider.snapshot(nowMillis = 3_200L)
    assertEquals(ContextFreshness.AVAILABLE, snapshot.rearCamera.freshness)
    assertTrue(snapshot.rearCamera.isSimulated)

    val obj = snapshot.rearCamera.objects.single()
    assertEquals(DetectedObjectType.MOTORCYCLE, obj.detectionClass)
    assertEquals(AlertDirection.REAR_LEFT, obj.direction)
    assertNotNull(obj.trackId)
    assertTrue(obj.isSimulated)

    val hazard = snapshot.rearCamera.hazards.first()
    assertEquals(AlertDirection.REAR_LEFT, hazard.direction)
    assertEquals(RiskLevel.HIGH, hazard.severity)
    assertEquals(SafetyAlertSeverity.CRITICAL, hazard.alertSeverity)

    val promptContext = provider.buildPromptContext(nowMillis = 3_200L)
    assertTrue(promptContext.contains("motorcycle"))
    assertTrue(promptContext.contains("rear-left"))
    assertTrue(promptContext.contains("REAR_LEFT"))
    assertTrue(promptContext.contains("CRITICAL"))
    assertTrue(promptContext.contains("SIMULATED/TEST DATA"))
  }

  @Test
  fun missingAndStaleCameraDataAreReportedTruthfully() {
    val provider = RideAwareContextProvider(staleThresholdMillis = 5_000L)

    // Initially unavailable
    val initialSnap = provider.snapshot(nowMillis = 1_000L)
    assertEquals(ContextFreshness.UNAVAILABLE, initialSnap.rearCamera.freshness)
    assertEquals(ContextFreshness.UNAVAILABLE, initialSnap.frontCamera.freshness)
    val unavailablePrompt = provider.buildPromptContext(nowMillis = 1_000L)
    assertTrue(unavailablePrompt.contains("REAR CAMERA (behind the rider): STATUS=UNAVAILABLE"))
    assertTrue(unavailablePrompt.contains("cannot confirm what is behind the rider"))

    // Record rear detection at t = 2_000L
    val pipeline = CameraDetectionPipeline(
      frameSource = SimulatedCameraFrameSource(CameraSource.REAR),
      detector = SimulatedYoloDetector()
    )
    provider.recordCameraAnalysis(pipeline.analyzeNextFrame(2_000L), isSimulated = true, nowMillis = 2_000L)
    assertEquals(ContextFreshness.AVAILABLE, provider.snapshot(nowMillis = 4_000L).rearCamera.freshness)

    // After staleThresholdMillis (2_000L + 5_001L = 7_001L), rear camera becomes STALE and clears stale objects
    val staleSnap = provider.snapshot(nowMillis = 8_000L)
    assertEquals(ContextFreshness.STALE, staleSnap.rearCamera.freshness)
    assertTrue(staleSnap.rearCamera.objects.isEmpty())
    assertTrue(staleSnap.rearCamera.hazards.isEmpty())
    val stalePrompt = provider.buildPromptContext(nowMillis = 8_000L)
    assertTrue(stalePrompt.contains("REAR CAMERA (behind the rider): STATUS=STALE"))
  }

  @Test
  fun distinguishesSimulatedFromRealCameraData() {
    val provider = RideAwareContextProvider()
    val frame = CameraFrame(
      source = CameraSource.FRONT,
      capturedAtMillis = 1_000L,
      sequence = 1L,
      scenario = SimulationScenario.EMPTY_ROAD,
      bitmap = null
    )
    val detection = DetectionResult(
      objectType = DetectedObjectType.CAR,
      confidence = 0.89f,
      boundingBox = NormalizedBoundingBox(0.40f, 0.30f, 0.60f, 0.60f),
      timestampMillis = 1_000L,
      source = CameraSource.FRONT,
      frameSequence = 1L
    )
    val analysis = CameraAnalysis(
      frame = frame,
      detections = listOf(detection),
      safetyEvents = emptyList()
    )

    provider.recordCameraAnalysis(analysis, isSimulated = true, nowMillis = 1_000L)
    assertTrue(provider.snapshot(1_100L).frontCamera.isSimulated)
    assertTrue(provider.buildPromptContext(1_100L).contains("DATA_SOURCE=SIMULATED/TEST DATA"))

    provider.recordCameraAnalysis(analysis, isSimulated = false, nowMillis = 1_200L)
    assertFalse(provider.snapshot(1_300L).frontCamera.isSimulated)
    assertTrue(provider.buildPromptContext(1_300L).contains("DATA_SOURCE=REAL CAMERA DATA"))
  }

  @Test
  fun preventsUnsupportedSpeedAndDistanceClaimsInContext() {
    val provider = RideAwareContextProvider()
    val alertWithFakeNumbers = SafetyAlert(
      id = "alt-1",
      title = "Vehicle approaching rear-right",
      description = "Closing speed +28 km/h at 24 meters.",
      riskLevel = RiskLevel.HIGH,
      direction = AlertDirection.REAR_RIGHT,
      timestamp = "08:16:32",
      voiceWarning = "Caution: Fast vehicle approaching rear right",
      hazardType = HazardType.VEHICLE_APPROACHING_FAST
    )
    provider.recordActiveAlert(alertWithFakeNumbers, nowMillis = 2_000L, isSimulated = true)

    val contextText = provider.buildPromptContext(nowMillis = 2_100L)
    // Unmeasured description strings with fake speeds/distances must not be injected as facts
    assertFalse(contextText.contains("+28 km/h"))
    assertFalse(contextText.contains("24 meters"))
    // Authoritative alert fields ARE included
    assertTrue(contextText.contains("VEHICLE_APPROACHING_FAST"))
    assertTrue(contextText.contains("CRITICAL"))
    assertTrue(contextText.contains("REAR_RIGHT"))
    assertTrue(contextText.contains("08:16:32"))
    // Sanitizer strips unmeasured units
    val sanitized = RideAwareContextProvider.sanitizeReason("Approaching at 55 km/h from 18 meters")
    assertFalse(sanitized.contains("55 km/h"))
    assertFalse(sanitized.contains("18 meters"))
    assertTrue(sanitized.contains("[unmeasured]"))
  }

  @Test
  fun stage5aAndStage5bBothUseSharedRideAwareContextProvider() {
    val provider = RideAwareContextProvider(clock = { 3_500L })
    val pipeline = CameraDetectionPipeline(
      frameSource = SimulatedCameraFrameSource(
        source = CameraSource.REAR,
        scenario = SimulationScenario.LEFT_BLIND_SPOT_VEHICLE
      ),
      detector = SimulatedYoloDetector()
    )
    repeat(3) { step ->
      val t = (step + 1) * 1_000L
      provider.recordCameraAnalysis(pipeline.analyzeNextFrame(t), isSimulated = true, nowMillis = t)
    }

    // Stage 5A text assistant (gemini-3.5-flash-lite)
    val textAssistant = GeminiAssistantImpl(apiKey = "test-key", contextProvider = provider)
    assertEquals("gemini-3.5-flash-lite", GeminiAssistantImpl.MODEL)
    val contextualInput = textAssistant.buildContextualInput("What is behind me?", nowMillis = 3_500L)
    assertTrue(contextualInput.contains("motorcycle"))
    assertTrue(contextualInput.contains("rear-left"))
    assertTrue(contextualInput.contains("Rider question: What is behind me?"))

    // Stage 5B/5C voice assistant (gemini-2.5-flash-native-audio-preview-12-2025)
    val voiceAssistant = GeminiLiveVoiceAssistantImpl(apiKey = "test-key", contextProvider = provider)
    assertEquals("gemini-2.5-flash-native-audio-preview-12-2025", GeminiLiveVoiceAssistantImpl.MODEL)
    voiceAssistant.handleServerMessage(
      """{"toolCall":{"functionCalls":[{"id":"call-42","name":"get_rideaware_safety_context","args":{}}]}}"""
    )
    val toolResponseJson = JSONObject(voiceAssistant.sentClientMessages.last())
    val fnResponse = toolResponseJson
      .getJSONObject("toolResponse")
      .getJSONArray("functionResponses")
      .getJSONObject(0)
    assertEquals("call-42", fnResponse.getString("id"))
    assertEquals("get_rideaware_safety_context", fnResponse.getString("name"))
    val safetyContextResult = fnResponse
      .getJSONObject("response")
      .getJSONObject("result")
      .getString("safety_context")
    assertTrue(safetyContextResult.contains("motorcycle"))
    assertTrue(safetyContextResult.contains("rear-left"))
  }

  @Test
  fun voiceAssistantAvoidsRedundantContextUpdatesAndDeduplicatesSafetyAlerts() {
    var currentTime = 1_000L
    val provider = RideAwareContextProvider(clock = { currentTime })
    val voiceAssistant = GeminiLiveVoiceAssistantImpl(apiKey = "test-key", contextProvider = provider)
    voiceAssistant.handleServerMessage("""{"setupComplete":{}}""")

    val pipeline = CameraDetectionPipeline(
      frameSource = SimulatedCameraFrameSource(CameraSource.REAR, SimulationScenario.LEFT_BLIND_SPOT_VEHICLE),
      detector = SimulatedYoloDetector()
    )
    provider.recordCameraAnalysis(pipeline.analyzeNextFrame(1_000L), isSimulated = true, nowMillis = 1_000L)

    // First push succeeds because context signature changed
    currentTime = 3_000L
    assertTrue(voiceAssistant.pushLatestContextIfChanged(force = false, nowMillis = 3_000L))
    val countAfterFirst = voiceAssistant.sentClientMessages.size

    // Calling again with identical context does NOT send redundant messages
    currentTime = 4_800L
    assertFalse(voiceAssistant.pushLatestContextIfChanged(force = false, nowMillis = 4_800L))
    assertEquals(countAfterFirst, voiceAssistant.sentClientMessages.size)

    // Deduplicate repeated safety alert announcements within cooldown
    val alert = SafetyAlert(
      id = "alt-rear-left",
      title = "Motorcycle in blind spot",
      description = "Two-wheeler detected in left mirror blind zone.",
      riskLevel = RiskLevel.HIGH,
      direction = AlertDirection.REAR_LEFT,
      timestamp = "08:21:10",
      voiceWarning = "Alert: Motorcycle in your rear left",
      hazardType = HazardType.BLIND_SPOT_OCCUPIED
    )
    assertTrue(voiceAssistant.announceSafetyAlert(alert, isSimulated = true))
    assertFalse(voiceAssistant.announceSafetyAlert(alert, isSimulated = true))
  }
}
