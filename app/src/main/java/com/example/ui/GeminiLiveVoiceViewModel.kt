package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.ai.GeminiLiveVoiceAssistant
import com.example.ai.GeminiLiveVoiceAssistantImpl
import com.example.ai.GeminiLiveVoiceState
import com.example.ai.LiveVoiceTurn
import com.example.model.SafetyAlert
import kotlinx.coroutines.flow.StateFlow

class GeminiLiveVoiceViewModel(
  private val assistant: GeminiLiveVoiceAssistant = GeminiLiveVoiceAssistantImpl()
) : ViewModel() {
  val state: StateFlow<GeminiLiveVoiceState> = assistant.state
  val errorMessage: StateFlow<String?> = assistant.errorMessage
  val statusDetail: StateFlow<String?> = assistant.statusDetail
  val latestTurn: StateFlow<LiveVoiceTurn?> = assistant.latestTurn

  fun start() = assistant.connect()
  fun stop() = assistant.disconnect()
  fun finishTurn() = assistant.finishUserTurn()
  fun announceSafetyAlert(alert: SafetyAlert, isSimulated: Boolean = true): Boolean =
    assistant.announceSafetyAlert(alert, isSimulated)

  override fun onCleared() {
    assistant.disconnect()
    super.onCleared()
  }
}