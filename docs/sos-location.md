# SOS location

The in-app SOS button requests foreground coarse/fine location permission and starts the existing 10-second countdown. Permission approval is optional and does not delay the countdown. The Permissions screen's existing Location switch uses the same Android permissions; Android settings handle revocation or permanent denial.

After the countdown, `SosViewModel` calls `PhoneSosLocationProvider.capture()` and passes its result to `SosRepository`. Android network/GPS providers race for the first usable fix, with an 8-second overall timeout. Requests are cancelled after a fix, timeout, or coroutine cancellation. A fix must be at most 15 seconds old, have valid coordinates and accuracy, and include a capture timestamp. Approximate permission is supported through the network provider. No background location or continuous tracking is requested.

Each new `users/{uid}/sosEvents/{eventId}` retains `status`, `source`, `contactId`, and server-generated `createdAt`, and adds:

```text
locationStatus: available | permission_denied | disabled | timeout | unavailable
location:                      // present only when available
  latitude: number             // degrees, -90..90
  longitude: number            // degrees, -180..180
  accuracy: number             // horizontal accuracy in meters
  capturedAt: Timestamp        // phone-reported fix time, not event server time
```

No coordinates are included for fallback statuses. Location failures do not prevent event creation; the existing authentication, emergency-contact, and Firestore-write requirements still apply. The existing SOS screen and states are unchanged.

Rules require owner authentication, the owner's saved contact, server-generated event `createdAt`, valid location status and field types/ranges, and no unexpected fields. SOS events remain immutable. Historical events without location remain readable.

## Device tests

`SosLocationDeviceTest` runs a real 10-second countdown, captures via the Android provider, writes through the production repository to a temporary anonymous user's Firestore account, reads the server result, and removes its test data/account. Instrumentation argument `sosLocationCase` selects:

- `denied`: revoke both location permissions before the test; expects `permission_denied` and a saved event without coordinates.
- `disabled`: grant foreground permission but turn off device Location; expects `disabled` and a saved event.
- `granted`: grant permissions, turn on Location, and enable the test app's mock-location app-op; injects a controlled fix through Android test providers and verifies all four stored fields.
- `timeout`: same permissions/test-provider setup, with no fixes injected; verifies the 8-second timeout still saves an event.
- `live`: foreground permission and Location enabled, with no mock provider; attempts a real fix and verifies the recorded result, including timeout/unavailability indoors.

Restore device Location, runtime permissions, and mock-location app-op after testing. Controlled provider coordinates are test data, not evidence of a real GPS fix. The Firestore rules suite also tests malformed coordinates/statuses and cross-user access denial.

## Verified on 2026-09-18

- Samsung SM-M115F, Android 12: denied, disabled, controlled granted, and controlled timeout cases passed, including server reads of each saved event.
- Live location attempt: no real fix within 8 seconds; the event saved successfully with `locationStatus: timeout`.
- SOS countdown/cancellation/missing-contact and UI-state regression suite: 4 tests passed.
- Firestore emulator rules suite: 11 tests passed; rules deployed to `rideaware-dev`.
- `assembleDebug`, `assembleAndroidTest`, and `installDebug` succeeded.
- Temporary test contacts, events, and anonymous accounts removed. Device Location restored to off, app location permissions to denied, and mock-location app-op to default.
