package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ErrorDemoType
import com.example.model.HelmetStatus
import com.example.ui.components.EmptyErrorStateView
import com.example.ui.components.ErrorStatesDemoSheet
import com.example.ui.components.GloveButton
import com.example.ui.components.GloveOutlinedButton
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealGlow
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange

@Composable
fun HomeScreen(
  helmetStatus: HelmetStatus,
  currentError: ErrorDemoType,
  onSelectError: (ErrorDemoType) -> Unit,
  onStartRide: () -> Unit,
  onOpenLiveCameras: () -> Unit,
  onOpenGenie: () -> Unit,
  modifier: Modifier = Modifier
) {
  val systemReady = helmetStatus.isConnected && helmetStatus.frontCameraReady && helmetStatus.rearCameraReady && helmetStatus.audioReady && helmetStatus.batteryPercent > 20 && helmetStatus.storageFreeGb > 5 && currentError == ErrorDemoType.NONE
  var showDemoErrorSheet by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
  ) {
    // Header Row with User Greeting & Device Connection Indicator
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Good morning,",
          color = TextSecondary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = "Youssef",
          color = TextPrimary,
          fontSize = 26.sp,
          fontWeight = FontWeight.Black
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        StatusPill(
          label = if (helmetStatus.isConnected) "Helmet" else "Disconnected",
          isActive = helmetStatus.isConnected,
          activeColor = TealPrimary,
          inactiveColor = DangerRed
        )

        Spacer(modifier = Modifier.width(8.dp))

        // State Simulator Launcher Icon Button
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, CircleShape)
            .clickable(role = Role.Button) { showDemoErrorSheet = true }
            .testTag("state_simulator_launcher"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = "Test Error States",
            tint = if (currentError != ErrorDemoType.NONE) WarningOrange else TealAccent,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Active Empty/Error State Demo banner (if simulator active)
      if (currentError != ErrorDemoType.NONE) {
        EmptyErrorStateView(
          errorType = currentError,
          onActionClick = {
            onSelectError(ErrorDemoType.NONE)
          },
          onDismiss = {
            onSelectError(ErrorDemoType.NONE)
          }
        )
      }

      // System Readiness Banner
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(Color.White.copy(alpha = 0.04f))
          .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
          .padding(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .background(TealPrimary.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, TealPrimary.copy(alpha = 0.5f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
              Text(
                text = if (systemReady) "Ready to ride" else "Needs attention",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (systemReady) "Sample readings · no helmet connected" else "Review the status below before continuing",
                color = TealAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }

      // Large Glove-Friendly Primary "Start Ride" Action
      Button(
        onClick = onStartRide,
        enabled = systemReady,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 80.dp)
          .testTag("home_start_ride_button"),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = TealPrimary,
          contentColor = Color(0xFF020408)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .background(Color(0xFF020408).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Filled.PlayArrow,
              contentDescription = null,
              tint = Color(0xFF020408),
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Start ride",
              fontSize = 19.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "Review checks, then explore a sample ride",
              fontSize = 12.sp,
              color = Color(0xFF020408).copy(alpha = 0.8f),
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // Quick Action Dual Buttons: Live Cameras & Genie Voice Assistant
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Live Cameras Quick Button
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .clickable(role = Role.Button) { onOpenLiveCameras() }
            .padding(16.dp)
            .testTag("home_live_cameras_button")
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .background(TealGlow, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Filled.Videocam,
                  contentDescription = null,
                  tint = TealPrimary,
                  modifier = Modifier.size(22.dp)
                )
              }
              // Live red pulse
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(DangerRed, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("GENI", color = TealAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Camera preview",
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Simulated front & rear",
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }

        // Genie Voice Assistant Quick Button
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .clickable(role = Role.Button) { onOpenGenie() }
            .padding(16.dp)
            .testTag("home_genie_button")
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .background(Color(0x333DEFE7), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Filled.Mic,
                  contentDescription = null,
                  tint = CyanAccent,
                  modifier = Modifier.size(22.dp)
                )
              }
              Text("GENI", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Geni Voice",
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Try sample responses",
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }
      }

      // Helmet Telemetry Grid (Battery, Storage, Cameras, Audio, GPS)
      Text(
        text = "Sample helmet status",
        color = TextPrimary,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricCard(
          title = "Battery",
          value = "${helmetStatus.batteryPercent}%",
          unit = "sample reading",
          icon = Icons.Filled.BatteryChargingFull,
          accentColor = SuccessGreen,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "Storage",
          value = "${helmetStatus.storageFreeGb}",
          unit = "GB free",
          icon = Icons.Filled.SdCard,
          accentColor = CyanAccent,
          modifier = Modifier.weight(1f)
        )
      }

      // Hardware Readiness Matrix Rows
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(DarkSurface)
          .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Front Camera
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Videocam, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Front camera", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
          }
          StatusPill(label = if (helmetStatus.frontCameraReady) "Ready" else "Unavailable", isActive = helmetStatus.frontCameraReady)
        }

        // Rear Camera
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Videocam, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Rear camera", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
          }
          StatusPill(label = if (helmetStatus.rearCameraReady) "Ready" else "Unavailable", isActive = helmetStatus.rearCameraReady)
        }

        // Intercom Audio
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Headset, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Audio & microphone", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
          }
          StatusPill(label = if (helmetStatus.audioReady) "Ready" else "Unavailable", isActive = helmetStatus.audioReady)
        }

        // GPS Telemetry
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Helmet GPS Signal", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
          }
          StatusPill(label = helmetStatus.gpsStatus, isActive = true)
        }
      }

      // Subtle Note on Phone Position
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
          .padding(12.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Set up your ride while parked. Stay aware of your surroundings.",
          color = TextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
    }
  }

  // Error States Demo Bottom Sheet
  if (showDemoErrorSheet) {
    ErrorStatesDemoSheet(
      currentError = currentError,
      onSelectError = onSelectError,
      onDismiss = { showDemoErrorSheet = false }
    )
  }
}
