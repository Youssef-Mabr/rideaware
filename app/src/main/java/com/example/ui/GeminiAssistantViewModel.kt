package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAssistant
import com.example.ai.GeminiAssistantImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GeminiAssistantState(
  val response: String? = null,
  val loading: Boolean = false,
  val error: String? = null
)

class GeminiAssistantViewModel(
  private val assistant: GeminiAssistant = GeminiAssistantImpl()
) : ViewModel() {
  private val mutableState = MutableStateFlow(GeminiAssistantState())
  val state = mutableState.asStateFlow()

  fun ask(prompt: String) {
    if (mutableState.value.loading) return
    mutableState.value = GeminiAssistantState(loading = true)
    viewModelScope.launch {
      assistant.ask(prompt).fold(
        onSuccess = { mutableState.value = GeminiAssistantState(response = it) },
        onFailure = { mutableState.value = GeminiAssistantState(error = it.message ?: "Gemini request failed.") }
      )
    }
  }
}