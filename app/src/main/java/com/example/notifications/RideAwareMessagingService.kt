package com.example.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RideAwareMessagingService : FirebaseMessagingService() {
  override fun onNewToken(token: String) {
    Log.i("RideAwareMessaging", "FCM token refreshed")
    FcmTokenRepository.onNewToken(applicationContext, token)
  }

  override fun onMessageReceived(message: RemoteMessage) {
    RideAwareNotifications.showMessage(this, message)
  }
}
