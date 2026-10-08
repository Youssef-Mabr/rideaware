package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.detection.DetectedObjectType
import com.example.detection.DetectionResult
import com.example.detection.NormalizedBoundingBox
import com.example.model.CameraSource
import com.example.tracking.MovementDirection
import com.example.tracking.TemporalObjectTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemporalObjectTrackerSmokeTest {
  @Test fun samsungKeepsTrackIdAcrossSequentialVehicleFrames() {
    val tracker = TemporalObjectTracker()
    val first = tracker.update(listOf(detection(1L, 0.40f)))
    val second = tracker.update(listOf(detection(2L, 0.50f)))

    assertEquals(1, first.size)
    assertEquals(1, second.size)
    assertEquals(first.single().trackId, second.single().trackId)
    assertEquals(MovementDirection.RIGHT, second.single().movementDirection)
    assertNotNull(second.single().previousPosition)
  }

  private fun detection(sequence: Long, centerX: Float) = DetectionResult(
    objectType = DetectedObjectType.CAR,
    confidence = 0.9f,
    boundingBox = NormalizedBoundingBox(centerX - 0.1f, 0.4f, centerX + 0.1f, 0.6f),
    timestampMillis = sequence * 100L,
    source = CameraSource.FRONT,
    frameSequence = sequence
  )
}