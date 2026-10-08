package com.example.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object FcmTokenRepository {
  fun syncLatestToken(context: Context) {
    val appContext = context.applicationContext
    FirebaseMessaging.getInstance().token
      .addOnSuccessListener { token -> onNewToken(appContext, token) }
      .addOnFailureListener { error ->
        Log.e(TAG, "Could not retrieve FCM token; will retry on next launch", error)
        cachedToken(appContext)?.let(::saveForCurrentUser)
      }
  }

  fun onNewToken(context: Context, token: String) {
    if (token.isBlank()) return
    // Retain tokens received before authentication; sync again after profile creation.
    context.getSharedPreferences("rideaware_messaging", Context.MODE_PRIVATE)
      .edit().putString("fcm_token", token).apply()
    saveForCurrentUser(token)
  }

  private fun cachedToken(context: Context) = context
    .getSharedPreferences("rideaware_messaging", Context.MODE_PRIVATE).getString("fcm_token", null)

  private fun saveForCurrentUser(token: String) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    // update cannot create an incomplete user document or replace profile timestamps.
    FirebaseFirestore.getInstance().collection("users").document(uid).update(mapOf(
      "fcmToken" to token,
      "fcmTokenUpdatedAt" to FieldValue.serverTimestamp()
    ))
      .addOnSuccessListener { Log.i(TAG, "FCM token and server timestamp saved to users/$uid") }
      .addOnFailureListener { error ->
        Log.e(TAG, "FCM token sync failed for users/$uid; retry after profile sync or next launch", error)
      }
  }

  private const val TAG = "RideAwareMessaging"
}
