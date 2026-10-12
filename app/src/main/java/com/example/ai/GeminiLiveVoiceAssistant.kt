package com.example.ai

import android.media.AudioAttributes
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import com.example.model.SafetyAlert
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

enum class GeminiLiveVoiceState { DISCONNECTED, CONNECTING, LISTENING, SPEAKING, ERROR }

data class LiveVoiceTurn(
  val id: Long,
  val userTranscript: String,
  val modelTranscript: String
)

interface GeminiLiveVoiceAssistant {
  val state: StateFlow<GeminiLiveVoiceState>
  val errorMessage: StateFlow<String?>
    get() = MutableStateFlow(null)
  val statusDetail: StateFlow<String?>
    get() = MutableStateFlow(null)
  val latestTurn: StateFlow<LiveVoiceTurn?>
    get() = MutableStateFlow(null)

  fun connect()
  fun startListening()
  fun stopListening()
  fun disconnect()
  fun finishUserTurn() {}
  fun announceSafetyAlert(alert: SafetyAlert, isSimulated: Boolean = true): Boolean = false
}

class GeminiLiveVoiceAssistantImpl(
  private val apiKey: String = BuildConfig.GEMINI_API_KEY,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .pingInterval(20, TimeUnit.SECONDS)
    .build(),
  private val contextProvider: RideAwareContextProvider = RideAwareContextStore.provider
) : GeminiLiveVoiceAssistant {
  private val mutableState = MutableStateFlow(GeminiLiveVoiceState.DISCONNECTED)
  override val state: StateFlow<GeminiLiveVoiceState> = mutableState.asStateFlow()

  private val mutableErrorMessage = MutableStateFlow<String?>(null)
  override val errorMessage: StateFlow<String?> = mutableErrorMessage.asStateFlow()

  private val mutableStatusDetail = MutableStateFlow<String?>(null)
  override val statusDetail: StateFlow<String?> = mutableStatusDetail.asStateFlow()

  private val mutableLatestTurn = MutableStateFlow<LiveVoiceTurn?>(null)
  override val latestTurn: StateFlow<LiveVoiceTurn?> = mutableLatestTurn.asStateFlow()

  private var socket: WebSocket? = null
  private var captureJob: Job? = null
  private var playbackJob: Job? = null
  private var contextSyncJob: Job? = null
  private var playbackChannel: Channel<PlaybackCommand>? = null
  private var recorder: AudioRecord? = null
  private var player: AudioTrack? = null
  private var currentOutputSampleRate = OUTPUT_RATE

  @Volatile private var setupComplete = false
  @Volatile private var userDisconnected = false
  @Volatile private var lastSpeakerActiveMs = 0L
  @Volatile private var speechDetectedInTurn = false
  @Volatile private var audioStreamEndSentForTurn = false
  @Volatile private var lastSpeechMs = 0L
  @Volatile private var lastSentContextSignature = ""
  @Volatile private var lastContextSentAtMs = 0L

  internal val sentClientMessages = mutableListOf<String>()
  private val announcedAlertTimestamps = mutableMapOf<String, Long>()

  private var setupAttemptIndex = 0
  private var sentChunkCount = 0
  private var receivedChunkCount = 0
  private var turnIdCounter = 0L
  private val currentInputTranscript = StringBuilder()
  private val currentOutputTranscript = StringBuilder()

  private sealed interface PlaybackCommand {
    data class PcmChunk(val bytes: ByteArray, val sampleRate: Int) : PlaybackCommand
    data object TurnFinished : PlaybackCommand
    data object FlushAndStop : PlaybackCommand
  }

  private data class SetupCandidate(
    val model: String,
    val includeTranscription: Boolean,
    val includeTools: Boolean = false
  )

  override fun connect() {
    if (state.value == GeminiLiveVoiceState.CONNECTING || socket != null) return
    val trimmedKey = apiKey.trim()
    if (trimmedKey.isBlank() || trimmedKey == "YOUR_GEMINI_API_KEY" || trimmedKey == "your_api_key_here") {
      mutableErrorMessage.value = "Gemini API key is not configured. Add GEMINI_API_KEY in the AI Studio Secrets panel."
      mutableStatusDetail.value = null
      mutableState.value = GeminiLiveVoiceState.ERROR
      return
    }
    userDisconnected = false
    setupAttemptIndex = 0
    mutableErrorMessage.value = null
    mutableStatusDetail.value = "Connecting to Geni Live..."
    openSocketWithCandidate(trimmedKey, SETUP_CANDIDATES[setupAttemptIndex])
  }

  private fun openSocketWithCandidate(trimmedKey: String, candidate: SetupCandidate) {
    setupComplete = false
    mutableState.value = GeminiLiveVoiceState.CONNECTING
    val request = Request.Builder().url("$LIVE_ENDPOINT?key=$trimmedKey").build()
    socket = client.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        val now = System.currentTimeMillis()
        val initialContext = contextProvider.buildPromptContext(now)
        lastSentContextSignature = contextProvider.contextSignature(now)
        lastContextSentAtMs = now
        val payload = setupMessage(
          model = candidate.model,
          includeTranscription = candidate.includeTranscription,
          includeTools = candidate.includeTools,
          safetyContextText = initialContext
        )
        webSocket.send(payload)
        Log.i(TAG, "Gemini Live WebSocket opened (model=${candidate.model}, transcription=${candidate.includeTranscription}, tools=${candidate.includeTools}); waiting for setupComplete")
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        handleServerMessage(text)
      }

      override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        handleServerMessage(bytes.utf8())
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "Gemini Live WebSocket failed: ${t.message}", t)
        if (!userDisconnected && !setupComplete && tryNextSetupCandidate(trimmedKey)) {
          return
        }
        cleanup()
        socket = null
        mutableErrorMessage.value = "Live voice connection failed: ${t.localizedMessage ?: "Check your internet connection."}"
        mutableStatusDetail.value = null
        mutableState.value = GeminiLiveVoiceState.ERROR
      }

      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(TAG, "Gemini Live closing: code=$code reason=$reason")
        if (!userDisconnected && !setupComplete && code != 1000 && tryNextSetupCandidate(trimmedKey)) {
          webSocket.close(1000, "retrying with fallback model")
          return
        }
        cleanup()
        if (code != 1000 && !userDisconnected) {
          mutableErrorMessage.value = reason.ifBlank { "Live voice session closed (code $code)." }
          mutableStatusDetail.value = null
          mutableState.value = GeminiLiveVoiceState.ERROR
        }
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(TAG, "Gemini Live closed: code=$code reason=$reason")
        if (socket === webSocket) {
          socket = null
        }
        if (mutableState.value != GeminiLiveVoiceState.ERROR && ! (!userDisconnected && !setupComplete)) {
          mutableStatusDetail.value = null
          mutableState.value = GeminiLiveVoiceState.DISCONNECTED
        }
      }
    })
  }

  private fun tryNextSetupCandidate(trimmedKey: String): Boolean {
    val nextIndex = setupAttemptIndex + 1
    if (nextIndex >= SETUP_CANDIDATES.size) return false
    setupAttemptIndex = nextIndex
    val nextCandidate = SETUP_CANDIDATES[nextIndex]
    Log.w(TAG, "Retrying Gemini Live setup with model=${nextCandidate.model} transcription=${nextCandidate.includeTranscription}")
    socket?. runCatching { cancel() }
    socket = null
    openSocketWithCandidate(trimmedKey, nextCandidate)
    return true
  }

  override fun startListening() {
    if (captureJob != null || socket == null || !setupComplete) return
    val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
    if (minBuffer <= 0) {
      mutableErrorMessage.value = "Microphone initialization failed (invalid buffer size)."
      mutableState.value = GeminiLiveVoiceState.ERROR
      return
    }
    try {
      val bufferSizeInBytes = maxOf(minBuffer * 2, CHUNK_BYTES * 2)
      val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        SAMPLE_RATE,
        CHANNEL_IN,
        ENCODING,
        bufferSizeInBytes
      )
      if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
        audioRecord.release()
        mutableErrorMessage.value = "Microphone is unavailable or in use by another app."
        mutableState.value = GeminiLiveVoiceState.ERROR
        return
      }
      recorder = audioRecord
      audioRecord.startRecording()
      speechDetectedInTurn = false
      audioStreamEndSentForTurn = false
      lastSpeechMs = System.currentTimeMillis()
      Log.i(TAG, "Microphone started: ${SAMPLE_RATE}Hz mono PCM16")

      captureJob = scope.launch {
        val buffer = ByteArray(CHUNK_BYTES)
        var noiseFloorRms = 180.0
        var consecutiveSpeechFrames = 0
        while (isActive) {
          val read = recorder?.read(buffer, 0, buffer.size) ?: break
          if (read <= 0) continue

          val now = System.currentTimeMillis()
          // Avoid feeding speaker output back into the microphone while Geni is speaking.
          if (mutableState.value == GeminiLiveVoiceState.SPEAKING || (now - lastSpeakerActiveMs) < SPEAKER_ECHO_GUARD_MS) {
            consecutiveSpeechFrames = 0
            continue
          }

          val rms = calculatePcm16Rms(buffer, read)
          val speechThreshold = maxOf(noiseFloorRms * 2.2, MIN_SPEECH_RMS)
          if (rms > speechThreshold) {
            consecutiveSpeechFrames++
            if (consecutiveSpeechFrames >= 2) {
              if (!speechDetectedInTurn) {
                speechDetectedInTurn = true
                audioStreamEndSentForTurn = false
                if (currentInputTranscript.isEmpty()) {
                  mutableStatusDetail.value = "Hearing you..."
                }
              }
              lastSpeechMs = now
            }
          } else {
            consecutiveSpeechFrames = 0
            noiseFloorRms = (noiseFloorRms * 0.95) + (rms * 0.05)
          }

          val payloadBytes = if (read == buffer.size) buffer else buffer.copyOf(read)
          val chunk = Base64.encodeToString(payloadBytes, Base64.NO_WRAP)
          val sent = socket?.send(
            "{\"realtimeInput\":{\"audio\":{\"mimeType\":\"audio/pcm;rate=16000\",\"data\":\"$chunk\"}}}"
          ) == true
          if (sent) {
            sentChunkCount++
            if (sentChunkCount == 1 || sentChunkCount % 50 == 0) {
              Log.i(TAG, "Microphone PCM chunks sent=$sentChunkCount bytes=$read rms=${rms.toInt()}")
            }
          } else {
            Log.e(TAG, "Microphone PCM send failed")
            break
          }

          // Hybrid VAD: when user has spoken and then paused for SILENCE_AFTER_SPEECH_MS,
          // signal audioStreamEnd so Gemini Live responds promptly even in noisy environments.
          if (speechDetectedInTurn && !audioStreamEndSentForTurn && (now - lastSpeechMs) >= SILENCE_AFTER_SPEECH_MS) {
            sendAudioStreamEnd()
          }
        }
      }
    } catch (error: SecurityException) {
      Log.e(TAG, "Microphone permission missing: ${error.message}")
      mutableErrorMessage.value = "Microphone permission is required for live voice."
      mutableState.value = GeminiLiveVoiceState.ERROR
      cleanup()
    } catch (error: Exception) {
      Log.e(TAG, "Microphone capture failed: ${error.message}", error)
      mutableErrorMessage.value = "Could not access microphone: ${error.localizedMessage ?: "unknown error"}"
      mutableState.value = GeminiLiveVoiceState.ERROR
      cleanup()
    }
  }

  override fun finishUserTurn() {
    if (!setupComplete || socket == null) return
    sendAudioStreamEnd()
  }

  override fun announceSafetyAlert(alert: SafetyAlert, isSimulated: Boolean): Boolean {
    val now = System.currentTimeMillis()
    contextProvider.recordActiveAlert(alert, nowMillis = now, isSimulated = isSimulated)
    val dedupKey = "${alert.hazardType}:${alert.direction}:${alert.title}"
    synchronized(announcedAlertTimestamps) {
      val previous = announcedAlertTimestamps[dedupKey]
      if (previous != null && (now - previous) < ALERT_ANNOUNCEMENT_COOLDOWN_MS) {
        return false
      }
      announcedAlertTimestamps[dedupKey] = now
    }
    if (setupComplete && mutableState.value == GeminiLiveVoiceState.LISTENING && !speechDetectedInTurn) {
      pushLatestContextIfChanged(force = true, nowMillis = now)
    }
    return true
  }

  internal fun pushLatestContextIfChanged(
    force: Boolean = false,
    nowMillis: Long = System.currentTimeMillis()
  ): Boolean {
    if (!setupComplete) return false
    val sig = contextProvider.contextSignature(nowMillis)
    if (!force && sig == lastSentContextSignature) return false
    if (!force && lastContextSentAtMs > 0L && (nowMillis - lastContextSentAtMs) < MIN_CONTEXT_UPDATE_INTERVAL_MS) {
      return false
    }
    val payload = buildContextUpdateMessage(contextProvider.buildPromptContext(nowMillis), turnComplete = false)
    val sent = sendTrackedMessage(payload)
    if (sent) {
      lastSentContextSignature = sig
      lastContextSentAtMs = nowMillis
    }
    return sent
  }

  private fun sendTrackedMessage(payload: String): Boolean {
    synchronized(sentClientMessages) {
      sentClientMessages.add(payload)
    }
    val ws = socket ?: return true
    return ws.send(payload)
  }

  private fun sendAudioStreamEnd() {
    val ws = socket ?: return
    // Ensure any pending RideAware safety context changes are pushed before ending the user audio turn.
    val now = System.currentTimeMillis()
    if (contextProvider.contextSignature(now) != lastSentContextSignature) {
      pushLatestContextIfChanged(force = true, nowMillis = now)
    }
    audioStreamEndSentForTurn = true
    speechDetectedInTurn = false
    val sent = ws.send("{\"realtimeInput\":{\"audioStreamEnd\":true}}")
    Log.i(TAG, "Sent audioStreamEnd (sent=$sent)")
    if (mutableState.value == GeminiLiveVoiceState.LISTENING) {
      mutableStatusDetail.value = "Thinking..."
    }
  }

  override fun stopListening() {
    captureJob?.cancel()
    captureJob = null
    recorder?.runCatching { stop(); release() }
    recorder = null
    if (socket != null) mutableState.value = GeminiLiveVoiceState.DISCONNECTED
  }

  override fun disconnect() {
    userDisconnected = true
    cleanup()
    socket?.close(1000, "client stopped")
    socket = null
    mutableStatusDetail.value = null
    mutableState.value = GeminiLiveVoiceState.DISCONNECTED
  }

  internal fun handleServerMessage(text: String) {
    try {
      val root = JSONObject(text)
      if (root.has("setupComplete")) {
        setupComplete = true
        mutableErrorMessage.value = null
        mutableStatusDetail.value = "Listening — speak now"
        mutableState.value = GeminiLiveVoiceState.LISTENING
        startPlaybackWorker()
        startContextSyncWorker()
        startListening()
        Log.i(TAG, "Gemini Live setup completed")
        return
      }
      root.optJSONObject("error")?.let { error ->
        val msg = error.optString("message", "Live voice server error")
        Log.e(TAG, "Gemini Live API error: $msg")
        mutableErrorMessage.value = msg
        mutableStatusDetail.value = null
        mutableState.value = GeminiLiveVoiceState.ERROR
        return
      }
      root.optJSONObject("toolCall")?.let { toolCall ->
        handleToolCall(toolCall)
        return
      }

      val serverContent = root.optJSONObject("serverContent")
      if (serverContent == null) {
        Log.i(TAG, "Gemini Live non-content message received")
        return
      }

      if (serverContent.optBoolean("interrupted", false)) {
        Log.i(TAG, "Gemini Live interrupted by user")
        playbackChannel?.trySend(PlaybackCommand.FlushAndStop)
        mutableStatusDetail.value = "Listening — speak now"
        mutableState.value = GeminiLiveVoiceState.LISTENING
        return
      }

      serverContent.optJSONObject("inputTranscription")?.optString("text")?.takeIf { it.isNotEmpty() }?.let { chunk ->
        currentInputTranscript.append(chunk)
        mutableStatusDetail.value = "You: \"${currentInputTranscript.toString().trim()}\""
      }

      serverContent.optJSONObject("outputTranscription")?.optString("text")?.takeIf { it.isNotEmpty() }?.let { chunk ->
        currentOutputTranscript.append(chunk)
        mutableStatusDetail.value = "Geni: \"${currentOutputTranscript.toString().trim()}\""
      }

      val modelTurn = serverContent.optJSONObject("modelTurn")
      val parts = modelTurn?.optJSONArray("parts")
      if (parts != null) {
        for (index in 0 until parts.length()) {
          val part = parts.optJSONObject(index) ?: continue
          if (!part.optBoolean("thought", false)) {
            val partText = part.optString("text")
            if (partText.isNotEmpty() && !serverContent.has("outputTranscription")) {
              currentOutputTranscript.append(partText)
            }
          }
          val inline = part.optJSONObject("inlineData") ?: continue
          val data = inline.optString("data")
          val mimeType = inline.optString("mimeType", "audio/pcm;rate=24000")
          if (data.isNotEmpty()) {
            val bytes = Base64.decode(data, Base64.DEFAULT)
            val sampleRate = parseSampleRate(mimeType)
            receivedChunkCount++
            if (receivedChunkCount == 1 || receivedChunkCount % 25 == 0) {
              Log.i(TAG, "Model audio chunk received=$receivedChunkCount bytes=${bytes.size} rate=$sampleRate")
            }
            enqueueAudioChunk(bytes, sampleRate)
          }
        }
      }

      if (serverContent.optBoolean("turnComplete", false)) {
        Log.i(TAG, "Gemini Live turnComplete")
        val userText = currentInputTranscript.toString().trim()
        val modelText = currentOutputTranscript.toString().trim()
        if (userText.isNotEmpty() || modelText.isNotEmpty() || receivedChunkCount > 0) {
          turnIdCounter++
          mutableLatestTurn.value = LiveVoiceTurn(
            id = turnIdCounter,
            userTranscript = userText.ifEmpty { "🎤 Voice prompt" },
            modelTranscript = modelText.ifEmpty { "🔊 Spoken voice response" }
          )
        }
        currentInputTranscript.clear()
        currentOutputTranscript.clear()
        speechDetectedInTurn = false
        audioStreamEndSentForTurn = false
        val ch = playbackChannel
        if (ch != null) {
          ch.trySend(PlaybackCommand.TurnFinished)
        } else {
          mutableStatusDetail.value = "Listening — speak now"
          mutableState.value = GeminiLiveVoiceState.LISTENING
        }
      }
    } catch (error: Exception) {
      Log.e(TAG, "Gemini Live message parsing failed: ${error.message}", error)
      mutableErrorMessage.value = "Voice stream error: ${error.localizedMessage ?: "parse failure"}"
      mutableState.value = GeminiLiveVoiceState.ERROR
    }
  }

  private fun startPlaybackWorker() {
    playbackJob?.cancel()
    playbackChannel?.close()
    val channel = Channel<PlaybackCommand>(Channel.UNLIMITED)
    playbackChannel = channel
    playbackJob = scope.launch {
      for (cmd in channel) {
        when (cmd) {
          is PlaybackCommand.PcmChunk -> {
            writePcmBlocking(cmd.bytes, cmd.sampleRate)
          }
          is PlaybackCommand.TurnFinished -> {
            flushAudioTrackTail()
            lastSpeakerActiveMs = System.currentTimeMillis()
            delay(SPEAKER_ECHO_GUARD_MS)
            if (setupComplete && !userDisconnected && mutableState.value != GeminiLiveVoiceState.ERROR) {
              mutableStatusDetail.value = "Listening — speak now"
              mutableState.value = GeminiLiveVoiceState.LISTENING
            }
          }
          is PlaybackCommand.FlushAndStop -> {
            player?.runCatching {
              pause()
              flush()
              play()
            }
          }
        }
      }
    }
  }

  private fun enqueueAudioChunk(bytes: ByteArray, sampleRate: Int) {
    mutableState.value = GeminiLiveVoiceState.SPEAKING
    if (currentOutputTranscript.isEmpty()) {
      mutableStatusDetail.value = "Geni is speaking..."
    }
    lastSpeakerActiveMs = System.currentTimeMillis()
    val ch = playbackChannel
    if (ch != null) {
      ch.trySend(PlaybackCommand.PcmChunk(bytes, sampleRate))
    } else {
      writePcmBlocking(bytes, sampleRate)
    }
  }

  private fun writePcmBlocking(bytes: ByteArray, sampleRate: Int) {
    try {
      mutableState.value = GeminiLiveVoiceState.SPEAKING
      lastSpeakerActiveMs = System.currentTimeMillis()
      if (player == null || currentOutputSampleRate != sampleRate) {
        player?.runCatching { stop(); release() }
        currentOutputSampleRate = sampleRate
        val minSize = AudioTrack.getMinBufferSize(sampleRate, CHANNEL_OUT, ENCODING).coerceAtLeast(4096)
        val bufferSize = maxOf(minSize * 2, sampleRate) // ~0.5s buffer at 16-bit mono
        player = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setSampleRate(sampleRate)
              .setEncoding(ENCODING)
              .setChannelMask(CHANNEL_OUT)
              .build()
          )
          .setBufferSizeInBytes(bufferSize)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
        player?.play()
      } else if (player?.playState != AudioTrack.PLAYSTATE_PLAYING) {
        player?.play()
      }
      val written = player?.write(bytes, 0, bytes.size, AudioTrack.WRITE_BLOCKING) ?: -1
      lastSpeakerActiveMs = System.currentTimeMillis()
      if (written < 0) {
        throw IllegalStateException("AudioTrack write error ($written)")
      }
    } catch (error: Exception) {
      Log.e(TAG, "Audio playback failed: ${error.message}", error)
      mutableErrorMessage.value = "Speaker playback error: ${error.localizedMessage ?: "audio output failed"}"
      mutableState.value = GeminiLiveVoiceState.ERROR
    }
  }

  private fun flushAudioTrackTail() {
    val activePlayer = player ?: return
    runCatching {
      val minSize = AudioTrack.getMinBufferSize(currentOutputSampleRate, CHANNEL_OUT, ENCODING).coerceAtLeast(2048)
      val silencePadding = ByteArray(minSize)
      activePlayer.write(silencePadding, 0, silencePadding.size, AudioTrack.WRITE_BLOCKING)
    }
  }

  private fun handleToolCall(toolCall: JSONObject) {
    val functionCalls = toolCall.optJSONArray("functionCalls") ?: return
    val contextText = contextProvider.buildPromptContext()
    val responses = JSONArray()
    for (i in 0 until functionCalls.length()) {
      val call = functionCalls.optJSONObject(i) ?: continue
      val callId = call.optString("id", "call-$i")
      val name = call.optString("name", SAFETY_CONTEXT_TOOL_NAME)
      responses.put(
        JSONObject()
          .put("id", callId)
          .put("name", name)
          .put(
            "response",
            JSONObject().put(
              "result",
              JSONObject().put("safety_context", contextText)
            )
          )
      )
    }
    if (responses.length() > 0) {
      val payload = JSONObject()
        .put("toolResponse", JSONObject().put("functionResponses", responses))
        .toString()
      sendTrackedMessage(payload)
      lastSentContextSignature = contextProvider.contextSignature()
      lastContextSentAtMs = System.currentTimeMillis()
      Log.i(TAG, "Responded to Gemini Live toolCall (${responses.length()} function call(s)) with latest RideAware context")
    }
  }

  private fun startContextSyncWorker() {
    contextSyncJob?.cancel()
    contextSyncJob = scope.launch {
      contextProvider.updates.collect {
        if (setupComplete && mutableState.value == GeminiLiveVoiceState.LISTENING && !speechDetectedInTurn) {
          pushLatestContextIfChanged(force = false)
        }
      }
    }
  }

  private fun cleanup() {
    setupComplete = false
    captureJob?.cancel()
    captureJob = null
    contextSyncJob?.cancel()
    contextSyncJob = null
    playbackJob?.cancel()
    playbackJob = null
    playbackChannel?.close()
    playbackChannel = null
    recorder?.runCatching { stop(); release() }
    recorder = null
    player?.runCatching { stop(); release() }
    player = null
    currentInputTranscript.clear()
    currentOutputTranscript.clear()
  }

  companion object {
    private const val TAG = "RideAwareGeminiLive"
    const val MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"
    const val SAFETY_CONTEXT_TOOL_NAME = "get_rideaware_safety_context"
    const val SAMPLE_RATE = 16_000
    const val OUTPUT_RATE = 24_000
    private const val CHUNK_BYTES = 1600 // 50ms at 16kHz 16-bit mono
    private const val MIN_SPEECH_RMS = 320.0
    private const val SILENCE_AFTER_SPEECH_MS = 1000L
    private const val SPEAKER_ECHO_GUARD_MS = 300L
    private const val MIN_CONTEXT_UPDATE_INTERVAL_MS = 1_500L
    private const val ALERT_ANNOUNCEMENT_COOLDOWN_MS = 8_000L
    private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
    private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private const val LIVE_ENDPOINT =
      "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    private const val BASE_SYSTEM_INSTRUCTION =
      "You are Geni, the RideAware motorcycle safety voice assistant. Answer riders' questions concisely (1 to 2 short sentences) using ONLY the verified RideAware safety context provided in this session or returned by get_rideaware_safety_context. Existing hazard detection and SafetyAlert severity are authoritative; never lower or override them. If rear-camera data is UNAVAILABLE or STALE when asked what is behind the rider, clearly say you cannot currently confirm what is behind them. Clearly distinguish SIMULATED/TEST data from real camera data, and never invent distances, speeds, or unmeasured observations."

    private val SETUP_CANDIDATES = listOf(
      SetupCandidate(model = MODEL, includeTranscription = true, includeTools = true),
      SetupCandidate(model = MODEL, includeTranscription = true, includeTools = false),
      SetupCandidate(model = MODEL, includeTranscription = false, includeTools = false),
      SetupCandidate(model = "gemini-3.1-flash-live-preview", includeTranscription = false, includeTools = false),
      SetupCandidate(model = "gemini-3.8-live", includeTranscription = false, includeTools = false)
    )

    internal fun buildSystemInstruction(
      safetyContextText: String = RideAwareContextStore.provider.buildPromptContext()
    ): String {
      return "$BASE_SYSTEM_INSTRUCTION\n\n$safetyContextText"
    }

    internal fun setupMessage(
      model: String = MODEL,
      includeTranscription: Boolean = true,
      includeTools: Boolean = true,
      safetyContextText: String = RideAwareContextStore.provider.buildPromptContext()
    ): String {
      val setupObj = JSONObject()
        .put("model", "models/$model")
        .put(
          "generationConfig",
          JSONObject()
            .put("responseModalities", JSONArray().put("AUDIO"))
            .put(
              "speechConfig",
              JSONObject().put(
                "voiceConfig",
                JSONObject().put(
                  "prebuiltVoiceConfig",
                  JSONObject().put("voiceName", "Puck")
                )
              )
            )
        )
        .put(
          "systemInstruction",
          JSONObject().put(
            "parts",
            JSONArray().put(
              JSONObject().put("text", buildSystemInstruction(safetyContextText))
            )
          )
        )
      if (includeTools) {
        val toolDecl = JSONObject()
          .put("name", SAFETY_CONTEXT_TOOL_NAME)
          .put(
            "description",
            "Returns the latest verified RideAware front and rear camera detections, tracked object directions, hazard types/severities, and active safety alerts."
          )
        setupObj.put(
          "tools",
          JSONArray().put(
            JSONObject().put("functionDeclarations", JSONArray().put(toolDecl))
          )
        )
      }
      if (includeTranscription) {
        setupObj.put("inputAudioTranscription", JSONObject())
        setupObj.put("outputAudioTranscription", JSONObject())
      }
      return JSONObject().put("setup", setupObj).toString()
    }

    internal fun buildContextUpdateMessage(
      contextText: String,
      turnComplete: Boolean = false
    ): String {
      val part = JSONObject().put("text", "[RIDEAWARE_CONTEXT_UPDATE]\n$contextText")
      val turn = JSONObject()
        .put("role", "user")
        .put("parts", JSONArray().put(part))
      val clientContent = JSONObject()
        .put("turns", JSONArray().put(turn))
        .put("turnComplete", turnComplete)
      return JSONObject().put("clientContent", clientContent).toString()
    }

    internal fun parseSampleRate(mimeType: String): Int {
      val rateMatch = Regex("rate=(\\d+)").find(mimeType)
      return rateMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: OUTPUT_RATE
    }

    internal fun calculatePcm16Rms(buffer: ByteArray, length: Int): Double {
      val sampleCount = length / 2
      if (sampleCount <= 0) return 0.0
      var sumSquares = 0.0
      var i = 0
      while (i + 1 < length) {
        val low = buffer[i].toInt() and 0xFF
        val high = buffer[i + 1].toInt()
        val sample = (high shl 8) or low
        val s = sample.toDouble()
        sumSquares += s * s
        i += 2
      }
      return sqrt(sumSquares / sampleCount)
    }
  }
}
