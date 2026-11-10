package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MockDataProvider
import com.example.model.preRideChecks
import com.example.recording.BundledTestVideoSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StartRideJourneyTest {

  @Test
  fun homeScreenReadinessAndPreRideChecksAreReadyByDefault() {
    val helmet = MockDataProvider.defaultHelmetStatus
    val contact = MockDataProvider.defaultEmergencyContact

    // Verify helmet readiness
    assertTrue("Helmet must be connected by default", helmet.isConnected)
    assertTrue("Front camera ready", helmet.frontCameraReady)
    assertTrue("Rear camera ready", helmet.rearCameraReady)
    assertTrue("Audio ready", helmet.audioReady)
    assertTrue("Battery > 20%", helmet.batteryPercent > 20)
    assertTrue("Storage > 5GB", helmet.storageFreeGb > 5)

    // Verify all pre-ride checks are ready
    val checks = preRideChecks(helmet, contact)
    assertTrue("All pre-ride checks must pass", checks.all { it.ready })
    assertEquals(8, checks.size)
  }

  @Test
  fun startRideSessionInitializesSafelyWithoutCrashingWhenAssetsMissing() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val recordingSource = BundledTestVideoSource(context)

    // Must not throw FileNotFoundException or crash
    val session = recordingSource.start(System.currentTimeMillis())
    assertNotNull(session)
    assertNotNull(session.metadata)
    assertTrue(session.metadata.clipId.startsWith("clip-"))

    val stoppedMetadata = session.stop(System.currentTimeMillis() + 10_000L)
    assertNotNull(stoppedMetadata)
    assertTrue(stoppedMetadata.durationSeconds >= 10)
  }
}
