package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.notifications.FcmTokenRepository
import com.example.notifications.RideAwareNotifications

class RideAwareApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    RideAwareNotifications.createChannel(this)

    // Run once per app process, outside Compose and activity recreation.
    val auth = FirebaseAuth.getInstance()
    val existingUser = auth.currentUser
    if (existingUser != null) {
      Log.i(AUTH_TAG, "Already signed in. UID: ${existingUser.uid}")
      syncUserProfile(existingUser.uid)
      return
    }

    auth.signInAnonymously()
      .addOnSuccessListener { result ->
        val user = result.user
        if (user == null) {
          Log.e(AUTH_TAG, "Anonymous sign-in returned no user")
          return@addOnSuccessListener
        }
        Log.i(AUTH_TAG, "Anonymous sign-in successful. UID: ${user.uid}")
        syncUserProfile(user.uid)
      }
      .addOnFailureListener { error ->
        Log.e(AUTH_TAG, "Anonymous sign-in failed", error)
      }
  }

  private fun syncUserProfile(uid: String) {
    val firestore = FirebaseFirestore.getInstance()
    val userDocument = firestore.collection("users").document(uid)

    // A transaction prevents concurrent launches from replacing createdAt.
    firestore.runTransaction { transaction ->
      val snapshot = transaction.get(userDocument)
      if (snapshot.exists()) {
        transaction.update(userDocument, "lastSeenAt", FieldValue.serverTimestamp())
        "Updated lastSeenAt for users/$uid; createdAt preserved"
      } else {
        transaction.set(userDocument, mapOf(
          "uid" to uid,
          "accountType" to "anonymous",
          "createdAt" to FieldValue.serverTimestamp()
        ))
        "Created users/$uid with a server timestamp"
      }
    }
      .addOnSuccessListener { message ->
        Log.i(FIRESTORE_TAG, message)
        FcmTokenRepository.syncLatestToken(this)
      }
      .addOnFailureListener { error ->
        Log.e(FIRESTORE_TAG, "Could not sync users/$uid", error)
      }
  }

  private companion object {
    const val AUTH_TAG = "RideAwareAuth"
    const val FIRESTORE_TAG = "RideAwareFirestore"
  }
}
