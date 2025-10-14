package com.exp.memoria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exp.memoria.data.local.entity.RawMemory

@Dao
interface RawMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rawMemory: RawMemory)

    // Placeholder function for now
    @Query("SELECT * FROM raw_memory ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentMemories(): List<RawMemory>
}