package com.smartvoice.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for command history operations.
 */
@Dao
interface CommandHistoryDao {

    @Query("SELECT * FROM command_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CommandHistoryEntity>>

    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<CommandHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CommandHistoryEntity)

    @Query("DELETE FROM command_history")
    suspend fun clearAll()

    @Query("DELETE FROM command_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM command_history")
    suspend fun getCount(): Int
}
