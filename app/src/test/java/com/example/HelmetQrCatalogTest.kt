package com.example

import com.example.helmet.HelmetQrCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HelmetQrCatalogTest {
  @Test fun provisionedSampleQrIsAcceptedWithWhitespaceAndCaseNormalization() {
    assertEquals("RA-DEMO-001", HelmetQrCatalog.helmetIdFromQr("  ra-demo-001  "))
  }

  @Test fun emptyAndUnknownQrPayloadsAreRejected() {
    assertNull(HelmetQrCatalog.helmetIdFromQr(""))
    assertNull(HelmetQrCatalog.helmetIdFromQr("RA-OTHER-002"))
    assertNull(HelmetQrCatalog.helmetIdFromQr("https://unrelated.example/qr"))
  }
}
