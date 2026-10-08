package com.example.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.example.model.RideSummary
import com.example.recording.RecordingClipMetadata

object RideRepository {
  fun loadRides(uid: String): Task<List<RideSummary>> = FirebaseFirestore.getInstance()
    .collection("users").document(uid).collection("rides")
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .get(Source.SERVER)
    .continueWith { task ->
      val snapshot = task.result
      snapshot.documents.map { document -> mapFirestoreRide(document.id, document.data.orEmpty()) }
    }

  fun saveCompletedRide(
    startTimeMillis: Long,
    endTimeMillis: Long,
    durationSeconds: Int,
    distanceKm: Float,
    maxSpeedKmH: Int,
    safetyEventCount: Int,
    clip: RecordingClipMetadata? = null
  ): Task<String>? {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid == null) {
      Log.e(TAG, "Ride was not saved: no authenticated user")
      return null
    }

    val ride = FirebaseFirestore.getInstance()
      .collection("users").document(uid).collection("rides").document()
    val values = mapOf(
      "startTime" to Timestamp(Date(startTimeMillis)),
      "endTime" to Timestamp(Date(endTimeMillis)),
      "durationSeconds" to durationSeconds,
      "distanceKm" to distanceKm.toDouble(),
      "averageSpeedKmH" to if (durationSeconds > 0) distanceKm.toDouble() * 3600 / durationSeconds else 0.0,
      "maxSpeedKmH" to maxSpeedKmH,
      "safetyEventCount" to safetyEventCount,
      "status" to "completed",
      "createdAt" to FieldValue.serverTimestamp(),
      "clips" to (clip?.let { listOf<Map<String, Any>>(mapOf(
        "clipId" to it.clipId,
        "filePath" to it.filePath,
        "durationSeconds" to it.durationSeconds,
        "createdAtMillis" to it.createdAtMillis,
        "sourceAsset" to it.sourceAsset
      )) } ?: emptyList<Map<String, Any>>())
    )

    Log.i(TAG, "Saving ${ride.path}: durationSeconds=$durationSeconds, distanceKm=$distanceKm, maxSpeedKmH=$maxSpeedKmH, safetyEventCount=$safetyEventCount")
    ride.set(values)
      .addOnSuccessListener { Log.i(TAG, "Saved completed ride: ${ride.path}") }
      .addOnFailureListener { error -> Log.e(TAG, "Failed to save ${ride.path}", error) }
    return ride.id.let { com.google.android.gms.tasks.Tasks.forResult(it) }
  }

  private const val TAG = "RideAwareRides"
}
