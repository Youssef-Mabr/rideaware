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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.model.CameraSource
import com.example.model.ProtectedClip
import com.example.ui.components.GloveButton
import com.example.ui.components.GloveOutlinedButton
import com.example.ui.components.RideAwareTopBar
import com.example.ui.components.SimulatedRoadScene
import com.example.ui.theme.CyanAccent
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
import kotlinx.coroutines.delay

@Composable
fun VideoPlaybackScreen(
  clip: ProtectedClip,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isPlaying by remember { mutableStateOf(true) }
  var currentPositionSeconds by remember { mutableFloatStateOf(10f) }
  val totalDuration = clip.durationSeconds.toFloat()
  var exportDialogMessage by remember { mutableStateOf<String?>(null) }

  // Playback timer ticker
  LaunchedEffect(isPlaying) {
    while (isPlaying) {
      delay(500)
      if (currentPositionSeconds < totalDuration) {
        currentPositionSeconds = (currentPositionSeconds + 0.5f).coerceAtMost(totalDuration)
      } else {
        isPlaying = false
      }
    }
  }

  val formattedCurrent = String.format("00:%02d", currentPositionSeconds.toInt())
  val formattedTotal = String.format("00:%02d", totalDuration.toInt())

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    RideAwareTopBar(
      title = clip.title,
      subtitle = "${clip.timestamp} • ${clip.hazardTag}",
      onBack = onBack
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Synchronized Dual-Camera Video Players (Front & Rear stacked)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
          .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Front Angle Player
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
        ) {
          SimulatedRoadScene(
            source = CameraSource.FRONT,
            showDetectionBoxes = true,
            modifier = Modifier.fillMaxSize()
          )

          // Front tag
          Box(
            modifier = Modifier
              .padding(8.dp)
              .align(Alignment.BottomStart)
              .background(Color(0xCC000000), RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text("FRONT 1080p SYNC", color = TealAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Rear Angle Player
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
        ) {
          SimulatedRoadScene(
            source = CameraSource.REAR,
            showDetectionBoxes = true,
            modifier = Modifier.fillMaxSize()
          )

          // Rear tag
          Box(
            modifier = Modifier
              .padding(8.dp)
              .align(Alignment.BottomStart)
              .background(Color(0xCC000000), RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text("REAR 1080p RADAR SYNC", color = WarningOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Telemetry Overlay HUD Banner
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
          .padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Speed
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Speed, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("${clip.speedAtEventKmH} km/h", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
          }

          // GPS
          Text(clip.coordinates, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)

          // Hazard Tag
          Row(
            modifier = Modifier
              .background(WarningOrange.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(clip.riskLevel, color = WarningOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Playback Controls & Timeline Slider
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
          .padding(16.dp)
      ) {
        // Timeline Slider with Hazard Marker Indicator
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(formattedCurrent, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          Text("Hazard Event @ 00:10", color = WarningOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Text(formattedTotal, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        Slider(
          value = currentPositionSeconds,
          onValueChange = {
            currentPositionSeconds = it
          },
          valueRange = 0f..totalDuration,
          colors = SliderDefaults.colors(
            thumbColor = TealPrimary,
            activeTrackColor = TealPrimary,
            inactiveTrackColor = DarkSurfaceElevated
          ),
          modifier = Modifier.testTag("playback_timeline_slider")
        )

        // Transport Controls Row (Jump -10s, Play/Pause, Jump +10s)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // -10s
          IconButton(
            onClick = { currentPositionSeconds = (currentPositionSeconds - 10f).coerceAtLeast(0f) },
            modifier = Modifier.size(48.dp).testTag("playback_jump_back_10")
          ) {
            Icon(Icons.Filled.Replay10, contentDescription = "Back 10s", tint = TextPrimary, modifier = Modifier.size(28.dp))
          }

          Spacer(modifier = Modifier.width(16.dp))

          // Play/Pause Big Button
          Box(
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(TealPrimary)
              .clickable { isPlaying = !isPlaying }
              .testTag("playback_play_pause_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (isPlaying) "Pause" else "Play",
              tint = Color(0xFF041912),
              modifier = Modifier.size(32.dp)
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          // +10s
          IconButton(
            onClick = { currentPositionSeconds = (currentPositionSeconds + 10f).coerceAtMost(totalDuration) },
            modifier = Modifier.size(48.dp).testTag("playback_jump_forward_10")
          ) {
            Icon(Icons.Filled.Forward10, contentDescription = "Forward 10s", tint = TextPrimary, modifier = Modifier.size(28.dp))
          }
        }
      }

      // Video Export Options Section
      Text(
        text = "Export Incident Evidence",
        color = TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        GloveOutlinedButton(
          text = "Front (1080p)",
          onClick = { exportDialogMessage = "Front camera incident video exported to Gallery (vault_clip_front.mp4)." },
          icon = Icons.Filled.FileDownload,
          modifier = Modifier.weight(1f),
          testTag = "export_front_button"
        )

        GloveOutlinedButton(
          text = "Rear Radar",
          onClick = { exportDialogMessage = "Rear camera radar clip exported to Gallery (vault_clip_rear.mp4)." },
          icon = Icons.Filled.FileDownload,
          modifier = Modifier.weight(1f),
          testTag = "export_rear_button"
        )
      }

      GloveButton(
        text = "Export Both Angles (Synchronized Dual)",
        onClick = { exportDialogMessage = "Synchronized dual-angle package with GPS & speed telemetry exported." },
        icon = Icons.Filled.Share,
        testTag = "export_both_button"
      )

      Spacer(modifier = Modifier.height(10.dp))
    }
  }

  // Export Confirmation Dialog
  if (exportDialogMessage != null) {
    AlertDialog(
      onDismissRequest = { exportDialogMessage = null },
      containerColor = DarkSurface,
      title = {
        Text("Export Successful", color = TextPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(exportDialogMessage ?: "", color = TextSecondary, fontSize = 13.sp)
      },
      confirmButton = {
        GloveButton(
          text = "Done",
          onClick = { exportDialogMessage = null },
          modifier = Modifier.width(120.dp),
          testTag = "export_confirm_done"
        )
      }
    )
  }
}
