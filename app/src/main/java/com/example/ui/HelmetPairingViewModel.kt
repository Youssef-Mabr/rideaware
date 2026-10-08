package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.data.HelmetPairingRepository
import com.example.data.PairedHelmet
import com.example.helmet.HelmetQrCatalog
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HelmetPairingState(
  val pairedHelmet: PairedHelmet? = null,
  val loading: Boolean = true,
  val unauthenticated: Boolean = false,
  val loadError: String? = null,
  val operationInProgress: Boolean = false,
  val operationError: String? = null
)

class HelmetPairingViewModel : ViewModel() {
  private val auth = FirebaseAuth.getInstance()
  private val mutableState = MutableStateFlow(HelmetPairingState())
  val state = mutableState.asStateFlow()
  private var registration: ListenerRegistration? = null
  private var observedUid: String? = null
  private val authListener = FirebaseAuth.AuthStateListener { observeCurrentUser() }

  init { auth.addAuthStateListener(authListener) }

  fun retry() {
    observedUid = null
    observeCurrentUser()
  }

  fun submitQr(rawPayload: String) {
    val helmetId = HelmetQrCatalog.helmetIdFromQr(rawPayload)
    if (rawPayload.isBlank()) {
      setOperationError("Scan or enter a RideAware helmet QR code.")
      return
    }
    if (helmetId == null) {
      setOperationError("This QR code is not a RideAware helmet. Try the sample QR: ${HelmetQrCatalog.SAMPLE_HELMET_ID}.")
      return
    }
    mutate("pair") { uid -> HelmetPairingRepository.pair(uid, helmetId) }
  }

  fun unpair() = mutate("unpair") { uid -> HelmetPairingRepository.unpair(uid) }
  fun clearOperationError() { mutableState.value = mutableState.value.copy(operationError = null) }

  private fun observeCurrentUser() {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      registration?.remove()
      registration = null
      observedUid = null
      mutableState.value = HelmetPairingState(loading = false, unauthenticated = true)
      return
    }
    if (uid == observedUid && registration != null) return
    registration?.remove()
    observedUid = uid
    mutableState.value = HelmetPairingState(loading = true)
    registration = HelmetPairingRepository.observe(uid,
      onPairing = { helmet ->
        if (auth.currentUser?.uid == uid) {
          mutableState.value = mutableState.value.copy(
            pairedHelmet = helmet, loading = false, unauthenticated = false, loadError = null
          )
        }
      },
      onError = { error ->
        if (auth.currentUser?.uid == uid) {
          mutableState.value = mutableState.value.copy(
            loading = false, loadError = "We couldn't load your helmet pairing. Check your connection and try again."
          )
          Log.e(TAG, "Could not load helmet pairing for $uid", error)
        }
      }
    )
  }

  private fun mutate(action: String, operation: (String) -> Task<*>) {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      setOperationError("Sign-in is not ready. Try again shortly.")
      return
    }
    mutableState.value = mutableState.value.copy(operationInProgress = true, operationError = null)
    try {
      operation(uid)
        .addOnSuccessListener {
          mutableState.value = mutableState.value.copy(operationInProgress = false)
          Log.i(TAG, "Helmet $action succeeded for $uid")
        }
        .addOnFailureListener { error ->
          mutableState.value = mutableState.value.copy(
            operationInProgress = false,
            operationError = if (action == "pair") "Helmet couldn't be paired. Please try again." else "Helmet couldn't be unpaired. Please try again."
          )
          Log.e(TAG, "Helmet $action failed for $uid", error)
        }
    } catch (error: IllegalArgumentException) {
      setOperationError(error.message ?: "This QR code is not a RideAware helmet.")
    }
  }

  private fun setOperationError(message: String) {
    mutableState.value = mutableState.value.copy(operationInProgress = false, operationError = message)
  }

  override fun onCleared() {
    registration?.remove()
    auth.removeAuthStateListener(authListener)
    super.onCleared()
  }

  private companion object { const val TAG = "RideAwareHelmetPairing" }
}
