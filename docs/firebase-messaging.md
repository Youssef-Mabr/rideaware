# Firebase Cloud Messaging

The app creates the `rideaware_alerts` channel, shown as **RideAware Alerts**. The existing Notifications switch on the Permissions screen requests `POST_NOTIFICATIONS` on Android 13+, or opens Android notification settings when access must be managed there. Returning from settings refreshes the switch. Skipping setup never grants permission.

`RideAwareMessagingService` handles token refresh and incoming foreground messages. Notification payloads and data payloads with `title` / `body` are supported. A notification opens the app when tapped. Empty data messages do not produce blank notifications; disabled permissions/channels are respected.

After profile synchronization, the SDK token is saved to `users/{uid}` in `fcmToken`, with `fcmTokenUpdatedAt` as a server timestamp. Only these fields are updated. Tokens received before sign-in are cached and synchronized after the profile exists. Failures are logged and retried after profile synchronization or on the next launch. This single-token field represents the user's most recently synchronized device.

Log tag: `RideAwareMessaging`. Raw tokens and message bodies are not logged.

No sender, Cloud Function, or backend has been added. For an end-to-end remote test, use Firebase Console's Messaging test-message feature with the token from the user's Firestore document. Keep the app open to exercise the foreground handler, or background it to check FCM's default notification handling. Local notification tests do not verify delivery through FCM's network.

Verified on Samsung SM-M115F (Android 12): build and launch, real FCM token retrieval and Firestore synchronization, token timestamp refresh without changing `createdAt` or `accountType`, channel creation, foreground notification rendering with a local data payload, and Permissions screen state/skip behavior. Three phone tests passed. Two Robolectric checks passed for Android 12 and Android 13 notification permission handling. The Android 13 system dialog was not tested on physical hardware, and no remote test push was sent.
