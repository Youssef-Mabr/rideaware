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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.model.RideSummary
import com.example.ui.components.GloveButton
import com.example.ui.components.GloveOutlinedButton
import com.example.ui.components.MetricCard
import com.example.ui.components.RideAwareTopBar
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
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.delay

@Composable
fun RideSummaryScreen(
  rideSummary: RideSummary,
  onViewClips: () -> Unit,
  onBackToHome: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isSavingLogs by remember { mutableStateOf(true) }
  var isSavedConfirmed by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    delay(1200)
    isSavingLogs = false
    isSavedConfirmed = true
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    RideAwareTopBar(
      title = "Ride Summary",
      subtitle = "${rideSummary.date} • ${rideSummary.startTime} - ${rideSummary.endTime}",
      onBack = onBackToHome
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Syncing / Saving Status Header
      if (isSavingLogs) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, TealPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = TealPrimary, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Encrypting ride logs & syncing protected vault clips...", color = TealAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
          }
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF04241C))
            .border(1.dp, SuccessGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Ride safely saved to offline helmet vault & log history.", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Safety Score Hero Display
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(22.dp))
          .padding(20.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "SAFETY SCORE",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )

          Spacer(modifier = Modifier.height(10.dp))

          Box(
            modifier = Modifier
              .size(110.dp)
              .background(TealPrimary.copy(alpha = 0.12f), CircleShape)
              .border(3.dp, TealPrimary, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "${rideSummary.safetyScore}",
                color = TextPrimary,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black
              )
              Text(
                text = "/ 100",
                color = TealAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "Smooth throttle control, zero panic stops.",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      // Telemetry Metric Cards 2x2 Grid
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricCard(
          title = "Total Distance",
          value = "${rideSummary.distanceKm}",
          unit = "km",
          accentColor = TealPrimary,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "Duration",
          value = "${rideSummary.durationMinutes}",
          unit = "mins",
          accentColor = CyanAccent,
          modifier = Modifier.weight(1f)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricCard(
          title = "Average Speed",
          value = "${rideSummary.averageSpeedKmH}",
          unit = "km/h",
          accentColor = TealPrimary,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "Max Speed",
          value = "${rideSummary.maxSpeedKmH}",
          unit = "km/h",
          accentColor = WarningOrange,
          modifier = Modifier.weight(1f)
        )
      }

      // Safety Events & Battery Breakdown Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricCard(
          title = "Safety Events",
          value = "${rideSummary.safetyEventCount}",
          unit = "protected",
          accentColor = WarningOrange,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "Battery Consumed",
          value = "${rideSummary.batteryConsumedPercent}%",
          unit = "helmet",
          accentColor = SuccessGreen,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Action Buttons
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      GloveButton(
        text = "View Recorded Clips (${rideSummary.safetyEventCount})",
        onClick = onViewClips,
        icon = Icons.Filled.Videocam,
        testTag = "summary_view_clips_button"
      )

      GloveOutlinedButton(
        text = "Back to Home",
        onClick = onBackToHome,
        icon = Icons.Filled.Home,
        testTag = "summary_back_home_button"
      )
    }
  }
}
