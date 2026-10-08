package com.example.ui

import android.util.Log
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SosEventCreationResult
import com.example.data.SosRepository
import com.example.sos.SosEngine
import com.example.sos.SosTriggerExecutor
import com.example.sos.SosTriggerOutcome
import com.example.sos.SosTriggerSource
import com.example.sos.PhoneSosLocationProvider
import kotlinx.coroutines.CancellationException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SosViewModel(application: Application) : AndroidViewModel(application) {
  private val locationProvider = PhoneSosLocationProvider(application)
  private val auth = FirebaseAuth.getInstance()
  private val engine = SosEngine(
    scope = viewModelScope,
    triggerExecutor = SosTriggerExecutor { source -> createEvent(source) }
  )

  val state = engine.state

  /** Shared entry point for the current UI simulation and a future BLE handler. */
  fun trigger(source: SosTriggerSource) = engine.trigger(source)

  fun triggerSimulatedHelmetButton() = trigger(SosTriggerSource.SIMULATED_HELMET_BUTTON)

  fun cancel() = engine.cancel()

  fun reset() = engine.reset()

  private suspend fun createEvent(source: SosTriggerSource): SosTriggerOutcome {
    val uid = auth.currentUser?.uid
      ?: return SosTriggerOutcome.Failure("Sign-in is not ready. Try SOS again in a moment.")
    return try {
      val location = locationProvider.capture()
      when (val result = SosRepository.createTriggeredEventForFirstContact(uid, source, location = location).awaitResult()) {
        SosEventCreationResult.MissingContact -> SosTriggerOutcome.Failure(SosEngine.MISSING_CONTACT_MESSAGE)
        is SosEventCreationResult.Created -> {
          Log.i(TAG, "Created SOS event ${result.receipt.eventId}; locationStatus=${location.status.firestoreValue}")
          SosTriggerOutcome.Success(result.receipt)
        }
      }
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (error: Exception) {
      Log.e(TAG, "Could not create SOS event for $uid", error)
      SosTriggerOutcome.Failure(SosEngine.GENERIC_ERROR_MESSAGE)
    }
  }

  private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(result) }
    addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
    addOnCanceledListener { continuation.cancel() }
  }

  private companion object { const val TAG = "RideAwareSos" }
}
