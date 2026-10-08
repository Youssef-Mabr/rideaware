package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.sos.SosEngine
import com.example.sos.SosState
import com.example.sos.SosTriggerReceipt
import com.example.ui.screens.CrashDetectionSosScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test

class SosUiStateTest {
  @get:Rule val compose = createComposeRule()

  @Test fun allSosStatesAreClearlyRendered() {
    val state = mutableStateOf<SosState>(SosState.Idle)
    compose.setContent {
      MyApplicationTheme { CrashDetectionSosScreen(state.value, {}, {}) }
    }
    compose.onNodeWithTag("sos_state_idle").assertIsDisplayed()

    compose.runOnIdle { state.value = SosState.Countdown(10) }
    compose.onNodeWithTag("sos_state_countdown").assertIsDisplayed()
    compose.onNodeWithTag("sos_countdown").assertIsDisplayed()

    compose.runOnIdle { state.value = SosState.Cancelled }
    compose.onNodeWithTag("sos_state_cancelled").assertIsDisplayed()

    compose.runOnIdle {
      state.value = SosState.Triggered(SosTriggerReceipt("event-1", "contact-1", "Mariam"))
    }
    compose.onNodeWithTag("sos_state_triggered").assertIsDisplayed()

    compose.runOnIdle { state.value = SosState.Error(SosEngine.MISSING_CONTACT_MESSAGE) }
    compose.onNodeWithTag("sos_state_error").assertIsDisplayed()
  }
}
