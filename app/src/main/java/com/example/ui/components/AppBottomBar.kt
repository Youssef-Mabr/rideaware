package com.example.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppDestination
import com.example.ui.theme.*

@Composable
fun AppBottomBar(
  currentDestination: AppDestination,
  onNavigate: (AppDestination) -> Unit,
  modifier: Modifier = Modifier
) {
  val tabs = listOf(
    Triple(AppDestination.HOME, "Home", Icons.Filled.Home),
    Triple(AppDestination.RIDES, "Rides", Icons.Filled.Route),
    Triple(AppDestination.GENIE, "Geni", Icons.Filled.Mic),
    Triple(AppDestination.HELMET, "Helmet", Icons.Filled.Security),
    Triple(AppDestination.SETTINGS, "Settings", Icons.Filled.Settings)
  )
  NavigationBar(modifier = modifier.heightIn(min = 80.dp), containerColor = DarkSurface) {
    tabs.forEach { (destination, label, icon) ->
      NavigationBarItem(
        selected = currentDestination == destination,
        onClick = { onNavigate(destination) },
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label, fontSize = 12.sp) },
        alwaysShowLabel = true,
        modifier = Modifier.testTag("nav_tab_${label.lowercase()}"),
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = TealPrimary,
          selectedTextColor = TealPrimary,
          indicatorColor = TealDark,
          unselectedIconColor = TextSecondary,
          unselectedTextColor = TextSecondary
        )
      )
    }
  }
}
