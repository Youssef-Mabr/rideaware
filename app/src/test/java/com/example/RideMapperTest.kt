package com.example

import com.example.data.mapFirestoreRide
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class RideMapperTest {
  @Test fun storedNumericTypesAndShortRidesKeepTheirValues() {
    val ride = mapFirestoreRide("ride-1", mapOf(
      "startTime" to Timestamp(1700000000, 0), "endTime" to Timestamp(1700000047, 0),
      "durationSeconds" to 47L, "distanceKm" to 0.84833336,
      "averageSpeedKmH" to 64.98, "maxSpeedKmH" to 68L, "safetyEventCount" to 1L
    ))
    assertEquals("ride-1", ride.id)
    assertEquals("47s", ride.durationLabel)
    assertEquals(0.84833336f, ride.distanceKm, 0.00001f)
    assertEquals(64, ride.averageSpeedKmH)
    assertEquals(68, ride.maxSpeedKmH)
    assertEquals(1, ride.safetyEventCount)
    assertFalse(ride.hasSafetyScore)
    assertFalse(ride.isFavorite)
    assertTrue(ride.protectedClips.isEmpty())
    assertTrue(ride.alerts.isEmpty())
  }

  @Test fun optionalFieldsUseStoredValuesAndDoNotInventSampleMetrics() {
    val ride = mapFirestoreRide("ride-2", mapOf(
      "createdAt" to Timestamp(1700000000, 0), "title" to "Evening ride",
      "isFavorite" to true, "safetyScore" to 90L, "durationSeconds" to 125L
    ))
    assertEquals("Evening ride", ride.title)
    assertEquals("2m 5s", ride.durationLabel)
    assertTrue(ride.isFavorite)
    assertTrue(ride.hasSafetyScore)
    assertEquals(90, ride.safetyScore)
    assertEquals(0, ride.safetyEventCount)
    assertEquals(0, ride.protectedClipsCount)
    assertEquals("—", ride.endTime)
  }

  @Test(expected = IllegalStateException::class)
  fun missingTimestampIsReportedInsteadOfShowingAnInventedDate() {
    mapFirestoreRide("broken", emptyMap())
  }
}
