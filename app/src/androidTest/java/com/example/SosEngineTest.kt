package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sos.SosEngine
import com.example.sos.SosState
import com.example.sos.SosTriggerExecutor
import com.example.sos.SosTriggerOutcome
import com.example.sos.SosTriggerReceipt
import com.example.sos.SosTriggerSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SosEngineTest {
  private val receipt = SosTriggerReceipt("event-1", "contact-1", "Mariam")

  @Test fun tenSecondCountdownTriggersExactlyOnce() = runTest {
    var triggerCount = 0
    val engine = SosEngine(this, SosTriggerExecutor {
      triggerCount++
      SosTriggerOutcome.Success(receipt)
    })

    engine.trigger(SosTriggerSource.SIMULATED_HELMET_BUTTON)
    runCurrent()
    assertEquals(SosState.Countdown(10), engine.state.value)
    advanceTimeBy(9_000)
    runCurrent()
    assertEquals(SosState.Countdown(1), engine.state.value)
    advanceTimeBy(1_000)
    advanceUntilIdle()
    assertEquals(SosState.Triggered(receipt), engine.state.value)
    assertEquals(1, triggerCount)
  }

  @Test fun cancellationStopsCountdownAndPreventsTrigger() = runTest {
    var triggerCount = 0
    val engine = SosEngine(this, SosTriggerExecutor {
      triggerCount++
      SosTriggerOutcome.Success(receipt)
    })

    engine.trigger(SosTriggerSource.SIMULATED_HELMET_BUTTON)
    runCurrent()
    advanceTimeBy(3_000)
    runCurrent()
    engine.cancel()
    runCurrent()
    assertEquals(SosState.Cancelled, engine.state.value)
    advanceUntilIdle()
    assertEquals(0, triggerCount)
  }

  @Test fun missingContactBecomesClearError() = runTest {
    val engine = SosEngine(
      scope = this,
      countdownSeconds = 1,
      triggerExecutor = SosTriggerExecutor {
        SosTriggerOutcome.Failure(SosEngine.MISSING_CONTACT_MESSAGE)
      }
    )

    engine.trigger(SosTriggerSource.SIMULATED_HELMET_BUTTON)
    runCurrent()
    advanceTimeBy(1_000)
    advanceUntilIdle()
    val error = engine.state.value
    assertTrue(error is SosState.Error)
    assertEquals(SosEngine.MISSING_CONTACT_MESSAGE, (error as SosState.Error).message)
  }
}
