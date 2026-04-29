package com.smartvoice.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity storing command execution history.
 */
@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawText: String,
    val commandType: String,
    val language: String,
    val success: Boolean,
    val responseMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)
