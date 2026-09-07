package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.GloveButton
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealGlow
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(
  onGetStarted: () -> Unit,
  onAlreadyPaired: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "radarRings")
  val radarPulse by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "radarPulse"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
      .navigationBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Brand Top Bar
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .background(TealPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .border(1.dp, TealPrimary, RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Geni",
          color = TextPrimary,
          fontSize = 24.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.sp
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "YOUR GUARDIAN HELMET COMPANION",
        color = TealAccent,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Smart Helmet Hero Card with Front & Rear Awareness Visualization
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(280.dp),
      contentAlignment = Alignment.Center
    ) {
      // Background Radar Awareness Arcs Canvas
      Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width * 0.44f

        // Rear awareness radar cone (behind)
        drawArc(
          color = CyanAccent.copy(alpha = 0.15f * (1f - radarPulse)),
          startAngle = 50f,
          sweepAngle = 80f,
          useCenter = true,
          topLeft = Offset(center.x - maxRadius * radarPulse, center.y - maxRadius * radarPulse),
          size = Size(maxRadius * 2 * radarPulse, maxRadius * 2 * radarPulse)
        )

        // Front awareness radar cone (ahead)
        drawArc(
          color = TealPrimary.copy(alpha = 0.18f * (1f - radarPulse)),
          startAngle = 230f,
          sweepAngle = 80f,
          useCenter = true,
          topLeft = Offset(center.x - maxRadius * radarPulse, center.y - maxRadius * radarPulse),
          size = Size(maxRadius * 2 * radarPulse, maxRadius * 2 * radarPulse)
        )

        // Fixed outer perimeter ring
        drawCircle(
          color = TealGlow,
          radius = maxRadius,
          center = center,
          style = Stroke(width = 1.dp.toPx())
        )
      }

      // Helmet Hero Image
      Box(
        modifier = Modifier
          .size(210.dp)
          .clip(CircleShape)
          .background(DarkSurfaceElevated)
          .border(2.dp, TealPrimary.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_smart_helmet_hero),
          contentDescription = "Geni One Smart Helmet",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
      }

      // Awareness Sensor Badges
      // Front Camera Badge
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .background(Color(0xEE091322), RoundedCornerShape(14.dp))
          .border(1.dp, TealPrimary, RoundedCornerShape(14.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Videocam, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("FRONT 1080p AWARENESS", color = TealAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Rear Camera Badge
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .background(Color(0xEE091322), RoundedCornerShape(14.dp))
          .border(1.dp, CyanAccent, RoundedCornerShape(14.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Videocam, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("REAR BLIND-ZONE RADAR", color = CyanAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Title & Subtitle
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = "Ride smarter.\nStay aware.",
        color = TextPrimary,
        fontSize = 32.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        lineHeight = 38.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Inspired by Ali Baba's legendary guardian. Geni guards every road ahead and behind you.",
        color = TextSecondary,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
      )

      Spacer(modifier = Modifier.height(16.dp))

      // No Cable Highlight
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .background(DarkSurfaceElevated, RoundedCornerShape(20.dp))
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
          .padding(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(TealPrimary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "No cable between your phone and helmet",
          color = TextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Primary & Secondary Actions
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      GloveButton(
        text = "Get Started",
        onClick = onGetStarted,
        icon = Icons.Filled.ArrowForward,
        testTag = "welcome_get_started_button"
      )

      Spacer(modifier = Modifier.height(8.dp))

      TextButton(
        onClick = onAlreadyPaired,
        modifier = Modifier.testTag("welcome_already_paired_button")
      ) {
        Text(
          text = "I already paired a helmet",
          color = TextSecondary,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
