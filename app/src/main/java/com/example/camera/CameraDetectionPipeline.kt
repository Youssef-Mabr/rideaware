package com.example.camera

import com.example.detection.DetectionResult
import com.example.detection.ObjectDetector
import com.example.safety.SafetyEvent
import com.example.safety.SafetyEventEvaluator
import com.example.safety.HazardEvaluator
import com.example.safety.HazardResult
import com.example.tracking.TemporalObjectTracker
import com.example.tracking.TrackedObject

data class CameraAnalysis(
  val frame: CameraFrame,
  val detections: List<DetectionResult>,
  val safetyEvents: List<SafetyEvent>,
  val trackedObjects: List<TrackedObject> = emptyList(),
  val hazards: List<HazardResult> = emptyList()
)

class CameraDetectionPipeline(
  private val frameSource: CameraFrameSource,
  private val detector: ObjectDetector,
  private val safetyEvaluator: SafetyEventEvaluator = SafetyEventEvaluator(),
  private val tracker: TemporalObjectTracker = TemporalObjectTracker(),
  private val hazardEvaluator: HazardEvaluator = HazardEvaluator()
) {
  fun analyzeNextFrame(nowMillis: Long = System.currentTimeMillis()): CameraAnalysis {
    val frame = frameSource.nextFrame(nowMillis)
    val detections = detector.detect(frame)
    val trackedObjects = tracker.update(detections)
    return CameraAnalysis(
      frame = frame,
      detections = detections,
      safetyEvents = safetyEvaluator.evaluate(detections),
      trackedObjects = trackedObjects,
      hazards = hazardEvaluator.evaluate(trackedObjects)
    )
  }
}