package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var lastAttemptedPrompt: String? = null

    init {
        checkApiKey()
    }

    private fun checkApiKey() {
        viewModelScope.launch {
            val isConfigured = repository.isApiKeyConfigured()
            _uiState.update { state ->
                state.copy(
                    isApiKeyConfigured = isConfigured,
                    errorMessage = if (!isConfigured) MISSING_API_KEY_MESSAGE else null,
                )
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onSend() {
        val promptText = _uiState.value.prompt.trim()
        if (promptText.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!_uiState.value.isApiKeyConfigured) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        lastAttemptedPrompt = promptText
        val userMessage = ChatMessage(text = promptText, isUser = true)

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        executeGeneration(promptText)
    }

    fun onRetry() {
        val promptText = lastAttemptedPrompt
            ?: _uiState.value.messages.lastOrNull { it.isUser }?.text
            ?: return

        if (!_uiState.value.isApiKeyConfigured) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        executeGeneration(promptText)
    }

    private fun executeGeneration(promptText: String) {
        viewModelScope.launch {
            repository.generateText(promptText).fold(
                onSuccess = { text ->
                    val assistantMessage = ChatMessage(text = text, isUser = false)
                    _uiState.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong",
                        )
                    }
                },
            )
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "API key not configured. Add GEMINI_API_KEY to local.properties and rebuild."

        fun factory(repository: GeminiRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository) as T
            }
    }
}
