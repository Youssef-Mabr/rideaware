package com.example.data

import com.example.model.RideSummary
import com.example.model.ProtectedClip
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

fun mapFirestoreRide(id: String, data: Map<String, Any>): RideSummary {
  val start = (data["startTime"] as? Timestamp ?: data["createdAt"] as? Timestamp)
    ?.toDate() ?: error("Ride $id has no start time")
  val end = (data["endTime"] as? Timestamp)?.toDate()
  fun number(key: String) = (data[key] as? Number)?.toDouble()?.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
  val seconds = number("durationSeconds").coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()
  val events = number("safetyEventCount").coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()
  val averageSpeed = number("averageSpeedKmH").toInt()
  val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
  val clips = (data["clips"] as? List<*>)?.mapNotNull { raw ->
    val clip = raw as? Map<*, *> ?: return@mapNotNull null
    ProtectedClip(
      id = clip["clipId"] as? String ?: return@mapNotNull null,
      title = "Trip recording",
      timestamp = timeFormat.format(start),
      durationSeconds = (clip["durationSeconds"] as? Number)?.toInt() ?: seconds,
      durationSec = (clip["durationSeconds"] as? Number)?.toInt() ?: seconds,
      hazardTag = "Recorded test video",
      riskLevel = "LOW",
      filePath = clip["filePath"] as? String,
      rideId = id,
      createdAtMillis = (clip["createdAtMillis"] as? Number)?.toLong()
    )
  }.orEmpty()
  return RideSummary(
    id = id,
    title = (data["title"] as? String)?.takeIf { it.isNotBlank() } ?: "Ride · ${timeFormat.format(start)}",
    date = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(start),
    time = timeFormat.format(start),
    startTime = timeFormat.format(start),
    endTime = end?.let(timeFormat::format) ?: "—",
    durationMinutes = seconds / 60,
    durationSeconds = seconds,
    distanceKm = number("distanceKm").toFloat(),
    avgSpeedKmH = averageSpeed,
    averageSpeedKmH = averageSpeed,
    maxSpeedKmH = number("maxSpeedKmH").toInt(),
    safetyScore = number("safetyScore").toInt().coerceIn(0, 100),
    hasSafetyScore = data["safetyScore"] is Number,
    alertCount = events,
    safetyEventCount = events,
    protectedClipsCount = clips.size,
    clips = clips,
    protectedClips = clips,
    batteryUsedPercent = 0,
    batteryConsumedPercent = 0,
    safetyBadge = "$events Events",
    isSafeRide = false,
    isFavorite = data["isFavorite"] == true
  )
}
