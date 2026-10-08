package com.example.tracking

import com.example.detection.DetectedObjectType
import com.example.detection.DetectionResult
import com.example.detection.NormalizedBoundingBox
import com.example.model.CameraSource
import kotlin.math.hypot

data class NormalizedPoint(val x: Float, val y: Float)

enum class MovementDirection {
  STATIONARY,
  LEFT,
  RIGHT,
  UP,
  DOWN,
  UP_LEFT,
  UP_RIGHT,
  DOWN_LEFT,
  DOWN_RIGHT
}

data class TrackedObject(
  val trackId: Long,
  val objectType: DetectedObjectType,
  val source: CameraSource,
  val confidence: Float,
  val boundingBox: NormalizedBoundingBox,
  val timestampMillis: Long,
  val frameSequence: Long,
  val previousPosition: NormalizedPoint?,
  val currentPosition: NormalizedPoint,
  val movementDirection: MovementDirection
)

class TemporalObjectTracker(
  private val minimumIou: Float = 0.15f,
  private val maximumCenterDistance: Float = 0.20f,
  private val maxMissedFrames: Int = 2
) {
  private data class TrackState(
    val trackId: Long,
    var objectType: DetectedObjectType,
    var source: CameraSource,
    var confidence: Float,
    var boundingBox: NormalizedBoundingBox,
    var timestampMillis: Long,
    var frameSequence: Long,
    var previousPosition: NormalizedPoint?,
    var currentPosition: NormalizedPoint,
    var missedFrames: Int = 0
  )

  private val tracks = mutableListOf<TrackState>()
  private var nextTrackId = 1L

  fun update(detections: List<DetectionResult>): List<TrackedObject> {
    val unmatchedTracks = tracks.toMutableSet()
    val unmatchedDetections = detections.indices.toMutableSet()

    detections.indices
      .sortedByDescending { detections[it].confidence }
      .forEach { detectionIndex ->
        val detection = detections[detectionIndex]
        val match = unmatchedTracks
          .filter { it.objectType == detection.objectType }
          .map { it to matchScore(it, detection) }
          .filter { (_, score) -> score >= 0f }
          .maxByOrNull { (_, score) -> score }
          ?.first
        if (match != null) {
          unmatchedTracks.remove(match)
          unmatchedDetections.remove(detectionIndex)
          updateTrack(match, detection)
        }
      }

    unmatchedTracks.forEach { it.missedFrames++ }
    unmatchedDetections.forEach { index -> createTrack(detections[index]) }
    tracks.removeAll { it.missedFrames > maxMissedFrames }

    return tracks
      .filter { it.missedFrames == 0 }
      .map { it.toTrackedObject() }
  }

  fun reset() {
    tracks.clear()
    nextTrackId = 1L
  }

  private fun matchScore(track: TrackState, detection: DetectionResult): Float {
    val iou = intersectionOverUnion(track.boundingBox, detection.boundingBox)
    val distance = centerDistance(track.currentPosition, detection.boundingBox.center())
    return if (iou >= minimumIou || distance <= maximumCenterDistance) iou - distance * 0.25f else -1f
  }

  private fun updateTrack(track: TrackState, detection: DetectionResult) {
    track.previousPosition = track.currentPosition
    track.currentPosition = detection.boundingBox.center()
    track.objectType = detection.objectType
    track.source = detection.source
    track.confidence = detection.confidence
    track.boundingBox = detection.boundingBox
    track.timestampMillis = detection.timestampMillis
    track.frameSequence = detection.frameSequence
    track.missedFrames = 0
  }

  private fun createTrack(detection: DetectionResult) {
    tracks += TrackState(
      trackId = nextTrackId++,
      objectType = detection.objectType,
      source = detection.source,
      confidence = detection.confidence,
      boundingBox = detection.boundingBox,
      timestampMillis = detection.timestampMillis,
      frameSequence = detection.frameSequence,
      previousPosition = null,
      currentPosition = detection.boundingBox.center()
    )
  }

  private fun TrackState.toTrackedObject() = TrackedObject(
    trackId, objectType, source, confidence, boundingBox, timestampMillis, frameSequence,
    previousPosition, currentPosition, direction(previousPosition, currentPosition)
  )

  private fun direction(previous: NormalizedPoint?, current: NormalizedPoint): MovementDirection {
    if (previous == null) return MovementDirection.STATIONARY
    val dx = current.x - previous.x
    val dy = current.y - previous.y
    if (hypot(dx.toDouble(), dy.toDouble()) < 0.01) return MovementDirection.STATIONARY
    val horizontal = if (dx < -0.01) -1 else if (dx > 0.01) 1 else 0
    val vertical = if (dy < -0.01) -1 else if (dy > 0.01) 1 else 0
    return when (vertical to horizontal) {
      0 to -1 -> MovementDirection.LEFT
      0 to 1 -> MovementDirection.RIGHT
      -1 to 0 -> MovementDirection.UP
      1 to 0 -> MovementDirection.DOWN
      -1 to -1 -> MovementDirection.UP_LEFT
      -1 to 1 -> MovementDirection.UP_RIGHT
      1 to -1 -> MovementDirection.DOWN_LEFT
      else -> MovementDirection.DOWN_RIGHT
    }
  }

  private fun NormalizedBoundingBox.center() = NormalizedPoint((left + right) / 2f, (top + bottom) / 2f)
  private fun centerDistance(first: NormalizedPoint, second: NormalizedPoint) = hypot((first.x - second.x).toDouble(), (first.y - second.y).toDouble()).toFloat()
  private fun intersectionOverUnion(first: NormalizedBoundingBox, second: NormalizedBoundingBox): Float {
    val left = maxOf(first.left, second.left)
    val top = maxOf(first.top, second.top)
    val right = minOf(first.right, second.right)
    val bottom = minOf(first.bottom, second.bottom)
    val intersection = ((right - left).coerceAtLeast(0f) * (bottom - top).coerceAtLeast(0f))
    val union = first.area() + second.area() - intersection
    return if (union <= 0f) 0f else intersection / union
  }
  private fun NormalizedBoundingBox.area() = (right - left) * (bottom - top)
}