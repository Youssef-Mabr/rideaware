package com.example.data

import com.example.helmet.HelmetQrCatalog
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.util.Date

data class PairedHelmet(
  val helmetId: String,
  val pairedAt: Date?
)

object HelmetPairingRepository {
  fun observe(
    uid: String,
    onPairing: (PairedHelmet?) -> Unit,
    onError: (Exception) -> Unit,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
  ): ListenerRegistration = userDocument(firestore, uid).addSnapshotListener { snapshot, error ->
    if (error != null) {
      onError(error)
      return@addSnapshotListener
    }
    try {
      onPairing(snapshot?.toPairedHelmet())
    } catch (mappingError: Exception) {
      onError(mappingError)
    }
  }

  fun pair(
    uid: String,
    helmetId: String,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
  ): Task<Void> {
    require(HelmetQrCatalog.helmetIdFromQr(helmetId) != null) { "This QR code is not a RideAware helmet." }
    return userDocument(firestore, uid).set(mapOf(
      "helmetId" to helmetId,
      "helmetPairedAt" to FieldValue.serverTimestamp()
    ), SetOptions.merge())
  }

  fun unpair(
    uid: String,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
  ): Task<Void> = userDocument(firestore, uid).update(mapOf(
    "helmetId" to FieldValue.delete(),
    "helmetPairedAt" to FieldValue.delete()
  ))

  private fun userDocument(firestore: FirebaseFirestore, uid: String) = firestore
    .collection("users").document(uid)

  private fun DocumentSnapshot.toPairedHelmet(): PairedHelmet? {
    val helmetId = getString("helmetId")
    val pairedAt = getTimestamp("helmetPairedAt")?.toDate()
    if (helmetId == null && pairedAt == null) return null
    require(helmetId != null && HelmetQrCatalog.helmetIdFromQr(helmetId) != null) {
      "The saved helmet pairing is invalid."
    }
    return PairedHelmet(helmetId, pairedAt)
  }
}
