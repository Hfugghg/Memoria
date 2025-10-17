package com.exp.memoria.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_header")
data class ConversationHeader(
    @PrimaryKey
    val conversationId: String,
    val creationTimestamp: Long,
    var lastUpdateTimestamp: Long
)
