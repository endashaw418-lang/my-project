package com.smartvoice.assistant.data.repository

import com.smartvoice.assistant.data.local.AppDatabase
import com.smartvoice.assistant.data.local.CommandHistoryEntity
import com.smartvoice.assistant.data.model.Command
import com.smartvoice.assistant.data.model.CommandResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository managing command history persistence.
 */
class CommandRepository(private val database: AppDatabase) {

    private val dao = database.commandHistoryDao()

    fun getRecentHistory(limit: Int = 50): Flow<List<CommandHistoryEntity>> {
        return dao.getRecentHistory(limit)
    }

    fun getAllHistory(): Flow<List<CommandHistoryEntity>> {
        return dao.getAllHistory()
    }

    suspend fun saveCommandResult(command: Command, result: CommandResult) {
        val entity = CommandHistoryEntity(
            rawText = command.rawText,
            commandType = command.type.name,
            language = command.language.code,
            success = result.success,
            responseMessage = result.message,
            timestamp = command.timestamp
        )
        dao.insert(entity)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }
}
