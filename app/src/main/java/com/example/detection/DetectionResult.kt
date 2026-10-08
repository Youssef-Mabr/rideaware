package com.example.detection

import com.example.camera.CameraFrame

data class NormalizedBoundingBox(
  val left: Float,
  val top: Float,
  val right: Float,
  val bottom: Float
) {
  init {
    require(left in 0f..1f && top in 0f..1f)
    require(right in 0f..1f && bottom in 0f..1f)
    require(left < right && top < bottom)
  }
}

data class DetectionResult(
  val objectType: DetectedObjectType,
  val confidence: Float,
  val boundingBox: NormalizedBoundingBox,
  val timestampMillis: Long,
  val source: com.example.model.CameraSource,
  val frameSequence: Long
) {
  init { require(confidence in 0f..1f) }
}

enum class DetectedObjectType {
  CAR,
  MOTORCYCLE,
  BICYCLE,
  PEDESTRIAN,
  TRAFFIC_LIGHT
}

interface ObjectDetector {
  fun detect(frame: CameraFrame): List<DetectionResult>
}

data class YoloDetectionCandidate(
  val centerX: Float,
  val centerY: Float,
  val width: Float,
  val height: Float,
  val classIndex: Int,
  val confidence: Float
)

object YoloPostprocessor {
  const val PERSON_CLASS = 0
  const val CAR_CLASS = 2
  const val MOTORCYCLE_CLASS = 3

  fun candidatesToResults(
    candidates: List<YoloDetectionCandidate>,
    timestampMillis: Long,
    source: com.example.model.CameraSource,
    frameSequence: Long,
    confidenceThreshold: Float = 0.25f,
    iouThreshold: Float = 0.45f
  ): List<DetectionResult> {
    val allowed = setOf(PERSON_CLASS, CAR_CLASS, MOTORCYCLE_CLASS)
    val kept = candidates
      .filter { it.classIndex in allowed && it.confidence >= confidenceThreshold }
      .sortedByDescending { it.confidence }
      .fold(mutableListOf<YoloDetectionCandidate>()) { output, candidate ->
        if (output.none { it.classIndex == candidate.classIndex && iou(it, candidate) >= iouThreshold }) {
          output += candidate
        }
        output
      }
    return kept.mapNotNull { candidate ->
      val left = (candidate.centerX - candidate.width / 2f).coerceIn(0f, 1f)
      val top = (candidate.centerY - candidate.height / 2f).coerceIn(0f, 1f)
      val right = (candidate.centerX + candidate.width / 2f).coerceIn(0f, 1f)
      val bottom = (candidate.centerY + candidate.height / 2f).coerceIn(0f, 1f)
      if (left >= right || top >= bottom) return@mapNotNull null
      DetectionResult(
        objectType = when (candidate.classIndex) {
          PERSON_CLASS -> DetectedObjectType.PEDESTRIAN
          CAR_CLASS -> DetectedObjectType.CAR
          MOTORCYCLE_CLASS -> DetectedObjectType.MOTORCYCLE
          else -> return@mapNotNull null
        },
        confidence = candidate.confidence,
        boundingBox = NormalizedBoundingBox(left, top, right, bottom),
        timestampMillis = timestampMillis,
        source = source,
        frameSequence = frameSequence
      )
    }
  }

  private fun iou(first: YoloDetectionCandidate, second: YoloDetectionCandidate): Float {
    val left = maxOf(first.centerX - first.width / 2f, second.centerX - second.width / 2f)
    val top = maxOf(first.centerY - first.height / 2f, second.centerY - second.height / 2f)
    val right = minOf(first.centerX + first.width / 2f, second.centerX + second.width / 2f)
    val bottom = minOf(first.centerY + first.height / 2f, second.centerY + second.height / 2f)
    val intersection = ((right - left).coerceAtLeast(0f) * (bottom - top).coerceAtLeast(0f))
    val union = first.width * first.height + second.width * second.height - intersection
    return if (union <= 0f) 0f else intersection / union
  }
}