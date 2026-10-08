package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sos.PhoneSosLocationProvider

data class LocationPermissionControl(
  val enabled: Boolean,
  val requestIfNeeded: () -> Unit,
  val onToggle: (Boolean) -> Unit
)

@Composable
fun rememberLocationPermission(): LocationPermissionControl {
  val context = LocalContext.current
  val owner = LocalLifecycleOwner.current
  var enabled by remember { mutableStateOf(PhoneSosLocationProvider.hasPermission(context)) }
  val preferences = remember(context) { context.getSharedPreferences("rideaware_location", Context.MODE_PRIVATE) }
  fun canAsk(): Boolean = !preferences.getBoolean("requested", false) ||
    (context.locationActivity()?.let {
      ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION) ||
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
    } == true)
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
    enabled = PhoneSosLocationProvider.hasPermission(context)
  }
  DisposableEffect(owner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) enabled = PhoneSosLocationProvider.hasPermission(context)
    }
    owner.lifecycle.addObserver(observer)
    onDispose { owner.lifecycle.removeObserver(observer) }
  }
  val request: () -> Unit = {
    if (!PhoneSosLocationProvider.hasPermission(context) && canAsk()) {
      preferences.edit().putBoolean("requested", true).apply()
      launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
  }
  return LocationPermissionControl(enabled, request) { requested ->
    if (requested && !enabled && canAsk()) request()
    else context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
  }
}

private fun Context.locationActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.locationActivity()
  else -> null
}
