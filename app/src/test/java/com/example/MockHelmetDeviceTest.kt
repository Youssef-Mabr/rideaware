package com.example

import com.example.helmet.ButtonAction
import com.example.helmet.HapticIntensity
import com.example.helmet.HelmetButton
import com.example.helmet.HelmetCommand
import com.example.helmet.HelmetConnectionState
import com.example.helmet.HelmetMessage
import com.example.helmet.LedPattern
import com.example.helmet.MockHelmetDevice
import com.example.helmet.SimulatedHelmetCommunicator
import com.example.ui.HelmetCommunicationViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MockHelmetDeviceTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @Test
  fun simulatedConnectAndDisconnectTransitionsStateAndEmitsInitialTelemetry() = runTest(testDispatcher) {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = false)
    val mockDevice = MockHelmetDevice(communicator = communicator, scope = testScope)

    // 1. Initial state is Disconnected
    assertEquals(HelmetConnectionState.Disconnected, communicator.connectionState.value)

    val receivedMessages = mutableListOf<HelmetMessage>()
    val collectJob = launch {
      communicator.inboundMessages.collect { receivedMessages.add(it) }
    }

    // 2. Connect demo helmet
    mockDevice.connectDemo(delayMillis = 0L, autoSendTelemetry = true)

    val connectedState = communicator.connectionState.value
    assertTrue(connectedState is HelmetConnectionState.Connected)
    val connected = connectedState as HelmetConnectionState.Connected
    assertEquals("DEMO-HELMET-01", connected.deviceId)
    assertTrue(connected.deviceName.contains("DEMO / SIMULATED"))

    // Initial telemetry emitted
    assertTrue(receivedMessages.any { it is HelmetMessage.BatteryLevel })
    assertTrue(receivedMessages.any { it is HelmetMessage.DeviceInfo })

    // 3. Disconnect demo helmet
    mockDevice.disconnectDemo()
    assertEquals(HelmetConnectionState.Disconnected, communicator.connectionState.value)

    collectJob.cancel()
  }

  @Test
  fun inboundMessageDeliveryToCollectors() = runTest(testDispatcher) {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = false)
    val mockDevice = MockHelmetDevice(communicator = communicator, scope = testScope)

    val received = mutableListOf<HelmetMessage>()
    val collectJob = launch {
      communicator.inboundMessages.collect { received.add(it) }
    }

    mockDevice.sendBatteryLevel(percent = 95, isCharging = true)
    mockDevice.sendButtonEvent(button = HelmetButton.SOS, action = ButtonAction.LONG_PRESS)
    mockDevice.sendHeartbeat(uptimeSeconds = 360L)
    mockDevice.sendDeviceInfo(
      serialNumber = "DEMO-SN-999",
      firmwareVersion = "v2.5.0-DEMO",
      hardwareRevision = "Rev 4-SIM"
    )

    assertEquals(4, received.size)

    val battery = received[0] as HelmetMessage.BatteryLevel
    assertEquals(95, battery.percent)
    assertTrue(battery.isCharging)

    val button = received[1] as HelmetMessage.ButtonEvent
    assertEquals(HelmetButton.SOS, button.button)
    assertEquals(ButtonAction.LONG_PRESS, button.action)

    val heartbeat = received[2] as HelmetMessage.Heartbeat
    assertEquals(360L, heartbeat.uptimeSeconds)

    val devInfo = received[3] as HelmetMessage.DeviceInfo
    assertEquals("DEMO-SN-999", devInfo.serialNumber)
    assertEquals("v2.5.0-DEMO", devInfo.firmwareVersion)

    collectJob.cancel()
  }

  @Test
  fun outboundCommandAndAcknowledgementHandling() = runTest(testDispatcher) {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = false)
    val mockDevice = MockHelmetDevice(communicator = communicator, scope = testScope)
    mockDevice.connectDemo(delayMillis = 0L, autoSendTelemetry = false)

    val inbound = mutableListOf<HelmetMessage>()
    val collectJob = launch {
      communicator.inboundMessages.collect { inbound.add(it) }
    }

    // Ping command -> Ack + Heartbeat
    assertTrue(communicator.sendCommand(HelmetCommand.Ping(commandId = "cmd-ping-1")))
    val pingAck = inbound.filterIsInstance<HelmetMessage.CommandAck>().firstOrNull { it.targetCommandId == "cmd-ping-1" }
    assertNotNull(pingAck)
    assertTrue(pingAck!!.success)
    assertTrue(inbound.any { it is HelmetMessage.Heartbeat })

    // RequestBattery -> Ack + BatteryLevel
    assertTrue(communicator.sendCommand(HelmetCommand.RequestBattery(commandId = "cmd-bat-2")))
    val batAck = inbound.filterIsInstance<HelmetMessage.CommandAck>().firstOrNull { it.targetCommandId == "cmd-bat-2" }
    assertNotNull(batAck)
    assertTrue(batAck!!.success)
    assertTrue(inbound.any { it is HelmetMessage.BatteryLevel })

    // RequestDeviceInfo -> Ack + DeviceInfo
    assertTrue(communicator.sendCommand(HelmetCommand.RequestDeviceInfo(commandId = "cmd-info-3")))
    val infoAck = inbound.filterIsInstance<HelmetMessage.CommandAck>().firstOrNull { it.targetCommandId == "cmd-info-3" }
    assertNotNull(infoAck)
    assertTrue(infoAck!!.success)
    assertTrue(inbound.any { it is HelmetMessage.DeviceInfo })

    // SetLedPattern -> Ack + LedStatus
    assertTrue(communicator.sendCommand(HelmetCommand.SetLedPattern(pattern = LedPattern.SOS_RED_STROBE, commandId = "cmd-led-4")))
    val ledAck = inbound.filterIsInstance<HelmetMessage.CommandAck>().firstOrNull { it.targetCommandId == "cmd-led-4" }
    assertNotNull(ledAck)
    assertTrue(ledAck!!.success)
    val ledStatus = inbound.filterIsInstance<HelmetMessage.LedStatus>().firstOrNull()
    assertNotNull(ledStatus)
    assertEquals(LedPattern.SOS_RED_STROBE, ledStatus!!.pattern)
    assertTrue(ledStatus.active)

    // TriggerHapticFeedback -> Ack
    assertTrue(communicator.sendCommand(HelmetCommand.TriggerHapticFeedback(intensity = HapticIntensity.HIGH, commandId = "cmd-haptic-5")))
    val hapticAck = inbound.filterIsInstance<HelmetMessage.CommandAck>().firstOrNull { it.targetCommandId == "cmd-haptic-5" }
    assertNotNull(hapticAck)
    assertTrue(hapticAck!!.success)

    // Disconnect -> Ack + Disconnected
    assertTrue(communicator.sendCommand(HelmetCommand.Disconnect(commandId = "cmd-disc-6")))
    val discAck = inbound.filterIsInstance<HelmetMessage.CommandAck>().firstOrNull { it.targetCommandId == "cmd-disc-6" }
    assertNotNull(discAck)
    assertTrue(discAck!!.success)
    assertEquals(HelmetConnectionState.Disconnected, communicator.connectionState.value)

    collectJob.cancel()
  }

  @Test
  fun batteryAndButtonEventHandling() = runTest(testDispatcher) {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = false)
    val mockDevice = MockHelmetDevice(communicator = communicator, scope = testScope)

    val received = mutableListOf<HelmetMessage>()
    val collectJob = launch {
      communicator.inboundMessages.collect { received.add(it) }
    }

    // Battery coercion limits: -10 coerces to 0, 150 coerces to 100
    mockDevice.sendBatteryLevel(-10, isCharging = false)
    mockDevice.sendBatteryLevel(150, isCharging = true)
    mockDevice.sendBatteryLevel(74, isCharging = false)

    val batteries = received.filterIsInstance<HelmetMessage.BatteryLevel>()
    assertEquals(3, batteries.size)
    assertEquals(0, batteries[0].percent)
    assertEquals(100, batteries[1].percent)
    assertEquals(74, batteries[2].percent)

    // All button types and actions
    mockDevice.sendButtonEvent(HelmetButton.VOICE_PTT, ButtonAction.PRESS)
    mockDevice.sendButtonEvent(HelmetButton.VOICE_PTT, ButtonAction.RELEASE)
    mockDevice.sendButtonEvent(HelmetButton.CAMERA_CAPTURE, ButtonAction.PRESS)
    mockDevice.sendButtonEvent(HelmetButton.POWER, ButtonAction.LONG_PRESS)
    mockDevice.sendButtonEvent(HelmetButton.SOS, ButtonAction.PRESS)

    val buttons = received.filterIsInstance<HelmetMessage.ButtonEvent>()
    assertEquals(5, buttons.size)
    assertEquals(HelmetButton.VOICE_PTT, buttons[0].button)
    assertEquals(ButtonAction.PRESS, buttons[0].action)
    assertEquals(ButtonAction.RELEASE, buttons[1].action)
    assertEquals(HelmetButton.CAMERA_CAPTURE, buttons[2].button)
    assertEquals(HelmetButton.POWER, buttons[3].button)
    assertEquals(ButtonAction.LONG_PRESS, buttons[3].action)
    assertEquals(HelmetButton.SOS, buttons[4].button)

    collectJob.cancel()
  }

  @Test
  fun errorStateAndRecovery() = runTest(testDispatcher) {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = false)
    val mockDevice = MockHelmetDevice(communicator = communicator, scope = testScope)

    mockDevice.connectDemo(delayMillis = 0L, autoSendTelemetry = false)
    assertTrue(communicator.connectionState.value is HelmetConnectionState.Connected)

    // Simulate connection failure
    mockDevice.simulateConnectionError("DEMO / SIMULATED: Bluetooth link dropped", recoverable = true)
    val errorState = communicator.connectionState.value
    assertTrue(errorState is HelmetConnectionState.Error)
    val error = errorState as HelmetConnectionState.Error
    assertEquals("DEMO / SIMULATED: Bluetooth link dropped", error.message)
    assertTrue(error.recoverable)

    // Commands rejected during Error state
    assertFalse(communicator.sendCommand(HelmetCommand.Ping()))

    // Recover connection
    mockDevice.recoverConnection(autoSendTelemetry = true)
    assertTrue(communicator.connectionState.value is HelmetConnectionState.Connected)

    // Commands accepted after recovery
    assertTrue(communicator.sendCommand(HelmetCommand.Ping("recovered-ping")))
    assertTrue(mockDevice.receivedCommands.any { it.commandId == "recovered-ping" })
  }

  @Test
  fun safeHandlingOfUnexpectedOrInvalidMessages() = runTest(testDispatcher) {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = false)
    val mockDevice = MockHelmetDevice(communicator = communicator, scope = testScope)
    mockDevice.connectDemo(delayMillis = 0L, autoSendTelemetry = false)

    // Custom unsupported command
    val unsupportedCommand = com.example.helmet.MockUnsupportedCommand(
      commandId = "unsupported-test-01"
    )

    // Should NOT throw exception
    val accepted = communicator.sendCommand(unsupportedCommand)
    assertTrue(accepted)

    val ack = mockDevice.dispatchedAcks.firstOrNull { it.targetCommandId == "unsupported-test-01" }
    assertNotNull(ack)
    assertFalse(ack!!.success)
    assertTrue(ack.errorMessage?.contains("unsupported") == true)

    // Raw packet edge cases
    assertFalse(mockDevice.handleRawPacketSafely(null))
    assertFalse(mockDevice.handleRawPacketSafely(byteArrayOf()))
    assertTrue(mockDevice.handleRawPacketSafely("PING".toByteArray(Charsets.UTF_8)))
    assertTrue(mockDevice.handleRawPacketSafely(byteArrayOf(0x00, 0xFF.toByte(), 0xFE.toByte(), 0xAA.toByte())))
  }

  @Test
  fun viewModelManagesLifecycleAndLogs() = runTest(testDispatcher) {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = false)
    val mockDevice = MockHelmetDevice(communicator = communicator, scope = testScope)
    val viewModel = HelmetCommunicationViewModel(
      communicator = communicator,
      mockHelmetDevice = mockDevice,
      externalScope = testScope
    )

    // Initial state
    assertEquals(HelmetConnectionState.Disconnected, viewModel.state.value.connectionState)
    assertTrue(viewModel.state.value.recentLogs.isNotEmpty())
    assertTrue(viewModel.state.value.recentLogs.first().contains("Disconnected"))

    // Connect immediately
    viewModel.connectDemoHelmet(delayMillis = 0L)

    assertTrue(viewModel.state.value.connectionState is HelmetConnectionState.Connected)
    assertNotNull(viewModel.state.value.latestBattery)

    // Send Ping
    viewModel.sendTestPing()
    assertNotNull(viewModel.state.value.latestAck)
    assertTrue(viewModel.state.value.latestAck!!.success)

    // Send Button Event
    viewModel.sendSampleButtonEvent(HelmetButton.VOICE_PTT, ButtonAction.PRESS)
    assertEquals(HelmetButton.VOICE_PTT, viewModel.state.value.latestButtonEvent?.button)

    // Fail & Recover
    viewModel.simulateConnectionFailure(recoverable = true)
    assertTrue(viewModel.state.value.connectionState is HelmetConnectionState.Error)

    viewModel.reconnectAndRecover()
    assertTrue(viewModel.state.value.connectionState is HelmetConnectionState.Connected)

    // Disconnect
    viewModel.disconnectDemoHelmet()
    assertEquals(HelmetConnectionState.Disconnected, viewModel.state.value.connectionState)

    // Verify logs
    assertTrue(viewModel.state.value.recentLogs.any { it.contains("[DEMO / SIMULATED]") })
  }
}
