package com.example.helmet

/**
 * Provisioned QR payloads for the software-only pairing pilot.
 * Replace this catalogue with a secured hardware registry before shipping
 * physical helmets. QR parsing is kept separate from the UI and Firestore.
 */
object HelmetQrCatalog {
  const val SAMPLE_HELMET_ID = "RA-DEMO-001"

  fun helmetIdFromQr(payload: String): String? {
    val candidate = payload.trim().uppercase()
    return candidate.takeIf { it == SAMPLE_HELMET_ID }
  }
}
