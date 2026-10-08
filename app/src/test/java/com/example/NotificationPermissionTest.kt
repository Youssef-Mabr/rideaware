package com.example

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.notifications.RideAwareNotifications
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class NotificationPermissionTest {
  @Test fun android13RequiresGrantBeforeDisplayingNotifications() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
    assertTrue(RideAwareNotifications.needsRuntimePermission(context))
    assertFalse(RideAwareNotifications.areEnabled(context))
    shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    assertFalse(RideAwareNotifications.needsRuntimePermission(context))
  }

  @Test @Config(sdk = [31]) fun android12DoesNotRequestAndroid13Permission() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    assertFalse(RideAwareNotifications.needsRuntimePermission(context))
  }
}
