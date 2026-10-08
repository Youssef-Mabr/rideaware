package com.example.recording

import android.content.Context
import java.io.File
import java.util.UUID

data class RecordingClipMetadata(
  val clipId: String,
  val filePath: String,
  val durationSeconds: Int,
  val createdAtMillis: Long,
  val sourceAsset: String,
  val rideId: String? = null
)

interface TripRecordingSource {
  fun start(startedAtMillis: Long = System.currentTimeMillis()): RecordingSession
}

interface RecordingSession {
  val metadata: RecordingClipMetadata
  fun stop(endedAtMillis: Long = System.currentTimeMillis()): RecordingClipMetadata
}

class BundledTestVideoSource(
  private val context: Context,
  private val assetName: String = DEFAULT_ASSET_NAME,
  private val durationSeconds: Int = DEFAULT_DURATION_SECONDS
) : TripRecordingSource {
  override fun start(startedAtMillis: Long): RecordingSession {
    val clipId = "clip-${UUID.randomUUID()}"
    val output = File(File(context.filesDir, "ride-recordings"), "$clipId.mp4")
    output.parentFile?.mkdirs()
    context.assets.open(assetName).use { input -> output.outputStream().use { input.copyTo(it) } }
    return BundledRecordingSession(
      RecordingClipMetadata(clipId, output.absolutePath, durationSeconds, startedAtMillis, assetName)
    )
  }

  private class BundledRecordingSession(
    override val metadata: RecordingClipMetadata
  ) : RecordingSession {
    override fun stop(endedAtMillis: Long): RecordingClipMetadata = metadata.copy(
      durationSeconds = metadata.durationSeconds.coerceAtLeast(((endedAtMillis - metadata.createdAtMillis) / 1000L).toInt())
    )
  }

  private companion object {
    const val DEFAULT_ASSET_NAME = "Town Street Rhythm _ Philippines Motorcycle POV #Shorts.mp4"
    const val DEFAULT_DURATION_SECONDS = 30
  }
}