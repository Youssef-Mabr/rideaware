package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.squareup.moshi.JsonReader
import okio.Buffer
import java.io.IOException
import java.util.concurrent.TimeUnit

interface GeminiAssistant {
  suspend fun ask(prompt: String): Result<String>
}

class GeminiAssistantImpl(
  private val apiKey: String = BuildConfig.GEMINI_API_KEY,
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build(),
  private val endpoint: String = "https://generativelanguage.googleapis.com/v1beta/interactions"
) : GeminiAssistant {
  override suspend fun ask(prompt: String): Result<String> = withContext(Dispatchers.IO) {
    if (prompt.isBlank()) return@withContext Result.failure(IllegalArgumentException("Enter a message first."))
    if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
      return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
    }
    val body = "{\"model\":\"${MODEL.jsonEscape()}\",\"input\":\"${prompt.trim().jsonEscape()}\"}"
      .toRequestBody(JSON_MEDIA_TYPE)
    val request = Request.Builder()
      .url("$endpoint?key=$apiKey")
      .post(body)
      .header("Accept", "application/json")
      .build()
    try {
      client.newCall(request).execute().use { response ->
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(IOException("Gemini request failed (${response.code})."))
        }
        GeminiResponseParser.parseText(responseBody)?.let { Result.success(it) }
          ?: Result.failure(IOException("Gemini returned an empty response."))
      }
    } catch (_: java.net.SocketTimeoutException) {
      Result.failure(IOException("Gemini request timed out. Check your connection and try again."))
    } catch (_: IOException) {
      Result.failure(IOException("Gemini is unavailable. Check your connection and try again."))
    }
  }

  companion object { const val MODEL = "gemini-3.5-flash-lite"; private val JSON_MEDIA_TYPE = "application/json".toMediaType() }
}

private fun String.jsonEscape() = buildString {
  for (character in this@jsonEscape) {
    when (character) {
      '\\' -> append("\\\\")
      '"' -> append("\\\"")
      '\n' -> append("\\n")
      '\r' -> append("\\r")
      '\t' -> append("\\t")
      else -> append(character)
    }
  }
}

object GeminiResponseParser {
  fun parseText(response: String): String? {
    return try {
      val reader = JsonReader.of(Buffer().writeUtf8(response))
      var result: String? = null
      reader.beginObject()
      while (reader.hasNext()) {
        when (reader.nextName()) {
          "outputs", "steps" -> readTextArray(reader) { text -> if (result == null) result = text }
          else -> reader.skipValue()
        }
      }
      reader.endObject()
      result
    } catch (_: Exception) {
      null
    }
  }

  private fun readTextArray(reader: JsonReader, onText: (String) -> Unit) {
    reader.beginArray()
    while (reader.hasNext()) {
      reader.beginObject()
      while (reader.hasNext()) {
        when (reader.nextName()) {
          "text" -> onText(reader.nextString().trim())
          "content" -> {
            reader.beginArray()
            while (reader.hasNext()) {
              reader.beginObject()
              while (reader.hasNext()) {
                if (reader.nextName() == "text") onText(reader.nextString().trim()) else reader.skipValue()
              }
              reader.endObject()
            }
            reader.endArray()
          }
          else -> reader.skipValue()
        }
      }
      reader.endObject()
    }
    reader.endArray()
  }
}