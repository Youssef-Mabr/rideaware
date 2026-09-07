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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.ui.components.GloveButton
import com.example.ui.components.RideAwareTopBar
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class PermissionItem(
  val id: String,
  val title: String,
  val reason: String,
  val icon: ImageVector,
  val isRequired: Boolean = true
)

@Composable
fun PermissionScreen(
  onPermissionsAllowed: () -> Unit,
  onContinueLimited: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val permissions = remember {
    listOf(
      PermissionItem(
        id = "bluetooth",
        title = "Bluetooth",
        reason = "Streams helmet intercom audio, battery telemetry, and glove control signals.",
        icon = Icons.Filled.Bluetooth
      ),
      PermissionItem(
        id = "nearby",
        title = "Nearby Devices",
        reason = "Discovers your Geni One helmet beacon automatically as you approach your bike.",
        icon = Icons.Filled.DevicesOther
      ),
      PermissionItem(
        id = "microphone",
        title = "Microphone",
        reason = "Allows hands-free Geni voice assistant interactions without letting go of the handlebars.",
        icon = Icons.Filled.Mic
      ),
      PermissionItem(
        id = "notifications",
        title = "Notifications",
        reason = "Pushes high-priority collision radar alerts and system readiness updates.",
        icon = Icons.Filled.NotificationsActive
      ),
      PermissionItem(
        id = "location",
        title = "Emergency Coordinates Only",
        reason = "Transmits your exact GPS coordinates only during verified crash or SOS events. Never used for tracking.",
        icon = Icons.Filled.MyLocation
      )
    )
  }

  // Simulated permission states
  val permissionStates = remember {
    mutableStateMapOf<String, Boolean>().apply {
      permissions.forEach { put(it.id, true) }
    }
  }

  var allAllowedAnim by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    RideAwareTopBar(
      title = "System Access",
      subtitle = "Required for wireless helmet operation",
      onBack = onBack
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = "Permissions & Privacy",
        color = TextPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Geni keeps your phone in your pocket. The following permissions allow seamless wireless operation with your helmet.",
        color = TextSecondary,
        fontSize = 13.sp,
        lineHeight = 19.sp
      )

      Spacer(modifier = Modifier.height(4.dp))

      permissions.forEach { item ->
        val isGranted = permissionStates[item.id] == true

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(
              1.dp,
              if (isGranted) TealPrimary.copy(alpha = 0.3f) else DarkSurfaceBorder,
              RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
        ) {
          Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .background(
                  if (isGranted) TealPrimary.copy(alpha = 0.15f) else DarkSurfaceElevated,
                  CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (isGranted) TealPrimary else TextMuted,
                modifier = Modifier.size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = item.title,
                  color = TextPrimary,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )

                Switch(
                  checked = isGranted,
                  onCheckedChange = { permissionStates[item.id] = it },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF041912),
                    checkedTrackColor = TealPrimary,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurfaceElevated
                  ),
                  modifier = Modifier.testTag("switch_${item.id}")
                )
              }

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = item.reason,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
              )
            }
          }
        }
      }
    }

    // Bottom Navigation with Progress Indicators & Action Buttons
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Progress Dots (Step 2 of 4)
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 14.dp)
      ) {
        Box(modifier = Modifier.size(6.dp).background(DarkSurfaceBorder, CircleShape))
        Box(modifier = Modifier.size(24.dp, 6.dp).background(TealPrimary, RoundedCornerShape(3.dp)))
        Box(modifier = Modifier.size(6.dp).background(DarkSurfaceBorder, CircleShape))
        Box(modifier = Modifier.size(6.dp).background(DarkSurfaceBorder, CircleShape))
      }

      GloveButton(
        text = "Allow Required Permissions",
        onClick = {
          // Simulate full approval
          permissions.forEach { permissionStates[it.id] = true }
          onPermissionsAllowed()
        },
        icon = Icons.Filled.Check,
        testTag = "allow_permissions_button"
      )

      Spacer(modifier = Modifier.height(8.dp))

      TextButton(
        onClick = onContinueLimited,
        modifier = Modifier.testTag("continue_limited_button")
      ) {
        Text(
          text = "Continue with limited features",
          color = TextSecondary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}
