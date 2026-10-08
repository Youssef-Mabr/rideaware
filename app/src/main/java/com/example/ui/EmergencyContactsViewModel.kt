package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.data.EmergencyContactRepository
import com.example.model.EmergencyContact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EmergencyContactsState(
  val contacts: List<EmergencyContact> = emptyList(),
  val loading: Boolean = true,
  val loadError: String? = null,
  val operationInProgress: Boolean = false,
  val operationError: String? = null
)

class EmergencyContactsViewModel : ViewModel() {
  private val auth = FirebaseAuth.getInstance()
  private val mutableState = MutableStateFlow(EmergencyContactsState())
  val state = mutableState.asStateFlow()
  private var registration: ListenerRegistration? = null
  private var observedUid: String? = null
  private val authListener = FirebaseAuth.AuthStateListener { observeCurrentUser() }

  init { auth.addAuthStateListener(authListener) }

  fun retry() {
    observedUid = null
    observeCurrentUser()
  }

  fun add(contact: EmergencyContact) = mutate("add") { uid -> EmergencyContactRepository.add(uid, contact) }
  fun update(contact: EmergencyContact) = mutate("update") { uid -> EmergencyContactRepository.update(uid, contact) }
  fun delete(contactId: String) = mutate("delete") { uid -> EmergencyContactRepository.delete(uid, contactId) }
  fun clearOperationError() { mutableState.value = mutableState.value.copy(operationError = null) }

  private fun observeCurrentUser() {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      registration?.remove()
      registration = null
      observedUid = null
      mutableState.value = EmergencyContactsState(loading = true)
      return
    }
    if (uid == observedUid && registration != null) return
    registration?.remove()
    observedUid = uid
    mutableState.value = EmergencyContactsState(loading = true)
    registration = EmergencyContactRepository.observe(uid,
      onContacts = { contacts ->
        if (auth.currentUser?.uid == uid) {
          mutableState.value = mutableState.value.copy(contacts = contacts, loading = false, loadError = null)
          Log.i(TAG, "Loaded ${contacts.size} emergency contacts for $uid")
        }
      },
      onError = { error ->
        if (auth.currentUser?.uid == uid) {
          mutableState.value = mutableState.value.copy(loading = false, loadError = "We couldn't load your contacts. Check your connection and try again.")
          Log.e(TAG, "Could not load emergency contacts for $uid", error)
        }
      }
    )
  }

  private fun mutate(action: String, operation: (String) -> Task<*>) {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      mutableState.value = mutableState.value.copy(operationError = "Sign-in is not ready. Try again shortly.")
      return
    }
    mutableState.value = mutableState.value.copy(operationInProgress = true, operationError = null)
    try {
      operation(uid)
        .addOnSuccessListener {
          mutableState.value = mutableState.value.copy(operationInProgress = false)
          Log.i(TAG, "Emergency contact $action succeeded for $uid")
        }
        .addOnFailureListener { error ->
          mutableState.value = mutableState.value.copy(operationInProgress = false, operationError = "Contact couldn't be saved. Please try again.")
          Log.e(TAG, "Emergency contact $action failed for $uid", error)
        }
    } catch (error: IllegalArgumentException) {
      mutableState.value = mutableState.value.copy(operationInProgress = false, operationError = error.message ?: "Check the contact details.")
    }
  }

  override fun onCleared() {
    registration?.remove()
    auth.removeAuthStateListener(authListener)
    super.onCleared()
  }

  private companion object { const val TAG = "RideAwareContacts" }
}
