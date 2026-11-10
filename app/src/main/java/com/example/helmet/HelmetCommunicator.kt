package com.example.helmet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Core communication interface for interacting with the smart helmet.
 * Manages connection lifecycle, command dispatch, telemetry streaming,
 * and audio route coordination.
 */
interface HelmetCommunicator {
  /**
   * Current connection state of the helmet link.
   */
  val connectionState: StateFlow<HelmetConnectionState>

  /**
   * Inbound stream of typed messages and telemetry received from the helmet.
   */
  val inboundMessages: SharedFlow<HelmetMessage>

  /**
   * Audio routing manager for helmet audio peripherals.
   */
  val audioRouter: HelmetAudioRouter

  /**
   * Initiates a connection to a helmet device.
   *
   * @param deviceAddress Device identifier or address to connect to.
   * @return True if connection initiation was accepted, false otherwise.
   */
  fun connect(deviceAddress: String): Boolean

  /**
   * Disconnects the active helmet session and resets state to [HelmetConnectionState.Disconnected].
   */
  fun disconnect()

  /**
   * Transmits a typed command to the connected helmet.
   *
   * @param command The typed command to dispatch.
   * @return True if the command was dispatched, false if not connected or dispatch rejected.
   */
  fun sendCommand(command: HelmetCommand): Boolean
}

/**
 * Testable, software-only implementation of [HelmetCommunicator].
 * Allows testing connection-state transitions, command transmission, and telemetry handling
 * without requiring physical Bluetooth hardware or runtime device discovery.
 */
class SimulatedHelmetCommunicator(
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
  override val audioRouter: DefaultHelmetAudioRouter = DefaultHelmetAudioRouter(),
  private val autoConnect: Boolean = true
) : HelmetCommunicator {

  private val _connectionState = MutableStateFlow<HelmetConnectionState>(HelmetConnectionState.Disconnected)
  override val connectionState: StateFlow<HelmetConnectionState> = _connectionState.asStateFlow()

  private val _inboundMessages = MutableSharedFlow<HelmetMessage>(replay = 0, extraBufferCapacity = 64)
  override val inboundMessages: SharedFlow<HelmetMessage> = _inboundMessages.asSharedFlow()

  private val _commandStream = MutableSharedFlow<HelmetCommand>(replay = 0, extraBufferCapacity = 64)
  val commandStream: SharedFlow<HelmetCommand> = _commandStream.asSharedFlow()

  /**
   * Optional synchronous command listener for immediate mock device dispatch.
   */
  var directCommandListener: ((HelmetCommand) -> Unit)? = null

  private val _sentCommands = mutableListOf<HelmetCommand>()
  val sentCommands: List<HelmetCommand>
    get() = synchronized(_sentCommands) { _sentCommands.toList() }

  override fun connect(deviceAddress: String): Boolean {
    if (deviceAddress.isBlank()) {
      _connectionState.value = HelmetConnectionState.Error(
        message = "Device address cannot be blank.",
        recoverable = true
      )
      return false
    }

    val current = _connectionState.value
    if (current is HelmetConnectionState.Connected && current.deviceId == deviceAddress) {
      return true
    }

    _connectionState.value = HelmetConnectionState.Connecting(
      deviceAddress = deviceAddress,
      attempt = if (current is HelmetConnectionState.Connecting) current.attempt + 1 else 1
    )

    if (autoConnect) {
      _connectionState.value = HelmetConnectionState.Connected(
        deviceId = deviceAddress,
        deviceName = "Geni One ($deviceAddress)",
        connectedAtMillis = System.currentTimeMillis()
      )
    }

    return true
  }

  override fun disconnect() {
    _connectionState.value = HelmetConnectionState.Disconnected
    audioRouter.resetToDefault()
  }

  override fun sendCommand(command: HelmetCommand): Boolean {
    if (_connectionState.value !is HelmetConnectionState.Connected) {
      return false
    }

    synchronized(_sentCommands) {
      _sentCommands.add(command)
    }

    directCommandListener?.invoke(command)

    scope.launch {
      _commandStream.emit(command)
    }

    if (command is HelmetCommand.Disconnect) {
      disconnect()
    }

    return true
  }

  /**
   * Sets the state to [HelmetConnectionState.Connecting].
   */
  fun simulateConnecting(deviceAddress: String, attempt: Int = 1) {
    _connectionState.value = HelmetConnectionState.Connecting(
      deviceAddress = deviceAddress,
      attempt = attempt
    )
  }

  /**
   * Simulates successful connection completion when [autoConnect] is false.
   */
  fun simulateConnectionSuccess(deviceId: String, deviceName: String = "Geni One") {
    _connectionState.value = HelmetConnectionState.Connected(
      deviceId = deviceId,
      deviceName = deviceName,
      connectedAtMillis = System.currentTimeMillis()
    )
  }

  /**
   * Simulates an unexpected error or communication failure.
   */
  fun simulateError(message: String, cause: Throwable? = null, recoverable: Boolean = true) {
    _connectionState.value = HelmetConnectionState.Error(
      message = message,
      cause = cause,
      recoverable = recoverable
    )
  }

  /**
   * Dispatches a simulated inbound message/telemetry event to collectors.
   */
  fun simulateInboundMessage(message: HelmetMessage) {
    scope.launch {
      _inboundMessages.emit(message)
    }
  }

  /**
   * Clears the recorded command history.
   */
  fun clearSentCommands() {
    synchronized(_sentCommands) {
      _sentCommands.clear()
    }
  }
}
