package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HelmetStatus
import com.example.model.SafetyAlert
import com.example.ui.components.GloveButton
import com.example.ui.components.GloveOutlinedButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerGlow
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ImmersiveSlate
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealGlow
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.WhiteBorder
import com.example.ui.theme.WhiteGlass
import kotlinx.coroutines.delay

@Composable
fun ActiveRideScreen(
  helmetStatus: HelmetStatus,
  onSaveMoment: () -> Unit,
  onEndRide: () -> Unit,
  onOpenLiveCamera: () -> Unit,
  onOpenGenie: () -> Unit,
  onTriggerHazardAlert: () -> Unit,
  onTriggerCrashSos: () -> Unit,
  activeAlert: SafetyAlert? = null,
  modifier: Modifier = Modifier
) {
  // Simulated ride duration (seconds)
  var rideSeconds by remember { mutableIntStateOf(342) } // starts at 5m 42s
  // Simulated realistic speed with subtle natural fluctuations (km/h)
  var currentSpeed by remember { mutableIntStateOf(58) }
  var distanceKm by remember { mutableFloatStateOf(4.2f) }

  LaunchedEffect(Unit) {
    while (true) {
      delay(1000)
      rideSeconds++
      // Subtle realistic speed oscillation
      val variance = ((-3..3).random())
      currentSpeed = (currentSpeed + variance).coerceIn(46, 68)
      distanceKm += (currentSpeed / 3600f)
    }
  }

  val minutes = rideSeconds / 60
  val seconds = rideSeconds % 60
  val durationFormatted = String.format("%02d:%02d", minutes, seconds)

  // Blinking REC transition
  val recTransition = rememberInfiniteTransition(label = "recBlink")
  val recAlpha by recTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(700, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "recAlpha"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 18.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.SpaceBetween,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Top Immersive Header: Connected RideAware One & Battery/REC Capsule
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = if (helmetStatus.isConnected) "CONNECTED" else "OFFLINE",
          color = TealPrimary.copy(alpha = 0.7f),
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 2.sp
        )
        Text(
          text = "Geni One",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = (-0.3).sp
        )
      }

      // Sleek Pill Capsule: Battery & REC
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(100.dp))
          .background(Color.White.copy(alpha = 0.05f))
          .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
          .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(TealPrimary, CircleShape)
          )
          Text(
            text = "${helmetStatus.batteryPercent}%",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }

        Box(
          modifier = Modifier
            .width(1.dp)
            .height(12.dp)
            .background(Color.White.copy(alpha = 0.2f))
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "REC",
            color = Color.White.copy(alpha = if (recAlpha > 0.5f) 0.8f else 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
          Box(
            modifier = Modifier
              .size(6.dp)
              .background(DangerRed.copy(alpha = recAlpha), CircleShape)
          )
        }
      }
    }

    // Dual Camera Preview Cards (FRONT & REAR)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onOpenLiveCamera() }
        .testTag("active_ride_live_cameras"),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Front Camera Card
      Box(
        modifier = Modifier
          .weight(1f)
          .height(104.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(ImmersiveSlate)
          .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
          drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.35f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.65f),
            strokeWidth = 1.dp.toPx()
          )
          drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.65f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.35f),
            strokeWidth = 1.dp.toPx()
          )
        }
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
              )
            )
        )
        Row(
          modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Box(modifier = Modifier.size(5.dp).background(DangerRed.copy(alpha = recAlpha), CircleShape))
          Text("FRONT", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Text(
          text = "1080p 60fps",
          color = Color.White.copy(alpha = 0.5f),
          fontSize = 9.sp,
          modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        )
      }

      // Rear Camera Card
      Box(
        modifier = Modifier
          .weight(1f)
          .height(104.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(ImmersiveSlate)
          .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
          drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.6f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.4f),
            strokeWidth = 1.dp.toPx()
          )
        }
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
              )
            )
        )
        Row(
          modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Box(modifier = Modifier.size(5.dp).background(DangerRed.copy(alpha = recAlpha), CircleShape))
          Text("REAR", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Text(
          text = "AI Radar Active",
          color = TealAccent.copy(alpha = 0.7f),
          fontSize = 9.sp,
          modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        )
      }
    }

    // Center Speed HUD with Concentric Radar Rings
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      // Concentric Radar Rings from the Immersive UI design
      Canvas(modifier = Modifier.size(240.dp)) {
        drawCircle(
          color = TealPrimary.copy(alpha = 0.09f),
          style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
          color = TealPrimary.copy(alpha = 0.06f),
          radius = size.minDimension / 2.7f,
          style = Stroke(width = 1.dp.toPx())
        )
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = "$currentSpeed",
          color = Color.White,
          fontSize = 112.sp,
          fontWeight = FontWeight.Thin,
          lineHeight = 108.sp,
          letterSpacing = (-3).sp
        )
        Text(
          text = "KM/H",
          color = TealPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 4.sp,
          modifier = Modifier.padding(top = 0.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Duration & Distance Grid
        Row(
          horizontalArrangement = Arrangement.spacedBy(40.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "DURATION",
              color = Color.White.copy(alpha = 0.4f),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = durationFormatted,
              color = Color.White,
              fontSize = 20.sp,
              fontWeight = FontWeight.Medium
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "DISTANCE",
              color = Color.White.copy(alpha = 0.4f),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = String.format("%.1f km", distanceKm),
              color = Color.White,
              fontSize = 20.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }

    // Safety Alert Card (Safety Orange from Design HTML #FF5C00)
    if (activeAlert != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(WarningOrange)
          .clickable { onTriggerHazardAlert() }
          .padding(horizontal = 16.dp, vertical = 14.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
          ) {
            Text("!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "BLIND SPOT WARNING",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )
            Text(
              text = activeAlert.description.ifEmpty { "Vehicle approaching from rear right." },
              color = Color.White.copy(alpha = 0.9f),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    } else {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(Color.White.copy(alpha = 0.04f))
          .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
          .clickable { onTriggerHazardAlert() }
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(TealPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "360° AI RADAR GUARDING",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "All blind spots clear • Audio alerts active",
              color = Color.White.copy(alpha = 0.5f),
              fontSize = 10.sp
            )
          }
        }
      }
    }

    // Prototype Validation Triggers
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
        .padding(horizontal = 12.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Prototype Triggers:", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .background(WarningOrange.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .border(1.dp, WarningOrange, RoundedCornerShape(8.dp))
            .clickable { onTriggerHazardAlert() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("demo_trigger_hazard")
        ) {
          Text("Hazard Alert", color = WarningOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Box(
          modifier = Modifier
            .background(DangerRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .border(1.dp, DangerRed, RoundedCornerShape(8.dp))
            .clickable { onTriggerCrashSos() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("demo_trigger_sos")
        ) {
          Text("Crash SOS", color = DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // 3-Column Immersive Action Controls: Save Moment, Giant Genie Orb, End Ride
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 1: Save Moment Card Button
      Column(
        modifier = Modifier
          .weight(1f)
          .height(72.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(Color.White.copy(alpha = 0.05f))
          .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
          .clickable { onSaveMoment() }
          .testTag("active_ride_save_moment_button"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text("Save", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("MOMENT", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
      }

      // 2: Genie Glowing Center Orb Button
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(TealPrimary)
          .clickable { onOpenGenie() }
          .testTag("active_ride_genie"),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
              )
            )
        )
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(Icons.Filled.Mic, contentDescription = "Genie", tint = Color(0xFF020408), modifier = Modifier.size(26.dp))
          Text(
            text = "GENIE",
            color = Color(0xFF020408),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
          )
        }
      }

      // 3: End Ride Card Button
      Column(
        modifier = Modifier
          .weight(1f)
          .height(72.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(DangerRed.copy(alpha = 0.12f))
          .border(1.dp, DangerRed.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
          .clickable { onEndRide() }
          .testTag("active_ride_end_ride_button"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text("End", color = DangerRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("RIDE", color = DangerRed.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
      }
    }
  }
}
