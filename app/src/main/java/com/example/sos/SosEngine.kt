package com.example.sos

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SosTriggerSource(val firestoreValue: String) {
  SIMULATED_HELMET_BUTTON("simulated_helmet_button")
}

data class SosTriggerReceipt(
  val eventId: String,
  val contactId: String,
  val contactName: String
)

sealed interface SosTriggerOutcome {
  data class Success(val receipt: SosTriggerReceipt) : SosTriggerOutcome
  data class Failure(val message: String) : SosTriggerOutcome
}

fun interface SosTriggerExecutor {
  suspend fun execute(source: SosTriggerSource): SosTriggerOutcome
}

sealed interface SosState {
  data object Idle : SosState
  data class Countdown(val secondsRemaining: Int) : SosState
  data object Cancelled : SosState
  data class Triggered(val receipt: SosTriggerReceipt) : SosState
  data class Error(val message: String) : SosState
}

class SosEngine(
  private val scope: CoroutineScope,
  private val triggerExecutor: SosTriggerExecutor,
  private val countdownSeconds: Int = 10,
  private val tickDelayMillis: Long = 1_000L
) {
  private val mutableState = MutableStateFlow<SosState>(SosState.Idle)
  val state = mutableState.asStateFlow()
  private var countdownJob: Job? = null

  init {
    require(countdownSeconds > 0) { "Countdown must be longer than zero" }
    require(tickDelayMillis > 0) { "Countdown tick must be longer than zero" }
  }

  fun trigger(source: SosTriggerSource) {
    if (countdownJob?.isActive == true) return
    countdownJob = scope.launch {
      try {
        for (remaining in countdownSeconds downTo 1) {
          mutableState.value = SosState.Countdown(remaining)
          delay(tickDelayMillis)
        }

        // Zero means the countdown has finished and cancellation is closed while
        // the owner-scoped event is being created.
        mutableState.value = SosState.Countdown(0)
        mutableState.value = when (val outcome = triggerExecutor.execute(source)) {
          is SosTriggerOutcome.Success -> SosState.Triggered(outcome.receipt)
          is SosTriggerOutcome.Failure -> SosState.Error(outcome.message)
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        mutableState.value = SosState.Error(GENERIC_ERROR_MESSAGE)
      }
    }
  }

  fun cancel() {
    val countdown = mutableState.value as? SosState.Countdown ?: return
    if (countdown.secondsRemaining <= 0) return
    countdownJob?.cancel()
    countdownJob = null
    mutableState.value = SosState.Cancelled
  }

  fun reset() {
    countdownJob?.cancel()
    countdownJob = null
    mutableState.value = SosState.Idle
  }

  companion object {
    const val MISSING_CONTACT_MESSAGE = "No emergency contact is saved. Add one in Settings before using SOS."
    const val GENERIC_ERROR_MESSAGE = "The SOS event could not be created. Check your connection and try again."
  }
}
