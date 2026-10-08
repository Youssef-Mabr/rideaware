package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notifications.RideAwareNotifications

data class NotificationPermissionControl(val enabled: Boolean, val onToggle: (Boolean) -> Unit)

@Composable
fun rememberNotificationPermission(): NotificationPermissionControl {
  val context = LocalContext.current
  val owner = LocalLifecycleOwner.current
  val preferences = remember(context) { context.getSharedPreferences("rideaware_messaging", Context.MODE_PRIVATE) }
  var enabled by remember { mutableStateOf(RideAwareNotifications.areEnabled(context)) }
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    enabled = RideAwareNotifications.areEnabled(context)
  }
  DisposableEffect(owner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) enabled = RideAwareNotifications.areEnabled(context)
    }
    owner.lifecycle.addObserver(observer)
    onDispose { owner.lifecycle.removeObserver(observer) }
  }
  return NotificationPermissionControl(enabled) { requested ->
    val activity = context.findActivity()
    val canAskAgain = !preferences.getBoolean("notification_permission_requested", false) ||
      (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS))
    if (requested && RideAwareNotifications.needsRuntimePermission(context) && canAskAgain) {
      preferences.edit().putBoolean("notification_permission_requested", true).apply()
      launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      // Android owns revocation and permanently denied / channel settings.
      val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
      } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
      }
      context.startActivity(intent)
    }
  }
}

private fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}
