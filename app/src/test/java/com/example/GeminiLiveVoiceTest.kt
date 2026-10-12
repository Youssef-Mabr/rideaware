package com.example

import com.example.ai.GeminiLiveVoiceAssistant
import com.example.ai.GeminiLiveVoiceAssistantImpl
import com.example.ai.GeminiLiveVoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
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

  @Test fun setupMessageUsesGenerationConfigAndNativeAudioModel() {
    val payload = GeminiLiveVoiceAssistantImpl.setupMessage()
    val root = JSONObject(payload)
    val setup = root.getJSONObject("setup")
    assertEquals(
      "models/gemini-2.5-flash-native-audio-preview-12-2025",
      setup.getString("model")
    )
    val generationConfig = setup.getJSONObject("generationConfig")
    val modalities = generationConfig.getJSONArray("responseModalities")
    assertEquals(1, modalities.length())
    assertEquals("AUDIO", modalities.getString(0))
    assertTrue(setup.has("systemInstruction"))
    assertTrue(setup.has("inputAudioTranscription"))
    assertTrue(setup.has("outputAudioTranscription"))
  }

  @Test fun handleServerMessageParsesTranscriptionsAndTurnComplete() {
    val assistant = GeminiLiveVoiceAssistantImpl(apiKey = "test-key")
    assistant.handleServerMessage("""{"serverContent":{"inputTranscription":{"text":"How is my battery?"},"outputTranscription":{"text":"Your helmet battery is at 86 percent."},"turnComplete":true}}""")
    val turn = assistant.latestTurn.value
    assertNotNull(turn)
    assertEquals("How is my battery?", turn?.userTranscript)
    assertEquals("Your helmet battery is at 86 percent.", turn?.modelTranscript)
  }

  @Test fun parseSampleRateExtractsRateOrDefaultsTo24kHz() {
    assertEquals(24000, GeminiLiveVoiceAssistantImpl.parseSampleRate("audio/pcm;rate=24000"))
    assertEquals(16000, GeminiLiveVoiceAssistantImpl.parseSampleRate("audio/pcm;rate=16000"))
    assertEquals(24000, GeminiLiveVoiceAssistantImpl.parseSampleRate("audio/pcm"))
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