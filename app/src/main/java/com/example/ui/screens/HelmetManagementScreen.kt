package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HelmetStatus
import com.example.ui.components.GloveButton
import com.example.ui.components.GloveOutlinedButton
import com.example.ui.components.RideAwareTopBar
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
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
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.delay

@Composable
fun HelmetManagementScreen(
  helmetStatus: HelmetStatus,
  onUnpairHelmet: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isUpdatingFirmware by remember { mutableStateOf(false) }
  var updateProgress by remember { mutableFloatStateOf(0f) }
  var showUnpairDialog by remember { mutableStateOf(false) }

  // Toggles
  var frontHdr by remember { mutableStateOf(true) }
  var rearBlindSpotRadar by remember { mutableStateOf(true) }
  var windNoiseSuppression by remember { mutableStateOf(true) }

  LaunchedEffect(isUpdatingFirmware) {
    if (isUpdatingFirmware) {
      updateProgress = 0f
      while (updateProgress < 1f) {
        delay(300)
        updateProgress += 0.15f
      }
      isUpdatingFirmware = false
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
  ) {
    RideAwareTopBar(
      title = "Helmet Management",
      subtitle = "Geni One • Connected"
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Device Info Header Card
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(DarkSurface)
          .border(1.dp, TealPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
          .padding(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .background(TealPrimary.copy(alpha = 0.15f), CircleShape)
                .border(1.5.dp, TealPrimary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Filled.Security, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("Geni One", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
              Text("Hardware Rev 3 • Serial #GENI-8820", color = TextSecondary, fontSize = 11.sp)
            }
          }

          Box(
            modifier = Modifier
              .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Text("CONNECTED", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
          }
        }
      }

      // Firmware Section
      Text("Firmware & Updates", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
          .padding(16.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Firmware Version v2.4.1", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
              Text(
                text = if (isUpdatingFirmware) "Transmitting OTA binary via 5GHz Wi-Fi..." else "Latest build with low-latency radar",
                color = if (isUpdatingFirmware) TealAccent else TextSecondary,
                fontSize = 11.sp
              )
            }

            if (!isUpdatingFirmware) {
              GloveOutlinedButton(
                text = "Check Update",
                onClick = { isUpdatingFirmware = true },
                modifier = Modifier.width(130.dp),
                testTag = "check_firmware_update"
              )
            }
          }

          if (isUpdatingFirmware) {
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
              progress = { updateProgress.coerceIn(0f, 1f) },
              color = TealPrimary,
              trackColor = DarkSurfaceElevated,
              modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Flash Progress: ${(updateProgress * 100).toInt()}%",
              color = TealAccent,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Camera Hardware Settings
      Text("Sensors & Optics", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Front Camera HDR
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Front Camera 1080p HDR", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              Text("Wide dynamic range for bright sunlight & tunnels", color = TextSecondary, fontSize = 11.sp)
            }
          }
          Switch(
            checked = frontHdr,
            onCheckedChange = { frontHdr = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF041912), checkedTrackColor = TealPrimary)
          )
        }

        // Rear Blind Spot Radar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Videocam, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Rear Blind-Spot Radar Guard", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              Text("Audible chimes when approaching speed > 15 km/h", color = TextSecondary, fontSize = 11.sp)
            }
          }
          Switch(
            checked = rearBlindSpotRadar,
            onCheckedChange = { rearBlindSpotRadar = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF041912), checkedTrackColor = TealPrimary)
          )
        }

        // Wind Noise Suppression
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Headset, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Dual-Mic Beamforming Noise Cut", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              Text("Cancels high speed aerodynamic turbulence", color = TextSecondary, fontSize = 11.sp)
            }
          }
          Switch(
            checked = windNoiseSuppression,
            onCheckedChange = { windNoiseSuppression = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF041912), checkedTrackColor = TealPrimary)
          )
        }
      }

      // Storage Management
      Text("Storage & Memory Vault", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
          .padding(16.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("High Endurance MicroSD", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("72 GB Free / 128 GB", color = TealAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          LinearProgressIndicator(
            progress = { 0.44f },
            color = TealPrimary,
            trackColor = DarkSurfaceElevated,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            GloveOutlinedButton(
              text = "Format SD Card",
              onClick = { /* simulated format */ },
              modifier = Modifier.weight(1f),
              testTag = "format_sd_button"
            )
            GloveOutlinedButton(
              text = "Backup All Vault Clips",
              onClick = { /* simulated backup */ },
              modifier = Modifier.weight(1f),
              testTag = "backup_clips_button"
            )
          }
        }
      }

      // Disconnect or Unpair Helmet Option
      GloveButton(
        text = "Unpair Geni One Helmet",
        onClick = { showUnpairDialog = true },
        icon = Icons.Filled.BluetoothDisabled,
        isDanger = true,
        testTag = "unpair_helmet_button"
      )

      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  // Unpair Confirmation Dialog
  if (showUnpairDialog) {
    AlertDialog(
      onDismissRequest = { showUnpairDialog = false },
      containerColor = DarkSurface,
      title = {
        Text("Unpair Helmet?", color = TextPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          "Unpairing will disconnect Bluetooth telemetry and 5GHz video links until scanned again.",
          color = TextSecondary,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        GloveButton(
          text = "Confirm Unpair",
          onClick = {
            showUnpairDialog = false
            onUnpairHelmet()
          },
          isDanger = true,
          modifier = Modifier.width(160.dp),
          testTag = "confirm_unpair_button"
        )
      },
      dismissButton = {
        TextButton(onClick = { showUnpairDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}
