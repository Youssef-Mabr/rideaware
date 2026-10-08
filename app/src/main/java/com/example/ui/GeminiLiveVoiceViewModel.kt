package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.ai.GeminiLiveVoiceAssistant
import com.example.ai.GeminiLiveVoiceAssistantImpl
import com.example.ai.GeminiLiveVoiceState
import kotlinx.coroutines.flow.StateFlow

class GeminiLiveVoiceViewModel(
  private val assistant: GeminiLiveVoiceAssistant = GeminiLiveVoiceAssistantImpl()
) : ViewModel() {
  val state: StateFlow<GeminiLiveVoiceState> = assistant.state
  fun start() = assistant.connect()
  fun stop() = assistant.disconnect()
  override fun onCleared() { assistant.disconnect(); super.onCleared() }
}