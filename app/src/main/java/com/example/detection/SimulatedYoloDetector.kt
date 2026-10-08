package com.example.detection

import com.example.camera.CameraFrame
import com.example.camera.SimulationScenario

class SimulatedYoloDetector : ObjectDetector {
  override fun detect(frame: CameraFrame): List<DetectionResult> {
    val objectType = when (frame.scenario) {
      SimulationScenario.PEDESTRIAN_CROSSING -> DetectedObjectType.PEDESTRIAN
      SimulationScenario.LEFT_BLIND_SPOT_VEHICLE -> DetectedObjectType.MOTORCYCLE
      SimulationScenario.EMPTY_ROAD -> return emptyList()
      else -> DetectedObjectType.CAR
    }
    val box = when (frame.scenario) {
      SimulationScenario.REAR_APPROACHING_VEHICLE -> NormalizedBoundingBox(0.48f, 0.35f, 0.86f, 0.72f)
      SimulationScenario.LEFT_BLIND_SPOT_VEHICLE -> NormalizedBoundingBox(0.08f, 0.42f, 0.36f, 0.72f)
      SimulationScenario.PEDESTRIAN_CROSSING -> NormalizedBoundingBox(0.42f, 0.28f, 0.58f, 0.68f)
      else -> NormalizedBoundingBox(0.36f, 0.30f, 0.68f, 0.58f)
    }
    return listOf(DetectionResult(objectType, 0.92f, box, frame.capturedAtMillis, frame.source, frame.sequence))
  }
}