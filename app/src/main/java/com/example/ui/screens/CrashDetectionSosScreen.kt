package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sos.SosState
import com.example.ui.components.GloveButton
import com.example.ui.theme.*

@Composable
fun CrashDetectionSosScreen(
  state: SosState,
  onCancelCountdown: () -> Unit,
  onReturnToRide: () -> Unit,
  modifier: Modifier = Modifier
) {
  val title = when (state) {
    SosState.Idle -> "SOS ready"
    is SosState.Countdown -> if (state.secondsRemaining > 0) "Emergency countdown" else "Creating SOS event"
    SosState.Cancelled -> "SOS cancelled"
    is SosState.Triggered -> "SOS event recorded"
    is SosState.Error -> "SOS could not start"
  }
  val explanation = when (state) {
    SosState.Idle -> "Use the SOS button during a ride to simulate the helmet button."
    is SosState.Countdown -> if (state.secondsRemaining > 0) {
      "The simulated helmet button was activated. Cancel if emergency help is not needed."
    } else {
      "Selecting your first saved emergency contact and securely recording the event."
    }
    SosState.Cancelled -> "The countdown stopped. No SOS event was created and no one was contacted."
    is SosState.Triggered -> "The event was saved to your account. No SMS was sent and no emergency service was contacted."
    is SosState.Error -> state.message
  }

  Column(
    modifier.fillMaxSize().background(DarkCanvas).padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Row(
      Modifier.fillMaxWidth().background(DangerRed.copy(alpha = 0.12f), RoundedCornerShape(16.dp)).padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(Icons.Filled.Warning, null, tint = DangerRed)
      Text("SOS", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }

    Column(
      Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      Text(
        title,
        color = TextPrimary,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .testTag(state.testTag())
          .semantics { liveRegion = LiveRegionMode.Polite }
      )
      Text(
        explanation,
        color = if (state is SosState.Error) DangerRed else TextSecondary,
        fontSize = 16.sp,
        textAlign = TextAlign.Center
      )

      if (state is SosState.Countdown) {
        if (state.secondsRemaining > 0) {
          Text(
            "${state.secondsRemaining}",
            color = TextPrimary,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("sos_countdown")
          )
          Text("seconds remaining", color = TextSecondary, fontSize = 14.sp)
        } else {
          CircularProgressIndicator(color = DangerRed, modifier = Modifier.size(48.dp).testTag("sos_creating_event"))
        }
      }

      ContactStatusCard(state)

      Text(
        "For immediate help, use your phone's emergency calling feature. This app does not call, message, or request rescue services.",
        color = TextPrimary,
        fontSize = 16.sp,
        textAlign = TextAlign.Center
      )
    }

    when (state) {
      is SosState.Countdown -> if (state.secondsRemaining > 0) {
        GloveButton("I'm okay · cancel", onCancelCountdown, testTag = "sos_i_am_okay_button")
      }
      else -> GloveButton("Return to ride", onReturnToRide, testTag = "sos_return_button")
    }
  }
}

@Composable
private fun ContactStatusCard(state: SosState) {
  val contactName = (state as? SosState.Triggered)?.receipt?.contactName
  Column(
    Modifier.fillMaxWidth().background(DarkSurfaceElevated, RoundedCornerShape(16.dp)).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text("Emergency contact", color = TealAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Text(
      when {
        !contactName.isNullOrBlank() -> contactName
        state is SosState.Error -> "Not available"
        else -> "First saved contact"
      },
      color = TextPrimary,
      fontSize = 18.sp
    )
    Text(
      when (state) {
        is SosState.Triggered -> "Selected for the event · not contacted"
        is SosState.Error -> "No event created"
        SosState.Cancelled -> "Countdown cancelled · not contacted"
        else -> "Selected after the countdown · no message will be sent"
      },
      color = WarningOrange,
      fontSize = 14.sp
    )
  }
}

private fun SosState.testTag(): String = when (this) {
  SosState.Idle -> "sos_state_idle"
  is SosState.Countdown -> "sos_state_countdown"
  SosState.Cancelled -> "sos_state_cancelled"
  is SosState.Triggered -> "sos_state_triggered"
  is SosState.Error -> "sos_state_error"
}
