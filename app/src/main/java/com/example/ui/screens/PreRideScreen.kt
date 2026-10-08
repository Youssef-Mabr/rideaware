package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EmergencyContact
import com.example.model.HelmetStatus
import com.example.model.preRideChecks
import com.example.ui.components.GloveButton
import com.example.ui.components.RideAwareTopBar
import com.example.ui.theme.*

@Composable
fun PreRideScreen(
  helmetStatus: HelmetStatus,
  emergencyContact: EmergencyContact,
  onStartActiveRide: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val checks = preRideChecks(helmetStatus, emergencyContact)
  val ready = checks.all { it.ready }
  Column(modifier.fillMaxSize().background(DarkCanvas)) {
    RideAwareTopBar("Pre-ride check", "Review your checks before starting", onBack)
    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(if (ready) "Ready to start your ride" else "Some checks need attention", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
      Text("Complete these checks while parked before starting your ride.", color = TextSecondary, fontSize = 14.sp)
      checks.forEach { check ->
        val color = if (check.ready) SuccessGreen else WarningOrange
        Row(
          Modifier.fillMaxWidth().background(DarkSurface, RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) {}.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(if (check.ready) Icons.Filled.CheckCircle else Icons.Filled.Warning, null, tint = color)
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(check.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(check.detail, color = TextSecondary, fontSize = 13.sp)
            Text(if (check.ready) "Ready" else "Needs attention", color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
          }
        }
      }
      if (!ready) Text("Go back to check your helmet or update your contact in Settings.", color = WarningOrange, fontSize = 14.sp, modifier = Modifier.testTag("pre_ride_blocked_reason"))
      Spacer(Modifier.height(8.dp))
    }
    GloveButton(
      text = "Start ride", onClick = onStartActiveRide, enabled = ready,
      icon = Icons.Filled.PlayArrow, modifier = Modifier.padding(20.dp),
      testTag = "start_recording_and_ride_button"
    )
  }
}
