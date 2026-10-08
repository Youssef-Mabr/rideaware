package com.example

import com.example.ai.GeminiResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiAssistantTest {
  @Test fun stage5aUsesFlashLiteModel() {
    assertEquals("gemini-3.5-flash-lite", com.example.ai.GeminiAssistantImpl.MODEL)
  }
  @Test fun parsesTextOutput() {
    assertEquals("Hello from Gemini", GeminiResponseParser.parseText("""{"outputs":[{"type":"text","text":"Hello from Gemini"}]}"""))
  }

  @Test fun emptyOutputReturnsNull() {
    assertNull(GeminiResponseParser.parseText("""{"outputs":[]}"""))
  }

  @Test fun parsesInteractionsStepsModelOutput() {
    val response = """{"steps":[{"type":"model_output","content":[{"type":"text","text":"Hello from RideAware"}]}]}"""
    assertEquals("Hello from RideAware", GeminiResponseParser.parseText(response))
  }

  @Test fun malformedOutputReturnsNull() {
    assertNull(GeminiResponseParser.parseText("{}"))
  }
}