package com.fahim.geminiApiComposeStarter.ui.chat

import java.util.UUID

/**
 * Represents a single message in the chat conversation.
 * @param id Stable unique identifier for LazyColumn keys.
 * @param text Content of the message.
 * @param isUser True if sent by the user, false if from Gemini assistant.
 * @param timestamp Timestamp in milliseconds when the message was created.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)
