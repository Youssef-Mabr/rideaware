package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.HelmetPairingRepository
import com.example.helmet.HelmetQrCatalog
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HelmetPairingFirestoreTest {
  @Test fun anonymousUserCanPairAndUnpairTheProvisionedHelmet() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val app = FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "helmet-pairing-${System.nanoTime()}")
      ?: error("Could not initialize isolated Firebase app")
    val auth = FirebaseAuth.getInstance(app)
    val firestore = FirebaseFirestore.getInstance(app)
    var uid: String? = null
    try {
      uid = Tasks.await(auth.signInAnonymously(), 30, TimeUnit.SECONDS).user?.uid
        ?: error("Anonymous sign-in did not return a user")
      Tasks.await(HelmetPairingRepository.pair(uid, HelmetQrCatalog.SAMPLE_HELMET_ID, firestore), 30, TimeUnit.SECONDS)
      val paired = Tasks.await(firestore.collection("users").document(uid).get(Source.SERVER), 30, TimeUnit.SECONDS)
      assertEquals(HelmetQrCatalog.SAMPLE_HELMET_ID, paired.getString("helmetId"))
      assertNotNull(paired.getTimestamp("helmetPairedAt"))

      Tasks.await(HelmetPairingRepository.unpair(uid, firestore), 30, TimeUnit.SECONDS)
      val unpaired = Tasks.await(firestore.collection("users").document(uid).get(Source.SERVER), 30, TimeUnit.SECONDS)
      assertFalse(unpaired.contains("helmetId"))
      assertFalse(unpaired.contains("helmetPairedAt"))
    } finally {
      uid?.let { Tasks.await(firestore.collection("users").document(it).delete(), 30, TimeUnit.SECONDS) }
      auth.currentUser?.let { Tasks.await(it.delete(), 30, TimeUnit.SECONDS) }
      app.delete()
    }
  }
}
