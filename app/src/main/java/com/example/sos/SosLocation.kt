package com.example.sos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

enum class SosLocationStatus(val firestoreValue: String) {
  AVAILABLE("available"), PERMISSION_DENIED("permission_denied"),
  DISABLED("disabled"), TIMEOUT("timeout"), UNAVAILABLE("unavailable")
}

data class SosCoordinates(
  val latitude: Double,
  val longitude: Double,
  val accuracy: Double,
  val capturedAtMillis: Long
) {
  init {
    require(latitude.isFinite() && latitude in -90.0..90.0)
    require(longitude.isFinite() && longitude in -180.0..180.0)
    require(accuracy.isFinite() && accuracy in 0.0..20_000_000.0)
    require(capturedAtMillis > 0)
  }
}

data class SosLocationResult(
  val status: SosLocationStatus,
  val coordinates: SosCoordinates? = null
) {
  init { require((status == SosLocationStatus.AVAILABLE) == (coordinates != null)) }
}

/** One foreground fix per SOS; never tracks continuously or returns stale last-known data. */
class PhoneSosLocationProvider(context: Context, private val timeoutMillis: Long = 8_000L) {
  private val context = context.applicationContext

  @SuppressLint("MissingPermission")
  suspend fun capture(): SosLocationResult = withContext(Dispatchers.Main.immediate) {
    if (!hasPermission(context)) return@withContext SosLocationResult(SosLocationStatus.PERMISSION_DENIED)
    try {
      val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return@withContext SosLocationResult(SosLocationStatus.UNAVAILABLE)
      if (!LocationManagerCompat.isLocationEnabled(manager)) {
        return@withContext SosLocationResult(SosLocationStatus.DISABLED)
      }
      val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
      val providers = listOfNotNull(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER.takeIf { fine }
      ).filter { manager.allProviders.contains(it) && manager.isProviderEnabled(it) }
      if (providers.isEmpty()) return@withContext SosLocationResult(SosLocationStatus.UNAVAILABLE)

      withTimeoutOrNull(timeoutMillis) {
        coroutineScope {
          val results = Channel<Location?>(Channel.UNLIMITED)
          val requests = providers.map { provider ->
            launch { results.send(currentLocation(manager, provider)) }
          }
          try {
            repeat(providers.size) {
              val fix = results.receive()
              if (fix != null && isUsable(fix)) {
                return@coroutineScope SosLocationResult(
                  SosLocationStatus.AVAILABLE,
                  SosCoordinates(fix.latitude, fix.longitude, fix.accuracy.toDouble(), fix.time)
                )
              }
            }
            SosLocationResult(SosLocationStatus.UNAVAILABLE)
          } finally {
            requests.forEach { it.cancel() }
            results.close()
          }
        }
      } ?: SosLocationResult(SosLocationStatus.TIMEOUT)
    } catch (_: SecurityException) {
      SosLocationResult(SosLocationStatus.PERMISSION_DENIED)
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      // Location failure must never prevent the SOS document from being written.
      SosLocationResult(SosLocationStatus.UNAVAILABLE)
    }
  }

  @SuppressLint("MissingPermission")
  private suspend fun currentLocation(manager: LocationManager, provider: String): Location? =
    suspendCancellableCoroutine { continuation ->
      val signal = android.os.CancellationSignal()
      continuation.invokeOnCancellation { signal.cancel() }
      try {
        LocationManagerCompat.getCurrentLocation(
          manager, provider, signal, ContextCompat.getMainExecutor(context)
        ) { location -> if (continuation.isActive) continuation.resume(location) }
      } catch (error: SecurityException) {
        continuation.resumeWith(Result.failure(error))
      } catch (_: Exception) {
        if (continuation.isActive) continuation.resume(null)
      }
    }

  private fun isUsable(fix: Location): Boolean {
    val ageMillis = (SystemClock.elapsedRealtimeNanos() - fix.elapsedRealtimeNanos) / 1_000_000
    return ageMillis in 0..15_000 && fix.time > 0 && fix.hasAccuracy() &&
      fix.accuracy.isFinite() && fix.accuracy in 0f..20_000_000f &&
      fix.latitude.isFinite() && fix.latitude in -90.0..90.0 &&
      fix.longitude.isFinite() && fix.longitude in -180.0..180.0
  }

  companion object {
    fun hasPermission(context: Context): Boolean =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
  }
}
