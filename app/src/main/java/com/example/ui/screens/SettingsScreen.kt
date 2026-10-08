package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EmergencyContact
import com.example.model.validContact
import com.example.ui.components.GloveOutlinedButton
import com.example.ui.components.RideAwareTopBar
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
  emergencyContacts: List<EmergencyContact>,
  contactsLoading: Boolean,
  contactsError: String?,
  contactOperationInProgress: Boolean,
  contactOperationError: String?,
  onAddContact: (EmergencyContact) -> Unit,
  onUpdateContact: (EmergencyContact) -> Unit,
  onDeleteContact: (String) -> Unit,
  onRetryContacts: () -> Unit,
  onClearContactError: () -> Unit,
  onOpenHelmetPairing: () -> Unit = {},
  speedUnitKmH: Boolean = true,
  onSpeedUnitChange: (Boolean) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val preferences = remember { context.getSharedPreferences("rideaware_preferences", Context.MODE_PRIVATE) }
  var showEditContactDialog by rememberSaveable { mutableStateOf(false) }
  var editingContactId by rememberSaveable { mutableStateOf<String?>(null) }
  var contactName by rememberSaveable { mutableStateOf("") }
  var contactRelation by rememberSaveable { mutableStateOf("") }
  var contactPhone by rememberSaveable { mutableStateOf("") }
  var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
  var crashSensitivity by remember { mutableStateOf(preferences.getString("sensitivity", "Medium") ?: "Medium") }
  var lowBatteryThreshold by remember { mutableStateOf(preferences.getInt("battery_threshold", 20)) }
  var wakeWord by remember { mutableStateOf(preferences.getBoolean("wake_word", true)) }
  var showAboutDialog by rememberSaveable { mutableStateOf(false) }

  Column(modifier.fillMaxSize().background(DarkCanvas)) {
    RideAwareTopBar("Settings", "Saved on this phone")
    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      Text("Manage your helmet preferences and emergency contacts.", color = TextSecondary, fontSize = 14.sp)
      SettingsCard("Emergency contacts") {
        Text("Saved securely to your account. No messages will be sent.", color = TextSecondary, fontSize = 13.sp)
        when {
          contactsLoading -> Row(
            Modifier.fillMaxWidth().testTag("contacts_loading"),
            horizontalArrangement = Arrangement.Center
          ) { CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(28.dp)) }
          contactsError != null -> {
            Text(contactsError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.testTag("contacts_error"))
            GloveOutlinedButton("Try again", onRetryContacts, testTag = "retry_contacts_button")
          }
          emergencyContacts.isEmpty() -> Text(
            "No emergency contacts yet. Add one before starting a ride.",
            color = TextSecondary, fontSize = 14.sp, modifier = Modifier.testTag("contacts_empty")
          )
          else -> emergencyContacts.forEach { contact ->
            Column(
              Modifier.fillMaxWidth().background(DarkSurfaceElevated, RoundedCornerShape(14.dp)).padding(14.dp)
                .testTag("contact_${contact.id}"),
              verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Text(contact.name, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
              if (contact.relationship.isNotBlank()) Text(contact.relationship, color = TextSecondary, fontSize = 13.sp)
              Text(contact.phoneNumber, color = TealAccent, fontSize = 15.sp)
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                  onClick = {
                    editingContactId = contact.id
                    contactName = contact.name
                    contactRelation = contact.relationship
                    contactPhone = contact.phoneNumber
                    onClearContactError()
                    showEditContactDialog = true
                  },
                  enabled = !contactOperationInProgress,
                  modifier = Modifier.testTag("edit_contact_${contact.id}")
                ) { Text("Edit") }
                TextButton(
                  onClick = { pendingDeleteId = contact.id },
                  enabled = !contactOperationInProgress,
                  modifier = Modifier.testTag("delete_contact_${contact.id}")
                ) { Text("Delete", color = DangerRed) }
              }
            }
          }
        }
        if (contactOperationError != null) Text(
          contactOperationError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
          modifier = Modifier.testTag("contact_operation_error")
        )
        GloveOutlinedButton("Add contact", {
          editingContactId = null
          contactName = ""
          contactRelation = ""
          contactPhone = ""
          onClearContactError()
          showEditContactDialog = true
        }, testTag = "add_contact_button")
      }
      SettingsCard("Helmet pairing") {
        Text("Pair a provisioned RideAware helmet QR to your account. The sample QR works without camera or hardware access.", color = TextSecondary, fontSize = 13.sp)
        GloveOutlinedButton("Open helmet pairing", onOpenHelmetPairing, testTag = "open_helmet_pairing_button")
      }
      SettingsCard("Speed unit") {
        Text("Changes the speed and distance shown during a ride. Ride history keeps its recorded units.", color = TextSecondary, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = speedUnitKmH, onClick = { onSpeedUnitChange(true) },
            label = { Text("km/h · km") }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("speed_unit_metric")
          )
          FilterChip(
            selected = !speedUnitKmH, onClick = { onSpeedUnitChange(false) },
            label = { Text("mph · mi") }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("speed_unit_imperial")
          )
        }
      }
      SettingsCard("Crash sensitivity preview") {
        Text("A saved preference for the prototype; it does not control crash detection.", color = TextSecondary, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("Low", "Medium", "High").forEach { level ->
            FilterChip(
              selected = crashSensitivity == level,
              onClick = {
                crashSensitivity = level
                preferences.edit().putString("sensitivity", level).apply()
              },
              label = { Text(level) },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("sens_tab_${level.lowercase()}")
            )
          }
        }
      }
      SettingsCard("Battery reminder preview") {
        Text("Choose the battery level for a low-battery warning.", color = TextSecondary, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(15, 20, 25).forEach { threshold ->
            FilterChip(
              selected = lowBatteryThreshold == threshold,
              onClick = {
                lowBatteryThreshold = threshold
                preferences.edit().putInt("battery_threshold", threshold).apply()
              },
              label = { Text("$threshold%") },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            )
          }
        }
      }
      SettingsCard("Voice activation preview") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Text("Use “Hey Geni”", color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
          Switch(
            checked = wakeWord,
            onCheckedChange = {
              wakeWord = it
              preferences.edit().putBoolean("wake_word", it).apply()
            },
            modifier = Modifier.semantics { contentDescription = "Voice activation preview" }
          )
        }
        Text("The microphone is not listening. Try the sample prompts in the Geni tab.", color = TextSecondary, fontSize = 13.sp)
      }
      GloveOutlinedButton("About Geni", { showAboutDialog = true }, testTag = "about_geni_button")
      Spacer(Modifier.height(16.dp))
    }
  }

  if (showEditContactDialog) {
    val valid = validContact(contactName, contactPhone) && contactRelation.isNotBlank()
    AlertDialog(
      onDismissRequest = { showEditContactDialog = false },
      containerColor = DarkSurface,
      title = { Text(if (editingContactId == null) "Add emergency contact" else "Edit emergency contact") },
      text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = contactName, onValueChange = { if (it.length <= 80) contactName = it }, label = { Text("Name") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("contact_name")
          )
          OutlinedTextField(
            value = contactRelation, onValueChange = { if (it.length <= 50) contactRelation = it }, label = { Text("Relationship") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("contact_relationship")
          )
          OutlinedTextField(
            value = contactPhone, onValueChange = { if (it.length <= 30) contactPhone = it }, label = { Text("Phone number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            isError = contactPhone.isNotEmpty() && !validContact("Contact", contactPhone),
            supportingText = { Text("Include the country code, for example +65 8123 4567. Use 7–15 digits.") },
            modifier = Modifier.fillMaxWidth().testTag("contact_phone")
          )
          Text("Saved to your account. Emergency messaging is not connected yet.", color = TextSecondary, fontSize = 13.sp)
        }
      },
      confirmButton = {
        TextButton(
          enabled = valid && !contactOperationInProgress,
          onClick = {
            val contact = EmergencyContact(
              id = editingContactId.orEmpty(), name = contactName.trim(),
              relationship = contactRelation.trim(), phoneNumber = contactPhone.trim(), phone = contactPhone.trim()
            )
            if (editingContactId == null) onAddContact(contact) else onUpdateContact(contact)
            showEditContactDialog = false
          },
          modifier = Modifier.heightIn(min = 48.dp).testTag("save_contact_button")
        ) { Text(if (contactOperationInProgress) "Saving…" else "Save contact") }
      },
      dismissButton = { TextButton(onClick = { showEditContactDialog = false }) { Text("Cancel") } }
    )
  }
  pendingDeleteId?.let { contactId ->
    val contactNameForDelete = emergencyContacts.firstOrNull { it.id == contactId }?.name ?: "this contact"
    AlertDialog(
      onDismissRequest = { if (!contactOperationInProgress) pendingDeleteId = null },
      title = { Text("Delete emergency contact?") },
      text = { Text("$contactNameForDelete will be removed from your account.") },
      confirmButton = {
        TextButton(
          enabled = !contactOperationInProgress,
          onClick = { onDeleteContact(contactId); pendingDeleteId = null },
          modifier = Modifier.testTag("confirm_delete_contact")
        ) { Text("Delete", color = DangerRed) }
      },
      dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") } }
    )
  }
  if (showAboutDialog) {
    AlertDialog(
      onDismissRequest = { showAboutDialog = false },
      title = { Text("Geni · interactive prototype") },
      text = { Text("Explore a smart helmet companion using sample rides, camera illustrations and scripted voice responses.\n\nBluetooth pairing, recording, cloud backup, crash detection and emergency messaging are not connected. Use the app while parked.") },
      confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close") } }
    )
  }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
  Column(
    Modifier.fillMaxWidth().background(DarkSurface, RoundedCornerShape(18.dp)).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    content()
  }
}
