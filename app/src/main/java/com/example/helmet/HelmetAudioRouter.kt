package com.example.helmet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Target routing options for helmet audio channels.
 */
enum class AudioRoute {
  /**
   * Default routing: Phone's internal microphone and speaker.
   * This is active by default and preserves all existing phone audio flows.
   */
  PHONE_DEFAULT,

  /**
   * Bidirectional helmet headset audio (SCO / Bluetooth hands-free profile / BLE audio).
   */
  HELMET_HEADSET,

  /**
   * Media audio output directed to helmet stereo speakers; phone mic remains primary.
   */
  HELMET_MEDIA_OUTPUT
}

/**
 * Interface defining contract for managing audio routing between phone and helmet.
 */
interface HelmetAudioRouter {
  /**
   * Currently active audio route.
   */
  val currentRoute: StateFlow<AudioRoute>

  /**
   * Indicates whether helmet audio peripherals (Bluetooth SCO/A2DP headset) are discovered and available.
   */
  val isHelmetAudioAvailable: StateFlow<Boolean>

  /**
   * Request switching audio route.
   *
   * @param route The requested target audio route.
   * @return True if the requested route was accepted, false otherwise.
   */
  fun requestRoute(route: AudioRoute): Boolean

  /**
   * Reset audio routing to the phone's default microphone and speaker.
   */
  fun resetToDefault(): Boolean
}

/**
 * Default implementation of [HelmetAudioRouter] that safely keeps audio on the phone's
 * default microphone and speaker, preserving the existing working phone microphone,
 * Gemini Live voice sessions, and phone speaker playback.
 */
class DefaultHelmetAudioRouter(
  initialHelmetAudioAvailable: Boolean = false
) : HelmetAudioRouter {
  private val _currentRoute = MutableStateFlow(AudioRoute.PHONE_DEFAULT)
  override val currentRoute: StateFlow<AudioRoute> = _currentRoute.asStateFlow()

  private val _isHelmetAudioAvailable = MutableStateFlow(initialHelmetAudioAvailable)
  override val isHelmetAudioAvailable: StateFlow<Boolean> = _isHelmetAudioAvailable.asStateFlow()

  override fun requestRoute(route: AudioRoute): Boolean {
    if (route == AudioRoute.PHONE_DEFAULT) {
      _currentRoute.value = AudioRoute.PHONE_DEFAULT
      return true
    }
    return if (_isHelmetAudioAvailable.value) {
      _currentRoute.value = route
      true
    } else {
      false
    }
  }

  override fun resetToDefault(): Boolean {
    _currentRoute.value = AudioRoute.PHONE_DEFAULT
    return true
  }

  /**
   * Test/internal helper to simulate helmet audio peripheral availability without physical hardware.
   */
  fun setHelmetAudioAvailable(available: Boolean) {
    _isHelmetAudioAvailable.value = available
    if (!available && _currentRoute.value != AudioRoute.PHONE_DEFAULT) {
      _currentRoute.value = AudioRoute.PHONE_DEFAULT
    }
  }
}
