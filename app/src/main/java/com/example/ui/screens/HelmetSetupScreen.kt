package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraSource
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.GloveButton
import com.example.ui.components.GloveOutlinedButton
import com.example.ui.components.RideAwareTopBar
import com.example.ui.components.SimulatedRoadScene
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
import kotlinx.coroutines.delay

data class HardwareCheck(
  val id: String,
  val title: String,
  val subtitle: String,
  val icon: ImageVector
)

@Composable
fun HelmetSetupScreen(
  onFinishSetup: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val checks = listOf(
    HardwareCheck("bt", "Bluetooth Connection", "AptX Low Latency Audio Link", Icons.Filled.Bluetooth),
    HardwareCheck("wifi", "Helmet Wi-Fi Stream", "5GHz Direct Camera Pipeline", Icons.Filled.Wifi),
    HardwareCheck("front_cam", "Front Camera", "1080p 60fps HDR Active", Icons.Filled.CameraAlt),
    HardwareCheck("rear_cam", "Rear Camera", "1080p 60fps Blind-Zone Radar", Icons.Filled.Videocam),
    HardwareCheck("intercom", "Intercom Audio", "Dual Noise-Canceling Speakers", Icons.Filled.Headset),
    HardwareCheck("mic", "Microphone", "Wind-Canceling Beamforming Mic", Icons.Filled.Mic),
    HardwareCheck("battery", "Battery Level", "86% Available (Approx 5.5h)", Icons.Filled.BatteryChargingFull),
    HardwareCheck("storage", "Storage Memory", "72 GB Free / 128 GB Vault", Icons.Filled.SdCard)
  )

  // Stepwise check completion simulation
  var completedCount by remember { mutableIntStateOf(0) }
  var showAudioTestDialog by remember { mutableStateOf(false) }
  var showCameraTestDialog by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    for (i in 1..checks.size) {
      delay(350)
      completedCount = i
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    RideAwareTopBar(
      title = "Helmet Setup & Testing",
      subtitle = "Diagnostic telemetry verification",
      onBack = onBack
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Geni One Telemetry",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = if (completedCount == checks.size) "All 8 diagnostic modules operational." else "Verifying helmet hardware...",
            color = if (completedCount == checks.size) TealPrimary else TextSecondary,
            fontSize = 12.sp
          )
        }

        Box(
          modifier = Modifier
            .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(
            text = "$completedCount/${checks.size}",
            color = if (completedCount == checks.size) SuccessGreen else TealAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
          )
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      checks.forEachIndexed { index, item ->
        val isReady = index < completedCount

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(
              1.dp,
              if (isReady) TealPrimary.copy(alpha = 0.25f) else DarkSurfaceBorder,
              RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(
                  if (isReady) TealPrimary.copy(alpha = 0.15f) else DarkSurfaceElevated,
                  CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (isReady) TealPrimary else TextMuted,
                modifier = Modifier.size(18.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = item.subtitle,
                color = TextSecondary,
                fontSize = 11.sp
              )
            }

            if (isReady) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Icon(
                  imageVector = Icons.Filled.Check,
                  contentDescription = null,
                  tint = SuccessGreen,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Ready",
                  color = SuccessGreen,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            } else {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                CircularProgressIndicator(
                  color = TealPrimary,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Checking",
                  color = TextMuted,
                  fontSize = 11.sp
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Diagnostic Tools (Test Audio, Test Cameras)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        GloveOutlinedButton(
          text = "Test Audio",
          onClick = { showAudioTestDialog = true },
          icon = Icons.Filled.VolumeUp,
          modifier = Modifier.weight(1f),
          testTag = "test_audio_button"
        )

        GloveOutlinedButton(
          text = "Test Cameras",
          onClick = { showCameraTestDialog = true },
          icon = Icons.Filled.CameraAlt,
          modifier = Modifier.weight(1f),
          testTag = "test_cameras_button"
        )
      }
    }

    // Finish Setup Primary Action
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      GloveButton(
        text = "Finish Setup",
        onClick = onFinishSetup,
        enabled = completedCount >= checks.size,
        icon = Icons.Filled.CheckCircle,
        testTag = "finish_setup_button"
      )
    }
  }

  // Audio Testing Modal Dialog
  if (showAudioTestDialog) {
    AlertDialog(
      onDismissRequest = { showAudioTestDialog = false },
      containerColor = DarkSurface,
      title = {
        Text("Testing Helmet Speakers", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
      },
      text = {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
          Text(
            text = "Playing high-frequency intercom diagnostic tone & vocal prompt to helmet speakers.",
            color = TextSecondary,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(18.dp))
          AudioWaveformVisualizer(
            isAnimating = true,
            color = TealPrimary,
            modifier = Modifier.fillMaxWidth().height(48.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "\"Genie: Intercom audio calibrated at 100% clarity.\"",
            color = TealAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      },
      confirmButton = {
        GloveButton(
          text = "Audio Sounds Good",
          onClick = { showAudioTestDialog = false },
          modifier = Modifier.width(180.dp),
          testTag = "audio_dialog_confirm"
        )
      }
    )
  }

  // Camera Testing Modal Dialog
  if (showCameraTestDialog) {
    AlertDialog(
      onDismissRequest = { showCameraTestDialog = false },
      containerColor = DarkSurface,
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Camera Check (Dual Feed)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
          IconButton(onClick = { showCameraTestDialog = false }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextMuted)
          }
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text("Front Camera (Horizon View):", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          SimulatedRoadScene(
            source = CameraSource.FRONT,
            showDetectionBoxes = true,
            modifier = Modifier.fillMaxWidth().height(120.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))

          Text("Rear Camera (Blind Zone Radar):", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          SimulatedRoadScene(
            source = CameraSource.REAR,
            showDetectionBoxes = true,
            modifier = Modifier.fillMaxWidth().height(120.dp)
          )
        }
      },
      confirmButton = {
        GloveButton(
          text = "Cameras Confirmed Ready",
          onClick = { showCameraTestDialog = false },
          modifier = Modifier.fillMaxWidth(),
          testTag = "camera_dialog_confirm"
        )
      }
    )
  }
}
