package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.EmergencyContactRepository
import com.example.model.EmergencyContact
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EmergencyContactsFirestoreTest {
  @Test fun authenticatedUserCanAddLoadEditAndDeleteContact() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Phone must be signed in")
    val marker = "Contact test ${System.currentTimeMillis()}"
    val created = Tasks.await(EmergencyContactRepository.add(uid, EmergencyContact(
      name = marker, phoneNumber = "+65 8123 4567", relationship = "Test"
    )), 30, TimeUnit.SECONDS)
    try {
      val initial = Tasks.await(created.get(Source.SERVER), 30, TimeUnit.SECONDS)
      assertEquals(marker, initial.getString("name"))
      assertEquals("+65 8123 4567", initial.getString("phoneNumber"))
      assertEquals("Test", initial.getString("relationship"))
      val createdAt = requireNotNull(initial.getTimestamp("createdAt"))
      val firstUpdatedAt = requireNotNull(initial.getTimestamp("updatedAt"))

      Thread.sleep(1100)
      Tasks.await(EmergencyContactRepository.update(uid, EmergencyContact(
        id = created.id, name = "$marker edited", phoneNumber = "+65 8765 4321", relationship = "Friend"
      )), 30, TimeUnit.SECONDS)
      val edited = Tasks.await(created.get(Source.SERVER), 30, TimeUnit.SECONDS)
      assertEquals("$marker edited", edited.getString("name"))
      assertEquals("+65 8765 4321", edited.getString("phoneNumber"))
      assertEquals("Friend", edited.getString("relationship"))
      assertEquals(createdAt, edited.getTimestamp("createdAt"))
      assertTrue(edited.getTimestamp("updatedAt")!! > firstUpdatedAt)

      val loaded = Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid)
        .collection("emergencyContacts").get(Source.SERVER), 30, TimeUnit.SECONDS)
      assertTrue(loaded.documents.any { it.id == created.id })
    } finally {
      Tasks.await(EmergencyContactRepository.delete(uid, created.id), 30, TimeUnit.SECONDS)
    }
    assertFalse(Tasks.await(created.get(Source.SERVER), 30, TimeUnit.SECONDS).exists())
  }
}
