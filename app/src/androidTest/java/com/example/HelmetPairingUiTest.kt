package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.PairedHelmet
import com.example.ui.HelmetPairingState
import com.example.ui.screens.HelmetPairingScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test

class HelmetPairingUiTest {
  @get:Rule val compose = createComposeRule()

  private fun show(state: HelmetPairingState) {
    compose.setContent { MyApplicationTheme {
      HelmetPairingScreen(state, {}, {}, {}, {}, {})
    } }
  }

  @Test fun unpairedScreenShowsTheProvisionedSampleQr() {
    show(HelmetPairingState(loading = false))
    compose.onNodeWithTag("helmet_unpaired").assertExists()
    compose.onNodeWithTag("sample_helmet_qr").assertExists()
    compose.onNodeWithText("RA-DEMO-001").assertExists()
  }

  @Test fun pairedScreenShowsAClearPairedStatus() {
    show(HelmetPairingState(loading = false, pairedHelmet = PairedHelmet("RA-DEMO-001", null)))
    compose.onNodeWithTag("helmet_paired").assertExists()
    compose.onNodeWithText("Helmet Paired").assertExists()
  }

  @Test fun unauthenticatedStateIsExplicit() {
    show(HelmetPairingState(loading = false, unauthenticated = true))
    compose.onNodeWithTag("helmet_pairing_unauthenticated").assertExists()
    compose.onNodeWithText("Sign-in required").assertExists()
  }

  @Test fun invalidQrErrorStateIsExplicit() {
    show(HelmetPairingState(loading = false, operationError = "This QR code is not a RideAware helmet."))
    compose.onNodeWithTag("helmet_pairing_operation_error").assertExists()
    compose.onNodeWithText("This QR code is not a RideAware helmet.").assertExists()
  }
}
