package com.example.ai

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class GeminiLiveVoiceState { DISCONNECTED, CONNECTING, LISTENING, SPEAKING, ERROR }

interface GeminiLiveVoiceAssistant {
  val state: kotlinx.coroutines.flow.StateFlow<GeminiLiveVoiceState>
  fun connect()
  fun startListening()
  fun stopListening()
  fun disconnect()
}

class GeminiLiveVoiceAssistantImpl(
  private val apiKey: String = BuildConfig.GEMINI_API_KEY,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .build()
) : GeminiLiveVoiceAssistant {
  private val mutableState = MutableStateFlow(GeminiLiveVoiceState.DISCONNECTED)
  override val state = mutableState.asStateFlow()
  private var socket: WebSocket? = null
  private var captureJob: Job? = null
  private var recorder: AudioRecord? = null
  private var player: AudioTrack? = null
  private var setupComplete = false
  private var sentChunkCount = 0
  private var receivedChunkCount = 0

  override fun connect() {
    if (state.value == GeminiLiveVoiceState.CONNECTING || socket != null) return
    if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
      mutableState.value = GeminiLiveVoiceState.ERROR
      return
    }
    mutableState.value = GeminiLiveVoiceState.CONNECTING
    // Production releases should use constrained ephemeral tokens or backend mediation,
    // rather than exposing a long-lived API key in a client application.
    val request = Request.Builder().url("$LIVE_ENDPOINT?key=$apiKey").build()
    socket = client.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
        webSocket.send(setupMessage())
        Log.i(TAG, "Gemini Live WebSocket opened; waiting for setup confirmation")
      }

      override fun onMessage(webSocket: WebSocket, text: String) = handleServerMessage(text)
      override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
        Log.e(TAG, "Gemini Live WebSocket failed: ${t.message}")
        cleanup(); socket = null; mutableState.value = GeminiLiveVoiceState.ERROR
      }
      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { Log.i(TAG, "Gemini Live closing: $code"); cleanup() }
      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(TAG, "Gemini Live closed: code=$code reason=$reason")
        socket = null
        if (mutableState.value != GeminiLiveVoiceState.ERROR) mutableState.value = GeminiLiveVoiceState.DISCONNECTED
      }
    })
  }

  override fun startListening() {
    if (captureJob != null || socket == null || !setupComplete) return
    val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
    if (minBuffer <= 0) { mutableState.value = GeminiLiveVoiceState.ERROR; return }
    try {
      recorder = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_IN, ENCODING, minBuffer * 2)
      recorder?.startRecording()
      Log.i(TAG, "Microphone started: ${SAMPLE_RATE}Hz mono PCM16")
      captureJob = scope.launch {
        val buffer = ByteArray(minBuffer)
        while (true) {
          val read = recorder?.read(buffer, 0, buffer.size) ?: break
          if (read > 0) {
            val chunk = Base64.encodeToString(buffer.copyOf(read), Base64.NO_WRAP)
            val sent = socket?.send("{\"realtimeInput\":{\"audio\":{\"mimeType\":\"audio/pcm;rate=16000\",\"data\":\"$chunk\"}}}") == true
            if (sent) {
              sentChunkCount++
              if (sentChunkCount == 1 || sentChunkCount % 50 == 0) Log.i(TAG, "Microphone PCM chunks sent=$sentChunkCount bytes=$read")
            } else {
              Log.e(TAG, "Microphone PCM send failed")
              break
            }
          }
        }
      }
    } catch (_: Exception) { mutableState.value = GeminiLiveVoiceState.ERROR; cleanup() }
  }

  override fun stopListening() {
    captureJob?.cancel(); captureJob = null
    recorder?.runCatching { stop(); release() }; recorder = null
    if (socket != null) mutableState.value = GeminiLiveVoiceState.DISCONNECTED
  }

  override fun disconnect() {
    cleanup()
    socket?.close(1000, "client stopped")
    socket = null
    mutableState.value = GeminiLiveVoiceState.DISCONNECTED
  }

  private fun handleServerMessage(text: String) {
    try {
      val root = JSONObject(text)
      if (root.has("setupComplete")) {
        setupComplete = true
        mutableState.value = GeminiLiveVoiceState.LISTENING
        startListening()
        Log.i(TAG, "Gemini Live setup completed")
        return
      }
      root.optJSONObject("error")?.let { error ->
        Log.e(TAG, "Gemini Live API error: ${error.optString("message", "unknown error")}")
        mutableState.value = GeminiLiveVoiceState.ERROR
        return
      }
      val modelTurn = root.optJSONObject("serverContent")?.optJSONObject("modelTurn")
      val parts = modelTurn?.optJSONArray("parts")
      if (parts == null && !root.has("serverContent")) {
        Log.i(TAG, "Gemini Live server message received without audio content")
      }
      if (parts != null) for (index in 0 until parts.length()) {
        val inline = parts.optJSONObject(index)?.optJSONObject("inlineData") ?: continue
        val data = inline.optString("data")
        if (data.isNotEmpty()) {
          val bytes = Base64.decode(data, Base64.DEFAULT)
          receivedChunkCount++
          Log.i(TAG, "Model audio chunk received=$receivedChunkCount bytes=${bytes.size}")
          playPcm(bytes)
        }
      }
      if (root.optJSONObject("serverContent")?.optBoolean("turnComplete") == true) {
        Log.i(TAG, "Gemini Live turnComplete")
        mutableState.value = GeminiLiveVoiceState.LISTENING
      }
    } catch (error: Exception) {
      Log.e(TAG, "Gemini Live message parsing failed: ${error.message}")
      mutableState.value = GeminiLiveVoiceState.ERROR
    }
  }

  private fun playPcm(bytes: ByteArray) {
    try {
      mutableState.value = GeminiLiveVoiceState.SPEAKING
      if (player == null) {
        val size = AudioTrack.getMinBufferSize(OUTPUT_RATE, CHANNEL_OUT, ENCODING)
        player = AudioTrack.Builder().setAudioFormat(AudioFormat.Builder().setSampleRate(OUTPUT_RATE).setEncoding(ENCODING).setChannelMask(CHANNEL_OUT).build()).setBufferSizeInBytes(size * 2).build()
        player?.play()
      }
      val written = player?.write(bytes, 0, bytes.size) ?: -1
      if (written <= 0) throw IllegalStateException("AudioTrack wrote no audio")
    } catch (error: Exception) {
      Log.e(TAG, "Audio playback failed: ${error.message}")
      mutableState.value = GeminiLiveVoiceState.ERROR
    }
  }

  private fun cleanup() {
    setupComplete = false
    captureJob?.cancel(); captureJob = null
    recorder?.runCatching { stop(); release() }; recorder = null
    player?.runCatching { stop(); release() }; player = null
  }

  private fun setupMessage() = """{"setup":{"model":"models/$MODEL","responseModalities":["AUDIO"],"systemInstruction":{"parts":[{"text":"$SYSTEM_INSTRUCTION"}]}}}"""

  companion object {
    private const val TAG = "RideAwareGeminiLive"
    const val MODEL = "gemini-3.1-flash-live-preview"
    const val SAMPLE_RATE = 16_000
    const val OUTPUT_RATE = 24_000
    private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
    private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private const val LIVE_ENDPOINT = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    private const val SYSTEM_INSTRUCTION = "You are the RideAware voice assistant. Be concise and helpful. This is a motorcycle safety application. At this stage you do not receive live camera or hazard data, so do not claim to see the rider's surroundings."
  }
}