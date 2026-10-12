package com.example.ai

import com.example.camera.CameraAnalysis
import com.example.camera.SimulationScenario
import com.example.detection.DetectedObjectType
import com.example.detection.NormalizedBoundingBox
import com.example.model.AlertDirection
import com.example.model.CameraSource
import com.example.model.HazardType
import com.example.model.HelmetStatus
import com.example.model.RiskLevel
import com.example.model.SafetyAlert
import com.example.safety.AlertSeverity
import com.example.safety.defaultSeverityByHazard
import com.example.tracking.MovementDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ContextFreshness {
  AVAILABLE,
  STALE,
  UNAVAILABLE
}

data class ObservedObjectContext(
  val cameraSource: CameraSource,
  val detectionClass: DetectedObjectType,
  val confidence: Float,
  val trackId: Long?,
  val movementDirection: MovementDirection?,
  val direction: AlertDirection,
  val timestampMillis: Long,
  val isSimulated: Boolean
)

data class ObservedHazardContext(
  val hazardType: HazardType,
  val severity: RiskLevel,
  val alertSeverity: AlertSeverity,
  val cameraSource: CameraSource,
  val detectionClass: DetectedObjectType,
  val trackId: Long?,
  val direction: AlertDirection,
  val timestampMillis: Long,
  val reason: String,
  val isSimulated: Boolean
)

data class ActiveAlertContext(
  val id: String,
  val title: String,
  val hazardType: HazardType,
  val riskLevel: RiskLevel,
  val alertSeverity: AlertSeverity,
  val direction: AlertDirection,
  val timestamp: String,
  val voiceWarning: String,
  val recordedAtMillis: Long,
  val isSimulated: Boolean
)

data class CameraSourceContext(
  val source: CameraSource,
  val freshness: ContextFreshness,
  val isSimulated: Boolean,
  val capturedAtMillis: Long?,
  val ageMillis: Long?,
  val scenario: SimulationScenario?,
  val objects: List<ObservedObjectContext>,
  val hazards: List<ObservedHazardContext>
)

data class RideAwareSafetyContext(
  val generatedAtMillis: Long,
  val staleThresholdMillis: Long,
  val frontCamera: CameraSourceContext,
  val rearCamera: CameraSourceContext,
  val activeAlert: ActiveAlertContext?,
  val helmetConnected: Boolean? = null,
  val helmetBatteryPercent: Int? = null
)

