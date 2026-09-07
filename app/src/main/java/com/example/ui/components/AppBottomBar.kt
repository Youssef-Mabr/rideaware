package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppDestination
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealGlow
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

sealed class NavTab(
  val destination: AppDestination,
  val title: String,
  val filledIcon: ImageVector,
  val outlinedIcon: ImageVector
) {
  object Home : NavTab(AppDestination.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home)
  object Rides : NavTab(AppDestination.RIDES, "Rides", Icons.Filled.Route, Icons.Outlined.Route)
  object Genie : NavTab(AppDestination.GENIE, "Geni", Icons.Filled.Mic, Icons.Outlined.Mic)
  object Helmet : NavTab(AppDestination.HELMET, "Helmet", Icons.Filled.Security, Icons.Outlined.Security)
  object Settings : NavTab(AppDestination.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val mainNavTabs = listOf(
  NavTab.Home,
  NavTab.Rides,
  NavTab.Genie,
  NavTab.Helmet,
  NavTab.Settings
)

@Composable
fun AppBottomBar(
  currentDestination: AppDestination,
  onNavigate: (AppDestination) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(DarkSurface)
      .border(1.dp, Color.White.copy(alpha = 0.05f))
      .navigationBarsPadding()
      .padding(top = 8.dp, bottom = 10.dp, start = 8.dp, end = 8.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      mainNavTabs.forEach { tab ->
        val isSelected = currentDestination == tab.destination
        val isGenie = tab == NavTab.Genie

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onNavigate(tab.destination) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("nav_tab_${tab.title.lowercase()}")
        ) {
          if (isGenie) {
            // Special glowing Genie orb button in center
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(40.dp)
                .background(
                  color = if (isSelected) TealPrimary else Color.White.copy(alpha = 0.08f),
                  shape = CircleShape
                )
                .border(
                  1.5.dp,
                  if (isSelected) TealAccent else Color.White.copy(alpha = 0.15f),
                  CircleShape
                )
            ) {
              Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = tab.title,
                tint = if (isSelected) Color(0xFF020408) else TealPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
          } else {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(34.dp)
                .background(
                  color = if (isSelected) TealPrimary.copy(alpha = 0.18f) else Color.Transparent,
                  shape = RoundedCornerShape(8.dp)
                )
                .border(
                  width = 1.dp,
                  color = if (isSelected) TealPrimary else Color.White.copy(alpha = 0.25f),
                  shape = RoundedCornerShape(8.dp)
                )
            ) {
              Icon(
                imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                contentDescription = tab.title,
                tint = if (isSelected) TealPrimary else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = tab.title.uppercase(),
            color = if (isSelected) TealPrimary else Color.White.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          )
        }
      }
    }
  }
}
