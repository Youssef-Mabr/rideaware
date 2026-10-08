package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.model.*
import com.example.sos.SosState
import com.example.sos.SosTriggerReceipt
import com.example.ui.screens.CrashDetectionSosScreen
import com.example.ui.screens.ActiveRideScreen
import com.example.ui.screens.PreRideScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UsabilityTest {
  @get:Rule val compose = createComposeRule()

  @Test fun disconnectedHelmetDisablesStartAndExplainsWhy() {
    compose.setContent {
      MyApplicationTheme {
        PreRideScreen(HelmetStatus(isConnected = false), EmergencyContact(), {}, {})
      }
    }
    compose.onNodeWithTag("start_recording_and_ride_button").assertIsNotEnabled()
    compose.onNodeWithText("Some checks need attention").assertIsDisplayed()
  }

  @Test fun largeTextKeepsStartActionReachable() {
    var started = false
    compose.setContent {
      val density = LocalDensity.current.density
      CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
        MyApplicationTheme {
          PreRideScreen(HelmetStatus(), EmergencyContact(), { started = true }, {})
        }
      }
    }
    compose.onNodeWithTag("start_recording_and_ride_button").assertIsDisplayed().performClick()
    compose.runOnIdle { assertTrue(started) }
  }

  @Test fun triggeredSosClearlySaysNoMessageWasSent() {
    val state = SosState.Triggered(SosTriggerReceipt("event-1", "contact-1", "Mariam"))
    compose.setContent { MyApplicationTheme { CrashDetectionSosScreen(state, {}, {}) } }
    compose.onNodeWithTag("sos_state_triggered").assertIsDisplayed()
    compose.onNodeWithText("No SMS was sent", substring = true).assertIsDisplayed()
    compose.onNodeWithTag("sos_return_button").assertIsDisplayed()
  }

  @Test fun sosCancelRemainsAvailableWithLargeText() {
    var cancelled = false
    compose.setContent {
      val density = LocalDensity.current.density
      CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
        MyApplicationTheme { CrashDetectionSosScreen(SosState.Countdown(10), { cancelled = true }, {}) }
      }
    }
    compose.onNodeWithTag("sos_i_am_okay_button").assertIsDisplayed().performClick()
    compose.runOnIdle { assertTrue(cancelled) }
  }

  @Test fun activeRideControlsStayVisibleWithLargeText() {
    var endRequested = false
    compose.setContent {
      val density = LocalDensity.current.density
      CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
        MyApplicationTheme {
          ActiveRideScreen(HelmetStatus(), {}, { endRequested = true }, {}, {}, {}, {})
        }
      }
    }
    compose.onNodeWithTag("active_ride_save_moment_button").assertIsDisplayed()
    compose.onNodeWithTag("active_ride_genie").assertIsDisplayed()
    compose.onNodeWithTag("active_ride_end_ride_button").assertIsDisplayed().performClick()
    compose.runOnIdle { assertTrue(endRequested) }
  }
}
