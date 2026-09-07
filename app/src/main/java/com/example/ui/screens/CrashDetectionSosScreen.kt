package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EmergencyContact
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.GloveButton
import com.example.ui.components.GloveOutlinedButton
import com.example.ui.theme.DangerGlow
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun CrashDetectionSosScreen(
  emergencyContact: EmergencyContact,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier
) {
  var countdownSeconds by remember { mutableIntStateOf(30) }
  var isSosTriggered by remember { mutableStateOf(false) }

  // 30-second Countdown Timer
  LaunchedEffect(isSosTriggered) {
    if (!isSosTriggered) {
      while (countdownSeconds > 0) {
        delay(1000)
        countdownSeconds--
        if (countdownSeconds == 0) {
          isSosTriggered = true
        }
      }
    }
  }

  // Red Strobe Strobe Animation
  val strobeTransition = rememberInfiniteTransition(label = "strobe")
  val strobeAlpha by strobeTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(
      animation = tween(350, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "strobeAlpha"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0D0303))
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Top Warning Strobe Badge
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(DangerRed.copy(alpha = strobeAlpha * 0.3f), RoundedCornerShape(14.dp))
        .border(1.5.dp, DangerRed, RoundedCornerShape(14.dp))
        .padding(12.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(Icons.Filled.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(24.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = if (!isSosTriggered) "HELMET IMPACT SENSORS TRIGGERED" else "EMERGENCY DISPATCH INITIATED",
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp
      )
    }

    if (!isSosTriggered) {
      // Countdown Phase
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 12.dp)
      ) {
        Text(
          text = "Are you okay?",
          color = Color.White,
          fontSize = 32.sp,
          fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Severe deceleration detected by helmet IMU.",
          color = TextSecondary,
          fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Big Animated Countdown Ring
        Box(
          modifier = Modifier
            .size(170.dp)
            .background(DangerRed.copy(alpha = 0.15f), CircleShape)
            .border(4.dp, DangerRed.copy(alpha = strobeAlpha), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "$countdownSeconds",
              color = Color.White,
              fontSize = 58.sp,
              fontWeight = FontWeight.Black
            )
            Text(
              text = "SECONDS",
              color = DangerRed,
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sound & Strobe Indicator
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Filled.Emergency, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Helmet Loud Alarm & LED Strobe Active", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text("Alert will automatically notify ${emergencyContact.name} if no response", color = TextSecondary, fontSize = 10.sp)
            }
            AudioWaveformVisualizer(isAnimating = true, color = DangerRed, modifier = Modifier.width(42.dp).height(20.dp))
          }
        }
      }

      // Countdown Buttons: "I am okay" (Cancel) & "Send SOS Now"
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // "I am okay" Button
        Button(
          onClick = onCancel,
          modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .testTag("sos_i_am_okay_button"),
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = SuccessGreen,
            contentColor = Color(0xFF041912)
          )
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("I AM OKAY (CANCEL SOS)", fontSize = 17.sp, fontWeight = FontWeight.Black)
          }
        }

        // "Send SOS Now"
        Button(
          onClick = { isSosTriggered = true },
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("sos_send_now_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = DangerRed,
            contentColor = Color.White
          )
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send SOS Now", fontSize = 15.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
      // SOS Triggered / Dispatched State
      Column(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .background(DangerRed.copy(alpha = 0.2f), CircleShape)
            .border(2.dp, DangerRed, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Filled.Emergency, contentDescription = null, tint = DangerRed, modifier = Modifier.size(38.dp))
        }

        Text(
          text = "Emergency Alert Dispatched",
          color = Color.White,
          fontSize = 22.sp,
          fontWeight = FontWeight.Black,
          textAlign = TextAlign.Center
        )

        Text(
          text = "Your emergency contact and rescue services have received your location broadcast.",
          color = TextSecondary,
          fontSize = 13.sp,
          textAlign = TextAlign.Center
        )

        // GPS Coordinates Card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = DangerRed, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("Accident Location Transmitted:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text("25.2048° N, 55.2708° E (Sheikh Zayed Rd)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
          }
        }

        // SMS Message Preview
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("SMS Broadcast to ${emergencyContact.name}:", color = TealPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text("DELIVERED", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "\"RIDEAWAY CRASH ALERT: Youssef's helmet detected a severe crash at 25.2048N, 55.2708E. Emergency services notified. Front and rear telemetry clips are locked in cloud vault.\"",
              color = TextPrimary,
              fontSize = 12.sp,
              lineHeight = 17.sp
            )
          }
        }
      }

      // Action Buttons after SOS: Direct Call 911 / Cancel
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { /* simulated call */ },
          modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("call_emergency_services_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = DangerRed,
            contentColor = Color.White
          )
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("CALL RESCUE SERVICES (911/999)", fontSize = 15.sp, fontWeight = FontWeight.Black)
          }
        }

        GloveOutlinedButton(
          text = "Cancel Alarm & Return to Ride",
          onClick = onCancel,
          testTag = "sos_return_button"
        )
      }
    }
  }
}
