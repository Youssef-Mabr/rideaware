package com.example.helmet

import java.util.UUID

/**
 * Visual warning indicator patterns supported by the smart helmet LED strips.
 */
enum class LedPattern {
  OFF,
  SOLID_WHITE,
  PULSE_TEAL,
  HAZARD_AMBER_FLASH,
  SOS_RED_STROBE
}

/**
 * Haptic alert intensities supported by the helmet collar/liner vibrator.
 */
enum class HapticIntensity {
  LOW,
  MEDIUM,
  HIGH
}

/**
 * Physical helmet buttons mapped to rider inputs.
 */
enum class HelmetButton {
  SOS,
  VOICE_PTT,
  CAMERA_CAPTURE,
  POWER
}

/**
 * Actions performed on helmet buttons.
 */
enum class ButtonAction {
  PRESS,
  LONG_PRESS,
  RELEASE
}

/**
 * High-level typed commands dispatched from the phone to the smart helmet.
 * These represent domain intentions without prescribing proprietary wire/byte framing.
 */
sealed interface HelmetCommand {
  val commandId: String

  data class Ping(
    override val commandId: String = UUID.randomUUID().toString()
  ) : HelmetCommand

  data class RequestBattery(
    override val commandId: String = UUID.randomUUID().toString()
  ) : HelmetCommand

  data class RequestDeviceInfo(
    override val commandId: String = UUID.randomUUID().toString()
  ) : HelmetCommand

  data class SetLedPattern(
    val pattern: LedPattern,
    val durationMillis: Long = 0L,
    override val commandId: String = UUID.randomUUID().toString()
  ) : HelmetCommand

  data class TriggerHapticFeedback(
    val intensity: HapticIntensity,
    val durationMillis: Long = 300L,
    override val commandId: String = UUID.randomUUID().toString()
  ) : HelmetCommand

  data class Disconnect(
    override val commandId: String = UUID.randomUUID().toString()
  ) : HelmetCommand
}

/**
 * Typed messages received from the helmet by the phone app.
 */
sealed interface HelmetMessage {
  val timestampMillis: Long

  data class BatteryLevel(
    val percent: Int,
    val isCharging: Boolean,
    override val timestampMillis: Long = System.currentTimeMillis()
  ) : HelmetMessage

  data class ButtonEvent(
    val button: HelmetButton,
    val action: ButtonAction,
    override val timestampMillis: Long = System.currentTimeMillis()
  ) : HelmetMessage

  data class DeviceInfo(
    val serialNumber: String,
    val firmwareVersion: String,
    val hardwareRevision: String,
    override val timestampMillis: Long = System.currentTimeMillis()
  ) : HelmetMessage

  data class LedStatus(
    val pattern: LedPattern,
    val active: Boolean,
    override val timestampMillis: Long = System.currentTimeMillis()
  ) : HelmetMessage

  data class Heartbeat(
    val uptimeSeconds: Long,
    override val timestampMillis: Long = System.currentTimeMillis()
  ) : HelmetMessage

  data class CommandAck(
    val targetCommandId: String,
    val success: Boolean,
    val errorMessage: String? = null,
    override val timestampMillis: Long = System.currentTimeMillis()
  ) : HelmetMessage
}
