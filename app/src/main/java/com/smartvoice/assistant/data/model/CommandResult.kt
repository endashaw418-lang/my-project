package com.smartvoice.assistant.data.model

/**
 * Result of executing a voice command.
 */
data class CommandResult(
    val success: Boolean,
    val message: String,
    val command: Command,
    val responseLanguage: Language = command.language,
    val timestamp: Long = System.currentTimeMillis()
)
