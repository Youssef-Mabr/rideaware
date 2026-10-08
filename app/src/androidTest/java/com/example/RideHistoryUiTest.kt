package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.RideSummary
import com.example.ui.screens.RidesVaultScreen
import com.example.ui.screens.RideDetailScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RideHistoryUiTest {
  @get:Rule val compose = createComposeRule()

  @Test fun loadingDoesNotShowAnEmptyHistory() {
    compose.setContent { MyApplicationTheme { RidesVaultScreen(emptyList(), {}, isLoading = true) } }
    compose.onNodeWithTag("ride_history_loading").assertIsDisplayed()
    compose.onNodeWithText("No rides yet").assertDoesNotExist()
  }

  @Test fun errorOffersRetryWithoutShowingEmptyHistory() {
    var retries = 0
    compose.setContent { MyApplicationTheme {
      RidesVaultScreen(emptyList(), {}, errorMessage = "Connection unavailable", onRetry = { retries++ })
    } }
    compose.onNodeWithText("Connection unavailable").assertIsDisplayed()
    compose.onNodeWithTag("ride_history_retry").performClick()
    compose.runOnIdle { assertEquals(1, retries) }
    compose.onNodeWithText("No rides yet").assertDoesNotExist()
  }

  @Test fun emptyFiltersHaveSpecificMessages() {
    compose.setContent { MyApplicationTheme { RidesVaultScreen(emptyList(), {}) } }
    compose.onNodeWithText("No rides yet").assertIsDisplayed()
    compose.onNodeWithTag("vault_tab_protected").performClick()
    compose.onNodeWithText("No protected events yet").assertIsDisplayed()
    compose.onNodeWithTag("vault_tab_favorites").performClick()
    compose.onNodeWithText("No favorite rides yet").assertIsDisplayed()
  }

  @Test fun filtersPreserveRealRideSelectionAndDoNotInventFavorites() {
    val ride = RideSummary("real-id", date = "17 Sep 2026", durationMinutes = 0,
      distanceKm = 0.8f, durationSeconds = 47, safetyEventCount = 1, hasSafetyScore = false)
    var selected = ""
    compose.setContent { MyApplicationTheme { RidesVaultScreen(listOf(ride), { selected = it.id }) } }
    compose.onNodeWithTag("vault_tab_protected").performClick()
    compose.onNodeWithTag("ride_card_real-id").performClick()
    compose.runOnIdle { assertEquals("real-id", selected) }
    compose.onNodeWithTag("vault_tab_favorites").performClick()
    compose.onNodeWithTag("ride_card_real-id").assertDoesNotExist()
    compose.onNodeWithText("No favorite rides yet").assertIsDisplayed()
  }

  @Test fun detailsWithoutVideoCannotOpenSamplePlayback() {
    val ride = RideSummary("real-id", date = "17 Sep 2026", durationMinutes = 0,
      distanceKm = 0.8f, durationSeconds = 47, safetyEventCount = 1, hasSafetyScore = false)
    compose.setContent { MyApplicationTheme { RideDetailScreen(ride, {}, {}) } }
    compose.onNodeWithTag("watch_dual_playback_button").assertIsNotEnabled()
    compose.onNodeWithText("47s").assertExists()
    compose.onNodeWithText("Fast closing vehicle from behind").assertDoesNotExist()
  }
}
