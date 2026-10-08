package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

object RideAwareNotifications {
  const val CHANNEL_ID = "rideaware_alerts"

  fun createChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(CHANNEL_ID, "RideAware Alerts", NotificationManager.IMPORTANCE_HIGH)
      channel.description = "Ride alerts and system updates"
      context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
  }

  fun needsRuntimePermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

  fun areEnabled(context: Context): Boolean {
    if (needsRuntimePermission(context) || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
      context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
  }

  fun showMessage(context: Context, message: RemoteMessage) {
    createChannel(context)
    val title = message.notification?.title?.takeIf { it.isNotBlank() }
      ?: message.data["title"]?.takeIf { it.isNotBlank() }
    val body = message.notification?.body?.takeIf { it.isNotBlank() }
      ?: message.data["body"]?.takeIf { it.isNotBlank() }
    if (title == null && body == null) {
      Log.i("RideAwareMessaging", "Message received without display content")
      return
    }
    if (!areEnabled(context)) {
      Log.i("RideAwareMessaging", "Message received; notifications are disabled")
      return
    }
    val openApp = PendingIntent.getActivity(context, 0,
      Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title ?: "RideAware Alerts")
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
      .setContentIntent(openApp)
      .setAutoCancel(true)
      .build()
    try {
      NotificationManagerCompat.from(context).notify(message.messageId ?: UUID.randomUUID().toString(), 0, notification)
      Log.i("RideAwareMessaging", "Message displayed on RideAware Alerts")
    } catch (error: SecurityException) {
      Log.w("RideAwareMessaging", "Notification permission changed before delivery", error)
    }
  }
}