class RideAwareContextProvider(
  private val staleThresholdMillis: Long = DEFAULT_STALE_THRESHOLD_MS,
  private val clock: () -> Long = { System.currentTimeMillis() }
) {
  private data class StoredCameraRecord(
    val source: CameraSource,
    val capturedAtMillis: Long,
    val isSimulated: Boolean,
    val scenario: SimulationScenario?,
    val objects: List<ObservedObjectContext>,
    val hazards: List<ObservedHazardContext>
  )

  private val lock = Any()
  private var frontRecord: StoredCameraRecord? = null
  private var rearRecord: StoredCameraRecord? = null
  private var activeAlertRecord: ActiveAlertContext? = null
  private var helmetConnected: Boolean? = null
  private var helmetBatteryPercent: Int? = null

  private val mutableUpdates = MutableStateFlow(0L)
  val updates: StateFlow<Long> = mutableUpdates.asStateFlow()

  fun recordCameraAnalysis(
    analysis: CameraAnalysis,
    isSimulated: Boolean = analysis.frame.bitmap == null,
    nowMillis: Long = analysis.frame.capturedAtMillis
  ) {
    val source = when (analysis.frame.source) {
      CameraSource.REAR -> CameraSource.REAR
      else -> CameraSource.FRONT
    }
    val tracksById = analysis.trackedObjects.associateBy { it.trackId }

    val objects: List<ObservedObjectContext> = if (analysis.trackedObjects.isNotEmpty()) {
      analysis.trackedObjects.map { tracked ->
        ObservedObjectContext(
          cameraSource = source,
          detectionClass = tracked.objectType,
          confidence = tracked.confidence,
          trackId = tracked.trackId,
          movementDirection = tracked.movementDirection,
          direction = inferDirection(
            source = source,
            objectType = tracked.objectType,
            box = tracked.boundingBox,
            scenario = analysis.frame.scenario
          ),
          timestampMillis = tracked.timestampMillis,
          isSimulated = isSimulated
        )
      }
    } else {
      analysis.detections.map { detection ->
        ObservedObjectContext(
          cameraSource = source,
          detectionClass = detection.objectType,
          confidence = detection.confidence,
          trackId = null,
          movementDirection = null,
          direction = inferDirection(
            source = source,
            objectType = detection.objectType,
            box = detection.boundingBox,
            scenario = analysis.frame.scenario
          ),
          timestampMillis = detection.timestampMillis,
          isSimulated = isSimulated
        )
      }
    }

    val severityMap = defaultSeverityByHazard()
    val hazardsFromEvaluator = analysis.hazards.map { hazard ->
      val matchingTrack = tracksById[hazard.trackId]
      val direction = inferHazardDirection(
        source = source,
        hazardType = hazard.hazardType,
        reason = hazard.reason,
        box = matchingTrack?.boundingBox,
        scenario = analysis.frame.scenario
      )
      ObservedHazardContext(
        hazardType = hazard.hazardType,
        severity = hazard.severity,
        alertSeverity = severityMap[hazard.hazardType] ?: riskToAlertSeverity(hazard.severity),
        cameraSource = source,
        detectionClass = hazard.objectType,
        trackId = hazard.trackId,
        direction = direction,
        timestampMillis = hazard.timestampMillis,
        reason = sanitizeReason(hazard.reason),
        isSimulated = isSimulated
      )
    }

    val hazardsFromEvents = analysis.safetyEvents
      .filter { event ->
        hazardsFromEvaluator.none {
          it.hazardType == event.hazardType && it.detectionClass == event.detection.objectType
        }
      }
      .map { event ->
        val matchingTrack = analysis.trackedObjects.firstOrNull {
          it.objectType == event.detection.objectType &&
            it.frameSequence == event.detection.frameSequence
        }
        val direction = inferEventDirection(
          source = source,
          defaultDirection = event.direction,
          box = event.detection.boundingBox,
          scenario = analysis.frame.scenario
        )
        ObservedHazardContext(
          hazardType = event.hazardType,
          severity = event.riskLevel,
          alertSeverity = severityMap[event.hazardType] ?: riskToAlertSeverity(event.riskLevel),
          cameraSource = source,
          detectionClass = event.detection.objectType,
          trackId = matchingTrack?.trackId,
          direction = direction,
          timestampMillis = event.createdAtMillis,
          reason = "Detected ${formatObjectType(event.detection.objectType)} (${formatHazardType(event.hazardType)})",
          isSimulated = isSimulated
        )
      }

    val record = StoredCameraRecord(
      source = source,
      capturedAtMillis = nowMillis,
      isSimulated = isSimulated,
      scenario = if (isSimulated) analysis.frame.scenario else null,
      objects = objects,
      hazards = (hazardsFromEvaluator + hazardsFromEvents).distinctBy {
        Triple(it.hazardType, it.detectionClass, it.direction)
      }
    )

    synchronized(lock) {
      if (source == CameraSource.REAR) {
        rearRecord = record
      } else {
        frontRecord = record
      }
    }
    notifyUpdated()
  }

  fun clearCameraSource(source: CameraSource) {
    synchronized(lock) {
      when (source) {
        CameraSource.REAR -> rearRecord = null
        CameraSource.FRONT -> frontRecord = null
        else -> {
          frontRecord = null
          rearRecord = null
        }
      }
    }
    notifyUpdated()
  }

  fun recordActiveAlert(
    alert: SafetyAlert?,
    nowMillis: Long = clock(),
    isSimulated: Boolean = true
  ) {
    synchronized(lock) {
      activeAlertRecord = alert?.let {
        val severityMap = defaultSeverityByHazard()
        ActiveAlertContext(
          id = it.id,
          title = it.title,
          hazardType = it.hazardType,
          riskLevel = it.riskLevel,
          alertSeverity = severityMap[it.hazardType] ?: riskToAlertSeverity(it.riskLevel),
          direction = it.direction,
          timestamp = it.timestamp,
          voiceWarning = it.voiceWarning,
          recordedAtMillis = nowMillis,
          isSimulated = isSimulated
        )
      }
    }
    notifyUpdated()
  }

  fun clearActiveAlert() {
    synchronized(lock) {
      activeAlertRecord = null
    }
    notifyUpdated()
  }

  fun updateHelmetStatus(status: HelmetStatus?) {
    synchronized(lock) {
      helmetConnected = status?.isConnected
      helmetBatteryPercent = status?.batteryPercent
    }
    notifyUpdated()
  }

  fun reset() {
    synchronized(lock) {
      frontRecord = null
      rearRecord = null
      activeAlertRecord = null
      helmetConnected = null
      helmetBatteryPercent = null
    }
    notifyUpdated()
  }

  fun snapshot(nowMillis: Long = clock()): RideAwareSafetyContext {
    val (front, rear, alert, connected, battery) = synchronized(lock) {
      Quint(frontRecord, rearRecord, activeAlertRecord, helmetConnected, helmetBatteryPercent)
    }
    return RideAwareSafetyContext(
      generatedAtMillis = nowMillis,
      staleThresholdMillis = staleThresholdMillis,
      frontCamera = buildCameraSourceContext(CameraSource.FRONT, front, nowMillis),
      rearCamera = buildCameraSourceContext(CameraSource.REAR, rear, nowMillis),
      activeAlert = alert,
      helmetConnected = connected,
      helmetBatteryPercent = battery
    )
  }

  fun contextSignature(nowMillis: Long = clock()): String {
    val snap = snapshot(nowMillis)
    return buildString {
      append("rear:")
      append(snap.rearCamera.freshness)
      append(":")
      append(snap.rearCamera.isSimulated)
      append(":")
      snap.rearCamera.objects.forEach {
        append("${it.detectionClass}@${it.direction}#${it.trackId ?: "-"}/${it.movementDirection ?: "-"};")
      }
      snap.rearCamera.hazards.forEach {
        append("${it.hazardType}:${it.severity}:${it.alertSeverity}@${it.direction};")
      }
      append("|front:")
      append(snap.frontCamera.freshness)
      append(":")
      append(snap.frontCamera.isSimulated)
      append(":")
      snap.frontCamera.objects.forEach {
        append("${it.detectionClass}@${it.direction}#${it.trackId ?: "-"}/${it.movementDirection ?: "-"};")
      }
      snap.frontCamera.hazards.forEach {
        append("${it.hazardType}:${it.severity}:${it.alertSeverity}@${it.direction};")
      }
      append("|alert:")
      val a = snap.activeAlert
      if (a != null) {
        append("${a.id}:${a.hazardType}:${a.riskLevel}:${a.alertSeverity}@${a.direction}:${a.isSimulated}")
      } else {
        append("none")
      }
    }
  }

  fun buildPromptContext(nowMillis: Long = clock()): String {
    val snap = snapshot(nowMillis)
    return buildString {
      appendLine("=== RIDEAWARE VERIFIED SAFETY CONTEXT ===")
      appendCameraSection("REAR CAMERA (behind the rider)", snap.rearCamera)
      appendCameraSection("FRONT CAMERA (ahead of the rider)", snap.frontCamera)
      val alert = snap.activeAlert
      if (alert != null) {
        val originLabel = if (alert.isSimulated) "SIMULATED/TEST ALERT" else "LIVE ALERT"
        appendLine(
          "ACTIVE SAFETY ALERT: [$originLabel] title=\"${alert.title}\", " +
            "hazardType=${alert.hazardType} (${formatHazardType(alert.hazardType)}), " +
            "severity=${alert.alertSeverity} (riskLevel=${alert.riskLevel}), " +
            "direction=${formatDirection(alert.direction)} (${alert.direction}), " +
            "timestamp=${alert.timestamp}, " +
            "voiceWarning=\"${alert.voiceWarning}\""
        )
      } else {
        appendLine("ACTIVE SAFETY ALERT: NONE")
      }
      if (snap.helmetConnected != null || snap.helmetBatteryPercent != null) {
        appendLine(
          "HELMET STATUS: connected=${snap.helmetConnected ?: "unknown"}, " +
            "battery=${snap.helmetBatteryPercent?.let { "$it%" } ?: "unknown"}"
        )
      }
      appendLine("GROUNDING & TRUTHFULNESS RULES:")
      appendLine("1. Use ONLY the verified RideAware detections, tracking directions, hazard types, and alert severities listed above.")
      appendLine("2. If REAR CAMERA status is UNAVAILABLE or STALE and the rider asks what is behind them, you MUST state that rear-camera data is currently unavailable or stale and you cannot confirm what is behind the rider.")
      appendLine("3. NEVER invent or guess exact distances (meters/feet), speeds (km/h or mph), vehicle colors, or lane openings—RideAware does not measure them.")
      appendLine("4. Existing hazard detection and SafetyAlert severity (CRITICAL/HIGH, WARNING/MEDIUM, INFO/LOW) are authoritative; never downplay or override them.")
      appendLine("5. Clearly distinguish SIMULATED/TEST data from real camera data when describing source status, and never claim live physical camera hardware when the source is marked SIMULATED.")
      append("6. Keep answers concise (1 to 2 short sentences) and safe for a motorcycle rider.")
    }
  }

  fun buildContextualUserPrompt(
    userPrompt: String,
    nowMillis: Long = clock()
  ): String {
    return buildString {
      appendLine(buildPromptContext(nowMillis))
      appendLine()
      append("Rider question: ")
      append(userPrompt.trim())
    }
  }

  fun buildCompactStatusLabel(nowMillis: Long = clock()): String {
    val snap = snapshot(nowMillis)
    val rearPart = when (snap.rearCamera.freshness) {
      ContextFreshness.UNAVAILABLE -> "Rear cam: No recent data"
      ContextFreshness.STALE -> "Rear cam: Stale"
      ContextFreshness.AVAILABLE -> {
        val origin = if (snap.rearCamera.isSimulated) "simulated" else "live"
        val firstHazard = snap.rearCamera.hazards.firstOrNull()
        val firstObj = snap.rearCamera.objects.firstOrNull()
        when {
          firstHazard != null ->
            "Rear cam ($origin): ${formatObjectType(firstHazard.detectionClass)} • ${formatDirection(firstHazard.direction)} • ${firstHazard.alertSeverity} (${firstHazard.severity})"
          firstObj != null ->
            "Rear cam ($origin): ${formatObjectType(firstObj.detectionClass)} • ${formatDirection(firstObj.direction)}"
          else -> "Rear cam ($origin): Clear (0 objects)"
        }
      }
    }
    val alertPart = snap.activeAlert?.let {
      " | Alert: ${it.alertSeverity} (${it.riskLevel}) ${formatDirection(it.direction)}"
    }.orEmpty()
    return rearPart + alertPart
  }

  private fun StringBuilder.appendCameraSection(title: String, camera: CameraSourceContext) {
    when (camera.freshness) {
      ContextFreshness.UNAVAILABLE -> {
        appendLine("$title: STATUS=UNAVAILABLE (no recent ${camera.source.name.lowercase()}-camera data).")
      }
      ContextFreshness.STALE -> {
        val ageSec = (camera.ageMillis ?: 0L) / 1000L
        appendLine(
          "$title: STATUS=STALE (last frame was ${ageSec}s ago, exceeding ${staleThresholdMillis / 1000L}s freshness window; cannot confirm current ${camera.source.name.lowercase()} view)."
        )
      }
      ContextFreshness.AVAILABLE -> {
        val origin = if (camera.isSimulated) "SIMULATED/TEST DATA" else "REAL CAMERA DATA"
        appendLine("$title: STATUS=AVAILABLE, DATA_SOURCE=$origin")
        if (camera.objects.isEmpty() && camera.hazards.isEmpty()) {
          appendLine("  - Detections: none")
        } else {
          camera.objects.forEach { obj ->
            val trackInfo = obj.trackId?.let { "trackId=$it" } ?: "trackId=untracked"
            val moveInfo = obj.movementDirection?.let { "movementDirection=$it" } ?: "movementDirection=unknown"
            appendLine(
              "  - Detected object: class=${formatObjectType(obj.detectionClass)} (${obj.detectionClass}), " +
                "direction=${formatDirection(obj.direction)} (${obj.direction}), " +
                "$trackInfo, $moveInfo, " +
                "simulated=${obj.isSimulated}"
            )
          }
          camera.hazards.forEach { hazard ->
            val trackInfo = hazard.trackId?.let { "trackId=$it" } ?: "trackId=untracked"
            appendLine(
              "  - Active hazard: type=${hazard.hazardType} (${formatHazardType(hazard.hazardType)}), " +
                "severity=${hazard.alertSeverity} (riskLevel=${hazard.severity}), " +
                "object=${formatObjectType(hazard.detectionClass)}, " +
                "direction=${formatDirection(hazard.direction)} (${hazard.direction}), " +
                "$trackInfo, reason=\"${hazard.reason}\", simulated=${hazard.isSimulated}"
            )
          }
        }
      }
    }
  }

  private fun buildCameraSourceContext(
    source: CameraSource,
    record: StoredCameraRecord?,
    nowMillis: Long
  ): CameraSourceContext {
    if (record == null) {
      return CameraSourceContext(
        source = source,
        freshness = ContextFreshness.UNAVAILABLE,
        isSimulated = false,
        capturedAtMillis = null,
        ageMillis = null,
        scenario = null,
        objects = emptyList(),
        hazards = emptyList()
      )
    }
    val age = (nowMillis - record.capturedAtMillis).coerceAtLeast(0L)
    val isStale = (nowMillis - record.capturedAtMillis) > staleThresholdMillis
    return if (isStale) {
      CameraSourceContext(
        source = source,
        freshness = ContextFreshness.STALE,
        isSimulated = record.isSimulated,
        capturedAtMillis = record.capturedAtMillis,
        ageMillis = age,
        scenario = record.scenario,
        objects = emptyList(),
        hazards = emptyList()
      )
    } else {
      CameraSourceContext(
        source = source,
        freshness = ContextFreshness.AVAILABLE,
        isSimulated = record.isSimulated,
        capturedAtMillis = record.capturedAtMillis,
        ageMillis = age,
        scenario = record.scenario,
        objects = record.objects,
        hazards = record.hazards
      )
    }
  }

  private fun notifyUpdated() {
    mutableUpdates.value = mutableUpdates.value + 1L
  }

  private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
  )

  companion object {
    const val DEFAULT_STALE_THRESHOLD_MS = 5_000L

    fun inferDirection(
      source: CameraSource,
      objectType: DetectedObjectType,
      box: NormalizedBoundingBox,
      scenario: SimulationScenario? = null
    ): AlertDirection {
      val centerX = (box.left + box.right) / 2f
      return if (source == CameraSource.REAR) {
        if (scenario == SimulationScenario.LEFT_BLIND_SPOT_VEHICLE || centerX < 0.40f) {
          AlertDirection.REAR_LEFT
        } else {
          AlertDirection.REAR_RIGHT
        }
      } else {
        when {
          scenario == SimulationScenario.LEFT_BLIND_SPOT_VEHICLE || centerX < 0.35f -> AlertDirection.BLIND_SPOT_LEFT
          centerX > 0.65f -> AlertDirection.BLIND_SPOT_RIGHT
          objectType == DetectedObjectType.PEDESTRIAN -> AlertDirection.FRONT_CENTER
          else -> AlertDirection.AHEAD
        }
      }
    }

    private fun inferHazardDirection(
      source: CameraSource,
      hazardType: HazardType,
      reason: String,
      box: NormalizedBoundingBox?,
      scenario: SimulationScenario?
    ): AlertDirection {
      val centerX = box?.let { (it.left + it.right) / 2f }
      if (source == CameraSource.REAR) {
        return if (
          reason.contains("left", ignoreCase = true) ||
          scenario == SimulationScenario.LEFT_BLIND_SPOT_VEHICLE ||
          (centerX != null && centerX < 0.40f)
        ) {
          AlertDirection.REAR_LEFT
        } else {
          AlertDirection.REAR_RIGHT
        }
      }
      return when (hazardType) {
        HazardType.BLIND_SPOT_OCCUPIED ->
          if (reason.contains("right", ignoreCase = true) || (centerX != null && centerX > 0.65f)) {
            AlertDirection.BLIND_SPOT_RIGHT
          } else {
            AlertDirection.BLIND_SPOT_LEFT
          }
        HazardType.PEDESTRIAN_CROSSING -> AlertDirection.FRONT_CENTER
        else -> AlertDirection.AHEAD
      }
    }

    private fun inferEventDirection(
      source: CameraSource,
      defaultDirection: AlertDirection,
      box: NormalizedBoundingBox,
      scenario: SimulationScenario?
    ): AlertDirection {
      val centerX = (box.left + box.right) / 2f
      if (source == CameraSource.REAR) {
        return if (scenario == SimulationScenario.LEFT_BLIND_SPOT_VEHICLE || centerX < 0.40f) {
          AlertDirection.REAR_LEFT
        } else {
          AlertDirection.REAR_RIGHT
        }
      }
      return defaultDirection
    }

    fun riskToAlertSeverity(riskLevel: RiskLevel): AlertSeverity = when (riskLevel) {
      RiskLevel.HIGH -> AlertSeverity.CRITICAL
      RiskLevel.MEDIUM -> AlertSeverity.WARNING
      RiskLevel.LOW -> AlertSeverity.INFO
    }

    fun formatObjectType(type: DetectedObjectType): String = when (type) {
      DetectedObjectType.CAR -> "car"
      DetectedObjectType.MOTORCYCLE -> "motorcycle"
      DetectedObjectType.BICYCLE -> "bicycle"
      DetectedObjectType.PEDESTRIAN -> "pedestrian"
      DetectedObjectType.TRAFFIC_LIGHT -> "traffic light"
    }

    fun formatDirection(direction: AlertDirection): String = when (direction) {
      AlertDirection.REAR_LEFT -> "rear-left"
      AlertDirection.REAR_RIGHT -> "rear-right"
      AlertDirection.FRONT_CENTER -> "front-center"
      AlertDirection.BLIND_SPOT_LEFT -> "left blind spot"
      AlertDirection.BLIND_SPOT_RIGHT -> "right blind spot"
      AlertDirection.AHEAD -> "ahead"
    }

    fun formatHazardType(type: HazardType): String = when (type) {
      HazardType.VEHICLE_APPROACHING_FAST -> "vehicle approaching fast"
      HazardType.BLIND_SPOT_OCCUPIED -> "blind spot occupied"
      HazardType.PEDESTRIAN_CROSSING -> "pedestrian crossing"
      HazardType.UNSAFE_FOLLOWING_DISTANCE -> "unsafe following distance"
      HazardType.TRAFFIC_LIGHT_CHANGE -> "traffic light change"
      HazardType.ROAD_HAZARD -> "road hazard"
    }

    internal fun sanitizeReason(rawReason: String): String {
      return rawReason
        .replace(Regex("\\b\\d+(\\.\\d+)?\\s*(km/h|mph|m/s|meters|meter|m)\\b", RegexOption.IGNORE_CASE), "[unmeasured]")
        .trim()
    }
  }
}

object RideAwareContextStore {
  val provider: RideAwareContextProvider = RideAwareContextProvider()
}
