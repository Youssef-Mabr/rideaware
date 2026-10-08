package com.example

import com.example.recording.RecordingClipMetadata
import com.example.recording.RecordingSession
import com.example.recording.TripRecordingSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripRecordingTest {
  @Test fun recordingSessionFinalizesClipMetadata() {
    val source = FakeRecordingSource()
    val session = source.start(1_000L)

    val clip = session.stop(6_000L)

    assertEquals("clip-test", clip.clipId)
    assertEquals("/data/user/0/ride-recordings/clip-test.mp4", clip.filePath)
    assertEquals(5, clip.durationSeconds)
    assertTrue(clip.sourceAsset.endsWith(".mp4"))
  }

  private class FakeRecordingSource : TripRecordingSource {
    override fun start(startedAtMillis: Long): RecordingSession {
      val initial = RecordingClipMetadata("clip-test", "/data/user/0/ride-recordings/clip-test.mp4", 30, startedAtMillis, "test.mp4")
      return object : RecordingSession {
        override val metadata = initial
        override fun stop(endedAtMillis: Long) = metadata.copy(durationSeconds = ((endedAtMillis - metadata.createdAtMillis) / 1000).toInt())
      }
    }
  }
}