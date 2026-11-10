package com.example.helmet

/**
 * Connection states representing the lifecycle of helmet communication.
 */
sealed interface HelmetConnectionState {
  /**
   * The helmet is not connected and no connection attempt is currently active.
   */
  data object Disconnected : HelmetConnectionState

  /**
   * A connection to the helmet is in progress.
   *
   * @param deviceAddress Optional MAC or identifier of the target device.
   * @param attempt The current attempt count for reconnection logic.
   */
  data class Connecting(
    val deviceAddress: String? = null,
    val attempt: Int = 1
  ) : HelmetConnectionState

  /**
   * The helmet is actively connected and ready for communication.
   *
   * @param deviceId Identifier of the connected helmet.
   * @param deviceName User-facing or broadcast name of the device.
   * @param connectedAtMillis Epoch timestamp when connection was established.
   */
  data class Connected(
    val deviceId: String,
    val deviceName: String,
    val connectedAtMillis: Long = System.currentTimeMillis()
  ) : HelmetConnectionState

  /**
   * A connection attempt or active session failed.
   *
   * @param message Human-readable error description.
   * @param cause Underlying exception, if available.
   * @param recoverable Whether the error can be recovered from via reconnect.
   */
  data class Error(
    val message: String,
    val cause: Throwable? = null,
    val recoverable: Boolean = true
  ) : HelmetConnectionState
}
