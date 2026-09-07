package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EmergencyContact
import com.example.model.HelmetStatus
import com.example.ui.components.GloveButton
import com.example.ui.components.RideAwareTopBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class PreRideItem(
  val title: String,
  val value: String,
  val icon: ImageVector,
  val isReady: Boolean
)

@Composable
fun PreRideScreen(
  helmetStatus: HelmetStatus,
  emergencyContact: EmergencyContact,
  onStartActiveRide: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val checklist = listOf(
    PreRideItem("Front Camera", "1080p 60fps Active", Icons.Filled.CameraAlt, helmetStatus.frontCameraReady),
    PreRideItem("Rear Camera", "Blind-Zone Radar Active", Icons.Filled.Videocam, helmetStatus.rearCameraReady),
    PreRideItem("Helmet Battery", "${helmetStatus.batteryPercent}% (5h 20m remaining)", Icons.Filled.BatteryChargingFull, helmetStatus.batteryPercent > 20),
    PreRideItem("Storage Available", "${helmetStatus.storageFreeGb} GB free (Vault ready)", Icons.Filled.SdCard, helmetStatus.storageFreeGb > 5),
    PreRideItem("GPS Signal", "Strong (Lock established)", Icons.Filled.LocationOn, true),
    PreRideItem("Audio System", "Intercom & Mic calibrated", Icons.Filled.Headset, helmetStatus.audioReady),
    PreRideItem("Emergency Contact", "${emergencyContact.name} (${emergencyContact.relationship})", Icons.Filled.ContactPhone, true)
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    RideAwareTopBar(
      title = "Pre-Ride Verification",
      subtitle = "Quick flight-check for maximum safety",
      onBack = onBack
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text(
        text = "Ready to Roll?",
        color = TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black
      )

      // Safety Notice Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, TealPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
          .padding(16.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .background(TealPrimary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Filled.Security, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Keep your phone in your pocket. Voice and helmet controls remain active.",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      checklist.forEach { item ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .background(TealPrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = null,
                  tint = TealPrimary,
                  modifier = Modifier.size(17.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = item.title,
                  color = TextPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = item.value,
                  color = TextSecondary,
                  fontSize = 11.sp
                )
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Ready",
                color = SuccessGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // Bottom Action
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      GloveButton(
        text = "Start Recording & Ride",
        onClick = onStartActiveRide,
        icon = Icons.Filled.PlayArrow,
        testTag = "start_recording_and_ride_button"
      )
    }
  }
}
