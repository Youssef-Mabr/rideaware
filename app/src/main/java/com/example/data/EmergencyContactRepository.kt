package com.example.data

import com.example.model.EmergencyContact
import com.example.model.validContact
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object EmergencyContactRepository {
  fun observe(
    uid: String,
    onContacts: (List<EmergencyContact>) -> Unit,
    onError: (Exception) -> Unit
  ): ListenerRegistration = collection(uid)
    .orderBy("createdAt", Query.Direction.ASCENDING)
    .addSnapshotListener { snapshot, error ->
      if (error != null) {
        onError(error)
        return@addSnapshotListener
      }
      try {
        val contacts = snapshot?.documents.orEmpty().map { document ->
          EmergencyContact(
            id = document.id,
            name = document.getString("name") ?: error("Contact ${document.id} has no name"),
            phoneNumber = document.getString("phoneNumber") ?: error("Contact ${document.id} has no phone number"),
            phone = document.getString("phoneNumber") ?: "",
            relationship = document.getString("relationship") ?: ""
          )
        }
        onContacts(contacts)
      } catch (mappingError: Exception) {
        onError(mappingError)
      }
    }

  fun add(uid: String, contact: EmergencyContact): Task<DocumentReference> {
    requireValid(contact)
    return collection(uid).add(mapOf(
      "name" to contact.name.trim(),
      "phoneNumber" to contact.phoneNumber.trim(),
      "relationship" to contact.relationship.trim(),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    ))
  }

  fun update(uid: String, contact: EmergencyContact): Task<Void> {
    require(contact.id.isNotBlank()) { "A contact ID is required for editing" }
    requireValid(contact)
    return collection(uid).document(contact.id).update(mapOf(
      "name" to contact.name.trim(),
      "phoneNumber" to contact.phoneNumber.trim(),
      "relationship" to contact.relationship.trim(),
      "updatedAt" to FieldValue.serverTimestamp()
    ))
  }

  fun delete(uid: String, contactId: String): Task<Void> {
    require(contactId.isNotBlank()) { "A contact ID is required for deletion" }
    return collection(uid).document(contactId).delete()
  }

  private fun requireValid(contact: EmergencyContact) {
    require(validContact(contact.name, contact.phoneNumber) && contact.relationship.isNotBlank()) {
      "Enter a valid name, phone number, and relationship"
    }
    require(contact.name.trim().length <= 80) { "Name is too long" }
    require(contact.relationship.trim().length <= 50) { "Relationship is too long" }
    require(contact.phoneNumber.trim().length <= 30) { "Phone number is too long" }
  }

  private fun collection(uid: String) = FirebaseFirestore.getInstance()
    .collection("users").document(uid).collection("emergencyContacts")
}
