package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.data.RideRepository
import com.example.model.RideSummary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RideHistoryState {
  data object Loading : RideHistoryState
  data class Ready(val rides: List<RideSummary>) : RideHistoryState
  data class Error(val message: String) : RideHistoryState
}

class RideHistoryViewModel : ViewModel() {
  private val auth = FirebaseAuth.getInstance()
  private val mutableState = MutableStateFlow<RideHistoryState>(RideHistoryState.Loading)
  val state = mutableState.asStateFlow()
  private var requestVersion = 0
  private val authListener = FirebaseAuth.AuthStateListener { refresh() }

  init { auth.addAuthStateListener(authListener) }

  fun refresh() {
    val version = ++requestVersion
    val uid = auth.currentUser?.uid
    if (uid == null) {
      mutableState.value = RideHistoryState.Error("Sign-in is not ready. Please try again shortly.")
      return
    }
    mutableState.value = RideHistoryState.Loading
    RideRepository.loadRides(uid)
      .addOnSuccessListener { rides ->
        if (version == requestVersion && auth.currentUser?.uid == uid) {
          mutableState.value = RideHistoryState.Ready(rides)
          Log.i("RideAwareHistory", "Loaded ${rides.size} rides for $uid, newest first")
        }
      }
      .addOnFailureListener { error ->
        if (version == requestVersion && auth.currentUser?.uid == uid) {
          mutableState.value = RideHistoryState.Error("We couldn't load your rides. Check your connection and try again.")
          Log.e("RideAwareHistory", "Failed to load rides for $uid", error)
        }
      }
  }

  override fun onCleared() {
    requestVersion++
    auth.removeAuthStateListener(authListener)
    super.onCleared()
  }
}
