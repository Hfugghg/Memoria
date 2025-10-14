package com.exp.memoria.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "raw_memory")
data class RawMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // [cite: 55]
    val user_query: String, // [cite: 56]
    val llm_response: String, // [cite: 57]
    val timestamp: Long // [cite: 58]
)