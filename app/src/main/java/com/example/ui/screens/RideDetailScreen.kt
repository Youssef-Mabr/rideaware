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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.RideSummary
import com.example.ui.components.GloveButton
import com.example.ui.components.MetricCard
import com.example.ui.components.RideAwareTopBar
import com.example.ui.components.SimulatedRoadScene
import com.example.ui.theme.CyanAccent
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

@Composable
fun RideDetailScreen(
  ride: RideSummary,
  onWatchDualPlayback: (ProtectedClip) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val defaultClip = ride.protectedClips.firstOrNull() ?: ProtectedClip(
    id = "clip_fallback",
    title = "Protected Dual Angle Moment",
    timestamp = "10:14 AM",
    durationSeconds = 30,
    hazardTag = "Fast closing vehicle from behind",
    riskLevel = "HIGH",
    cameraSource = CameraSource.DUAL_SPLIT,
    frontThumbnail = "thumb_front_1",
    rearThumbnail = "thumb_rear_1",
    speedAtEventKmH = 64,
    coordinates = "25.2048° N, 55.2708° E"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    RideAwareTopBar(
      title = ride.title,
      subtitle = "${ride.date} • ${ride.distanceKm} km",
      onBack = onBack
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Metrics Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricCard(
          title = "Safety Score",
          value = "${ride.safetyScore}",
          unit = "/100",
          accentColor = TealPrimary,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "Duration",
          value = "${ride.durationMinutes}",
          unit = "mins",
          accentColor = CyanAccent,
          modifier = Modifier.weight(1f)
        )
      }

      // Event Timeline Section
      Text(
        text = "Event Timeline & Hazard Markers",
        color = TextPrimary,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Marker 1: Fast closing vehicle from behind (10:14)
        Row(verticalAlignment = Alignment.Top) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .background(WarningOrange.copy(alpha = 0.2f), CircleShape)
                .border(1.5.dp, WarningOrange, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Box(modifier = Modifier.size(8.dp).background(WarningOrange, CircleShape))
            }
            Box(modifier = Modifier.size(2.dp, 36.dp).background(DarkSurfaceBorder))
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Fast closing vehicle from behind", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("10:14 AM", color = WarningOrange, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Text("Rear Radar detected 28 km/h delta approach • 30s Dual Clip Locked", color = TextSecondary, fontSize = 11.sp)
          }
        }

        // Marker 2: Pedestrian warning near crosswalk (10:28)
        Row(verticalAlignment = Alignment.Top) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .background(TealPrimary.copy(alpha = 0.2f), CircleShape)
                .border(1.5.dp, TealPrimary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Box(modifier = Modifier.size(8.dp).background(TealPrimary, CircleShape))
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Pedestrian warning near crosswalk", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("10:28 AM", color = TealAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Text("Front AI camera flagged jaywalker stepping off curb • Alert Chime Sounded", color = TextSecondary, fontSize = 11.sp)
          }
        }
      }

      // Clip Preview Cards Section
      Text(
        text = "Synchronized Incident Vault Clips",
        color = TextPrimary,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
      )

      ride.protectedClips.forEach { clip ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onWatchDualPlayback(clip) }
            .testTag("clip_preview_card_${clip.id}"),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurface),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(WarningOrange.copy(alpha = 0.4f), DarkSurfaceBorder))
          )
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            // Video Thumbnail Simulation
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
            ) {
              SimulatedRoadScene(
                source = CameraSource.REAR,
                showDetectionBoxes = true,
                modifier = Modifier.fillMaxSize()
              )

              // Play Icon Overlay
              Box(
                modifier = Modifier
                  .size(46.dp)
                  .align(Alignment.Center)
                  .background(Color(0xBB000000), CircleShape)
                  .border(1.dp, TealPrimary, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Filled.PlayCircleFilled,
                  contentDescription = "Play",
                  tint = TealPrimary,
                  modifier = Modifier.size(36.dp)
                )
              }

              // Badges on Thumbnail
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Box(
                  modifier = Modifier
                    .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text("FRONT + REAR", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                  modifier = Modifier
                    .background(WarningOrange, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(clip.riskLevel, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(clip.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${clip.timestamp} • ${clip.durationSeconds}s • ${clip.speedAtEventKmH} km/h", color = TextSecondary, fontSize = 11.sp)
              }

              Icon(Icons.Filled.Lock, contentDescription = "Protected", tint = WarningOrange, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
    }

    // Action: Watch Synchronized Dual Playback Button
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      GloveButton(
        text = "Watch Synchronized Dual Playback",
        onClick = { onWatchDualPlayback(defaultClip) },
        icon = Icons.Filled.PlayCircleFilled,
        testTag = "watch_dual_playback_button"
      )
    }
  }
}
