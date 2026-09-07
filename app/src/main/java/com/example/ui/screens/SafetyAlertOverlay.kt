package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HazardType
import com.example.model.SafetyAlert
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.GloveButton
import com.example.ui.components.GloveOutlinedButton
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerGlow
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange

fun getHazardIcon(type: HazardType): ImageVector = when (type) {
  HazardType.VEHICLE_APPROACHING_FAST -> Icons.Filled.Warning
  HazardType.PEDESTRIAN_CROSSING -> Icons.Filled.DirectionsWalk
  HazardType.BLIND_SPOT_OCCUPIED -> Icons.Filled.ElectricBike
  HazardType.TRAFFIC_LIGHT_CHANGE -> Icons.Filled.Traffic
  HazardType.UNSAFE_FOLLOWING_DISTANCE -> Icons.Filled.Warning
  HazardType.ROAD_HAZARD -> Icons.Filled.NotificationsActive
}

@Composable
fun SafetyAlertOverlay(
  alert: SafetyAlert,
  onSaveEvent: () -> Unit,
  onDismiss: () -> Unit,
  onSos: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isHighRisk = alert.riskLevel == com.example.model.RiskLevel.HIGH
  val primaryColor = if (isHighRisk) DangerRed else WarningOrange

  // Warning pulse animation
  val infiniteTransition = rememberInfiniteTransition(label = "hazardPulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xDD03080F))
      .padding(20.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(24.dp, spotColor = primaryColor.copy(alpha = 0.6f))
        .testTag("safety_alert_overlay_card"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
      border = CardDefaults.outlinedCardBorder().copy(
        brush = Brush.verticalGradient(
          listOf(primaryColor, primaryColor.copy(alpha = pulseAlpha))
        )
      )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top Badges: Risk Level & Direction Indicator
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Risk Level Pill
          Row(
            modifier = Modifier
              .background(primaryColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
              .border(1.dp, primaryColor, RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(modifier = Modifier.size(8.dp).background(primaryColor, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "RISK LEVEL: ${alert.riskLevel}",
              color = primaryColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Black
            )
          }

          // Direction of Danger
          Row(
            modifier = Modifier
              .background(DarkSurface, RoundedCornerShape(12.dp))
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = alert.direction.name.replace('_', ' '),
              color = TextPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Center Warning Icon
        Box(
          modifier = Modifier
            .size(72.dp)
            .background(primaryColor.copy(alpha = 0.16f), CircleShape)
            .border(2.dp, primaryColor, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = getHazardIcon(alert.hazardType),
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier.size(38.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Warning Title & Directional Description
        Text(
          text = alert.title,
          color = TextPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = alert.description,
          color = TextSecondary,
          fontSize = 14.sp,
          textAlign = TextAlign.Center,
          lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Voice Warning Indicator Bar (Audio Waveform)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.Filled.VolumeUp,
              contentDescription = null,
              tint = TealAccent,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Helmet Voice Chime:",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "\"${alert.voiceWarning}\"",
                color = TealAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
            AudioWaveformVisualizer(
              isAnimating = true,
              color = TealPrimary,
              modifier = Modifier.width(48.dp).height(24.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Protected Clip Simulation Note
        Text(
          text = "10 seconds before and 20 seconds after event protected.",
          color = TextMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Three Action Buttons: Save Event, Dismiss, SOS
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          GloveButton(
            text = "Save Event (Protected)",
            onClick = onSaveEvent,
            icon = Icons.Filled.Bookmark,
            testTag = "alert_save_event_button"
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            GloveOutlinedButton(
              text = "Dismiss",
              onClick = onDismiss,
              modifier = Modifier.weight(1f),
              testTag = "alert_dismiss_button"
            )

            Button(
              onClick = onSos,
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("alert_sos_button"),
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = DangerRed,
                contentColor = Color.White
              )
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Emergency, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SOS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              }
            }
          }
        }
      }
    }
  }
}
