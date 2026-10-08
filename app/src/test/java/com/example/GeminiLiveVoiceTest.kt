package com.example

import com.example.ai.GeminiLiveVoiceAssistant
import com.example.ai.GeminiLiveVoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiLiveVoiceTest {
  @Test fun fakeAssistantCanModelConnectListeningAndDisconnect() {
    val fake = FakeLiveAssistant()
    assertEquals(GeminiLiveVoiceState.DISCONNECTED, fake.state.value)
    fake.connect()
    assertEquals(GeminiLiveVoiceState.LISTENING, fake.state.value)
    fake.disconnect()
    assertEquals(GeminiLiveVoiceState.DISCONNECTED, fake.state.value)
  }

  @Test fun disconnectIsIdempotent() {
    val fake = FakeLiveAssistant()
    fake.disconnect()
    fake.disconnect()
    assertTrue(fake.disconnectCount == 2)
  }

  private class FakeLiveAssistant : GeminiLiveVoiceAssistant {
    override val state = MutableStateFlow(GeminiLiveVoiceState.DISCONNECTED)
    var disconnectCount = 0
    override fun connect() { state.value = GeminiLiveVoiceState.LISTENING }
    override fun startListening() { state.value = GeminiLiveVoiceState.LISTENING }
    override fun stopListening() { state.value = GeminiLiveVoiceState.DISCONNECTED }
    override fun disconnect() { disconnectCount++; state.value = GeminiLiveVoiceState.DISCONNECTED }
  }
}