package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helmet.ButtonAction
import com.example.helmet.HelmetButton
import com.example.helmet.HelmetCommand
import com.example.helmet.HelmetConnectionState
import com.example.helmet.HelmetMessage
import com.example.helmet.LedPattern
import com.example.helmet.MockHelmetDevice
import com.example.helmet.SimulatedHelmetCommunicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class HelmetCommunicationUiState(
  val connectionState: HelmetConnectionState = HelmetConnectionState.Disconnected,
  val latestBattery: HelmetMessage.BatteryLevel? = null,
  val latestButtonEvent: HelmetMessage.ButtonEvent? = null,
  val latestAck: HelmetMessage.CommandAck? = null,
  val latestSentCommand: HelmetCommand? = null,
  val recentLogs: List<String> = emptyList()
)

class HelmetCommunicationViewModel(
  val communicator: SimulatedHelmetCommunicator = SimulatedHelmetCommunicator(autoConnect = false),
  val mockHelmetDevice: MockHelmetDevice = MockHelmetDevice(communicator),
  private val externalScope: CoroutineScope? = null
) : ViewModel() {

  private val scope: CoroutineScope
    get() = externalScope ?: viewModelScope

  private val _state = MutableStateFlow(HelmetCommunicationUiState())
  val state: StateFlow<HelmetCommunicationUiState> = _state.asStateFlow()

  private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

  init {
    scope.launch {
      communicator.connectionState.collect { connectionState ->
        _state.update { current ->
          current.copy(connectionState = connectionState)
        }
        val label = when (connectionState) {
          is HelmetConnectionState.Disconnected -> "Disconnected"
          is HelmetConnectionState.Connecting -> "Connecting to ${connectionState.deviceAddress ?: "helmet"} (attempt ${connectionState.attempt})"
          is HelmetConnectionState.Connected -> "Connected to ${connectionState.deviceName}"
          is HelmetConnectionState.Error -> "Error: ${connectionState.message} (recoverable=${connectionState.recoverable})"
        }
        appendLog("[STATE] $label")
      }
    }

    scope.launch {
      communicator.inboundMessages.collect { message ->
        handleInboundMessage(message)
      }
    }
  }

  private fun handleInboundMessage(message: HelmetMessage) {
    _state.update { current ->
      when (message) {
        is HelmetMessage.BatteryLevel -> current.copy(latestBattery = message)
        is HelmetMessage.ButtonEvent -> current.copy(latestButtonEvent = message)
        is HelmetMessage.CommandAck -> current.copy(latestAck = message)
        else -> current
      }
    }

    val logMsg = when (message) {
      is HelmetMessage.BatteryLevel -> "INBOUND: Battery ${message.percent}% (charging=${message.isCharging})"
      is HelmetMessage.ButtonEvent -> "INBOUND: Button ${message.button} -> ${message.action}"
      is HelmetMessage.CommandAck -> "INBOUND: Ack for ${message.targetCommandId.take(8)}... -> success=${message.success}${if (message.errorMessage != null) " (${message.errorMessage})" else ""}"
      is HelmetMessage.DeviceInfo -> "INBOUND: DeviceInfo FW ${message.firmwareVersion}, SN ${message.serialNumber}"
      is HelmetMessage.LedStatus -> "INBOUND: LedStatus ${message.pattern} (active=${message.active})"
      is HelmetMessage.Heartbeat -> "INBOUND: Heartbeat uptime=${message.uptimeSeconds}s"
    }
    appendLog(logMsg)
  }

  fun connectDemoHelmet(delayMillis: Long = 400L) {
    appendLog("ACTION: Connect Demo Helmet pressed")
    mockHelmetDevice.connectDemo(delayMillis = delayMillis, autoSendTelemetry = true)
  }

  fun disconnectDemoHelmet() {
    appendLog("ACTION: Disconnect Demo Helmet pressed")
    mockHelmetDevice.disconnectDemo()
  }

  fun simulateConnectionFailure(recoverable: Boolean = true) {
    appendLog("ACTION: Simulate Connection Failure pressed")
    mockHelmetDevice.simulateConnectionError(
      reason = "DEMO / SIMULATED: Bluetooth link dropped",
      recoverable = recoverable
    )
  }

  fun reconnectAndRecover() {
    appendLog("ACTION: Reconnect & Recover pressed")
    mockHelmetDevice.recoverConnection(autoSendTelemetry = true)
  }

  fun sendSampleBattery(percent: Int = 85) {
    appendLog("ACTION: Send sample battery $percent%")
    mockHelmetDevice.sendBatteryLevel(percent = percent, isCharging = false)
  }

  fun sendSampleButtonEvent(button: HelmetButton = HelmetButton.VOICE_PTT, action: ButtonAction = ButtonAction.PRESS) {
    appendLog("ACTION: Send sample button event $button ($action)")
    mockHelmetDevice.sendButtonEvent(button = button, action = action)
  }

  fun sendTestPing() {
    val cmd = HelmetCommand.Ping()
    _state.update { it.copy(latestSentCommand = cmd) }
    appendLog("OUTBOUND: Dispatching Ping command (${cmd.commandId.take(8)}...)")
    communicator.sendCommand(cmd)
  }

  fun sendTestBatteryRequest() {
    val cmd = HelmetCommand.RequestBattery()
    _state.update { it.copy(latestSentCommand = cmd) }
    appendLog("OUTBOUND: Dispatching RequestBattery command (${cmd.commandId.take(8)}...)")
    communicator.sendCommand(cmd)
  }

  fun sendTestLedCommand(pattern: LedPattern = LedPattern.PULSE_TEAL) {
    val cmd = HelmetCommand.SetLedPattern(pattern = pattern, durationMillis = 2000L)
    _state.update { it.copy(latestSentCommand = cmd) }
    appendLog("OUTBOUND: Dispatching SetLedPattern($pattern)")
    communicator.sendCommand(cmd)
  }

  fun sendUnsupportedCommandForSafetyTest() {
    // Custom unsupported command to test safe non-crashing rejection and error acknowledgement
    val cmd = com.example.helmet.MockUnsupportedCommand()
    _state.update { it.copy(latestSentCommand = cmd) }
    appendLog("OUTBOUND: Dispatching unsupported test command (${cmd.commandId.take(8)}...)")
    communicator.sendCommand(cmd)
  }

  fun clearLogs() {
    _state.update { it.copy(recentLogs = emptyList()) }
  }

  private fun appendLog(entry: String) {
    val timestamp = timeFormat.format(Date())
    val formatted = "$timestamp [DEMO / SIMULATED] $entry"
    _state.update { current ->
      current.copy(recentLogs = (listOf(formatted) + current.recentLogs).take(20))
    }
  }
}
