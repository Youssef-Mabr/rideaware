package com.example

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.SosEventCreationResult
import com.example.data.SosRepository
import com.example.sos.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Run with sosLocationCase=granted|denied|disabled|timeout. Granted uses an Android test provider. */
@RunWith(AndroidJUnit4::class)
class SosLocationDeviceTest {
  @Suppress("DEPRECATION")
  @Test fun countdownCapturesLocationAndSavesEvent() = runBlocking {
    val mode = InstrumentationRegistry.getArguments().getString("sosLocationCase") ?: "denied"
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val mockProviders = mutableListOf<String>()
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    val app = FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "sos-location-${System.nanoTime()}")!!
    val auth = FirebaseAuth.getInstance(app)
    val firestore = FirebaseFirestore.getInstance(app)
    var contactId: String? = null
    var eventId: String? = null
    var injection: Job? = null
    try {
      if (mode == "granted" || mode == "timeout") {
        assertTrue("Location permission must be granted before this test", PhoneSosLocationProvider.hasPermission(context))
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
          manager.addTestProvider(provider, false, false, false, false, true, true, true, 1, 1)
          mockProviders.add(provider)
          manager.setTestProviderEnabled(provider, true)
        }
        if (mode == "granted") injection = launch(Dispatchers.Default) {
          while (isActive) {
            mockProviders.forEach { provider ->
              manager.setTestProviderLocation(provider, Location(provider).apply {
                latitude = 1.3521
                longitude = 103.8198
                accuracy = 12.5f
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
              })
            }
            delay(200)
          }
        }
      }
      val uid = Tasks.await(auth.signInAnonymously(), 30, TimeUnit.SECONDS).user!!.uid
      val contacts = firestore.collection("users").document(uid).collection("emergencyContacts")
      contactId = Tasks.await(contacts.add(mapOf(
        "name" to "Location test contact", "phoneNumber" to "+65 8123 4567", "relationship" to "Test",
        "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
      )), 30, TimeUnit.SECONDS).id

      var locationResult: SosLocationResult? = null
      val expected = when (mode) {
        "granted" -> SosLocationStatus.AVAILABLE
        "disabled" -> SosLocationStatus.DISABLED
        "timeout" -> SosLocationStatus.TIMEOUT
        else -> SosLocationStatus.PERMISSION_DENIED
      }
      val engine = SosEngine(this, SosTriggerExecutor { source ->
        val start = SystemClock.elapsedRealtime()
        val location = PhoneSosLocationProvider(context).capture()
        assertTrue("Location capture exceeded its time budget", SystemClock.elapsedRealtime() - start < 10_000)
        locationResult = location
        val result = withContext(Dispatchers.IO) {
          Tasks.await(SosRepository.createTriggeredEventForFirstContact(uid, source, firestore, location), 30, TimeUnit.SECONDS)
        } as SosEventCreationResult.Created
        eventId = result.receipt.eventId
        SosTriggerOutcome.Success(result.receipt)
      })
      engine.trigger(SosTriggerSource.SIMULATED_HELMET_BUTTON)
      val finalState = withTimeout(65_000) { engine.state.first { it is SosState.Triggered || it is SosState.Error } }
      assertTrue("Expected Triggered but got $finalState", finalState is SosState.Triggered)
      if (mode == "live") {
        assertTrue(locationResult!!.status in listOf(SosLocationStatus.AVAILABLE, SosLocationStatus.TIMEOUT, SosLocationStatus.UNAVAILABLE))
      } else assertEquals(expected, locationResult!!.status)
      android.util.Log.i("SosLocationDeviceTest", "case=$mode locationStatus=${locationResult!!.status.firestoreValue}; SOS event saved")
      val event = Tasks.await(firestore.collection("users").document(uid).collection("sosEvents")
        .document(eventId!!).get(Source.SERVER), 30, TimeUnit.SECONDS)
      assertEquals("triggered", event.getString("status"))
      assertEquals("simulated_helmet_button", event.getString("source"))
      assertEquals(contactId, event.getString("contactId"))
      assertNotNull(event.getTimestamp("createdAt"))
      assertEquals(locationResult!!.status.firestoreValue, event.getString("locationStatus"))
      if (mode == "granted") {
        assertEquals(1.3521, event.getDouble("location.latitude")!!, 0.000001)
        assertEquals(103.8198, event.getDouble("location.longitude")!!, 0.000001)
        assertEquals(12.5, event.getDouble("location.accuracy")!!, 0.001)
        assertNotNull(event.getTimestamp("location.capturedAt"))
      } else if (locationResult!!.status == SosLocationStatus.AVAILABLE) {
        assertNotNull(event.getDouble("location.latitude"))
        assertNotNull(event.getDouble("location.longitude"))
        assertNotNull(event.getDouble("location.accuracy"))
        assertNotNull(event.getTimestamp("location.capturedAt"))
      } else assertFalse(event.contains("location"))
    } finally {
      injection?.cancelAndJoin()
      mockProviders.forEach { manager.removeTestProvider(it) }
      auth.currentUser?.let { user ->
        val userRef = firestore.collection("users").document(user.uid)
        eventId?.let { Tasks.await(userRef.collection("sosEvents").document(it).delete(), 30, TimeUnit.SECONDS) }
        contactId?.let { Tasks.await(userRef.collection("emergencyContacts").document(it).delete(), 30, TimeUnit.SECONDS) }
        Tasks.await(user.delete(), 30, TimeUnit.SECONDS)
      }
      app.delete()
      scenario.close()
    }
  }
}
