# Helmet QR pairing

The pairing screen is available from **Settings → Helmet pairing**. It is a software-only preparation flow: it displays a real QR code for the provisioned sample payload `RA-DEMO-001`, provides a simulated scan button, and accepts entered QR content to exercise error handling. It does not request camera access or communicate through Bluetooth or Wi-Fi.

Only `RA-DEMO-001` is provisioned in `HelmetQrCatalog` for this pilot. Empty or unrelated payloads are rejected before Firestore is written. A future hardware registry can replace that catalogue without changing the screen, ViewModel, or repository contract.

Pairing writes `helmetId` and server-generated `helmetPairedAt` into `users/{uid}`. Unpairing deletes those two fields. Firestore rules require the authenticated owner, the exact provisioned ID, and a server timestamp; paired values remain valid when unrelated profile fields such as FCM or `lastSeenAt` are updated.
