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
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EmergencyContact
import com.example.ui.components.GloveButton
import com.example.ui.components.GloveOutlinedButton
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

@Composable
fun SettingsScreen(
  emergencyContact: EmergencyContact,
  onUpdateContact: (EmergencyContact) -> Unit,
  modifier: Modifier = Modifier
) {
  var showEditContactDialog by remember { mutableStateOf(false) }
  var contactName by remember { mutableStateOf(emergencyContact.name) }
  var contactRelation by remember { mutableStateOf(emergencyContact.relationship) }
  var contactPhone by remember { mutableStateOf(emergencyContact.phoneNumber) }

  // Preferences
  var crashSensitivity by remember { mutableStateOf("Medium (Recommended)") }
  var speedUnitKmH by remember { mutableStateOf(true) }
  var lowBatteryThreshold by remember { mutableStateOf("20%") }
  var wakeWordGenie by remember { mutableStateOf(true) }
  var showAboutDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
  ) {
    RideAwareTopBar(
      title = "Settings",
      subtitle = "Companion preferences & emergency SOS"
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Emergency Contact Card
      Text("Emergency SOS Contact", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .background(TealPrimary.copy(alpha = 0.15f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Filled.ContactPhone, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "${emergencyContact.name} (${emergencyContact.relationship})",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = emergencyContact.phoneNumber,
                color = TealAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Automated crash coordinates receiver",
                color = TextSecondary,
                fontSize = 11.sp
              )
            }
          }

          IconButton(
            onClick = { showEditContactDialog = true },
            modifier = Modifier.testTag("edit_contact_button")
          ) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit Contact", tint = TealPrimary)
          }
        }
      }

      // 2. Crash Detection Sensitivity
      Text("Crash Sensor IMU Sensitivity", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
          .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        listOf("Low", "Medium", "High").forEach { level ->
          val isSelected = crashSensitivity.startsWith(level)
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) TealPrimary else Color.Transparent)
              .clickable {
                crashSensitivity = when (level) {
                  "Low" -> "Low (Track days)"
                  "Medium" -> "Medium (Recommended)"
                  else -> "High (City commuting)"
                }
              }
              .padding(vertical = 10.dp)
              .testTag("sens_tab_${level.lowercase()}"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = level,
              color = if (isSelected) Color(0xFF041912) else TextSecondary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // 3. Speed Unit Preference & Battery Alert Threshold
      Text("Telemetry Preferences", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Speed Unit (km/h vs mph)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Speed, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Speed Metrics Unit", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              Text(if (speedUnitKmH) "Metric (km/h)" else "Imperial (mph)", color = TextSecondary, fontSize = 11.sp)
            }
          }

          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
              .clickable { speedUnitKmH = !speedUnitKmH }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("toggle_speed_unit")
          ) {
            Text(if (speedUnitKmH) "km/h" else "mph", color = TealAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Low Battery Threshold
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BatteryAlert, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Low Battery Audio Warning", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              Text("Warning prompt before auto-sleep", color = TextSecondary, fontSize = 11.sp)
            }
          }

          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
              .clickable {
                lowBatteryThreshold = when (lowBatteryThreshold) {
                  "15%" -> "20%"
                  "20%" -> "25%"
                  else -> "15%"
                }
              }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("toggle_battery_threshold")
          ) {
            Text(lowBatteryThreshold, color = TealAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Voice Assistant Wake Word
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Wake Word \"Hey Genie\"", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              Text("Hands-free microphone detection", color = TextSecondary, fontSize = 11.sp)
            }
          }
          Switch(
            checked = wakeWordGenie,
            onCheckedChange = { wakeWordGenie = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF041912), checkedTrackColor = TealPrimary)
          )
        }
      }

      // About Geni Button
      GloveOutlinedButton(
        text = "About Geni & Architecture",
        onClick = { showAboutDialog = true },
        icon = Icons.Filled.Info,
        testTag = "about_geni_button"
      )

      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  // Edit Emergency Contact Dialog
  if (showEditContactDialog) {
    AlertDialog(
      onDismissRequest = { showEditContactDialog = false },
      containerColor = DarkSurface,
      title = { Text("Edit Emergency SOS Contact", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = contactName,
            onValueChange = { contactName = it },
            label = { Text("Contact Name") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
          )
          OutlinedTextField(
            value = contactRelation,
            onValueChange = { contactRelation = it },
            label = { Text("Relationship") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
          )
          OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = { Text("Phone Number") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
          )
        }
      },
      confirmButton = {
        GloveButton(
          text = "Save Contact",
          onClick = {
            onUpdateContact(EmergencyContact(contactName, contactRelation, contactPhone))
            showEditContactDialog = false
          },
          modifier = Modifier.width(140.dp),
          testTag = "save_contact_button"
        )
      },
      dismissButton = {
        TextButton(onClick = { showEditContactDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // About Prototype Dialog
  if (showAboutDialog) {
    AlertDialog(
      onDismissRequest = { showAboutDialog = false },
      containerColor = DarkSurface,
      title = { Text("About Geni Prototype", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Geni Companion v1.0.0", color = TealAccent, fontWeight = FontWeight.Bold)
          Text("AI smart helmet companion inspired by Ali Baba, guarding riders with 360° radar and voice intelligence.", color = TextSecondary, fontSize = 12.sp)
          Spacer(modifier = Modifier.height(6.dp))
          Text("Architecture Principles:", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
          Text("• Wireless BLE + 5GHz Wi-Fi dual video stream (zero cables)", color = TextSecondary, fontSize = 11.sp)
          Text("• Offline edge risk detection (no cloud dependency for alerts)", color = TextSecondary, fontSize = 11.sp)
          Text("• Glove-friendly high-contrast UI with 56dp+ touch targets", color = TextSecondary, fontSize = 11.sp)
          Text("• Distraction-free: phone stays safely in rider's pocket", color = TextSecondary, fontSize = 11.sp)
        }
      },
      confirmButton = {
        GloveButton(
          text = "Close",
          onClick = { showAboutDialog = false },
          modifier = Modifier.width(100.dp),
          testTag = "about_close_button"
        )
      }
    )
  }
}
