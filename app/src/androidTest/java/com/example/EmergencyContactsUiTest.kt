package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.EmergencyContact
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EmergencyContactsUiTest {
  @get:Rule val compose = createComposeRule()

  private fun show(
    contacts: List<EmergencyContact> = emptyList(),
    loading: Boolean = false,
    error: String? = null,
    onAdd: (EmergencyContact) -> Unit = {},
    onUpdate: (EmergencyContact) -> Unit = {},
    onDelete: (String) -> Unit = {}
  ) {
    compose.setContent { MyApplicationTheme {
      SettingsScreen(
        emergencyContacts = contacts, contactsLoading = loading, contactsError = error,
        contactOperationInProgress = false, contactOperationError = null,
        onAddContact = onAdd, onUpdateContact = onUpdate, onDeleteContact = onDelete,
        onRetryContacts = {}, onClearContactError = {}
      )
    } }
  }

  @Test fun loadingStateIsShown() {
    show(loading = true)
    compose.onNodeWithTag("contacts_loading").assertExists()
    compose.onNodeWithTag("contacts_empty").assertDoesNotExist()
  }

  @Test fun emptyStateIsShown() {
    show()
    compose.onNodeWithTag("contacts_empty").assertExists()
  }

  @Test fun errorStateIsShown() {
    show(error = "Could not load contacts")
    compose.onNodeWithTag("contacts_error").assertTextEquals("Could not load contacts")
  }

  @Test fun addRequiresValidNameAndPhone() {
    var added: EmergencyContact? = null
    show(onAdd = { added = it })
    compose.onNodeWithTag("add_contact_button").performScrollTo().performClick()
    compose.onNodeWithTag("save_contact_button").assertIsNotEnabled()
    compose.onNodeWithTag("contact_name").performTextInput("Alex")
    compose.onNodeWithTag("contact_phone").performTextInput("123")
    compose.onNodeWithTag("save_contact_button").assertIsNotEnabled()
    compose.onNodeWithTag("contact_phone").performTextClearance()
    compose.onNodeWithTag("contact_phone").performTextInput("+65 8123 4567")
    compose.onNodeWithTag("save_contact_button").assertIsNotEnabled()
    compose.onNodeWithTag("contact_relationship").performTextInput("Family")
    compose.onNodeWithTag("save_contact_button").assertIsEnabled().performClick()
    compose.runOnIdle {
      assertEquals("Alex", added?.name)
      assertEquals("+65 8123 4567", added?.phoneNumber)
      assertEquals("Family", added?.relationship)
    }
  }

  @Test fun editAndDeleteUseTheSelectedFirestoreId() {
    val contact = EmergencyContact(id = "contact-1", name = "Mariam", relationship = "Family", phoneNumber = "+65 8123 4567")
    var updated: EmergencyContact? = null
    var deleted = ""
    show(listOf(contact), onUpdate = { updated = it }, onDelete = { deleted = it })
    compose.onNodeWithTag("edit_contact_contact-1").performScrollTo().performClick()
    compose.onNodeWithTag("contact_name").performTextClearance()
    compose.onNodeWithTag("contact_name").performTextInput("Mary")
    compose.onNodeWithTag("save_contact_button").performClick()
    compose.runOnIdle { assertEquals("contact-1", updated?.id) }
    compose.onNodeWithTag("delete_contact_contact-1").performScrollTo().performClick()
    compose.onNodeWithTag("confirm_delete_contact").performClick()
    compose.runOnIdle { assertEquals("contact-1", deleted) }
  }
}
