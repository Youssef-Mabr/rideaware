package com.example

import com.example.helmet.AudioRoute
import com.example.helmet.ButtonAction
import com.example.helmet.DefaultHelmetAudioRouter
import com.example.helmet.HapticIntensity
import com.example.helmet.HelmetButton
import com.example.helmet.HelmetCommand
import com.example.helmet.HelmetConnectionState
import com.example.helmet.HelmetMessage
import com.example.helmet.LedPattern
import com.example.helmet.SimulatedHelmetCommunicator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HelmetCommunicatorTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @Test
  fun initialStateIsDisconnected() {
    val communicator = SimulatedHelmetCommunicator(scope = testScope)
    assertEquals(HelmetConnectionState.Disconnected, communicator.connectionState.value)
  }

  @Test
  fun validAutoConnectTransitionsToConnected() {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = true)
    val success = communicator.connect("RA-DEMO-001")

    assertTrue(success)
    val state = communicator.connectionState.value
    assertTrue(state is HelmetConnectionState.Connected)
    val connected = state as HelmetConnectionState.Connected
    assertEquals("RA-DEMO-001", connected.deviceId)
    assertTrue(connected.deviceName.contains("RA-DEMO-001"))
  }

  @Test
  fun stepByStepConnectionTransitionsThroughConnectingToConnected() {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = false)

    val accepted = communicator.connect("RA-HELMET-42")
    assertTrue(accepted)

    val connectingState = communicator.connectionState.value
    assertTrue(connectingState is HelmetConnectionState.Connecting)
    assertEquals("RA-HELMET-42", (connectingState as HelmetConnectionState.Connecting).deviceAddress)

    communicator.simulateConnectionSuccess(deviceId = "RA-HELMET-42", deviceName = "Geni Smart One")
    val connectedState = communicator.connectionState.value
    assertTrue(connectedState is HelmetConnectionState.Connected)
    val connected = connectedState as HelmetConnectionState.Connected
    assertEquals("RA-HELMET-42", connected.deviceId)
    assertEquals("Geni Smart One", connected.deviceName)
  }

  @Test
  fun connectingCanTransitionToError() {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = false)
    communicator.connect("RA-TIMEOUT-001")

    assertTrue(communicator.connectionState.value is HelmetConnectionState.Connecting)

    communicator.simulateError("Connection timed out after 10s", recoverable = true)
    val errorState = communicator.connectionState.value
    assertTrue(errorState is HelmetConnectionState.Error)
    val error = errorState as HelmetConnectionState.Error
    assertEquals("Connection timed out after 10s", error.message)
    assertTrue(error.recoverable)
  }

  @Test
  fun blankDeviceAddressTransitionsDirectlyToError() {
    val communicator = SimulatedHelmetCommunicator(scope = testScope)
    val accepted = communicator.connect("   ")

    assertFalse(accepted)
    val state = communicator.connectionState.value
    assertTrue(state is HelmetConnectionState.Error)
    assertEquals("Device address cannot be blank.", (state as HelmetConnectionState.Error).message)
  }

  @Test
  fun errorStateCanRecoverViaNewConnectionAttempt() {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = false)
    communicator.simulateError("Peripheral out of range", recoverable = true)
    assertTrue(communicator.connectionState.value is HelmetConnectionState.Error)

    // Reconnecting from error should move back to Connecting
    val reconnectAccepted = communicator.connect("RA-RETRY-002")
    assertTrue(reconnectAccepted)
    val connecting = communicator.connectionState.value
    assertTrue(connecting is HelmetConnectionState.Connecting)
    assertEquals("RA-RETRY-002", (connecting as HelmetConnectionState.Connecting).deviceAddress)

    communicator.simulateConnectionSuccess("RA-RETRY-002")
    assertTrue(communicator.connectionState.value is HelmetConnectionState.Connected)
  }

  @Test
  fun disconnectTransitionsFromConnectedToDisconnected() {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = true)
    communicator.connect("RA-DEMO-001")
    assertTrue(communicator.connectionState.value is HelmetConnectionState.Connected)

    communicator.disconnect()
    assertEquals(HelmetConnectionState.Disconnected, communicator.connectionState.value)
  }

  @Test
  fun disconnectTransitionsFromErrorToDisconnected() {
    val communicator = SimulatedHelmetCommunicator(scope = testScope)
    communicator.simulateError("Fatal link error", recoverable = false)
    assertTrue(communicator.connectionState.value is HelmetConnectionState.Error)

    communicator.disconnect()
    assertEquals(HelmetConnectionState.Disconnected, communicator.connectionState.value)
  }

  @Test
  fun commandsOnlyDispatchedWhenConnected() {
    val communicator = SimulatedHelmetCommunicator(scope = testScope, autoConnect = false)

    // 1. Rejected when Disconnected
    assertFalse(communicator.sendCommand(HelmetCommand.Ping()))
    assertTrue(communicator.sentCommands.isEmpty())

    // 2. Rejected when Connecting
    communicator.connect("RA-DEMO-001")
    assertFalse(communicator.sendCommand(HelmetCommand.RequestBattery()))
    assertTrue(communicator.sentCommands.isEmpty())

    // 3. Accepted when Connected
    communicator.simulateConnectionSuccess("RA-DEMO-001")
    assertTrue(communicator.sendCommand(HelmetCommand.Ping("cmd-1")))
    assertTrue(communicator.sendCommand(HelmetCommand.RequestBattery("cmd-2")))
    assertTrue(communicator.sendCommand(HelmetCommand.SetLedPattern(LedPattern.HAZARD_AMBER_FLASH, durationMillis = 2000L, commandId = "cmd-3")))
    assertTrue(communicator.sendCommand(HelmetCommand.TriggerHapticFeedback(HapticIntensity.HIGH, durationMillis = 500L, commandId = "cmd-4")))

    val sent = communicator.sentCommands
    assertEquals(4, sent.size)
    assertEquals("cmd-1", sent[0].commandId)
    assertEquals("cmd-2", sent[1].commandId)
    assertEquals(LedPattern.HAZARD_AMBER_FLASH, (sent[2] as HelmetCommand.SetLedPattern).pattern)
    assertEquals(HapticIntensity.HIGH, (sent[3] as HelmetCommand.TriggerHapticFeedback).intensity)

    // 4. Disconnect command automatically disconnects
    assertTrue(communicator.sendCommand(HelmetCommand.Disconnect("cmd-5")))
    assertEquals(HelmetConnectionState.Disconnected, communicator.connectionState.value)
  }

  @Test
  fun inboundMessagesBroadcastToCollectors() = runTest(testDispatcher) {
    val communicator = SimulatedHelmetCommunicator(scope = testScope)
    val received = mutableListOf<HelmetMessage>()

    val job = launch {
      communicator.inboundMessages.collect { received.add(it) }
    }

    communicator.simulateInboundMessage(HelmetMessage.BatteryLevel(percent = 92, isCharging = true))
    communicator.simulateInboundMessage(HelmetMessage.ButtonEvent(button = HelmetButton.SOS, action = ButtonAction.PRESS))
    communicator.simulateInboundMessage(HelmetMessage.Heartbeat(uptimeSeconds = 120))
    communicator.simulateInboundMessage(HelmetMessage.CommandAck(targetCommandId = "cmd-1", success = true))

    assertEquals(4, received.size)
    assertTrue(received[0] is HelmetMessage.BatteryLevel)
    assertEquals(92, (received[0] as HelmetMessage.BatteryLevel).percent)

    assertTrue(received[1] is HelmetMessage.ButtonEvent)
    assertEquals(HelmetButton.SOS, (received[1] as HelmetMessage.ButtonEvent).button)

    assertTrue(received[2] is HelmetMessage.Heartbeat)
    assertEquals(120L, (received[2] as HelmetMessage.Heartbeat).uptimeSeconds)

    assertTrue(received[3] is HelmetMessage.CommandAck)
    assertEquals("cmd-1", (received[3] as HelmetMessage.CommandAck).targetCommandId)

    job.cancel()
  }

  @Test
  fun audioRouterDefaultsToPhoneAndResetsOnDisconnect() {
    val audioRouter = DefaultHelmetAudioRouter(initialHelmetAudioAvailable = false)
    val communicator = SimulatedHelmetCommunicator(scope = testScope, audioRouter = audioRouter, autoConnect = true)

    // Default route is phone
    assertEquals(AudioRoute.PHONE_DEFAULT, audioRouter.currentRoute.value)
    assertFalse(audioRouter.isHelmetAudioAvailable.value)

    // Routing to helmet headset fails if helmet audio is unavailable
    assertFalse(audioRouter.requestRoute(AudioRoute.HELMET_HEADSET))
    assertEquals(AudioRoute.PHONE_DEFAULT, audioRouter.currentRoute.value)

    // When helmet audio is available, route can be switched
    audioRouter.setHelmetAudioAvailable(true)
    assertTrue(audioRouter.isHelmetAudioAvailable.value)
    assertTrue(audioRouter.requestRoute(AudioRoute.HELMET_HEADSET))
    assertEquals(AudioRoute.HELMET_HEADSET, audioRouter.currentRoute.value)

    // Disconnecting resets route to phone default
    communicator.disconnect()
    assertEquals(AudioRoute.PHONE_DEFAULT, audioRouter.currentRoute.value)
  }
}
