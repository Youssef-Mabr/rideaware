package com.example

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GeminiLiveProtocolDiagnosticTest {
  @Test fun setupAndTextProtocolWithoutAudio() {
    val key = BuildConfig.GEMINI_API_KEY
    assertTrue("Gemini API key is not configured", key.isNotBlank() && key != "YOUR_GEMINI_API_KEY")

    val finished = CountDownLatch(1)
    val setupComplete = CountDownLatch(1)
    val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    val endpoint = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$key"
    val request = Request.Builder().url(endpoint).build()
    var textSent = false
    var setupReceived = false
    var failureMessage: String? = null
    var closeCode: Int? = null
    var closeReason: String? = null
    val events = mutableListOf<String>()

    val socket = client.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.i(TAG, "onOpen http=${response.code}")
        events += "onOpen:${response.code}"
        webSocket.send("""{"setup":{"model":"models/gemini-3.8-live","generationConfig":{"responseModalities":["AUDIO"],"speechConfig":{"voiceConfig":{"prebuiltVoiceConfig":{"voiceName":"Puck"}}}}}}""")
        Log.i(TAG, "setupSent")
        events += "setupSent"
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        val root = runCatching { JSONObject(text) }.getOrNull()
        when {
          root?.has("setupComplete") == true -> {
            setupReceived = true
            Log.i(TAG, "setupComplete received keys=${root.keys().asSequence().toList()}")
            events += "setupComplete"
            setupComplete.countDown()
            if (!textSent) {
              textSent = true
              webSocket.send("""{"realtimeInput":{"text":"Hello RideAware, can you hear me?"}}""")
              Log.i(TAG, "textSent")
            }
          }
          root?.has("error") == true -> {
            val error = root.optJSONObject("error")
            failureMessage = error?.optString("message", "unknown")
            events += "serverError:${error?.optString("code", "unknown")}:$failureMessage"
            Log.e(TAG, "serverError code=${error?.optString("code", "unknown")} message=$failureMessage")
            finished.countDown()
          }
          else -> {
            val serverContent = root?.optJSONObject("serverContent")
            val modelTurn = serverContent?.optJSONObject("modelTurn")
            val parts = modelTurn?.optJSONArray("parts")
            var audioChunks = 0
            var base64Length = 0
            var mimeType: String? = null
            if (parts != null) for (index in 0 until parts.length()) {
              val inline = parts.optJSONObject(index)?.optJSONObject("inlineData") ?: continue
              val data = inline.optString("data")
              if (data.isNotEmpty()) { audioChunks++; base64Length += data.length }
              mimeType = inline.optString("mimeType", mimeType)
            }
            if (serverContent != null || modelTurn != null || audioChunks > 0) {
              Log.i(TAG, "serverContent=${serverContent != null} modelTurn=${modelTurn != null} audioChunks=$audioChunks base64Length=$base64Length mimeType=$mimeType turnComplete=${serverContent?.optBoolean("turnComplete", false)}")
              if (serverContent?.optBoolean("turnComplete", false) == true) finished.countDown()
            } else {
              Log.i(TAG, "serverMessage keys=${root?.keys()?.asSequence()?.toList() ?: emptyList<String>()}")
              events += "serverMessage:${root?.keys()?.asSequence()?.toList() ?: emptyList<String>()}"
            }
          }
        }
      }

      override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
        failureMessage = throwable.message
        events += "failure:${throwable.message}"
        Log.e(TAG, "webSocketFailure http=${response?.code} message=${throwable.message}")
        finished.countDown()
      }

      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        closeCode = code
        closeReason = reason
        events += "closing:$code:$reason"
        Log.i(TAG, "webSocketClosing code=$code reason=$reason")
        finished.countDown()
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        closeCode = code
        closeReason = reason
        events += "closed:$code:$reason"
        Log.i(TAG, "webSocketClosed code=$code reason=$reason")
        finished.countDown()
      }
    })

    try {
      assertTrue("setupComplete was not received; events=$events failure=$failureMessage close=$closeCode/$closeReason", setupComplete.await(90, TimeUnit.SECONDS))
      assertTrue("Live diagnostic did not finish; failure=$failureMessage close=$closeCode/$closeReason", finished.await(90, TimeUnit.SECONDS))
      assertTrue("Live setup was not observed", setupReceived)
    } finally {
      socket.close(1000, "diagnostic complete")
      client.dispatcher.executorService.shutdownNow()
    }
  }

  private companion object { const val TAG = "GeminiLiveProtocolDiag" }
}