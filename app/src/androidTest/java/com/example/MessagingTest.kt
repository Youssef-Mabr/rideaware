package com.example

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notifications.FcmTokenRepository
import com.example.notifications.RideAwareNotifications
import com.example.ui.screens.PermissionScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class MessagingTest {
  @get:Rule val compose = createComposeRule()
  private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

  @Test fun foregroundDataMessageUsesAlertsChannelAndOpensApp() {
    compose.setContent { MyApplicationTheme { PermissionScreen({}, {}, {}) } }
    assumeTrue(RideAwareNotifications.areEnabled(context))
    val manager = context.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= 26) {
      assertEquals("RideAware Alerts", manager.getNotificationChannel(RideAwareNotifications.CHANNEL_ID).name.toString())
    }
    val tag = "rideaware-local-notification-test"
    val message = RemoteMessage.Builder("local-test")
      .setMessageId(tag).addData("title", "RideAware notification check")
      .addData("body", "Local notification handler test").build()
    try {
      RideAwareNotifications.showMessage(context, message)
      var posted = manager.activeNotifications.firstOrNull { it.tag == tag }
      repeat(20) {
        if (posted == null) {
          Thread.sleep(100)
          posted = manager.activeNotifications.firstOrNull { it.tag == tag }
        }
      }
      assertNotNull("Foreground message must create a notification", posted)
      val notification = posted!!.notification
      assertEquals("RideAware notification check", notification.extras.getString(Notification.EXTRA_TITLE))
      assertEquals("Local notification handler test", notification.extras.getString(Notification.EXTRA_TEXT))
      if (Build.VERSION.SDK_INT >= 26) assertEquals(RideAwareNotifications.CHANNEL_ID, notification.channelId)
      assertNotNull(notification.contentIntent)
      assertEquals(context.packageName, notification.contentIntent.creatorPackage)
    } finally { manager.cancel(tag, 0) }
  }

  @Test fun tokenIsStoredForAuthenticatedUserWithoutOverwritingProfile() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Phone must be signed in")
    val token = Tasks.await(FirebaseMessaging.getInstance().token, 60, TimeUnit.SECONDS)
    assertTrue(token.isNotBlank())
    val document = FirebaseFirestore.getInstance().collection("users").document(uid)
    val before = Tasks.await(document.get(Source.SERVER), 30, TimeUnit.SECONDS)
    assertNotNull(before.getTimestamp("createdAt"))
    // Exercise the refresh path with the real SDK token, never a fabricated token.
    FcmTokenRepository.onNewToken(context, token)
    var after = Tasks.await(document.get(Source.SERVER), 30, TimeUnit.SECONDS)
    repeat(20) {
      if (after.getString("fcmToken") != token || after.getTimestamp("fcmTokenUpdatedAt") == null || after.getTimestamp("fcmTokenUpdatedAt") == before.getTimestamp("fcmTokenUpdatedAt")) {
        Thread.sleep(500)
        after = Tasks.await(document.get(Source.SERVER), 30, TimeUnit.SECONDS)
      }
    }
    assertEquals(token, after.getString("fcmToken"))
    assertNotNull(after.getTimestamp("fcmTokenUpdatedAt"))
    assertNotEquals(before.getTimestamp("fcmTokenUpdatedAt"), after.getTimestamp("fcmTokenUpdatedAt"))
    assertEquals(before.getTimestamp("createdAt"), after.getTimestamp("createdAt"))
    assertEquals(before.getString("accountType"), after.getString("accountType"))
  }

  @Test fun permissionScreenShowsSystemStateAndSkipDoesNotGrantAccess() {
    val enabledBefore = RideAwareNotifications.areEnabled(context)
    var skipped = false
    compose.setContent { MyApplicationTheme { PermissionScreen({}, { skipped = true }, {}) } }
    val toggle = compose.onNodeWithTag("switch_notifications").performScrollTo()
    if (enabledBefore) toggle.assertIsOn() else toggle.assertIsOff()
    compose.onNodeWithTag("continue_limited_button").performClick()
    compose.runOnIdle {
      assertTrue(skipped)
      assertEquals(enabledBefore, RideAwareNotifications.areEnabled(context))
    }
  }
}
