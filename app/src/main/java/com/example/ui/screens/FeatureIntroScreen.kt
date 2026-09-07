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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiOff
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
import com.example.ui.components.GloveButton
import com.example.ui.components.RideAwareTopBar
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

data class FeatureCardItem(
  val number: String,
  val title: String,
  val description: String,
  val icon: ImageVector,
  val tintColor: Color
)

@Composable
fun FeatureIntroScreen(
  onContinue: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val featureCards = listOf(
    FeatureCardItem(
      number = "01",
      title = "See both directions",
      description = "Front and rear cameras keep the full ride in view with real-time blind zone radar.",
      icon = Icons.Filled.CameraAlt,
      tintColor = TealPrimary
    ),
    FeatureCardItem(
      number = "02",
      title = "Ask without touching",
      description = "Genie handles directions, translation, and quick assistance by voice without gloves on your phone.",
      icon = Icons.Filled.Mic,
      tintColor = CyanAccent
    ),
    FeatureCardItem(
      number = "03",
      title = "Protect what matters",
      description = "Important risk events are saved automatically with synchronized time and GPS location.",
      icon = Icons.Filled.Security,
      tintColor = Color(0xFF38BDF8)
    )
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    RideAwareTopBar(
      title = "Why Geni",
      subtitle = "Key helmet capabilities",
      onBack = onBack
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = "Engineered for pure rider focus.",
        color = TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp
      )

      featureCards.forEach { item ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
        ) {
          Row(verticalAlignment = Alignment.Top) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .background(item.tintColor.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                .border(1.dp, item.tintColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.tintColor,
                modifier = Modifier.size(24.dp)
              )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = item.title,
                  color = TextPrimary,
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = item.number,
                  color = item.tintColor,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Black
                )
              }

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = item.description,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
              )
            }
          }
        }
      }

      // Offline Resilience Banner
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, TealPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
          .padding(16.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(TealPrimary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Filled.WifiOff,
              contentDescription = null,
              tint = TealAccent,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "Always-On Edge Safety",
              color = TealAccent,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Core safety alerts continue working without internet.",
              color = TextPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }

    // Bottom Navigation Bar with Progress Indicators & Continue Button
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Progress Dots (Step 1 of 4)
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 14.dp)
      ) {
        Box(modifier = Modifier.size(24.dp, 6.dp).background(TealPrimary, RoundedCornerShape(3.dp)))
        Box(modifier = Modifier.size(6.dp).background(DarkSurfaceBorder, CircleShape))
        Box(modifier = Modifier.size(6.dp).background(DarkSurfaceBorder, CircleShape))
        Box(modifier = Modifier.size(6.dp).background(DarkSurfaceBorder, CircleShape))
      }

      GloveButton(
        text = "Continue",
        onClick = onContinue,
        icon = Icons.Filled.ArrowForward,
        testTag = "feature_intro_continue_button"
      )
    }
  }
}
