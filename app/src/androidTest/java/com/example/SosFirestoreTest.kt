package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.EmergencyContactRepository
import com.example.data.SosEventCreationResult
import com.example.data.SosRepository
import com.example.model.EmergencyContact
import com.example.sos.SosTriggerSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SosFirestoreTest {
  @Test fun userWithoutContactGetsMissingContactResult() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val app = FirebaseApp.initializeApp(
      context,
      FirebaseApp.getInstance().options,
      "sos-missing-${System.currentTimeMillis()}"
    ) ?: error("Could not initialize isolated Firebase app")
    val auth = FirebaseAuth.getInstance(app)
    try {
      val uid = Tasks.await(auth.signInAnonymously(), 30, TimeUnit.SECONDS).user?.uid
        ?: error("Anonymous sign-in did not return a user")
      val result = Tasks.await(
        SosRepository.createTriggeredEventForFirstContact(
          uid,
          SosTriggerSource.SIMULATED_HELMET_BUTTON,
          FirebaseFirestore.getInstance(app)
        ),
        30,
        TimeUnit.SECONDS
      )
      assertEquals(SosEventCreationResult.MissingContact, result)
      Tasks.await(auth.currentUser!!.delete(), 30, TimeUnit.SECONDS)
    } finally {
      app.delete()
    }
  }

  @Test fun eventUsesFirstSavedContactAndServerTimestamp() {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
      ?: Tasks.await(auth.signInAnonymously(), 30, TimeUnit.SECONDS).user?.uid
      ?: error("Anonymous sign-in did not return a user")
    val firestore = FirebaseFirestore.getInstance()
    val contacts = firestore.collection("users").document(uid).collection("emergencyContacts")
    val first = Tasks.await(
      contacts.orderBy("createdAt", Query.Direction.ASCENDING).limit(1).get(Source.SERVER),
      30,
      TimeUnit.SECONDS
    ).documents.firstOrNull()

    var temporaryContactId: String? = null
    val expectedContactId = if (first != null) {
      first.id
    } else {
      val created = Tasks.await(EmergencyContactRepository.add(uid, EmergencyContact(
        name = "SOS test contact",
        relationship = "Test",
        phoneNumber = "+65 8123 4567"
      )), 30, TimeUnit.SECONDS)
      temporaryContactId = created.id
      created.id
    }

    var eventId: String? = null
    try {
      val result = Tasks.await(
        SosRepository.createTriggeredEventForFirstContact(uid, SosTriggerSource.SIMULATED_HELMET_BUTTON),
        30,
        TimeUnit.SECONDS
      )
      assertTrue(result is SosEventCreationResult.Created)
      val receipt = (result as SosEventCreationResult.Created).receipt
      eventId = receipt.eventId
      assertEquals(expectedContactId, receipt.contactId)

      val event = Tasks.await(
        firestore.collection("users").document(uid).collection("sosEvents")
          .document(receipt.eventId).get(Source.SERVER),
        30,
        TimeUnit.SECONDS
      )
      assertTrue(event.exists())
      assertEquals("triggered", event.getString("status"))
      assertEquals("simulated_helmet_button", event.getString("source"))
      assertEquals(expectedContactId, event.getString("contactId"))
      assertNotNull(event.getTimestamp("createdAt"))
    } finally {
      eventId?.let {
        Tasks.await(
          firestore.collection("users").document(uid).collection("sosEvents").document(it).delete(),
          30,
          TimeUnit.SECONDS
        )
      }
      temporaryContactId?.let {
        Tasks.await(EmergencyContactRepository.delete(uid, it), 30, TimeUnit.SECONDS)
      }
    }
  }
}
