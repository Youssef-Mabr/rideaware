package com.example.data

import com.example.sos.SosTriggerReceipt
import com.example.sos.SosTriggerSource
import com.example.sos.SosLocationResult
import com.example.sos.SosLocationStatus
import com.google.firebase.Timestamp
import java.util.Date
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source

sealed interface SosEventCreationResult {
  data object MissingContact : SosEventCreationResult
  data class Created(val receipt: SosTriggerReceipt) : SosEventCreationResult
}

object SosRepository {
  fun createTriggeredEventForFirstContact(
    uid: String,
    source: SosTriggerSource,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    location: SosLocationResult = SosLocationResult(SosLocationStatus.UNAVAILABLE)
  ): Task<SosEventCreationResult> {
    require(uid.isNotBlank()) { "An authenticated user ID is required" }
    return firstContact(firestore, uid).continueWithTask { contactTask ->
      val contact = contactTask.result.documents.firstOrNull()
        ?: return@continueWithTask Tasks.forResult(SosEventCreationResult.MissingContact)

      val data = mutableMapOf<String, Any>(
        "status" to "triggered",
        "source" to source.firestoreValue,
        "contactId" to contact.id,
        "createdAt" to FieldValue.serverTimestamp(),
        "locationStatus" to location.status.firestoreValue
      )
      location.coordinates?.let { coordinates ->
        data["location"] = mapOf(
          "latitude" to coordinates.latitude,
          "longitude" to coordinates.longitude,
          "accuracy" to coordinates.accuracy,
          "capturedAt" to Timestamp(Date(coordinates.capturedAtMillis))
        )
      }
      events(firestore, uid).add(data).continueWith { eventTask ->
        val event = eventTask.result
        SosEventCreationResult.Created(
          SosTriggerReceipt(
            eventId = event.id,
            contactId = contact.id,
            contactName = contact.getString("name").orEmpty()
          )
        )
      }
    }
  }

  private fun firstContact(
    firestore: FirebaseFirestore,
    uid: String
  ): Task<com.google.firebase.firestore.QuerySnapshot> =
    contacts(firestore, uid)
      .orderBy("createdAt", Query.Direction.ASCENDING)
      .limit(1)
      .get(Source.SERVER)

  private fun contacts(firestore: FirebaseFirestore, uid: String) = firestore
    .collection("users").document(uid).collection("emergencyContacts")

  private fun events(firestore: FirebaseFirestore, uid: String) = firestore
    .collection("users").document(uid).collection("sosEvents")
}
